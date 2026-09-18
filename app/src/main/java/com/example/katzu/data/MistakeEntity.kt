package com.example.katzu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a persisted German language mistake logged during practice sessions.
 * Enables post-session mistake reviews and adaptive spaced repetition across app sessions.
 */
@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val scenarioId: String,
    val original: String,
    val corrected: String,
    val grammarRule: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasHintUsed: Boolean = false
)
