# DecreaseScreenTime
App that rewards low screentime and punishes high screen time

## Features

- **Screen Unlock Counter**: Tracks every time you unlock your device (Android 12+)
- **Persistent Storage**: Unlock count is saved and persists across app restarts
- **Simple UI**: Clean interface showing your unlock count
- **Reset Counter**: Ability to reset the unlock count at any time

## How It Works

The app uses Android's `ACTION_USER_PRESENT` broadcast intent to detect when the device screen is unlocked. This broadcast is sent after the user has unlocked the device and is available on Android 12 and higher.

### Key Components

1. **ScreenUnlockReceiver**: A BroadcastReceiver registered in the AndroidManifest that listens for `ACTION_USER_PRESENT` events and increments the unlock count
2. **MainActivity**: Displays the current unlock count and provides a reset button
3. **SharedPreferences**: Used to persist the unlock count across app sessions

## Requirements

- Android 12 (API level 31) or higher
- No special permissions required - screen unlock events are publicly available

## Building

```bash
./gradlew assembleDebug
```

## Installation

```bash
./gradlew installDebug
```
