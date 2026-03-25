# AGENTS.md

## Project Overview

**AI ScoreCast** (`com.soccertips.predictx`) - Android football predictions app built with Jetpack Compose, MVVM + Repository pattern, Hilt DI, and Firebase services.

## Architecture

**Data Flow:**
```
API/Firebase → Repository → ViewModel (StateFlow<UiState<T>>) → Compose UI
```

**Layer Structure:**
```
com.soccertips.predictx/
├── admob/              # AdMob integration (app open, interstitial, banner, native ads)
├── data/               # Data models, entities, local database
│   └── local/         # Room database (favorites persistence)
├── di/                 # Hilt dependency injection modules
├── firebase/           # Firebase services integration
├── network/            # Retrofit API services and interceptors
├── notification/       # FCM, WorkManager notification jobs
├── repository/         # Repository implementations
├── ui/                 # Jetpack Compose screens and components
│   ├── components/    # Reusable composables
│   └── theme/         # Material 3 theme
├── util/              # Utilities (performance, networking, workers)
└── viewmodel/         # State management ViewModels
```

**Key Architectural Patterns:**
- **MVVM with Repository:** ViewModels manage `StateFlow<UiState<T>>`, Repositories abstract data sources (API, Firebase, Room)
- **Navigation:** Jetpack Compose Navigation with sealed class routes in `Routes.kt`, deep linking support
- **Dependency Injection:** Hilt provides singletons via `AppModule.kt` and `WorkManagerModule.kt`

## Essential Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew installDebug         # Install to device
./gradlew test                  # Unit tests
./gradlew lint                  # Lint checks (baseline in lint-baseline.xml)
```

## Configuration

Create `dot.env` in project root with:
```properties
API_KEY=your_key
API_HOST=your_host
API_BASE_URL=https://...
DAILY_BONUS_BASE_URL=https://...
API_BASE_URL_VALUE=https://...
```

## Critical Patterns

### State Management
All ViewModels use this pattern:
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### Adding New Screens
1. Create ViewModel with `@HiltViewModel` in `viewmodel/`
2. Define state as `StateFlow<UiState<YourData>>`
3. Create Composable in `ui/`
4. Add sealed class route to `navigation/Routes.kt`
5. Wire in `AppNavigation.kt`

### AdMob Safety
- `AdStateManager` prevents overlapping full-screen ads
- Device manufacturer blacklist for full-screen ads (Huawei, Honor, Oppo, Vivo, Realme)
- Always validate Activity state before showing ads
- Check UMP consent via `canShowAdsWithConsent()`

### Theme Colors
Use composition locals for consistent styling:
- `LocalCardColors.current`
- `LocalCardElevation.current`
- Material 3 theme defined in [ui/theme/](app/src/main/java/com/soccertips/predictx/ui/theme/)

### Repository Fallback Strategy
See `PredictionRepository.kt`:
1. Check preloaded cache
2. Try API call
3. On 403 → Firebase Realtime Database fallback
4. On network error → exponential backoff retry (max 5 attempts)
5. Last resort → cached fallback (30-min TTL)

**Network Layer Details:**
- Dual API configurations: Default API and FixtureDetailsService
- Custom OkHttp interceptors:
  - Cache interceptor with endpoint-specific TTLs (fixtures: 1h, predictions: 24h, standings: 10min)
  - `SocketTaggingInterceptor` for background execution
  - `DnsFailureInterceptor` for DNS recovery
- Room database for favorites ([FavoriteItem](app/src/main/java/com/soccertips/predictx/data/local/FavoriteItem.kt) entity)
- Database migrations: v1→v2 (fixtureId Integer→String), v2→v3 (added completedTimestamp)


## Tech Stack

- **Compose BOM:** 2026.02.01 | **Min SDK:** 26 | **Target SDK/Compile SDK:** 36
- **DI:** Hilt (KSP, not KAPT)
- **DB:** Room with migrations (v1→v2→v3)
- **Network:** Retrofit 3.0.0 + OkHttp 5.3.2 with custom interceptors
- **Firebase:** Messaging, Realtime DB, Remote Config, Analytics, Crashlytics

## Background Jobs: AlarmManager & WorkManager

### AlarmManager (Exact Timing)
Used for time-critical notifications that must fire at exact times, even in Doze mode.

**`DailyReminderAlarmScheduler`** - Schedules daily reminders:
- Morning reminder at 9:00 AM (`MORNING_REQUEST_CODE = 1001`)
- Afternoon reminder at 3:30 PM (`AFTERNOON_REQUEST_CODE = 1002`)
- Uses `setExactAndAllowWhileIdle()` for Doze mode compatibility
- On Android 12+, requires `SCHEDULE_EXACT_ALARM` permission check via `canScheduleExactAlarms()`

**`NotificationScheduler`** - Match reminder notifications:
- Android 12+ (API 31+): Uses AlarmManager for exact timing
- Below Android 12: Falls back to WorkManager
- Schedules 15 minutes before match time
- Intent action: `com.soccertips.predictx.ACTION_MATCH_REMINDER`

### WorkManager (Deferrable/Periodic Tasks)
Used for background tasks that can be deferred and need constraints.

| Worker | Purpose | Schedule |
|--------|---------|----------|
| `BettingSuccessWorker` | Check for perfect betting days | Every 12 hours (periodic) |
| `DailyReminderWorker` | Post localized reminder notifications | Triggered by AlarmManager |
| `UpdateMatchNotificationWorker` | Update live match notification with score | One-time, during match |
| `DelayedNotificationWorker` | Show match reminder (pre-Android 12) | One-time with delay |
| `RescheduleWorker` | Re-schedule notifications after reboot | One-time on boot |
| `FavoriteCleanupWorker` | Clean old favorites from Room DB | Periodic |

**Key patterns:**
- All workers use `@HiltWorker` with `@AssistedInject`
- Network-dependent tasks use `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`
- Unique work names prevent duplicates: `enqueueUniqueWork()` / `enqueueUniquePeriodicWork()`
- Job cleanup in `App.kt` prevents hitting the 100-job WorkManager limit

**Adding a new Worker:**
```kotlin
@HiltWorker
class MyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MyRepository  // Hilt-injected
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Return Result.success(), Result.failure(), or Result.retry()
    }
}
```

## FCM Server-Side Notifications

### Betting Success via FCM (Recommended for Time-Controlled Delivery)
Server-side FCM is preferred for time-sensitive notifications (e.g., 8-10 AM) because:
- Guaranteed delivery timing controlled by server
- No dependency on device Doze mode or WorkManager delays
- Centralized logic for calculating betting success

**FCM Data Payload Format** (handled in `FirebaseMessagingService.kt`):
```json
{
  "to": "<device_token_or_topic>",
  "data": {
    "type": "all_matches_won",
    "title": "🎉 Perfect Betting Day!",
    "body": "All 5 matches won in Today Tips!",
    "date": "2026-02-04",
    "match_count": "5",
    "win_count": "5",
    "success_rate": "100",
    "category_name": "Today Tips",
    "category_url": "today",
    "matches": "Team A vs Team B: 2-1 ✅\nTeam C vs Team D: 1-0 ✅",
    "summary": "Perfect day! All predictions correct."
  }
}
```

**Server-Side Implementation (Cloud Functions / Node.js):**
```javascript
// Schedule with Cloud Scheduler to run at 8:00 AM daily
exports.sendBettingSuccessNotifications = functions.pubsub
  .schedule('0 8 * * *')  // 8:00 AM daily
  .timeZone('Africa/Nairobi')  // User's timezone
  .onRun(async (context) => {
    const yesterday = getYesterdayDate();
    
    // 1. Fetch categories and check for perfect results
    const perfectCategories = await checkPerfectBettingDays(yesterday);
    
    // 2. For each category with 100% success, send FCM
    for (const category of perfectCategories) {
      const message = {
        topic: 'betting_success',  // Or individual tokens
        data: {
          type: 'all_matches_won',
          title: `🎉 Perfect Day: ${category.name}!`,
          body: `All ${category.winCount} matches won!`,
          date: yesterday,
          match_count: String(category.matchCount),
          win_count: String(category.winCount),
          success_rate: '100',
          category_name: category.name,
          category_url: category.url
        }
      };
      await admin.messaging().send(message);
    }
  });
```

**Client Handling** (already implemented in `FirebaseMessagingService.kt`):
- Notification type `"all_matches_won"` triggers `handleBettingSuccessNotification()`
- Opens `MainActivity` with action `ACTION_VIEW_BETTING_SUCCESS`
- Extras include: `betting_date`, `match_count`, `win_count`, `success_rate`

**Hybrid Approach (Fallback with Duplicate Prevention):**
Keep `BettingSuccessWorker` as fallback for:
- Users not subscribed to FCM topic
- Network issues during FCM delivery
- Server downtime

**Duplicate Prevention:** When FCM notification is received, `FirebaseMessagingService` calls `bettingSuccessChecker.markDateAsNotifiedFromFcm(date, categoryUrl)` which marks the date in SharedPreferences. When `BettingSuccessWorker` runs later, it checks this flag and skips already-notified dates.

## Performance Considerations

- `DevicePerformanceManager` detects low-memory devices (<3GB RAM)
- `LazyInitManager` defers non-essential initialization
- `StartupTimeTracker` for cold start metrics
- WorkManager job cleanup to avoid 100-job limit


