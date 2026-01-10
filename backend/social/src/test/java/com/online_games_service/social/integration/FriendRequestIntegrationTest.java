package com.online_games_service.social.integration;

import com.online_games_service.social.dto.FriendRequestResponseDto;
import com.online_games_service.social.exception.FriendRequestException;
import com.online_games_service.social.exception.FriendRequestException.ErrorCode;
import com.online_games_service.social.model.FriendRequest;
import com.online_games_service.social.model.FriendRequest.Status;
import com.online_games_service.social.model.SocialProfile;
import com.online_games_service.social.repository.FriendRequestRepository;
import com.online_games_service.social.repository.SocialProfileRepository;
import com.online_games_service.social.service.FriendRequestService;
import com.online_games_service.test.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Integration tests for FriendRequestService with real MongoDB and Redis.
 */
@SpringBootTest
public class FriendRequestIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FriendRequestService friendRequestService;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private SocialProfileRepository socialProfileRepository;

    @BeforeMethod
    public void setUp() {
        friendRequestRepository.deleteAll();
        socialProfileRepository.deleteAll();
    }

    @Test
    public void sendFriendRequest_HappyPath_CreatesRequestInDatabase() {
        // Given
        String senderId = "sender123";
        String targetId = "target456";

        // Create target profile (required for validation)
        socialProfileRepository.save(new SocialProfile(targetId));

        // When
        FriendRequestResponseDto response = friendRequestService.sendFriendRequest(
                senderId, "Sender Name", targetId);

        // Then
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getRequestId());
        Assert.assertEquals(response.getStatus(), "PENDING");

        // Verify in database
        Optional<FriendRequest> savedRequest = friendRequestRepository.findById(response.getRequestId());
        Assert.assertTrue(savedRequest.isPresent());
        Assert.assertEquals(savedRequest.get().getRequesterId(), senderId);
        Assert.assertEquals(savedRequest.get().getAddresseeId(), targetId);
        Assert.assertEquals(savedRequest.get().getStatus(), Status.PENDING);
    }

    @Test
    public void sendFriendRequest_SelfReferential_ThrowsException() {
        // Given
        String userId = "user123";

        // When & Then
        try {
            friendRequestService.sendFriendRequest(userId, "User", userId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.SELF_REFERENTIAL_REQUEST);
        }

        // Verify nothing saved
        Assert.assertEquals(friendRequestRepository.count(), 0);
    }

    @Test
    public void sendFriendRequest_TargetNotExists_ThrowsException() {
        // Given
        String senderId = "sender123";
        String nonExistentTarget = "nonexistent";

        // When & Then
        try {
            friendRequestService.sendFriendRequest(senderId, "Sender", nonExistentTarget);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.USER_NOT_FOUND);
        }

        // Verify nothing saved
        Assert.assertEquals(friendRequestRepository.count(), 0);
    }

    @Test
    public void sendFriendRequest_AlreadyFriends_ThrowsException() {
        // Given
        String user1 = "user1";
        String user2 = "user2";

        SocialProfile profile1 = new SocialProfile(user1);
        profile1.addFriend(user2);
        socialProfileRepository.save(profile1);
        socialProfileRepository.save(new SocialProfile(user2));

        // When & Then
        try {
            friendRequestService.sendFriendRequest(user1, "User1", user2);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.ALREADY_FRIENDS);
        }
    }

    @Test
    public void sendFriendRequest_DuplicatePending_ThrowsException() {
        // Given
        String senderId = "sender123";
        String targetId = "target456";

        socialProfileRepository.save(new SocialProfile(targetId));

        // First request succeeds
        friendRequestService.sendFriendRequest(senderId, "Sender", targetId);

        // When & Then - second request should fail
        try {
            friendRequestService.sendFriendRequest(senderId, "Sender", targetId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_PENDING);
        }

        // Verify only one request exists
        Assert.assertEquals(friendRequestRepository.count(), 1);
    }

    @Test
    public void acceptFriendRequest_HappyPath_UpdatesStatusAndAddsFriends() {
        // Given
        String senderId = "sender123";
        String targetId = "target456";

        socialProfileRepository.save(new SocialProfile(senderId));
        socialProfileRepository.save(new SocialProfile(targetId));

        FriendRequest request = new FriendRequest(senderId, targetId);
        request = friendRequestRepository.save(request);
        String requestId = request.getId();

        // When
        FriendRequestResponseDto response = friendRequestService.acceptFriendRequest(
                targetId, "Target Name", requestId);

        // Then
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatus(), "ACCEPTED");

        // Verify request status updated
        FriendRequest updatedRequest = friendRequestRepository.findById(requestId).orElseThrow();
        Assert.assertEquals(updatedRequest.getStatus(), Status.ACCEPTED);

        // Verify both profiles have each other as friends
        SocialProfile senderProfile = socialProfileRepository.findById(senderId).orElseThrow();
        Assert.assertTrue(senderProfile.getFriendIds().contains(targetId));

        SocialProfile targetProfile = socialProfileRepository.findById(targetId).orElseThrow();
        Assert.assertTrue(targetProfile.getFriendIds().contains(senderId));
    }

    @Test
    public void acceptFriendRequest_NotFound_ThrowsException() {
        // Given
        String userId = "user123";
        String fakeRequestId = "nonexistent";

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(userId, "User", fakeRequestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_NOT_FOUND);
        }
    }

    @Test
    public void acceptFriendRequest_NotBelongToUser_ThrowsException() {
        // Given
        String senderId = "sender123";
        String actualTargetId = "target456";
        String wrongUserId = "wrongUser";

        socialProfileRepository.save(new SocialProfile(actualTargetId));

        FriendRequest request = new FriendRequest(senderId, actualTargetId);
        request = friendRequestRepository.save(request);
        String requestId = request.getId();

        // When & Then - wrong user tries to accept
        try {
            friendRequestService.acceptFriendRequest(wrongUserId, "Wrong User", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_NOT_FOUND);
        }

        // Verify request still pending
        FriendRequest unchanged = friendRequestRepository.findById(requestId).orElseThrow();
        Assert.assertEquals(unchanged.getStatus(), Status.PENDING);
    }

    @Test
    public void acceptFriendRequest_AlreadyAccepted_ThrowsException() {
        // Given
        String senderId = "sender123";
        String targetId = "target456";

        socialProfileRepository.save(new SocialProfile(senderId));
        socialProfileRepository.save(new SocialProfile(targetId));

        FriendRequest request = new FriendRequest(senderId, targetId);
        request.setStatus(Status.ACCEPTED);
        request = friendRequestRepository.save(request);
        String requestId = request.getId();

        // When & Then
        try {
            friendRequestService.acceptFriendRequest(targetId, "Target", requestId);
            Assert.fail("Expected FriendRequestException");
        } catch (FriendRequestException e) {
            Assert.assertEquals(e.getErrorCode(), ErrorCode.REQUEST_ALREADY_ACCEPTED);
        }
    }

    @Test
    public void getPendingRequests_ReturnsOnlyPending() {
        // Given
        String targetId = "target123";

        // Create some requests
        FriendRequest pending1 = new FriendRequest("sender1", targetId);
        FriendRequest pending2 = new FriendRequest("sender2", targetId);
        FriendRequest accepted = new FriendRequest("sender3", targetId);
        accepted.setStatus(Status.ACCEPTED);

        friendRequestRepository.save(pending1);
        friendRequestRepository.save(pending2);
        friendRequestRepository.save(accepted);

        // When
        var pendingRequests = friendRequestService.getPendingRequests(targetId);

        // Then
        Assert.assertEquals(pendingRequests.size(), 2);
        Assert.assertTrue(pendingRequests.stream().allMatch(r -> r.getStatus() == Status.PENDING));
    }

    @Test
    public void fullFlow_SendAndAccept_BothUsersBecomeFriends() {
        // Given
        String alice = "alice";
        String bob = "bob";

        socialProfileRepository.save(new SocialProfile(alice));
        socialProfileRepository.save(new SocialProfile(bob));

        // When - Alice sends request to Bob
        FriendRequestResponseDto sendResponse = friendRequestService.sendFriendRequest(
                alice, "Alice", bob);

        // Then - request is pending
        Assert.assertEquals(sendResponse.getStatus(), "PENDING");

        // When - Bob accepts
        FriendRequestResponseDto acceptResponse = friendRequestService.acceptFriendRequest(
                bob, "Bob", sendResponse.getRequestId());

        // Then - request is accepted
        Assert.assertEquals(acceptResponse.getStatus(), "ACCEPTED");

        // And - both are friends
        SocialProfile aliceProfile = socialProfileRepository.findById(alice).orElseThrow();
        SocialProfile bobProfile = socialProfileRepository.findById(bob).orElseThrow();

        Assert.assertTrue(aliceProfile.getFriendIds().contains(bob));
        Assert.assertTrue(bobProfile.getFriendIds().contains(alice));
        Assert.assertEquals(aliceProfile.getFriendCount(), 1);
        Assert.assertEquals(bobProfile.getFriendCount(), 1);
    }
}
