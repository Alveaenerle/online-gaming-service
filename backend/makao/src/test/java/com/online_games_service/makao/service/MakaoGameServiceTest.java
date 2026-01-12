package com.online_games_service.makao.service;

import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.model.Card;
import com.online_games_service.makao.dto.DrawCardResponse;
import com.online_games_service.makao.dto.PlayCardRequest;
import com.online_games_service.makao.dto.PlayerTimeoutMessage;
import com.online_games_service.makao.model.MakaoDeck;
import com.online_games_service.makao.model.MakaoGame;
import com.online_games_service.makao.repository.mongo.MakaoGameResultRepository;
import com.online_games_service.makao.repository.redis.MakaoGameRedisRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

public class MakaoGameServiceTest {

	@Mock
	private MakaoGameRedisRepository gameRepository;
	@Mock
	private MakaoGameResultRepository gameResultRepository;
	@Mock
	private RedisTemplate<String, Object> redisTemplate;
	@Mock
	private ValueOperations<String, Object> valueOps;
	@Mock
	private RabbitTemplate rabbitTemplate;
	@Mock
	private TopicExchange topicExchange;
	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private MakaoGameService service;
	private AutoCloseable mocks;

	@BeforeMethod
	public void setUp() {
		mocks = MockitoAnnotations.openMocks(this);
		when(topicExchange.getName()).thenReturn("exchange");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		service = new MakaoGameService(
				gameRepository,
				gameResultRepository,
				redisTemplate,
				rabbitTemplate,
				topicExchange,
				messagingTemplate);
		ReflectionTestUtils.setField(service, "finishRoutingKey", "finish.key");
		ReflectionTestUtils.setField(service, "turnTimeoutSeconds", 60L);
	}

	@AfterMethod
	public void tearDown() throws Exception {
		if (mocks != null) {
			mocks.close();
		}
	}

	@Test
	public void setCardEffect_appliesExpectedFlags() {
		MakaoGame game = new MakaoGame();
		Card two = new Card(CardSuit.CLUBS, CardRank.TWO);
		PlayCardRequest jackRequest = new PlayCardRequest();
		jackRequest.setRequestRank(CardRank.ACE);
		PlayCardRequest aceRequest = new PlayCardRequest();
		aceRequest.setRequestSuit(CardSuit.HEARTS);

		ReflectionTestUtils.invokeMethod(service, "setCardEffect", game, two, new PlayCardRequest());
		assertTrue(game.isSpecialEffectActive());
		assertEquals(game.getPendingDrawCount(), 2);

		Card jack = new Card(CardSuit.SPADES, CardRank.JACK);
		ReflectionTestUtils.invokeMethod(service, "setCardEffect", game, jack, jackRequest);
		assertEquals(game.getDemandedRank(), CardRank.ACE);

		Card ace = new Card(CardSuit.DIAMONDS, CardRank.ACE);
		ReflectionTestUtils.invokeMethod(service, "setCardEffect", game, ace, aceRequest);
		assertEquals(game.getDemandedSuit(), CardSuit.HEARTS);

		Card king = new Card(CardSuit.HEARTS, CardRank.KING);
		boolean initialReverse = game.isReverseMovement();
		ReflectionTestUtils.invokeMethod(service, "setCardEffect", game, king, new PlayCardRequest());
		assertEquals(game.isReverseMovement(), !initialReverse);
	}

	@Test
	public void isPlayable_respectsDemandsAndEffects() {
		MakaoGame game = baseGameWithTopCard(new Card(CardSuit.HEARTS, CardRank.TWO));
		game.setSpecialEffectActive(true);

		Card allowed = new Card(CardSuit.SPADES, CardRank.THREE);
		Card blocked = new Card(CardSuit.CLUBS, CardRank.FOUR);

		boolean allowedPlay = ReflectionTestUtils.invokeMethod(service, "isPlayable", game, allowed);
		boolean blockedPlay = ReflectionTestUtils.invokeMethod(service, "isPlayable", game, blocked);
		assertTrue(allowedPlay);
		assertFalse(blockedPlay);

		game.setSpecialEffectActive(false);
		game.setDemandedRank(CardRank.KING);
		Card jack = new Card(CardSuit.SPADES, CardRank.JACK);
		Card king = new Card(CardSuit.CLUBS, CardRank.KING);
		assertTrue(ReflectionTestUtils.invokeMethod(service, "isPlayable", game, king));
		assertTrue(ReflectionTestUtils.invokeMethod(service, "isPlayable", game, jack));
	}

	@Test
	public void gatherPlayableCards_filtersNonMatching() {
		MakaoGame game = baseGameWithTopCard(new Card(CardSuit.HEARTS, CardRank.FIVE));
		Map<String, List<Card>> hands = new HashMap<>();
		List<Card> playerHand = new ArrayList<>();
		playerHand.add(new Card(CardSuit.SPADES, CardRank.FIVE));
		playerHand.add(new Card(CardSuit.CLUBS, CardRank.SIX));
		playerHand.add(new Card(CardSuit.HEARTS, CardRank.TWO));
		hands.put("p1", playerHand);
		game.setPlayersHands(hands);

		List<Card> playable = ReflectionTestUtils.invokeMethod(service, "gatherPlayableCards", game, "p1");
		assertEquals(playable.size(), 2);
		assertTrue(playable.contains(new Card(CardSuit.SPADES, CardRank.FIVE)));
		assertTrue(playable.contains(new Card(CardSuit.HEARTS, CardRank.TWO)));
	}

	@Test
	public void drawWithRecycle_reusesDiscardPile() {
		MakaoGame game = new MakaoGame();
		Card keepOnTop = new Card(CardSuit.HEARTS, CardRank.ACE);
		Card toRecycle = new Card(CardSuit.CLUBS, CardRank.NINE);

		game.setDrawDeck(new MakaoDeck(new ArrayList<>()));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(toRecycle, keepOnTop))));

		Card drawn = ReflectionTestUtils.invokeMethod(service, "drawWithRecycle", game);

		assertNotNull(drawn);
		assertEquals(drawn, toRecycle);
		assertEquals(game.getDiscardDeck().size(), 1);
		assertEquals(game.getCurrentCard(), keepOnTop);
	}

	@Test
	public void applySpecialEffectPenalty_handlesSkipEffect() {
		MakaoGame game = baseGameWithTopCard(new Card(CardSuit.SPADES, CardRank.FOUR));
		game.setPendingSkipTurns(2);
		Map<String, Integer> skipTurns = new HashMap<>();
		skipTurns.put("p1", 1);
		game.setPlayersSkipTurns(skipTurns);

		ReflectionTestUtils.invokeMethod(service, "applySpecialEffectPenalty", game, "p1");

		assertEquals(game.getPlayersSkipTurns().get("p1").intValue(), 3);
		assertEquals(game.getPendingSkipTurns(), 0);
		assertFalse(game.isSpecialEffectActive());
	}

	@Test
	public void utilityMethods_containAndScoreHands() {
		List<Card> hand = List.of(
				new Card(CardSuit.HEARTS, CardRank.TWO),
				new Card(CardSuit.SPADES, CardRank.KING));

		boolean contains = ReflectionTestUtils.invokeMethod(service, "containsCard", hand, new Card(CardSuit.HEARTS, CardRank.TWO));
		int score = ReflectionTestUtils.invokeMethod(service, "calculateHandValue", hand);

		assertTrue(contains);
		assertEquals(score, (CardRank.TWO.ordinal() + 2) + (CardRank.KING.ordinal() + 2));
	}

	@Test
	public void handleTurnTimeout_replacesPlayerWithBot() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("p1");
		game.setPlayersOrderIds(new ArrayList<>(List.of("p1", "p2")));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("p1", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))));
		hands.put("p2", new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		game.setPlayersHands(hands);
		Map<String, Integer> skips = new HashMap<>();
		skips.put("p1", 0);
		skips.put("p2", 0);
		game.setPlayersSkipTurns(skips);
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("p1", "Alice", "p2", "Bob")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.THREE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "handleTurnTimeout", "room-1", "p1");

		assertTrue(game.getPlayersHands().containsKey("bot-1"));
		assertTrue(game.getPlayersOrderIds().contains("bot-1"));
		assertTrue(game.getLosers().contains("p1"));
		assertTrue(game.getPlayersHands().get("bot-1").size() >= 1);
	}

	@Test
	public void scheduleTurnTimeout_skipsBotsAndStatuses() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-2");
		game.setStatus(RoomStatus.FINISHED);
		game.setActivePlayerId("user-1");

		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);
		Map<?, ?> timeouts = (Map<?, ?>) ReflectionTestUtils.getField(service, "turnTimeouts");
		assertTrue(timeouts.isEmpty());

		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("bot-1");
		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);
		assertTrue(timeouts.isEmpty());

		game.setActivePlayerId("user-2");
		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);
		assertFalse(timeouts.isEmpty());
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-2");
	}

	@Test
	public void playCard_endsGameWhenHandEmpty() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of(userId, "Player")));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.SEVEN))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.SEVEN);
		req.setCardSuit(CardSuit.HEARTS);

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		service.playCard(req, userId);

		assertEquals(game.getStatus(), RoomStatus.FINISHED);
		verify(gameRepository).deleteById("room-1");
	}

	@Test
	public void drawCard_marksPlayableAndKeepsTurn() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.SEVEN)))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		DrawCardResponse response = service.drawCard(userId);

		assertTrue(response.isPlayable());
		assertEquals(game.getDrawnCard(), new Card(CardSuit.HEARTS, CardRank.SEVEN));
		assertEquals(game.getActivePlayerPlayableCards().size(), 1);
	}

	@Test
	public void drawCard_unplayableAdvancesTurn() {
		String userId = "p1";
		String nextId = "p2";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId, nextId)));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put(userId, new ArrayList<>());
		hands.put(nextId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.TEN))));
		game.setPlayersHands(hands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0, nextId, 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of(userId, "U1", nextId, "U2")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.THREE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		DrawCardResponse response = service.drawCard(userId);

		assertFalse(response.isPlayable());
		assertEquals(game.getActivePlayerId(), nextId);
		assertNull(game.getDrawnCard());
		assertFalse(game.getActivePlayerPlayableCards().isEmpty());
	}

	@Test
	public void drawCard_userNotAuthenticatedThrows() {
		try {
			service.drawCard(null);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("authenticated"));
		}
	}

	@Test
	public void drawCard_roomNotFoundThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn(null);

		try {
			service.drawCard(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Room not found"));
		}
	}

	@Test
	public void drawCard_alreadyDrewThisTurnThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setDrawnCard(new Card(CardSuit.CLUBS, CardRank.SEVEN));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.drawCard(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("already drawn"));
		}
	}

	@Test
	public void drawCard_specialEffectActiveThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setSpecialEffectActive(true);

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.drawCard(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("Special effect"));
		}
	}

	@Test
	public void drawCard_noCardsLeftThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>()));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.drawCard(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("No cards left"));
		}
	}

	@Test
	public void acceptEffect_appliesDrawPenaltyAndAdvances() {
		String userId = "p1";
		String nextId = "p2";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId, nextId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(
				userId, new ArrayList<>(),
				nextId, new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE))))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0, nextId, 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of(userId, "U1", nextId, "U2")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.TWO)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.DIAMONDS, CardRank.FIVE), new Card(CardSuit.HEARTS, CardRank.SEVEN)))));
		game.setSpecialEffectActive(true);
		game.setPendingDrawCount(2);

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		service.acceptEffect(userId);

		assertEquals(game.getPlayersHands().get(userId).size(), 2);
		assertEquals(game.getPendingDrawCount(), 0);
		assertFalse(game.isSpecialEffectActive());
		assertEquals(game.getActivePlayerId(), nextId);
	}

	@Test
	public void acceptEffect_noPendingEffectThrows() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.acceptEffect(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("No pending special effect"));
		}
	}

	@Test
	public void skipDrawnCard_clearsDrawnAndMovesTurn() {
		String userId = "p1";
		String nextId = "p2";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId, nextId)));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put(userId, new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		hands.put(nextId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.SEVEN))));
		game.setPlayersHands(hands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0, nextId, 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of(userId, "U1", nextId, "U2")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));
		game.setDrawnCard(new Card(CardSuit.CLUBS, CardRank.JACK));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		service.skipDrawnCard(userId);

		assertNull(game.getDrawnCard());
		assertEquals(game.getActivePlayerId(), nextId);
	}

	@Test
	public void skipDrawnCard_noDrawnCardThrows() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.skipDrawnCard(userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("No drawn card"));
		}
	}

	@Test
	public void playDrawnCard_userNotAuthenticatedThrows() {
		try {
			service.playDrawnCard(new PlayCardRequest(), null);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("authenticated"));
		}
	}

	@Test
	public void playDrawnCard_delegatesToPlayCard() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.SEVEN)))));
		game.setDrawnCard(new Card(CardSuit.HEARTS, CardRank.FIVE));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		MakaoGameService spyService = org.mockito.Mockito.spy(service);
		doReturn(game).when(spyService).playCard(any(PlayCardRequest.class), eq(userId));

		PlayCardRequest req = new PlayCardRequest();
		req.setRequestRank(CardRank.ACE);
		req.setRequestSuit(CardSuit.SPADES);

		spyService.playDrawnCard(req, userId);

		verify(spyService).playCard(any(PlayCardRequest.class), eq(userId));
		assertNull(game.getDrawnCard());
	}

	@Test
	public void initializeGameAfterStart_setsPlayableAndSchedulesTimeout() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-3");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("human-1");
		game.setPlayersOrderIds(new ArrayList<>(List.of("human-1", "bot-1")));
		Map<String, List<Card>> initHands = new HashMap<>();
		initHands.put("human-1", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.TEN))));
		initHands.put("bot-1", new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.SIX))));
		game.setPlayersHands(initHands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of("human-1", 0, "bot-1", 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("human-1", "Human", "bot-1", "Bot")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.TEN)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN)))));

		when(gameRepository.findById("room-3")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		service.initializeGameAfterStart("room-3");

		assertFalse(game.getActivePlayerPlayableCards().isEmpty());
		Map<?, ?> timeouts = (Map<?, ?>) ReflectionTestUtils.getField(service, "turnTimeouts");
		assertFalse(timeouts.isEmpty());
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-3");
	}

	@Test
	public void playCard_rejectsMissingJackRequestRank() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.JACK))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.FIVE)))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.JACK);
		req.setCardSuit(CardSuit.HEARTS);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Jack"));
		}
	}

	@Test
	public void playCard_rejectsMissingAceRequestSuit() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.ACE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.ACE);
		req.setCardSuit(CardSuit.HEARTS);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Ace"));
		}
	}

	@Test
	public void playCard_invalidCardNotInHand() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.FIVE)))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.SIX);
		req.setCardSuit(CardSuit.SPADES);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("specified card"));
		}
	}

	@Test
	public void playCard_userNotAuthenticatedThrows() {
		try {
			service.playCard(new PlayCardRequest(), null);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("authenticated"));
		}
	}

	@Test
	public void playCard_roomNotFoundThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn(null);

		try {
			service.playCard(new PlayCardRequest(), userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Room not found"));
		}
	}

	@Test
	public void playCard_discardPileEmptyThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.SEVEN))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>()));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.SEVEN);
		req.setCardSuit(CardSuit.SPADES);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("Discard pile is empty"));
		}
	}

	@Test
	public void playCard_notPlayersTurnThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("someone-else");
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.SEVEN))))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.SEVEN);
		req.setCardSuit(CardSuit.SPADES);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("not this player's turn"));
		}
	}

	@Test
	public void playCard_handMissingThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<>());
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.playCard(new PlayCardRequest(), userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("Player is not part"));
		}
	}

	@Test
	public void playCard_notPlayableThrows() {
		String userId = "p1";
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.SEVEN))))));
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of(userId, 0)));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		PlayCardRequest req = new PlayCardRequest();
		req.setCardRank(CardRank.SEVEN);
		req.setCardSuit(CardSuit.SPADES);

		try {
			service.playCard(req, userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("cannot be played"));
		}
	}

	@Test
	public void playCardAsBot_winsAndEndsGame() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-x");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("bot-1");
		game.setPlayersOrderIds(new ArrayList<>(List.of("bot-1")));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of("bot-1", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));

		when(gameRepository.findById("room-x")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "playCardAsBot", game, "bot-1",
				new Card(CardSuit.HEARTS, CardRank.FIVE), null, null);

		assertEquals(game.getStatus(), RoomStatus.FINISHED);
	}

	@Test
	public void playCardAsBot_notPlayableClearsDrawn() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-y");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("bot-1");
		game.setPlayersOrderIds(new ArrayList<>(List.of("bot-1")));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of("bot-1", new ArrayList<>(List.of(
				new Card(CardSuit.CLUBS, CardRank.SEVEN),
				new Card(CardSuit.DIAMONDS, CardRank.ACE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE)))));
		game.setDrawnCard(new Card(CardSuit.DIAMONDS, CardRank.ACE));

		ReflectionTestUtils.invokeMethod(service, "playCardAsBot", game, "bot-1",
				new Card(CardSuit.DIAMONDS, CardRank.ACE), null, null);

		assertNull(game.getDrawnCard());
	}

	@Test
	public void handleBotTurn_drawnPlayableGetsPlayed() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-bot");
		game.setStatus(RoomStatus.PLAYING);
		game.setPlayersOrderIds(new ArrayList<>(List.of("bot-1")));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of("bot-1", new ArrayList<>())));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))) ;
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.ACE)))));

		ReflectionTestUtils.invokeMethod(service, "handleBotTurn", game, "bot-1", new ArrayList<>());

		assertEquals(game.getPlayersHands().get("bot-1").size(), 0);
		assertEquals(game.getDiscardDeck().size(), 1);
		assertEquals(game.getStatus(), RoomStatus.FINISHED);
		assertNull(game.getDrawnCard());
	}

	@Test
	public void playDrawnCard_withoutDrawnCardThrows() {
		String userId = "p1";
		when(valueOps.get("game:user-room:id:" + userId)).thenReturn("room-1");
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId(userId);
		game.setPlayersOrderIds(new ArrayList<>(List.of(userId)));
		game.setPlayersHands(new HashMap<String, List<Card>>(Map.of(userId, new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))))));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.FIVE)))));

		when(gameRepository.findById("room-1")).thenReturn(Optional.of(game));

		try {
			service.playDrawnCard(new PlayCardRequest(), userId);
			assertTrue(false, "Expected exception");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("No drawn card"));
		}
	}

	@Test
	public void persistGameResult_skipsWhenNoId() {
		MakaoGame game = new MakaoGame();
		game.setRoomId(null);
		game.setGameId(null);
		ReflectionTestUtils.invokeMethod(service, "persistGameResult", (Object) game);
		verifyNoInteractions(gameResultRepository);
	}

	@Test
	public void persistGameResult_savesWhenIdPresent() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-1");
		game.setGameId("game-1");
		ReflectionTestUtils.invokeMethod(service, "persistGameResult", (Object) game);
		verify(gameResultRepository).save(any());
	}

	@Test
	public void nextTurn_onlyBotsEndsGame() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-bots");
		game.setStatus(RoomStatus.PLAYING);
		game.setPlayersOrderIds(new ArrayList<>(List.of("bot-1", "bot-2")));
		Map<String, List<Card>> botHands = new HashMap<>();
		botHands.put("bot-1", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))));
		botHands.put("bot-2", new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		game.setPlayersHands(botHands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of("bot-1", 0, "bot-2", 0)));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));

		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "nextTurn", game);

		assertEquals(game.getStatus(), RoomStatus.FINISHED);
	}

	// ============================================
	// Turn Timer Tests
	// ============================================

	@Test
	public void calculateTurnRemainingSeconds_returnsNullWhenNoStartTime() {
		MakaoGame game = new MakaoGame();
		game.setTurnStartTime(null);

		Integer remaining = ReflectionTestUtils.invokeMethod(service, "calculateTurnRemainingSeconds", game);

		assertNull(remaining);
	}

	@Test
	public void calculateTurnRemainingSeconds_returnsCorrectValue() {
		MakaoGame game = new MakaoGame();
		// Set start time to 30 seconds ago
		game.setTurnStartTime(System.currentTimeMillis() - 30_000);

		Integer remaining = ReflectionTestUtils.invokeMethod(service, "calculateTurnRemainingSeconds", game);

		assertNotNull(remaining);
		// Should be around 30 seconds remaining (60 - 30 = 30), allow some tolerance
		assertTrue(remaining >= 28 && remaining <= 32, "Expected ~30 seconds remaining, got: " + remaining);
	}

	@Test
	public void calculateTurnRemainingSeconds_returnsZeroWhenExpired() {
		MakaoGame game = new MakaoGame();
		// Set start time to 90 seconds ago (past the 60 second timeout)
		game.setTurnStartTime(System.currentTimeMillis() - 90_000);

		Integer remaining = ReflectionTestUtils.invokeMethod(service, "calculateTurnRemainingSeconds", game);

		assertNotNull(remaining);
		assertEquals(remaining.intValue(), 0);
	}

	@Test
	public void scheduleTurnTimeout_setsTurnStartTimeForHumanPlayer() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-timer-1");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("human-player");

		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);

		assertNotNull(game.getTurnStartTime());
		assertNotNull(game.getTurnRemainingSeconds());
		assertEquals(game.getTurnRemainingSeconds().intValue(), 60);

		// Cleanup
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-timer-1");
	}

	@Test
	public void scheduleTurnTimeout_clearsTurnTimeForBot() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-timer-2");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("bot-1");
		// Set some previous values that should be cleared
		game.setTurnStartTime(System.currentTimeMillis());
		game.setTurnRemainingSeconds(30);

		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);

		assertNull(game.getTurnStartTime());
		assertNull(game.getTurnRemainingSeconds());
	}

	@Test
	public void scheduleTurnTimeout_clearsTurnTimeWhenNotPlaying() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-timer-3");
		game.setStatus(RoomStatus.FINISHED);
		game.setActivePlayerId("human-player");
		// Set some previous values that should be cleared
		game.setTurnStartTime(System.currentTimeMillis());
		game.setTurnRemainingSeconds(30);

		ReflectionTestUtils.invokeMethod(service, "scheduleTurnTimeout", game);

		assertNull(game.getTurnStartTime());
		assertNull(game.getTurnRemainingSeconds());
	}

	@Test
	public void handleTurnTimeout_resetsTimerForNextPlayer() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-timer-4");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("p1");
		game.setPlayersOrderIds(new ArrayList<>(List.of("p1", "p2")));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("p1", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))));
		hands.put("p2", new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		game.setPlayersHands(hands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of("p1", 0, "p2", 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("p1", "Alice", "p2", "Bob")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.THREE)))));
		// Set initial timer
		game.setTurnStartTime(System.currentTimeMillis() - 60_000);
		game.setTurnRemainingSeconds(0);

		when(gameRepository.findById("room-timer-4")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "handleTurnTimeout", "room-timer-4", "p1");

		// Player p1 should be replaced by a bot, and the next player (p2) should have a new timer
		assertTrue(game.getLosers().contains("p1"));
		// Cleanup
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-timer-4");
	}

	// ============================================
	// Timeout Notification Tests
	// ============================================

	@Test
	public void notifyPlayerTimeout_sendsMessageToKickedPlayer() {
		String playerId = "user-123";
		String roomId = "room-abc";
		String botId = "bot-1";

		ReflectionTestUtils.invokeMethod(service, "notifyPlayerTimeout", playerId, roomId, botId);

		ArgumentCaptor<PlayerTimeoutMessage> messageCaptor = ArgumentCaptor.forClass(PlayerTimeoutMessage.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/makao/" + playerId + "/timeout"), messageCaptor.capture());

		PlayerTimeoutMessage sentMessage = messageCaptor.getValue();
		assertEquals(sentMessage.getRoomId(), roomId);
		assertEquals(sentMessage.getPlayerId(), playerId);
		assertEquals(sentMessage.getReplacedByBotId(), botId);
		assertEquals(sentMessage.getType(), "PLAYER_TIMEOUT");
		assertNotNull(sentMessage.getMessage());
	}

	@Test
	public void notifyPlayerTimeout_doesNotSendToBots() {
		String botId = "bot-1";
		String roomId = "room-abc";
		String replacingBotId = "bot-2";

		ReflectionTestUtils.invokeMethod(service, "notifyPlayerTimeout", botId, roomId, replacingBotId);

		// Should not send any message for bots
		verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(
				org.mockito.ArgumentMatchers.anyString(),
				any(PlayerTimeoutMessage.class)
		);
	}

	@Test
	public void notifyPlayerTimeout_doesNotSendForNullPlayer() {
		ReflectionTestUtils.invokeMethod(service, "notifyPlayerTimeout", null, "room-abc", "bot-1");

		// Should not send any message for null player
		verify(messagingTemplate, org.mockito.Mockito.never()).convertAndSend(
				org.mockito.ArgumentMatchers.anyString(),
				any(PlayerTimeoutMessage.class)
		);
	}

	@Test
	public void handleTurnTimeout_sendsTimeoutNotificationToPlayer() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-notify");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("player-to-kick");
		game.setPlayersOrderIds(new ArrayList<>(List.of("player-to-kick", "other-player")));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("player-to-kick", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))));
		hands.put("other-player", new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		game.setPlayersHands(hands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of("player-to-kick", 0, "other-player", 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("player-to-kick", "KickedUser", "other-player", "OtherUser")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.THREE)))));

		when(gameRepository.findById("room-notify")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "handleTurnTimeout", "room-notify", "player-to-kick");

		// Verify timeout notification was sent to the kicked player
		ArgumentCaptor<PlayerTimeoutMessage> messageCaptor = ArgumentCaptor.forClass(PlayerTimeoutMessage.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/makao/player-to-kick/timeout"), messageCaptor.capture());

		PlayerTimeoutMessage sentMessage = messageCaptor.getValue();
		assertEquals(sentMessage.getRoomId(), "room-notify");
		assertEquals(sentMessage.getPlayerId(), "player-to-kick");
		assertEquals(sentMessage.getReplacedByBotId(), "bot-1");
		assertEquals(sentMessage.getType(), "PLAYER_TIMEOUT");

		// Player should be in losers list
		assertTrue(game.getLosers().contains("player-to-kick"));

		// Cleanup
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-notify");
	}

	// ============================================
	// Player Room Mapping Cleanup Tests
	// ============================================

	@Test
	public void clearPlayerRoomMapping_deletesRedisKey() {
		String playerId = "user-cleanup-1";

		ReflectionTestUtils.invokeMethod(service, "clearPlayerRoomMapping", playerId);

		verify(redisTemplate).delete("game:user-room:id:" + playerId);
	}

	@Test
	public void clearPlayerRoomMapping_skipsBotsAndNullPlayers() {
		ReflectionTestUtils.invokeMethod(service, "clearPlayerRoomMapping", (String) null);
		ReflectionTestUtils.invokeMethod(service, "clearPlayerRoomMapping", "bot-1");

		// Neither should trigger a delete
		verify(redisTemplate, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	public void handleTurnTimeout_clearsPlayerRoomMapping() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-cleanup");
		game.setStatus(RoomStatus.PLAYING);
		game.setActivePlayerId("timeout-player");
		game.setPlayersOrderIds(new ArrayList<>(List.of("timeout-player", "other")));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("timeout-player", new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.FIVE))));
		hands.put("other", new ArrayList<>(List.of(new Card(CardSuit.CLUBS, CardRank.SEVEN))));
		game.setPlayersHands(hands);
		game.setPlayersSkipTurns(new HashMap<String, Integer>(Map.of("timeout-player", 0, "other", 0)));
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("timeout-player", "TimeoutUser", "other", "Other")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));
		game.setDrawDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.HEARTS, CardRank.THREE)))));

		when(gameRepository.findById("room-cleanup")).thenReturn(Optional.of(game));
		doReturn(game).when(gameRepository).save(game);

		ReflectionTestUtils.invokeMethod(service, "handleTurnTimeout", "room-cleanup", "timeout-player");

		// Verify Redis mapping was cleared for the timed-out player
		verify(redisTemplate).delete("game:user-room:id:timeout-player");

		// Cleanup
		ReflectionTestUtils.invokeMethod(service, "cancelTurnTimeout", "room-cleanup");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void endGame_clearsGameInProgressMap() {
		MakaoGame game = new MakaoGame();
		game.setRoomId("room-end-cleanup");
		game.setStatus(RoomStatus.PLAYING);
		game.setPlayersOrderIds(new ArrayList<>(List.of("p1")));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("p1", new ArrayList<>());
		game.setPlayersHands(hands);
		game.setPlayersUsernames(new HashMap<String, String>(Map.of("p1", "Player1")));
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(new Card(CardSuit.SPADES, CardRank.NINE)))));

		doReturn(game).when(gameRepository).save(game);

		// Simulate that gameInProgress has an entry for this room
		Map<String, java.util.concurrent.atomic.AtomicBoolean> gameInProgress = 
				(Map<String, java.util.concurrent.atomic.AtomicBoolean>) ReflectionTestUtils.getField(service, "gameInProgress");
		assertNotNull(gameInProgress);

		// Add an entry
		gameInProgress.put("room-end-cleanup", new java.util.concurrent.atomic.AtomicBoolean(false));
		assertTrue(gameInProgress.containsKey("room-end-cleanup"));

		// End the game
		ReflectionTestUtils.invokeMethod(service, "endGame", game);

		// Verify the entry was removed
		assertFalse(gameInProgress.containsKey("room-end-cleanup"));
	}

	private MakaoGame baseGameWithTopCard(Card topCard) {
		MakaoGame game = new MakaoGame();
		game.setDiscardDeck(new MakaoDeck(new ArrayList<>(List.of(topCard))));
		Map<String, List<Card>> hands = new HashMap<>();
		hands.put("p1", new ArrayList<>());
		game.setPlayersHands(hands);
		return game;
	}
}
