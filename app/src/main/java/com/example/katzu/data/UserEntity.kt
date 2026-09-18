package com.example.katzu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local user profile & subscription state.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "current_user",
    val email: String = "",
    val googleAccountEmail: String? = null,
    val displayName: String? = null,
    val isLoggedIn: Boolean = false,
    val subscriptionExpiresAt: String? = null,
    val isSubscriptionActive: Boolean = false,
    val lastCheckedAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
