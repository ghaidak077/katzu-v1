package com.example.katzu.data

import com.example.katzu.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct HTTP client for user progress synchronization with Katzu Cloudflare Worker backend.
 * Base URL: https://katzu-auth-worker.ghaidakalosh008.workers.dev
 */
class ProgressApiClient(
    private val baseUrl: String = com.example.katzu.BuildConfig.WORKER_BASE_URL.ifBlank { "https://katzu-auth-worker.ghaidakalosh008.workers.dev" }
) {

    data class CloudTrainingItem(
        val scenarioId: String,
        val studiedAt: Long?,
        val quizAttempted: Boolean,
        val lastScore: Int?,
        val effectiveLevel: String?,
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class CloudStats(
        val level: String = "A1",
        val streakDays: Int = 0,
        val totalPoints: Int = 0,
        val lastActiveDate: String? = null,
        val practiceHours: Double = 0.0,
        val fluencyRatePercent: Double = 0.0,
        val updatedAt: Long = System.currentTimeMillis()
    )

    data class CloudProgressPayload(
        val updatedAt: Long?,
        val stats: CloudStats,
        val trainings: List<CloudTrainingItem>,
        val savedWordIds: List<String>
    )

    private fun postJson(endpoint: String, jsonBody: JSONObject): String {
        val targetUrl = "$baseUrl$endpoint"
        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
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

        AppLogger.i("ProgressApiClient", "<-- HTTP $code from $endpoint")

        if (code in 200..299) {
            return response
        } else {
            AppLogger.e("ProgressApiClient", "HTTP Error $code from $endpoint. Error response: $response")
            throw IllegalStateException("HTTP $code: $response")
        }
    }

    suspend fun syncProgress(
        idToken: String,
        stats: CloudStats,
        trainings: List<CloudTrainingItem>,
        savedWordIds: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) {
            AppLogger.w("ProgressApiClient", "syncProgress skipped: blank idToken")
            return@withContext false
        }

        try {
            val statsJson = JSONObject().apply {
                put("level", stats.level)
                put("streak_days", stats.streakDays)
                put("total_points", stats.totalPoints)
                put("last_active_date", stats.lastActiveDate ?: JSONObject.NULL)
                put("practice_hours", stats.practiceHours)
                put("fluency_rate_percent", stats.fluencyRatePercent)
                put("updated_at", stats.updatedAt)
            }

            val trainingsJson = JSONArray().apply {
                trainings.forEach { item ->
                    put(JSONObject().apply {
                        put("scenario_id", item.scenarioId)
                        put("studied_at", item.studiedAt ?: JSONObject.NULL)
                        put("quiz_attempted", item.quizAttempted)
                        put("last_score", item.lastScore ?: JSONObject.NULL)
                        put("effective_level", item.effectiveLevel ?: JSONObject.NULL)
                        put("study_completed", item.studiedAt != null)
                        put("quiz_completed", item.quizAttempted)
                        put("chat_unlocked", item.studiedAt != null && item.quizAttempted)
                        put("updated_at", item.updatedAt)
                    })
                }
            }

            val savedWordsJson = JSONArray().apply {
                savedWordIds.forEach { put(it) }
            }

            val reqBody = JSONObject().apply {
                put("id_token", idToken.trim())
                put("stats", statsJson)
                put("trainings", trainingsJson)
                put("saved_word_ids", savedWordsJson)
            }

            val responseStr = postJson("/progress/sync", reqBody)
            val json = JSONObject(responseStr)
            val success = json.optBoolean("success", false)
            AppLogger.i("ProgressApiClient", "syncProgress result: success=$success")
            success
        } catch (e: Exception) {
            AppLogger.e("ProgressApiClient", "syncProgress failed: ${e::class.java.name}: ${e.message}", e)
            false
        }
    }

    suspend fun fetchProgress(idToken: String): CloudProgressPayload? = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) {
            AppLogger.w("ProgressApiClient", "fetchProgress skipped: blank idToken")
            return@withContext null
        }

        try {
            val reqBody = JSONObject().apply {
                put("id_token", idToken.trim())
            }
            val responseStr = postJson("/progress/get", reqBody)
            val json = JSONObject(responseStr)

            val updatedAt = if (json.has("updated_at") && !json.isNull("updated_at")) json.optLong("updated_at") else null

            val statsObj = json.optJSONObject("stats")
            val stats = if (statsObj != null) {
                CloudStats(
                    level = statsObj.optString("level", "A1"),
                    streakDays = statsObj.optInt("streak_days", 0),
                    totalPoints = statsObj.optInt("total_points", 0),
                    lastActiveDate = if (statsObj.has("last_active_date") && !statsObj.isNull("last_active_date")) statsObj.getString("last_active_date") else null,
                    practiceHours = statsObj.optDouble("practice_hours", 0.0),
                    fluencyRatePercent = statsObj.optDouble("fluency_rate_percent", 0.0),
                    updatedAt = statsObj.optLong("updated_at", System.currentTimeMillis())
                )
            } else {
                CloudStats()
            }

            val trainingsList = mutableListOf<CloudTrainingItem>()
            val trainingsArr = json.optJSONArray("trainings")
            if (trainingsArr != null) {
                for (i in 0 until trainingsArr.length()) {
                    val t = trainingsArr.optJSONObject(i) ?: continue
                    val scenarioId = t.optString("scenario_id", "").ifBlank {
                        t.optString("scenarioId", "")
                    }
                    if (scenarioId.isBlank()) continue

                    val studiedAt = if (t.has("studied_at") && !t.isNull("studied_at")) {
                        t.optLong("studied_at")
                    } else if (t.optBoolean("study_completed", false)) {
                        System.currentTimeMillis()
                    } else null

                    val quizAttempted = t.optBoolean("quiz_attempted", false) || t.optBoolean("quiz_completed", false)
                    val lastScore = if (t.has("last_score") && !t.isNull("last_score")) t.optInt("last_score") else null
                    val effectiveLevel = if (t.has("effective_level") && !t.isNull("effective_level")) t.optString("effective_level") else null
                    val itemUpdatedAt = t.optLong("updated_at", System.currentTimeMillis())

                    trainingsList.add(
                        CloudTrainingItem(
                            scenarioId = scenarioId,
                            studiedAt = studiedAt,
                            quizAttempted = quizAttempted,
                            lastScore = lastScore,
                            effectiveLevel = effectiveLevel,
                            updatedAt = itemUpdatedAt
                        )
                    )
                }
            }

            val savedWordsList = mutableListOf<String>()
            val wordsArr = json.optJSONArray("saved_word_ids")
            if (wordsArr != null) {
                for (i in 0 until wordsArr.length()) {
                    val id = wordsArr.optString(i, "")
                    if (id.isNotBlank()) savedWordsList.add(id)
                }
            }

            AppLogger.i("ProgressApiClient", "fetchProgress success: ${trainingsList.size} trainings, ${savedWordsList.size} saved words")
            CloudProgressPayload(
                updatedAt = updatedAt,
                stats = stats,
                trainings = trainingsList,
                savedWordIds = savedWordsList
            )
        } catch (e: Exception) {
            AppLogger.e("ProgressApiClient", "fetchProgress failed: ${e::class.java.name}: ${e.message}", e)
            null
        }
    }
}
