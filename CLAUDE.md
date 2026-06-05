# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**tracker-app** is a native Android app (Kotlin + Jetpack Compose) that monitors a fixed geographic location using Android's Geofencing API and fires a local notification when the device enters the target zone. No login, no backend, no remote database.

- **Target location:** Lat `-2.1812473`, Lng `-79.8146685`, radius `100m`
- **Trigger:** `GEOFENCE_TRANSITION_ENTER` only
- **Notification fires once per entry** — resets when the user exits the geofence

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Geofencing | Google Play Services `play-services-location` |
| Notifications | `NotificationCompat` (AndroidX) |
| Min SDK | 26 (Android 8.0) |

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architecture

Single-screen app, no navigation graph needed.

```
app/src/main/
├── java/com/trackerapp/
│   ├── MainActivity.kt          # Entry point; requests permissions, registers geofence
│   ├── GeofenceReceiver.kt      # BroadcastReceiver — handles ENTER transition, fires notification
│   ├── GeofenceManager.kt       # Wraps GeofencingClient; add/remove geofence logic
│   ├── NotificationHelper.kt    # Creates notification channel, builds and posts notification
│   └── ui/
│       └── MainScreen.kt        # Compose UI — shows target coords, radius, and live status
└── AndroidManifest.xml
```

### Key design decisions

- **GeofenceReceiver** is a `BroadcastReceiver` registered in the manifest so Android can wake it even when the app is killed.
- Geofence state (`isInside: Boolean`) is persisted in `SharedPreferences` to enforce "notify once per entry" semantics — cleared on `GEOFENCE_TRANSITION_EXIT`.
- `NEVER_EXPIRE` duration is used; the geofence is re-registered on app launch in case it was dropped by the OS.

## Required Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Runtime permission request order matters on Android 10+: request `ACCESS_FINE_LOCATION` first, then `ACCESS_BACKGROUND_LOCATION` in a separate prompt.

## Key Constants

```kotlin
object GeofenceConstants {
    const val TARGET_LAT  = -2.1812473
    const val TARGET_LNG  = -79.8146685
    const val RADIUS_M    = 100f
    const val GEOFENCE_ID = "TRACKER_DESTINATION"
    const val CHANNEL_ID  = "tracker_channel"
    const val NOTIF_ID    = 1001
}
```

## Dependencies (build.gradle.kts — app module)

```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```
