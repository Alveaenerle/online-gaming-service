package com.online_games_service.social.service;

import com.online_games_service.social.dto.FriendDto;
import com.online_games_service.social.dto.FriendRequestResponseDto;
import com.online_games_service.social.exception.FriendRequestException;
import com.online_games_service.social.exception.FriendRequestException.ErrorCode;
import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.model.FriendRequest.Status;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.FriendRequestRepository;
import com.online_games_service.social.repository.SocialProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling friend request operations.
 * Ensures atomic database operations and only publishes to Redis on success.
 */
@Service
public class FriendRequestService {

    private static final Logger logger = LoggerFactory.getLogger(FriendRequestService.class);

    private final FriendRequestRepository friendRequestRepository;
    private final SocialProfileRepository socialProfileRepository;
    private final RedisNotificationPublisher notificationPublisher;
    private final FriendNotificationService friendNotificationService;
    private final PresenceService presenceService;

    public FriendRequestService(
            FriendRequestRepository friendRequestRepository,
            SocialProfileRepository socialProfileRepository,
            RedisNotificationPublisher notificationPublisher,
            FriendNotificationService friendNotificationService,
            PresenceService presenceService) {
        this.friendRequestRepository = friendRequestRepository;
        this.socialProfileRepository = socialProfileRepository;
        this.notificationPublisher = notificationPublisher;
        this.friendNotificationService = friendNotificationService;
        this.presenceService = presenceService;
    }

    /**
     * Sends a friend request from the current user to the target user.
     * 
     * @param currentUserId The ID of the user sending the request
     * @param currentUserName The name of the user sending the request (for notification)
     * @param targetUserId The ID of the user to send the request to
     * @return FriendRequestResponseDto with the created request details
     * @throws FriendRequestException if validation fails or DB operation fails
     */
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(String currentUserId, String currentUserName, String targetUserId) {
        logger.info("User {} sending friend request to user {}", currentUserId, targetUserId);

        // Validation 1: Cannot send request to yourself
        if (currentUserId.equals(targetUserId)) {
            throw new FriendRequestException(ErrorCode.SELF_REFERENTIAL_REQUEST);
        }

        // Validation 2: Target user profile should exist (lazy create if not)
        if (!socialProfileRepository.existsById(targetUserId)) {
            SocialProfile targetProfile = new SocialProfile(targetUserId);
            socialProfileRepository.save(targetProfile);
            logger.info("Lazily created social profile for target user: {}", targetUserId);
        }

        // Validation 3: Check if already friends
        SocialProfile currentUserProfile = socialProfileRepository.findById(currentUserId)
                .orElseGet(() -> {
                    // Create profile if doesn't exist
                    SocialProfile newProfile = new SocialProfile(currentUserId);
                    return socialProfileRepository.save(newProfile);
                });
        
        if (currentUserProfile.getFriendIds().contains(targetUserId)) {
            throw new FriendRequestException(ErrorCode.ALREADY_FRIENDS);
        }

        // Validation 4: Check for existing pending request (in either direction)
        if (friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                currentUserId, targetUserId, Status.PENDING)) {
            throw new FriendRequestException(ErrorCode.REQUEST_ALREADY_PENDING);
        }
        
        // Check if there's a pending request from target to current user
        if (friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                targetUserId, currentUserId, Status.PENDING)) {
            throw new FriendRequestException(ErrorCode.REQUEST_ALREADY_PENDING, 
                    "There is already a pending request from this user to you");
        }

        // Atomic DB Write: Create friend request
        FriendRequest request;
        try {
            request = new FriendRequest(currentUserId, currentUserName, targetUserId);
            request = friendRequestRepository.save(request);
            logger.info("Friend request created with ID: {}", request.getId());
        } catch (DataAccessException e) {
            logger.error("Database error while creating friend request: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }

        // Side Effect: Publish notifications ONLY after successful DB write
        try {
            notificationPublisher.publishFriendRequest(targetUserId, currentUserId, currentUserName);
            // Also send via WebSocket for real-time updates
            friendNotificationService.sendFriendRequestNotification(targetUserId, currentUserId, currentUserName);
        } catch (Exception e) {
            // Log but don't fail the request - notification is best-effort
            logger.warn("Failed to publish friend request notification: {}", e.getMessage());
        }

        return new FriendRequestResponseDto(
                request.getId(),
                request.getRequesterId(),
                request.getRequesterUsername(),
                request.getAddresseeId(),
                request.getStatus().name(),
                request.getCreatedAt(),
                "Friend request sent successfully"
        );
    }

    /**
     * Accepts a friend request.
     * 
     * @param currentUserId The ID of the user accepting the request
     * @param currentUserName The name of the user accepting the request (for notification)
     * @param requestId The ID of the friend request to accept
     * @return FriendRequestResponseDto with the updated request details
     * @throws FriendRequestException if validation fails or DB operation fails
     */
    @Transactional
    public FriendRequestResponseDto acceptFriendRequest(String currentUserId, String currentUserName, String requestId) {
        logger.info("User {} accepting friend request {}", currentUserId, requestId);

        // Validation 1: Request must exist and belong to current user
        FriendRequest request = friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId)
                .orElseThrow(() -> new FriendRequestException(ErrorCode.REQUEST_NOT_FOUND));

        // Validation 2: Request must be in PENDING status
        if (request.getStatus() == Status.ACCEPTED) {
            throw new FriendRequestException(ErrorCode.REQUEST_ALREADY_ACCEPTED);
        }

        String requesterId = request.getRequesterId();
        String requesterUsername = request.getRequesterUsername();

        // Atomic DB Write: Update request status and add friends to both profiles
        try {
            // Update request status
            request.setStatus(Status.ACCEPTED);
            request = friendRequestRepository.save(request);

            // Add friend to current user's profile (with requester's username)
            SocialProfile currentUserProfile = socialProfileRepository.findById(currentUserId)
                    .orElseGet(() -> new SocialProfile(currentUserId));
            currentUserProfile.addFriend(requesterId, requesterUsername);
            socialProfileRepository.save(currentUserProfile);

            // Add friend to requester's profile (with current user's username)
            SocialProfile requesterProfile = socialProfileRepository.findById(requesterId)
                    .orElseGet(() -> new SocialProfile(requesterId));
            requesterProfile.addFriend(currentUserId, currentUserName);
            socialProfileRepository.save(requesterProfile);

            logger.info("Friend request {} accepted. {} and {} are now friends", 
                    requestId, currentUserId, requesterId);
        } catch (DataAccessException e) {
            logger.error("Database error while accepting friend request: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }

        // Side Effect: Publish notifications ONLY after successful DB write
        try {
            boolean isOnline = presenceService.isUserOnline(currentUserId);
            notificationPublisher.publishRequestAccepted(requesterId, currentUserId, currentUserName, isOnline);
            // Also send via WebSocket for real-time updates
            friendNotificationService.sendRequestAcceptedNotification(requesterId, currentUserId, currentUserName);
            // Send mutual presence updates so both users see each other's online status
            friendNotificationService.sendMutualPresenceUpdates(currentUserId, requesterId);
        } catch (Exception e) {
            // Log but don't fail the request - notification is best-effort
            logger.warn("Failed to publish request accepted notification: {}", e.getMessage());
        }

        return new FriendRequestResponseDto(
                request.getId(),
                request.getRequesterId(),
                request.getRequesterUsername(),
                request.getAddresseeId(),
                request.getStatus().name(),
                request.getCreatedAt(),
                "Friend request accepted successfully"
        );
    }

    /**
     * Gets all pending friend requests for a user.
     * 
     * @param userId The user ID
     * @return List of pending friend requests
     */
    public java.util.List<FriendRequestResponseDto> getPendingRequests(String userId) {
        return friendRequestRepository.findAllByAddresseeIdAndStatus(userId, Status.PENDING)
                .stream()
                .map(req -> new FriendRequestResponseDto(
                        req.getId(),
                        req.getRequesterId(),
                        req.getRequesterUsername(),
                        req.getAddresseeId(),
                        req.getStatus().name(),
                        req.getCreatedAt(),
                        null
                ))
                .toList();
    }

    /**
     * Gets all sent pending friend requests for a user.
     * 
     * @param userId The user ID
     * @return List of sent pending friend requests
     */
    public java.util.List<FriendRequestResponseDto> getSentRequests(String userId) {
        return friendRequestRepository.findAllByRequesterIdAndStatus(userId, Status.PENDING)
                .stream()
                .map(req -> new FriendRequestResponseDto(
                        req.getId(),
                        req.getRequesterId(),
                        req.getRequesterUsername(),
                        req.getAddresseeId(),
                        req.getStatus().name(),
                        req.getCreatedAt(),
                        null
                ))
                .toList();
    }

    /**
     * Gets all friends for a user.
     * 
     * @param userId The user ID
     * @return List of friends with their status
     */
    public java.util.List<FriendDto> getFriends(String userId) {
        return socialProfileRepository.findById(userId)
                .map(profile -> profile.getFriendIds().stream()
                        .map(friendId -> {
                            String status = presenceService.isUserOnline(friendId) ? "ONLINE" : "OFFLINE";
                            String username = profile.getFriendUsername(friendId);
                            return new FriendDto(friendId, username, status, null);
                        })
                        .toList())
                .orElse(java.util.Collections.emptyList());
    }

    /**
     * Rejects a friend request by deleting it.
     * This allows the sender to send a new request later.
     * 
     * @param currentUserId The ID of the user rejecting the request
     * @param requestId The ID of the friend request to reject
     * @return FriendRequestResponseDto with the deleted request details
     * @throws FriendRequestException if validation fails or DB operation fails
     */
    @Transactional
    public FriendRequestResponseDto rejectFriendRequest(String currentUserId, String requestId) {
        logger.info("User {} rejecting friend request {}", currentUserId, requestId);

        // Validation: Request must exist and belong to current user
        FriendRequest request = friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId)
                .orElseThrow(() -> new FriendRequestException(ErrorCode.REQUEST_NOT_FOUND));

        // Validation: Request must be in PENDING status
        if (request.getStatus() != Status.PENDING) {
            throw new FriendRequestException(ErrorCode.REQUEST_ALREADY_ACCEPTED, "Request is no longer pending");
        }

        // Store request details before deletion
        String requestIdValue = request.getId();
        String requesterId = request.getRequesterId();
        String requesterUsername = request.getRequesterUsername();
        String addresseeId = request.getAddresseeId();
        java.time.LocalDateTime createdAt = request.getCreatedAt();

        try {
            // Delete the request so sender can send a new one later
            friendRequestRepository.delete(request);
            logger.info("Friend request {} rejected and deleted", requestIdValue);
        } catch (DataAccessException e) {
            logger.error("Database error while rejecting friend request: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }

        return new FriendRequestResponseDto(
                requestIdValue,
                requesterId,
                requesterUsername,
                addresseeId,
                "REJECTED",
                createdAt,
                "Friend request rejected"
        );
    }

    /**
     * Removes a friend from the current user's friend list.
     * 
     * @param currentUserId The ID of the user removing the friend
     * @param friendId The ID of the friend to remove
     * @throws FriendRequestException if validation fails or DB operation fails
     */
    @Transactional
    public void removeFriend(String currentUserId, String friendId) {
        logger.info("User {} removing friend {}", currentUserId, friendId);

        try {
            // Remove from current user's profile
            SocialProfile currentUserProfile = socialProfileRepository.findById(currentUserId)
                    .orElse(null);
            if (currentUserProfile != null) {
                currentUserProfile.removeFriend(friendId);
                socialProfileRepository.save(currentUserProfile);
            }

            // Remove from the other user's profile
            SocialProfile friendProfile = socialProfileRepository.findById(friendId)
                    .orElse(null);
            if (friendProfile != null) {
                friendProfile.removeFriend(currentUserId);
                socialProfileRepository.save(friendProfile);
            }

            // Also delete any accepted friend request between them
            friendRequestRepository.deleteByRequesterIdAndAddresseeIdAndStatus(currentUserId, friendId, Status.ACCEPTED);
            friendRequestRepository.deleteByRequesterIdAndAddresseeIdAndStatus(friendId, currentUserId, Status.ACCEPTED);

            logger.info("Friend {} removed from user {}'s friend list", friendId, currentUserId);
        } catch (DataAccessException e) {
            logger.error("Database error while removing friend: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }
    }
}
