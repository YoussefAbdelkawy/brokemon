package com.joecode.brokemon.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.CatchCube
import com.joecode.brokemon.ui.components.LedCluster
import com.joecode.brokemon.ui.components.LensLed
import com.joecode.brokemon.ui.components.Led
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var agreed by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LensLed()
            Spacer(Modifier.width(12.dp))
            LedCluster()
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> CatchPage()
                    else -> PrivacyPage(agreed) { agreed = it }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(3) { Led(if (it == pager.currentPage) DexColors.LedGreen else DexColors.Outline, 1f, 10.dp) }
        }
        val last = pager.currentPage == 2
        PixelButton(
            text = if (last) "Start my Brodex" else "Next",
            enabled = !last || agreed,
            onClick = { if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WelcomePage() {
    val float by rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "floatY",
    )
    CatchCube(Modifier.size(140.dp).graphicsLayer { translationY = float; rotationZ = float / 2 })
    Spacer(Modifier.height(32.dp))
    Text("BROKEMON", style = PixelText.Title, color = DexColors.DexRed)
    Spacer(Modifier.height(16.dp))
    Text(
        "Gotta catch all your bros. Turn your real-life friends into collectible cards that level up as your friendship does.",
        color = DexColors.Text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun CatchPage() {
    val bounce by rememberInfiniteTransition(label = "parade").animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "bounce",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(BroType.HYPE, BroType.GAMER, BroType.FOODIE).forEachIndexed { i, type ->
            Box(Modifier.size(84.dp).graphicsLayer { translationY = if (i % 2 == 0) bounce else -bounce - 12f }) {
                BroSprite(seed = 42L + i * 7, stage = i, type1 = type, type2 = null, shiny = i == 2, modifier = Modifier.fillMaxSize())
            }
        }
    }
    Spacer(Modifier.height(32.dp))
    Text("CATCH. BOND. EVOLVE.", style = PixelText.Header, color = DexColors.LedYellow, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text(
        "Log memories, check-ins and facts. The more you hang out, the more your bros evolve. Form squads and trade cards by QR.",
        color = DexColors.Text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun PrivacyPage(agreed: Boolean, onAgreedChange: (Boolean) -> Unit) {
    ScreenPanel(title = "Privacy first") {
        Text(
            "Your Brodex lives only on this phone. No account, no cloud, no ads, no tracking. " +
                "Cards leave your phone only when you show someone a QR code, and photos never do.",
            color = DexColors.ScreenText,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(Modifier.height(24.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onAgreedChange(!agreed) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = agreed,
            onCheckedChange = onAgreedChange,
            colors = CheckboxDefaults.colors(checkedColor = DexColors.DexRed),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "I'll only add photos, videos and details of friends who are cool with it.",
            color = DexColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
