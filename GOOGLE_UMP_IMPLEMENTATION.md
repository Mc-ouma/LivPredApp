# Google UMP (User Messaging Platform) Implementation Guide

## Overview
This document explains the Google UMP consent management implementation in the LivPredApp. UMP is required for GDPR compliance and managing user consent for personalized ads.

## Implementation Details

### 1. Consent Initialization

**Location:** `App.kt` - `initializeConsent()`

The consent flow is initialized once when the first activity is created:

```kotlin
private fun initializeConsent(activity: Activity)
```

**Key Features:**
- ✅ Initialized only once (prevents multiple consent requests)
- ✅ Debug mode for testing (EEA geography simulation)
- ✅ Proper error handling
- ✅ Detailed logging for debugging

### 2. Debug Configuration

For **development and testing**, the implementation includes debug settings:

```kotlin
if (BuildConfig.DEBUG) {
    val debugSettings = ConsentDebugSettings.Builder(activity)
        .setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA)
        // Add your test device ID here:
        // .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID")
        .build()
    paramsBuilder.setConsentDebugSettings(debugSettings)
}
```

**To get your test device ID:**
1. Run the app
2. Check Logcat for: `"Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("DEVICE_ID"))"`
3. Add the device ID to the debug settings

### 3. Consent Form Flow

**Location:** `App.kt` - `loadAndShowConsentFormIfRequired()`

The consent form is shown automatically if required:

```kotlin
private fun loadAndShowConsentFormIfRequired(activity: Activity)
```

**Consent Statuses:**
- `OBTAINED` - User has provided consent
- `REQUIRED` - Consent is needed but not yet provided
- `NOT_REQUIRED` - User is not in a region requiring consent
- `UNKNOWN` - Status not yet determined

### 4. MobileAds Initialization

MobileAds SDK is **only initialized after consent is complete**:

```kotlin
private fun initializeMobileAdsIfReady()
```

**Conditions for initialization:**
- ✅ Consent process is complete (`isConsentComplete = true`)
- ✅ MobileAds not already initialized
- ✅ Valid activity context available

### 5. Consent Checking Before Showing Ads

**Location:** `ads.kt` - `canShowAdsWithConsent()`

All ad types check consent before displaying:

```kotlin
private fun canShowAdsWithConsent(activity: Activity): Boolean
```

**Checked in:**
- ✅ Interstitial Ads (`InterstitialAdManager`)
- ✅ Rewarded Ads (`RewardedAdManager`)
- ✅ App Open Ads (`AppOpenAdManager`)
- ✅ Banner Ads (handled by MobileAds SDK automatically)

### 6. Consent Reset (Privacy Options)

**Location:** `App.kt` - `resetConsent()`

Users can reset their consent choices:

```kotlin
fun resetConsent(activity: Activity)
```

**When to use:**
- Privacy settings screen
- User requests to change consent preferences
- Testing different consent scenarios

### 7. State Management

The implementation tracks consent state with flags:

```kotlin
private var isConsentInitialized = false  // Consent flow started
private var isConsentComplete = false      // Consent flow finished
```

**Benefits:**
- Prevents multiple consent requests
- Prevents race conditions with MobileAds initialization
- Clear state tracking for debugging

## Testing Guide

### Test EEA User (Requires Consent)

1. Add your test device ID in `App.kt`:
```kotlin
.addTestDeviceHashedId("YOUR_TEST_DEVICE_ID")
```

2. Set debug geography to EEA:
```kotlin
.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA)
```

3. Run the app - consent form should appear

### Test Non-EEA User

Change debug geography:
```kotlin
.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
```

### Reset Consent for Testing

```kotlin
// In your settings/debug screen
(application as App).resetConsent(this)
```

## Google AdMob Console Setup

### 1. Enable User Messaging Platform

1. Go to [AdMob Console](https://apps.admob.google.com/)
2. Navigate to **Privacy & messaging**
3. Create a **GDPR message**
4. Configure message settings:
   - Message type: Consent
   - Publishers: Select your app
   - Design: Choose a template

### 2. Configure Funding Choices (CMP)

1. Go to **Funding Choices** in AdMob
2. Create a new message for your app
3. Select **GDPR** as the regulation
4. Configure ad partners and purposes
5. Publish the message

### 3. Test the Implementation

Use the **Test Ads** feature:
1. Add test device IDs in AdMob console
2. Enable test mode in your app (already configured)
3. Verify consent form appears

## Privacy Policy Requirements

Your privacy policy **must** include:

1. **Information about ads:**
   - "We use Google AdMob to serve ads"
   - "AdMob may collect and use data for personalized advertising"

2. **User consent:**
   - "Users in the EEA/UK will be asked for consent"
   - "Users can change consent preferences anytime"

3. **Data collection:**
   - List data types collected
   - Explain how data is used
   - Link to Google's privacy policy

4. **User rights:**
   - How to access/delete data
   - How to withdraw consent
   - Contact information

## Code Integration Checklist

- [x] UMP SDK dependency added to `build.gradle`
- [x] Consent initialization in `App.kt`
- [x] Debug configuration for testing
- [x] Consent check before ad loading
- [x] Consent check before ad display
- [x] Reset consent function
- [x] State management (prevent multiple inits)
- [x] Error handling and logging
- [x] Activity lifecycle awareness
- [ ] Add consent reset UI in settings
- [ ] Add test device ID for your device
- [ ] Configure message in AdMob console
- [ ] Update privacy policy

## Common Issues & Solutions

### Issue: Consent form not showing

**Solutions:**
1. Check if debug geography is set to EEA
2. Verify test device ID is correct
3. Clear app data and restart
4. Check AdMob console message is published

### Issue: Ads not showing after consent

**Solutions:**
1. Check logs for `canRequestAds()` result
2. Verify MobileAds initialized after consent
3. Check for other blocking conditions (device safety, activity state)

### Issue: Multiple consent requests

**Solutions:**
1. Verify `isConsentInitialized` flag is working
2. Check if consent is being called in multiple activities
3. Review activity lifecycle callbacks

### Issue: Consent status unclear in logs

**Solutions:**
1. Enable verbose logging in debug builds
2. Check Timber logs with tag "Consent:"
3. Use Firebase Crashlytics logs for production

## Monitoring & Analytics

### Key Metrics to Track

1. **Consent rate:** % of users who provide consent
2. **Consent form impressions:** How often form is shown
3. **Consent errors:** Failed consent requests
4. **Ad fill rate:** Before/after consent implementation

### Firebase Crashlytics Logging

The implementation logs key events:
```kotlin
FirebaseCrashlytics.getInstance().log("Consent: OBTAINED")
FirebaseCrashlytics.getInstance().log("Consent: REQUIRED")
FirebaseCrashlytics.getInstance().log("Consent: NOT_REQUIRED")
```

Check Crashlytics console for consent-related events.

## Compliance Notes

### GDPR (EU/EEA/UK)
- ✅ Consent is required for personalized ads
- ✅ Users can withdraw consent anytime
- ✅ Clear privacy policy required

### CCPA (California)
- Limited data collection for users who opt-out
- Privacy policy must explain data usage

### COPPA (Children's apps)
- No personalized ads for children under 13
- Use `setTagForUnderAgeOfConsent(true)` if needed

## Additional Resources

- [Google UMP SDK Documentation](https://developers.google.com/admob/ump/android/quick-start)
- [GDPR Compliance Guide](https://support.google.com/admob/answer/9999955)
- [AdMob EU Consent](https://support.google.com/admob/answer/10113207)
- [Funding Choices Help](https://support.google.com/fundingchoices/)

## Support & Questions

For implementation questions:
1. Check Timber logs with "Consent:" tag
2. Review Firebase Crashlytics for consent events
3. Test with different debug geographies
4. Verify AdMob console configuration

---

**Last Updated:** October 24, 2025
**Implementation Version:** 1.0
**Tested With:** Google UMP SDK 2.x, AdMob SDK 22.x
