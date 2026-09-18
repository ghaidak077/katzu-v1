package com.example.katzu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Audit log of codes redeemed locally on device.
 */
@Entity(tableName = "redeemed_codes")
data class RedeemedCodeEntity(
    @PrimaryKey val code: String,
    val redeemedAt: Long = System.currentTimeMillis(),
    val monthsGranted: Int = 1
)
