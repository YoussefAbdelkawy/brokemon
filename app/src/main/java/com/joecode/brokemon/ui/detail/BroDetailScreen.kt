package com.joecode.brokemon.ui.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.Fact
import com.joecode.brokemon.data.model.FactCategories
import com.joecode.brokemon.data.model.MediaType
import com.joecode.brokemon.data.model.Memory
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.Birthdays
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.EvolutionInfo
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.share.ShareImage
import com.joecode.brokemon.ui.share.StoryImages
import com.joecode.brokemon.ui.share.StoryPreviewDialog
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.domain.SeasonEvents
import com.joecode.brokemon.ui.components.AudioPlayButton
import com.joecode.brokemon.ui.components.AudioPlayerState
import com.joecode.brokemon.ui.components.AvatarBuilder
import com.joecode.brokemon.ui.components.PixelWaveform
import com.joecode.brokemon.ui.components.TiltState
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.dragToTilt
import com.joecode.brokemon.ui.components.rememberTiltState
import com.joecode.brokemon.data.CheckInResult
import com.joecode.brokemon.domain.CheckOnBro
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.foundation.layout.widthIn
import com.joecode.brokemon.ui.components.VoiceRecorderDialog
import com.joecode.brokemon.ui.components.rememberAudioPlayer
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.MediaThumbnail
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.RarityStars
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.components.StatBar
import com.joecode.brokemon.ui.components.TypeBadge
import com.joecode.brokemon.ui.components.rarityGlow
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.joecode.brokemon.ui.components.ArrowSide
import com.joecode.brokemon.ui.components.CoachMark
import com.joecode.brokemon.ui.components.Hints
import com.joecode.brokemon.ui.components.StatHexagon
import com.joecode.brokemon.ui.components.rememberHintDismisser
import com.joecode.brokemon.ui.components.rememberSeenHints

@Composable
fun BroDetailScreen(
    onBack: () -> Unit,
    onShare: (Long) -> Unit,
    onVisitRoom: (Long) -> Unit = {},
    viewModel: BroDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val evolutionEvent by viewModel.evolutionEvent.collectAsStateWithLifecycle()
    val captionTarget by viewModel.captionTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDexEntry by rememberSaveable { mutableStateOf(false) }
    val seenHints = rememberSeenHints()
    val dismissHint = rememberHintDismisser()
    var showRarity by rememberSaveable { mutableStateOf(false) }
    var showLookEditor by rememberSaveable { mutableStateOf(false) }
    var showStory by rememberSaveable { mutableStateOf(false) }
    var recordingMemory by rememberSaveable { mutableStateOf(false) }
    var recordingVoiceLine by rememberSaveable { mutableStateOf(false) }
    val audio = rememberAudioPlayer()
    val tilt = rememberTiltState()
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showAddFact by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Memory?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onCaptureResult(it)
    }
    val captureVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) {
        viewModel.onCaptureResult(it)
    }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            viewModel.onPicked(uri, isVideo)
        }
    }
    fun launchCamera(type: MediaType) {
        val uri = viewModel.prepareCapture(type)
        try {
            if (type == MediaType.PHOTO) takePicture.launch(uri) else captureVideo.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.onCaptureResult(false)
            scope.launch { snackbar.showSnackbar("No camera app found on this device.") }
        }
    }

    val bro = state.bro
    Box(Modifier.fillMaxSize()) {
        DexScaffold(
            title = bro?.let { "${it.dexNumber} ${it.name}" } ?: "Bro",
            onBack = onBack,
            snackbarHostState = snackbar,
            actions = {
                if (bro != null) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = DexColors.Text)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Visit their room") },
                            leadingIcon = { Icon(Icons.Filled.Weekend, null) },
                            onClick = { menuOpen = false; onVisitRoom(bro.id) },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menuOpen = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit dex entry") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                            onClick = { menuOpen = false; showDexEntry = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit look") },
                            leadingIcon = { Icon(Icons.Filled.Face, null) },
                            onClick = { menuOpen = false; showLookEditor = true },
                        )
                        DropdownMenuItem(
                            text = { Text(if (bro.voiceLine == null) "Record voice line" else "Re-record voice line") },
                            leadingIcon = { Icon(Icons.Filled.RecordVoiceOver, null) },
                            onClick = { menuOpen = false; recordingVoiceLine = true },
                        )
                        if (bro.voiceLine != null) {
                            DropdownMenuItem(
                                text = { Text("Delete voice line") },
                                leadingIcon = { Icon(Icons.Filled.VoiceOverOff, null) },
                                onClick = { menuOpen = false; audio.stop(); viewModel.setVoiceLine(null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Change rarity") },
                            leadingIcon = { Icon(Icons.Filled.Star, null) },
                            onClick = { menuOpen = false; showRarity = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Release bro", color = DexColors.LedRed) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = DexColors.LedRed) },
                            onClick = { menuOpen = false; showDelete = true },
                        )
                    }
                }
            },
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator(color = DexColors.DexRed)
                }
                bro == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text("This bro has left the dex.", color = DexColors.TextMuted)
                }
                else -> LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        CardShowcase(
                            bro = bro,
                            evolution = state.evolution!!,
                            tilt = tilt,
                            onTap = { bro.voiceLine?.let { audio.toggle(it) } },
                        )
                    }
                    item {
                        CoachMark(
                            id = Hints.DETAIL_CHECK_IN,
                            text = "Talked to ${bro.name} today? Tap here!",
                            visible = seenHints != null && Hints.DETAIL_CHECK_IN !in seenHints && !CheckOnBro.checkedInToday(bro),
                            arrow = ArrowSide.BOTTOM,
                            arrowBias = 1f / 6f,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        ActionRow(
                            bro = bro,
                            onCheckIn = {
                                dismissHint(Hints.DETAIL_CHECK_IN)
                                viewModel.checkIn { result ->
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            when (result) {
                                                CheckInResult.CHECKED_IN -> "Checked in with ${bro.name}! +${Evolution.CHECK_IN_POINTS} pts"
                                                else -> "Already checked in with ${bro.name} today. Come back tomorrow!"
                                            },
                                        )
                                    }
                                }
                            },
                            onShareQr = { onShare(bro.id) },
                            onShareStory = { showStory = true },
                        )
                    }
                    item { EvolutionPanel(bro, state.evolution!!) }
                    item { StatsPanel(bro) }
                    item { MovesPanel(bro) }
                    item {
                        MemoryPanel(
                            memories = bro.memories,
                            onPhoto = { launchCamera(MediaType.PHOTO) },
                            onVideo = { launchCamera(MediaType.VIDEO) },
                            onVoice = { recordingMemory = true },
                            onPick = {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                            },
                            onOpen = { viewing = it },
                        )
                    }
                    item { FactsPanel(bro.facts, onAdd = { showAddFact = true }, onRemove = viewModel::removeFact) }
                    item {
                        InfoPanel(
                            bro = bro,
                            onPickMeetDate = { showDatePicker = true },
                            onTradeableChanged = viewModel::setTradeable,
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        if (bro != null) {
            evolutionEvent?.let { EvolutionOverlay(bro, it, viewModel::onEvolutionFinished) }
        }
    }

    // --- Dialogs ---------------------------------------------------------------
    if (bro != null) {
        captionTarget?.let { memory ->
            TextInputDialog(
                title = "Add a caption",
                label = "What happened here?",
                initial = "",
                confirm = "Save",
                dismiss = "Skip",
                onConfirm = { viewModel.setCaption(memory, it) },
                onDismiss = viewModel::dismissCaption,
            )
        }
        if (showRename) {
            TextInputDialog(
                title = "Rename bro",
                label = "Name",
                initial = bro.name,
                confirm = "Save",
                onConfirm = { viewModel.rename(it); showRename = false },
                onDismiss = { showRename = false },
            )
        }
        if (showDexEntry) {
            TextInputDialog(
                title = "Dex entry",
                label = "One line about ${bro.name}",
                initial = bro.flavorText,
                confirm = "Save",
                maxLength = Bro.MAX_FLAVOR,
                placeholder = "Has never once replied in under 3 hours.",
                onConfirm = { viewModel.setFlavorText(it); showDexEntry = false },
                onDismiss = { showDexEntry = false },
            )
        }
        if (showRarity) {
            AlertDialog(
                onDismissRequest = { showRarity = false },
                title = { Text("RARITY", style = PixelText.Header) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Rarity.entries.forEach { r ->
                            FilterChip(
                                selected = bro.rarity == r,
                                onClick = { viewModel.setRarity(r); showRarity = false },
                                label = { Text(r.label) },
                                leadingIcon = { RarityStars(r) },
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showRarity = false }) { Text("Close") } },
            )
        }
        if (showStory) {
            val stage = state.evolution?.stage ?: EvolutionStage.ROOKIE
            StoryPreviewDialog(
                title = "Share ${bro.name}",
                render = { StoryImages(context).card(bro, stage) },
                onShare = { ShareImage.share(context, it, "brokemon-${bro.dexNumber.drop(1)}", "Share ${bro.name}'s card") },
                onDismiss = { showStory = false },
            )
        }
        if (showLookEditor) {
            LookEditorDialog(
                initial = bro.resolvedLook,
                shiny = bro.isShiny,
                stage = state.evolution?.stage?.ordinal ?: 0,
                onSave = { viewModel.setLook(it); showLookEditor = false },
                onDismiss = { showLookEditor = false },
            )
        }
        if (showDelete) {
            AlertDialog(
                onDismissRequest = { showDelete = false },
                title = { Text("RELEASE ${bro.name.uppercase()}?", style = PixelText.Label) },
                text = { Text("This removes the card, all of its memories (photos and videos) and facts from this device. It can't be undone.") },
                confirmButton = {
                    TextButton(onClick = { showDelete = false; viewModel.deleteBro(onBack) }) {
                        Text("Release", color = DexColors.LedRed)
                    }
                },
                dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep") } },
            )
        }
        if (showAddFact) {
            AddFactDialog(
                onConfirm = { c, v, monthDay ->
                    viewModel.addFact(c, v, monthDay)
                    showAddFact = false
                    // Natural moment to ask: they just told us a date they want to remember.
                    if (monthDay != null && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showAddFact = false },
            )
        }
        if (showDatePicker) {
            MeetDateDialog(
                initial = bro.realMeetDate,
                onConfirm = { viewModel.setRealMeetDate(it); showDatePicker = false },
                onDismiss = { showDatePicker = false },
            )
        }
        if (recordingMemory) {
            VoiceRecorderDialog(
                title = "Voice memory",
                hint = "Record up to a minute, or pick a voice note they sent you from your phone.",
                maxSeconds = 60,
                importMaxSeconds = 600,
                newFile = { viewModel.newVoiceFile() },
                onSaved = viewModel::addVoiceMemory,
                onDismiss = { recordingMemory = false },
            )
        }
        if (recordingVoiceLine) {
            VoiceRecorderDialog(
                title = "${bro.name}'s voice line",
                hint = "Their signature line, catchphrase or laugh, up to 10 seconds (or a clip up to 15s from your phone). Tap their card to play it.",
                maxSeconds = 10,
                importMaxSeconds = 15,
                newFile = { viewModel.newVoiceFile("voice-") },
                onSaved = { viewModel.setVoiceLine(it) },
                onDismiss = { recordingVoiceLine = false },
            )
        }
        viewing?.let { memory ->
            MemoryViewer(
                memory = memory,
                audio = audio,
                onDelete = { viewModel.deleteMemory(memory); viewing = null },
                onDismiss = { viewing = null },
            )
        }
    }
}

/**
 * The card itself, big and centered, like holding it in Pokémon TCG Pocket:
 * press and drag to tilt it in 3D (the foil follows your finger), release to
 * let it spring back. Tap plays their voice line if they have one.
 */
@Composable
private fun CardShowcase(bro: Bro, evolution: EvolutionInfo, tilt: TiltState, onTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(vertical = 8.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.82f)
                .dragToTilt(tilt, scope),
        ) {
            BroCard(
                bro = bro,
                stage = evolution.stage,
                tilt = tilt,
                tiltDegrees = 16f,
                holoFloor = 0.14f,
                showDexEntry = true,
                onClick = onTap,
            )
        }
        Text(
            if (bro.voiceLine != null) "HOLD & DRAG TO TILT · TAP TO HEAR THEM" else "HOLD & DRAG THE CARD TO TILT IT",
            style = PixelText.Tiny,
            color = DexColors.Outline,
        )
    }
}

@Composable
private fun ActionRow(bro: Bro, onCheckIn: () -> Unit, onShareQr: () -> Unit, onShareStory: () -> Unit) {
    val checkedToday = CheckOnBro.checkedInToday(bro)
    // Three equal buttons: same size, icon over label.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelButton(
            text = if (checkedToday) "Done today" else "Check in",
            onClick = onCheckIn,
            enabled = !checkedToday,
            stacked = true,
            modifier = Modifier.weight(1f),
            color = DexColors.SurfaceHigh,
            leading = {
                Icon(
                    if (checkedToday) Icons.Filled.CheckCircle else Icons.Filled.WavingHand,
                    null,
                    tint = if (checkedToday) DexColors.LedGreen else DexColors.LedYellow,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        PixelButton(
            text = "QR trade",
            onClick = onShareQr,
            enabled = bro.isTradeable,
            stacked = true,
            modifier = Modifier.weight(1f),
            leading = {
                Icon(
                    if (bro.isTradeable) Icons.Filled.QrCode2 else Icons.Filled.Lock,
                    null,
                    tint = DexColors.Text,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        PixelButton(
            text = "Post",
            onClick = onShareStory,
            enabled = bro.isTradeable,
            stacked = true,
            modifier = Modifier.weight(1f),
            color = DexColors.LedBlue.copy(alpha = 0.8f),
            leading = { Icon(Icons.Filled.IosShare, null, tint = DexColors.Text, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun EvolutionPanel(bro: Bro, evolution: EvolutionInfo) {
    val progress by animateFloatAsState(evolution.progress, tween(900), label = "evo")
    ScreenPanel(title = "Bond level") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(evolution.stage.title.uppercase(), style = PixelText.Header, color = DexColors.ScreenText)
            Spacer(Modifier.weight(1f))
            Text("${evolution.score} PTS", style = PixelText.Label, color = DexColors.LedYellow)
        }
        Spacer(Modifier.height(10.dp))
        SegmentedBar(progress, DexColors.LedYellow, Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            evolution.pointsToNext?.let { "$it pts until ${evolution.stage.next!!.title}" } ?: "Max bond reached. Legendary friendship.",
            color = DexColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        val months = Evolution.monthsKnown(bro.catchDate)
        ScoreLine("Memories", bro.memories.size, Evolution.MEMORY_POINTS)
        ScoreLine("Check-ins", bro.checkInCount, Evolution.CHECK_IN_POINTS)
        ScoreLine("Facts (max ${Evolution.FACTS_CAP})", minOf(bro.facts.size, Evolution.FACTS_CAP), Evolution.FACT_POINTS)
        ScoreLine("Months", minOf(months, Evolution.MONTHS_CAP), Evolution.MONTH_POINTS)
    }
}

@Composable
private fun ScoreLine(label: String, count: Int, per: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
        Text("$count x$per", style = PixelText.Tiny, color = DexColors.Text)
        Text("= ${count * per}".padStart(6), style = PixelText.Tiny, color = DexColors.ScreenText, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun StatsPanel(bro: Bro) {
    var asBars by rememberSaveable { mutableStateOf(false) }
    ScreenPanel(title = "Base stats") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(false to "HEX", true to "BARS").forEach { (bars, label) ->
                    val selected = asBars == bars
                    Text(
                        label,
                        style = PixelText.Tiny,
                        color = if (selected) Color(0xFF101014) else DexColors.TextMuted,
                        modifier = Modifier
                            .background(if (selected) DexColors.ScreenText else Color.Transparent, CutCornerShape(3.dp))
                            .border(1.dp, DexColors.ScreenBorder, CutCornerShape(3.dp))
                            .clickable(role = Role.Tab) { asBars = bars }
                            .semantics { this.selected = selected }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
            if (asBars) {
                bro.stats.asList().forEach { (info, value) -> StatBar(info.label, value, bro.primaryType.color) }
            } else {
                StatHexagon(bro.stats, bro.primaryType.color)
            }
            Text(
                "TOTAL ${bro.stats.total}",
                style = PixelText.Tiny,
                color = DexColors.TextMuted,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovesPanel(bro: Bro) {
    ScreenPanel(title = "Signature moves") {
        if (bro.moves.isEmpty()) {
            Text("No moves yet. Every bro has one though.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bro.moves.forEach { move ->
                    val shape = CutCornerShape(4.dp)
                    Text(
                        move.uppercase(),
                        style = PixelText.Tiny,
                        color = DexColors.Text,
                        modifier = Modifier
                            .background(bro.primaryType.color.copy(alpha = 0.18f), shape)
                            .border(1.dp, bro.primaryType.color, shape)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryPanel(
    memories: List<Memory>,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onVoice: () -> Unit,
    onPick: () -> Unit,
    onOpen: (Memory) -> Unit,
) {
    ScreenPanel(title = "Memory log (${memories.size})") {
        if (memories.isEmpty()) {
            Text(
                "No memories yet. Each one is worth +${Evolution.MEMORY_POINTS} bond points.",
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories.sortedByDescending { it.date }, key = { it.id }) { memory ->
                    Column(Modifier.width(120.dp)) {
                        MediaThumbnail(
                            fileUri = memory.fileUri,
                            type = memory.mediaType,
                            maxPx = 256,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(1.dp, DexColors.ScreenBorder)
                                .clickable { onOpen(memory) },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            memory.caption.ifBlank { formatDate(memory.date) },
                            style = MaterialTheme.typography.bodySmall,
                            color = DexColors.TextMuted,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryButton("Photo", Icons.Filled.PhotoCamera, onPhoto, Modifier.weight(1f))
            MemoryButton("Video", Icons.Filled.Videocam, onVideo, Modifier.weight(1f))
            MemoryButton("Voice", Icons.Filled.Mic, onVoice, Modifier.weight(1f))
            MemoryButton("Gallery", Icons.Filled.PhotoLibrary, onPick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MemoryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = CutCornerShape(4.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = DexColors.ScreenText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.Text)
        }
    }
}

@Composable
private fun FactsPanel(facts: List<Fact>, onAdd: () -> Unit, onRemove: (Fact) -> Unit) {
    ScreenPanel(title = "Bro facts (${facts.size})") {
        if (facts.isEmpty()) {
            Text(
                "What's their go-to song? Their team? Each fact is +${Evolution.FACT_POINTS} pts.",
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        facts.forEach { fact ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fact.category.uppercase(), style = PixelText.Tiny, color = DexColors.ScreenText)
                    Spacer(Modifier.height(2.dp))
                    Text(fact.value, color = DexColors.Text)
                }
                IconButton(onClick = { onRemove(fact) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove fact", tint = DexColors.TextMuted)
                }
            }
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = DexColors.LedGreen)
            Spacer(Modifier.width(6.dp))
            Text("ADD FACT", style = PixelText.Tiny, color = DexColors.LedGreen)
        }
    }
}

@Composable
private fun InfoPanel(bro: Bro, onPickMeetDate: () -> Unit, onTradeableChanged: (Boolean) -> Unit) {
    ScreenPanel(title = "Catch info") {
        InfoLine(if (bro.isTraded) "Received" else "Caught", formatDate(bro.catchDate))
        if (bro.catchLocation.isNotBlank()) InfoLine("Location", bro.catchLocation)
        InfoLine("Check-ins", bro.checkInCount.toString())
        Birthdays.of(bro)?.let { md ->
            val days = Birthdays.daysUntil(md)
            InfoLine(
                "Birthday",
                when (days) {
                    0 -> "TODAY! 🎂"
                    1 -> "Tomorrow"
                    else -> "${md.format(DateTimeFormatter.ofPattern("MMM d"))} (in $days days)"
                },
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickMeetDate)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("MET IRL", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
            Text(
                bro.realMeetDate?.let { formatUtcDate(it) } ?: "Tap to set",
                color = if (bro.realMeetDate == null) DexColors.LedBlue else DexColors.Text,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (bro.isTradeable) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (bro.isTradeable) DexColors.LedGreen else DexColors.TextMuted,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (bro.isTradeable) "TRADEABLE" else "LOCKED", style = PixelText.Tiny, color = DexColors.Text)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (bro.isTradeable) "Can be shared as a QR card." else "Can't be shared. Stays on this phone only.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = bro.isTradeable,
                onCheckedChange = onTradeableChanged,
                colors = SwitchDefaults.colors(checkedTrackColor = DexColors.LedGreen.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, color = DexColors.Text)
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = "Cancel",
    maxLength: Int = 140,
    placeholder: String? = null,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase(), style = PixelText.Label) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(maxLength) },
                label = { Text(label) },
                placeholder = placeholder?.let { { Text(it) } },
                supportingText = { Text("${text.length}/$maxLength") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

private const val CUSTOM_CATEGORY = "Custom..."

/** Pick a category from the dropdown (or "Custom..." to type one), then fill in the value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFactDialog(onConfirm: (String, String, String?) -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var birthday by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    var choice by rememberSaveable { mutableStateOf(FactCategories.presets.first()) }
    var custom by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    val category = if (choice == CUSTOM_CATEGORY) custom else choice
    val isBirthday = choice == FactCategories.BIRTHDAY
    val birthdayLabel = Birthdays.parse(birthday)?.format(DateTimeFormatter.ofPattern("MMMM d"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW FACT", style = PixelText.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = choice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (FactCategories.presets + CUSTOM_CATEGORY).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { choice = option; expanded = false },
                            )
                        }
                    }
                }
                if (choice == CUSTOM_CATEGORY) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it.take(32) },
                        label = { Text("Your category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (isBirthday) {
                    // Birthdays are a real date so Brokemon can remind you on the day.
                    OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth(), shape = CutCornerShape(4.dp)) {
                        Text(birthdayLabel ?: "Pick the date")
                    }
                    Text(
                        "You'll get a heads-up on the day (Settings → Reminders).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DexColors.TextMuted,
                    )
                } else {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.take(80) },
                        label = { Text(category.ifBlank { "Value" }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            val ready = if (isBirthday) birthdayLabel != null else category.isNotBlank() && value.isNotBlank()
            TextButton(
                onClick = {
                    if (isBirthday) onConfirm(category, birthdayLabel!!, birthday) else onConfirm(category, value, null)
                },
                enabled = ready,
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (pickingDate) {
        BirthdayPicker(
            onPicked = { birthday = it; pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPicker(onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onPicked(Birthdays.toStorage(MonthDay.from(date)))
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state, title = { Text("Birthday (the year doesn't matter)", modifier = Modifier.padding(16.dp)) })
    }
}

@Composable
private fun LookEditorDialog(
    initial: BroLook,
    shiny: Boolean,
    stage: Int,
    onSave: (BroLook) -> Unit,
    onDismiss: () -> Unit,
) {
    var look by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(DexColors.Surface, CutCornerShape(8.dp))
                .border(2.dp, DexColors.Outline, CutCornerShape(8.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("EDIT LOOK", style = PixelText.Header, color = DexColors.Text)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(150.dp).background(DexColors.Screen, CutCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                BroSprite(look, stage, shiny, Modifier.size(140.dp))
            }
            Spacer(Modifier.height(12.dp))
            AvatarBuilder(look = look, onLookChange = { look = it })
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onSave(look) }) { Text("Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeetDateDialog(initial: Long?, onConfirm: (Long?) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(pickerState.selectedDateMillis) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = { onConfirm(null) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    ) {
        DatePicker(state = pickerState, title = { Text("When did you actually meet?", modifier = Modifier.padding(16.dp)) })
    }
}

@Composable
private fun MemoryViewer(memory: Memory, audio: AudioPlayerState, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    DisposableEffect(memory.id) { onDispose { audio.stop() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(DexColors.Surface, CutCornerShape(8.dp))
                .border(2.dp, DexColors.Outline, CutCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 480.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                when (memory.mediaType) {
                    MediaType.PHOTO -> MediaThumbnail(
                        fileUri = memory.fileUri,
                        type = memory.mediaType,
                        maxPx = 1440,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                    MediaType.VIDEO -> AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(memory.fileUri.toUri())
                                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                                setOnPreparedListener { start() }
                            }
                        },
                        onRelease = { it.stopPlayback() },
                        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true),
                    )
                    MediaType.AUDIO -> Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        PixelWaveform(Modifier.fillMaxWidth().height(90.dp), seed = memory.fileUri.hashCode())
                        AudioPlayButton(audio, memory.fileUri, Modifier.fillMaxWidth(), label = "Play")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (memory.caption.isNotBlank()) Text(memory.caption, color = DexColors.Text)
            Text(formatDate(memory.date), style = MaterialTheme.typography.bodySmall, color = DexColors.TextMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = DexColors.LedRed) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("DELETE MEMORY?", style = PixelText.Label) },
            text = { Text("The file is removed from this device for good.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = DexColors.LedRed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

internal fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/** The date picker stores UTC midnight, so format it in UTC to avoid an off-by-one day. */
private fun formatUtcDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(millis))
