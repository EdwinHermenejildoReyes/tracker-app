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

        @Suppress("DEPRECATION")
        val location = event.triggeringLocation
        val lat = location?.latitude ?: GeofenceConstants.TARGET_LAT
        val lng = location?.longitude ?: GeofenceConstants.TARGET_LNG

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                ArrivalReporter.report(context, lat, lng, "enter")
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                ArrivalReporter.report(context, lat, lng, "exit")
        }
    }
}
