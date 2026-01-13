package com.online_games_service.social.service;

import com.online_games_service.social.dto.PresenceUpdateMessage;
import com.online_games_service.social.dto.PresenceUpdateMessage.PresenceStatus;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.SocialProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Service responsible for notifying friends about presence updates via WebSocket.
 * When a user goes online or offline, this service finds all their friends
 * and sends them a real-time notification.
 */
@Service
public class FriendNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FriendNotificationService.class);
    private static final String PRESENCE_DESTINATION = "/queue/presence";

    private final SocialProfileRepository socialProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    public FriendNotificationService(
            SocialProfileRepository socialProfileRepository,
            SimpMessagingTemplate messagingTemplate,
            PresenceService presenceService) {
        this.socialProfileRepository = socialProfileRepository;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
    }

    /**
     * Notifies all online friends that a user has come online.
     *
     * @param userId The user who came online
     */
    public void notifyFriendsUserOnline(String userId) {
        notifyFriends(userId, PresenceStatus.ONLINE);
    }

    /**
     * Notifies all online friends that a user has gone offline.
     *
     * @param userId The user who went offline
     */
    public void notifyFriendsUserOffline(String userId) {
        notifyFriends(userId, PresenceStatus.OFFLINE);
    }

    private void notifyFriends(String userId, PresenceStatus status) {
        Set<String> friendIds = getFriendIds(userId);
        
        if (friendIds.isEmpty()) {
            logger.info("User {} has no friends to notify about {} status", userId, status);
            return;
        }

        logger.info("Notifying {} friends about user {} going {}", friendIds.size(), userId, status);
        PresenceUpdateMessage message = new PresenceUpdateMessage(userId, status);

        for (String friendId : friendIds) {
            boolean isOnline = presenceService.isUserOnline(friendId);
            logger.info("Friend {} online status: {}", friendId, isOnline);
            
            // Only send to friends who are currently online
            if (isOnline) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            friendId,
                            PRESENCE_DESTINATION,
                            message
                    );
                    logger.info("Sent {} notification for user {} to friend {}", status, userId, friendId);
                } catch (Exception e) {
                    logger.warn("Failed to notify friend {} about user {} going {}: {}", 
                            friendId, userId, status, e.getMessage());
                }
            } else {
                logger.info("Skipping friend {} - not online", friendId);
            }
        }
        
        logger.info("Notified friends of user {} about {} status change", userId, status);
    }

    private Set<String> getFriendIds(String userId) {
        Optional<SocialProfile> profileOpt = socialProfileRepository.findById(userId);
        return profileOpt.map(SocialProfile::getFriendIds)
                .orElse(Set.of());
    }

    /**
     * Sends a friend request notification to the target user via WebSocket.
     *
     * @param targetUserId The user to notify
     * @param fromUserId The user who sent the request
     * @param fromUserName The name of the user who sent the request
     */
    public void sendFriendRequestNotification(String targetUserId, String fromUserId, String fromUserName) {
        try {
            java.util.Map<String, Object> notification = java.util.Map.of(
                    "type", "NOTIFICATION_RECEIVED",
                    "subType", "FRIEND_REQUEST",
                    "senderId", fromUserId,
                    "senderName", fromUserName
            );
            messagingTemplate.convertAndSendToUser(
                    targetUserId,
                    "/queue/notifications",
                    notification
            );
            logger.info("Sent friend request notification to user {} from {}", targetUserId, fromUserName);
        } catch (Exception e) {
            logger.warn("Failed to send friend request notification to user {}: {}", targetUserId, e.getMessage());
        }
    }

    /**
     * Sends a request accepted notification to the original requester via WebSocket.
     *
     * @param targetUserId The user to notify (original requester)
     * @param accepterId The user who accepted the request
     * @param accepterName The name of the user who accepted
     */
    public void sendRequestAcceptedNotification(String targetUserId, String accepterId, String accepterName) {
        try {
            java.util.Map<String, Object> notification = java.util.Map.of(
                    "type", "NOTIFICATION_RECEIVED",
                    "subType", "REQUEST_ACCEPTED",
                    "accepterId", accepterId,
                    "accepterName", accepterName
            );
            messagingTemplate.convertAndSendToUser(
                    targetUserId,
                    "/queue/notifications",
                    notification
            );
            logger.info("Sent request accepted notification to user {} from {}", targetUserId, accepterName);
        } catch (Exception e) {
            logger.warn("Failed to send request accepted notification to user {}: {}", targetUserId, e.getMessage());
        }
    }

    /**
     * Sends a game invite notification to the target user via WebSocket.
     *
     * @param targetUserId The user to notify
     * @param inviteId The ID of the invite
     * @param senderId The user who sent the invite
     * @param senderUsername The username of the sender
     * @param lobbyId The lobby ID
     * @param lobbyName The lobby name
     * @param gameType The type of game
     * @param accessCode The room access code for joining
     */
    public void sendGameInviteNotification(String targetUserId, String inviteId, String senderId, 
                                            String senderUsername, String lobbyId, String lobbyName, 
                                            String gameType, String accessCode) {
        try {
            java.util.Map<String, Object> gameInvite = java.util.Map.of(
                    "id", inviteId,
                    "senderId", senderId,
                    "senderUsername", senderUsername,
                    "lobbyId", lobbyId,
                    "lobbyName", lobbyName,
                    "gameType", gameType,
                    "accessCode", accessCode != null ? accessCode : ""
            );
            java.util.Map<String, Object> notification = java.util.Map.of(
                    "type", "NOTIFICATION_RECEIVED",
                    "subType", "GAME_INVITE",
                    "gameInvite", gameInvite
            );
            messagingTemplate.convertAndSendToUser(
                    targetUserId,
                    "/queue/notifications",
                    notification
            );
            logger.info("Sent game invite notification to user {} from {} for lobby {}", 
                    targetUserId, senderUsername, lobbyName);
        } catch (Exception e) {
            logger.warn("Failed to send game invite notification to user {}: {}", targetUserId, e.getMessage());
        }
    }

    /**
     * Sends mutual presence updates when a new friendship is created.
     * Both users receive a presence update about each other's current status.
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     */
    public void sendMutualPresenceUpdates(String userId1, String userId2) {
        logger.info("Sending mutual presence updates for new friendship between {} and {}", userId1, userId2);
        
        boolean user1Online = presenceService.isUserOnline(userId1);
        boolean user2Online = presenceService.isUserOnline(userId2);
        
        // Notify user1 about user2's status
        if (user1Online) {
            try {
                PresenceUpdateMessage message = new PresenceUpdateMessage(
                        userId2, 
                        user2Online ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE
                );
                messagingTemplate.convertAndSendToUser(
                        userId1,
                        PRESENCE_DESTINATION,
                        message
                );
                logger.info("Sent presence update to {} about {}: {}", userId1, userId2, user2Online ? "ONLINE" : "OFFLINE");
            } catch (Exception e) {
                logger.warn("Failed to send presence update to {}: {}", userId1, e.getMessage());
            }
        }
        
        // Notify user2 about user1's status
        if (user2Online) {
            try {
                PresenceUpdateMessage message = new PresenceUpdateMessage(
                        userId1, 
                        user1Online ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE
                );
                messagingTemplate.convertAndSendToUser(
                        userId2,
                        PRESENCE_DESTINATION,
                        message
                );
                logger.info("Sent presence update to {} about {}: {}", userId2, userId1, user1Online ? "ONLINE" : "OFFLINE");
            } catch (Exception e) {
                logger.warn("Failed to send presence update to {}: {}", userId2, e.getMessage());
            }
        }
    }

    /**
     * Notifies a user that they have been removed from someone's friend list.
     * The removed user will see the friend disappear from their friends list in real-time.
     *
     * @param removedUserId The user who was removed (will receive the notification)
     * @param removedByUserId The user who performed the removal
     */
    public void sendFriendRemovedNotification(String removedUserId, String removedByUserId) {
        try {
            java.util.Map<String, Object> notification = java.util.Map.of(
                    "type", "NOTIFICATION_RECEIVED",
                    "subType", "FRIEND_REMOVED",
                    "removedByUserId", removedByUserId
            );
            messagingTemplate.convertAndSendToUser(
                    removedUserId,
                    "/queue/notifications",
                    notification
            );
            logger.info("Sent friend removed notification to user {} (removed by {})", removedUserId, removedByUserId);
        } catch (Exception e) {
            logger.warn("Failed to send friend removed notification to user {}: {}", removedUserId, e.getMessage());
        }
    }
}
