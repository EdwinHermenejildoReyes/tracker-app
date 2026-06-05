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
│   ├── GeofenceReceiver.kt      # BroadcastReceiver — handles ENTER/EXIT transitions
│   ├── GeofenceManager.kt       # Wraps GeofencingClient; add/remove geofence logic
│   ├── NotificationHelper.kt    # Creates notification channel, builds and posts notification
│   ├── ArrivalReporter.kt       # POSTs arrival event to backend via OkHttp (fire-and-forget)
│   ├── GeofenceConstants.kt     # All shared constants (coords, IDs, prefs keys)
│   └── ui/
│       └── MainScreen.kt        # Compose UI — shows target coords, radius, and live status
└── AndroidManifest.xml
```

### Key design decisions

- **GeofenceReceiver** is a `BroadcastReceiver` registered in the manifest so Android can wake it even when the app is killed.
- Geofence state (`isInside: Boolean`) is persisted in `SharedPreferences` to enforce "notify once per entry" semantics — cleared on `GEOFENCE_TRANSITION_EXIT`.
- `NEVER_EXPIRE` duration is used; the geofence is re-registered on app launch in case it was dropped by the OS.
- **ArrivalReporter** runs fire-and-forget HTTP calls on OkHttp's background threads; failures are logged and swallowed so they never block the `BroadcastReceiver`.

### Backend integration

On each ENTER transition `ArrivalReporter.report()` POSTs to `{BACKEND_URL}/api/arrivals/` with `latitude`, `longitude`, and `device_id` (Android ID). Configure in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BACKEND_URL", "\"http://YOUR_SERVER_IP:8001\"")
buildConfigField("String", "TRACKER_API_KEY", "\"change-this\"")
```

If `BACKEND_URL` still contains `YOUR_SERVER_IP` at runtime, `ArrivalReporter` skips the HTTP call and logs a warning.

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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```
