package com.online_games_service.social.controller;

import com.online_games_service.social.dto.AcceptFriendRequestDto;
import com.online_games_service.social.dto.FriendDto;
import com.online_games_service.social.dto.FriendRequestResponseDto;
import com.online_games_service.social.dto.SendFriendRequestDto;
import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.service.FriendRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for friend request operations.
 * All endpoints require authentication via SessionUserFilter.
 * The filter extracts userId and username from the session cookie.
 */
@RestController
@RequestMapping("/friends")
public class FriendRequestController {

    private static final Logger logger = LoggerFactory.getLogger(FriendRequestController.class);

    private final FriendRequestService friendRequestService;

    public FriendRequestController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    /**
     * Send a friend request to another user.
     * 
     * POST /friends/invite
     * 
     * @param request The request containing targetUserId
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @param username The authenticated user's name (from SessionUserFilter)
     * @return FriendRequestResponseDto with created request details
     */
    @PostMapping("/invite")
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestDto request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        
        if (userId == null) {
            logger.warn("Attempt to send friend request without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserName = username != null ? username : userId;

        logger.info("User {} sending friend request to {}", userId, request.getTargetUserId());

        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                userId, 
                currentUserName, 
                request.getTargetUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Accept a friend request.
     * 
     * POST /friends/accept
     * 
     * @param request The request containing requestId
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @param username The authenticated user's name (from SessionUserFilter)
     * @return FriendRequestResponseDto with updated request details
     */
    @PostMapping("/accept")
    public ResponseEntity<FriendRequestResponseDto> acceptFriendRequest(
            @Valid @RequestBody AcceptFriendRequestDto request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {
        
        if (userId == null) {
            logger.warn("Attempt to accept friend request without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserName = username != null ? username : userId;

        logger.info("User {} accepting friend request {}", userId, request.getRequestId());

        FriendRequestResponseDto response = friendRequestService.acceptFriendRequest(
                userId, 
                currentUserName, 
                request.getRequestId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Reject a friend request.
     * 
     * POST /friends/reject
     * 
     * @param request The request containing requestId
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @return FriendRequestResponseDto with updated request details
     */
    @PostMapping("/reject")
    public ResponseEntity<FriendRequestResponseDto> rejectFriendRequest(
            @Valid @RequestBody AcceptFriendRequestDto request,
            @RequestAttribute(value = "userId", required = false) String userId) {
        
        if (userId == null) {
            logger.warn("Attempt to reject friend request without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} rejecting friend request {}", userId, request.getRequestId());

        FriendRequestResponseDto response = friendRequestService.rejectFriendRequest(
                userId, 
                request.getRequestId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all pending friend requests for the current user.
     * 
     * GET /friends/pending
     * 
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @return List of pending friend requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<FriendRequestResponseDto>> getPendingRequests(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FriendRequestResponseDto> pendingRequests = friendRequestService.getPendingRequests(userId);

        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Get all sent friend requests for the current user.
     * 
     * GET /friends/sent
     * 
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @return List of sent friend requests
     */
    @GetMapping("/sent")
    public ResponseEntity<List<FriendRequestResponseDto>> getSentRequests(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FriendRequestResponseDto> sentRequests = friendRequestService.getSentRequests(userId);

        return ResponseEntity.ok(sentRequests);
    }

    /**
     * Get all friends for the current user.
     * 
     * GET /friends
     * 
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @return List of friends
     */
    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends(
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FriendDto> friends = friendRequestService.getFriends(userId);

        return ResponseEntity.ok(friends);
    }

    /**
     * Remove a friend.
     * 
     * DELETE /friends/{friendId}
     * 
     * @param friendId The ID of the friend to remove
     * @param userId The authenticated user's ID (from SessionUserFilter)
     * @return 204 No Content on success
     */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable String friendId,
            @RequestAttribute(value = "userId", required = false) String userId) {
        if (userId == null) {
            logger.warn("Attempt to remove friend without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} removing friend {}", userId, friendId);

        friendRequestService.removeFriend(userId, friendId);

        return ResponseEntity.noContent().build();
    }
}
