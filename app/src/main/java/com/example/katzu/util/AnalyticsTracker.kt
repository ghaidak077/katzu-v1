package com.example.katzu.util

import android.content.Context
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

object AnalyticsTracker {
    private const val TAG = "AnalyticsTracker"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // PostHog setup with default fallback or placeholder if not configured
            val config = PostHogAndroidConfig(
                apiKey = "phc_katzu_production_placeholder",
                host = "https://eu.i.posthog.com"
            ).apply {
                captureApplicationLifecycleEvents = true
                captureScreenViews = true
            }
            PostHogAndroid.setup(context.applicationContext, config)
            isInitialized = true
            AppLogger.i(TAG, "PostHog Analytics initialized successfully")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to initialize PostHog: ${e.message}")
        }
    }

    fun track(event: String, properties: Map<String, Any> = emptyMap()) {
        AppLogger.i(TAG, "[EVENT] $event -> $properties")
        try {
            if (isInitialized) {
                PostHog.capture(
                    event = event,
                    properties = properties
                )
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to capture event $event: ${e.message}")
        }
    }

    fun trackOnboardingStarted() {
        track("onboarding_started")
    }

    fun trackOnboardingCompleted(goalId: String = "", experience: String = "") {
        val props = mutableMapOf<String, Any>()
        if (goalId.isNotBlank()) props["goal_id"] = goalId
        if (experience.isNotBlank()) props["previous_experience"] = experience
        track("onboarding_completed", props)
    }

    fun trackSessionStarted(scenarioId: String, level: String) {
        track(
            "session_started", mapOf(
                "scenarioId" to scenarioId,
                "level" to level
            )
        )
    }

    fun trackSessionCompleted(
        scenarioId: String,
        level: String,
        accuracy: Int,
        durationSeconds: Int,
        hintsUsedCount: Int
    ) {
        track(
            "session_completed", mapOf(
                "scenarioId" to scenarioId,
                "level" to level,
                "accuracy" to accuracy,
                "durationSeconds" to durationSeconds,
                "hintsUsedCount" to hintsUsedCount
            )
        )
    }

    fun trackPaywallViewed(source: String = "general") {
        track("paywall_viewed", mapOf("source" to source))
    }

    fun trackTrialStarted(planId: String = "free_trial_7d") {
        track("trial_started", mapOf("plan_id" to planId))
    }
}
