package com.online_games_service.makao.service;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.model.Card;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.makao.dto.PlayCardRequest;
import com.online_games_service.makao.dto.PlayerCardView;
import com.online_games_service.makao.dto.DrawCardResponse;
import com.online_games_service.makao.dto.EndGameRequest;
import com.online_games_service.makao.dto.GameStateMessage;
import com.online_games_service.makao.model.MakaoDeck;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.model.MakaoGameResult;
import com.online_games_service.makao.repository.mongo.MakaoGameResultRepository;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakaoGameService {

    private final MakaoGameRedisRepository gameRepository;
    private final MakaoGameResultRepository gameResultRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange gameEventsExchange;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();
    private final ScheduledExecutorService turnTimeoutScheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> turnTimeouts = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> botMoveSchedules = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> gameInProgress = new ConcurrentHashMap<>();

    @Value("${makao.turn-timeout-seconds:65}")
    private long turnTimeoutSeconds;

    @Value("${makao.bot-delay-min-ms:1000}")
    private long botDelayMinMs;

    @Value("${makao.bot-delay-max-ms:3000}")
    private long botDelayMaxMs;

    @Value("${makao.amqp.routing.finish:makao.finish}")
    private String finishRoutingKey;

    private static final String KEY_USER_ROOM_BY_ID = "game:user-room:id:";

    public void forceEndGame(EndGameRequest request) {
        if (request == null || request.getRoomId() == null || request.getRoomId().isBlank()) {
            throw new IllegalArgumentException("roomId is required to end the game");
        }

        MakaoGame game = gameRepository.findById(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + request.getRoomId()));

        endGame(game);
    }

    public DrawCardResponse drawCard(String userId) {
        if (userId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room not found for player: " + userId);
        }

        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        if (!userId.equals(game.getActivePlayerId())) {
            throw new IllegalStateException("It is not this player's turn");
        }

        if (game.getDrawnCard() != null) {
            throw new IllegalStateException("Player has already drawn a card this turn");
        }

        if (game.isSpecialEffectActive()) {
            throw new IllegalStateException("Special effect is active; accept the effect instead of drawing");
        }

        Card drawn = drawWithRecycle(game);
        if (drawn == null) {
            throw new IllegalStateException("No cards left to draw");
        }

        game.addCardToHand(userId, drawn);
        game.setDrawnCard(drawn);

        boolean playable = isPlayable(game, drawn);
        String playerName = getPlayerDisplayName(game, userId);
        game.addMoveLog(String.format("%s drew a card", playerName));

        if (playable) {
            game.setActivePlayerPlayableCards(List.of(drawn));
            saveAndBroadcast(game);
            scheduleTurnTimeout(game);
            return new DrawCardResponse(drawn, true);
        }

        game.addMoveLog(String.format("%s skipped after drawing", playerName));
        game.setActivePlayerPlayableCards(new ArrayList<>());
        game.setDrawnCard(null);
        nextTurn(game);
        return new DrawCardResponse(drawn, false);
    }

    public MakaoGame playDrawnCard(PlayCardRequest request, String userId) {
        if (userId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room not found for player: " + userId);
        }

        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        if (!userId.equals(game.getActivePlayerId())) {
            throw new IllegalStateException("It is not this player's turn");
        }

        Card drawn = game.getDrawnCard();
        if (drawn == null) {
            throw new IllegalStateException("No drawn card to play");
        }

        PlayCardRequest derived = new PlayCardRequest();
        derived.setCardRank(drawn.getRank());
        derived.setCardSuit(drawn.getSuit());
        derived.setRequestRank(request != null ? request.getRequestRank() : null);
        derived.setRequestSuit(request != null ? request.getRequestSuit() : null);

        // Clear drawnCard before delegating to avoid re-use
        game.setDrawnCard(null);
        saveAndBroadcast(game);

        return playCard(derived, userId);
    }

    public MakaoGame skipDrawnCard(String userId) {
        if (userId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room not found for player: " + userId);
        }

        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        if (!userId.equals(game.getActivePlayerId())) {
            throw new IllegalStateException("It is not this player's turn");
        }

        if (game.getDrawnCard() == null) {
            throw new IllegalStateException("No drawn card to skip");
        }

        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        nextTurn(game);
        return game;
    }

    public MakaoGame acceptEffect(String userId) {
        if (userId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room not found for player: " + userId);
        }

        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        if (!userId.equals(game.getActivePlayerId())) {
            throw new IllegalStateException("It is not this player's turn");
        }

        boolean hasEffect = game.isSpecialEffectActive()
                || game.getPendingDrawCount() > 0
                || game.getPendingSkipTurns() > 0;
        if (!hasEffect) {
            throw new IllegalStateException("No pending special effect to accept");
        }

        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        applySpecialEffectPenalty(game, userId);
        return game;
    }

    public MakaoGame playCard(PlayCardRequest request, String userId) {
        if (userId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("Room not found for player: " + userId);
        }

        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        if (game.getDiscardDeck() == null || game.getDiscardDeck().isEmpty()) {
            throw new IllegalStateException("Discard pile is empty; game is not initialized correctly");
        }

        if (game.getActivePlayerId() == null || !game.getActivePlayerId().equals(userId)) {
            throw new IllegalStateException("It is not this player's turn");
        }

        List<Card> hand = game.getPlayersHands().get(userId);
        if (hand == null) {
            throw new IllegalArgumentException("Player is not part of this game");
        }

        Card playedCard = new Card(request.getCardSuit(), request.getCardRank());

        if (!hand.contains(playedCard)) {
            throw new IllegalArgumentException("Player does not have the specified card");
        }

        if (playedCard.getRank() == CardRank.JACK && request.getRequestRank() == null) {
            throw new IllegalArgumentException("Jack requires requestRank to be provided");
        }
        if (playedCard.getRank() == CardRank.ACE && request.getRequestSuit() == null) {
            throw new IllegalArgumentException("Ace requires requestSuit to be provided");
        }

        if (!isPlayable(game, playedCard)) {
            throw new IllegalStateException("This card cannot be played on the current top card");
        }

        hand.remove(playedCard);

        // Generate move log for the played card
        String playerName = getPlayerDisplayName(game, userId);
        String moveLog = formatMoveLog(playerName, playedCard, request);
        game.addMoveLog(moveLog);

        if (hand.isEmpty()) {
            endGame(game);
            return game;
        }

        if (game.getDiscardDeck() == null) {
            game.setDiscardDeck(new MakaoDeck(new ArrayList<>()));
        }
        game.getDiscardDeck().addCard(playedCard);

        setCardEffect(game, playedCard, request);

        nextTurn(game);
        return game;
    }

    private boolean isPlayable(MakaoGame game, Card card) {
        Card current = game.getCurrentCard();
        if (current == null) {
            throw new IllegalStateException("Discard pile has no top card");
        }

        if (game.isSpecialEffectActive()) {
            CardRank currentRank = current.getRank();
            if (currentRank == CardRank.TWO || currentRank == CardRank.THREE) {
                return card.getRank() == CardRank.TWO || card.getRank() == CardRank.THREE;
            }
            if (currentRank == CardRank.FOUR) {
                return card.getRank() == CardRank.FOUR;
            }
        }

        // If the previous player demanded a specific rank (Jack effect), respect it
        // unless Jack is played again
        if (game.getDemandedRank() != null) {
            return card.getRank() == game.getDemandedRank() || card.getRank() == CardRank.JACK;
        }

        // If the previous player demanded a specific suit (Ace effect), respect it
        // unless Ace is played again
        if (game.getDemandedSuit() != null) {
            return card.getSuit() == game.getDemandedSuit() || card.getRank() == CardRank.ACE;
        }

        return card.getRank() == current.getRank() || card.getSuit() == current.getSuit();
    }

    private void setCardEffect(MakaoGame game, Card card, PlayCardRequest request) {
        // Reset demands unless overwritten by effect
        game.setDemandedRank(null);
        game.setDemandedSuit(null);
        game.setSpecialEffectActive(false);
        game.setEffectNotification(null);

        CardRank rank = card.getRank();
        if (rank == null) {
            return;
        }

        switch (rank) {
            case TWO:
                game.setSpecialEffectActive(true);
                game.setPendingDrawCount(game.getPendingDrawCount() + 2);
                game.setEffectNotification(String.format("Next player must draw %d cards or play a 2/3!", game.getPendingDrawCount()));
                break;
            case THREE:
                game.setSpecialEffectActive(true);
                game.setPendingDrawCount(game.getPendingDrawCount() + 3);
                game.setEffectNotification(String.format("Next player must draw %d cards or play a 2/3!", game.getPendingDrawCount()));
                break;
            case FOUR:
                game.setSpecialEffectActive(true);
                game.setPendingSkipTurns(game.getPendingSkipTurns() + 1);
                game.setEffectNotification(String.format("Next player will skip %d turn(s) or play a 4!", game.getPendingSkipTurns()));
                break;
            case JACK:
                game.setDemandedRank(request.getRequestRank());
                game.setEffectNotification(String.format("Jack demands %s!", formatRankName(request.getRequestRank())));
                break;
            case ACE:
                game.setDemandedSuit(request.getRequestSuit());
                game.setEffectNotification(String.format("Ace demands %s!", formatSuitName(request.getRequestSuit())));
                break;
            case KING:
                if (card.getSuit() == CardSuit.HEARTS || card.getSuit() == CardSuit.SPADES) {
                    game.setReverseMovement(!game.isReverseMovement());
                    game.setEffectNotification("Play direction reversed!");
                }
                break;
            default:
                break;
        }
    }

    private void nextTurn(MakaoGame game) {
        List<String> order = game.getPlayersOrderIds();
        if (order == null || order.isEmpty()) {
            return;
        }

        boolean onlyBots = order.stream().allMatch(this::isBot);
        if (onlyBots) {
            endGame(game);
            return;
        }

        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        int currentIndex = order.indexOf(game.getActivePlayerId());
        boolean reverse = game.isReverseMovement();
        int nextIndex = currentIndex == -1 ? 0 : currentIndex;

        int maxHops = order.size() * 2; // safety to avoid infinite loop if data is inconsistent
        for (int i = 0; i < maxHops; i++) {
            nextIndex = reverse
                    ? (nextIndex - 1 + order.size()) % order.size()
                    : (nextIndex + 1) % order.size();

            String candidate = order.get(nextIndex);
            int skipTurns = game.getPlayersSkipTurns().getOrDefault(candidate, 0);

            if (skipTurns > 0) {
                String playerName = getPlayerDisplayName(game, candidate);
                game.addMoveLog(String.format("%s skips a turn (remaining: %d)", playerName, skipTurns - 1));
                game.setEffectNotification(String.format("%s skips a turn due to a 4", playerName));
                game.getPlayersSkipTurns().put(candidate, skipTurns - 1);
                saveAndBroadcast(game);
                continue; // skip this player and look for the next one
            }

            List<Card> playable = gatherPlayableCards(game, candidate);
            game.setActivePlayerPlayableCards(playable);

            if (playable.isEmpty() && game.isSpecialEffectActive()) {
                applySpecialEffectPenalty(game, candidate);
                return;
            }

            if (isBot(candidate)) {
                game.setActivePlayerId(candidate);
                saveAndBroadcast(game);
                scheduleBotMove(game.getRoomId(), candidate, playable);
                return;
            }

            game.setActivePlayerId(candidate);
            saveAndBroadcast(game);
            scheduleTurnTimeout(game);
            return;
        }
    }

    private void applySpecialEffectPenalty(MakaoGame game, String playerId) {
        Card current = game.getCurrentCard();
        if (current == null) {
            return;
        }

        String playerName = getPlayerDisplayName(game, playerId);

        switch (current.getRank()) {
            case TWO:
            case THREE:
                int drawCount = game.getPendingDrawCount();
                if (drawCount > 0 && game.getDrawDeck() != null) {
                    int actualDrawn = 0;
                    for (int i = 0; i < drawCount; i++) {
                        Card drawn = drawWithRecycle(game);
                        if (drawn != null) {
                            game.addCardToHand(playerId, drawn);
                            actualDrawn++;
                        }
                    }
                    String notification = String.format("%s draws %d card%s", playerName, actualDrawn, actualDrawn != 1 ? "s" : "");
                    game.addMoveLog(notification);
                    game.setEffectNotification(notification);
                }
                game.setPendingDrawCount(0);
                break;
            case FOUR:
                int skips = game.getPendingSkipTurns();
                if (skips > 0) {
                    int existing = game.getPlayersSkipTurns().getOrDefault(playerId, 0);
                    game.getPlayersSkipTurns().put(playerId, existing + skips);
                    String notification = String.format("%s will skip %d turn%s due to a 4", playerName, skips, skips != 1 ? "s" : "");
                    game.addMoveLog(notification);
                    game.setEffectNotification(notification);
                }
                game.setPendingSkipTurns(0);
                break;
            default:
                break;
        }

        game.setSpecialEffectActive(false);
        saveAndBroadcast(game);
        nextTurn(game);
    }

    private List<Card> gatherPlayableCards(MakaoGame game, String playerId) {
        List<Card> playable = new ArrayList<>();
        List<Card> hand = game.getPlayersHands().get(playerId);
        if (hand == null || hand.isEmpty()) {
            return playable;
        }

        for (Card card : hand) {
            if (isPlayable(game, card)) {
                playable.add(card);
            }
        }
        return playable;
    }

    public void initializeGameAfterStart(String roomId) {
        MakaoGame game = gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for roomId: " + roomId));

        String activePlayerId = game.getActivePlayerId();
        if (activePlayerId == null) {
            return;
        }

        List<Card> playable = gatherPlayableCards(game, activePlayerId);
        game.setActivePlayerPlayableCards(playable);
        game.addMoveLog("Game started!");
        saveAndBroadcast(game);

        if (isBot(activePlayerId)) {
            // Schedule bot move with delay instead of immediate execution
            scheduleBotMove(game.getRoomId(), activePlayerId, playable);
        } else {
            scheduleTurnTimeout(game);
        }
    }

    private void endGame(MakaoGame game) {
        cancelTurnTimeout(game.getRoomId());
        cancelBotMove(game.getRoomId());
        game.setStatus(RoomStatus.FINISHED);
        game.setSpecialEffectActive(false);
        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());
        game.addMoveLog("Game ended!");

        Map<String, List<Card>> hands = game.getPlayersHands();
        Map<String, Integer> ranking = new HashMap<>();
        Map<String, Integer> placement = new LinkedHashMap<>();

        if (hands != null) {
            hands.forEach((playerId, cards) -> ranking.put(playerId, calculateHandValue(cards)));

            final int[] index = {0};
            final Integer[] lastScore = {null};
            final int[] lastPlace = {0};

            ranking.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEachOrdered(entry -> {
                        index[0]++;
                        int score = entry.getValue();
                        int place = (lastScore[0] != null && lastScore[0].equals(score))
                                ? lastPlace[0]
                                : index[0];
                        placement.put(entry.getKey(), place);
                        lastScore[0] = score;
                        lastPlace[0] = place;
                    });
        }

        game.setRanking(ranking);
        game.setPlacement(placement);

        persistGameResult(game);

        GameFinishMessage message = new GameFinishMessage(
                game.getRoomId(),
                RoomStatus.FINISHED);

        rabbitTemplate.convertAndSend(gameEventsExchange.getName(), finishRoutingKey, message);
        saveAndBroadcast(game);

        if (game.getRoomId() != null) {
            gameRepository.deleteById(game.getRoomId());
        }
    }

    private void persistGameResult(MakaoGame game) {
        if (game == null) {
            return;
        }

        String gameId = game.getGameId() != null ? game.getGameId() : game.getRoomId();
        if (gameId == null) {
            log.warn("Skipping persistence: game has no id");
            return;
        }

        MakaoGameResult result = new MakaoGameResult(
            gameId,
            game.getMaxPlayers(),
            game.getPlayersUsernames() != null ? new HashMap<>(game.getPlayersUsernames()) : new HashMap<>(),
            game.getRanking() != null ? new HashMap<>(game.getRanking()) : new HashMap<>(),
            game.getPlacement() != null ? new HashMap<>(game.getPlacement()) : new HashMap<>(),
            game.getLosers() != null ? new ArrayList<>(game.getLosers()) : new ArrayList<>());

        try {
            gameResultRepository.save(result);
        } catch (Exception e) {
            log.error("Failed to persist Makao game result for {}", game.getGameId(), e);
        }
    }

    private void saveAndBroadcast(MakaoGame game) {
        if (game == null) {
            return;
        }
        gameRepository.save(game);
        broadcastPlayerStates(game);
    }

    private void broadcastPlayerStates(MakaoGame game) {
        if (game == null || game.getPlayersHands() == null) {
            return;
        }

        Map<String, List<Card>> hands = game.getPlayersHands();
        Map<String, Integer> cardsAmount = new HashMap<>();
        hands.forEach((pid, cards) -> cardsAmount.put(pid, cards != null ? cards.size() : 0));

        List<Card> activePlayable = game.getActivePlayerPlayableCards() != null
                ? game.getActivePlayerPlayableCards()
                : new ArrayList<>();

        for (Map.Entry<String, List<Card>> entry : hands.entrySet()) {
            String playerId = entry.getKey();
            if (isBot(playerId)) {
                continue;
            }

            List<Card> hand = entry.getValue() != null ? entry.getValue() : new ArrayList<>();
            List<PlayerCardView> myCards = new ArrayList<>();
            boolean isActive = playerId.equals(game.getActivePlayerId());

            for (Card card : hand) {
                boolean playable = isActive && containsCard(activePlayable, card);
                myCards.add(new PlayerCardView(card, playable));
            }

            GameStateMessage message = new GameStateMessage(
                    game.getRoomId(),
                    game.getActivePlayerId(),
                    game.getCurrentCard(),
                    myCards,
                    new HashMap<>(cardsAmount),
                    game.getPlayersSkipTurns() != null ? new HashMap<>(game.getPlayersSkipTurns()) : new HashMap<>(),
                    game.isSpecialEffectActive(),
                    game.getDemandedRank(),
                    game.getDemandedSuit(),
                    game.getRanking() != null ? new HashMap<>(game.getRanking()) : new HashMap<>(),
                    game.getPlacement() != null ? new HashMap<>(game.getPlacement()) : new HashMap<>(),
                    game.getLosers() != null ? new ArrayList<>(game.getLosers()) : new ArrayList<>(),
                    game.getStatus(),
                    game.getDrawDeck() != null ? game.getDrawDeck().size() : 0,
                    game.getDiscardDeck() != null ? game.getDiscardDeck().size() : 0,
                    game.getLastMoveLog(),
                    game.getEffectNotification(),
                    game.getMoveHistory() != null ? new ArrayList<>(game.getMoveHistory()) : new ArrayList<>()
            );

            messagingTemplate.convertAndSend("/topic/makao/" + playerId, message);
        }
    }

    private boolean containsCard(List<Card> cards, Card target) {
        if (cards == null || target == null) {
            return false;
        }
        for (Card c : cards) {
            if (c != null && c.getRank() == target.getRank() && c.getSuit() == target.getSuit()) {
                return true;
            }
        }
        return false;
    }

    private int calculateHandValue(List<Card> hand) {
        if (hand == null || hand.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (Card card : hand) {
            if (card == null || card.getRank() == null) {
                continue;
            }
            sum += card.getRank().ordinal() + 2; // assign minimal pip values starting at 2
        }
        return sum;
    }

    //
    // Bot logic
    //
    private boolean isBot(String playerId) {
        return playerId != null && playerId.startsWith("bot-");
    }

    private void handleBotTurn(MakaoGame game, String botId, List<Card> playableCards) {
        String botName = getPlayerDisplayName(game, botId);

        if (playableCards != null && !playableCards.isEmpty()) {
            Card toPlay = playableCards.get(random.nextInt(playableCards.size()));
            CardRank reqRank = toPlay.getRank() == CardRank.JACK ? randomRankDemand() : null;
            CardSuit reqSuit = toPlay.getRank() == CardRank.ACE ? randomSuitDemand() : null;
            playCardAsBot(game, botId, toPlay, reqRank, reqSuit);
            return;
        }

        // Bot draws a card
        Card drawn = drawWithRecycle(game);
        if (drawn != null) {
            game.addCardToHand(botId, drawn);
            game.setDrawnCard(drawn);
            game.addMoveLog(String.format("%s drew a card", botName));

            if (isPlayable(game, drawn)) {
                CardRank reqRank = drawn.getRank() == CardRank.JACK ? randomRankDemand() : null;
                CardSuit reqSuit = drawn.getRank() == CardRank.ACE ? randomSuitDemand() : null;
                playCardAsBot(game, botId, drawn, reqRank, reqSuit);
                return;
            }
        }

        // Bot skips turn after drawing non-playable card
        game.addMoveLog(String.format("%s skipped after drawing", botName));
        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());
    }

    private void playCardAsBot(MakaoGame game, String botId, Card card, CardRank requestRank, CardSuit requestSuit) {
        List<Card> hand = game.getPlayersHands().get(botId);
        if (hand == null || !hand.contains(card)) {
            return;
        }

        PlayCardRequest req = new PlayCardRequest();
        req.setCardRank(card.getRank());
        req.setCardSuit(card.getSuit());
        req.setRequestRank(requestRank);
        req.setRequestSuit(requestSuit);

        if (!isPlayable(game, card)) {
            game.setDrawnCard(null);
            game.setActivePlayerPlayableCards(new ArrayList<>());
            return;
        }

        hand.remove(card);

        // Generate move log for bot
        String botName = getPlayerDisplayName(game, botId);
        String moveLog = formatMoveLog(botName, card, req);
        game.addMoveLog(moveLog);

        if (hand.isEmpty()) {
            endGame(game);
            return;
        }

        if (game.getDiscardDeck() == null) {
            game.setDiscardDeck(new MakaoDeck(new ArrayList<>()));
        }
        game.getDiscardDeck().addCard(card);

        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        setCardEffect(game, card, req);
    }

    private CardRank randomRankDemand() {
        CardRank[] ranks = CardRank.values();
        return ranks[random.nextInt(ranks.length)];
    }

    private CardSuit randomSuitDemand() {
        CardSuit[] suits = CardSuit.values();
        return suits[random.nextInt(suits.length)];
    }

    private Card drawWithRecycle(MakaoGame game) {
        if (game.getDrawDeck() == null) {
            return null;
        }

        Card drawn = game.getDrawDeck().draw();
        if (drawn != null) {
            return drawn;
        }

        // Recycle discard into draw, leaving top card
        if (game.getDiscardDeck() == null || game.getDiscardDeck().size() <= 1) {
            return null;
        }

        List<Card> discardCards = new ArrayList<>(game.getDiscardDeck().getCards());
        Card top = discardCards.remove(discardCards.size() - 1);

        game.getDiscardDeck().clear();
        game.getDiscardDeck().addCard(top);

        MakaoDeck newDraw = new MakaoDeck(new ArrayList<>(discardCards));
        newDraw.shuffle();
        game.setDrawDeck(newDraw);

        return game.getDrawDeck().draw();
    }

    private void scheduleTurnTimeout(MakaoGame game) {
        if (game == null || game.getRoomId() == null) {
            return;
        }
        cancelTurnTimeout(game.getRoomId());

        if (game.getStatus() != RoomStatus.PLAYING) {
            return;
        }

        String activePlayer = game.getActivePlayerId();
        if (activePlayer == null || isBot(activePlayer)) {
            return;
        }

        ScheduledFuture<?> future = turnTimeoutScheduler.schedule(
                () -> handleTurnTimeout(game.getRoomId(), activePlayer),
                turnTimeoutSeconds,
                TimeUnit.SECONDS);
        turnTimeouts.put(game.getRoomId(), future);
    }

    @PreDestroy
    public void shutdown() {
        // Cancel all pending bot moves
        botMoveSchedules.values().forEach(future -> future.cancel(false));
        botMoveSchedules.clear();
        gameInProgress.clear();

        if (turnTimeoutScheduler != null && !turnTimeoutScheduler.isShutdown()) {
            turnTimeoutScheduler.shutdown();
            log.info("MakaoGameService: Shut down turnTimeoutScheduler");
        }
    }

    private void cancelTurnTimeout(String roomId) {
        if (roomId == null) {
            return;
        }
        ScheduledFuture<?> future = turnTimeouts.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void handleTurnTimeout(String roomId, String timedOutPlayer) {
        // Remove the triggered timer so a new one can be scheduled without being cancelled later in this call stack.
        turnTimeouts.remove(roomId);
        try {
            MakaoGame game = gameRepository.findById(roomId).orElse(null);
            if (game == null) {
                return;
            }

            if (game.getStatus() != RoomStatus.PLAYING) {
                return;
            }

            if (!timedOutPlayer.equals(game.getActivePlayerId())) {
                return;
            }

            int nextBot = game.getBotCounter() + 1;
            String botId = "bot-" + nextBot;
            game.setBotCounter(nextBot);

            Map<String, List<Card>> hands = new HashMap<>(game.getPlayersHands());
            List<Card> timedOutHand = hands.getOrDefault(timedOutPlayer, new ArrayList<>());
            hands.remove(timedOutPlayer);
            hands.put(botId, timedOutHand);
            game.setPlayersHands(hands);

            Map<String, Integer> skipTurns = game.getPlayersSkipTurns() != null
                    ? new HashMap<>(game.getPlayersSkipTurns())
                    : new HashMap<>();
            int pendingSkips = skipTurns.getOrDefault(timedOutPlayer, 0);
            skipTurns.remove(timedOutPlayer);
            skipTurns.put(botId, pendingSkips);
            game.setPlayersSkipTurns(skipTurns);

            Map<String, String> usernames = game.getPlayersUsernames() != null
                    ? new HashMap<>(game.getPlayersUsernames())
                    : new HashMap<>();
            usernames.remove(timedOutPlayer);
            usernames.put(botId, "Bot " + nextBot);
            game.setPlayersUsernames(usernames);

            List<String> updatedOrder = game.getPlayersOrderIds() != null
                    ? new ArrayList<>(game.getPlayersOrderIds())
                    : new ArrayList<>();
            int idx = updatedOrder.indexOf(timedOutPlayer);
            if (idx >= 0) {
                updatedOrder.set(idx, botId);
            }
            game.setPlayersOrderIds(updatedOrder);

            List<String> losers = game.getLosers() != null
                    ? new ArrayList<>(game.getLosers())
                    : new ArrayList<>();
            if (!losers.contains(timedOutPlayer)) {
                losers.add(timedOutPlayer);
            }
            game.setLosers(losers);

            game.setActivePlayerId(botId);

            List<Card> playable = gatherPlayableCards(game, botId);
            game.setActivePlayerPlayableCards(playable);

            // Add notification for player being replaced
            game.addMoveLog(String.format("Player timed out and was replaced by %s", "Bot " + nextBot));

            handleBotTurn(game, botId, playable);
            nextTurn(game);
        } catch (Exception e) {
            log.error("Failed to handle turn timeout for room {} player {}", roomId, timedOutPlayer, e);
        }
    }

    /**
     * Schedules a bot move with a random delay (1-3 seconds) to simulate human thinking.
     */
    private void scheduleBotMove(String roomId, String botId, List<Card> playableCards) {
        if (roomId == null || botId == null) {
            return;
        }

        // Cancel any existing scheduled bot move for this room
        cancelBotMove(roomId);

        // Calculate random delay between botDelayMinMs and botDelayMaxMs
        long delay = botDelayMinMs + (long) (random.nextDouble() * (botDelayMaxMs - botDelayMinMs));

        log.debug("Scheduling bot move for {} in room {} with delay {}ms", botId, roomId, delay);

        ScheduledFuture<?> future = turnTimeoutScheduler.schedule(
                () -> executeBotMove(roomId, botId, playableCards),
                delay,
                TimeUnit.MILLISECONDS);
        botMoveSchedules.put(roomId, future);
    }

    private void cancelBotMove(String roomId) {
        if (roomId == null) {
            return;
        }
        ScheduledFuture<?> future = botMoveSchedules.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Executes the bot move after the delay.
     * Reloads game state from Redis to ensure consistency.
     */
    private void executeBotMove(String roomId, String botId, List<Card> originalPlayableCards) {
        try {
            // Prevent concurrent bot moves for the same game
            AtomicBoolean inProgress = gameInProgress.computeIfAbsent(roomId, k -> new AtomicBoolean(false));
            if (!inProgress.compareAndSet(false, true)) {
                log.debug("Bot move already in progress for room {}", roomId);
                return;
            }

            try {
                // Reload game state from Redis for consistency
                MakaoGame game = gameRepository.findById(roomId).orElse(null);
                if (game == null) {
                    log.warn("Game not found for room {} when executing bot move", roomId);
                    return;
                }

                if (game.getStatus() != RoomStatus.PLAYING) {
                    log.debug("Game {} is not in PLAYING status, skipping bot move", roomId);
                    return;
                }

                // Verify the bot is still the active player
                if (!botId.equals(game.getActivePlayerId())) {
                    log.debug("Bot {} is no longer active player in room {}", botId, roomId);
                    return;
                }

                // Recalculate playable cards from fresh game state
                List<Card> playableCards = gatherPlayableCards(game, botId);
                game.setActivePlayerPlayableCards(playableCards);

                // Execute the bot's turn
                handleBotTurn(game, botId, playableCards);

                // Move to next turn (this will save and broadcast)
                nextTurn(game);
            } finally {
                inProgress.set(false);
            }
        } catch (Exception e) {
            log.error("Failed to execute bot move for room {} bot {}", roomId, botId, e);
            gameInProgress.computeIfPresent(roomId, (k, v) -> {
                v.set(false);
                return v;
            });
        }
    }

    /**
     * Gets the display name for a player (username or bot name).
     */
    private String getPlayerDisplayName(MakaoGame game, String playerId) {
        if (playerId == null) {
            return "Unknown";
        }
        if (game.getPlayersUsernames() != null && game.getPlayersUsernames().containsKey(playerId)) {
            return game.getPlayersUsernames().get(playerId);
        }
        return playerId;
    }

    /**
     * Formats a move log message for a played card.
     */
    private String formatMoveLog(String playerName, Card card, PlayCardRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(playerName).append(" played ").append(formatCardName(card));

        if (card.getRank() == CardRank.JACK && request != null && request.getRequestRank() != null) {
            sb.append(" (demands ").append(formatRankName(request.getRequestRank())).append(")");
        } else if (card.getRank() == CardRank.ACE && request != null && request.getRequestSuit() != null) {
            sb.append(" (demands ").append(formatSuitName(request.getRequestSuit())).append(")");
        }

        return sb.toString();
    }

    /**
     * Formats a card name for display (e.g., "Ace of Hearts").
     */
    private String formatCardName(Card card) {
        if (card == null) {
            return "unknown card";
        }
        return formatRankName(card.getRank()) + " of " + formatSuitName(card.getSuit());
    }

    /**
     * Formats a card rank for display.
     */
    private String formatRankName(CardRank rank) {
        if (rank == null) {
            return "unknown";
        }
        String name = rank.name();
        // Convert from enum name to title case (e.g., "ACE" -> "Ace", "TWO" -> "2")
        switch (rank) {
            case TWO: return "2";
            case THREE: return "3";
            case FOUR: return "4";
            case FIVE: return "5";
            case SIX: return "6";
            case SEVEN: return "7";
            case EIGHT: return "8";
            case NINE: return "9";
            case TEN: return "10";
            default: return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }

    /**
     * Formats a card suit for display.
     */
    private String formatSuitName(CardSuit suit) {
        if (suit == null) {
            return "unknown";
        }
        String name = suit.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
