package com.joecode.brokemon.ui.engage

import com.joecode.brokemon.ui.theme.Spacing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.EmptyState
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

@Composable
fun CheckOnBroScreen(
    onBack: () -> Unit,
    onOpenBro: (Long) -> Unit,
    viewModel: CheckOnBroViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DexScaffold(title = "Check on a bro", onBack = onBack) { padding ->
        val pick = state.pick
        if (!state.isLoading && pick == null) {
            EmptyState(
                title = "Nobody to check on",
                body = "Catch a bro first and the dex will start nudging you to keep in touch.",
                modifier = Modifier.padding(padding),
            )
            return@DexScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val wave by rememberInfiniteTransition(label = "wave").animateFloat(
                initialValue = -12f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                label = "waveAngle",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WavingHand,
                    null,
                    tint = DexColors.LedYellow,
                    modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = wave },
                )
                Spacer(Modifier.size(10.dp))
                Text("INCOMING SIGNAL", style = PixelText.Label, color = DexColors.LedYellow)
            }
            AnimatedContent(
                targetState = pick,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                },
                label = "pick",
            ) { entry ->
                if (entry != null) {
                    BroCard(entry.bro, entry.stage, Modifier.widthIn(max = 240.dp), onClick = { onOpenBro(entry.bro.id) })
                }
            }
            if (pick != null) {
                Text(
                    when (state.daysSince) {
                        0L -> "You caught up with ${pick.bro.name} today. Overachiever."
                        1L -> "It's been a day since you checked on ${pick.bro.name}."
                        else -> "It's been ${state.daysSince} days since you checked on ${pick.bro.name}."
                    },
                    color = DexColors.Text,
                    textAlign = TextAlign.Center,
                )
                ScreenPanel(title = "Conversation starters", modifier = Modifier.fillMaxWidth()) {
                    state.prompts.forEach {
                        Text("> $it", color = DexColors.ScreenText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = Spacing.xs))
                    }
                }
                PixelButton(
                    text = if (state.checkedIn) "Checked in today" else "I checked in +${Evolution.CHECK_IN_POINTS}",
                    onClick = viewModel::checkIn,
                    enabled = !state.checkedIn,
                    color = DexColors.LedGreen.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.canReroll) {
                    TextButton(onClick = viewModel::recommend) {
                        Icon(Icons.Filled.Casino, null, tint = DexColors.TextMuted)
                        Spacer(Modifier.size(6.dp))
                        Text("SOMEONE ELSE", style = PixelText.Tiny, color = DexColors.TextMuted)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
