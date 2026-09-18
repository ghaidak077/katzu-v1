package com.example.katzu.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local Room Entity for persisting real user conversation practice sessions.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scenarioId: String,
    val scenarioTitle: String = "",
    val cefrLevel: String = "A1",
    val sentencesSpoken: Int = 0,
    val wordsLearned: Int = 0,
    val accuracyPercent: Int = 100,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
