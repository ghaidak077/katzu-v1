package com.example.katzu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.katzu.model.VocabularyWord
import com.example.katzu.ui.theme.*

@Composable
fun FlashcardsDialog(
    words: List<VocabularyWord>,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val currentWord = words.getOrNull(currentIndex) ?: return

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }

                    Text(
                        text = "بطاقات التكرار المتباعد (${currentIndex + 1} / ${words.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = {
                        val text = if (currentWord.article.isNotBlank()) "${currentWord.article} ${currentWord.germanWord}" else currentWord.germanWord
                        onSpeak(text)
                    }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "نطق", tint = Primary)
                    }
                }

                // Flip Flashcard Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceCardSubtle)
                        .border(1.dp, if (isFlipped) Primary.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(20.dp))
                        .clickable { isFlipped = !isFlipped }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // Front Side: German
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (currentWord.article.isNotBlank()) {
                                Text(
                                    text = currentWord.article,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = currentWord.germanWord,
                                style = MaterialTheme.typography.displayLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentWord.phonetic,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "👆 انقر لقلب البطاقة ومعرفة المعنى",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    } else {
                        // Back Side: Arabic Meaning & Example (mirrored back so it's readable)
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentWord.arabicMeaning,
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "“${currentWord.exampleGerman}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryFixedDim,
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = currentWord.exampleArabic,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) {
                                isFlipped = false
                                currentIndex--
                            }
                        },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Text("السابق")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < words.size - 1) {
                                isFlipped = false
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer)
                    ) {
                        Text(
                            text = if (currentIndex < words.size - 1) "التالي" else "إنهاء التمرين",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarCheatSheetDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                    Text(
                        text = "دليل القواعد السريع • Katzu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rule 1: Der Die Das Colors
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "1. أدوات التعريف وألوان كاتزو الثلاثية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "الألمانية لا تتبع المنطق البشري في جنس الكلمات، لذا ابتكرنا نظام الألوان لتذكرها بالبصر:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDer.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("der", color = ArticleDer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المذكر", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("der Kaffee", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDie.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("die", color = ArticleDie, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المؤنث", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("die Milch", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArticleDas.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("das", color = ArticleDas, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("المحايد", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                        Text("das Wasser", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Rule 2: Akkusativ
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "2. المفعول به (Akkusativ) في المقهى",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "المذكر هو الوحيد الذي يتغير! Der تصبح den و ein تصبح einen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Ich möchte EINEN Kaffee (وليس ein Kaffee)",
                                    modifier = Modifier.padding(12.dp),
                                    color = StatusLearning,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }

                    // Rule 3: Dativ
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "3. المجرور (Dativ) مع حرف الجر mit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "بعد كلمة mit يأتي دائماً مجرور: der/das ينقلبان إلى dem، و die تنقلب إلى der.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "mit der Milch / mit dem Zucker",
                                    modifier = Modifier.padding(12.dp),
                                    color = PrimaryFixedDim,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
