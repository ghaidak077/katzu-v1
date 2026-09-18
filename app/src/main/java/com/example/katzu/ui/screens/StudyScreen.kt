package com.example.katzu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.katzu.R
import com.example.katzu.data.DialogueAnswerOption
import com.example.katzu.data.DialogueTurn
import com.example.katzu.data.GrammarEntity
import com.example.katzu.data.ScenarioDialogueManager
import com.example.katzu.data.ScenarioEntity
import com.example.katzu.data.StarterPhraseEntity
import com.example.katzu.data.TtsPlaybackState
import com.example.katzu.data.VocabularyEntity
import com.example.katzu.ui.components.bouncyClickable
import com.example.katzu.ui.theme.*
import com.example.katzu.util.GermanSpeechRecognizer
import com.example.katzu.util.KatzuHaptics
import kotlinx.coroutines.launch

@Composable
fun StudyScreen(
    scenario: ScenarioEntity?,
    vocabularyList: List<VocabularyEntity>,
    grammarList: List<GrammarEntity>,
    starterPhrases: List<StarterPhraseEntity> = emptyList(),
    cefrLevel: String = "A1",
    onBack: () -> Unit,
    onProceedToQuiz: () -> Unit,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState = TtsPlaybackState(),
    onStopSpeak: () -> Unit = {}
) {
    var isSlowSpeed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                            .testTag("study_back_btn")
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
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
                                    .background(Primary)
                            )
                            Text(
                                text = "الخطوة 1 من 2 • دراسة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Slow TTS Toggle
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = if (isSlowSpeed) SecondaryContainer.copy(alpha = 0.5f) else SurfaceCardSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { isSlowSpeed = !isSlowSpeed }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = "سرعة الصوت",
                                tint = if (isSlowSpeed) Primary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isSlowSpeed) "0.75x" else "1.0x",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSlowSpeed) Primary else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scenario Title & Subtitle
                if (scenario != null) {
                    Text(
                        text = scenario.title_de,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scenario.title_ar,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = BackgroundPure,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onProceedToQuiz,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(9999.dp),
                                ambientColor = Primary.copy(alpha = 0.3f),
                                spotColor = Primary.copy(alpha = 0.5f)
                            )
                            .testTag("study_proceed_quiz_btn"),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = TextPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "متابعة إلى الاختبار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Katzu Coach Study Prompt (Sticker placed directly on AMOLED black with witty speech bubble)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.katzu_practice),
                    contentDescription = "كاتزو رفيق الدراسة",
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "نصيحة كاتزو قبل البدء",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "“احفظ أدوات التعريف (der / die / das) مع كل كلمة، وإلا ستجد نفسك في حيرة أثناء المحادثة!”",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // --- Section 1: Vocabulary Flashcards ---
            if (vocabularyList.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { vocabularyList.size })

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "مفردات السيناريو",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Page Counter
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = SurfaceCardSubtle
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${vocabularyList.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Swipeable Flashcard Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) { pageIndex ->
                        val word = vocabularyList[pageIndex]
                        VocabStudyCard(
                            word = word,
                            isSlowSpeed = isSlowSpeed,
                            onSpeak = onSpeak,
                            ttsPlaybackState = ttsPlaybackState,
                            onStopSpeak = onStopSpeak
                        )
                    }

                    // Pager Dot Indicators
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(vocabularyList.size.coerceAtMost(10)) { dotIndex ->
                            val isSelected = pagerState.currentPage == dotIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 18.dp else 6.dp, 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Primary else TextMuted.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // --- Section 2: Scenario Grammar Rules ---
            if (grammarList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StatusLearning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "قواعد وتراكيب مهمة",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    grammarList.forEach { rule ->
                        GrammarRuleCard(
                            rule = rule,
                            onSpeak = onSpeak,
                            ttsPlaybackState = ttsPlaybackState,
                            onStopSpeak = onStopSpeak
                        )
                    }
                }
            }

            // --- Section 3: Full Multi-Turn Chat Simulation Dialogue (Question Card & Flippable Answer Cards) ---
            DialogueTrainingSection(
                scenario = scenario,
                cefrLevel = cefrLevel,
                starterPhrases = starterPhrases,
                isSlowSpeed = isSlowSpeed,
                onSpeak = onSpeak,
                ttsPlaybackState = ttsPlaybackState,
                onStopSpeak = onStopSpeak
            )

            if (vocabularyList.isEmpty() && grammarList.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "جاهز للانطلاق مباشرة!",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لا توجد مفردات جديدة محددة لهذا السيناريو، يمكنك المتابعة للاختبار ثم المحادثة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Ready for quiz card with Katzu sticker directly on dark card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProceedToQuiz() },
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.katzu_thumbs_up),
                        contentDescription = "كاتزو مستعد للاختبار",
                        modifier = Modifier.size(68.dp),
                        contentScale = ContentScale.Fit
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "أنهيت مراجعة المفردات؟",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "اضغط هنا لاختبار ذاكرتك في كويز سريع لتثبيت ما تعلمته قبل المحادثة الحية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VocabStudyCard(
    word: VocabularyEntity,
    isSlowSpeed: Boolean,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState = TtsPlaybackState(),
    onStopSpeak: () -> Unit = {}
) {
    val articleColor = when (word.article.lowercase().trim()) {
        "der" -> ArticleDer
        "die" -> ArticleDie
        "das" -> ArticleDas
        else -> Primary
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card Top Row: Article badge + Speaker
            val fullGerman = if (word.article.isNotBlank()) "${word.article} ${word.german}" else word.german
            val isWordPlaying = ttsPlaybackState.isPlaying && (ttsPlaybackState.text == fullGerman || ttsPlaybackState.text == word.german)
            val isExamplePlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == word.example_de

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (word.article.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = articleColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, articleColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = word.article.uppercase(),
                            color = articleColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle
                    ) {
                        Text(
                            text = word.part_of_speech.ifBlank { "Vokabel" },
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Speaker Icon
                IconButton(
                    onClick = {
                        if (isWordPlaying) {
                            onStopSpeak()
                        } else {
                            onSpeak(fullGerman, if (isSlowSpeed) 0.75f else 1.0f)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardSubtle)
                ) {
                    Icon(
                        imageVector = if (isWordPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = if (isWordPlaying) "إيقاف النطق" else "استمع للنطق",
                        tint = Primary
                    )
                }
            }

            // Word German & Arabic
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // German Word
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (word.article.isNotBlank()) {
                        Text(
                            text = word.article,
                            style = MaterialTheme.typography.headlineMedium,
                            color = articleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = word.german,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (word.plural.isNotBlank()) {
                    Text(
                        text = "الجمع: ${word.plural}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                HorizontalDivider(
                    color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Arabic Meaning
                Text(
                    text = word.translation_ar,
                    style = MaterialTheme.typography.titleLarge,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Bold
                )
            }

            // Example Sentence Box
            if (word.example_de.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceCardSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            KaraokeGermanText(
                                text = word.example_de,
                                isPlaying = isExamplePlaying,
                                charRange = ttsPlaybackState.charRange,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            if (word.example_ar.isNotBlank()) {
                                Text(
                                    text = word.example_ar,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isExamplePlaying) {
                                    onStopSpeak()
                                } else {
                                    onSpeak(word.example_de, if (isSlowSpeed) 0.75f else 1.0f)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExamplePlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isExamplePlaying) "إيقاف المثال" else "نطق المثال",
                                tint = if (isExamplePlaying) Primary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarRuleCard(
    rule: GrammarEntity,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState = TtsPlaybackState(),
    onStopSpeak: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.title_ar,
                    style = MaterialTheme.typography.titleSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = SurfaceCardSubtle
                ) {
                    Text(
                        text = rule.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = rule.explanation_ar,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            if (rule.example_de.isNotBlank()) {
                val isExamplePlaying = ttsPlaybackState.isPlaying && ttsPlaybackState.text == rule.example_de
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KaraokeGermanText(
                            text = rule.example_de,
                            isPlaying = isExamplePlaying,
                            charRange = ttsPlaybackState.charRange,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (isExamplePlaying) {
                                    onStopSpeak()
                                } else {
                                    onSpeak(rule.example_de, 1.0f)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isExamplePlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isExamplePlaying) "إيقاف المثال" else "استمع للمثال",
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

private fun generateLinguisticDialogueTip(germanText: String): String {
    val text = germanText.trim()
    return when {
        text.contains("Ich möchte", ignoreCase = true) || text.contains("Ich hätte gerne", ignoreCase = true) ->
            "صيغة تهذيب رفيعة (Konjunktiv II): استخدام 'möchte' أو 'hätte gerne' أفضل بكثير من 'Ich will' المباشرة عند الطلب."
        text.contains("Könnten Sie", ignoreCase = true) || text.contains("Können Sie", ignoreCase = true) ->
            "صيغة طلب بأدب: يبدأ السؤال بالفعل المساعد 'Könnten/Können' ثم الضمير الرسمي 'Sie'، ويأتي الفعل الأساسي في نهاية الجملة."
        text.contains("Haben Sie", ignoreCase = true) ->
            "سؤال نعم/لا (Ja/Nein-Frage): يبدأ الفعل 'Haben' في الموقع الأول مباشرة يليه الفاعل."
        text.contains("Wie viel", ignoreCase = true) || text.contains("Was kostet", ignoreCase = true) ->
            "سؤال عن السعر: تذكر أن السؤال عن القيمة يبدأ بأداة الاستفهام W-Frage ثم الفعل مباشرة."
        text.contains("Wo ist", ignoreCase = true) || text.contains("Wo finde", ignoreCase = true) ->
            "سؤال عن المكان: أداة الاستفهام 'Wo' تطلب موقعاً محدداً، ويأتي الفعل في الرتبة الثانية."
        text.contains("Entschuldigung", ignoreCase = true) || text.contains("Entschuldigen Sie", ignoreCase = true) ->
            "افتتاحية لبقة: كلمة 'Entschuldigung' تجذب انتباه الطرف الآخر بأدب قبل طرح أي سؤال أو طلب."
        text.contains("Vielen Dank", ignoreCase = true) || text.contains("Danke schön", ignoreCase = true) ->
            "عبارة شكر وامتنان: ختام مهذب يترك أثراً إيجابياً في أي حوار رسمي أو يومي."
        text.contains("Sie", ignoreCase = false) || text.contains("Ihnen", ignoreCase = false) ->
            "مخاطبة رسمية (Siezen): استخدم الضمير بحرف كبير (Sie/Ihnen) للاحترام مع الموظفين أو الغرباء."
        text.contains("du", ignoreCase = true) || text.contains("dir", ignoreCase = true) ->
            "مخاطبة ودية (Duzen): استخدم (du/dir) فقط مع الأصدقاء أو في المحادثات العفوية غير الرسمية."
        text.contains("?") ->
            "نبرة الاستفهام: ارفع نبرة صوتك قليلاً عند نهاية السؤال لجعل مقصدك واضحاً للطرف الآخر."
        else ->
            "قاعدة الفعل في المركز الثاني (V2): في الجملة الخبرية الألمانية، يقع الفعل المصرف دائماً في الموقع الثاني."
    }
}

@Composable
private fun DialogueTrainingSection(
    scenario: ScenarioEntity?,
    cefrLevel: String,
    starterPhrases: List<StarterPhraseEntity>,
    isSlowSpeed: Boolean,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState,
    onStopSpeak: () -> Unit
) {
    val dialogueTurns = remember(scenario?.id, cefrLevel, starterPhrases) {
        ScenarioDialogueManager.getDialogueForScenario(scenario, cefrLevel, starterPhrases)
    }
    if (dialogueTurns.isEmpty()) return

    var currentTurnIndex by remember(scenario?.id, cefrLevel) { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCardFlipped by remember(currentTurnIndex, scenario?.id, cefrLevel) { mutableStateOf(false) }
    val practicedTurnNumbers = remember(scenario?.id, cefrLevel) { mutableStateListOf<Int>() }
    val practicedAnswerIds = remember(scenario?.id, cefrLevel) { mutableStateListOf<String>() }

    val currentTurn = dialogueTurns.getOrElse(currentTurnIndex) { dialogueTurns.first() }
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    val flipRotation by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dialogueCardFlip"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "تدريب محاكاة الحوار الكامل قبل الشات",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تدرّب على خطوات المحادثة جولة بجولة. اضغط على بطاقة السؤال لقلبها ومشاهدة خيارات الرد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Stepper: Conversation Turn Progress Bar & Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dialogueTurns.forEachIndexed { index, turn ->
                val isSelected = currentTurnIndex == index
                val isTurnPracticed = practicedTurnNumbers.contains(turn.turnNumber)

                Surface(
                    onClick = {
                        if (currentTurnIndex != index) {
                            currentTurnIndex = index
                            isCardFlipped = false
                        }
                    },
                    shape = RoundedCornerShape(9999.dp),
                    color = when {
                        isSelected -> Primary
                        isTurnPracticed -> StatusSuccess.copy(alpha = 0.18f)
                        else -> SurfaceCardSubtle
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isSelected -> Primary
                            isTurnPracticed -> StatusSuccess.copy(alpha = 0.5f)
                            else -> BorderSubtle
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTurnPracticed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else StatusSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = "الجولة ${turn.turnNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> Color.White
                                isTurnPracticed -> StatusSuccess
                                else -> TextSecondary
                            },
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 3D Flippable Question / Answer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing))
                .graphicsLayer {
                    rotationY = flipRotation
                    cameraDistance = 14f * density
                }
        ) {
            if (flipRotation <= 90f) {
                // FRONT: Counterpart Question / Speech Card
                DialogueQuestionCard(
                    turn = currentTurn,
                    onFlip = {
                        KatzuHaptics.cardFlip(haptic, context)
                        isCardFlipped = true
                    },
                    isSlowSpeed = isSlowSpeed,
                    onSpeak = onSpeak,
                    ttsPlaybackState = ttsPlaybackState,
                    onStopSpeak = onStopSpeak
                )
            } else {
                // BACK: Suggested Answers Card (flipped 180 deg to prevent mirroring)
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationY = 180f
                    }
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        DialogueAnswersCard(
                            turn = currentTurn,
                            practicedAnswerIds = practicedAnswerIds,
                            onAnswerPracticed = { answerId ->
                                KatzuHaptics.tick(haptic, context)
                                if (!practicedAnswerIds.contains(answerId)) {
                                    practicedAnswerIds.add(answerId)
                                }
                                if (!practicedTurnNumbers.contains(currentTurn.turnNumber)) {
                                    practicedTurnNumbers.add(currentTurn.turnNumber)
                                }
                            },
                            onFlipBack = {
                                KatzuHaptics.cardFlip(haptic, context)
                                isCardFlipped = false
                            },
                            isSlowSpeed = isSlowSpeed,
                            onSpeak = onSpeak,
                            ttsPlaybackState = ttsPlaybackState,
                            onStopSpeak = onStopSpeak
                        )
                    }
                }
            }
        }

        // Stepper Navigation: Strict RTL Flow (Right = Previous, Left = Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // In RTL: The right-most element is "Previous" (Going back to the right)
            OutlinedButton(
                onClick = {
                    if (currentTurnIndex > 0) {
                        currentTurnIndex--
                        isCardFlipped = false
                    }
                },
                enabled = currentTurnIndex > 0,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary,
                    disabledContentColor = TextMuted.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, if (currentTurnIndex > 0) BorderSubtle else Color.Transparent),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("dialogue_prev_turn_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // In RTL, ArrowBack points right →
                    contentDescription = "الجولة السابقة",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "السابق",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentTurnIndex > 0) Primary else TextMuted.copy(alpha = 0.3f)
                )
            }

            // Center: Turn progress pill
            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "جولة ${currentTurn.turnNumber} من ${dialogueTurns.size} • ${currentTurn.phaseTitleAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // In RTL: The left-most element is "Next" (Advancing forward to the left)
            Button(
                onClick = {
                    if (currentTurnIndex < dialogueTurns.size - 1) {
                        currentTurnIndex++
                        isCardFlipped = false
                    }
                },
                enabled = currentTurnIndex < dialogueTurns.size - 1,
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                    disabledContainerColor = SurfaceCardSubtle.copy(alpha = 0.5f),
                    disabledContentColor = TextMuted.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("dialogue_next_turn_button")
            ) {
                Text(
                    text = "التالي",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (currentTurnIndex < dialogueTurns.size - 1) Color.White else TextMuted.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward, // In RTL, ArrowForward points left ←
                    contentDescription = "الجولة التالية",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DialogueQuestionCard(
    turn: DialogueTurn,
    onFlip: () -> Unit,
    isSlowSpeed: Boolean,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState,
    onStopSpeak: () -> Unit
) {
    val isSpeaking = ttsPlaybackState.isPlaying && ttsPlaybackState.text == turn.partnerGerman

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 280.dp)
            .clickable { onFlip() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top Bar: Counterpart Persona & Flip Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Primary.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(17.dp)
                            )
                        }
                        Column {
                            Text(
                                text = turn.partnerPersona,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سؤال الطرف الآخر • ${turn.phaseTitleAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Flip Trigger Pill
                    Surface(
                        onClick = { onFlip() },
                        shape = RoundedCornerShape(9999.dp),
                        color = Primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اقلب للرد 🔄",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // German Question Speech Bubble
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardSubtle,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // German Text in strict LTR layout
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                KaraokeGermanText(
                                    text = turn.partnerGerman,
                                    isPlaying = isSpeaking,
                                    charRange = ttsPlaybackState.charRange,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = Satoshi
                                    ),
                                    color = TextPrimary
                                )
                            }

                            // Arabic Translation in strict RTL layout
                            if (turn.partnerArabic.isNotBlank()) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = turn.partnerArabic,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = Cairo,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 22.sp
                                        ),
                                        color = TextSecondary,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        FilledTonalIconButton(
                            onClick = {
                                if (isSpeaking) {
                                    onStopSpeak()
                                } else {
                                    onSpeak(turn.partnerGerman, if (isSlowSpeed) 0.75f else 1.0f)
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = Primary
                            ),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "إيقاف الصوت" else "استمع لسؤال الطرف الآخر",
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Flip Hint Banner
            Surface(
                onClick = { onFlip() },
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, StatusLearning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = StatusLearning,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "كيف سترد على هذا السؤال؟",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusLearning
                        )
                        Text(
                            text = "انقر لقلب البطاقة واستعراض (${turn.answers.size} ردود مقترحة) 🔄",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueAnswersCard(
    turn: DialogueTurn,
    practicedAnswerIds: List<String>,
    onAnswerPracticed: (String) -> Unit,
    onFlipBack: () -> Unit,
    isSlowSpeed: Boolean,
    onSpeak: (String, Float) -> Unit,
    ttsPlaybackState: TtsPlaybackState,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedAnswerIndex by remember(turn.turnNumber) { mutableIntStateOf(0) }
    val currentAnswer = turn.answers.getOrElse(selectedAnswerIndex) { turn.answers.first() }
    val isAnswerSpeaking = ttsPlaybackState.isPlaying && ttsPlaybackState.text == currentAnswer.german
    val isPracticed = practicedAnswerIds.contains(currentAnswer.id)

    // Speech Practice Engine State
    val speechRecognizer = remember { GermanSpeechRecognizer(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    var isListeningPractice by remember { mutableStateOf(false) }
    var spokenPracticeText by remember(selectedAnswerIndex, turn.turnNumber) { mutableStateOf("") }
    var practiceMatchScore by remember(selectedAnswerIndex, turn.turnNumber) { mutableIntStateOf(-1) }
    var practiceErrorMessage by remember(selectedAnswerIndex, turn.turnNumber) { mutableStateOf<String?>(null) }

    fun startListeningForAnswer() {
        onStopSpeak()
        isListeningPractice = true
        spokenPracticeText = ""
        practiceMatchScore = -1
        practiceErrorMessage = null
        KatzuHaptics.micStart(haptic, context)

        speechRecognizer.startListening(object : GermanSpeechRecognizer.Listener {
            override fun onReadyForSpeech() {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResult(partialText: String) {
                spokenPracticeText = partialText
            }
            override fun onFinalResult(recognizedText: String) {
                isListeningPractice = false
                KatzuHaptics.micStop(haptic, context)
                spokenPracticeText = recognizedText
                if (recognizedText.isNotBlank()) {
                    val score = evaluateSpokenMatch(recognizedText, currentAnswer.german)
                    practiceMatchScore = score
                    if (score >= 50) {
                        KatzuHaptics.success(haptic, context)
                        onAnswerPracticed(currentAnswer.id)
                    } else {
                        KatzuHaptics.error(haptic, context)
                    }
                } else {
                    practiceErrorMessage = "لم نلتقط صوتاً واضحاً، تكلّم بالقرب من الميكروفون مجدداً"
                }
            }
            override fun onError(errorMessageAr: String, errorCode: Int) {
                isListeningPractice = false
                KatzuHaptics.micStop(haptic, context)
                practiceErrorMessage = if (errorCode == android.speech.SpeechRecognizer.ERROR_NETWORK ||
                    errorCode == android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                ) {
                    "تعذر اتصال خدمة الصوت بالإنترنت. يمكنك إعادة المحاولة أو المتابعة للخطوة التالية."
                } else if (errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH
                ) {
                    "لم نلتقط صوتاً واضحاً، تكلّم بالقرب من الميكروفون مجدداً"
                } else {
                    errorMessageAr
                }
            }
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningForAnswer()
        } else {
            practiceErrorMessage = "يرجى منح إذن الميكروفون لتفعيل التدرّب الصوتي"
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header: Answer count & Flip Back Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StatusSuccess.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(15.dp)
                            )
                        }
                        Text(
                            text = "خيارات الرد المقترحة (${turn.answers.size} خيارات)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        onClick = {
                            if (isListeningPractice) {
                                speechRecognizer.stopListening()
                                isListeningPractice = false
                            }
                            onFlipBack()
                        },
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "العودة للسؤال 🔄",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Answer Options Segmented Tab Selector (If more than 1 option)
                if (turn.answers.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        turn.answers.forEachIndexed { index, ans ->
                            val isSelected = selectedAnswerIndex == index
                            val isAnsPracticed = practicedAnswerIds.contains(ans.id)

                            Surface(
                                onClick = {
                                    if (isListeningPractice) {
                                        speechRecognizer.stopListening()
                                        isListeningPractice = false
                                    }
                                    selectedAnswerIndex = index
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isSelected -> Primary
                                    isAnsPracticed -> StatusSuccess.copy(alpha = 0.18f)
                                    else -> SurfaceCardSubtle
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isSelected -> Primary
                                        isAnsPracticed -> StatusSuccess.copy(alpha = 0.45f)
                                        else -> BorderSubtle
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAnsPracticed) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else StatusSuccess,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = if (ans.badge.isNotBlank()) ans.badge else "رد ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = Cairo,
                                            fontSize = 11.sp
                                        ),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> Color.White
                                            isAnsPracticed -> StatusSuccess
                                            else -> TextSecondary
                                        },
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Selected Answer Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCardSubtle,
                    border = BorderStroke(
                        1.dp,
                        if (isPracticed) StatusSuccess.copy(alpha = 0.4f) else BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option Tag & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPracticed) StatusSuccess.copy(alpha = 0.2f) else Primary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${selectedAnswerIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPracticed) StatusSuccess else Primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                if (currentAnswer.badge.isNotBlank()) {
                                    Text(
                                        text = currentAnswer.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isPracticed) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "تم التدرب ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // German Answer Text with Satoshi & Karaoke in LTR layout
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            KaraokeGermanText(
                                text = currentAnswer.german,
                                isPlaying = isAnswerSpeaking,
                                charRange = ttsPlaybackState.charRange,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 23.sp,
                                    fontFamily = Satoshi
                                ),
                                color = TextPrimary
                            )
                        }

                        // Arabic Translation in RTL layout
                        if (currentAnswer.translationAr.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = currentAnswer.translationAr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Cairo,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 21.sp
                                    ),
                                    color = TextSecondary,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        // Actions: Listen & Voice Practice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Listen via TTS
                            FilledTonalIconButton(
                                onClick = {
                                    if (isListeningPractice) {
                                        speechRecognizer.stopListening()
                                        isListeningPractice = false
                                    }
                                    if (isAnswerSpeaking) {
                                        onStopSpeak()
                                    } else {
                                        onSpeak(currentAnswer.german, if (isSlowSpeed) 0.75f else 1.0f)
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = SurfaceCard,
                                    contentColor = Primary
                                ),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAnswerSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isAnswerSpeaking) "إيقاف الصوت" else "استمع للفظ",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Microphone Practice Button
                            Button(
                                onClick = {
                                    if (isListeningPractice) {
                                        speechRecognizer.stopListening()
                                        isListeningPractice = false
                                        KatzuHaptics.micStop(haptic, context)
                                    } else {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            startListeningForAnswer()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isListeningPractice -> StatusError
                                        isPracticed -> StatusSuccess
                                        else -> Primary
                                    },
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(9999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("dialogue_practice_mic_button")
                            ) {
                                Icon(
                                    imageVector = when {
                                        isListeningPractice -> Icons.Default.GraphicEq
                                        isPracticed -> Icons.Default.Check
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isListeningPractice -> "جارٍ الاستماع... تكلّم الآن 🎙️"
                                        isPracticed -> "أتقنت النطق ✓ تدرّب مجدداً"
                                        else -> "تدرّب على هذا الرد بالنطق 🎙️"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = Cairo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // Speech Evaluation Feedback Banner
                        if (isListeningPractice) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusError.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = StatusError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "انطق الجملة الألمانية بصوت واضح الآن...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError
                                        )
                                        if (spokenPracticeText.isNotBlank()) {
                                            Text(
                                                text = spokenPracticeText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Satoshi),
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (practiceMatchScore >= 50) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusSuccess.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "نطق رائع ومتقن! أحسنت ($practiceMatchScore% تطابق) ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess
                                        )
                                        if (spokenPracticeText.isNotBlank()) {
                                            Text(
                                                text = "سمعنا: \"$spokenPracticeText\"",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Satoshi),
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (practiceMatchScore in 0..49) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusLearning.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, StatusLearning.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = StatusLearning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "محاولة جيدة! استمع للفظ 🔊 ثم حاول مجدداً للوصول لنطق أدق",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusLearning
                                        )
                                        if (spokenPracticeText.isNotBlank()) {
                                            Text(
                                                text = "سمعنا: \"$spokenPracticeText\"",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Satoshi),
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (practiceErrorMessage != null) {
                            Text(
                                text = practiceErrorMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Katzu Tip Footer
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🐾",
                        fontSize = 15.sp
                    )
                    Text(
                        text = "نصيحة كاتزو: اضغط على زر الميكروفون وتدرب على النطق بصوت مسموع لترسيخ الجملة في ذهنك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Calculates similarity match percentage between spoken input and expected German sentence.
 */
private fun evaluateSpokenMatch(spoken: String, expected: String): Int {
    if (spoken.isBlank() || expected.isBlank()) return 0
    val cleanSpoken = spoken.lowercase()
        .replace(Regex("[^a-zäöüß0-9 ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val cleanExpected = expected.lowercase()
        .replace(Regex("[^a-zäöüß0-9 ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (cleanExpected.isEmpty()) return 0

    var matchedWords = 0
    for (expWord in cleanExpected) {
        if (cleanSpoken.any { it == expWord || it.startsWith(expWord) || expWord.startsWith(it) }) {
            matchedWords++
        }
    }
    return ((matchedWords.toFloat() / cleanExpected.size.toFloat()) * 100).toInt().coerceIn(0, 100)
}
