package com.joecode.brokemon.ui.battle

import com.joecode.brokemon.ui.theme.Spacing
import com.joecode.brokemon.ui.components.PixelChip
import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import com.joecode.brokemon.ui.components.PixelAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.joecode.brokemon.battle.NearbyLink
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.battle.BattleAction
import com.joecode.brokemon.domain.battle.BattleEngine
import com.joecode.brokemon.domain.battle.BattleLines
import com.joecode.brokemon.domain.battle.BattleMath
import com.joecode.brokemon.share.QrBitmap
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.components.icon
import com.joecode.brokemon.ui.share.ShareImage
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val BATTLE_QR_PREFIX = "BRKBATTLE:"

@Composable
fun BattleScreen(
    onBack: () -> Unit,
    viewModel: BattleViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.phase) {
        BattlePhase.SETUP -> BattleSetup(state, viewModel, onBack)
        BattlePhase.LOBBY -> Lobby(state, viewModel)
        BattlePhase.BATTLE, BattlePhase.OVER -> Arena(state, viewModel, onBack)
    }
}

// --- Setup ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BattleSetup(state: BattleUiState, viewModel: BattleViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Bro?>(null) }
    var code by rememberSaveable { mutableStateOf("") }
    var nearbyAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) nearbyAction?.invoke() else permissionDenied = true
    }
    fun withNearbyPermissions(action: () -> Unit) {
        nearbyAction = action
        permissions.launch(NearbyLink.requiredPermissions)
    }
    val title = when (state.kind) {
        BattleKind.CPU -> "Practice vs CPU"
        BattleKind.LOCAL -> "Same-phone battle"
        BattleKind.HOST -> "Host a battle"
        BattleKind.JOIN -> "Join a battle"
        BattleKind.REPLAY -> "Replay"
    }
    val pool = state.bros.filter { b -> state.dexMembers?.let { b.id in it } ?: true }

    DexScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.tournamentLabel?.let {
                ScreenPanel(title = "Tournament match") {
                    Text(it, color = DexColors.Gold, style = MaterialTheme.typography.bodyMedium)
                    Text("Fair Mode is on for tournaments: everyone fights at Lv.${BattleMath.FAIR_LEVEL}.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.kind != BattleKind.JOIN) {
                ScreenPanel(title = "Rules") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FORMAT", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
                        listOf(1, 3).forEach { f ->
                            val on = state.format == f
                            Text(
                                "${f}V$f",
                                style = PixelText.Label,
                                color = if (on) DexColors.OnBright else DexColors.ScreenText,
                                modifier = Modifier
                                    .padding(start = Spacing.sm)
                                    .background(if (on) DexColors.ScreenText else Color.Transparent, DexShape(3.dp))
                                    .border(1.dp, DexColors.ScreenBorder, DexShape(3.dp))
                                    .clickable { viewModel.setFormat(f) }
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("FAIR MODE", style = PixelText.Tiny, color = DexColors.Text)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (state.fairMode) "Everyone fights at Lv.${BattleMath.FAIR_LEVEL}. Only team and moves matter."
                                else "Real friendship levels: long-time bros hit harder.",
                                color = DexColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = state.fairMode,
                            onCheckedChange = viewModel::setFair,
                            colors = SwitchDefaults.colors(checkedTrackColor = DexColors.LedGreen.copy(alpha = 0.6f)),
                        )
                    }
                }
            } else {
                Text("The host picks the format and Fair Mode. If it's 1v1, your first bro fights.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }

            if (state.kind == BattleKind.LOCAL) {
                Text(
                    if (state.pickingPlayer == 1) "PLAYER 1 (${state.myName.uppercase()}): PICK ${state.format}" else "PLAYER 2: PICK ${state.format}",
                    style = PixelText.Label,
                    color = DexColors.LedYellow,
                )
                if (state.pickingPlayer == 2) {
                    OutlinedTextField(
                        value = state.opponentName,
                        onValueChange = viewModel::setOpponentName,
                        label = { Text("Player 2 name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text("PICK YOUR TEAM (${state.currentPicks.size}/${state.format})", style = PixelText.Label, color = DexColors.LedYellow)
            }

            // Dex filter: battle with any regional dex (or everyone).
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item { FilterChipPixel("National", state.dexFilter == null) { viewModel.setDexFilter(null) } }
                items(state.dexes, key = { it.id }) { d -> FilterChipPixel(d.name, state.dexFilter == d.id) { viewModel.setDexFilter(d.id) } }
            }
            if (pool.isEmpty()) {
                Text("No bros here yet. Catch some first!", color = DexColors.TextMuted)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pool.forEach { bro ->
                    val order = state.currentPicks.indexOf(bro.id)
                    PickTile(bro, order, onClick = { viewModel.togglePick(bro.id) })
                }
            }
            val picked = state.currentPicks.mapNotNull { id -> state.bros.firstOrNull { it.id == id } }
            if (picked.isNotEmpty()) {
                Text("MOVES", style = PixelText.Tiny, color = DexColors.TextMuted)
                picked.forEach { bro ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${bro.name} · Lv.${BattleMath.level(Evolution.score(bro))}", color = DexColors.Text, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editing = bro }) { Text("EDIT MOVES", style = PixelText.Tiny, color = DexColors.LedBlue) }
                    }
                }
            }

            val ready = state.currentPicks.isNotEmpty()
            when (state.kind) {
                BattleKind.CPU -> PixelButton("Battle!", viewModel::startCpu, Modifier.fillMaxWidth(), enabled = ready)
                BattleKind.LOCAL -> if (state.pickingPlayer == 1) {
                    PixelButton("Next: Player 2 picks", viewModel::nextPicker, Modifier.fillMaxWidth(), enabled = ready)
                } else {
                    PixelButton("Battle!", viewModel::startLocal, Modifier.fillMaxWidth(), enabled = ready)
                    TextButton(onClick = viewModel::backToPicker1) { Text("BACK TO PLAYER 1", style = PixelText.Tiny, color = DexColors.TextMuted) }
                }
                BattleKind.HOST -> PixelButton("Open room", { withNearbyPermissions(viewModel::openRoom) }, Modifier.fillMaxWidth(), enabled = ready)
                BattleKind.JOIN -> {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(8) },
                        label = { Text("Room code") },
                        placeholder = { Text("K7Q2XM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        trailingIcon = {
                            IconButton(onClick = {
                                val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                                GmsBarcodeScanning.getClient(context, options).startScan().addOnSuccessListener { barcode ->
                                    barcode.rawValue?.removePrefix(BATTLE_QR_PREFIX)?.let { code = it }
                                }
                            }) { Icon(Icons.Filled.QrCodeScanner, "Scan the host's QR", tint = DexColors.LedBlue) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.lobby.error?.let { Text(it, color = DexColors.LedRed) }
                    PixelButton("Join", { withNearbyPermissions { viewModel.joinRoom(code) } }, Modifier.fillMaxWidth(), enabled = ready && code.isNotBlank())
                }
                BattleKind.REPLAY -> Unit
            }
            if (state.kind == BattleKind.HOST || state.kind == BattleKind.JOIN) {
                Text(
                    "Nearby battles go straight phone to phone over Bluetooth / Wi-Fi. No internet, no account. " +
                        "Only the battle card travels (name, look, types, level, 4 moves). Never photos, facts or memories.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    editing?.let { bro ->
        MovesSheet(bro, onSave = { eq, customs -> viewModel.saveLoadout(bro, eq, customs); editing = null }, onDismiss = { editing = null })
    }
    if (permissionDenied) {
        PixelAlertDialog(
            onDismissRequest = { permissionDenied = false },
            title = { Text("NEARBY NEEDS PERMISSION", style = PixelText.Label) },
            text = { Text("To find your friend's phone, Brokemon needs Nearby devices (Bluetooth / Wi-Fi) permission. It only uses it during the battle.") },
            confirmButton = { TextButton(onClick = { permissionDenied = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun FilterChipPixel(label: String, selected: Boolean, onClick: () -> Unit) {
    PixelChip(label, selected, onClick)
}

@Composable
private fun PickTile(bro: Bro, order: Int, onClick: () -> Unit) {
    val selected = order >= 0
    val shape = DexShape(6.dp)
    Column(
        Modifier
            .width(92.dp)
            .semantics(mergeDescendants = true) { contentDescription = bro.name + if (selected) ", picked ${order + 1}" else "" }
            .background(if (selected) bro.primaryType.color.copy(alpha = 0.2f) else DexColors.Surface, shape)
            .border(2.dp, if (selected) bro.primaryType.color else DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            BroSprite(bro, Evolution.info(bro).stage.ordinal, Modifier.size(64.dp))
            if (selected) {
                Text(
                    "${order + 1}",
                    style = PixelText.Tiny,
                    color = DexColors.OnBright,
                    modifier = Modifier.align(Alignment.TopEnd).background(DexColors.LedYellow, DexShape(2.dp)).padding(Spacing.xs),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(bro.name, style = PixelText.Tiny, color = DexColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("LV.${BattleMath.level(Evolution.score(bro))}", style = PixelText.Tiny, color = DexColors.TextMuted)
    }
}

// --- Lobby -----------------------------------------------------------------------------

@Composable
private fun Lobby(state: BattleUiState, viewModel: BattleViewModel) {
    val lobby = state.lobby
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    BackHandler(onBack = viewModel::leaveLobby)
    DexScaffold(title = if (state.kind == BattleKind.HOST) "Battle room" else "Joining", onBack = viewModel::leaveLobby) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("ROOM CODE", style = PixelText.Tiny, color = DexColors.TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = "Room code ${lobby.code.toList().joinToString(" ")}" }) {
                lobby.code.forEach { ch ->
                    Text(
                        "$ch",
                        style = PixelText.Title,
                        color = DexColors.LedYellow,
                        modifier = Modifier.background(DexColors.Screen, DexShape(Spacing.xs)).border(2.dp, DexColors.ScreenBorder, DexShape(Spacing.xs)).padding(10.dp),
                    )
                }
            }
            if (state.kind == BattleKind.HOST && !lobby.connected) {
                val qr = remember(lobby.code) {
                    QrBitmap.render(BATTLE_QR_PREFIX + lobby.code, Color(0xFF0B0B10).toArgb(), Color.White.toArgb()).asImageBitmap()
                }
                Image(
                    qr,
                    contentDescription = "QR code for room ${lobby.code}",
                    filterQuality = FilterQuality.None,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(180.dp).background(Color.White, DexShape(Spacing.xs)).padding(6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelButton(
                        "Copy",
                        { clipboard.setText(AnnotatedString(lobby.code)) },
                        color = DexColors.SurfaceHigh,
                        leading = { Icon(Icons.Filled.ContentCopy, null, tint = DexColors.Text, modifier = Modifier.size(16.dp)) },
                    )
                    PixelButton(
                        "Share",
                        {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Bro battle? Open Brokemon → Battle → Join and enter ${lobby.code} (we need to be next to each other).")
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, "Send the code")) }
                        },
                        color = DexColors.LedBlue.copy(alpha = 0.85f),
                        leading = { Icon(Icons.Filled.Share, null, tint = DexColors.Text, modifier = Modifier.size(16.dp)) },
                    )
                }
                Text("Codes expire after 10 minutes.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            ScreenPanel(title = "Status", modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!lobby.connected) CircularProgressIndicator(Modifier.size(18.dp), color = DexColors.LedBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            lobby.connected && lobby.opponentName != null -> "Connected to ${lobby.opponentName}!"
                            lobby.connected -> "Connected!"
                            state.kind == BattleKind.HOST -> "Waiting for your friend to join..."
                            else -> "Looking for room ${lobby.code} nearby..."
                        },
                        color = DexColors.ScreenText,
                    )
                }
                lobby.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = DexColors.LedRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            lobby.opponentTeam?.let { team ->
                Text("${(lobby.opponentName ?: "THEIR").uppercase()}'S TEAM", style = PixelText.Tiny, color = DexColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    team.forEach { f ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BroSprite(com.joecode.brokemon.data.model.BroLook.fromList(f.look), 0, f.shiny, Modifier.size(64.dp))
                            Text(f.name, style = PixelText.Tiny, color = DexColors.Text, maxLines = 1)
                        }
                    }
                }
            }
            if (lobby.connected) {
                PixelButton(
                    if (lobby.meReady) (if (lobby.themReady) "Starting..." else "Waiting for them...") else "Ready!",
                    viewModel::setReady,
                    Modifier.fillMaxWidth(),
                    enabled = !lobby.meReady && lobby.opponentTeam != null,
                )
                if (lobby.themReady && !lobby.meReady) Text("They're ready!", color = DexColors.LedGreen)
            }
            TextButton(onClick = viewModel::leaveLobby) { Text("LEAVE", style = PixelText.Tiny, color = DexColors.TextMuted) }
        }
    }
}

// --- Arena -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Arena(state: BattleUiState, viewModel: BattleViewModel, onBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var stepIndex by remember(state.steps) { mutableIntStateOf(0) }
    val step = state.steps.getOrNull(stepIndex)
    val shown = step?.shown ?: state.shown
    val shake = remember { Animatable(0f) }
    val flash = remember { Array(2) { Animatable(0f) } }
    var showTeam by remember { mutableStateOf(false) }
    var confirmForfeit by remember { mutableStateOf(false) }
    val bottomSide = if (state.kind == BattleKind.LOCAL) 0 else state.mySide
    val topSide = 1 - bottomSide
    val charDelay = when (state.textSpeed) { 0 -> 40L; 2 -> 8L; else -> 22L }

    BackHandler { if (state.phase == BattlePhase.OVER || state.kind == BattleKind.REPLAY) onBack() else confirmForfeit = true }

    // Effects for each step, then advance.
    LaunchedEffect(state.steps, stepIndex) {
        val s = step ?: run { if (state.steps.isNotEmpty()) viewModel.onPlaybackDone(); return@LaunchedEffect }
        when (s.effect) {
            EffectKind.BIG_HIT -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                launch { flash[s.effectSide].snapTo(1f); flash[s.effectSide].animateTo(0f, tween(400)) }
                for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(40))
            }
            EffectKind.HIT -> launch { flash[s.effectSide].snapTo(0.7f); flash[s.effectSide].animateTo(0f, tween(300)) }
            EffectKind.FAINT -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> Unit
        }
        delay(s.text.length * charDelay + 650L)
        stepIndex++
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1F2A44), Color(0xFF243B2E), Color(0xFF16241B)))),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (state.phase == BattlePhase.OVER || state.kind == BattleKind.REPLAY) onBack() else confirmForfeit = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Leave battle", tint = DexColors.Text)
                }
                Text(
                    (if (state.kind == BattleKind.REPLAY) "REPLAY · " else "") + "TURN ${(state.battle?.turn ?: 0) + 1}" +
                        if (state.fairMode) " · FAIR MODE" else "",
                    style = PixelText.Tiny,
                    color = DexColors.TextMuted,
                )
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().graphicsLayer { translationX = shake.value * density }) {
                if (shown.size == 2) {
                    // Opponent: top right.
                    FighterInfo(shown[topSide], Modifier.align(Alignment.TopStart).padding(start = Spacing.md, top = Spacing.sm))
                    val foe = minOf(maxWidth * 0.5f, maxHeight * 0.42f)
                    val mine = minOf(maxWidth * 0.55f, maxHeight * 0.5f)
                    FighterSprite(
                        shown[topSide],
                        flash[topSide].value,
                        Modifier.align(Alignment.TopEnd).padding(end = Spacing.md, top = 40.dp).size(foe),
                    )
                    // You: bottom left.
                    FighterSprite(
                        shown[bottomSide],
                        flash[bottomSide].value,
                        Modifier.align(Alignment.BottomStart).padding(start = Spacing.sm, bottom = Spacing.sm).size(mine),
                        mirror = true,
                    )
                    FighterInfo(shown[bottomSide], Modifier.align(Alignment.BottomEnd).padding(end = Spacing.md, bottom = Spacing.xl), showHpNumbers = true)
                    // Emote bubbles.
                    state.emotes.forEach { e ->
                        val top = if (e.side == topSide) Alignment.TopEnd else Alignment.BottomStart
                        Text(
                            e.text,
                            style = PixelText.Label,
                            color = DexColors.Ink,
                            modifier = Modifier
                                .align(top)
                                .padding(if (e.side == topSide) 40.dp else 40.dp, if (e.side == topSide) 36.dp else 190.dp)
                                .background(DexColors.Paper, DexShape(6.dp))
                                .padding(Spacing.sm),
                        )
                    }
                }
            }
            // Text box + commands.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .background(DexColors.Paper, DexShape(6.dp))
                    .border(4.dp, DexColors.Ink, DexShape(6.dp))
                    .padding(Spacing.md)
                    .heightIn(min = 96.dp),
            ) {
                when {
                    step != null -> Typewriter(step.text, charDelay)
                    state.phase == BattlePhase.OVER -> Text("Battle over.", color = DexColors.Ink, style = PixelText.Label)
                    state.awaiting != null -> Commands(state, viewModel, onSwitch = { showTeam = true }, onForfeit = { confirmForfeit = true })
                    state.waitingForOpponent -> Text("Waiting for ${state.battle?.sides?.get(1 - state.mySide)?.name ?: "them"}...", color = DexColors.Ink, style = PixelText.Label)
                    else -> Text("...", color = DexColors.Ink, style = PixelText.Label)
                }
            }
            if (state.kind == BattleKind.CPU || state.kind == BattleKind.HOST || state.kind == BattleKind.JOIN) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BattleLines.emotes.forEach { e ->
                        Text(
                            e,
                            style = PixelText.Tiny,
                            color = DexColors.Text,
                            modifier = Modifier
                                .background(DexColors.Surface, DexShape(4.dp))
                                .border(1.dp, DexColors.Outline, DexShape(4.dp))
                                .clickable(onClickLabel = "Send $e") { viewModel.emote(state.mySide, e) }
                                .padding(horizontal = 10.dp, vertical = Spacing.sm),
                        )
                    }
                }
            }
        }

        state.lobby.reconnectSecondsLeft?.let { secs ->
            Overlay {
                Text("CONNECTION LOST", style = PixelText.Header, color = DexColors.LedYellow)
                Spacer(Modifier.height(10.dp))
                Text("Waiting $secs s for them to come back. If they don't, you win.", color = DexColors.Text, textAlign = TextAlign.Center)
            }
        }
        state.passTo?.let { side ->
            val name = state.battle?.sides?.get(side)?.name ?: "Player ${side + 1}"
            Overlay(solid = true) {
                Text("PASS THE PHONE", style = PixelText.Header, color = DexColors.LedYellow)
                Spacer(Modifier.height(10.dp))
                Text("Hand it to $name. No peeking at their moves!", color = DexColors.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PixelButton("I'm $name", viewModel::onPassAccepted)
            }
        }
        if (state.phase == BattlePhase.OVER && state.steps.isEmpty()) {
            state.result?.let { result ->
                EndScreen(
                    state = state,
                    result = result,
                    onShare = {
                        state.battle?.let { b ->
                            ShareImage.share(context, BattleRecapImage.render(context, b, result), "brokemon-battle", "Share the battle")
                        }
                    },
                    onRematch = viewModel::rematch,
                    onDone = onBack,
                )
            }
        }
    }

    if (showTeam) {
        val battle = state.battle
        val side = state.awaiting
        if (battle != null && side != null) {
            PixelAlertDialog(
                onDismissRequest = { showTeam = false },
                title = { Text("SWITCH", style = PixelText.Label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        battle.sides[side].fighters.forEachIndexed { i, f ->
                            val legal = BattleAction.Switch(i) in BattleEngine.legalActions(battle, side)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = legal) { showTeam = false; viewModel.choose(BattleAction.Switch(i)) }
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BroSprite(com.joecode.brokemon.data.model.BroLook.fromList(f.snap.look), 0, f.snap.shiny, Modifier.size(40.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${f.snap.name}  ${f.hp}/${f.stats.hp}" + when {
                                        f.fainted -> " (AFK)"
                                        i == battle.sides[side].active -> " (in battle)"
                                        else -> ""
                                    },
                                    color = if (legal) DexColors.Text else DexColors.TextMuted,
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTeam = false }) { Text("Cancel") } },
            )
        }
    }
    if (confirmForfeit) {
        PixelAlertDialog(
            onDismissRequest = { confirmForfeit = false },
            title = { Text("FORFEIT?", style = PixelText.Label) },
            text = { Text("Leaving now counts as a loss.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForfeit = false
                    if (state.awaiting != null) viewModel.forfeit() else onBack()
                }) { Text("Forfeit", color = DexColors.LedRed) }
            },
            dismissButton = { TextButton(onClick = { confirmForfeit = false }) { Text("Keep fighting") } },
        )
    }
}

@Composable
private fun Typewriter(text: String, charDelay: Long) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (shown < text.length) {
            delay(charDelay)
            shown++
        }
    }
    Text(
        text.take(shown),
        color = DexColors.Ink,
        style = PixelText.Label.copy(lineHeight = PixelText.Header.lineHeight),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = text },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Commands(state: BattleUiState, viewModel: BattleViewModel, onSwitch: () -> Unit, onForfeit: () -> Unit) {
    val battle = state.battle ?: return
    val side = state.awaiting ?: return
    val fighter = battle.sides[side].current
    val legal = BattleEngine.legalActions(battle, side)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("WHAT WILL ${fighter.snap.name.uppercase()} DO?", style = PixelText.Tiny, color = DexColors.Ink, modifier = Modifier.weight(1f))
            Text("${state.timerLeft}s", style = PixelText.Tiny, color = if (state.timerLeft <= 10) DexColors.DexRed else Color(0xFF55555F))
        }
        SegmentedBar(state.timerLeft / TURN_SECONDS.toFloat(), if (state.timerLeft <= 10) DexColors.DexRed else DexColors.LedBlue, Modifier.fillMaxWidth().height(4.dp), segments = 30)
        val panic = BattleAction.Move(-1) in legal
        if (panic) {
            Text("Out of energy! Only a Panic Text left.", color = DexColors.Ink, style = MaterialTheme.typography.bodySmall)
            PixelButton("Panic Text", { viewModel.choose(BattleAction.Move(-1)) }, Modifier.fillMaxWidth())
        } else {
            fighter.moves.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    pair.forEachIndexed { col, move ->
                        val slot = row * 2 + col
                        val ok = BattleAction.Move(slot) in legal
                        val color = move.type?.color ?: Color(0xFF55555F)
                        Row(
                            Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${move.name}, ${move.type.battleLabel} type, ${fighter.energy[slot]} of ${move.energy} energy left" +
                                        if (!ok) ", can't use right now" else ""
                                }
                                .background(if (ok) Color.White else Color(0xFFE2DED0), DexShape(4.dp))
                                .border(2.dp, if (ok) color else Color(0xFFB0ACA0), DexShape(4.dp))
                                .clickable(enabled = ok) { viewModel.choose(BattleAction.Move(slot)) }
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(move.type.icon, null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(move.name.uppercase(), style = PixelText.Tiny, color = DexColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text("${fighter.energy[slot]}/${move.energy}", style = PixelText.Tiny, color = Color(0xFF55555F))
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val canSwitch = legal.any { it is BattleAction.Switch }
            TextButton(onClick = onSwitch, enabled = canSwitch) {
                Icon(Icons.Filled.SwapHoriz, null, tint = if (canSwitch) DexColors.LedBlue else Color(0xFFB0ACA0))
                Spacer(Modifier.width(4.dp))
                Text("SWITCH", style = PixelText.Tiny, color = if (canSwitch) DexColors.LedBlue else Color(0xFFB0ACA0))
            }
            TextButton(onClick = onForfeit) {
                Icon(Icons.Filled.Flag, null, tint = DexColors.DexRed)
                Spacer(Modifier.width(4.dp))
                Text("FORFEIT", style = PixelText.Tiny, color = DexColors.DexRed)
            }
        }
    }
}

@Composable
private fun FighterInfo(f: ShownFighter, modifier: Modifier, showHpNumbers: Boolean = false) {
    val hp by animateFloatAsState(f.hp / f.maxHp.toFloat().coerceAtLeast(1f), tween(500), label = "hp")
    val barColor = when {
        hp > 0.5f -> DexColors.LedGreen
        hp > 0.2f -> DexColors.LedYellow
        else -> DexColors.LedRed
    }
    Column(
        modifier
            .width(196.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${f.name}, level ${f.level}, ${f.hp} of ${f.maxHp} HP" }
            .background(DexColors.Paper, DexShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 2.dp, bottomEnd = 12.dp))
            .border(3.dp, DexColors.Ink, DexShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 2.dp, bottomEnd = 12.dp))
            .padding(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.name.uppercase(), style = PixelText.Tiny, color = DexColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("LV.${f.level}", style = PixelText.Tiny, color = DexColors.Ink)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            f.types.forEach { t -> Icon(t.icon, t.label, tint = t.color, modifier = Modifier.size(14.dp).padding(end = 2.dp)) }
            Spacer(Modifier.width(4.dp))
            Text("HP", style = PixelText.Tiny, color = DexColors.DexRed)
            Spacer(Modifier.width(4.dp))
            SegmentedBar(hp, barColor, Modifier.weight(1f).height(8.dp), segments = 16)
        }
        if (showHpNumbers) Text("${f.hp}/${f.maxHp}", style = PixelText.Tiny, color = DexColors.Ink, modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
        Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(f.teamSize) { i ->
                Box(Modifier.size(7.dp).background(if (i < f.teamLeft) DexColors.DexRed else Color(0xFFB0ACA0), DexShape(1.dp)))
            }
            f.statuses.forEach { s ->
                Text(s.label.uppercase(), style = PixelText.Tiny, color = DexColors.Epic)
            }
        }
    }
}

@Composable
private fun FighterSprite(f: ShownFighter, flash: Float, modifier: Modifier, mirror: Boolean = false) {
    // Fainted bros "go AFK": they sink a little and fade out. Nothing violent.
    val afk by animateFloatAsState(if (f.fainted) 1f else 0f, tween(700), label = "afk")
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            drawOval(Color(0xFF2F5A3A), Offset.Zero, size)
            drawOval(Color(0xFF3F7A4B), Offset(size.width * 0.06f, size.height * 0.08f), Size(size.width * 0.88f, size.height * 0.6f))
        }
        Box(
            Modifier
                .fillMaxSize(0.92f)
                .padding(bottom = 14.dp)
                .graphicsLayer {
                    scaleX = if (mirror) -1f else 1f
                    alpha = 1f - afk
                    translationY = afk * 30f * density
                },
        ) {
            BroSprite(f.look, 0, f.shiny, Modifier.fillMaxSize())
            if (flash > 0f) BroSprite(f.look, 0, f.shiny, Modifier.fillMaxSize().graphicsLayer { alpha = flash }, tint = Color.White)
            if (f.shiny) Sparkles(Modifier.fillMaxSize(), count = 5, seed = f.name.hashCode())
        }
        if (f.fainted) Text("AFK", style = PixelText.Label, color = DexColors.TextMuted, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun Overlay(solid: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(if (solid) Color(0xFF0B0B10) else Color.Black.copy(alpha = 0.8f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun EndScreen(state: BattleUiState, result: BattleResult, onShare: () -> Unit, onRematch: () -> Unit, onDone: () -> Unit) {
    Overlay {
        val title = when (result.youWon) {
            true -> "YOU WIN!"
            false -> "YOU LOST"
            null -> if (state.battle?.winner == -1) "DRAW" else "${result.winnerName.uppercase()} WINS!"
        }
        Text(title, style = PixelText.Title, color = if (result.youWon == false) DexColors.TextMuted else DexColors.Gold)
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            Sparkles(Modifier.size(150.dp), count = 7, seed = 3)
            BroSprite(com.joecode.brokemon.data.model.BroLook.fromList(result.mvp.look), 0, result.mvp.shiny, Modifier.size(120.dp))
        }
        Text("MVP: ${result.mvp.name.uppercase()}", style = PixelText.Label, color = DexColors.Text)
        Text("${result.mvpDamage} damage · ${result.turns} turns", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        result.bonusFor?.let {
            Spacer(Modifier.height(8.dp))
            Text("+1 bond for $it (battle bonus, once a day)", color = DexColors.LedGreen, style = MaterialTheme.typography.bodySmall)
        }
        if (result.dailyQuestDone) Text("Daily battle quest complete! +60 XP", color = DexColors.LedYellow, style = MaterialTheme.typography.bodySmall)
        result.champion?.let { Text("🏆 $it wins the tournament!", color = DexColors.Gold, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 420.dp)) {
            PixelButton("Share", onShare, Modifier.weight(1f), color = DexColors.LedBlue.copy(alpha = 0.85f))
            if (state.kind != BattleKind.REPLAY && state.tournamentLabel == null) PixelButton("Rematch", onRematch, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        PixelButton("Done", onDone, Modifier.widthIn(max = 420.dp).fillMaxWidth(), color = DexColors.SurfaceHigh)
    }
}
