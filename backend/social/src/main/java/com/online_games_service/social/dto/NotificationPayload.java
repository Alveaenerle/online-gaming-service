package com.online_games_service.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification payload for Redis Pub/Sub.
 * Published to channel: user:connected:{targetUserId}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPayload {
    
    private String type;
    private String subType;
    private UserInfo from;
    private FriendInfo newFriend;
    private GameInviteInfo gameInvite;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String id;
        private String name;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FriendInfo {
        private String id;
        private String name;
        private boolean isOnline;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GameInviteInfo {
        private String inviteId;
        private String senderId;
        private String senderUsername;
        private String lobbyId;
        private String lobbyName;
        private String gameType;
    }
    
    public static NotificationPayload friendRequest(String fromId, String fromName) {
        return NotificationPayload.builder()
                .type("NOTIFICATION")
                .subType("FRIEND_REQUEST")
                .from(new UserInfo(fromId, fromName))
                .build();
    }
    
    public static NotificationPayload requestAccepted(String friendId, String friendName, boolean isOnline) {
        return NotificationPayload.builder()
                .type("NOTIFICATION")
                .subType("REQUEST_ACCEPTED")
                .newFriend(new FriendInfo(friendId, friendName, isOnline))
                .build();
    }
    
    public static NotificationPayload gameInvite(String senderId, String senderUsername, 
                                                  String lobbyId, String lobbyName, 
                                                  String gameType, String inviteId) {
        return NotificationPayload.builder()
                .type("NOTIFICATION")
                .subType("GAME_INVITE")
                .gameInvite(GameInviteInfo.builder()
                        .inviteId(inviteId)
                        .senderId(senderId)
                        .senderUsername(senderUsername)
                        .lobbyId(lobbyId)
                        .lobbyName(lobbyName)
                        .gameType(gameType)
                        .build())
                .build();
    }
}
