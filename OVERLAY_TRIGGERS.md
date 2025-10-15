# Overlay Display Triggers - Technical Analysis

## What Triggers the Overlay?

The overlay is triggered by the following conditions:

### Primary Trigger Condition
```kotlin
if (minutes >= settingsManager.overlayThresholdMinutes) {
    // Show overlay
}
```

**Default threshold:** 15 minutes (configurable in SettingsManager)

The overlay will appear when your accumulated screen time for the day reaches or exceeds this threshold.

## How the Overlay System Works

### 1. Screen Time Tracking (ScreenTimeService)
- Tracks time with screen on using `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF` broadcasts
- Updates every 30 seconds while screen is on
- Broadcasts current screen time via `ACTION_SCREEN_TIME_UPDATE`
- Persists data to SharedPreferences

### 2. Overlay Display (OverlayService)
- Listens for screen time updates from ScreenTimeService
- Updates overlay every X seconds (configurable, default 30 seconds)
- Shows/hides overlay based on threshold comparison

### 3. Update Mechanisms

The overlay updates through three mechanisms:

#### A. Periodic Updates (Primary)
```kotlin
private val updateRunnable = object : Runnable {
    override fun run() {
        updateOverlay()
        handler.postDelayed(this, settingsManager.updateIntervalSeconds * 1000L)
    }
}
```
- Runs every 30 seconds (default)
- Checks current screen time against threshold
- Updates or removes overlay accordingly

#### B. Broadcast Updates (Secondary)
```kotlin
private val screenTimeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ScreenTimeService.ACTION_SCREEN_TIME_UPDATE) {
            currentScreenTime = intent.getLongExtra(EXTRA_SCREEN_TIME, 0)
            updateOverlay()
        }
    }
}
```
- Receives broadcasts from ScreenTimeService
- Updates immediately when new screen time data is available

#### C. Screen On Events (Immediate)
```kotlin
private val screenOnReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_ON) {
            isScreenOn = true
            requestScreenTimeUpdate()  // NEW: Request fresh data
            updateOverlay()
        }
    }
}
```
- Triggers on screen unlock
- Requests immediate update from ScreenTimeService
- Shows overlay if threshold is met

## Why Was It Random Before?

### Issue 1: Stale Data on Startup
**Problem:** When OverlayService started, it loaded screen time from SharedPreferences which might be outdated.

**Example Scenario:**
```
1. ScreenTimeService accumulates 20 minutes
2. OverlayService starts
3. Loads 15 minutes from SharedPreferences (stale)
4. Overlay shows "15 minutes" instead of "20 minutes"
5. After 30 seconds, gets broadcast and updates to "20 minutes"
```

**Fix:** Now requests immediate update on startup

### Issue 2: Screen State Desynchronization
**Problem:** Overlay didn't know if screen was on or off, leading to operations on invalid view states.

**Example Scenario:**
```
1. Screen off, overlay view is null
2. Periodic update runs
3. Tries to update overlay content on null view
4. Silently fails (logged as warning)
5. When screen turns on, no overlay appears until next update
```

**Fix:** Now tracks screen state and skips updates when screen is off

### Issue 3: Missed Broadcasts
**Problem:** System broadcasts can be delayed or missed due to battery optimization or system load.

**Example Scenario:**
```
1. ScreenTimeService sends broadcast at 12:00:00
2. System delays/drops broadcast due to load
3. OverlayService never receives update
4. Overlay shows stale time or doesn't appear
5. Next update at 12:00:30 fixes it
```

**Fix:** Now has request/response mechanism to pull fresh data on demand

### Issue 4: Race Condition on Service Start
**Problem:** If OverlayService started before ScreenTimeService, it would have no data.

**Example Scenario:**
```
1. Phone boots/app starts
2. OverlayService starts first
3. Loads 0 from SharedPreferences (no data yet)
4. Shows no overlay (below threshold)
5. ScreenTimeService starts later and broadcasts
6. Overlay appears after 30+ second delay
```

**Fix:** Now requests data immediately, and ScreenTimeService responds even if it just started

## Configuration Options

You can adjust overlay behavior through SettingsManager:

### overlayThresholdMinutes
- **Default:** 15 minutes
- **Description:** Minimum screen time before overlay appears
- **Location:** Settings screen in app

### updateIntervalSeconds
- **Default:** 30 seconds
- **Description:** How often overlay checks and updates
- **Location:** SettingsManager constants

### Screen time text size
- **smallTextSize:** 18f (for normal warnings)
- **largeTextSize:** 24f (for high/excessive warnings)

## Testing the Triggers

### To verify overlay works correctly:

1. **Test threshold crossing:**
   ```bash
   # Set threshold to 1 minute for quick testing
   # Accumulate 1+ minute of screen time
   # Overlay should appear
   ```

2. **Test screen on/off:**
   ```bash
   # With overlay showing:
   # - Turn screen off -> overlay disappears
   # - Turn screen on -> overlay reappears immediately
   ```

3. **Test service restart:**
   ```bash
   # With screen time above threshold:
   # - Kill app/service
   # - Restart app
   # - Overlay should appear immediately
   ```

4. **Monitor with logcat:**
   ```bash
   adb logcat -s OverlayService:D ScreenTimeService:D | grep -E "(updateOverlay|screenTime|overlay)"
   ```

## Summary of Improvements

| Issue | Before | After |
|-------|--------|-------|
| Startup data | Stale from SharedPreferences | Fresh via request/response |
| Screen state | Unknown | Tracked with isScreenOn flag |
| Broadcast reliability | Single mechanism | Multiple fallbacks |
| View lifecycle | Unmanaged | Properly managed with state |
| Update timing | Only periodic | Periodic + event-driven + on-demand |

The overlay should now appear **consistently and reliably** whenever screen time exceeds the configured threshold.
