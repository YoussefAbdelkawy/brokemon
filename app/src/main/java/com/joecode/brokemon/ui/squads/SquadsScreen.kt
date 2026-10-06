package com.joecode.brokemon.ui.squads

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.Squad
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.EmptyState
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

@Composable
fun SquadsScreen(
    onBack: () -> Unit,
    onSquadClick: (Long) -> Unit,
    viewModel: SquadsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }

    DexScaffold(
        title = "Squads",
        onBack = onBack,
        floatingActionButton = {
            if (state.squads.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = DexColors.DexRed,
                    contentColor = DexColors.Text,
                    shape = DexShape(8.dp),
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("NEW SQUAD", style = PixelText.Tiny) },
                )
            }
        },
    ) { padding ->
        if (!state.isLoading && state.squads.isEmpty()) {
            EmptyState(
                title = "No squads yet",
                body = "Group up to ${Squad.MAX_MEMBERS} bros into a squad and see how their types match up.",
                modifier = Modifier.padding(padding),
                action = { PixelButton("Form a squad", { showCreate = true }) },
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.squads, key = { it.squad.id }) { summary ->
                    SquadCard(summary, Modifier.animateItem()) { onSquadClick(summary.squad.id) }
                }
            }
        }
    }

    if (showCreate) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("NAME YOUR SQUAD", style = PixelText.Label) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("e.g. The Group Chat") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createSquad(name) { id -> showCreate = false; onSquadClick(id) } },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SquadCard(summary: SquadSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = DexShape(8.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(summary.squad.name.uppercase(), style = PixelText.Label, color = DexColors.Text, modifier = Modifier.weight(1f))
            Text("${summary.members.size}/${Squad.MAX_MEMBERS}", style = PixelText.Tiny, color = DexColors.TextMuted)
        }
        Spacer(Modifier.height(12.dp))
        val shown = 7
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(minOf(shown, maxOf(summary.members.size, 1))) { i ->
                val entry = summary.members.getOrNull(i)
                Box(
                    Modifier
                        .size(38.dp)
                        .background(DexColors.Screen, DexShape(3.dp))
                        .border(1.dp, DexColors.ScreenBorder, DexShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (entry != null) {
                        BroSprite(entry.bro, entry.stage.ordinal, Modifier.size(34.dp))
                    }
                }
            }
            if (summary.members.size > shown) {
                Text("+${summary.members.size - shown}", style = PixelText.Label, color = DexColors.TextMuted)
            }
        }
    }
}
