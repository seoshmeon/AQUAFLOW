package com.zenhold.app.data.cloud

import com.zenhold.app.BuildConfig
import com.zenhold.app.data.local.BreathHoldRecord
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LinkCode(val value: String, val expiresInSeconds: Int)
data class LinkStatus(val linked: Boolean, val telegramUsername: String?, val telegramFirstName: String?)
data class SyncResult(val accepted: Int, val duplicates: Int)

@Singleton
class AquaflowCloudApi @Inject constructor() {
    suspend fun createAnonymousProfile(): CloudCredentials = request("/v1/auth/anonymous") { json ->
        CloudCredentials(json.getString("userId"), json.getString("token"))
    }

    suspend fun createLinkCode(token: String): LinkCode = request("/v1/link/start", token = token) { json ->
        LinkCode(json.getString("code"), json.getInt("expiresInSeconds"))
    }

    suspend fun linkStatus(token: String): LinkStatus = request("/v1/link/status", token = token) { json ->
        LinkStatus(
            linked = json.getBoolean("linked"),
            telegramUsername = json.optString("telegramUsername").takeIf { it.isNotBlank() && it != "null" },
            telegramFirstName = json.optString("telegramFirstName").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    suspend fun syncRecords(token: String, records: List<BreathHoldRecord>): SyncResult {
        val array = JSONArray().apply {
            records.forEach { record ->
                put(JSONObject().apply {
                    put("clientRecordId", "room-${record.id}")
                    put("sessionId", record.sessionId)
                    put("attemptNumber", record.attemptNumber)
                    put("holdDurationMillis", record.holdDurationMillis)
                    put("recoveryDurationMillis", record.recoveryDurationMillis)
                    put("timestamp", record.timestamp)
                    put("comfortRating", record.comfortRating)
                })
            }
        }
        return request("/v1/records/sync", JSONObject().put("records", array), token) { json ->
            SyncResult(json.getInt("accepted"), json.getInt("duplicates"))
        }
    }

    private suspend fun <T> request(
        path: String,
        body: JSONObject = JSONObject(),
        token: String? = null,
        transform: (JSONObject) -> T,
    ): T = withContext(Dispatchers.IO) {
        var lastNetworkError: IOException? = null
        val origins = listOf(BuildConfig.AQUAFLOW_API_URL, BuildConfig.AQUAFLOW_API_FALLBACK_URL).distinct()
        origins.forEach { origin ->
            try {
                return@withContext executeRequest(origin, path, body, token, transform)
            } catch (error: CloudResponseException) {
                throw error
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }
        throw IOException(
            "Не удалось подключиться. Проверьте интернет или настройки DNS и попробуйте снова.",
            lastNetworkError,
        )
    }

    private fun <T> executeRequest(
        origin: String,
        path: String,
        body: JSONObject,
        token: String?,
        transform: (JSONObject) -> T,
    ): T {
        val connection = (URL(origin + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 18_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = responseText.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            if (status !in 200..299) {
                val reason = response.optString("error")
                throw CloudResponseException(
                    if (reason == "unauthorized") "Сеанс синхронизации недействителен"
                    else "Сервер временно недоступен ($status)",
                )
            }
            return transform(response)
        } finally {
            connection.disconnect()
        }
    }

    private class CloudResponseException(message: String) : IOException(message)
}
