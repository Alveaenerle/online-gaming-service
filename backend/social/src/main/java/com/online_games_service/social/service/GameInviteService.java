package com.online_games_service.social.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.dto.GameInviteDto;
import com.online_games_service.social.exception.GameInviteException;
import com.online_games_service.social.model.GameInvite;
import com.online_games_service.social.repository.GameInviteRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing game invitations.
 * Invites are stored in Redis and are ephemeral - they are deleted when the game starts.
 */
@Service
public class GameInviteService {

    private static final Logger logger = LoggerFactory.getLogger(GameInviteService.class);
    private static final String ROOM_KEY_PREFIX = "game:room:";

    private final GameInviteRedisRepository gameInviteRepository;
    private final PresenceService presenceService;
    private final FriendNotificationService friendNotificationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public GameInviteService(GameInviteRedisRepository gameInviteRepository,
                            PresenceService presenceService,
                            FriendNotificationService friendNotificationService,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.gameInviteRepository = gameInviteRepository;
        this.presenceService = presenceService;
        this.friendNotificationService = friendNotificationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send a game invite to another user.
     */
    public GameInviteDto sendGameInvite(String senderId, String senderUsername, 
                                        String targetId, String lobbyId, 
                                        String lobbyName, String gameType) {
        logger.info("User {} sending game invite to {} for lobby {}", senderId, targetId, lobbyId);

        // Validate: can't invite yourself
        if (senderId.equals(targetId)) {
            throw new GameInviteException(GameInviteException.ErrorCode.SELF_INVITE);
        }

        // Check for duplicate invite
        if (gameInviteRepository.existsBySenderIdAndTargetIdAndLobbyId(senderId, targetId, lobbyId)) {
            throw new GameInviteException(GameInviteException.ErrorCode.INVITE_ALREADY_PENDING);
        }

        // Fetch room data from Redis to validate and get accessCode
        String roomKey = ROOM_KEY_PREFIX + lobbyId;
        String roomJson = stringRedisTemplate.opsForValue().get(roomKey);
        if (roomJson == null) {
            logger.warn("Room {} not found in Redis", lobbyId);
            throw new GameInviteException(GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
        }

        String accessCode = null;
        try {
            JsonNode roomNode = objectMapper.readTree(roomJson);
            String status = roomNode.has("status") ? roomNode.get("status").asText() : null;
            
            // Validate lobby is joinable
            if (status == null || "STARTED".equals(status) || "FINISHED".equals(status)) {
                logger.info("Room {} is not joinable (status: {})", lobbyId, status);
                throw new GameInviteException(GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
            }
            
            // Extract accessCode if present
            if (roomNode.has("accessCode") && !roomNode.get("accessCode").isNull()) {
                accessCode = roomNode.get("accessCode").asText();
            }
        } catch (GameInviteException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error parsing room data: {}", e.getMessage());
            throw new GameInviteException(GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
        }

        // Create and save the invite with accessCode
        GameInvite invite = GameInvite.create(senderId, senderUsername, targetId, 
                                              lobbyId, lobbyName, gameType, accessCode);
        gameInviteRepository.save(invite);

        // Send real-time WebSocket notification if target is online
        if (presenceService.isUserOnline(targetId)) {
            friendNotificationService.sendGameInviteNotification(
                    targetId, invite.getId(), senderId, senderUsername, 
                    lobbyId, lobbyName, gameType, accessCode);
            logger.debug("Sent real-time game invite notification to {}", targetId);
        } else {
            logger.debug("Target user {} is offline, invite saved for later", targetId);
        }

        return toDto(invite);
    }

    /**
     * Get all pending game invites for a user, filtering out invalid lobbies.
     */
    public List<GameInviteDto> getPendingInvites(String userId) {
        logger.debug("Fetching pending game invites for user {}", userId);
        
        List<GameInvite> invites = gameInviteRepository.findByTargetId(userId);
        
        // Filter and clean up invites for invalid lobbies
        return invites.stream()
                .filter(invite -> {
                    if (isLobbyJoinable(invite.getLobbyId())) {
                        return true;
                    }
                    // Clean up invalid invite
                    logger.debug("Removing invite {} for invalid lobby {}", invite.getId(), invite.getLobbyId());
                    gameInviteRepository.deleteById(invite.getId());
                    return false;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Accept a game invite.
     */
    public GameInviteDto acceptInvite(String userId, String inviteId) {
        logger.info("User {} accepting game invite {}", userId, inviteId);
        
        GameInvite invite = gameInviteRepository.findById(inviteId)
                .orElseThrow(() -> new GameInviteException(GameInviteException.ErrorCode.INVITE_NOT_FOUND));
        
        // Validate the invite belongs to this user
        if (!invite.getTargetId().equals(userId)) {
            throw new GameInviteException(GameInviteException.ErrorCode.INVITE_NOT_FOUND);
        }

        // Validate lobby is still available
        if (!isLobbyJoinable(invite.getLobbyId())) {
            gameInviteRepository.deleteById(inviteId);
            throw new GameInviteException(GameInviteException.ErrorCode.LOBBY_NOT_AVAILABLE);
        }

        // Delete the invite (user will join via frontend)
        gameInviteRepository.deleteById(inviteId);
        
        return toDto(invite);
    }

    /**
     * Decline a game invite.
     */
    public GameInviteDto declineInvite(String userId, String inviteId) {
        logger.info("User {} declining game invite {}", userId, inviteId);
        
        GameInvite invite = gameInviteRepository.findById(inviteId)
                .orElseThrow(() -> new GameInviteException(GameInviteException.ErrorCode.INVITE_NOT_FOUND));
        
        // Validate the invite belongs to this user
        if (!invite.getTargetId().equals(userId)) {
            throw new GameInviteException(GameInviteException.ErrorCode.INVITE_NOT_FOUND);
        }

        // Delete the invite
        gameInviteRepository.deleteById(inviteId);
        
        return toDto(invite);
    }

    /**
     * Get all invites sent by a user.
     */
    public List<GameInviteDto> getSentInvites(String userId) {
        logger.debug("Fetching sent game invites for user {}", userId);
        return gameInviteRepository.findBySenderId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete all invites for a lobby (called when game starts via RabbitMQ).
     */
    public int deleteInvitesForLobby(String lobbyId) {
        logger.info("Deleting all invites for lobby {} (game started)", lobbyId);
        return gameInviteRepository.deleteAllByLobbyId(lobbyId);
    }

    /**
     * Check if a lobby is still joinable (status is WAITING or FULL).
     * Uses StringRedisTemplate to read raw JSON and parse it manually,
     * avoiding deserialization issues with classes from other services.
     */
    public boolean isLobbyJoinable(String lobbyId) {
        try {
            String roomKey = ROOM_KEY_PREFIX + lobbyId;
            String roomJson = stringRedisTemplate.opsForValue().get(roomKey);
            
            if (roomJson == null) {
                logger.warn("Lobby {} not found in Redis (key: {})", lobbyId, roomKey);
                return false;
            }
            
            logger.debug("Room JSON for lobby {}: {}", lobbyId, roomJson);
            
            // Parse the JSON to extract status field
            JsonNode roomNode = objectMapper.readTree(roomJson);
            String status = roomNode.has("status") ? roomNode.get("status").asText() : null;
            
            // Only WAITING lobbies are truly joinable, but we also allow FULL for pending invites
            boolean joinable = "WAITING".equals(status) || "FULL".equals(status);
            logger.debug("Lobby {} status: {}, joinable: {}", lobbyId, status, joinable);
            return joinable;
        } catch (Exception e) {
            logger.error("Error checking lobby status for {}", lobbyId, e);
            return false;
        }
    }

    private GameInviteDto toDto(GameInvite invite) {
        return GameInviteDto.builder()
                .id(invite.getId())
                .senderId(invite.getSenderId())
                .senderUsername(invite.getSenderUsername())
                .targetId(invite.getTargetId())
                .lobbyId(invite.getLobbyId())
                .lobbyName(invite.getLobbyName())
                .gameType(invite.getGameType())
                .accessCode(invite.getAccessCode())
                .createdAt(invite.getCreatedAt())
                .build();
    }
}
