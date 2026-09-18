package com.example.katzu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct HTTP client for Katzu Cloudflare Worker backend.
 * Base URL: https://katzu-auth-worker.ghaidakalosh008.workers.dev
 */
class KatzuContentApiClient(
    private val baseUrl: String = com.example.katzu.BuildConfig.WORKER_BASE_URL.ifBlank { "https://katzu-auth-worker.ghaidakalosh008.workers.dev" }
) {

    private fun get(urlString: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.use { it.readText() }
            conn.disconnect()
            return response
        } else {
            val errorStream = conn.errorStream
            val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            conn.disconnect()
            throw IllegalStateException("API error: $code - $errorMsg")
        }
    }

    suspend fun fetchScenarios(): List<ScenarioEntity> = withContext(Dispatchers.IO) {
        val jsonStr = get("$baseUrl/scenarios")
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<ScenarioEntity>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                ScenarioEntity(
                    id = obj.getString("id"),
                    title_de = obj.optString("title_de", ""),
                    title_ar = obj.optString("title_ar", ""),
                    ai_persona = obj.optString("ai_persona", ""),
                    category = obj.optString("category", ""),
                    icon = obj.optString("icon", "💬"),
                    initial_message_a1 = obj.optString("initial_message_a1", ""),
                    initial_message_a2 = obj.optString("initial_message_a2", ""),
                    initial_message_b1 = obj.optString("initial_message_b1", ""),
                    initial_message_b2 = obj.optString("initial_message_b2", "")
                )
            )
        }
        list
    }

    suspend fun fetchScenarioDetail(scenarioId: String): Pair<ScenarioEntity, List<StarterPhraseEntity>> = withContext(Dispatchers.IO) {
        val jsonStr = get("$baseUrl/scenarios/$scenarioId")
        val obj = JSONObject(jsonStr)
        val scenario = ScenarioEntity(
            id = obj.getString("id"),
            title_de = obj.optString("title_de", ""),
            title_ar = obj.optString("title_ar", ""),
            ai_persona = obj.optString("ai_persona", ""),
            category = obj.optString("category", ""),
            icon = obj.optString("icon", "💬"),
            initial_message_a1 = obj.optString("initial_message_a1", ""),
            initial_message_a2 = obj.optString("initial_message_a2", ""),
            initial_message_b1 = obj.optString("initial_message_b1", ""),
            initial_message_b2 = obj.optString("initial_message_b2", "")
        )

        val phrases = mutableListOf<StarterPhraseEntity>()
        val phrasesArray = obj.optJSONArray("starter_phrases")
        if (phrasesArray != null) {
            for (i in 0 until phrasesArray.length()) {
                val p = phrasesArray.getJSONObject(i)
                phrases.add(
                    StarterPhraseEntity(
                        id = p.optInt("id", (scenarioId.hashCode() * 31) + i),
                        scenario_id = p.optString("scenario_id", scenarioId),
                        level = p.optString("level", "A1"),
                        german = p.optString("german", ""),
                        translation_en = p.optString("translation_en", ""),
                        translation_ar = p.optString("translation_ar", ""),
                        sort_order = p.optInt("sort_order", i + 1)
                    )
                )
            }
        }
        Pair(scenario, phrases)
    }

    suspend fun fetchVocabulary(level: String? = null, topic: String? = null): List<VocabularyEntity> = withContext(Dispatchers.IO) {
        val queryParams = mutableListOf<String>()
        if (!level.isNullOrBlank()) queryParams.add("level=$level")
        if (!topic.isNullOrBlank()) queryParams.add("topic=$topic")
        val url = if (queryParams.isEmpty()) {
            "$baseUrl/vocabulary"
        } else {
            "$baseUrl/vocabulary?${queryParams.joinToString("&")}"
        }

        val jsonStr = get(url)
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<VocabularyEntity>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                VocabularyEntity(
                    id = obj.getInt("id"),
                    german = obj.optString("german", ""),
                    article = obj.optString("article", ""),
                    plural = obj.optString("plural", ""),
                    part_of_speech = obj.optString("part_of_speech", ""),
                    translation_ar = obj.optString("translation_ar", ""),
                    translation_en = obj.optString("translation_en", ""),
                    example_de = obj.optString("example_de", ""),
                    example_ar = obj.optString("example_ar", ""),
                    example_en = obj.optString("example_en", ""),
                    level = obj.optString("level", "A1"),
                    topic = obj.optString("topic", "general")
                )
            )
        }
        list
    }

    suspend fun fetchGrammar(level: String? = null): List<GrammarEntity> = withContext(Dispatchers.IO) {
        val url = if (level.isNullOrBlank()) {
            "$baseUrl/grammar"
        } else {
            "$baseUrl/grammar?level=$level"
        }

        val jsonStr = get(url)
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<GrammarEntity>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                GrammarEntity(
                    id = obj.getString("id"),
                    level = obj.optString("level", "A1"),
                    title_ar = obj.optString("title_ar", ""),
                    title_en = obj.optString("title_en", ""),
                    explanation_ar = obj.optString("explanation_ar", ""),
                    explanation_en = obj.optString("explanation_en", ""),
                    example_de = obj.optString("example_de", "")
                )
            )
        }
        list
    }

    private fun postJson(urlString: String, jsonBody: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.use { it.readText() }
            conn.disconnect()
            return response
        } else {
            val errorStream = conn.errorStream
            val errorMsg = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            conn.disconnect()
            throw IllegalStateException("API error: $code - $errorMsg")
        }
    }

    /**
     * Sends user message to the Worker conversation endpoint.
     * Returns Triple(roleplayReplyGerman, translationArabic, correctionDetailsNullable)
     */
    suspend fun sendConversationTurn(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList()
    ): Triple<String, String, String?> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("scenario_id", scenarioId)
            put("user_message", userMessage)
            val histArray = JSONArray()
            history.forEach { (sender, text) ->
                val msgObj = JSONObject().apply {
                    put("role", sender)
                    put("content", text)
                }
                histArray.put(msgObj)
            }
            put("history", histArray)
        }

        val responseStr = postJson("$baseUrl/conversation", payload.toString())
        val jsonObj = JSONObject(responseStr)
        val replyGerman = jsonObj.optString("reply_de", "")
        val transArabic = jsonObj.optString("reply_ar", "")
        val correction = if (jsonObj.has("correction") && !jsonObj.isNull("correction")) {
            jsonObj.optString("correction")
        } else null

        Triple(replyGerman, transArabic, correction)
    }
}

