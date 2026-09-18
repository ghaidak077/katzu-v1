package com.example.katzu.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.example.katzu.R
import com.example.katzu.model.UserProfile
import com.example.katzu.ui.theme.*
import com.example.katzu.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareProgressCardDialog(
    userProfile: UserProfile,
    streakDays: Int,
    totalWords: Int,
    fluencyRate: Int,
    completedScenarios: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var isSharing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // The Shareable Card Preview (Captured to Bitmap via GraphicsLayer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .testTag("shareable_progress_card_preview")
                ) {
                    ShareableProgressCardContent(
                        userProfile = userProfile,
                        streakDays = streakDays,
                        totalWords = totalWords,
                        fluencyRate = fluencyRate,
                        completedScenarios = completedScenarios
                    )
                }

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dismiss_share_button"),
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        enabled = !isSharing
                    ) {
                        Text(
                            text = "إغلاق",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (isSharing) return@Button
                            isSharing = true
                            coroutineScope.launch {
                                try {
                                    val bitmap: Bitmap = try {
                                        graphicsLayer.toImageBitmap().asAndroidBitmap()
                                    } catch (e: Throwable) {
                                        AppLogger.w("ShareProgress", "GraphicsLayer capture failed, using canvas generator: ${e.message}")
                                        generateProgressCardBitmap(
                                            context = context,
                                            userProfile = userProfile,
                                            streakDays = streakDays,
                                            totalWords = totalWords,
                                            fluencyRate = fluencyRate
                                        )
                                    }
                                    shareProgressCardBitmap(
                                        context = context,
                                        bitmap = bitmap,
                                        userProfile = userProfile,
                                        streakDays = streakDays,
                                        totalWords = totalWords
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    AppLogger.e("ShareProgress", "Failed to share progress card", e)
                                    Toast.makeText(context, "تعذر مشاركة البطاقة: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSharing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("share_native_sheet_button"),
                        shape = RoundedCornerShape(9999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = BackgroundPure
                        ),
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BackgroundPure,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "مشاركة عبر التطبيقات 📲",
                                    style = MaterialTheme.typography.labelLarge,
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

@Composable
fun ShareableProgressCardContent(
    userProfile: UserProfile,
    streakDays: Int,
    totalWords: Int,
    fluencyRate: Int,
    completedScenarios: Int = 0
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding & Watermark Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "KATZU • كورس الألمانية 🐾",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "katzu.app",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
            }

            // Mascot Artwork & Greeting Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardSubtle)
                        .border(2.dp, Primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.katzu_celebrating),
                        contentDescription = "Katzu Celebrating",
                        modifier = Modifier.fillMaxSize(0.9f),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = if (userProfile.name.isNotBlank()) "إنجاز ${userProfile.name}" else "إنجازي في المحادثة الألمانية 🇩🇪",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "أتحدث الألمانية بثقة وعفوية مع كاتزو",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // 4 Core Achievement Metric Pills
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProgressStatBox(
                        title = "أيام الحماسة",
                        value = "$streakDays أيام 🔥",
                        accentColor = StatusLearning,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressStatBox(
                        title = "المستوى الحالي",
                        value = userProfile.currentLevel,
                        accentColor = Primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProgressStatBox(
                        title = "الكلمات المتقنة",
                        value = "$totalWords كلمة 📚",
                        accentColor = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressStatBox(
                        title = "معدل الطلاقة",
                        value = "$fluencyRate% ⚡",
                        accentColor = if (fluencyRate >= 80) StatusSuccess else StatusLearning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mascot Witty Endorsement
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCardSubtle,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "«الألمان لا يعضون.. وقواعدهم يمكن ترويضها بالممارسة المستمرة! استمر في الحديث.»\n— كاتزو 🐾",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            // Mandatory Creator Attribution
            Text(
                text = "صُنع بواسطة غيدق علوش — ghaidak.com",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProgressStatBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardSubtle,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

suspend fun shareProgressCardBitmap(
    context: Context,
    bitmap: Bitmap,
    userProfile: UserProfile,
    streakDays: Int,
    totalWords: Int
) = withContext(Dispatchers.IO) {
    val sharesDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val cardFile = File(sharesDir, "katzu_progress_${System.currentTimeMillis()}.png")
    FileOutputStream(cardFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, cardFile)

    val shareText = buildString {
        appendLine("أحرزت تقدماً في تعلم التحدث بالألمانية مع كاتزو! 🐾🇩🇪")
        appendLine("• المستوى: ${userProfile.currentLevel}")
        appendLine("• الحماسة: $streakDays أيام متتالية 🔥")
        appendLine("• الكلمات المتقنة: $totalWords كلمة 📚")
        appendLine("خض تجربة المحادثة المباشرة مع كاتزو: https://ghaidak.com")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "إنجازي في تعلم الألمانية مع كاتزو 🐾")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(shareIntent, "مشاركة بطاقة إنجاز كاتزو 📲").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/**
 * Robust Native Android Canvas Bitmap Generator for high-resolution off-screen export
 */
fun generateProgressCardBitmap(
    context: Context,
    userProfile: UserProfile,
    streakDays: Int,
    totalWords: Int,
    fluencyRate: Int
): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // AMOLED pure black background
    canvas.drawColor(android.graphics.Color.parseColor("#000000"))

    // Main Card
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1A1826")
    }
    val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = android.graphics.Color.parseColor("#8B6FE8")
    }
    val cardRect = RectF(60f, 60f, width - 60f, height - 60f)
    canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)
    canvas.drawRoundRect(cardRect, 48f, 48f, cardBorder)

    // Mascot drawable
    val mascotDrawable = ContextCompat.getDrawable(context, R.drawable.katzu_celebrating)
    val mascotBitmap = mascotDrawable?.toBitmap(260, 260)
    if (mascotBitmap != null) {
        val left = (width - 260) / 2f
        canvas.drawBitmap(mascotBitmap, left, 140f, null)
    }

    // Header Text
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 54f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("إنجازي في المحادثة الألمانية 🇩🇪", width / 2f, 470f, titlePaint)

    val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#A8A2B8")
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    val nameTag = if (userProfile.name.isNotBlank()) "المتعلم: ${userProfile.name} • " else ""
    canvas.drawText("${nameTag}أتحدث الألمانية بثقة وعفوية مع كاتزو", width / 2f, 530f, subTitlePaint)

    // Stat boxes
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#221F33")
    }
    val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = android.graphics.Color.parseColor("#383254")
    }
    val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#8B6FE8")
        textSize = 46f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#A8A2B8")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // Row 1: Streak & Level
    val r1Top = 600f
    val r1Bottom = 730f
    val b1 = RectF(100f, r1Top, 520f, r1Bottom)
    val b2 = RectF(560f, r1Top, 980f, r1Bottom)
    canvas.drawRoundRect(b1, 24f, 24f, boxPaint)
    canvas.drawRoundRect(b1, 24f, 24f, boxBorder)
    canvas.drawRoundRect(b2, 24f, 24f, boxPaint)
    canvas.drawRoundRect(b2, 24f, 24f, boxBorder)

    canvas.drawText("الحماسة المتتالية", 310f, 650f, lblPaint)
    valPaint.color = android.graphics.Color.parseColor("#F0C674")
    canvas.drawText("$streakDays أيام 🔥", 310f, 705f, valPaint)

    canvas.drawText("المستوى الحالي", 770f, 650f, lblPaint)
    valPaint.color = android.graphics.Color.parseColor("#8B6FE8")
    canvas.drawText(userProfile.currentLevel, 770f, 705f, valPaint)

    // Row 2: Words & Fluency
    val r2Top = 760f
    val r2Bottom = 890f
    val b3 = RectF(100f, r2Top, 520f, r2Bottom)
    val b4 = RectF(560f, r2Top, 980f, r2Bottom)
    canvas.drawRoundRect(b3, 24f, 24f, boxPaint)
    canvas.drawRoundRect(b3, 24f, 24f, boxBorder)
    canvas.drawRoundRect(b4, 24f, 24f, boxPaint)
    canvas.drawRoundRect(b4, 24f, 24f, boxBorder)

    canvas.drawText("الكلمات المتقنة", 310f, 810f, lblPaint)
    valPaint.color = android.graphics.Color.parseColor("#7FD9A8")
    canvas.drawText("$totalWords كلمة 📚", 310f, 865f, valPaint)

    canvas.drawText("معدل الطلاقة", 770f, 810f, lblPaint)
    valPaint.color = if (fluencyRate >= 80) android.graphics.Color.parseColor("#7FD9A8") else android.graphics.Color.parseColor("#F0C674")
    canvas.drawText("$fluencyRate% ⚡", 770f, 865f, valPaint)

    // Quote Box
    val qBox = RectF(100f, 930f, 980f, 1140f)
    canvas.drawRoundRect(qBox, 24f, 24f, boxPaint)
    canvas.drawRoundRect(qBox, 24f, 24f, boxBorder)

    val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E0DDF0")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("«الألمان لا يعضون.. وقواعدهم يمكن ترويضها بالممارسة المستمرة!", width / 2f, 1000f, quotePaint)
    canvas.drawText("استمر في خوض المحادثات الواقعية.»", width / 2f, 1045f, quotePaint)
    quotePaint.color = android.graphics.Color.parseColor("#8B6FE8")
    quotePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("— كاتزو 🐾", width / 2f, 1100f, quotePaint)

    // Attribution footer
    val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#807A94")
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("صُنع بواسطة غيدق علوش — ghaidak.com", width / 2f, 1220f, footPaint)

    return bitmap
}
