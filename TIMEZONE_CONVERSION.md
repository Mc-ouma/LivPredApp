# Timezone Conversion Feature

## Overview
The app now automatically converts match times from UTC (Universal Time Coordinated) to the user's local timezone.

## Implementation

### TimeZoneConverter Utility
Located at: `app/src/main/java/com/soccertips/predictx/utils/TimeZoneConverter.kt`

This utility provides methods to:
1. Convert UTC time to local time
2. Convert UTC time with date (handling date changes due to timezone differences)
3. Get timezone offset information

### Key Functions

#### `convertUtcToLocal(utcTime: String?, utcDate: String?): String`
Converts a UTC time string to the user's local timezone.

**Parameters:**
- `utcTime`: Time in UTC format (e.g., "14:30" or "14:30:00")
- `utcDate`: Date in format "yyyy-MM-dd"

**Returns:**
- Formatted time string in user's local timezone (e.g., "14:30")

**Example:**
```kotlin
val utcTime = "14:30"
val utcDate = "2024-03-20"
val localTime = TimeZoneConverter.convertUtcToLocal(utcTime, utcDate)
// If user is in GMT+3, localTime will be "17:30"
```

#### `convertUtcToLocalWithDate(utcTime: String?, utcDate: String?): Pair<String, String>`
Converts UTC time to local time and returns both the adjusted date and time.

**Returns:**
- Pair of (localDate: String, localTime: String)

This is useful when timezone conversion causes the date to change (e.g., converting 23:00 UTC to a timezone that's ahead might result in the next day).

#### `getTimezoneOffset(): String`
Returns the current timezone offset as a string (e.g., "GMT+3", "GMT-5").

## Usage in the App

### ItemsListViewModel
The `ItemsListViewModel` automatically converts UTC times when fetching match data:

```kotlin
// Convert UTC time to local timezone
val localTime = TimeZoneConverter.convertUtcToLocal(
    serverResponse.mTime,
    serverResponse.mDate
)

ServerResponse(
    // ...
    mTime = localTime,
    // ...
)
```

### Data Flow
1. Server sends match time in UTC format
2. `ItemsListViewModel` converts UTC to local time using `TimeZoneConverter`
3. Converted time is stored in `ServerResponse` object
4. When item is added to favorites, the local time is saved to the database
5. UI displays the local time to the user

## Benefits
- Users see match times in their local timezone automatically
- No manual timezone selection needed
- Works across all timezones globally
- Handles timezone changes (DST, user travel, etc.) automatically

## Technical Notes
- Uses Java Time API (`java.time.*`)
- Requires Android API level 26+ (Android 8.0 Oreo)
- Uses device's system timezone settings
- Handles edge cases (invalid dates, missing data, etc.)
- Logs errors using Timber for debugging

## Testing Considerations
To test timezone conversion:
1. Change device timezone in system settings
2. Verify match times update accordingly
3. Test with times that would change dates (e.g., 23:00 UTC in GMT+2)
4. Check with various timezone offsets (negative, positive, fractional)
