# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AI ScoreCast** (package: `com.soccertips.predictx`) is an Android app for football match predictions and live scores. Built with Jetpack Compose, it follows MVVM architecture with a repository pattern, uses Firebase for backend services, and integrates AdMob for monetization.

**Current Version:** 2.1.0 (versionCode 30)

## Essential Commands

### Building
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (requires signing config)
./gradlew installDebug           # Install debug build to connected device
```

### Testing
```bash
./gradlew test                   # Run unit tests
./gradlew connectedCheck         # Run instrumentation tests
./gradlew test --tests "com.soccertips.predictx.ClassName"  # Run specific test class
```

### Code Quality
```bash
./gradlew lint                   # Run lint checks (lint-baseline.xml for suppressions)
./gradlew lintDebug              # Run lint on debug build only
```

### Cleaning
```bash
./gradlew clean                  # Clean build artifacts
```

## Configuration Setup

The app requires a `dot.env` file in the project root with the following keys:
```properties
API_KEY=your_api_key
API_HOST=your_api_host
API_BASE_URL=https://your-api-base-url.com
DAILY_BONUS_BASE_URL=https://your-daily-bonus-url.com
API_BASE_URL_VALUE=https://alternative-api-url.com
```

These values are loaded at build time and injected into `BuildConfig` fields.

## Architecture Overview

### Layer Structure

**Data Flow:** API/Firebase → Repository → ViewModel → Compose UI

```
com.soccertips.predictx/
├── admob/              # AdMob integration (app open, interstitial, banner, native ads)
├── data/               # Data models, entities, local database
│   └── local/         # Room database (favorites persistence)
├── di/                 # Hilt dependency injection modules
├── firebase/           # Firebase services integration
├── manager/            # Business logic managers
├── navigation/         # Navigation routes and deep linking
├── network/            # Retrofit API services and interceptors
├── notification/       # FCM, WorkManager notification jobs
├── repository/         # Repository implementations
├── ui/                 # Jetpack Compose screens and components
│   ├── components/    # Reusable composables
│   └── theme/         # Material 3 theme
├── viewmodel/         # State management ViewModels
└── util/              # Utilities (performance, networking, workers)
```

### Key Architectural Patterns

**MVVM with Repository Pattern:**
- ViewModels manage state via `StateFlow<UiState<T>>` where `UiState` is sealed class: `Loading`, `Success<T>`, `Error`
- Repositories abstract data sources (API, Firebase, Room database)
- ViewModels are Hilt-injected with `@HiltViewModel`

**Navigation:**
- Jetpack Compose Navigation with sealed class routes in [navigation/Routes.kt](app/src/main/java/com/soccertips/predictx/navigation/Routes.kt)
- Deep linking support: `app://com.soccertips.predictx/fixture/{fixtureId}`
- Navigation stored in SharedPreferences when coming from notifications

**Dependency Injection:**
- [AppModule.kt](app/src/main/java/com/soccertips/predictx/di/AppModule.kt) provides singletons: Retrofit, Room, repositories
- [WorkManagerModule.kt](app/src/main/java/com/soccertips/predictx/di/WorkManagerModule.kt) configures WorkManager with HiltWorkerFactory

### Data Layer & Fallback Strategy

The app implements a sophisticated multi-tier fallback strategy in [PredictionRepository.kt](app/src/main/java/com/soccertips/predictx/repository/PredictionRepository.kt):

1. **Primary:** Retrofit API call
2. **On 403 error:** Firebase Realtime Database fallback
3. **On network error:** Exponential backoff retry (max 5 attempts)
4. **Last resort:** Cached fallback data (30-minute TTL)

**Network Layer:**
- Dual API configurations: Default API and FixtureDetailsService
- Custom OkHttp interceptors:
  - Cache interceptor with endpoint-specific TTLs (fixtures: 1h, predictions: 24h, standings: 10min)
  - `SocketTaggingInterceptor` for background execution
  - `DnsFailureInterceptor` for DNS recovery

**Local Persistence:**
- Room database for favorites ([FavoriteItem](app/src/main/java/com/soccertips/predictx/data/local/FavoriteItem.kt) entity)
- Migration history: v1→v2 (fixtureId Integer→String), v2→v3 (added completedTimestamp)

### AdMob Integration

AdMob implementation in [admob/](app/src/main/java/com/soccertips/predictx/admob/) with consent management:

**Ad Types:**
- **App Open Ads:** [AppOpenAdManager.kt](app/src/main/java/com/soccertips/predictx/admob/AppOpenAdManager.kt) - shows on app resume, device-aware (blacklists problematic manufacturers)
- **Interstitial Ads:** [InterstitialAdManager.kt](app/src/main/java/com/soccertips/predictx/admob/InterstitialAdManager.kt) - rate limited (5s minimum between loads), exponential backoff retry
- **Banner Ads:** `BannerAdView`, `CollapsibleBannerAdView`, `InlineBannerAdView`
- **Native Ads:** `NativeAdItem` - styled to match app design

**Consent Management:**
- UMP (User Messaging Platform) integration in [App.kt](app/src/main/java/com/soccertips/predictx/App.kt)
- Consent-aware ad loading
- Debug settings for EEA testing

**Safety Features:**
- Device manufacturer blacklist for full-screen ads (Huawei, Honor, Oppo, Vivo, Realme)
- Activity state validation before showing ads
- Global `AdStateManager` prevents overlapping full-screen ads

### Firebase Integration

**Services Used:**
- **Messaging:** FCM with exponential backoff token registration, placeholder token fallback
- **Realtime Database:** Fallback data source at `predictions/{categoryKey}/current/data`
- **Remote Config:** API configuration values with BuildConfig defaults
- **Analytics:** Event tracking (betting success, reviews, startup)
- **Crashlytics:** Error reporting with ad system debugging logs

**Initialization:**
- Deferred in [App.onCreate()](app/src/main/java/com/soccertips/predictx/App.kt) to avoid blocking startup
- Consent-aware (waits for UMP completion)
- 15-second timeout for Firebase reads

### Notification System

**Components:**
- [FirebaseMessagingService](app/src/main/java/com/soccertips/predictx/firebase/FirebaseMessagingService.kt) handles FCM messages
- WorkManager jobs: `DelayedNotificationWorker`, `BettingSuccessWorker`, `UpdateMatchNotificationWorker`, `DailyReminderWorker`
- Deep link routing from notifications to specific screens

**Flow:** FCM message → Extract data → Schedule WorkManager job → Post notification → User tap → Navigate via deep link

### Performance Optimizations

**Startup:**
- [StartupTimeTracker](app/src/main/java/com/soccertips/predictx/util/StartupTimeTracker.kt) records metrics
- [LazyInitManager](app/src/main/java/com/soccertips/predictx/manager/LazyInitManager.kt) defers non-essential initialization
- Splash screen remains visible during initialization

**Device-Aware Configuration:**
- [DevicePerformanceManager](app/src/main/java/com/soccertips/predictx/util/DevicePerformanceManager.kt) detects low-memory devices (< 3GB RAM)
- Deferred ad initialization on low-end devices
- [LowRamOptimizer](app/src/main/java/com/soccertips/predictx/util/LowRamOptimizer.kt) reduces resource usage
- WorkManager job cleanup prevents 100-job limit

## Development Guidelines

### Adding New Screens

1. Create ViewModel in [viewmodel/](app/src/main/java/com/soccertips/predictx/viewmodel/) with `@HiltViewModel`
2. Define state as `StateFlow<UiState<YourDataType>>`
3. Create Composable in [ui/](app/src/main/java/com/soccertips/predictx/ui/)
4. Add route to [Routes.kt](app/src/main/java/com/soccertips/predictx/navigation/Routes.kt)
5. Update [AppNavigation.kt](app/src/main/java/com/soccertips/predictx/navigation/AppNavigation.kt)

### State Management Pattern

```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class YourViewModel @Inject constructor(
    private val repository: YourRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<YourData>>(UiState.Loading)
    val uiState: StateFlow<UiState<YourData>> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Use repository to fetch data
        }
    }
}
```

### Theme Usage

Use theme colors via composition locals:
- `LocalCardColors.current` for card color variants
- `LocalCardElevation.current` for elevation variants
- Material 3 theme defined in [ui/theme/](app/src/main/java/com/soccertips/predictx/ui/theme/)

### Working with Repositories

Repositories are the single source of truth for data:
- Handle API calls, Firebase fallbacks, and local caching
- Implement retry logic with exponential backoff
- Return Flow or suspend functions
- Inject via Hilt in ViewModels

## Critical Files to Understand

1. [MainActivity.kt](app/src/main/java/com/soccertips/predictx/MainActivity.kt) - Entry point, permission handling
2. [App.kt](app/src/main/java/com/soccertips/predictx/App.kt) - Application initialization, Firebase, consent
3. [AppModule.kt](app/src/main/java/com/soccertips/predictx/di/AppModule.kt) - Dependency injection configuration
4. [AppNavigation.kt](app/src/main/java/com/soccertips/predictx/navigation/AppNavigation.kt) - Navigation structure
5. [PredictionRepository.kt](app/src/main/java/com/soccertips/predictx/repository/PredictionRepository.kt) - Data flow with fallback strategies

## Testing

- Unit tests use Mockk for mocking
- Room database migrations tested with `MigrationTestHelper`
- Firebase debug mode enabled in debug builds
- AdMob test device IDs available for testing

## Build Configuration

- **Min SDK:** 21
- **Target SDK:** 34
- **Compile SDK:** 35
- **Java Version:** 11
- **Kotlin Version:** From libs.versions.toml
- **KSP:** Used instead of KAPT for annotation processing
- **ProGuard:** Enabled in release builds with R8 full mode
- **Lint:** Baseline file at `lint-baseline.xml`, several checks disabled
