package com.example.katzu.ui.components

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.GermanGender
import com.example.katzu.model.VocabularyWord
import com.example.katzu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordInsightBottomSheet(
    word: VocabularyWord?,
    onDismiss: () -> Unit,
    onSpeak: (String, Float) -> Unit,
    onToggleBookmark: (String) -> Unit
) {
    if (word == null) return

    var isSlowSpeed by remember { mutableStateOf(false) }
    var isSaved by remember(word) { mutableStateOf(word.isSaved) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Action Buttons: Audio, Speed, Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Play
                    IconButton(
                        onClick = {
                            val fullText = if (word.article.isNotBlank()) "${word.article} ${word.germanWord}" else word.germanWord
                            onSpeak(fullText, if (isSlowSpeed) 0.75f else 1.0f)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSubtle)
                            .testTag("word_insight_speak_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "استمع للنطق",
                            tint = Primary
                        )
                    }

                    // Slow speed toggle
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = if (isSlowSpeed) SecondaryContainer.copy(alpha = 0.5f) else SurfaceCardSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { isSlowSpeed = !isSlowSpeed }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = "Slow Speed",
                                tint = if (isSlowSpeed) Primary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isSlowSpeed) "0.75x بطيء" else "0.75x",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSlowSpeed) Primary else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bookmark button
                IconButton(
                    onClick = {
                        isSaved = !isSaved
                        onToggleBookmark(word.id)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSaved) SecondaryContainer.copy(alpha = 0.4f) else SurfaceCardSubtle)
                        .testTag("word_insight_bookmark_btn")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "حفظ الكلمة",
                        tint = if (isSaved) Primary else TextSecondary
                    )
                }
            }

            // Word Title & Phonetics
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (word.article.isNotBlank()) {
                        val articleColor = when (word.gender) {
                            GermanGender.Masculine -> ArticleDer
                            GermanGender.Feminine -> ArticleDie
                            GermanGender.Neuter -> ArticleDas
                            GermanGender.None -> TextSecondary
                        }
                        Text(
                            text = word.article,
                            style = MaterialTheme.typography.headlineLarge,
                            color = articleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = word.germanWord,
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(TextMuted)
                    )
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle
                    ) {
                        Text(
                            text = word.level,
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Linguistic Badges (Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = SurfaceCardSubtle
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = word.partOfSpeech,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (word.gender != GermanGender.None) {
                    val (genderLabel, genderColor) = when (word.gender) {
                        GermanGender.Masculine -> "مذكر • Maskulinum" to ArticleDer
                        GermanGender.Feminine -> "مؤنث • Femininum" to ArticleDie
                        GermanGender.Neuter -> "محايد • Neutrum" to ArticleDas
                        else -> "" to TextSecondary
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Male,
                                contentDescription = null,
                                tint = genderColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = genderLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = genderColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (word.plural.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCardSubtle
                    ) {
                        Text(
                            text = "Plural: ${word.plural}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Primary Translation Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardSubtle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المعنى العربي الأساسي",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = word.arabicMeaning,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "EN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = word.englishMeaning,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Contextual Example Sentence Block
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCardSubtle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "جملة توضيحية سياقية",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { onSpeak(word.exampleGerman, 1.0f) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "استمع للجملة",
                                tint = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "“${word.exampleGerman}”",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif
                    )

                    Text(
                        text = "“${word.exampleArabic}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Katzu Mnemonic / Cultural Tip Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusLearning.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.katzu_word_insight),
                            contentDescription = "Katzu Tip",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "تلميح كاتزو الذكي",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusLearning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = word.mnemonicTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Primary Action: Add to Smart Review Decks
            Button(
                onClick = {
                    isSaved = !isSaved
                    onToggleBookmark(word.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("word_insight_deck_action"),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) StatusSuccess.copy(alpha = 0.25f) else PrimaryContainer,
                    contentColor = if (isSaved) StatusSuccess else TextPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.Style,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isSaved) "تمت الإضافة إلى بطاقات المراجعة" else "إضافة إلى بطاقات المراجعة الذكية",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
