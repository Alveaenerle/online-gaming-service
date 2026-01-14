# Online Gaming Service - Complete Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Getting Started](#getting-started)
5. [Services Overview](#services-overview)
6. [Frontend Application](#frontend-application)
7. [Infrastructure](#infrastructure)
8. [Development Guide](#development-guide)
9. [Deployment](#deployment)
10. [API Reference](#api-reference)

---

## Project Overview

The **Online Gaming Service** is a modern, real-time multiplayer gaming platform featuring classic card and board games. The platform provides a seamless gaming experience with social features, real-time updates, and support for both registered users and guests.

### Key Features

- 🎮 **Multiple Games**: Makao (card game) and Ludo (board game)
- 👥 **Multiplayer**: Real-time gameplay with 2-8 players
- 🤖 **Bot Players**: Automatic replacement for disconnected players
- 💬 **Social Features**: Friends system, game invitations, presence tracking
- 🚀 **Quick Match**: Instant matchmaking for public games
- 🔒 **Private Rooms**: Password-protected game sessions
- 💻 **Responsive UI**: Modern, mobile-friendly interface

---

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         NGINX                                │
│                   (Reverse Proxy + SSL)                      │
├─────────────────────────────────────────────────────────────┤
│                        FRONTEND                              │
│                    React + TypeScript                        │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / WebSocket
┌──────────────────────────┴──────────────────────────────────┐
│                      API GATEWAY                             │
│                        (Nginx)                               │
├────────┬────────┬────────┬────────┬────────┬────────────────┤
│  AUTH  │  MENU  │ SOCIAL │ MAKAO  │  LUDO  │  STATISTICAL   │
│ :8080  │ :8080  │ :8080  │ :8080  │ :8080  │     :8080      │
└───┬────┴───┬────┴───┬────┴───┬────┴───┬────┴───────┬────────┘
    │        │        │        │        │            │
    └────────┴────────┴────────┴────────┴────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                    DATA LAYER                                │
├──────────────────┬──────────────────┬───────────────────────┤
│     MongoDB      │      Redis       │     RabbitMQ          │
│   (Persistence)  │  (Cache/Session) │   (Message Queue)     │
└──────────────────┴──────────────────┴───────────────────────┘
```

### Microservices Communication

```
┌─────────────┐    Game Start    ┌─────────────┐    Game Result   ┌─────────────┐
│    MENU     │ ───────────────► │   MAKAO     │ ─────────────────►│ STATISTICAL │
│   Service   │                  │   Service   │                   │   Service   │
└─────────────┘                  └─────────────┘                   └─────────────┘
       │                                                                  ▲
       │         Game Start      ┌─────────────┐    Game Result          │
       └──────────────────────►  │    LUDO     │ ────────────────────────┘
                                 │   Service   │
                                 └─────────────┘
```

---

## Technology Stack

### Backend

| Component        | Technology                    |
|------------------|-------------------------------|
| Framework        | Spring Boot 3.x               |
| Language         | Java 17+                      |
| Build Tool       | Maven                         |
| Database         | MongoDB                       |
| Cache/Session    | Redis                         |
| Message Queue    | RabbitMQ                      |
| WebSocket        | STOMP over SockJS             |
| Testing          | TestNG, JaCoCo                |

### Frontend

| Component        | Technology                    |
|------------------|-------------------------------|
| Framework        | React 18                      |
| Language         | TypeScript                    |
| Build Tool       | Vite                          |
| Styling          | Tailwind CSS                  |
| Animations       | Framer Motion                 |
| WebSocket        | STOMP.js + SockJS             |
| Routing          | React Router v6               |

### Infrastructure

| Component        | Technology                    |
|------------------|-------------------------------|
| Containerization | Docker                        |
| Orchestration    | Docker Compose                |
| Reverse Proxy    | Nginx                         |
| CI/CD            | Jenkins                       |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Git

### Quick Start (Production)

```bash
# Clone the repository
git clone <repository-url>
cd online-gaming-service

# Start all services
docker compose -f docker-compose.prod.yml up --build
```

Access the application at: http://localhost

### Development Mode

```bash
# Start with hot-reload
docker compose -f docker-compose.dev.yml up --build
```

- Frontend (hot-reload): http://localhost:5173
- Nginx proxy: http://localhost

### Stopping Services

```bash
docker compose -f docker-compose.prod.yml down
```

---

## Services Overview

### Authorization Service
**Purpose**: User authentication and session management

**Key Features**:
- User registration with email/password
- Login with session cookies
- Guest access support
- Profile management (username, password)

**Port**: 8080  
**Documentation**: [AUTHORIZATION.md](./AUTHORIZATION.md)

---

### Menu Service
**Purpose**: Game lobby and room management

**Key Features**:
- Create public/private game rooms
- Quick match (random matchmaking)
- Player ready system
- Lobby chat with rate limiting
- Game start coordination

**Port**: 8080  
**Documentation**: [MENU.md](./MENU.md)

---

### Social Service
**Purpose**: Social features and friend management

**Key Features**:
- Friend requests and management
- Real-time presence tracking
- Game invitations
- WebSocket notifications

**Port**: 8080  
**Documentation**: [SOCIAL.md](./SOCIAL.md)

---

### Makao Service
**Purpose**: Makao card game logic

**Key Features**:
- Full Makao rules implementation
- Special card effects (2, 3, 4, Jack, Ace, Kings)
- Effect stacking
- Bot players
- Turn timeouts

**Port**: 8080  
**Documentation**: [MAKAO.md](./MAKAO.md)

---

### Ludo Service
**Purpose**: Ludo board game logic

**Key Features**:
- Classic Ludo rules
- 4-player support
- Dice mechanics with extra rolls
- Pawn capture system
- Bot players
- Turn timeouts

**Port**: 8080  
**Documentation**: [LUDO.md](./LUDO.md)

---

### Statistical Service
**Purpose**: Player statistics and rankings tracking

**Key Features**:
- Automatic game result tracking via RabbitMQ
- Per-game statistics (games played, won, win ratio)
- Player rankings by games played and won
- Bot filtering (bots excluded from statistics)
- Public and authenticated endpoints

**Port**: 8080  
**Documentation**: [STATISTICAL.md](./STATISTICAL.md)

---

## Frontend Application

The frontend is a React SPA with real-time WebSocket integration.

**Key Components**:
- Landing page with game selection
- Authentication forms (login, register)
- Game lobbies with chat
- Interactive game boards
- Social center (friends, invites)
- User dashboard

**Documentation**: [FRONTEND.md](./FRONTEND.md)

---

## Infrastructure

### Docker Services

| Service       | Image/Build              | Purpose                |
|---------------|--------------------------|------------------------|
| authorization | backend (auth module)    | Authentication         |
| menu          | backend (menu module)    | Lobby management       |
| social        | backend (social module)  | Social features        |
| makao         | backend (makao module)   | Makao game             |
| ludo          | backend (ludo module)    | Ludo game              |
| statistical   | backend (statistical module) | Player statistics   |
| frontend      | frontend build           | React application      |
| rabbitmq      | rabbitmq:3-management    | Message queue          |
| mongodb       | mongo:latest             | Database               |
| redis         | redis:alpine             | Cache/Sessions         |

### Network Flow

```
Client Request
      │
      ▼
┌─────────────┐
│    Nginx    │  (Port 80/443)
└──────┬──────┘
       │
       ├──► /api/auth/*  ──► Authorization Service
       ├──► /api/menu/*  ──► Menu Service
       ├──► /api/social/* ──► Social Service
       ├──► /api/makao/* ──► Makao Service
       ├──► /api/ludo/*  ──► Ludo Service
       ├──► /api/statistical/* ──► Statistical Service
       └──► /*           ──► Frontend Static Files
```

---

## Development Guide

### Building Backend

```bash
cd backend
./mvnw clean install
```

### Building Individual Service

```bash
./mvnw clean package -pl authorization -am
```

### Running Tests

```bash
# All tests
./mvnw test

# Specific service
./mvnw test -pl makao

# With coverage
./mvnw verify -pl coverage-report
```

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

### Code Quality

```bash
# Backend - run tests with coverage
./mvnw verify

# Frontend - linting
npm run lint
```

---

## Deployment

### Production Deployment

1. **Build images**:
```bash
docker compose -f docker-compose.prod.yml build
```

2. **Start services**:
```bash
docker compose -f docker-compose.prod.yml up -d
```

3. **View logs**:
```bash
docker compose -f docker-compose.prod.yml logs -f
```

### CI/CD Pipeline (Jenkins)

The project includes a Jenkinsfile for automated:
- Building all services
- Running tests
- Building Docker images
- Deployment to staging/production

---

## API Reference

### Authorization Service Endpoints

#### Public Endpoints (No Authentication Required)

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | /api/auth/register          | Register new user with username, email, password |
| POST   | /api/auth/login             | Authenticate user and create session cookie      |
| POST   | /api/auth/guest             | Create guest session for anonymous play          |
| POST   | /api/auth/logout            | Destroy session and clear authentication cookie  |

#### Protected Endpoints (Authentication Required)

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| GET    | /api/auth/me                | Get current authenticated user information       |
| PUT    | /api/auth/update-username   | Update user's display name                       |
| PUT    | /api/auth/update-password   | Change user's password (requires current pass)   |
| GET    | /api/auth/email             | Get user's email address                         |

---

### Menu Service Endpoints

#### Room Management

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | /api/menu/create            | Create new game room (public or private)         |
| POST   | /api/menu/join              | Join existing room by code or quick match        |
| POST   | /api/menu/start             | Start game (host only, all players must be ready)|
| POST   | /api/menu/leave             | Leave current room                               |
| GET    | /api/menu/room-info         | Get current room information                     |
| GET    | /api/menu/waiting           | List public waiting rooms for a game type        |

#### Player Actions

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | /api/menu/ready             | Toggle player ready status                       |
| POST   | /api/menu/update-avatar     | Update player's avatar in lobby                  |
| POST   | /api/menu/kick-player       | Kick a player from room (host only)              |

#### WebSocket Chat Endpoints

| Destination                      | Direction | Description                            |
|----------------------------------|-----------|----------------------------------------|
| /app/chat/{lobbyId}/send         | Client→Server | Send chat message                  |
| /app/chat/{lobbyId}/typing       | Client→Server | Send typing indicator              |
| /app/chat/{lobbyId}/history      | Client→Server | Request chat history               |
| /topic/room/{lobbyId}/chat       | Server→Client | Receive chat messages              |
| /topic/room/{lobbyId}/typing     | Server→Client | Receive typing indicators          |
| /user/queue/chat-history         | Server→Client | Receive chat history               |
| /user/queue/chat-errors          | Server→Client | Receive chat errors                |

---

### Makao Service Endpoints

#### Game Actions

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | /api/makao/play-card        | Play a card from hand                            |
| POST   | /api/makao/draw-card        | Draw a card from deck                            |
| POST   | /api/makao/play-drawn       | Play the drawn card (if playable)                |
| POST   | /api/makao/skip-drawn       | Keep drawn card and end turn                     |
| POST   | /api/makao/accept-effect    | Accept special card effect (draw cards/skip)     |
| POST   | /api/makao/request-state    | Request current game state via WebSocket         |
| POST   | /api/makao/leave-game       | Leave game (player replaced by bot)              |

#### WebSocket Topics

| Topic                            | Description                                      |
|----------------------------------|--------------------------------------------------|
| /topic/makao/{roomId}/state      | Real-time game state updates                     |
| /topic/makao/{roomId}/finish     | Game completion notification                     |

---

### Ludo Service Endpoints

#### Game Actions

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| GET    | /api/ludo/{gameId}          | Get current game state                           |
| POST   | /api/ludo/{gameId}/roll     | Roll the dice                                    |
| POST   | /api/ludo/{gameId}/move     | Move a pawn (query param: pawnIndex=0-3)         |

#### Move Request Parameters
- `pawnIndex` (query param): Index of pawn to move (0-3)

#### WebSocket Topics

| Topic                            | Description                                      |
|----------------------------------|--------------------------------------------------|
| /topic/ludo/{roomId}/state       | Real-time game state updates                     |
| /topic/ludo/{roomId}/finish      | Game completion notification                     |

---

### Social Service Endpoints

#### Friend Management

| Method | Endpoint                          | Description                                  |
|--------|-----------------------------------|----------------------------------------------|
| POST   | /api/social/friends/request       | Send friend request to user                  |
| POST   | /api/social/friends/accept/{id}   | Accept pending friend request                |
| POST   | /api/social/friends/reject/{id}   | Reject pending friend request                |
| DELETE | /api/social/friends/{friendId}    | Remove friend from friend list               |
| GET    | /api/social/friends               | Get list of all friends                      |
| GET    | /api/social/friends/requests      | Get pending friend requests received         |
| GET    | /api/social/friends/sent          | Get friend requests sent by user             |

#### Game Invitations

| Method | Endpoint                          | Description                                  |
|--------|-----------------------------------|----------------------------------------------|
| POST   | /api/social/invites/send          | Send game invite to a friend                 |
| POST   | /api/social/invites/accept/{id}   | Accept game invitation                       |
| POST   | /api/social/invites/reject/{id}   | Reject game invitation                       |
| GET    | /api/social/invites               | Get pending game invitations                 |

#### Presence

| Method | Endpoint                          | Description                                  |
|--------|-----------------------------------|----------------------------------------------|
| GET    | /api/social/presence/friends      | Get online status of all friends             |

#### WebSocket Endpoints

| Destination                       | Direction     | Description                          |
|-----------------------------------|---------------|--------------------------------------|
| /app/presence.getFriendsStatus    | Client→Server | Request friends' online status       |
| /user/queue/notifications         | Server→Client | Personal notifications               |
| /user/queue/presence              | Server→Client | Friend presence updates (online/offline) |
| /user/queue/friends-status        | Server→Client | Response to friends status request   |

#### Notification Types

| Type                   | SubType          | Description                          |
|------------------------|------------------|--------------------------------------|
| NOTIFICATION_RECEIVED  | FRIEND_REQUEST   | New friend request received          |
| NOTIFICATION_RECEIVED  | REQUEST_ACCEPTED | Your friend request was accepted     |
| NOTIFICATION_RECEIVED  | GAME_INVITE      | Game invitation received             |
| NOTIFICATION_RECEIVED  | FRIEND_REMOVED   | You were removed from friend list    |

---

### Statistical Service Endpoints

#### Player Statistics

| Method | Endpoint                          | Auth | Description                          |
|--------|-----------------------------------|------|--------------------------------------|
| GET    | /api/statistical/me               | ✅   | Get all stats for current user       |
| GET    | /api/statistical/me/{gameType}    | ✅   | Get stats for specific game type     |
| GET    | /api/statistical/player/{playerId}| ❌   | Get all stats for any player         |
| GET    | /api/statistical/player/{id}/{type}| ❌  | Get player stats for specific game   |

#### Rankings

| Method | Endpoint                          | Auth | Description                          |
|--------|-----------------------------------|------|--------------------------------------|
| GET    | /api/statistical/rankings/{gameType} | ❌ | Get top players for a game type     |

#### Rankings Query Parameters
- `limit` (int, default: 30, max: 100) - Number of players to return

## Environment Variables

All required environment variables are documented in the respective module documentation files:

### Backend Services
- **Authorization**: See [AUTHORIZATION.md](./AUTHORIZATION.md#environment-variables)
- **Menu**: See [MENU.md](./MENU.md#environment-variables)
- **Social**: See [SOCIAL.md](./SOCIAL.md#environment-variables)
- **Makao**: See [MAKAO.md](./MAKAO.md#environment-variables)
- **Ludo**: See [LUDO.md](./LUDO.md#environment-variables)
- **Statistical**: See [STATISTICAL.md](./STATISTICAL.md#environment-variables)

### Frontend
- **React App**: See [FRONTEND.md](./FRONTEND.md#environment-variables)

### Where to Place `.env` Files

| Location                           | Description                              |
|------------------------------------|------------------------------------------|
| `frontend/.env`                    | Frontend environment variables (Vite)   |
| `backend/authorization/.env`       | Authorization service configuration     |
| `backend/menu/.env`                | Menu service configuration              |
| `backend/social/.env`              | Social service configuration            |
| `backend/makao/.env`               | Makao game service configuration        |
| `backend/ludo/.env`                | Ludo game service configuration         |
| `backend/statistical/.env`         | Statistical service configuration       |
| `.env` (project root)              | Docker Compose shared variables         |

### Docker Compose

When using Docker Compose, environment variables can be set in:
1. **`.env` file** in the project root (automatically loaded by Docker Compose)
2. **`environment:` section** in `docker-compose.yml` / `docker-compose.prod.yml`
3. **Shell environment** before running `docker compose` commands

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request

---

## License

This project is developed for educational purposes.

---

## Support

For questions or issues, please open a GitHub issue.
