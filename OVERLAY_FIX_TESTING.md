# Overlay Reliability Fix - Testing Guide

## Problem
The overlay was showing randomly due to several race conditions and synchronization issues between `OverlayService` and `ScreenTimeService`.

## Root Causes Identified and Fixed

### 1. Race Condition on Service Startup
**Problem:** When `OverlayService` started, it loaded screen time from SharedPreferences which could be stale or zero. The overlay update was triggered immediately without waiting for fresh data.

**Fix:** Added `requestScreenTimeUpdate()` method that sends a broadcast to `ScreenTimeService` requesting immediate update. This ensures fresh data is available on startup.

### 2. Missing Screen State Tracking
**Problem:** The overlay service didn't track whether the screen was on or off, leading to unnecessary operations and potential view lifecycle issues.

**Fix:** Added `isScreenOn` flag that tracks screen state. Overlay operations are now skipped when screen is off, preventing view lifecycle issues.

### 3. No Synchronization on Screen On/Off Events
**Problem:** When screen turned on, the overlay didn't request fresh screen time data, relying only on periodic broadcasts which might be delayed or missed.

**Fix:** Modified `screenOnReceiver` to immediately request screen time update when screen turns on, ensuring overlay has latest data.

### 4. Overlay Not Removed on Screen Off
**Problem:** The overlay view remained in memory even when screen was off, potentially causing view state corruption.

**Fix:** Added `removeOverlay()` call when screen turns off to clean up resources and prevent state issues.

### 5. No Validation of Overlay Creation
**Problem:** `updateOverlayContent()` was called even if overlay creation failed, potentially causing null pointer exceptions.

**Fix:** Added check to only update overlay content if `overlayView != null` after creation attempt.

## How to Test

### Manual Testing Steps

1. **Install the app** on a test device or emulator (API 31+)

2. **Grant overlay permission** when prompted

3. **Test Case 1: Normal Operation**
   - Open the app
   - Let screen time accumulate past threshold (default 15 minutes)
   - Observe that overlay appears consistently at the top of the screen
   - **Expected:** Overlay shows with correct screen time

4. **Test Case 2: Screen On/Off**
   - With overlay showing, turn screen off
   - Wait a few seconds
   - Turn screen back on
   - **Expected:** Overlay reappears immediately with updated screen time

5. **Test Case 3: Service Restart**
   - Force stop the app
   - Wait for a few seconds
   - Open the app again
   - If screen time is above threshold, overlay should appear
   - **Expected:** Overlay shows with correct accumulated screen time

6. **Test Case 4: Threshold Crossing**
   - Start with screen time below threshold (e.g., 10 minutes)
   - Use phone normally until crossing threshold (15 minutes)
   - **Expected:** Overlay appears when threshold is crossed

7. **Test Case 5: App Restart with Screen Time**
   - Accumulate screen time above threshold
   - Kill and restart the app
   - **Expected:** Overlay appears immediately on restart

### Logcat Verification

Run these commands to see detailed logging:

```bash
adb logcat -s OverlayService ScreenTimeService
```

Look for these key log messages:

**On Service Start:**
```
OverlayService: onCreate: screen is currently on/off
OverlayService: loadInitialScreenTime: loaded X ms from preferences
OverlayService: requestScreenTimeUpdate: requested screen time update
ScreenTimeService: updateScreenTime called (from request receiver)
```

**On Screen On:**
```
OverlayService: screenOnReceiver: screen turned on
OverlayService: requestScreenTimeUpdate: requested screen time update
OverlayService: updateOverlay: minutes=X, threshold=Y, overlayView=true/false
```

**On Screen Off:**
```
OverlayService: screenOnReceiver: screen turned off
OverlayService: removeOverlay: removing overlay
```

**On Overlay Update:**
```
OverlayService: updateOverlay: minutes=X, threshold=Y, overlayView=true/false
OverlayService: createOverlay: attempting to create overlay (if not exists)
OverlayService: createOverlay: overlay created successfully
OverlayService: updateOverlayContent: updating message to: [message]
```

## Code Changes Summary

### OverlayService.kt
- Added `isScreenOn` flag for screen state tracking
- Added `requestScreenTimeUpdate()` to request fresh data
- Modified `onCreate()` to check initial screen state and request update
- Modified `screenOnReceiver` to update state and request data
- Modified `updateOverlay()` to check screen state and validate overlay creation
- Added `ACTION_REQUEST_SCREEN_TIME_UPDATE` constant

### ScreenTimeService.kt
- Added `screenTimeRequestReceiver` to handle update requests
- Registered/unregistered new receiver in lifecycle methods
- Added `ACTION_REQUEST_SCREEN_TIME_UPDATE` constant

## Expected Behavior After Fix

1. **Consistent Display:** Overlay should appear consistently when screen time exceeds threshold
2. **Immediate Updates:** Overlay updates immediately on screen on events
3. **Clean State:** No stale or incorrect screen time values
4. **Resource Efficiency:** Overlay removed when screen is off
5. **Resilient to Restarts:** Works correctly after app/service restarts
