package com.joecode.brokemon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joecode.brokemon.ui.navigation.BrokemonApp
import com.joecode.brokemon.ui.theme.BrokemonTheme
import com.joecode.brokemon.ui.theme.DexColors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = (application as BrokemonApplication).container.prefs
        var onboardingKnown = false
        // Hold the splash until we know whether to show onboarding, so there's no flash.
        splash.setKeepOnScreenCondition { !onboardingKnown }

        setContent {
            BrokemonTheme {
                val done by prefs.onboardingDone.collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DexColors.Background),
                ) {
                    done?.let { isDone ->
                        onboardingKnown = true
                        // Read once: flipping the start destination later would reset the nav graph.
                        val showOnboarding = remember { !isDone }
                        BrokemonApp(
                            showOnboarding = showOnboarding,
                            onOnboardingDone = { scope.launch { prefs.setOnboardingDone() } },
                        )
                    }
                }
            }
        }
    }
}
