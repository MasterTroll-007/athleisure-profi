# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.fitness.SomeTestClass"

# Clean build
./gradlew clean build

# Build JAR for deployment
./gradlew bootJar
```

Output JAR: `build/libs/fitness-backend.jar`

## Project Overview

This is a **fitness/personal training reservation backend** built with:
- Spring Boot 3.2.1 + Kotlin 1.9
- PostgreSQL database with JPA/Hibernate
- JWT-based authentication
- GoPay payment integration (Czech payment gateway)

## Architecture

### Package Structure (`com.fitness`)

```
controller/     REST API endpoints
service/        Business logic
repository/     Spring Data JPA repositories
entity/         JPA entities (User, Reservation, CreditTransaction, etc.)
dto/            Data transfer objects
security/       JWT filter, service, and UserPrincipal
config/         Security and exception handling configuration
```

### Core Domain Concepts

1. **Credits System**: Users purchase credit packages (via GoPay) and spend credits to book reservations
2. **Availability Blocks**: Admin defines recurring or one-time time slots when bookings are allowed
3. **Reservations**: Users book slots from availability blocks, deducting credits
4. **Training Plans**: Predefined training offerings with credit costs

### Authentication Flow

- JWT access tokens (15 min) + refresh tokens (7 days)
- `JwtAuthenticationFilter` extracts claims and sets `UserPrincipal` in SecurityContext
- Roles: `client` (default), `admin`
- Admin endpoints require `ROLE_ADMIN` via `@PreAuthorize`

### Key Security Configuration

Public endpoints (no auth required):
- `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/verify-email`
- `GET /api/plans`, `GET /api/credits/packages`, `GET /api/availability/blocks/active`
- `/api/gopay/webhook`

Admin-only: `/api/admin/**`

### Database

- PostgreSQL with `hibernate.ddl-auto: update`
- UUIDs for all entity primary keys
- User credits stored directly on User entity, transactions logged separately

### Environment Variables

Key configuration in `application.yml` with env var overrides:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CORS_ORIGINS`
- `SMTP_*` for email verification
- `APP_BASE_URL` for email links

Default dev database: `jdbc:postgresql://localhost:5433/fitness`

Musis pouzit [docker-compose.local.yml](../docker-compose.local.yml) pro lokalni vyvoj