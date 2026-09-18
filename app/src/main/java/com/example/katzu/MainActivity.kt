package com.example.katzu

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.katzu.billing.BillingState
import com.example.katzu.billing.PlayBillingManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.example.katzu.data.ContentSyncStatus
import com.example.katzu.data.GeminiOnlineConversationService
import com.example.katzu.data.KatzuRepository
import com.example.katzu.data.ScenarioEntity
import com.example.katzu.data.ScenarioTranslations
import com.example.katzu.data.SecurityAndSubscriptionManager
import com.example.katzu.data.ServerStatusResult
import com.example.katzu.data.ServerVerifyResult
import com.example.katzu.data.mapCategoryToTopic
import com.example.katzu.data.toUiModel
import com.example.katzu.model.*
import com.example.katzu.ui.components.GOAL_PRESETS
import com.example.katzu.ui.components.GoalSelectionBottomSheet
import com.example.katzu.ui.components.KatzuBottomNavigationBar
import com.example.katzu.ui.components.TopHeaderBar
import com.example.katzu.ui.components.WordInsightBottomSheet
import com.example.katzu.ui.screens.*
import com.example.katzu.ui.theme.BackgroundPure
import com.example.katzu.ui.theme.KatzuTheme
import com.example.katzu.util.AnalyticsTracker
import com.example.katzu.util.AppLogger

class MainActivity : ComponentActivity() {

    private lateinit var repository: KatzuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)
        AnalyticsTracker.init(applicationContext)
        AppLogger.i("MainActivity", "onCreate called")
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

    val initialScreen = remember {
        val loggedIn = repository.isUserLoggedInSync()
        val subActive = repository.isSubscriptionActiveSync()
        val freeRemaining = repository.getFreeSessionsRemaining()
        val onboardingCompleted = repository.hasCompletedOnboarding()
        AppLogger.i("MainActivity", "Determined initial screen synchronously: loggedIn=$loggedIn, subActive=$subActive, freeRemaining=$freeRemaining, onboardingCompleted=$onboardingCompleted")
        when {
            loggedIn -> {
                if (subActive || freeRemaining > 0) AppScreen.MainTabs
                else AppScreen.SubscriptionRedemption
            }
            !onboardingCompleted -> AppScreen.Welcome
            subActive || freeRemaining > 0 -> AppScreen.MainTabs
            else -> AppScreen.Welcome
        }
    }

    var currentScreen by remember { mutableStateOf(initialScreen) }
    var showOnboardingGoalPicker by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(NavigationTab.Trail) }
    var selectedWordForInsight by remember { mutableStateOf<VocabularyWord?>(null) }
    var selectedScenarioId by remember { mutableStateOf("") }

    // Live Room / Cloudflare D1 content streams & sync status
    val syncStatus by repository.contentRepository.syncStatus.collectAsStateWithLifecycle()
    val isSyncing = syncStatus is ContentSyncStatus.Syncing

    val dbScenarios by repository.contentRepository.getScenariosFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val dbVocabEntities by repository.contentRepository.getVocabularyFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val dbGrammarEntities by repository.contentRepository.getGrammarFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val savedWordIds by repository.contentRepository.getSavedWordIdsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val ttsPlaybackState by repository.ttsPlaybackState.collectAsStateWithLifecycle()

    val activeScenarioEntity = remember(dbScenarios, selectedScenarioId) {
        dbScenarios.find { it.id == selectedScenarioId } ?: dbScenarios.firstOrNull()
    }
    val targetScenarioId = activeScenarioEntity?.id ?: selectedScenarioId
    var activeTrailLevel by remember(userProfile.currentLevel) {
        mutableStateOf(userProfile.currentLevel.take(2).ifBlank { "A1" })
    }
    val scenarioLevel = activeTrailLevel

    val currentScenarioStarterPhrases by remember(targetScenarioId, scenarioLevel) {
        repository.contentRepository.getStarterPhrasesByLevelFlow(targetScenarioId, scenarioLevel)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val currentScenarioTraining by remember(selectedScenarioId) {
        repository.getScenarioTrainingFlow(selectedScenarioId)
    }.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(dbScenarios) {
        if (selectedScenarioId.isBlank() || dbScenarios.none { it.id == selectedScenarioId }) {
            dbScenarios.firstOrNull()?.let { selectedScenarioId = it.id }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    // Convert live entities to UI models and sync bookmark state from Room
    val liveVocabList = remember(dbVocabEntities, savedWordIds) {
        dbVocabEntities.map { entity ->
            val uiModel = entity.toUiModel()
            uiModel.copy(isSaved = savedWordIds.contains(uiModel.id))
        }
    }

    val activeScenario = remember(activeScenarioEntity, currentScenarioStarterPhrases, scenarioLevel) {
        activeScenarioEntity?.toUiModel(currentScenarioStarterPhrases, scenarioLevel)
    }

    val allSessions by repository.getAllSessionsFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val allScenarioTraining by repository.getAllScenarioTrainingFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val allMistakes by repository.getAllMistakesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val savedWordsCount by repository.getSavedWordsCountFlow().collectAsStateWithLifecycle(initialValue = 0)
    val totalVocabularyCount by repository.getVocabularyCountFlow().collectAsStateWithLifecycle(initialValue = 0)

    var activeIntroMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var lastSentencesSpoken by remember { mutableIntStateOf(0) }
    var lastWordsLearned by remember { mutableIntStateOf(0) }
    var lastAccuracyPercent by remember { mutableIntStateOf(0) }
    var lastDurationSeconds by remember { mutableIntStateOf(0) }
    var lastSessionMistakes by remember { mutableStateOf<List<com.example.katzu.model.SessionMistakeSummary>>(emptyList()) }
    var lastPraisedSentence by remember { mutableStateOf<String?>(null) }
    var lastHintsUsedCount by remember { mutableIntStateOf(0) }
    var lastIndependentSentences by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val currentUserEntity by repository.userEntityFlow.collectAsStateWithLifecycle(initialValue = null)
    var currentIdToken by remember { mutableStateOf<String?>(null) }
    var isRedeemingCode by remember { mutableStateOf(false) }
    var redemptionErrorMessage by remember { mutableStateOf<String?>(null) }
    var showGoalSheetFromProgress by remember { mutableStateOf(false) }

    val billingManager = remember {
        PlayBillingManager(
            context = context,
            coroutineScope = coroutineScope,
            onPurchaseSuccess = { purchase, productId ->
                coroutineScope.launch {
                    repository.activatePlayBillingSubscription(
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                        orderId = purchase.orderId
                    )
                    AnalyticsTracker.trackTrialStarted("google_play_$productId")
                    currentScreen = AppScreen.MainTabs
                }
            }
        )
    }

    DisposableEffect(billingManager) {
        billingManager.startConnection()
        onDispose {
            billingManager.destroy()
        }
    }

    val billingPlans by billingManager.subscriptionPlans.collectAsStateWithLifecycle()
    val billingState by billingManager.billingState.collectAsStateWithLifecycle()
    val isPurchasing = billingState is BillingState.Purchasing

    suspend fun getOrRefreshIdToken(): String? {
        AppLogger.i("MainActivity", "getOrRefreshIdToken called. currentIdToken present: ${!currentIdToken.isNullOrBlank()}")
        if (!currentIdToken.isNullOrBlank()) return currentIdToken
        return try {
            val webClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                "754341831948-s76bt3vmcmnb7qvmcefbf1i8a16f11ss.apps.googleusercontent.com"
            }
            AppLogger.i("MainActivity", "Building silent GetGoogleIdOption request with webClientId: $webClientId")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            AppLogger.i("MainActivity", "Requesting credentialManager.getCredential silently with 10s timeout")
            val response = withTimeoutOrNull(10_000L) {
                credentialManager.getCredential(context = context, request = request)
            }
            val credential = response?.credential
            if (credential == null) {
                AppLogger.w("MainActivity", "Silent credential fetch timed out or returned null")
                return null
            }
            AppLogger.i("MainActivity", "Silent credential received. Class: ${credential::class.java.name}, Type: ${credential.type}")
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val token = googleIdTokenCredential.idToken
                AppLogger.i("MainActivity", "Silent token extracted successfully. Length: ${token.length}")
                currentIdToken = token
                token
            } else {
                AppLogger.w("MainActivity", "Silent credential did not match GoogleIdTokenCredential. Type: ${credential.type}")
                null
            }
        } catch (e: Exception) {
            AppLogger.w("MainActivity", "Silent token fetch exception: ${e::class.java.name}: ${e.message}", e)
            null
        }
    }

    fun launchSilentSubscriptionCheck() {
        val token = currentIdToken
        if (token.isNullOrBlank()) {
            AppLogger.i("MainActivity", "launchSilentSubscriptionCheck: in-memory token is absent. Verifying local subscription timestamp without CredentialManager prompt.")
            coroutineScope.launch {
                val user = repository.getCurrentUser()
                val isStillLocallyValid = user?.subscriptionExpiresAt?.let { SecurityAndSubscriptionManager.isLocallyActive(it) } ?: false
                if (!isStillLocallyValid && user?.isLoggedIn == true && user.isSubscriptionActive) {
                    AppLogger.w("MainActivity", "Local subscription has passed expiration date. Redirecting to SubscriptionRedemptionScreen")
                    currentScreen = AppScreen.SubscriptionRedemption
                }
            }
            return
        }

        coroutineScope.launch {
            try {
                AppLogger.i("MainActivity", "launchSilentSubscriptionCheck starting with in-memory token (len: ${token.length})")
                val status = repository.checkSubscriptionStatus(token)
                AppLogger.i("MainActivity", "repository.checkSubscriptionStatus returned: $status")
                if (status is ServerStatusResult.Expired) {
                    AppLogger.w("MainActivity", "Subscription expired/inactive on server. Redirecting to SubscriptionRedemptionScreen")
                    repository.markSubscriptionInactive()
                    currentScreen = AppScreen.SubscriptionRedemption
                } else if (status is ServerStatusResult.Active) {
                    repository.fetchAndRestoreCloudProgress(token)
                }
            } catch (e: Exception) {
                AppLogger.e("MainActivity", "launchSilentSubscriptionCheck exception: ${e::class.java.name}: ${e.message}", e)
            }
        }
    }

    var hasCheckedInitialAuth by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserEntity) {
        val user = currentUserEntity
        if (!hasCheckedInitialAuth && user != null) {
            hasCheckedInitialAuth = true
            val isUserLoggedIn = user.isLoggedIn && (!user.googleAccountEmail.isNullOrBlank() || !user.email.isNullOrBlank())
            val isSubActive = user.isSubscriptionActive && SecurityAndSubscriptionManager.isLocallyActive(user.subscriptionExpiresAt)
            val freeRemaining = repository.getFreeSessionsRemaining()
            val onboardingCompleted = repository.hasCompletedOnboarding()
            AppLogger.i("MainActivity", "Initial auth check from Room: loggedIn=$isUserLoggedIn, subActive=$isSubActive, freeRemaining=$freeRemaining, onboardingCompleted=$onboardingCompleted, email=${user.googleAccountEmail}")

            if (isUserLoggedIn) {
                repository.setCompletedOnboarding(true)
                if (isSubActive || freeRemaining > 0) {
                    if (currentScreen != AppScreen.MainTabs) {
                        currentScreen = AppScreen.MainTabs
                    }
                    if (isSubActive) {
                        launchSilentSubscriptionCheck()
                    }
                } else {
                    if (currentScreen != AppScreen.SubscriptionRedemption) {
                        currentScreen = AppScreen.SubscriptionRedemption
                    }
                }
            } else if (!onboardingCompleted) {
                if (currentScreen != AppScreen.Welcome) {
                    currentScreen = AppScreen.Welcome
                }
            } else {
                if (freeRemaining > 0) {
                    if (currentScreen != AppScreen.MainTabs) {
                        currentScreen = AppScreen.MainTabs
                    }
                } else {
                    if (currentScreen != AppScreen.Welcome && currentScreen != AppScreen.SignIn) {
                        currentScreen = AppScreen.Welcome
                    }
                }
            }
        }
    }

    // Global Back Navigation Handling
    BackHandler(enabled = selectedWordForInsight != null) {
        selectedWordForInsight = null
    }

    BackHandler(enabled = selectedWordForInsight == null && currentScreen == AppScreen.MainTabs && currentTab != NavigationTab.Trail) {
        currentTab = NavigationTab.Trail
    }

    BackHandler(
        enabled = selectedWordForInsight == null &&
                currentScreen != AppScreen.MainTabs &&
                currentScreen != AppScreen.Welcome &&
                currentScreen != AppScreen.LiveConversation &&
                currentScreen != AppScreen.Quiz
    ) {
        when (currentScreen) {
            AppScreen.SignIn -> {
                currentScreen = AppScreen.Welcome
            }
            AppScreen.SubscriptionRedemption -> {
                // If the user is logged into Google, don't boot them out to SignIn on back;
                // return to Welcome or keep them in the redemption context
                val isUserLoggedIn = currentUserEntity?.isLoggedIn == true && !currentUserEntity?.googleAccountEmail.isNullOrBlank()
                currentScreen = if (isUserLoggedIn) AppScreen.Welcome else AppScreen.SignIn
            }
            AppScreen.ScenarioDetail -> {
                currentTab = NavigationTab.Trail
                currentScreen = AppScreen.MainTabs
            }
            AppScreen.Study -> {
                currentScreen = AppScreen.ScenarioDetail
            }
            AppScreen.SessionReport -> {
                currentTab = NavigationTab.Trail
                currentScreen = AppScreen.MainTabs
            }
            else -> { /* No-op */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val isSequentialFlowForward = (initialState == AppScreen.Study && targetState == AppScreen.Quiz) ||
                    (initialState == AppScreen.Quiz && targetState == AppScreen.LiveConversation) ||
                    (initialState == AppScreen.LiveConversation && targetState == AppScreen.SessionReport)
                val isSequentialFlowBackward = (initialState == AppScreen.Quiz && targetState == AppScreen.Study) ||
                    (initialState == AppScreen.LiveConversation && targetState == AppScreen.Quiz)

                val isModalScreen = targetState in listOf(
                    AppScreen.LiveConversation,
                    AppScreen.Study,
                    AppScreen.Quiz,
                    AppScreen.ScenarioDetail,
                    AppScreen.SubscriptionRedemption,
                    AppScreen.SessionReport
                )
                val isReturningFromModal = initialState in listOf(
                    AppScreen.LiveConversation,
                    AppScreen.Study,
                    AppScreen.Quiz,
                    AppScreen.ScenarioDetail,
                    AppScreen.SubscriptionRedemption,
                    AppScreen.SessionReport
                )

                if (isSequentialFlowForward) {
                    (slideInHorizontally(
                        initialOffsetX = { it / 4 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = tween(180)
                    ) + fadeOut(animationSpec = tween(180)))
                } else if (isSequentialFlowBackward) {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                    (slideOutHorizontally(
                        targetOffsetX = { it / 4 },
                        animationSpec = tween(180)
                    ) + fadeOut(animationSpec = tween(180)))
                } else if (isModalScreen) {
                    (slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(240)) + scaleIn(initialScale = 0.94f)) togetherWith
                    (slideOutVertically(targetOffsetY = { -it / 16 }, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.02f))
                } else if (isReturningFromModal) {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 1.03f)) togetherWith
                    (slideOutVertically(
                        targetOffsetY = { it / 5 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.94f))
                } else {
                    (scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                    (scaleOut(targetScale = 1.03f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)))
                }
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.Welcome -> {
                    LaunchedEffect(Unit) {
                        AnalyticsTracker.trackOnboardingStarted()
                    }
                    WelcomeScreen(
                        onStartJourney = { enteredName ->
                            if (enteredName.isNotBlank()) {
                                repository.updateUserName(enteredName)
                            }
                            showOnboardingGoalPicker = true
                        },
                        onLoginClick = {
                            currentScreen = AppScreen.SignIn
                        }
                    )
                }

                AppScreen.SignIn -> {
                    SignInScreen(
                        onSignInSuccess = { idToken, email, displayName ->
                            AppLogger.i("MainActivity", "onSignInSuccess received. email: $email, displayName: $displayName, tokenLength: ${idToken.length}")
                            currentIdToken = idToken
                            repository.setActiveIdToken(idToken)
                            repository.setCompletedOnboarding(true)
                            coroutineScope.launch {
                                try {
                                    AppLogger.i("MainActivity", "Saving Google user to Room: $email ($displayName)")
                                    repository.saveGoogleUser(email, displayName)

                                    // Verify subscription directly with server as single source of truth with 10s timeout
                                    AppLogger.i("MainActivity", "Checking subscription status from server for Google user...")
                                    val serverStatus = withTimeoutOrNull(10_000L) {
                                        repository.checkSubscriptionStatus(idToken)
                                    } ?: ServerStatusResult.Error
                                    AppLogger.i("MainActivity", "Server status result: $serverStatus")

                                    val isSubActive = serverStatus is ServerStatusResult.Active

                                    AppLogger.i("MainActivity", "isSubActive check: $isSubActive (serverStatus=$serverStatus)")
                                    if (isSubActive) {
                                        AppLogger.i("MainActivity", "User has active subscription linked on server. Navigating directly to MainTabs!")
                                        currentScreen = AppScreen.MainTabs
                                        coroutineScope.launch(Dispatchers.IO) {
                                            repository.fetchAndRestoreCloudProgress(idToken)
                                        }
                                    } else if (repository.getFreeSessionsRemaining() > 0) {
                                        AppLogger.i("MainActivity", "User signed in, subscription unredeemed, but has free trial sessions. Navigating to MainTabs")
                                        currentScreen = AppScreen.MainTabs
                                    } else {
                                        AppLogger.i("MainActivity", "User subscription inactive/unredeemed on server and 0 free sessions. Enforcing SubscriptionRedemption")
                                        repository.markSubscriptionInactive()
                                        currentScreen = AppScreen.SubscriptionRedemption
                                    }
                                } catch (e: Exception) {
                                    AppLogger.e("MainActivity", "Error in onSignInSuccess coroutine: ${e::class.java.name}: ${e.message}", e)
                                    if (repository.getFreeSessionsRemaining() > 0) {
                                        currentScreen = AppScreen.MainTabs
                                    } else {
                                        repository.markSubscriptionInactive()
                                        currentScreen = AppScreen.SubscriptionRedemption
                                    }
                                }
                            }
                        },
                        onBack = {
                            currentScreen = AppScreen.Welcome
                        }
                    )
                }

                AppScreen.SubscriptionRedemption -> {
                    LaunchedEffect(Unit) {
                        AnalyticsTracker.trackPaywallViewed("subscription_redemption")
                    }
                    SubscriptionRedemptionScreen(
                        userEmail = currentUserEntity?.googleAccountEmail ?: currentUserEntity?.email,
                        isRedeeming = isRedeemingCode,
                        errorMessage = redemptionErrorMessage ?: (if (billingState is BillingState.Error) (billingState as BillingState.Error).message else null),
                        availablePlans = billingPlans,
                        isPurchasing = isPurchasing,
                        onSubscribeGooglePlay = { productId ->
                            AppLogger.i("MainActivity", "onSubscribeGooglePlay clicked: $productId")
                            val act = context as? Activity
                            if (act != null) {
                                billingManager.launchPurchaseFlow(act, productId)
                            } else {
                                AppLogger.w("MainActivity", "Context is not Activity, cannot launch billing flow")
                            }
                        },
                        onRestorePurchases = {
                            AppLogger.i("MainActivity", "onRestorePurchases clicked")
                            billingManager.queryExistingPurchases()
                        },
                        onRedeemCode = { code ->
                            AppLogger.i("MainActivity", "onRedeemCode invoked with code: $code")
                            coroutineScope.launch {
                                isRedeemingCode = true
                                redemptionErrorMessage = null
                                try {
                                    val token = getOrRefreshIdToken()
                                    if (token.isNullOrBlank()) {
                                        AppLogger.w("MainActivity", "Cannot redeem: token is null or blank")
                                        isRedeemingCode = false
                                        redemptionErrorMessage = "انتهت صلاحية جلسة Google، يرجى تسجيل الدخول مجدداً."
                                        currentScreen = AppScreen.SignIn
                                        return@launch
                                    }

                                    AppLogger.i("MainActivity", "Calling repository.verifyAndRedeemSubscription(code, tokenLen: ${token.length})")
                                    val verifyResult = repository.verifyAndRedeemSubscription(code, token)
                                    AppLogger.i("MainActivity", "verifyAndRedeemSubscription result: $verifyResult")
                                    isRedeemingCode = false
                                    when (verifyResult) {
                                        is ServerVerifyResult.Success -> {
                                            AppLogger.i("MainActivity", "Redemption successful! Navigating to MainTabs")
                                            AnalyticsTracker.trackTrialStarted("code_redemption")
                                            currentScreen = AppScreen.MainTabs
                                        }
                                        is ServerVerifyResult.AlreadyRedeemed -> {
                                            AppLogger.w("MainActivity", "Code already redeemed. Checking if it belongs to this account...")
                                            val status = repository.checkSubscriptionStatus(token)
                                            if (status is ServerStatusResult.Active) {
                                                AppLogger.i("MainActivity", "Account is active on server! Navigating to MainTabs")
                                                currentScreen = AppScreen.MainTabs
                                            } else {
                                                redemptionErrorMessage = "هذا الرمز استُخدم مسبقاً بحساب آخر. كاتزو لا يقبل البقايا!"
                                            }
                                        }
                                        is ServerVerifyResult.InvalidCode -> {
                                            AppLogger.w("MainActivity", "Invalid code. Reason: ${verifyResult.reason}")
                                            redemptionErrorMessage = "رمز غير صالح. تحقق من كتابته بدقة."
                                        }
                                        is ServerVerifyResult.NetworkError -> {
                                            AppLogger.e("MainActivity", "Network error during code redemption")
                                            redemptionErrorMessage = "تعذر الاتصال بالخادم. تأكد من اتصال الإنترنت وحاول مجدداً."
                                        }
                                    }
                                } catch (e: Exception) {
                                    isRedeemingCode = false
                                    AppLogger.e("MainActivity", "Exception during code redemption: ${e::class.java.name}: ${e.message}", e)
                                    redemptionErrorMessage = "حدث خطأ غير متوقع: ${e.message}"
                                }
                            }
                        },
                        onChangeAccount = {
                            AppLogger.i("MainActivity", "onChangeAccount invoked. Signing out and clearing token")
                            coroutineScope.launch {
                                try {
                                    repository.signOut()
                                    currentIdToken = null
                                    currentScreen = AppScreen.SignIn
                                } catch (e: Exception) {
                                    AppLogger.e("MainActivity", "Exception during signOut: ${e.message}", e)
                                }
                            }
                        },
                        onCheckExistingSubscription = {
                            AppLogger.i("MainActivity", "onCheckExistingSubscription invoked")
                            coroutineScope.launch {
                                isRedeemingCode = true
                                redemptionErrorMessage = null
                                try {
                                    val token = getOrRefreshIdToken()
                                    if (token.isNullOrBlank()) {
                                        isRedeemingCode = false
                                        redemptionErrorMessage = "انتهت صلاحية جلسة Google، يرجى تسجيل الدخول مجدداً."
                                        currentScreen = AppScreen.SignIn
                                        return@launch
                                    }
                                    val status = repository.checkSubscriptionStatus(token)
                                    isRedeemingCode = false
                                    if (status is ServerStatusResult.Active) {
                                        AppLogger.i("MainActivity", "Subscription active on server! Navigating to MainTabs")
                                        currentScreen = AppScreen.MainTabs
                                    } else if (status is ServerStatusResult.Expired) {
                                        redemptionErrorMessage = "لا يوجد اشتراك نشط مرتبط بهذا الحساب حالياً. يرجى إدخال رمز تفعيل جديد."
                                    } else {
                                        redemptionErrorMessage = "تعذر الاتصال بالخادم للتحقق من الاشتراك. تأكد من اتصال الإنترنت."
                                    }
                                } catch (e: Exception) {
                                    isRedeemingCode = false
                                    redemptionErrorMessage = "حدث خطأ أثناء التحقق: ${e.message}"
                                }
                            }
                        }
                    )
                }

                AppScreen.MainTabs -> {
                    Scaffold(
                        topBar = {
                            TopHeaderBar(
                                currentTab = currentTab,
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
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val tabOrder = listOf(NavigationTab.Trail, NavigationTab.Practice, NavigationTab.Progress, NavigationTab.Profile)
                                    val initialIndex = tabOrder.indexOf(initialState).coerceAtLeast(0)
                                    val targetIndex = tabOrder.indexOf(targetState).coerceAtLeast(0)
                                    val isForward = targetIndex > initialIndex
                                    if (isForward) {
                                        (slideInHorizontally(
                                            initialOffsetX = { it / 5 },
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(
                                            targetOffsetX = { -it / 5 },
                                            animationSpec = tween(180)
                                        ) + fadeOut(animationSpec = tween(180)))
                                    } else {
                                        (slideInHorizontally(
                                            initialOffsetX = { -it / 5 },
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))) togetherWith
                                        (slideOutHorizontally(
                                            targetOffsetX = { it / 5 },
                                            animationSpec = tween(180)
                                        ) + fadeOut(animationSpec = tween(180)))
                                    }
                                },
                                label = "main_tabs_motion"
                            ) { tab ->
                                when (tab) {
                                    NavigationTab.Trail -> {
                                        TrailScreen(
                                            userProfile = userProfile,
                                            scenarios = dbScenarios,
                                            sessions = allSessions,
                                            trainingList = allScenarioTraining,
                                            isSyncing = isSyncing,
                                            onRetrySync = {
                                                repository.contentRepository.refreshAll()
                                            },
                                            selectedLevel = activeTrailLevel,
                                            onLevelSelected = { level ->
                                                activeTrailLevel = level
                                                coroutineScope.launch {
                                                    repository.updateUserLevel(level)
                                                }
                                            },
                                            onSelectScenario = { scenarioId ->
                                                selectedScenarioId = scenarioId
                                                currentScreen = AppScreen.ScenarioDetail
                                            }
                                        )
                                    }

                                    NavigationTab.Practice -> {
                                        PracticeScreen(
                                            vocabularyList = liveVocabList,
                                            grammarList = dbGrammarEntities,
                                            mistakesList = allMistakes,
                                            totalVocabularyCount = totalVocabularyCount,
                                            savedWordsCount = savedWordsCount,
                                            masteredWordsCount = allSessions.sumOf { it.wordsLearned },
                                            isSyncing = isSyncing,
                                            onRetrySync = {
                                                repository.contentRepository.refreshAll()
                                            },
                                            onDeleteMistake = { mistakeId ->
                                                coroutineScope.launch {
                                                    repository.deleteMistake(mistakeId)
                                                }
                                            },
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
                                            userProfile = userProfile,
                                            sessions = allSessions,
                                            totalScenariosCount = dbScenarios.size,
                                            savedWordsCount = savedWordsCount,
                                            totalVocabularyCount = totalVocabularyCount,
                                            scenarios = dbScenarios,
                                            onOpenGoalDialog = {
                                                showGoalSheetFromProgress = true
                                            },
                                            onNavigateToTrail = {
                                                currentTab = NavigationTab.Trail
                                            }
                                        )
                                    }

                                    NavigationTab.Profile -> {
                                        ProfileSettingsScreen(
                                            userProfile = userProfile,
                                            userEntity = currentUserEntity,
                                            sessions = allSessions,
                                            savedWordsCount = savedWordsCount,
                                            onUpdateSpeechSpeed = { speed ->
                                                userProfile.speechSpeed = speed
                                            },
                                            onUpdateSarcasm = { sarcasm ->
                                                userProfile.sarcasmLevel = sarcasm
                                            },
                                            onUpdateReminder = { enabled, frequency, hour, minute ->
                                                repository.updateReminderSettings(enabled, frequency, hour, minute)
                                            },
                                            onUpdateGoal = { goalId, title, targetDays, targetMinutes, intensityPreset ->
                                                repository.updateLearningGoal(goalId, title, targetDays, targetMinutes)
                                            },
                                            onSendTestNotification = {
                                                repository.sendTestNotification()
                                            },
                                            onUpdateName = { newName ->
                                                repository.updateUserName(newName)
                                            },
                                            onSignOut = {
                                                coroutineScope.launch {
                                                    repository.signOut()
                                                    currentIdToken = null
                                                    currentScreen = AppScreen.Welcome
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AppScreen.ScenarioDetail -> {
                    ScenarioDetailScreen(
                        scenario = activeScenario,
                        vocabularyList = liveVocabList,
                        trainingProgress = currentScenarioTraining,
                        isSyncing = isSyncing,
                        onRetrySync = {
                            repository.contentRepository.refreshAll()
                        },
                        onBack = { currentScreen = AppScreen.MainTabs },
                        onStartStudy = {
                            currentScreen = AppScreen.Study
                        },
                        onStartQuiz = {
                            currentScreen = AppScreen.Quiz
                        },
                        onStartConversation = {
                            if (!repository.isSubscriptionActiveSync() && repository.getFreeSessionsRemaining() <= 0) {
                                AnalyticsTracker.trackPaywallViewed("free_trial_exhausted")
                                currentScreen = AppScreen.SubscriptionRedemption
                            } else {
                                val scId = activeScenario?.id ?: selectedScenarioId
                                val level = activeScenario?.cefrLevel ?: "A1"
                                AnalyticsTracker.trackSessionStarted(scenarioId = scId, level = level)
                                val scenarioEntity = dbScenarios.find { it.id == scId }
                                val introText = scenarioEntity?.getInitialMessageForLevel(level).orEmpty()
                                val introTrans = scenarioEntity?.getInitialMessageTranslationForLevel(level)
                                    ?.ifBlank { ScenarioTranslations.getByGermanText(introText) }
                                    .orEmpty()
                                activeIntroMessage = if (introText.isNotBlank()) {
                                    ChatMessage(
                                        id = "intro_${System.currentTimeMillis()}",
                                        sender = MessageSender.Katzu,
                                        germanText = introText,
                                        arabicTranslation = introTrans,
                                        timestamp = "الآن"
                                    )
                                } else null
                                if (introText.isNotBlank() && introTrans.isBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            val trans = GeminiOnlineConversationService.translateToArabic(germanText = introText)
                                            if (trans.isNotBlank()) {
                                                activeIntroMessage = activeIntroMessage?.copy(arabicTranslation = trans)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                currentScreen = AppScreen.LiveConversation
                            }
                        },
                        onWordClick = { word ->
                            selectedWordForInsight = word
                        }
                    )
                }

                AppScreen.Study -> {
                    val scId = activeScenario?.id ?: selectedScenarioId
                    val scenarioEntity = dbScenarios.find { it.id == scId }
                    val currentCefrLevel = activeScenario?.cefrLevel?.take(2) ?: "A1"
                    val mappedTopic = mapCategoryToTopic(scenarioEntity?.category ?: "")
                    val vocabForStudy = remember(dbVocabEntities, mappedTopic, scenarioEntity, currentCefrLevel) {
                        val topicMatched = dbVocabEntities.filter { it.topic.equals(mappedTopic, ignoreCase = true) }
                        if (topicMatched.isNotEmpty()) topicMatched else {
                            dbVocabEntities.filter { it.level.equals(currentCefrLevel, ignoreCase = true) }.ifEmpty { dbVocabEntities }
                        }
                    }
                    val grammarForStudy = remember(dbGrammarEntities, scenarioEntity, currentCefrLevel) {
                        dbGrammarEntities.filter { it.level.equals(currentCefrLevel, ignoreCase = true) }
                    }

                    StudyScreen(
                        scenario = scenarioEntity,
                        vocabularyList = vocabForStudy,
                        grammarList = grammarForStudy,
                        starterPhrases = currentScenarioStarterPhrases,
                        cefrLevel = currentCefrLevel,
                        onBack = { currentScreen = AppScreen.ScenarioDetail },
                        onProceedToQuiz = {
                            coroutineScope.launch {
                                repository.markScenarioStudied(scId)
                            }
                            currentScreen = AppScreen.Quiz
                        },
                        onSpeak = { text, speed ->
                            repository.speak(text, speed)
                        },
                        ttsPlaybackState = ttsPlaybackState,
                        onStopSpeak = {
                            repository.stopSpeaking()
                        }
                    )
                }

                AppScreen.Quiz -> {
                    val scId = activeScenario?.id ?: selectedScenarioId
                    val scenarioEntity = dbScenarios.find { it.id == scId }
                    val currentCefrLevel = activeScenario?.cefrLevel?.take(2) ?: "A1"
                    val mappedTopic = mapCategoryToTopic(scenarioEntity?.category ?: "")
                    val scenarioVocab = remember(dbVocabEntities, mappedTopic, scenarioEntity, currentCefrLevel) {
                        val topicMatched = dbVocabEntities.filter { it.topic.equals(mappedTopic, ignoreCase = true) }
                        if (topicMatched.isNotEmpty()) topicMatched else {
                            dbVocabEntities.filter { it.level.equals(currentCefrLevel, ignoreCase = true) }.ifEmpty { dbVocabEntities }
                        }
                    }
                    val allLevelVocab = remember(dbVocabEntities, currentCefrLevel) {
                        dbVocabEntities.filter { it.level.equals(currentCefrLevel, ignoreCase = true) }.ifEmpty { dbVocabEntities }
                    }

                    QuizScreen(
                        scenario = scenarioEntity,
                        scenarioVocabulary = scenarioVocab,
                        allLevelVocabulary = allLevelVocab,
                        onBack = { currentScreen = AppScreen.Study },
                        onFinishQuiz = { score ->
                            if (!repository.isSubscriptionActiveSync() && repository.getFreeSessionsRemaining() <= 0) {
                                AnalyticsTracker.trackPaywallViewed("free_trial_exhausted")
                                currentScreen = AppScreen.SubscriptionRedemption
                            } else {
                                val scId = activeScenario?.id ?: selectedScenarioId
                                val level = activeScenario?.cefrLevel ?: "A1"
                                AnalyticsTracker.trackSessionStarted(scenarioId = scId, level = level)
                                val scenarioEntity = dbScenarios.find { it.id == scId }
                                val introText = scenarioEntity?.getInitialMessageForLevel(level).orEmpty()
                                val introTrans = scenarioEntity?.getInitialMessageTranslationForLevel(level)
                                    ?.ifBlank { ScenarioTranslations.getByGermanText(introText) }
                                    .orEmpty()
                                activeIntroMessage = if (introText.isNotBlank()) {
                                    ChatMessage(
                                        id = "intro_${System.currentTimeMillis()}",
                                        sender = MessageSender.Katzu,
                                        germanText = introText,
                                        arabicTranslation = introTrans,
                                        timestamp = "الآن"
                                    )
                                } else null
                                if (introText.isNotBlank() && introTrans.isBlank()) {
                                    coroutineScope.launch {
                                        try {
                                            val trans = GeminiOnlineConversationService.translateToArabic(germanText = introText)
                                            if (trans.isNotBlank()) {
                                                activeIntroMessage = activeIntroMessage?.copy(arabicTranslation = trans)
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                coroutineScope.launch {
                                    repository.markScenarioQuizAttempted(scId, score)
                                }
                                currentScreen = AppScreen.LiveConversation
                            }
                        },
                        onSpeak = { text, speed ->
                            repository.speak(text, speed)
                        }
                    )
                }

                AppScreen.LiveConversation -> {
                    val scId = activeScenario?.id ?: selectedScenarioId
                    val level = activeScenario?.cefrLevel ?: "A1"
                    val scenarioEntity = dbScenarios.find { it.id == scId }
                    val initialEffectiveLevel = currentScenarioTraining?.effectiveLevel
                    val effectiveLevel = initialEffectiveLevel ?: level
                    val introText = scenarioEntity?.getInitialMessageForLevel(effectiveLevel).orEmpty()
                    val introTrans = scenarioEntity?.getInitialMessageTranslationForLevel(effectiveLevel)
                        ?.ifBlank { ScenarioTranslations.getByGermanText(introText) }
                        .orEmpty()
                    val initialMsg = activeIntroMessage?.takeIf {
                        it.germanText.isNotBlank() && (it.arabicTranslation.isNotBlank() || introTrans.isBlank())
                    } ?: remember(scId, effectiveLevel, scenarioEntity) {
                        if (introText.isNotBlank()) {
                            ChatMessage(
                                id = "intro_${System.currentTimeMillis()}",
                                sender = MessageSender.Katzu,
                                germanText = introText,
                                arabicTranslation = introTrans,
                                timestamp = "الآن"
                            )
                        } else null
                    }

                    val currentCefrLevel = effectiveLevel.take(2)
                    val mappedTopic = mapCategoryToTopic(scenarioEntity?.category ?: "")
                    val scenarioVocabWords = remember(dbVocabEntities, mappedTopic, currentCefrLevel) {
                        val topicAndLevelMatched = dbVocabEntities.filter {
                            it.topic.equals(mappedTopic, ignoreCase = true) &&
                                it.level.equals(currentCefrLevel, ignoreCase = true)
                        }
                        val selected = if (topicAndLevelMatched.isNotEmpty()) {
                            topicAndLevelMatched
                        } else {
                            AppLogger.w(
                                "MainActivity",
                                "Missing level-appropriate vocabulary in database for scenario='$scId' (topic='$mappedTopic', level='$currentCefrLevel'). Falling back to topic-only words."
                            )
                            val topicMatched = dbVocabEntities.filter { it.topic.equals(mappedTopic, ignoreCase = true) }
                            if (topicMatched.isNotEmpty()) topicMatched else {
                                dbVocabEntities.filter { it.level.equals(currentCefrLevel, ignoreCase = true) }.ifEmpty { dbVocabEntities }
                            }
                        }
                        selected.map { it.german }
                    }

                    LiveConversationScreen(
                        initialMessages = listOfNotNull(initialMsg),
                        scenarioId = scId,
                        cefrLevel = level,
                        initialEffectiveLevel = initialEffectiveLevel,
                        onUpdateEffectiveLevel = { newLevel ->
                            coroutineScope.launch {
                                repository.updateScenarioEffectiveLevel(scId, newLevel)
                            }
                        },
                        scenario = scenarioEntity,
                        starterPhrases = currentScenarioStarterPhrases,
                        curriculumVocabulary = scenarioVocabWords,
                        onSendMessageTurn = { scenarioId, text, history, turnLevel, scenario, curriculumVocab, isFinalTurn ->
                            repository.sendConversationTurn(scenarioId, text, history, turnLevel, scenario, curriculumVocab, isFinalTurn)
                        },
                        onInspectWord = { wordStr ->
                            coroutineScope.launch {
                                selectedWordForInsight = repository.lookupWord(wordStr)
                            }
                        },
                        onBack = { currentScreen = AppScreen.ScenarioDetail },
                        onFinishSession = { sentencesSpoken, wordsLearned, accuracyPercent, durationSeconds, mistakes, praisedSentence, hintsUsedCount, independentSentences ->
                            coroutineScope.launch {
                                repository.recordSession(
                                    scenarioId = scId,
                                    scenarioTitle = activeScenario?.titleGerman ?: "Konversation",
                                    cefrLevel = currentScenarioTraining?.effectiveLevel ?: level,
                                    sentencesSpoken = sentencesSpoken,
                                    wordsLearned = wordsLearned,
                                    accuracyPercent = accuracyPercent,
                                    durationSeconds = durationSeconds
                                )
                                repository.recordMistakes(
                                    scenarioId = scId,
                                    mistakes = mistakes
                                )
                            }
                            if (!repository.isSubscriptionActiveSync()) {
                                val remaining = repository.decrementFreeSessionsRemaining()
                                AppLogger.i("MainActivity", "Session completed under free trial. Remaining: $remaining")
                            }
                            AnalyticsTracker.trackSessionCompleted(
                                scenarioId = scId,
                                level = currentScenarioTraining?.effectiveLevel ?: level,
                                accuracy = accuracyPercent,
                                durationSeconds = durationSeconds,
                                hintsUsedCount = hintsUsedCount
                            )
                            lastSentencesSpoken = sentencesSpoken
                            lastWordsLearned = wordsLearned
                            lastAccuracyPercent = accuracyPercent
                            lastDurationSeconds = durationSeconds
                            lastSessionMistakes = mistakes
                            lastPraisedSentence = praisedSentence
                            lastHintsUsedCount = hintsUsedCount
                            lastIndependentSentences = independentSentences
                            currentScreen = AppScreen.SessionReport
                        },
                        onSpeak = { text, speed ->
                            repository.speak(text, speed)
                        },
                        ttsPlaybackState = ttsPlaybackState,
                        onStopSpeak = {
                            repository.stopSpeaking()
                        }
                    )
                }

                AppScreen.SessionReport -> {
                    SessionReportScreen(
                        sentencesSpoken = lastSentencesSpoken,
                        wordsLearned = lastWordsLearned,
                        streakDays = userProfile.streakDays,
                        accuracyPercent = lastAccuracyPercent,
                        cefrLevel = activeScenario?.cefrLevel ?: "A1",
                        scenarioTitle = activeScenario?.titleArabic ?: activeScenario?.titleGerman ?: "المحادثة الحية",
                        mistakes = lastSessionMistakes,
                        praisedSentence = lastPraisedSentence,
                        hintsUsedCount = lastHintsUsedCount,
                        independentSentences = lastIndependentSentences,
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
                        },
                        onUpgradeLevel = { nextLevel ->
                            coroutineScope.launch {
                                repository.updateUserLevel(nextLevel)
                                activeTrailLevel = nextLevel
                                currentTab = NavigationTab.Trail
                                currentScreen = AppScreen.MainTabs
                            }
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
                    coroutineScope.launch {
                        val isNowSaved = repository.toggleWordSaved(wordId)
                        selectedWordForInsight = selectedWordForInsight?.copy(isSaved = isNowSaved)
                    }
                }
            )
        }

        // Global Goal Selection Bottom Sheet (opened from Progress Tab)
        if (showGoalSheetFromProgress) {
            GoalSelectionBottomSheet(
                currentGoalId = userProfile.targetGoalId,
                currentIntensityPreset = userProfile.reminderFrequencyPreset,
                currentExperience = "never_studied",
                onDismiss = { showGoalSheetFromProgress = false },
                onSaveGoal = { goalId, title, targetDays, targetMinutes, intensityPreset, experience ->
                    val chosenLevel = GOAL_PRESETS.find { it.id == goalId }?.levelCode ?: "A1"
                    repository.updateUserLevel(chosenLevel)
                    activeTrailLevel = chosenLevel
                    repository.updateLearningGoal(goalId, title, targetDays, targetMinutes)
                    repository.updateReminderSettings(userProfile.dailyRemindersEnabled, intensityPreset, userProfile.reminderHour, userProfile.reminderMinute)
                    showGoalSheetFromProgress = false
                }
            )
        }

        // Onboarding Goal Selection Bottom Sheet (opened from Welcome Screen on first run)
        if (showOnboardingGoalPicker) {
            GoalSelectionBottomSheet(
                currentGoalId = userProfile.targetGoalId,
                currentIntensityPreset = userProfile.reminderFrequencyPreset,
                currentExperience = "never_studied",
                onDismiss = {
                    repository.setCompletedOnboarding(true)
                    showOnboardingGoalPicker = false
                    currentScreen = AppScreen.MainTabs
                },
                onSaveGoal = { goalId, title, targetDays, targetMinutes, intensityPreset, experience ->
                    val chosenLevel = GOAL_PRESETS.find { it.id == goalId }?.levelCode ?: "A1"
                    repository.updateUserLevel(chosenLevel)
                    activeTrailLevel = chosenLevel
                    repository.updateLearningGoal(goalId, title, targetDays, targetMinutes)
                    repository.updateReminderSettings(userProfile.dailyRemindersEnabled, intensityPreset, userProfile.reminderHour, userProfile.reminderMinute)
                    repository.setCompletedOnboarding(true)
                    AnalyticsTracker.trackOnboardingCompleted(goalId = goalId, experience = experience)
                    showOnboardingGoalPicker = false
                    currentScreen = AppScreen.MainTabs
                }
            )
        }

    }
}
