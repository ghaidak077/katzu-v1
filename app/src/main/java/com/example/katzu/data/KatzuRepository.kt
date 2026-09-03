package com.example.katzu.data

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.katzu.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class KatzuRepository(context: Context) {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.GERMAN)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f) {
        if (isTtsReady) {
            tts?.setSpeechRate(speed)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "katzu_tts_${System.currentTimeMillis()}")
        }
    }

    fun updateUserName(name: String) {
        val current = _userProfile.value
        _userProfile.value = current.copy(name = name.ifBlank { "سامر" })
    }

    fun toggleWordSaved(wordId: String) {
        val word = vocabularyList.find { it.id == wordId }
        word?.let {
            it.isSaved = !it.isSaved
        }
    }

    val trailNodes = listOf(
        TrailNode(
            id = "node_1",
            germanTitle = "Sich vorstellen & Begrüßung",
            arabicTitle = "التعارف والتحية الأولى",
            levelTag = "A1.1",
            status = TrailNodeStatus.Mastered,
            accuracyPercent = 94,
            stepInfo = "الدرس 1",
            timeEstimate = "3 دقائق",
            isClickable = true
        ),
        TrailNode(
            id = "im_cafe_bestellen",
            germanTitle = "Im Café bestellen",
            arabicTitle = "طلب قهوة وحساب المقهى",
            levelTag = "A1.1",
            status = TrailNodeStatus.Active,
            accuracyPercent = null,
            stepInfo = "خطوة 2 / 4",
            timeEstimate = "~ 4 دقائق",
            isClickable = true
        ),
        TrailNode(
            id = "node_3",
            germanTitle = "Im Supermarkt einkaufen",
            arabicTitle = "التسوق وسؤال الأسعار",
            levelTag = "A1.2",
            status = TrailNodeStatus.Upcoming,
            accuracyPercent = null,
            stepInfo = "الدرس 3",
            timeEstimate = "5 دقائق",
            isClickable = false
        ),
        TrailNode(
            id = "node_4",
            germanTitle = "Nach dem Weg fragen",
            arabicTitle = "السؤال عن الاتجاهات والمواصلات",
            levelTag = "A2.1",
            status = TrailNodeStatus.Locked,
            accuracyPercent = null,
            stepInfo = "الدرس 4",
            timeEstimate = "6 دقائق",
            isClickable = false
        )
    )

    val cafeScenario = Scenario(
        id = "im_cafe_bestellen",
        titleArabic = "طلب قهوة وحساب المقهى",
        titleGerman = "Im Café bestellen",
        scenarioNumber = "سيناريو 03",
        timeEstimate = "6 دقائق",
        cefrLevel = "A1 • مبتدئ",
        description = "تعلّم طلب مشروبك المفضل، السؤال عن الحساب، والتعامل مع نادل برليني غير مبالٍ باللغة الألمانية.",
        coachName = "كاتزو",
        coachTitle = "(نادل المقهى المتطلب)",
        coachRoastQuote = "“لو قلت Ein Kaffee بدل Einen Kaffee، سأتجاهلك بهدوء تام لمدة 5 ثوانٍ.”",
        goals = listOf(
            ScenarioGoal(
                id = "goal_1",
                titleArabic = "إلقاء التحية وطلب فنجان قهوة",
                subtitleGerman = "Akkusativ + Möchte",
                status = GoalStatus.Completed
            ),
            ScenarioGoal(
                id = "goal_2",
                titleArabic = "الاستفسار عن كعكة اليوم وحليب الشوفان",
                subtitleGerman = "Haben Sie...? / Gibt es...?",
                status = GoalStatus.InProgress
            ),
            ScenarioGoal(
                id = "goal_3",
                titleArabic = "طلب الفاتورة ودفع الحساب نقداً أو بالبطاقة",
                subtitleGerman = "Zahlen + Bar / Karte",
                status = GoalStatus.Pending
            )
        ),
        vocabularyWordIds = listOf("der_kaffee", "die_rechnung", "das_croissant", "bitte", "zahlen")
    )

    val vocabularyList = mutableListOf(
        VocabularyWord(
            id = "der_kaffee",
            germanWord = "Kaffee",
            article = "der",
            phonetic = "[kaˈfeː]",
            level = "A1 Basis",
            partOfSpeech = "اسم • Substantiv",
            gender = GermanGender.Masculine,
            plural = "die Kaffees",
            arabicMeaning = "قَهْوَة",
            englishMeaning = "Coffee",
            exampleGerman = "Ich trinke morgens immer einen Kaffee.",
            exampleArabic = "أشرب دائماً فنجان قهوة في الصباح.",
            mnemonicTip = "في ألمانيا، إذا طلبت Kaffee فقط فستحصل على قهوة سوداء مصفاة عادية. إن أردت إسبريسو أو كابتشينو، حدد ذلك بدقة.",
            category = "الأسماء",
            isMastered = true,
            isSaved = true
        ),
        VocabularyWord(
            id = "die_rechnung",
            germanWord = "Rechnung",
            article = "die",
            phonetic = "[ˈʁɛçnʊŋ]",
            level = "A1 Basis",
            partOfSpeech = "اسم • Substantiv",
            gender = GermanGender.Feminine,
            plural = "die Rechnungen",
            arabicMeaning = "الفاتورة / الحساب",
            englishMeaning = "Bill / Check",
            exampleGerman = "Könnte ich bitte die Rechnung haben?",
            exampleArabic = "هل يمكنني الحصول على الفاتورة من فضلك؟",
            mnemonicTip = "كل كلمة ألمانية تنتهي بـ -ung هي مؤنثة دائماً وأداتها die دون استثناء!",
            category = "عبارات المقهى",
            isMastered = true,
            isSaved = false
        ),
        VocabularyWord(
            id = "das_croissant",
            germanWord = "Croissant",
            article = "das",
            phonetic = "[kʁoaˈsɑ̃ː]",
            level = "A1",
            partOfSpeech = "اسم • Substantiv",
            gender = GermanGender.Neuter,
            plural = "die Croissants",
            arabicMeaning = "كرواسون",
            englishMeaning = "Croissant",
            exampleGerman = "Ein Croissant und ein Wasser, bitte.",
            exampleArabic = "كرواسون وماء، من فضلك.",
            mnemonicTip = "الكلمات الدخيلة من الفرنسية التي تنتهي بأصوات متحركة غالباً ما تأخذ الأداة المحايدة das.",
            category = "الأسماء",
            isMastered = false,
            isSaved = true
        ),
        VocabularyWord(
            id = "bitte",
            germanWord = "bitte",
            article = "",
            phonetic = "[ˈbɪtə]",
            level = "A1 Basis",
            partOfSpeech = "ظرف / تعبير • Partikel",
            gender = GermanGender.None,
            plural = "",
            arabicMeaning = "من فضلك / عفواً",
            englishMeaning = "Please / You're welcome",
            exampleGerman = "Einen Espresso, bitte!",
            exampleArabic = "إسبريسو، من فضلك!",
            mnemonicTip = "الكلمة السحرية في ألمانيا. استخدمها دائماً لتفادي النظرات الباردة من طاقم الخدمة.",
            category = "عبارات المقهى",
            isMastered = true,
            isSaved = true
        ),
        VocabularyWord(
            id = "zahlen",
            germanWord = "zahlen",
            article = "",
            phonetic = "[ˈtsaːlən]",
            level = "A1 Basis",
            partOfSpeech = "فعل • Verb",
            gender = GermanGender.None,
            plural = "",
            arabicMeaning = "يدفع (المال)",
            englishMeaning = "To pay",
            exampleGerman = "Wir möchten bitte zahlen.",
            exampleArabic = "نود أن ندفع الحساب من فضلك.",
            mnemonicTip = "في المطاعم الألمانية سيسألك النادل: 'Zusammen oder getrennt?' (معاً أم منفصلين؟).",
            category = "الأفعال",
            isMastered = false,
            isSaved = false
        ),
        VocabularyWord(
            id = "das_wasser",
            germanWord = "Wasser",
            article = "das",
            phonetic = "[ˈvasɐ]",
            level = "A1 Basis",
            partOfSpeech = "اسم • Substantiv",
            gender = GermanGender.Neuter,
            plural = "die Wässer",
            arabicMeaning = "ماء",
            englishMeaning = "Water",
            exampleGerman = "Ein stilles Wasser, bitte.",
            exampleArabic = "ماء بدون غاز، من فضلك.",
            mnemonicTip = "انتبه! إذا طلبت Wasser في ألمانيا ستحصل تلقائياً على ماء غازي (mit Kohlensäure). اطلب 'stilles Wasser' إن أردت ماء عادياً.",
            category = "الأسماء",
            isMastered = true,
            isSaved = false
        )
    )

    fun getInitialConversation(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                id = "msg_1",
                sender = MessageSender.Katzu,
                germanText = "Guten Tag! Was darf es denn sein?",
                arabicTranslation = "طاب يومك! ماذا يمكنني أن أقدم لك؟",
                timestamp = "10:42 ص"
            ),
            ChatMessage(
                id = "msg_2",
                sender = MessageSender.User,
                germanText = "Guten Tag! Ich will ein Kaffee mit Milch.",
                arabicTranslation = "\"أريد قهوة بالحليب.\"",
                hasCorrection = true,
                originalMistake = "Ich will ein Kaffee",
                correctedGerman = "Ich möchte bitte einen Kaffee mit Milch.",
                roastComment = "في برلين، طلبك بـ Ich will قد يضمن لك نظرة ازدراء فورية من النادل! قل بدلاً منها Ich möchte bitte بلباقة. ولأن der Kaffee مذكر ووقع هنا مفعولاً به (Akkusativ)، تنقلب أداة النكرة حتماً إلى einen.",
                grammarRule = "لباقة + Akkusativ",
                timestamp = "10:43 ص"
            )
        )
    }
}
