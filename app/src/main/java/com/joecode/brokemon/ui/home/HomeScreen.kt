package com.joecode.brokemon.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.DexView
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.ArrowSide
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.CatchCube
import com.joecode.brokemon.ui.components.CoachMark
import com.joecode.brokemon.ui.components.DexListRow
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.EventBanner
import com.joecode.brokemon.ui.components.Hints
import com.joecode.brokemon.ui.components.MysteryCard
import com.joecode.brokemon.ui.components.MysteryRow
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.rarityGlow
import com.joecode.brokemon.ui.components.rememberHintDismisser
import com.joecode.brokemon.ui.components.rememberSeenHints
import com.joecode.brokemon.ui.navigation.sharedCard
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.trainer.TrainerStrip
import com.joecode.brokemon.ui.wild.ShakeEffect
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    onCatch: () -> Unit,
    onBroClick: (Long) -> Unit,
    onSquads: () -> Unit,
    onTrade: () -> Unit,
    onCheckOnBro: () -> Unit,
    onWrapped: () -> Unit,
    onSettings: () -> Unit,
    onEnterRoom: (Long) -> Unit,
    onTrainer: () -> Unit = {},
    onJournal: () -> Unit = {},
    onWild: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val seen = rememberSeenHints()
    val dismissHint = rememberHintDismisser()
    val hasBros = state.totalCaught > 0

    // Shake the phone: a wild bro appears.
    ShakeEffect(enabled = hasBros && !state.isLoading) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        dismissHint(Hints.HOME_WILD)
        onWild()
    }

    // One coach mark at a time on Home, in this order.
    val activeHint = seen?.let { s ->
        listOfNotNull(
            Hints.HOME_JOURNAL,
            Hints.HOME_VIEWS.takeIf { hasBros },
            Hints.HOME_WILD.takeIf { hasBros },
        ).firstOrNull { it !in s }
    }

    val actions = HomeActions(
        onCatch = onCatch,
        onBroClick = onBroClick,
        onLongPress = { id ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onEnterRoom(id)
        },
        onSquads = onSquads,
        onTrade = onTrade,
        onCheckOnBro = onCheckOnBro,
        onWrapped = onWrapped,
        onTrainer = onTrainer,
        onJournal = { dismissHint(Hints.HOME_JOURNAL); onJournal() },
        onWild = { dismissHint(Hints.HOME_WILD); onWild() },
        onOpenFilters = { showFilters = true },
    )

    DexScaffold(
        title = "Brodex",
        actions = {
            if (hasBros) {
                DexViewToggle(state.view) {
                    dismissHint(Hints.HOME_VIEWS)
                    viewModel.setView(it)
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = DexColors.Text)
            }
        },
        floatingActionButton = { if (hasBros) CatchFab(onCatch) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = DexColors.DexRed)
            }
            !hasBros -> EmptyDex(state, activeHint, actions, Modifier.padding(padding))
            else -> AnimatedContent(
                targetState = state.view,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 1.04f))
                },
                label = "dexView",
                modifier = Modifier.padding(padding),
            ) { view ->
                when (view) {
                    DexView.CARDS -> CardGrid(state, activeHint, actions, viewModel)
                    DexView.LIST -> PokedexList(state, activeHint, actions, viewModel)
                    DexView.BINDER -> Binder(state, actions, viewModel)
                }
            }
        }
    }
    if (showFilters) {
        FilterSheet(
            filter = state.filter,
            onFilterChanged = viewModel::onFilterChanged,
            onClear = viewModel::clearFilters,
            resultCount = state.entries.size,
            onDismiss = { showFilters = false },
        )
    }
}

private class HomeActions(
    val onCatch: () -> Unit,
    val onBroClick: (Long) -> Unit,
    val onLongPress: (Long) -> Unit,
    val onSquads: () -> Unit,
    val onTrade: () -> Unit,
    val onCheckOnBro: () -> Unit,
    val onWrapped: () -> Unit,
    val onTrainer: () -> Unit,
    val onJournal: () -> Unit,
    val onWild: () -> Unit,
    val onOpenFilters: () -> Unit,
)

private val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)

@Composable
private fun CardGrid(state: HomeUiState, activeHint: String?, actions: HomeActions, viewModel: HomeViewModel) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { HomeHeader(state, activeHint, actions, viewModel) }
        items(state.entries, key = { it.bro.id }) { entry ->
            BroCard(
                bro = entry.bro,
                stage = entry.stage,
                onClick = { actions.onBroClick(entry.bro.id) },
                onLongClick = { actions.onLongPress(entry.bro.id) },
                modifier = Modifier.animateItem().sharedCard(entry.bro.id),
            )
        }
        items(state.mysterySlots, key = { "slot$it" }) { slot ->
            MysteryCard(slot, Modifier.animateItem(), onClick = actions.onCatch)
        }
    }
}

/** The classic Pokédex list: one row per bro, sprite, number, name, types, rarity. */
@Composable
private fun PokedexList(state: HomeUiState, activeHint: String?, actions: HomeActions, viewModel: HomeViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { HomeHeader(state, activeHint, actions, viewModel) }
        items(state.entries, key = { it.bro.id }) { entry ->
            DexListRow(
                bro = entry.bro,
                stage = entry.stage,
                onClick = { actions.onBroClick(entry.bro.id) },
                onLongClick = { actions.onLongPress(entry.bro.id) },
                modifier = Modifier.animateItem().sharedCard(entry.bro.id),
            )
        }
        items(state.mysterySlots, key = { "slot$it" }) { slot ->
            MysteryRow(slot, Modifier.animateItem(), onClick = actions.onCatch)
        }
    }
}

/** Binder: one big card at a time, swipe left and right like flipping a card binder. */
@Composable
private fun Binder(state: HomeUiState, actions: HomeActions, viewModel: HomeViewModel) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            SearchBar(
                query = state.query,
                onQueryChanged = viewModel::onQueryChanged,
                filter = state.filter,
                onFilterChanged = viewModel::onFilterChanged,
                onOpenFilters = actions.onOpenFilters,
                onClear = viewModel::clearFilters,
                shown = state.entries.size,
                total = state.totalCaught,
            )
        }
        if (state.entries.isEmpty()) {
            NoMatches(viewModel)
            return@Column
        }
        val pager = rememberPagerState { state.entries.size }
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            key = { state.entries[it].bro.id },
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val entry = state.entries[page]
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                BroCard(
                    bro = entry.bro,
                    stage = entry.stage,
                    showDexEntry = true,
                    onClick = { actions.onBroClick(entry.bro.id) },
                    onLongClick = { actions.onLongPress(entry.bro.id) },
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .graphicsLayer {
                            // Neighbors shrink and dim, like pages curling away.
                            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                            val scale = lerp(1f, 0.86f, offset)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(1f, 0.5f, offset)
                            rotationY = ((pager.currentPage - page) + pager.currentPageOffsetFraction) * -12f
                            cameraDistance = 14f * density
                        }
                        .sharedCard(entry.bro.id),
                )
            }
        }
        Text(
            "%02d / %02d · SWIPE TO FLIP".format(pager.currentPage + 1, state.entries.size),
            style = PixelText.Tiny,
            color = DexColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp, top = 4.dp),
        )
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, activeHint: String?, actions: HomeActions, viewModel: HomeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CoachMark(
            id = Hints.HOME_VIEWS,
            text = "New: switch between Cards, the Pokédex list and the Binder up here.",
            visible = activeHint == Hints.HOME_VIEWS,
            arrow = ArrowSide.TOP,
            arrowBias = 0.72f,
        )
        TrainerStrip(
            trainer = state.progress.trainer,
            level = state.progress.level,
            questsDone = state.progress.questsDone,
            questsTotal = state.progress.quests.size,
            rewardsReady = state.progress.rewardsReady,
            onOpenTrainer = actions.onTrainer,
            onOpenJournal = actions.onJournal,
        )
        CoachMark(
            id = Hints.HOME_JOURNAL,
            text = "Your Trainer's Journal has starter quests. Finish them for frames, badges and a new hat!",
            visible = activeHint == Hints.HOME_JOURNAL,
            arrow = ArrowSide.TOP,
            arrowBias = 0.93f,
        )
        DexCounter(state.totalCaught, state.shinyCount, state.legendaryCount)
        state.event?.let { EventBanner(it) }
        state.wrappedYear?.let { WrappedBanner(it, actions.onWrapped) }
        CoachMark(
            id = Hints.HOME_WILD,
            text = "Shake your phone (or tap WILD) and a random bro jumps out. Perfect excuse to text them!",
            visible = activeHint == Hints.HOME_WILD,
            arrow = ArrowSide.BOTTOM,
            arrowBias = 0.875f,
        )
        QuickActions(actions)
        SearchBar(
            query = state.query,
            onQueryChanged = viewModel::onQueryChanged,
            filter = state.filter,
            onFilterChanged = viewModel::onFilterChanged,
            onOpenFilters = actions.onOpenFilters,
            onClear = viewModel::clearFilters,
            shown = state.entries.size,
            total = state.totalCaught,
        )
        if (state.showRoomHint && activeHint == null) RoomHint(viewModel::dismissRoomHint)
        if (state.entries.isEmpty()) NoMatches(viewModel)
    }
}

@Composable
private fun NoMatches(viewModel: HomeViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text("No bros match.", color = DexColors.TextMuted, textAlign = TextAlign.Center)
        TextButton(onClick = { viewModel.onQueryChanged(""); viewModel.clearFilters() }) {
            Text("CLEAR SEARCH & FILTERS", style = PixelText.Tiny, color = DexColors.LedYellow)
        }
    }
}

/**
 * First run: never a blank screen. Six "???" silhouettes wait to be caught,
 * with a pulsing button that says exactly what to do next.
 */
@Composable
private fun EmptyDex(state: HomeUiState, activeHint: String?, actions: HomeActions, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "first").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "pulse",
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = listPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TrainerStrip(
                    trainer = state.progress.trainer,
                    level = state.progress.level,
                    questsDone = state.progress.questsDone,
                    questsTotal = state.progress.quests.size,
                    rewardsReady = state.progress.rewardsReady,
                    onOpenTrainer = actions.onTrainer,
                    onOpenJournal = actions.onJournal,
                )
                CoachMark(
                    id = Hints.HOME_JOURNAL,
                    text = "Your Trainer's Journal has starter quests. Finish them for frames, badges and a new hat!",
                    visible = activeHint == Hints.HOME_JOURNAL,
                    arrow = ArrowSide.TOP,
                    arrowBias = 0.93f,
                )
                DexCounter(0, 0, 0)
                state.event?.let { EventBanner(it) }
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelButton(
                        text = "Catch your first Bro",
                        onClick = actions.onCatch,
                        leading = { CatchCube(Modifier.size(24.dp)) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .graphicsLayer { scaleX = pulse; scaleY = pulse }
                            .rarityGlow(Rarity.RARE),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Six empty slots. Who's your day one?",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        items(state.mysterySlots, key = { "slot$it" }) { slot -> MysteryCard(slot, onClick = actions.onCatch) }
    }
}

/** Card grid / Pokédex list / Binder switch in the top bar. */
@Composable
private fun DexViewToggle(current: DexView, onChange: (DexView) -> Unit) {
    val shape = CutCornerShape(5.dp)
    Row(
        Modifier
            .background(Color.Black.copy(alpha = 0.3f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(2.dp),
    ) {
        listOf(
            Triple(DexView.CARDS, Icons.Filled.GridView, "Card grid view"),
            Triple(DexView.LIST, Icons.AutoMirrored.Filled.ViewList, "Pokédex list view"),
            Triple(DexView.BINDER, Icons.Filled.ViewCarousel, "Binder view"),
        ).forEach { (view, icon, label) ->
            val selected = view == current
            Box(
                Modifier
                    .size(34.dp)
                    .background(if (selected) DexColors.LedYellow else Color.Transparent, CutCornerShape(4.dp))
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab, onClickLabel = label) { onChange(view) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = if (selected) Color(0xFF101014) else DexColors.Text, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CatchFab(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "fab")
    val wobble by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "wobble",
    )
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = DexColors.DexRed,
        contentColor = DexColors.Text,
        shape = CutCornerShape(8.dp),
        icon = { CatchCube(Modifier.size(28.dp).graphicsLayer { rotationZ = wobble }) },
        text = { Text("CATCH", style = PixelText.Label) },
    )
}

@Composable
private fun DexCounter(total: Int, shiny: Int, legendary: Int) {
    ScreenPanel(title = "Dex status") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Counter("CAUGHT", total, DexColors.ScreenText)
            Counter("SHINY", shiny, DexColors.Gold)
            Counter("LEGEND", legendary, DexColors.Legendary)
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%03d".format(value), style = PixelText.Title, color = color)
        Spacer(Modifier.height(6.dp))
        Text(label, style = PixelText.Tiny, color = DexColors.TextMuted)
    }
}

@Composable
private fun QuickActions(actions: HomeActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction("Squads", Icons.Filled.Groups, DexColors.LedBlue, actions.onSquads, Modifier.weight(1f))
        QuickAction("Trade", Icons.Filled.QrCode2, DexColors.LedGreen, actions.onTrade, Modifier.weight(1f))
        QuickAction("Check in", Icons.Filled.WavingHand, DexColors.LedYellow, actions.onCheckOnBro, Modifier.weight(1f))
        QuickAction("Wild", Icons.Filled.Grass, DexColors.DexRedLight, actions.onWild, Modifier.weight(1f))
    }
}

@Composable
private fun WrappedBanner(year: Int, onOpen: () -> Unit) {
    ScreenPanel(title = "Happy New Year", modifier = Modifier.rarityGlow(Rarity.LEGENDARY)) {
        Text("YOUR $year BRODEX WRAPPED IS HERE", style = PixelText.Label, color = DexColors.Gold)
        Spacer(Modifier.height(10.dp))
        PixelButton("Open Wrapped", onOpen, Modifier.fillMaxWidth(), color = DexColors.DexRed)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    val shape = CutCornerShape(6.dp)
    Column(
        modifier
            .background(DexColors.Surface, shape)
            .border(2.dp, color.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.height(8.dp))
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.Text, maxLines = 1)
    }
}

@Composable
private fun RoomHint(onDismiss: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DexColors.LedBlue.copy(alpha = 0.12f), CutCornerShape(6.dp))
            .border(1.dp, DexColors.LedBlue.copy(alpha = 0.5f), CutCornerShape(6.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TouchApp, null, tint = DexColors.LedBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Tip: press and hold a card to step inside their room.",
            color = DexColors.Text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Dismiss tip", tint = DexColors.TextMuted) }
    }
}
