# Power Cuts Log - Android App

A Material 3 Android app that monitors power outages while your phone is plugged into a charger.

## Features

- **Power Monitoring**: Foreground service that listens to power connect/disconnect events
- **Event Logging**: Stores power outages in local Room database with timezone handling
- **Real-time UI**: Jetpack Compose interface with Material 3 design
- **Export & Share**: Generate CSV files and share as ZIP via system share sheet
- **Boot Recovery**: Auto-resumes monitoring after device reboot
- **Debounce Control**: Configurable sensitivity to filter out flaky adapter bounces
- **Settings**: Adjustable debounce settings and debug tools

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room (SQLite)
- **Preferences**: DataStore
- **State Management**: ViewModel + Flow
- **Async**: Coroutines
- **Min SDK**: 24
- **Target SDK**: 36

## Project Structure

```
app/src/main/java/com/powercuts/powercutmonitor/
├── MainActivity.kt                    # Single activity with Compose
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── entities/
│   │   │   ├── PowerEvent.kt         # Power event entity
│   │   │   └── DailyTotal.kt         # Daily totals data class
│   │   └── dao/
│   │       └── EventDao.kt           # Database access object
│   ├── prefs/
│   │   └── PrefsManager.kt           # DataStore preferences
│   └── repository/
│       └── EventRepository.kt       # Repository pattern
├── domain/
│   ├── ExportManager.kt              # CSV/ZIP export logic
│   └── MonitorController.kt          # Service binding
├── service/
│   ├── PowerMonitorService.kt        # Foreground service
│   ├── PowerReceiver.kt              # Broadcast receiver
│   ├── BootReceiver.kt                # Boot completion receiver
│   └── NotificationManager.kt        # Notification management
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt             # Main Compose screen
│   │   └── SettingsScreen.kt         # Settings screen
│   ├── viewmodels/
│   │   ├── MainViewModel.kt          # Main screen state
│   │   └── SettingsViewModel.kt      # Settings state
│   └── theme/
│       ├── Theme.kt                  # Material 3 theme
│       ├── Color.kt                  # Color palette
│       └── Type.kt                   # Typography
└── util/
    ├── Constants.kt                  # App constants
    ├── DateTimeUtils.kt              # Timezone utilities
    └── FormatUtils.kt                # UI formatting
```

## Android Studio Sync Issues

If you're experiencing sync issues with Android Studio:

1. **Check Java Setup**: Ensure JAVA_HOME is set correctly
2. **Gradle Sync**: Try "File > Sync Project with Gradle Files"
3. **Clean Build**: Try "Build > Clean Project" then "Build > Rebuild Project"
4. **Invalidate Caches**: Try "File > Invalidate Caches and Restart"

## Dependencies Added

The following dependencies were added to `app/build.gradle.kts`:

- Room database (runtime, ktx, compiler)
- DataStore preferences
- Lifecycle ViewModel Compose
- Material Icons Extended
- Kotlin Coroutines Android

## Permissions Required

- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`
- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `WAKE_LOCK`

## Testing

1. **Build the project** in Android Studio
2. **Install on device** with USB debugging enabled
3. **Test power monitoring** by plugging/unplugging charger
4. **Test export** by tapping "Share CSV" button
5. **Test boot recovery** by rebooting with monitoring ON

## Known Issues

- Requires Java/JDK setup for Gradle builds
- Some OEMs may delay boot broadcasts by minutes
- Manual charger unplugging is treated as power outage (by design)

## Privacy

- No data collection
- All logs stored locally on device
- Share only via user action
- No internet permissions
