package com.online_games_service.social.controller;

import com.online_games_service.social.dto.AcceptFriendRequestDto;
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

import java.security.Principal;
import java.util.List;

/**
 * REST controller for friend request operations.
 * All endpoints require authentication via Principal.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendRequestController {

    private static final Logger logger = LoggerFactory.getLogger(FriendRequestController.class);

    private final FriendRequestService friendRequestService;

    public FriendRequestController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    /**
     * Send a friend request to another user.
     * 
     * POST /api/friends/invite
     * 
     * @param request The request containing targetUserId
     * @param principal The authenticated user
     * @return FriendRequestResponseDto with created request details
     */
    @PostMapping("/invite")
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestDto request,
            Principal principal) {
        
        if (principal == null) {
            logger.warn("Attempt to send friend request without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserId = principal.getName();
        // For now, use the user ID as the name. In a real app, you'd fetch the user's display name.
        String currentUserName = currentUserId;

        logger.info("User {} sending friend request to {}", currentUserId, request.getTargetUserId());

        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                currentUserId, 
                currentUserName, 
                request.getTargetUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Accept a friend request.
     * 
     * POST /api/friends/accept
     * 
     * @param request The request containing requestId
     * @param principal The authenticated user
     * @return FriendRequestResponseDto with updated request details
     */
    @PostMapping("/accept")
    public ResponseEntity<FriendRequestResponseDto> acceptFriendRequest(
            @Valid @RequestBody AcceptFriendRequestDto request,
            Principal principal) {
        
        if (principal == null) {
            logger.warn("Attempt to accept friend request without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserId = principal.getName();
        String currentUserName = currentUserId;

        logger.info("User {} accepting friend request {}", currentUserId, request.getRequestId());

        FriendRequestResponseDto response = friendRequestService.acceptFriendRequest(
                currentUserId, 
                currentUserName, 
                request.getRequestId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all pending friend requests for the current user.
     * 
     * GET /api/friends/pending
     * 
     * @param principal The authenticated user
     * @return List of pending friend requests
     */
    @GetMapping("/pending")
    public ResponseEntity<List<FriendRequest>> getPendingRequests(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentUserId = principal.getName();
        List<FriendRequest> pendingRequests = friendRequestService.getPendingRequests(currentUserId);

        return ResponseEntity.ok(pendingRequests);
    }
}
