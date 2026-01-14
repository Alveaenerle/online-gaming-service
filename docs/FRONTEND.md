# Frontend Documentation

## Overview

The **Frontend** is a React-based single-page application (SPA) that provides the user interface for the Online Gaming Service platform. It features real-time game interactions, social features, and a modern, responsive design.

## Technology Stack

- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS with custom configuration
- **Animations**: Framer Motion
- **Real-time**: STOMP over WebSocket (SockJS)
- **Routing**: React Router v6
- **State Management**: React Context API
- **HTTP Client**: Fetch API

## Project Structure

```
frontend/
├── public/
│   ├── avatars/          # Player avatar images
│   └── SVG-cards-1.3/    # Card graphics for Makao
├── src/
│   ├── assets/           # Static assets
│   ├── components/       # React components
│   │   ├── Auth/         # Login, Register forms
│   │   ├── Dashboard/    # User dashboard
│   │   ├── Games/        # Game components
│   │   │   ├── Ludo/     # Ludo game UI
│   │   │   ├── Makao/    # Makao game UI
│   │   │   └── utils/    # Shared game utilities
│   │   ├── HomePage/     # Home page components
│   │   ├── LandingPage/  # Public landing page
│   │   ├── Pages/        # Page layouts
│   │   └── Shared/       # Reusable components
│   ├── context/          # React Context providers
│   ├── services/         # API and WebSocket services
│   ├── App.tsx           # Main app component
│   └── main.tsx          # Application entry point
├── index.html
├── vite.config.ts
├── tailwind.config.js
└── package.json
```

## Key Components

### Context Providers

#### AuthContext
Manages user authentication state:
```typescript
interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginAsGuest: () => Promise<void>;
  logout: () => Promise<void>;
}
```

#### LobbyContext
Manages game lobby state and WebSocket subscriptions:
```typescript
interface LobbyContextType {
  currentLobby: LobbyInfo | null;
  isLoading: boolean;
  refreshLobbyStatus: () => Promise<void>;
  clearLobby: () => void;
}
```

#### SocialContext
Manages social features (friends, requests, invites):
```typescript
interface SocialContextType {
  friends: Friend[];
  pendingRequests: FriendRequest[];
  sentRequests: FriendRequest[];
  gameInvites: GameInvite[];
  isLoading: boolean;
  refreshSocialData: () => Promise<void>;
  sendFriendRequest: (targetUserId: string) => Promise<void>;
  acceptRequest: (requestId: string) => Promise<void>;
  // ... more methods
}
```

#### ToastContext
Manages toast notifications across the app.

### Services

#### authService
Handles authentication API calls:
- `login(credentials)` - User login
- `register(data)` - User registration
- `loginAsGuest()` - Guest access
- `logout()` - Session termination
- `getCurrentUser()` - Fetch current user
- `updateUsername(newUsername)` - Update display name
- `updatePassword(current, new)` - Change password

#### lobbyService
Manages game lobbies:
- `createRoom(gameType, maxPlayers, name, isPrivate)`
- `joinRoom(accessCode, gameType, isRandom)`
- `startGame()` - Start game (host only)
- `toggleReady()` - Toggle ready status
- `updateAvatar(avatarId)` - Change avatar
- `leaveRoom()` - Exit lobby
- `kickPlayer(userId)` - Remove player (host only)

#### socketService
WebSocket connection management:
- `connect()` - Establish STOMP connection
- `subscribe(topic, callback)` - Subscribe to topic
- `unsubscribe(topic)` - Unsubscribe from topic
- `send(destination, body)` - Send message

#### chatService
Lobby chat functionality:
- `connectToLobby(lobbyId)` - Join chat
- `sendMessage(content)` - Send chat message
- `startTyping()` / `stopTyping()` - Typing indicators
- `requestHistory()` - Fetch message history

#### makaoGameService
Makao game actions:
- `playCard(payload)` - Play a card
- `drawCard()` - Draw from deck
- `playDrawnCard(payload)` - Play drawn card
- `skipDrawnCard()` - Keep drawn card
- `acceptEffect()` - Accept special effect
- `leaveGame()` - Leave game (replaced by bot)

#### socialService
Social features:
- `sendFriendRequest(targetUserId)`
- `acceptFriendRequest(requestId)`
- `rejectFriendRequest(requestId)`
- `getFriends()`
- `removeFriend(friendId)`
- `sendGameInvite(targetUserId)`

## Game Components

### Ludo Game

#### LudoLobby
Pre-game lobby with player management:
- Player list with avatars
- Ready status toggles
- Chat widget
- Start game button (host)

#### LudoGame
Main game interface:
- Interactive game board
- Dice rolling animation
- Pawn movement
- Turn indicators
- Winner announcement

### Makao Game

#### MakaoLobby
Pre-game lobby similar to Ludo:
- Player management
- Avatar selection
- Chat integration

#### MakaoGame
Card game interface:
- Hand display with playable highlights
- Discard pile visualization
- Card effects display
- Turn timer
- Move history log
- MAKAO announcements

### Shared Components

#### SocialCenter
Floating social panel:
- Friend list with online status
- Friend requests
- Game invitations
- Quick actions

#### ChatWidget
In-lobby chat component:
- Real-time messaging
- Typing indicators
- Message history
- Profanity filtering

#### LobbyHeader
Common lobby header:
- Room name
- Access code (private rooms)
- Back navigation

#### LobbyPlayersSection
Player grid with:
- Avatar display
- Ready status
- Kick button (host)
- Friend request button

## Styling

### Tailwind Configuration
Custom theme extensions:
```javascript
theme: {
  extend: {
    colors: {
      // Custom color palette
    },
    animation: {
      // Custom animations
    }
  }
}
```

### Design System
- Dark theme with purple accents
- Gradient backgrounds
- Glassmorphism effects
- Smooth animations with Framer Motion

## Environment Variables

Create a `.env` file in the `frontend/` directory:

| Variable                | Description                           | Default                    |
|-------------------------|---------------------------------------|----------------------------|
| `VITE_API_URL`          | Authorization API base URL            | `/api/auth`                |
| `VITE_MENU_API_URL`     | Menu/Lobby API base URL               | `/api/menu`                |
| `VITE_MENU_WS_URL`      | Menu WebSocket URL                    | `/api/menu/ws`             |
| `VITE_MAKAO_API_URL`    | Makao game API base URL               | `/api/makao`               |
| `VITE_LUDO_API_URL`     | Ludo game API base URL                | `/api/ludo`                |
| `VITE_API_SOCIAL_URL`   | Social service API base URL           | `/api/social`              |

### Example `.env` file

```env
# API Endpoints
VITE_API_URL=/api/auth
VITE_MENU_API_URL=/api/menu
VITE_MENU_WS_URL=/api/menu/ws
VITE_MAKAO_API_URL=/api/makao
VITE_LUDO_API_URL=/api/ludo
VITE_API_SOCIAL_URL=/api/social
```

## Build Configuration

### Development
```bash
npm run dev
```
- Hot module replacement
- Source maps
- Development proxy

### Production
```bash
npm run build
```
- Minification
- Tree shaking
- Asset optimization

### Docker
```dockerfile
# Multi-stage build
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
```

## Nginx Configuration

### Production (nginx.conf)
- Static file serving
- API reverse proxy
- WebSocket upgrade support
- Gzip compression

### Development (nginx.dev.conf)
- Development proxy settings
- CORS handling

## Testing

### Running Tests
```bash
npm run test
```

### Linting
```bash
npm run lint
```

## Routing

| Path                    | Component           | Description           |
|-------------------------|---------------------|-----------------------|
| `/`                     | LandingPage         | Public landing        |
| `/home`                 | HomePage            | Authenticated home    |
| `/login`                | LoginPage           | Login form            |
| `/register`             | RegisterPage        | Registration form     |
| `/dashboard`            | Dashboard           | User dashboard        |
| `/makao/lobby`          | MakaoLobby          | Makao game lobby      |
| `/makao/game`           | MakaoGame           | Makao game play       |
| `/ludo/lobby`           | LudoLobby           | Ludo game lobby       |
| `/ludo/game`            | LudoGame            | Ludo game play        |

## Dependencies

### Main Dependencies
- `react` - UI library
- `react-router-dom` - Routing
- `framer-motion` - Animations
- `sockjs-client` - WebSocket fallback
- `stompjs` - STOMP protocol
- `tailwindcss` - Utility CSS

### Dev Dependencies
- `vite` - Build tool
- `typescript` - Type checking
- `eslint` - Linting
- `postcss` - CSS processing
