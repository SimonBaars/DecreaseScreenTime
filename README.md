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

### Local Build

```bash
./gradlew assembleDebug
```

### CI/CD

The project includes GitHub Actions workflows for automated builds:

- **Build APK** (`build.yml`): Runs on every push to main/develop branches and pull requests, creates debug APK artifacts
- **Build and Release APK** (`build-release.yml`): Runs when a version tag is pushed, creates a signed release APK and attaches it to GitHub releases

## Installation

### From APK

Download the latest APK from the [Releases](https://github.com/SimonBaars/DecreaseScreenTime/releases) page.

### Local Installation

```bash
./gradlew installDebug
```

## Creating a Release

To create a new release with an APK artifact:

1. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. The GitHub Actions workflow will automatically:
   - Build the release APK
   - Sign the APK (requires signing secrets configured in repository settings)
   - Create a GitHub release
   - Attach the signed APK to the release

### Required Secrets for Signed Releases

Configure these secrets in your repository settings for APK signing:
- `SIGNING_KEY`: Base64-encoded keystore file
- `KEY_ALIAS`: Keystore alias
- `KEY_STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password
