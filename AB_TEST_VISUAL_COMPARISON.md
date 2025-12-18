# Visual Flow Comparison: Rewarded vs Interstitial

## Side-by-Side User Journey

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         VARIANT A: REWARDED ADS                            │
└────────────────────────────────────────────────────────────────────────────┘

Step 1: Categories Screen
┌──────────────────────┐
│ 🏠 Categories        │
│                      │
│ ⚽ Free Category     │  ← Click: Direct access
│ 🔒 Premium (Locked)  │  ← Click: Dialog shows
│ 🔒 VIP (Locked)      │  ← Click: Dialog shows
└──────────────────────┘

Step 2: User clicks locked category
┌──────────────────────────────────────┐
│ Watch Ad to Unlock                   │
│                                      │
│ Watch a short ad to unlock           │
│ "Premium" for 24 hours               │
│                                      │
│  [Cancel]      [Watch Ad] ← Click   │
└──────────────────────────────────────┘

Step 3: Rewarded ad plays
┌──────────────────────┐
│                      │
│   🎥 VIDEO AD        │
│   (15-30 seconds)    │
│                      │
│   [Skip in 5s...]    │
└──────────────────────┘

Step 4: Category unlocked → Navigate to items
┌──────────────────────┐
│ ⬅ Premium Category   │
│                      │
│ ⚽ Match 1           │
│ ⚽ Match 2           │
│ ⚽ Match 3           │
└──────────────────────┘

Step 5: User presses back button
┌──────────────────────┐
│ 🏠 Categories        │  ← Direct return
│                      │     NO AD!
│ ⚽ Free Category     │
│ ✓ Premium (Unlocked) │  ← Unlocked for 24h
│ 🔒 VIP (Locked)      │
└──────────────────────┘

═══════════════════════════════════════════════════════════════════════════

┌────────────────────────────────────────────────────────────────────────────┐
│                      VARIANT B: INTERSTITIAL ADS                           │
└────────────────────────────────────────────────────────────────────────────┘

Step 1: Categories Screen
┌──────────────────────┐
│ 🏠 Categories        │
│                      │
│ ⚽ Free Category     │  ← Click: Immediate access
│ ⚽ Premium Category   │  ← Click: Immediate access (no lock)
│ ⚽ VIP Category       │  ← Click: Immediate access (no lock)
└──────────────────────┘
            │
            │ No dialog, no friction!
            ▼

Step 2: Immediately navigate to items
┌──────────────────────┐
│ ⬅ Premium Category   │
│                      │
│ ⚽ Match 1           │
│ ⚽ Match 2           │
│ ⚽ Match 3           │
└──────────────────────┘

Step 3: User browses content freely
(No ads while browsing)

Step 4: User presses back button
┌─────────────────────────────────┐
│                                 │
│    INTERSTITIAL AD              │
│    (Full Screen)                │
│    5-10 seconds                 │
│                          [X]    │
└─────────────────────────────────┘

Step 5: Ad dismisses → Return to categories
┌──────────────────────┐
│ 🏠 Categories        │  ← Returned after ad
│                      │
│ ⚽ Free Category     │
│ ⚽ Premium Category   │  ← Can tap again
│ ⚽ VIP Category       │
└──────────────────────┘
            │
            │ If user taps category again...
            ▼
        Repeat Step 2-5
        (Ad shows EVERY time on back press)
```

## Timing Comparison

### Rewarded Ads (Variant A)
```
Session Example: User explores 3 categories

Time 0s:    Open app → Categories screen
Time 5s:    Tap locked category → Dialog
Time 10s:   Click "Watch Ad"
Time 40s:   Ad finishes (30s ad)
Time 45s:   Browse category 1 items
Time 90s:   Press back → Direct return (NO AD)
Time 95s:   Tap another locked category → Dialog
Time 100s:  Click "Watch Ad"
Time 130s:  Ad finishes (30s ad)
Time 135s:  Browse category 2 items
Time 180s:  Press back → Direct return (NO AD)
Time 185s:  Tap category 1 again → Direct access (still unlocked!)
Time 210s:  Press back → Direct return (NO AD)

Total session: 210 seconds
Ads shown: 2 rewarded ads (60s total)
Ad interruptions: 2 (at category entry)
Back press interruptions: 0
```

### Interstitial Ads (Variant B)
```
Session Example: User explores 3 categories

Time 0s:    Open app → Categories screen
Time 5s:    Tap any category → Immediate access
Time 50s:   Browse category 1 items
Time 60s:   Press back → Interstitial ad (10s)
Time 70s:   Back to categories
Time 75s:   Tap category 2 → Immediate access
Time 120s:  Browse category 2 items
Time 130s:  Press back → Interstitial ad (10s)
Time 140s:  Back to categories
Time 145s:  Tap category 3 → Immediate access
Time 190s:  Browse category 3 items
Time 200s:  Press back → Interstitial ad (10s)
Time 210s:  Back to categories

Total session: 210 seconds
Ads shown: 3 interstitial ads (30s total)
Ad interruptions: 0 (at category entry)
Back press interruptions: 3 (every back press)
```

## User Experience Matrix

| Aspect | Rewarded Ads | Interstitial Ads |
|--------|--------------|------------------|
| **Initial Click** | ❌ Friction (dialog) | ✅ Immediate |
| **First Ad** | Before content | After viewing |
| **Value Exchange** | ✅ Clear ("unlock") | ❌ Unclear (interruption) |
| **Back Navigation** | ✅ Smooth | ❌ Interrupted |
| **Category Re-access** | ✅ Free (24h) | ❌ Ad on every back |
| **User Control** | ✅ High (can cancel) | ❌ Low (automatic) |
| **Predictability** | ✅ High (know when ad) | ❌ Low (surprise on back) |
| **Power User Impact** | Low (unlock once) | HIGH (ad every back) |

## Revenue Scenarios

### Scenario 1: Casual User (2 categories/day)
```
Rewarded Ads:
- 2 ads watched/day
- eCPM: $10
- Revenue: 2 × $0.01 = $0.02/day

Interstitial Ads:
- 2 back presses = 2 interstitials
- eCPM: $5
- Revenue: 2 × $0.005 = $0.01/day

Winner: Rewarded (+100%)
```

### Scenario 2: Active User (5 categories/day)
```
Rewarded Ads:
- 5 ads watched/day
- eCPM: $10
- Revenue: 5 × $0.01 = $0.05/day

Interstitial Ads:
- 5 back presses = 5 interstitials
- eCPM: $5
- Revenue: 5 × $0.005 = $0.025/day

Winner: Rewarded (+100%)
```

### Scenario 3: Power User (10+ categories/day)
```
Rewarded Ads:
- Watch 3-4 ads (most unlock for 24h)
- eCPM: $10
- Revenue: 4 × $0.01 = $0.04/day
- User experience: GOOD (less friction after unlocks)

Interstitial Ads:
- 10+ back presses = 10+ interstitials
- eCPM: $5
- Revenue: 10 × $0.005 = $0.05/day
- User experience: BAD (ad fatigue, may quit)

Winner: Depends on retention impact!
```

## Psychological Impact

### Rewarded Ads
```
User Thought Process:
1. "I want to see premium content"
2. "I need to watch an ad to unlock"
3. *Watches ad* "Fair trade"
4. "Now I have access for 24 hours!"
5. *Explores freely without ads*
6. "This is reasonable"

Emotion: ✅ Satisfaction (value received)
```

### Interstitial Ads
```
User Thought Process:
1. "Let me check this category"
2. *Browses content*
3. "Time to go back"
4. *Ad pops up* "Ugh, another ad?!"
5. "I didn't ask for this"
6. *Repeats every time* "This is annoying"

Emotion: ❌ Frustration (interruption)
```

## Recommendation Matrix

### Choose Rewarded Ads If:
- ✅ You have premium/locked categories system
- ✅ You value user satisfaction
- ✅ You want clear value proposition
- ✅ You prioritize retention over impressions
- ✅ Your users are power users (browse a lot)
- ✅ You want predictable UX

### Choose Interstitial Ads If:
- ✅ You want maximum ad impressions
- ✅ All categories should be freely accessible
- ✅ You can tolerate potential annoyance
- ✅ Your sessions are short (1-2 categories)
- ✅ You prioritize revenue over satisfaction
- ✅ Your users are casual browsers

## Critical Insight

**The key difference is WHERE and WHEN the friction occurs:**

| Variant | Friction Point | User Perception |
|---------|---------------|-----------------|
| Rewarded | **Entry** (before content) | "I'm paying for access" ✅ |
| Interstitial | **Exit** (after content) | "Why am I being punished?" ❌ |

**Bottom Line:**
- Rewarded = "Pay to enter" (acceptable)
- Interstitial = "Taxed on exit" (annoying)

This psychological difference is crucial and may matter more than raw revenue numbers!

## Test Duration Recommendation

**Minimum**: 2 weeks
**Recommended**: 4 weeks

Why? You need to capture:
1. First impression (day 1-3)
2. Habit formation (day 4-7)
3. Retention impact (day 7-14)
4. Long-term behavior (day 14-30)

Look for:
- D1, D7, D30 retention rates
- Session frequency drop-off
- User complaints/reviews
- Total revenue per user
- Category exploration depth
