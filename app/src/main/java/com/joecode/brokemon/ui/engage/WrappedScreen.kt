package com.joecode.brokemon.ui.engage

import com.joecode.brokemon.ui.theme.Spacing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.joecode.brokemon.domain.Wrapped
import com.joecode.brokemon.ui.share.ShareImage
import com.joecode.brokemon.ui.share.StoryImages
import com.joecode.brokemon.ui.share.StoryPreviewDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.domain.WrappedSummary
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.CatchCube
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.Led
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.detail.formatDate
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlin.math.absoluteValue

private const val PAGES = 6

@Composable
fun WrappedScreen(
    onBack: () -> Unit,
    viewModel: WrappedViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pager = rememberPagerState { PAGES }
    var showShare by remember { mutableStateOf(false) }

    DexScaffold(
        title = "Brodex Wrapped",
        onBack = onBack,
        actions = {
            if (state.summary != null) {
                IconButton(onClick = { showShare = true }) {
                    Icon(Icons.Filled.IosShare, contentDescription = "Share my Wrapped", tint = DexColors.Text)
                }
            }
        },
    ) { padding ->
        val summary = state.summary ?: run {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = DexColors.DexRed)
            }
            return@DexScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isPreview) {
                Text(
                    "DEBUG PREVIEW: in release builds Wrapped only appears Dec 20 to Jan 15.",
                    style = PixelText.Tiny,
                    color = DexColors.LedYellow,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg)
                        .graphicsLayer {
                            // Read the scroll offset here (draw phase) so paging doesn't recompose.
                            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                            alpha = 1f - offset.coerceIn(0f, 1f) * 0.5f
                            val scale = 1f - offset.coerceIn(0f, 1f) * 0.1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .background(
                            Brush.verticalGradient(listOf(slideColor(page).copy(alpha = 0.35f), DexColors.Surface)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val active = pager.currentPage == page
                    WrappedSlide(page, summary, state.stages, active)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            ) {
                repeat(PAGES) { i ->
                    Led(if (i == pager.currentPage) DexColors.LedGreen else DexColors.Outline, 1f, 10.dp)
                }
            }
        }
    }
    if (showShare) WrappedShare(state) { showShare = false }
}

@Composable
private fun WrappedShare(state: WrappedUiState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val summary = state.summary ?: return
    StoryPreviewDialog(
        title = "Share your ${summary.year}",
        render = { StoryImages(context).wrapped(summary, state.stages) },
        onShare = { ShareImage.share(context, it, "brodex-wrapped-${summary.year}", "Share your Brodex Wrapped") },
        onDismiss = onDismiss,
    )
}

private fun slideColor(page: Int): Color = listOf(
    DexColors.DexRed,
    DexColors.LedBlue,
    DexColors.LedGreen,
    DexColors.LedYellow,
    DexColors.Legendary,
    DexColors.DexRedLight,
)[page % 6]

@Composable
private fun WrappedSlide(page: Int, s: WrappedSummary, stages: Map<Long, EvolutionStage>, active: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        when (page) {
            0 -> {
                CatchCube(Modifier.width(96.dp).height(96.dp))
                Big("BRODEX\nWRAPPED")
                Text(s.year.toString(), style = PixelText.Title, color = DexColors.LedYellow)
                Text("YOU'RE A", style = PixelText.Tiny, color = DexColors.TextMuted)
                Text(Wrapped.title(s).uppercase(), style = PixelText.Header, color = DexColors.Gold, textAlign = TextAlign.Center)
                Body("Your year in bros. Swipe to relive it.")
            }

            1 -> {
                val count by animateIntAsState(if (active) s.caughtCount else 0, tween(1200), label = "count")
                Label("Bros caught")
                Text("%03d".format(count), style = PixelText.Title.copy(fontSize = PixelText.Title.fontSize * 2.5f), color = DexColors.ScreenText)
                Body(
                    when {
                        s.caughtCount == 0 -> "No new catches this year. Quality over quantity."
                        s.shinyCount > 0 -> "Including ${s.shinyCount} shiny! Lucky."
                        else -> "Every one of them a certified bro."
                    },
                )
            }

            2 -> {
                Label("Most caught types")
                if (s.topTypes.isEmpty()) Body("No types logged yet.")
                val max = s.topTypes.maxOfOrNull { it.second } ?: 1
                s.topTypes.forEachIndexed { i, (type, n) ->
                    val fill by animateFloatAsState(if (active) n / max.toFloat() else 0f, tween(900, delayMillis = i * 200), label = "type$i")
                    Column(Modifier.fillMaxWidth()) {
                        Row {
                            Text("${i + 1}. ${type.label.uppercase()}", style = PixelText.Label, color = type.color, modifier = Modifier.weight(1f))
                            Text("x$n", style = PixelText.Label, color = DexColors.Text)
                        }
                        Spacer(Modifier.height(6.dp))
                        SegmentedBar(fill, type.color, Modifier.fillMaxWidth().height(14.dp))
                    }
                }
            }

            3 -> {
                Label("First catch of the year")
                val first = s.firstBro
                if (first == null) Body("Your first catch is still out there.")
                else {
                    BroCard(first, stages[first.id] ?: EvolutionStage.ROOKIE, Modifier.widthIn(max = 200.dp))
                    Body("${first.name}, caught ${formatDate(first.catchDate)}.")
                }
            }

            4 -> {
                Label("Rarest catches")
                if (s.rarest.isEmpty()) Body("All commons this year. Commons are the backbone.")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    s.rarest.forEach { bro ->
                        BroCard(bro, stages[bro.id] ?: EvolutionStage.ROOKIE, Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Box(contentAlignment = Alignment.Center) {
                    if (active) Sparkles(Modifier.width(240.dp).height(160.dp), count = 16)
                    Label("Memory MVP")
                }
                val mvp = s.mostMemories
                if (mvp == null) Body("No memories logged in ${s.year}. Grab a pic next hangout!")
                else {
                    BroCard(mvp.first, stages[mvp.first.id] ?: EvolutionStage.ROOKIE, Modifier.widthIn(max = 200.dp))
                    Body("${mvp.second} memories with ${mvp.first.name}.")
                }
                Text("${s.memoriesMade} memories total in ${s.year}", style = PixelText.Tiny, color = DexColors.TextMuted)
            }
        }
    }
}

@Composable
private fun Big(text: String) = Text(text, style = PixelText.Title, color = DexColors.Text, textAlign = TextAlign.Center)

@Composable
private fun Label(text: String) = Text(text.uppercase(), style = PixelText.Header, color = DexColors.Text, textAlign = TextAlign.Center)

@Composable
private fun Body(text: String) =
    Text(text, color = DexColors.TextMuted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
