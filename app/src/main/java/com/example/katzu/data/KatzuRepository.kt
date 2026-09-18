package com.example.katzu.data

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.katzu.model.*
import com.example.katzu.util.AppLogger
import com.example.katzu.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale

data class TtsPlaybackState(
    val utteranceId: String? = null,
    val text: String = "",
    val isPlaying: Boolean = false,
    val speed: Float = 1.0f,
    val charRange: Pair<Int, Int>? = null
)

class KatzuRepository(private val context: Context) {

    val contentRepository = KatzuContentRepository(context)
    val authApiClient = AuthApiClient()
    val progressApiClient = ProgressApiClient()
    private val database = KatzuDatabase.getInstance(context)
    private val dao = database.katzuDao()

    private val authPrefs = context.getSharedPreferences("katzu_auth_prefs", Context.MODE_PRIVATE)

    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var ttsHighlightJob: Job? = null

    private var activeIdToken: String? = null

    fun setActiveIdToken(token: String?) {
        activeIdToken = token
        if (!token.isNullOrBlank()) {
            authPrefs.edit().putString("saved_id_token", token).apply()
        }
    }

    fun getActiveIdToken(): String? {
        if (activeIdToken.isNullOrBlank()) {
            activeIdToken = authPrefs.getString("saved_id_token", null)
        }
        return activeIdToken
    }

    fun isUserLoggedInSync(): Boolean {
        val prefsLoggedIn = authPrefs.getBoolean("is_logged_in", false)
        val prefsEmail = authPrefs.getString("google_email", null)
        if (prefsLoggedIn && !prefsEmail.isNullOrBlank()) {
            return true
        }
        return try {
            runBlocking(Dispatchers.IO) {
                val user = dao.getUser()
                val loggedIn = user?.isLoggedIn == true && (!user.googleAccountEmail.isNullOrBlank() || !user.email.isNullOrBlank())
                if (user != null && loggedIn) {
                    syncUserToPrefs(user)
                }
                loggedIn
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isSubscriptionActiveSync(): Boolean {
        val loggedIn = isUserLoggedInSync()
        if (!loggedIn) return false
        val prefsActive = authPrefs.getBoolean("is_subscription_active", false)
        val expiresAt = authPrefs.getString("subscription_expires_at", null)
        if (prefsActive && SecurityAndSubscriptionManager.isLocallyActive(expiresAt)) {
            return true
        }
        return try {
            runBlocking(Dispatchers.IO) {
                val user = dao.getUser()
                val active = user?.isSubscriptionActive == true && SecurityAndSubscriptionManager.isLocallyActive(user.subscriptionExpiresAt)
                if (user != null) {
                    syncUserToPrefs(user)
                }
                active
            }
        } catch (e: Exception) {
            false
        }
    }

    fun syncUserToPrefs(user: UserEntity) {
        val hasAccount = !user.googleAccountEmail.isNullOrBlank() || user.email.isNotBlank()
        authPrefs.edit()
            .putBoolean("is_logged_in", user.isLoggedIn && hasAccount)
            .putBoolean("is_subscription_active", user.isSubscriptionActive)
            .putString("subscription_expires_at", user.subscriptionExpiresAt)
            .putString("google_email", user.googleAccountEmail ?: user.email)
            .putString("display_name", user.displayName)
            .apply()
    }

    fun clearAuthPrefs() {
        authPrefs.edit().clear().apply()
        activeIdToken = null
    }

    val userEntityFlow: Flow<UserEntity?> = dao.getUserFlow()

    private val settingsPrefs = context.getSharedPreferences("katzu_settings_prefs", Context.MODE_PRIVATE)

    fun getDailyRemindersEnabled(): Boolean = settingsPrefs.getBoolean("reminder_enabled", true)
    fun getReminderFrequencyPreset(): String = settingsPrefs.getString("reminder_frequency", "regular") ?: "regular"
    fun getReminderHour(): Int = settingsPrefs.getInt("reminder_hour", 20)
    fun getReminderMinute(): Int = settingsPrefs.getInt("reminder_minute", 0)

    fun getTargetGoalId(): String = settingsPrefs.getString("goal_id", "goal_a2") ?: "goal_a2"
    fun getTargetGoalTitle(): String = settingsPrefs.getString("goal_title", "الوصول إلى مستوى A2 والتحدث بثقة") ?: "الوصول إلى مستوى A2 والتحدث بثقة"
    fun getTargetWeeklyDays(): Int = settingsPrefs.getInt("goal_target_days", 5)
    fun getTargetDailyMinutes(): Int = settingsPrefs.getInt("goal_daily_minutes", 10)

    fun getFreeSessionsRemaining(): Int {
        if (!settingsPrefs.contains("free_sessions_remaining")) {
            settingsPrefs.edit().putInt("free_sessions_remaining", 3).apply()
        }
        return settingsPrefs.getInt("free_sessions_remaining", 3)
    }

    fun decrementFreeSessionsRemaining(): Int {
        val current = getFreeSessionsRemaining()
        val next = (current - 1).coerceAtLeast(0)
        settingsPrefs.edit().putInt("free_sessions_remaining", next).apply()
        AppLogger.i("KatzuRepository", "Decremented free sessions remaining: $current -> $next")
        return next
    }

    fun markSessionCompletedToday() {
        val now = System.currentTimeMillis()
        settingsPrefs.edit().putLong("last_session_completed_at", now).apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        if (isUserLoggedInSync()) {
            return true
        }
        return settingsPrefs.getBoolean("has_completed_onboarding", false)
    }

    fun setCompletedOnboarding(completed: Boolean = true) {
        settingsPrefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    private val _userProfile = MutableStateFlow(
        UserProfile(
            name = settingsPrefs.getString("user_preferred_name", "") ?: "",
            currentLevel = settingsPrefs.getString("selected_level", "A1") ?: "A1",
            dailyRemindersEnabled = settingsPrefs.getBoolean("reminder_enabled", true),
            reminderFrequencyPreset = settingsPrefs.getString("reminder_frequency", "regular") ?: "regular",
            reminderHour = settingsPrefs.getInt("reminder_hour", 20),
            reminderMinute = settingsPrefs.getInt("reminder_minute", 0),
            targetGoalId = settingsPrefs.getString("goal_id", "goal_a2") ?: "goal_a2",
            targetGoalTitle = settingsPrefs.getString("goal_title", "الوصول إلى مستوى A2 والتحدث بثقة") ?: "الوصول إلى مستوى A2 والتحدث بثقة",
            targetWeeklyDays = settingsPrefs.getInt("goal_target_days", 5),
            targetDailyMinutes = settingsPrefs.getInt("goal_daily_minutes", 10)
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val _ttsPlaybackState = MutableStateFlow(TtsPlaybackState())
    val ttsPlaybackState: StateFlow<TtsPlaybackState> = _ttsPlaybackState.asStateFlow()
    private var activeSpokenText: String = ""

    init {
        NotificationHelper.createNotificationChannel(context)
        if (getDailyRemindersEnabled()) {
            NotificationHelper.scheduleReminder(
                context,
                getReminderFrequencyPreset(),
                getReminderHour(),
                getReminderMinute()
            )
        }

        val ttsInitListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.GERMAN)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                if (isTtsReady) {
                    configureOptimalGermanVoice()
                    setupUtteranceListener()
                }
            }
        }
        tts = try {
            TextToSpeech(context, ttsInitListener, "com.google.android.tts")
        } catch (e: Exception) {
            TextToSpeech(context, ttsInitListener)
        }
        CoroutineScope(Dispatchers.IO).launch {
            userEntityFlow.collect { user ->
                if (user != null) {
                    syncUserToPrefs(user)
                    val preferredName = settingsPrefs.getString("user_preferred_name", null)?.trim()?.ifBlank { null }
                    val resolvedName = preferredName
                        ?: user.displayName?.ifBlank { null }
                        ?: user.googleAccountEmail?.substringBefore("@")?.ifBlank { null }
                        ?: ""
                    val resolvedEmail = user.googleAccountEmail ?: user.email
                    _userProfile.value = _userProfile.value.copy(
                        name = resolvedName,
                        email = resolvedEmail,
                        isProActive = user.isSubscriptionActive
                    )
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            getAllSessionsFlow().collect { sessions ->
                if (sessions.isNotEmpty()) {
                    val days = sessions.map {
                        java.time.Instant.ofEpochMilli(it.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }.distinct().sortedDescending()
                    var streak = 0
                    var current = java.time.LocalDate.now()
                    for (day in days) {
                        if (day == current || day == current.minusDays(1)) {
                            streak++
                            current = day
                        } else {
                            break
                        }
                    }
                    val calculatedStreak = streak.coerceAtLeast(1)
                    val calculatedXp = sessions.sumOf { (it.sentencesSpoken * 15) + (it.wordsLearned * 10) + (it.accuracyPercent / 2) }
                    val completedScenarios = sessions.map { it.scenarioId }.distinct().size
                    val totalSeconds = sessions.sumOf { it.durationSeconds }
                    val avgFluency = sessions.map { it.accuracyPercent }.average().toInt()

                    _userProfile.value = _userProfile.value.copy(
                        streakDays = calculatedStreak,
                        xp = calculatedXp,
                        completedScenarios = completedScenarios,
                        practiceHours = totalSeconds / 3600.0,
                        fluencyRatePercent = avgFluency
                    )
                }
            }
        }
    }

    suspend fun getCurrentUser(): UserEntity? = dao.getUser()

    suspend fun saveGoogleUser(email: String, displayName: String? = null) {
        AppLogger.i("KatzuRepository", "saveGoogleUser called for email: $email, displayName: $displayName")
        val current = dao.getUser() ?: UserEntity()
        val userEnteredName = settingsPrefs.getString("user_preferred_name", null)?.trim()?.ifBlank { null }
            ?: _userProfile.value.name.trim().ifBlank { null }
        val finalDisplayName = userEnteredName ?: displayName?.ifBlank { null } ?: current.displayName
        // Only retain active subscription if it was ALREADY active and confirmed for this exact Google email
        val isSameActiveAccount = !current.googleAccountEmail.isNullOrBlank() &&
                current.googleAccountEmail.equals(email, ignoreCase = true) &&
                current.isSubscriptionActive &&
                SecurityAndSubscriptionManager.isLocallyActive(current.subscriptionExpiresAt)

        val updated = current.copy(
            googleAccountEmail = email,
            email = email,
            displayName = finalDisplayName,
            isLoggedIn = true,
            isSubscriptionActive = isSameActiveAccount,
            subscriptionExpiresAt = if (isSameActiveAccount) current.subscriptionExpiresAt else null,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertUser(updated)
        syncUserToPrefs(updated)
        setCompletedOnboarding(true)
        AppLogger.i("KatzuRepository", "UserEntity saved to Room: $updated")
        _userProfile.value = _userProfile.value.copy(
            email = email,
            name = userEnteredName ?: finalDisplayName ?: _userProfile.value.name,
            isProActive = updated.isSubscriptionActive
        )
    }

    suspend fun markSubscriptionInactive() {
        val current = dao.getUser() ?: UserEntity()
        val updated = current.copy(
            isSubscriptionActive = false,
            subscriptionExpiresAt = null,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertUser(updated)
        syncUserToPrefs(updated)
        _userProfile.value = _userProfile.value.copy(isProActive = false)
        AppLogger.i("KatzuRepository", "Subscription marked inactive locally")
    }

    suspend fun verifyAndRedeemSubscription(code: String, idToken: String): ServerVerifyResult {
        setActiveIdToken(idToken)
        AppLogger.i("KatzuRepository", "verifyAndRedeemSubscription called with code: $code")
        val result = authApiClient.verifySubscriptionCode(code = code, idToken = idToken)
        AppLogger.i("KatzuRepository", "authApiClient.verifySubscriptionCode returned: $result")
        if (result is ServerVerifyResult.Success) {
            val current = dao.getUser() ?: UserEntity()
            val updated = current.copy(
                googleAccountEmail = result.email ?: current.googleAccountEmail,
                email = result.email ?: current.email,
                isSubscriptionActive = true,
                subscriptionExpiresAt = result.expiresAt,
                isLoggedIn = true,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertUser(updated)
            syncUserToPrefs(updated)
            dao.insertRedeemedCode(
                RedeemedCodeEntity(
                    code = code.trim().uppercase(),
                    monthsGranted = result.monthsAdded,
                    redeemedAt = System.currentTimeMillis()
                )
            )
            _userProfile.value = _userProfile.value.copy(isProActive = true)
            AppLogger.i("KatzuRepository", "Saved active subscription to Room: expiresAt=${result.expiresAt}")
            fetchAndRestoreCloudProgress(idToken)
        }
        return result
    }

    suspend fun activatePlayBillingSubscription(
        productId: String,
        purchaseToken: String,
        orderId: String? = null
    ): Boolean {
        AppLogger.i("KatzuRepository", "activatePlayBillingSubscription: productId=$productId, orderId=$orderId, tokenPrefix=${purchaseToken.take(12)}...")
        val current = dao.getUser() ?: UserEntity()
        val months = if (productId.contains("annual") || productId.contains("yearly")) 12 else 1
        val newExpiresAt = SecurityAndSubscriptionManager.calculateNewExpiration(current.subscriptionExpiresAt, months)
        val userEmail = if (current.email.isNotBlank()) current.email else (current.googleAccountEmail ?: "play_subscriber@katzu.app")
        val updated = current.copy(
            email = userEmail,
            googleAccountEmail = current.googleAccountEmail ?: userEmail,
            isSubscriptionActive = true,
            subscriptionExpiresAt = newExpiresAt,
            isLoggedIn = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertUser(updated)
        syncUserToPrefs(updated)
        dao.insertRedeemedCode(
            RedeemedCodeEntity(
                code = "PLAY-${productId.uppercase()}",
                monthsGranted = months,
                redeemedAt = System.currentTimeMillis()
            )
        )
        _userProfile.value = _userProfile.value.copy(isProActive = true)
        AppLogger.i("KatzuRepository", "Successfully activated Google Play subscription: expiresAt=$newExpiresAt, isSubscriptionActive=true")
        return true
    }

    suspend fun checkSubscriptionStatus(idToken: String): ServerStatusResult {
        setActiveIdToken(idToken)
        AppLogger.i("KatzuRepository", "checkSubscriptionStatus starting")
        val result = authApiClient.checkSubscriptionStatus(idToken)
        AppLogger.i("KatzuRepository", "checkSubscriptionStatus returned: $result")
        when (result) {
            is ServerStatusResult.Active -> {
                val current = dao.getUser() ?: UserEntity()
                val updated = current.copy(
                    isSubscriptionActive = true,
                    subscriptionExpiresAt = result.expiresAt ?: current.subscriptionExpiresAt,
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertUser(updated)
                syncUserToPrefs(updated)
                _userProfile.value = _userProfile.value.copy(isProActive = true)
                AppLogger.i("KatzuRepository", "Updated local user with active subscription: expiresAt=${updated.subscriptionExpiresAt}")
            }
            is ServerStatusResult.Expired -> {
                val current = dao.getUser() ?: UserEntity()
                val updated = current.copy(
                    isSubscriptionActive = false,
                    subscriptionExpiresAt = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertUser(updated)
                syncUserToPrefs(updated)
                _userProfile.value = _userProfile.value.copy(isProActive = false)
                AppLogger.w("KatzuRepository", "Updated local user with EXPIRED subscription")
            }
            is ServerStatusResult.Error -> {
                // Keep local state intact
                AppLogger.w("KatzuRepository", "Subscription check error, keeping local state intact")
            }
        }
        return result
    }

    suspend fun fetchAndRestoreCloudProgress(idToken: String) = withContext(Dispatchers.IO) {
        setActiveIdToken(idToken)
        try {
            AppLogger.i("KatzuRepository", "Fetching cloud progress for user...")
            val cloudData = progressApiClient.fetchProgress(idToken) ?: return@withContext
            AppLogger.i("KatzuRepository", "Cloud progress received: ${cloudData.trainings.size} trainings, ${cloudData.savedWordIds.size} saved words")

            // 1. Restore Stats & Level into UserProfile & SharedPreferences
            val stats = cloudData.stats
            if (stats.level.isNotBlank()) {
                settingsPrefs.edit().putString("selected_level", stats.level).apply()
            }
            _userProfile.value = _userProfile.value.copy(
                currentLevel = if (stats.level.isNotBlank()) stats.level else _userProfile.value.currentLevel,
                streakDays = if (stats.streakDays > _userProfile.value.streakDays) stats.streakDays else _userProfile.value.streakDays,
                xp = if (stats.totalPoints > _userProfile.value.xp) stats.totalPoints else _userProfile.value.xp,
                practiceHours = if (stats.practiceHours > _userProfile.value.practiceHours) stats.practiceHours else _userProfile.value.practiceHours,
                fluencyRatePercent = if (stats.fluencyRatePercent > 0) stats.fluencyRatePercent.toInt() else _userProfile.value.fluencyRatePercent
            )

            // 2. Restore Scenario Trainings into Room
            if (cloudData.trainings.isNotEmpty()) {
                val entitiesToInsert = cloudData.trainings.map { cloudItem ->
                    val existing = dao.getScenarioTraining(cloudItem.scenarioId)
                    ScenarioTrainingEntity(
                        scenarioId = cloudItem.scenarioId,
                        userId = "current_user",
                        studiedAt = cloudItem.studiedAt ?: existing?.studiedAt,
                        quizAttempted = cloudItem.quizAttempted || (existing?.quizAttempted == true),
                        lastScore = cloudItem.lastScore ?: existing?.lastScore,
                        effectiveLevel = cloudItem.effectiveLevel ?: existing?.effectiveLevel,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                dao.insertScenarioTrainings(entitiesToInsert)
                AppLogger.i("KatzuRepository", "Restored ${entitiesToInsert.size} scenario trainings into Room!")
            }

            // 3. Restore Saved Words (Bookmarks)
            if (cloudData.savedWordIds.isNotEmpty()) {
                val wordsToInsert = cloudData.savedWordIds.map { SavedWordEntity(wordId = it) }
                dao.insertSavedWords(wordsToInsert)
                AppLogger.i("KatzuRepository", "Restored ${wordsToInsert.size} saved words into Room!")
            }
        } catch (e: Exception) {
            AppLogger.e("KatzuRepository", "fetchAndRestoreCloudProgress error: ${e.message}", e)
        }
    }

    fun syncProgressToServer() {
        val token = getActiveIdToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = _userProfile.value
                val trainings = dao.getAllScenarioTrainings()
                val savedWordIds = dao.getAllSavedWordIds()

                val cloudStats = ProgressApiClient.CloudStats(
                    level = profile.currentLevel,
                    streakDays = profile.streakDays,
                    totalPoints = profile.xp,
                    lastActiveDate = null,
                    practiceHours = profile.practiceHours,
                    fluencyRatePercent = profile.fluencyRatePercent.toDouble(),
                    updatedAt = System.currentTimeMillis()
                )

                val cloudTrainings = trainings.map {
                    ProgressApiClient.CloudTrainingItem(
                        scenarioId = it.scenarioId,
                        studiedAt = it.studiedAt,
                        quizAttempted = it.quizAttempted,
                        lastScore = it.lastScore,
                        effectiveLevel = it.effectiveLevel,
                        updatedAt = it.updatedAt
                    )
                }

                val success = progressApiClient.syncProgress(
                    idToken = token,
                    stats = cloudStats,
                    trainings = cloudTrainings,
                    savedWordIds = savedWordIds
                )
                AppLogger.i("KatzuRepository", "syncProgressToServer completed: success=$success")
            } catch (e: Exception) {
                AppLogger.e("KatzuRepository", "syncProgressToServer failed: ${e.message}", e)
            }
        }
    }

    suspend fun signOut() {
        val current = dao.getUser() ?: UserEntity()
        dao.insertUser(
            current.copy(
                googleAccountEmail = null,
                isLoggedIn = false,
                isSubscriptionActive = false,
                subscriptionExpiresAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )
        clearAuthPrefs()
        _userProfile.value = _userProfile.value.copy(isProActive = false)
    }

    private fun configureOptimalGermanVoice() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            val availableVoices = tts?.voices ?: emptySet()
            val germanVoices = availableVoices.filter { voice ->
                voice.locale.language == "de" &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }

            val preferredVoice = germanVoices.sortedWith(
                compareByDescending<Voice> { voice ->
                    val name = voice.name.lowercase()
                    when {
                        name.contains("journey") || name.contains("studio") -> 10
                        name.contains("neural") || name.contains("wavenet") -> 9
                        name.contains("network") && (name.contains("deg") || name.contains("deb") || name.contains("dfa") || name.contains("dea")) -> 8
                        name.contains("deg") || name.contains("deb") || name.contains("dfa") || name.contains("dea") -> 7
                        name.contains("cfs") || name.contains("gft") -> 6
                        name.contains("network") -> 5
                        name.contains("de-de-x") -> 4
                        else -> 1
                    }
                }
                .thenByDescending { it.quality }
                .thenByDescending { it.locale.country.equals("DE", ignoreCase = true) }
            ).firstOrNull()

            if (preferredVoice != null) {
                tts?.voice = preferredVoice
                AppLogger.i("KatzuRepository", "Configured optimal German TTS voice: ${preferredVoice.name} (quality=${preferredVoice.quality})")
            } else {
                AppLogger.i("KatzuRepository", "Using default system German TTS voice")
            }
            tts?.setPitch(1.0f)
        } catch (e: Exception) {
            AppLogger.w("KatzuRepository", "Could not configure custom TTS voice: ${e.message}")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _ttsPlaybackState.value = _ttsPlaybackState.value.copy(
                    utteranceId = utteranceId,
                    text = activeSpokenText,
                    isPlaying = true,
                    charRange = null
                )
            }

            override fun onDone(utteranceId: String?) {
                ttsHighlightJob?.cancel()
                ttsHighlightJob = null
                _ttsPlaybackState.value = TtsPlaybackState()
            }

            override fun onError(utteranceId: String?) {
                ttsHighlightJob?.cancel()
                ttsHighlightJob = null
                _ttsPlaybackState.value = TtsPlaybackState()
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                if (_ttsPlaybackState.value.isPlaying && _ttsPlaybackState.value.utteranceId == utteranceId) {
                    val textLen = activeSpokenText.length
                    if (textLen == 0) return
                    val safeStart = start.coerceIn(0, textLen)
                    val rawEnd = if (end <= safeStart && (safeStart + end) <= textLen) {
                        safeStart + end
                    } else {
                        end
                    }
                    val safeEnd = rawEnd.coerceIn(safeStart, textLen)
                    if (safeEnd > safeStart) {
                        _ttsPlaybackState.value = _ttsPlaybackState.value.copy(
                            charRange = Pair(safeStart, safeEnd)
                        )
                    }
                }
            }
        })
    }

    private fun startHighlightTracking(text: String, speed: Float, utteranceId: String) {
        ttsHighlightJob?.cancel()
        if (text.isBlank()) return

        val regex = Regex("\\S+")
        val wordMatches = regex.findAll(text).toList()
        if (wordMatches.isEmpty()) return

        // Calibrated speech rate scaling (slow mode 0.75f stretches word duration proportionally)
        val speedMultiplier = (1.0f / speed.coerceIn(0.4f, 2.0f))

        ttsHighlightJob = repositoryScope.launch {
            // Buffer delay before speech begins producing sound
            delay((90 * speedMultiplier).toLong())
            for (i in wordMatches.indices) {
                if (!_ttsPlaybackState.value.isPlaying || _ttsPlaybackState.value.utteranceId != utteranceId) break

                val match = wordMatches[i]
                val word = match.value
                val wordStart = match.range.first
                val wordEnd = match.range.last + 1

                val currentRange = _ttsPlaybackState.value.charRange
                if (currentRange == null || currentRange.first <= wordStart) {
                    _ttsPlaybackState.value = _ttsPlaybackState.value.copy(
                        charRange = Pair(wordStart, wordEnd)
                    )
                }

                // Word length and punctuation timing model calibrated for German syllables
                val cleanWord = word.trim(',', '.', '!', '?', ':', ';', '-', '"')
                var wordDurationMs = 280L + (cleanWord.length.coerceAtLeast(1) * 28L)
                if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?") || word.endsWith(":") || word.endsWith(";")) {
                    wordDurationMs += 160L
                } else if (word.endsWith(",")) {
                    wordDurationMs += 90L
                }
                val adjustedMs = (wordDurationMs * speedMultiplier).toLong().coerceIn(160L, 2200L)
                delay(adjustedMs)
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f) {
        if (!isTtsReady || text.isBlank()) return

        // If the exact same text is currently playing at the same speed, treat click as stop toggle
        if (_ttsPlaybackState.value.isPlaying && _ttsPlaybackState.value.text == text && _ttsPlaybackState.value.speed == speed) {
            stopSpeaking()
            return
        }

        stopSpeaking()
        val utteranceId = "katzu_tts_${System.currentTimeMillis()}"
        activeSpokenText = text
        _ttsPlaybackState.value = TtsPlaybackState(
            utteranceId = utteranceId,
            text = text,
            isPlaying = true,
            speed = speed,
            charRange = null
        )

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.setSpeechRate(speed)
        tts?.setPitch(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        startHighlightTracking(text, speed, utteranceId)
    }

    fun stopSpeaking() {
        ttsHighlightJob?.cancel()
        ttsHighlightJob = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            AppLogger.w("KatzuRepository", "Error stopping TTS: ${e.message}")
        }
        _ttsPlaybackState.value = TtsPlaybackState()
    }

    fun updateUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            settingsPrefs.edit()
                .putString("user_preferred_name", trimmed)
                .apply()
            _userProfile.value = _userProfile.value.copy(name = trimmed)
            CoroutineScope(Dispatchers.IO).launch {
                val current = dao.getUser() ?: UserEntity()
                val updated = current.copy(displayName = trimmed, updatedAt = System.currentTimeMillis())
                dao.insertUser(updated)
                syncUserToPrefs(updated)
                syncProgressToServer()
            }
        }
    }

    suspend fun toggleWordSaved(wordId: String): Boolean {
        val result = contentRepository.toggleSaveWord(wordId)
        syncProgressToServer()
        return result
    }

    fun updateUserLevel(level: String) {
        settingsPrefs.edit().putString("selected_level", level).apply()
        _userProfile.value = _userProfile.value.copy(currentLevel = level)
        syncProgressToServer()
    }

    suspend fun sendConversationTurn(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        cefrLevel: String = "A1",
        scenario: ScenarioEntity? = null,
        curriculumVocabulary: List<String> = emptyList(),
        isFinalTurn: Boolean = false
    ): GeminiOnlineConversationService.ConversationTurnResult {
        return GeminiOnlineConversationService.processTurn(
            scenarioId = scenarioId,
            userMessage = userMessage,
            history = history,
            cefrLevel = cefrLevel,
            scenario = scenario,
            curriculumVocabulary = curriculumVocabulary,
            isFinalTurn = isFinalTurn
        )
    }

    fun getScenarioTrainingFlow(scenarioId: String): Flow<ScenarioTrainingEntity?> {
        return dao.getScenarioTrainingFlow(scenarioId)
    }

    suspend fun getScenarioTraining(scenarioId: String): ScenarioTrainingEntity? {
        return dao.getScenarioTraining(scenarioId)
    }

    suspend fun markScenarioStudied(scenarioId: String) {
        val existing = dao.getScenarioTraining(scenarioId)
        val updated = existing?.copy(
            studiedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ) ?: ScenarioTrainingEntity(
            scenarioId = scenarioId,
            studiedAt = System.currentTimeMillis()
        )
        dao.insertScenarioTraining(updated)
        AppLogger.i("KatzuRepository", "Marked scenario '$scenarioId' as studied at ${updated.studiedAt}")
        syncProgressToServer()
    }

    suspend fun markScenarioQuizAttempted(scenarioId: String, score: Int = 0) {
        val existing = dao.getScenarioTraining(scenarioId)
        val updated = existing?.copy(
            quizAttempted = true,
            lastScore = score,
            updatedAt = System.currentTimeMillis()
        ) ?: ScenarioTrainingEntity(
            scenarioId = scenarioId,
            quizAttempted = true,
            lastScore = score
        )
        dao.insertScenarioTraining(updated)
        AppLogger.i("KatzuRepository", "Marked scenario '$scenarioId' quiz attempted with score $score")
        syncProgressToServer()
    }

    suspend fun updateScenarioEffectiveLevel(scenarioId: String, effectiveLevel: String?) {
        val existing = dao.getScenarioTraining(scenarioId)
        val updated = existing?.copy(
            effectiveLevel = effectiveLevel,
            updatedAt = System.currentTimeMillis()
        ) ?: ScenarioTrainingEntity(
            scenarioId = scenarioId,
            effectiveLevel = effectiveLevel,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertScenarioTraining(updated)
        AppLogger.i("KatzuRepository", "Updated scenario '$scenarioId' effectiveLevel to: $effectiveLevel")
        syncProgressToServer()
    }

    suspend fun getInitialIntroMessageForScenario(scenarioId: String, cefrLevel: String): String {
        val scenario = dao.getScenarioById(scenarioId)
        return scenario?.getInitialMessageForLevel(cefrLevel).orEmpty()
    }

    suspend fun recordSession(
        scenarioId: String,
        scenarioTitle: String,
        cefrLevel: String,
        sentencesSpoken: Int,
        wordsLearned: Int,
        accuracyPercent: Int,
        durationSeconds: Int
    ): SessionEntity {
        val session = SessionEntity(
            scenarioId = scenarioId,
            scenarioTitle = scenarioTitle,
            cefrLevel = cefrLevel,
            sentencesSpoken = sentencesSpoken,
            wordsLearned = wordsLearned,
            accuracyPercent = accuracyPercent,
            durationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis()
        )
        dao.insertSession(session)
        markSessionCompletedToday()
        AppLogger.i("KatzuRepository", "Recorded practice session to Room: $session")
        syncProgressToServer()
        return session
    }

    suspend fun recordMistakes(
        scenarioId: String,
        mistakes: List<com.example.katzu.model.SessionMistakeSummary>,
        userId: String = "current_user"
    ) {
        if (mistakes.isEmpty()) return
        val currentUid = dao.getUser()?.id ?: userId
        val entities = mistakes.map { m ->
            MistakeEntity(
                userId = currentUid,
                scenarioId = scenarioId,
                original = m.originalMistake,
                corrected = m.correctedGerman,
                grammarRule = m.grammarRule,
                timestamp = System.currentTimeMillis(),
                wasHintUsed = m.wasHintUsed
            )
        }
        dao.insertMistakes(entities)
        AppLogger.i("KatzuRepository", "Persisted ${entities.size} mistakes to Room table 'mistakes'")
    }

    fun getAllMistakesFlow(): Flow<List<MistakeEntity>> = dao.getAllMistakesFlow()

    suspend fun getAllMistakes(): List<MistakeEntity> = dao.getAllMistakes()

    fun updateReminderSettings(enabled: Boolean, frequencyPreset: String, hour: Int, minute: Int) {
        settingsPrefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .putString("reminder_frequency", frequencyPreset)
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            dailyRemindersEnabled = enabled,
            reminderFrequencyPreset = frequencyPreset,
            reminderHour = hour,
            reminderMinute = minute
        )

        if (enabled) {
            NotificationHelper.scheduleReminder(context, frequencyPreset, hour, minute)
        } else {
            NotificationHelper.cancelReminder(context)
        }
        AppLogger.i("KatzuRepository", "Updated reminder settings: enabled=$enabled, preset=$frequencyPreset, time=$hour:$minute")
    }

    fun updateLearningGoal(goalId: String, title: String, targetDays: Int, targetMinutes: Int) {
        settingsPrefs.edit()
            .putString("goal_id", goalId)
            .putString("goal_title", title)
            .putInt("goal_target_days", targetDays)
            .putInt("goal_daily_minutes", targetMinutes)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            targetGoalId = goalId,
            targetGoalTitle = title,
            targetWeeklyDays = targetDays,
            targetDailyMinutes = targetMinutes
        )
        AppLogger.i("KatzuRepository", "Updated learning goal: id=$goalId, title=$title, targetDays=$targetDays, targetMinutes=$targetMinutes")
    }

    fun sendTestNotification() {
        NotificationHelper.showReminderNotification(context)
    }

    fun getAllSessionsFlow(): Flow<List<SessionEntity>> = dao.getAllSessionsFlow()
    fun getAllScenarioTrainingFlow(): Flow<List<ScenarioTrainingEntity>> = dao.getAllScenarioTrainingFlow()
    fun getCompletedSessionsCountFlow(): Flow<Int> = dao.getCompletedSessionsCountFlow()
    fun getTotalSentencesSpokenFlow(): Flow<Int> = dao.getTotalSentencesSpokenFlow()
    fun getTotalWordsLearnedFlow(): Flow<Int> = dao.getTotalWordsLearnedFlow()
    fun getTotalDurationSecondsFlow(): Flow<Int> = dao.getTotalDurationSecondsFlow()
    fun getSavedWordsCountFlow(): Flow<Int> = dao.getSavedWordsCountFlow()
    fun getVocabularyCountFlow(): Flow<Int> = dao.getVocabularyCountFlow()
    fun getVocabularyCountByLevelFlow(level: String): Flow<Int> = dao.getVocabularyCountByLevelFlow(level)

    fun getMistakesForUserFlow(userId: String = "current_user"): Flow<List<MistakeEntity>> = dao.getMistakesForUserFlow(userId)
    suspend fun insertMistakes(mistakes: List<MistakeEntity>) = dao.insertMistakes(mistakes)
    suspend fun deleteMistake(id: Long) = dao.deleteMistake(id)

    fun getInitialConversation(): List<ChatMessage> {
        return emptyList()
    }

    suspend fun lookupWord(rawWord: String): VocabularyWord {
        val clean = rawWord.trim().trim(',', '.', '!', '?', '"', '«', '»', ':', ';', '(', ')', '[', ']', '{', '}')
        if (clean.isBlank()) {
            return VocabularyWord(
                id = "empty",
                germanWord = "",
                article = "",
                phonetic = "",
                level = "",
                partOfSpeech = "",
                gender = GermanGender.None,
                plural = "",
                arabicMeaning = "",
                englishMeaning = "",
                exampleGerman = "",
                exampleArabic = "",
                mnemonicTip = "",
                category = "",
                isMastered = false,
                isSaved = false
            )
        }
        val entity = dao.findVocabularyByExactGerman(clean) ?: dao.findVocabularyByFuzzyGerman(clean)
        if (entity != null) {
            val isSaved = dao.isWordSaved(entity.id.toString())
            return entity.toUiModel().copy(isSaved = isSaved)
        }
        val fallbackId = "word_${clean.lowercase()}"
        val isSaved = dao.isWordSaved(fallbackId)
        return VocabularyWord(
            id = fallbackId,
            germanWord = clean,
            article = "",
            phonetic = "[$clean]",
            level = "محادثة",
            partOfSpeech = "مفردة في المحادثة",
            gender = GermanGender.None,
            plural = "",
            arabicMeaning = "انقر للاستماع للنطق أو حفظ الكلمة في مراجعاتك",
            englishMeaning = clean,
            exampleGerman = clean,
            exampleArabic = "",
            mnemonicTip = "وردت أثناء المحادثة الحية",
            category = "مفردات المحادثة",
            isMastered = false,
            isSaved = isSaved
        )
    }
}
