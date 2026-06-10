package com.trackerapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registerGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(GeofenceConstants.GEOFENCE_ID)
            .setCircularRegion(
                GeofenceConstants.TARGET_LAT,
                GeofenceConstants.TARGET_LNG,
                GeofenceConstants.RADIUS_M
            )
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()

        client.addGeofences(request, pendingIntent)
            .addOnSuccessListener { Log.d("GeofenceManager", "Geofence registrada OK") }
            .addOnFailureListener { e -> Log.e("GeofenceManager", "Error al registrar geofence: ${e.message}") }
    }

    fun removeGeofence() {
        client.removeGeofences(pendingIntent)
    }
}
