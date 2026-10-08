package com.joecode.brokemon.ui.battle

import com.joecode.brokemon.ui.theme.Spacing
import com.joecode.brokemon.ui.components.PixelChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import com.joecode.brokemon.ui.components.PixelAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.BattleRecord
import com.joecode.brokemon.domain.battle.Brackets
import com.joecode.brokemon.domain.battle.DailyBattleQuest
import com.joecode.brokemon.domain.battle.TournamentFormat
import com.joecode.brokemon.domain.battle.TournamentMatch
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.icon
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import java.text.DateFormat
import java.util.Date

/** Battle home: today's quest, how to play, tournaments and records. */
@Composable
fun BattleHubScreen(
    onBack: (() -> Unit)?,
    onPlay: (BattleKind) -> Unit,
    onTournaments: () -> Unit,
    onRecords: () -> Unit,
    viewModel: BattleRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val records by viewModel.state.collectAsStateWithLifecycle()
    val daily = DailyBattleQuest.typeFor()
    DexScaffold(title = "Bro Battles", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ScreenPanel(title = "Daily battle quest") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(daily.icon, null, tint = daily.color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("WIN WITH A ${daily.label.uppercase()}-TYPE BRO", style = PixelText.Tiny, color = DexColors.ScreenText)
                        Spacer(Modifier.height(4.dp))
                        Text("+${DailyBattleQuest.XP} trainer XP. New quest every day.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ScreenPanel(title = "Season ${records.seasonLabel}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("W", records.seasonWins, DexColors.LedGreen)
                    Stat("L", records.seasonLosses, DexColors.LedRed)
                    Stat("STREAK", records.currentStreak, DexColors.LedYellow)
                    Stat("TROPHIES", records.trophies, DexColors.Gold)
                }
            }
            ModeButton("Practice vs CPU", "Offline. Learn battling with no one around.", Icons.Filled.SmartToy, DexColors.LedBlue) { onPlay(BattleKind.CPU) }
            ModeButton("Same phone", "Pass and play with a friend next to you.", Icons.Filled.PhoneAndroid, DexColors.LedGreen) { onPlay(BattleKind.LOCAL) }
            ModeButton("Host a Nearby battle", "Phone to phone over Bluetooth / Wi-Fi. Free, no internet. Get a code.", Icons.Filled.Wifi, DexColors.DexRedLight) { onPlay(BattleKind.HOST) }
            ModeButton("Join with a code", "Your friend hosts, you enter their 6-letter code.", Icons.Filled.Login, DexColors.Epic) { onPlay(BattleKind.JOIN) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PixelButton(
                    "Tournaments",
                    onTournaments,
                    Modifier.weight(1f),
                    color = DexColors.SurfaceHigh,
                    stacked = true,
                    leading = { Icon(Icons.Filled.EmojiEvents, null, tint = DexColors.Gold, modifier = Modifier.size(20.dp)) },
                )
                PixelButton(
                    "Records",
                    onRecords,
                    Modifier.weight(1f),
                    color = DexColors.SurfaceHigh,
                    stacked = true,
                    leading = { Icon(Icons.Filled.Leaderboard, null, tint = DexColors.LedBlue, modifier = Modifier.size(20.dp)) },
                )
            }
            Text(
                "Every battle is free and works offline. Nothing goes to a server.",
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = PixelText.Header, color = color)
        Spacer(Modifier.height(4.dp))
        Text(label, style = PixelText.Tiny, color = DexColors.TextMuted)
    }
}

@Composable
private fun ModeButton(title: String, body: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val shape = DexShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, color.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = PixelText.Label, color = DexColors.Text)
            Spacer(Modifier.height(4.dp))
            Text(body, color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// --- Records & private leaderboard -----------------------------------------------------

@Composable
fun BattleRecordsScreen(
    onBack: () -> Unit,
    onReplay: (Long) -> Unit,
    viewModel: BattleRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    DexScaffold(title = "Battle records", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenPanel(title = "All time") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("WINS", s.wins, DexColors.LedGreen)
                        Stat("LOSSES", s.losses, DexColors.LedRed)
                        Stat("BEST STREAK", s.bestStreak, DexColors.LedYellow)
                        Stat("TROPHIES", s.trophies, DexColors.Gold)
                    }
                    s.mvpName?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("MVP BRO: ${it.uppercase()} (${s.mvpCount} wins)", style = PixelText.Tiny, color = DexColors.ScreenText)
                    }
                }
            }
            item { Text("YOU VS YOUR FRIENDS", style = PixelText.Label, color = DexColors.DexRedLight) }
            if (s.rivals.isEmpty()) item { Text("No battles yet. Practice vs CPU or host a Nearby battle.", color = DexColors.TextMuted) }
            items(s.rivals, key = { "r" + it.name }) { r ->
                Row(
                    Modifier.fillMaxWidth().background(DexColors.Surface, DexShape(6.dp)).padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(r.name, color = DexColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${r.wins}W ${r.losses}L", style = PixelText.Tiny, color = DexColors.ScreenText)
                    if (r.streak > 1) Text("  🔥${r.streak}", style = PixelText.Tiny, color = DexColors.LedYellow)
                }
            }
            item { Text("HISTORY & REPLAYS", style = PixelText.Label, color = DexColors.DexRedLight, modifier = Modifier.padding(top = Spacing.sm)) }
            items(s.records, key = { it.id }) { r -> RecordRow(r) { onReplay(r.id) } }
            item {
                Text(
                    "Records stay on this phone. The season table resets each month.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun RecordRow(r: BattleRecord, onReplay: () -> Unit) {
    val result = when (r.won) {
        true -> "WIN"
        false -> "LOSS"
        null -> if (r.mode == "LOCAL") "PLAYED" else "DRAW"
    }
    Row(
        Modifier.fillMaxWidth().background(DexColors.Surface, DexShape(6.dp)).padding(start = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text("$result vs ${r.opponentName}", style = PixelText.Tiny, color = if (r.won == true) DexColors.LedGreen else DexColors.Text)
            Spacer(Modifier.height(4.dp))
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.date)) + " · " + r.mode.lowercase() + (r.mvpName.takeIf { it.isNotBlank() }?.let { " · MVP $it" } ?: ""),
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onReplay) { Icon(Icons.Filled.PlayArrow, "Watch replay", tint = DexColors.LedBlue) }
    }
}

// --- Tournaments -------------------------------------------------------------------------

@Composable
fun TournamentsScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: TournamentsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val list by viewModel.tournaments.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    DexScaffold(
        title = "Tournaments",
        onBack = onBack,
        actions = { IconButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "New tournament", tint = DexColors.Text) } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "Start a cup for a squad or a regional dex. The bracket lives on this phone: play matches here (same phone), on Nearby, or record who won.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (list.isEmpty()) item { PixelButton("New tournament", { creating = true }, Modifier.fillMaxWidth()) }
            items(list, key = { it.id }) { t ->
                Row(
                    Modifier.fillMaxWidth().background(DexColors.Surface, DexShape(6.dp)).border(2.dp, if (t.champion != null) DexColors.Gold else DexColors.Outline, DexShape(6.dp)).clickable { onOpen(t.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = if (t.champion != null) DexColors.Gold else DexColors.TextMuted)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name.uppercase(), style = PixelText.Label, color = DexColors.Text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${t.players.size} players · " + (TournamentFormat.entries.firstOrNull { it.name == t.format }?.label ?: "") +
                                (t.champion?.let { " · Champion: $it" } ?: " · CODE ${t.code}"),
                            color = DexColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
    if (creating) CreateTournamentDialog(viewModel, onDismiss = { creating = false }, onCreated = { creating = false; onOpen(it) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateTournamentDialog(viewModel: TournamentsViewModel, onDismiss: () -> Unit, onCreated: (Long) -> Unit) {
    val dexes by viewModel.dexes.collectAsStateWithLifecycle()
    val squads by viewModel.squads.collectAsStateWithLifecycle()
    val bros by viewModel.bros.collectAsStateWithLifecycle()
    val refs by viewModel.dexRefs.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("Bro Cup") }
    var format by remember { mutableStateOf(TournamentFormat.BRACKET) }
    val players = remember { mutableStateListOf<String>() }
    var newPlayer by remember { mutableStateOf("") }
    var dexId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { if (players.isEmpty()) players += viewModel.trainerName() }
    PixelAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW TOURNAMENT", style = PixelText.Label) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, label = { Text("Name") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentFormat.entries.forEach { f -> SmallChip(f.label, f == format) { format = f } }
                }
                Text("PLAYERS FROM A DEX OR SQUAD", style = PixelText.Tiny, color = DexColors.TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dexes.forEach { d ->
                        SmallChip(d.name, dexId == d.id) {
                            dexId = d.id
                            val names = refs.filter { it.dexId == d.id }.mapNotNull { r -> bros.firstOrNull { it.id == r.broId }?.name }
                            names.filterNot { it in players }.forEach { players += it }
                        }
                    }
                    squads.forEach { sq ->
                        SmallChip(sq.name, false) {
                            sq.memberIds.mapNotNull { id -> bros.firstOrNull { it.id == id }?.name }.filterNot { it in players }.forEach { players += it }
                        }
                    }
                }
                players.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. $p", modifier = Modifier.weight(1f))
                        IconButton(onClick = { players.removeAt(i) }) { Icon(Icons.Filled.Delete, "Remove $p", tint = DexColors.TextMuted) }
                    }
                }
                OutlinedTextField(
                    value = newPlayer,
                    onValueChange = { newPlayer = it.take(16) },
                    label = { Text("Add a player") },
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { if (newPlayer.isNotBlank() && newPlayer !in players) players += newPlayer.trim(); newPlayer = "" }) { Text("Add") }
                    },
                )
                Text("Players are your friends (one phone each, or play on this phone).", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.create(name, format, players.toList(), dexId, onCreated) }, enabled = players.size >= 2) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    PixelChip(label, selected, onClick)
}

@Composable
fun TournamentScreen(
    onBack: () -> Unit,
    onPlayHere: (TournamentMatch) -> Unit,
    onPlayNearby: (TournamentMatch) -> Unit,
    viewModel: TournamentsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val view by viewModel.current.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<TournamentMatch?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val v = view
    DexScaffold(
        title = v?.tournament?.name ?: "Tournament",
        onBack = onBack,
        actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete tournament", tint = DexColors.Text) } },
    ) { padding ->
        if (v == null) return@DexScaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ScreenPanel(title = "${v.format.label} · code ${v.tournament.code}") {
                Text(
                    v.tournament.champion?.let { "🏆 CHAMPION: ${it.uppercase()}" } ?: "${v.tournament.players.size} players. Tap a ready match to play it.",
                    style = if (v.tournament.champion != null) PixelText.Label else MaterialTheme.typography.bodySmall,
                    color = if (v.tournament.champion != null) DexColors.Gold else DexColors.ScreenText,
                )
            }
            if (v.format == TournamentFormat.BRACKET) {
                // The bracket, round by round, left to right.
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    (0 until Brackets.rounds(v.matches)).forEach { round ->
                        val inRound = v.matches.filter { it.round == round }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(170.dp)) {
                            val label = when (Brackets.rounds(v.matches) - round) {
                                1 -> "FINAL"
                                2 -> "SEMIS"
                                else -> "ROUND ${round + 1}"
                            }
                            Text(label, style = PixelText.Tiny, color = DexColors.TextMuted)
                            inRound.forEach { m ->
                                Spacer(Modifier.height((((1 shl round) - 1) * 18).dp))
                                MatchCard(m) { if (m.ready) picking = m }
                            }
                        }
                    }
                }
            } else {
                Text("STANDINGS", style = PixelText.Label, color = DexColors.DexRedLight)
                v.standings.forEachIndexed { i, st ->
                    Row(Modifier.fillMaxWidth().background(DexColors.Surface, DexShape(Spacing.xs)).padding(10.dp)) {
                        Text("${i + 1}. ${st.player}", modifier = Modifier.weight(1f), color = DexColors.Text)
                        Text("${st.wins}W ${st.losses}L", style = PixelText.Tiny, color = DexColors.ScreenText)
                    }
                }
                Text("MATCHES", style = PixelText.Label, color = DexColors.DexRedLight)
                v.matches.forEach { m -> MatchCard(m) { if (m.ready) picking = m } }
            }
        }
    }
    picking?.let { m ->
        PixelAlertDialog(
            onDismissRequest = { picking = null },
            title = { Text("${m.a} VS ${m.b}".uppercase(), style = PixelText.Label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Play it here (pass the phone), host it on Nearby (you play as ${m.a}), or record a result from another phone.", style = MaterialTheme.typography.bodySmall)
                    PixelButton("Play on this phone", { picking = null; onPlayHere(m) }, Modifier.fillMaxWidth())
                    PixelButton("Host on Nearby", { picking = null; onPlayNearby(m) }, Modifier.fillMaxWidth(), color = DexColors.SurfaceHigh)
                    Text("OR RECORD THE WINNER", style = PixelText.Tiny, color = DexColors.TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        listOfNotNull(m.a, m.b).forEach { p ->
                            TextButton(onClick = { viewModel.recordWinner(m, p); picking = null }) { Text(p) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = null }) { Text("Close") } },
        )
    }
    if (confirmDelete) {
        PixelAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("DELETE TOURNAMENT?", style = PixelText.Label) },
            text = { Text("The bracket is removed. Battle records stay.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) { Text("Delete", color = DexColors.LedRed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun MatchCard(m: TournamentMatch, onClick: () -> Unit) {
    val shape = DexShape(5.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, if (m.ready) DexColors.LedYellow else DexColors.Outline, shape)
            .clickable(enabled = m.ready, onClick = onClick)
            .padding(Spacing.sm),
    ) {
        listOf(m.a, m.b).forEach { p ->
            val won = p != null && m.winner == p
            Text(
                (p ?: if (m.isBye) "BYE" else "TBD").uppercase(),
                style = PixelText.Tiny,
                color = when {
                    won -> DexColors.Gold
                    m.winner != null -> DexColors.TextMuted
                    else -> DexColors.Text
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
        if (m.ready) Text("TAP TO PLAY", style = PixelText.Tiny, color = DexColors.LedYellow, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
    }
}
