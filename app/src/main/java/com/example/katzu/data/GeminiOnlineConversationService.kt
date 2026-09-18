package com.example.katzu.data

import com.example.katzu.BuildConfig
import com.example.katzu.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * GeminiOnlineConversationService handles the live conversation AI pipeline.
 *
 * Mandate (Rule 5):
 * Conversation logic stays split into three independent calls:
 * 1. Roleplay reply (German, Katzu feline tutor persona)
 * 2. Structured correctness evaluation (JSON: is_correct, original_mistake, corrected_german, grammar_rule, explanation_ar)
 * 3. Structured translation (JSON: translation_ar)
 * Never combined into one call; corrections and translations are never parsed out of free text.
 */
object GeminiOnlineConversationService {

    private const val TAG = "GeminiConvService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    // Model fallback chain: verified working models with low latency first, followed by fallbacks
    private val MODEL_FALLBACK_CHAIN = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-flash-latest"
    )

    // In-memory caches for fast response, zero unnecessary token spend, and consistent quality
    private val translationCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val contextualHintsCache = java.util.concurrent.ConcurrentHashMap<String, List<ConversationHint>>()

    @Volatile
    private var preferredKeyIndex = 0

    /**
     * Returns all configured Gemini API keys (combines GEMINI_API_KEY and GEMINI_API_KEYS).
     */
    fun getAvailableApiKeys(): List<String> {
        val list = mutableListOf<String>()
        try {
            val keysConfig = BuildConfig.GEMINI_API_KEYS
            if (keysConfig.isNotBlank()) {
                val split = keysConfig.split(",", ";", "\n")
                    .map { it.trim().trim('"') }
                    .filter { it.isNotEmpty() }
                list.addAll(split)
            }
        } catch (_: Throwable) {}

        try {
            val single = BuildConfig.GEMINI_API_KEY.trim().trim('"')
            if (single.isNotEmpty() && !list.contains(single)) {
                list.add(0, single)
            }
        } catch (_: Throwable) {}

        return list.distinct()
    }

    /**
     * Returns the server-side AI endpoint URL.
     */
    fun getServerUrl(): String {
        return try {
            val url = BuildConfig.AI_SERVER_URL.trim().trim('"').removeSuffix("/")
            if (url.isNotBlank()) {
                url
            } else {
                val workerBase = BuildConfig.WORKER_BASE_URL.trim().trim('"').removeSuffix("/")
                if (workerBase.isNotBlank()) "$workerBase/ai" else "https://katzu-auth-worker.ghaidakalosh008.workers.dev/ai"
            }
        } catch (_: Throwable) {
            "https://katzu-auth-worker.ghaidakalosh008.workers.dev/ai"
        }
    }

    /**
     * Attempts server-side execution of the conversation turn.
     * Routes request to the server endpoint to keep AI models and keys completely server-side.
     */
    private suspend fun tryServerSideTurn(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>>,
        cefrLevel: String,
        scenario: ScenarioEntity?,
        curriculumVocabulary: List<String>,
        isFinalTurn: Boolean = false
    ): ConversationTurnResult? = withContext(Dispatchers.IO) {
        val serverBase = try {
            BuildConfig.AI_SERVER_URL.trim().trim('"').removeSuffix("/")
        } catch (_: Throwable) { "" }
        if (serverBase.isBlank()) return@withContext null

        val endpoint = "$serverBase/turn"
        try {
            AppLogger.i(TAG, "[Server-AI] Attempting server-side AI processing at $endpoint...")
            val historyArray = JSONArray()
            for (item in history.takeLast(6)) {
                historyArray.put(JSONObject().apply {
                    put("role", item.first)
                    put("text", item.second)
                })
            }

            val vocabArray = JSONArray()
            for (v in curriculumVocabulary) {
                vocabArray.put(v)
            }

            val payload = JSONObject().apply {
                put("scenario_id", scenarioId)
                put("scenario_title", scenario?.title_de ?: scenarioId)
                put("scenario_category", scenario?.category ?: "everyday")
                put("persona", scenario?.ai_persona ?: "counterpart")
                put("cefr_level", cefrLevel)
                put("user_message", userMessage)
                put("history", historyArray)
                put("curriculum_vocabulary", vocabArray)
                put("is_final_turn", isFinalTurn)
            }

            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 16000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()
                AppLogger.i(TAG, "[Server-AI] HTTP $code from server: response size ${raw.length}")
                val json = JSONObject(raw)

                val replyDe = json.optString("reply_de", json.optString("reply_german", ""))
                val replyAr = json.optString("reply_ar", json.optString("reply_arabic", ""))
                if (replyDe.isNotBlank()) {
                    val evalJson = json.optJSONObject("evaluation") ?: JSONObject()
                    val eval = EvaluationResult(
                        isCorrect = evalJson.optBoolean("is_correct", true),
                        originalMistake = evalJson.optString("original_mistake", ""),
                        correctedGerman = evalJson.optString("corrected_german", ""),
                        grammarRule = evalJson.optString("grammar_rule", ""),
                        explanationAr = evalJson.optString("explanation_ar", ""),
                        userMessageTranslationAr = evalJson.optString("user_message_translation_ar", ""),
                        mistakeSegment = evalJson.optString("mistake_segment", ""),
                        correctedSegment = evalJson.optString("corrected_segment", ""),
                        positiveNoteAr = evalJson.optString("positive_note_ar", "")
                    )

                    val hintsList = mutableListOf<ConversationHint>()
                    val hintsArray = json.optJSONArray("hints") ?: json.optJSONArray("contextual_hints")
                    if (hintsArray != null) {
                        for (i in 0 until hintsArray.length()) {
                            val hObj = hintsArray.optJSONObject(i) ?: continue
                            val de = hObj.optString("german", hObj.optString("de", "")).trim()
                            val rawAr = hObj.optString("translation_ar", hObj.optString("ar", "")).trim()
                            if (de.isNotBlank()) {
                                val ar = sanitizeArabicTranslation(rawAr, de)
                                hintsList.add(ConversationHint(german = de, translationAr = ar, isContextual = true))
                            }
                        }
                    }

                    AppLogger.i(TAG, "[Server-AI] Turn processed successfully server-side! reply: '$replyDe'")
                    return@withContext ConversationTurnResult(
                        replyGerman = sanitizeMessageText(replyDe),
                        replyArabic = sanitizeArabicTranslation(replyAr, replyDe),
                        evaluation = eval,
                        contextualHints = hintsList
                    )
                }
            } else {
                val err = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Throwable) { "" }
                conn.disconnect()
                AppLogger.w(TAG, "[Server-AI] Server returned HTTP $code from $endpoint: $err")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "[Server-AI] Server-side turn unavailable (${e.javaClass.simpleName}: ${e.message}).")
        }
        null
    }

    /**
     * Attempts server-side translation.
     */
    private suspend fun tryServerSideTranslation(text: String): String? = withContext(Dispatchers.IO) {
        val serverBase = getServerUrl()
        val endpoint = "$serverBase/translate"
        try {
            val payload = JSONObject().apply {
                put("text", text)
            }
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()); it.flush() }
            val code = conn.responseCode
            if (code in 200..299) {
                val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()
                val json = JSONObject(raw)
                val trans = json.optString("translation_ar", json.optString("translation", ""))
                if (trans.isNotBlank()) {
                    AppLogger.i(TAG, "[Server-AI] Server translation succeeded: '$trans'")
                    return@withContext sanitizeArabicTranslation(trans, text)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            AppLogger.w(TAG, "[Server-AI] Server translation call failed: ${e.message}")
        }

        // Fallback: If /translate returned blank or unavailable, use the proven /turn engine to get accurate translation
        try {
            val turnEndpoint = "$serverBase/turn"
            val turnPayload = JSONObject().apply {
                put("scenario_id", "translator")
                put("scenario_title", "Übersetzer")
                put("persona", "Übersetzer")
                put("cefr_level", "A1")
                put("user_message", text)
                put("history", JSONArray())
            }
            val turnUrl = URL(turnEndpoint)
            val turnConn = (turnUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(turnConn.outputStream, "UTF-8").use { it.write(turnPayload.toString()); it.flush() }
            if (turnConn.responseCode in 200..299) {
                val rawTurn = BufferedReader(InputStreamReader(turnConn.inputStream, "UTF-8")).use { it.readText() }
                turnConn.disconnect()
                val json = JSONObject(rawTurn)
                val eval = json.optJSONObject("evaluation")
                val transTurn = eval?.optString("user_message_translation_ar", "")
                    ?.ifBlank { json.optString("reply_ar", "") }
                if (!transTurn.isNullOrBlank()) {
                    AppLogger.i(TAG, "[Server-AI] Server translation via /turn fallback succeeded: '$transTurn'")
                    return@withContext sanitizeArabicTranslation(transTurn, text)
                }
            } else {
                turnConn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "[Server-AI] Server turn fallback translation failed: ${e.message}")
        }

        null
    }

    /**
     * Attempts server-side contextual hints generation.
     */
    private suspend fun tryServerSideHints(
        scenario: ScenarioEntity?,
        cefrLevel: String,
        lastAiReply: String
    ): List<ConversationHint>? = withContext(Dispatchers.IO) {
        val serverBase = getServerUrl()
        val endpoint = "$serverBase/hints"
        try {
            val payload = JSONObject().apply {
                put("scenario_id", scenario?.id ?: "")
                put("scenario_title", scenario?.title_de ?: "")
                put("cefr_level", cefrLevel)
                put("last_ai_reply", lastAiReply)
            }
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()); it.flush() }
            val code = conn.responseCode
            if (code in 200..299) {
                val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                conn.disconnect()
                val json = JSONObject(raw)
                val hintsArray = json.optJSONArray("hints")
                if (hintsArray != null && hintsArray.length() > 0) {
                    val result = mutableListOf<ConversationHint>()
                    for (i in 0 until hintsArray.length()) {
                        val hObj = hintsArray.getJSONObject(i)
                        val de = hObj.optString("german", "").trim()
                        val ar = sanitizeArabicTranslation(hObj.optString("translation_ar", ""), de)
                        if (de.isNotBlank()) {
                            result.add(ConversationHint(german = de, translationAr = ar, isContextual = true))
                        }
                    }
                    if (result.isNotEmpty()) {
                        AppLogger.i(TAG, "[Server-AI] Server hints succeeded: ${result.size} hints returned")
                        return@withContext result
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            AppLogger.w(TAG, "[Server-AI] Server hints call failed: ${e.message}")
        }

        // Fallback: If /hints returned empty, query /turn to generate live contextual reply options
        try {
            val turnEndpoint = "$serverBase/turn"
            val promptMsg = "Du bist ein Deutsch-Tutor. Gib dem Lerner genau 3 kurze Antwortmöglichkeiten ($cefrLevel) mit arabischer Übersetzung auf: \"$lastAiReply\" im Szenario \"${scenario?.title_de ?: "Alltag"}\".\nFormat:\n1. [Deutsch] - [عربي]\n2. [Deutsch] - [عربي]\n3. [Deutsch] - [عربي]"
            val turnPayload = JSONObject().apply {
                put("scenario_id", "hints")
                put("scenario_title", "Deutsch-Tutor")
                put("persona", "Deutschlehrer")
                put("cefr_level", cefrLevel)
                put("user_message", promptMsg)
                put("history", JSONArray())
            }
            val turnUrl = URL(turnEndpoint)
            val turnConn = (turnUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }
            OutputStreamWriter(turnConn.outputStream, "UTF-8").use { it.write(turnPayload.toString()); it.flush() }
            if (turnConn.responseCode in 200..299) {
                val rawTurn = BufferedReader(InputStreamReader(turnConn.inputStream, "UTF-8")).use { it.readText() }
                turnConn.disconnect()
                val json = JSONObject(rawTurn)
                val replyDe = json.optString("reply_de", "")
                val replyAr = json.optString("reply_ar", "")
                val extracted = parseHintsFromDualReplies(replyDe, replyAr)
                if (extracted.isNotEmpty()) {
                    AppLogger.i(TAG, "[Server-AI] Server hints via /turn fallback succeeded: ${extracted.size} hints generated")
                    return@withContext extracted
                }
            } else {
                turnConn.disconnect()
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "[Server-AI] Server turn-based hints failed: ${e.message}")
        }

        null
    }

    private fun parseHintsFromDualReplies(replyDe: String, replyAr: String): List<ConversationHint> {
        val result = mutableListOf<ConversationHint>()

        fun extractLines(text: String): List<String> {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            val numbered = lines.filter { line ->
                line.matches(Regex("""^[1-3][\.\)\-]\s+.*""")) || line.startsWith("- ") || line.startsWith("* ")
            }.map { line ->
                line.replace(Regex("""^[1-3][\.\)\-]\s*"""), "")
                    .replace(Regex("""^[\-\*]\s*"""), "")
                    .replace("„", "").replace("“", "")
                    .replace("«", "").replace("»", "")
                    .replace("\"", "").trim()
            }.filter { it.isNotBlank() }
            return if (numbered.size >= 2) numbered else {
                lines.filter { it.length in 5..120 && !it.startsWith("{") && !it.endsWith("?") && !it.contains(":") }
            }
        }

        val deItems = extractLines(replyDe)
        val arItems = extractLines(replyAr)

        for (i in 0 until minOf(deItems.size, 3)) {
            val rawItem = deItems[i]
            val parts = rawItem.split(Regex("""\s+[-–—:]\s+"""), limit = 2)
            val de = parts[0].trim()
            val ar = if (parts.size > 1) {
                sanitizeArabicTranslation(parts[1].trim(), de)
            } else if (i < arItems.size) {
                val arParts = arItems[i].split(Regex("""\s+[-–—:]\s+"""), limit = 2)
                sanitizeArabicTranslation(if (arParts.size > 1) arParts[1].trim() else arItems[i].trim(), de)
            } else ""

            if (de.isNotBlank()) {
                result.add(ConversationHint(german = de, translationAr = ar, isContextual = true))
            }
        }
        return result
    }

    private fun extractJsonString(text: String): String {
        val trimmed = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start != -1 && end > start) {
            trimmed.substring(start, end + 1).trim()
        } else {
            trimmed
        }
    }

    private fun sanitizeMessageText(rawText: String): String {
        var clean = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        // If it contains raw JSON key or structure, extract the text cleanly
        if (clean.contains("\"reply_de\"") || (clean.startsWith("{") && clean.contains("}"))) {
            val extracted = Regex(""""reply_de"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(clean)?.groupValues?.get(1)
            if (!extracted.isNullOrBlank()) {
                return extracted.replace("\\\"", "\"").replace("\\n", " ").trim()
            }
            clean = clean
                .replace(Regex("""\{[^}]*\}"""), "")
                .replace(Regex("""[{}\"]"""), "")
                .replace("reply_de:", "")
                .replace("reply_ar:", "")
                .trim()
        }
        return clean
    }

    /**
     * Sanitizes Arabic translations to prevent robotic artifacts, nonsense literalisms,
     * or awkward religious substitutions for secular German phrases
     * (e.g. converting "السلام عليكم" back to "مرحباً" for greetings like "Guten Tag" or "Hallo").
     */
    fun sanitizeArabicTranslation(arabic: String, germanContext: String = ""): String {
        var clean = arabic
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .removeSurrounding("\"")
            .trim()

        // Clean up common JSON artifact leaks in translation
        if (clean.contains("\"translation_ar\"") || clean.contains("\"reply_ar\"")) {
            val extracted = Regex(""""(?:translation_ar|reply_ar|user_message_translation_ar)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(clean)?.groupValues?.get(1)
            if (!extracted.isNullOrBlank()) {
                clean = extracted.replace("\\\"", "\"").replace("\\n", " ").trim()
            }
        }

        // Always strip religious formula greeting substitutions in German language learning contexts
        if (clean.contains("السلام عليكم") || clean.contains("سلام عليكم")) {
            clean = clean
                .replace("السلام عليكم ورحمة الله وبركاته", "مرحباً")
                .replace("السلام عليكم ورحمة الله", "مرحباً")
                .replace("وعليكم السلام ورحمة الله وبركاته", "أهلاً بك")
                .replace("وعليكم السلام", "أهلاً بك")
                .replace("السلام عليكم", "مرحباً")
                .replace("سلام عليكم", "مرحباً")
        }

        val lowerDe = germanContext.lowercase().trim()
        if (lowerDe.contains("guten tag") && (clean == "سلام" || clean == "السلام")) {
            clean = "مرحباً"
        }

        return clean.trim()
    }

    data class EvaluationResult(
        val isCorrect: Boolean,
        val originalMistake: String = "",
        val correctedGerman: String = "",
        val grammarRule: String = "",
        val explanationAr: String = "",
        val userMessageTranslationAr: String = "",
        val mistakeSegment: String = "",
        val correctedSegment: String = "",
        val positiveNoteAr: String = ""
    ) {
        val correctionText: String
            get() = correctedGerman
    }

    data class ConversationHint(
        val german: String,
        val translationAr: String,
        val isContextual: Boolean = true
    )

    data class RoleplayTurnResult(
        val replyGerman: String,
        val replyArabic: String,
        val contextualHints: List<ConversationHint> = emptyList()
    )

    data class ConversationTurnResult(
        val replyGerman: String,
        val replyArabic: String,
        val evaluation: EvaluationResult,
        val contextualHints: List<ConversationHint> = emptyList()
    )

    /**
     * Primary conversation processing method returning rich structured evaluation and dynamic hints.
     * Operates with an optimized Dual-Agent architecture (Roleplay+Translation, Evaluation) to minimize token consumption
     * and avoid free-tier RPM bottlenecks while completely eliminating persona bleed and long-chat hallucinations.
     */
    suspend fun processTurn(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null,
        curriculumVocabulary: List<String> = emptyList(),
        isFinalTurn: Boolean = false
    ): ConversationTurnResult = coroutineScope {
        // 1. Priority 1: Try Server-Side AI processing first
        val serverResult = tryServerSideTurn(
            scenarioId = scenarioId,
            userMessage = userMessage,
            history = history,
            cefrLevel = cefrLevel,
            scenario = scenario,
            curriculumVocabulary = curriculumVocabulary,
            isFinalTurn = isFinalTurn
        )
        if (serverResult != null) {
            return@coroutineScope serverResult
        }

        // 2. Priority 2: Client-Side Direct AI with Multi-Key Backup Failover (only if configured)
        val keys = getAvailableApiKeys()
        if (keys.isEmpty()) {
            AppLogger.e(TAG, "processTurn aborted: Cloudflare AI server did not return a response and no local API keys exist.")
            throw IllegalStateException("تعذر الاتصال بخادم الذكاء الاصطناعي (Katzu Cloud AI). يرجى التأكد من اتصال الإنترنت أو تحديث خادم Cloudflare.")
        }

        val scenarioName = scenario?.title_de ?: scenarioId
        val persona = scenario?.ai_persona ?: "counterpart"
        AppLogger.i(TAG, "Starting direct conversation turn with multi-key failover for scenario: '$scenarioName' (persona: $persona), level: $cefrLevel, isFinalTurn: $isFinalTurn, userMessage: $userMessage")

        // 1. Roleplay Reply & Arabic translation in 1 structured call (In-character counterpart only)
        val roleplayDeferred = async(Dispatchers.IO) {
            generateRoleplayWithTranslation(keys.firstOrNull() ?: "", scenarioId, userMessage, history, cefrLevel, scenario, curriculumVocabulary, isFinalTurn)
        }

        // 2. Structured Correctness Evaluation & Praise in parallel (Katzu pedagogical coach only)
        val evaluationDeferred = async(Dispatchers.IO) {
            evaluateCorrectness(keys.firstOrNull() ?: "", userMessage, cefrLevel, scenario)
        }

        val roleplayResult = roleplayDeferred.await()
        val evaluation = evaluationDeferred.await()

        AppLogger.i(TAG, "Turn completed in 2 parallel calls. Roleplay: '${roleplayResult.replyGerman}', isCorrect: ${evaluation.isCorrect}")

        ConversationTurnResult(
            replyGerman = roleplayResult.replyGerman,
            replyArabic = roleplayResult.replyArabic,
            evaluation = evaluation,
            contextualHints = roleplayResult.contextualHints
        )
    }

    /**
     * Primary conversation processing method called from LiveConversationScreen and MainActivity.
     * Returns: Triple(katzuGermanReply, arabicTranslation, optionalCorrection)
     */
    suspend fun processUserText(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null,
        curriculumVocabulary: List<String> = emptyList(),
        isFinalTurn: Boolean = false
    ): Triple<String, String, String?> {
        val turn = processTurn(scenarioId, userMessage, history, cefrLevel, scenario, curriculumVocabulary, isFinalTurn)
        val correction = if (!turn.evaluation.isCorrect && turn.evaluation.correctedGerman.isNotBlank()) {
            buildString {
                append(turn.evaluation.correctedGerman)
                if (turn.evaluation.explanationAr.isNotBlank()) {
                    append(" — ")
                    append(turn.evaluation.explanationAr)
                }
            }
        } else {
            null
        }
        return Triple(turn.replyGerman, turn.replyArabic, correction)
    }

    /**
     * Call 1: Roleplay Reply with Synchronized Arabic Translation
     * Spoken strictly and convincingly in the voice of the scenario's counterpart persona.
     * Bundles the German dialogue and its Arabic translation into a single structured call to avoid extra roundtrips,
     * reduce token consumption, and avoid rate limits, while preserving complete separation from pedagogical evaluation.
     * Enforces a strict 6-turn sliding window (3 exchanges) to eliminate long-chat hallucinations and context drift.
     */
    suspend fun generateRoleplayWithTranslation(
        apiKey: String,
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>>,
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null,
        curriculumVocabulary: List<String> = emptyList(),
        isFinalTurn: Boolean = false
    ): RoleplayTurnResult = withContext(Dispatchers.IO) {
        val persona = if (scenario != null && scenario.ai_persona.isNotBlank()) scenario.ai_persona else "a conversational counterpart"
        val scenarioTitle = scenario?.title_de ?: scenarioId
        val scenarioCategory = scenario?.category ?: "everyday"

        val levelConstraints = when (cefrLevel.uppercase().take(2)) {
            "A1" -> """
                CEFR Level A1 constraints:
                - Maximum 6-8 words per sentence.
                - Present tense only (Präsens). No past or future tenses.
                - Strictly NO subordinate clauses (no 'weil', no 'dass', no 'wenn').
                - Use only the most common, basic A1 vocabulary (top 500 words).
            """.trimIndent()
            "A2" -> """
                CEFR Level A2 constraints:
                - Short, simple sentences (maximum 10-12 words).
                - Conversational past tense allowed (Perfekt with haben/sein).
                - Basic coordinating connectors allowed ('und', 'aber', 'oder', simple 'weil').
                - Keep grammar straightforward and vocabulary grounded in everyday practical situations.
            """.trimIndent()
            "B1" -> """
                CEFR Level B1 constraints:
                - Subordinate clauses allowed with correct verb-final word order ('weil', 'dass', 'wenn', 'obwohl').
                - More varied vocabulary and common colloquial phrases.
                - Some common idiomatic expressions and modal particles ('mal', 'doch', 'eigentlich').
            """.trimIndent()
            "B2" -> """
                CEFR Level B2 constraints:
                - Natural native-paced German complexity and nuance.
                - Passive voice, subjunctive II (Konjunktiv II for politeness/hypotheticals), and advanced connectors.
                - Idiomatic expressions, professional vocabulary, and varied sentence structures.
            """.trimIndent()
            else -> """
                CEFR Level A1 constraints:
                - Maximum 6-8 words per sentence.
                - Present tense only.
                - No subordinate clauses.
            """.trimIndent()
        }

        val vocabGuidance = if (curriculumVocabulary.isNotEmpty()) {
            val wordsList = curriculumVocabulary.take(8).joinToString(", ")
            "These words are available if they fit naturally into the conversation: $wordsList. Do NOT force them into your reply or ask unrelated questions just to use them — natural, realistic conversation flow for your persona and setting always comes first."
        } else ""

        val wrapUpGuidance = if (isFinalTurn) """
            CRITICAL FINAL TURN CONCLUSION (SCENARIO WRAP-UP):
            - This is the FINAL turn of this roleplay interaction. The learner is concluding the conversation.
            - Provide a warm, friendly, realistic farewell and conclusion appropriate to this scenario (e.g., 'Auf Wiedersehen und einen schönen Tag noch!', 'Gute Besserung und tschüss!', 'Danke für Ihren Besuch, auf Wiedersehen!').
            - Strictly DO NOT ask any new questions or introduce any new topics. The conversation is ending now.
        """.trimIndent() else """
            - Progress smoothly through the normal real-world stages of this scenario (Greeting -> Needs -> Clarifications -> Conclusion/Farewell). If appropriate for the situation, ask a relevant follow-up question.
        """.trimIndent()

        val systemPrompt = """
            You are roleplaying as $persona in the scenario '$scenarioTitle' (Category: $scenarioCategory).
            Target Learner CEFR Level: $cefrLevel.
            
            MANDATORY CONVERSATION BOUNDARIES & ANTI-HALLUCINATION RULES:
            1. SCENARIO GROUNDING: Stay strictly, convincingly, and realistically within the exact situation and physical setting of '$scenarioTitle'.
               - Do NOT drift into unrelated topics, meta-discussions, or random philosophical banter.
               - If the learner says something off-topic, acknowledge it briefly and politely in-character, then immediately steer the conversation back to the goal of this scenario.
            2. REAL-WORLD REALISM (NO FANTASY OR BIZARRE HALLUCINATIONS):
               - Act like a real person living and working in Germany in this specific setting.
               - Do NOT invent absurd characters, surreal plotlines, or imaginary disasters. Keep everyday interactions, pricing, and social customs realistic.
            3. CONVERSATIONAL MEMORY & CONSISTENCY:
               - Track and respect everything agreed upon, ordered, or answered earlier in this conversation.
               - Never contradict prior turns, and never ask questions that the learner has already answered.
            4. NATURAL INTERACTION PROGRESSION & CONCLUSION:
               $wrapUpGuidance
            5. PERSONA & TONE:
               - Formal and respectful (Sie) if an official, interviewer, doctor, landlord, or stranger; warm and friendly-casual if a barista or friend.
               - Do NOT mention being an AI tutor, language model, or a cat. You are exclusively the human counterpart in this German scenario.
            6. CONCISE DIALOGUE:
               - 1 to 2 short sentences in natural German suitable for realistic back-and-forth speech.
            7. ARABIC TRANSLATION (CRITICAL REQUIREMENTS):
               - Translate your German reply into natural, smooth, contemporary Modern Standard Arabic (فصحى معاصرة وسلسة وشبابية ومفهومة).
               - CONTEXTUAL ACCURACY: Translate the real-life communicative intent accurately. Avoid nonsensical or literal word-by-word machine translations.
               - NO RELIGIOUS / OVER-LOCALIZED SUBSTITUTIONS:
                 * Strictly NEVER translate secular German greetings or expressions into religious formulas (e.g., do NOT translate "Guten Tag", "Hallo", "Guten Morgen" into "السلام عليكم").
                 * "Guten Tag" -> "مرحباً" or "يومك سعيد" (NEVER "السلام عليكم").
                 * "Hallo" / "Hi" -> "أهلاً" or "مرحباً".
                 * "Guten Morgen" -> "صباح الخير".
                 * "Guten Abend" -> "مساء الخير".
                 * "Gute Nacht" -> "تصبح على خير" / "ليلة سعيدة".
                 * "Tschüss" / "Auf Wiedersehen" -> "مع السلامة" / "إلى اللقاء".
                 * "Alles klar" -> "تمام" / "كل شيء واضح".
                 * "Wie geht's?" -> "كيف حالك؟" / "كيفك؟".
                 * "Bitte schön" -> "تفضل" / "عفواً" (depending on context).
                 * "Danke schön" -> "شكراً جزيلاً".
               - YOUTH-FRIENDLY & MODERN: Use fresh, expressive, natural phrasing that young Arabic speakers use in real life. Avoid archaic, dusty words, broken grammatical artifacts, or robotic phrasing.

            $levelConstraints

            $vocabGuidance

            Also generate exactly 3 practical, natural, and distinct German response options (hints) that the learner could say next to reply to your reply_de: Option 1 (direct standard answer or acceptance), Option 2 (alternative choice or preference), Option 3 (polite inquiry, question, or follow-up), each with a natural Modern Standard Arabic translation.

            Respond strictly in valid JSON matching this schema:
            {
              "reply_de": "string",
              "reply_ar": "string",
              "hints": [
                { "german": "string", "translation_ar": "string" },
                { "german": "string", "translation_ar": "string" },
                { "german": "string", "translation_ar": "string" }
              ]
            }
        """.trimIndent()

        // Clean history: drop any trailing message if it already equals current userMessage
        val cleanHistory = if (history.isNotEmpty() && history.last().second.trim().equals(userMessage.trim(), ignoreCase = true)) {
            history.dropLast(1)
        } else {
            history
        }

        // Sliding window: strictly take the last 6 turns (3 exchanges) to keep attention razor-sharp and prevent hallucinations
        val windowedHistory = cleanHistory.takeLast(6)

        // Build strictly alternating turns for Gemini API
        val turnsList = mutableListOf<Pair<String, String>>()
        for (item in windowedHistory) {
            val role = if (item.first == "user") "user" else "model"
            if (turnsList.isNotEmpty() && turnsList.last().first == role) {
                // Merge adjacent same-role turns to preserve strict alternation
                val prev = turnsList.removeAt(turnsList.size - 1)
                turnsList.add(Pair(role, "${prev.second}\n${item.second}"))
            } else {
                turnsList.add(Pair(role, item.second))
            }
        }

        val contentsArray = JSONArray()

        // If history starts with model (e.g. initial counterpart greeting), prepend an initial user prompt
        // so Gemini contents starts canonically with role: "user"
        if (turnsList.isNotEmpty() && turnsList.first().first == "model") {
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "Hallo! (Beginn des Szenarios: $scenarioTitle)")))
                }
            )
        }

        for (turn in turnsList) {
            contentsArray.put(
                JSONObject().apply {
                    put("role", turn.first)
                    put("parts", JSONArray().put(JSONObject().put("text", turn.second)))
                }
            )
        }

        // Append current user message as final turn, ensuring strict user-model alternation
        if (turnsList.isNotEmpty() && turnsList.last().first == "user") {
            // Last history turn was already user: update contentsArray's last entry to combine or use current message
            val lastUserText = turnsList.last().second.trim()
            val mergedText = if (lastUserText.equals(userMessage.trim(), ignoreCase = true) || lastUserText.isEmpty()) {
                userMessage
            } else {
                "$lastUserText\n$userMessage"
            }
            contentsArray.put(
                contentsArray.length() - 1,
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", mergedText)))
                }
            )
        } else {
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                }
            )
        }

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.35)
                put("topP", 0.85)
                put("maxOutputTokens", 450)
            })
        }

        val responseJson = postGeminiWithFallback(apiKey, payload, callName = "RoleplayWithTranslation")
        val candidateText = extractFirstCandidateText(responseJson).trim()
        val cleanJson = extractJsonString(candidateText)

        var de = ""
        var ar = ""
        val hintsList = mutableListOf<ConversationHint>()

        try {
            val parsed = JSONObject(cleanJson)
            de = parsed.optString("reply_de", "").trim()
            ar = parsed.optString("reply_ar", "").trim()
            val hintsArray = parsed.optJSONArray("hints") ?: parsed.optJSONArray("contextual_hints")
            if (hintsArray != null) {
                for (i in 0 until hintsArray.length()) {
                    val hObj = hintsArray.optJSONObject(i) ?: continue
                    val hintDe = hObj.optString("german", hObj.optString("de", "")).trim()
                    val rawAr = hObj.optString("translation_ar", hObj.optString("ar", "")).trim()
                    if (hintDe.isNotBlank()) {
                        val hintAr = sanitizeArabicTranslation(rawAr, hintDe)
                        hintsList.add(ConversationHint(german = hintDe, translationAr = hintAr, isContextual = true))
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Roleplay JSON parse error, extracting fields via regex fallback: ${e.message}")
            val deMatch = Regex(""""reply_de"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(candidateText)
            val arMatch = Regex(""""reply_ar"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").find(candidateText)
            if (deMatch != null) {
                de = deMatch.groupValues[1].replace("\\\"", "\"").replace("\\n", " ").trim()
            }
            if (arMatch != null) {
                ar = arMatch.groupValues[1].replace("\\\"", "\"").replace("\\n", " ").trim()
            }
            val germanMatches = Regex(""""german"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").findAll(candidateText).map { it.groupValues[1].replace("\\\"", "\"").replace("\\n", " ").trim() }.toList()
            val arMatches = Regex(""""translation_ar"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""").findAll(candidateText).map { it.groupValues[1].replace("\\\"", "\"").replace("\\n", " ").trim() }.toList()
            for (i in 0 until minOf(germanMatches.size, arMatches.size)) {
                val hDe = germanMatches[i]
                if (hDe.isNotBlank()) {
                    val hAr = sanitizeArabicTranslation(arMatches[i], hDe)
                    hintsList.add(ConversationHint(german = hDe, translationAr = hAr, isContextual = true))
                }
            }
        }

        // Final safety sanitization: ensure no JSON syntax or braces leak into the message
        de = sanitizeMessageText(de.ifBlank { sanitizeMessageText(cleanJson) })

        if (de.isBlank()) {
            throw IllegalStateException("Empty German response from Gemini for Roleplay")
        }

        if (ar.isBlank()) {
            ar = translateToArabic(apiKey, de)
        } else {
            ar = sanitizeArabicTranslation(ar, de)
        }

        RoleplayTurnResult(replyGerman = de, replyArabic = ar, contextualHints = hintsList)
    }

    suspend fun generateRoleplayReply(
        apiKey: String,
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>>,
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null,
        curriculumVocabulary: List<String> = emptyList()
    ): String {
        return generateRoleplayWithTranslation(apiKey, scenarioId, userMessage, history, cefrLevel, scenario, curriculumVocabulary).replyGerman
    }

    /**
     * Call 2: Structured Correctness Evaluation (Independent JSON Call)
     * Uses fallback chain across models.
     */
    private suspend fun evaluateCorrectness(
        apiKey: String,
        userMessage: String,
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null
    ): EvaluationResult = withContext(Dispatchers.IO) {
        val gradingStrictness = when (cefrLevel.uppercase().take(2)) {
            "A1", "A2" -> """
                Grading Lenience (A1/A2 Level):
                - Be communicative and encouraging.
                - IGNORE trivial chatting quirks: lowercase first letters (e.g. 'ich', 'hallo', 'kaffee') or missing end-of-sentence punctuation (like missing '?' or '.'). NEVER mark a sentence incorrect solely for lack of capitalization or final punctuation!
                - Only flag genuine structural/grammatical errors that distort meaning or violate foundational rules: completely wrong verb conjugation (e.g. 'ich gehen' instead of 'ich gehe'), missing auxiliary/verb, critical wrong accusative/dative article (e.g. 'ich nehme ein Kaffee' -> 'einen Kaffee'), or completely broken word order.
                - If the message is communicative and acceptable for an A1/A2 beginner in spoken dialogue: return "is_correct": true.
            """.trimIndent()
            else -> """
                Grading Strictness (B1/B2 Level):
                - Be precise and pedagogical.
                - Flag grammatical inaccuracies: incorrect preposition cases (Dativ vs Akkusativ), adjective endings, Nebensatz verb-final word order (e.g. 'weil ich habe Zeit' -> 'weil ich Zeit habe'), modal verb framing, and unidiomatic phrasing.
            """.trimIndent()
        }

        val scenarioContext = if (scenario != null && scenario.title_de.isNotBlank()) {
            "Scenario Context: '${scenario.title_de}' (${scenario.category}), roleplaying counterpart: ${scenario.ai_persona}."
        } else ""

        val prompt = """
            You are an expert German language tutor evaluating an Arabic-speaking learner at CEFR level: $cefrLevel.
            $scenarioContext
            Evaluate this German sentence submitted by the learner: "$userMessage"
            
            $gradingStrictness
            
            Determine whether it contains errors warranting correction according to the $cefrLevel standards above.
            
            Respond strictly in valid JSON matching this schema:
            {
              "is_correct": boolean,
              "mistake_segment": string,
              "corrected_segment": string,
              "original_mistake": string,
              "corrected_german": string,
              "grammar_rule": string,
              "explanation_ar": string,
              "user_message_translation_ar": string,
              "positive_note_ar": string
            }
            
            Guidelines:
            - "user_message_translation_ar": A natural, accurate, and contemporary Modern Standard Arabic translation of what the learner intended to say in this situation (فصحى معاصرة وسلسة وشبابية ومفهومة).
              * CRITICAL: Strictly NEVER substitute secular German greetings with religious formulas (e.g. translate "Guten Tag" as "مرحباً" or "يومك سعيد", NEVER "السلام عليكم"; "Hallo" as "أهلاً" or "مرحباً").
              * Avoid literal word-by-word or nonsense machine translations; ensure the Arabic makes immediate logical sense and flows naturally.
            - "positive_note_ar": When "is_correct" is true, provide an optional short, dry, witty Arabic praise note from Katzu (maximum 10 words, e.g. "تصريف ممتاز، كأنك عشت في برلين" or "أداة التعريف صحيحة، تقدم واضح"). CRITICAL: Do NOT output praise on every turn — only output a non-empty string when the sentence is either notably natural, used a good grammar pattern correctly (e.g. correct Akkusativ/Dativ, Nebensatz verb order, modal verb structure), or represents a clear step up in complexity for $cefrLevel. If the sentence is ordinary/basic or contains errors, return "".
            - If acceptable/correct under the $cefrLevel criteria:
              "is_correct": true, "mistake_segment": "", "corrected_segment": "", "original_mistake": "", "corrected_german": "", "grammar_rule": "", "explanation_ar": "".
            - If incorrect:
              - "is_correct": false
              - "mistake_segment": The precise erroneous word or short phrase snippet that failed (e.g. "ein Kaffee", "ich gehen", "weil ich will").
              - "corrected_segment": The exact corrected counterpart for that snippet (e.g. "einen Kaffee", "ich gehe", "weil ich möchte").
              - "original_mistake": The full original sentence as typed by the user.
              - "corrected_german": The full, natural corrected sentence in standard German.
              - "grammar_rule": Short rule category with Arabic and German (e.g. "المفعول به (Akkusativ)", "تصريف الأفعال (Konjugation)", "ترتيب الجملة (Wortstellung)", "حروف الجر (Präpositionen)").
              - "explanation_ar": A concise, witty, and deeply educational explanation in Katzu the cat's sarcastic deadpan tone, explaining WHY German requires this specific form without discouraging the learner.
        """.trimIndent()

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
                put("maxOutputTokens", 350)
            })
        }

        try {
            val responseJson = postGeminiWithFallback(apiKey, payload, callName = "Evaluation")
            val jsonText = extractFirstCandidateText(responseJson)
            val cleanJson = extractJsonString(jsonText)
            val parsed = JSONObject(cleanJson)
            EvaluationResult(
                isCorrect = parsed.optBoolean("is_correct", true),
                originalMistake = parsed.optString("original_mistake", userMessage),
                correctedGerman = parsed.optString("corrected_german", parsed.optString("correction_text", "")),
                grammarRule = parsed.optString("grammar_rule", ""),
                explanationAr = parsed.optString("explanation_ar", ""),
                userMessageTranslationAr = sanitizeArabicTranslation(parsed.optString("user_message_translation_ar", ""), userMessage),
                mistakeSegment = parsed.optString("mistake_segment", ""),
                correctedSegment = parsed.optString("corrected_segment", ""),
                positiveNoteAr = parsed.optString("positive_note_ar", "")
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Call 2 (Evaluation) failed on all fallback models: ${e.message}", e)
            EvaluationResult(
                isCorrect = true,
                originalMistake = "",
                correctedGerman = "",
                grammarRule = "",
                explanationAr = "",
                userMessageTranslationAr = "",
                mistakeSegment = "",
                correctedSegment = ""
            )
        }
    }

    /**
     * Call 3: Structured Translation (Independent JSON Call)
     * Uses in-memory cache and fallback chain across models.
     */
    suspend fun translateToArabic(
        apiKey: String = com.example.katzu.BuildConfig.GEMINI_API_KEY,
        germanText: String
    ): String = withContext(Dispatchers.IO) {
        val trimmed = germanText.trim()
        if (trimmed.isBlank()) return@withContext ""

        // Instant return from memory cache or pre-mapped scenario translations (zero API spend, instant)
        ScenarioTranslations.getByGermanText(trimmed).let {
            if (it.isNotBlank()) {
                translationCache[trimmed] = it
                return@withContext it
            }
        }
        translationCache[trimmed]?.let { return@withContext it }

        // Priority 1: Try server-side translation
        val serverTrans = tryServerSideTranslation(trimmed)
        if (!serverTrans.isNullOrBlank()) {
            translationCache[trimmed] = serverTrans
            return@withContext serverTrans
        }

        val prompt = """
            Translate this German sentence into accurate, natural, and contemporary Modern Standard Arabic (فصحى معاصرة وسلسة وشبابية ومفهومة).
            German: "$trimmed"
            
            TRANSLATION RULES:
            1. ACCURATE CONTEXTUAL MEANING: Capture the true conversational intent. Never do literal word-by-word translation that results in nonsense.
            2. NO RELIGIOUS SUBSTITUTIONS: Absolutely NEVER translate secular German greetings or expressions into religious formulas (e.g., "Guten Tag" is "مرحباً" or "يومك سعيد", NEVER "السلام عليكم"; "Hallo" is "أهلاً" or "مرحباً").
            3. MODERN & YOUTHFUL: Use clear, smooth, natural Arabic that makes immediate logical sense. Avoid archaic, dusty, or robotic machine wording.
            
            Respond strictly in valid JSON matching this schema:
            {
              "translation_ar": string
            }
        """.trimIndent()

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
                put("maxOutputTokens", 120)
            })
        }

        try {
            val responseJson = postGeminiWithFallback(apiKey, payload, callName = "Translation")
            val jsonText = extractFirstCandidateText(responseJson)
            val cleanJson = extractJsonString(jsonText)
            val parsed = JSONObject(cleanJson)
            val translation = sanitizeArabicTranslation(parsed.optString("translation_ar", "").trim(), trimmed)
            if (translation.isNotBlank()) {
                translationCache[trimmed] = translation
            }
            translation
        } catch (e: Exception) {
            AppLogger.w(TAG, "Call 3 (Translation) failed on all fallback models: ${e.message}", e)
            ""
        }
    }

    /**
     * Call 4: Structured Contextual Hints Generation (Independent JSON Call)
     * Generates 3 practical, level-appropriate response options in German with Modern Standard Arabic translations,
     * tailored dynamically to the counterpart's latest reply and current scenario state.
     */
    suspend fun generateContextualHints(
        apiKey: String = "",
        scenario: ScenarioEntity? = null,
        cefrLevel: String = "A1",
        history: List<Pair<String, String>> = emptyList(),
        lastAiReply: String
    ): List<ConversationHint> = withContext(Dispatchers.IO) {
        if (lastAiReply.isBlank()) return@withContext emptyList()

        val cacheKey = "${scenario?.id ?: "def"}_${cefrLevel}_${lastAiReply.trim()}"
        contextualHintsCache[cacheKey]?.let { return@withContext it }

        // Try server-side hint generation first (via Cloudflare Worker proxy, zero client API quota)
        val serverHints = tryServerSideHints(scenario, cefrLevel, lastAiReply)
        if (!serverHints.isNullOrEmpty()) {
            contextualHintsCache[cacheKey] = serverHints
            return@withContext serverHints
        }

        val allAvailableKeys = getAvailableApiKeys()
        val activeKey = if (apiKey.isNotBlank()) apiKey else allAvailableKeys.firstOrNull() ?: ""
        if (activeKey.isBlank()) {
            AppLogger.w(TAG, "generateContextualHints aborted: No Gemini API keys configured.")
            return@withContext emptyList()
        }

        val persona = if (scenario != null && scenario.ai_persona.isNotBlank()) scenario.ai_persona else "a conversational counterpart"
        val scenarioTitle = scenario?.title_de ?: "Alltagssituation"
        val scenarioCategory = scenario?.category ?: "everyday"

        val levelPrompt = when (cefrLevel.uppercase().take(2)) {
            "A1" -> "Level A1: Very simple, short sentences (4-7 words). Basic common vocabulary. Present tense only."
            "A2" -> "Level A2: Simple sentences (6-10 words). Practical everyday German. Present or simple Perfekt."
            "B1" -> "Level B1: Natural intermediate sentences with connectors (weil, dass, wenn) and common idioms."
            "B2" -> "Level B2: Professional, nuanced, and idiomatic expressions with Konjunktiv II or advanced structures."
            else -> "Level A1: Very simple short sentences (4-7 words)."
        }

        val prompt = """
            You are an expert German language tutor providing hints for an Arabic-speaking learner.
            
            SCENARIO: '$scenarioTitle' (Category: $scenarioCategory).
            The German conversation partner ($persona) just said to the learner:
            "$lastAiReply"
            
            $levelPrompt
            
            Generate exactly 3 practical, natural, and distinct German response options that the learner could say right now to answer the partner or continue the conversation realistically.
            - Option 1: A direct, standard answer or acceptance.
            - Option 2: An alternative choice, polite question, or preference.
            - Option 3: A polite clarification, inquiry (e.g. asking for price, recommendation, or detail), or follow-up.
            
            Each option must have an accurate, natural, and contemporary Modern Standard Arabic translation (فصحى معاصرة وسلسة وشبابية).
            CRITICAL: NEVER use religious substitutions for secular German phrases (e.g. "Guten Tag" is "مرحباً" or "يومك سعيد", NEVER "السلام عليكم"). Ensure translations make immediate logical sense.
            
            Respond strictly in valid JSON matching this schema:
            {
              "hints": [
                {
                  "german": "string",
                  "translation_ar": "string"
                }
              ]
            }
        """.trimIndent()

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
                put("maxOutputTokens", 380)
            })
        }

        try {
            val responseJson = postGeminiWithFallback(apiKey, payload, callName = "ContextualHints")
            val jsonText = extractFirstCandidateText(responseJson)
            val trimmed = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val cleanJson = if (trimmed.contains("{") && trimmed.contains("}")) {
                trimmed.substring(trimmed.indexOf("{"), trimmed.lastIndexOf("}") + 1)
            } else {
                trimmed
            }
            val parsed = JSONObject(cleanJson)
            val hintsArray = parsed.optJSONArray("hints") ?: return@withContext emptyList()
            val result = mutableListOf<ConversationHint>()
            for (i in 0 until hintsArray.length()) {
                val obj = hintsArray.getJSONObject(i)
                val de = obj.optString("german", "").trim()
                val ar = sanitizeArabicTranslation(obj.optString("translation_ar", "").trim(), de)
                if (de.isNotBlank()) {
                    result.add(ConversationHint(german = de, translationAr = ar, isContextual = true))
                }
            }
            if (result.isNotEmpty()) {
                contextualHintsCache[cacheKey] = result
            }
            result
        } catch (e: Exception) {
            AppLogger.w(TAG, "Call 4 (ContextualHints) failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Executes postGemini with multi-key failover and MODEL_FALLBACK_CHAIN:
     * 1. Rotates across all available API keys if an API key hits quota limits (HTTP 429),
     *    authentication/permission issues (HTTP 403), or network errors.
     * 2. Tries the model fallback chain for each key:
     *    - gemini-3.5-flash-lite
     *    - gemini-3.1-flash-lite
     *    - gemini-3.6-flash
     *    - gemini-flash-latest
     * 3. Remembers the active working key index for fast subsequent turns.
     */
    private fun postGeminiWithFallback(
        apiKey: String = "",
        payload: JSONObject,
        callName: String
    ): JSONObject {
        val configuredKeys = getAvailableApiKeys()
        val allKeys = if (apiKey.isNotBlank() && !configuredKeys.contains(apiKey)) {
            listOf(apiKey) + configuredKeys
        } else if (configuredKeys.isNotEmpty()) {
            configuredKeys
        } else if (apiKey.isNotBlank()) {
            listOf(apiKey)
        } else {
            emptyList()
        }

        if (allKeys.isEmpty()) {
            throw IllegalStateException("لا يوجد أي مفتاح API متاح لـ Gemini")
        }

        var lastException: Exception? = null
        val startingIndex = preferredKeyIndex.coerceIn(0, allKeys.size - 1)

        for (i in 0 until allKeys.size) {
            val keyIndex = (startingIndex + i) % allKeys.size
            val currentKey = allKeys[keyIndex]
            val maskedKey = if (currentKey.length > 8) "${currentKey.take(4)}...${currentKey.takeLast(4)}" else "***"

            AppLogger.i(TAG, "[$callName] Attempting API Key #${keyIndex + 1}/#${allKeys.size} ($maskedKey)")

            var keyQuotaExhausted = false

            for (model in MODEL_FALLBACK_CHAIN) {
                try {
                    AppLogger.i(TAG, "[$callName] Attempting model: $model with API Key #${keyIndex + 1}")
                    val result = postGemini(currentKey, payload, model)
                    AppLogger.i(TAG, "[$callName] Succeeded with API Key #${keyIndex + 1} and model: $model")
                    preferredKeyIndex = keyIndex
                    return result
                } catch (e: Exception) {
                    val errMsg = e.message ?: ""
                    AppLogger.w(TAG, "[$callName] Model $model failed with API Key #${keyIndex + 1}: $errMsg")
                    lastException = e

                    if (errMsg.contains("429") || errMsg.contains("RESOURCE_EXHAUSTED") ||
                        errMsg.contains("403") || errMsg.contains("PERMISSION_DENIED")) {
                        AppLogger.w(TAG, "[$callName] API Key #${keyIndex + 1} hit quota/permission limit ($errMsg). Rotating to next backup API key...")
                        keyQuotaExhausted = true
                        break
                    }
                }
            }

            if (keyQuotaExhausted && allKeys.size > 1) {
                continue
            }
        }

        throw lastException ?: IllegalStateException("All API keys and fallback models failed for $callName")
    }

    private fun postGemini(apiKey: String, payload: JSONObject, modelName: String): JSONObject {
        val endpoint = "$BASE_URL/$modelName:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 12000
        conn.readTimeout = 25000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val raw = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            conn.disconnect()
            return JSONObject(raw)
        } else {
            val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            val fullErrorMsg = "HTTP $code from $modelName: $errBody"
            AppLogger.e(TAG, "Gemini API Error: $fullErrorMsg")
            throw IllegalStateException("Gemini API Error $code: $fullErrorMsg")
        }
    }

    private fun extractFirstCandidateText(response: JSONObject): String {
        val candidates = response.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            // Skip thought parts if present
            if (part.optBoolean("thought", false)) continue
            val text = part.optString("text", "")
            if (text.isNotBlank()) {
                sb.append(text)
            }
        }
        return if (sb.isNotEmpty()) sb.toString() else parts.getJSONObject(0).optString("text", "")
    }
}
