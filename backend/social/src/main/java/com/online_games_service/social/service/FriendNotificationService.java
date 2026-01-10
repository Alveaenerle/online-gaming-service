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
            logger.debug("User {} has no friends to notify about {} status", userId, status);
            return;
        }

        PresenceUpdateMessage message = new PresenceUpdateMessage(userId, status);

        for (String friendId : friendIds) {
            // Only send to friends who are currently online
            if (presenceService.isUserOnline(friendId)) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            friendId,
                            PRESENCE_DESTINATION,
                            message
                    );
                    logger.debug("Sent {} notification for user {} to friend {}", status, userId, friendId);
                } catch (Exception e) {
                    logger.warn("Failed to notify friend {} about user {} going {}: {}", 
                            friendId, userId, status, e.getMessage());
                }
            }
        }
        
        logger.info("Notified friends of user {} about {} status change", userId, status);
    }

    private Set<String> getFriendIds(String userId) {
        Optional<SocialProfile> profileOpt = socialProfileRepository.findById(userId);
        return profileOpt.map(SocialProfile::getFriendIds)
                .orElse(Set.of());
    }
}
