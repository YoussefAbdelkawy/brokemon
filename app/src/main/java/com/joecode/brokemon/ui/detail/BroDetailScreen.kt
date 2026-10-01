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
import androidx.compose.material.icons.filled.Face
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
import com.joecode.brokemon.ui.components.AvatarBuilder
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

@Composable
fun BroDetailScreen(
    onBack: () -> Unit,
    onShare: (Long) -> Unit,
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
    var showRarity by rememberSaveable { mutableStateOf(false) }
    var showLookEditor by rememberSaveable { mutableStateOf(false) }
    var showStory by rememberSaveable { mutableStateOf(false) }
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
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menuOpen = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit look") },
                            leadingIcon = { Icon(Icons.Filled.Face, null) },
                            onClick = { menuOpen = false; showLookEditor = true },
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
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { HeroPanel(bro, state.evolution!!) }
                    item {
                        ActionRow(
                            bro = bro,
                            onCheckIn = {
                                viewModel.checkIn()
                                scope.launch { snackbar.showSnackbar("Checked in with ${bro.name}! +${Evolution.CHECK_IN_POINTS} pts") }
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
        viewing?.let { memory ->
            MemoryViewer(
                memory = memory,
                onDelete = { viewModel.deleteMemory(memory); viewing = null },
                onDismiss = { viewing = null },
            )
        }
    }
}

@Composable
private fun HeroPanel(bro: Bro, evolution: EvolutionInfo) {
    val transition = rememberInfiniteTransition(label = "hero")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "bob",
    )
    val auraPulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "aura",
    )
    ScreenPanel(title = "Bro data", modifier = Modifier.rarityGlow(bro.rarity)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (evolution.stage.ordinal > 0) {
                // Evolved bros keep a soft aura that grows with each stage.
                val color = bro.primaryType.color
                Canvas(Modifier.size(200.dp)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(color.copy(alpha = 0.45f * auraPulse * evolution.stage.ordinal / 2f), Color.Transparent),
                        ),
                    )
                }
            }
            BroSprite(
                bro = bro,
                stage = evolution.stage.ordinal,
                modifier = Modifier
                    .size(176.dp)
                    .graphicsLayer { translationY = bob },
            )
            if (bro.isShiny) Sparkles(Modifier.fillMaxSize(), count = 14, seed = bro.id.toInt())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bro.dexNumber, style = PixelText.Label, color = DexColors.TextMuted)
            Spacer(Modifier.width(10.dp))
            Text(bro.name.uppercase(), style = PixelText.Header, color = DexColors.Text, modifier = Modifier.weight(1f))
            RarityStars(bro.rarity, size = 14.dp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            bro.types.forEach { TypeBadge(it) }
            Spacer(Modifier.weight(1f))
            if (bro.isShiny) Text("SHINY", style = PixelText.Tiny, color = DexColors.Gold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            bro.primaryType.blurb,
            color = DexColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ActionRow(bro: Bro, onCheckIn: () -> Unit, onShareQr: () -> Unit, onShareStory: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PixelButton(
            text = "Check in",
            onClick = onCheckIn,
            modifier = Modifier.weight(1.3f),
            color = DexColors.SurfaceHigh,
            leading = { Icon(Icons.Filled.WavingHand, null, tint = DexColors.LedYellow, modifier = Modifier.size(18.dp)) },
        )
        PixelButton(
            text = "QR",
            onClick = onShareQr,
            enabled = bro.isTradeable,
            modifier = Modifier.weight(1f),
            leading = {
                Icon(
                    if (bro.isTradeable) Icons.Filled.QrCode2 else Icons.Filled.Lock,
                    "Share QR",
                    tint = DexColors.Text,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        PixelButton(
            text = "Post",
            onClick = onShareStory,
            enabled = bro.isTradeable,
            modifier = Modifier.weight(1f),
            color = DexColors.LedBlue.copy(alpha = 0.8f),
            leading = { Icon(Icons.Filled.IosShare, "Share story image", tint = DexColors.Text, modifier = Modifier.size(18.dp)) },
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
        ScoreLine("Facts", bro.facts.size, Evolution.FACT_POINTS)
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
    ScreenPanel(title = "Base stats") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bro.stats.asList().forEach { (info, value) -> StatBar(info.label, value, bro.primaryType.color) }
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
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase(), style = PixelText.Label) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(140) }, label = { Text(label) }) },
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
private fun MemoryViewer(memory: Memory, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
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
