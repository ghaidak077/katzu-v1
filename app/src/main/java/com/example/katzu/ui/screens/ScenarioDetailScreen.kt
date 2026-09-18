package com.example.katzu.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.data.ScenarioTrainingEntity
import com.example.katzu.model.*
import com.example.katzu.ui.components.ArticleBadge
import com.example.katzu.ui.components.ContentEmptyStateView
import com.example.katzu.ui.components.bouncyClickable
import com.example.katzu.ui.theme.*
import com.example.katzu.util.KatzuHaptics

@Composable
fun ScenarioDetailScreen(
    scenario: Scenario?,
    vocabularyList: List<VocabularyWord>,
    onBack: () -> Unit,
    onStartStudy: () -> Unit = {},
    onStartQuiz: () -> Unit = {},
    onStartConversation: () -> Unit,
    trainingProgress: ScenarioTrainingEntity? = null,
    onWordClick: (VocabularyWord) -> Unit,
    isSyncing: Boolean = false,
    onRetrySync: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    if (scenario == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPure)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "رجوع",
                        tint = TextPrimary
                    )
                }

                ContentEmptyStateView(
                    title = "تفاصيل السيناريو غير متاحة",
                    message = "لم نتمكن من تحميل تفاصيل هذا السيناريو لعدم توفر اتصال بالإنترنت أو عدم اكتمال المزامنة.",
                    buttonText = "إعادة المحاولة",
                    isRetrying = isSyncing,
                    onRetry = onRetrySync
                )
            }
        }
        return
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
            // Top Navigation & Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .testTag("scenario_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward, // In RTL, forward arrow points to back
                        contentDescription = "رجوع",
                        tint = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = scenario.cefrLevel,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryFixedDim,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = scenario.timeEstimate,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Scenario Title Banner
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = scenario.scenarioNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scenario.titleArabic,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = scenario.titleGerman,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 20.sp
                )
            }

            // Coach Persona Card with improved quote hierarchy
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hostAvatarRes = remember(scenario) {
                            val category = scenario.category.lowercase()
                            val persona = (if (scenario.aiPersona.isNotBlank()) scenario.aiPersona else scenario.coachTitle).lowercase()
                            when {
                                category.contains("cafe") || category.contains("restaurant") || category.contains("food") || persona.contains("barista") || persona.contains("kellner") -> R.drawable.katzu_barista
                                category.contains("trail") || category.contains("travel") || category.contains("airport") || category.contains("reisen") -> R.drawable.katzu_trail_guide
                                category.contains("interview") || category.contains("official") || category.contains("amt") || category.contains("work") || category.contains("doctor") || persona.contains("arzt") -> R.drawable.katzu_scenario_host
                                else -> R.drawable.katzu_scenario_host
                            }
                        }
                        Image(
                            painter = painterResource(id = hostAvatarRes),
                            contentDescription = scenario.coachName,
                            modifier = Modifier.size(68.dp),
                            contentScale = ContentScale.Fit
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = scenario.coachName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = Primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = scenario.coachTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Prominent quote speech-bubble box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCardSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = scenario.coachRoastQuote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Conversation Goals Checklist
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "مهارات ستكتسبها في هذه المحطة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (scenario.goals.isEmpty()) {
                        Text(
                            text = "لا توجد أهداف محددة لهذا السيناريو حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        scenario.goals.forEach { goal ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (goal.status) {
                                                GoalStatus.Completed -> StatusSuccess.copy(alpha = 0.2f)
                                                GoalStatus.InProgress -> Primary.copy(alpha = 0.2f)
                                                GoalStatus.Pending -> SurfaceContainerLow
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (goal.status) {
                                            GoalStatus.Completed -> Icons.Default.Check
                                            GoalStatus.InProgress -> Icons.Default.PlayArrow
                                            GoalStatus.Pending -> Icons.Default.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                        tint = when (goal.status) {
                                            GoalStatus.Completed -> StatusSuccess
                                            GoalStatus.InProgress -> Primary
                                            GoalStatus.Pending -> TextMuted
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = goal.titleArabic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = goal.subtitleGerman,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Key Vocabulary Chips
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مفردات ستلزمك في المحادثة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    val matchingWords = vocabularyList.filter { scenario.vocabularyWordIds.contains(it.id) }
                    val scenarioWords = if (matchingWords.isNotEmpty()) matchingWords else vocabularyList.take(4)

                    if (scenarioWords.isEmpty()) {
                        Text(
                            text = "لا توجد مفردات محملة حالياً لهذا السيناريو.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scenarioWords.take(4).forEach { word ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCardSubtle,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .bouncyClickable {
                                            KatzuHaptics.tick(haptic)
                                            onWordClick(word)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (word.article.isNotBlank()) {
                                            ArticleBadge(word.article)
                                        }
                                        Text(
                                            text = word.germanWord,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Space to ensure content is not hidden behind the sticky bottom CTA bar
            Spacer(modifier = Modifier.height(100.dp))
        }

        val hasStudied = trainingProgress?.studiedAt != null
        val hasAttemptedQuiz = trainingProgress?.quizAttempted == true

        // Sticky Bottom Dock for CTAs
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = BackgroundPure.copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasStudied) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                KatzuHaptics.press(haptic)
                                onStartStudy()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("scenario_review_study_btn"),
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = TextPrimary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "مراجعة الكلمات",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (hasAttemptedQuiz) {
                            OutlinedButton(
                                onClick = {
                                    KatzuHaptics.press(haptic)
                                    onStartQuiz()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("scenario_retake_quiz_btn"),
                                shape = RoundedCornerShape(9999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = StatusLearning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "إعادة الاختبار",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                val (buttonText, buttonIcon, onPrimaryClick) = when {
                    !hasStudied -> Triple(
                        "ابدأ الدراسة",
                        Icons.Default.MenuBook,
                        onStartStudy
                    )
                    !hasAttemptedQuiz -> Triple(
                        "ابدأ الاختبار",
                        Icons.Default.Psychology,
                        onStartQuiz
                    )
                    else -> Triple(
                        "ابدأ المحادثة الحية",
                        Icons.Default.Mic,
                        onStartConversation
                    )
                }

                Button(
                    onClick = {
                        KatzuHaptics.press(haptic)
                        onPrimaryClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(9999.dp),
                            ambientColor = Primary.copy(alpha = 0.4f),
                            spotColor = Primary.copy(alpha = 0.6f)
                        )
                        .testTag("scenario_start_voice_btn"),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = TextPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
