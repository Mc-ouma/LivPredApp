# A/B Test Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Firebase Console                         │
├─────────────────────────────────────────────────────────────┤
│  Remote Config Parameter: category_ad_strategy              │
│                                                              │
│  ┌────────────────┐              ┌─────────────────┐       │
│  │  Variant A     │              │   Variant B     │       │
│  │  "rewarded"    │              │ "interstitial"  │       │
│  │   (50% users)  │              │   (50% users)   │       │
│  └────────────────┘              └─────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ fetch & activate
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   RemoteConfigRepository                     │
├─────────────────────────────────────────────────────────────┤
│  getCategoryAdStrategy(): String                            │
│    ├─ Returns: "rewarded" or "interstitial"                │
│    └─ Default: "rewarded" (if fetch fails)                 │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   CategoriesViewModel                        │
├─────────────────────────────────────────────────────────────┤
│  val adStrategy: StateFlow<String>                          │
│    └─ Loads strategy on init                               │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    CategoriesScreen                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐                                        │
│  │ RewardedAdMgr   │  (Interstitial is in ItemsListScreen) │
│  └─────────────────┘                                        │
│           │                                                  │
│     Based on adStrategy                                     │
│           │                                                  │
│       ┌───┴────────────────┐                               │
│       ▼                     ▼                               │
│  If "rewarded"      If "interstitial"                       │
│  └─ Preload          └─ Navigate directly                  │
│     rewarded ad          Interstitial loads                │
│                          in ItemsListScreen                 │
└─────────────────────────────────────────────────────────────┘
           │                            │
           ▼                            ▼
┌──────────────────┐         ┌───────────────────┐
│ ItemsListScreen  │         │  ItemsListScreen  │
│ (Rewarded mode)  │         │ (Interstitial)    │
│                  │         │                   │
│ Back press →     │         │ Back press →      │
│ Direct return    │         │ Show interstitial │
└──────────────────┘         └───────────────────┘
```

## User Flow Comparison

### Variant A: Rewarded Ads Flow

```
┌─────────────────────┐
│   Categories        │
│   Screen            │
└──────────┬──────────┘
           │
           │ User taps locked category
           ▼
┌─────────────────────┐
│  Show Dialog:       │
│  "Watch ad to       │
│   unlock [Name]"    │
│                     │
│  [Watch Ad] [Cancel]│
└──────────┬──────────┘
           │
           │ User clicks "Watch Ad"
           ▼
┌─────────────────────┐
│  Rewarded Ad Plays  │
│  (15-30 seconds)    │
│                     │
│  [Skip after 5s →]  │
└──────────┬──────────┘
           │
           │ Ad completed
           ▼
┌─────────────────────┐
│  ✓ Category Unlocked│
│  (for 24 hours)     │
└──────────┬──────────┘
           │
           │ Auto-navigate
           ▼
┌─────────────────────┐
│  Category Items     │
│  Screen             │
└─────────────────────┘

Next 24 hours:
  └─ Access freely (no ad)

After 24 hours:
  └─ Show dialog again
```

### Variant B: Interstitial Ads Flow

```
┌─────────────────────┐
│   Categories        │
│   Screen            │
└──────────┬──────────┘
           │
           │ User taps ANY category (no lock)
           ▼
┌─────────────────────┐
│  Navigate           │
│  immediately        │
│  (no dialog)        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  ItemsListScreen    │
│                     │
│  [User views items] │
└──────────┬──────────┘
           │
           │ User presses back button
           ▼
┌─────────────────────┐
│  Full-Screen        │
│  Interstitial Ad    │
│  (5-10 seconds)     │
│  [From ItemsList]   │
│  [Close X]          │
└──────────┬──────────┘
           │
           │ Ad dismissed
           ▼
┌─────────────────────┐
│   Categories        │
│   Screen            │
│   (returned)        │
└─────────────────────┘

Every back press from ItemsListScreen:
  └─ Show interstitial ad
```

## Ad Loading Strategy

### Rewarded Ads (Variant A)

```
Screen Composition
      │
      ├─ LaunchedEffect(rewardedAdManager)
      │     │
      │     └─ Check if ad ready
      │           │
      │           ├─ Yes → Do nothing
      │           └─ No → loadRewardedAd()
      │
User taps locked category
      │
      └─ Check if ad ready
            │
            ├─ Yes → Show immediately
            └─ No → Show loading
                      │
                      ├─ Load ad
                      ├─ Wait 3 seconds
                      └─ Show error if failed
```

### Interstitial Ads (Variant B)

```
Screen Composition (CategoriesScreen)
      │
      └─ No special setup needed
            │
User taps any category
      │
      └─ Navigate immediately to ItemsListScreen
            │
            └─ ItemsListScreen loads with InterstitialAdManager
                  │
                  └─ Ad preloads automatically
                        │
User presses back button
      │
      └─ navigateBackWithAd() in ItemsListScreen
            │
            ├─ Check if ad ready
            ├─ Yes → Show interstitial ad
            │          └─ On dismiss → popBackStack()
            └─ No → Direct popBackStack()
```

## Data Flow

```
Firebase Remote Config
      │
      ├─ Fetched on app start
      ├─ Cached locally
      └─ Re-fetched every 30 min
            │
            ▼
RemoteConfigRepository.getCategoryAdStrategy()
            │
            ├─ Returns: "rewarded" or "interstitial"
            └─ Default: "rewarded" on error
                  │
                  ▼
CategoriesViewModel.adStrategy: StateFlow<String>
                  │
                  ├─ Emits value to UI
                  └─ UI observes with collectAsStateWithLifecycle()
                        │
                        ▼
CategoriesScreen
                        │
                        ├─ Preloads correct ad type
                        └─ Shows appropriate UI
                              │
                              ├─ Rewarded: Dialog before access
                              └─ Interstitial: Ad on return
```

## Ad Manager Injection

```
@EntryPoint
@InstallIn(ActivityComponent::class)
interface CategoriesRewardedAdManagerEntryPoint {
    fun rewardedAdManager(): RewardedAdManager
    fun interstitialAdManager(): InterstitialAdManager
}
                │
                │
                ▼
CategoriesScreen @Composable
                │
                ├─ val activity = LocalActivity.current
                │
                ├─ EntryPointAccessors.fromActivity(activity, ...)
                │         │
                │         ├─ rewardedAdManager()
                │         └─ interstitialAdManager()
                │
                └─ Pass to CategoriesContent()
```

## Category Unlock Persistence (Rewarded Only)

```
SharedPreferences: "category_unlock_prefs"

Key Format: "unlock_[category_url]"
Value: Unix timestamp (Long)

Example:
  "unlock_/api/categories/premium" = 1699876543000

Check unlock status:
  currentTime - unlockTimestamp < 24 hours
        │
        ├─ Yes → Category is unlocked
        └─ No → Category is locked

When ad completed:
  Save: currentTime to SharedPreferences
  
Clear after:
  24 hours (86,400,000 ms)
```

## A/B Test Metrics Pipeline

```
User Interaction
      │
      ├─ Firebase Analytics (automatic)
      │     └─ screen_view, user_engagement, etc.
      │
      ├─ Custom Events (optional)
      │     ├─ category_rewarded_dialog_shown
      │     ├─ category_rewarded_ad_completed
      │     └─ category_interstitial_shown
      │
      └─ AdMob Reporting
            └─ Impressions, eCPM, revenue
                  │
                  ▼
Firebase A/B Testing Dashboard
                  │
                  ├─ Compare variants
                  ├─ Statistical significance
                  └─ Declare winner
                        │
                        ▼
Update Remote Config default
                        │
                        └─ Roll out to 100%
```

## Error Handling & Fallbacks

```
Remote Config Fetch
      │
      ├─ Success → Use fetched value
      │
      └─ Failure
            │
            └─ Use default: "rewarded"
                  │
Ad Loading
      │
      ├─ Success → Show ad
      │
      └─ Failure
            │
            ├─ Rewarded: Show error dialog
            │              └─ User can retry or cancel
            │
            └─ Interstitial: Skip ad
                               └─ User continues normally

Navigation
      │
      └─ Always works regardless of ads
            │
            └─ No ad should block user access
```

## State Management

```
CategoriesScreen State:
├─ uiState: UiState<List<Category>>
│    └─ From ViewModel
├─ announcements: List<Announcement>
│    └─ From ViewModel
├─ adStrategy: String
│    └─ From ViewModel (Remote Config)
│
CategoriesContent Local State:
├─ showRewardAdDialog: Boolean
├─ pendingCategory: Category?
├─ isLoadingAd: Boolean
├─ showAdNotAvailableDialog: Boolean
└─ hasNavigatedAway: Boolean
     └─ Tracks if user navigated away
          (for interstitial timing)
```

## Testing Strategy

```
Development
├─ Test Device Setup
│    ├─ Add test device ID to AdMob
│    └─ Use test ad units
│
├─ Remote Config Override
│    ├─ Set minimumFetchInterval = 0
│    └─ Force specific variant
│
└─ Manual Testing
     ├─ Test rewarded flow
     ├─ Test interstitial flow
     └─ Test fallbacks

Staging (10% rollout)
├─ Monitor crash logs
├─ Check ad fill rates
└─ Verify metrics collection

Production (50% rollout)
├─ Run for 2-4 weeks
├─ Monitor Firebase A/B Testing
└─ Compare variants

Rollout Winner (100%)
└─ Update Remote Config default
```

---

This architecture ensures:
- ✅ Seamless A/B testing without code changes
- ✅ Instant rollback via Remote Config
- ✅ Safe fallbacks for errors
- ✅ No blocking of user access
- ✅ Easy monitoring and metrics
