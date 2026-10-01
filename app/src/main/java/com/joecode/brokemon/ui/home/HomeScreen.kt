package com.joecode.brokemon.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.ui.components.rarityGlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.CatchCube
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.EmptyState
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

@Composable
fun HomeScreen(
    onCatch: () -> Unit,
    onBroClick: (Long) -> Unit,
    onSquads: () -> Unit,
    onTrade: () -> Unit,
    onCheckOnBro: () -> Unit,
    onWrapped: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Debug builds always show Wrapped so it can be tested outside the New Year window.
    val isDebug = remember { context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    val showWrapped = state.wrappedYear != null || isDebug

    DexScaffold(
        title = "Brodex",
        actions = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = DexColors.Text)
            }
        },
        floatingActionButton = {
            if (state.totalCaught > 0) CatchFab(onCatch)
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = DexColors.DexRed)
            }

            state.totalCaught == 0 -> EmptyState(
                title = "Your Brodex is empty",
                body = "Every legend starts with a first catch. Who's your day one?",
                modifier = Modifier.padding(padding),
                action = { PixelButton("Catch your first bro", onCatch) },
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DexCounter(state.totalCaught, state.shinyCount, state.legendaryCount)
                }
                state.wrappedYear?.let { year ->
                    item(span = { GridItemSpan(maxLineSpan) }) { WrappedBanner(year, onWrapped) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    QuickActions(onSquads, onTrade, onCheckOnBro, onWrapped.takeIf { showWrapped })
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchAndFilter(
                        query = state.query,
                        onQueryChanged = viewModel::onQueryChanged,
                        selected = state.typeFilter,
                        onTypeSelected = viewModel::onTypeFilterSelected,
                    )
                }
                if (state.entries.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No bros match that search.",
                            color = DexColors.TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(state.entries, key = { it.bro.id }) { entry ->
                    BroCard(
                        bro = entry.bro,
                        stage = entry.stage,
                        onClick = { onBroClick(entry.bro.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
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
private fun QuickActions(
    onSquads: () -> Unit,
    onTrade: () -> Unit,
    onCheckOnBro: () -> Unit,
    onWrapped: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction("Squads", Icons.Filled.Groups, DexColors.LedBlue, onSquads, Modifier.weight(1f))
        QuickAction("Trade", Icons.Filled.QrCode2, DexColors.LedGreen, onTrade, Modifier.weight(1f))
        QuickAction("Check in", Icons.Filled.WavingHand, DexColors.LedYellow, onCheckOnBro, Modifier.weight(1f))
        if (onWrapped != null) {
            QuickAction("Wrapped", Icons.Filled.AutoAwesome, DexColors.DexRedLight, onWrapped, Modifier.weight(1f))
        }
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
private fun SearchAndFilter(
    query: String,
    onQueryChanged: (String) -> Unit,
    selected: BroType?,
    onTypeSelected: (BroType?) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search your bros") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DexColors.DexRed,
                unfocusedBorderColor = DexColors.Outline,
            ),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(BroType.entries) { type ->
                val isSelected = type == selected
                val shape = CutCornerShape(4.dp)
                Text(
                    type.label.uppercase(),
                    style = PixelText.Tiny,
                    color = if (isSelected) Color(0xFF101014) else type.color,
                    modifier = Modifier
                        .background(if (isSelected) type.color else Color.Transparent, shape)
                        .border(1.dp, type.color, shape)
                        .clickable { onTypeSelected(type) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
