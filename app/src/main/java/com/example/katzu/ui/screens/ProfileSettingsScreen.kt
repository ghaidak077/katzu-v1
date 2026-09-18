package com.example.katzu.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.data.SecurityAndSubscriptionManager
import com.example.katzu.data.SessionEntity
import com.example.katzu.data.UserEntity
import com.example.katzu.model.UserProfile
import com.example.katzu.ui.components.GoalSelectionBottomSheet
import com.example.katzu.ui.theme.*
import com.example.katzu.util.AppLogger
import com.example.katzu.util.NotificationHelper

@Composable
fun ProfileSettingsScreen(
    userProfile: UserProfile,
    userEntity: UserEntity? = null,
    sessions: List<SessionEntity> = emptyList(),
    savedWordsCount: Int = 0,
    onUpdateSpeechSpeed: (Float) -> Unit = {},
    onUpdateSarcasm: (String) -> Unit = {},
    onUpdateReminder: (enabled: Boolean, frequencyPreset: String, hour: Int, minute: Int) -> Unit = { _, _, _, _ -> },
    onUpdateGoal: (goalId: String, title: String, targetDays: Int, targetMinutes: Int, intensityPreset: String) -> Unit = { _, _, _, _, _ -> },
    onSendTestNotification: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current

    val displayName = userProfile.name.trim().ifBlank { null }
        ?: userEntity?.displayName?.trim()?.ifBlank { null }
        ?: userEntity?.googleAccountEmail?.substringBefore("@")?.ifBlank { null }
        ?: "مستخدم كاتزو"

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf(displayName) }

    val displayEmail = userEntity?.googleAccountEmail?.ifBlank { null }
        ?: userEntity?.email?.ifBlank { null }
        ?: if (userProfile.email.isNotBlank()) userProfile.email else "حساب غير متصل"

    val isSubActive = userEntity?.isSubscriptionActive == true &&
        SecurityAndSubscriptionManager.isLocallyActive(userEntity.subscriptionExpiresAt)

    val expiryFormatted = SecurityAndSubscriptionManager.formatExpirationDate(userEntity?.subscriptionExpiresAt)

    val computedStreak = if (sessions.isNotEmpty()) {
        val days = sessions.map {
            java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.distinct().sortedDescending()
        var streak = 0
        var current = java.time.LocalDate.now()
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

    val totalWordsLearned = if (sessions.isNotEmpty()) sessions.sumOf { it.wordsLearned } else savedWordsCount

    var promoCode by remember { mutableStateOf("") }
    var isPromoApplied by remember { mutableStateOf(false) }
    var selectedLanguageIndex by remember { mutableIntStateOf(0) } // 0: العربية, 1: English
    var selectedSpeedIndex by remember { mutableIntStateOf(if (userProfile.speechSpeed <= 0.85f) 1 else 0) } // 0: عادي (1.0x), 1: بطيء (0.8x)
    var showRealtimeTranslation by remember { mutableStateOf(true) }
    var showGoalSheet by remember { mutableStateOf(false) }

    var dailyRemindersEnabled by remember(userProfile.dailyRemindersEnabled) { mutableStateOf(userProfile.dailyRemindersEnabled) }
    var reminderFrequency by remember(userProfile.reminderFrequencyPreset) { mutableStateOf(userProfile.reminderFrequencyPreset) }
    var reminderHour by remember(userProfile.reminderHour) { mutableIntStateOf(userProfile.reminderHour) }
    var reminderMinute by remember(userProfile.reminderMinute) { mutableIntStateOf(userProfile.reminderMinute) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dailyRemindersEnabled = true
            onUpdateReminder(true, reminderFrequency, reminderHour, reminderMinute)
            Toast.makeText(context, "تم تفعيل التنبيهات بنجاح 🔔", Toast.LENGTH_SHORT).show()
        } else {
            dailyRemindersEnabled = false
            onUpdateReminder(false, reminderFrequency, reminderHour, reminderMinute)
            Toast.makeText(context, "إذن التنبيهات غير مفعّل في النظام", Toast.LENGTH_SHORT).show()
        }
    }

    // Dialog states for informational sections
    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .padding(horizontal = 20.dp)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Profile & Identity Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_identity_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardSubtle)
                                .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_settings_mascot),
                                contentDescription = "Katzu Mascot",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            tempNameInput = displayName
                                            showEditNameDialog = true
                                        }
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الاسم",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = if (isSubActive) PrimaryContainer.copy(alpha = 0.25f) else SurfaceCardSubtle
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSubActive) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isSubActive) Primary else TextMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isSubActive) "Katzu Pro • مفعّل" else "خطة مجانية",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSubActive) Primary else TextMuted,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = displayEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Quick Stats Grid Inside Profile Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCardSubtle.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Streak
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "أيام الحماس",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = StatusLearning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$computedStreak",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Column 2: Conversation Level
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "مستوى المحادثة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = ArticleDer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = userProfile.currentLevel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Column 3: Mastered Phrases
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "العبارات المتقنة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = StatusSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$totalWordsLearned",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Subscription & Pro Perks Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscription_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSubActive) Primary.copy(alpha = 0.15f) else SurfaceCardSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (isSubActive) Primary else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isSubActive) "اشتراك Katzu Pro نشط" else "الاشتراك غير مفعّل",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isSubActive) "صالح حتى $expiryFormatted" else "قم بتفعيل كود الاشتراك للوصول الكامل",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isSubActive) StatusSuccess.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSubActive) StatusSuccess else StatusError)
                                )
                                Text(
                                    text = if (isSubActive) "فعّال" else "غير نشط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSubActive) StatusSuccess else StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Mini Perks Bento
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCardSubtle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "محادثات لا نهائية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "بدون وقت انتظار",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = ArticleDas,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "سيناريوهات أوفلاين",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Promo Code Redeem Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "هل لديك كود ترويجي أو إهداء؟",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoCode,
                                onValueChange = { promoCode = it },
                                placeholder = {
                                    Text(
                                        text = "أدخل رمز القسيمة...",
                                        color = TextMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("promo_code_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceContainerLow,
                                    unfocusedContainerColor = SurfaceContainerLow,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (promoCode.isNotBlank()) {
                                        isPromoApplied = true
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("redeem_promo_button")
                            ) {
                                Text(
                                    text = if (isPromoApplied) "مفعّل ✓" else "تفعيل",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2.5 Learning Goal & Pace Card (Duolingo Style)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("learning_goal_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = StatusLearning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "خطة التعلّم والهدف الشخصي",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${userProfile.targetWeeklyDays} أيام/أسبوع",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceCardSubtle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = userProfile.targetGoalTitle.ifBlank { "الوصول إلى مستوى A2 والتحدث بثقة" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "الوتيرة اليومية: ${userProfile.targetDailyMinutes} دقيقة تدريب صوتي • التكرار الأسبوعي: ${userProfile.targetWeeklyDays} أيام",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showGoalSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("change_learning_goal_button"),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerLow,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Text("تعديل الهدف والوتيرة الأسبوعية", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 3. App & Learning Preferences Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_preferences_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "تفضيلات التطبيق والتعلّم",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    // Row 1: Interface Language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "لغة الواجهة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        // Segmented Control Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SegmentedOption(
                                    text = "العربية",
                                    isSelected = selectedLanguageIndex == 0,
                                    onClick = { selectedLanguageIndex = 0 },
                                    testTag = "lang_ar_button"
                                )
                                SegmentedOption(
                                    text = "English",
                                    isSelected = selectedLanguageIndex == 1,
                                    onClick = { selectedLanguageIndex = 1 },
                                    testTag = "lang_en_button"
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceCardSubtle, thickness = 1.dp)

                    // Row 2: Coach Voice Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "سرعة صوت كاتزو",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "معدل نطق الجمل الألمانية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Segmented Control Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceContainerLow,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SegmentedOption(
                                    text = "عادي",
                                    isSelected = selectedSpeedIndex == 0,
                                    onClick = {
                                        selectedSpeedIndex = 0
                                        onUpdateSpeechSpeed(1.0f)
                                    },
                                    testTag = "speed_normal_button"
                                )
                                SegmentedOption(
                                    text = "بطيء",
                                    isSelected = selectedSpeedIndex == 1,
                                    onClick = {
                                        selectedSpeedIndex = 1
                                        onUpdateSpeechSpeed(0.8f)
                                    },
                                    testTag = "speed_slow_button"
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceCardSubtle, thickness = 1.dp)

                    // Row 3: Real-time Translation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "إظهار الترجمة الفورية",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "عرض المعنى العربي فور الاستماع",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        StitchSwitch(
                            checked = showRealtimeTranslation,
                            onCheckedChange = { showRealtimeTranslation = it },
                            testTag = "realtime_translation_switch"
                        )
                    }

                    HorizontalDivider(color = SurfaceCardSubtle, thickness = 1.dp)

                    // Row 4: Real Notification Settings (Frequency & Schedule)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (dailyRemindersEnabled) Primary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "التنبيهات وتذكيرات التعلّم الذكية",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    val timeLabel = when (reminderHour) {
                                        20 -> "8:00 مساءً"
                                        14 -> "2:00 بعد الظهر"
                                        9 -> "9:00 صباحاً"
                                        22 -> "10:00 مساءً"
                                        else -> "$reminderHour:00"
                                    }
                                    val freqLabel = when (reminderFrequency) {
                                        "casual" -> "خفيف (3 أيام/أسبوع)"
                                        "regular" -> "منتظم (5 أيام/أسبوع)"
                                        "serious" -> "جاد (يومياً)"
                                        "intense" -> "مكثف (مرتان يومياً)"
                                        else -> "منتظم (5 أيام/أسبوع)"
                                    }
                                    Text(
                                        text = if (dailyRemindersEnabled) "$freqLabel • $timeLabel" else "التنبيهات معطلة حالياً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            StitchSwitch(
                                checked = dailyRemindersEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (!NotificationHelper.hasNotificationPermission(context)) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            dailyRemindersEnabled = true
                                            onUpdateReminder(true, reminderFrequency, reminderHour, reminderMinute)
                                            Toast.makeText(context, "تم تفعيل التنبيهات 🔔", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        dailyRemindersEnabled = false
                                        onUpdateReminder(false, reminderFrequency, reminderHour, reminderMinute)
                                        Toast.makeText(context, "تم إيقاف التنبيهات", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                testTag = "daily_reminders_switch"
                            )
                        }

                        if (dailyRemindersEnabled) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceCardSubtle,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Frequency Presets
                                    Text(
                                        text = "تكرار التذكير الأسبوعي:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val freqOptions = listOf(
                                            "casual" to "3 أيام",
                                            "regular" to "5 أيام",
                                            "serious" to "يومياً",
                                            "intense" to "مكثف"
                                        )
                                        freqOptions.forEach { (presetKey, presetLabel) ->
                                            val isSelected = reminderFrequency == presetKey
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Primary else SurfaceContainerLow,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        reminderFrequency = presetKey
                                                        onUpdateReminder(true, presetKey, reminderHour, reminderMinute)
                                                    }
                                                    .testTag("freq_chip_$presetKey")
                                            ) {
                                                Text(
                                                    text = presetLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) BackgroundPure else TextPrimary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Preferred Time
                                    Text(
                                        text = "الموعد المفضل للتنبيه:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val timeOptions = listOf(
                                            9 to "9 ص",
                                            14 to "2 م",
                                            20 to "8 م",
                                            22 to "10 م"
                                        )
                                        timeOptions.forEach { (hour, label) ->
                                            val isSelected = reminderHour == hour
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Primary else SurfaceContainerLow,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        reminderHour = hour
                                                        onUpdateReminder(true, reminderFrequency, hour, 0)
                                                    }
                                                    .testTag("time_chip_$hour")
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) BackgroundPure else TextPrimary,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Send test notification button
                                    Button(
                                        onClick = {
                                            onSendTestNotification()
                                            Toast.makeText(context, "تم إرسال إشعار تجريبي 🐾 تفقد شريط التنبيهات!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .testTag("test_notification_button"),
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SurfaceContainerLow,
                                            contentColor = Primary
                                        ),
                                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "إرسال إشعار تجريبي الآن للتأكد 🔔",
                                                style = MaterialTheme.typography.labelMedium,
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

        // 4. Sarcastic Mascot Voice Box Note
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mascot_voice_note_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.katzu_profile_card),
                            contentDescription = "Katzu",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Bidi isolation around German articles: \u2066(der, die, das)\u2069
                        Text(
                            text = "«إيقاف التنبيهات لن يعفيك من حفظ جنس الأسماء \u2066(der, die, das)\u2069.. سأبقى بانتظارك.»",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "— كاتزو",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // 5. About & Support Section
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_support_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "حول التطبيق والدعم",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Privacy Policy
                    AboutMenuItem(
                        icon = Icons.Default.VerifiedUser,
                        title = "سياسة الخصوصية",
                        onClick = {
                            activeDialogTitle = "سياسة الخصوصية"
                            activeDialogContent = "بياناتك الشخصية وتطور محادثاتك تُحفظ بأمان محلياً وسحابياً لأغراض تقييم تقدمك اللغوي فقط دون مشاركتها مع أي جهة خارجية."
                        },
                        testTag = "privacy_policy_row"
                    )

                    // Terms of Use
                    AboutMenuItem(
                        icon = Icons.Default.Gavel,
                        title = "شروط الاستخدام",
                        onClick = {
                            activeDialogTitle = "شروط الاستخدام"
                            activeDialogContent = "استخدامك لتطبيق كاتزو يخضع لمعايير التعلم التفاعلي النزيه. الردود الذكية مصممة لتدريبك على المحادثة الألمانية الواقعية."
                        },
                        testTag = "terms_row"
                    )

                    // Help Center & FAQ
                    AboutMenuItem(
                        icon = Icons.Default.HelpCenter,
                        title = "مركز المساعدة والأسئلة الشائعة",
                        onClick = {
                            activeDialogTitle = "مركز المساعدة والأسئلة الشائعة"
                            activeDialogContent = "هل تحتاج إلى استفسار حول السيناريوهات أو تفعيل القسائم أو قواعد النحو الألمانية؟ يمكنك التواصل المباشر عبر ghaidak.com أو متابعة إرشادات كاتزو التفاعلية."
                        },
                        testTag = "help_center_row"
                    )

                    // Diagnostic Log Tools (strictly debug builds only)
                    if (com.example.katzu.BuildConfig.DEBUG) {
                        AboutMenuItem(
                            icon = Icons.Default.BugReport,
                            title = "إرسال سجل الأخطاء",
                            onClick = {
                                AppLogger.shareLogFile(context)
                            },
                            testTag = "send_error_log_row"
                        )

                        AboutMenuItem(
                            icon = Icons.Default.ContentCopy,
                            title = "نسخ آخر سجل",
                            onClick = {
                                AppLogger.copyLastLinesToClipboard(context, 50)
                            },
                            testTag = "copy_last_log_row"
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Version & Mandatory Attribution
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Katzu v1.0.4 (Build 42)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "صُنع بواسطة غيدق علوش — ghaidak.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ghaidak.com"))
                                    context.startActivity(intent)
                                }
                                .testTag("attribution_link")
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 6. Sign Out Trigger
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable { showSignOutDialog = true }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .testTag("sign_out_button"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        tint = StatusError,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "تسجيل الخروج",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            }
        }
    }

    // Info Dialog for Privacy, Terms, FAQ
    if (activeDialogTitle != null && activeDialogContent != null) {
        AlertDialog(
            onDismissRequest = {
                activeDialogTitle = null
                activeDialogContent = null
            },
            title = {
                Text(
                    text = activeDialogTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = activeDialogContent.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeDialogTitle = null
                        activeDialogContent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("حسناً", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "تسجيل الخروج من الحساب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "«هل تريد المغادرة حقاً؟ سأظل هنا في انتظارك، ولن تسقط أيام حماسك ما دمت عائداً قريباً.»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("تأكيد الخروج", color = BackgroundPure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text("إلغاء", color = TextMuted)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showGoalSheet) {
        GoalSelectionBottomSheet(
            currentGoalId = userProfile.targetGoalId,
            currentIntensityPreset = reminderFrequency,
            onDismiss = { showGoalSheet = false },
            onSaveGoal = { goalId, goalTitle, targetDays, targetMinutes, intensityPreset, _ ->
                reminderFrequency = intensityPreset
                onUpdateGoal(goalId, goalTitle, targetDays, targetMinutes, intensityPreset)
                onUpdateReminder(dailyRemindersEnabled, intensityPreset, reminderHour, reminderMinute)
                showGoalSheet = false
                Toast.makeText(context, "تم حفظ هدفك وخطة التعلّم بنجاح! 🎯", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    text = "ما الذي يجب علي مناداتك؟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الاسم المفضل الذي يوجهه لك كاتزو في المحادثات:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        placeholder = { Text("اسمك المفضل...", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceContainerLow,
                            unfocusedContainerColor = SurfaceContainerLow
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempNameInput.isNotBlank()) {
                            onUpdateName(tempNameInput.trim())
                            Toast.makeText(context, "تم تحديث اسم المناداة بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("حفظ الاسم", color = BackgroundPure, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SegmentedOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BackgroundPure else TextMuted,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StitchSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        label = "switch_thumb_offset"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(if (checked) PrimaryContainer else SurfaceCardSubtle)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            }
            .testTag(testTag),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) TextPrimary else TextMuted)
        )
    }
}

@Composable
private fun AboutMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
