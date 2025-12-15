package com.online_games_service.social.model;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FriendRequestTest {

    @Test
    public void shouldInitializeCorrectly() {
        // Given
        String requester = "userA";
        String addressee = "userB";

        // When
        FriendRequest request = new FriendRequest(requester, addressee);

        // Then
        Assert.assertEquals(request.getRequesterId(), requester);
        Assert.assertEquals(request.getAddresseeId(), addressee);
        
        Assert.assertNull(request.getCreatedAt());
        Assert.assertNull(request.getId());
    }
}