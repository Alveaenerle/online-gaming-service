package com.online_games_service.menu.service;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.config.GameLimitsConfig;
import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameLimitsConfig gameLimitsConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    // --- REDIS KEYS ---
    private static final String KEY_ROOM = "game:room:";       // Store object GameRoom
    private static final String KEY_WAITING = "game:waiting:"; // Set ID public rooms (for Quick Match/Lobby)
    private static final String KEY_CODE = "game:code:";       // Map CODE -> ID (for Private Join)
    private static final String KEY_USER_ROOM = "game:user-room:"; // Map USER -> ID (for Start/Leave)

    private static final Duration ROOM_TTL = Duration.ofHours(1);

    public GameRoom createRoom(CreateRoomRequest request, String hostUsername) {
        if (getUserCurrentRoomId(hostUsername) != null) {
            throw new IllegalStateException("You are already in a room. Leave it first.");
        }

        GameLimitsConfig.Limit limit = gameLimitsConfig.getLimitFor(request.getGameType());
        validatePlayerLimits(request.getMaxPlayers(), limit);

        GameRoom newRoom = new GameRoom(
                request.getName(),
                request.getGameType(),
                hostUsername,
                request.getMaxPlayers(),
                request.isPrivate()
        );

        newRoom.setId(UUID.randomUUID().toString());

        // unique code generation for private rooms only for Redis (not persisted)
        String uniqueCode = null;
        boolean isUnique = false;
        int attempts = 0;

        while (!isUnique && attempts < 5) {
            String potentialCode = generateAccessCode();

            isUnique = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(KEY_CODE + potentialCode, newRoom.getId(), ROOM_TTL)
            );

            if (isUnique) {
                uniqueCode = potentialCode;
            }
            attempts++;
        }

        if (uniqueCode == null) {
            throw new IllegalStateException("Server busy: Could not generate unique room code.");
        }
        
        newRoom.setAccessCode(uniqueCode);

        saveRoomToRedis(newRoom);
        mapUserToRoom(hostUsername, newRoom.getId());

        if (!request.isPrivate()) {
            addToWaitingPool(newRoom);
        }

        log.info("Created room {} (Redis) with code {} for host {}", newRoom.getId(), uniqueCode, hostUsername);
        return newRoom;
    }

    public GameRoom joinRoom(JoinGameRequest request, String username) {
        // Handle re-join or lock
        String existingRoomId = getUserCurrentRoomId(username);
        if (existingRoomId != null) {
            GameRoom existingRoom = getRoomFromRedis(existingRoomId);
            if (existingRoom != null) {
                return existingRoom;
            }
            clearUserRoomMapping(username);
        }

        if (request.isRandom()) {
            return handleRandomJoin(request, username);
        } else {
            return handlePrivateJoin(request, username);
        }
    }

    private GameRoom handleRandomJoin(JoinGameRequest request, String username) {
        String waitingKey = KEY_WAITING + request.getGameType();
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
                    room.getPlayersUsernames().size() < room.getMaxPlayers() &&
                    !room.getPlayersUsernames().contains(username)) {

                    return addUserToRoom(room, username);
                }
            }
        }

        log.info("No matching room found. Creating new one for {}", username);
        CreateRoomRequest createRequest = new CreateRoomRequest();
        createRequest.setName("Room #" + (1000 + new java.util.Random().nextInt(9000)));
        createRequest.setGameType(request.getGameType());
        createRequest.setMaxPlayers(request.getMaxPlayers());
        createRequest.setPrivate(false);

        return createRoom(createRequest, username);
    }

    private GameRoom handlePrivateJoin(JoinGameRequest request, String username) {
        if (request.getAccessCode() == null || request.getAccessCode().isBlank()) {
            throw new IllegalArgumentException("Access code is required");
        }

        String roomId = (String) redisTemplate.opsForValue().get(KEY_CODE + request.getAccessCode());
        if (roomId == null) throw new IllegalArgumentException("Invalid access code");

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null) throw new IllegalArgumentException("Room expired");

        if (room.getPlayersUsernames().contains(username)) return room;
        if (room.getPlayersUsernames().size() >= room.getMaxPlayers()) throw new IllegalStateException("Room full");

        return addUserToRoom(room, username);
    }

    private GameRoom addUserToRoom(GameRoom room, String username) {
        room.addPlayer(username);
        saveRoomToRedis(room);
        mapUserToRoom(username, room.getId());

        if (room.getPlayersUsernames().size() >= room.getMaxPlayers()) {
            removeFromWaitingPool(room);
        }
        return room;
    }

    @Transactional
    public GameRoom startGame(String username) {
        String roomId = getUserCurrentRoomId(username);
        if (roomId == null) throw new IllegalStateException("User is not in a room");

        GameRoom room = getRoomFromRedis(roomId);
        if (room == null) {
            clearUserRoomMapping(username);
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

        GameRoom savedInDb = gameRoomRepository.save(room);
        log.info("Game started! Room {} persisted to MongoDB.", savedInDb.getId());
        
        return savedInDb;
    }

    public void leaveRoom(String username) {
        log.info("User {} is attempting to leave room...", username);

        String roomId = getUserCurrentRoomId(username);
        if (roomId == null) return;

        GameRoom room = getRoomFromRedis(roomId);
        clearUserRoomMapping(username);

        if (room == null) return;

        room.removePlayer(username); 
        
        if (room.getPlayersUsernames().isEmpty()) {
            deleteRoom(room);
            log.info("Room {} was empty and has been deleted.", roomId);
        } else {
            saveRoomToRedis(room);

            if (!room.isPrivate() && room.getStatus() == RoomStatus.WAITING) {
                addToWaitingPool(room);
            }
            
            log.info("User {} left room {}. Host is now: {}", username, roomId, room.getHostUsername());
        }
    }

    public List<GameRoom> getWaitingRooms(GameType gameType) {
        if (gameType == null) return Collections.emptyList();

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

    private void mapUserToRoom(String username, String roomId) {
        redisTemplate.opsForValue().set(KEY_USER_ROOM + username, roomId, ROOM_TTL);
    }

    private String getUserCurrentRoomId(String username) {
        return (String) redisTemplate.opsForValue().get(KEY_USER_ROOM + username);
    }

    private void clearUserRoomMapping(String username) {
        redisTemplate.delete(KEY_USER_ROOM + username);
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
        if (limit == null) return;
        if (requestedMaxPlayers < limit.getMin() || requestedMaxPlayers > limit.getMax()) {
            throw new IllegalArgumentException("Invalid player count for this game type");
        }
    }

    private String generateAccessCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public RoomInfoResponse getPlayerRoomInfo(String username) {
        String roomId = getUserCurrentRoomId(username);
        
        if (roomId == null) {
            throw new IllegalStateException("You are not currently in any room.");
        }

        GameRoom room = getRoomFromRedis(roomId);
        
        if (room == null) {
            clearUserRoomMapping(username);
            throw new IllegalStateException("Room no longer exists.");
        }

        return new RoomInfoResponse(
            room.getName(),
            room.getPlayersUsernames(),
            room.getMaxPlayers(),
            room.isPrivate(),
            room.getAccessCode(),
            room.getHostUsername(),
            room.getStatus()
        );
    }
}