package com.trackerapp

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.trackerapp.db.AppDatabase

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).eventDao()
        val events = dao.getAll()
        if (events.isEmpty()) return Result.success()

        for (event in events) {
            val sent = ArrivalReporter.report(event)
            if (!sent) {
                Log.w("UploadWorker", "Falló envío de ${event.eventId}, reintentando después")
                return Result.retry()
            }
            dao.deleteById(event.eventId)
            Log.d("UploadWorker", "Enviado y eliminado: ${event.eventId}")
        }
        return Result.success()
    }
}
