# DecreaseScreenTime
App that rewards low screentime and punishes high screen time

## Features

- **Screen Unlock Counter**: Tracks every time you unlock your device (Android 12+)
- **Screen Time Tracking**: Monitors total screen-on time throughout the day
- **Progressive Annoyance System**: Increasingly intrusive warnings as screen time increases
  - **>15 minutes** (configurable): Persistent overlay message showing current screen time
  - **>30 minutes** (configurable): Larger warning message with increased visibility
  - **>1 hour** (configurable): Screen dimming that progressively gets darker + popup warnings every 2 minutes
- **Fully Configurable Settings**: All thresholds, intervals, and display settings can be customized
- **Daily Reset**: Screen time automatically resets at midnight
- **Persistent Storage**: All data is saved and persists across app restarts
- **CSV Export**: Export your screen time history to CSV for analysis
- **Simple UI**: Clean interface showing both unlock count and screen time
- **Reset All**: Ability to reset both unlock count and screen time at any time

## How It Works

The app uses multiple Android system features to track and discourage excessive phone usage:

1. **Unlock Tracking**: Uses `ACTION_USER_PRESENT` broadcast intent to count screen unlocks
2. **Screen Time Tracking**: Monitors `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF` broadcasts to calculate total screen-on time
3. **Foreground Service**: Runs persistently in the background to track screen time accurately
4. **Screen Overlay**: Displays warning messages using `SYSTEM_ALERT_WINDOW` permission when usage exceeds thresholds
5. **Progressive Interventions**: Implements increasingly annoying features to encourage users to reduce screen time

### Key Components

1. **ScreenUnlockReceiver**: Listens for screen unlock events and ensures tracking services are running
2. **ScreenTimeService**: Foreground service that tracks screen-on time and broadcasts updates
3. **OverlayService**: Manages the on-screen overlay that displays warnings based on usage thresholds
4. **MainActivity**: Displays current statistics and provides controls to the user
5. **SettingsActivity**: Allows users to configure all thresholds and display preferences
6. **SettingsManager**: Manages user preferences and provides default values
7. **CsvExporter**: Exports screen time history data to CSV format
8. **DailyStats**: Manages daily screen time statistics and history
9. **SharedPreferences**: Persists all tracking data and settings across app sessions and device reboots

## Configuration

All app behavior can be customized through the Settings screen, accessible from the main app interface. The following values are configurable:

### Screen Time Thresholds
- **Overlay Threshold** (default: 15 minutes): When to start showing the overlay warning
- **High Screen Time Threshold** (default: 30 minutes): Warning level with larger text
- **Excessive Screen Time Threshold** (default: 60 minutes): Critical level with dimming and popups

### Popup Settings
- **Popup Frequency** (default: 2 minutes): How often to show popup warnings when threshold is exceeded

### Update Settings
- **Update Interval** (default: 30 seconds): How frequently to refresh the overlay display

### Text Size Settings
- **Small Text Size** (default: 18sp): Text size for normal warnings
- **Large Text Size** (default: 24sp): Text size for high/excessive warnings

### Screen Dimming Settings
- **Initial Dim Amount** (default: 0.3): Screen dimming opacity when threshold is first exceeded (0.0-1.0)
- **Dim Increment Amount** (default: 0.1): Additional dimming amount per interval (0.0-1.0)
- **Dim Increment Interval** (default: 60 minutes): Time interval for applying dim increments
- **Max Dim Amount** (default: 0.5): Maximum screen dimming opacity (0.0-1.0)

Example: With defaults, dimming increases by 10% every 60 minutes. To get 5% every 10 minutes, set Dim Increment Amount to 0.05 and Dim Increment Interval to 10.

All settings can be reset to their default values using the "Reset to Defaults" button in the Settings screen.

## Requirements

- Android 12 (API level 31) or higher
- **Permissions Required**:
  - `SYSTEM_ALERT_WINDOW`: To display overlay warnings on top of other apps
  - `FOREGROUND_SERVICE`: To run persistent background service for screen time tracking
  - `FOREGROUND_SERVICE_DATA_SYNC`: Required for data sync foreground service (Android 14+)
  - `POST_NOTIFICATIONS`: To show foreground service notification (Android 13+)

## Building

### Local Build

```bash
./gradlew assembleDebug
```

### CI/CD

The project includes GitHub Actions workflows for automated builds:

- **Build APK** (`build.yml`): Runs on every push to main/develop branches and pull requests, creates debug APK artifacts
- **Build and Release APK** (`build-release.yml`): Runs when a version tag is pushed, creates a signed release APK and attaches it to GitHub releases

#### Automatic Versioning

Every CI build automatically generates a unique version number:
- **Version Code**: Format `YYYYMMDDRRR` (e.g., `20240115001` for January 15, 2024, run #1)
  - `YYYYMMDD`: Current date in UTC
  - `RRR`: Zero-padded 3-digit run number (001-999)
- **Version Name**: Format `YYYY.MM.DD.RUN` (e.g., `2025.01.15.1`)

The zero-padded run number ensures monotonically increasing version codes across different days (e.g., `20250115999` < `20250116001`). This allows proper installation of newer APKs over older ones, as long as they're signed with the same key. Local builds default to version `1` / `1.0`.

## Installation

### From APK

Download the latest APK from the [Releases](https://github.com/SimonBaars/DecreaseScreenTime/releases) page.

**Important**: Debug APKs (from workflow artifacts) and Release APKs (from releases) are signed with different keys. You cannot install one type over the other without first uninstalling the existing app. To preserve your data when upgrading:
- Always use the same type of APK (debug or release)
- OR export your data before uninstalling, then import after reinstalling

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
