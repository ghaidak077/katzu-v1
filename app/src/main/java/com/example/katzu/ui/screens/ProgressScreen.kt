package com.example.katzu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.data.ScenarioEntity
import com.example.katzu.data.SessionEntity
import com.example.katzu.model.UserProfile
import com.example.katzu.ui.components.ShareProgressCardDialog
import com.example.katzu.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    userProfile: UserProfile,
    sessions: List<SessionEntity> = emptyList(),
    totalScenariosCount: Int = 0,
    savedWordsCount: Int = 0,
    totalVocabularyCount: Int = 0,
    scenarios: List<ScenarioEntity> = emptyList(),
    onOpenGoalDialog: () -> Unit = {},
    onNavigateToTrail: () -> Unit = {}
) {
    // Memoize aggregated metrics to ensure zero jank during scroll/recomposition
    var showShareCardDialog by remember { mutableStateOf(false) }

    val completedScenarios = remember(sessions) {
        sessions.map { it.scenarioId }.distinct().size
    }

    val totalPracticeSeconds = remember(sessions) {
        sessions.sumOf { it.durationSeconds }
    }

    val practiceDisplay = remember(totalPracticeSeconds) {
        val hours = totalPracticeSeconds / 3600f
        if (hours >= 1f) {
            String.format(Locale.US, "%.1f س", hours)
        } else {
            "${totalPracticeSeconds / 60} د"
        }
    }

    val fluencyRate = remember(sessions) {
        if (sessions.isNotEmpty()) {
            sessions.map { it.accuracyPercent }.average().toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    val totalSentences = remember(sessions) {
        sessions.sumOf { it.sentencesSpoken }
    }

    val totalWords = remember(sessions) {
        sessions.sumOf { it.wordsLearned }
    }

    val streakDays = remember(sessions) {
        if (sessions.isNotEmpty()) {
            val days = sessions.map {
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }.distinct().sortedDescending()
            var streak = 0
            var current = LocalDate.now()
            for (day in days) {
                if (day == current || day == current.minusDays(1)) {
                    streak++
                    current = day
                } else {
                    break
                }
            }
            streak.coerceAtLeast(1)
        } else {
            0
        }
    }

    // Calculate real progress towards user's chosen goal
    val (goalCompletedCount, goalTotalCount, goalPercent) = remember(
        sessions,
        scenarios,
        userProfile.targetGoalId,
        userProfile.targetWeeklyDays
    ) {
        val goalId = userProfile.targetGoalId
        when {
            goalId.startsWith("goal_a1") -> {
                val totalA1 = scenarios.size.coerceAtLeast(1)
                val doneA1 = sessions.filter { it.cefrLevel.startsWith("A1", ignoreCase = true) }.map { it.scenarioId }.distinct().size
                Triple(doneA1, totalA1, ((doneA1.toFloat() / totalA1) * 100).toInt().coerceIn(0, 100))
            }
            goalId.startsWith("goal_a2") -> {
                val totalA2 = scenarios.size.coerceAtLeast(1)
                val doneA2 = sessions.filter { it.cefrLevel.startsWith("A2", ignoreCase = true) }.map { it.scenarioId }.distinct().size
                Triple(doneA2, totalA2, ((doneA2.toFloat() / totalA2) * 100).toInt().coerceIn(0, 100))
            }
            goalId.startsWith("goal_b1") -> {
                val totalB1 = scenarios.size.coerceAtLeast(1)
                val doneB1 = sessions.filter { it.cefrLevel.startsWith("B1", ignoreCase = true) }.map { it.scenarioId }.distinct().size
                Triple(doneB1, totalB1, ((doneB1.toFloat() / totalB1) * 100).toInt().coerceIn(0, 100))
            }
            goalId.startsWith("goal_b2") -> {
                val totalB2 = scenarios.size.coerceAtLeast(1)
                val doneB2 = sessions.filter { it.cefrLevel.startsWith("B2", ignoreCase = true) }.map { it.scenarioId }.distinct().size
                Triple(doneB2, totalB2, ((doneB2.toFloat() / totalB2) * 100).toInt().coerceIn(0, 100))
            }
            else -> {
                // Habit goal: active practice days this week
                val today = LocalDate.now()
                val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
                val activeDaysThisWeek = sessions.map {
                    Instant.ofEpochMilli(it.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.distinct().count { !it.isBefore(weekStart) }
                val target = userProfile.targetWeeklyDays.coerceAtLeast(1)
                Triple(activeDaysThisWeek, target, ((activeDaysThisWeek.toFloat() / target) * 100).toInt().coerceIn(0, 100))
            }
        }
    }

    // Dynamic 7-day activity chart (real data)
    val today = remember { LocalDate.now() }
    val last7Days = remember(today) { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale("ar")) }
    val weekDays = remember(last7Days) { last7Days.map { it.format(dayFormatter) } }

    val (weeklyTotalMinutes, activityHeights) = remember(sessions, last7Days) {
        val secondsPerDay = last7Days.map { date ->
            sessions.filter {
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate() == date
            }.sumOf { it.durationSeconds }
        }
        val totalMin = secondsPerDay.sum() / 60
        val maxSeconds = (secondsPerDay.maxOrNull() ?: 0).coerceAtLeast(60)
        val heights = secondsPerDay.map {
            if (it == 0) 0.06f else (it.toFloat() / maxSeconds).coerceIn(0.12f, 1.0f)
        }
        Pair(totalMin, heights)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "سجل الإنجاز والتطور",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "شاهد ثمار التزامك",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "كل محادثة تخوضها تقرّبك خطوة حقيقية من الطلاقة العفوية. الأرقام هنا تعكس رحلتك خطوة بخطوة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.katzu_progress_mascot),
                    contentDescription = "Katzu Mascot",
                    modifier = Modifier
                        .size(68.dp)
                        .padding(start = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Share Progress Card Call-to-Action Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showShareCardDialog = true }
                    .testTag("open_share_card_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة",
                                tint = Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "شارك بطاقة إنجازك 🚀",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "شارك مستواك (${userProfile.currentLevel}) وحماستك (${streakDays} أيام) كصورة أنيقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Primary,
                        contentColor = BackgroundPure
                    ) {
                        Text(
                            text = "مشاركة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Real Goal Card (Interactive with Duolingo-style presets)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGoalDialog() }
                    .testTag("current_goal_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = StatusLearning.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "الهدف • ${userProfile.targetWeeklyDays} أيام/أسبوع (${userProfile.targetDailyMinutes} د/يوم)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusLearning,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = userProfile.targetGoalTitle.ifBlank { "الوصول إلى مستوى A2 والتحدث بثقة" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = if (userProfile.targetGoalId.startsWith("goal_daily_habit")) {
                                "أنجزت $goalCompletedCount من $goalTotalCount أيام التدريب هذا الأسبوع"
                            } else {
                                "أنجزت $goalCompletedCount من $goalTotalCount سيناريوهات المستوى"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اضغط لتغيير الهدف أو وتيرة التعلّم",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSubtle)
                            .border(4.dp, Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$goalPercent%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "مكتمل",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid with Hero Stat on Top
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // HERO STAT CARD
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "إجمالي وقت التحدث الفعلي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // Streak Pill Badge
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = StatusLearning.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, StatusLearning.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = StatusLearning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$streakDays أيام متتالية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusLearning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Big Hero Metric Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = practiceDisplay,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryFixedDim
                                )
                                Text(
                                    text = "ممارسة صوتية مباشرة مع كاتزو",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Fluency Gauge mini-card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceCardSubtle,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = if (sessions.isNotEmpty()) "$fluencyRate%" else "—",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fluencyRate >= 80) StatusSuccess else StatusLearning
                                    )
                                    Text(
                                        text = "معدل الدقة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Secondary Row: Completed Scenarios & Sentences Spoken
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (totalScenariosCount > 0) "$completedScenarios / $totalScenariosCount" else "$completedScenarios",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(text = "سيناريو مكتمل", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            Text(text = "$totalSentences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "جمل قِيلت في المحادثات", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }

                // Tertiary Row: Words Learned & Vocabulary Saved
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Spellcheck, contentDescription = null, tint = ArticleDas, modifier = Modifier.size(20.dp))
                            Text(text = "$totalWords", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "كلمات تم تداولها", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = Tertiary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (totalVocabularyCount > 0) "$savedWordsCount / $totalVocabularyCount" else "$savedWordsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(text = "مفردات محفوظة", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        // Weekly Voice Practice Activity Chart (Real 7-day data)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نشاط التحدث هذا الأسبوع",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "المجموع: $weeklyTotalMinutes دقيقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }

                    // Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekDays.forEachIndexed { index, day ->
                            val heightFraction = activityHeights[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (index == weekDays.lastIndex) Primary else SurfaceCardSubtle
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = day.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index == weekDays.lastIndex) Primary else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real Performance Insights / Katzu Recommendation Card
        item {
            val hasSessions = sessions.isNotEmpty()
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(
                    1.dp,
                    if (!hasSessions) Primary.copy(alpha = 0.3f)
                    else if (fluencyRate < 75) StatusError.copy(alpha = 0.3f)
                    else if (fluencyRate < 90) StatusLearning.copy(alpha = 0.3f)
                    else StatusSuccess.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (!hasSessions) Icons.Default.School
                            else if (fluencyRate < 75) Icons.Default.WarningAmber
                            else if (fluencyRate < 90) Icons.Default.TipsAndUpdates
                            else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (!hasSessions) Primary
                            else if (fluencyRate < 75) StatusError
                            else if (fluencyRate < 90) StatusLearning
                            else StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (!hasSessions) "جاهز للبدء"
                            else if (fluencyRate < 75) "نقطة تحتاج تركيزاً إضافياً"
                            else if (fluencyRate < 90) "توجيه كاتزو لرفع الطلاقة"
                            else "إتقان ممتاز وطلاقة عالية",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!hasSessions) Primary
                            else if (fluencyRate < 75) StatusError
                            else if (fluencyRate < 90) StatusLearning
                            else StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (!hasSessions) "بانتظار محادثتك الصوتية الأولى 🐾"
                        else if (fluencyRate < 75) "ترتيب الجملة الألمانية وهدوء النطق"
                        else if (fluencyRate < 90) "أدوات الربط (Konnektoren) وتوسيع التعبير"
                        else "دقة استثنائية تفوق 90%! حان وقت رفع التحدي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (!hasSessions) {
                            "لم تقم بعد بأي محادثة صوتية مع كاتزو. اختر سيناريو من المسار وابدأ الحديث؛ سيقوم كاتزو بتحليل نبرتك ودقتك وتحديد أخطائك بدقة تلقائياً."
                        } else if (fluencyRate < 75) {
                            "معدل دقتك الحالي $fluencyRate%. يلاحظ كاتزو أن السرعة تؤثر على ترتيب الكلمات. تذكر: في الجملة الألمانية البسيطة، الفعل دائماً في المركز الثاني. خذ نفساً وتكلّم بهدوء!"
                        } else if (fluencyRate < 90) {
                            "أداؤك جيد جداً بمعدل دقة $fluencyRate%! الخطوة التالية هي التحدث بجمل مركبة باستخدام weil و dass و aber لتبدو كمتحدث طبيعي."
                        } else {
                            "كاتزو معجب بثقتك ودقتك ($fluencyRate%) على غير عادته! لا تتوقف هنا؛ جرب سيناريوهات المستويات الأعلى لتثبيت مفردات المحادثة المعقدة."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    if (!hasSessions) {
                        Button(
                            onClick = onNavigateToTrail,
                            shape = RoundedCornerShape(9999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("start_first_conversation_button")
                        ) {
                            Text(
                                text = "ابدأ أول محادثة الآن",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Real Session History List (سجل المحادثات المنجزة)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المحادثات الأخيرة (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "محفوظ محلياً في Room",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "لا توجد محادثات مسجلة بعد",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "كل محادثة تنهيها في كاتزو ستظهر هنا مع إحصاءات الدقة والوقت والجمل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            val recentSessions = sessions.take(8)
            items(recentSessions, key = { it.id }) { session ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = Primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = session.cefrLevel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = session.scenarioTitle.ifBlank { "جلسة محادثة" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            val minutes = session.durationSeconds / 60
                            val seconds = session.durationSeconds % 60
                            val durationText = if (minutes > 0) "$minutes د و $seconds ث" else "$seconds ثوانٍ"

                            Text(
                                text = "تدريب: $durationText • ${session.sentencesSpoken} جمل • ${session.wordsLearned} كلمات",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Accuracy badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (session.accuracyPercent >= 85) StatusSuccess.copy(alpha = 0.15f)
                            else if (session.accuracyPercent >= 70) StatusLearning.copy(alpha = 0.15f)
                            else StatusError.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${session.accuracyPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (session.accuracyPercent >= 85) StatusSuccess
                                else if (session.accuracyPercent >= 70) StatusLearning
                                else StatusError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Motivational Footer Micro-card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.katzu_badge),
                            contentDescription = "Katzu Badge",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "نصيحة كاتزو: الثقة في المحادثة نصف الطلاقة. الألمان يفضلون شخصاً يحاول حتى مع الأخطاء على شخص صامت تماماً!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showShareCardDialog) {
        ShareProgressCardDialog(
            userProfile = userProfile,
            streakDays = streakDays,
            totalWords = totalVocabularyCount,
            fluencyRate = fluencyRate,
            completedScenarios = completedScenarios,
            onDismiss = { showShareCardDialog = false }
        )
    }
}
