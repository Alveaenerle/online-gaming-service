# Backend Common Modules Documentation

## Overview

The backend contains several shared modules that provide common functionality across all game services. This document covers the **common**, **common-test-support**, and **coverage-report** modules.

---

## Common Module

The `common` module provides shared code, models, and utilities used by all backend services.

### Location
```
backend/common/
├── src/main/java/com/online_games_service/common/
│   ├── config/         # Shared configurations
│   ├── enums/          # Shared enumerations
│   ├── filter/         # HTTP filters
│   ├── messaging/      # AMQP message models
│   └── model/          # Shared domain models
└── pom.xml
```

### Shared Enumerations

#### GameType
```java
public enum GameType {
    MAKAO,
    LUDO
}
```

#### RoomStatus
```java
public enum RoomStatus {
    WAITING,    // Room is waiting for players
    PLAYING,    // Game in progress
    FINISHED    // Game completed
}
```

#### CardSuit
```java
public enum CardSuit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}
```

#### CardRank
```java
public enum CardRank {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, ACE
}
```

### Shared Models

#### Card
Base card model used by card games (Makao):
```java
public class Card {
    private CardSuit suit;
    private CardRank rank;
}
```

#### Deck
Base deck implementation with shuffle and draw operations:
```java
public class Deck {
    private List<Card> cards;
    
    public Card draw();
    public void shuffle();
    public void addCard(Card card);
    public boolean isEmpty();
}
```

### HTTP Filters

#### Session Filter
Validates user sessions from cookies and extracts user information:
- Extracts session token from HTTP-only cookie
- Validates session with Redis
- Adds `userId` and `username` to request attributes
- Allows public endpoints without authentication

### Messaging Models

#### GameStartMessage
Published when a game starts:
```java
public class GameStartMessage {
    private String roomId;
    private GameType gameType;
    private List<String> playerIds;
    private String hostUserId;
    private Map<String, String> usernames;
    private Map<String, String> avatars;
}
```

#### GameFinishMessage
Published when a game ends:
```java
public class GameFinishMessage {
    private String roomId;
    private String gameType;
    private String winnerId;
    private Map<String, Integer> placement;
}
```

#### PlayerLeaveMessage
Published when a player leaves during a game:
```java
public class PlayerLeaveMessage {
    private String roomId;
    private String playerId;
}
```

### Configuration

#### CORS Configuration
Shared CORS settings for all services:
```java
@Configuration
public class CorsConfig {
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
}
```

---

## Common Test Support Module

The `common-test-support` module provides shared testing utilities and base classes.

### Location
```
backend/common-test-support/
├── src/main/java/com/online_games_service/common_test_support/
│   └── ... testing utilities
└── pom.xml
```

### Features

- **Base Test Classes**: Common setup for integration tests
- **Test Containers**: Docker container management for tests
- **Mock Utilities**: Shared mock configurations
- **Test Data Builders**: Factory methods for test objects

### Usage
Add as test dependency in service modules:
```xml
<dependency>
    <groupId>com.online_games_service</groupId>
    <artifactId>common-test-support</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Coverage Report Module

The `coverage-report` module aggregates test coverage from all services.

### Location
```
backend/coverage-report/
├── pom.xml
└── target/site/
    └── jacoco-aggregate/    # Aggregated coverage report
```

### Purpose
- Aggregates JaCoCo coverage from all modules
- Generates unified coverage report
- Used in CI/CD for coverage thresholds

### Generating Report
```bash
cd backend
./mvnw verify -pl coverage-report
```

Report location: `coverage-report/target/site/jacoco-aggregate/index.html`

---

## Backend Tests Module

The `tests` module contains end-to-end and integration tests.

### Location
```
backend/tests/
├── src/test/java/
│   └── ... e2e tests
└── pom.xml
```

### Features
- Cross-service integration tests
- Full game flow tests
- API contract tests

---

## Parent POM

The `backend/pom.xml` serves as the parent POM for all modules.

### Module Declaration
```xml
<modules>
    <module>common</module>
    <module>common-test-support</module>
    <module>authorization</module>
    <module>menu</module>
    <module>social</module>
    <module>makao</module>
    <module>ludo</module>
    <module>coverage-report</module>
    <module>tests</module>
</modules>
```

### Shared Dependencies
- Spring Boot Parent
- Lombok
- TestNG
- JaCoCo

### Building All Modules
```bash
cd backend
./mvnw clean install
```

### Running All Tests
```bash
cd backend
./mvnw test
```

---

## Docker Configuration

### Dockerfile
The shared `backend/Dockerfile` builds any service module:
```dockerfile
ARG MODULE_NAME
# Multi-stage build for production
```

### Build Arguments
- `MODULE_NAME`: Which service to build (authorization, menu, etc.)

### Example Build
```bash
docker build --build-arg MODULE_NAME=authorization -t auth-service ./backend
```

---

## Maven Wrapper

All backend modules include Maven Wrapper for consistent builds:
- `mvnw` / `mvnw.cmd`: Platform-specific scripts
- `.mvn/wrapper/`: Maven wrapper configuration

Usage:
```bash
./mvnw clean package -DskipTests
```
