package com.example.katzu.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContentSyncStatus {
    object Idle : ContentSyncStatus
    object Syncing : ContentSyncStatus
    data class Success(val timestamp: Long = System.currentTimeMillis()) : ContentSyncStatus
    data class Error(val message: String, val hasCachedData: Boolean) : ContentSyncStatus
}

/**
 * Repository coordinating between the Cloudflare Worker API and the local Room Database.
 * - Fetches scenarios, vocabulary, and grammar on start and on pull-to-refresh
 * - Caches responses into Room entities
 * - Falls back to Room data when offline or on network failure
 * - Never shows fake/placeholder content on failure: exposes clear sync status
 */
class KatzuContentRepository(
    context: Context,
    private val apiClient: KatzuContentApiClient = KatzuContentApiClient(),
    private val database: KatzuDatabase = KatzuDatabase.getInstance(context)
) {
    private val dao = database.katzuDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<ContentSyncStatus>(ContentSyncStatus.Idle)
    val syncStatus: StateFlow<ContentSyncStatus> = _syncStatus.asStateFlow()

    init {
        // Initial fetch on app start
        refreshAll()
    }

    // --- Scenarios ---
    fun getScenariosFlow(): Flow<List<ScenarioEntity>> = dao.getAllScenariosFlow()

    fun getScenarioFlow(id: String): Flow<ScenarioEntity?> = dao.getScenarioByIdFlow(id)

    suspend fun getScenario(id: String): ScenarioEntity? = dao.getScenarioById(id)

    fun getStarterPhrasesFlow(scenarioId: String, level: String? = null): Flow<List<StarterPhraseEntity>> {
        return if (level == null) {
            dao.getStarterPhrasesFlow(scenarioId)
        } else {
            dao.getStarterPhrasesByLevelFlow(scenarioId, level)
        }
    }

    fun getStarterPhrasesByLevelFlow(scenarioId: String, level: String): Flow<List<StarterPhraseEntity>> {
        return dao.getStarterPhrasesByLevelFlow(scenarioId, level)
    }

    // --- Vocabulary ---
    fun getVocabularyFlow(level: String? = null, topic: String? = null): Flow<List<VocabularyEntity>> {
        return when {
            level != null && topic != null -> dao.getVocabularyByLevelAndTopicFlow(level, topic)
            level != null -> dao.getVocabularyByLevelFlow(level)
            topic != null -> dao.getVocabularyByTopicFlow(topic)
            else -> dao.getAllVocabularyFlow()
        }
    }

    suspend fun getVocabularyById(id: Int): VocabularyEntity? = dao.getVocabularyById(id)

    // --- Grammar ---
    fun getGrammarFlow(level: String? = null): Flow<List<GrammarEntity>> {
        return if (level != null) dao.getGrammarByLevelFlow(level) else dao.getAllGrammarFlow()
    }

    // --- Saved Words (Bookmarks) ---
    fun getSavedWordIdsFlow(): Flow<List<String>> = dao.getAllSavedWordIdsFlow()

    suspend fun toggleSaveWord(wordId: String): Boolean {
        val currentlySaved = dao.isWordSaved(wordId)
        if (currentlySaved) {
            dao.deleteSavedWord(wordId)
            return false
        } else {
            dao.insertSavedWord(SavedWordEntity(wordId = wordId))
            return true
        }
    }


    // --- Refresh / Pull-To-Refresh ---
    fun refreshAll(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            _syncStatus.value = ContentSyncStatus.Syncing
            try {
                // 1. Fetch & cache scenarios
                val remoteScenarios = apiClient.fetchScenarios()
                if (remoteScenarios.isNotEmpty()) {
                    dao.insertScenarios(remoteScenarios)

                    // Pre-cache starter phrases for scenarios
                    for (s in remoteScenarios) {
                        try {
                            val (_, phrases) = apiClient.fetchScenarioDetail(s.id)
                            if (phrases.isNotEmpty()) {
                                dao.deleteStarterPhrasesForScenario(s.id)
                                dao.insertStarterPhrases(phrases)
                            }
                        } catch (e: Exception) {
                            Log.w("KatzuContentRepo", "Could not fetch phrases for scenario ${s.id}", e)
                        }
                    }
                }

                // 2. Fetch & cache vocabulary
                val remoteVocab = apiClient.fetchVocabulary()
                if (remoteVocab.isNotEmpty()) {
                    dao.insertVocabulary(remoteVocab)
                }

                // 3. Fetch & cache grammar
                val remoteGrammar = apiClient.fetchGrammar()
                if (remoteGrammar.isNotEmpty()) {
                    dao.insertGrammar(remoteGrammar)
                }

                _syncStatus.value = ContentSyncStatus.Success()
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e("KatzuContentRepo", "Failed to fetch content from worker", e)
                val existingScenarios = dao.getAllScenarios()
                val hasCachedData = existingScenarios.isNotEmpty()
                _syncStatus.value = ContentSyncStatus.Error(
                    message = e.localizedMessage ?: "فشل الاتصال بخادم المحتوى",
                    hasCachedData = hasCachedData
                )
                onComplete?.invoke(false)
            }
        }
    }

    suspend fun refreshScenarioDetail(scenarioId: String): Boolean {
        return try {
            val (scenario, phrases) = apiClient.fetchScenarioDetail(scenarioId)
            dao.updateScenarioWithPhrases(scenario, phrases)
            true
        } catch (e: Exception) {
            Log.e("KatzuContentRepo", "Failed to refresh scenario $scenarioId", e)
            false
        }
    }

    suspend fun sendConversationTurn(
        scenarioId: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList()
    ): Triple<String, String, String?> {
        return apiClient.sendConversationTurn(scenarioId, userMessage, history)
    }
}

