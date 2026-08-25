package com.trackerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_POWER_CONNECTED -> {
                context.startForegroundService(Intent(context, LocationTrackingService::class.java))
                GeofenceManager(context).registerGeofence()
                EventQueue.scheduleUpload(context)
                WatchdogWorker.schedule(context)
            }
        }
    }
}
