package com.trackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            GeofenceConstants.CHANNEL_ID,
            "Tracker Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Geofence arrival alerts"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showArrivalNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, GeofenceConstants.CHANNEL_ID)
            .setContentTitle("Destino alcanzado")
            .setContentText("Has llegado a la ubicación configurada.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(GeofenceConstants.NOTIF_ID, notification)
    }
}
