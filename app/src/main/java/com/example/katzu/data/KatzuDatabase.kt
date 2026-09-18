package com.example.katzu.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.katzu.util.AppLogger
import kotlinx.coroutines.flow.Flow

@Dao
interface KatzuDao {

    // --- Scenarios ---
    @Query("SELECT * FROM scenarios")
    fun getAllScenariosFlow(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios")
    suspend fun getAllScenarios(): List<ScenarioEntity>

    @Query("SELECT * FROM scenarios WHERE id = :id LIMIT 1")
    suspend fun getScenarioById(id: String): ScenarioEntity?

    @Query("SELECT * FROM scenarios WHERE id = :id LIMIT 1")
    fun getScenarioByIdFlow(id: String): Flow<ScenarioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarios(scenarios: List<ScenarioEntity>)

    // --- Starter Phrases ---
    @Query("SELECT * FROM starter_phrases WHERE scenario_id = :scenarioId ORDER BY sort_order ASC")
    fun getStarterPhrasesFlow(scenarioId: String): Flow<List<StarterPhraseEntity>>

    @Query("SELECT * FROM starter_phrases WHERE scenario_id = :scenarioId AND level = :level ORDER BY sort_order ASC")
    fun getStarterPhrasesByLevelFlow(scenarioId: String, level: String): Flow<List<StarterPhraseEntity>>

    @Query("SELECT * FROM starter_phrases WHERE scenario_id = :scenarioId AND level = :level ORDER BY sort_order ASC")
    suspend fun getStarterPhrasesByLevel(scenarioId: String, level: String): List<StarterPhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStarterPhrases(phrases: List<StarterPhraseEntity>)

    @Query("DELETE FROM starter_phrases WHERE scenario_id = :scenarioId")
    suspend fun deleteStarterPhrasesForScenario(scenarioId: String): Int

    @Transaction
    suspend fun updateScenarioWithPhrases(scenario: ScenarioEntity, phrases: List<StarterPhraseEntity>) {
        insertScenarios(listOf(scenario))
        deleteStarterPhrasesForScenario(scenario.id)
        insertStarterPhrases(phrases)
    }

    // --- Vocabulary ---
    @Query("SELECT * FROM vocabulary")
    fun getAllVocabularyFlow(): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE level = :level")
    fun getVocabularyByLevelFlow(level: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE level = :level AND topic = :topic")
    fun getVocabularyByLevelAndTopicFlow(level: String, topic: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE topic = :topic")
    fun getVocabularyByTopicFlow(topic: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1")
    suspend fun getVocabularyById(id: Int): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE LOWER(german) = LOWER(:word) LIMIT 1")
    suspend fun findVocabularyByExactGerman(word: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE LOWER(german) LIKE '%' || LOWER(:word) || '%' OR LOWER(:word) LIKE '%' || LOWER(german) || '%' LIMIT 1")
    suspend fun findVocabularyByFuzzyGerman(word: String): VocabularyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(words: List<VocabularyEntity>)

    // --- Grammar ---
    @Query("SELECT * FROM grammar")
    fun getAllGrammarFlow(): Flow<List<GrammarEntity>>

    @Query("SELECT * FROM grammar WHERE level = :level")
    fun getGrammarByLevelFlow(level: String): Flow<List<GrammarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammar(grammarRules: List<GrammarEntity>)

    // --- Saved Words (Bookmarks) ---
    @Query("SELECT wordId FROM saved_words")
    fun getAllSavedWordIdsFlow(): Flow<List<String>>

    @Query("SELECT wordId FROM saved_words")
    suspend fun getAllSavedWordIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE wordId = :wordId)")
    suspend fun isWordSaved(wordId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWord(savedWord: SavedWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWords(savedWords: List<SavedWordEntity>)

    @Query("DELETE FROM saved_words WHERE wordId = :wordId")
    suspend fun deleteSavedWord(wordId: String)

    // --- User Profile & Subscription ---
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserFlow(id: String = "current_user"): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUser(id: String = "current_user"): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- Redeemed Codes ---
    @Query("SELECT EXISTS(SELECT 1 FROM redeemed_codes WHERE code = :code)")
    suspend fun isCodeRedeemed(code: String): Boolean

    @Query("SELECT * FROM redeemed_codes WHERE code = :code LIMIT 1")
    suspend fun getRedeemedCode(code: String): RedeemedCodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeemedCode(redeemedCode: RedeemedCodeEntity)

    // --- Practice Sessions ---
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions")
    fun getCompletedSessionsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getCompletedSessionsCount(): Int

    @Query("SELECT COALESCE(SUM(sentencesSpoken), 0) FROM sessions")
    fun getTotalSentencesSpokenFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(wordsLearned), 0) FROM sessions")
    fun getTotalWordsLearnedFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM sessions")
    fun getTotalDurationSecondsFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    // --- Saved Words Count ---
    @Query("SELECT COUNT(*) FROM saved_words")
    fun getSavedWordsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM saved_words")
    suspend fun getSavedWordsCount(): Int

    // --- Vocabulary Count ---
    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getVocabularyCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE level = :level")
    fun getVocabularyCountByLevelFlow(level: String): Flow<Int>

    // --- Scenario Training Progress ---
    @Query("SELECT * FROM scenario_training WHERE scenarioId = :scenarioId AND userId = :userId LIMIT 1")
    fun getScenarioTrainingFlow(scenarioId: String, userId: String = "current_user"): Flow<ScenarioTrainingEntity?>

    @Query("SELECT * FROM scenario_training WHERE scenarioId = :scenarioId AND userId = :userId LIMIT 1")
    suspend fun getScenarioTraining(scenarioId: String, userId: String = "current_user"): ScenarioTrainingEntity?

    @Query("SELECT * FROM scenario_training WHERE userId = :userId")
    fun getAllScenarioTrainingFlow(userId: String = "current_user"): Flow<List<ScenarioTrainingEntity>>

    @Query("SELECT * FROM scenario_training WHERE userId = :userId")
    suspend fun getAllScenarioTrainings(userId: String = "current_user"): List<ScenarioTrainingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioTraining(training: ScenarioTrainingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioTrainings(trainings: List<ScenarioTrainingEntity>)

    // --- Mistakes ---
    @Query("SELECT * FROM mistakes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMistakesForUserFlow(userId: String = "current_user"): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getMistakesForUser(userId: String = "current_user"): List<MistakeEntity>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakesFlow(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    suspend fun getAllMistakes(): List<MistakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistakes(mistakes: List<MistakeEntity>)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: Long)
}

/**
 * The single, unified Room Database instance for Katzu.
 */
@Database(
    entities = [
        ScenarioEntity::class,
        StarterPhraseEntity::class,
        VocabularyEntity::class,
        GrammarEntity::class,
        SavedWordEntity::class,
        UserEntity::class,
        RedeemedCodeEntity::class,
        SessionEntity::class,
        ScenarioTrainingEntity::class,
        MistakeEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class KatzuDatabase : RoomDatabase() {

    abstract fun katzuDao(): KatzuDao

    companion object {
        @Volatile
        private var INSTANCE: KatzuDatabase? = null

        /**
         * Helper to add any optional/nullable columns to `users` if migrating from early schemas.
         */
        private fun ensureUserColumns(db: SupportSQLiteDatabase) {
            try {
                val cursor = db.query("PRAGMA table_info(`users`)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx >= 0) {
                        existingColumns.add(cursor.getString(nameIdx))
                    }
                }
                cursor.close()

                if (!existingColumns.contains("googleAccountEmail")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `googleAccountEmail` TEXT")
                    AppLogger.i("KatzuDatabase", "Added googleAccountEmail column to users")
                }
                if (!existingColumns.contains("displayName")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `displayName` TEXT")
                    AppLogger.i("KatzuDatabase", "Added displayName column to users")
                }
                if (!existingColumns.contains("lastCheckedAt")) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `lastCheckedAt` TEXT")
                    AppLogger.i("KatzuDatabase", "Added lastCheckedAt column to users")
                }
            } catch (e: Exception) {
                AppLogger.w("KatzuDatabase", "Column check on users table skipped/failed: ${e.message}")
            }
        }

        /**
         * Migration 1 -> 2:
         * Add saved_words table for offline vocabulary bookmarking.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_1_2: creating saved_words table")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_words` (" +
                            "`wordId` TEXT NOT NULL, " +
                            "`savedAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`wordId`))"
                )
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 2 -> 3:
         * Add sessions table for real user practice session history and accuracy metrics.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_2_3: creating sessions table")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sessions` (" +
                            "`id` TEXT NOT NULL, " +
                            "`scenarioId` TEXT NOT NULL, " +
                            "`scenarioTitle` TEXT NOT NULL, " +
                            "`cefrLevel` TEXT NOT NULL, " +
                            "`sentencesSpoken` INTEGER NOT NULL, " +
                            "`wordsLearned` INTEGER NOT NULL, " +
                            "`accuracyPercent` INTEGER NOT NULL, " +
                            "`durationSeconds` INTEGER NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 3 -> 4:
         * Add redeemed_codes table for local code redemption audit logging.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_3_4: creating redeemed_codes table")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `redeemed_codes` (" +
                            "`code` TEXT NOT NULL, " +
                            "`redeemedAt` INTEGER NOT NULL, " +
                            "`monthsGranted` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`code`))"
                )
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 4 -> 5:
         * Add scenario_training table for tracking Study & Quiz progression per scenario.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_4_5: creating scenario_training table")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scenario_training` (" +
                            "`scenarioId` TEXT NOT NULL, " +
                            "`userId` TEXT NOT NULL, " +
                            "`studiedAt` INTEGER, " +
                            "`quizAttempted` INTEGER NOT NULL, " +
                            "`lastScore` INTEGER, " +
                            "`updatedAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`scenarioId`, `userId`))"
                )
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 5 -> 6:
         * Version parity alignment migration.
         * All 9 entities (scenarios, starter_phrases, vocabulary, grammar, saved_words,
         * users, redeemed_codes, sessions, scenario_training) were fully declared in v5.
         * Version 6 was a developmental version bump without entity schema changes.
         * This migration safely verifies user table columns and ensures schema parity
         * so Room's version upgrade check succeeds without dropping any tables or user state.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running MIGRATION_5_6: version parity alignment and user table column verification")
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 6 -> 7:
         * Add effectiveLevel column to scenario_training table for real-time difficulty nudge.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_6_7: adding effectiveLevel to scenario_training")
                try {
                    db.execSQL("ALTER TABLE `scenario_training` ADD COLUMN `effectiveLevel` TEXT")
                } catch (e: Exception) {
                    AppLogger.w("KatzuDatabase", "ALTER TABLE scenario_training failed (column may already exist): ${e.message}")
                }
                ensureUserColumns(db)
            }
        }

        /**
         * Migration 7 -> 8:
         * Add mistakes table to persist learning errors across sessions for spaced review.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("KatzuDatabase", "Running additive MIGRATION_7_8: creating mistakes table")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mistakes` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`userId` TEXT NOT NULL, " +
                            "`scenarioId` TEXT NOT NULL, " +
                            "`original` TEXT NOT NULL, " +
                            "`corrected` TEXT NOT NULL, " +
                            "`grammarRule` TEXT NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "`wasHintUsed` INTEGER NOT NULL)"
                )
                ensureUserColumns(db)
            }
        }

        fun getInstance(context: Context): KatzuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KatzuDatabase::class.java,
                    "katzu_database.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
