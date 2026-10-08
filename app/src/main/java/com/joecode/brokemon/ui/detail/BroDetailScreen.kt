package com.joecode.brokemon.ui.detail

import androidx.compose.material.icons.filled.Style
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.components.pixelBox
import com.joecode.brokemon.ui.components.DexySays
import com.joecode.brokemon.ui.components.StickerImage
import com.joecode.brokemon.domain.Cosmetic
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.joecode.brokemon.ui.components.PixelChip
import com.joecode.brokemon.ui.components.PixelProgressBar
import com.joecode.brokemon.ui.components.PixelPanel
import com.joecode.brokemon.domain.Missing
import com.joecode.brokemon.domain.Completeness
import com.joecode.brokemon.ui.feedback.ToastKind
import com.joecode.brokemon.ui.feedback.LocalFeedback
import androidx.compose.ui.semantics.contentDescription
import com.joecode.brokemon.ui.components.PixelIconImage
import com.joecode.brokemon.ui.components.PixelIcon
import com.joecode.brokemon.ui.theme.MinTouch
import com.joecode.brokemon.ui.navigation.sharedCard
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import com.joecode.brokemon.ui.theme.Spacing
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
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CollectionsBookmark
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
import com.joecode.brokemon.ui.components.PixelAlertDialog
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
import com.joecode.brokemon.ui.components.rememberDeviceTilt
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
    /** Quick action from the Home long-press sheet: "memory" or "edit". */
    focus: String? = null,
    viewModel: BroDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val evolutionEvent by viewModel.evolutionEvent.collectAsStateWithLifecycle()
    val captionTarget by viewModel.captionTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val feedback = LocalFeedback.current
    var showAddMoves by rememberSaveable { mutableStateOf(false) }
    var pickingSlot by rememberSaveable { mutableStateOf(-1) }
    var showFrames by rememberSaveable { mutableStateOf(false) }
    val ownedCosmetics = com.joecode.brokemon.ui.components.LocalOwnedCosmetics.current
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDexEntry by rememberSaveable { mutableStateOf(false) }
    var showHabitat by rememberSaveable { mutableStateOf(false) }
    var showDexes by rememberSaveable { mutableStateOf(false) }
    val dexMembership by viewModel.dexMembership.collectAsStateWithLifecycle()
    val shinyMoment by viewModel.shinyMoment.collectAsStateWithLifecycle()
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

    // Quick actions open the card with the right dialog already up.
    var focusHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focus, state.bro?.id) {
        if (focus == null || focusHandled || state.bro == null) return@LaunchedEffect
        focusHandled = true
        when (focus) {
            "memory" -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            "edit" -> showRename = true
        }
    }

    val bro = state.bro
    val position by viewModel.position.collectAsStateWithLifecycle()
    // Flip animation when swiping to another bro: slide out, switch, slide back in from the other side.
    val slide = remember { Animatable(0f) }
    suspend fun flip(step: Int) {
        slide.animateTo(-step * 0.25f, tween(110))
        if (viewModel.swipe(step)) {
            slide.snapTo(step * 0.25f)
            slide.animateTo(0f, tween(170))
        } else {
            slide.animateTo(0f, spring())
        }
    }
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
                            text = { Text("Regional dexes") },
                            leadingIcon = { Icon(Icons.Filled.CollectionsBookmark, null) },
                            onClick = { menuOpen = false; showDexes = true },
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
                            text = { Text("Card frame") },
                            leadingIcon = { Icon(Icons.Filled.Style, null) },
                            onClick = { menuOpen = false; showFrames = true },
                        )
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
                        .padding(padding)
                        // Swipe anywhere outside the card to flip to the next or previous bro in the dex.
                        .pointerInput(Unit) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = { if (abs(total) > 140f) scope.launch { flip(if (total < 0) 1 else -1) } },
                                onHorizontalDrag = { _, delta -> total += delta },
                            )
                        }
                        .graphicsLayer {
                            translationX = slide.value * size.width
                            alpha = (1f - abs(slide.value) * 2.5f).coerceIn(0f, 1f)
                        },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
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
                        SwipeRow(position, onPrev = { scope.launch { flip(-1) } }, onNext = { scope.launch { flip(1) } })
                    }
                    item {
                        CompletenessNudge(bro) { missing ->
                            when (missing) {
                                Missing.MOVES -> showAddMoves = true
                                Missing.FACT -> showAddFact = true
                                Missing.MEMORY -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                Missing.DEX_ENTRY -> showDexEntry = true
                                Missing.HABITAT -> showHabitat = true
                            }
                        }
                    }
                    item {
                        CoachMark(
                            id = Hints.DETAIL_CHECK_IN,
                            text = "Talked to ${bro.name} today? Tap here!",
                            visible = seenHints != null && Hints.DETAIL_CHECK_IN !in seenHints && !CheckOnBro.checkedInToday(bro),
                            arrow = ArrowSide.BOTTOM,
                            arrowBias = 1f / 6f,
                            modifier = Modifier.padding(bottom = Spacing.xs),
                        )
                        ActionRow(
                            bro = bro,
                            onCheckIn = {
                                dismissHint(Hints.DETAIL_CHECK_IN)
                                viewModel.checkIn { result ->
                                    if (result == CheckInResult.CHECKED_IN) {
                                        feedback?.success("Checked in with ${bro.name}! +${Evolution.CHECK_IN_POINTS} pts")
                                    } else {
                                        feedback?.toast("Already checked in with ${bro.name} today", ToastKind.INFO)
                                    }
                                }
                            },
                            onShareQr = { onShare(bro.id) },
                            onShareStory = { showStory = true },
                        )
                    }
                    item { EvolutionPanel(bro, state.evolution!!) }
                    item { StatsPanel(bro) }
                    item { MovesPanel(bro, onAddMoves = { showAddMoves = true }) }
                    item { StickerPanel(bro, onSlot = { pickingSlot = it }, onFrame = { showFrames = true }) }
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
                            onEditHabitat = { showHabitat = true },
                            onTradeableChanged = viewModel::setTradeable,
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        if (bro != null) {
            evolutionEvent?.let { EvolutionOverlay(bro, it, viewModel::onEvolutionFinished) }
            if (shinyMoment && captionTarget == null && evolutionEvent == null) {
                ShinyMoment(
                    bro = bro,
                    stage = state.evolution?.stage ?: EvolutionStage.ROOKIE,
                    onShare = {
                        val stage = state.evolution?.stage ?: EvolutionStage.ROOKIE
                        ShareImage.share(context, StoryImages(context).card(bro, stage), "brokemon-shiny-${bro.dexNumber.drop(1)}", "Share shiny ${bro.name}")
                    },
                    onDismiss = viewModel::dismissShinyMoment,
                )
            }
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
                onConfirm = { viewModel.rename(it); showRename = false; feedback?.success("Renamed") },
                onDismiss = { showRename = false },
            )
        }
        if (showDexes) {
            RegionalDexDialog(
                broName = bro.name,
                membership = dexMembership,
                onToggle = viewModel::setInDex,
                onCreate = viewModel::createDexWithBro,
                onDismiss = { showDexes = false },
            )
        }
        if (showHabitat) {
            HabitatDialog(
                initial = bro.habitat.orEmpty(),
                onConfirm = { viewModel.setHabitat(it); showHabitat = false; feedback?.success("Habitat saved") },
                onDismiss = { showHabitat = false },
            )
        }
        if (pickingSlot >= 0 && bro != null) {
            StickerPickerDialog(
                owned = ownedCosmetics,
                current = bro.stickers.getOrNull(pickingSlot),
                onPick = { viewModel.setSticker(pickingSlot, it); pickingSlot = -1; feedback?.success(if (it == null) "Sticker removed" else "Sticker placed") },
                onDismiss = { pickingSlot = -1 },
            )
        }
        if (showFrames && bro != null) {
            FramePickerDialog(
                owned = ownedCosmetics,
                current = bro.cardFrame,
                onPick = { viewModel.setCardFrame(it); showFrames = false; feedback?.success("Frame changed") },
                onDismiss = { showFrames = false },
            )
        }
        if (showAddMoves && bro != null) {
            AddMovesDialog(
                existing = bro.moves,
                onConfirm = { viewModel.addMoves(it); showAddMoves = false; feedback?.success("Moves added") },
                onDismiss = { showAddMoves = false },
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
                onConfirm = { viewModel.setFlavorText(it); showDexEntry = false; feedback?.success("Dex entry saved") },
                onDismiss = { showDexEntry = false },
            )
        }
        if (showRarity) {
            PixelAlertDialog(
                onDismissRequest = { showRarity = false },
                title = { Text("RARITY", style = PixelText.Header) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
            PixelAlertDialog(
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
                    feedback?.success("Fact added")
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
 * The card itself, big and centered, like holding a real trading card:
 * press and drag to tilt it in 3D (the foil follows your finger), release to
 * let it spring back. Tap plays their voice line if they have one.
 */
@Composable
private fun CardShowcase(bro: Bro, evolution: EvolutionInfo, tilt: TiltState, onTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(vertical = Spacing.sm)
                .widthIn(max = 300.dp)
                .fillMaxWidth(0.82f)
                .sharedCard(bro.id)
                .dragToTilt(tilt, scope),
        ) {
            BroCard(
                bro = bro,
                stage = evolution.stage,
                tilt = tilt,
                tiltDegrees = 16f,
                deviceTilt = rememberDeviceTilt(),
                showDexEntry = true,
                spriteReacts = true,
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
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(false to "HEX", true to "BARS").forEach { (bars, label) ->
                    val selected = asBars == bars
                    Text(
                        label,
                        style = PixelText.Tiny,
                        color = if (selected) DexColors.OnBright else DexColors.TextMuted,
                        modifier = Modifier
                            .background(if (selected) DexColors.ScreenText else Color.Transparent, DexShape(3.dp))
                            .border(1.dp, DexColors.ScreenBorder, DexShape(3.dp))
                            .clickable(role = Role.Tab) { asBars = bars }
                            .semantics { this.selected = selected }
                            .padding(horizontal = Spacing.sm, vertical = 5.dp),
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
private fun MovesPanel(bro: Bro, onAddMoves: (() -> Unit)? = null) {
    ScreenPanel(title = "Signature moves") {
        if (bro.moves.isEmpty()) {
            Text("No moves yet. Every bro has one though.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            if (onAddMoves != null) {
                Spacer(Modifier.height(Spacing.sm))
                PixelButton("Add moves", onClick = onAddMoves, color = DexColors.LedBlue)
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                bro.moves.forEach { move ->
                    val shape = DexShape(4.dp)
                    Text(
                        move.uppercase(),
                        style = PixelText.Tiny,
                        color = DexColors.Text,
                        modifier = Modifier
                            .background(bro.primaryType.color.copy(alpha = 0.18f), shape)
                            .border(1.dp, bro.primaryType.color, shape)
                            .padding(horizontal = 10.dp, vertical = Spacing.sm),
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MemoryButton("Photo", Icons.Filled.PhotoCamera, onPhoto, Modifier.weight(1f))
            MemoryButton("Video", Icons.Filled.Videocam, onVideo, Modifier.weight(1f))
            MemoryButton("Voice", Icons.Filled.Mic, onVoice, Modifier.weight(1f))
            MemoryButton("Gallery", Icons.Filled.PhotoLibrary, onPick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MemoryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = DexShape(Spacing.xs), contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xs)) {
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
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
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
private fun InfoPanel(bro: Bro, onPickMeetDate: () -> Unit, onEditHabitat: () -> Unit, onTradeableChanged: (Boolean) -> Unit) {
    ScreenPanel(title = "Catch info") {
        InfoLine(if (bro.isTraded) "Received" else "Caught", formatDate(bro.catchDate))
        if (bro.catchLocation.isNotBlank()) InfoLine("Location", bro.catchLocation)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Edit habitat", onClick = onEditHabitat)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("HABITAT", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
            Text(bro.habitat ?: "Tap to set", color = if (bro.habitat == null) DexColors.LedBlue else DexColors.Text)
        }
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
    PixelAlertDialog(
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
    PixelAlertDialog(
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
                    OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth(), shape = DexShape(4.dp)) {
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
        DatePicker(state = state, title = { Text("Birthday (the year doesn't matter)", modifier = Modifier.padding(Spacing.lg)) })
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
                .padding(Spacing.md)
                .background(DexColors.Surface, DexShape(8.dp))
                .border(2.dp, DexColors.Outline, DexShape(8.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("EDIT LOOK", style = PixelText.Header, color = DexColors.Text)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(150.dp).background(DexColors.Screen, DexShape(4.dp)), contentAlignment = Alignment.Center) {
                BroSprite(look, stage, shiny, Modifier.size(140.dp))
            }
            Spacer(Modifier.height(12.dp))
            AvatarBuilder(look = look, onLookChange = { look = it })
            Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.End) {
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
        DatePicker(state = pickerState, title = { Text("When did you actually meet?", modifier = Modifier.padding(Spacing.lg)) })
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
                .padding(Spacing.lg)
                .background(DexColors.Surface, DexShape(8.dp))
                .border(2.dp, DexColors.Outline, DexShape(8.dp))
                .padding(Spacing.md),
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
        PixelAlertDialog(
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

@Composable
private fun HabitatDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HABITAT", style = PixelText.Label) },
        text = { com.joecode.brokemon.ui.catchbro.HabitatField(text, { text = it.take(Bro.MAX_HABITAT) }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RegionalDexDialog(
    broName: String,
    membership: List<Pair<com.joecode.brokemon.data.model.RegionalDex, Boolean>>,
    onToggle: (Long, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf("") }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${broName.uppercase()}'S DEXES", style = PixelText.Label) },
        text = {
            Column {
                Text("Always in the National Dex. Add them to as many regional dexes as you like.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                membership.forEach { (dex, member) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggle(dex.id, !member) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = member, onCheckedChange = { onToggle(dex.id, it) })
                        Text(dex.name)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(com.joecode.brokemon.data.model.RegionalDex.MAX_NAME) },
                    label = { Text("New dex (e.g. Uni Dex)") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { onCreate(newName); newName = "" }, enabled = newName.isNotBlank()) { Text("Add") }
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Previous / next arrows with the position in the dex ("3 / 12"), the tap alternative to swiping. */
@Composable
private fun SwipeRow(position: Pair<Int, Int>?, onPrev: () -> Unit, onNext: () -> Unit) {
    if (position == null || position.second < 2) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(MinTouch).clickable(role = Role.Button, onClickLabel = "Previous bro", onClick = onPrev),
            contentAlignment = Alignment.Center,
        ) { PixelIconImage(PixelIcon.BACK, tint = DexColors.Text, size = 24.dp) }
        Text(
            "%02d / %02d".format(position.first, position.second),
            style = PixelText.Tiny,
            color = DexColors.TextMuted,
            modifier = Modifier.padding(horizontal = Spacing.md).semantics { contentDescription = "Bro ${position.first} of ${position.second}" },
        )
        Box(
            Modifier.size(MinTouch).clickable(role = Role.Button, onClickLabel = "Next bro", onClick = onNext),
            contentAlignment = Alignment.Center,
        ) { PixelIconImage(PixelIcon.BACK, tint = DexColors.Text, size = 24.dp, modifier = Modifier.graphicsLayer { scaleX = -1f }) }
    }
    Text(
        "SWIPE LEFT OR RIGHT (OUTSIDE THE CARD) TO FLIP THROUGH BROS",
        style = PixelText.Tiny,
        color = DexColors.Outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}


/** "Card 40% complete: add moves?" Tapping opens the matching editor. Disappears at 100%. */
@Composable
private fun CompletenessNudge(bro: Bro, onAct: (Missing) -> Unit) {
    val c = remember(bro) { Completeness.of(bro) }
    val next = c.missing.firstOrNull() ?: return
    PixelPanel(
        Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = next.action) { onAct(next) },
        border = DexColors.LedYellow,
        fill = DexColors.Surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CARD ${c.percent}% COMPLETE", style = PixelText.Tiny, color = DexColors.LedYellow)
                Spacer(Modifier.height(Spacing.xs))
                PixelProgressBar(c.percent / 100f, DexColors.LedYellow, Modifier.fillMaxWidth(0.8f), segments = 20, height = 12.dp)
                Spacer(Modifier.height(Spacing.xs))
                Text(c.nudge.orEmpty(), color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            PixelIconImage(PixelIcon.PLUS, tint = DexColors.LedYellow, size = 24.dp)
        }
    }
}

/** Pick up to a few preset moves, or type your own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMovesDialog(existing: List<String>, onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val room = (com.joecode.brokemon.ui.catchbro.BroState.MAX_MOVES - existing.size).coerceAtLeast(1)
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var custom by rememberSaveable { mutableStateOf("") }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADD MOVES (${picked.size}/$room)", style = PixelText.Label, color = DexColors.Text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    com.joecode.brokemon.ui.catchbro.PresetMoves.all.filter { m -> existing.none { it.equals(m, true) } }.forEach { m ->
                        PixelChip(m, m in picked, {
                            picked = if (m in picked) picked - m else if (picked.size < room) picked + m else picked
                        }, color = DexColors.DexRedLight)
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.take(com.joecode.brokemon.share.QrCodec.MAX_MOVE_LENGTH) },
                    label = { Text("Custom move") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PixelButton("Add", onClick = {
                val extra = custom.trim().takeIf { it.isNotEmpty() && picked.size < room }
                onConfirm(picked + listOfNotNull(extra))
            }, enabled = picked.isNotEmpty() || custom.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", style = PixelText.Tiny, color = DexColors.TextMuted) } },
    )
}


/** Three sticker slots (collected from Daily Packs) and the card frame shortcut. Purely cosmetic. */
@Composable
private fun StickerPanel(bro: Bro, onSlot: (Int) -> Unit, onFrame: () -> Unit) {
    PixelPanel(title = "Stickers & frame", modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { slot ->
                val sticker = bro.stickers.getOrNull(slot)?.let { Cosmetic.from(it) }
                Box(
                    Modifier
                        .size(64.dp)
                        .semantics { contentDescription = if (sticker != null) "Sticker slot ${slot + 1}: ${sticker.label}" else "Empty sticker slot ${slot + 1}" }
                        .pixelBox(DexColors.Screen, DexColors.ScreenBorder, Borders.normal, 2.dp)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { onSlot(slot) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (sticker != null) StickerImage(sticker, 48.dp) else PixelIconImage(PixelIcon.PLUS, tint = DexColors.TextMuted, size = 24.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            PixelButton("Frame", onClick = onFrame, color = DexColors.LedBlue)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickerPickerDialog(owned: Set<String>, current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val stickers = Cosmetic.stickers().filter { it.id in owned }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PICK A STICKER", style = PixelText.Label, color = DexColors.Text) },
        text = {
            if (stickers.isEmpty()) {
                DexySays("No stickers yet! Open your Daily Pack to collect some.")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    stickers.forEach { st ->
                        Box(
                            Modifier
                                .size(64.dp)
                                .semantics { contentDescription = st.label }
                                .pixelBox(DexColors.Screen, if (st.id == current) DexColors.LedYellow else DexColors.ScreenBorder, Borders.normal, 2.dp)
                                .clickable(role = androidx.compose.ui.semantics.Role.Button) { onPick(st.id) },
                            contentAlignment = Alignment.Center,
                        ) { StickerImage(st, 48.dp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", style = PixelText.Tiny, color = DexColors.LedYellow) } },
        dismissButton = if (current != null) ({ TextButton(onClick = { onPick(null) }) { Text("REMOVE", style = PixelText.Tiny, color = DexColors.TextMuted) } }) else null,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FramePickerDialog(owned: Set<String>, current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val frames = Cosmetic.entries.filter { it.kind == com.joecode.brokemon.domain.CosmeticKind.FRAME && it.id in owned }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CARD FRAME", style = PixelText.Label, color = DexColors.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PixelChip("Default (type colors)", current == null, { onPick(null) })
                frames.forEach { f -> PixelChip(f.label, current == f.frame, { onPick(f.frame) }) }
                if (frames.isEmpty()) Text("Frames come from the Daily Pack.", color = DexColors.TextMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", style = PixelText.Tiny, color = DexColors.LedYellow) } },
    )
}
