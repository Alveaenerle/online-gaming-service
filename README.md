
# 🎮 Online Gaming Service

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://reactjs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)

A robust, microservice-based platform for real-time multiplayer card and board games. Experience seamless gameplay, social interactions, and competitive rankings in a modern web environment.

🌐 **Live Demo:** [https://demo.yapyap.pl/](https://demo.yapyap.pl/)

---

## 📸 Screenshots

### User Interface & Lobby
| Dashboard View | Game Lobby |
| :---: | :---: |
| ![Dashboard](screenshots/dashboard_screenshot.png) | ![Lobby](screenshots/lobby_screenshot.png) |
| *Modern dashboard with quick access to games and friends.* | *Real-time lobby with chat and player readiness status.* |

### Gameplay Experience
| Makao Card Game | Ludo Board Game |
| :---: | :---: |
| ![Makao](screenshots/makao_screenshot.png) | ![Ludo](screenshots/ludo_screenshot.png) |
| *Smooth animations and real-time state synchronization.* | *Interactive board with automated move validation.* |

---

## ✨ Features

* **Diverse Game Library:** Play classic games like **Makao** (card game) and **Ludo** (board game) with friends or bots.
* **Real-time Interaction:** Advanced WebSocket-based messaging for instant gameplay updates and live chat.
* **Social System:**
    * Add/remove friends.
    * Presence tracking (Online/Away/In-game).
    * Direct game invitations.
* **Competitive Edge:** Global rankings and detailed player statistics integrated across all games.
* **Secure Auth:** Full support for local accounts and **Google OAuth 2.0** integration.
* **Responsive Design:** Styled with **Tailwind CSS** and animated with **Framer Motion** for a premium feel.

---

## 🛠️ Tech Stack

### Backend (Microservices)
* **Framework:** Java 21 with Spring Boot 3.5.
* **Data:** MongoDB (Primary Database), Redis (Session & Real-time state).
* **Messaging:** RabbitMQ (Inter-service communication), STOMP/WebSockets.
* **Security:** Spring Security with JWT and OAuth2.

### Frontend
* **Core:** React 19, TypeScript, Vite.
* **State Management:** React Context API & Custom Hooks.
* **Styling:** Tailwind CSS, Lucide React (Icons), Framer Motion.

### Infrastructure
* **Containerization:** Docker & Docker Compose.
* **Proxy:** Nginx (Gateway).
* **CI/CD:** Jenkins Pipeline.

---

## 🚀 Getting Started

### Prerequisites
* **Docker** and **Docker Compose** installed on your machine.

### Quick Start (Production Mode)
```bash
# Clone the repository
git clone https://github.com/alveaenerle/online-gaming-service.git
cd online-gaming-service

# Start the entire stack
docker compose up --build

```

The application will be available at `http://localhost`.

### Development Mode

To run with hot-reload for the frontend:

```bash
docker compose -f docker-compose.dev.yml up --build

```

* **Frontend:** `http://localhost:5173`
* **Backend Services:** Proxied through `http://localhost`

---

## 🧪 Testing

The project maintains high code quality with extensive test coverage.

**Backend (Maven + TestNG):**

```bash
cd backend
./mvnw test

```

**Frontend (Jest):**

```bash
cd frontend
npm test

```

---

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.

---

