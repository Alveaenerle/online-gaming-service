package com.online_games_service.menu.service;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.config.GameLimitsConfig;
import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.repository.GameRoomRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameRoomServiceTest {

	@Mock
	private GameRoomRepository gameRoomRepository;

	@Mock
	private GameLimitsConfig gameLimitsConfig;

	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private ValueOperations<String, Object> valueOperations;

	@Mock
	private SetOperations<String, Object> setOperations;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private AutoCloseable mocks;
	private GameRoomService gameRoomService;
	private GameLimitsConfig.Limit defaultLimit;

	@BeforeMethod
	public void setUp() {
		mocks = MockitoAnnotations.openMocks(this);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		lenient().when(setOperations.members(anyString())).thenReturn(Collections.emptySet());
		lenient().when(setOperations.add(anyString(), any())).thenReturn(1L);
		lenient().when(setOperations.remove(anyString(), any())).thenReturn(1L);
		lenient().when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
		lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
		lenient().when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);

		defaultLimit = new GameLimitsConfig.Limit();
		defaultLimit.setMin(2);
		defaultLimit.setMax(6);
		lenient().when(gameLimitsConfig.getLimitFor(any())).thenReturn(defaultLimit);

		gameRoomService = new GameRoomService(gameRoomRepository, gameLimitsConfig, redisTemplate, messagingTemplate);
	}

	@AfterMethod
	public void tearDown() throws Exception {
		mocks.close();
	}

	@Test
	public void shouldCreatePublicRoomAndAddToWaitingPool() {
		CreateRoomRequest request = buildCreateRequest(false);
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);

		GameRoom room = gameRoomService.createRoom(request, "host");

		Assert.assertEquals(room.getHostUsername(), "host");
		Assert.assertFalse(room.isPrivate());
		Assert.assertNotNull(room.getAccessCode());

		verify(valueOperations).setIfAbsent(startsWith("game:code:"), eq(room.getId()), any(Duration.class));
		verify(valueOperations).set(eq(keyForRoom(room.getId())), eq(room), any(Duration.class));
		verify(valueOperations).set(eq(keyForUser("host")), eq(room.getId()), any(Duration.class));
		verify(setOperations).add(eq(waitingKey(request.getGameType())), eq(room.getId()));
		verify(redisTemplate).expire(eq(waitingKey(request.getGameType())), any(Duration.class));
	}

	@Test
	public void shouldCreatePrivateRoomWithoutWaitingEntry() {
		CreateRoomRequest request = buildCreateRequest(true);
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);

		GameRoom room = gameRoomService.createRoom(request, "host");

		Assert.assertTrue(room.isPrivate());
		verify(setOperations, never()).add(eq(waitingKey(request.getGameType())), any());
	}

	@Test
	public void shouldRejectRoomCreationWhenHostAlreadyMapped() {
		CreateRoomRequest request = buildCreateRequest(false);
		when(valueOperations.get(keyForUser("host"))).thenReturn("existing-room");

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.createRoom(request, "host"));

		Assert.assertTrue(exception.getMessage().contains("already in a room"));
	}

	@Test
	public void shouldFailWhenAccessCodeCannotBeGenerated() {
		CreateRoomRequest request = buildCreateRequest(true);
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);
		when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.createRoom(request, "host"));

		Assert.assertTrue(exception.getMessage().contains("Server busy"));
	}

	@Test
	public void shouldJoinExistingRandomRoom() {
		JoinGameRequest request = buildJoinRequest(true);
		request.setMaxPlayers(2);

		GameRoom waitingRoom = buildRoom("room-join", GameType.LUDO, "host", 2, false);
		Set<Object> waitingIds = new HashSet<>();
		waitingIds.add(waitingRoom.getId());

		when(valueOperations.get(keyForUser("player"))).thenReturn(null);
		when(setOperations.members(waitingKey(GameType.LUDO))).thenReturn(waitingIds);
		when(valueOperations.get(keyForRoom(waitingRoom.getId()))).thenReturn(waitingRoom);

		GameRoom result = gameRoomService.joinRoom(request, "player");

		Assert.assertTrue(result.getPlayersUsernames().contains("player"));
		verify(valueOperations).set(eq(keyForUser("player")), eq(waitingRoom.getId()), any(Duration.class));
		verify(setOperations).remove(eq(waitingKey(GameType.LUDO)), eq(waitingRoom.getId()));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + waitingRoom.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldCreateRoomWhenNoRandomMatchExists() {
		JoinGameRequest request = buildJoinRequest(true);
		request.setGameType(GameType.MAKAO);
		when(valueOperations.get(keyForUser("player"))).thenReturn(null);
		when(setOperations.members(waitingKey(GameType.MAKAO))).thenReturn(Collections.emptySet());

		GameRoom createdRoom = buildRoom("generated-room", GameType.MAKAO, "player", 4, false);
		GameRoomService spyService = spy(gameRoomService);
		doReturn(createdRoom).when(spyService).createRoom(any(CreateRoomRequest.class), eq("player"));

		GameRoom result = spyService.joinRoom(request, "player");

		Assert.assertEquals(result.getId(), createdRoom.getId());
		verify(spyService).createRoom(any(CreateRoomRequest.class), eq("player"));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + createdRoom.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldRemoveStaleWaitingRoomBeforeCreatingNewOne() {
		JoinGameRequest request = buildJoinRequest(true);
		when(valueOperations.get(keyForUser("player"))).thenReturn(null);

		Set<Object> waitingIds = new HashSet<>();
		waitingIds.add("stale-room");
		when(setOperations.members(waitingKey(GameType.LUDO))).thenReturn(waitingIds);
		when(valueOperations.get(keyForRoom("stale-room"))).thenReturn(null);

		GameRoom createdRoom = buildRoom("fallback-room", GameType.LUDO, "player", 4, false);
		GameRoomService spyService = spy(gameRoomService);
		doReturn(createdRoom).when(spyService).createRoom(any(CreateRoomRequest.class), eq("player"));

		GameRoom result = spyService.joinRoom(request, "player");

		Assert.assertEquals(result.getId(), createdRoom.getId());
		verify(setOperations).remove(eq(waitingKey(GameType.LUDO)), eq("stale-room"));
		verify(spyService).createRoom(any(CreateRoomRequest.class), eq("player"));
	}

	@Test
	public void shouldJoinPrivateRoomByAccessCode() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode("ABC123");

		GameRoom room = buildRoom("room-private", GameType.LUDO, "host", 4, true);

		when(valueOperations.get(keyForUser("guest"))).thenReturn(null);
		when(valueOperations.get(codeKey("ABC123"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		GameRoom result = gameRoomService.joinRoom(request, "guest");

		Assert.assertTrue(result.getPlayersUsernames().contains("guest"));
		verify(valueOperations).set(eq(keyForUser("guest")), eq(room.getId()), any(Duration.class));
		verify(valueOperations).set(eq(keyForRoom(room.getId())), eq(room), any(Duration.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldRejectPrivateJoinWhenCodeMissing() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode(null);

		when(valueOperations.get(keyForUser("guest"))).thenReturn(null);

		IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
				() -> gameRoomService.joinRoom(request, "guest"));

		Assert.assertTrue(exception.getMessage().contains("Access code"));
	}

	@Test
	public void shouldRejectPrivateJoinWithoutValidCode() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode("MISSING");

		when(valueOperations.get(keyForUser("guest"))).thenReturn(null);
		when(valueOperations.get(codeKey("MISSING"))).thenReturn(null);

		IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
				() -> gameRoomService.joinRoom(request, "guest"));

		Assert.assertTrue(exception.getMessage().contains("Invalid access code"));
	}

	@Test
	public void shouldRejectPrivateJoinWhenRoomExpired() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode("OLD123");

		when(valueOperations.get(keyForUser("guest"))).thenReturn(null);
		when(valueOperations.get(codeKey("OLD123"))).thenReturn("room-old");
		when(valueOperations.get(keyForRoom("room-old"))).thenReturn(null);

		IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
				() -> gameRoomService.joinRoom(request, "guest"));

		Assert.assertTrue(exception.getMessage().contains("Room expired"));
	}

	@Test
	public void shouldReturnExistingRoomWhenAlreadyParticipantInPrivateRoom() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode("KEEPME");

		GameRoom room = buildRoom("room-existing-private", GameType.LUDO, "host", 4, true);
		room.addPlayer("guest");

		when(valueOperations.get(keyForUser("guest"))).thenReturn(null);
		when(valueOperations.get(codeKey("KEEPME"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		GameRoom result = gameRoomService.joinRoom(request, "guest");

		Assert.assertEquals(result.getId(), room.getId());
		verify(valueOperations, never()).set(eq(keyForUser("guest")), any(), any(Duration.class));
	}

	@Test
	public void shouldRejectPrivateJoinWhenRoomFull() {
		JoinGameRequest request = buildJoinRequest(false);
		request.setAccessCode("FULL99");

		GameRoom room = buildRoom("room-full", GameType.LUDO, "host", 2, true);
		room.addPlayer("guest1");

		when(valueOperations.get(keyForUser("guest3"))).thenReturn(null);
		when(valueOperations.get(codeKey("FULL99"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.joinRoom(request, "guest3"));

		Assert.assertTrue(exception.getMessage().contains("Room full"));
	}

	@Test
	public void shouldReturnExistingRoomWhenUserIsMapped() {
		GameRoom room = buildRoom("room-existing", GameType.LUDO, "host", 4, false);
		when(valueOperations.get(keyForUser("player"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		GameRoom result = gameRoomService.joinRoom(buildJoinRequest(true), "player");

		Assert.assertEquals(result.getId(), room.getId());
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldClearStaleMappingBeforeJoining() {
		JoinGameRequest request = buildJoinRequest(true);
		GameRoom createdRoom = buildRoom("room-fresh", GameType.LUDO, "player", 4, false);
		GameRoomService spyService = spy(gameRoomService);
		doReturn(createdRoom).when(spyService).createRoom(any(CreateRoomRequest.class), eq("player"));

		when(valueOperations.get(keyForUser("player"))).thenReturn("stale-room");
		when(valueOperations.get(keyForRoom("stale-room"))).thenReturn(null);
		when(setOperations.members(waitingKey(GameType.LUDO))).thenReturn(Collections.emptySet());

		GameRoom result = spyService.joinRoom(request, "player");

		Assert.assertEquals(result.getId(), createdRoom.getId());
		verify(redisTemplate).delete(eq(keyForUser("player")));
	}

	@Test
	public void shouldStartGameAndPersistRoom() {
		GameRoom room = buildRoom("room-start", GameType.LUDO, "host", 4, false);
		room.setAccessCode("START01");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		GameRoom result = gameRoomService.startGame("host");

		Assert.assertEquals(result.getStatus(), RoomStatus.PLAYING);
		verify(setOperations).remove(eq(waitingKey(room.getGameType())), eq(room.getId()));
		verify(redisTemplate).delete(eq(codeKey("START01")));
		verify(valueOperations).set(eq(keyForRoom(room.getId())), eq(room), any(Duration.class));
		verify(gameRoomRepository).save(room);
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldRejectStartGameWhenUserNotInRoom() {
		when(valueOperations.get(keyForUser("ghost"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.startGame("ghost"));

		Assert.assertTrue(exception.getMessage().contains("not in a room"));
	}

	@Test
	public void shouldRejectStartGameForNonHost() {
		GameRoom room = buildRoom("room-start", GameType.LUDO, "owner", 4, false);
		when(valueOperations.get(keyForUser("intruder"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.startGame("intruder"));

		Assert.assertTrue(exception.getMessage().contains("Only host"));
	}

	@Test
	public void shouldClearMappingWhenRoomMissingOnStart() {
		when(valueOperations.get(keyForUser("host"))).thenReturn("missing-room");
		when(valueOperations.get(keyForRoom("missing-room"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.startGame("host"));

		Assert.assertTrue(exception.getMessage().contains("Room no longer exists"));
		verify(redisTemplate).delete(eq(keyForUser("host")));
	}

	@Test
	public void shouldDeleteRoomWhenLastPlayerLeaves() {
		GameRoom room = buildRoom("room-leave", GameType.LUDO, "host", 4, false);
		room.setAccessCode("LEAVE1");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		gameRoomService.leaveRoom("host");

		verify(redisTemplate).delete(eq(keyForUser("host")));
		verify(redisTemplate).delete(eq(keyForRoom(room.getId())));
		verify(redisTemplate).delete(eq(codeKey("LEAVE1")));
		verify(setOperations).remove(eq(waitingKey(room.getGameType())), eq(room.getId()));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldUpdateRoomWhenHostLeavesButPlayersRemain() {
		GameRoom room = buildRoom("room-leave", GameType.LUDO, "host", 4, false);
		room.addPlayer("p2");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		gameRoomService.leaveRoom("host");

		Assert.assertEquals(room.getHostUsername(), "p2");
		verify(valueOperations).set(eq(keyForRoom(room.getId())), eq(room), any(Duration.class));
		verify(setOperations).add(eq(waitingKey(room.getGameType())), eq(room.getId()));
		verify(redisTemplate).delete(eq(keyForUser("host")));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldDoNothingWhenRoomAlreadyGoneOnLeave() {
		when(valueOperations.get(keyForUser("ghost"))).thenReturn("missing-room");
		when(valueOperations.get(keyForRoom("missing-room"))).thenReturn(null);

		gameRoomService.leaveRoom("ghost");

		verify(redisTemplate).delete(eq(keyForUser("ghost")));
		verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldIgnoreLeaveWhenUserHasNoRoom() {
		when(valueOperations.get(keyForUser("ghost"))).thenReturn(null);

		gameRoomService.leaveRoom("ghost");

		verify(redisTemplate, never()).delete(eq(keyForUser("ghost")));
		verify(messagingTemplate, never()).convertAndSend(anyString(), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldReturnWaitingRoomsAndCleanupStaleEntries() {
		GameRoom validRoom = buildRoom("room-valid", GameType.LUDO, "host", 4, false);
		Set<Object> ids = new HashSet<>();
		ids.add(validRoom.getId());
		ids.add("stale-id");

		when(setOperations.members(waitingKey(GameType.LUDO))).thenReturn(ids);
		when(valueOperations.get(keyForRoom(validRoom.getId()))).thenReturn(validRoom);
		when(valueOperations.get(keyForRoom("stale-id"))).thenReturn(null);

		List<GameRoom> rooms = gameRoomService.getWaitingRooms(GameType.LUDO);

		Assert.assertEquals(rooms.size(), 1);
		Assert.assertEquals(rooms.get(0).getId(), validRoom.getId());
		verify(setOperations).remove(eq(waitingKey(GameType.LUDO)), eq("stale-id"));
	}

	@Test
	public void shouldReturnEmptyWaitingRoomsWhenGameTypeMissing() {
		List<GameRoom> rooms = gameRoomService.getWaitingRooms(null);

		Assert.assertTrue(rooms.isEmpty());
		verify(setOperations, never()).members(anyString());
	}

	@Test
	public void shouldReturnPlayerRoomInfo() {
		GameRoom room = buildRoom("room-info", GameType.LUDO, "host", 4, false);
		when(valueOperations.get(keyForUser("user"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		RoomInfoResponse response = gameRoomService.getPlayerRoomInfo("user");

		Assert.assertEquals(response.getId(), room.getId());
		Assert.assertEquals(response.getHostUsername(), "host");
	}

	@Test
	public void shouldClearMappingWhenRoomInfoIsMissing() {
		when(valueOperations.get(keyForUser("user"))).thenReturn("missing");
		when(valueOperations.get(keyForRoom("missing"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.getPlayerRoomInfo("user"));

		Assert.assertTrue(exception.getMessage().contains("Room no longer exists"));
		verify(redisTemplate).delete(eq(keyForUser("user")));
	}

	@Test
	public void shouldRejectRoomInfoWhenUserNotMapped() {
		when(valueOperations.get(keyForUser("ghost"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.getPlayerRoomInfo("ghost"));

		Assert.assertTrue(exception.getMessage().contains("not currently in any room"));
	}

	@Test
	public void shouldKickPlayerAndReopenSlot() {
		GameRoom room = buildRoom("room-kick", GameType.LUDO, "host", 2, false);
		room.addPlayer("victim");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		String message = gameRoomService.kickPlayer("host", "victim");

		Assert.assertTrue(message.contains("victim"));
		Assert.assertEquals(room.getStatus(), RoomStatus.WAITING);
		verify(redisTemplate).delete(eq(keyForUser("victim")));
		verify(valueOperations).set(eq(keyForRoom(room.getId())), eq(room), any(Duration.class));
		verify(setOperations).add(eq(waitingKey(room.getGameType())), eq(room.getId()));
		verify(messagingTemplate).convertAndSend(eq("/topic/room/" + room.getId()), any(RoomInfoResponse.class));
	}

	@Test
	public void shouldRejectKickWhenHostNotInRoom() {
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.kickPlayer("host", "victim"));

		Assert.assertTrue(exception.getMessage().contains("not in any room"));
	}

	@Test
	public void shouldRejectKickWhenRoomMissing() {
		when(valueOperations.get(keyForUser("host"))).thenReturn("ghost-room");
		when(valueOperations.get(keyForRoom("ghost-room"))).thenReturn(null);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.kickPlayer("host", "victim"));

		Assert.assertTrue(exception.getMessage().contains("Room no longer exists"));
	}

	@Test
	public void shouldRejectKickWhenCallerIsNotHost() {
		GameRoom room = buildRoom("room-kick", GameType.LUDO, "owner", 2, false);
		room.addPlayer("victim");

		when(valueOperations.get(keyForUser("intruder"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.kickPlayer("intruder", "victim"));

		Assert.assertTrue(exception.getMessage().contains("Only the host"));
	}

	@Test
	public void shouldRejectKickWhenHostTargetsSelf() {
		GameRoom room = buildRoom("room-kick", GameType.LUDO, "host", 2, false);
		room.addPlayer("guest");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		IllegalStateException exception = Assert.expectThrows(IllegalStateException.class,
				() -> gameRoomService.kickPlayer("host", "host"));

		Assert.assertTrue(exception.getMessage().contains("cannot kick yourself"));
	}

	@Test
	public void shouldRejectKickWhenPlayerNotPresent() {
		GameRoom room = buildRoom("room-kick", GameType.LUDO, "host", 2, false);
		room.addPlayer("guest");

		when(valueOperations.get(keyForUser("host"))).thenReturn(room.getId());
		when(valueOperations.get(keyForRoom(room.getId()))).thenReturn(room);

		IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
				() -> gameRoomService.kickPlayer("host", "stranger"));

		Assert.assertTrue(exception.getMessage().contains("not in this room"));
	}

	@Test
	public void shouldRejectRoomCreationWhenPlayersOutsideLimit() {
		CreateRoomRequest request = buildCreateRequest(false);
		request.setMaxPlayers(10);
		defaultLimit.setMax(4);
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);

		IllegalArgumentException exception = Assert.expectThrows(IllegalArgumentException.class,
				() -> gameRoomService.createRoom(request, "host"));

		Assert.assertTrue(exception.getMessage().contains("Invalid player count"));
	}

	@Test
	public void shouldAllowRoomCreationWhenLimitMissing() {
		CreateRoomRequest request = buildCreateRequest(false);
		request.setMaxPlayers(8);
		when(gameLimitsConfig.getLimitFor(any())).thenReturn(null);
		when(valueOperations.get(keyForUser("host"))).thenReturn(null);

		GameRoom room = gameRoomService.createRoom(request, "host");

		Assert.assertEquals(room.getMaxPlayers(), 8);
	}

	private CreateRoomRequest buildCreateRequest(boolean isPrivate) {
		CreateRoomRequest request = new CreateRoomRequest();
		request.setName("Test Room");
		request.setGameType(GameType.LUDO);
		request.setMaxPlayers(4);
		request.setPrivate(isPrivate);
		return request;
	}

	private JoinGameRequest buildJoinRequest(boolean isRandom) {
		JoinGameRequest request = new JoinGameRequest();
		request.setGameType(GameType.LUDO);
		request.setMaxPlayers(4);
		request.setRandom(isRandom);
		return request;
	}

	private GameRoom buildRoom(String id, GameType type, String host, int maxPlayers, boolean isPrivate) {
		GameRoom room = new GameRoom("Room-" + id, type, host, maxPlayers, isPrivate);
		room.setId(id);
		room.setAccessCode("CODE-" + id);
		return room;
	}

	private String keyForUser(String username) {
		return "game:user-room:" + username;
	}

	private String keyForRoom(String roomId) {
		return "game:room:" + roomId;
	}

	private String codeKey(String code) {
		return "game:code:" + code;
	}

	private String waitingKey(GameType type) {
		return "game:waiting:" + type;
	}
}
