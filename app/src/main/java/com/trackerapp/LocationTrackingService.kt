package com.trackerapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var stationaryAnchor: Location? = null
    private var stationaryStart: Long = 0L
    private var stationaryReported: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                Thread { checkProximity(loc) }.start()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(GeofenceConstants.NOTIF_ID, buildNotification())
        requestLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5 * 60_000L)
            .setMinUpdateIntervalMillis(3 * 60_000L)
            .build()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        Log.d("LocationTrackingService", "Location updates iniciados (cada 30s)")
    }

    private fun checkProximity(location: Location) {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            GeofenceConstants.TARGET_LAT, GeofenceConstants.TARGET_LNG,
            results
        )
        val distanceM = results[0]
        val isInside = distanceM <= GeofenceConstants.RADIUS_M
        val prefs = getSharedPreferences(GeofenceConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val wasInside = prefs.getBoolean(GeofenceConstants.KEY_INSIDE, false)

        Log.d("LocationTrackingService", "Distancia al punto: ${distanceM.toInt()}m | dentro=$isInside | estabaAdentro=$wasInside")

        when {
            isInside && !wasInside -> {
                prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, true).apply()
                EventQueue.enqueue(this, location.latitude, location.longitude, "enter")
                Log.d("LocationTrackingService", "ENTER enviado")
            }
            !isInside && wasInside -> {
                prefs.edit().putBoolean(GeofenceConstants.KEY_INSIDE, false).apply()
                EventQueue.enqueue(this, location.latitude, location.longitude, "exit")
                Log.d("LocationTrackingService", "EXIT enviado")
            }
        }

        if (isInside) {
            resetStationary(location)
        } else {
            checkStationary(location)
        }
    }

    private fun checkStationary(location: Location) {
        val anchor = stationaryAnchor
        if (anchor == null) {
            stationaryAnchor = location
            stationaryStart = System.currentTimeMillis()
            return
        }
        val distFromAnchor = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            anchor.latitude, anchor.longitude,
            distFromAnchor
        )
        if (distFromAnchor[0] > STATIONARY_THRESHOLD_M) {
            resetStationary(location)
            stationaryAnchor = location
            stationaryStart = System.currentTimeMillis()
            Log.d("LocationTrackingService", "Movimiento detectado (${distFromAnchor[0].toInt()}m), reiniciando estacionario")
            return
        }
        if (!stationaryReported && System.currentTimeMillis() - stationaryStart >= STATIONARY_TIMEOUT_MS) {
            EventQueue.enqueue(this, location.latitude, location.longitude, "stationary")
            stationaryReported = true
            Log.d("LocationTrackingService", "STATIONARY enviado")
        }
    }

    private fun resetStationary(currentLocation: Location? = null) {
        if (stationaryReported && currentLocation != null && stationaryStart > 0L) {
            val durationSeconds = ((System.currentTimeMillis() - stationaryStart) / 1000).toInt()
            val anchor = stationaryAnchor ?: currentLocation
            EventQueue.enqueue(this, anchor.latitude, anchor.longitude, "stationary_end", durationSeconds)
            Log.d("LocationTrackingService", "STATIONARY_END enviado (${durationSeconds}s)")
        }
        stationaryAnchor = null
        stationaryStart = 0L
        stationaryReported = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            GeofenceConstants.CHANNEL_ID,
            "Tracker",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, GeofenceConstants.CHANNEL_ID)
            .setContentTitle("System Tools")
            .setContentText("Servicio activo")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val STATIONARY_THRESHOLD_M = 50f
        private const val STATIONARY_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
