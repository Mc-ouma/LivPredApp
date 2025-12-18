# Reward Ad Category Implementation

## Overview
Implemented a feature that allows certain categories to require users to watch a rewarded ad before accessing them. **Following best practices, categories remain unlocked for 24 hours after watching the ad**, preventing user frustration from repeatedly watching ads.

## Changes Made

### 1. Category Data Model (`Category.kt`)
- **Added field**: `requiresRewardAd: Boolean = false`
- Categories can now be marked as requiring a reward ad to access
- Defaults to `false` for backward compatibility

### 2. Firebase Repository (`FirebaseRepository.kt`)
- **Updated**: `getCategories()` method to read `requiresRewardAd` field from Firebase
- The field is optional in Firebase - if not present, it defaults to `false`

### 3. Category Card UI (`CategoryCard.kt`)
- **Added**: Lock icon indicator for categories that require reward ad
- Lock icon appears **only when locked** (not yet unlocked)
- Uses Material Icons `Lock` icon with the category's theme color
- Lock disappears after user watches ad and unlocks category

### 4. Categories Screen (`CategoriesScreen.kt`)
- **Added**: Reward ad flow integration with 24-hour unlock mechanism
- **Added**: `CategoriesRewardedAdManagerEntryPoint` for dependency injection
- **Added**: SharedPreferences to persist unlock timestamps
- **Added**: Alert dialog explaining the 24-hour unlock period
- **Added**: State management for pending category navigation
- **Flow**:
  1. User clicks on a reward-gated category
  2. Check if already unlocked (within 24 hours)
  3. If unlocked → Navigate immediately
  4. If locked → Show alert dialog
  5. User watches rewarded ad
  6. Category unlocked for 24 hours
  7. Navigate to category

### 5. String Resources (`strings.xml`)
- **Added**:
  - `watch_ad_to_unlock` - Dialog title
  - `watch_ad_unlock_24h` - Dialog message explaining 24-hour unlock
  - `watch_ad` - Confirm button text

## How to Use

### In Firebase Database

To mark a category as requiring a reward ad, add the `requiresRewardAd` field to the category object:

```json
{
  "categories": {
    "category1": {
      "name": "Premium Tips",
      "url": "https://example.com/premium",
      "iconResId": "ic_star_24",
      "colorHex": "#FFD700",
      "requiresRewardAd": true
    },
    "category2": {
      "name": "Free Tips",
      "url": "https://example.com/free",
      "iconResId": "ic_trending_up_24",
      "colorHex": "#4CAF50"
      // requiresRewardAd omitted = defaults to false
    }
  }
}
```

### Visual Indicators
- Categories with `requiresRewardAd: true` and **not unlocked** display a lock icon 🔒
- Once unlocked (within 24 hours), the lock icon disappears
- Regular categories always display normally

### User Flow
1. **Regular Category**: Click → Navigate immediately
2. **Locked Reward Category**: 
   - Click → Show dialog → Watch ad → Unlock for 24h → Navigate
3. **Unlocked Reward Category** (within 24h):
   - Click → Navigate immediately (no ad required)

## Technical Details

### Unlock Persistence
- Uses `SharedPreferences` with key: `category_unlock_prefs`
- Stores unlock timestamp per category: `unlock_{categoryUrl}`
- Checks if current time is within 24 hours of unlock timestamp
- Automatic expiry after 24 hours

### Unlock Duration
```kotlin
val unlockDuration = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
```

**Easy to customize**: Change the duration by modifying this value:
- 12 hours: `12 * 60 * 60 * 1000L`
- 48 hours: `48 * 60 * 60 * 1000L`
- 7 days: `7 * 24 * 60 * 60 * 1000L`

### Dependency Injection
Uses Hilt's `EntryPoint` pattern to inject `RewardedAdManager` into the composable:
- `CategoriesRewardedAdManagerEntryPoint` interface
- Installed in `ActivityComponent`
- Accessed via `EntryPointAccessors.fromActivity()`

### Ad Manager Integration
- Uses existing `RewardedAdManager` from `ads.kt`
- Handles ad loading, showing, and callbacks
- Includes error handling for ad failures
- Safe device checks for problematic manufacturers

### State Management
- `showRewardAdDialog`: Controls dialog visibility
- `pendingCategory`: Stores the category waiting for reward
- `isCategoryUnlocked()`: Checks unlock status from SharedPreferences
- `unlockCategory()`: Saves unlock timestamp to SharedPreferences

## Benefits

1. **Better UX**: Users don't have to watch ads repeatedly ✅
2. **Monetization**: Generate revenue from premium category access
3. **User-Friendly**: 24-hour unlock period respects user time
4. **Flexible**: Easy to toggle any category between free and reward-gated
5. **Clear UX**: Lock icon and dialog clearly indicate reward requirement
6. **Persistent**: Unlock status survives app restarts
7. **Safe**: Includes all existing ad safety checks and consent verification

## Best Practices Implemented

✅ **24-Hour Unlock**: Industry standard for rewarded content  
✅ **Visual Feedback**: Lock disappears when unlocked  
✅ **Clear Communication**: Dialog explains unlock duration  
✅ **Persistent Storage**: SharedPreferences survives app lifecycle  
✅ **Graceful Expiry**: Automatic re-lock after 24 hours  

## Future Enhancements

Potential improvements:
- Add countdown timer showing time remaining until re-lock
- Track rewarded category access in analytics
- Configurable unlock duration per category (from Firebase)
- Reward points/credits system
- Multiple reward tiers (Bronze: 12h, Silver: 24h, Gold: unlimited)
- Push notification when unlock is about to expire

## Testing

To test:
1. Update a category in Firebase to set `requiresRewardAd: true`
2. Launch app and navigate to Categories screen
3. Observe lock icon 🔒 on the reward-gated category
4. Click the category
5. Verify dialog mentions "24 hours"
6. Click "Watch Ad" and complete the ad
7. Verify lock icon disappears
8. Verify navigation to category works
9. Close and reopen app
10. Verify category is still unlocked (no lock icon)
11. Click category - should navigate without showing ad
12. (Optional) Change device time +24 hours to test re-lock

## Notes

- RewardedAdManager must be properly initialized in MainActivity
- Ensure reward ad unit ID is configured in `strings.xml`
- Test on various devices, especially those in the problematic device list
- Consider UMP consent requirements for showing ads
- SharedPreferences data persists until app is uninstalled
- Clear app data to reset all unlocks for testing
