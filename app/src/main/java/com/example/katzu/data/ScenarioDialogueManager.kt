package com.example.katzu.data

/**
 * Data structures representing a full multi-turn conversation training flow.
 * Each step consists of a Partner Question/Prompt and 2-3 Answer Options the learner can reply with.
 */
data class DialogueAnswerOption(
    val id: String,
    val german: String,
    val translationAr: String,
    val badge: String = "",
    val tipAr: String = ""
)

data class DialogueTurn(
    val turnNumber: Int,
    val totalTurns: Int,
    val phaseTitleAr: String,
    val partnerGerman: String,
    val partnerArabic: String,
    val partnerPersona: String,
    val answers: List<DialogueAnswerOption>
)

object ScenarioDialogueManager {

    /**
     * Builds a complete, multi-turn dialogue practice flow for any scenario and CEFR level.
     * Guaranteed to include 3 to 4 complete turns, each with 2 to 3 answer variations,
     * integrating the real initial message and real starter phrases from D1.
     */
    fun getDialogueForScenario(
        scenario: ScenarioEntity?,
        cefrLevel: String,
        starterPhrases: List<StarterPhraseEntity> = emptyList()
    ): List<DialogueTurn> {
        if (scenario == null) return emptyList()

        val level = cefrLevel.uppercase().trim().take(2).ifBlank { "A1" }
        val persona = scenario.ai_persona.ifBlank { "طرف الحوار" }
        val initialMsg = scenario.getInitialMessageForLevel(level).ifBlank { "Guten Tag! Wie kann ich Ihnen helfen?" }

        // Find matching starter phrases for this level, falling back to all available
        val levelStarters = starterPhrases.filter { it.level.equals(level, ignoreCase = true) }.ifEmpty { starterPhrases }
        val primaryStarter = levelStarters.firstOrNull()

        return when (scenario.id) {
            "cafe_order" -> buildCafeOrderDialogue(persona, level, initialMsg, levelStarters)
            "embassy_appointment" -> buildEmbassyDialogue(persona, level, initialMsg, primaryStarter)
            "job_interview" -> buildJobInterviewDialogue(persona, level, initialMsg, primaryStarter)
            "doctor_visit" -> buildDoctorVisitDialogue(persona, level, initialMsg, primaryStarter)
            "apartment_viewing" -> buildApartmentDialogue(persona, level, initialMsg, primaryStarter)
            else -> buildGenericDialogue(scenario, persona, level, initialMsg, levelStarters)
        }
    }

    private fun buildCafeOrderDialogue(
        persona: String,
        level: String,
        initialMsg: String,
        starters: List<StarterPhraseEntity>
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()
        val primaryStarter = starters.firstOrNull()

        // Turn 1: Initial Order
        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "cafe_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else if (level in listOf("B1", "B2")) "Ja gerne! Ich hätte bitte einen Cappuccino mit Hafermilch." else "Ja gerne! Ich möchte bitte einen Kaffee.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else if (level in listOf("B1", "B2")) "نعم بكل سرور! أود كابتشينو بحليب الشوفان من فضلك." else "نعم بكل سرور! أريد قهوة من فضلك.",
                badge = "طلب إيجابي"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "cafe_1_2",
                german = if (starters.size > 1 && starters[1].german.isNotBlank()) starters[1].german else if (level in listOf("B1", "B2")) "Nein danke, im Moment noch nicht. Ich brauche noch einen kurzen Augenblick." else "Nein danke, noch nicht. Ich brauche noch einen Moment.",
                translationAr = if (starters.size > 1 && starters[1].translation_ar.isNotBlank()) starters[1].translation_ar else if (level in listOf("B1", "B2")) "لا شكراً، ليس في هذه اللحظة. أحتاج إلى برهة قصيرة." else "لا شكراً، ليس الآن بعد. أحتاج إلى دقيقة واحدة.",
                badge = "طلب مهلة"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "cafe_1_3",
                german = if (starters.size > 2 && starters[2].german.isNotBlank()) starters[2].german else if (level in listOf("B1", "B2")) "Haben Sie anstelle von Kaffee auch frischen Ingwertee oder Saft?" else "Haben Sie stattdessen auch schwarzen Tee oder Saft?",
                translationAr = if (starters.size > 2 && starters[2].translation_ar.isNotBlank()) starters[2].translation_ar else if (level in listOf("B1", "B2")) "هل لديكم بدلاً من القهوة شاي بالزنجبيل الطازج أو عصير؟" else "هل لديكم بدلاً من ذلك شاي أسود أو عصير؟",
                badge = "خيار بديل"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 4,
                phaseTitleAr = "التحية والطلب الأول",
                partnerGerman = initialMsg,
                partnerArabic = when (level) {
                    "A2" -> "مرحباً! ماذا يمكنني أن أحضر لك اليوم؟"
                    "B1" -> "طاب يومك، هل اخترت أم تحتاج لحظة إضافية؟"
                    "B2" -> "طاب يومك، هل تسمح لي باقتراح توصياتنا لليوم قبل أن تقرر؟"
                    else -> "مرحباً! ماذا ترغب أن تشرب؟"
                },
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        // Turn 2: Size & Details
        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 4,
                phaseTitleAr = "تحديد الإضافات والحجم",
                partnerGerman = if (level in listOf("B1", "B2")) "Sehr gerne. Möchten Sie Milch und Zucker dazu, oder trinken Sie ihn schwarz?" else "Sehr gerne. Möchten Sie Milch und Zucker dazu?",
                partnerArabic = if (level in listOf("B1", "B2")) "بكل سرور. هل ترغب بحليب وسكر معه، أم تفضله سادة؟" else "بكل سرور. هل ترغب بحليب وسكر مع القهوة؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "cafe_2_1",
                        german = if (level in listOf("B1", "B2")) "Ja bitte, mit etwas warmer Milch und einem Löffel Zucker." else "Ja bitte, mit Milch und Zucker.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم من فضلك، مع حليب دافئ وملعقة سكر." else "نعم من فضلك، مع حليب وسكر.",
                        badge = "بالإيجاب (نعم / مع إضافات)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_2_2",
                        german = if (level in listOf("B1", "B2")) "Nein danke, ich trinke ihn grundsätzlich schwarz und ungesüßt." else "Nein danke, bitte schwarz und ohne Zucker.",
                        translationAr = if (level in listOf("B1", "B2")) "لا شكراً، أشربها عادة سادة تماماً وبدون سكر." else "لا شكراً، من فضلك سادة وبدون سكر.",
                        badge = "بالنفي (لا / بدون إضافات)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_2_3",
                        german = if (level in listOf("B1", "B2")) "Hätten Sie stattdessen Hafermilch? Und bitte im Becher zum Mitnehmen." else "Haben Sie Hafermilch? Und bitte zum Mitnehmen.",
                        translationAr = if (level in listOf("B1", "B2")) "هل لديكم بدلاً من ذلك حليب شوفان؟ ومن فضلك في كوب سفري (to-go)." else "هل لديكم حليب شوفان؟ ومن فضلك سفري.",
                        badge = "خيار بديل (حليب نباتي / سفري)"
                    )
                )
            )
        )

        // Turn 3: Snacks / Pastries
        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 4,
                phaseTitleAr = "طلب وجبة خفيفة أو حلوى",
                partnerGerman = if (level in listOf("B1", "B2")) "Kommt sofort. Möchten Sie vielleicht noch ein frisches Stück Kuchen oder ein Gebäck dazu?" else "Möchten Sie auch etwas essen? Ein Stück Kuchen vielleicht?",
                partnerArabic = if (level in listOf("B1", "B2")) "سيصل فوراً. هل ترغب ربما بقطعة كعك طازجة أو معجنات معها؟" else "هل ترغب في تناول شيء أيضاً؟ قطعة كعك مثلاً؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "cafe_3_1",
                        german = if (level in listOf("B1", "B2")) "Ja sehr gerne, das frische Buttercroissant klingt hervorragend." else "Ja gerne, ein Stück Käsekuchen bitte.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم بكل سرور، كرواسون الزبدة الطازج يبدو رائعاً." else "نعم بكل سرور، قطعة تشيزكيك من فضلك.",
                        badge = "بالإيجاب (نعم / إضافة حلوى)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_3_2",
                        german = if (level in listOf("B1", "B2")) "Vielen Dank, aber ich bleibe heute ausschließlich beim Getränk." else "Nein danke, ich möchte nur den Kaffee trinken.",
                        translationAr = if (level in listOf("B1", "B2")) "شكراً جزيلاً، لكني سأكتفي بالمشروب فقط اليوم." else "لا شكراً، أريد فقط شرب القهوة.",
                        badge = "بالنفي (لا / اكتفاء بالمشروب)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_3_3",
                        german = if (level in listOf("B1", "B2")) "Haben Sie neben Süßem auch herzhafte Kleinigkeiten wie ein Sandwich?" else "Haben Sie auch etwas Herzhaftes, wie ein Sandwich?",
                        translationAr = if (level in listOf("B1", "B2")) "هل لديكم إلى جانب الحلويات وجبات خفيفة مالحة كشطيرة مثلاً؟" else "هل لديكم شيء مالح، مثل ساندوتش؟",
                        badge = "خيار بديل (سناك مالح)"
                    )
                )
            )
        )

        // Turn 4: Bill and Payment
        turns.add(
            DialogueTurn(
                turnNumber = 4,
                totalTurns = 4,
                phaseTitleAr = "الحساب والدفع والوداع",
                partnerGerman = if (level in listOf("B1", "B2")) "Hier ist Ihre Bestellung. Das macht zusammen 4 Euro 50. Zahlen Sie mit Karte?" else "Das macht zusammen 3 Euro 80, bitte. Zahlen Sie mit Karte?",
                partnerArabic = if (level in listOf("B1", "B2")) "تفضل طلبك. الحساب الإجمالي 4 يورو و 50 سنتاً. هل تدفع بالبطاقة؟" else "الحساب الإجمالي 3 يورو و 80 سنتاً. هل تدفع بالبطاقة؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "cafe_4_1",
                        german = if (level in listOf("B1", "B2")) "Ja, ich zahle sehr gerne kontaktlos mit Karte." else "Ja gerne, mit Karte bitte.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، سأدفع بالبطاقة بدون تلامس بكل سرور." else "نعم بكل سرور، بالبطاقة من فضلك.",
                        badge = "بالإيجاب (نعم / بالبطاقة)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_4_2",
                        german = if (level in listOf("B1", "B2")) "Nein, lieber bar. Hier sind 5 Euro, behalten Sie den Rest bitte!" else "Nein, bar bitte. Hier sind 5 Euro, stimmt so!",
                        translationAr = if (level in listOf("B1", "B2")) "لا، أفضل نقداً. تفضل 5 يورو، واحتفظ بالباقي من فضلك!" else "لا، نقداً من فضلك. تفضل 5 يورو، الباقي لك!",
                        badge = "بالنفي (لا / نقداً)"
                    ),
                    DialogueAnswerOption(
                        id = "cafe_4_3",
                        german = if (level in listOf("B1", "B2")) "Könnten wir bitte getrennt abrechnen? Und ich brauche eine Quittung." else "Können wir getrennt zahlen? Und eine Quittung bitte.",
                        translationAr = if (level in listOf("B1", "B2")) "هل يمكننا الحساب بشكل منفصل؟ وأحتاج إلى إيصال من فضلك." else "هل يمكننا الدفع منفصلين؟ وإيصال من فضلك.",
                        badge = "خيار بديل (حساب منفصل / إيصال)"
                    )
                )
            )
        )

        return turns
    }

    private fun buildEmbassyDialogue(
        persona: String,
        level: String,
        initialMsg: String,
        primaryStarter: StarterPhraseEntity?
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()

        // Turn 1: Reason for visit
        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "emb_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else if (level in listOf("B1", "B2")) "Ja, ich habe heute um 10 Uhr einen bestätigten Termin bezüglich meines Visumantrags." else "Ja, ich habe heute einen Termin für mein Visum.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else if (level in listOf("B1", "B2")) "نعم، لدي موعد مؤكد اليوم بخصوص طلب التأشيرة." else "نعم، لدي موعد اليوم من أجل التأشيرة.",
                badge = "بالإيجاب (نعم / تأكيد الموعد)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "emb_1_2",
                german = if (level in listOf("B1", "B2")) "Nein, leider konnte ich online keinen freien Termin buchen. Gibt es eine Notfallsprechstunde?" else "Nein, leider habe ich keinen Termin. Kann ich trotzdem kurz warten?",
                translationAr = if (level in listOf("B1", "B2")) "لا، للأسف لم أتمكن من حجز موعد شاغر عبر الإنترنت. هل توجد مراجعات طارئة؟" else "لا، للأسف ليس لدي موعد. هل يمكنني الانتظار قليلاً؟",
                badge = "بالنفي (لا / بدون موعد)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "emb_1_3",
                german = if (level in listOf("B1", "B2")) "Ich bin nur hier, um eine nachgeforderte Unterlage für meine Akte persönlich abzugeben." else "Ich möchte nur ein fehlendes Dokument für meine Akte abgeben.",
                translationAr = if (level in listOf("B1", "B2")) "أنا هنا فقط لتسليم مستند إضافي مطلوب لملفي شخصياً." else "أود فقط تسليم وثيقة ناقصة لملفي.",
                badge = "خيار بديل (تسليم أوراق)"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 4,
                phaseTitleAr = "الاستقبال وموعد الحضور",
                partnerGerman = initialMsg,
                partnerArabic = when (level) {
                    "A2" -> "طاب يومك. هل لديك موعد مسبق اليوم؟"
                    "B1" -> "طاب يومك، تفضل بالجلوس. هل لديك موعد مسجل اليوم لطلب التأشيرة؟"
                    "B2" -> "طاب يومك، أهلاً بك. هل لديك موعد محجوز في جدول المواعيد لليوم؟"
                    else -> "طاب يومك. هل لديك موعد اليوم؟"
                },
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        // Turn 2: Documents presentation
        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 4,
                phaseTitleAr = "تقديم الوثائق والمستندات",
                partnerGerman = if (level in listOf("B1", "B2")) "Sehr gut. Haben Sie alle erforderlichen Originale sowie die Antragsformulare vollständig dabei?" else "Haben Sie alle Dokumente und das Formular dabei?",
                partnerArabic = if (level in listOf("B1", "B2")) "ممتاز. هل لديك جميع الوثائق الأصلية المطلوبة واستمارات الطلب كاملة؟" else "هل لديك جميع الوثائق واستمارة الطلب كاملة؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "emb_2_1",
                        german = if (level in listOf("B1", "B2")) "Ja, hier sind mein Reisepass, das ausgefüllte Formular und alle geforderten Nachweise." else "Ja, hier sind mein Reisepass und alle Formulare komplett.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، تفضل جواز سفري والاستمارة المعبأة وكافة الإثباتات المطلوبة." else "نعم، تفضل جواز سفري وجميع الاستمارات مكتملة.",
                        badge = "بالإيجاب (نعم / أوراق كاملة)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_2_2",
                        german = if (level in listOf("B1", "B2")) "Nein, leider fehlt mir noch die offizielle Bestätigung der Krankenversicherung." else "Nein, die Versicherungsbestätigung fehlt mir leider noch.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، للأسف ما زال ينقصني الإثبات الرسمي للتأمين الصحي." else "لا، للأسف ينقصني إثبات التأمين.",
                        badge = "بالنفي (لا / مستند ناقص)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_2_3",
                        german = if (level in listOf("B1", "B2")) "Wäre es möglich, die fehlende Bescheinigung heute Nachmittag digital per E-Mail nachzureichen?" else "Kann ich das fehlende Dokument heute per E-Mail nachreichen?",
                        translationAr = if (level in listOf("B1", "B2")) "هل من الممكن إرسال الإثبات الناقص رقمياً عبر البريد الإلكتروني بعد ظهر اليوم؟" else "هل يمكنني إرسال الوثيقة الناقصة بالإيميل اليوم؟",
                        badge = "خيار بديل (إرسال إلكتروني)"
                    )
                )
            )
        )

        // Turn 3: Purpose of travel / Travel history
        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 4,
                phaseTitleAr = "السفر السابق والغرض",
                partnerGerman = if (level in listOf("B1", "B2")) "Waren Sie in den letzten Jahren schon einmal in Deutschland oder im Schengen-Raum?" else "Waren Sie schon einmal früher in Deutschland?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل زرت ألمانيا أو منطقة شنغن في السنوات الأخيرة؟" else "هل زرت ألمانيا من قبل؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "emb_3_1",
                        german = if (level in listOf("B1", "B2")) "Ja, ich war vor zwei Jahren schon einmal für einen Sprachkurs in Deutschland." else "Ja, ich war vor zwei Jahren schon einmal in Deutschland.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، كنت في ألمانيا قبل عامين من أجل دورة لغة." else "نعم، زرت ألمانيا قبل سنتين.",
                        badge = "بالإيجاب (نعم / زيارة سابقة)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_3_2",
                        german = if (level in listOf("B1", "B2")) "Nein, das ist meine allererste Reise nach Europa und in den Schengen-Raum." else "Nein, das ist meine allererste Reise nach Deutschland.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، هذه أول رحلة لي إلى أوروبا ومنطقة شنغن على الإطلاق." else "لا، هذه أول زيارة لي إلى ألمانيا.",
                        badge = "بالنفي (لا / أول زيارة)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_3_3",
                        german = if (level in listOf("B1", "B2")) "In Deutschland direkt noch nicht, allerdings habe ich früher die Schweiz besucht." else "In Deutschland nicht, aber ich war einmal in Österreich.",
                        translationAr = if (level in listOf("B1", "B2")) "في ألمانيا مباشرة لا، لكني زرت سويسرا سابقاً." else "في ألمانيا لا، لكنني زرت النمسا مرة واحدة.",
                        badge = "خيار بديل (دولة أخرى)"
                    )
                )
            )
        )

        // Turn 4: Receipt & Pickup
        turns.add(
            DialogueTurn(
                turnNumber = 4,
                totalTurns = 4,
                phaseTitleAr = "استلام الإيصال وطريقة الاستلام",
                partnerGerman = if (level in listOf("B1", "B2")) "Alles ist erfasst. Die Bearbeitung dauert etwa zwei Wochen. Möchten Sie den Pass persönlich abholen?" else "Die Bearbeitung dauert zwei Wochen. Möchten Sie den Pass persönlich abholen?",
                partnerArabic = if (level in listOf("B1", "B2")) "تم تسجيل كل شيء. تستغرق المعالجة أسبوعين تقريباً. هل ترغب باستلام الجواز شخصياً؟" else "تستغرق المعالجة أسبوعين. هل ترغب باستلام الجواز شخصياً هنا؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "emb_4_1",
                        german = if (level in listOf("B1", "B2")) "Ja sehr gerne, ich hole den Pass persönlich hier in der Botschaft ab." else "Ja gerne, ich hole den Pass persönlich ab.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم بكل سرور، سأستلم الجواز شخصياً هنا في السفارة." else "نعم بكل سرور، سأستلم الجواز شخصياً.",
                        badge = "بالإيجاب (نعم / استلام شخصي)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_4_2",
                        german = if (level in listOf("B1", "B2")) "Nein, ich wohne sehr weit weg. Wäre eine Zusendung per Post oder Kurier möglich?" else "Nein, ich wohne weit weg. Bitte schicken Sie ihn per Post.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، أسكن في مكان بعيد جداً. هل يمكن إرساله بالبريد المسجل أو التوصيل السريع؟" else "لا، أسكن بعيداً. من فضلك أرسلوه بالبريد.",
                        badge = "بالنفي (لا / إرسال بالبريد)"
                    ),
                    DialogueAnswerOption(
                        id = "emb_4_3",
                        german = if (level in listOf("B1", "B2")) "Kann stattdessen eine bevollmächtigte Person mit schriftlicher Vollmacht den Pass abholen?" else "Kann ein Freund mit einer Vollmacht den Pass abholen?",
                        translationAr = if (level in listOf("B1", "B2")) "هل يمكن لشخص موكل يحمل تفويضاً خطياً استلام الجواز نيابة عني؟" else "هل يمكن لصديق يحمل توكيلاً استلام الجواز؟",
                        badge = "خيار بديل (توكيل شخص)"
                    )
                )
            )
        )

        return turns
    }

    private fun buildJobInterviewDialogue(
        persona: String,
        level: String,
        initialMsg: String,
        primaryStarter: StarterPhraseEntity?
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()

        // Turn 1: Introduction & Arrival
        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "job_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else if (level in listOf("B1", "B2")) "Ja, vielen Dank! Dank Ihrer genauen Wegbeschreibung habe ich das Büro sofort gefunden." else "Ja, vielen Dank! Die Wegbeschreibung war sehr einfach.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else if (level in listOf("B1", "B2")) "نعم شكراً جزيلاً! بفضل الوصف الدقيق للطريق وجدت المكتب مباشرة." else "نعم شكراً جزيلاً! كان وصف الطريق سهلاً جداً.",
                badge = "بالإيجاب (نعم / وصول سهل)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "job_1_2",
                german = if (level in listOf("B1", "B2")) "Nein, leider hatte die Bahn etwas Verspätung, aber ich bin zum Glück pünktlich angekommen." else "Nein, der Zug hatte etwas Verspätung, aber ich bin pünktlich hier.",
                translationAr = if (level in listOf("B1", "B2")) "لا، للأسف تأخر القطار قليلاً، لكن لحسن الحظ وصلت في الوقت المحدد." else "لا، تأخر القطار قليلاً، لكني وصلت في الوقت المحدد.",
                badge = "بالنفي (لا / صعوبة مواصلات)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "job_1_3",
                german = if (level in listOf("B1", "B2")) "Ich kenne diese Gegend bereits sehr gut, da ich früher in der Nachbarschaft gearbeitet habe." else "Ich kenne die Gegend schon gut, ich habe hier in der Nähe gewohnt.",
                translationAr = if (level in listOf("B1", "B2")) "أعرف هذه المنطقة جيداً بالفعل، لأنني عملت سابقاً في هذا الحي." else "أعرف المنطقة جيداً، فقد سكنت قريباً من هنا.",
                badge = "خيار بديل (معرفة سابقة)"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 4,
                phaseTitleAr = "الترحيب والوصول للمقابلة",
                partnerGerman = initialMsg,
                partnerArabic = when (level) {
                    "A2" -> "طاب يومك! هل تمكنت من الوصول لمقرنا بسهولة؟"
                    "B1" -> "طاب يومك، أهلاً بك. هل كانت الطريق إلى مقر شركتنا سهلة ولم تواجه صعوبة؟"
                    "B2" -> "طاب يومك، يسعدنا لقاؤك. هل استطعت العثور على مبنى شركتنا بدون عناء؟"
                    else -> "مرحباً! هل وجدت المكان بسهولة؟"
                },
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        // Turn 2: Experience & Qualifications
        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 4,
                phaseTitleAr = "الخبرة المهنية في المجال",
                partnerGerman = if (level in listOf("B1", "B2")) "Haben Sie bereits praktische Berufserfahrung in genau diesem Aufgabengebiet gesammelt?" else "Haben Sie schon Erfahrung in diesem Bereich?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل لديك خبرة عملية سابقة في هذا المجال من المهام تحديداً؟" else "هل لديك بالفعل خبرة سابقة في هذا العمل؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "job_2_1",
                        german = if (level in listOf("B1", "B2")) "Ja, ich bringe über drei Jahre fundierte Berufserfahrung in genau diesem Bereich mit." else "Ja, ich habe schon zwei Jahre in diesem Bereich gearbeitet.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، أمتلك أكثر من ثلاث سنوات من الخبرة المهنية المتينة في هذا المجال." else "نعم، عملت لمدة سنتين في هذا العمل.",
                        badge = "بالإيجاب (نعم / خبرة متوفرة)"
                    ),
                    DialogueAnswerOption(
                        id = "job_2_2",
                        german = if (level in listOf("B1", "B2")) "Nein, in dieser speziellen Branche noch nicht, aber ich eigne mir neues Fachwissen extrem schnell an." else "Nein, in diesem Bereich noch nicht, aber ich lerne sehr schnell.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، ليس في هذا القطاع التخصصي بعد، لكني أكتسب المعرفة الجديدة بسرعة فائقة." else "لا، ليس في هذا المجال بعد، لكني أتعلم بسرعة.",
                        badge = "بالنفي (لا / بدون خبرة مباشرة)"
                    ),
                    DialogueAnswerOption(
                        id = "job_2_3",
                        german = if (level in listOf("B1", "B2")) "Nicht direkt in diesem Bereich, jedoch lassen sich meine bisherigen Qualifikationen nahtlos übertragen." else "Ich habe ähnliche Aufgaben in meinem alten Beruf gemacht.",
                        translationAr = if (level in listOf("B1", "B2")) "ليس في هذا المجال مباشرة، لكن مهاراتي السابقة يمكن تطبيقها والاستفادة منها فوراً." else "قمت بمهام مشابهة في وظيفتي السابقة.",
                        badge = "خيار بديل (مهارات قابلة للنقل)"
                    )
                )
            )
        )

        // Turn 3: Weekend / Shift Flexibility
        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 4,
                phaseTitleAr = "المرونة وأوقات العمل",
                partnerGerman = if (level in listOf("B1", "B2")) "Wären Sie bei Bedarf bereit, an Wochenenden zu arbeiten oder Schichtdienst zu übernehmen?" else "Können Sie bei Bedarf auch am Wochenende arbeiten?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل أنت مستعد عند الحاجة للعمل في عطلة نهاية الأسبوع أو بنظام الورديات؟" else "هل يمكنك عند الضرورة العمل في نهاية الأسبوع؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "job_3_1",
                        german = if (level in listOf("B1", "B2")) "Ja, absolut. Ich bin zeitlich sehr flexibel und Wochenendarbeit gewohnt." else "Ja, das ist kein Problem für mich. Ich bin sehr flexibel.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، بالتأكيد. لدي مرونة تامة في الوقت واعتدت على العمل في عطلات نهاية الأسبوع." else "نعم، هذه ليست مشكلة بالنسبة لي. أنا مرن جداً.",
                        badge = "بالإيجاب (نعم / مرونة تامة)"
                    ),
                    DialogueAnswerOption(
                        id = "job_3_2",
                        german = if (level in listOf("B1", "B2")) "Nein, an Wochenenden ist es mir aus familiären Gründen leider nicht möglich, wochentags jedoch gerne." else "Nein, am Wochenende kann ich leider nicht, aber unter der Woche gerne.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، في عطلة نهاية الأسبوع لا يناسبني لظروف عائلية، لكن خلال أيام الأسبوع متاح كلياً." else "لا، في الويكند لا أستطيع، لكن خلال الأسبوع متاح تماماً.",
                        badge = "بالنفي (لا / أيام الأسبوع فقط)"
                    ),
                    DialogueAnswerOption(
                        id = "job_3_3",
                        german = if (level in listOf("B1", "B2")) "Wäre es stattdessen denkbar, flexible Arbeitszeiten mit teilweisem Homeoffice zu vereinbaren?" else "Kann man auch einen Tag pro Woche von zu Hause arbeiten?",
                        translationAr = if (level in listOf("B1", "B2")) "هل يمكن بدلاً من ذلك الاتفاق على ساعات عمل مرنة مع عمل جزئي من المنزل؟" else "هل يمكن العمل يوماً في الأسبوع من البيت؟",
                        badge = "خيار بديل (عمل عن بعد / مرونة)"
                    )
                )
            )
        )

        // Turn 4: Questions for company & Closing
        turns.add(
            DialogueTurn(
                turnNumber = 4,
                totalTurns = 4,
                phaseTitleAr = "طرح الأسئلة والختام",
                partnerGerman = if (level in listOf("B1", "B2")) "Vielen Dank für die Einblicke. Haben Sie an dieser Stelle noch offene Fragen an mich oder das Team?" else "Haben Sie noch offene Fragen an uns?",
                partnerArabic = if (level in listOf("B1", "B2")) "شكراً جزيلاً على هذه الإيضاحات. هل لديك أي أسئلة أخرى موجهة لي أو للفريق؟" else "هل لديك أي أسئلة أخرى لنا؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "job_4_1",
                        german = if (level in listOf("B1", "B2")) "Ja, mich würde sehr interessieren, wie die Einarbeitungsphase im Team konkret gestaltet ist." else "Ja, wie groß ist das Team, in dem ich arbeiten würde?",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، يهمني جداً معرفة كيف يتم تصميم وتنظيم مرحلة التدريب والتأهيل في الفريق." else "نعم، كم يبلغ عدد أفراد الفريق الذي سأعمل معه؟",
                        badge = "بالإيجاب (نعم / سؤال عن الفريق)"
                    ),
                    DialogueAnswerOption(
                        id = "job_4_2",
                        german = if (level in listOf("B1", "B2")) "Nein, vielen Dank. Sie haben im Gespräch alle wichtigen Punkte bereits ausführlich beantwortet." else "Nein danke, Sie haben mir schon alle Punkte gut erklärt.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، شكراً جزيلاً. لقد أجبت خلال حديثنا عن كافة الأسئلة المهمة باستفاضة." else "لا شكراً، لقد وضحت لي كل النقاط بشكل كافٍ.",
                        badge = "بالنفي (لا / كل شيء واضح)"
                    ),
                    DialogueAnswerOption(
                        id = "job_4_3",
                        german = if (level in listOf("B1", "B2")) "Bis wann planen Sie ungefähr, eine endgültige Entscheidung bezüglich der Stelle zu treffen?" else "Wann kann ich mit einer Rückmeldung von Ihnen rechnen?",
                        translationAr = if (level in listOf("B1", "B2")) "متى تخططون تقريباً لاتخاذ القرار النهائي بشأن شغل هذا المنصب؟" else "متى يمكنني أن أتوقع رداً من طرفكم؟",
                        badge = "خيار بديل (موعد الرد والقرار)"
                    )
                )
            )
        )

        return turns
    }

    private fun buildDoctorVisitDialogue(
        persona: String,
        level: String,
        initialMsg: String,
        primaryStarter: StarterPhraseEntity?
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()

        // Turn 1: Symptoms & Pain
        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "doc_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else if (level in listOf("B1", "B2")) "Ja, ich leide seit gestern unter sehr starken Kopf- und Halsschmerzen." else "Ja, ich habe seit gestern starke Kopfschmerzen und Halsschmerzen.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else if (level in listOf("B1", "B2")) "نعم، أعاني منذ الأمس من صداع شديد وألم في الحلق." else "نعم، أعاني من صداع وألم في الحلق.",
                badge = "بالإيجاب (نعم / ألم حاد)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "doc_1_2",
                german = if (level in listOf("B1", "B2")) "Nein, akute Schmerzen habe ich nicht, aber eine extreme Schwäche und trockenen Husten." else "Nein, keine starken Schmerzen, aber ich fühle mich sehr schwach und habe Husten.",
                translationAr = if (level in listOf("B1", "B2")) "لا، ليس لدي ألم حاد، لكني أشعر بإنهاك شديد وسعال جاف." else "لا، ليس لدي ألم قوي، لكني أشعر بضعف وسعال.",
                badge = "بالنفي (لا / بدون ألم حاد)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "doc_1_3",
                german = if (level in listOf("B1", "B2")) "Ich benötige primär eine Folgeverordnung für meine Medikamente und ein ärztliches Attest." else "Ich brauche eigentlich nur ein Rezept und eine kurze Untersuchung.",
                translationAr = if (level in listOf("B1", "B2")) "أحتاج بالدرجة الأولى إلى تجديد وصفة أدوية وفحص طبي سريع." else "أحتاج فقط لوصفة دواء وفحص سريع.",
                badge = "خيار بديل (فحص روتيني / وصفة)"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 4,
                phaseTitleAr = "وصف الشكوى الصحية والألم",
                partnerGerman = initialMsg,
                partnerArabic = when (level) {
                    "A2" -> "طاب يومك. هل تشعر بآلام حادة أو شديدة؟"
                    "B1" -> "طاب يومك، تفضل بالجلوس. هل تعاني من آلام حادة أو مفاجئة في جسمك؟"
                    "B2" -> "طاب يومك، أهلاً بك. هل تشعر بآلام حادة مستمرة تتطلب تدخلاً فورياً؟"
                    else -> "طاب يومك. هل لديك ألم شديد؟"
                },
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        // Turn 2: Temperature & Fever
        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 4,
                phaseTitleAr = "قياس درجة الحرارة والحمى",
                partnerGerman = if (level in listOf("B1", "B2")) "Haben Sie zu Hause Ihre Körpertemperatur mit einem Thermometer gemessen?" else "Haben Sie zu Hause Fieber gemessen?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل قست درجة حرارة جسمك في المنزل بميزان الحرارة؟" else "هل قست الحرارة في البيت؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "doc_2_1",
                        german = if (level in listOf("B1", "B2")) "Ja, das Thermometer zeigte heute früh 38,8 Grad an." else "Ja, ich hatte heute Morgen 38,5 Grad Fieber.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، أظهر ميزان الحرارة صباح اليوم 38.8 درجة." else "نعم، كانت حرارتي صباح اليوم 38.5 درجة.",
                        badge = "بالإيجاب (نعم / حرارة مرتفعة)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_2_2",
                        german = if (level in listOf("B1", "B2")) "Nein, meine Temperatur war normal, allerdings habe ich ständige Schüttelfrost-Attacken." else "Nein, ich habe kein Fieber, mir ist nur schwindelig.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، كانت حرارتي طبيعية، غير أنني أعاني من نوبات قشعريرة متواصلة." else "لا، ليس لدي حرارة، أشعر فقط بالدوار.",
                        badge = "بالنفي (لا / حرارة طبيعية)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_2_3",
                        german = if (level in listOf("B1", "B2")) "Ich hatte leider kein Fieberthermometer zur Hand, aber mein Körper fühlt sich glühend heiß an." else "Ich habe noch nicht gemessen, aber ich fühle mich sehr heiß.",
                        translationAr = if (level in listOf("B1", "B2")) "لم يتوفر لدي مقياس حرارة للأسف، لكن جسمي يبدو ملتهباً وساخناً للغاية." else "لم أقس بعد، لكني أشعر بحرارة وسخونة في جسمي.",
                        badge = "خيار بديل (لم أتمكن من القياس)"
                    )
                )
            )
        )

        // Turn 3: Allergies against medication
        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 4,
                phaseTitleAr = "التحسس من الأدوية",
                partnerGerman = if (level in listOf("B1", "B2")) "Haben Sie bekannte Allergien oder Unverträglichkeiten gegen bestimmte Medikamente wie Penicillin?" else "Haben Sie Allergien gegen Medikamente wie Penicillin?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل لديك أي حساسية معروفة أو عدم تحمل لأدوية معينة كالبنسلين؟" else "هل لديك حساسية من أدوية مثل البنسلين؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "doc_3_1",
                        german = if (level in listOf("B1", "B2")) "Ja, ich reagiere nachweislich allergisch auf Penicillin und Penicillin-Derivate." else "Ja, ich habe eine starke Allergie gegen Penicillin.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، أعاني من حساسية مؤكدة تجاه البنسلين ومشتقاته." else "نعم، لدي حساسية شديدة من البنسلين.",
                        badge = "بالإيجاب (نعم / وجود حساسية)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_3_2",
                        german = if (level in listOf("B1", "B2")) "Nein, mir sind keinerlei Allergien oder Unverträglichkeiten gegen Medikamente bekannt." else "Nein, mir sind keine Allergien bekannt.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، لا علم لي بأي حساسية أو عدم تحمل لأي دواء على الإطلاق." else "لا، ليس لدي أي حساسية لأي دواء.",
                        badge = "بالنفي (لا / لا حساسية)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_3_3",
                        german = if (level in listOf("B1", "B2")) "Keine echte Allergie, aber mein Magen reagiert empfindlich auf Schmerzmittel wie Ibuprofen." else "Ich habe nur manchmal Magenschmerzen von starken Tabletten.",
                        translationAr = if (level in listOf("B1", "B2")) "ليست حساسية حقيقية، لكن معدتي حساسة تجاه المسكنات القوية مثل الإيبوبروفين." else "فقط معدتي تؤلمني أحياناً من الحبوب القوية.",
                        badge = "خيار بديل (حساسية معدة خفيفة)"
                    )
                )
            )
        )

        // Turn 4: Sick note / Prescription
        turns.add(
            DialogueTurn(
                turnNumber = 4,
                totalTurns = 4,
                phaseTitleAr = "الإجازة المرضية والتقرير الطبي",
                partnerGerman = if (level in listOf("B1", "B2")) "Ich schreibe Sie für drei Tage krank. Benötigen Sie eine Arbeitsunfähigkeitsbescheinigung für Ihren Arbeitgeber?" else "Ich schreibe Sie für drei Tage krank. Brauchen Sie eine Krankschreibung für den Arbeitgeber?",
                partnerArabic = if (level in listOf("B1", "B2")) "سأمنحك إجازة مرضية لثلاثة أيام. هل تحتاج لشهادة إجازة مرضية لجهة عملك؟" else "سأكتب لك إجازة لثلاثة أيام. هل تحتاج لتقرير مرضي لعملك؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "doc_4_1",
                        german = if (level in listOf("B1", "B2")) "Ja bitte, mein Arbeitgeber verlangt ab dem ersten Krankheitstag ein ärztliches Attest." else "Ja bitte, ich brauche die Krankschreibung für meinen Chef.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم من فضلك، جهة عملي تطلب تقريراً طبياً منذ اليوم الأول للمرض." else "نعم من فضلك، أحتاج التقرير المرضي لمديري في العمل.",
                        badge = "بالإيجاب (نعم / طلب إجازة)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_4_2",
                        german = if (level in listOf("B1", "B2")) "Nein danke, ich arbeite derzeit freiberuflich von zu Hause und benötige keine Bescheinigung." else "Nein danke, ich habe diese Woche Urlaub und brauche kein Attest.",
                        translationAr = if (level in listOf("B1", "B2")) "لا شكراً، أعمل حالياً حراً من المنزل ولست بحاجة لشهادة طبية." else "لا شكراً، أنا في إجازة هذا الأسبوع ولا أحتاج لتقرير.",
                        badge = "بالنفي (لا / لست بحاجة)"
                    ),
                    DialogueAnswerOption(
                        id = "doc_4_3",
                        german = if (level in listOf("B1", "B2")) "Wird die elektronische Arbeitsunfähigkeitsbescheinigung (eAU) direkt digital an die Krankenkasse übermittelt?" else "Wird das Attest automatisch digital an die Kasse geschickt?",
                        translationAr = if (level in listOf("B1", "B2")) "هل يتم نقل الإجازة المرضية الإلكترونية (eAU) رقمياً ومباشرة إلى التأمين الصحي؟" else "هل يُرسل التقرير إلكترونياً للتأمين؟",
                        badge = "خيار بديل (إرسال رقمي eAU)"
                    )
                )
            )
        )

        return turns
    }

    private fun buildApartmentDialogue(
        persona: String,
        level: String,
        initialMsg: String,
        primaryStarter: StarterPhraseEntity?
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()

        // Turn 1: Viewing entrance & Impression
        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "apt_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else if (level in listOf("B1", "B2")) "Ja, absolut! Die Wohnung wirkt wunderbar hell und der Schnitt gefällt mir außerordentlich gut." else "Ja, sehr! Die Wohnung ist wirklich sehr schön und hell.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else if (level in listOf("B1", "B2")) "نعم بكل تأكيد! الشقة تبدو مضيئة جداً وتوزيع الغرف أعجبني للغاية." else "نعم جداً! الشقة جميلة ومضيئة للغاية.",
                badge = "بالإيجاب (نعم / إعجاب كبير)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "apt_1_2",
                german = if (level in listOf("B1", "B2")) "Ehrlich gesagt wirkt das Wohnzimmer etwas kleiner und dunkler als auf den Fotos im Inserat." else "Nein, leider ist das Zimmer etwas zu klein für mich.",
                translationAr = if (level in listOf("B1", "B2")) "بصراحة تبدو غرفة المعيشة أصغر وأكثر عتمة مقارنة بالصور في الإعلان." else "لا، للأسف الغرفة صغيرة بعض الشيء بالنسبة لي.",
                badge = "بالنفي (لا / صغيرة أو مظلمة)"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "apt_1_3",
                german = if (level in listOf("B1", "B2")) "Die Raumaufteilung ist gut, aber wie sieht es mit dem Verkehrslärm zur Straßenseite hin aus?" else "Die Wohnung ist schön, aber ist es nachts laut hier?",
                translationAr = if (level in listOf("B1", "B2")) "توزيع الغرف جيد، لكن كيف هو وضع ضجيج المرور من جهة الشارع؟" else "الشقة جميلة، لكن هل يوجد ضجيج هنا ليلاً؟",
                badge = "خيار بديل (استفسار عن الهدوء)"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 4,
                phaseTitleAr = "استقبال المعاينة والانطباع الأول",
                partnerGerman = initialMsg,
                partnerArabic = when (level) {
                    "A2" -> "طاب يومك، مرحباً بك. هل تعجبك الشقة من النظرة الأولى؟"
                    "B1" -> "طاب يومك، تفضل بالدخول. ما هو انطباعك الأولي عن هذه الشقة وموقعها؟"
                    "B2" -> "طاب يومك، أهلاً بك في المعاينة. هل تلبي مواصفات وتفاصيل هذه الشقة تطلعاتك؟"
                    else -> "طاب يومك! مرحباً بك في الشقة. هل تعجبك؟"
                },
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        // Turn 2: Rent and Budget
        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 4,
                phaseTitleAr = "الإيجار والميزانية الشهرية",
                partnerGerman = if (level in listOf("B1", "B2")) "Die Warmmiete beträgt 850 Euro inklusive Nebenkosten. Wäre dieser monatliche Betrag für Sie im Budget?" else "Die Miete kostet warm 850 Euro. Passt das für Sie?",
                partnerArabic = if (level in listOf("B1", "B2")) "الإيجار الشامل يبلغ 850 يورو متضمناً الخدمات. هل هذا المبلغ الشهري يناسب ميزانيتك؟" else "الإيجار الشامل 850 يورو. هل يناسبك هذا السعر؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "apt_2_1",
                        german = if (level in listOf("B1", "B2")) "Ja, das liegt absolut im Rahmen meines Budgets für eine Wohnung dieser Größe." else "Ja, der Preis passt sehr gut für mich.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، هذا يقع تماماً ضمن نطاق ميزانيتي لشقة بهذا الحجم والموقع." else "نعم، هذا السعر يناسبني تماماً.",
                        badge = "بالإيجاب (نعم / السعر مناسب)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_2_2",
                        german = if (level in listOf("B1", "B2")) "Nein, das liegt leider etwas über meiner kalkulierten Schmerzgrenze für die monatlichen Fixkosten." else "Nein, das ist leider etwas zu teuer für mein Budget.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، للأسف هذا أعلى قليلاً من الحد الأقصى لميزانيتي والتكاليف الشهرية." else "لا، للأسف هذا السعر مكلف قليلاً على ميزانيتي.",
                        badge = "بالنفي (لا / مرتفع عن الميزانية)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_2_3",
                        german = if (level in listOf("B1", "B2")) "Sind in den Nebenkosten auch Heizung und Warmwasser vollständig enthalten, oder muss ich das separat anmelden?" else "Sind die Heizkosten schon dabei, oder muss ich extra zahlen?",
                        translationAr = if (level in listOf("B1", "B2")) "هل التدفئة والماء الساخن مشمولة بالكامل ضمن الخدمات، أم تسجل بشكل منفصل؟" else "هل تكاليف التدفئة مشمولة أم أدفعها منفصلة؟",
                        badge = "خيار بديل (تفاصيل التدفئة والخدمات)"
                    )
                )
            )
        )

        // Turn 3: Move-in date
        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 4,
                phaseTitleAr = "موعد الانتقال والسكن",
                partnerGerman = if (level in listOf("B1", "B2")) "Wäre ein Einzug zum ersten des nächsten Monats für Sie zeitlich realisierbar?" else "Können Sie ab dem ersten nächsten Monat einziehen?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل الانتقال في الأول من الشهر القادم ممكن بالنسبة لك من ناحية التوقيت؟" else "هل يمكنك الانتقال ابتداءً من أول الشهر القادم؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "apt_3_1",
                        german = if (level in listOf("B1", "B2")) "Ja, perfekt! Da mein bisheriger Mietvertrag zeitnah ausläuft, kann ich sofort einziehen." else "Ja, der erste nächste Monat ist perfekt für mich.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم ممتاز! لأن عقد إيجاري الحالي ينتهي قريباً، يمكنني الانتقال مباشرة." else "نعم، أول الشهر القادم موعد مثالي بالنسبة لي.",
                        badge = "بالإيجاب (نعم / جاهز فوراً)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_3_2",
                        german = if (level in listOf("B1", "B2")) "Nein, so kurzfristig schaffe ich es wegen der Kündigungsfrist meiner aktuellen Wohnung leider nicht." else "Nein, so schnell geht es leider nicht wegen der alten Kündigung.",
                        translationAr = if (level in listOf("B1", "B2")) "لا، في وقت قريب كهذا لا أستطيع بسبب مهلة إنهاء عقد شقتي الحالية." else "لا، بهذه السرعة لا أستطيع بسبب إنهاء العقد القديم.",
                        badge = "بالنفي (لا / أحتاج مهلة أطول)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_3_3",
                        german = if (level in listOf("B1", "B2")) "Wäre alternativ ein Einzugstermin ab Mitte des Monats oder ab dem übernächsten Monat denkbar?" else "Ginge es auch ab dem fünfzehnten oder einen Monat später?",
                        translationAr = if (level in listOf("B1", "B2")) "هل يمكن كبديل تحديد موعد انتقال منتصف الشهر أو بداية الشهر الذي يليه؟" else "هل يمكن في منتصف الشهر أو بعد شهر من الآن؟",
                        badge = "خيار بديل (اقتراح موعد آخر)"
                    )
                )
            )
        )

        // Turn 4: Application & Documents
        turns.add(
            DialogueTurn(
                turnNumber = 4,
                totalTurns = 4,
                phaseTitleAr = "ملف التقديم والأوراق المطلوبة",
                partnerGerman = if (level in listOf("B1", "B2")) "Haben Sie alle notwendigen Bewerbungsunterlagen wie Gehaltsnachweise und Schufa-Auskunft bereits vorbereitet?" else "Haben Sie die Unterlagen wie Gehaltsnachweise und Schufa bereit?",
                partnerArabic = if (level in listOf("B1", "B2")) "هل جهزت جميع أوراق ومستندات التقديم المطلوبة مثل كشوفات الراتب وسجل الشوفا؟" else "هل المستندات مثل كشف الراتب والشوفا جاهزة معك؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "apt_4_1",
                        german = if (level in listOf("B1", "B2")) "Ja, meine Bewerbungsmappe inklusive Schufa und der letzten drei Gehaltsnachweise liegt vollständig vor." else "Ja, ich habe alle Unterlagen ausgedruckt und digital bereit.",
                        translationAr = if (level in listOf("B1", "B2")) "نعم، ملف التقديم بما فيه الشوفا وآخر ثلاثة كشوفات راتب جاهز ومكتمل تماماً." else "نعم، جميع الأوراق جاهزة لدي مطبوعة ورقمية.",
                        badge = "بالإيجاب (نعم / جاهزة كاملة)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_4_2",
                        german = if (level in listOf("B1", "B2")) "Noch nicht ganz, ich warte aktuell noch auf die postalische Zusendung meiner aktuellen Schufa-Auskunft." else "Nein, die Schufa-Auskunft fehlt mir noch, ich bekomme sie diese Woche.",
                        translationAr = if (level in listOf("B1", "B2")) "ليس كلياً بعد، ما زلت أنتظر وصول شهادة الشوفا الحديثة بالبريد." else "لا، ينقصني سجل الشوفا فقط وسأستلمه خلال هذا الأسبوع.",
                        badge = "بالنفي (لا / قيد الاستخراج)"
                    ),
                    DialogueAnswerOption(
                        id = "apt_4_3",
                        german = if (level in listOf("B1", "B2")) "Ich kann zusätzlich eine Mietbürgschaft meiner Familie beilegen, falls dies Ihre Entscheidung erleichtert." else "Kann ich auch eine Bürgschaft von meiner Familie einreichen?",
                        translationAr = if (level in listOf("B1", "B2")) "يمكنني إضافة كفالة ضامن (Bürgschaft) من عائلتي لتسهيل اتخاذ القرار." else "هل يمكنني تقديم كفالة ضامن من عائلتي أيضاً؟",
                        badge = "خيار بديل (كفالة ضامن Bürgschaft)"
                    )
                )
            )
        )

        return turns
    }

    private fun buildGenericDialogue(
        scenario: ScenarioEntity,
        persona: String,
        level: String,
        initialMsg: String,
        starters: List<StarterPhraseEntity>
    ): List<DialogueTurn> {
        val turns = mutableListOf<DialogueTurn>()
        val primaryStarter = starters.firstOrNull()

        val turn1Answers = mutableListOf<DialogueAnswerOption>()
        turn1Answers.add(
            DialogueAnswerOption(
                id = "gen_1_1",
                german = if (primaryStarter != null && primaryStarter.german.isNotBlank()) primaryStarter.german else "Guten Tag! Ich möchte mich gerne zu ${scenario.title_de} erkundigen.",
                translationAr = if (primaryStarter != null && primaryStarter.translation_ar.isNotBlank()) primaryStarter.translation_ar else "طاب يومك! أود الاستفسار حول ${scenario.title_ar}.",
                badge = "بدء الحديث"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "gen_1_2",
                german = if (starters.size > 1 && starters[1].german.isNotBlank()) starters[1].german else "Hallo! Könnten Sie mir bitte kurz weiterhelfen?",
                translationAr = if (starters.size > 1 && starters[1].translation_ar.isNotBlank()) starters[1].translation_ar else "مرحباً! هل يمكنك مساعدتي لبرهة من فضلك؟",
                badge = "طلب المساعدة"
            )
        )
        turn1Answers.add(
            DialogueAnswerOption(
                id = "gen_1_3",
                german = if (starters.size > 2 && starters[2].german.isNotBlank()) starters[2].german else "Entschuldigung, ich bin zum ersten Mal hier. Wie läuft das genau ab?",
                translationAr = if (starters.size > 2 && starters[2].translation_ar.isNotBlank()) starters[2].translation_ar else "عذراً، أنا هنا لأول مرة. كيف تجري الأمور هنا تحديداً؟",
                badge = "استفسار توضيحي"
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 1,
                totalTurns = 3,
                phaseTitleAr = "افتتاح المحادثة والتواصل الأول",
                partnerGerman = initialMsg,
                partnerArabic = "طاب يومك، مرحباً بك! كيف يمكنني مساعدتك؟",
                partnerPersona = persona,
                answers = turn1Answers
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 2,
                totalTurns = 3,
                phaseTitleAr = "عرض التفاصيل والطلب الأساسي",
                partnerGerman = "Sehr gerne! Sagen Sie mir bitte genau, was Sie benötigen oder worum es geht.",
                partnerArabic = "بكل سرور! تفضل بإخباري بالضبط بما تحتاجه أو ما هو موضوعك.",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "gen_2_1",
                        german = "Ich habe bereits alles vorbereitet und möchte dies gerne abschließen.",
                        translationAr = "لقد جهزت كل شيء مسبقاً وأود إتمام هذا الأمر الآن.",
                        badge = "إتمام الإجراء"
                    ),
                    DialogueAnswerOption(
                        id = "gen_2_2",
                        german = "Ich bin mir bei einigen Punkten noch unsicher und brauche Ihre Empfehlung.",
                        translationAr = "لست متأكداً تماماً من بعض النقاط وأحتاج إلى مشورتك وتوصيتك.",
                        badge = "طلب نصيحة"
                    ),
                    DialogueAnswerOption(
                        id = "gen_2_3",
                        german = "Welche Möglichkeiten oder Alternativen schlagen Sie mir hierfür vor?",
                        translationAr = "ما هي الخيارات أو البدائل التي تقترحها علي في هذا الشأن؟",
                        badge = "طلب بدائل"
                    )
                )
            )
        )

        turns.add(
            DialogueTurn(
                turnNumber = 3,
                totalTurns = 3,
                phaseTitleAr = "التأكيد والختام الودي",
                partnerGerman = "Wunderbar, das lässt sich schnell erledigen. Haben Sie sonst noch ein Anliegen?",
                partnerArabic = "رائع، يمكن تسوية ذلك بسرعة. هل لديك أي استفسار آخر؟",
                partnerPersona = persona,
                answers = listOf(
                    DialogueAnswerOption(
                        id = "gen_3_1",
                        german = "Vielen Dank für Ihre Hilfe, das war wirklich sehr aufschlussreich! Einen schönen Tag noch.",
                        translationAr = "شكراً جزيلاً لمساعدتك، كان ذلك مفيداً وواضحاً للغاية! أتمنى لك بقية يوم سعيدة.",
                        badge = "شكر وختام"
                    ),
                    DialogueAnswerOption(
                        id = "gen_3_2",
                        german = "Eine kurze Frage noch: Bis wann kann ich mit einer Rückmeldung rechnen?",
                        translationAr = "سؤال قصير أخير: متى يمكنني توقع الرد أو النتيجة تقريباً؟",
                        badge = "سؤال عن الموعد"
                    ),
                    DialogueAnswerOption(
                        id = "gen_3_3",
                        german = "Nein, alles bestens verstanden. Vielen Dank und auf Wiedersehen!",
                        translationAr = "لا، فهمت كل شيء بأفضل صورة. شكراً جزيلاً وإلى اللقاء!",
                        badge = "وداع إيجابي"
                    )
                )
            )
        )

        return turns
    }
}
