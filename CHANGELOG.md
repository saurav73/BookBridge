# BookBridge Changelog

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
