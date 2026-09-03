package com.example.katzu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.katzu.data.KatzuRepository
import com.example.katzu.model.*
import com.example.katzu.ui.components.KatzuBottomNavigationBar
import com.example.katzu.ui.components.TopHeaderBar
import com.example.katzu.ui.components.WordInsightBottomSheet
import com.example.katzu.ui.screens.*
import com.example.katzu.ui.theme.BackgroundPure
import com.example.katzu.ui.theme.KatzuTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: KatzuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = KatzuRepository(this)

        setContent {
            KatzuTheme {
                // Arabic-first RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    KatzuApp(repository)
                }
            }
        }
    }
}

@Composable
fun KatzuApp(repository: KatzuRepository) {
    val userProfile by repository.userProfile.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppScreen.Welcome) }
    var currentTab by remember { mutableStateOf(NavigationTab.Trail) }
    var selectedWordForInsight by remember { mutableStateOf<VocabularyWord?>(null) }

    val activeScenario = repository.cafeScenario
    val trailNodes = repository.trailNodes
    val vocabularyList = repository.vocabularyList
    val initialConversation = remember { repository.getInitialConversation() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.Welcome -> {
                    WelcomeScreen(
                        onStartJourney = { enteredName ->
                            if (enteredName.isNotBlank()) {
                                repository.updateUserName(enteredName)
                            }
                            currentScreen = AppScreen.MainTabs
                        }
                    )
                }

                AppScreen.MainTabs -> {
                    Scaffold(
                        topBar = {
                            TopHeaderBar(
                                streakDays = userProfile.streakDays,
                                xp = userProfile.xp,
                                onProfileClick = {
                                    currentTab = NavigationTab.Profile
                                }
                            )
                        },
                        bottomBar = {
                            KatzuBottomNavigationBar(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
                            )
                        },
                        containerColor = BackgroundPure
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                NavigationTab.Trail -> {
                                    TrailScreen(
                                        userProfile = userProfile,
                                        trailNodes = trailNodes,
                                        onSelectScenario = {
                                            currentScreen = AppScreen.ScenarioDetail
                                        }
                                    )
                                }

                                NavigationTab.Practice -> {
                                    PracticeScreen(
                                        vocabularyList = vocabularyList,
                                        onWordClick = { word ->
                                            selectedWordForInsight = word
                                        },
                                        onSpeak = { text ->
                                            repository.speak(text, userProfile.speechSpeed)
                                        }
                                    )
                                }

                                NavigationTab.Progress -> {
                                    ProgressScreen(
                                        userProfile = userProfile
                                    )
                                }

                                NavigationTab.Profile -> {
                                    ProfileSettingsScreen(
                                        userProfile = userProfile,
                                        onUpdateSpeechSpeed = { speed ->
                                            userProfile.speechSpeed = speed
                                        },
                                        onUpdateSarcasm = { sarcasm ->
                                            userProfile.sarcasmLevel = sarcasm
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                AppScreen.ScenarioDetail -> {
                    ScenarioDetailScreen(
                        scenario = activeScenario,
                        vocabularyList = vocabularyList,
                        onBack = { currentScreen = AppScreen.MainTabs },
                        onStartConversation = { currentScreen = AppScreen.LiveConversation },
                        onWordClick = { word ->
                            selectedWordForInsight = word
                        }
                    )
                }

                AppScreen.LiveConversation -> {
                    LiveConversationScreen(
                        initialMessages = initialConversation,
                        onBack = { currentScreen = AppScreen.ScenarioDetail },
                        onFinishSession = { currentScreen = AppScreen.SessionReport },
                        onSpeak = { text, speed ->
                            repository.speak(text, speed)
                        }
                    )
                }

                AppScreen.SessionReport -> {
                    SessionReportScreen(
                        onReturnToTrail = {
                            currentTab = NavigationTab.Trail
                            currentScreen = AppScreen.MainTabs
                        },
                        onReviewPractice = {
                            currentTab = NavigationTab.Practice
                            currentScreen = AppScreen.MainTabs
                        },
                        onSpeak = { text ->
                            repository.speak(text, userProfile.speechSpeed)
                        }
                    )
                }
            }
        }

        // Global Word Insight Bottom Sheet
        if (selectedWordForInsight != null) {
            WordInsightBottomSheet(
                word = selectedWordForInsight,
                onDismiss = { selectedWordForInsight = null },
                onSpeak = { text, speed ->
                    repository.speak(text, speed)
                },
                onToggleBookmark = { wordId ->
                    repository.toggleWordSaved(wordId)
                }
            )
        }
    }
}
