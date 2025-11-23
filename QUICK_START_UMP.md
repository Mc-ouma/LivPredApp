# 🚀 Quick Start: Google UMP Implementation

**Time to complete:** 15-30 minutes

## Step 1: Add Your Test Device ID (5 minutes)

1. **Run the app once** on your device
2. **Open Logcat** in Android Studio
3. **Search for:** `"UMP"` or `"Ads"` 
4. **Look for a message like:**
   ```
   Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
   ```
5. **Copy the device ID** (the long string in quotes)
6. **Open:** `app/src/main/java/com/soccertips/predictx/App.kt`
7. **Find line ~415** (in `initializeConsent()` method)
8. **Uncomment and replace:**
   ```kotlin
   // .addTestDeviceHashedId("YOUR_TEST_DEVICE_ID")
   ```
   **With:**
   ```kotlin
   .addTestDeviceHashedId("33BE2250B43518CCDA7DE426D04EE231")  // Use YOUR device ID
   ```

## Step 2: Test the Implementation (10 minutes)

### Test 1: EEA User (Consent Required)

1. **Ensure this is set** in `App.kt` (should already be):
   ```kotlin
   .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
   ```

2. **Clear app data:**
   - Settings → Apps → Your App → Storage → Clear Data

3. **Run the app**

4. **Expected result:**
   - ✅ Consent form appears on launch
   - ✅ You can accept or decline
   - ✅ After accepting, ads can load
   - ✅ On next launch, no consent form (already granted)

5. **Check logs:**
   ```
   Consent: Requesting consent info update...
   Consent: Form is available
   Consent: Status = OBTAINED
   Consent: Can request ads = true
   ```

### Test 2: Non-EEA User (No Consent Required)

1. **Change geography** in `App.kt`:
   ```kotlin
   .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
   ```

2. **Clear app data** again

3. **Run the app**

4. **Expected result:**
   - ✅ No consent form appears
   - ✅ Ads can load immediately
   - ✅ App works normally

5. **Check logs:**
   ```
   Consent: Status = NOT_REQUIRED
   Consent: Can request ads = true
   ```

### Test 3: Consent Reset (If UI Implemented)

1. **Go to app settings** (if you've added the reset UI)
2. **Tap "Change Consent Preferences"**
3. **Confirm reset**
4. **Expected result:**
   - ✅ Consent form appears again
   - ✅ Can re-grant or deny consent

## Step 3: Configure AdMob Console (10 minutes)

### Quick Setup

1. **Go to:** https://apps.admob.google.com/

2. **Navigate to:** Privacy & messaging (in sidebar)

3. **Click:** "Create" button

4. **Select:**
   - Your app from dropdown
   - "GDPR" as message type
   - "Consent" as purpose

5. **Choose template:**
   - Recommended: "Consent" template
   - Click "Next"

6. **Configure:**
   - Ad partners: Select all or customize
   - Purposes: Keep defaults or customize
   - Click "Next"

7. **Customize (optional):**
   - Edit message text if desired
   - Preview on different devices
   - Click "Next"

8. **Review and publish:**
   - Review settings
   - Click "Publish"
   - ⚠️ Wait 15-30 minutes for changes to propagate

### Verify Setup

After 30 minutes:

1. **Clear app data** on your device
2. **Disable WiFi** (use cellular)
3. **Run the app**
4. **Expected:** Real consent form appears (not test mode)

## Step 4: Add Consent Reset UI (Optional, 5 minutes)

### Quick Compose Implementation

Add this to your settings screen:

```kotlin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.soccertips.predictx.App

@Composable
fun ConsentResetButton() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Change Consent Preferences")
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Reset Consent") },
            text = { Text("This will show the consent form again.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val activity = context as? android.app.Activity
                    activity?.let {
                        (context.applicationContext as App).resetConsent(it)
                    }
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}
```

## Step 5: Verify Everything Works

### Checklist

- [ ] Test device ID added
- [ ] Consent form appears in test mode (EEA)
- [ ] Consent form doesn't appear in test mode (Non-EEA)
- [ ] Ads load after granting consent
- [ ] Ads don't load without consent (in EEA)
- [ ] AdMob console configured and published
- [ ] Logs show correct consent status
- [ ] No crashes or errors

### Common Issues

#### Issue: Consent form not appearing

**Solutions:**
1. Check test device ID is correct
2. Verify debug geography is set to EEA
3. Clear app data completely
4. Check internet connection
5. Wait for AdMob console changes to propagate (30 min)

#### Issue: Consent form appears but crashes

**Solutions:**
1. Check Activity is not finishing
2. Verify window is attached
3. Check logs for specific error
4. Ensure UMP SDK is latest version

#### Issue: Ads still don't show after consent

**Solutions:**
1. Check `canRequestAds()` returns true
2. Verify MobileAds initialized after consent
3. Check for other blocking conditions
4. Review Timber logs with tag "Consent:"

## What's Next?

### For Development
- ✅ Implementation is complete
- 📝 Test thoroughly with different scenarios
- 🎨 Consider adding consent reset UI
- 📱 Test on multiple devices

### For Production
1. **Update privacy policy** (see `GOOGLE_UMP_IMPLEMENTATION.md`)
2. **Remove test device IDs** (or comment them out)
3. **Test release build**
4. **Monitor consent metrics** after launch

### For More Information

- **Complete guide:** `GOOGLE_UMP_IMPLEMENTATION.md`
- **UI examples:** `CONSENT_RESET_EXAMPLE.md`
- **Full checklist:** `UMP_CHECKLIST.md`
- **Summary:** `UMP_IMPLEMENTATION_SUMMARY.md`

## Need Help?

### Debug Logs to Check

```
Tag: "Consent:"
- "Consent: Requesting consent info update..."
- "Consent: Debug mode enabled with EEA geography"
- "Consent: Form is available"
- "Consent: Process completed"
- "Consent: Status = [status]"
- "Consent: Can request ads = [true/false]"
- "Consent: Proceeding with MobileAds initialization"
```

### Key Methods to Review

- `App.kt` → `initializeConsent()`
- `App.kt` → `loadAndShowConsentFormIfRequired()`
- `App.kt` → `initializeMobileAdsIfReady()`
- `ads.kt` → `canShowAdsWithConsent()`

### Support Resources

- **Google UMP Docs:** https://developers.google.com/admob/ump/android/quick-start
- **AdMob Help:** https://support.google.com/admob
- **Test Mode Guide:** https://developers.google.com/admob/ump/android/test

---

## Summary

✅ **Code:** Implementation complete  
⏳ **Config:** Add device ID + configure AdMob  
⏳ **Test:** Verify consent flow works  
⏳ **UI:** Add reset option (optional)  
⏳ **Policy:** Update privacy policy  

**Estimated time to production:** 1-2 hours (mostly testing and policy updates)

**Current Status:** Ready for testing with your device ID and AdMob configuration

---

**Quick Start Version:** 1.0  
**Last Updated:** October 24, 2025
