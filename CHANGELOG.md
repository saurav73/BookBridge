# BookBridge Changelog

## 2026-10-22
- Removed the unused `java.util.List` import from `BookController` (no references in the file body)
- Re-verified README tech line against `pom.xml`: Java 11 / Spring Boot / session auth all still accurate; no doc changes needed

## 2026-10-21
- README config table now documents the unit of `server.servlet.session.timeout` (seconds; default `86400` = 24 hours)
- Verified the WebSocket/STOMP chat docs against `WebSocketConfig` and `MessageController`: `/ws` endpoint, `/app` prefix, `/topic`+`/queue` broker, `/queue/messages` user destination all still accurate
- No secrets committed: `application.properties` carries no credentials beyond an empty MySQL password; `uploads/` stays git-ignored and untracked

## 2026-10-20
- Fixed `mvnw` file mode in git: the 2026-10-11 entry claimed it was executable, but the git index still carried mode 100644 — now recorded as 100755 so fresh clones can run `./mvnw` directly
- README config table now states the actual default port (`8080`)

## 2026-10-19
- Documented the WebSocket/STOMP chat surface in the README: `/ws` SockJS endpoint, `/app/chat` send destination with payload fields, `/user/queue/messages` receive destination

## 2026-10-17
- Added method-level Javadoc to all service classes: chat, message, and notification services (22 methods); transaction, user, book, and category services (45 methods)
- Added endpoint Javadoc to all 9 controllers (47 endpoint methods)
- Added method-level Javadoc to `FileStorageService`, `PasswordEncoder`, and all 7 repository interfaces (39 methods)

## 2026-10-15
- Added class-level Javadoc to all model classes and enums (16), repository interfaces (7), exception classes (4), WebConfig, and the application entry point

## 2026-10-13
- Added class-level Javadoc to all 9 controllers, both configs (session, WebSocket), and the storage/encoder utils

## 2026-10-11
- Removed a committed user ID-card photo from git tracking; `uploads/` is now git-ignored (user uploads don't belong in the repo)
- Made `mvnw` executable
- Moved the Spring Boot project from the `Bookbridge/` subdirectory to the repo root
- Added README (features, setup, configuration reference) and class-level Javadoc to the service layer

## Initial commit
- Spring Boot book exchange platform for students: books & categories, auth & users, WebSocket chat, transactions, notifications, file uploads (`hi`)
