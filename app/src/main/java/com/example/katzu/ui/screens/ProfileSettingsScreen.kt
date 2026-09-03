package com.example.katzu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.UserProfile
import com.example.katzu.ui.theme.*

@Composable
fun ProfileSettingsScreen(
    userProfile: UserProfile,
    onUpdateSpeechSpeed: (Float) -> Unit,
    onUpdateSarcasm: (String) -> Unit
) {
    var promoCode by remember { mutableStateOf("") }
    var isPromoApplied by remember { mutableStateOf(false) }
    var speechSpeed by remember { mutableStateOf(userProfile.speechSpeed) }
    var sarcasmLevel by remember { mutableStateOf(userProfile.sarcasmLevel) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Profile & Identity Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
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
                                .border(1.dp, Primary.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_mascot),
                                contentDescription = "Profile Mascot",
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
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = Primary.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Katzu Pro • سنوي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = userProfile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Quick Stats Inside Profile
                    Surface(
                        shape = RoundedCornerShape(16.dp),
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "أيام الحماس", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = StatusLearning, modifier = Modifier.size(14.dp))
                                    Text(text = "18", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Box(modifier = Modifier.size(1.dp, 24.dp).background(BorderSubtle))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "المستوى", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = ArticleDer, modifier = Modifier.size(14.dp))
                                    Text(text = "A2.2", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Box(modifier = Modifier.size(1.dp, 24.dp).background(BorderSubtle))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "العبارات المتقنة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                                    Text(text = "342", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subscription & Pro Perks Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
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
                                    .background(Primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(text = "اشتراك Katzu Pro نشط", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "يتجدد الاشتراك في 15 أكتوبر 2025", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = StatusSuccess.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StatusSuccess))
                                Text(text = "فعّال", style = MaterialTheme.typography.labelSmall, color = StatusSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Mini Perks Bento
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardSubtle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Text("محادثات لا نهائية", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
                                Text("بدون وقت انتظار", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                Text("سيناريوهات أوفلاين", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }

                    // Promo code redeem row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "هل لديك كود ترويجي أو هدية؟", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoCode,
                                onValueChange = { promoCode = it },
                                placeholder = { Text("أدخل رمز القسيمة...", color = TextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SurfaceCardSubtle,
                                    unfocusedContainerColor = SurfaceCardSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Button(
                                onClick = { isPromoApplied = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    text = if (isPromoApplied) "مفعّل ✓" else "تفعيل",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // App & Learning Preferences Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تفضيلات التطبيق والتعلّم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Speech speed
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Text("سرعة نطق كاتزو الألماني", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            Text(
                                text = "${speechSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }

                        Slider(
                            value = speechSpeed,
                            onValueChange = {
                                speechSpeed = it
                                onUpdateSpeechSpeed(it)
                            },
                            valueRange = 0.7f..1.2f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = Primary,
                                activeTrackColor = Primary,
                                inactiveTrackColor = SurfaceCardSubtle
                            )
                        )
                    }

                    // Sarcasm Level
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mood, contentDescription = null, tint = StatusLearning, modifier = Modifier.size(20.dp))
                            Text("درجة سخرية كاتزو في التصحيح", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("لاذع وساخر", "متزن ومساعد").forEach { level ->
                                val isSelected = sarcasmLevel == level
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) PrimaryContainer else SurfaceCardSubtle,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            sarcasmLevel = level
                                            onUpdateSarcasm(level)
                                        }
                                ) {
                                    Text(
                                        text = level,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Daily Notifications Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("تذكيرات الحماس اليومية", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text("تنبيه لطيف قبل انقطاع أيام الحماس", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceCardSubtle
                            )
                        )
                    }
                }
            }
        }
    }
}
