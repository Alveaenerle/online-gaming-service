# Makao Game Service Documentation

## Overview

The **Makao Service** is a Spring Boot microservice implementing the Polish card game "Makao" (similar to Crazy Eights/UNO) with real-time multiplayer support, special card effects, and bot players.

## Technology Stack

- **Framework**: Spring Boot
- **Real-time Communication**: WebSocket with STOMP protocol
- **State Management**: Redis for game state persistence
- **Message Queue**: RabbitMQ for inter-service communication
- **Testing**: TestNG

## Architecture

```
makao/
├── config/           # WebSocket, Redis, AMQP configuration
├── controller/       # REST API and WebSocket handlers
├── dto/              # Game state messages, requests
├── enums/            # CardSuit, CardRank
├── exception/        # Game-specific exceptions
├── model/            # MakaoGame, MakaoDeck, Card
├── repository/       # Redis repository
└── service/          # Game logic (MakaoGameService)
```

## Game Rules Implementation

### Card Deck
- Standard 52-card deck (2 decks for 5+ players)
- All cards from 2 through Ace in 4 suits

### Basic Play
- Match the top card by **suit** or **rank**
- Draw a card if unable to play
- Play drawn card if valid, or keep it

### Special Cards

| Card  | Effect                                           |
|-------|--------------------------------------------------|
| 2     | Next player draws 2 cards (stacks with 2s/3s)    |
| 3     | Next player draws 3 cards (stacks with 2s/3s)    |
| 4     | Next player skips their turn (stacks)            |
| Jack  | Request any rank (except 2,3,4,J,A,K)            |
| Ace   | Request any suit                                 |
| King ♥♠ | Next player draws 5 cards (combat Kings)       |

### MAKAO Rule
- Announce "MAKAO!" when down to 1 card
- Visual notification to all players

### Win Condition
- First player to empty their hand wins
- Points calculated based on remaining cards

## API Endpoints

### REST Endpoints

| Method | Endpoint          | Description                    |
|--------|-------------------|--------------------------------|
| POST   | `/play-card`      | Play a card from hand          |
| POST   | `/draw-card`      | Draw a card from deck          |
| POST   | `/play-drawn`     | Play the drawn card            |
| POST   | `/skip-drawn`     | Keep drawn card, end turn      |
| POST   | `/accept-effect`  | Accept special card penalty    |
| POST   | `/request-state`  | Request current game state     |
| POST   | `/leave-game`     | Leave game (replaced by bot)   |

### WebSocket Topics

| Topic                            | Description                      |
|----------------------------------|----------------------------------|
| `/topic/makao/{roomId}/state`    | Game state updates               |
| `/topic/makao/{roomId}/finish`   | Game completion notification     |

## Game State Model

### MakaoGame
```json
{
  "roomId": "string",
  "gameId": "MAKAO-uuid",
  "status": "PLAYING",
  "hostUserId": "string",
  "maxPlayers": 4,
  "playersOrderIds": ["p1", "p2", ...],
  "activePlayerId": "string",
  "reverseMovement": false,
  "specialEffectActive": true,
  "activePlayerPlayableCards": [...],
  "drawnCard": null,
  "playersSkipTurns": {},
  "playersHands": {},
  "pendingDrawCount": 2,
  "demandedRank": "SEVEN",
  "demandedSuit": "HEARTS",
  "makaoPlayerId": null
}
```

### Card
```json
{
  "suit": "HEARTS",
  "rank": "SEVEN"
}
```

### GameStateMessage (WebSocket)
```json
{
  "gameId": "string",
  "status": "PLAYING",
  "currentCard": {...},
  "activePlayerId": "string",
  "yourHand": [...],
  "playableCards": [...],
  "opponentCardCounts": {"p2": 5},
  "pendingDrawCount": 0,
  "demandedRank": null,
  "demandedSuit": null,
  "effectNotification": "Next player must draw 2 cards!",
  "moveHistory": [...]
}
```

## Features

### Playable Card Detection
- Automatic detection of valid plays
- Highlights playable cards for active player
- Considers special effects and demands

### Effect Stacking
- 2s and 3s stack draw penalties
- 4s stack skip turn penalties
- Combat Kings add 5 cards to pending draws

### Bot Players
- Automatic bot replacement for disconnected players
- Smart play strategy:
  1. Play matching special cards to counter effects
  2. Play any valid card
  3. Draw if no valid plays

### Turn Timeout
- Configurable timeout with automatic bot takeover
- Maintains game flow for remaining players

### Move History
- Last 20 moves logged
- Displayed in game UI
- Includes card plays and effects

## Inter-Service Communication

### RabbitMQ Events

**Published Events:**
- `makao.finish` - Game completion with results

**Consumed Events:**
- Game start trigger from Menu Service

## Error Handling

| Exception            | HTTP Status | Description                    |
|----------------------|-------------|--------------------------------|
| IllegalStateException| 400         | Invalid game state             |
| IllegalArgumentException | 400     | Invalid request parameters     |

## Configuration

### Environment Variables

| Variable                   | Description                           | Default                    |
|----------------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`              | Application port                      | `8080`                     |
| `REDIS_HOST`               | Redis server host                     | `localhost`                |
| `REDIS_PORT`               | Redis server port                     | `6379`                     |
| `RABBITMQ_HOST`            | RabbitMQ server host                  | `localhost`                |
| `RABBITMQ_PORT`            | RabbitMQ server port                  | `5672`                     |
| `RABBITMQ_USERNAME`        | RabbitMQ username                     | `guest`                    |
| `RABBITMQ_PASSWORD`        | RabbitMQ password                     | `guest`                    |
| `CORS_ORIGINS`             | Allowed CORS origins                  | `http://localhost:5173`    |
| `MAKAO_TURN_TIMEOUT`       | Turn timeout in seconds               | `60`                       |

### Application Properties

```yaml
server.port: ${SERVER_PORT:8080}
makao.turn-timeout-seconds: ${MAKAO_TURN_TIMEOUT:60}
makao.amqp.exchange: game.events
makao.amqp.routing.finish: makao.finish
makao.http.cors.allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
```

## Testing

Comprehensive test coverage:

- **MakaoGameServiceTest**: Game logic unit tests
- **MakaoGameTest**: Model validation tests
- **Integration tests**: Full game flow tests

Run tests with:
```bash
./mvnw test -pl makao
```

## Game Flow

```
1. Game Initialized (5 cards dealt per player)
   ↓
2. First non-special card on discard pile
   ↓
3. Active Player's Turn
   ↓
4. Play Card / Draw Card / Accept Effect
   ↓
5. Apply Card Effects (if any)
   ↓
6. Check MAKAO / Win Condition
   ↓
7. Next Player's Turn
   ↓
8. Broadcast State Update
```

## Dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-websocket`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-amqp`
- `common` (shared module)
