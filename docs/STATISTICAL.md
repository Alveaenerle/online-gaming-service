# Statistical Service Documentation

## Overview

The **Statistical Service** is a microservice responsible for tracking and managing player game statistics across all games in the Online Gaming Service platform. It listens for game result events via RabbitMQ and provides REST API endpoints for querying player statistics and rankings.

## Table of Contents

1. [Architecture](#architecture)
2. [API Endpoints](#api-endpoints)
3. [Data Models](#data-models)
4. [DTOs](#dtos)
5. [Messaging](#messaging)
6. [Configuration](#configuration)
7. [Dependencies](#dependencies)

---

## Architecture

### Package Structure

```
com.online_games_service.statistical/
├── StatisticalApplication.java      # Main Spring Boot application
├── config/
│   ├── FilterConfig.java            # Session filter for protected endpoints
│   └── RabbitMQConfig.java          # RabbitMQ queue/exchange configuration
├── controller/
│   └── StatisticsController.java    # REST API endpoints
├── dto/
│   ├── PlayerStatisticsDto.java     # Single game type stats response
│   ├── PlayerAllStatisticsDto.java  # All game types stats response
│   └── RankingsDto.java             # Rankings response
├── messaging/
│   └── GameResultListener.java      # RabbitMQ game result consumer
├── model/
│   └── PlayerStatistics.java        # MongoDB document entity
├── repository/
│   └── PlayerStatisticsRepository.java  # MongoDB repository
└── service/
    └── StatisticsService.java       # Business logic layer
```

### Component Interaction

```
┌─────────────────────────────────────────────────────────────┐
│                      RabbitMQ                               │
│                  (game.events exchange)                      │
└────────────────────────┬────────────────────────────────────┘
                         │ *.game.result
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   GameResultListener                         │
│              (statistical.game-result.queue)                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   StatisticsService                          │
│         (Record results, query stats, rankings)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                PlayerStatisticsRepository                    │
│                     (MongoDB)                                │
└─────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### Base Path: `/api/statistical`

### Current User Statistics

| Method | Endpoint           | Auth | Description                                |
|--------|-------------------|------|--------------------------------------------|
| GET    | `/me`             | ✅   | Get all statistics for current user        |
| GET    | `/me/{gameType}`  | ✅   | Get statistics for specific game type      |

### Player Statistics (Public)

| Method | Endpoint                    | Auth | Description                                |
|--------|----------------------------|------|--------------------------------------------|
| GET    | `/player/{playerId}`       | ❌   | Get all statistics for any player          |
| GET    | `/player/{playerId}/{gameType}` | ❌ | Get player stats for specific game type |

### Rankings

| Method | Endpoint               | Auth | Description                                 |
|--------|------------------------|------|---------------------------------------------|
| GET    | `/rankings/{gameType}` | ❌   | Get top players for a game type             |

#### Query Parameters for Rankings

| Parameter | Type   | Default | Max | Description                |
|-----------|--------|---------|-----|----------------------------|
| `limit`   | int    | 30      | 100 | Number of players to return |

### Health Check

| Method | Endpoint  | Description         |
|--------|-----------|---------------------|
| GET    | `/health` | Service health check |

---

## Data Models

### PlayerStatistics

MongoDB document stored in `player_statistics` collection.

```java
@Document(collection = "player_statistics")
public class PlayerStatistics {
    @Id
    private String id;
    
    @Indexed
    private String playerId;       // User account ID
    
    private String username;       // Display name
    
    @Indexed
    private String gameType;       // "MAKAO", "LUDO"
    
    @Indexed
    private int gamesPlayed;       // Total games played
    
    @Indexed
    private int gamesWon;          // Total games won (1st place)
}
```

#### Indexes

- `playerId` - Fast lookup by player
- `gameType` - Fast filtering by game
- `gamesPlayed` - Sorting for rankings
- `gamesWon` - Sorting for rankings

---

## DTOs

### PlayerStatisticsDto

Response for single game type statistics.

```json
{
  "playerId": "user123",
  "username": "JohnDoe",
  "gameType": "MAKAO",
  "gamesPlayed": 50,
  "gamesWon": 20,
  "winRatio": 40.0
}
```

### PlayerAllStatisticsDto

Response for all game types statistics.

```json
{
  "playerId": "user123",
  "statistics": [
    {
      "playerId": "user123",
      "username": "JohnDoe",
      "gameType": "MAKAO",
      "gamesPlayed": 50,
      "gamesWon": 20,
      "winRatio": 40.0
    },
    {
      "playerId": "user123",
      "username": "JohnDoe",
      "gameType": "LUDO",
      "gamesPlayed": 30,
      "gamesWon": 10,
      "winRatio": 33.33
    }
  ]
}
```

### RankingsDto

Response for game rankings.

```json
{
  "gameType": "MAKAO",
  "topByGamesPlayed": [
    { "playerId": "user1", "username": "TopPlayer", "gamesPlayed": 100, "gamesWon": 45, "winRatio": 45.0 }
  ],
  "topByGamesWon": [
    { "playerId": "user2", "username": "WinMaster", "gamesPlayed": 80, "gamesWon": 50, "winRatio": 62.5 }
  ]
}
```

---

## Messaging

### RabbitMQ Configuration

| Property                       | Value                          |
|--------------------------------|--------------------------------|
| Exchange                       | `game.events`                  |
| Queue                          | `statistical.game-result.queue`|
| Routing Key Pattern            | `*.game.result`                |

### GameResultMessage

The service listens for `GameResultMessage` from game services (Makao, Ludo):

```java
public record GameResultMessage(
    String roomId,
    String gameType,           // "MAKAO" or "LUDO"
    Map<String, String> participants,  // playerId -> username
    String winnerId            // ID of the winner (1st place)
) {}
```

### Event Flow

```
Game Service (Makao/Ludo)
         │
         │ Publishes: GameResultMessage
         │ Routing Key: makao.game.result / ludo.game.result
         ▼
    RabbitMQ Exchange (game.events)
         │
         │ Routes to: statistical.game-result.queue
         ▼
    GameResultListener
         │
         │ Updates player statistics
         ▼
    MongoDB (player_statistics)
```

### Bot Filtering

The service automatically excludes bots from statistics tracking. A player is considered a bot if their ID:
- Contains the word "bot" (case-insensitive)
- Starts with "bot_"

---

## Configuration

### Environment Variables

| Variable                             | Description                           | Default                    |
|--------------------------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`                        | Application port                      | `8080`                     |
| `SPRING_DATA_MONGODB_HOST`           | MongoDB server host                   | `localhost`                |
| `SPRING_DATA_MONGODB_PORT`           | MongoDB server port                   | `27017`                    |
| `SPRING_DATA_MONGODB_DATABASE`       | MongoDB database name                 | `statistical_db`           |
| `SPRING_DATA_MONGODB_USERNAME`       | MongoDB username                      | `root`                     |
| `SPRING_DATA_MONGODB_PASSWORD`       | MongoDB password                      | `rootpassword`             |
| `SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE` | MongoDB auth database       | `admin`                    |
| `SPRING_DATA_MONGODB_AUTO_INDEX_CREATION` | Auto-create indexes             | `true`                     |
| `REDIS_HOST`                         | Redis server host                     | `localhost`                |
| `REDIS_PORT`                         | Redis server port                     | `6379`                     |
| `REDIS_PASSWORD`                     | Redis password                        | `` (empty)                 |
| `RABBITMQ_HOST`                      | RabbitMQ server host                  | `rabbitmq`                 |
| `RABBITMQ_PORT`                      | RabbitMQ server port                  | `5672`                     |
| `SPRING_RABBITMQ_USERNAME`           | RabbitMQ username                     | `guest`                    |
| `SPRING_RABBITMQ_PASSWORD`           | RabbitMQ password                     | `guest`                    |

### Application Properties

```properties
# Application
spring.application.name=statistical

# MongoDB Configuration
spring.data.mongodb.host=${SPRING_DATA_MONGODB_HOST:localhost}
spring.data.mongodb.port=${SPRING_DATA_MONGODB_PORT:27017}
spring.data.mongodb.database=${SPRING_DATA_MONGODB_DATABASE:statistical_db}
spring.data.mongodb.username=${SPRING_DATA_MONGODB_USERNAME:root}
spring.data.mongodb.password=${SPRING_DATA_MONGODB_PASSWORD:rootpassword}
spring.data.mongodb.authentication-database=${SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE:admin}
spring.data.mongodb.auto-index-creation=${SPRING_DATA_MONGODB_AUTO_INDEX_CREATION:true}

# Redis Configuration
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}

# RabbitMQ Configuration
spring.rabbitmq.host=${RABBITMQ_HOST:rabbitmq}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${SPRING_RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${SPRING_RABBITMQ_PASSWORD:guest}

# AMQP Queue Configuration
statistical.amqp.exchange=game.events
statistical.amqp.queue.game-result=statistical.game-result.queue
statistical.amqp.routing.game-result=*.game.result
```

---

## Dependencies

### Maven Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- Internal Modules -->
    <dependency>
        <groupId>com.online_games_service</groupId>
        <artifactId>common</artifactId>
    </dependency>
    <dependency>
        <groupId>com.online_games_service</groupId>
        <artifactId>common-test-support</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testng</groupId>
        <artifactId>testng</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### External Service Dependencies

| Service   | Purpose                                    |
|-----------|--------------------------------------------|
| MongoDB   | Persistent storage for player statistics   |
| Redis     | Session validation (via common module)     |
| RabbitMQ  | Receiving game result events               |

---

## Usage Examples

### Get Current User's All Statistics

```bash
curl -X GET "http://localhost:8080/api/statistical/me" \
  -H "Cookie: ogs_session=<session_id>"
```

### Get Current User's Makao Statistics

```bash
curl -X GET "http://localhost:8080/api/statistical/me/MAKAO" \
  -H "Cookie: ogs_session=<session_id>"
```

### Get Any Player's Statistics

```bash
curl -X GET "http://localhost:8080/api/statistical/player/user123"
```

### Get Makao Rankings (Top 50)

```bash
curl -X GET "http://localhost:8080/api/statistical/rankings/MAKAO?limit=50"
```

---

## Error Handling

| HTTP Status | Scenario                                |
|-------------|-----------------------------------------|
| 200         | Successful request                      |
| 401         | User not authenticated (for /me endpoints) |
| 404         | Player or statistics not found          |
| 500         | Internal server error                   |

---

## Testing

### Running Tests

```bash
cd backend
./mvnw test -pl statistical
```

### Test Coverage

The service includes tests for:
- `StatisticsService` - Business logic
- `GameResultListener` - Message handling
- Bot filtering logic
- Edge cases (null values, empty participants)
