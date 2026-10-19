# Betting Success Notification Testing Guide

This guide explains how to test the complete betting success notification system, including FCM server notifications and WorkManager fallback.

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION DELIVERY                            │
├─────────────────────────────────────────────────────────────────────┤
│  PRIMARY: FCM Server (8-10 AM via Cloud Functions)                  │
│         └─→ FirebaseMessagingService.onMessageReceived()            │
│         └─→ handleBettingSuccessNotification()                      │
│         └─→ bettingSuccessChecker.markDateAsNotifiedFromFcm()       │
│                                                                     │
│  FALLBACK: WorkManager (Every 12 hours)                             │
│         └─→ BettingSuccessWorker.doWork()                           │
│         └─→ bettingSuccessChecker.checkYesterdaysBettingSuccess()   │
│         └─→ Checks SharedPreferences → Skips if already notified    │
└─────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

1. ✅ App installed on device/emulator
2. ✅ Notification permissions granted  
3. ✅ Network connectivity
4. ✅ Subscribed to `betting_success` FCM topic

---

## 1. Verify FCM Topic Subscription

### Check Subscription Status

```kotlin
// Add this debug method to any Fragment/Activity for testing
private fun checkTopicSubscription() {
    val prefs = requireContext().getSharedPreferences("fcm_preferences", Context.MODE_PRIVATE)
    val topics = prefs.getStringSet("subscribed_topics", emptySet())
    
    Timber.d("Subscribed topics: $topics")
    
    if (topics?.contains("betting_success") == true) {
        Toast.makeText(context, "✅ Subscribed to betting_success", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "❌ NOT subscribed to betting_success", Toast.LENGTH_LONG).show()
    }
}
```

### Force Subscribe (if needed)

```kotlin
// Call TokenRepository.subscribeToDefaultTopics() to subscribe
lifecycleScope.launch {
    tokenRepository.subscribeToDefaultTopics()
}
```

### ADB Command to Check Logs

```bash
adb logcat | grep -E "subscribeToTopic|betting_success|FCM"
```

---

## 2. Test WorkManager Fallback (Local Testing)

### Option A: Use Manual Check (Immediate)

```kotlin
// Inject BettingSuccessScheduler in your test activity/fragment
@Inject
lateinit var bettingSuccessScheduler: BettingSuccessScheduler

// Call this to trigger immediate check
lifecycleScope.launch {
    // Check specific date (YYYY-MM-DD format)
    bettingSuccessScheduler.checkImmediately("2026-02-05")
    
    // Or check today + yesterday
    bettingSuccessScheduler.checkImmediately()
}
```

### Option B: Use BettingSuccessChecker Directly

```kotlin
@Inject
lateinit var bettingSuccessChecker: BettingSuccessChecker

lifecycleScope.launch {
    // Force check for a specific date (removes from notified list first)
    bettingSuccessChecker.manualCheckForDate("2026-02-05")
}
```

### Option C: ADB WorkManager Test

```bash
# List all scheduled work
adb shell dumpsys jobscheduler | grep -A 20 "predictx"

# Force run WorkManager jobs
adb shell cmd jobscheduler run -f com.soccertips.predictx 1
```

---

## 3. Test FCM Notification (Firebase Console)

### Step-by-Step:

1. Go to **Firebase Console** → **Cloud Messaging**
2. Click **"New Campaign"** → **"Notifications"**
3. Select **"Send test message"**
4. Add your FCM token (from logs or debug menu)
5. Use **Custom Data** payload:

```json
{
  "type": "all_matches_won",
  "title": "🎉 Perfect Day: Today Tips!",
  "body": "All 5 matches won!",
  "date": "2026-02-05",
  "match_count": "5",
  "win_count": "5",
  "success_rate": "100",
  "category_name": "Today Tips",
  "category_url": "today",
  "matches": "Arsenal vs Chelsea: 2-1 ✅\nMan City vs Liverpool: 3-0 ✅",
  "summary": "Perfect day! All predictions correct."
}
```

6. Click **"Test"**

### Expected Result:
- Notification appears on device
- Date is marked as notified in SharedPreferences
- WorkManager fallback will skip this date

---

## 4. Test Cloud Function (Server-Side)

### Test Endpoint (if deployed)

```bash
# Call the test endpoint
curl "https://<region>-<project>.cloudfunctions.net/testBettingSuccess?date=2026-02-05"
```

### Local Testing with Firebase Emulator

```bash
cd functions
npm run serve

# Test locally
curl "http://localhost:5001/YOUR_PROJECT/us-central1/testBettingSuccess?date=2026-02-05"
```

---

## 5. Verify Duplicate Prevention

### Check SharedPreferences

```kotlin
// Add this debug method
private fun checkNotifiedDates() {
    val prefs = requireContext().getSharedPreferences("betting_success_checker", Context.MODE_PRIVATE)
    val notifiedDates = prefs.getStringSet("notified_dates", emptySet())
    
    Timber.d("Notified dates: $notifiedDates")
    Toast.makeText(context, "Notified: $notifiedDates", Toast.LENGTH_LONG).show()
}
```

### Clear Notified Dates (for retesting)

```kotlin
private fun clearNotifiedDates() {
    val prefs = requireContext().getSharedPreferences("betting_success_checker", Context.MODE_PRIVATE)
    prefs.edit {
        remove("notified_dates")
    }
    Toast.makeText(context, "Cleared notified dates", Toast.LENGTH_SHORT).show()
}
```

### ADB Commands

```bash
# View SharedPreferences
adb shell run-as com.soccertips.predictx cat /data/data/com.soccertips.predictx/shared_prefs/betting_success_checker.xml

# Clear SharedPreferences (requires debuggable app)
adb shell run-as com.soccertips.predictx rm /data/data/com.soccertips.predictx/shared_prefs/betting_success_checker.xml
```

---

## 6. View Logs

### Filter for Betting Success

```bash
# All betting success related logs
adb logcat | grep -E "BettingSuccess|betting_success|all_matches_won"

# Specific components
adb logcat | grep "BettingSuccessChecker"
adb logcat | grep "BettingSuccessWorker"
adb logcat | grep "FirebaseMessagingService"
```

### Expected Log Output

```
D/BettingSuccessChecker: Starting betting success check worker
D/BettingSuccessChecker: Category 'Today Tips' achieved perfect results for 2026-02-05
D/BettingSuccessChecker: Analysis for 2026-02-05: Total=5, WithResults=5, Wins=5, Losses=0, Rate=100%
I/BettingSuccessChecker: Sent congratulations notification for perfect betting day in 'Today Tips': 2026-02-05
```

---

## 7. Integration Test Flow

### Complete Test Scenario

1. **Clear previous state:**
   ```kotlin
   clearNotifiedDates()
   ```

2. **Verify topic subscription:**
   ```kotlin
   checkTopicSubscription()
   ```

3. **Send FCM test message** (via Firebase Console)

4. **Verify notification received:**
   - Check notification drawer
   - Check logs for `handleBettingSuccessNotification`

5. **Verify duplicate prevention:**
   ```kotlin
   checkNotifiedDates() // Should show the date
   ```

6. **Trigger WorkManager fallback:**
   ```kotlin
   bettingSuccessScheduler.checkImmediately("2026-02-05")
   ```

7. **Verify no duplicate notification:**
   - Check logs for "Already notified for date: 2026-02-05, skipping check"

---

## 8. Quick Verification Checklist

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Check FCM topic subscription | `betting_success` in subscribed topics |
| 2 | Send FCM test message | Notification appears |
| 3 | Check SharedPreferences | Date marked as notified |
| 4 | Run manual check for same date | Logs: "Already notified...skipping" |
| 5 | Clear notified dates | SharedPreferences cleared |
| 6 | Run manual check again | Notification appears (if data meets criteria) |

---

## 9. Debugging Common Issues

### No Notification Received

1. **Check notification permissions:**
   ```kotlin
   NotificationManagerCompat.from(context).areNotificationsEnabled()
   ```

2. **Check notification channel:**
   ```kotlin
   val channel = notificationManager.getNotificationChannel("betting_success_channel")
   Timber.d("Channel importance: ${channel?.importance}")
   ```

3. **Verify FCM token is valid:**
   ```kotlin
   FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
       Timber.d("FCM Token: $token")
   }
   ```

### Duplicate Notifications

1. Check if `markDateAsNotifiedFromFcm()` is being called
2. Verify SharedPreferences is updating correctly
3. Check if WorkManager is running before FCM arrives

### WorkManager Not Running

```bash
# Check Work status
adb shell dumpsys activity services | grep WorkManager
```

---

## 10. Production Monitoring

### Firebase Analytics Events (Optional)

Add custom events to track notification success:

```kotlin
// In BettingSuccessChecker.sendCongratulationsNotification()
FirebaseAnalytics.getInstance(context).logEvent("betting_success_notification") {
    param("category", categoryResult.categoryName)
    param("date", date)
    param("win_count", categoryResult.analysis.winningMatches.toLong())
}
```

### Crashlytics Custom Logs

```kotlin
FirebaseCrashlytics.getInstance().log("Sent betting success notification for $date")
```

---

## Summary

The betting success notification system has:

1. **Primary Delivery:** FCM server sends at 8-10 AM (requires Cloud Functions deployment)
2. **Fallback Delivery:** WorkManager checks every 12 hours
3. **Duplicate Prevention:** SharedPreferences tracks notified dates
4. **Testing:** Use `manualCheckForDate()` or Firebase Console for testing

