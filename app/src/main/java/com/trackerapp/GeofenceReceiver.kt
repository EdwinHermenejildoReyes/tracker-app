package com.trackerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val prefs = context.getSharedPreferences(GeofenceConstants.PREFS_NAME, Context.MODE_PRIVATE)

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                val alreadyInside = prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false)
                if (!alreadyInside) {
                    NotificationHelper.showArrivalNotification(context)
                    prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, true).apply()

                    @Suppress("DEPRECATION")
                    val location = event.triggeringLocation
                    ArrivalReporter.report(
                        context,
                        location?.latitude ?: GeofenceConstants.TARGET_LAT,
                        location?.longitude ?: GeofenceConstants.TARGET_LNG
                    )
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, false).apply()
            }
        }
    }
}
