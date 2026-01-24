# Push Notifications Implementation Plan

## Overview

This plan implements Firebase Cloud Messaging (FCM) push notifications for the Athleisure-Domi fitness app. The feature enables real-time notifications for reservation updates, reminders, and admin alerts.

## Architecture

```
Backend (Spring Boot):
  NotificationController → NotificationService → FirebaseMessaging
                        → DeviceTokenService  → DeviceTokenRepository
                                                        ↓
                                              DeviceToken Entity (PostgreSQL)

Mobile App (Android):
  FitnessMessagingService (FCM) → NotificationRepository → ApiService
                               → Deep Link Navigation
```

## Implementation Steps

### Phase 1: Backend - Database & Entities

**1.1 Add Firebase Admin SDK dependency**
- File: `backend/build.gradle.kts`
- Add: `implementation("com.google.firebase:firebase-admin:9.2.0")`

**1.2 Create DeviceToken entity**
- New file: `backend/src/main/kotlin/com/fitness/entity/DeviceToken.kt`
- Fields: id, userId, token, deviceName, deviceOS, osVersion, appVersion, isActive, lastUsedAt

**1.3 Create NotificationLog entity**
- New file: `backend/src/main/kotlin/com/fitness/entity/NotificationLog.kt`
- Fields: id, userId, type, title, body, referenceId, sentAt, isRead, createdAt

**1.4 Create repositories**
- New file: `backend/src/main/kotlin/com/fitness/repository/DeviceTokenRepository.kt`
- New file: `backend/src/main/kotlin/com/fitness/repository/NotificationLogRepository.kt`

**1.5 Update init.sql with new tables**
- Add device_tokens and notification_logs tables with indexes

### Phase 2: Backend - DTOs & Services

**2.1 Create notification DTOs**
- New file: `backend/src/main/kotlin/com/fitness/dto/NotificationDTOs.kt`
- RegisterDeviceTokenRequest, UnregisterDeviceTokenRequest, PushNotificationPayload

**2.2 Create Firebase configuration**
- New file: `backend/src/main/kotlin/com/fitness/config/FirebaseConfig.kt`
- Initialize FirebaseApp and FirebaseMessaging beans

**2.3 Create DeviceTokenService**
- New file: `backend/src/main/kotlin/com/fitness/service/DeviceTokenService.kt`
- Methods: registerDeviceToken, unregisterDeviceToken, getUserDeviceTokens, logoutAllDevices

**2.4 Create NotificationService**
- New file: `backend/src/main/kotlin/com/fitness/service/NotificationService.kt`
- Methods: sendToUser, notifyReservationCreated, notifyReservationCancelled, notifyReservationReminder, notifyAdminNewReservation

### Phase 3: Backend - Controller & Integration

**3.1 Create NotificationController**
- New file: `backend/src/main/kotlin/com/fitness/controller/NotificationController.kt`
- Endpoints:
  - POST /api/notifications/device-tokens/register
  - POST /api/notifications/device-tokens/unregister
  - GET /api/notifications/device-tokens
  - POST /api/notifications/logout-all-devices
  - GET /api/notifications/history

**3.2 Update SecurityConfig**
- File: `backend/src/main/kotlin/com/fitness/config/SecurityConfig.kt`
- Add /api/notifications/** to authenticated routes

**3.3 Integrate with ReservationService**
- File: `backend/src/main/kotlin/com/fitness/service/ReservationService.kt`
- Add notification calls on reservation create/cancel

**3.4 Create ReservationReminderScheduler (optional)**
- New file: `backend/src/main/kotlin/com/fitness/service/ReservationReminderScheduler.kt`
- Scheduled task to send 24h and 1h reminders

### Phase 4: Mobile App - Dependencies & Config

**4.1 Add Firebase dependencies**
- File: `mobile-app/build.gradle.kts`
  - Add: `id("com.google.gms.google-services") version "4.4.0" apply false`
- File: `mobile-app/app/build.gradle.kts`
  - Add plugin: `id("com.google.gms.google-services")`
  - Add: `implementation(platform("com.google.firebase:firebase-bom:33.1.0"))`
  - Add: `implementation("com.google.firebase:firebase-messaging")`

**4.2 Add google-services.json**
- Download from Firebase Console
- Place at: `mobile-app/app/google-services.json`

**4.3 Update AndroidManifest.xml**
- Add POST_NOTIFICATIONS permission
- Register FitnessMessagingService

### Phase 5: Mobile App - Data Layer

**5.1 Create notification DTOs**
- New file: `mobile-app/app/src/main/java/com/fitness/app/data/dto/NotificationDTOs.kt`

**5.2 Update ApiService**
- File: `mobile-app/app/src/main/java/com/fitness/app/data/api/ApiService.kt`
- Add notification endpoints

**5.3 Create NotificationRepository**
- New file: `mobile-app/app/src/main/java/com/fitness/app/data/repository/NotificationRepository.kt`

### Phase 6: Mobile App - FCM Implementation

**6.1 Create FitnessMessagingService**
- New file: `mobile-app/app/src/main/java/com/fitness/app/data/messaging/FitnessMessagingService.kt`
- Handle onNewToken and onMessageReceived
- Show system notifications

**6.2 Create NotificationChannelManager**
- New file: `mobile-app/app/src/main/java/com/fitness/app/data/messaging/NotificationChannelManager.kt`
- Create notification channels for Android 8+

**6.3 Create FirebaseTokenManager**
- New file: `mobile-app/app/src/main/java/com/fitness/app/data/messaging/FirebaseTokenManager.kt`
- Handle token initialization and registration with backend

**6.4 Create NotificationPermissionHelper**
- New file: `mobile-app/app/src/main/java/com/fitness/app/util/NotificationPermissionHelper.kt`
- Request POST_NOTIFICATIONS permission on Android 13+

### Phase 7: Mobile App - Integration

**7.1 Update AuthRepository**
- File: `mobile-app/app/src/main/java/com/fitness/app/data/repository/AuthRepository.kt`
- Register FCM token on login
- Unregister FCM token on logout

**7.2 Update MainActivity**
- File: `mobile-app/app/src/main/java/com/fitness/app/MainActivity.kt`
- Request notification permission
- Handle deep links from notifications

**7.3 Update FitnessApplication**
- File: `mobile-app/app/src/main/java/com/fitness/app/FitnessApplication.kt`
- Initialize notification channels

## Files Summary

### Backend - New Files (10)
1. `backend/src/main/kotlin/com/fitness/entity/DeviceToken.kt`
2. `backend/src/main/kotlin/com/fitness/entity/NotificationLog.kt`
3. `backend/src/main/kotlin/com/fitness/repository/DeviceTokenRepository.kt`
4. `backend/src/main/kotlin/com/fitness/repository/NotificationLogRepository.kt`
5. `backend/src/main/kotlin/com/fitness/dto/NotificationDTOs.kt`
6. `backend/src/main/kotlin/com/fitness/config/FirebaseConfig.kt`
7. `backend/src/main/kotlin/com/fitness/service/DeviceTokenService.kt`
8. `backend/src/main/kotlin/com/fitness/service/NotificationService.kt`
9. `backend/src/main/kotlin/com/fitness/controller/NotificationController.kt`
10. `backend/src/main/kotlin/com/fitness/service/ReservationReminderScheduler.kt` (optional)

### Backend - Modified Files (3)
1. `backend/build.gradle.kts` - Add Firebase dependency
2. `backend/src/main/kotlin/com/fitness/config/SecurityConfig.kt` - Add notification routes
3. `backend/src/main/resources/init.sql` - Add tables

### Mobile App - New Files (6)
1. `mobile-app/app/google-services.json` - Firebase config (from console)
2. `mobile-app/app/src/main/java/com/fitness/app/data/dto/NotificationDTOs.kt`
3. `mobile-app/app/src/main/java/com/fitness/app/data/messaging/FitnessMessagingService.kt`
4. `mobile-app/app/src/main/java/com/fitness/app/data/messaging/NotificationChannelManager.kt`
5. `mobile-app/app/src/main/java/com/fitness/app/data/messaging/FirebaseTokenManager.kt`
6. `mobile-app/app/src/main/java/com/fitness/app/util/NotificationPermissionHelper.kt`

### Mobile App - Modified Files (5)
1. `mobile-app/build.gradle.kts` - Add google-services plugin
2. `mobile-app/app/build.gradle.kts` - Add Firebase dependencies
3. `mobile-app/app/src/main/AndroidManifest.xml` - Add permission & service
4. `mobile-app/app/src/main/java/com/fitness/app/data/api/ApiService.kt` - Add endpoints
5. `mobile-app/app/src/main/java/com/fitness/app/data/repository/AuthRepository.kt` - FCM integration

## Prerequisites

1. **Firebase Project Setup**
   - Create Firebase project at console.firebase.google.com
   - Add Android app with package name `com.fitness.app`
   - Download `google-services.json`
   - Generate service account key JSON for backend

2. **Environment Variables**
   - `FIREBASE_SERVICE_ACCOUNT_JSON` - Backend service account credentials

## Testing Plan

1. **Backend Unit Tests**
   - DeviceTokenService: register, unregister, deactivate tokens
   - NotificationService: send notifications, handle invalid tokens

2. **Backend Integration Tests**
   - NotificationController endpoints
   - Database operations

3. **Mobile App Testing**
   - FCM token generation and refresh
   - Notification display
   - Deep link navigation
   - Permission handling

4. **E2E Testing**
   - Complete flow: Login → Register token → Create reservation → Receive notification → Click → Navigate

## Notification Types

| Type | Trigger | Recipients | Deep Link |
|------|---------|------------|-----------|
| RESERVATION_CREATED | User books slot | User | /reservations/{id} |
| RESERVATION_CANCELLED | Cancellation | User | /credits |
| RESERVATION_REMINDER | 24h/1h before | User | /reservations/{id} |
| ADMIN_NEW_RESERVATION | New booking | All admins | /admin/calendar |
| LOW_CREDITS | Credit balance ≤ 5 | User | /credits/buy |

## Estimated Effort

- **Backend**: ~20 files, ~1500 lines of code
- **Mobile App**: ~10 files, ~800 lines of code
- **Total**: ~30 files, ~2300 lines of code
