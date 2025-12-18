# Google UMP Implementation Checklist

Use this checklist to ensure your Google UMP consent implementation is complete and working.

## Phase 1: Code Implementation ✅ COMPLETE

- [x] Add consent state flags (`isConsentInitialized`, `isConsentComplete`)
- [x] Implement `initializeConsent()` with debug settings
- [x] Implement `loadAndShowConsentFormIfRequired()`
- [x] Add `initializeMobileAdsIfReady()` helper
- [x] Add `resetConsent()` function
- [x] Add `canShowAds()` check function
- [x] Update consent initialization in lifecycle
- [x] Prevent MobileAds init before consent
- [x] Add consent checks to InterstitialAd
- [x] Add consent checks to RewardedAd
- [x] Add consent checks to AppOpenAd
- [x] Add efficient `canShowAdsWithConsent()` helper

## Phase 2: Configuration (ACTION REQUIRED)

### A. Get Your Test Device ID
- [ ] Run the app once
- [ ] Check Logcat/Android Studio logs
- [ ] Look for message: "Use RequestConfiguration.Builder().setTestDeviceIds..."
- [ ] Copy the device ID (format: "33BE2250B43518CCDA7DE426D04EE231")
- [ ] Add to `App.kt` line ~415:
  ```kotlin
  .addTestDeviceHashedId("YOUR_DEVICE_ID_HERE")
  ```

### B. AdMob Console Setup
- [ ] Go to https://apps.admob.google.com/
- [ ] Navigate to "Privacy & messaging"
- [ ] Click "Create"
- [ ] Select your app
- [ ] Choose "GDPR" message type
- [ ] Configure message:
  - [ ] Select template (recommended: "Consent" template)
  - [ ] Customize text if needed
  - [ ] Select ad partners
  - [ ] Configure purposes
- [ ] Save and publish the message
- [ ] Wait 15-30 minutes for propagation

### C. Funding Choices (Optional but Recommended)
- [ ] Go to Funding Choices (linked from AdMob)
- [ ] Create new message
- [ ] Link to your app
- [ ] Configure GDPR settings
- [ ] Publish

## Phase 3: UI Implementation (CHOOSE ONE)

### Option A: Compose (Recommended for modern apps)
- [ ] Copy code from `CONSENT_RESET_EXAMPLE.md` → Option 1
- [ ] Add `PrivacySettingsSection()` to settings screen
- [ ] Test the UI
- [ ] Verify reset works

### Option B: Traditional Views
- [ ] Copy code from `CONSENT_RESET_EXAMPLE.md` → Option 2
- [ ] Create `fragment_privacy_settings.xml`
- [ ] Create `PrivacySettingsFragment.kt`
- [ ] Add fragment to navigation
- [ ] Test the UI

### Option C: Simple Menu Item
- [ ] Copy code from `CONSENT_RESET_EXAMPLE.md` → Option 3
- [ ] Add menu item to settings
- [ ] Implement reset dialog
- [ ] Test functionality

## Phase 4: Privacy Policy

- [ ] Create/update privacy policy
- [ ] Add section: "Advertising"
  - [ ] Mention Google AdMob usage
  - [ ] Explain personalized ads
  - [ ] Mention data collection
- [ ] Add section: "User Consent"
  - [ ] Explain consent requirement (EEA/UK)
  - [ ] Describe consent process
  - [ ] Explain how to change preferences
- [ ] Add section: "Your Rights"
  - [ ] Right to access data
  - [ ] Right to delete data
  - [ ] Right to withdraw consent
- [ ] Link to Google's privacy policy
- [ ] Add your contact information
- [ ] Host privacy policy online
- [ ] Add privacy policy link to app:
  - [ ] In settings screen
  - [ ] In about screen
  - [ ] In Google Play Store listing

## Phase 5: Testing

### Debug Testing (EEA Simulation)
- [ ] Ensure debug mode is enabled
- [ ] Clear app data
- [ ] Launch app
- [ ] **Expected:** Consent form appears
- [ ] Grant consent
- [ ] **Expected:** Ads can load
- [ ] Check logs for "Consent: OBTAINED"
- [ ] Restart app
- [ ] **Expected:** No consent form (already granted)
- [ ] Test consent reset from settings
- [ ] **Expected:** Consent form appears again

### Debug Testing (Non-EEA)
- [ ] Change debug geography to `DEBUG_GEOGRAPHY_NOT_EEA`
- [ ] Clear app data
- [ ] Launch app
- [ ] **Expected:** No consent form
- [ ] **Expected:** Ads can load
- [ ] Check logs for "Consent: NOT_REQUIRED"

### Production Testing (Your Device)
- [ ] Build release APK/AAB
- [ ] Install on test device
- [ ] Clear app data
- [ ] Launch app
- [ ] **Expected:** Consent form appears (if in EEA)
- [ ] Test consent grant
- [ ] Test consent denial
- [ ] Test consent reset
- [ ] Verify ads respect consent status

### Multi-Device Testing
- [ ] Test on Android 9 (API 28)
- [ ] Test on Android 10 (API 29)
- [ ] Test on Android 11 (API 30)
- [ ] Test on Android 12+ (API 31+)
- [ ] Test on tablet
- [ ] Test landscape orientation
- [ ] Test with VPN (simulate different regions)

## Phase 6: Monitoring Setup

### Firebase Crashlytics
- [ ] Verify Crashlytics is initialized
- [ ] Check for consent-related logs:
  - [ ] "Consent: OBTAINED"
  - [ ] "Consent: REQUIRED"
  - [ ] "Consent: NOT_REQUIRED"
  - [ ] "Consent: Manual reset by user"

### Analytics (Optional)
- [ ] Track consent form impressions
- [ ] Track consent grant rate
- [ ] Track consent denial rate
- [ ] Track ad performance pre/post consent

### Timber Logs
- [ ] Enable Timber in debug
- [ ] Monitor logs with tag "Consent:"
- [ ] Verify all consent steps are logged
- [ ] Check for any error messages

## Phase 7: Pre-Release Validation

### Code Review
- [ ] All consent checks in place
- [ ] Error handling comprehensive
- [ ] State management correct
- [ ] No race conditions
- [ ] Proper activity lifecycle handling

### Compliance Review
- [ ] GDPR requirements met
- [ ] Privacy policy complete
- [ ] User can control consent
- [ ] Data usage transparent
- [ ] Consent persists correctly

### UX Review
- [ ] Consent form is user-friendly
- [ ] Reset option is easy to find
- [ ] Privacy policy is accessible
- [ ] App doesn't block without consent (just ads)
- [ ] Clear communication about ads

## Phase 8: Deployment

### Pre-Production
- [ ] Remove or comment test device IDs (for release)
- [ ] Set appropriate debug geography (or remove for production)
- [ ] Verify ProGuard/R8 rules include UMP SDK
- [ ] Build release AAB
- [ ] Test release build

### Play Store
- [ ] Update app description (mention ad-supported)
- [ ] Link to privacy policy
- [ ] Complete "Advertising" section
- [ ] Complete "Data safety" section
- [ ] Mention consent management
- [ ] Upload release AAB
- [ ] Submit for review

### Post-Launch
- [ ] Monitor consent metrics
- [ ] Watch for crash reports related to consent
- [ ] Check ad fill rates
- [ ] Monitor user feedback
- [ ] Be ready to iterate

## Phase 9: Ongoing Maintenance

### Regular Checks (Monthly)
- [ ] Check UMP SDK for updates
- [ ] Review AdMob console settings
- [ ] Monitor consent grant rates
- [ ] Check for policy changes
- [ ] Update privacy policy if needed

### Quarterly Reviews
- [ ] Review consent flow UX
- [ ] Update documentation
- [ ] Check compliance with new regulations
- [ ] Analyze consent metrics
- [ ] Optimize consent message if needed

## Quick Reference

### Important Files Modified
- `app/src/main/java/com/soccertips/predictx/App.kt`
- `app/src/main/java/com/soccertips/predictx/admob/ads.kt`
- `app/src/main/java/com/soccertips/predictx/admob/AppOpenAdManager.kt`

### Documentation Files
- `GOOGLE_UMP_IMPLEMENTATION.md` - Complete guide
- `CONSENT_RESET_EXAMPLE.md` - UI examples
- `UMP_IMPLEMENTATION_SUMMARY.md` - What changed
- `UMP_CHECKLIST.md` - This file

### Key Logs to Monitor
```
"Consent: Requesting consent info update..."
"Consent: Status = [status]"
"Consent: Can request ads = [true/false]"
"Consent: Process completed"
"Consent: OBTAINED"
```

### Support Resources
- Google UMP SDK Docs: https://developers.google.com/admob/ump/android/quick-start
- AdMob Help: https://support.google.com/admob
- GDPR Compliance: https://support.google.com/admob/answer/9999955

---

## Current Status

**Phase 1:** ✅ Complete  
**Phase 2:** ⏳ Pending (Add device ID, configure AdMob)  
**Phase 3:** ⏳ Pending (Implement reset UI)  
**Phase 4:** ⏳ Pending (Update privacy policy)  
**Phase 5:** ⏳ Pending (Testing)  

**Next Action:** Configure your test device ID and AdMob console

---

**Last Updated:** October 24, 2025  
**Checklist Version:** 1.0
