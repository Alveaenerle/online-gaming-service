package com.online_games_service.makao.service;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.model.Card;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.makao.dto.PlayCardRequest;
import com.online_games_service.makao.dto.DrawCardResponse;
import com.online_games_service.makao.model.MakaoDeck;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakaoGameService {

    private final MakaoGameRedisRepository gameRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange gameEventsExchange;
    private final Random random = new Random();

    @Value("${makao.amqp.routing.finish:makao.finish}")
    private String finishRoutingKey;

    private static final String KEY_USER_ROOM_BY_ID = "game:user-room:id:";

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

        Card drawn = drawWithRecycle(game);
        if (drawn == null) {
            throw new IllegalStateException("No cards left to draw");
        }

        game.addCardToHand(userId, drawn);
        game.setDrawnCard(drawn);

        boolean playable = isPlayable(game, drawn);
        if (playable) {
            game.setActivePlayerPlayableCards(List.of(drawn));
            gameRepository.save(game);
            return new DrawCardResponse(drawn, true);
        }

        game.setActivePlayerPlayableCards(new ArrayList<>());
        game.setDrawnCard(null);
        nextTurn(game);
        gameRepository.save(game);
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
        gameRepository.save(game);

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
        gameRepository.save(game);
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
            endGame(game, userId);
            return game;
        }

        if (game.getDiscardDeck() == null) {
            game.setDiscardDeck(new MakaoDeck(new ArrayList<>()));
        }
        game.getDiscardDeck().addCard(playedCard);

        setCardEffect(game, playedCard, request);

        nextTurn(game);

        gameRepository.save(game);
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
                continue; // skip this player and look for the next one
            }

            List<Card> playable = gatherPlayableCards(game, candidate);
            game.setActivePlayerPlayableCards(playable);

            if (playable.isEmpty() && game.isSpecialEffectActive()) {
                applySpecialEffectPenalty(game, candidate);
                gameRepository.save(game);
                return;
            }

            if (isBot(candidate)) {
                game.setActivePlayerId(candidate);
                handleBotTurn(game, candidate, playable);
                return;
            }

            game.setActivePlayerId(candidate);
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
        gameRepository.save(game);
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
        gameRepository.save(game);

        if (isBot(activePlayerId)) {
            handleBotTurn(game, activePlayerId, playable);
        }
    }

    private void endGame(MakaoGame game, String winnerId) {
        game.setStatus(RoomStatus.FINISHED);
        game.setSpecialEffectActive(false);
        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        Map<String, Integer> ranking = new HashMap<>();
        ranking.put(winnerId, 0);

        for (Map.Entry<String, List<Card>> entry : game.getPlayersHands().entrySet()) {
            String playerId = entry.getKey();
            if (winnerId.equals(playerId)) {
                continue;
            }
            int value = calculateHandValue(entry.getValue());
            ranking.put(playerId, value);
        }

        game.setRanking(ranking);

        GameFinishMessage message = new GameFinishMessage(
                game.getRoomId(),
                RoomStatus.FINISHED);

        rabbitTemplate.convertAndSend(gameEventsExchange.getName(), finishRoutingKey, message);
        gameRepository.save(game);
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
        nextTurn(game);
        gameRepository.save(game);
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
            nextTurn(game);
            return;
        }

        hand.remove(card);

        if (hand.isEmpty()) {
            endGame(game, botId);
            return;
        }

        if (game.getDiscardDeck() == null) {
            game.setDiscardDeck(new MakaoDeck(new ArrayList<>()));
        }
        game.getDiscardDeck().addCard(card);

        game.setDrawnCard(null);
        game.setActivePlayerPlayableCards(new ArrayList<>());

        setCardEffect(game, card, req);
        nextTurn(game);
        gameRepository.save(game);
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
