package com.joecode.brokemon.ui.pack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.domain.Cosmetic
import com.joecode.brokemon.domain.CosmeticKind
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.Dexy
import com.joecode.brokemon.ui.components.DexyMood
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.PixelIcon
import com.joecode.brokemon.ui.components.PixelIconImage
import com.joecode.brokemon.ui.components.StickerImage
import com.joecode.brokemon.ui.components.cardFrameBrush
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import com.joecode.brokemon.ui.feedback.Sfx
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PackUiState(
    val loaded: Boolean = false,
    val owned: Set<String> = emptySet(),
    /** Today's pack is still sealed. */
    val available: Boolean = false,
    /** What came out of the pack that was just opened (null = collection already complete). */
    val opened: Cosmetic? = null,
    val justOpened: Boolean = false,
)

/**
 * The Daily Pack: one free cosmetic a day. There is no streak and no penalty for missing a day:
 * the pack is "today's" whenever you open the app, and an opened pack is just done until tomorrow.
 * Everything inside is cosmetic only.
 */
class PackViewModel(private val prefs: UserPrefs) : ViewModel() {
    private val opened = MutableStateFlow<Pair<Cosmetic?, Boolean>?>(null)

    val state: StateFlow<PackUiState> = combine(prefs.ownedCosmetics, prefs.packDay, opened) { owned, day, result ->
        PackUiState(
            loaded = true,
            owned = owned,
            available = result == null && day != LocalDate.now().toEpochDay(),
            opened = result?.first,
            justOpened = result != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackUiState())

    fun open() {
        viewModelScope.launch {
            val owned = state.value.owned
            val item = Cosmetic.roll(owned)
            prefs.setPackDay(LocalDate.now().toEpochDay())
            item?.let { prefs.addCosmetic(it.id) }
            opened.update { item to true }
        }
    }
}

@Composable
fun PackScreen(
    onBack: () -> Unit,
    onCollection: () -> Unit,
    viewModel: PackViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val tear = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    var tearing by remember { mutableStateOf(false) }

    fun startTear() {
        if (tearing || !state.available) return
        tearing = true
        scope.launch {
            feedback?.play(Sfx.PACK_TEAR, 0.7f)
            feedback?.thump()
            viewModel.open()
            tear.animateTo(1f, tween(if (reduce) 1 else 750, easing = FastOutSlowInEasing))
            delay(if (reduce) 0 else 150)
            feedback?.play(Sfx.PACK_OPEN, 0.7f)
            revealed = true
        }
    }

    DexScaffold(title = "Daily pack", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                !state.loaded -> Unit
                revealed || (!state.available && state.justOpened) -> RevealedPack(state.opened, state.owned.size)
                state.available -> {
                    Text("TAP THE PACK TO TEAR IT OPEN", style = PixelText.Label, color = DexColors.LedYellow, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Spacing.xl))
                    FoilPack(tear.value, Modifier.semantics { contentDescription = "Daily pack. Double tap to open." }.clickable(
                        remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = ::startTear,
                    ))
                    Spacer(Modifier.height(Spacing.xl))
                    Text(
                        "One free cosmetic a day. Cosmetics never change how a bro fights.",
                        color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    Dexy(DexyMood.SLEEPY, size = 96.dp)
                    Spacer(Modifier.height(Spacing.lg))
                    Text("TODAY'S PACK IS OPEN", style = PixelText.Label, color = DexColors.Text)
                    Spacer(Modifier.height(Spacing.sm))
                    Text("A fresh one will be here tomorrow. No streaks, no pressure.", color = DexColors.TextMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Spacing.xl))
                    PixelButton("Collection ${state.owned.size}/${Cosmetic.packable.size}", onClick = onCollection, color = DexColors.LedBlue)
                }
            }
            if (revealed || (!state.available && state.justOpened)) {
                Spacer(Modifier.height(Spacing.lg))
                PixelButton("Collection ${state.owned.size}/${Cosmetic.packable.size}", onClick = onCollection, color = DexColors.LedBlue, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.sm))
                PixelButton("Done", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** A pixel foil pack. [tear] 0..1: the top strip rips off and flies away. */
@Composable
private fun FoilPack(tear: Float, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 168.dp, height = 240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = if (tear in 0.01f..0.5f) (kotlin.math.sin(tear * 60f) * 3f) else 0f }) {
            val u = 6.dp.toPx()
            val cols = (size.width / u).toInt()
            val rows = (size.height / u).toInt()
            val body = listOf(Color(0xFFD7263D), Color(0xFFB0172F))
            val stripH = 3
            // Body (below the tear strip): diagonal foil stripes.
            for (y in stripH until rows) for (x in 0 until cols) {
                val edge = x == 0 || x == cols - 1 || y == rows - 1
                val color = when {
                    edge -> Color(0xFF0B0B10)
                    x == 1 || x == cols - 2 || y == rows - 2 -> Color(0xFFFFD54A)
                    else -> body[((x + y) / 3) % 2]
                }
                drawRect(color, Offset(x * u, y * u), Size(u + 0.5f, u + 0.5f))
            }
            // Crimped top strip: moves up/right, spins off and fades out.
            val dx = tear * size.width * 0.7f
            val dy = -tear * size.height * 0.35f
            for (y in 0 until stripH) for (x in 0 until cols) {
                val edge = x == 0 || x == cols - 1 || y == 0
                val color = if (edge) Color(0xFF0B0B10) else if ((x + y) % 2 == 0) Color(0xFFFFD54A) else Color(0xFFE6B800)
                drawRect(color.copy(alpha = 1f - tear), Offset(x * u + dx, y * u + dy - tear * u * x * 0.3f), Size(u + 0.5f, u + 0.5f))
            }
            // A dashed tear line.
            if (tear == 0f) for (x in 1 until cols - 1 step 2) drawRect(Color(0xFF0B0B10), Offset(x * u, stripH * u - 1f), Size(u, 3f))
            // Light burst inside the pack as it opens.
            if (tear > 0f) drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.8f * (1f - tear)), Color.Transparent), Offset(size.width / 2, u * stripH), size.width * 0.7f * tear + 1f), Offset.Zero, size)
        }
        // The pack's label: an original badge, not a logo of anything.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 36.dp)) {
            PixelIconImage(PixelIcon.PACK_OPEN, tint = Color(0xFFFFF3B0), accent = Color.White, size = 56.dp)
            Spacer(Modifier.height(Spacing.sm))
            Text("BRO PACK", style = PixelText.Label, color = Color(0xFFFFF3B0))
            Text("DAILY", style = PixelText.Tiny, color = Color.White)
        }
    }
}

@Composable
private fun RevealedPack(item: Cosmetic?, ownedCount: Int) {
    val scale = remember { Animatable(0.2f) }
    val reduce = LocalReduceMotion.current
    androidx.compose.runtime.LaunchedEffect(item) {
        if (reduce) scale.snapTo(1f) else scale.animateTo(1f, androidx.compose.animation.core.spring(0.45f, 300f))
    }
    if (item == null) {
        Dexy(DexyMood.WOW, size = 96.dp)
        Spacer(Modifier.height(Spacing.md))
        Text("YOU HAVE IT ALL!", style = PixelText.Header, color = DexColors.LedYellow)
        Text("The whole collection is yours.", color = DexColors.TextMuted)
        return
    }
    Text("NEW ${item.kind.label.uppercase()}!", style = PixelText.Header, color = DexColors.LedYellow, textAlign = TextAlign.Center)
    Spacer(Modifier.height(Spacing.lg))
    Box(
        Modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .size(180.dp)
            .background(Brush.radialGradient(listOf(DexColors.LedYellow.copy(alpha = 0.25f), Color.Transparent)))
            .semantics { contentDescription = "${item.kind.label}: ${item.label}" },
        contentAlignment = Alignment.Center,
    ) { CosmeticPreview(item, 140.dp) }
    Spacer(Modifier.height(Spacing.md))
    Text(item.label.uppercase(), style = PixelText.Label, color = DexColors.Text)
    Spacer(Modifier.height(Spacing.xs))
    Text(usageHint(item), color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
}

fun usageHint(item: Cosmetic): String = when (item.kind) {
    CosmeticKind.STICKER -> "Place it on the back of any card."
    CosmeticKind.FRAME -> "Pick it from a card's menu: Card frame."
    CosmeticKind.BACKDROP -> "Now available in the character builder."
    CosmeticKind.ACCESSORY -> "Now available in the character builder."
    CosmeticKind.DECOR -> "Now available in your Trainer Room."
}

/** A picture of any cosmetic: art for stickers, a sample character for looks, a swatch for frames. */
@Composable
fun CosmeticPreview(item: Cosmetic, size: androidx.compose.ui.unit.Dp, locked: Boolean = false) {
    when (item.kind) {
        CosmeticKind.STICKER -> StickerImage(item, size, locked = locked)
        CosmeticKind.BACKDROP, CosmeticKind.ACCESSORY -> {
            val base = BroLook(skin = 2, hair = 1, outfit = 1, outfitColor = 1)
            val look = when (item.lookPart) {
                com.joecode.brokemon.data.model.LookPart.BACKGROUND -> base.copy(background = item.lookIndex)
                else -> base.copy(hat = item.lookIndex)
            }
            BroSprite(look, 0, false, Modifier.size(size), tint = if (locked) DexColors.Outline else null)
        }
        CosmeticKind.FRAME -> Box(
            Modifier.size(size * 0.8f, size).background(DexColors.Surface)
                .let { m -> item.frame?.let { f -> cardFrameBrush(f) }?.let { b -> m.border3(b) } ?: m },
        )
        CosmeticKind.DECOR -> PixelIconImage(PixelIcon.ROOM, tint = if (locked) DexColors.Outline else DexColors.LedYellow, size = size * 0.6f)
    }
}

private fun Modifier.border3(brush: Brush): Modifier = this.then(Modifier.border(Borders.heavy, brush, androidx.compose.ui.graphics.RectangleShape))
