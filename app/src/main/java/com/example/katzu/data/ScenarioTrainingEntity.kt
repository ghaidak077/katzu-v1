package com.example.katzu.data

import androidx.room.Entity

/**
 * Tracks per-scenario, per-user training completion (Study & Quiz flow).
 */
@Entity(
    tableName = "scenario_training",
    primaryKeys = ["scenarioId", "userId"]
)
data class ScenarioTrainingEntity(
    val scenarioId: String,
    val userId: String = "current_user",
    val studiedAt: Long? = null,
    val quizAttempted: Boolean = false,
    val lastScore: Int? = null,
    val effectiveLevel: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Maps scenario.category to vocabulary.topic.
 * "official" -> "documents", "daily_life" -> "food", everything else maps to itself.
 */
fun mapCategoryToTopic(category: String): String {
    return when (category.lowercase().trim()) {
        "official" -> "documents"
        "daily_life" -> "food"
        else -> category.lowercase().trim()
    }
}
