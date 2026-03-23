# Server-Side FCM Betting Success Notifications

This document describes how to implement time-controlled betting success notifications (8-10 AM delivery) using Firebase Cloud Functions and FCM.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        SERVER SIDE                               │
├─────────────────────────────────────────────────────────────────┤
│  Cloud Scheduler (8:00 AM daily)                                │
│         ↓                                                        │
│  Cloud Function: checkBettingSuccess                            │
│         ↓                                                        │
│  1. Fetch yesterday's matches from Firebase RTDB/API            │
│  2. Calculate win/loss for each category                        │
│  3. For categories with 100% wins, send FCM                     │
│         ↓                                                        │
│  FCM Data Message → Topic: betting_success                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT SIDE                               │
├─────────────────────────────────────────────────────────────────┤
│  FirebaseMessagingService.onMessageReceived()                   │
│         ↓                                                        │
│  type == "all_matches_won"                                      │
│         ↓                                                        │
│  handleBettingSuccessNotification() → Show notification         │
└─────────────────────────────────────────────────────────────────┘
```

## 1. Firebase Cloud Functions Implementation

### Install Dependencies

```bash
cd functions
npm install firebase-admin firebase-functions
```

### Cloud Function Code (`functions/index.js`)

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

/**
 * Scheduled function to check betting success and send notifications.
 * Runs daily at 8:00 AM in the specified timezone.
 */
exports.sendBettingSuccessNotifications = functions.pubsub
  .schedule('0 8 * * *')  // 8:00 AM daily (cron syntax)
  .timeZone('Africa/Nairobi')  // Adjust to your primary user timezone
  .onRun(async (context) => {
    console.log('Starting betting success check...');
    
    try {
      const yesterday = getYesterdayDate();
      console.log(`Checking betting results for: ${yesterday}`);
      
      // Get all categories
      const categories = await getCategories();
      
      // Check each category for perfect results
      const perfectCategories = [];
      
      for (const category of categories) {
        const result = await checkCategoryResults(category.url, yesterday);
        
        if (result.isPerfect && result.winCount >= 3) {
          perfectCategories.push({
            name: category.name,
            url: category.url,
            matchCount: result.matchCount,
            winCount: result.winCount,
            matches: result.matchDetails
          });
        }
      }
      
      console.log(`Found ${perfectCategories.length} categories with perfect results`);
      
      // Send notifications for each perfect category
      for (const category of perfectCategories) {
        await sendBettingSuccessNotification(category, yesterday);
      }
      
      return null;
    } catch (error) {
      console.error('Error in betting success check:', error);
      throw error;
    }
  });

/**
 * Get yesterday's date in YYYY-MM-DD format
 */
function getYesterdayDate() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return date.toISOString().split('T')[0];
}

/**
 * Fetch categories from Firebase Realtime Database
 */
async function getCategories() {
  const snapshot = await admin.database()
    .ref('categories')
    .once('value');
  
  if (!snapshot.exists()) {
    // Fallback categories
    return [
      { url: 'today', name: 'Today Tips' },
      { url: '1', name: 'Home Win' },
      { url: '2', name: 'Away Win' },
      { url: 'over', name: 'Over 2.5 Goals' },
      { url: 'gg', name: 'Both Teams Score' }
    ];
  }
  
  return snapshot.val();
}

/**
 * Check results for a specific category and date
 */
async function checkCategoryResults(categoryUrl, date) {
  try {
    // Fetch from Firebase RTDB
    const snapshot = await admin.database()
      .ref(`predictions/${categoryUrl}/current/data`)
      .once('value');
    
    if (!snapshot.exists()) {
      return { isPerfect: false, matchCount: 0, winCount: 0, matchDetails: '' };
    }
    
    const matches = snapshot.val().serverResponse || [];
    
    // Filter matches for the target date
    const dateMatches = matches.filter(m => m.mDate === date);
    
    let winCount = 0;
    let loseCount = 0;
    const matchDetails = [];
    
    for (const match of dateMatches) {
      const outcome = (match.outcome || '').toLowerCase();
      
      if (outcome === 'win') {
        winCount++;
        matchDetails.push(`✅ ${match.homeTeam} vs ${match.awayTeam}: ${match.result}`);
      } else if (outcome === 'lose') {
        loseCount++;
        matchDetails.push(`❌ ${match.homeTeam} vs ${match.awayTeam}: ${match.result}`);
      }
      // Skip matches without results
    }
    
    const matchCount = winCount + loseCount;
    const isPerfect = matchCount > 0 && loseCount === 0;
    
    return {
      isPerfect,
      matchCount,
      winCount,
      matchDetails: matchDetails.join('\n')
    };
  } catch (error) {
    console.error(`Error checking category ${categoryUrl}:`, error);
    return { isPerfect: false, matchCount: 0, winCount: 0, matchDetails: '' };
  }
}

/**
 * Send FCM notification for betting success
 */
async function sendBettingSuccessNotification(category, date) {
  const message = {
    topic: 'betting_success',  // Users must subscribe to this topic
    data: {
      type: 'all_matches_won',
      title: `🎉 Perfect Day: ${category.name}!`,
      body: `All ${category.winCount} matches won!`,
      date: date,
      match_count: String(category.matchCount),
      win_count: String(category.winCount),
      success_rate: '100',
      category_name: category.name,
      category_url: category.url,
      matches: category.matches,
      summary: `Perfect day! All ${category.winCount} predictions correct.`
    },
    android: {
      priority: 'high',
      ttl: 86400000  // 24 hours in milliseconds
    }
  };
  
  try {
    const response = await admin.messaging().send(message);
    console.log(`Notification sent for ${category.name}:`, response);
  } catch (error) {
    console.error(`Failed to send notification for ${category.name}:`, error);
  }
}

/**
 * HTTP endpoint for manual testing
 * Call: https://<region>-<project>.cloudfunctions.net/testBettingSuccess?date=2026-02-04
 */
exports.testBettingSuccess = functions.https.onRequest(async (req, res) => {
  const date = req.query.date || getYesterdayDate();
  
  try {
    const categories = await getCategories();
    const results = [];
    
    for (const category of categories) {
      const result = await checkCategoryResults(category.url, date);
      results.push({
        category: category.name,
        ...result
      });
    }
    
    res.json({ date, results });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

## 2. Deploy Cloud Functions

```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login and initialize
firebase login
firebase init functions

# Deploy
firebase deploy --only functions
```

## 3. Client-Side Topic Subscription

Add this to `App.kt` or during user onboarding:

```kotlin
// Subscribe to betting success topic
FirebaseMessaging.getInstance().subscribeToTopic("betting_success")
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Timber.d("Subscribed to betting_success topic")
        } else {
            Timber.e("Failed to subscribe to betting_success topic")
        }
    }
```

## 4. Already Implemented Client Handler

The client already handles `all_matches_won` notifications in `FirebaseMessagingService.kt`:

```kotlin
when (notificationType) {
    "all_matches_won" -> {
        handleBettingSuccessNotification(
            remoteMessage.data["title"] ?: "🎉 Perfect Betting Day!",
            remoteMessage.data["body"] ?: "All matches won today!",
            remoteMessage.data
        )
    }
    // ...
}
```

## 5. Hybrid Strategy with Duplicate Prevention

Keep `BettingSuccessWorker` as a fallback, but duplicates are prevented:

### How Duplicate Prevention Works

1. **FCM notification arrives** → `FirebaseMessagingService.handleBettingSuccessNotification()`
2. **Mark date as notified** → `bettingSuccessChecker.markDateAsNotifiedFromFcm(date, categoryUrl)`
3. **WorkManager runs later** → `BettingSuccessChecker.checkBettingSuccessForDate()` checks SharedPreferences
4. **Skip if already notified** → No duplicate notification sent

### Code Flow

```kotlin
// In FirebaseMessagingService.kt
private fun handleBettingSuccessNotification(...) {
    val date = data["date"] ?: ""
    val categoryUrl = data["category_url"] ?: ""
    
    // Mark this date as notified to prevent duplicates
    if (date.isNotEmpty()) {
        bettingSuccessChecker.markDateAsNotifiedFromFcm(date, categoryUrl)
    }
    
    // ... show notification
}

// In BettingSuccessChecker.kt - checkBettingSuccessForDate()
val notifiedDates = sharedPrefs.getStringSet(NOTIFIED_DATES_KEY, emptySet()) ?: emptySet()
if (notifiedDates.contains(dateString)) {
    Timber.d("Already notified for date: $dateString, skipping check")
    return@withContext  // Skip - FCM already sent notification
}
```

### Reduce WorkManager Frequency (Optional)

Since FCM is the primary delivery mechanism, you can reduce WorkManager frequency:

```kotlin
// In BettingSuccessScheduler.kt, reduce frequency since FCM is primary
val bettingCheckWork = PeriodicWorkRequestBuilder<BettingSuccessWorker>(
    24, TimeUnit.HOURS  // Changed from 12 to 24 hours - just a backup
)
```

## 6. Testing

### Test FCM Payload via Firebase Console

1. Go to Firebase Console → Cloud Messaging
2. Create new campaign → "Test on device"
3. Use this data payload:

```json
{
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
```

### Test Cloud Function Locally

```bash
cd functions
npm run serve

# Call the test endpoint
curl "http://localhost:5001/YOUR_PROJECT/us-central1/testBettingSuccess?date=2026-02-04"
```

## 7. Timezone Considerations

For global users, consider:

1. **Single timezone (simplest):** Send at 8 AM in your primary user timezone
2. **Multiple schedules:** Create separate Cloud Scheduler jobs for different timezones
3. **User preference:** Store user timezone in Firestore, batch users by timezone

```javascript
// Multiple timezone example
exports.sendBettingSuccess_EAT = functions.pubsub
  .schedule('0 8 * * *')
  .timeZone('Africa/Nairobi')
  .onRun(sendToTimezone('Africa/Nairobi'));

exports.sendBettingSuccess_WAT = functions.pubsub
  .schedule('0 8 * * *')
  .timeZone('Africa/Lagos')
  .onRun(sendToTimezone('Africa/Lagos'));
```


