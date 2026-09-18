package com.example.katzu.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.NavigationTab
import com.example.katzu.ui.theme.*
import com.example.katzu.util.KatzuHaptics

@Composable
fun TopHeaderBar(
    currentTab: NavigationTab = NavigationTab.Trail,
    streakDays: Int,
    xp: Int,
    levelProgress: Float = 0.45f,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side (in RTL, this is start): Dynamic Tab Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (currentTab) {
                        NavigationTab.Trail -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCardSubtle)
                                    .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.katzu_trail_header),
                                    contentDescription = "Katzu Mascot",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column {
                                Text(
                                    text = "المسار",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Katzu • خطة التعلّم",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        NavigationTab.Practice -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCardSubtle)
                                    .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "التدريب",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "التدريب",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "مفردات وقواعد وتكرار",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        NavigationTab.Progress -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCardSubtle)
                                    .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = "التقدم",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "التقدم",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "الأداء وسجل التدريب",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        NavigationTab.Profile -> {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCardSubtle)
                                    .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "الملف الشخصي",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "حسابي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "الإعدادات والاشتراك",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Left side (in RTL, this is end): Streak flame badge + Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Unified Streak & XP pill: [🔥 3 | ⚡ 120 XP]
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = StatusLearning,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$streakDays",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            // Subtle vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(BorderSubtle)
                            )

                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "XP",
                                tint = StatusLearning,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$xp XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusLearning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Profile Avatar Button
                    val isProfileTab = currentTab == NavigationTab.Profile
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSubtle)
                            .border(
                                width = if (isProfileTab) 2.dp else 1.dp,
                                color = if (isProfileTab) Primary else Primary.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable(enabled = !isProfileTab, onClick = onProfileClick)
                            .testTag("top_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.user_avatar),
                            contentDescription = "حسابي",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Slim level progress bar along the bottom of the header
            LinearProgressIndicator(
                progress = { levelProgress.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Primary,
                trackColor = BorderSubtle
            )
        }
    }
}

@Composable
fun KatzuBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Primary.copy(alpha = 0.3f),
                    spotColor = Primary.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceCard.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    title = "المسار",
                    icon = Icons.Default.Route,
                    isSelected = currentTab == NavigationTab.Trail,
                    onClick = { onTabSelected(NavigationTab.Trail) },
                    testTag = "nav_tab_trail"
                )
                NavTabItem(
                    title = "التدريب",
                    icon = Icons.Default.FitnessCenter,
                    isSelected = currentTab == NavigationTab.Practice,
                    onClick = { onTabSelected(NavigationTab.Practice) },
                    testTag = "nav_tab_practice"
                )
                NavTabItem(
                    title = "التقدم",
                    icon = Icons.Default.Insights,
                    isSelected = currentTab == NavigationTab.Progress,
                    onClick = { onTabSelected(NavigationTab.Progress) },
                    testTag = "nav_tab_progress"
                )
                NavTabItem(
                    title = "حسابي",
                    icon = Icons.Default.AccountCircle,
                    isSelected = currentTab == NavigationTab.Profile,
                    onClick = { onTabSelected(NavigationTab.Profile) },
                    testTag = "nav_tab_profile"
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .bouncyClickable {
                KatzuHaptics.tick(haptic, context)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) Primary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Primary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 14.dp, height = 3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Primary, Secondary)
                        )
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Composable
fun ArticleBadge(article: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (article.lowercase()) {
        "der" -> ArticleDer.copy(alpha = 0.2f) to ArticleDer
        "die" -> ArticleDie.copy(alpha = 0.2f) to ArticleDie
        "das" -> ArticleDas.copy(alpha = 0.2f) to ArticleDas
        else -> SurfaceCardSubtle to TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = article,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AudioWaveformBar(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val heights = listOf(14.dp, 24.dp, 36.dp, 28.dp, 40.dp, 22.dp, 16.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val animHeight by infiniteTransition.animateValue(
                initialValue = 6.dp,
                targetValue = if (isListening) baseHeight else 4.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300 + index * 60, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animHeight)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        if (index % 2 == 0) Primary else Tertiary
                    )
            )
        }
    }
}

@Composable
fun ContentEmptyStateView(
    title: String = "لا يوجد محتوى متاح حالياً",
    message: String = "لا يوجد اتصال بالإنترنت — يرجى الاتصال لتحميل المحتوى.",
    buttonText: String = "إعادة المحاولة",
    isRetrying: Boolean = false,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, Primary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.katzu_trail_header),
                contentDescription = "Katzu waiting",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRetry,
            enabled = !isRetrying,
            shape = RoundedCornerShape(9999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = TextPrimary,
                disabledContainerColor = Primary.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .height(48.dp)
                .testTag("retry_sync_button")
        ) {
            if (isRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جارٍ الاتصال...",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modern bouncy spring press interaction with tactile haptics and GPU layer scaling.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.95f,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            KatzuHaptics.press(haptic)
            animatedScale.animateTo(
                targetValue = targetScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    this
        .graphicsLayer {
            scaleX = animatedScale.value
            scaleY = animatedScale.value
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = {
                KatzuHaptics.tick(haptic, context)
                onClick()
            }
        )
}
