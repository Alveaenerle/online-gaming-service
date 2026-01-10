package com.online_games_service.social.controller;

import com.online_games_service.social.dto.FriendsStatusRequest;
import com.online_games_service.social.dto.FriendsStatusResponse;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.service.FriendNotificationService;
import com.online_games_service.social.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket controller for handling presence-related messages.
 * Handles PING heartbeats and friends status requests.
 */
@Controller
public class PresenceController {

    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);

    private final PresenceService presenceService;
    private final FriendNotificationService friendNotificationService;

    public PresenceController(
            PresenceService presenceService,
            FriendNotificationService friendNotificationService) {
        this.presenceService = presenceService;
        this.friendNotificationService = friendNotificationService;
    }

    /**
     * Handles heartbeat PING messages from clients.
     * Refreshes the user's presence TTL in Redis.
     * 
     * Client sends: STOMP message to /app/presence.ping
     */
    @MessageMapping("/presence.ping")
    public void handlePing(Principal principal) {
        if (principal == null) {
            logger.warn("Received PING without authenticated principal");
            return;
        }

        String userId = principal.getName();
        logger.debug("Received PING from user: {}", userId);
        
        boolean wasOnline = presenceService.isUserOnline(userId);
        presenceService.refreshUserPresence(userId);
        
        // If user was not online before, notify friends
        if (!wasOnline) {
            friendNotificationService.notifyFriendsUserOnline(userId);
        }
    }

    /**
     * Handles requests for friends' online status.
     * Uses MGET for efficient bulk status lookup.
     * 
     * Client sends: STOMP message to /app/presence.getFriendsStatus
     * Client receives: Response on /user/queue/friends-status
     * 
     * @param request Contains list of friend IDs to check
     * @return FriendsStatusResponse with online status of each friend
     */
    @MessageMapping("/presence.getFriendsStatus")
    @SendToUser("/queue/friends-status")
    public FriendsStatusResponse getFriendsStatus(@Payload FriendsStatusRequest request, Principal principal) {
        if (principal == null) {
            logger.warn("Received getFriendsStatus without authenticated principal");
            return new FriendsStatusResponse(List.of());
        }

        String userId = principal.getName();
        logger.debug("User {} requesting friends status for {} friends", userId, 
                request.getFriendIds() != null ? request.getFriendIds().size() : 0);

        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(request.getFriendIds());
        
        return new FriendsStatusResponse(statuses);
    }

    /**
     * Handles explicit logout requests.
     * Immediately removes user from online status and notifies friends.
     * 
     * Client sends: STOMP message to /app/presence.logout
     */
    @MessageMapping("/presence.logout")
    public void handleLogout(Principal principal) {
        if (principal == null) {
            logger.warn("Received logout without authenticated principal");
            return;
        }

        String userId = principal.getName();
        logger.info("User {} explicitly logging out", userId);
        
        presenceService.removeUserOnline(userId);
        friendNotificationService.notifyFriendsUserOffline(userId);
    }
}
