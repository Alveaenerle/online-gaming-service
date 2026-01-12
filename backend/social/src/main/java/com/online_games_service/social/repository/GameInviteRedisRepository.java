package com.online_games_service.social.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.social.model.GameInvite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis repository for GameInvite entities.
 * 
 * Key structure:
 * - game:invite:{inviteId} -> GameInvite JSON (TTL: 1 hour)
 * - game:invites:user:{userId} -> Set of invite IDs for a user (target)
 * - game:invites:lobby:{lobbyId} -> Set of invite IDs for a lobby
 * - game:invites:sender:{senderId}:{targetId}:{lobbyId} -> flag key for duplicate check
 */
@Repository
public class GameInviteRedisRepository {

    private static final Logger logger = LoggerFactory.getLogger(GameInviteRedisRepository.class);

    private static final String INVITE_KEY_PREFIX = "game:invite:";
    private static final String USER_INVITES_KEY_PREFIX = "game:invites:user:";
    private static final String LOBBY_INVITES_KEY_PREFIX = "game:invites:lobby:";
    private static final String SENDER_INVITE_KEY_PREFIX = "game:invites:sender:";
    private static final long INVITE_TTL_HOURS = 1;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public GameInviteRedisRepository(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Save a game invite to Redis.
     */
    public GameInvite save(GameInvite invite) {
        try {
            String key = INVITE_KEY_PREFIX + invite.getId();
            String json = objectMapper.writeValueAsString(invite);
            
            // Save the invite with TTL
            redisTemplate.opsForValue().set(key, json, INVITE_TTL_HOURS, TimeUnit.HOURS);
            
            // Add to user's invite set
            String userKey = USER_INVITES_KEY_PREFIX + invite.getTargetId();
            redisTemplate.opsForSet().add(userKey, invite.getId());
            
            // Add to lobby's invite set
            String lobbyKey = LOBBY_INVITES_KEY_PREFIX + invite.getLobbyId();
            redisTemplate.opsForSet().add(lobbyKey, invite.getId());
            
            // Add sender-target-lobby key for duplicate check
            String senderKey = SENDER_INVITE_KEY_PREFIX + invite.getSenderId() + ":" + 
                              invite.getTargetId() + ":" + invite.getLobbyId();
            redisTemplate.opsForValue().set(senderKey, invite.getId(), INVITE_TTL_HOURS, TimeUnit.HOURS);
            
            logger.debug("Saved game invite {} from {} to {} for lobby {}", 
                        invite.getId(), invite.getSenderId(), invite.getTargetId(), invite.getLobbyId());
            
            return invite;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize game invite", e);
            throw new RuntimeException("Failed to save game invite", e);
        }
    }

    /**
     * Find an invite by ID.
     */
    public Optional<GameInvite> findById(String inviteId) {
        String key = INVITE_KEY_PREFIX + inviteId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(objectMapper.readValue(value.toString(), GameInvite.class));
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize game invite {}", inviteId, e);
            return Optional.empty();
        }
    }

    /**
     * Find all pending invites for a user (target).
     */
    public List<GameInvite> findByTargetId(String targetId) {
        String userKey = USER_INVITES_KEY_PREFIX + targetId;
        Set<Object> inviteIds = redisTemplate.opsForSet().members(userKey);
        
        if (inviteIds == null || inviteIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return inviteIds.stream()
                .map(id -> findById(id.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Find all invites sent by a user.
     */
    public List<GameInvite> findBySenderId(String senderId) {
        // This requires scanning all invites or maintaining another index
        // For efficiency, we'll scan the user invites and filter
        Set<String> keys = redisTemplate.keys(INVITE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        
        return keys.stream()
                .map(key -> {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value == null) return null;
                    try {
                        return objectMapper.readValue(value.toString(), GameInvite.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(invite -> senderId.equals(invite.getSenderId()))
                .collect(Collectors.toList());
    }

    /**
     * Check if an invite already exists from sender to target for a specific lobby.
     */
    public boolean existsBySenderIdAndTargetIdAndLobbyId(String senderId, String targetId, String lobbyId) {
        String senderKey = SENDER_INVITE_KEY_PREFIX + senderId + ":" + targetId + ":" + lobbyId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(senderKey));
    }

    /**
     * Delete an invite by ID.
     */
    public void deleteById(String inviteId) {
        Optional<GameInvite> inviteOpt = findById(inviteId);
        if (inviteOpt.isEmpty()) {
            return;
        }
        
        GameInvite invite = inviteOpt.get();
        
        // Delete the main invite key
        redisTemplate.delete(INVITE_KEY_PREFIX + inviteId);
        
        // Remove from user's invite set
        String userKey = USER_INVITES_KEY_PREFIX + invite.getTargetId();
        redisTemplate.opsForSet().remove(userKey, inviteId);
        
        // Remove from lobby's invite set
        String lobbyKey = LOBBY_INVITES_KEY_PREFIX + invite.getLobbyId();
        redisTemplate.opsForSet().remove(lobbyKey, inviteId);
        
        // Remove sender-target-lobby key
        String senderKey = SENDER_INVITE_KEY_PREFIX + invite.getSenderId() + ":" + 
                          invite.getTargetId() + ":" + invite.getLobbyId();
        redisTemplate.delete(senderKey);
        
        logger.debug("Deleted game invite {}", inviteId);
    }

    /**
     * Delete all invites for a lobby (called when game starts).
     */
    public int deleteAllByLobbyId(String lobbyId) {
        String lobbyKey = LOBBY_INVITES_KEY_PREFIX + lobbyId;
        Set<Object> inviteIds = redisTemplate.opsForSet().members(lobbyKey);
        
        if (inviteIds == null || inviteIds.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (Object inviteId : inviteIds) {
            deleteById(inviteId.toString());
            count++;
        }
        
        // Delete the lobby set itself
        redisTemplate.delete(lobbyKey);
        
        logger.info("Deleted {} game invites for lobby {}", count, lobbyId);
        return count;
    }
}
