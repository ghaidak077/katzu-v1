package com.example.katzu.model

enum class AppScreen {
    Welcome,
    MainTabs,
    ScenarioDetail,
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
    val timestamp: String = ""
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
    val vocabularyWordIds: List<String>
)

data class UserProfile(
    var name: String = "سامر الشامي",
    var email: String = "samer@example.com",
    var streakDays: Int = 5,
    var xp: Int = 1240,
    var currentLevel: String = "A1.1",
    var masteredWordsCount: Int = 114,
    var learningWordsCount: Int = 28,
    var totalWordsCount: Int = 142,
    var completedScenarios: Int = 19,
    var practiceHours: Double = 4.8,
    var fluencyRatePercent: Int = 88,
    var speechSpeed: Float = 1.0f,
    var sarcasmLevel: String = "لاذع وساخر",
    var isProActive: Boolean = true
)
