package com.online_games_service.social.service;

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
    private final PresenceService presenceService;

    public FriendRequestService(
            FriendRequestRepository friendRequestRepository,
            SocialProfileRepository socialProfileRepository,
            RedisNotificationPublisher notificationPublisher,
            PresenceService presenceService) {
        this.friendRequestRepository = friendRequestRepository;
        this.socialProfileRepository = socialProfileRepository;
        this.notificationPublisher = notificationPublisher;
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

        // Validation 2: Target user must exist
        if (!socialProfileRepository.existsById(targetUserId)) {
            throw new FriendRequestException(ErrorCode.USER_NOT_FOUND);
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
            request = new FriendRequest(currentUserId, targetUserId);
            request = friendRequestRepository.save(request);
            logger.info("Friend request created with ID: {}", request.getId());
        } catch (DataAccessException e) {
            logger.error("Database error while creating friend request: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }

        // Side Effect: Publish to Redis ONLY after successful DB write
        try {
            notificationPublisher.publishFriendRequest(targetUserId, currentUserId, currentUserName);
        } catch (Exception e) {
            // Log but don't fail the request - notification is best-effort
            logger.warn("Failed to publish friend request notification: {}", e.getMessage());
        }

        return new FriendRequestResponseDto(
                request.getId(),
                request.getRequesterId(),
                request.getAddresseeId(),
                request.getStatus().name(),
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

        // Atomic DB Write: Update request status and add friends to both profiles
        try {
            // Update request status
            request.setStatus(Status.ACCEPTED);
            request = friendRequestRepository.save(request);

            // Add friend to current user's profile
            SocialProfile currentUserProfile = socialProfileRepository.findById(currentUserId)
                    .orElseGet(() -> new SocialProfile(currentUserId));
            currentUserProfile.addFriend(requesterId);
            socialProfileRepository.save(currentUserProfile);

            // Add friend to requester's profile
            SocialProfile requesterProfile = socialProfileRepository.findById(requesterId)
                    .orElseGet(() -> new SocialProfile(requesterId));
            requesterProfile.addFriend(currentUserId);
            socialProfileRepository.save(requesterProfile);

            logger.info("Friend request {} accepted. {} and {} are now friends", 
                    requestId, currentUserId, requesterId);
        } catch (DataAccessException e) {
            logger.error("Database error while accepting friend request: {}", e.getMessage(), e);
            throw new FriendRequestException(ErrorCode.DATABASE_ERROR, e);
        }

        // Side Effect: Publish to Redis ONLY after successful DB write
        try {
            boolean isOnline = presenceService.isUserOnline(currentUserId);
            notificationPublisher.publishRequestAccepted(requesterId, currentUserId, currentUserName, isOnline);
        } catch (Exception e) {
            // Log but don't fail the request - notification is best-effort
            logger.warn("Failed to publish request accepted notification: {}", e.getMessage());
        }

        return new FriendRequestResponseDto(
                request.getId(),
                request.getRequesterId(),
                request.getAddresseeId(),
                request.getStatus().name(),
                "Friend request accepted successfully"
        );
    }

    /**
     * Gets all pending friend requests for a user.
     * 
     * @param userId The user ID
     * @return List of pending friend requests
     */
    public java.util.List<FriendRequest> getPendingRequests(String userId) {
        return friendRequestRepository.findAllByAddresseeIdAndStatus(userId, Status.PENDING);
    }
}
