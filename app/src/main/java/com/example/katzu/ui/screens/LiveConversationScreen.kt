package com.example.katzu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.example.katzu.R
import com.example.katzu.data.GeminiOnlineConversationService
import com.example.katzu.data.ScenarioEntity
import com.example.katzu.data.StarterPhraseEntity
import com.example.katzu.model.ChatMessage
import com.example.katzu.model.MessageSender
import com.example.katzu.ui.components.AudioWaveformBar
import com.example.katzu.ui.components.bouncyClickable
import com.example.katzu.ui.theme.*
import com.example.katzu.util.AppLogger
import com.example.katzu.util.GermanSpeechRecognizer
import com.example.katzu.util.KatzuHaptics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveConversationScreen(
    initialMessages: List<ChatMessage>,
    scenarioId: String = "",
    cefrLevel: String = "A1",
    initialEffectiveLevel: String? = null,
    onUpdateEffectiveLevel: ((String) -> Unit)? = null,
    scenario: ScenarioEntity? = null,
    starterPhrases: List<StarterPhraseEntity> = emptyList(),
    curriculumVocabulary: List<String> = emptyList(),
    onSendMessageTurn: (suspend (scenarioId: String, text: String, history: List<Pair<String, String>>, cefrLevel: String, scenario: ScenarioEntity?, curriculumVocabulary: List<String>, isFinalTurn: Boolean) -> GeminiOnlineConversationService.ConversationTurnResult)? = null,
    onInspectWord: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    onFinishSession: (
        sentencesSpoken: Int,
        wordsLearned: Int,
        accuracyPercent: Int,
        durationSeconds: Int,
        mistakes: List<com.example.katzu.model.SessionMistakeSummary>,
        praisedSentence: String?,
        hintsUsedCount: Int,
        independentSentences: Int
    ) -> Unit,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: com.example.katzu.data.TtsPlaybackState = com.example.katzu.data.TtsPlaybackState(),
    onStopSpeak: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val cefrLevels = listOf("A1", "A2", "B1", "B2")
    var currentEffectiveLevel by remember(cefrLevel, initialEffectiveLevel) {
        mutableStateOf(initialEffectiveLevel?.ifBlank { null } ?: cefrLevel)
    }
    var showLevelMenu by remember { mutableStateOf(false) }
    var messages by remember(initialMessages) {
        val first = initialMessages.firstOrNull()
        val processed = if (first != null && first.sender == MessageSender.Katzu && first.arabicTranslation.isBlank()) {
            val trans = scenario?.getInitialMessageTranslationForLevel(currentEffectiveLevel)
                ?.ifBlank { com.example.katzu.data.ScenarioTranslations.getByGermanText(first.germanText) }
                .orEmpty()
            if (trans.isNotBlank()) {
                listOf(first.copy(arabicTranslation = trans)) + initialMessages.drop(1)
            } else {
                initialMessages
            }
        } else {
            initialMessages
        }
        mutableStateOf(processed)
    }
    val sessionStartTime = remember { System.currentTimeMillis() }
    var showTranslation by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var audioRmsDb by remember { mutableFloatStateOf(0f) }
    val animatedRms by animateFloatAsState(
        targetValue = if (isListening) audioRmsDb.coerceIn(0f, 12f) else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "audio_rms_spring"
    )
    var speechPartialText by remember { mutableStateOf("") }
    var showHintsBottomSheet by remember { mutableStateOf(false) }
    var showInlineSuggestionCard by remember { mutableStateOf(false) }
    var showMoreSuggestions by remember { mutableStateOf(false) }
    var activeSelectedSuggestionIndex by remember { mutableIntStateOf(0) }
    var hintsUsedCount by remember { mutableIntStateOf(0) }
    var currentTurnViewedOrUsedHint by remember { mutableStateOf(false) }
    var showMoreQuickReplies by remember { mutableStateOf(false) }
    var showMoreSheetHints by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var userDraftInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var speechNoticeMessage by remember { mutableStateOf<String?>(null) }
    var speechNoticeIsNetworkError by remember { mutableStateOf(false) }
    var isWaitingForKatzu by remember { mutableStateOf(false) }
    var conversationError by remember { mutableStateOf<String?>(null) }
    var lastFailedUserMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Conversational exchange length calibrated to CEFR levels (A1=3, A2=4, B1=5, B2=6 user turns)
    // Ensures learners have a complete, satisfying dialogue before the wrap-up without feeling cut off prematurely.
    val targetExchangeCount = remember(currentEffectiveLevel) {
        when (currentEffectiveLevel.uppercase().take(2)) {
            "A1" -> 3
            "A2" -> 4
            "B1" -> 5
            "B2" -> 6
            else -> 3
        }
    }
    var isConversationCompleted by remember { mutableStateOf(false) }
    var allowBonusChat by remember { mutableStateOf(false) }

    fun finishAndSaveSession() {
        val userMessages = messages.filter { it.sender == MessageSender.User }
        val sentencesSpoken = userMessages.size
        val wordsLearned = userMessages.sumOf { msg ->
            msg.germanText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        }
        val actualHintsUsed = userMessages.count { it.wasHintUsed }
        val independentSentences = userMessages.count { !it.wasHintUsed }
        val independentMistakes = userMessages.count { !it.wasHintUsed && it.hasCorrection }
        
        // Prevent fake progress: evaluate mastery based on independent formulation
        val accuracyPercent = when {
            independentSentences > 0 -> {
                val independentCorrect = (independentSentences - independentMistakes).coerceAtLeast(0)
                ((independentCorrect.toFloat() / independentSentences) * 100).toInt().coerceIn(0, 100)
            }
            sentencesSpoken > 0 && actualHintsUsed == sentencesSpoken -> {
                // Completed fully with hints assistance - legitimate completion with training tier
                65
            }
            else -> 100
        }
        val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt().coerceAtLeast(1)

        val corrections = userMessages.filter { it.hasCorrection }
        val sessionMistakes = corrections.map { msg ->
            com.example.katzu.model.SessionMistakeSummary(
                originalMistake = msg.originalMistake.ifBlank { msg.germanText },
                correctedGerman = msg.correctedGerman,
                grammarRule = msg.grammarRule,
                roastComment = msg.roastComment,
                wasHintUsed = msg.wasHintUsed
            )
        }
        val praisedSentence = userMessages.firstOrNull { !it.hasCorrection && !it.wasHintUsed && it.germanText.split("\\s+".toRegex()).size >= 3 }?.germanText
            ?: userMessages.firstOrNull { !it.hasCorrection && !it.wasHintUsed }?.germanText
            ?: userMessages.firstOrNull { !it.hasCorrection }?.germanText

        onFinishSession(
            sentencesSpoken,
            wordsLearned,
            accuracyPercent,
            durationSeconds,
            sessionMistakes,
            praisedSentence,
            actualHintsUsed,
            independentSentences
        )
    }

    val counterpartAvatarRes = remember(scenario) {
        val category = scenario?.category?.lowercase().orEmpty()
        val persona = scenario?.ai_persona?.lowercase().orEmpty()
        when {
            category.contains("cafe") || category.contains("restaurant") || category.contains("food") || persona.contains("barista") || persona.contains("kellner") -> R.drawable.katzu_barista
            category.contains("trail") || category.contains("travel") || category.contains("airport") || category.contains("reisen") -> R.drawable.katzu_trail_guide
            category.contains("interview") || category.contains("official") || category.contains("amt") || category.contains("work") || category.contains("doctor") || category.contains("arzt") -> R.drawable.katzu_scenario_host
            else -> R.drawable.katzu_avatar
        }
    }

    fun handleBackNavigation() {
        val hasUserMessages = messages.any { it.sender == MessageSender.User }
        if (hasUserMessages) {
            showExitConfirmDialog = true
        } else {
            onStopSpeak()
            onBack()
        }
    }

    BackHandler {
        handleBackNavigation()
    }

    val speechRecognizer = remember { GermanSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
            onStopSpeak()
        }
    }

    // Active hints: initialized with scenario starter phrases, then dynamically refreshed after each AI turn
    var activeHints by remember(starterPhrases) {
        mutableStateOf(
            starterPhrases.map { phrase ->
                GeminiOnlineConversationService.ConversationHint(
                    german = phrase.german,
                    translationAr = phrase.translation_ar,
                    isContextual = false
                )
            }
        )
    }
    var hintsForKatzuMessageId by remember { mutableStateOf<String?>(null) }
    var isGeneratingHints by remember { mutableStateOf(false) }

    fun refreshHints(targetMsgId: String? = null) {
        val lastKatzu = messages.lastOrNull { it.sender == MessageSender.Katzu }
        val lastKatzuMsg = lastKatzu?.germanText
        if (lastKatzuMsg.isNullOrBlank()) return

        coroutineScope.launch {
            isGeneratingHints = true
            try {
                val historyPairs = messages.map {
                    val role = if (it.sender == MessageSender.User) "user" else "assistant"
                    Pair(role, it.germanText)
                }
                val newHints = GeminiOnlineConversationService.generateContextualHints(
                    scenario = scenario,
                    cefrLevel = currentEffectiveLevel,
                    history = historyPairs,
                    lastAiReply = lastKatzuMsg
                )
                if (newHints.isNotEmpty()) {
                    activeHints = newHints
                    hintsForKatzuMessageId = targetMsgId ?: lastKatzu.id
                }
            } catch (e: Exception) {
                AppLogger.w("LiveConvScreen", "Could not refresh hints: ${e.message}")
            } finally {
                isGeneratingHints = false
            }
        }
    }

    LaunchedEffect(messages.firstOrNull()?.id, currentEffectiveLevel) {
        val firstMsg = messages.firstOrNull()
        if (firstMsg != null && firstMsg.sender == MessageSender.Katzu) {
            // Eagerly pre-load next hint for opening turn in the background
            if (hintsForKatzuMessageId != firstMsg.id && !isGeneratingHints) {
                refreshHints(firstMsg.id)
            }
            if (firstMsg.arabicTranslation.isBlank()) {
                val directTrans = scenario?.getInitialMessageTranslationForLevel(currentEffectiveLevel)
                    ?.ifBlank { com.example.katzu.data.ScenarioTranslations.getByGermanText(firstMsg.germanText) }
                    .orEmpty()
                if (directTrans.isNotBlank()) {
                    messages = messages.mapIndexed { index, chatMsg ->
                        if (index == 0) chatMsg.copy(arabicTranslation = directTrans) else chatMsg
                    }
                } else {
                    try {
                        val translation = GeminiOnlineConversationService.translateToArabic(germanText = firstMsg.germanText)
                        if (translation.isNotBlank()) {
                            messages = messages.mapIndexed { index, chatMsg ->
                                if (index == 0) chatMsg.copy(arabicTranslation = translation) else chatMsg
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.w("LiveConvScreen", "Could not translate intro message: ${e.message}")
                    }
                }
            }
        }
    }

    val sentUserTexts = remember(messages) {
        messages.filter { it.sender == MessageSender.User }.map { it.germanText.trim().lowercase() }
    }

    fun isPhraseUsed(germanPhrase: String): Boolean {
        val norm = germanPhrase.trim().lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }
        if (norm.isBlank()) return false
        return sentUserTexts.any { sent ->
            val cleanSent = sent.filter { it.isLetterOrDigit() || it.isWhitespace() }
            if (cleanSent.isBlank()) false
            else if (cleanSent == norm) true
            else if (norm.length >= 15 && cleanSent.contains(norm)) true
            else false
        }
    }

    data class StarterSuggestion(
        val german: String,
        val translationAr: String,
        val sortOrder: Int,
        val isContextual: Boolean
    )

    val latestKatzuMessage = messages.lastOrNull { it.sender == MessageSender.Katzu }
    val hasContextualForCurrentTurn = hintsForKatzuMessageId != null &&
        hintsForKatzuMessageId == latestKatzuMessage?.id &&
        activeHints.any { it.isContextual }

    val unusedStarterSuggestions = remember(starterPhrases, activeHints, sentUserTexts, hasContextualForCurrentTurn) {
        if (hasContextualForCurrentTurn) {
            // Live contextual suggestions generated for the current turn take priority
            val contextual = activeHints
                .filter { it.isContextual && !isPhraseUsed(it.german) }
                .mapIndexed { index, hint ->
                    StarterSuggestion(
                        german = hint.german,
                        translationAr = hint.translationAr,
                        sortOrder = index + 1,
                        isContextual = true
                    )
                }
            if (contextual.isNotEmpty()) {
                contextual
            } else {
                activeHints.filter { it.isContextual }.mapIndexed { index, hint ->
                    StarterSuggestion(hint.german, hint.translationAr, index + 1, true)
                }
            }
        } else {
            // Unused starter phrases from D1
            val unusedStarters = starterPhrases
                .filter { !isPhraseUsed(it.german) }
                .sortedBy { it.sort_order }
                .map {
                    StarterSuggestion(
                        german = it.german,
                        translationAr = it.translation_ar,
                        sortOrder = it.sort_order,
                        isContextual = false
                    )
                }
            if (unusedStarters.isNotEmpty()) {
                unusedStarters
            } else {
                // If starter phrases are exhausted, show any unused active hints
                val unusedActive = activeHints
                    .filter { !isPhraseUsed(it.german) }
                    .mapIndexed { index, hint ->
                        StarterSuggestion(
                            german = hint.german,
                            translationAr = hint.translationAr,
                            sortOrder = index + 1,
                            isContextual = hint.isContextual
                        )
                    }
                if (unusedActive.isNotEmpty()) {
                    unusedActive
                } else if (starterPhrases.isNotEmpty()) {
                    starterPhrases.sortedBy { it.sort_order }.map {
                        StarterSuggestion(it.german, it.translation_ar, it.sort_order, false)
                    }
                } else {
                    activeHints.mapIndexed { index, hint ->
                        StarterSuggestion(hint.german, hint.translationAr, index + 1, hint.isContextual)
                    }
                }
            }
        }
    }
    val primarySuggestedPhrase = unusedStarterSuggestions.firstOrNull()
    val activeSuggestedPhrase = remember(unusedStarterSuggestions, activeSelectedSuggestionIndex) {
        unusedStarterSuggestions.getOrNull(activeSelectedSuggestionIndex) ?: unusedStarterSuggestions.firstOrNull()
    }
    val otherUnusedPhrases = remember(unusedStarterSuggestions, activeSuggestedPhrase) {
        unusedStarterSuggestions.filter { it != activeSuggestedPhrase }
    }

    val userTurnsCount = messages.count { it.sender == MessageSender.User }
    var isMissionsExpanded by remember { mutableStateOf(false) }
    val microMissions = remember(scenario, userTurnsCount, targetExchangeCount) {
        val cat = ((scenario?.category ?: "") + " " + (scenario?.id ?: "") + " " + (scenario?.title_ar ?: "")).lowercase()
        val rawMissions = when {
            cat.contains("cafe") || cat.contains("قهوة") || cat.contains("مطعم") || cat.contains("barista") || cat.contains("essen") -> listOf(
                "إلقاء التحية وطلب ما ترغب به",
                "تحديد الإضافات أو السؤال عن السعر",
                "طلب الحساب والدفع بلباقة"
            )
            cat.contains("shop") || cat.contains("تسوق") || cat.contains("market") || cat.contains("kauf") -> listOf(
                "التحية وتحديد الغرض الذي تبحث عنه",
                "السؤال عن التفاصيل أو المقاس أو التكلفة",
                "إتمام الشراء والوداع"
            )
            cat.contains("travel") || cat.contains("سفر") || cat.contains("مطار") || cat.contains("فندق") || cat.contains("zug") || cat.contains("bahn") -> listOf(
                "التعريف بنفسك وتوضيح وجهتك أو حجزك",
                "الاستفسار عن التوقيت، المسار أو الغرفة",
                "شكر الموظف واستلام التذكرة أو المفتاح"
            )
            cat.contains("doctor") || cat.contains("طبيب") || cat.contains("صيدل") || cat.contains("apotheke") || cat.contains("arzt") -> listOf(
                "وصف الأعراض والمشكلة الصحية",
                "الإجابة عن استفسارات الطبيب أو الصيدلي",
                "السؤال عن طريقة الاستخدام واستلام الوصفة"
            )
            else -> listOf(
                "التحية وافتتاح الحوار بثقة",
                "تبادل التفاصيل وطرح الأسئلة المناسبة",
                "إنهاء المحادثة بالشكر والوداع"
            )
        }

        val step1Done = userTurnsCount >= 1
        val step2Done = userTurnsCount >= (targetExchangeCount / 2).coerceAtLeast(2)
        val step3Done = userTurnsCount >= targetExchangeCount

        listOf(
            ConversationMission(1, rawMissions[0], isCompleted = step1Done, isCurrent = !step1Done),
            ConversationMission(2, rawMissions[1], isCompleted = step2Done, isCurrent = step1Done && !step2Done),
            ConversationMission(3, rawMissions[2], isCompleted = step3Done, isCurrent = step2Done && !step3Done)
        )
    }

    fun sendUserMessage(germanText: String) {
        val trimmed = germanText.trim()
        if (trimmed.isEmpty()) return

        val usedCurrentHint = currentTurnViewedOrUsedHint ||
            (activeSuggestedPhrase != null && isPhraseUsed(activeSuggestedPhrase.german)) ||
            (unusedStarterSuggestions.any { isPhraseUsed(it.german) }) ||
            activeHints.any { it.german.trim().equals(trimmed, ignoreCase = true) } ||
            starterPhrases.any { it.german.trim().equals(trimmed, ignoreCase = true) }
        if (usedCurrentHint) {
            hintsUsedCount++
        }
        currentTurnViewedOrUsedHint = false
        showInlineSuggestionCard = false
        showMoreSuggestions = false
        activeSelectedSuggestionIndex = 0

        speechPartialText = ""
        // Check if user tapped or spoke a known starter phrase or active hint for instant translation
        val matchedStarter = starterPhrases.find { it.german.trim().equals(trimmed, ignoreCase = true) }
        val matchedHint = activeHints.find { it.german.trim().equals(trimmed, ignoreCase = true) }
        val immediateTrans = matchedStarter?.translation_ar
            ?: matchedHint?.translationAr
            ?: ""

        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = MessageSender.User,
            germanText = trimmed,
            arabicTranslation = immediateTrans,
            wasHintUsed = usedCurrentHint
        )
        messages = messages + userMsg
        conversationError = null
        lastFailedUserMessage = null

        // If translation is not yet known from cache, resolve translation immediately in parallel
        if (immediateTrans.isBlank()) {
            coroutineScope.launch {
                try {
                    val directTrans = GeminiOnlineConversationService.translateToArabic(germanText = trimmed)
                    if (directTrans.isNotBlank()) {
                        messages = messages.map {
                            if (it.id == userMsg.id && it.arabicTranslation.isBlank()) {
                                it.copy(arabicTranslation = directTrans)
                            } else it
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("LiveConvScreen", "Could not resolve user message translation immediately: ${e.message}")
                }
            }
        }

        // Trigger Katzu dynamic reply (via API if available or contextual fallback)
        coroutineScope.launch {
            isWaitingForKatzu = true
            listState.animateScrollToItem(messages.size - 1)

            try {
                // Pass prior history strictly before the new userMsg to prevent duplication
                val history = messages.dropLast(1).map {
                    val role = if (it.sender == MessageSender.User) "user" else "assistant"
                    Pair(role, it.germanText)
                }

                val userTurnsSoFar = messages.count { it.sender == MessageSender.User }
                val isFinalTurn = !allowBonusChat && userTurnsSoFar >= targetExchangeCount

                val turnResult = if (onSendMessageTurn != null) {
                    onSendMessageTurn(scenarioId, trimmed, history, currentEffectiveLevel, scenario, curriculumVocabulary, isFinalTurn)
                } else {
                    GeminiOnlineConversationService.processTurn(scenarioId, trimmed, history, currentEffectiveLevel, scenario, curriculumVocabulary, isFinalTurn)
                }

                // Attach real user message translation and any rich diagnostic correction details
                val evaluation = turnResult.evaluation
                val realUserTranslation = evaluation.userMessageTranslationAr
                messages = messages.map {
                    if (it.id == userMsg.id) {
                        val finalTrans = if (realUserTranslation.isNotBlank()) {
                            realUserTranslation
                        } else if (it.arabicTranslation.isNotBlank()) {
                            it.arabicTranslation
                        } else {
                            ""
                        }
                        it.copy(
                            arabicTranslation = finalTrans,
                            hasCorrection = !evaluation.isCorrect && evaluation.correctedGerman.isNotBlank(),
                            originalMistake = if (!evaluation.isCorrect) evaluation.originalMistake.ifBlank { trimmed } else "",
                            correctedGerman = if (!evaluation.isCorrect) evaluation.correctedGerman else "",
                            roastComment = if (!evaluation.isCorrect) evaluation.explanationAr else "",
                            grammarRule = if (!evaluation.isCorrect) evaluation.grammarRule.ifBlank { "قواعد ألمانية" } else "",
                            mistakeSegment = if (!evaluation.isCorrect) evaluation.mistakeSegment else "",
                            correctedSegment = if (!evaluation.isCorrect) evaluation.correctedSegment else "",
                            positiveNoteAr = if (evaluation.isCorrect) evaluation.positiveNoteAr else ""
                        )
                    } else it
                }

                val katzuReply = ChatMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    sender = MessageSender.Katzu,
                    germanText = turnResult.replyGerman,
                    arabicTranslation = turnResult.replyArabic
                )
                messages = messages + katzuReply
                lastFailedUserMessage = null
                onSpeak(katzuReply.germanText, 1.0f)

                // Eagerly pre-load next hint along with the AI answer so it's ready immediately with 0ms delay!
                if (turnResult.contextualHints.isNotEmpty()) {
                    activeHints = turnResult.contextualHints
                    hintsForKatzuMessageId = katzuReply.id
                    activeSelectedSuggestionIndex = 0
                } else {
                    refreshHints(katzuReply.id)
                }

                // Check if user has completed the conversational exchanges expected for this CEFR level
                val completedUserExchanges = messages.count { it.sender == MessageSender.User }
                if (completedUserExchanges >= targetExchangeCount) {
                    isConversationCompleted = true
                    KatzuHaptics.success(haptic, context)
                }
            } catch (e: Exception) {
                AppLogger.e("LiveConversationScreen", "Conversation turn failed: ${e.message}", e)
                lastFailedUserMessage = trimmed
                val isAuthError = e.message?.contains("401") == true || e.message?.contains("UNAUTHENTICATED") == true
                conversationError = if (isAuthError) {
                    "تعذر الاتصال بكاتزو الآن: يرجى التحقق من صلاحية مفتاح Gemini API في لوحة الإعدادات."
                } else {
                    "تعذر الاتصال بكاتزو الآن. يرجى التحقق من اتصالك بالإنترنت والمحاولة مجدداً."
                }
            } finally {
                isWaitingForKatzu = false
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun startListeningFlow() {
        conversationError = null
        speechNoticeMessage = null
        speechPartialText = ""
        audioRmsDb = 0f
        speechRecognizer.startListening(object : GermanSpeechRecognizer.Listener {
            override fun onReadyForSpeech() {
                isListening = true
                KatzuHaptics.micStart(haptic, context)
            }

            override fun onBeginningOfSpeech() {
                KatzuHaptics.tick(haptic, context)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // SpeechRecognizer rmsdB typically ranges from -2f up to 10f+
                val normalized = (rmsdB + 2f).coerceAtLeast(0f)
                audioRmsDb = normalized
            }

            override fun onPartialResult(partialText: String) {
                speechPartialText = partialText
                if (partialText.isNotBlank()) {
                    userDraftInput = partialText
                }
            }

            override fun onFinalResult(recognizedText: String) {
                isListening = false
                audioRmsDb = 0f
                speechPartialText = ""
                KatzuHaptics.micStop(haptic, context)
                val clean = recognizedText.trim()
                if (clean.isNotBlank()) {
                    userDraftInput = clean
                }
            }

            override fun onError(errorMessageAr: String, errorCode: Int) {
                isListening = false
                audioRmsDb = 0f
                speechPartialText = ""
                KatzuHaptics.micStop(haptic, context)
                AppLogger.e("LiveConversationScreen", "SpeechRecognizer onError raw code: $errorCode, message: '$errorMessageAr'")
                // Show clear retry prompt rather than dropping turn silently
                speechNoticeMessage = errorMessageAr
                speechNoticeIsNetworkError = (errorCode == android.speech.SpeechRecognizer.ERROR_NETWORK ||
                        errorCode == android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
            }

            override fun onEndOfSpeech() {
                isListening = false
                audioRmsDb = 0f
                KatzuHaptics.micStop(haptic, context)
            }
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningFlow()
        } else {
            speechNoticeMessage = "يتطلب كاتزو إذن الميكروفون للاستماع إلى نطقك بالألمانية."
            speechNoticeIsNetworkError = false
        }
    }

    fun toggleListening() {
        if (isListening) {
            KatzuHaptics.micStop(haptic, context)
            speechRecognizer.stopListening()
            isListening = false
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                startListeningFlow()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sleek Modern Header (Spacious, modern layout with dedicated exchange progress strip)
            Surface(
                color = BackgroundPure,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF221F33).copy(alpha = 0.7f))
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Navigation & Title Bar (Spacious and breathable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        IconButton(
                            onClick = { handleBackNavigation() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "رجوع",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Center Scenario Info (Uncrowded, ample space to display titles)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCard)
                                    .border(1.2.dp, Primary.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = counterpartAvatarRes),
                                    contentDescription = "الشخصية",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.weight(1f)
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = scenario?.title_de ?: "Gespräch mit Katzu",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                        ),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = scenario?.title_ar ?: "محادثة حية",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    fontFamily = Cairo,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right Actions: Level Pill + Quick Translate Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Interactive CEFR Level Badge
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentEffectiveLevel != cefrLevel) StatusLearning.copy(alpha = 0.18f) else Primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (currentEffectiveLevel != cefrLevel) StatusLearning.copy(alpha = 0.4f) else Primary.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showLevelMenu = true }
                                ) {
                                    Text(
                                        text = currentEffectiveLevel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (currentEffectiveLevel != cefrLevel) StatusLearning else Primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showLevelMenu,
                                    onDismissRequest = { showLevelMenu = false },
                                    modifier = Modifier.background(SurfaceCard)
                                ) {
                                    cefrLevels.forEach { lvl ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "المستوى $lvl",
                                                    color = if (lvl == currentEffectiveLevel) Primary else TextPrimary,
                                                    fontWeight = if (lvl == currentEffectiveLevel) FontWeight.Bold else FontWeight.Normal,
                                                    fontFamily = Cairo
                                                )
                                            },
                                            onClick = {
                                                currentEffectiveLevel = lvl
                                                onUpdateEffectiveLevel?.invoke(lvl)
                                                showLevelMenu = false
                                                Toast.makeText(context, "تم ضبط الصعوبة على $lvl", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }

                            // Quick Translate Toggle Button
                            IconButton(
                                onClick = { showTranslation = !showTranslation },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (showTranslation) Primary.copy(alpha = 0.25f) else SurfaceCardSubtle)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = if (showTranslation) "إخفاء الترجمة" else "إظهار الترجمة",
                                    tint = if (showTranslation) Primary else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Modern Dedicated Exchange Progress Strip (Clean, scannable, uncrowded)
                    val userTurnsCount = messages.count { it.sender == MessageSender.User }
                    val isTargetReached = userTurnsCount >= targetExchangeCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isTargetReached) StatusSuccess else Primary)
                            )
                            Text(
                                text = when {
                                    isTargetReached -> "اكتملت جولات المحادثة ✓"
                                    userTurnsCount == targetExchangeCount - 1 -> "جولة الختام القادمة (${userTurnsCount + 1}/$targetExchangeCount)"
                                    else -> "الجولة $userTurnsCount من $targetExchangeCount"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isTargetReached) StatusSuccess else TextSecondary,
                                fontFamily = Cairo
                            )
                        }

                        // Segmented dash indicators
                        val dashWidth = if (targetExchangeCount > 4) 14.dp else 18.dp
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (step in 1..targetExchangeCount) {
                                val isStepCompleted = step <= userTurnsCount
                                Box(
                                    modifier = Modifier
                                        .width(dashWidth)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isStepCompleted) {
                                                if (isTargetReached) StatusSuccess else Primary
                                            } else {
                                                BorderSubtle.copy(alpha = 0.45f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // Micro-Missions progress row (Clean, focused, collapsible)
                    val currentMission = microMissions.firstOrNull { it.isCurrent } ?: microMissions.last()
                    val completedMissionsCount = microMissions.count { it.isCompleted }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color = BorderSubtle.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isMissionsExpanded = !isMissionsExpanded }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "🎯", fontSize = 11.sp)
                            Text(
                                text = if (completedMissionsCount == 3) "اكتملت جميع أهداف المحادثة ✓" else "الهدف: ${currentMission.titleAr}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = Cairo,
                                    fontSize = 11.sp
                                ),
                                color = if (completedMissionsCount == 3) StatusSuccess else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$completedMissionsCount/3",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completedMissionsCount == 3) StatusSuccess else Primary
                                )
                            )
                            Icon(
                                imageVector = if (isMissionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isMissionsExpanded) "إغلاق الأهداف" else "عرض الأهداف",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isMissionsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            microMissions.forEach { mission ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    mission.isCompleted -> StatusSuccess.copy(alpha = 0.2f)
                                                    mission.isCurrent -> Primary.copy(alpha = 0.2f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    mission.isCompleted -> StatusSuccess
                                                    mission.isCurrent -> Primary
                                                    else -> BorderSubtle
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (mission.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = StatusSuccess,
                                                modifier = Modifier.size(9.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${mission.id}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (mission.isCurrent) Primary else TextMuted
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = mission.titleAr,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontFamily = Cairo,
                                            fontWeight = if (mission.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = when {
                                            mission.isCompleted -> TextMuted
                                            mission.isCurrent -> TextPrimary
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Icebreaker Banner (Breaks the ice on first turn without clutter)
            if (userTurnsCount == 0 && !isWaitingForKatzu && messages.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B1726),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❄️", fontSize = 13.sp)
                        Text(
                            text = "ابدأ بكسر الجليد: استمع لنطق المحاور أو اضغط 💡 للمقترح، ثم تحدّث بالميكروفون 🎙️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontFamily = Cairo
                            ),
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (!showInlineSuggestionCard && primarySuggestedPhrase != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        showInlineSuggestionCard = true
                                        currentTurnViewedOrUsedHint = true
                                    }
                            ) {
                                Text(
                                    text = "فكرة للرد",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Cairo
                                    ),
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Message Stream
            if (conversationError != null) {
                Surface(
                    color = StatusError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, StatusError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = conversationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError,
                            modifier = Modifier.weight(1f)
                        )
                        if (lastFailedUserMessage != null && !isWaitingForKatzu) {
                            TextButton(
                                onClick = {
                                    val failedText = lastFailedUserMessage ?: return@TextButton
                                    conversationError = null
                                    sendUserMessage(failedText)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "إعادة المحاولة",
                                    tint = StatusError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إعادة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusError
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                conversationError = null
                                lastFailedUserMessage = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Subtle scenario illustration watermark in background to prevent empty voids
                Image(
                    painter = painterResource(id = counterpartAvatarRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.Center)
                        .graphicsLayer(alpha = 0.04f),
                    contentScale = ContentScale.Fit
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                items(messages) { message ->
                    if (message.sender == MessageSender.Katzu) {
                        KatzuMessageBubble(
                            message = message,
                            showTranslation = showTranslation,
                            avatarRes = counterpartAvatarRes,
                            ttsPlaybackState = ttsPlaybackState,
                            onSpeak = onSpeak,
                            onStopSpeak = onStopSpeak,
                            onWordClick = onInspectWord
                        )
                    } else {
                        UserMessageBubble(
                            message = message,
                            showTranslation = showTranslation,
                            ttsPlaybackState = ttsPlaybackState,
                            onSpeak = onSpeak,
                            onStopSpeak = onStopSpeak,
                            onUseCorrection = { correctedSentence ->
                                userDraftInput = correctedSentence
                            },
                            onWordClick = onInspectWord
                        )
                    }
                }

                // Thinking / typing bubble while waiting for Katzu's response
                if (isWaitingForKatzu) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_listening),
                                contentDescription = "كاتزو يفكر",
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    KatzuThinkingWave()
                                    Text(
                                        text = "كاتزو يفكّر في الرد المناسب...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Automatically display celebration card when conversation completes
                if (isConversationCompleted) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFC9A8FF), Primary, Color(0xFFF5B8E0)))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp)
                                .testTag("conversation_completed_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = Color(0xFFF0C674),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "اكتمل الحوار بنجاح! أحسنت 🏆",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = Cairo
                                    )
                                }
                                Text(
                                    text = "أجريت الحوار كاملاً حسب معايير المستوى $currentEffectiveLevel ($targetExchangeCount جولات متبادلة بنجاح). يمكنك الآن مراجعة تقرير أدائك وتقييم كاتزو، أو الاستمرار بالحديث لمزيد من الممارسة.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontFamily = Cairo
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            KatzuHaptics.tick(haptic, context)
                                            allowBonusChat = true
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            text = "متابعة الحديث 💬",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextSecondary,
                                            fontFamily = Cairo
                                        )
                                    }
                                    Button(
                                        onClick = { finishAndSaveSession() },
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            text = "عرض التقرير 📊",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            fontFamily = Cairo
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            }

            // Modern Native Chat Input Bar
            Surface(
                color = BackgroundPure,
                border = BorderStroke(1.dp, Color(0xFF221F33)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Non-blocking Speech Notice with direct keyboard focus & retry actions
                    AnimatedVisibility(
                        visible = speechNoticeMessage != null,
                        enter = fadeIn(animationSpec = tween(180)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(140)) + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, if (speechNoticeIsNetworkError) StatusLearning.copy(alpha = 0.5f) else StatusError.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (speechNoticeIsNetworkError) Icons.Default.WifiOff else Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = if (speechNoticeIsNetworkError) StatusLearning else StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = speechNoticeMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = Cairo,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { speechNoticeMessage = null },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            speechNoticeMessage = null
                                            try {
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Primary,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Keyboard,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الكتابة الآن ⌨️",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Cairo, fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            speechNoticeMessage = null
                                            startListeningFlow()
                                        },
                                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إعادة المحاولة 🎙️",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = Cairo),
                                            color = Primary
                                        )
                                    }
                                }

                                if (speechNoticeIsNetworkError) {
                                    Text(
                                        text = "💡 نصيحة: يمكنك أيضاً الضغط على أيقونة المايك داخل لوحة المفاتيح (Gboard) للتحدث مباشرة، أو تحميل حزمة الألمانية للاستخدام بلا إنترنت من تطبيق Google.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = Cairo,
                                            fontSize = 10.5.sp,
                                            color = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Audio listening status indicator
                    if (isListening) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusError)
                            )
                            Text(
                                text = if (speechPartialText.isNotBlank()) "أستمع: $speechPartialText" else "أستمع إليك الآن... تكلّم بالألمانية",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    speechRecognizer.stopListening()
                                    isListening = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("تم", color = StatusSuccess, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Single suggestion card controlled by the Lamp button (minimalist, modern, less is more)
                    if (showInlineSuggestionCard) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeSuggestedPhrase != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SurfaceCard,
                                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("primary_suggested_reply_btn")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header: Lightbulb + Title + Cycle Switcher + Close
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = StatusLearning,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "مقترح للرد",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = Cairo
                                                    ),
                                                    color = StatusLearning
                                                )
                                                if (isGeneratingHints) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        strokeWidth = 1.5.dp,
                                                        color = Primary
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Cycle to next preloaded suggestion instantly without reload!
                                                if (unusedStarterSuggestions.size > 1) {
                                                    Surface(
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = SurfaceCardSubtle,
                                                        border = BorderStroke(1.dp, BorderSubtle),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(9999.dp))
                                                            .clickable {
                                                                KatzuHaptics.tick(haptic, context)
                                                                activeSelectedSuggestionIndex = (activeSelectedSuggestionIndex + 1) % unusedStarterSuggestions.size
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "اقتراح آخر",
                                                                tint = Primary,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = "اقتراح آخر (${(activeSelectedSuggestionIndex % unusedStarterSuggestions.size) + 1}/${unusedStarterSuggestions.size})",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 11.sp,
                                                                    fontFamily = Cairo,
                                                                    fontWeight = FontWeight.Medium
                                                                ),
                                                                color = Primary
                                                            )
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        showInlineSuggestionCard = false
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "إغلاق المقترح",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // German Phrase with word-by-word karaoke highlight (forced LTR)
                                        val isMainHintPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == activeSuggestedPhrase.german
                                        KaraokeGermanText(
                                            text = activeSuggestedPhrase.german,
                                            isPlaying = isMainHintPlaying,
                                            charRange = ttsPlaybackState.charRange,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = TextPrimary
                                        )

                                        // Arabic Translation
                                        if (activeSuggestedPhrase.translationAr.isNotBlank()) {
                                            Text(
                                                text = activeSuggestedPhrase.translationAr,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                                color = TextSecondary,
                                                fontFamily = Cairo
                                            )
                                        }

                                        // Action bar: Listen & Use Phrase (Clean, minimal, high contrast)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        if (isMainHintPlaying) {
                                                            onStopSpeak()
                                                        } else {
                                                            onSpeak(activeSuggestedPhrase.german, 1.0f)
                                                        }
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isMainHintPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                                    contentDescription = if (isMainHintPlaying) "إيقاف النطق" else "استمع للعبارة",
                                                    tint = Primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isMainHintPlaying) "إيقاف" else "استمع",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = Cairo
                                                    ),
                                                    color = Primary
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(9999.dp),
                                                color = if (isListening) StatusError else Primary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(9999.dp))
                                                    .pointerInput(isListening) {
                                                        detectTapGestures(
                                                            onPress = {
                                                                hintsUsedCount++
                                                                onStopSpeak()
                                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                                    context,
                                                                    Manifest.permission.RECORD_AUDIO
                                                                ) == PackageManager.PERMISSION_GRANTED
                                                                if (!hasPermission) {
                                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                                    return@detectTapGestures
                                                                }
                                                                if (isListening) {
                                                                    KatzuHaptics.micStop(haptic, context)
                                                                    speechRecognizer.stopListening()
                                                                    isListening = false
                                                                    audioRmsDb = 0f
                                                                    return@detectTapGestures
                                                                }
                                                                startListeningFlow()
                                                                val pressStart = System.currentTimeMillis()
                                                                val released = tryAwaitRelease()
                                                                val pressDuration = System.currentTimeMillis() - pressStart
                                                                if (released && pressDuration >= 400) {
                                                                    KatzuHaptics.micStop(haptic, context)
                                                                    speechRecognizer.stopListening()
                                                                    isListening = false
                                                                    audioRmsDb = 0f
                                                                }
                                                            }
                                                        )
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                                        contentDescription = if (isListening) "إيقاف الاستماع" else "تحدث بالعبارة",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = if (isListening) "جارٍ الاستماع... تكلّم الآن 🎙️" else "تحدث بالعبارة 🎙️",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = Cairo
                                                        ),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "تحدّث بالعبارة بصوتك أو اكتبها — التدريب الحقيقي يثبت بالنطق المباشر ✦",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontFamily = Cairo
                                            ),
                                            color = TextMuted,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            } else if (isGeneratingHints) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceCardSubtle,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Primary
                                        )
                                        Text(
                                            text = "كاتزو يجهّز مقترح الرد...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontFamily = Cairo
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Real-time Voice Waves Visualizer Banner when listening
                    AnimatedVisibility(
                        visible = isListening,
                        enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFC9A8FF), Primary, Color(0xFFF5B8E0)))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val infiniteDot = rememberInfiniteTransition(label = "recording_dot")
                                        val dotAlpha by infiniteDot.animateFloat(
                                            initialValue = 0.4f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "dot_alpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(StatusError.copy(alpha = dotAlpha))
                                        )
                                        Text(
                                            text = "جاري الاستماع... تحدث بالألمانية",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = Cairo
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                KatzuHaptics.micStop(haptic, context)
                                                speechRecognizer.stopListening()
                                                isListening = false
                                                speechPartialText = ""
                                                audioRmsDb = 0f
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "إلغاء",
                                                color = TextMuted,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = Cairo
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                KatzuHaptics.micStop(haptic, context)
                                                speechRecognizer.stopListening()
                                                isListening = false
                                                audioRmsDb = 0f
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                            shape = RoundedCornerShape(9999.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = "تم التحدث",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                fontFamily = Cairo
                                            )
                                        }
                                    }
                                }

                                // Dynamic animated voice wave equalizer bars with harmonic oscillation
                                val infiniteWaveTransition = rememberInfiniteTransition(label = "wave_oscillator")
                                val wavePulsePhase by infiniteWaveTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 6.283f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(850, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "wave_phase"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val waveMultipliers = listOf(0.45f, 0.75f, 1.25f, 1.7f, 2.0f, 1.7f, 1.25f, 0.75f, 0.45f)
                                    for (i in 0 until 9) {
                                        val phase = wavePulsePhase + (i * 0.62f)
                                        val sineA = kotlin.math.sin(phase.toDouble()).toFloat() * 0.5f + 0.5f
                                        val sineB = kotlin.math.cos((phase * 1.5).toDouble()).toFloat() * 0.5f + 0.5f
                                        val harmonicWave = (sineA * 0.65f + sineB * 0.35f)
                                        val dynamicEnergy = (animatedRms / 8f).coerceIn(0f, 1f)
                                        val rawHeight = 10f + (harmonicWave * 15f * waveMultipliers[i]) + (dynamicEnergy * 20f * waveMultipliers[i])
                                        val barHeight = rawHeight.coerceIn(8f, 38.dp.value).dp

                                        Box(
                                            modifier = Modifier
                                                .width(5.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color(0xFFC9A8FF),
                                                            Primary,
                                                            Color(0xFFF5B8E0)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                if (speechPartialText.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceCardSubtle,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "« $speechPartialText »",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                            ),
                                            color = Color(0xFFC9A8FF),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Banner when conversation target reached
                    AnimatedVisibility(
                        visible = isConversationCompleted && !allowBonusChat,
                        enter = fadeIn(animationSpec = tween(250)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceCardSubtle,
                            border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "اكتملت جولات الحوار المستهدفة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary,
                                        fontFamily = Cairo
                                    )
                                }
                                Button(
                                    onClick = { finishAndSaveSession() },
                                    shape = RoundedCornerShape(9999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "عرض التقرير 📊",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                        color = TextPrimary,
                                        fontFamily = Cairo
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // SINGLE HINT BUTTON: The Lamp Icon toggles single suggestion instantly
                        IconButton(
                            onClick = {
                                KatzuHaptics.tick(haptic, context)
                                showInlineSuggestionCard = !showInlineSuggestionCard
                                if (showInlineSuggestionCard) {
                                    currentTurnViewedOrUsedHint = true
                                    // Hints are pre-loaded eagerly as soon as the AI replies.
                                    // Fallback only if no hints or suggestions are available at all.
                                    val latestKatzu = messages.lastOrNull { it.sender == MessageSender.Katzu }
                                    if (latestKatzu != null && hintsForKatzuMessageId != latestKatzu.id && unusedStarterSuggestions.isEmpty() && !isGeneratingHints) {
                                        refreshHints(latestKatzu.id)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (showInlineSuggestionCard) Primary.copy(alpha = 0.22f) else SurfaceCardSubtle)
                                .testTag("conversation_hint_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "تلميحات مساعدة",
                                tint = if (showInlineSuggestionCard) Primary else StatusLearning,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Inline Chat Text Field (Pure LTR German layout for natural left-aligned typing)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = userDraftInput,
                                onValueChange = {
                                    userDraftInput = it
                                    if (speechNoticeMessage != null) {
                                        speechNoticeMessage = null
                                    }
                                    if (isConversationCompleted && !allowBonusChat) {
                                        allowBonusChat = true
                                    }
                                },
                                enabled = !isWaitingForKatzu,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("conversation_text_input_field"),
                                placeholder = {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = if (isConversationCompleted && !allowBonusChat) "اكتمل الحوار! انقر هنا للمتابعة أو راجع التقرير..." else if (isWaitingForKatzu) "كاتزو يفكّر في الرد..." else if (isListening) "تحدث الآن بالألمانية..." else "اكتب ردك بالألمانية...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                            color = if (isWaitingForKatzu) Primary.copy(alpha = 0.8f) else if (isListening) Primary else TextMuted,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceCard,
                                    unfocusedContainerColor = SurfaceCard,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = if (isListening) Primary else BorderSubtle,
                                    cursorColor = Primary
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Start,
                                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                ),
                                trailingIcon = {
                                    if (userDraftInput.isNotEmpty()) {
                                        IconButton(onClick = { userDraftInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // Mic Button with animated pulse when active
                        val infiniteTransition = rememberInfiniteTransition(label = "mic_transition")
                        val micPulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.22f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "mic_scale"
                        )

                        Box(contentAlignment = Alignment.Center) {
                            if (isListening) {
                                val energyBoost = (animatedRms / 8f).coerceIn(0f, 1f)
                                // Outer radiating soundwave ring 1
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .graphicsLayer(
                                            scaleX = micPulseScale * (1.15f + energyBoost * 0.35f),
                                            scaleY = micPulseScale * (1.15f + energyBoost * 0.35f),
                                            alpha = (0.35f - (energyBoost * 0.1f)).coerceAtLeast(0.12f)
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFC9A8FF).copy(alpha = 0.5f),
                                                    Primary.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                // Mid soundwave ripple ring 2
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .graphicsLayer(
                                            scaleX = micPulseScale * (1.06f + energyBoost * 0.2f),
                                            scaleY = micPulseScale * (1.06f + energyBoost * 0.2f),
                                            alpha = 0.6f
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFF5B8E0).copy(alpha = 0.5f),
                                                    Primary.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isWaitingForKatzu) SurfaceCardSubtle.copy(alpha = 0.5f)
                                        else if (isListening) StatusError
                                        else if (userDraftInput.isBlank()) Primary
                                        else SurfaceCardSubtle
                                    )
                                    .clickable(enabled = !isWaitingForKatzu) {
                                        if (isConversationCompleted && !allowBonusChat) {
                                            allowBonusChat = true
                                        }
                                        onStopSpeak()
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@clickable
                                        }
                                        if (isListening) {
                                            KatzuHaptics.micStop(haptic, context)
                                            speechRecognizer.stopListening()
                                            isListening = false
                                            audioRmsDb = 0f
                                        } else {
                                            startListeningFlow()
                                        }
                                    }
                                    .testTag("conversation_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                    contentDescription = if (isListening) "إيقاف الاستماع" else "تحدث بالصوت",
                                    tint = if (isWaitingForKatzu) TextMuted else if (isListening || userDraftInput.isBlank()) TextPrimary else Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Send Button (appears when input is ready)
                        if (userDraftInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (isWaitingForKatzu) return@IconButton
                                    val textToSend = userDraftInput.trim()
                                    if (textToSend.isNotEmpty()) {
                                        KatzuHaptics.press(haptic)
                                        sendUserMessage(textToSend)
                                        userDraftInput = ""
                                    }
                                },
                                enabled = !isWaitingForKatzu,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isWaitingForKatzu) Primary.copy(alpha = 0.5f) else Primary)
                                    .testTag("conversation_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = if (isWaitingForKatzu) TextMuted else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExitConfirmDialog) {
            val userCount = messages.count { it.sender == MessageSender.User }
            val isShortSession = userCount <= 1
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                containerColor = SurfaceCard,
                icon = {
                    Image(
                        painter = painterResource(id = R.drawable.katzu_peace),
                        contentDescription = "كاتزو يودعك",
                        modifier = Modifier.size(76.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                title = {
                    Text(
                        text = if (isShortSession) "المغادرة الآن؟" else "إنهاء المحادثة؟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = if (isShortSession) {
                            "بدأت للتو! يمكنك المغادرة الآن والعودة متى شئت لمتابعة هذه المحادثة مع كاتزو."
                        } else {
                            "لقد تحدثت بـ $userCount جملة في هذه الجلسة. هل ترغب في إنهاء الجلسة وحفظ نتيجتك في سجل التقدّم، أم المتابعة؟"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitConfirmDialog = false
                            finishAndSaveSession()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("إنهاء وحفظ النتيجة", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                showExitConfirmDialog = false
                                onBack()
                            }
                        ) {
                            Text("خروج بدون حفظ", color = TextSecondary)
                        }
                        TextButton(
                            onClick = { showExitConfirmDialog = false }
                        ) {
                            Text("متابعة الحديث", color = TextPrimary)
                        }
                    }
                }
            )
        }

        // Hints Bottom Sheet (instant, zero AI calls, uses starter_phrases)
        if (showHintsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHintsBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = BackgroundPure,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(TextMuted.copy(alpha = 0.4f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = StatusLearning,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "تلميحات وعبارات مساعدة",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontFamily = Cairo,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { refreshHints() },
                                enabled = !isGeneratingHints,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث التلميحات وفق المحادثة",
                                    tint = if (isGeneratingHints) TextMuted else Primary
                                )
                            }
                            IconButton(
                                onClick = { showHintsBottomSheet = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    if (isGeneratingHints) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCardSubtle,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "كاتزو يبتكر تلميحات ذكية تناسب مجريات المحادثة...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryFixed,
                                    fontFamily = Cairo
                                )
                            }
                        }
                    }

                    val hasContextual = activeHints.any { it.isContextual }

                    if (primarySuggestedPhrase != null) {
                        Text(
                            text = if (hasContextual) {
                                "الرد المقترح الأنسب للرد الأخير (اضغط لوضعه في حقل الرد):"
                            } else {
                                "الرد الأنسب للبداية (اضغط لوضعه في حقل الرد):"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = Cairo
                        )

                        // 1. Prominent Primary Suggestion Card (Lowest sort_order unused)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceCard,
                            border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            val isPrimarySheetPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == primarySuggestedPhrase.german
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = StatusLearning.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = if (primarySuggestedPhrase.isContextual) "⭐ المقترح الأنسب الآن" else "⭐ الاقتراح الأسهل للبداية",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusLearning,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = Cairo,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    KaraokeGermanText(
                                        text = primarySuggestedPhrase.german,
                                        isPlaying = isPrimarySheetPlaying,
                                        charRange = ttsPlaybackState.charRange,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )

                                    if (primarySuggestedPhrase.translationAr.isNotBlank()) {
                                        Text(
                                            text = primarySuggestedPhrase.translationAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontFamily = Cairo
                                        )
                                    }

                                    Text(
                                        text = "تدرّب على قراءتها بصوتك 🎙️ أو كتابتها ✍️",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextMuted,
                                        fontFamily = Cairo
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isPrimarySheetPlaying) {
                                            onStopSpeak()
                                        } else {
                                            onSpeak(primarySuggestedPhrase.german, 1.0f)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceCardSubtle)
                                ) {
                                    Icon(
                                        imageVector = if (isPrimarySheetPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = if (isPrimarySheetPlaying) "إيقاف النطق" else "استمع للعبارة",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 2. "عرض المزيد من الخيارات" text link below it
                        if (otherUnusedPhrases.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = { showMoreSheetHints = !showMoreSheetHints },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (showMoreSheetHints) "إخفاء الخيارات الإضافية" else "عرض المزيد من الخيارات (${otherUnusedPhrases.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryFixed,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Cairo
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (showMoreSheetHints) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PrimaryFixed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 3. Revealed unused starter phrases / suggestions
                            AnimatedVisibility(
                                visible = showMoreSheetHints,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    otherUnusedPhrases.forEach { hint ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = SurfaceCard,
                                            border = BorderStroke(
                                                1.dp,
                                                if (hint.isContextual) Primary.copy(alpha = 0.35f) else BorderSubtle
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                        ) {
                                            val isItemPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == hint.german
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        KaraokeGermanText(
                                                            text = hint.german,
                                                            isPlaying = isItemPlaying,
                                                            charRange = ttsPlaybackState.charRange,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = TextPrimary
                                                        )
                                                        if (hint.isContextual) {
                                                            Surface(
                                                                shape = RoundedCornerShape(9999.dp),
                                                                color = Primary.copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = "مقترح ذكي",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = PrimaryFixed,
                                                                    fontSize = 10.sp,
                                                                    fontFamily = Cairo,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (hint.translationAr.isNotBlank()) {
                                                        Text(
                                                            text = hint.translationAr,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary,
                                                            fontFamily = Cairo
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (isItemPlaying) {
                                                                onStopSpeak()
                                                            } else {
                                                                onSpeak(hint.german, 1.0f)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(SurfaceCardSubtle)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isItemPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                                            contentDescription = if (isItemPlaying) "إيقاف النطق" else "استمع للعبارة",
                                                            tint = Primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = Primary.copy(alpha = 0.15f),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(9999.dp))
                                                            .clickable {
                                                                val idx = unusedStarterSuggestions.indexOf(hint)
                                                                if (idx >= 0) {
                                                                    activeSelectedSuggestionIndex = idx
                                                                }
                                                                showHintsBottomSheet = false
                                                                showInlineSuggestionCard = true
                                                            }
                                                    ) {
                                                        Text(
                                                            text = "اختر كمرجع",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                            color = Primary,
                                                            fontFamily = Cairo,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!isGeneratingHints) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "تم استخدام المقترحات السابقة. يمكنك توليد مقترحات ذكية جديدة تناسب رد الطرف الآخر الأخير:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = Cairo,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        val latestKatzu = messages.lastOrNull { it.sender == MessageSender.Katzu }
                                        refreshHints(latestKatzu?.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = RoundedCornerShape(9999.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "💡 توليد مقترحات ذكية الآن",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary,
                                        fontFamily = Cairo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KaraokeGermanText(
    text: String,
    isPlaying: Boolean,
    charRange: Pair<Int, Int>?,
    style: androidx.compose.ui.text.TextStyle,
    color: Color = TextPrimary,
    onWordClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val satoshiStyle = style.copy(
        fontFamily = Satoshi,
        fontWeight = if (style.fontWeight != null && style.fontWeight!! >= FontWeight.SemiBold) style.fontWeight else FontWeight.Bold,
        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
    )

    val annotatedString = remember(text, isPlaying, charRange) {
        buildAnnotatedString {
            val regex = Regex("\\S+")
            val wordMatches = regex.findAll(text).toList()

            var activeSpanStart = -1
            var activeSpanEnd = -1

            if (isPlaying && charRange != null && text.isNotEmpty() && wordMatches.isNotEmpty()) {
                val safeStart = charRange.first.coerceIn(0, text.length)
                val safeEnd = charRange.second.coerceIn(safeStart, text.length)

                val matchedWord = wordMatches.firstOrNull { match ->
                    val mStart = match.range.first
                    val mEnd = match.range.last + 1
                    maxOf(mStart, safeStart) < minOf(mEnd, maxOf(safeEnd, safeStart + 1))
                } ?: wordMatches.minByOrNull { match ->
                    kotlin.math.abs(match.range.first - safeStart)
                }

                if (matchedWord != null) {
                    activeSpanStart = matchedWord.range.first
                    activeSpanEnd = matchedWord.range.last + 1
                }
            }

            var lastIndex = 0
            for (match in wordMatches) {
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1
                if (matchStart > lastIndex) {
                    append(text.substring(lastIndex, matchStart))
                }
                val rawToken = match.value
                val cleanWord = rawToken.trim().trim(',', '.', '!', '?', '"', '«', '»', ':', ';', '(', ')', '[', ']', '{', '}')
                pushStringAnnotation(tag = "WORD", annotation = cleanWord)
                append(rawToken)
                pop()
                lastIndex = matchEnd
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }

            if (activeSpanEnd > activeSpanStart && activeSpanStart >= 0) {
                addStyle(
                    SpanStyle(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Satoshi,
                        background = Primary.copy(alpha = 0.55f)
                    ),
                    start = activeSpanStart,
                    end = activeSpanEnd
                )
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (onWordClick != null) {
            ClickableText(
                text = annotatedString,
                style = satoshiStyle.copy(color = color),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "WORD", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            if (annotation.item.isNotBlank()) {
                                onWordClick(annotation.item)
                            }
                        }
                },
                modifier = modifier
            )
        } else {
            Text(
                text = annotatedString,
                style = satoshiStyle,
                color = color,
                textAlign = TextAlign.Start,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MiniAudioVisualizer(
    modifier: Modifier = Modifier,
    color: Color = Primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_wave")
    val heights = listOf(6.dp, 16.dp, 10.dp)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, targetH ->
            val h by infiniteTransition.animateValue(
                initialValue = 4.dp,
                targetValue = targetH,
                typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 260 + index * 80, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "h_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun KatzuMessageBubble(
    message: ChatMessage,
    showTranslation: Boolean,
    avatarRes: Int = R.drawable.katzu_barista,
    ttsPlaybackState: com.example.katzu.data.TtsPlaybackState = com.example.katzu.data.TtsPlaybackState(),
    onSpeak: (String, Float) -> Unit,
    onStopSpeak: () -> Unit = {},
    onWordClick: ((String) -> Unit)? = null
) {
    val isCurrentlyPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == message.germanText
    val isNormalPlaying = isCurrentlyPlaying && ttsPlaybackState.speed == 1.0f
    val isSlowPlaying = isCurrentlyPlaying && ttsPlaybackState.speed == 0.75f
    var isTranslationRevealedLocally by remember { mutableStateOf(false) }
    val isTranslationVisible = showTranslation || isTranslationRevealedLocally
    val isGenerating = message.isGenerating || message.germanText.endsWith("...") || message.germanText.isBlank()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceCardSubtle)
                .border(
                    1.dp,
                    if (isCurrentlyPlaying) Primary else Primary.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "Katzu",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Coach bubble with distinct visual identity: subtle purple brand tint and purple rim (Item 5)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF201A30),
                border = BorderStroke(
                    1.dp,
                    if (isCurrentlyPlaying) Primary else Primary.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    KaraokeGermanText(
                        text = message.germanText,
                        isPlaying = isCurrentlyPlaying,
                        charRange = ttsPlaybackState.charRange,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextPrimary,
                        onWordClick = onWordClick
                    )

                    AnimatedVisibility(visible = isTranslationVisible && message.arabicTranslation.isNotBlank()) {
                        Text(
                            text = message.arabicTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            fontFamily = Cairo
                        )
                    }

                    // Gated playback controls behind generation-complete state (Item 8)
                    if (isGenerating) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "كاتزو يجهّز الرد والصوت...",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontFamily = Cairo
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isNormalPlaying) {
                                        onStopSpeak()
                                    } else {
                                        onSpeak(message.germanText, 1.0f)
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNormalPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isNormalPlaying) "إيقاف" else "نطق",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isSlowPlaying) {
                                        onStopSpeak()
                                    } else {
                                        onSpeak(message.germanText, 0.75f)
                                    }
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSlowPlaying) Icons.Default.Stop else Icons.Outlined.Speed,
                                    contentDescription = if (isSlowPlaying) "إيقاف" else "نطق بطيء",
                                    tint = if (isSlowPlaying) Primary else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Inline per-message translation toggle button (Item 6)
                            if (message.arabicTranslation.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isTranslationVisible) Primary.copy(alpha = 0.15f) else SurfaceCardSubtle,
                                    border = BorderStroke(0.5.dp, if (isTranslationVisible) Primary.copy(alpha = 0.4f) else BorderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isTranslationRevealedLocally = !isTranslationRevealedLocally }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Translate,
                                            contentDescription = null,
                                            tint = if (isTranslationVisible) Primary else TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isTranslationVisible) "إخفاء" else "ترجمة",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = if (isTranslationVisible) Primary else TextSecondary,
                                            fontFamily = Cairo
                                        )
                                    }
                                }
                            }

                            if (isCurrentlyPlaying) {
                                MiniAudioVisualizer(
                                    color = Primary,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(
    message: ChatMessage,
    showTranslation: Boolean,
    ttsPlaybackState: com.example.katzu.data.TtsPlaybackState = com.example.katzu.data.TtsPlaybackState(),
    onSpeak: (String, Float) -> Unit,
    onStopSpeak: () -> Unit = {},
    onUseCorrection: (String) -> Unit,
    onWordClick: ((String) -> Unit)? = null
) {
    val isCurrentlyPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == message.germanText
    val isNormalPlaying = isCurrentlyPlaying && ttsPlaybackState.speed == 1.0f
    val isSlowPlaying = isCurrentlyPlaying && ttsPlaybackState.speed == 0.75f
    var isExplanationExpanded by remember { mutableStateOf(false) }
    var isUserTranslationRevealed by remember { mutableStateOf(false) }
    val isUserTranslationVisible = showTranslation || isUserTranslationRevealed
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // User speech bubble: pure dark #1A1826 with neutral border (Item 5)
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
            color = SurfaceCard,
            border = BorderStroke(
                1.dp,
                if (isCurrentlyPlaying) Primary else BorderSubtle
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                KaraokeGermanText(
                    text = message.germanText,
                    isPlaying = isCurrentlyPlaying,
                    charRange = ttsPlaybackState.charRange,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = TextPrimary,
                    onWordClick = onWordClick
                )

                AnimatedVisibility(visible = isUserTranslationVisible && message.arabicTranslation.isNotBlank()) {
                    Text(
                        text = message.arabicTranslation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                        fontFamily = Cairo
                    )
                }

                // Positive reinforcement card with Katzu thumbs-up sticker (Item 4)
                if (!message.hasCorrection && message.positiveNoteAr.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusSuccess.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("positive_reinforcement_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_thumbs_up),
                                contentDescription = "أحسنت",
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "ممتاز جداً! 👏",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = StatusSuccess,
                                    fontFamily = Cairo
                                )
                                Text(
                                    text = message.positiveNoteAr,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = TextPrimary,
                                    fontFamily = Cairo
                                )
                            }
                        }
                    }
                }

                // Voice audio playback buttons + translation toggle
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isNormalPlaying) {
                                onStopSpeak()
                            } else {
                                onSpeak(message.germanText, 1.0f)
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isNormalPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isNormalPlaying) "إيقاف" else "نطق عادي",
                            tint = Primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isSlowPlaying) {
                                onStopSpeak()
                            } else {
                                onSpeak(message.germanText, 0.75f)
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isSlowPlaying) Icons.Default.Stop else Icons.Outlined.Speed,
                            contentDescription = if (isSlowPlaying) "إيقاف" else "نطق بطيء",
                            tint = if (isSlowPlaying) Primary else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (message.arabicTranslation.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isUserTranslationVisible) Primary.copy(alpha = 0.15f) else SurfaceCardSubtle,
                            border = BorderStroke(0.5.dp, if (isUserTranslationVisible) Primary.copy(alpha = 0.4f) else BorderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isUserTranslationRevealed = !isUserTranslationRevealed }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Translate,
                                    contentDescription = null,
                                    tint = if (isUserTranslationVisible) Primary else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isUserTranslationVisible) "إخفاء" else "ترجمة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = if (isUserTranslationVisible) Primary else TextSecondary,
                                    fontFamily = Cairo
                                )
                            }
                        }
                    }

                    if (isCurrentlyPlaying) {
                        MiniAudioVisualizer(
                            color = Primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // Smart Correction Card (Clean, concise, and immediately clear)
        if (message.hasCorrection) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StatusError.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("correction_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header: Quick status badge with Katzu practice sticker (Item 4) + Clickable grammar rule pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_practice),
                                contentDescription = "كاتزو يصحح",
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    text = "تصحيح سريع",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = StatusError,
                                    fontFamily = Cairo
                                )
                                if (message.grammarRule.isNotBlank()) {
                                    Text(
                                        text = message.grammarRule,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = TextSecondary,
                                        fontFamily = Cairo
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isExplanationExpanded) StatusError.copy(alpha = 0.25f) else SurfaceCardSubtle,
                            border = BorderStroke(1.dp, if (isExplanationExpanded) StatusError else BorderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .clickable { isExplanationExpanded = !isExplanationExpanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isExplanationExpanded) "إغلاق التوضيح" else "شرح كاتزو",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isExplanationExpanded) StatusError else TextSecondary,
                                    fontFamily = Cairo
                                )
                                Icon(
                                    imageVector = if (isExplanationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = if (isExplanationExpanded) StatusError else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Direct comparison block: Mistake vs Correction
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Struck-through mistake with LTR isolation (Rule 8)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusError
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = isolateGerman(message.originalMistake.ifBlank { message.germanText }),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Satoshi,
                                        textDecoration = TextDecoration.LineThrough,
                                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                    ),
                                    color = StatusError.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Correct German with one-tap listen & copy
                        val isCorrectedPlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == message.correctedGerman
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = StatusSuccess
                            )
                            KaraokeGermanText(
                                text = message.correctedGerman,
                                isPlaying = isCorrectedPlaying,
                                charRange = ttsPlaybackState.charRange,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                ),
                                color = StatusSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (isCorrectedPlaying) {
                                        onStopSpeak()
                                    } else {
                                        onSpeak(message.correctedGerman, 1.0f)
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCorrectedPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = "استمع للصواب",
                                    tint = if (isCorrectedPlaying) Primary else StatusSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.correctedGerman))
                                    Toast.makeText(context, "تم نسخ الجملة الصحيحة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "نسخ",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Expandable dry Arabic Katzu explanation
                    AnimatedVisibility(
                        visible = isExplanationExpanded && message.roastComment.isNotBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundPure.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "💡", fontSize = 12.sp)
                                Text(
                                    text = message.roastComment,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = TextSecondary,
                                    fontFamily = Cairo
                                )
                            }
                        }
                    }

                    // Clear action button with 48dp min height and distinct styling (Item 7)
                    Button(
                        onClick = { onUseCorrection(message.correctedGerman) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF261D3B),
                            contentColor = Primary
                        ),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("use_correction_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "جرّب هذه الصياغة في ردك الآن",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Primary,
                                fontFamily = Cairo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KatzuThinkingWave(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "katzu_wave")
    val dotCount = 3
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until dotCount) {
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = i * 130, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_offset_$i"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = i * 130, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        this.alpha = alpha
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFC9A8FF), Color(0xFF8B6FE8))
                        )
                    )
            )
        }
    }
}

data class ConversationMission(
    val id: Int,
    val titleAr: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

