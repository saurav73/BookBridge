# BookBridge Changelog

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
