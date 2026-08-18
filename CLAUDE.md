# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**tracker-app** is a native Android app (Kotlin + Jetpack Compose) that monitors a fixed geographic location using two parallel mechanisms and reports ENTER, EXIT, and STATIONARY events to a backend API.

- **Target location:** Lat `-2.1812473`, Lng `-79.8146685`
- **Radius:** `100 m` debug / `50 m` release (via `BuildConfig.GEOFENCE_RADIUS_M`)
- **Backend:** configured in `defaultConfig` of `app/build.gradle.kts`

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Geofencing | Google Play Services `play-services-location` |
| HTTP | OkHttp 4.12.0 (synchronous `execute()` on a spawned Thread) |
| Notifications | `NotificationCompat` (AndroidX) |
| Min SDK | 26 (Android 8.0) |

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Build release APK (requires keystore entries in local.properties)
./gradlew assembleRelease

# Run unit tests (Robolectric, no device needed)
./gradlew test
./gradlew test --tests "com.trackerapp.GeofenceReceiverPrefsTest"

# Lint
./gradlew lint
```

Release signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from `local.properties`.

## Architecture

```
app/src/main/
├── java/com/trackerapp/
│   ├── MainActivity.kt            # Entry point; requests permissions, registers geofence,
│   │                              #   starts LocationTrackingService
│   ├── GeofenceReceiver.kt        # BroadcastReceiver — handles ENTER/EXIT from OS geofence API
│   ├── GeofenceManager.kt         # Wraps GeofencingClient; add/remove geofence logic
│   ├── LocationTrackingService.kt # Foreground service; polls location every 5 min,
│   │                              #   computes proximity manually, reports stationary episodes
│   ├── BootReceiver.kt            # Re-registers geofence and restarts service after reboot
│   ├── ArrivalReporter.kt         # POSTs arrival events to backend via OkHttp
│   ├── NotificationHelper.kt      # Creates notification channel, builds and posts notification
│   ├── GeofenceConstants.kt       # All shared constants (coords, IDs, prefs keys)
│   └── ui/
│       └── MainScreen.kt          # Compose UI — shows target coords, radius, and live status
└── AndroidManifest.xml
```

### Dual detection

- **GeofenceReceiver** is a `BroadcastReceiver` registered in the manifest so Android can wake it even when the app is killed. It handles `GEOFENCE_TRANSITION_ENTER` and `GEOFENCE_TRANSITION_EXIT`, then calls `goAsync()` and spawns a Thread to run `ArrivalReporter.report()`.
- **LocationTrackingService** is a foreground service (notification title: "System Tools") that polls the fused location provider every 5 minutes via `LocationRequest`. It computes distance to the target using `Location.distanceBetween()` and tracks enter/exit via `SharedPreferences`. It also detects a `"stationary"` condition: if the device hasn't moved more than 50 m from an anchor point for 10 minutes while outside the zone, it reports one `"stationary"` event per episode.

### Event types reported to backend

| Event | Source | Condition |
|---|---|---|
| `"enter"` | Both | Device crosses into zone |
| `"exit"` | Both | Device crosses out of zone |
| `"stationary"` | LocationTrackingService only | Motionless outside zone for 10+ min |

### Duplicate-event suppression

`LocationTrackingService` guards against duplicate ENTER/EXIT via `SharedPreferences` key `KEY_INSIDE`. `GeofenceReceiver` has no such guard — it forwards every OS-delivered transition.

### Offline queue (store-and-forward)

Events are never sent directly over HTTP. Instead:
1. `EventQueue.enqueue()` saves a `PendingEvent` to Room DB (fast, works offline)
2. `EventQueue.scheduleUpload()` enqueues a `UploadWorker` via WorkManager with `NetworkType.CONNECTED` constraint
3. `UploadWorker.doWork()` drains the queue: sends each event via `ArrivalReporter.report()`, deletes it on success, retries with exponential backoff on failure
4. Each event carries a UUID `event_id`; the backend returns HTTP 200 (not 201) for duplicates, so retries are safe

### Backend integration

`ArrivalReporter.report()` POSTs to `{BACKEND_URL}/api/arrivals/` with `event_id`, `latitude`, `longitude`, `device_id` (Android ID), and `event_type`. Configure in `app/build.gradle.kts`:

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
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

Runtime permission request order matters on Android 10+: request `ACCESS_FINE_LOCATION` first, then `ACCESS_BACKGROUND_LOCATION` in a separate prompt.

## Key Constants (`GeofenceConstants.kt`)

```kotlin
object GeofenceConstants {
    const val TARGET_LAT  = -2.1812473
    const val TARGET_LNG  = -79.8146685
    val RADIUS_M get()    = BuildConfig.GEOFENCE_RADIUS_M   // 100f debug / 50f release
    const val GEOFENCE_ID = "TRACKER_DESTINATION"
    const val CHANNEL_ID  = "tracker_channel"
    const val NOTIF_ID    = 1001
    const val PREFS_NAME  = "tracker_prefs"
    const val KEY_INSIDE  = "is_inside_geofence"
}
```

`LocationTrackingService` stationary thresholds (companion object):
- `STATIONARY_THRESHOLD_M = 50f`
- `STATIONARY_TIMEOUT_MS = 10 * 60 * 1000L`
