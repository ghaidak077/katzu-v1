package com.example.katzu.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirror of the Cloudflare D1 / Worker backend: GET /scenarios
 */
@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    val title_de: String,
    val title_ar: String,
    val ai_persona: String,
    val category: String,
    val icon: String,
    val initial_message_a1: String,
    val initial_message_a2: String,
    val initial_message_b1: String,
    val initial_message_b2: String
) {
    fun getInitialMessageForLevel(cefrLevel: String): String {
        val level = cefrLevel.uppercase().trim().take(2)
        val msg = when (level) {
            "A2" -> initial_message_a2.ifBlank { initial_message_a1 }
            "B1" -> initial_message_b1.ifBlank { initial_message_a1 }
            "B2" -> initial_message_b2.ifBlank { initial_message_b1.ifBlank { initial_message_a1 } }
            else -> initial_message_a1
        }
        return msg.ifBlank {
            when {
                initial_message_a1.isNotBlank() -> initial_message_a1
                initial_message_a2.isNotBlank() -> initial_message_a2
                initial_message_b1.isNotBlank() -> initial_message_b1
                initial_message_b2.isNotBlank() -> initial_message_b2
                else -> ""
            }
        }
    }

    fun getInitialMessageTranslationForLevel(cefrLevel: String): String {
        val level = cefrLevel.uppercase().trim().take(2)
        val germanMsg = getInitialMessageForLevel(level).trim()
        val byGerman = ScenarioTranslations.getByGermanText(germanMsg)
        if (byGerman.isNotBlank()) return byGerman
        return ScenarioTranslations.getByScenarioAndLevel(id, level)
    }
}

/**
 * Mirror of the starter phrases nested in: GET /scenarios/:id
 */
@Entity(
    tableName = "starter_phrases",
    foreignKeys = [
        ForeignKey(
            entity = ScenarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["scenario_id"]),
        Index(value = ["scenario_id", "level"])
    ]
)
data class StarterPhraseEntity(
    @PrimaryKey val id: Int,
    val scenario_id: String,
    val level: String, // A1 / A2 / B1 / B2
    val german: String,
    val translation_en: String,
    val translation_ar: String,
    val sort_order: Int
)

/**
 * Mirror of the Cloudflare D1 / Worker backend: GET /vocabulary
 */
@Entity(
    tableName = "vocabulary",
    indices = [
        Index(value = ["level"]),
        Index(value = ["topic"]),
        Index(value = ["level", "topic"])
    ]
)
data class VocabularyEntity(
    @PrimaryKey val id: Int,
    val german: String,
    val article: String = "",
    val plural: String = "",
    val part_of_speech: String = "",
    val translation_ar: String,
    val translation_en: String,
    val example_de: String = "",
    val example_ar: String = "",
    val example_en: String = "",
    val level: String, // A1 / A2 / B1 / B2
    val topic: String
)

/**
 * Mirror of the Cloudflare D1 / Worker backend: GET /grammar
 */
@Entity(
    tableName = "grammar",
    indices = [
        Index(value = ["level"])
    ]
)
data class GrammarEntity(
    @PrimaryKey val id: String,
    val level: String, // A1 / A2 / B1 / B2
    val title_ar: String,
    val title_en: String,
    val explanation_ar: String,
    val explanation_en: String,
    val example_de: String
)

// --- Mapper Extension Functions ---

fun VocabularyEntity.toUiModel(): com.example.katzu.model.VocabularyWord {
    val parsedGender = when (article.lowercase().trim()) {
        "der" -> com.example.katzu.model.GermanGender.Masculine
        "die" -> com.example.katzu.model.GermanGender.Feminine
        "das" -> com.example.katzu.model.GermanGender.Neuter
        else -> com.example.katzu.model.GermanGender.None
    }

    return com.example.katzu.model.VocabularyWord(
        id = id.toString(),
        germanWord = german,
        article = article,
        phonetic = if (article.isNotBlank()) "[$article $german]" else "[$german]",
        level = level,
        partOfSpeech = part_of_speech.ifBlank { "مفردة • Vokabel" },
        gender = parsedGender,
        plural = plural,
        arabicMeaning = translation_ar,
        englishMeaning = translation_en,
        exampleGerman = example_de.ifBlank { "Ich lerne $german." },
        exampleArabic = example_ar.ifBlank { translation_ar },
        mnemonicTip = "موضوع: $topic • مستوى: $level",
        category = when (topic.lowercase().trim()) {
            "cafe", "food" -> "عبارات المقهى"
            "shopping" -> "التسوق"
            else -> if (part_of_speech.contains("verb", ignoreCase = true)) "الأفعال" else "الأسماء"
        },
        isMastered = false,
        isSaved = false
    )
}

fun ScenarioEntity.toUiModel(
    starterPhrases: List<StarterPhraseEntity> = emptyList(),
    level: String = "A1"
): com.example.katzu.model.Scenario {
    val goals = starterPhrases.mapIndexed { index, phrase ->
        com.example.katzu.model.ScenarioGoal(
            id = "goal_${phrase.id}",
            titleArabic = phrase.translation_ar.ifBlank { phrase.translation_en },
            subtitleGerman = phrase.german,
            status = if (index == 0) com.example.katzu.model.GoalStatus.InProgress else com.example.katzu.model.GoalStatus.Pending
        )
    }

    return com.example.katzu.model.Scenario(
        id = id,
        titleArabic = title_ar,
        titleGerman = title_de,
        scenarioNumber = "سيناريو $category",
        timeEstimate = "~ 5 دقائق",
        cefrLevel = level,
        description = "تدرّب مع شخصية $ai_persona في سيناريو واقعي للحياة اليومية في ألمانيا.",
        coachName = "كاتزو",
        coachTitle = "($ai_persona)",
        coachRoastQuote = "“تحدث بثقة واضحة ولا تخف من ارتكاب الأخطاء، أنا هنا للتصحيح وليس للمجاملة.”",
        goals = goals,
        vocabularyWordIds = emptyList(),
        category = category,
        aiPersona = ai_persona
    )
}

/**
 * Entity for tracking user bookmarks/saved words in Room locally
 */
@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey val wordId: String,
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * Direct bilingual translations for scenario initial messages across all levels and scenarios.
 * Ensures zero-latency, 100% reliable offline/online Arabic translations for conversation openers.
 */
object ScenarioTranslations {
    private val germanToArabic = mapOf(
        // Embassy appointment
        "Guten Tag. Wie kann ich Ihnen helfen?" to "طاب يومك. كيف يمكنني مساعدتك؟",
        "Guten Tag! Wie kann ich Ihnen helfen?" to "طاب يومك! كيف يمكنني مساعدتك؟",
        "Guten Tag. Was ist der Grund Ihres Besuchs heute?" to "طاب يومك. ما هو سبب زيارتك اليوم؟",
        "Guten Tag, bitte nehmen Sie Platz und schildern Sie mir Ihr Anliegen." to "طاب يومك، تفضل بالجلوس واشرح لي ما هو طلبك.",
        "Guten Tag, ich würde gerne die Einzelheiten Ihres Antrags im Detail mit Ihnen besprechen." to "طاب يومك، أود أن أناقش معك تفاصيل طلبك بالتفصيل.",

        // Cafe order
        "Hallo! Was möchten Sie trinken?" to "مرحباً! ماذا ترغب أن تشرب؟",
        "Hallo! Was darf ich Ihnen heute bringen?" to "مرحباً! ماذا يمكنني أن أحضر لك اليوم؟",
        "Guten Tag, haben Sie schon gewählt, oder brauchen Sie noch einen Moment?" to "طاب يومك، هل اخترت أم ما زلت تحتاج إلى لحظة؟",
        "Guten Tag, darf ich Ihnen unsere heutigen Empfehlungen vorschlagen, bevor Sie sich entscheiden?" to "طاب يومك، هل تسمح لي باقتراح توصياتنا لليوم قبل أن تقرر؟",

        // Job interview
        "Hallo, schön dass Sie da sind. Wie heißen Sie?" to "مرحباً، يسعدني حضورك. ما اسمك؟",
        "Guten Tag, erzählen Sie mir bitte kurz etwas über sich." to "طاب يومك، حدثني عن نفسك باختصار من فضلك.",
        "Guten Tag, könnten Sie mir Ihre bisherige Berufserfahrung genauer erläutern?" to "طاب يومك، هل يمكنك توضيح خبرتك المهنية السابقة بمزيد من التفصيل؟",
        "Guten Tag, inwiefern glauben Sie, dass Ihre Qualifikationen zu den Anforderungen dieser Position passen?" to "طاب يومك، إلى أي مدى تعتقد أن مؤهلاتك تتناسب مع متطلبات هذا المنصب؟",

        // Doctor visit
        "Guten Tag. Was fehlt Ihnen?" to "طاب يومك. مم تشكو؟ (ما الذي يزعجك؟)",
        "Guten Tag, wo genau haben Sie Schmerzen?" to "طاب يومك، أين تشعر بالألم تحديداً؟",
        "Guten Tag, seit wann bestehen diese Beschwerden schon?" to "طاب يومك، منذ متى وأنت تعاني من هذه الأعراض؟",
        "Guten Tag, könnten Sie mir Ihre Symptome möglichst genau und in der zeitlichen Reihenfolge schildern?" to "طاب يومك، هل يمكنك وصف أعراضك بأكبر قدر من الدقة وحسب تسلسلها الزمني؟",

        // Apartment viewing
        "Hallo! Willkommen. Das ist die Wohnung." to "مرحباً! أهلاً وسهلاً. هذه هي الشقة.",
        "Hallo! Schön, dass Sie da sind. Ich zeige Ihnen die Wohnung." to "مرحباً! يسعدني حضورك. سأريك الشقة.",
        "Guten Tag, kommen Sie bitte herein — ich zeige Ihnen zunächst das Wohnzimmer." to "طاب يومك، تفضل بالدخول — سأريك غرفة الجلوس أولاً.",
        "Guten Tag, bevor wir beginnen, hätten Sie im Voraus schon spezielle Fragen zur Wohnung oder zum Mietvertrag?" to "طاب يومك، قبل أن نبدأ، هل لديك مسبقاً أي أسئلة خاصة حول الشقة أو عقد الإيجار؟"
    )

    fun getByGermanText(german: String): String {
        val trimmed = german.trim()
        if (trimmed.isBlank()) return ""
        germanToArabic[trimmed]?.let { return it }

        // Normalization fallback (ignore trailing punctuation or case variations)
        val normalized = trimmed.trimEnd('.', '!', '?', ' ')
        return germanToArabic.entries.firstOrNull {
            it.key.trimEnd('.', '!', '?', ' ').equals(normalized, ignoreCase = true)
        }?.value.orEmpty()
    }

    fun getByScenarioAndLevel(scenarioId: String, cefrLevel: String): String {
        val level = cefrLevel.uppercase().trim().take(2)
        return when (scenarioId) {
            "cafe_order" -> when (level) {
                "A2" -> "مرحباً! ماذا يمكنني أن أحضر لك اليوم؟"
                "B1" -> "طاب يومك، هل اخترت أم ما زلت تحتاج إلى لحظة؟"
                "B2" -> "طاب يومك، هل تسمح لي باقتراح توصياتنا لليوم قبل أن تقرر؟"
                else -> "مرحباً! ماذا ترغب أن تشرب؟"
            }
            "embassy_appointment" -> when (level) {
                "A2" -> "طاب يومك. ما هو سبب زيارتك اليوم؟"
                "B1" -> "طاب يومك، تفضل بالجلوس واشرح لي ما هو طلبك."
                "B2" -> "طاب يومك، أود أن أناقش معك تفاصيل طلبك بالتفصيل."
                else -> "طاب يومك. كيف يمكنني مساعدتك؟"
            }
            "job_interview" -> when (level) {
                "A2" -> "طاب يومك، حدثني عن نفسك باختصار من فضلك."
                "B1" -> "طاب يومك، هل يمكنك توضيح خبرتك المهنية السابقة بمزيد من التفصيل؟"
                "B2" -> "طاب يومك، إلى أي مدى تعتقد أن مؤهلاتك تتناسب مع متطلبات هذا المنصب؟"
                else -> "مرحباً، يسعدني حضورك. ما اسمك؟"
            }
            "doctor_visit" -> when (level) {
                "A2" -> "طاب يومك، أين تشعر بالألم تحديداً؟"
                "B1" -> "طاب يومك، منذ متى وأنت تعاني من هذه الأعراض؟"
                "B2" -> "طاب يومك، هل يمكنك وصف أعراضك بأكبر قدر من الدقة وحسب تسلسلها الزمني؟"
                else -> "طاب يومك. مم تشكو؟ (ما الذي يزعجك؟)"
            }
            "apartment_viewing" -> when (level) {
                "A2" -> "مرحباً! يسعدني حضورك. سأريك الشقة."
                "B1" -> "طاب يومك، تفضل بالدخول — سأريك غرفة الجلوس أولاً."
                "B2" -> "طاب يومك، قبل أن نبدأ، هل لديك مسبقاً أي أسئلة خاصة حول الشقة أو عقد الإيجار؟"
                else -> "مرحباً! أهلاً وسهلاً. هذه هي الشقة."
            }
            else -> ""
        }
    }
}

