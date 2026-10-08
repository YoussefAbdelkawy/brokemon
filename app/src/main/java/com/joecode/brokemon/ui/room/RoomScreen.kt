package com.joecode.brokemon.ui.room

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import com.joecode.brokemon.ui.components.AvatarBitmaps
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroRoom
import com.joecode.brokemon.data.model.RoomPart
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.domain.CheckOnBro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.ui.components.MediaThumbnail
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.ui.graphics.graphicsLayer
import com.joecode.brokemon.domain.HumanSprite
import com.joecode.brokemon.domain.RoomRenderer
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.rememberAudioPlayer
import com.joecode.brokemon.ui.navigation.LocalNavAnimatedScope
import com.joecode.brokemon.ui.navigation.sharedCard
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlinx.coroutines.delay

/** Inside the card: the bro hanging out in their own pixel room. */
@Composable
fun RoomScreen(
    broId: Long,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit,
    viewModel: RoomViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bro = state.bro
    // Zoom "through" the card on the way in.
    val zoom = LocalNavAnimatedScope.current?.run {
        Modifier.animateEnterExit(enter = scaleIn(tween(520), initialScale = 1.6f) + fadeIn(tween(320)))
    } ?: Modifier

    Box(
        Modifier
            .fillMaxSize()
            .sharedCard(broId)
            .background(DexColors.Background),
    ) {
        if (state.isLoading || bro == null) {
            if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = DexColors.DexRed)
            return@Box
        }
        Column(Modifier.fillMaxSize().then(zoom)) {
            RoomStage(bro, state.stage, Modifier.windowInsetsPadding(WindowInsets.statusBars), onBack)
            RoomControls(
                bro = bro,
                stage = state.stage,
                onOpenCard = { onOpenCard(bro.id) },
                onRoomChange = viewModel::setRoom,
                onHangOut = viewModel::hangOut,
            )
        }
    }
}

@Composable
private fun RoomStage(bro: Bro, stage: EvolutionStage, modifier: Modifier, onBack: () -> Unit) {
    val room = bro.resolvedRoom
    val place = RoomRenderer.placement(room)
    val roomBitmap = remember(room) { roomImage(room) }
    val broBitmap = remember(bro.resolvedLook, stage, bro.isShiny, place.sitting) {
        AvatarBitmaps.fullBody(bro.resolvedLook, stage.ordinal, bro.isShiny, place.sitting).asImageBitmap()
    }
    val audio = rememberAudioPlayer()
    val haptics = LocalHapticFeedback.current
    var bob by remember { mutableIntStateOf(0) }
    var hop by remember { mutableIntStateOf(0) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var bubbleKey by remember { mutableIntStateOf(0) }

    // Idle breathing: a one-pixel bob, the way old handheld sprites idle.
    LaunchedEffect(Unit) {
        while (true) {
            delay(520)
            bob = if (bob == 0) 1 else 0
        }
    }
    LaunchedEffect(bubbleKey) {
        if (bubbleKey == 0) return@LaunchedEffect
        hop = 3
        delay(120)
        hop = 1
        delay(100)
        hop = 0
        delay(2200)
        bubble = null
    }

    Box(modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(RoomRenderer.W / RoomRenderer.H.toFloat()),
        ) {
            val px = maxWidth / RoomRenderer.W
            Image(
                roomBitmap,
                contentDescription = "${bro.name}'s room",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                filterQuality = AvatarBitmaps.filterQuality,
            )
            Image(
                broBitmap,
                contentDescription = bro.name,
                filterQuality = AvatarBitmaps.filterQuality,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset { IntOffset((px * place.x).roundToPx(), (px * (place.y + bob - hop)).roundToPx()) }
                    .size(px * HumanSprite.BODY_W, px * HumanSprite.BODY_H)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        bubble = quip(bro)
                        bubbleKey++
                        bro.voiceLine?.let { audio.play(it) }
                    }
                    .semantics { contentDescription = "Tap ${bro.name} to say hi" },
            )
            bubble?.let { text ->
                Text(
                    text,
                    style = PixelText.Tiny,
                    color = Color(0xFF101014),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .offset(x = px * (place.x - 14), y = px * (place.y - 13))
                        .width(px * 60)
                        .background(Color.White, DexShape(6.dp))
                        .border(2.dp, Color(0xFF101014), DexShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                )
            }
        }
        // Back button + name plate floating over the room.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.width(8.dp))
            Text(
                "${bro.name.uppercase()}'S ROOM",
                style = PixelText.Label,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), DexShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RoomControls(
    bro: Bro,
    stage: EvolutionStage,
    onOpenCard: () -> Unit,
    onRoomChange: (BroRoom) -> Unit,
    onHangOut: ((CheckInResult) -> Unit) -> Unit,
) {
    var decorating by rememberSaveable { mutableStateOf(false) }
    var hangMessage by remember { mutableStateOf<String?>(null) }
    val checkedToday = CheckOnBro.checkedInToday(bro)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Tap ${bro.name} to say hi${if (bro.voiceLine != null) " (plays their voice line)" else ""}.",
            color = DexColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        RoomStats(bro, stage)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelButton(
                text = if (checkedToday) "Hung out" else "Hang out",
                onClick = {
                    onHangOut { result ->
                        hangMessage = when (result) {
                            CheckInResult.CHECKED_IN -> "You hung out with ${bro.name}. +${Evolution.CHECK_IN_POINTS} bond!"
                            else -> "You already hung out today. Come back tomorrow."
                        }
                    }
                },
                enabled = !checkedToday,
                stacked = true,
                modifier = Modifier.weight(1f),
                color = DexColors.LedGreen.copy(alpha = 0.75f),
                leading = { Icon(Icons.Filled.WavingHand, null, tint = DexColors.Text, modifier = Modifier.size(20.dp)) },
            )
            PixelButton(
                text = if (decorating) "Done" else "Decorate",
                onClick = { decorating = !decorating },
                stacked = true,
                modifier = Modifier.weight(1f),
                color = if (decorating) DexColors.LedGreen.copy(alpha = 0.8f) else DexColors.DexRed,
                leading = {
                    Icon(if (decorating) Icons.Filled.Check else Icons.Filled.Brush, null, tint = DexColors.Text, modifier = Modifier.size(18.dp))
                },
            )
            PixelButton(
                text = "Card",
                onClick = onOpenCard,
                stacked = true,
                modifier = Modifier.weight(1f),
                color = DexColors.SurfaceHigh,
                leading = { Icon(Icons.Filled.Style, null, tint = DexColors.Text, modifier = Modifier.size(18.dp)) },
            )
        }
        hangMessage?.let { Text(it, color = DexColors.LedGreen, style = MaterialTheme.typography.bodySmall) }
        AnimatedVisibility(decorating, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            RoomDecorator(bro.resolvedRoom, onRoomChange)
        }
        MemoryWall(bro)
    }
}

@Composable
private fun RoomDecorator(room: BroRoom, onRoomChange: (BroRoom) -> Unit) {
    var part by rememberSaveable { mutableStateOf(RoomPart.SEAT) }
    ScreenPanel(title = "Decorate") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(RoomPart.entries) { p ->
                val selected = p == part
                Text(
                    p.label.uppercase(),
                    style = PixelText.Tiny,
                    color = if (selected) Color(0xFF101014) else DexColors.Text,
                    modifier = Modifier
                        .background(if (selected) DexColors.LedYellow else DexColors.SurfaceHigh, DexShape(4.dp))
                        .clickable { part = p }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until part.count).toList()) { i ->
                val option = room.with(part, i)
                val preview = remember(option) { roomImage(option) }
                val selected = room[part] == i
                Column(
                    Modifier
                        .width(112.dp)
                        .clickable { onRoomChange(option) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        preview,
                        contentDescription = part.options[i],
                        filterQuality = AvatarBitmaps.filterQuality,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(RoomRenderer.W / RoomRenderer.H.toFloat())
                            .border(2.dp, if (selected) DexColors.LedYellow else DexColors.ScreenBorder, DexShape(3.dp)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        part.options[i].uppercase(),
                        style = PixelText.Tiny,
                        color = if (selected) DexColors.LedYellow else DexColors.TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The room as pixel art. */
private fun roomImage(room: com.joecode.brokemon.data.model.BroRoom): ImageBitmap =
    pixels(RoomRenderer.render(room), RoomRenderer.W, RoomRenderer.H)

private fun pixels(px: IntArray, w: Int, h: Int): ImageBitmap =
    Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()

/** What the bro says when you tap them. */
private fun quip(bro: Bro): String {
    val lines = bro.moves.map { "${it.uppercase()}!" } +
        listOf("YO!", "WHAT'S GOOD?", "MISSED ME?", "${bro.primaryType.label.uppercase()} MODE") +
        bro.facts.take(2).map { "${it.value.uppercase().take(18)}!" }
    return lines.random()
}

/** A little status strip, like a game's room HUD. */
@Composable
private fun RoomStats(bro: Bro, stage: EvolutionStage) {
    val days = CheckOnBro.daysSinceContact(bro)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "BOND" to stage.title.uppercase(),
            "MEMORIES" to bro.memories.size.toString(),
            "LAST HANG" to when (days) { 0L -> "TODAY"; 1L -> "1 DAY"; else -> "$days DAYS" },
        ).forEach { (label, value) ->
            Column(
                Modifier
                    .weight(1f)
                    .background(DexColors.Screen, DexShape(4.dp))
                    .border(1.dp, DexColors.ScreenBorder, DexShape(4.dp))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, style = PixelText.Label, color = DexColors.ScreenText, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(label, style = PixelText.Tiny, color = DexColors.TextMuted)
            }
        }
    }
}

/** Their photos pinned up like polaroids. */
@Composable
private fun MemoryWall(bro: Bro) {
    val photos = bro.memories.filter { it.mediaType == com.joecode.brokemon.data.model.MediaType.PHOTO }.sortedByDescending { it.date }
    ScreenPanel(title = "Memory wall") {
        if (photos.isEmpty()) {
            Text(
                "No photos yet. Add one from ${bro.name}'s card and it gets pinned up here.",
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos, key = { it.id }) { memory ->
                    val tilt = ((memory.id.hashCode() % 7) - 3).toFloat()
                    Column(
                        Modifier
                            .width(110.dp)
                            .graphicsLayer { rotationZ = tilt }
                            .background(Color(0xFFF5F2EA))
                            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 4.dp),
                    ) {
                        MediaThumbnail(memory.fileUri, memory.mediaType, Modifier.fillMaxWidth().aspectRatio(1f), maxPx = 256)
                        Text(
                            memory.caption.ifBlank { " " },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3A3A44),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
