package com.trackerapp

import android.content.Context
import android.provider.Settings
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.trackerapp.db.AppDatabase
import com.trackerapp.db.PendingEvent
import java.util.UUID
import java.util.concurrent.TimeUnit

object EventQueue {

    fun enqueue(context: Context, latitude: Double, longitude: Double, eventType: String) {
        val deviceId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: ""
        val event = PendingEvent(
            eventId = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            deviceId = deviceId,
            eventType = eventType,
        )
        AppDatabase.get(context).eventDao().insert(event)
        scheduleUpload(context)
    }

    fun scheduleUpload(context: Context) {
        val work = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload_pending_events",
            ExistingWorkPolicy.KEEP,
            work,
        )
    }
}
