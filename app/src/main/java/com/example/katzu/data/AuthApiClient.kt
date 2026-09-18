package com.example.katzu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.katzu.util.AppLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Result sealed classes for code verification and status check
 */
sealed class ServerVerifyResult {
    data class Success(val monthsAdded: Int, val expiresAt: String, val email: String? = null) : ServerVerifyResult()
    object AlreadyRedeemed : ServerVerifyResult()
    data class InvalidCode(val reason: String = "invalid") : ServerVerifyResult()
    object NetworkError : ServerVerifyResult()
}

sealed class ServerStatusResult {
    data class Active(val expiresAt: String?, val daysRemaining: Int = 0) : ServerStatusResult()
    object Expired : ServerStatusResult()
    object Error : ServerStatusResult()
}

/**
 * Utility to calculate expiration timestamps without client-side guessing.
 */
object SecurityAndSubscriptionManager {
    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    private fun parseDate(dateStr: String): Date? {
        val trimmed = dateStr.trim()
        for (pattern in isoFormats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = format.parse(trimmed)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    private val defaultIsoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun calculateNewExpiration(currentExpiresAt: String?, monthsAdded: Int): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = calendar.time

        val baseDate: Date = if (!currentExpiresAt.isNullOrBlank()) {
            val parsed = parseDate(currentExpiresAt)
            if (parsed != null && parsed.after(now)) parsed else now
        } else {
            now
        }

        calendar.time = baseDate
        calendar.add(Calendar.MONTH, monthsAdded)
        return defaultIsoFormat.format(calendar.time)
    }

    fun isLocallyActive(expiresAt: String?): Boolean {
        if (expiresAt.isNullOrBlank()) return false
        val parsed = parseDate(expiresAt)
        return parsed != null && parsed.after(Date())
    }

    fun formatExpirationDate(expiresAt: String?): String {
        if (expiresAt.isNullOrBlank()) return "غير محدد"
        val date = parseDate(expiresAt) ?: return expiresAt
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        return sdf.format(date)
    }
}

/**
 * Real API client for Katzu Auth Worker.
 * Base URL: https://katzu-auth-worker.ghaidakalosh008.workers.dev
 */
class AuthApiClient(
    private val baseUrl: String = com.example.katzu.BuildConfig.WORKER_BASE_URL.ifBlank { "https://katzu-auth-worker.ghaidakalosh008.workers.dev" }
) {

    private fun postJson(endpoint: String, jsonBody: JSONObject): String {
        val url = URL("$baseUrl$endpoint")
        AppLogger.i("AuthApiClient", "--> POST $url (payload keys: ${jsonBody.keys().asSequence().toList()})")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonBody.toString())
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.let {
            BufferedReader(InputStreamReader(it, "UTF-8")).use { reader -> reader.readText() }
        } ?: ""
        conn.disconnect()

        AppLogger.i("AuthApiClient", "<-- HTTP $code from $endpoint. Body: $response")

        if (code in 200..299) {
            return response
        } else {
            AppLogger.e("AuthApiClient", "HTTP Error $code from $endpoint. Error response: $response")
            throw IllegalStateException("HTTP $code: $response")
        }
    }

    /**
     * POST /verify
     * Request: { "code": "<string>", "id_token": "<Google ID token>" }
     * Response success: { "valid": true, "months": <number>, "expiresAt": "<ISO 8601 UTC string>", "email": "<string>" }
     * Response failure: { "valid": false, "reason": "already_redeemed" | "invalid_signature" | "malformed" | "missing_id_token" | "invalid_id_token" }
     */
    suspend fun verifySubscriptionCode(
        code: String,
        idToken: String
    ): ServerVerifyResult = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("AuthApiClient", "verifySubscriptionCode called. Code: $code, Token length: ${idToken.length}")
            val reqBody = JSONObject().apply {
                put("code", code.trim())
                put("id_token", idToken.trim())
            }
            val responseStr = postJson("/verify", reqBody)
            val json = JSONObject(responseStr)

            val isValid = json.optBoolean("valid", false)
            if (isValid) {
                val months = json.optInt("months", 1)
                val expiresAt = json.optString("expiresAt", "")
                val email = if (json.has("email") && !json.isNull("email")) json.getString("email") else null
                AppLogger.i("AuthApiClient", "verifySubscriptionCode SUCCESS. months: $months, expiresAt: $expiresAt, email: $email")
                ServerVerifyResult.Success(monthsAdded = months, expiresAt = expiresAt, email = email)
            } else {
                val reason = json.optString("reason", "invalid")
                AppLogger.w("AuthApiClient", "verifySubscriptionCode INVALID: reason=$reason")
                if (reason == "already_redeemed") {
                    ServerVerifyResult.AlreadyRedeemed
                } else {
                    ServerVerifyResult.InvalidCode(reason)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AuthApiClient", "verifySubscriptionCode failed with exception: ${e::class.java.name}: ${e.message}", e)
            ServerVerifyResult.NetworkError
        }
    }

    /**
     * POST /check-status
     * Request: { "id_token": "<Google ID token>" }
     * Response: { "active": <bool>, "days_remaining": <number>, "server_time": "<ISO>", "expiresAt": "<ISO string or null>" }
     */
    suspend fun checkSubscriptionStatus(
        idToken: String
    ): ServerStatusResult = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) {
            AppLogger.w("AuthApiClient", "checkSubscriptionStatus called with blank idToken")
            return@withContext ServerStatusResult.Expired
        }

        try {
            AppLogger.i("AuthApiClient", "checkSubscriptionStatus called. Token length: ${idToken.length}")
            val reqBody = JSONObject().apply {
                put("id_token", idToken.trim())
            }
            val responseStr = postJson("/check-status", reqBody)
            val json = JSONObject(responseStr)

            val isActive = json.optBoolean("active", false)
            val daysRemaining = json.optInt("days_remaining", 0)
            val expiresAt = if (json.has("expiresAt") && !json.isNull("expiresAt")) json.getString("expiresAt") else null

            AppLogger.i("AuthApiClient", "checkSubscriptionStatus result: active=$isActive, daysRemaining=$daysRemaining, expiresAt=$expiresAt")
            if (isActive) {
                ServerStatusResult.Active(expiresAt = expiresAt, daysRemaining = daysRemaining)
            } else {
                ServerStatusResult.Expired
            }
        } catch (e: Exception) {
            AppLogger.e("AuthApiClient", "checkSubscriptionStatus network failure: ${e::class.java.name}: ${e.message}", e)
            // Network failure must NOT lock user out of an active subscription
            ServerStatusResult.Error
        }
    }
}
