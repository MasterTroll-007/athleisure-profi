# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Athleisure-Domi is a fitness reservation and personal training booking system with a credit-based payment model. Users purchase credits via GoPay (Czech payment gateway) and spend them to book training sessions.

## Tech Stack

- **Backend**: Spring Boot 3.2.1 + Kotlin 1.9, PostgreSQL, JWT auth
- **Frontend**: React 18 + TypeScript, Vite, Tailwind CSS, Zustand, React Query
- **Mobile App**: Android (Kotlin, Jetpack Compose, Hilt, Retrofit)
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

### Mobile App (`/mobile-app`)

```bash
./gradlew assembleDebug                     # Build debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease                   # Build release APK
./gradlew installDebug                      # Install on connected device/emulator
./gradlew test                              # Run unit tests
./gradlew connectedAndroidTest              # Run instrumented tests
```

API base URL for emulator: `http://10.0.2.2:8080/api` (configured in `app/build.gradle.kts`)

### Docker (Local Development)

**IMPORTANT**: For local development, always use `docker-compose.local.yml` which builds from local source code:

```bash
docker compose -f docker-compose.local.yml up --build      # Build and start all services
docker compose -f docker-compose.local.yml up -d           # Start in background
docker compose -f docker-compose.local.yml down -v         # Stop and remove volumes (resets DB)
docker compose -f docker-compose.local.yml build --no-cache backend   # Force rebuild backend
docker compose -f docker-compose.local.yml logs -f         # Follow logs
```

The default `docker-compose.yml` uses pre-built images from Docker Hub (`jveselka/fitness-*:latest`) for production deployment - do NOT use it for local development as your code changes won't be included.

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

### Mobile App Structure (`/mobile-app/app/src/main/java/com/fitness/app`)

MVVM with Hilt DI:
```
data/           Data layer
  api/          Retrofit API service (ApiService.kt), AuthInterceptor, TokenAuthenticator
  dto/          Data transfer objects matching backend API (AuthDTOs, ReservationDTOs, etc.)
  local/        TokenManager (encrypted), PreferencesManager (DataStore)
  repository/   Repository implementations (AuthRepository, ReservationRepository, etc.)
ui/             Presentation layer (Jetpack Compose)
  components/   Reusable UI components (BottomNavigation, LoadingContent)
  navigation/   Navigation graph (FitnessNavHost), Routes, AuthNavigationViewModel
  screens/      Screen composables with ViewModels (auth/, home/, admin/, reservations/, credits/, plans/, profile/)
  theme/        Material3 theme, colors, typography
di/             Hilt dependency injection modules (NetworkModule)
util/           Utilities (DateUtils, LocaleHelper)
```

### Core Domain Concepts

1. **Credits**: Users buy credit packages (via GoPay), spend credits to book reservations
2. **Availability Blocks**: Admin-defined recurring time slots (days_of_week pattern like "1,2,3,4,5")
3. **Reservations**: Booking a slot deducts credits from user
4. **Training Plans**: Downloadable training offerings with credit costs
5. **Pricing Items**: Training types defining credit costs per reservation

### Internationalization

- Primary language: Czech (cs), Secondary: English (en)
- Database entities store bilingual fields: `name_cs`/`name_en`, `description_cs`/`description_en`
- Frontend: i18n translations in `/frontend/src/i18n/`
- Mobile: Android string resources in `res/values/strings.xml` (English) and `res/values-cs/strings.xml` (Czech)

### Authentication

- JWT access tokens (15 min) + refresh tokens (7 days)
- Roles: `client` (default), `admin`
- Admin endpoints: `/api/admin/**` require `ROLE_ADMIN`
- Public endpoints: `/api/auth/*`, `GET /api/plans`, `GET /api/credits/packages`, `/api/gopay/webhook`
- Mobile app stores tokens in Android EncryptedSharedPreferences

### Frontend State Management

- **Zustand**: Auth state (user, tokens, login/logout), theme (dark/light)
- **React Query**: Server state with automatic caching and refetching
- **React Hook Form + Zod**: Form handling with typed validation

### Mobile App State Management

- **Hilt**: Dependency injection
- **ViewModel + StateFlow**: UI state management
- **DataStore**: Persistent preferences
- **Retrofit + Coroutines**: Async API calls

## Database

PostgreSQL with Hibernate auto-DDL. Schema initialized via `init.sql`. UUIDs for all primary keys.

Dev connection: `localhost:5433/fitness` (port 5433 maps to PostgreSQL 5432 in container)

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
Android App → Backend (8080) → PostgreSQL (5432)
```

## Testing Admin Access

Default admins/users credentials are in `init.sql`:

## Testing User Registration

If login doesn't work, register a user via the API endpoint and then manually activate and configure in the database:

1. Register via API:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234","firstName":"Test","lastName":"User","phone":"123456789"}'
```

2. Activate user and set role/credits in database:
```sql
UPDATE users SET email_verified = true, role = 'admin', credits = 100 WHERE email = 'test@example.com';
```

## Mobile App Testing with MCP

When taking screenshots of the mobile app via MCP tools, **always save them to `./claudeScreenshot/`** instead of embedding in context:

```
mobile_save_screenshot(device, saveTo: "./claudeScreenshot/screenshot.png")
```

Then use `Read` tool to view when needed. This prevents context bloat and avoids API errors with large/multiple images.
