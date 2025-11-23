# A/B Testing: Rewarded Ads vs Interstitial Ads for Category Access

## Overview
This A/B test compares two different ad strategies for category access:
- **Variant A (Rewarded)**: Users watch a rewarded ad to unlock a category for 24 hours
- **Variant B (Interstitial)**: Users access categories freely, but see an interstitial ad when returning to the categories screen

## Implementation

### 1. Firebase Remote Config Setup

#### Remote Config Parameter
- **Parameter Name**: `category_ad_strategy`
- **Type**: String
- **Default Value**: `"rewarded"`
- **Possible Values**:
  - `"rewarded"` - Show rewarded ad dialog before accessing locked categories
  - `"interstitial"` - Show interstitial ad when returning to categories screen

#### Setting Up in Firebase Console

1. Go to **Firebase Console** > **Remote Config**
2. Add a new parameter:
   - **Key**: `category_ad_strategy`
   - **Default value**: `rewarded`
   - **Description**: Controls which ad strategy to use for category access

### 2. Setting Up A/B Test in Firebase

#### Option A: Using Firebase A/B Testing (Recommended)

1. Go to **Firebase Console** > **A/B Testing** > **Create experiment**
2. Choose **Remote Config** as the experiment type
3. Configure experiment:
   - **Name**: "Category Ad Strategy Test"
   - **Description**: "Compare rewarded ads vs interstitial ads for category access"
   - **Target**: Your app with appropriate user percentage
   
4. Set variants:
   - **Baseline (Control)**: `category_ad_strategy = "rewarded"`
   - **Variant A**: `category_ad_strategy = "interstitial"`
   
5. Set goals (choose relevant metrics):
   - Primary: **User engagement** (e.g., session duration, categories viewed)
   - Secondary: **Ad revenue** (track from AdMob)
   - Secondary: **Retention** (D1, D7 retention rates)
   
6. Configure audience:
   - **Traffic allocation**: 50% control, 50% variant (or custom split)
   - **Targeting**: All users or specific user properties
   
7. Start the experiment

#### Option B: Manual Remote Config Conditions

If you want more control or don't want to use A/B Testing:

1. Go to **Firebase Console** > **Remote Config**
2. Click **Add condition** for `category_ad_strategy`
3. Create conditions based on:
   - **User in random percentile**: Split users 50/50
   - **User property**: Target specific user segments
   - **App version**: Test on specific versions first
   
Example conditions:
```
Condition 1: "Test Group A - Rewarded"
- User in random percentile: 0-49
- Value: "rewarded"

Condition 2: "Test Group B - Interstitial"  
- User in random percentile: 50-99
- Value: "interstitial"
```

### 3. Code Implementation

The following components have been updated:

#### RemoteConfigRepository
```kotlin
companion object {
    const val CATEGORY_AD_STRATEGY_KEY = "category_ad_strategy"
    const val AD_STRATEGY_REWARDED = "rewarded"
    const val AD_STRATEGY_INTERSTITIAL = "interstitial"
}

suspend fun getCategoryAdStrategy(): String {
    fetchAndActivate()
    return remoteConfig.getString(CATEGORY_AD_STRATEGY_KEY)
        .ifBlank { AD_STRATEGY_REWARDED }
}
```

#### CategoriesViewModel
- Added `adStrategy` StateFlow that fetches from Remote Config
- Automatically loads strategy on initialization

#### CategoriesScreen
- Supports RewardedAdManager (InterstitialAdManager already in ItemsListScreen)
- Preloads rewarded ad when using rewarded strategy
- Different navigation behavior based on strategy:
  - **Rewarded**: Shows dialog, user watches ad, unlocks category for 24h
  - **Interstitial**: Navigate immediately, existing interstitial ad shows on back press in ItemsListScreen

## Behavior Details

### Rewarded Ad Strategy (Variant A)
1. User taps locked category
2. Dialog appears: "Watch Ad to Unlock"
3. User watches rewarded ad
4. Category unlocked for 24 hours
5. User navigates to category
6. User views items in ItemsListScreen
7. User presses back button
8. **Returns to categories (no ad on back press)**
9. Subsequent visits within 24h don't require ad

**Pros:**
- Clear value exchange (watch ad → get access)
- Better user consent
- Higher engagement with ad content
- Category remains unlocked for 24h
- No ads on back navigation

**Cons:**
- Friction before accessing content
- Some users may abandon at dialog
- Lower overall ad impressions
- Only applies to locked categories

### Interstitial Ad Strategy (Variant B)
1. User taps any category (no lock)
2. Navigates immediately to category
3. User views items in ItemsListScreen
4. User presses back button
5. **Interstitial ad is shown (full screen)**
6. User returns to categories screen
7. Process repeats for every back navigation

**Pros:**
- No friction to access content
- More ad impressions (every back press)
- Smoother initial user flow
- Higher category exploration
- Applies to all categories equally

**Cons:**
- Ads may feel intrusive on back press
- No explicit value exchange
- Could impact retention if too frequent
- May annoy power users who browse multiple categories
- Interrupts navigation flow

## Monitoring & Analytics

### Key Metrics to Track

#### User Engagement
- **Categories viewed per session**
- **Session duration**
- **Items viewed per category**
- **Return rate to categories screen**

#### Ad Performance
- **Ad impressions** (compare total between variants)
- **Ad revenue** (eCPM, total revenue)
- **Ad fill rate**
- **Ad completion rate** (for rewarded ads)

#### User Behavior
- **Category unlock rate** (for rewarded variant)
- **Dialog abandonment rate** (for rewarded variant)
- **User drop-off points**
- **Categories accessed per user**

#### Retention
- **D1 retention** (1-day retention)
- **D7 retention** (7-day retention)
- **D30 retention** (30-day retention)

### Firebase Analytics Events

You may want to add custom events to track:

```kotlin
// Log when user sees rewarded ad dialog
Firebase.analytics.logEvent("category_rewarded_dialog_shown") {
    param("category_name", category.name)
}

// Log when user watches rewarded ad
Firebase.analytics.logEvent("category_rewarded_ad_completed") {
    param("category_name", category.name)
}

// Log when interstitial is shown
Firebase.analytics.logEvent("category_interstitial_shown") {
    param("returning_from", "category_view")
}

// Log ad strategy variant
Firebase.analytics.setUserProperty("ad_strategy_variant", adStrategy)
```

### AdMob Reporting

1. Go to **AdMob Console** > **Reporting**
2. Group by:
   - **Ad unit** (to separate rewarded vs interstitial)
   - **App version** (if testing on specific versions)
3. Compare:
   - **Impressions**
   - **Match rate**
   - **eCPM**
   - **Estimated earnings**

## Testing Locally

To test both variants during development:

### Method 1: Override Remote Config (Recommended)
```kotlin
// In your Application class or MainActivity onCreate
if (BuildConfig.DEBUG) {
    val remoteConfig = Firebase.remoteConfig
    val configSettings = remoteConfigSettings {
        minimumFetchIntervalInSeconds = 0 // Fetch immediately in debug
    }
    remoteConfig.setConfigSettingsAsync(configSettings)
    
    // Force a specific variant for testing
    val overrides = mapOf(
        "category_ad_strategy" to "interstitial" // or "rewarded"
    )
    remoteConfig.setDefaultsAsync(overrides)
}
```

### Method 2: Test Both Variants
1. Test Rewarded Variant:
   - Set Remote Config to `"rewarded"`
   - Verify dialog shows before category access
   - Verify ad plays and unlocks category
   - Verify 24h unlock persists

2. Test Interstitial Variant:
   - Set Remote Config to `"interstitial"`
   - Verify immediate navigation to categories
   - Navigate to category and back
   - Verify interstitial shows on return

## Rollout Strategy

### Phase 1: Staged Rollout (Week 1-2)
- Start with 10% of users in test (5% each variant)
- Monitor for crashes or major issues
- Check initial metrics

### Phase 2: Expand Test (Week 3-4)
- Increase to 50% of users (25% each variant)
- Continue monitoring metrics
- Look for statistical significance

### Phase 3: Full Rollout (Week 5+)
- Analyze results and choose winner
- Roll out winning variant to 100% of users
- Update default value in Remote Config

### Decision Criteria

Choose Rewarded Ads if:
- User retention is better or same
- Revenue per user is higher
- User feedback is more positive
- Ad completion rates are good (>70%)

Choose Interstitial Ads if:
- Total ad revenue is significantly higher (>20%)
- User engagement is better (more categories viewed)
- Retention rates are same or better
- Session duration increases

## Fallback & Safety

The implementation includes several safety measures:

1. **Default Value**: If Remote Config fails, defaults to `"rewarded"`
2. **Graceful Degradation**: If ad managers are null, navigation still works
3. **Error Handling**: Ad load failures don't block category access
4. **Logging**: Extensive Timber logging for debugging

## Rolling Back

If you need to roll back or switch strategies:

1. **Immediate**: Update Remote Config default to preferred variant
2. **Gradual**: Adjust condition percentiles
3. **Emergency**: Set all users to `"rewarded"` (most user-friendly)

## Next Steps

1. ✅ Code implementation complete
2. ⏳ Set up Remote Config parameter in Firebase Console
3. ⏳ Create A/B test experiment in Firebase
4. ⏳ Add custom analytics events (optional)
5. ⏳ Test both variants locally
6. ⏳ Start with 10% rollout
7. ⏳ Monitor metrics for 2-4 weeks
8. ⏳ Analyze results and choose winner
9. ⏳ Roll out to 100%

## Questions or Issues?

- Check Firebase Console for Remote Config status
- Review Logcat for "CategoriesScreen" and "RemoteConfig" tags
- Verify both ad units are properly configured in AdMob
- Ensure consent is properly set up (UMP SDK)
