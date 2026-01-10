package com.online_games_service.social.controller;

import com.online_games_service.social.dto.FriendsStatusRequest;
import com.online_games_service.social.dto.FriendsStatusResponse;
import com.online_games_service.social.dto.UserPresenceStatus;
import com.online_games_service.social.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for presence-related operations.
 * Provides an alternative to WebSocket for getting friends status.
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceRestController {

    private static final Logger logger = LoggerFactory.getLogger(PresenceRestController.class);

    private final PresenceService presenceService;

    public PresenceRestController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Gets the online status of multiple friends.
     * Uses MGET for efficient bulk lookup.
     * 
     * POST /api/presence/friends-status
     * 
     * @param request Contains list of friend IDs to check
     * @return FriendsStatusResponse with online status of each friend
     */
    @PostMapping("/friends-status")
    public ResponseEntity<FriendsStatusResponse> getFriendsStatus(@RequestBody FriendsStatusRequest request) {
        logger.debug("REST request for friends status: {} friends", 
                request.getFriendIds() != null ? request.getFriendIds().size() : 0);

        List<UserPresenceStatus> statuses = presenceService.getUsersOnlineStatus(request.getFriendIds());
        
        return ResponseEntity.ok(new FriendsStatusResponse(statuses));
    }

    /**
     * Checks if a single user is online.
     * 
     * GET /api/presence/{userId}
     * 
     * @param userId The user ID to check
     * @return UserPresenceStatus with online flag
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserPresenceStatus> getUserPresence(@PathVariable String userId) {
        logger.debug("REST request for user {} presence", userId);
        
        boolean isOnline = presenceService.isUserOnline(userId);
        
        return ResponseEntity.ok(new UserPresenceStatus(userId, isOnline));
    }
}
