package com.example.katzu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.TrailNode
import com.example.katzu.model.TrailNodeStatus
import com.example.katzu.model.UserProfile
import com.example.katzu.ui.theme.*

@Composable
fun TrailScreen(
    userProfile: UserProfile,
    trailNodes: List<TrailNode>,
    onSelectScenario: (String) -> Unit
) {
    var selectedLevelIndex by remember { mutableStateOf(0) }
    val levels = listOf("A1 • مبتدئ", "A2 • أساسي", "B1 • متوسط", "B2 • متقدم")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Daily Focus Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Primary.copy(alpha = 0.2f),
                        spotColor = Primary.copy(alpha = 0.3f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "التحدي اليومي A1.1",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "متبقي 4 ساعات",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                                painter = painterResource(id = R.drawable.katzu_thumbs_up),
                                contentDescription = "Katzu Mascot",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "مرحباً، ${userProfile.name}.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "“هل أنت مستعد لمواجهة قواعد المجرور (Dativ) اليوم قبل أن تبرد قهوتك؟”",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Next lesson progress inside Daily Focus
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardSubtle,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "الدرس التالي: Im Café bestellen",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "طلب قهوة وحساب المقهى • خطوة 2 من 4",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = { onSelectScenario("im_cafe_bestellen") },
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("trail_continue_button")
                            ) {
                                Text(
                                    text = "متابعة",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Level Segmented Selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    levels.forEachIndexed { index, levelTitle ->
                        val isSelected = selectedLevelIndex == index
                        val isLocked = index > 1

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isLocked) selectedLevelIndex = index
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp).padding(end = 2.dp)
                                    )
                                }
                                Text(
                                    text = levelTitle.split(" • ")[0],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> TextPrimary
                                        isLocked -> TextMuted
                                        else -> TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title: مسار التعلّم
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مسار التعلّم التفاعلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "A1.1 المستوى المبتدئ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Learning Trail Timeline Nodes
        itemsIndexed(trailNodes) { index, node ->
            TrailNodeItem(
                node = node,
                isLast = index == trailNodes.size - 1,
                onClick = {
                    if (node.isClickable) {
                        onSelectScenario(node.id)
                    }
                }
            )
        }

        // Motivational Footer Quip Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.katzu_trail_guide),
                            contentDescription = "Katzu Tip",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "ملاحظة كاتزو السريعة",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusLearning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الألمان يقدّرون من يقول Bitte حتى وإن كانت بقية جملتك متعثرة تماماً. لا تخف من المحاولة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailNodeItem(
    node: TrailNode,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = node.isClickable, onClick = onClick)
            .testTag("trail_node_${node.id}"),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline Indicator Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (node.status) {
                            TrailNodeStatus.Mastered -> StatusSuccess.copy(alpha = 0.2f)
                            TrailNodeStatus.Active -> PrimaryContainer
                            TrailNodeStatus.Upcoming -> SurfaceCardSubtle
                            TrailNodeStatus.Locked -> SurfaceCardSubtle.copy(alpha = 0.5f)
                        }
                    )
                    .border(
                        1.5.dp,
                        when (node.status) {
                            TrailNodeStatus.Mastered -> StatusSuccess
                            TrailNodeStatus.Active -> Primary
                            TrailNodeStatus.Upcoming -> BorderSubtle
                            TrailNodeStatus.Locked -> Color.Transparent
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (node.status) {
                    TrailNodeStatus.Mastered -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "مكتمل",
                        tint = StatusSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    TrailNodeStatus.Active -> Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "الدرس الحالي",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    TrailNodeStatus.Upcoming -> Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "قريباً",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    TrailNodeStatus.Locked -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "مغلق",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(
                            if (node.status == TrailNodeStatus.Mastered) StatusSuccess.copy(alpha = 0.5f)
                            else BorderSubtle
                        )
                )
            }
        }

        // Node Content Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (node.status == TrailNodeStatus.Active) SurfaceCard
                else SurfaceCardSubtle
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (node.status == TrailNodeStatus.Active) BorderActive
                else BorderSubtle
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
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
                        text = node.arabicTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (node.status == TrailNodeStatus.Locked) TextMuted else TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = when (node.status) {
                            TrailNodeStatus.Mastered -> StatusSuccess.copy(alpha = 0.15f)
                            TrailNodeStatus.Active -> Primary.copy(alpha = 0.15f)
                            else -> SurfaceContainerLow
                        }
                    ) {
                        Text(
                            text = when (node.status) {
                                TrailNodeStatus.Mastered -> "${node.accuracyPercent}% إتقان"
                                TrailNodeStatus.Active -> node.stepInfo
                                TrailNodeStatus.Upcoming -> "متاح قريباً"
                                TrailNodeStatus.Locked -> "مغلق"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (node.status) {
                                TrailNodeStatus.Mastered -> StatusSuccess
                                TrailNodeStatus.Active -> Primary
                                else -> TextMuted
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = node.germanTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (node.status == TrailNodeStatus.Locked) TextMuted else TextSecondary,
                    fontFamily = FontFamily.Serif
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = node.timeEstimate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = "• ${node.levelTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
