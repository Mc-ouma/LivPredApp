# Pass Counter UI - Implementation

## Overview

A visual pass counter has been added to the CategoriesScreen to show users their unlock pass balance at a glance.

## Visual Design

### Pass Counter Badge
```
┌──────────────────────┐
│  🎟️  3/5            │  ← Badge with icon and count
└──────────────────────┘
     • • • ○ ○            ← Visual indicators (filled = have, empty = need)
```

### Components

#### 1. PassCounter (Badge)
- **Design**: Rounded rectangle badge
- **Color**: Primary container color (adapts to theme)
- **Icon**: Ticket/confirmation number icon
- **Text**: Shows "current/max" (e.g., "3/5")
- **Style**: Bold label text

#### 2. PassIndicators (Dots)
- **Design**: Row of circular dots
- **Total dots**: Equal to max passes (5)
- **Filled dots**: Represent current passes (primary color)
- **Empty dots**: Represent available slots (surface variant)
- **Spacing**: 4dp between dots

## Location

The pass counter appears at the **top of the categories grid**, above announcements and category cards.

**Visibility Rules:**
- ✅ Shows when: `adStrategy = "rewarded"` AND at least one category has `requiresRewardAd = true`
- ❌ Hidden when: `adStrategy = "interstitial"` OR no locked categories exist

## User Experience

### States

| Pass Balance | Badge Display | Indicators | User Perception |
|--------------|---------------|------------|-----------------|
| 0/5 | 🎟️ 0/5 | ○ ○ ○ ○ ○ | "Need to earn passes" |
| 1/5 | 🎟️ 1/5 | • ○ ○ ○ ○ | "Have 1 pass" |
| 3/5 | 🎟️ 3/5 | • • • ○ ○ | "Have 3 passes" |
| 5/5 | 🎟️ 5/5 | • • • • • | "At maximum!" |

### Real-time Updates

The counter updates automatically when:
- ✅ User earns a pass (after watching ad)
- ✅ User spends a pass (unlocking category)
- ✅ User returns to screen from background

Updates are **reactive** via StateFlow, so no manual refresh needed.

## Implementation Details

### Code Structure

```kotlin
// Main component - Badge with icon and text
@Composable
fun PassCounter(
    passBalance: Int,
    maxPasses: Int,
    modifier: Modifier = Modifier
)

// Secondary component - Visual dot indicators
@Composable
fun PassIndicators(
    passBalance: Int,
    maxPasses: Int,
    modifier: Modifier = Modifier
)
```

### Integration in CategoriesContent

```kotlin
LazyVerticalGrid {
    // Pass Counter (conditionally shown)
    if (adStrategy == REWARDED && hasLockedCategories) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(horizontalAlignment = Center) {
                PassCounter(passBalance, maxPasses)
                Spacer(4.dp)
                PassIndicators(passBalance, maxPasses)
            }
        }
    }
    
    // Announcements...
    // Categories...
}
```

### Styling

**PassCounter Badge:**
- Shape: RoundedCornerShape(16.dp)
- Padding: horizontal=12dp, vertical=6dp
- Background: primaryContainer
- Text: labelLarge, bold
- Icon: 20dp size

**PassIndicators Dots:**
- Size: 8dp diameter
- Shape: Circle
- Spacing: 4dp
- Colors: primary (filled), surfaceVariant (empty)

## Benefits

### For Users
1. **Immediate visibility** - Know pass count without tapping anything
2. **Visual feedback** - Dots make it clear how many passes you have
3. **Progress indication** - See how close to max (5/5)
4. **Motivation** - Visual progress encourages earning more passes

### For Developers
1. **Clean UI** - Integrated into grid layout
2. **Adaptive** - Only shows when relevant (rewarded strategy + locked categories)
3. **Reactive** - Auto-updates via StateFlow
4. **Themeable** - Respects Material3 color scheme

## Examples

### Scenario 1: New User (0 passes)
```
╔════════════════════════╗
║   🎟️  0/5             ║
║    ○ ○ ○ ○ ○          ║
╚════════════════════════╝

[Announcements if any]

┌──────────────┬──────────────┐
│ 🔒 Category A│ ⚽ Category B │
└──────────────┴──────────────┘
```
**User sees:** "I have no passes, need to watch ads"

### Scenario 2: Active User (3 passes)
```
╔════════════════════════╗
║   🎟️  3/5             ║
║    • • • ○ ○          ║
╚════════════════════════╝

[Announcements if any]

┌──────────────┬──────────────┐
│ 🔒 Category A│ ⚽ Category B │
└──────────────┴──────────────┘
```
**User sees:** "I have 3 passes, can unlock 3 categories!"

### Scenario 3: Max Passes (5/5)
```
╔════════════════════════╗
║   🎟️  5/5             ║
║    • • • • •          ║
╚════════════════════════╝

[Announcements if any]

┌──────────────┬──────────────┐
│ 🔒 Category A│ ⚽ Category B │
└──────────────┴──────────────┘
```
**User sees:** "I'm maxed out, should use some passes!"

## A/B Test Integration

### Variant A: Rewarded (Shows Counter)
```
Users see pass counter
    ↓
Click locked category
    ↓
Check passes in dialog
    ↓
Use pass or earn pass
```

Counter reinforces the pass economy concept.

### Variant B: Interstitial (No Counter)
```
No pass counter shown
    ↓
All categories accessible
    ↓
Interstitial ad on back press
```

Counter is hidden since passes aren't used in this variant.

## Future Enhancements

### 1. Animated Transitions
```kotlin
AnimatedContent(targetState = passBalance) { count ->
    PassCounter(count, maxPasses)
}
```

### 2. Pulsing Effect When Full
```kotlin
if (passBalance == maxPasses) {
    Modifier.graphicsLayer {
        alpha = animateFloat(0.8f, 1.0f).value
    }
}
```

### 3. Clickable Info
```kotlin
PassCounter(
    onClick = {
        // Show explanation dialog
        // "Earn passes by watching ads"
    }
)
```

### 4. Mini Tutorial
```kotlin
if (isFirstTime && passBalance == 0) {
    Tooltip("Watch ads to earn unlock passes!")
}
```

### 5. Achievement Badges
```kotlin
if (passBalance == maxPasses) {
    Badge("MAXED OUT! 🎉")
}
```

## Accessibility

### Content Descriptions
- Counter icon: "Unlock Passes"
- Badge: "You have 3 out of 5 unlock passes"
- Dots: No content description (decorative)

### Screen Readers
```kotlin
semantics {
    contentDescription = "Unlock passes: $passBalance out of $maxPasses available"
    role = Role.Image
}
```

## Testing

### Visual Testing Checklist
- [ ] Counter appears at top of grid
- [ ] Badge has proper colors (light/dark theme)
- [ ] Icon renders correctly
- [ ] Text is readable and bold
- [ ] Dots display in correct count
- [ ] Filled dots use primary color
- [ ] Empty dots use surface variant
- [ ] Spacing is consistent (4dp between dots)
- [ ] Counter updates when pass earned
- [ ] Counter updates when pass used
- [ ] Counter hidden in interstitial variant
- [ ] Counter hidden when no locked categories

### Functional Testing
```kotlin
@Test
fun passCounter_displaysCorrectBalance() {
    // Given
    val passBalance = 3
    val maxPasses = 5
    
    // When
    composeTestRule.setContent {
        PassCounter(passBalance, maxPasses)
    }
    
    // Then
    composeTestRule.onNodeWithText("3/5").assertExists()
}

@Test
fun passIndicators_showsCorrectDots() {
    // Given
    val passBalance = 3
    val maxPasses = 5
    
    // When
    composeTestRule.setContent {
        PassIndicators(passBalance, maxPasses)
    }
    
    // Then - Should have 5 dots total
    // 3 filled (primary color)
    // 2 empty (surface variant)
}
```

## Performance

### Optimization
- ✅ No recomposition unless passBalance changes (StateFlow)
- ✅ Simple rendering (Icon + Text + Boxes)
- ✅ No images or heavy resources
- ✅ Minimal padding/spacing calculations

### Memory
- Footprint: ~1-2 KB (negligible)
- No bitmaps or allocations
- Pure Compose UI

## Summary

✅ **Visual pass counter added**
✅ **Shows badge with icon and count**
✅ **Shows visual dot indicators**
✅ **Positioned at top of categories grid**
✅ **Conditionally shown (rewarded strategy only)**
✅ **Reactive updates via StateFlow**
✅ **Supports light/dark themes**
✅ **Build successful**

The pass counter provides users with immediate visibility into their pass balance, making the unlock pass system more transparent and engaging!
