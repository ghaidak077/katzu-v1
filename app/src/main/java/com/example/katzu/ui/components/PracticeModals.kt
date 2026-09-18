package com.example.katzu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import com.example.katzu.data.MistakeEntity
import com.example.katzu.model.VocabularyWord
import com.example.katzu.ui.theme.*

@Composable
fun FlashcardsDialog(
    words: List<VocabularyWord>,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val currentWord = words.getOrNull(currentIndex) ?: return

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }

                    Text(
                        text = "بطاقات التكرار المتباعد (${currentIndex + 1} / ${words.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = {
                        val text = if (currentWord.article.isNotBlank()) "${currentWord.article} ${currentWord.germanWord}" else currentWord.germanWord
                        onSpeak(text)
                    }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "نطق", tint = Primary)
                    }
                }

                // Flip Flashcard Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceCardSubtle)
                        .border(1.dp, if (isFlipped) Primary.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { isFlipped = !isFlipped }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front Side: German
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (currentWord.article.isNotBlank()) {
                                Text(
                                    text = currentWord.article,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = currentWord.germanWord,
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentWord.phonetic,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "👆 انقر لقلب البطاقة ومعرفة المعنى",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        // Back Side: Arabic Meaning & Example (mirrored back so it's readable)
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentWord.arabicMeaning,
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "“${currentWord.exampleGerman}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryFixedDim,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentWord.exampleArabic,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) {
                                isFlipped = false
                                currentIndex--
                            }
                        },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text("السابق")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < words.size - 1) {
                                isFlipped = false
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
                    ) {
                        Text(
                            text = if (currentIndex < words.size - 1) "التالي" else "إنهاء التمرين",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarCheatSheetDialog(
    grammarList: List<com.example.katzu.data.GrammarEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                    Text(
                        text = "دليل القواعد السريع • Katzu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rule 1: Der Die Das Colors
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "1. أدوات التعريف وألوان كاتزو الثلاثية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "الألمانية لا تتبع المنطق البشري في جنس الكلمات، لذا ابتكرنا نظام الألوان لتذكرها بالبصر:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDer.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("der", color = ArticleDer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المذكر", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("der Kaffee", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDie.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("die", color = ArticleDie, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المؤنث", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("die Milch", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDas.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("das", color = ArticleDas, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المحايد", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("das Wasser", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Rule 2: Akkusativ
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "2. المفعول به (Akkusativ) في المقهى",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "المذكر هو الوحيد الذي يتغير! Der تصبح den و ein تصبح einen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Ich möchte EINEN Kaffee (وليس ein Kaffee)",
                                    modifier = Modifier.padding(12.dp),
                                    color = StatusLearning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Rule 3: Dativ
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "3. المجرور (Dativ) مع حرف الجر mit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "بعد كلمة mit يأتي دائماً مجرور: der/das ينقلبان إلى dem، و die تنقلب إلى der.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "mit der Milch / mit dem Zucker",
                                    modifier = Modifier.padding(12.dp),
                                    color = PrimaryFixedDim,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dynamically loaded grammar rules from Cloudflare D1 / Room
                    grammarList.forEachIndexed { idx, rule ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 4}. ${rule.title_ar}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = Primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = rule.level,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = rule.explanation_ar.ifBlank { rule.explanation_en },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (rule.example_de.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SurfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = rule.example_de,
                                            modifier = Modifier.padding(12.dp),
                                            color = StatusSuccess,
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
}

/**
 * Interactive Mistakes Bank Dialog (Item 8)
 * Enables learners to review and drill their persisted conversation mistakes stored in Room.
 * Features: Struck-through error vs bold correction, native German TTS with normal & slow speeds,
 * and an interactive typing drill with instant validation.
 */
@Composable
fun MistakesBankDialog(
    mistakes: List<MistakeEntity>,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit = {},
    onDeleteMistake: (Long) -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(0) }
    var drillInput by remember { mutableStateOf("") }
    var practicedMistakeIds by remember { mutableStateOf(setOf<Long>()) }

    val currentMistake = mistakes.getOrNull(currentIndex)

    LaunchedEffect(currentIndex) {
        drillInput = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بنك الأخطاء وتثبيت الصواب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = Cairo
                        )
                        if (mistakes.isNotEmpty()) {
                            Text(
                                text = "خطأ ${currentIndex + 1} من ${mistakes.size} • ${practicedMistakeIds.size} أُتقنت",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontFamily = Cairo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                if (mistakes.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "لا توجد أخطاء مسجلة حالياً! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = Cairo
                        )
                        Text(
                            text = "سجلك اللغوي نظيف تماماً، أو أنك لم تبدأ المحادثات بعد. خض سيناريو تدريب مع كاتزو وسيقوم بتسجيل وتصحيح أي تعبير ترغب في صقله.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontFamily = Cairo,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("العودة إلى التمارين", fontFamily = Cairo, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (currentMistake != null) {
                    val isPracticed = practicedMistakeIds.contains(currentMistake.id)
                    val cleanInput = drillInput.trim().lowercase().replace(Regex("[^a-zäöüß0-9 ]"), "")
                    val cleanTarget = currentMistake.corrected.trim().lowercase().replace(Regex("[^a-zäöüß0-9 ]"), "")
                    val isInputCorrect = cleanInput.isNotBlank() && cleanInput == cleanTarget

                    // Mistake Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Scenario tag & rule
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = currentMistake.scenarioId.ifBlank { "محادثة عامة" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontFamily = Cairo
                                    )
                                }

                                if (isPracticed || isInputCorrect) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "تم الإتقان ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusSuccess,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontFamily = Cairo
                                        )
                                    }
                                }
                            }

                            // Original Mistake (Struck-through)
                            if (currentMistake.original.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "الخطأ الذي قلته:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusError,
                                        fontFamily = Cairo
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = "✕ ${currentMistake.original}",
                                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                                            color = StatusError,
                                            fontFamily = SourceSerif4,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Correct German Formulation
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "الصواب الألماني النموذجي:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusSuccess,
                                    fontFamily = Cairo
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "✓ ${currentMistake.corrected}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess,
                                        fontFamily = SourceSerif4,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Grammar Rule
                            if (currentMistake.grammarRule.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceContainerLow,
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
                                            tint = Tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = currentMistake.grammarRule,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontFamily = Cairo,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            // Audio Listen Actions (Normal & Slow)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onSpeak(currentMistake.corrected) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نطق عادي", fontFamily = Cairo, style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = { onSpeak(currentMistake.corrected) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نطق هادئ", fontFamily = Cairo, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Interactive Drill Box
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "تمرين الكتابة للتثبيت:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontFamily = Cairo
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    OutlinedTextField(
                                        value = drillInput,
                                        onValueChange = {
                                            drillInput = it
                                            val cIn = it.trim().lowercase().replace(Regex("[^a-zäöüß0-9 ]"), "")
                                            val cTar = currentMistake.corrected.trim().lowercase().replace(Regex("[^a-zäöüß0-9 ]"), "")
                                            if (cIn.isNotBlank() && cIn == cTar) {
                                                practicedMistakeIds = practicedMistakeIds + currentMistake.id
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "اكتب الجملة الألمانية الصحيحة...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                fontFamily = SourceSerif4
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isInputCorrect) StatusSuccess else Primary,
                                            unfocusedBorderColor = if (isInputCorrect) StatusSuccess else BorderSubtle,
                                            focusedContainerColor = SurfaceCard,
                                            unfocusedContainerColor = SurfaceCard
                                        ),
                                        singleLine = false,
                                        maxLines = 3
                                    )
                                }

                                if (isInputCorrect) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "أحسنت! أتقنت كتابة الجملة بالصواب.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusSuccess,
                                            fontFamily = Cairo,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Remove / Archive mistake button
                            TextButton(
                                onClick = {
                                    onDeleteMistake(currentMistake.id)
                                    if (currentIndex >= mistakes.size - 1) {
                                        currentIndex = maxOf(0, mistakes.size - 2)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف هذا الخطأ من قائمتي", color = TextMuted, style = MaterialTheme.typography.labelSmall, fontFamily = Cairo)
                            }
                        }
                    }

                    // Bottom navigation controls (Next / Previous)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            enabled = currentIndex > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardSubtle, contentColor = TextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("السابق", fontFamily = Cairo)
                        }

                        Button(
                            onClick = { if (currentIndex < mistakes.size - 1) currentIndex++ },
                            enabled = currentIndex < mistakes.size - 1,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التالي", fontFamily = Cairo, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

