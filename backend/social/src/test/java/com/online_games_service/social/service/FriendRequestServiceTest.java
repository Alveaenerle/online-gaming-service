package com.online_games_service.social.service;

import com.online_games_service.social.dto.FriendRequestResponseDto;
import com.online_games_service.social.exception.FriendRequestException;
import com.online_games_service.social.exception.FriendRequestException.ErrorCode;
import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.model.FriendRequest.Status;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.FriendRequestRepository;
import com.online_games_service.social.repository.SocialProfileRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for FriendRequestService.
 * Uses mocks for all dependencies to ensure isolation.
 */
public class FriendRequestServiceTest {

    private FriendRequestService friendRequestService;
    private FriendRequestRepository friendRequestRepository;
    private SocialProfileRepository socialProfileRepository;
    private RedisNotificationPublisher notificationPublisher;
    private PresenceService presenceService;

    @BeforeMethod
    public void setUp() {
        friendRequestRepository = mock(FriendRequestRepository.class);
        socialProfileRepository = mock(SocialProfileRepository.class);
        notificationPublisher = mock(RedisNotificationPublisher.class);
        presenceService = mock(PresenceService.class);

        friendRequestService = new FriendRequestService(
                friendRequestRepository,
                socialProfileRepository,
                notificationPublisher,
                presenceService
        );
    }

    // ============================================================
    // SEND FRIEND REQUEST - HAPPY PATH TESTS
    // ============================================================

    @Test
    public void sendFriendRequest_HappyPath_SavesAndPublishes() {
        // Given
        String currentUserId = "user1";
        String currentUserName = "Alice";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        SocialProfile targetProfile = new SocialProfile(targetUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                currentUserId, targetUserId, Status.PENDING)).thenReturn(false);
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                targetUserId, currentUserId, Status.PENDING)).thenReturn(false);

        FriendRequest savedRequest = new FriendRequest(currentUserId, targetUserId);
        savedRequest.setId("request123");
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

        // When
        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                currentUserId, currentUserName, targetUserId);

        // Then
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getRequestId(), "request123");
        Assert.assertEquals(response.getRequesterId(), currentUserId);
        Assert.assertEquals(response.getAddresseeId(), targetUserId);
        Assert.assertEquals(response.getStatus(), Status.PENDING.name());

        // Verify DB save was called
        ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(requestCaptor.capture());
        FriendRequest capturedRequest = requestCaptor.getValue();
        Assert.assertEquals(capturedRequest.getRequesterId(), currentUserId);
        Assert.assertEquals(capturedRequest.getAddresseeId(), targetUserId);

        // Verify Redis publish was called with correct parameters
        verify(notificationPublisher).publishFriendRequest(targetUserId, currentUserId, currentUserName);
    }

    @Test
    public void sendFriendRequest_CreatesProfileIfNotExists() {
        // Given
        String currentUserId = "newUser";
        String currentUserName = "NewUser";
        String targetUserId = "existingUser";

        SocialProfile targetProfile = new SocialProfile(targetUserId);
        SocialProfile newProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.empty());
        when(socialProfileRepository.save(any(SocialProfile.class))).thenReturn(newProfile);
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(false);

        FriendRequest savedRequest = new FriendRequest(currentUserId, targetUserId);
        savedRequest.setId("request456");
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

        // When
        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                currentUserId, currentUserName, targetUserId);

        // Then
        Assert.assertNotNull(response);
        verify(socialProfileRepository).save(any(SocialProfile.class));
    }

    // ============================================================
    // SEND FRIEND REQUEST - EDGE CASE TESTS
    // ============================================================

    @Test
    public void sendFriendRequest_SelfReferential_ThrowsException() {
        // Given
        String userId = "user1";

        // When & Then
        try {
            friendRequestService.sendFriendRequest(userId, "User1", userId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.SELF_REFERENTIAL_REQUEST);
        }

        // Verify no DB or Redis operations
        verify(friendRequestRepository, never()).save(any());
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_TargetUserNotFound_ThrowsException() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "nonexistent";

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(false);

        // When & Then
        try {
            friendRequestService.sendFriendRequest(currentUserId, "User1", targetUserId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.USER_NOT_FOUND);
        }

        verify(friendRequestRepository, never()).save(any());
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_AlreadyFriends_ThrowsException() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        currentProfile.addFriend(targetUserId); // Already friends

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));

        // When & Then
        try {
            friendRequestService.sendFriendRequest(currentUserId, "User1", targetUserId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.ALREADY_FRIENDS);
        }

        verify(friendRequestRepository, never()).save(any());
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_RequestAlreadyPending_ThrowsException() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                currentUserId, targetUserId, Status.PENDING)).thenReturn(true);

        // When & Then
        try {
            friendRequestService.sendFriendRequest(currentUserId, "User1", targetUserId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_PENDING);
        }

        verify(friendRequestRepository, never()).save(any());
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_ReverseRequestPending_ThrowsException() {
        // Given - Target already sent a request to current user
        String currentUserId = "user1";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                currentUserId, targetUserId, Status.PENDING)).thenReturn(false);
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                targetUserId, currentUserId, Status.PENDING)).thenReturn(true);

        // When & Then
        try {
            friendRequestService.sendFriendRequest(currentUserId, "User1", targetUserId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_PENDING);
        }

        verify(friendRequestRepository, never()).save(any());
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    // ============================================================
    // SEND FRIEND REQUEST - TRANSACTIONAL SAFETY TESTS
    // ============================================================

    @Test
    public void sendFriendRequest_DatabaseFails_DoesNotPublishToRedis() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(false);

        // Mock database failure
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("Database connection lost"));

        // When & Then
        try {
            friendRequestService.sendFriendRequest(currentUserId, "User1", targetUserId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.DATABASE_ERROR);
        }

        // CRITICAL: Verify Redis publish was NOT called
        verify(notificationPublisher, never()).publishFriendRequest(anyString(), anyString(), anyString());
    }

    @Test
    public void sendFriendRequest_RedisPublishFails_RequestStillSucceeds() {
        // Given
        String currentUserId = "user1";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(false);

        FriendRequest savedRequest = new FriendRequest(currentUserId, targetUserId);
        savedRequest.setId("request789");
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

        // Mock Redis failure
        doThrow(new RuntimeException("Redis connection failed"))
                .when(notificationPublisher).publishFriendRequest(anyString(), anyString(), anyString());

        // When - should not throw
        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                currentUserId, "User1", targetUserId);

        // Then - request should still succeed
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getRequestId(), "request789");

        // Verify DB save was called
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    // ============================================================
    // ACCEPT FRIEND REQUEST - HAPPY PATH TESTS
    // ============================================================

    @Test
    public void acceptFriendRequest_HappyPath_UpdatesAndPublishes() {
        // Given
        String currentUserId = "user2";
        String currentUserName = "Bob";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        SocialProfile requesterProfile = new SocialProfile(requesterId);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(socialProfileRepository.findById(requesterId)).thenReturn(Optional.of(requesterProfile));
        when(socialProfileRepository.save(any(SocialProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(currentUserId)).thenReturn(true);

        // When
        FriendRequestResponseDto response = friendRequestService.acceptFriendRequest(
                currentUserId, currentUserName, requestId);

        // Then
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getRequestId(), requestId);
        Assert.assertEquals(response.getStatus(), Status.ACCEPTED.name());

        // Verify DB operations
        verify(friendRequestRepository).save(argThat(req -> req.getStatus() == Status.ACCEPTED));
        verify(socialProfileRepository, times(2)).save(any(SocialProfile.class));

        // Verify Redis publish was called
        verify(notificationPublisher).publishRequestAccepted(requesterId, currentUserId, currentUserName, true);
    }

    @Test
    public void acceptFriendRequest_AddsFriendsToBothProfiles() {
        // Given
        String currentUserId = "user2";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        SocialProfile requesterProfile = new SocialProfile(requesterId);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(socialProfileRepository.findById(requesterId)).thenReturn(Optional.of(requesterProfile));
        
        ArgumentCaptor<SocialProfile> profileCaptor = ArgumentCaptor.forClass(SocialProfile.class);
        when(socialProfileRepository.save(profileCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(anyString())).thenReturn(false);

        // When
        friendRequestService.acceptFriendRequest(currentUserId, "Bob", requestId);

        // Then - verify both profiles were updated with correct friends
        var savedProfiles = profileCaptor.getAllValues();
        Assert.assertEquals(savedProfiles.size(), 2);

        // Find and verify current user's profile
        SocialProfile savedCurrentProfile = savedProfiles.stream()
                .filter(p -> p.getId().equals(currentUserId))
                .findFirst().orElseThrow();
        Assert.assertTrue(savedCurrentProfile.getFriendIds().contains(requesterId));

        // Find and verify requester's profile
        SocialProfile savedRequesterProfile = savedProfiles.stream()
                .filter(p -> p.getId().equals(requesterId))
                .findFirst().orElseThrow();
        Assert.assertTrue(savedRequesterProfile.getFriendIds().contains(currentUserId));
    }

    // ============================================================
    // ACCEPT FRIEND REQUEST - EDGE CASE TESTS
    // ============================================================

    @Test
    public void acceptFriendRequest_RequestNotFound_ThrowsException() {
        // Given
        String currentUserId = "user2";
        String requestId = "nonexistent";

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.empty());

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(currentUserId, "User2", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_NOT_FOUND);
        }

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
        verify(socialProfileRepository, never()).save(any(SocialProfile.class));
        verify(notificationPublisher, never()).publishRequestAccepted(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void acceptFriendRequest_AlreadyAccepted_ThrowsException() {
        // Given
        String currentUserId = "user2";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest acceptedRequest = new FriendRequest(requesterId, currentUserId);
        acceptedRequest.setId(requestId);
        acceptedRequest.setStatus(Status.ACCEPTED); // Already accepted

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(acceptedRequest));

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(currentUserId, "User2", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_ACCEPTED);
        }

        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
        verify(socialProfileRepository, never()).save(any(SocialProfile.class));
        verify(notificationPublisher, never()).publishRequestAccepted(anyString(), anyString(), anyString(), anyBoolean());
    }

    // ============================================================
    // ACCEPT FRIEND REQUEST - TRANSACTIONAL SAFETY TESTS
    // ============================================================

    @Test
    public void acceptFriendRequest_DatabaseFails_DoesNotPublishToRedis() {
        // Given
        String currentUserId = "user2";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));

        // Mock database failure on save
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenThrow(new DataAccessResourceFailureException("Database connection lost"));

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(currentUserId, "User2", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.DATABASE_ERROR);
        }

        // CRITICAL: Verify Redis publish was NOT called
        verify(notificationPublisher, never()).publishRequestAccepted(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void acceptFriendRequest_ProfileSaveFails_DoesNotPublishToRedis() {
        // Given
        String currentUserId = "user2";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));

        // Mock database failure on profile save
        when(socialProfileRepository.save(any(SocialProfile.class)))
                .thenThrow(new DataAccessResourceFailureException("Database connection lost"));

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(currentUserId, "User2", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.DATABASE_ERROR);
        }

        // CRITICAL: Verify Redis publish was NOT called
        verify(notificationPublisher, never()).publishRequestAccepted(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void acceptFriendRequest_RedisPublishFails_RequestStillSucceeds() {
        // Given
        String currentUserId = "user2";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        SocialProfile requesterProfile = new SocialProfile(requesterId);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(socialProfileRepository.findById(requesterId)).thenReturn(Optional.of(requesterProfile));
        when(socialProfileRepository.save(any(SocialProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(currentUserId)).thenReturn(true);

        // Mock Redis failure
        doThrow(new RuntimeException("Redis connection failed"))
                .when(notificationPublisher).publishRequestAccepted(anyString(), anyString(), anyString(), anyBoolean());

        // When - should not throw
        FriendRequestResponseDto response = friendRequestService.acceptFriendRequest(
                currentUserId, "Bob", requestId);

        // Then - request should still succeed
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), Status.ACCEPTED.name());

        // Verify DB operations completed
        verify(friendRequestRepository).save(any(FriendRequest.class));
        verify(socialProfileRepository, times(2)).save(any(SocialProfile.class));
    }

    // ============================================================
    // NOTIFICATION PAYLOAD TESTS
    // ============================================================

    @Test
    public void sendFriendRequest_PublishesCorrectPayload() {
        // Given
        String currentUserId = "user1";
        String currentUserName = "Alice";
        String targetUserId = "user2";

        SocialProfile currentProfile = new SocialProfile(currentUserId);

        when(socialProfileRepository.existsById(targetUserId)).thenReturn(true);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(anyString(), anyString(), any()))
                .thenReturn(false);

        FriendRequest savedRequest = new FriendRequest(currentUserId, targetUserId);
        savedRequest.setId("request123");
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(savedRequest);

        // When
        friendRequestService.sendFriendRequest(currentUserId, currentUserName, targetUserId);

        // Then - verify exact payload
        verify(notificationPublisher).publishFriendRequest(
                eq(targetUserId),    // Notification goes to target
                eq(currentUserId),   // From current user
                eq(currentUserName)  // With current user's name
        );
    }

    @Test
    public void acceptFriendRequest_PublishesCorrectPayloadWithOnlineStatus() {
        // Given
        String currentUserId = "user2";
        String currentUserName = "Bob";
        String requesterId = "user1";
        String requestId = "request123";

        FriendRequest pendingRequest = new FriendRequest(requesterId, currentUserId);
        pendingRequest.setId(requestId);
        pendingRequest.setStatus(Status.PENDING);

        SocialProfile currentProfile = new SocialProfile(currentUserId);
        SocialProfile requesterProfile = new SocialProfile(requesterId);

        when(friendRequestRepository.findByIdAndAddresseeId(requestId, currentUserId))
                .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);
        when(socialProfileRepository.findById(currentUserId)).thenReturn(Optional.of(currentProfile));
        when(socialProfileRepository.findById(requesterId)).thenReturn(Optional.of(requesterProfile));
        when(socialProfileRepository.save(any(SocialProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presenceService.isUserOnline(currentUserId)).thenReturn(false);

        // When
        friendRequestService.acceptFriendRequest(currentUserId, currentUserName, requestId);

        // Then - verify exact payload
        verify(notificationPublisher).publishRequestAccepted(
                eq(requesterId),      // Notification goes to original requester
                eq(currentUserId),    // New friend ID
                eq(currentUserName),  // New friend's name
                eq(false)             // Online status
        );
    }
}
