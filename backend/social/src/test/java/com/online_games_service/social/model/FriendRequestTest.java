package com.online_games_service.social.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

public class FriendRequestTest {

    @Test
    public void shouldCreateRequestWithCurrentDate() {
        // Given
        String requester = "userA";
        String addressee = "userB";

        // When
        FriendRequest request = new FriendRequest(requester, addressee);

        // Then
        Assert.assertEquals(request.getRequesterId(), requester);
        Assert.assertEquals(request.getAddresseeId(), addressee);
        Assert.assertNotNull(request.getCreatedAt(), "CreatedAt should be set automatically");
        Assert.assertNull(request.getId(), "ID should be null before saving to DB");
    }

    @Test
    public void testLombokEqualsAndHashCode() {
        // Given
        FriendRequest req1 = new FriendRequest("userA", "userB");
        FriendRequest req2 = new FriendRequest("userA", "userB");
        
        LocalDateTime now = LocalDateTime.now();
        req1.setCreatedAt(now);
        req2.setCreatedAt(now);
        req1.setId("123");
        req2.setId("123");

        // When & Then
        Assert.assertEquals(req1, req2, "Objects with same data should be equal");
        Assert.assertEquals(req1.hashCode(), req2.hashCode(), "HashCodes should be equal");
    }

    @Test
    public void testLombokToString() {
        // Given
        FriendRequest request = new FriendRequest("userX", "userY");
        request.setId("777");

        // When
        String toString = request.toString();

        // Then
        Assert.assertTrue(toString.contains("userX"));
        Assert.assertTrue(toString.contains("userY"));
        Assert.assertTrue(toString.contains("777"));
        Assert.assertTrue(toString.contains("FriendRequest"));
    }
}