package com.joecode.brokemon.ui.squads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.domain.TypeMatchups
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroRow
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.home.DexEntry
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

@Composable
fun SquadDetailScreen(
    onBack: () -> Unit,
    onBroClick: (Long) -> Unit,
    viewModel: SquadDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val squad = state.squad

    DexScaffold(
        title = squad?.name ?: "Squad",
        onBack = onBack,
        actions = {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More", tint = DexColors.Text) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { menuOpen = false; showRename = true },
                )
                DropdownMenuItem(
                    text = { Text("Disband squad", color = DexColors.LedRed) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = DexColors.LedRed) },
                    onClick = { menuOpen = false; showDelete = true },
                )
            }
        },
    ) { padding ->
        if (state.isLoading || squad == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator(color = DexColors.DexRed)
            }
            return@DexScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PARTY ${state.members.size}/${Squad.MAX_MEMBERS}",
                        style = PixelText.Label,
                        color = DexColors.DexRedLight,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPicker = true }, enabled = state.allBros.isNotEmpty()) {
                        Icon(Icons.Filled.PersonAdd, null, tint = DexColors.LedGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("EDIT", style = PixelText.Tiny, color = DexColors.LedGreen)
                    }
                }
            }
            if (state.members.isEmpty()) {
                item {
                    Text(
                        if (state.allBros.isEmpty()) "Catch some bros first, then recruit them here."
                        else "Empty squad. Recruit up to ${Squad.MAX_MEMBERS} bros.",
                        color = DexColors.TextMuted,
                    )
                }
            }
            items(state.members, key = { it.bro.id }) { entry ->
                BroRow(
                    entry.bro,
                    entry.stage,
                    Modifier.animateItem().clickable { onBroClick(entry.bro.id) },
                ) {
                    IconButton(onClick = { viewModel.removeMember(entry.bro) }) {
                        Icon(Icons.Filled.Close, "Remove ${entry.bro.name}", tint = DexColors.TextMuted)
                    }
                }
            }
            if (state.members.isNotEmpty()) {
                item { MatchupPanel(state.report) }
                item { RivalPanel(state, viewModel::selectRival) }
            }
        }
    }

    if (showPicker) {
        MemberPicker(
            all = state.allBros,
            initial = state.members.map { it.bro.id },
            onConfirm = { viewModel.setMembers(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
    if (showRename && squad != null) {
        var name by rememberSaveable { mutableStateOf(squad.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("RENAME SQUAD", style = PixelText.Label) },
            text = { OutlinedTextField(name, { name = it.take(24) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.rename(name); showRename = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("DISBAND SQUAD?", style = PixelText.Label) },
            text = { Text("Your bros stay in the Brodex; only the squad goes away.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; viewModel.delete(onBack) }) {
                    Text("Disband", color = DexColors.LedRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchupPanel(report: List<TypeMatchups.TypeReport>) {
    var selected by remember { mutableStateOf<BroType?>(null) }
    ScreenPanel(title = "Type matchups") {
        Text(
            "How your squad fares against each type. Tap one for the scouting report.",
            color = DexColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            report.forEach { r ->
                val verdictColor = when (r.verdict) {
                    TypeMatchups.Verdict.STRONG -> DexColors.LedGreen
                    TypeMatchups.Verdict.EVEN -> DexColors.Outline
                    TypeMatchups.Verdict.WEAK -> DexColors.LedRed
                }
                val shape = DexShape(4.dp)
                Column(
                    Modifier
                        .background(if (selected == r.opponent) r.opponent.color.copy(alpha = 0.25f) else Color.Transparent, shape)
                        .border(2.dp, verdictColor, shape)
                        .clickable { selected = if (selected == r.opponent) null else r.opponent }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(r.opponent.label.uppercase(), style = PixelText.Tiny, color = r.opponent.color)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (r.verdict) {
                            TypeMatchups.Verdict.STRONG -> "WIN"
                            TypeMatchups.Verdict.EVEN -> "EVEN"
                            TypeMatchups.Verdict.WEAK -> "RISK"
                        },
                        style = PixelText.Tiny,
                        color = verdictColor,
                    )
                }
            }
        }
        val detail = report.firstOrNull { it.opponent == selected }
        AnimatedVisibility(detail != null) {
            detail?.let { r ->
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("VS ${r.opponent.label.uppercase()}: ${r.verdict.label.uppercase()}", style = PixelText.Tiny, color = DexColors.Text)
                    r.counters.forEach { bro ->
                        val t = bro.types.first { r.opponent in TypeMatchups.beats(it) }
                        Text("+ ${bro.name} ${TypeMatchups.reason(t, r.opponent)}", color = DexColors.LedGreen, style = MaterialTheme.typography.bodySmall)
                    }
                    r.threatened.forEach { bro ->
                        val t = bro.types.first { it in TypeMatchups.beats(r.opponent) }
                        Text("- ${r.opponent.label} ${TypeMatchups.reason(r.opponent, t)} (${bro.name})", color = DexColors.LedRed, style = MaterialTheme.typography.bodySmall)
                    }
                    if (r.counters.isEmpty() && r.threatened.isEmpty()) {
                        Text("Nobody has an edge here. Pure vibes.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RivalPanel(state: SquadDetailUiState, onSelect: (Long?) -> Unit) {
    ScreenPanel(title = "Squad showdown") {
        if (state.otherSquads.isEmpty()) {
            Text("Make a second squad to stage a showdown.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            return@ScreenPanel
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.otherSquads.forEach { other ->
                FilterChip(
                    selected = state.rival?.squad?.id == other.squad.id,
                    onClick = { onSelect(if (state.rival?.squad?.id == other.squad.id) null else other.squad.id) },
                    label = { Text(other.squad.name) },
                )
            }
        }
        val clash = state.clash
        val rival = state.rival
        if (clash != null && rival != null) {
            val total = (clash.homeScore + clash.awayScore).coerceAtLeast(0.01f)
            val homeShare by animateFloatAsState(clash.homeScore / total, tween(800), label = "share")
            Spacer(Modifier.height(12.dp))
            Row {
                Text(state.squad?.name.orEmpty().uppercase(), style = PixelText.Tiny, color = DexColors.LedBlue, modifier = Modifier.weight(1f))
                Text(rival.squad.name.uppercase(), style = PixelText.Tiny, color = DexColors.DexRedLight)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).background(DexColors.DexRedLight)) {
                SegmentedBar(homeShare, DexColors.LedBlue, Modifier.fillMaxWidth().height(14.dp), segments = 24)
            }
            Spacer(Modifier.height(8.dp))
            val verdict = when {
                clash.homeScore > clash.awayScore * 1.1f -> "Your squad has the type edge."
                clash.awayScore > clash.homeScore * 1.1f -> "${rival.squad.name} has the type edge. Recruit wisely."
                else -> "Dead even. It comes down to who brings snacks."
            }
            Text(verdict, color = DexColors.Text, style = MaterialTheme.typography.bodyMedium)
            clash.highlight?.let {
                Spacer(Modifier.height(4.dp))
                Text("Key matchup: $it", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        } else if (rival != null) {
            Spacer(Modifier.height(8.dp))
            Text("${rival.squad.name} has no members yet.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MemberPicker(
    all: List<DexEntry>,
    initial: List<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val chosen = remember { mutableStateListOf<Long>().apply { addAll(initial) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PICK YOUR PARTY ${chosen.size}/${Squad.MAX_MEMBERS}", style = PixelText.Tiny) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(all, key = { it.bro.id }) { entry ->
                    val checked = entry.bro.id in chosen
                    val enabled = checked || chosen.size < Squad.MAX_MEMBERS
                    BroRow(entry.bro, entry.stage, Modifier.clickable(enabled = enabled) {
                        if (checked) chosen.remove(entry.bro.id) else chosen.add(entry.bro.id)
                    }) {
                        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen.toList()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
