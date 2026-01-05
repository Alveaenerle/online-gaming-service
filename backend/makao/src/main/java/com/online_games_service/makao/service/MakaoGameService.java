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
        if (playable) {
            game.setActivePlayerPlayableCards(List.of(drawn));
            saveAndBroadcast(game);
            return new DrawCardResponse(drawn, true);
        }

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

        CardRank rank = card.getRank();
        if (rank == null) {
            return;
        }

        switch (rank) {
            case TWO:
                game.setSpecialEffectActive(true);
                game.setPendingDrawCount(game.getPendingDrawCount() + 2);
                break;
            case THREE:
                game.setSpecialEffectActive(true);
                game.setPendingDrawCount(game.getPendingDrawCount() + 3);
                break;
            case FOUR:
                game.setSpecialEffectActive(true);
                game.setPendingSkipTurns(game.getPendingSkipTurns() + 1);
                break;
            case JACK:
                game.setDemandedRank(request.getRequestRank());
                break;
            case ACE:
                game.setDemandedSuit(request.getRequestSuit());
                break;
            case KING:
                if (card.getSuit() == CardSuit.HEARTS || card.getSuit() == CardSuit.SPADES) {
                    game.setReverseMovement(!game.isReverseMovement());
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
                game.getPlayersSkipTurns().put(candidate, skipTurns - 1);
                saveAndBroadcast(game);
                continue; // skip this player and look for the next one
            }

            List<Card> playable = gatherPlayableCards(game, candidate);
            game.setActivePlayerPlayableCards(playable);

            if (playable.isEmpty() && game.isSpecialEffectActive()) {
                applySpecialEffectPenalty(game, candidate);
                saveAndBroadcast(game);
                return;
            }

            if (isBot(candidate)) {
                game.setActivePlayerId(candidate);
                handleBotTurn(game, candidate, playable);
                saveAndBroadcast(game);
                nextTurn(game);
                return;
            }

            game.setActivePlayerId(candidate);
            saveAndBroadcast(game);
            return;
        }
    }

    private void applySpecialEffectPenalty(MakaoGame game, String playerId) {
        Card current = game.getCurrentCard();
        if (current == null) {
            return;
        }

        switch (current.getRank()) {
            case TWO:
            case THREE:
                int drawCount = game.getPendingDrawCount();
                if (drawCount > 0 && game.getDrawDeck() != null) {
                    for (int i = 0; i < drawCount; i++) {
                        Card drawn = game.getDrawDeck().draw();
                        if (drawn != null) {
                            game.addCardToHand(playerId, drawn);
                        }
                    }
                }
                game.setPendingDrawCount(0);
                break;
            case FOUR:
                int skips = game.getPendingSkipTurns();
                if (skips > 0) {
                    int existing = game.getPlayersSkipTurns().getOrDefault(playerId, 0);
                    game.getPlayersSkipTurns().put(playerId, existing + skips);
                }
                game.setPendingSkipTurns(0);
                break;
            default:
                break;
        }

        game.setSpecialEffectActive(false);
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
        saveAndBroadcast(game);

        if (isBot(activePlayerId)) {
            handleBotTurn(game, activePlayerId, playable);
            nextTurn(game);
        }
    }

    private void endGame(MakaoGame game) {
        game.setStatus(RoomStatus.FINISHED);
        game.setSpecialEffectActive(false);
        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

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
            game.getPlacement() != null ? new HashMap<>(game.getPlacement()) : new HashMap<>());

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
                    game.getDiscardDeck() != null ? game.getDiscardDeck().size() : 0
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
        if (playableCards != null && !playableCards.isEmpty()) {
            Card toPlay = playableCards.get(random.nextInt(playableCards.size()));
            CardRank reqRank = toPlay.getRank() == CardRank.JACK ? randomRankDemand() : null;
            CardSuit reqSuit = toPlay.getRank() == CardRank.ACE ? randomSuitDemand() : null;
            playCardAsBot(game, botId, toPlay, reqRank, reqSuit);
            return;
        }

        Card drawn = drawWithRecycle(game);
        if (drawn != null) {
            game.addCardToHand(botId, drawn);
            game.setDrawnCard(drawn);

            if (isPlayable(game, drawn)) {
                CardRank reqRank = drawn.getRank() == CardRank.JACK ? randomRankDemand() : null;
                CardSuit reqSuit = drawn.getRank() == CardRank.ACE ? randomSuitDemand() : null;
                playCardAsBot(game, botId, drawn, reqRank, reqSuit);
                return;
            }
        }

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
}
