package com.example.katzu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.data.ScenarioEntity
import com.example.katzu.data.VocabularyEntity
import com.example.katzu.ui.components.bouncyClickable
import com.example.katzu.ui.theme.*
import com.example.katzu.util.KatzuHaptics

data class QuizQuestion(
    val word: VocabularyEntity,
    val options: List<String>,
    val correctOption: String
)

@Composable
fun QuizScreen(
    scenario: ScenarioEntity?,
    scenarioVocabulary: List<VocabularyEntity>,
    allLevelVocabulary: List<VocabularyEntity>,
    onBack: () -> Unit,
    onFinishQuiz: (score: Int) -> Unit,
    onSpeak: (String, Float) -> Unit
) {
    // Generate 5 questions
    val questions = remember(scenarioVocabulary, allLevelVocabulary) {
        val pool = if (scenarioVocabulary.isNotEmpty()) scenarioVocabulary else allLevelVocabulary
        val targetWords = pool.shuffled().take(5).let { list ->
            if (list.size < 5 && allLevelVocabulary.size >= 5) {
                (list + allLevelVocabulary.filter { it !in list }.shuffled().take(5 - list.size)).distinctBy { it.id }
            } else list
        }

        val allDistinctTranslations = (allLevelVocabulary + scenarioVocabulary)
            .map { it.translation_ar.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        targetWords.map { targetWord ->
            val wrongOptions = allDistinctTranslations
                .filter { it != targetWord.translation_ar.trim() }
                .shuffled()
                .take(3)

            val allOptions = (wrongOptions + targetWord.translation_ar.trim()).distinct().shuffled()

            QuizQuestion(
                word = targetWord,
                options = allOptions,
                correctOption = targetWord.translation_ar
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isQuizCompleted by remember { mutableStateOf(false) }
    val missedQuestions = remember { mutableStateListOf<QuizQuestion>() }

    BackHandler(onBack = onBack)

    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        containerColor = BackgroundPure,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundPure)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .testTag("quiz_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }

                    // Progress Badge
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isQuizCompleted) StatusSuccess else StatusLearning)
                            )
                            Text(
                                text = if (!isQuizCompleted) "السؤال ${currentIndex + 1} من ${questions.size}" else "النتيجة النهائية",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(modifier = Modifier.size(42.dp))
                }

                if (!isQuizCompleted && questions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Discrete Segmented Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        questions.forEachIndexed { index, _ ->
                            val isPast = index < currentIndex
                            val isCurrent = index == currentIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(
                                        when {
                                            isPast -> Primary
                                            isCurrent && isAnswerSubmitted -> Primary
                                            isCurrent -> Primary.copy(alpha = 0.55f)
                                            else -> SurfaceCardSubtle
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = BackgroundPure,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (isQuizCompleted) {
                        // Finished CTAs: Proceed to Conversation or Retake Quiz
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    KatzuHaptics.press(haptic, context)
                                    currentIndex = 0
                                    selectedOption = null
                                    isAnswerSubmitted = false
                                    score = 0
                                    missedQuestions.clear()
                                    isQuizCompleted = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("quiz_retake_btn"),
                                shape = RoundedCornerShape(9999.dp),
                                border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text(
                                    text = "إعادة الاختبار",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    KatzuHaptics.press(haptic, context)
                                    onFinishQuiz(score)
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(9999.dp),
                                        ambientColor = Primary.copy(alpha = 0.3f),
                                        spotColor = Primary.copy(alpha = 0.5f)
                                    )
                                    .testTag("quiz_proceed_conversation_btn"),
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "المحادثة الحية",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else if (isAnswerSubmitted) {
                        // Next Question CTA
                        Button(
                            onClick = {
                                KatzuHaptics.press(haptic, context)
                                if (currentIndex + 1 < questions.size) {
                                    currentIndex++
                                    selectedOption = null
                                    isAnswerSubmitted = false
                                } else {
                                    isQuizCompleted = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_next_question_btn"),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(
                                text = if (currentIndex + 1 < questions.size) "السؤال التالي" else "عرض النتيجة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isQuizCompleted) {
            // --- Quiz Score & Summary View with Motivating Tone & Missed Words Review ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Katzu Mascot Sticker (Placed directly on AMOLED black)
                val katzuResultMascot = remember(score, questions.size) {
                    when {
                        score == questions.size || score >= 4 -> R.drawable.katzu_celebrating
                        score >= 3 -> R.drawable.katzu_thumbs_up
                        score >= 2 -> R.drawable.katzu_peace
                        else -> R.drawable.katzu_practice
                    }
                }

                Image(
                    painter = painterResource(id = katzuResultMascot),
                    contentDescription = "كاتزو في نتيجة الاختبار",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "اكتمل الاختبار!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                // Stats Overview Row
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score/${questions.size}",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (score >= 4) StatusSuccess else if (score >= 2) StatusLearning else StatusError,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الإجابات الصحيحة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(36.dp),
                            color = BorderSubtle
                        )

                        val accuracyPercent = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$accuracyPercent%",
                                style = MaterialTheme.typography.titleLarge,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نسبة الدقة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Katzu Motivating Sassy/Encouraging Commentary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "كاتزو يهمس لك:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        val katzuComment = when {
                            score == questions.size -> "“رائع جداً! حتى أنا كقط مدقق لم أجد أي خطأ. أنت مستعد تماماً للتحدث بثقة.”"
                            score >= 3 -> "“أداء جميل! لديك أساس متين، وسنصقل هذه الكلمات عملياً مع شخصية السيناريو الآن.”"
                            else -> "“بداية طيبة! الأخطاء هي أسرع طريقة للتعلم. راجع الكلمات أدناه ثم انطلق للمحادثة لتجربتها عملياً.”"
                        }
                        Text(
                            text = katzuComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Missed Words Review Section (if any)
                if (missedQuestions.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "كلمات للمراجعة السريعة (${missedQuestions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            missedQuestions.forEach { missed ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCardSubtle,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (missed.word.article.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(9999.dp),
                                                        color = Primary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = missed.word.article,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Primary,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = missed.word.german,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = missed.correctOption,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusSuccess
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val fullGerman = if (missed.word.article.isNotBlank()) "${missed.word.article} ${missed.word.german}" else missed.word.german
                                                onSpeak(fullGerman, 1.0f)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "استمع للنطق",
                                                tint = Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (currentQuestion != null) {
            // --- Active Quiz Question ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question Header Counter with Katzu Mascot
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
                            painter = painterResource(id = R.drawable.katzu_scenario_host),
                            contentDescription = "كاتزو يختبرك",
                            modifier = Modifier.size(38.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column {
                            Text(
                                text = "السؤال ${currentIndex + 1} من ${questions.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اختر المعنى العربي الصحيح",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "${((currentIndex.toFloat() / questions.size.coerceAtLeast(1)) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // German Word Hero Card (Cairo Font, no Serif)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val word = currentQuestion.word
                        val articleColor = when (word.article.lowercase().trim()) {
                            "der" -> ArticleDer
                            "die" -> ArticleDie
                            "das" -> ArticleDas
                            else -> Primary
                        }

                        if (word.article.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = articleColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, articleColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = word.article.uppercase(),
                                    color = articleColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Word Display with Cairo font everywhere
                        Text(
                            text = if (word.article.isNotBlank()) "${word.article} ${word.german}" else word.german,
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        // Pronunciation Button
                        IconButton(
                            onClick = {
                                val fullGerman = if (word.article.isNotBlank()) "${word.article} ${word.german}" else word.german
                                onSpeak(fullGerman, 1.0f)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "استمع للنطق",
                                tint = Primary
                            )
                        }
                    }
                }

                // Options List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentQuestion.options.forEachIndexed { optionIndex, option ->
                        val isSelected = selectedOption == option
                        val isCorrect = option == currentQuestion.correctOption

                        val (cardColor, borderColor, textColor) = when {
                            !isAnswerSubmitted -> {
                                if (isSelected) Triple(SurfaceCardSubtle, Primary, TextPrimary)
                                else Triple(SurfaceCard, BorderSubtle, TextPrimary)
                            }
                            isCorrect -> Triple(StatusSuccess.copy(alpha = 0.15f), StatusSuccess, StatusSuccess)
                            isSelected && !isCorrect -> Triple(StatusError.copy(alpha = 0.15f), StatusError, StatusError)
                            else -> Triple(SurfaceCard, BorderSubtle, TextMuted)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isAnswerSubmitted) {
                                    selectedOption = option
                                    isAnswerSubmitted = true
                                    if (isCorrect) {
                                        KatzuHaptics.success(haptic, context)
                                        score++
                                    } else {
                                        KatzuHaptics.error(haptic, context)
                                        if (!missedQuestions.contains(currentQuestion)) {
                                            missedQuestions.add(currentQuestion)
                                        }
                                    }
                                }
                                .testTag("quiz_option_${optionIndex}"),
                            shape = RoundedCornerShape(16.dp),
                            color = cardColor,
                            border = BorderStroke(1.5.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (isAnswerSubmitted) {
                                    if (isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "إجابة صحيحة",
                                            tint = StatusSuccess,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إجابة خاطئة",
                                            tint = StatusError,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Explanation & Feedback Box when Answer is Submitted
                AnimatedVisibility(
                    visible = isAnswerSubmitted,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val isCorrect = selectedOption == currentQuestion.correctOption
                    val word = currentQuestion.word
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCorrect) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isCorrect) StatusSuccess.copy(alpha = 0.4f) else StatusError.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isCorrect) StatusSuccess else StatusError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isCorrect) "إجابة صحيحة! أحسنت." else "الإجابة الصحيحة هي: «${currentQuestion.correctOption}»",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCorrect) StatusSuccess else StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Show sentence example if available
                            if (word.example_de.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceCard,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "مثال في جملة:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = word.example_de,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (word.example_ar.isNotBlank()) {
                                            Text(
                                                text = word.example_ar,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            } else if (!isCorrect) {
                                Text(
                                    text = "تذكّر: ${if (word.article.isNotBlank()) "${word.article} " else ""}${word.german} تعني «${word.translation_ar}».",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // Empty questions fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "لا توجد أسئلة كافية للاختبار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "مفردات هذا السيناريو لا تزال قيد التحميل أو غير كافية لإنشاء 5 أسئلة. يمكنك المتابعة إلى المحادثة الحية مباشرة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = { onFinishQuiz(0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("المتابعة إلى المحادثة الحية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
