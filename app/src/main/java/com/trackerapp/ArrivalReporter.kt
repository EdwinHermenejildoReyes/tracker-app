package com.trackerapp

import android.util.Log
import com.trackerapp.db.PendingEvent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ArrivalReporter {

    private val client = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun report(event: PendingEvent): Boolean {
        if (BuildConfig.BACKEND_URL.contains("YOUR_SERVER_IP")) {
            Log.w("ArrivalReporter", "BACKEND_URL no configurado en build.gradle.kts")
            return true
        }

        val body = JSONObject().apply {
            put("event_id", event.eventId)
            put("latitude", event.latitude)
            put("longitude", event.longitude)
            put("device_id", event.deviceId)
            put("event_type", event.eventType)
            event.durationSeconds?.let { put("duration_seconds", it) }
        }.toString().toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_URL}/api/arrivals/")
            .addHeader("X-API-Key", BuildConfig.TRACKER_API_KEY)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                Log.d("ArrivalReporter", "HTTP ${response.code} para ${event.eventType} ${event.eventId}")
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.w("ArrivalReporter", "Sin red: ${e.message}")
            false
        }
    }
}
