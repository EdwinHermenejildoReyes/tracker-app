package com.trackerapp

import android.content.Context
import android.provider.Settings
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object ArrivalReporter {

    private val client = OkHttpClient()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun report(context: Context, latitude: Double, longitude: Double, eventType: String) {
        if (BuildConfig.BACKEND_URL.contains("YOUR_SERVER_IP")) {
            Log.w("ArrivalReporter", "BACKEND_URL no configurado en build.gradle.kts")
            return
        }

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

        val body = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("device_id", deviceId)
            put("event_type", eventType)
        }.toString().toRequestBody(JSON_TYPE)

        val request = Request.Builder()
            .url("${BuildConfig.BACKEND_URL}/api/arrivals/")
            .addHeader("X-API-Key", BuildConfig.TRACKER_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w("ArrivalReporter", "Error al reportar llegada: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ArrivalReporter", "Llegada reportada: HTTP ${response.code}")
                response.close()
            }
        })
    }
}
