# Authorization Service Documentation

## Overview

The **Authorization Service** is a Spring Boot microservice responsible for user authentication, session management, and account operations in the Online Gaming Service platform.

## Technology Stack

- **Framework**: Spring Boot
- **Security**: Spring Security with BCrypt password encoding
- **Session Management**: Redis-based session storage with HTTP-only cookies
- **Database**: MongoDB for account persistence
- **Testing**: TestNG with MockMvc

## Architecture

```
authorization/
├── config/           # Security configuration (CORS, filters, beans)
├── controller/       # REST API endpoints
├── dto/              # Data Transfer Objects
├── exception/        # Custom exceptions
├── model/            # Domain entities (Account, User)
├── repository/       # MongoDB repositories
├── security/         # Authentication filters
└── service/          # Business logic (AuthService, SessionService)
```

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint    | Description                          |
|--------|-------------|--------------------------------------|
| POST   | `/register` | Register a new user account          |
| POST   | `/login`    | Authenticate and create session      |
| POST   | `/guest`    | Create a guest session               |
| POST   | `/logout`   | Destroy session and clear cookie     |

### Protected Endpoints (Authentication Required)

| Method | Endpoint           | Description                      |
|--------|--------------------|----------------------------------|
| GET    | `/me`              | Get current authenticated user   |
| PUT    | `/update-username` | Update user's display name       |
| PUT    | `/update-password` | Change user's password           |
| GET    | `/email`           | Get user's email address         |

## Features

### User Registration
- Username validation (3-20 characters)
- Email format validation
- Password encoding with BCrypt (strength 10)
- Duplicate email detection

### Authentication
- Email and password-based login
- Session cookie with configurable expiration
- Stateless JWT-like session tokens stored in Redis

### Guest Access
- Temporary guest accounts for quick game access
- Limited functionality (cannot update username/password)
- Unique guest identifiers

### Session Management
- HTTP-only secure cookies
- Redis-backed session storage
- Automatic session validation on each request
- Clean session invalidation on logout

## Request/Response DTOs

### RegisterRequest
```json
{
  "username": "string (3-20 chars)",
  "email": "valid-email@example.com",
  "password": "string"
}
```

### LoginRequest
```json
{
  "email": "valid-email@example.com",
  "password": "string"
}
```

### User Response
```json
{
  "id": "uuid-string",
  "username": "string",
  "isGuest": false
}
```

## Security Features

- **Password Hashing**: BCrypt with configurable rounds
- **Session Tokens**: Secure, HTTP-only cookies
- **CORS Configuration**: Configurable allowed origins
- **Stateless Design**: No server-side session state (Redis-backed)
- **Request Filtering**: AuthTokenFilter validates all protected requests

## Error Handling

| Exception                    | HTTP Status | Description                    |
|------------------------------|-------------|--------------------------------|
| EmailAlreadyExistsException  | 400         | Email already registered       |
| UsernameAlreadyExistsException | 400       | Username already taken         |
| InvalidCredentialsException  | 401         | Wrong email or password        |

## Configuration

### Environment Variables

| Variable              | Description                           | Default                    |
|-----------------------|---------------------------------------|----------------------------|
| `SERVER_PORT`         | Application port                      | `8080`                     |
| `MONGODB_URI`         | MongoDB connection string             | `mongodb://localhost:27017/auth` |
| `REDIS_HOST`          | Redis server host                     | `localhost`                |
| `REDIS_PORT`          | Redis server port                     | `6379`                     |
| `CORS_ORIGINS`        | Allowed CORS origins                  | `http://localhost:5173`    |
| `SESSION_COOKIE_NAME` | Name of the session cookie            | `SESSION`                  |
| `SESSION_MAX_AGE`     | Session expiration time (seconds)     | `86400` (24h)              |

### Application Properties

```yaml
server.port: ${SERVER_PORT:8080}
app.cors.allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
spring.data.mongodb.uri: ${MONGODB_URI:mongodb://localhost:27017/auth}
spring.data.redis.host: ${REDIS_HOST:localhost}
spring.data.redis.port: ${REDIS_PORT:6379}
```

## Testing

The service includes comprehensive unit and integration tests:

- **AuthControllerTest**: API endpoint testing with MockMvc
- **AuthServiceTest**: Business logic unit tests
- **SessionServiceTest**: Session management tests

Run tests with:
```bash
./mvnw test -pl authorization
```

## Dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-mongodb`
- `spring-boot-starter-data-redis`
- `lombok`
- `testng`
