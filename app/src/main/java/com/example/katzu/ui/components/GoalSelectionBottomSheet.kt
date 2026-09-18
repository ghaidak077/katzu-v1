package com.example.katzu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.ui.theme.*

data class GoalPresetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val levelCode: String?,
    val icon: ImageVector
)

data class IntensityPresetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val weeklyDays: Int,
    val dailyMinutes: Int,
    val badge: String
)

val GOAL_PRESETS = listOf(
    GoalPresetItem(
        id = "goal_a1",
        title = "أساسيات التواصل والمحادثة (A1)",
        subtitle = "نطق التحيات، الطلبات البسيطة، وبناء الثقة الأولى",
        levelCode = "A1",
        icon = Icons.Default.ChatBubbleOutline
    ),
    GoalPresetItem(
        id = "goal_a2",
        title = "التحدث اليومي بلا خجل (A2)",
        subtitle = "المطعم، التسوق، السؤال عن الطريق، والروتين اليومي",
        levelCode = "A2",
        icon = Icons.Default.RecordVoiceOver
    ),
    GoalPresetItem(
        id = "goal_b1",
        title = "الاستعداد للعمل والإقامة (B1)",
        subtitle = "المقابلات الرسمية، المحادثات المعقدة، والطلاقة الوظيفية",
        levelCode = "B1",
        icon = Icons.Default.WorkOutline
    ),
    GoalPresetItem(
        id = "goal_b2",
        title = "الطلاقة والمفردات المتقدمة (B2)",
        subtitle = "النقاشات التلقائية، التعبيرات المجازية، والحديث كالناطقين",
        levelCode = "B2",
        icon = Icons.Default.AutoAwesome
    ),
    GoalPresetItem(
        id = "goal_daily_habit",
        title = "بناء عادة يومية ثابتة",
        subtitle = "المحافظة على الحماس والتحدث كل يوم دون انقطاع",
        levelCode = null,
        icon = Icons.Default.LocalFireDepartment
    )
)

val INTENSITY_PRESETS = listOf(
    IntensityPresetItem(
        id = "casual",
        title = "خفيف • Casual",
        subtitle = "3 أيام في الأسبوع • 5 دقائق/يوم",
        weeklyDays = 3,
        dailyMinutes = 5,
        badge = "للمشغولين جداً"
    ),
    IntensityPresetItem(
        id = "regular",
        title = "منتظم • Regular",
        subtitle = "5 أيام في الأسبوع • 10 دقائق/يوم",
        weeklyDays = 5,
        dailyMinutes = 10,
        badge = "موصى به ⭐"
    ),
    IntensityPresetItem(
        id = "serious",
        title = "جاد • Serious",
        subtitle = "كل يوم (7 أيام) • 15 دقيقة/يوم",
        weeklyDays = 7,
        dailyMinutes = 15,
        badge = "تقدم سريع"
    ),
    IntensityPresetItem(
        id = "intense",
        title = "مكثف • Intense",
        subtitle = "مرتان يومياً • 20 دقيقة/يوم",
        weeklyDays = 7,
        dailyMinutes = 20,
        badge = "قبل السفر أو الامتحان"
    )
)

data class GermanExperienceItem(
    val id: String,
    val title: String,
    val subtitle: String
)

val EXPERIENCE_PRESETS = listOf(
    GermanExperienceItem(
        id = "never_studied",
        title = "لم أدرس الألمانية من قبل",
        subtitle = "أبدأ من الصفر تماماً مع كاتزو وبناء الثقة الأولى"
    ),
    GermanExperienceItem(
        id = "studied_in_school",
        title = "درستها في المدرسة أو المعهد",
        subtitle = "أعرف بعض القواعد لكن أجد صعوبة في التحدث بطلاقة"
    ),
    GermanExperienceItem(
        id = "live_in_germany",
        title = "أعيش أو أعمل في ألمانيا",
        subtitle = "أحتاج لمحادثة حقيقية يومية لإدارة حياتي وعملي"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSelectionBottomSheet(
    currentGoalId: String,
    currentIntensityPreset: String,
    currentExperience: String = "never_studied",
    onDismiss: () -> Unit,
    onSaveGoal: (goalId: String, goalTitle: String, targetDays: Int, targetMinutes: Int, intensityPreset: String, experience: String) -> Unit
) {
    var selectedGoalId by remember { mutableStateOf(currentGoalId) }
    var selectedIntensityId by remember { mutableStateOf(currentIntensityPreset) }
    var selectedExperienceId by remember { mutableStateOf(currentExperience) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundPure,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderSubtle)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "اختر هدفك وخطة التعلّم",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "يساعدك كاتزو في قياس تقدمك الحقيقي وتذكيرك وفق خطتك المختارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Section 1: Choose Goal
            item {
                Text(
                    text = "1. الهدف اللغوي الأساسي",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            items(GOAL_PRESETS.size) { index ->
                val goal = GOAL_PRESETS[index]
                val isSelected = goal.id == selectedGoalId

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SurfaceCardSubtle else SurfaceCard,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) Primary else BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedGoalId = goal.id }
                        .testTag("goal_option_${goal.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceCardSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = goal.icon,
                                contentDescription = null,
                                tint = if (isSelected) Primary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                            Text(
                                text = goal.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedGoalId = goal.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Primary,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }
            }

            // Section 2: Learning Intensity Preset (Duolingo style)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "2. وتيرة التعلّم الأسبوعية (مثل دوولينجو)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            items(INTENSITY_PRESETS.size) { index ->
                val intensity = INTENSITY_PRESETS[index]
                val isSelected = intensity.id == selectedIntensityId

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SurfaceCardSubtle else SurfaceCard,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) Primary else BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedIntensityId = intensity.id }
                        .testTag("intensity_option_${intensity.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = intensity.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (isSelected) Primary.copy(alpha = 0.2f) else SurfaceContainerLow
                                ) {
                                    Text(
                                        text = intensity.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Primary else StatusLearning,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = intensity.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedIntensityId = intensity.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Primary,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }
            }

            // Section 3: German Experience
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "3. ما هي تجربتك السابقة مع الألمانية؟",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            items(EXPERIENCE_PRESETS.size) { index ->
                val experience = EXPERIENCE_PRESETS[index]
                val isSelected = experience.id == selectedExperienceId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedExperienceId = experience.id }
                        .testTag("experience_item_${experience.id}"),
                    color = if (isSelected) SurfaceCard else BackgroundPure,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Primary else BorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = experience.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = experience.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedExperienceId = experience.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Primary,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }
            }

            // CTA Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val chosenGoal = GOAL_PRESETS.find { it.id == selectedGoalId } ?: GOAL_PRESETS[1]
                        val chosenIntensity = INTENSITY_PRESETS.find { it.id == selectedIntensityId } ?: INTENSITY_PRESETS[1]
                        onSaveGoal(
                            chosenGoal.id,
                            chosenGoal.title,
                            chosenIntensity.weeklyDays,
                            chosenIntensity.dailyMinutes,
                            chosenIntensity.id,
                            selectedExperienceId
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_goal_button"),
                    shape = RoundedCornerShape(9999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(
                        text = "حفظ الخطة والهدف",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
