# A/B Test Quick Start Guide

## 🚀 Quick Setup (5 minutes)

### Step 1: Firebase Remote Config Setup
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Navigate to **Remote Config**
3. Click **Add parameter**
4. Enter:
   - **Parameter key**: `category_ad_strategy`
   - **Default value**: `rewarded`
   - **Description**: Controls category ad strategy (rewarded vs interstitial)
5. Click **Publish changes**

### Step 2: Create A/B Test
1. Go to **A/B Testing** in Firebase
2. Click **Create experiment**
3. Select **Remote Config** as experiment type
4. Fill in:
   - **Name**: Category Ad Strategy
   - **App**: Select your app
   - **Target**: % of users to include (start with 10-20%)
5. Click **Next**
6. Set experiment variants:
   - **Baseline**: `category_ad_strategy = "rewarded"`
   - **Variant A**: `category_ad_strategy = "interstitial"`
7. Set goals:
   - Primary: User engagement / Session duration
   - Secondary: Ad revenue (from AdMob)
8. **Start experiment**

### Step 3: Monitor Results
- Wait 1-2 weeks for statistical significance
- Check Firebase A/B Testing dashboard
- Compare:
  - User retention
  - Ad revenue
  - Session metrics

## 📊 What Each Variant Does

### Variant A: Rewarded Ads (Control)
```
User taps locked category
     ↓
Dialog: "Watch ad to unlock"
     ↓
User watches 15-30s video ad
     ↓
Category unlocked for 24 hours
     ↓
User accesses category
     ↓
User views items
     ↓
User presses back
     ↓
Returns to categories (no ad on back press)
```

**When user sees ad**: Before accessing locked content
**Frequency**: Once per 24 hours per locked category
**User action**: Explicit (must click "Watch Ad")
**Back press ad**: No interstitial on back press

### Variant B: Interstitial Ads (Test)
```
User taps any category
     ↓
Immediately navigate to category
     ↓
User views items
     ↓
User presses back button
     ↓
Full-screen interstitial ad shown
     ↓
Returns to categories
```

**When user sees ad**: When pressing back from ItemsListScreen
**Frequency**: Every time user presses back
**User action**: Automatic (no permission needed)
**Back press ad**: Yes - interstitial on every back press

## 🧪 Testing Locally

### Test Rewarded Variant
```kotlin
// In your Firebase Console, set for your device:
category_ad_strategy = "rewarded"

// Or in code (debug only):
Firebase.remoteConfig.setDefaultsAsync(
    mapOf("category_ad_strategy" to "rewarded")
)
```

1. Launch app
2. Tap any locked category
3. See "Watch Ad to Unlock" dialog
4. Click "Watch Ad"
5. Ad plays → category unlocks

### Test Interstitial Variant
```kotlin
// In your Firebase Console, set for your device:
category_ad_strategy = "interstitial"

// Or in code (debug only):
Firebase.remoteConfig.setDefaultsAsync(
    mapOf("category_ad_strategy" to "interstitial")
)
```

1. Launch app
2. Tap any category
3. Immediately navigate to content
4. Press back to return
5. Interstitial ad shows

## 🎯 Success Metrics

Track in Firebase Analytics:

| Metric | Rewarded (Expected) | Interstitial (Expected) |
|--------|-------------------|------------------------|
| Ad Impressions/User | Lower (1-3) | Higher (5-10) |
| Ad Revenue/User | Higher eCPM | Lower eCPM, but more impressions |
| Categories Accessed | May be lower | May be higher |
| Session Duration | Potentially higher | Potentially lower |
| D1 Retention | Baseline | Compare |

## 🔧 Troubleshooting

### Remote Config not updating?
```kotlin
// Check logs for:
"Ad Strategy loaded: [value]"

// Force fetch (debug only):
Firebase.remoteConfig.fetchAndActivate()
```

### Ads not showing?
1. Check AdMob test device setup
2. Verify consent is given (UMP SDK)
3. Check logs: `adb logcat -s CategoriesScreen`
4. Ensure ad units are active in AdMob

### Want to change variant distribution?
1. Firebase Console > A/B Testing
2. Select your experiment
3. Click "Modify"
4. Adjust traffic allocation
5. Save changes

## 🏁 Deciding the Winner

After 2-4 weeks:

### Choose Rewarded If:
- ✅ Higher ad revenue per user
- ✅ Better or same retention
- ✅ Positive user feedback
- ✅ Good ad completion rate (>70%)

### Choose Interstitial If:
- ✅ Total revenue is 20%+ higher
- ✅ More categories accessed
- ✅ Same or better retention
- ✅ Higher session duration

### Roll Out Winner:
1. Firebase Console > Remote Config
2. Update default value to winner
3. Stop A/B test
4. Monitor for 1 week
5. Done! 🎉

## 📝 Notes

- **Default**: If Remote Config fails, app uses rewarded ads (safer)
- **Gradual**: Start with 10% of users, then expand
- **Reversible**: Can switch back anytime via Remote Config
- **No code deploy**: Changes apply instantly via Firebase

## 🆘 Emergency Rollback

If something goes wrong:

1. Firebase Console > Remote Config
2. Change `category_ad_strategy` to `"rewarded"`
3. Click **Publish changes**
4. All users switch to rewarded strategy immediately
5. No app update needed!

---

For detailed documentation, see: `AB_TEST_CATEGORY_ADS.md`
