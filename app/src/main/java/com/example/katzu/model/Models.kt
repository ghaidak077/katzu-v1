package com.example.katzu.model

enum class AppScreen {
    Welcome,
    SignIn,
    SubscriptionRedemption,
    MainTabs,
    ScenarioDetail,
    Study,
    Quiz,
    LiveConversation,
    SessionReport
}

enum class NavigationTab {
    Trail,
    Practice,
    Progress,
    Profile
}

enum class TrailNodeStatus {
    Mastered,
    Active,
    Upcoming,
    Locked
}

data class TrailNode(
    val id: String,
    val germanTitle: String,
    val arabicTitle: String,
    val levelTag: String,
    val status: TrailNodeStatus,
    val accuracyPercent: Int? = null,
    val stepInfo: String,
    val timeEstimate: String,
    val isClickable: Boolean
)

enum class GoalStatus {
    Completed,
    InProgress,
    Pending
}

data class ScenarioGoal(
    val id: String,
    val titleArabic: String,
    val subtitleGerman: String,
    val status: GoalStatus
)

enum class GermanGender {
    Masculine,
    Feminine,
    Neuter,
    None
}

data class VocabularyWord(
    val id: String,
    val germanWord: String,
    val article: String = "", // der, die, das
    val phonetic: String,
    val level: String = "A1",
    val partOfSpeech: String,
    val gender: GermanGender = GermanGender.None,
    val plural: String = "",
    val arabicMeaning: String,
    val englishMeaning: String,
    val exampleGerman: String,
    val exampleArabic: String,
    val mnemonicTip: String,
    val category: String, // All, Nouns, Verbs, CafePhrases
    var isMastered: Boolean = false,
    var isSaved: Boolean = false
)

enum class MessageSender {
    Katzu,
    User
}

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val germanText: String,
    val arabicTranslation: String,
    val hasCorrection: Boolean = false,
    val originalMistake: String = "",
    val correctedGerman: String = "",
    val roastComment: String = "",
    val grammarRule: String = "",
    val timestamp: String = "",
    val mistakeSegment: String = "",
    val correctedSegment: String = "",
    val positiveNoteAr: String = "",
    val wasHintUsed: Boolean = false,
    val isGenerating: Boolean = false
)

data class SessionMistakeSummary(
    val originalMistake: String,
    val correctedGerman: String,
    val grammarRule: String,
    val roastComment: String,
    val wasHintUsed: Boolean = false
)

data class Scenario(
    val id: String,
    val titleArabic: String,
    val titleGerman: String,
    val scenarioNumber: String,
    val timeEstimate: String,
    val cefrLevel: String,
    val description: String,
    val coachName: String,
    val coachTitle: String,
    val coachRoastQuote: String,
    val goals: List<ScenarioGoal>,
    val vocabularyWordIds: List<String>,
    val category: String = "",
    val aiPersona: String = ""
)

data class UserProfile(
    var name: String = "",
    var email: String = "",
    var streakDays: Int = 0,
    var xp: Int = 0,
    var currentLevel: String = "A1",
    var masteredWordsCount: Int = 0,
    var learningWordsCount: Int = 0,
    var totalWordsCount: Int = 0,
    var completedScenarios: Int = 0,
    var practiceHours: Double = 0.0,
    var fluencyRatePercent: Int = 0,
    var speechSpeed: Float = 1.0f,
    var sarcasmLevel: String = "لاذع وساخر",
    var isProActive: Boolean = false,
    var dailyRemindersEnabled: Boolean = true,
    var reminderFrequencyPreset: String = "regular", // "casual", "regular", "serious", "intense"
    var reminderHour: Int = 20,
    var reminderMinute: Int = 0,
    var targetGoalId: String = "goal_a2",
    var targetGoalTitle: String = "الوصول إلى مستوى A2 والتحدث بثقة",
    var targetWeeklyDays: Int = 5,
    var targetDailyMinutes: Int = 10
)
