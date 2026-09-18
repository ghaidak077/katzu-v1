package com.example.katzu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.katzu.R
import com.example.katzu.model.VocabularyWord
import com.example.katzu.data.MistakeEntity
import com.example.katzu.ui.components.ArticleBadge
import com.example.katzu.ui.components.ContentEmptyStateView
import com.example.katzu.ui.components.FlashcardsDialog
import com.example.katzu.ui.components.GrammarCheatSheetDialog
import com.example.katzu.ui.components.MistakesBankDialog
import com.example.katzu.ui.theme.*

@Composable
fun PracticeScreen(
    vocabularyList: List<VocabularyWord>,
    grammarList: List<com.example.katzu.data.GrammarEntity> = emptyList(),
    mistakesList: List<MistakeEntity> = emptyList(),
    totalVocabularyCount: Int = 0,
    savedWordsCount: Int = 0,
    masteredWordsCount: Int = 0,
    isSyncing: Boolean = false,
    onRetrySync: () -> Unit = {},
    onDeleteMistake: (Long) -> Unit = {},
    onWordClick: (VocabularyWord) -> Unit,
    onSpeak: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    val categories = listOf("الكل", "الأسماء", "الأفعال", "عبارات المقهى")

    var showFlashcards by remember { mutableStateOf(false) }
    var showGrammarSheet by remember { mutableStateOf(false) }
    var showMistakesBank by remember { mutableStateOf(false) }

    val displayTotal = remember(totalVocabularyCount, vocabularyList) {
        if (totalVocabularyCount > 0) totalVocabularyCount else vocabularyList.size
    }
    val displayLearning = remember(savedWordsCount, vocabularyList) {
        if (savedWordsCount > 0) savedWordsCount else vocabularyList.count { it.isSaved }
    }
    val displayMastered = remember(masteredWordsCount, displayTotal, displayLearning) {
        if (masteredWordsCount > 0) masteredWordsCount else maxOf(0, displayTotal - displayLearning)
    }
    val flashcardCount = remember(vocabularyList) { vocabularyList.size.coerceAtMost(15) }

    val filteredWords = remember(searchQuery, selectedCategory, vocabularyList) {
        vocabularyList.filter { word ->
            val matchesCategory = selectedCategory == "الكل" || word.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    word.germanWord.contains(searchQuery, ignoreCase = true) ||
                    word.arabicMeaning.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    if (showFlashcards) {
        FlashcardsDialog(
            words = vocabularyList,
            onDismiss = { showFlashcards = false },
            onSpeak = onSpeak
        )
    }

    if (showGrammarSheet) {
        GrammarCheatSheetDialog(
            grammarList = grammarList,
            onDismiss = { showGrammarSheet = false }
        )
    }

    if (showMistakesBank) {
        MistakesBankDialog(
            mistakes = mistakesList,
            onDismiss = { showMistakesBank = false },
            onSpeak = onSpeak,
            onDeleteMistake = onDeleteMistake
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ميدان الصقل والتدريب",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "صقل المفردات والقواعد",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "المحادثة تصنع الطلاقة، والتدريب يمنحك الدقة. راجع الكلمات التي تعثرت بها لتثبيتها في ذاكرتك طويلة المدى.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.katzu_practice),
                    contentDescription = "Katzu Practice Coach",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(start = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Quick Practice Tools (Two cards in row)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Flashcards
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showFlashcards = true }
                        .testTag("practice_flashcards_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "تمرين البطاقات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "مراجعة سريعة • $flashcardCount كلمة",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Card 2: Grammar Sheet
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showGrammarSheet = true }
                        .testTag("practice_grammar_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "دليل القواعد",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "أدوات التعريف والداتيف",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Card 3: Mistakes Bank (بنك الأخطاء وتثبيت الصواب)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable { showMistakesBank = true }
                    .testTag("practice_mistakes_bank_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (mistakesList.isEmpty()) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (mistakesList.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Spellcheck,
                            contentDescription = null,
                            tint = if (mistakesList.isEmpty()) StatusSuccess else StatusError,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "بنك الأخطاء وتثبيت الصواب",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (mistakesList.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusError.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${mistakesList.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (mistakesList.isEmpty()) "سجلك اللغوي نظيف • أعد ممارسة المحادثات لتسجيل التعبيرات" else "${mistakesList.size} أخطاء تحتاج لتثبيت الصواب وكتابتها",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Stats Summary Bento Row
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCardSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$displayTotal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "إجمالي المفردات", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Box(modifier = Modifier.size(1.dp, 28.dp).background(BorderSubtle))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$displayLearning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusLearning)
                        Text(text = "قيد التعلّم", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Box(modifier = Modifier.size(1.dp, 28.dp).background(BorderSubtle))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$displayMastered", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        Text(text = "أتقنتها تماماً", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في مفرداتك وقواعدك...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practice_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // Filter Category Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Primary else BorderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Vocabulary Word Items
        if (vocabularyList.isEmpty()) {
            item {
                ContentEmptyStateView(
                    title = "قائمة المفردات فارغة",
                    message = "لا يوجد اتصال بالإنترنت — يرجى الاتصال لتحميل بنك المفردات وقواعد اللغة.",
                    buttonText = "إعادة تحميل المفردات",
                    isRetrying = isSyncing,
                    onRetry = onRetrySync,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else if (filteredWords.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.katzu_word_insight),
                            contentDescription = "كاتزو يبحث",
                            modifier = Modifier.size(76.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "لا توجد نتائج تطابق بحثك",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "“فتشت قاموسي ولم أجد هذه الكلمة بعد... تأكد من صحة الحروف أو جرّب تصنيفاً آخر.”",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredWords,
                key = { it.id }
            ) { word ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onWordClick(word) }
                        .testTag("vocab_item_${word.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (word.article.isNotBlank()) {
                                ArticleBadge(word.article)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = word.germanWord,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = word.arabicMeaning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val text = if (word.article.isNotBlank()) "${word.article} ${word.germanWord}" else word.germanWord
                                    onSpeak(text)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "نطق",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
