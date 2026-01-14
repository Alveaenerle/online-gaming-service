# Menu Service Documentation

## Overview

The **Menu Service** is a Spring Boot microservice that manages game lobbies, room creation, player matchmaking, and lobby chat functionality. It serves as the central hub for game session management.

## Technology Stack

- **Framework**: Spring Boot
- **Real-time Communication**: WebSocket with STOMP protocol
- **State Management**: Redis for room state
- **Message Queue**: RabbitMQ for game service communication
- **Database**: MongoDB for persistent storage
- **Testing**: TestNG

## Architecture

```
menu/
├── config/           # WebSocket, Redis, AMQP, CORS configuration
├── controller/       # REST API and WebSocket handlers
├── dto/              # Request/Response DTOs
│   └── chat/         # Chat-specific DTOs
├── messaging/        # RabbitMQ publishers and listeners
├── model/            # GameRoom, PlayerState, ChatMessage
├── repository/       # MongoDB repositories
└── service/          # Business logic
    └── chat/         # Chat service with rate limiting
```

## API Endpoints

### Room Management

| Method | Endpoint        | Description                          |
|--------|-----------------|--------------------------------------|
| POST   | `/create`       | Create a new game room               |
| POST   | `/join`         | Join existing room (by code/random)  |
| POST   | `/leave`        | Leave current room                   |
| POST   | `/start`        | Start the game (host only)           |
| GET    | `/room-info`    | Get current room information         |
| POST   | `/ready`        | Toggle ready status                  |
| POST   | `/update-avatar`| Update player avatar                 |
| POST   | `/kick-player`  | Kick a player (host only)            |
| GET    | `/waiting`      | List public waiting rooms            |

### WebSocket Endpoints

| Topic                              | Description                    |
|------------------------------------|--------------------------------|
| `/topic/room/{roomId}`             | Room state updates             |
| `/topic/room/{roomId}/kicked/{id}` | Kick notifications             |
| `/topic/room/{roomId}/chat`        | Chat messages                  |
| `/topic/room/{roomId}/typing`      | Typing indicators              |

## Room Model

### GameRoom
```json
{
  "id": "uuid-string",
  "name": "My Game Room",
  "gameType": "MAKAO",
  "hostUserId": "string",
  "hostUsername": "HostPlayer",
  "players": {
    "userId1": {...},
    "userId2": {...}
  },
  "maxPlayers": 4,
  "isPrivate": true,
  "accessCode": "ABC123",
  "status": "WAITING",
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:05:00"
}
```

### PlayerState
```json
{
  "username": "PlayerName",
  "isReady": true,
  "avatarId": "avatar_1.png",
  "joinedAt": "2024-01-01T12:00:00"
}
```

## Features

### Room Creation
- Support for multiple game types (MAKAO, LUDO)
- Configurable max players (game-specific limits)
- Private rooms with access codes
- Auto-generated room names

### Quick Match
- Join random public room for selected game type
- Automatic room selection from waiting pool
- Falls back to room creation if none available

### Player Management
- Ready/unready toggle
- Avatar selection (6 preset avatars)
- Host can kick players
- Automatic host reassignment on host leave

### Room State Broadcasting
- Real-time updates via WebSocket
- All players receive state changes
- Kicked player notifications

### Lobby Chat
- Real-time messaging in lobby
- Typing indicators
- Rate limiting (spam protection)
- Profanity filter
- Message history

## Chat System

### Message Structure
```json
{
  "id": "uuid",
  "lobbyId": "room-id",
  "userId": "sender-id",
  "username": "SenderName",
  "avatar": "/avatars/avatar_1.png",
  "content": "Hello everyone!",
  "timestamp": "2024-01-01T12:00:00",
  "isFiltered": false
}
```

### Chat Features
- **Rate Limiting**: Prevents message spam
- **Profanity Filter**: Replaces inappropriate words
- **History**: Last 50 messages stored
- **Typing Indicators**: Shows who is typing

## Inter-Service Communication

### RabbitMQ Events

**Published Events:**
- `game.start.makao` - Trigger Makao game initialization
- `game.start.ludo` - Trigger Ludo game initialization

**Consumed Events:**
- `player.leave.queue` - Handle player disconnections from games

### Game Start Message
```json
{
  "roomId": "string",
  "gameType": "MAKAO",
  "playerIds": ["p1", "p2"],
  "hostUserId": "p1",
  "usernames": {"p1": "Player1", "p2": "Player2"},
  "avatars": {"p1": "avatar_1.png", "p2": "avatar_2.png"}
}
```

## Configuration

### Environment Variables

| Variable                | Description                           | Default                    |
|-------------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`           | Application port                      | `8080`                     |
| `MONGODB_URI`           | MongoDB connection string             | `mongodb://localhost:27017/menu` |
| `REDIS_HOST`            | Redis server host                     | `localhost`                |
| `REDIS_PORT`            | Redis server port                     | `6379`                     |
| `RABBITMQ_HOST`         | RabbitMQ server host                  | `localhost`                |
| `RABBITMQ_PORT`         | RabbitMQ server port                  | `5672`                     |
| `RABBITMQ_USERNAME`     | RabbitMQ username                     | `guest`                    |
| `RABBITMQ_PASSWORD`     | RabbitMQ password                     | `guest`                    |
| `CORS_ORIGINS`          | Allowed CORS origins                  | `http://localhost:5173`    |
| `CHAT_RATE_LIMIT`       | Max messages per minute               | `20`                       |

### Game Limits
```yaml
game-limits:
  makao:
    min-players: 2
    max-players: 8
  ludo:
    min-players: 2
    max-players: 4
```

### Redis Keys
| Key Pattern                    | Description                    |
|--------------------------------|--------------------------------|
| `game:room:{id}`               | Room object storage            |
| `game:waiting:{gameType}`      | Public rooms waiting pool      |
| `game:code:{code}`             | Access code to room ID mapping |
| `game:user-room:id:{userId}`   | User to room mapping           |

## Error Handling

| Scenario                    | HTTP Status | Response                     |
|-----------------------------|-------------|------------------------------|
| Room not found              | 404         | "Room not found"             |
| Room is full                | 400         | "Room is full"               |
| Not room host               | 403         | "Only host can start"        |
| Already in a room           | 400         | "Already in a room"          |
| Not enough players          | 400         | "Need more players to start" |

## Testing

Comprehensive test coverage:

- **GameRoomServiceTest**: Room management logic
- **ChatServiceTest**: Chat functionality tests
- **Integration tests**: Full flow tests

Run tests with:
```bash
./mvnw test -pl menu
```

## Room Lifecycle

```
1. Room Created (Host joins automatically)
   ↓
2. Players Join (via code or quick match)
   ↓
3. Players Ready Up
   ↓
4. Host Starts Game (all players ready)
   ↓
5. Game Start Message Published
   ↓
6. Room Status → PLAYING
   ↓
7. Game Finish → Room Cleanup
```

## Dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-websocket`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-amqp`
- `common` (shared module)
