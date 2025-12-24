# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug                     # Build debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease                   # Build release APK
./gradlew installDebug                      # Install on connected device/emulator
./gradlew test                              # Run unit tests
./gradlew connectedAndroidTest              # Run instrumented tests
./gradlew clean assembleDebug               # Clean and rebuild
```

## Project Overview

Android client for the Athleisure-Domi fitness reservation system. Supports both client and admin roles with full feature parity to the web frontend.

**Tech Stack:**
- Kotlin 1.9.21, Java 17, Compile SDK 34, Min SDK 26
- Jetpack Compose with Material 3
- Hilt for dependency injection
- Retrofit + OkHttp + Kotlinx Serialization
- Coroutines + StateFlow for async/state

## Architecture

MVVM pattern with clean layer separation:

```
data/                   Data Layer
├── api/                ApiService.kt (Retrofit), AuthInterceptor, TokenAuthenticator
├── dto/                Serializable DTOs matching backend API
├── local/              TokenManager (encrypted), PreferencesManager (DataStore)
└── repository/         Result<T> wrapper pattern, all business operations

ui/                     Presentation Layer
├── screens/            Screen composables + @HiltViewModel ViewModels
├── components/         BottomNavigation, LoadingContent, ErrorContent, EmptyContent
├── navigation/         Routes.kt, FitnessNavHost.kt, AuthNavigationViewModel
└── theme/              Material 3 theme (Color, Type, Theme)

di/                     NetworkModule.kt - Hilt providers
util/                   DateUtils, LocaleHelper
```

## API Configuration

Base URL configured in `app/build.gradle.kts`:
- Debug: `http://10.0.2.2:8080/api` (emulator localhost)
- Release: `https://your-production-url.com/api`

**Public endpoints** (no auth): `/auth/login`, `/auth/register`, `/auth/verify-email`, `/auth/resend-verification`, `/auth/refresh`, `/plans`, `/credits/packages`

All other endpoints require JWT Bearer token via `AuthInterceptor`.

## Token Management

Tokens stored in `EncryptedSharedPreferences` (AES256_GCM):
- `TokenManager` exposes `StateFlow<String?>` for access/refresh tokens
- `TokenAuthenticator` handles automatic 401 → token refresh → retry
- Failed refresh clears tokens and triggers logout

## Navigation Routes

```kotlin
// Auth
Routes.Login, Routes.Register, Routes.VerifyEmail(email)

// Client
Routes.Home, Routes.Reservations, Routes.NewReservation
Routes.Credits, Routes.BuyCredits, Routes.Plans, Routes.MyPlans
Routes.Profile, Routes.ChangePassword

// Admin
Routes.Admin, Routes.AdminCalendar, Routes.AdminTemplates
Routes.AdminClients, Routes.AdminClientDetail(id)
Routes.AdminPlans, Routes.AdminPricing, Routes.AdminPayments
```

Bottom nav shows Admin tab conditionally when `user.role == "admin"`.

## State Management Pattern

All ViewModels follow this pattern:

```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SomeUiState())
    val uiState: StateFlow<SomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getData()) {
                is Result.Success -> _uiState.update { it.copy(data = result.data, isLoading = false) }
                is Result.Error -> _uiState.update { it.copy(errorResId = R.string.error, isLoading = false) }
            }
        }
    }
}
```

## Result<T> Sealed Class

All repository methods return `Result<T>`:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
}
```

## Internationalization

- Languages: Czech (cs), English (en), system default
- String resources: `res/values/strings.xml` (en), `res/values-cs/strings.xml` (cs)
- `LocaleHelper.wrapContext()` applied in `MainActivity.attachBaseContext()`
- `PreferencesManager` stores selected locale

## Key Files

| File | Purpose |
|------|---------|
| `ApiService.kt` | All Retrofit endpoint definitions (45+ endpoints) |
| `NetworkModule.kt` | Hilt DI for OkHttp, Retrofit, ApiService |
| `TokenManager.kt` | Encrypted token storage with StateFlow |
| `TokenAuthenticator.kt` | Auto token refresh on 401 |
| `AuthRepository.kt` | Login, register, profile, currentUser StateFlow |
| `FitnessNavHost.kt` | Navigation graph with auth state handling |
| `Routes.kt` | All route definitions |

## Adding New Screens

1. Create DTO in `data/dto/` if new API response
2. Add endpoint to `ApiService.kt`
3. Add repository method in appropriate `data/repository/`
4. Create `ui/screens/feature/FeatureScreen.kt` + `FeatureViewModel.kt`
5. Add route to `Routes.kt` and navigation in `FitnessNavHost.kt`
6. Add strings to both `values/strings.xml` and `values-cs/strings.xml`

## Testing with Emulator

The app connects to `10.0.2.2:8080` which maps to host machine's localhost. Ensure backend is running on port 8080 before testing.

## MCP Screenshot Testing

Save screenshots to `../claudeScreenshot/` instead of embedding in context:

```
mobile_save_screenshot(device, saveTo: "../claudeScreenshot/screenshot.png")
```
