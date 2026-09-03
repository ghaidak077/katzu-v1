package com.example.katzu.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.NavigationTab
import com.example.katzu.ui.theme.*

@Composable
fun TopHeaderBar(
    streakDays: Int,
    xp: Int,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = BackgroundPure.copy(alpha = 0.85f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right side (in RTL, this is start): Mascot + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.katzu_mascot),
                        contentDescription = "Katzu Mascot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column {
                    Text(
                        text = "Trail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Katzu • الألمانية بذكاء",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Left side (in RTL, this is end): Streak flame badge + Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak & XP pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = StatusLearning,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$streakDays أيام",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(TextMuted)
                    )
                    Text(
                        text = "$xp XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusLearning,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard)
                        .border(1.dp, Primary.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = onProfileClick)
                        .testTag("top_profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "حسابي",
                        tint = Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
