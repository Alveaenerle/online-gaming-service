# Social Service Documentation

## Overview

The **Social Service** is a Spring Boot microservice that manages social features including friend lists, friend requests, presence tracking, game invitations, and real-time notifications.

## Technology Stack

- **Framework**: Spring Boot
- **Real-time Communication**: WebSocket with STOMP protocol
- **Database**: MongoDB for social profiles and relationships
- **State Management**: Redis for presence tracking
- **Testing**: TestNG

## Architecture

```
social/
├── config/           # WebSocket, security configuration
├── controller/       # REST API and WebSocket handlers
├── dto/              # Data Transfer Objects
├── exception/        # Custom exceptions
├── messaging/        # Event listeners
├── model/            # SocialProfile, FriendRequest, GameInvite
├── repository/       # MongoDB repositories
└── service/          # Business logic
```

## API Endpoints

### Friend Management

| Method | Endpoint                | Description                    |
|--------|-------------------------|--------------------------------|
| POST   | `/friends/request`      | Send friend request            |
| POST   | `/friends/accept/{id}`  | Accept friend request          |
| POST   | `/friends/reject/{id}`  | Reject friend request          |
| DELETE | `/friends/{friendId}`   | Remove friend                  |
| GET    | `/friends`              | Get friends list               |
| GET    | `/friends/requests`     | Get pending requests           |
| GET    | `/friends/sent`         | Get sent requests              |

### Game Invitations

| Method | Endpoint                      | Description                    |
|--------|-------------------------------|--------------------------------|
| POST   | `/invites/send`               | Send game invite to friend     |
| POST   | `/invites/accept/{inviteId}`  | Accept game invite             |
| POST   | `/invites/reject/{inviteId}`  | Reject game invite             |
| GET    | `/invites`                    | Get pending game invites       |

### Presence

| Method | Endpoint                | Description                    |
|--------|-------------------------|--------------------------------|
| GET    | `/presence/friends`     | Get friends' online status     |

### WebSocket Endpoints

| Destination                       | Description                    |
|-----------------------------------|--------------------------------|
| `/queue/notifications`            | Personal notifications         |
| `/queue/presence`                 | Friend presence updates        |
| `/queue/friends-status`           | Friends status response        |
| `/app/presence.getFriendsStatus`  | Request friends' status        |

## Data Models

### SocialProfile
```json
{
  "id": "userId",
  "friendIds": ["friend1", "friend2"],
  "friendUsernames": {
    "friend1": "FriendOne",
    "friend2": "FriendTwo"
  },
  "friendCount": 2
}
```

### FriendRequest
```json
{
  "id": "request-uuid",
  "senderId": "user1",
  "senderUsername": "User One",
  "targetId": "user2",
  "status": "PENDING",
  "createdAt": "2024-01-01T12:00:00"
}
```

### GameInvite
```json
{
  "id": "invite-uuid",
  "senderId": "user1",
  "senderUsername": "User One",
  "lobbyId": "lobby-id",
  "lobbyName": "Game Room",
  "gameType": "MAKAO",
  "accessCode": "ABC123",
  "createdAt": "2024-01-01T12:00:00"
}
```

## Features

### Friend System
- Send/accept/reject friend requests
- Remove existing friends
- Mutual friendship (both users add each other)
- Username caching for display

### Real-time Presence
- Online/offline status tracking
- Friends notified when you come online
- Live status updates via WebSocket

### Friend Request Notifications
- Instant WebSocket notification on new request
- Notification when request accepted
- Request history

### Game Invitations
- Invite friends to current lobby
- Include access code for private rooms
- Accept to auto-join lobby
- Expire after set time

### Friend Removal Notifications
- Real-time notification when removed
- Friend list auto-updates

## Notification Types

### WebSocket Notification Structure
```json
{
  "type": "NOTIFICATION_RECEIVED",
  "subType": "FRIEND_REQUEST",
  ...additional fields
}
```

### Notification SubTypes

| SubType          | Description                        |
|------------------|------------------------------------|
| FRIEND_REQUEST   | New friend request received        |
| REQUEST_ACCEPTED | Your request was accepted          |
| GAME_INVITE      | Game invitation received           |
| FRIEND_REMOVED   | Removed from someone's friend list |
| PRESENCE_UPDATE  | Friend online/offline status       |

## Presence Service

### Tracking Methods
- WebSocket connection monitoring
- Heartbeat mechanism
- Disconnect detection

### Status Updates
```json
{
  "userId": "friend-id",
  "status": "ONLINE",
  "timestamp": "2024-01-01T12:00:00"
}
```

## Error Handling

| Scenario                    | HTTP Status | Response                     |
|-----------------------------|-------------|------------------------------|
| User not found              | 404         | "User not found"             |
| Already friends             | 400         | "Already friends"            |
| Request already sent        | 400         | "Request already pending"    |
| Cannot add self             | 400         | "Cannot add yourself"        |
| Invite expired              | 400         | "Invite has expired"         |

## Configuration

### Environment Variables

| Variable                   | Description                           | Default                    |
|----------------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`              | Application port                      | `8080`                     |
| `MONGODB_URI`              | MongoDB connection string             | `mongodb://localhost:27017/social` |
| `REDIS_HOST`               | Redis server host                     | `localhost`                |
| `REDIS_PORT`               | Redis server port                     | `6379`                     |
| `CORS_ORIGINS`             | Allowed CORS origins                  | `http://localhost:5173`    |
| `INVITE_EXPIRY_MINUTES`    | Game invite expiration time           | `30`                       |
| `PRESENCE_TIMEOUT`         | Presence heartbeat timeout (seconds)  | `60`                       |

### Application Properties

```yaml
server.port: ${SERVER_PORT:8080}
spring.data.mongodb.uri: ${MONGODB_URI:mongodb://localhost:27017/social}
spring.data.redis.host: ${REDIS_HOST:localhost}
spring.data.redis.port: ${REDIS_PORT:6379}
social.invite.expiry-minutes: ${INVITE_EXPIRY_MINUTES:30}
```

## Testing

Comprehensive test coverage:

- **FriendNotificationServiceTest**: Notification logic tests
- **PresenceServiceTest**: Presence tracking tests
- **SocialServiceTest**: Friend management tests

Run tests with:
```bash
./mvnw test -pl social
```

## Friend Request Flow

```
1. User A sends request to User B
   ↓
2. WebSocket notification to User B
   ↓
3. User B accepts request
   ↓
4. Both users added to each other's friend lists
   ↓
5. WebSocket notification to User A
   ↓
6. Presence tracking begins
```

## Game Invite Flow

```
1. User A in lobby, invites friend B
   ↓
2. Invite saved with lobby details
   ↓
3. WebSocket notification to User B
   ↓
4. User B accepts invite
   ↓
5. Auto-redirect to join lobby
   ↓
6. Invite marked as used
```

## Dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-websocket`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-data-redis`
- `common` (shared module)
