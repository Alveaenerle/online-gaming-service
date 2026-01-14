# Ludo Game Service Documentation

## Overview

The **Ludo Service** is a Spring Boot microservice that implements the classic Ludo board game with real-time multiplayer support, bot players, and turn timeout handling.

## Technology Stack

- **Framework**: Spring Boot
- **Real-time Communication**: WebSocket with STOMP protocol
- **State Management**: Redis for game state persistence
- **Message Queue**: RabbitMQ for inter-service communication
- **Database**: MongoDB for game history/results
- **Testing**: TestNG

## Architecture

```
ludo/
├── config/           # WebSocket, Redis, AMQP configuration
├── controller/       # REST API and WebSocket handlers
├── dto/              # Game state messages
├── enums/            # PlayerColor, game constants
├── exception/        # Game-specific exceptions
├── model/            # LudoGame, LudoPlayer, LudoPawn
├── repository/       # Redis and MongoDB repositories
└── service/          # Game logic (LudoService)
```

## Game Rules Implementation

### Board Layout
- **40 position circular board**
- **4 players** with colored pawns (RED, GREEN, YELLOW, BLUE)
- **4 pawns per player**
- Each player has a unique **start position** and **home lane**

### Movement Rules
- Roll a **6** to move a pawn from base to start position
- Move pawns clockwise by the dice roll value
- Roll a **6** grants an additional roll
- **Capture**: Landing on an opponent's pawn sends it back to base
- **Home**: Pawns reaching home are safe and scored

### Win Condition
- First player to get all 4 pawns into home wins
- Other players are ranked by pawns in home

## API Endpoints

### REST Endpoints

| Method | Endpoint              | Description                    |
|--------|-----------------------|--------------------------------|
| GET    | `/{gameId}`           | Get current game state         |
| POST   | `/{gameId}/roll`      | Roll the dice                  |
| POST   | `/{gameId}/move`      | Move a pawn                    |

### WebSocket Topics

| Topic                          | Description                      |
|--------------------------------|----------------------------------|
| `/topic/ludo/{roomId}/state`   | Game state updates               |
| `/topic/ludo/{roomId}/finish`  | Game completion notification     |

## Game State Model

### LudoGame
```json
{
  "roomId": "string",
  "gameId": "LUDO-uuid",
  "status": "PLAYING",
  "hostUserId": "string",
  "maxPlayers": 4,
  "players": [...],
  "currentPlayerColor": "RED",
  "activePlayerId": "string",
  "lastDiceRoll": 4,
  "diceRolled": true,
  "waitingForMove": true,
  "rollsLeft": 1,
  "winnerId": null,
  "placement": {}
}
```

### LudoPlayer
```json
{
  "userId": "string",
  "color": "RED",
  "pawns": [...],
  "isBot": false
}
```

### LudoPawn
```json
{
  "id": 0,
  "position": 5,
  "color": "RED",
  "stepsMoved": 5,
  "inBase": false,
  "inHome": false
}
```

## Features

### Turn Management
- **3 rolls** when all pawns in base (to get a 6)
- **1 roll** when at least one pawn is active
- Rolling 6 grants extra roll

### Bot Players
- Automatic bot replacement for disconnected/timed-out players
- Bot naming convention: `bot-{counter}`
- Smart pawn selection strategy:
  1. Prioritize leaving base on 6
  2. Move any available pawn

### Turn Timeout
- Configurable timeout (default: 60 seconds)
- Player replaced by bot after timeout
- Game continues automatically

### Collision Handling
- Landing on opponent pawn sends it to base
- Safe positions prevent captures
- Cannot land on own pawns

## Inter-Service Communication

### RabbitMQ Events

**Published Events:**
- `ludo.finish` - Game completion with results

**Consumed Events:**
- Game start trigger from Menu Service

## Error Handling

| Exception            | HTTP Status | Description                    |
|----------------------|-------------|--------------------------------|
| GameLogicException   | 400         | Invalid game action            |
| InvalidMoveException | 400         | Invalid pawn movement          |
| IllegalStateException| 400         | Game state violation           |

## Configuration

### Environment Variables

| Variable                   | Description                           | Default                    |
|----------------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`              | Application port                      | `8080`                     |
| `REDIS_HOST`               | Redis server host                     | `localhost`                |
| `REDIS_PORT`               | Redis server port                     | `6379`                     |
| `MONGODB_URI`              | MongoDB connection string             | `mongodb://localhost:27017/ludo` |
| `RABBITMQ_HOST`            | RabbitMQ server host                  | `localhost`                |
| `RABBITMQ_PORT`            | RabbitMQ server port                  | `5672`                     |
| `RABBITMQ_USERNAME`        | RabbitMQ username                     | `guest`                    |
| `RABBITMQ_PASSWORD`        | RabbitMQ password                     | `guest`                    |
| `CORS_ORIGINS`             | Allowed CORS origins                  | `http://localhost:5173`    |
| `LUDO_TURN_TIMEOUT`        | Turn timeout in seconds               | `60`                       |

### Application Properties

```yaml
server.port: ${SERVER_PORT:8080}
ludo.turn-timeout-seconds: ${LUDO_TURN_TIMEOUT:60}
ludo.amqp.exchange: game.events
ludo.amqp.routing.finish: ludo.finish
ludo.http.cors.allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
```

## Testing

The service includes comprehensive tests:

- **LudoServiceTest**: Game logic unit tests
- **LudoServiceIntegrationTest**: Redis integration tests
- **LudoModelTest**: Model validation tests

Run tests with:
```bash
./mvnw test -pl ludo
```

## Game Flow

```
1. Game Created (via Menu Service)
   ↓
2. First Player's Turn
   ↓
3. Roll Dice → Move Pawn (or pass if no moves)
   ↓
4. Check Win Condition
   ↓
5. Next Player's Turn (or End Game)
   ↓
6. Broadcast State Update
```

## Dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-websocket`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-amqp`
- `common` (shared module)
