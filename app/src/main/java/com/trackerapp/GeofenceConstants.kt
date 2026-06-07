package com.trackerapp

object GeofenceConstants {
    const val TARGET_LAT = -2.1812473
    const val TARGET_LNG = -79.8146685
    val RADIUS_M get() = BuildConfig.GEOFENCE_RADIUS_M
    const val GEOFENCE_ID = "TRACKER_DESTINATION"
    const val CHANNEL_ID = "tracker_channel"
    const val NOTIF_ID = 1001
    const val PREFS_NAME = "tracker_prefs"
    const val KEY_INSIDE = "is_inside_geofence"
}
