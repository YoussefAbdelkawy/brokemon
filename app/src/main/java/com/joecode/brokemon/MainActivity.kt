package com.joecode.brokemon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.joecode.brokemon.domain.Journal
import com.joecode.brokemon.ui.components.LocalHintStore
import com.joecode.brokemon.ui.components.LocalUnlockedRewards
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joecode.brokemon.ui.navigation.BrokemonApp
import com.joecode.brokemon.ui.navigation.Routes
import androidx.lifecycle.lifecycleScope
import com.joecode.brokemon.ui.feedback.FeedbackController
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import com.joecode.brokemon.ui.theme.BrokemonTheme
import com.joecode.brokemon.ui.theme.DexColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** A card to open, set when launched from the widget or a notification. */
    private val pendingBroId = mutableStateOf<Long?>(null)

    /** A route to open, set by an app-icon shortcut. */
    private val pendingRoute = mutableStateOf<String?>(null)

    private lateinit var feedback: FeedbackController

    override fun onStart() {
        super.onStart()
        feedback.refreshMotion()
        feedback.resumeMusic()
    }

    override fun onStop() {
        super.onStop()
        feedback.pauseMusic()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            pendingBroId.value = intent.broIdExtra()
            handleShortcut(intent)
        }

        val prefs = (application as BrokemonApplication).container.prefs
        feedback = FeedbackController(applicationContext, lifecycleScope, prefs)
        var onboardingKnown = false
        // Hold the splash until we know whether to show onboarding, so there's no flash.
        splash.setKeepOnScreenCondition { !onboardingKnown }

        setContent {
            BrokemonTheme {
                val done by prefs.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                val view = LocalView.current
                SideEffect { feedback.view = view }
                val reduceMotion by feedback.reduceMotion.collectAsStateWithLifecycle()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DexColors.Background),
                ) {
                    val claimed by prefs.claimedQuests.collectAsStateWithLifecycle(initialValue = emptySet())
                    val shinyEarned by prefs.shinyEarned.collectAsStateWithLifecycle(initialValue = false)
                    val champion by prefs.tournamentWon.collectAsStateWithLifecycle(initialValue = false)
                    done?.let { isDone ->
                        onboardingKnown = true
                        // Read once: flipping the start destination later would reset the nav graph.
                        val showOnboarding = remember { !isDone }
                        CompositionLocalProvider(
                            LocalHintStore provides prefs,
                            LocalFeedback provides feedback,
                            LocalReduceMotion provides reduceMotion,
                            LocalUnlockedRewards provides Journal.unlocked(claimed, shinyEarned, champion),
                        ) {
                            BrokemonApp(
                                showOnboarding = showOnboarding,
                                onOnboardingDone = { scope.launch { prefs.setOnboardingDone() } },
                                openBroId = pendingBroId.value,
                                onOpenHandled = { pendingBroId.value = null },
                                openRoute = pendingRoute.value,
                                onRouteHandled = { pendingRoute.value = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.broIdExtra()?.let { pendingBroId.value = it }
        handleShortcut(intent)
    }

    /** App-icon shortcuts: Catch a Bro, Scan QR, Random Bro. */
    private fun handleShortcut(intent: Intent?) {
        when (intent?.action) {
            ACTION_CATCH -> pendingRoute.value = Routes.CATCH_BRO
            ACTION_SCAN -> pendingRoute.value = Routes.TRADE_SCAN
            ACTION_RANDOM -> lifecycleScope.launch {
                val bros = (application as BrokemonApplication).container.repository.allBrosOnce()
                if (bros.isEmpty()) pendingRoute.value = Routes.CATCH_BRO else pendingBroId.value = bros.random().id
            }
        }
    }

    private fun Intent?.broIdExtra(): Long? =
        this?.getLongExtra(EXTRA_BRO_ID, -1L)?.takeIf { it > 0 }

    companion object {
        const val EXTRA_BRO_ID = "com.joecode.brokemon.extra.BRO_ID"
        const val ACTION_CATCH = "com.joecode.brokemon.action.CATCH"
        const val ACTION_SCAN = "com.joecode.brokemon.action.SCAN"
        const val ACTION_RANDOM = "com.joecode.brokemon.action.RANDOM"
    }
}
