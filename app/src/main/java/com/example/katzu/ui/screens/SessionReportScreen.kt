package com.example.katzu.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.ui.theme.*

@Composable
fun SessionReportScreen(
    wordsLearned: Int = 0,
    sentencesSpoken: Int = 0,
    streakDays: Int = 1,
    accuracyPercent: Int = 100,
    cefrLevel: String = "A1",
    scenarioTitle: String = "المحادثة الحية",
    mistakes: List<com.example.katzu.model.SessionMistakeSummary> = emptyList(),
    praisedSentence: String? = null,
    hintsUsedCount: Int = 0,
    independentSentences: Int = sentencesSpoken,
    onReturnToTrail: () -> Unit,
    onReviewPractice: () -> Unit,
    onSpeak: (String) -> Unit,
    onUpgradeLevel: (String) -> Unit = {}
) {
    BackHandler(onBack = onReturnToTrail)

    val context = LocalContext.current

    fun shareProgressCard() {
        val shareBody = buildString {
            appendLine("🎉 أنجزت محادثة ألمانية جديدة في تطبيق كاتزو (Katzu)!")
            appendLine("─────────────────────────")
            appendLine("🇩🇪 السيناريو: $scenarioTitle ($cefrLevel)")
            appendLine("🎯 الدقة المستقلة: $accuracyPercent%")
            appendLine("🔥 سلسلة الحماس: $streakDays ${if (streakDays == 1) "يوم" else "أيام"}")
            appendLine("💬 الجمل المنطوقة: $sentencesSpoken (منها $independentSentences بتحدث مستقل)")
            if (hintsUsedCount > 0) {
                appendLine("💡 جمل بمساعدة ذكية: $hintsUsedCount")
            }
            if (!praisedSentence.isNullOrBlank()) {
                appendLine("⭐ جملة متميزة: \"$praisedSentence\"")
            }
            appendLine("─────────────────────────")
            appendLine("تحدث الألمانية بثقة وبلا خجل مع كاتزو 🐾")
            appendLine("صُنع بواسطة غيدق علوش — ghaidak.com")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "إنجازي اليوم في كاتزو 🐾")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        val chooser = Intent.createChooser(sendIntent, "مشاركة بطاقة الإنجاز")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    val nextLevel = when (cefrLevel.trim().uppercase()) {
        "A1" -> "A2"
        "A2" -> "B1"
        "B1" -> "B2"
        else -> null
    }
    val isEligibleForPromotion = accuracyPercent >= 75 && independentSentences >= 2 && nextLevel != null

    // Interactive mistake practice state
    var activePracticingIndex by remember { mutableStateOf<Int?>(null) }
    var practiceInputText by remember { mutableStateOf("") }
    var practicedMistakesSet by remember { mutableStateOf(setOf<Int>()) }
    var practiceValidationState by remember { mutableStateOf<Boolean?>(null) }

    // Celebration pulse and glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_fx")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val headlineText = when {
        hintsUsedCount > 0 && independentSentences == 0 -> "خطوة بداية: محادثة مع الاستعانة بالمصباح! 💡"
        hintsUsedCount > 0 -> "محاولة جيدة مع بعض التلميحات! 👏"
        accuracyPercent >= 85 -> "محادثة استثنائية أتقنتها باستقلالية تامة! 👑"
        accuracyPercent >= 60 -> "أحسنت! طلاقة مستقلة بخطوات واثقة! 🌟"
        else -> "شجاعة ممتازة في التحدث بمفردك! 💪"
    }

    val feedbackText = when {
        hintsUsedCount > 0 && independentSentences == 0 -> "خضت محادثة في \"$scenarioTitle\" معتمداً على تلميحات المصباح. هذا ممتاز للتعلم الأولي! هدفك في الجلسة القادمة صياغة رد واحد على الأقل دون مساعدة."
        hintsUsedCount > 0 -> "أداء مبشّر في \"$scenarioTitle\"! صغت $independentSentences جمل بنفسك، بينما استعنت بالمصباح في $hintsUsedCount مرات. نقاط الدقة والطلاقة تُحسب فقط لما صغته باستقلالية لمنع التقدم الوهمي."
        accuracyPercent >= 85 -> "تحدثت بثقة وسلاسة كاملة في \"$scenarioTitle\" دون طلب أي مساعدة! أفكارك وصلت بوضوح وقواعدك كانت متماسكة ومثيرة للإعجاب."
        accuracyPercent >= 60 -> "صغت كل ردودك بنفسك في \"$scenarioTitle\" دون الحاجة للتلميحات، وسنصقل التراكيب اللغوية مع الاستمرار."
        else -> "خوضك محادثة كاملة في \"$scenarioTitle\" معتمداً على نفسك فقط هو بحد ذاته إنجاز كبير يستحق التقدير."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Mascot Celebration Hero with Dopamine Aura
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft Radial Dopamine Glow
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFC9A8FF).copy(alpha = glowAlpha),
                                        Color(0xFF8B6FE8).copy(alpha = glowAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    Image(
                        painter = painterResource(id = R.drawable.katzu_celebrating),
                        contentDescription = "Katzu Celebrating",
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
                        contentScale = ContentScale.Fit
                    )
                }

                // Dopamine Reward Chips (XP, Streak, Accuracy)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val xpGained = if (hintsUsedCount == 0 && accuracyPercent >= 80) 60 else if (independentSentences > 0) 40 else 20
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Primary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+$xpGained XP ⚡",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = StatusLearning.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, StatusLearning.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🔥 $streakDays أيام حماس",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusLearning,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = StatusSuccess.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🎯 $accuracyPercent% دقة",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = StatusSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Next Level Promotion Banner (Encourage Progression when doing great)
            if (isEligibleForPromotion) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.5.dp, Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("level_promotion_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "مستعد للانتقال للمستوى التالي ($nextLevel)؟ 🚀",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "أثبتت جدارتك في مستوى $cefrLevel بدقة $accuracyPercent% واستقلالية ممتازة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "الانتقال إلى مستوى $nextLevel يفتح لك مفردات وتراكيب أوسع وحوارات أكثر احترافية.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = TextPrimary
                        )

                        Button(
                            onClick = { onUpgradeLevel(nextLevel!!) },
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("upgrade_level_button")
                        ) {
                            Text(
                                text = "الانتقال إلى مستوى $nextLevel الآن 🏆",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Assistance Diagnostic Banner (Transparent Honest Score Tracking)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (hintsUsedCount > 0) StatusLearning.copy(alpha = 0.45f) else StatusSuccess.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (hintsUsedCount > 0) StatusLearning.copy(alpha = 0.15f) else StatusSuccess.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hintsUsedCount > 0) Icons.Default.Lightbulb else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hintsUsedCount > 0) StatusLearning else StatusSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (hintsUsedCount > 0) "تقرير الاعتماد على التلميحات" else "استقلالية كاملة في المحادثة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = if (hintsUsedCount > 0) StatusLearning.copy(alpha = 0.15f) else StatusSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (hintsUsedCount > 0) "استعنت بالمصباح ($hintsUsedCount)" else "بدون مساعدة 🌟",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hintsUsedCount > 0) StatusLearning else StatusSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (hintsUsedCount > 0) {
                                "تم الاستعانة بالمصباح في $hintsUsedCount جولات. لمنع التقدم الوهمي، تم احتساب الدقة والطلاقة فقط لـ $independentSentences جمل صغتها باستقلالية تامة."
                            } else {
                                "تحدثت وصغت جميع ردودك معتمداً على نفسك فقط دون تلميحات. جميع نقاط الجلسة مستحقة لجهدك الشخصي!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Stat Tiles (3 Bento Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "$wordsLearned", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(text = "كلمات أُتقنت", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (hintsUsedCount > 0) "$independentSentences / $sentencesSpoken" else "$sentencesSpoken",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hintsUsedCount > 0) "جمل مستقلة" else "جمل مستقلة 🌟",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = StatusLearning, modifier = Modifier.size(18.dp))
                            Text(text = "$streakDays", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "أيام حماس", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            // CEFR Fluency Mastery Card (Evaluates ONLY independent sentences)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "دقة الصياغة المستقلة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val tierText = when {
                                hintsUsedCount > 0 && independentSentences == 0 -> "تحت التدريب"
                                accuracyPercent >= 90 -> "ممتاز"
                                accuracyPercent >= 80 -> "متقن"
                                else -> "قيد التطور"
                            }
                            val tierColor = when {
                                hintsUsedCount > 0 && independentSentences == 0 -> StatusLearning
                                accuracyPercent >= 80 -> StatusSuccess
                                accuracyPercent >= 60 -> StatusLearning
                                else -> StatusError
                            }
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = tierColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = tierText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tierColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (hintsUsedCount > 0) "$cefrLevel • تقييم الجمل المستقلة فقط ($independentSentences من $sentencesSpoken)" else "$cefrLevel • تقييم المحادثة الفعلي",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSubtle)
                            .border(3.dp, Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$accuracyPercent%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Praised Phrase Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Text(
                                text = "جملة نالت استحسان كاتزو النادر",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Cairo
                            )
                        }

                        if (!praisedSentence.isNullOrBlank()) {
                            IconButton(
                                onClick = { onSpeak(praisedSentence) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "استمع للجملة", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (!praisedSentence.isNullOrBlank()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "“${isolateGerman(praisedSentence)}”",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = Satoshi,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start
                            )
                        }
                        Text(
                            text = "صيغت بدقة وطلاقة أثارت إعجاب شخصيات السيناريو دون أخطاء تُذكر.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "“${isolateGerman("Keine perfekte Formulierung heute.")}”",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = Satoshi,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Start
                            )
                        }
                        Text(
                            text = "لم تُسجّل جملة طويلة ومثالية بالكامل في هذه الجلسة، لكن الاستمرار هو سر الإتقان.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Diagnostic Review & Practice Section (Interactive mistake practice)
            if (mistakes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تدريب على الأخطاء (${practicedMistakesSet.size}/${mistakes.size} أُتقنت)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Cairo
                    )
                    if (practicedMistakesSet.size == mistakes.size) {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = StatusSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "أصلحت كل الأخطاء! 🎉",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                mistakes.forEachIndexed { index, mistake ->
                    val isPracticed = practicedMistakesSet.contains(index)
                    val isCurrentlyPracticing = activePracticingIndex == index

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPracticed) StatusSuccess.copy(alpha = 0.08f) else SurfaceCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isPracticed) StatusSuccess.copy(alpha = 0.4f) else StatusError.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("mistake_card_$index")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPracticed) Icons.Outlined.Check else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isPracticed) StatusSuccess else StatusError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPracticed) "تم إتقان الصواب ✓" else mistake.grammarRule.ifBlank { "تصحيح نحوي" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPracticed) StatusSuccess else StatusError,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Cairo
                                    )
                                }

                                if (mistake.correctedGerman.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSpeak(mistake.correctedGerman) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "استمع للتصحيح", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (mistake.originalMistake.isNotBlank()) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "✕ ${isolateGerman(mistake.originalMistake)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = Satoshi,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        textAlign = TextAlign.Start,
                                        color = StatusError
                                    )
                                }
                            }

                            if (mistake.correctedGerman.isNotBlank()) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "✓ ${isolateGerman(mistake.correctedGerman)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = Satoshi,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                        color = if (isPracticed) StatusSuccess else TextPrimary
                                    )
                                }
                            }

                            if (mistake.roastComment.isNotBlank()) {
                                Text(
                                    text = "💡 ${mistake.roastComment}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Interactive Practice Toggle & Box
                            if (!isPracticed) {
                                if (!isCurrentlyPracticing) {
                                    OutlinedButton(
                                        onClick = {
                                            activePracticingIndex = index
                                            practiceInputText = ""
                                            practiceValidationState = null
                                        },
                                        shape = RoundedCornerShape(9999.dp),
                                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FitnessCenter,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تدرّب على تصحيح هذه الجملة الآن",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Primary
                                        )
                                    }
                                } else {
                                    // Active Drill Form
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceCardSubtle)
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "اكتب أو ردد الجملة بالصياغة الصحيحة:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )

                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            OutlinedTextField(
                                                value = practiceInputText,
                                                onValueChange = {
                                                    practiceInputText = it
                                                    practiceValidationState = null
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("practice_input_$index"),
                                                shape = RoundedCornerShape(12.dp),
                                                placeholder = {
                                                    Text("اكتب الجملة الألمانية الصحيحة هنا...", style = MaterialTheme.typography.bodySmall)
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    textAlign = TextAlign.Start,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Primary,
                                                    unfocusedBorderColor = BorderSubtle
                                                ),
                                                maxLines = 2
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    // Normalize text for comparison: remove punctuation and extra spaces
                                                    val cleanInput = practiceInputText.trim().replace(Regex("[.,!?;:]"), "").lowercase()
                                                    val cleanTarget = mistake.correctedGerman.trim().replace(Regex("[.,!?;:]"), "").lowercase()
                                                    if (cleanInput.isNotBlank() && (cleanInput == cleanTarget || cleanTarget.contains(cleanInput))) {
                                                        practicedMistakesSet = practicedMistakesSet + index
                                                        practiceValidationState = true
                                                        activePracticingIndex = null
                                                    } else {
                                                        practiceValidationState = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(9999.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                                modifier = Modifier.weight(1f).height(40.dp)
                                            ) {
                                                Text("تحقق من إجابتي", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            }

                                            TextButton(
                                                onClick = {
                                                    activePracticingIndex = null
                                                    practiceValidationState = null
                                                },
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("إلغاء", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                        }

                                        if (practiceValidationState == false) {
                                            Text(
                                                text = "اقتربت! تأكد من كتابة الصياغة التالية بدقة:\n${mistake.correctedGerman}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusLearning
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (sentencesSpoken > 0) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Text(
                                text = "جلسة خالية من الأخطاء! 🎉",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Cairo
                            )
                        }

                        Text(
                            text = "لم يجد كاتزو أي هفوة نحوية ليعلّق عليها. دقة لغوية استثنائية تناسب مستوى $cefrLevel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = Cairo
                        )
                    }
                }
            }

            // CTA Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onReturnToTrail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("report_back_to_trail_btn"),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        text = "متابعة المسار",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { shareProgressCard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("report_share_card_btn"),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "مشاركة بطاقة الإنجاز",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Cairo
                        )
                    }
                }

                OutlinedButton(
                    onClick = onReviewPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("report_review_words_btn"),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "صقل الكلمات في قسم التدريب",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

