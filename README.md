# BookBridge

**Book exchange platform for students** — a Spring Boot backend for buying, selling, and exchanging second-hand books, with real-time chat between buyers and sellers.

## Features

- **Books & categories** — list books, browse by category (`BookController`, `CategoryController`)
- **Auth & users** — registration/login, user profiles, admin panel (`AuthController`, `UserController`, `AdminController`)
- **Real-time chat** — WebSocket-based messaging between users (`ChatController`, `MessageController`, `WebSocketConfig`)
- **Transactions** — buy/sell/exchange flows (`TransactionController`)
- **Notifications** — in-app notifications (`NotificationController`)
- **File uploads** — book photos stored under `uploads/` (git-ignored; configure via `file.upload-dir`)

## Tech

Java 11, Spring Boot 2.7.14, Spring Security (session-based, see `SessionConfig`), WebSocket, JPA/Hibernate, MySQL, Maven.

## Setup

```bash
./mvnw spring-boot:run
```

Configure in `src/main/resources/application.properties` first:

| Key | Purpose |
|---|---|
| `server.port` | HTTP port (default `8080`) |
| `spring.datasource.url` / `username` / `password` | MySQL connection (default database `bookbridge`, auto-created via `createDatabaseIfNotExist=true`) |
| `spring.jpa.hibernate.ddl-auto` | Schema handling (default `update`) |
| `file.upload-dir` | Where uploaded photos are stored (`uploads/`, not committed) |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | Upload caps: 100 MB per file, 150 MB per request |
| `server.servlet.session.timeout` | Session timeout in seconds (default `86400` = 24 hours) |
| `spring.session.store-type` | Session storage backend — set to `none` in `application.properties`, so sessions are in-memory only and are not persisted across restarts |

## Real-time chat over WebSocket

Live chat uses STOMP over SockJS. Connect to `/ws`, then:

- **Send** a message to `/app/chat` with a payload containing
  `senderId`, `receiverId`, `content`, and optional `bookId`
- **Receive** messages on the user destination `/user/queue/messages`
  (the simple broker also serves `/topic` and `/queue`)

Message history and REST endpoints for chat live under
`MessageController` (`/api/messages`).

## Tests

```bash
./mvnw test
```

The suite is currently a single Spring context-load test
(`BookbridgeApplicationTests`); there are no controller or service tests yet.

## Project structure

| Path | What it is |
|---|---|
| `src/main/java/com/bookbridge/controller/` | REST + WebSocket controllers |
| `src/main/java/com/bookbridge/config/` | Session, web, and WebSocket config |
| `src/main/java/com/bookbridge/model/` | JPA entities (books, users, chats, transactions) |
| `src/main/java/com/bookbridge/repository/` | Spring Data repositories |
| `src/main/java/com/bookbridge/service/` | Business logic |
| `src/main/java/com/bookbridge/util/` | File storage + password helpers |
| `src/main/java/com/bookbridge/exception/` | Domain exceptions |
| `src/main/resources/application.properties` | All configuration |
| `CHANGELOG.md` | Dated build history |
