package com.example.katzu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.ChatMessage
import com.example.katzu.model.MessageSender
import com.example.katzu.ui.components.AudioWaveformBar
import com.example.katzu.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LiveConversationScreen(
    initialMessages: List<ChatMessage>,
    onBack: () -> Unit,
    onFinishSession: () -> Unit,
    onSpeak: (String, Float) -> Unit
) {
    var messages by remember { mutableStateOf(initialMessages) }
    var showTranslation by remember { mutableStateOf(true) }
    var isListening by remember { mutableStateOf(false) }
    var userDraftInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickReplies = listOf(
        "Ich möchte bitte einen Kaffee mit Hafermilch.",
        "Haben Sie heute frischen Kuchen?",
        "Könnte ich bitte zahlen?"
    )

    fun sendUserMessage(germanText: String) {
        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            sender = MessageSender.User,
            germanText = germanText,
            arabicTranslation = "رسالتك الصوتية"
        )
        messages = messages + userMsg

        // Trigger Katzu dynamic reply
        coroutineScope.launch {
            kotlinx.coroutines.delay(1200)
            val katzuReply = when {
                germanText.contains("zahlen", ignoreCase = true) -> ChatMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    sender = MessageSender.Katzu,
                    germanText = "Das macht zusammen 3,80 Euro. Zahlen Sie bar oder mit Karte?",
                    arabicTranslation = "الحساب الإجمالي 3.80 يورو. هل تدفع نقداً أم بالبطاقة؟"
                )
                germanText.contains("Kuchen", ignoreCase = true) -> ChatMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    sender = MessageSender.Katzu,
                    germanText = "Ja, wir haben Apfelstrudel und Käsekuchen. Beides frisch.",
                    arabicTranslation = "نعم، لدينا شترودل التفاح وكعكة الجبن. كلاهما طازج."
                )
                else -> ChatMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    sender = MessageSender.Katzu,
                    germanText = "Sehr gerne. Einen Moment bitte, kommt sofort!",
                    arabicTranslation = "بكل سرور. لحظة من فضلك، طلبك قادم فوراً!"
                )
            }
            messages = messages + katzuReply
            onSpeak(katzuReply.germanText, 1.0f)
            listState.animateScrollToItem(messages.size - 1)
        }

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = SurfaceCard.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward, // RTL back
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Im Café bestellen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Serif
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess)
                            )
                            Text(
                                text = "مهمة 2 من 3 • طلب الحساب",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Translation toggle button
                    FilledTonalButton(
                        onClick = { showTranslation = !showTranslation },
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showTranslation) Primary.copy(alpha = 0.2f) else SurfaceCardSubtle
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = if (showTranslation) Primary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showTranslation) "ترجمة" else "ألماني",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showTranslation) Primary else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    if (message.sender == MessageSender.Katzu) {
                        KatzuMessageBubble(
                            message = message,
                            showTranslation = showTranslation,
                            onSpeak = onSpeak
                        )
                    } else {
                        UserMessageBubble(
                            message = message,
                            showTranslation = showTranslation,
                            onSpeak = onSpeak
                        )
                    }
                }

                // Finish scenario quick action card at bottom of conversation
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCardSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "هل انتهيت من طلبك وتحدثت مع النادل؟",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "احصل على تقييم نطقك وأخطائك القواعدية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = onFinishSession,
                                shape = RoundedCornerShape(9999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                modifier = Modifier.testTag("conversation_finish_btn")
                            ) {
                                Text(
                                    text = "إنهاء وتقييم",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BackgroundPure,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick suggested replies
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickReplies.forEach { reply ->
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { sendUserMessage(reply) }
                    ) {
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }

            // Voice Interaction Dock
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isListening) {
                        AudioWaveformBar(isListening = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.katzu_listening),
                                contentDescription = "كاتزو يستمع",
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "كاتزو يستمع لنطقك الآن... تحدث بالألمانية",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary
                            )
                        }
                    } else {
                        Text(
                            text = "اضغط على المايك للتحدث أو اختر عبارة من الأعلى",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keyboard input dialog or trigger
                        IconButton(
                            onClick = {
                                sendUserMessage("Ich möchte bitte zahlen.")
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "كتابة",
                                tint = TextSecondary
                            )
                        }

                        // Big Bioluminescent Mic Button (76dp)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    ambientColor = Primary.copy(alpha = 0.5f),
                                    spotColor = Primary.copy(alpha = 0.8f)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Brush.linearGradient(listOf(StatusError, Primary))
                                    else Brush.linearGradient(listOf(Primary, PrimaryContainer))
                                )
                                .clickable {
                                    isListening = !isListening
                                    if (isListening) {
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2500)
                                            isListening = false
                                            sendUserMessage("Ich möchte bitte einen Kaffee mit Hafermilch.")
                                        }
                                    }
                                }
                                .testTag("conversation_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = "تسجيل صوتي",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Listen sample / hint button
                        IconButton(
                            onClick = {
                                onSpeak("Einen Kaffee, bitte!", 1.0f)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "استمع لنموذج",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KatzuMessageBubble(
    message: ChatMessage,
    showTranslation: Boolean,
    onSpeak: (String, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceCardSubtle)
                .border(1.dp, Primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.katzu_barista),
                contentDescription = "Katzu",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.germanText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    )

                    AnimatedVisibility(visible = showTranslation) {
                        Text(
                            text = message.arabicTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSpeak(message.germanText, 1.0f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "نطق",
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onSpeak(message.germanText, 0.75f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = "نطق بطيء",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(
    message: ChatMessage,
    showTranslation: Boolean,
    onSpeak: (String, Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Primary.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.germanText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontFamily = FontFamily.Serif
                )
                if (showTranslation) {
                    Text(
                        text = message.arabicTranslation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Sarcastic Diagnostic Correction Card if present
        if (message.hasCorrection) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceCardSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.katzu_peace),
                                    contentDescription = "تصحيح كاتزو",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(
                                text = "تصحيح كاتزو الساخر",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusLearning,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = StatusError.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = message.grammarRule,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Comparison: Strike-through vs Correct
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = message.originalMistake,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = StatusError,
                            fontFamily = FontFamily.Serif
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "✓ ${message.correctedGerman}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            IconButton(
                                onClick = { onSpeak(message.correctedGerman, 1.0f) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = message.roastComment,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
