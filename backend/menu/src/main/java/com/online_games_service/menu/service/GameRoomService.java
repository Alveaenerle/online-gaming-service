package com.online_games_service.menu.service;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.config.GameLimitsConfig;
import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.messaging.GameStartPublisher;
import com.online_games_service.menu.model.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoomService {

    private final GameLimitsConfig gameLimitsConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameStartPublisher gameStartPublisher;

    // --- REDIS KEYS ---
    private static final String KEY_ROOM = "game:room:"; // Store object GameRoom
    private static final String KEY_WAITING = "game:waiting:"; // Set ID public rooms (for Quick Match/Lobby)
    private static final String KEY_CODE = "game:code:"; // Map CODE -> ID (for Private Join)
    private static final String KEY_USER_ROOM_BY_ID = "game:user-room:id:"; // Map userId -> room ID
    private static final String KEY_USER_ROOM_BY_USERNAME = "game:user-room:uname:"; // Map username -> room ID
                                                                                     // (fallback)

    private static final Duration ROOM_TTL = Duration.ofHours(1);
    private static final Random RANDOM = new Random();

    public GameRoom createRoom(CreateRoomRequest request, String hostUserId, String hostUsername) {
        if (getUserCurrentRoomId(hostUserId, hostUsername) != null) {
            throw new IllegalStateException("You are already in a room. Leave it first.");
        }

        GameLimitsConfig.Limit limit = gameLimitsConfig.getLimitFor(request.getGameType());
        validatePlayerLimits(request.getMaxPlayers(), limit);

        GameRoom newRoom = new GameRoom(
                request.getName(),
                request.getGameType(),
                hostUserId,
                hostUsername,
                request.getMaxPlayers(),
                request.isPrivate());

        newRoom.setId(UUID.randomUUID().toString());

        // unique code generation for private rooms only for Redis (not persisted)
        String uniqueCode = null;
        for (int attempts = 0; attempts < 5; attempts++) {
            String potentialCode = generateAccessCode();

            boolean isUnique = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(KEY_CODE + potentialCode, newRoom.getId(), ROOM_TTL));

            if (isUnique) {
                uniqueCode = potentialCode;
                break;
            }
        }

        if (uniqueCode == null) {
            throw new IllegalStateException("Server busy: Could not generate unique room code.");
        }

        newRoom.setAccessCode(uniqueCode);

        saveRoomToRedis(newRoom);
        mapUserToRoom(hostUserId, hostUsername, newRoom.getId());

        if (!request.isPrivate()) {
            addToWaitingPool(newRoom);
        }

        log.info("Created room {} (Redis) with code {} for host {}", newRoom.getId(), uniqueCode, hostUsername);
        return newRoom;
    }

    private void broadcastRoomUpdate(GameRoom room) {
        RoomInfoResponse response = new RoomInfoResponse(
                room.getId(),
                room.getName(),
                room.getGameType(),
                room.getPlayers(),
                room.getMaxPlayers(),
                room.isPrivate(),
                room.getAccessCode(),
                room.getHostUserId(),
                room.getHostUsername(),
                room.getStatus());

        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), response);

        log.info("Broadcasted room update for room {}", room.getId());
    }

    public GameRoom joinRoom(JoinGameRequest request, String userId, String username) {
        // Handle re-join or lock
        String existingRoomId = getUserCurrentRoomId(userId, username);
        if (existingRoomId != null) {
            GameRoom existingRoom = getRoomFromRedis(existingRoomId);
            if (existingRoom != null) {
                broadcastRoomUpdate(existingRoom);
                return existingRoom;
            }
            clearUserRoomMapping(userId, username);
        }

        GameRoom room;
        if (request.isRandom()) {
            room = handleRandomJoin(request, userId, username);
        } else {
            room = handlePrivateJoin(request, userId, username);
        }

        broadcastRoomUpdate(room);
        return room;
    }

    private GameRoom handleRandomJoin(JoinGameRequest request, String userId, String username) {
        GameType requestedType = request.getGameType();
        if (requestedType == null) {
            throw new IllegalArgumentException("Game type is required for random join");
        }

        String waitingKey = KEY_WAITING + requestedType;
        Set<Object> waitingRoomIds = redisTemplate.opsForSet().members(waitingKey);

        if (waitingRoomIds != null) {
            for (Object idObj : waitingRoomIds) {
                String roomId = (String) idObj;
                GameRoom room = getRoomFromRedis(roomId);

                if (room == null) {
                    redisTemplate.opsForSet().remove(waitingKey, roomId);
                    continue;
                }

                if (room.getMaxPlayers() == request.getMaxPlayers() &&
                        room.getPlayers().size() < room.getMaxPlayers() &&
                        !room.getPlayers().containsKey(userId)) {

                    return addUserToRoom(room, userId, username);
                }
            }
        }

        log.info("No matching room found. Creating new one for {}", username);
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setName("Room #" + (1000 + RANDOM.nextInt(9000)));
        createRequest.setGameType(requestedType);
        createRequest.setMaxPlayers(request.getMaxPlayers());
        createRequest.setPrivate(false);

        return createRoom(createRequest, userId, username);
    }

    private GameRoom handlePrivateJoin(JoinGameRequest request, String userId, String username) {
        if (request.getAccessCode() == null || request.getAccessCode().isBlank()) {
            throw new IllegalArgumentException("Access code is required");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_CODE + request.getAccessCode());
        if (roomId == null)
            throw new IllegalArgumentException("Invalid access code");

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null)
            throw new IllegalArgumentException("Room expired");

        if (room.getPlayers().containsKey(userId))
            return room;
        if (room.getPlayers().size() >= room.getMaxPlayers())
            throw new IllegalStateException("Room full");

        return addUserToRoom(room, userId, username);
    }

    private GameRoom addUserToRoom(GameRoom room, String userId, String username) {
        room.addPlayer(userId, username);
        saveRoomToRedis(room);
        mapUserToRoom(userId, username, room.getId());

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            removeFromWaitingPool(room);
        }
        return room;
    }

    public GameRoom startGame(String userId, String username) {
        String roomId = getUserCurrentRoomId(userId, username);
        if (roomId == null)
            throw new IllegalStateException("User is not in a room");

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null) {
            clearUserRoomMapping(userId, username);
            throw new IllegalStateException("Room no longer exists");
        }

        if (!room.getHostUsername().equals(username)) {
            throw new IllegalStateException("Only host can start the game");
        }

        room.setStatus(RoomStatus.PLAYING);

        removeFromWaitingPool(room);
        if (room.getAccessCode() != null) {
            redisTemplate.delete(KEY_CODE + room.getAccessCode());
        }

        saveRoomToRedis(room);

        gameStartPublisher.publish(room);

        broadcastRoomUpdate(room);
        return room;
    }

    public void markFinished(String roomId, RoomStatus status) {
        if (roomId == null || roomId.isBlank()) {
            log.warn("Cannot finish room: roomId is blank");
            return;
        }

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null) {
            log.warn("Cannot finish room {}: not found in Redis", roomId);
            return;
        }

        room.setStatus(status != null ? status : RoomStatus.FINISHED);
        saveRoomToRedis(room);
        broadcastRoomUpdate(room);
        log.info("Marked room {} as {} via makao.finish", roomId, room.getStatus());
    }

    public String leaveRoom(String userId, String username) {
        log.info("User {} is attempting to leave room...", username);

        String roomId = getUserCurrentRoomId(userId, username);
        if (roomId == null) {
            throw new IllegalStateException("You are not in any room.");
        }

        GameRoom room = getRoomFromRedis(roomId);
        clearUserRoomMapping(userId, username);

        if (room == null) {
            return "Left room (room already expired).";
        }

        room.removePlayerById(userId);

        String message;
        if (room.getPlayers().isEmpty()) {
            deleteRoom(room);
            log.info("Room {} was empty and has been deleted.", roomId);
            message = "Left room " + roomId + ". Room was deleted (no players left).";
        } else {
            saveRoomToRedis(room);

            if (!room.isPrivate() && room.getStatus() == RoomStatus.WAITING) {
                addToWaitingPool(room);
            }

            log.info("User {} left room {}. Host is now: {}", username, roomId, room.getHostUsername());
            message = "Left room " + roomId + ". New host: " + room.getHostUsername();
        }

        if (room != null)
            broadcastRoomUpdate(room);
        return message;
    }

    public List<GameRoom> getWaitingRooms(GameType gameType) {
        if (gameType == null)
            return Collections.emptyList();

        String waitingKey = KEY_WAITING + gameType;
        Set<Object> waitingRoomIds = redisTemplate.opsForSet().members(waitingKey);

        if (waitingRoomIds == null || waitingRoomIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<GameRoom> rooms = new ArrayList<>();
        for (Object idObj : waitingRoomIds) {
            String roomId = (String) idObj;
            GameRoom room = getRoomFromRedis(roomId);
            if (room != null) {
                rooms.add(room);
            } else {
                redisTemplate.opsForSet().remove(waitingKey, roomId);
            }
        }
        return rooms;
    }

    private void deleteRoom(GameRoom room) {
        redisTemplate.delete(KEY_ROOM + room.getId());
        removeFromWaitingPool(room);
        if (room.getAccessCode() != null) {
            redisTemplate.delete(KEY_CODE + room.getAccessCode());
        }
    }

    private void mapUserToRoom(String userId, String username, String roomId) {
        redisTemplate.opsForValue().set(KEY_USER_ROOM_BY_ID + userId, roomId, ROOM_TTL);
        if (username != null) {
            redisTemplate.opsForValue().set(KEY_USER_ROOM_BY_USERNAME + username, roomId, ROOM_TTL);
        }
    }

    private String getUserCurrentRoomId(String userId, String username) {
        String roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_ID + userId);
        if (roomId == null && username != null) {
            roomId = (String) redisTemplate.opsForValue().get(KEY_USER_ROOM_BY_USERNAME + username);
        }
        return roomId;
    }

    private void clearUserRoomMapping(String userId, String username) {
        redisTemplate.delete(KEY_USER_ROOM_BY_ID + userId);
        if (username != null) {
            redisTemplate.delete(KEY_USER_ROOM_BY_USERNAME + username);
        }
    }

    private void saveRoomToRedis(GameRoom room) {
        redisTemplate.opsForValue().set(KEY_ROOM + room.getId(), room, ROOM_TTL);
    }

    private GameRoom getRoomFromRedis(String roomId) {
        return (GameRoom) redisTemplate.opsForValue().get(KEY_ROOM + roomId);
    }

    private void addToWaitingPool(GameRoom room) {
        redisTemplate.opsForSet().add(KEY_WAITING + room.getGameType(), room.getId());
        redisTemplate.expire(KEY_WAITING + room.getGameType(), ROOM_TTL);
    }

    private void removeFromWaitingPool(GameRoom room) {
        redisTemplate.opsForSet().remove(KEY_WAITING + room.getGameType(), room.getId());
    }

    private void validatePlayerLimits(int requestedMaxPlayers, GameLimitsConfig.Limit limit) {
        if (limit == null)
            return;
        if (requestedMaxPlayers < limit.getMin() || requestedMaxPlayers > limit.getMax()) {
            throw new IllegalArgumentException("Invalid player count for this game type");
        }
    }

    private String generateAccessCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public RoomInfoResponse getPlayerRoomInfo(String userId, String username) {
        String roomId = getUserCurrentRoomId(userId, username);

        if (roomId == null) {
            throw new IllegalStateException("You are not currently in any room.");
        }

        GameRoom room = getRoomFromRedis(roomId);

        if (room == null) {
            clearUserRoomMapping(userId, username);
            throw new IllegalStateException("Room no longer exists.");
        }

        return new RoomInfoResponse(
                room.getId(),
                room.getName(),
                room.getGameType(),
                room.getPlayers(),
                room.getMaxPlayers(),
                room.isPrivate(),
                room.getAccessCode(),
                room.getHostUserId(),
                room.getHostUsername(),
                room.getStatus());
    }

    public String kickPlayer(String hostUserId, String hostUsername, String playerToKickUserId) {
        String roomId = getUserCurrentRoomId(hostUserId, hostUsername);
        if (roomId == null) {
            throw new IllegalStateException("You are not in any room.");
        }

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null) {
            throw new IllegalStateException("Room no longer exists.");
        }

        if (!room.getHostUsername().equals(hostUsername)) {
            throw new IllegalStateException("Only the host can kick players.");
        }

        if (hostUserId.equals(playerToKickUserId)) {
            throw new IllegalStateException("You cannot kick yourself. Use /leave endpoint instead.");
        }

        if (playerToKickUserId == null || playerToKickUserId.isBlank()) {
            throw new IllegalArgumentException("Player userId is required to kick a player.");
        }

        if (!room.getPlayers().containsKey(playerToKickUserId)) {
            throw new IllegalArgumentException("Player is not in this room.");
        }

        String kickedUsername = room.getPlayers().get(playerToKickUserId);

        room.removePlayerById(playerToKickUserId);
        clearUserRoomMapping(playerToKickUserId, kickedUsername);

        saveRoomToRedis(room);

        if (!room.isPrivate() && room.getStatus() == RoomStatus.WAITING) {
            addToWaitingPool(room);
        }

        log.info("Host {} kicked user {} from room {}", hostUsername, kickedUsername, roomId);

        broadcastRoomUpdate(room);
        return "Player " + kickedUsername + " has been kicked from the room.";
    }
}