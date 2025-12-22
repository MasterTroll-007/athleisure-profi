# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Athleisure-Domi is a fitness reservation and personal training booking system with a credit-based payment model. Users purchase credits via GoPay (Czech payment gateway) and spend them to book training sessions.

## Tech Stack

- **Backend**: Spring Boot 3.2.1 + Kotlin 1.9, PostgreSQL, JWT auth
- **Frontend**: React 18 + TypeScript, Vite, Tailwind CSS, Zustand, React Query
- **Infrastructure**: Docker Compose with Nginx reverse proxy

## Build and Run Commands

### Backend (`/backend`)

```bash
./gradlew bootRun                           # Start dev server (port 8080)
./gradlew build                             # Full build
./gradlew test                              # Run all tests
./gradlew test --tests "com.fitness.XTest"  # Run single test class
./gradlew bootJar                           # Build JAR → build/libs/fitness-backend.jar
```

### Frontend (`/frontend`)

```bash
npm install          # Install dependencies
npm run dev          # Start Vite dev server (port 3000, proxies /api to 8080)
npm run build        # Production build → dist/
npm run lint         # ESLint (0 warnings required)
```

### Docker

```bash
docker-compose up           # Start all services
docker-compose up --build   # Rebuild and start
docker-compose down -v      # Stop and remove volumes (resets DB)
```

## Architecture

### Backend Package Structure (`com.fitness`)

```
controller/     REST endpoints (Auth, Reservation, Availability, Credit, Plan, Admin, Gopay)
service/        Business logic
repository/     Spring Data JPA repositories
entity/         JPA entities (User, Reservation, AvailabilityBlock, CreditTransaction, etc.)
dto/            Request/response DTOs
security/       JwtService, JwtAuthenticationFilter, RateLimiter
config/         SecurityConfig, GlobalExceptionHandler
```

### Frontend Structure (`/frontend/src`)

```
pages/          Route components (Home, Login, reservations/, admin/, credits/, plans/)
components/     ui/ (Button, Card, Modal, Toast) + layout/ (Header, BottomNav)
stores/         Zustand stores (authStore, themeStore)
services/       api.ts (Axios with JWT interceptors)
i18n/           Czech + English translations
```

### Core Domain Concepts

1. **Credits**: Users buy credit packages, spend credits to book reservations
2. **Availability Blocks**: Admin-defined time slots (recurring or one-time)
3. **Reservations**: Booking a slot deducts credits from user
4. **Training Plans**: Offerings with defined credit costs

### Authentication

- JWT access tokens (15 min) + refresh tokens (7 days)
- Roles: `client` (default), `admin`
- Admin endpoints: `/api/admin/**` require `ROLE_ADMIN`
- Public endpoints: `/api/auth/*`, `GET /api/plans`, `GET /api/credits/packages`, `/api/gopay/webhook`

### Frontend State Management

- **Zustand**: Auth state (user, tokens, login/logout), theme (dark/light)
- **React Query**: Server state with automatic caching and refetching
- **React Hook Form + Zod**: Form handling with typed validation

## Database

PostgreSQL with Hibernate auto-DDL. Schema initialized via `init.sql`. UUIDs for all primary keys.

Dev connection: `localhost:5433/fitness`

## Environment Variables

Required in `.env` (see `.env.example`):
- `DB_PASSWORD` - PostgreSQL password
- `JWT_SECRET` - 256-bit secret for token signing
- `GMAIL_USER`, `GMAIL_APP_PASSWORD` - Email verification SMTP
- `APP_BASE_URL` - Base URL for email links
- `CORS_ORIGINS` - Allowed origins (comma-separated)

## Network Architecture

```
Browser → Nginx (80) → /api/* → Backend (8080) → PostgreSQL (5432)
                     → /*     → Frontend (3000)
```

### Admin je veselka.j@email.cz / Nujfo6oJbo
- pokud uzivatel neexistuje, tak ho rucne v databazi vytvor pro testovani s roli admin