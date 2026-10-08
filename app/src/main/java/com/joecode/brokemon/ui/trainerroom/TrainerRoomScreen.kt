package com.joecode.brokemon.ui.trainerroom

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.BroRepository
import com.joecode.brokemon.data.UserPrefs
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroRoom
import com.joecode.brokemon.data.model.RoomPart
import com.joecode.brokemon.domain.Cosmetic
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.domain.RoomRenderer
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.AvatarBitmaps
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.LocalOwnedCosmetics
import com.joecode.brokemon.ui.components.PixelIcon
import com.joecode.brokemon.ui.components.PixelIconImage
import com.joecode.brokemon.ui.components.PixelPanel
import com.joecode.brokemon.ui.components.pixelBox
import com.joecode.brokemon.ui.components.rememberClock
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.feedback.LocalReduceMotion
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class TrainerRoomState(
    val room: BroRoom = DEFAULT_ROOM,
    val look: BroLook = BroLook(),
    val name: String = "",
    val favorites: List<Bro> = emptyList(),
    val trophies: Int = 0,
    val champion: Boolean = false,
    val shiny: Boolean = false,
    val loaded: Boolean = false,
) {
    companion object {
        /** The trainer starts standing in a plain room. */
        val DEFAULT_ROOM = BroRoom(seat = 0)
    }
}

/** Your own room: the trainer, three favorite bros wandering around, and a shelf of trophies. */
class TrainerRoomViewModel(repository: BroRepository, private val prefs: UserPrefs) : ViewModel() {
    val state: StateFlow<TrainerRoomState> = combine(
        prefs.trainer, prefs.trainerRoom, repository.bros, prefs.trophies,
        combine(prefs.tournamentWon, prefs.shinyEarned) { c, s -> c to s },
    ) { trainer, roomParts, bros, trophies, flags ->
        TrainerRoomState(
            room = roomParts?.let(BroRoom::fromList) ?: TrainerRoomState.DEFAULT_ROOM,
            look = trainer?.look ?: BroLook(),
            name = trainer?.name.orEmpty(),
            favorites = bros.sortedByDescending { Evolution.score(it) }.take(3),
            trophies = trophies,
            champion = flags.first,
            shiny = flags.second,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrainerRoomState())

    fun setRoom(room: BroRoom) {
        viewModelScope.launch { prefs.setTrainerRoom(room.toList()) }
    }
}

@Composable
fun TrainerRoomScreen(onBack: () -> Unit, viewModel: TrainerRoomViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val owned = LocalOwnedCosmetics.current
    val feedback = LocalFeedback.current
    DexScaffold(title = "Trainer room", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            RoomStage(state)
            TrophyShelf(state)
            Decorator(state.room, owned) { feedback?.success("Room updated"); viewModel.setRoom(it) }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun RoomStage(state: TrainerRoomState) {
    val room = state.room
    val place = RoomRenderer.placement(room)
    val roomBitmap = remember(room) { pixels(RoomRenderer.render(room)) }
    val me = remember(state.look, place.sitting) { AvatarBitmaps.fullBody(state.look, 0, false, place.sitting).asImageBitmap() }
    val reduce = LocalReduceMotion.current
    val clock = rememberClock(10)
    val wanderers = remember(state.favorites) {
        state.favorites.map { it to AvatarBitmaps.fullBody(it.resolvedLook, Evolution.info(it).stage.ordinal, it.isShiny, false).asImageBitmap() }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(RoomRenderer.W / RoomRenderer.H.toFloat())) {
        val px = maxWidth / RoomRenderer.W
        Image(
            roomBitmap, "${state.name.ifBlank { "Your" }} room",
            Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = AvatarBitmaps.filterQuality,
        )
        // Wandering favorites, smaller than you and walking back and forth along the floor.
        wanderers.forEachIndexed { i, (bro, bmp) ->
            val speed = 0.035f + i * 0.012f
            val t = if (reduce) (i + 1) / 4f else ((clock.value / 1000f) * speed + i * 0.31f) % 1f
            val tri = 1f - abs(2f * t - 1f) // 0..1..0
            val goingRight = t < 0.5f
            val scale = 0.6f
            val w = HumanSprite.BODY_W * scale
            val h = HumanSprite.BODY_H * scale
            val x = 6f + tri * (RoomRenderer.W - 12f - w)
            val hop = if (reduce) 0f else abs(kotlin.math.sin((clock.value / 1000f) * 6f + i)) * 1.2f
            Image(
                bmp, bro.name,
                Modifier
                    .offset { IntOffset((px * x).roundToPx(), (px * (87f - h - hop)).roundToPx()) }
                    .size(px * w, px * h)
                    .graphicsLayer { scaleX = if (goingRight) 1f else -1f },
                contentScale = ContentScale.FillBounds, filterQuality = AvatarBitmaps.filterQuality,
            )
        }
        Image(
            me, state.name.ifBlank { "You" },
            Modifier
                .offset { IntOffset((px * place.x).roundToPx(), (px * place.y).roundToPx()) }
                .size(px * HumanSprite.BODY_W, px * HumanSprite.BODY_H),
            contentScale = ContentScale.FillBounds, filterQuality = AvatarBitmaps.filterQuality,
        )
    }
}

@Composable
private fun TrophyShelf(state: TrainerRoomState) {
    PixelPanel(title = "Trophy shelf", modifier = Modifier.fillMaxWidth()) {
        val medals = buildList {
            repeat(state.trophies.coerceAtMost(10)) { add("Battle trophy" to DexColors.Gold) }
            if (state.champion) add("Tournament champion" to DexColors.Legendary)
            if (state.shiny) add("Shiny hunter" to DexColors.LedBlue)
        }
        if (medals.isEmpty()) {
            Text("Win battles and tournaments to fill this shelf.", color = DexColors.TextMuted)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.semantics { contentDescription = "${medals.size} trophies" }) {
                medals.take(8).forEach { (label, color) -> PixelIconImage(PixelIcon.TROPHY, tint = color, size = 32.dp) }
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(medals.map { it.first }.distinct().joinToString(" · "), color = DexColors.TextMuted, style = PixelText.Tiny)
        }
    }
}

@Composable
private fun Decorator(room: BroRoom, owned: Set<String>, onChange: (BroRoom) -> Unit) {
    var part by rememberSaveable { mutableStateOf(RoomPart.SEAT) }
    PixelPanel(title = "Decorate", modifier = Modifier.fillMaxWidth()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(RoomPart.entries) { p -> com.joecode.brokemon.ui.components.PixelChip(p.label, p == part, { part = p }) }
        }
        Spacer(Modifier.height(Spacing.md))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items((0 until part.count).toList()) { i ->
                val option = room.with(part, i)
                val lock = Cosmetic.forRoom(part, i)
                val locked = lock != null && lock.id !in owned
                val preview = remember(option) { pixels(RoomRenderer.render(option)) }
                val selected = room[part] == i
                Column(
                    Modifier
                        .width(112.dp)
                        .semantics { contentDescription = part.options[i] + if (locked) ", locked: find it in the Daily Pack" else "" }
                        .clickable(enabled = !locked) { onChange(option) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            preview, null,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(RoomRenderer.W / RoomRenderer.H.toFloat())
                                .graphicsLayer { alpha = if (locked) 0.3f else 1f }
                                .border(Borders.normal, if (selected) DexColors.LedYellow else DexColors.ScreenBorder),
                            contentScale = ContentScale.FillBounds, filterQuality = AvatarBitmaps.filterQuality,
                        )
                        if (locked) PixelIconImage(PixelIcon.LOCK, tint = DexColors.LedYellow, size = 24.dp)
                    }
                    Text(
                        (if (locked) "LOCKED" else part.options[i]).uppercase(),
                        style = PixelText.Tiny,
                        color = if (selected) DexColors.LedYellow else DexColors.TextMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }
    }
}

private fun pixels(px: IntArray): ImageBitmap =
    Bitmap.createBitmap(px, RoomRenderer.W, RoomRenderer.H, Bitmap.Config.ARGB_8888).asImageBitmap()
