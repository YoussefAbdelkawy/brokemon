package com.joecode.brokemon.ui.battle

import com.joecode.brokemon.ui.theme.Spacing
import com.joecode.brokemon.ui.components.PixelChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.CustomMoveSpec
import com.joecode.brokemon.domain.Evolution
import com.joecode.brokemon.domain.battle.BattleMath
import com.joecode.brokemon.domain.battle.BattleMove
import com.joecode.brokemon.domain.battle.MoveCatalog
import com.joecode.brokemon.domain.battle.MoveTemplate
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.icon
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

/**
 * Pick the 4 moves a bro brings to battle, from everything their friendship
 * level has unlocked, or make a custom signature move from a fixed template.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovesSheet(bro: Bro, onSave: (equipped: List<String>, customs: List<CustomMoveSpec>) -> Unit, onDismiss: () -> Unit) {
    val score = Evolution.score(bro)
    var customs by remember { mutableStateOf(bro.resolvedBattle.customs) }
    val preview = bro.copy(battle = bro.resolvedBattle.copy(customs = customs))
    val available = MoveCatalog.available(preview, score)
    var equipped by remember { mutableStateOf(MoveCatalog.equipped(bro, score).map { it.key }) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(bro.primaryType) }
    var newTemplate by remember { mutableStateOf(MoveTemplate.STRIKE) }
    val locked = MoveCatalog.catalog.filter { it.unlockScore > score }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = DexColors.Surface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("${bro.name.uppercase()}'S MOVES", style = PixelText.Header, color = DexColors.Text)
            Text(
                "Lv.${BattleMath.level(score)} · Equip up to 4. More unlock as your friendship grows.",
                color = DexColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("EQUIPPED ${equipped.size}/4", style = PixelText.Tiny, color = DexColors.LedYellow)
            available.forEach { move ->
                val on = move.key in equipped
                MoveRow(move, on) {
                    equipped = when {
                        on && equipped.size > 1 -> equipped - move.key
                        on -> equipped
                        equipped.size < 4 -> equipped + move.key
                        else -> equipped
                    }
                }
            }
            if (locked.isNotEmpty()) {
                Text("LOCKED (FRIENDSHIP)", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.padding(top = Spacing.sm))
                locked.forEach { move ->
                    Text(
                        "${move.name} · unlocks at Lv.${BattleMath.level(move.unlockScore)}",
                        color = DexColors.Outline,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("NEW SIGNATURE MOVE", style = PixelText.Label, color = DexColors.DexRedLight)
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it.take(24) },
                label = { Text("Move name") },
                placeholder = { Text("Deadline Panic") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("TYPE", style = PixelText.Tiny, color = DexColors.TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bro.types.forEach { t -> Chip(t.label, newType == t, t.color) { newType = t } }
            }
            Text("TEMPLATE (FIXED POWER, SO IT STAYS FAIR)", style = PixelText.Tiny, color = DexColors.TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MoveTemplate.entries.forEach { t -> Chip(t.label, newTemplate == t, DexColors.LedBlue) { newTemplate = t } }
            }
            Text(
                "${newTemplate.blurb} Power ${newTemplate.power}, accuracy ${newTemplate.accuracy}%, ${newTemplate.energy} energy.",
                color = DexColors.ScreenText,
                style = MaterialTheme.typography.bodySmall,
            )
            PixelButton(
                "Add move",
                onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty() && customs.none { it.name.equals(name, true) }) {
                        customs = customs + CustomMoveSpec(name, newType.name, newTemplate.name)
                        newName = ""
                    }
                },
                enabled = newName.isNotBlank(),
                color = DexColors.SurfaceHigh,
                modifier = Modifier.fillMaxWidth(),
            )
            PixelButton("Save moves", { onSave(equipped, customs) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    PixelChip(label, selected, onClick, color = color)
}

@Composable
fun MoveRow(move: BattleMove, selected: Boolean, onClick: () -> Unit) {
    val shape = DexShape(5.dp)
    val color = move.type?.color ?: DexColors.TextMuted
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = "${move.name}, ${move.type?.label ?: "Bro"} type, power ${move.power}, accuracy ${move.accuracy}"
            }
            .background(if (selected) color.copy(alpha = 0.18f) else DexColors.SurfaceHigh, shape)
            .border(2.dp, if (selected) color else DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(move.type.icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(move.name.uppercase(), style = PixelText.Tiny, color = DexColors.Text)
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    append(if (move.power > 0) "PWR ${move.power} · ACC ${move.accuracy}%" else "SUPPORT")
                    append(" · ${move.energy} ENERGY")
                    move.status?.let { append(" · ${it.label.uppercase()}") }
                },
                style = PixelText.Tiny,
                color = DexColors.TextMuted,
            )
        }
        if (selected) Text("ON", style = PixelText.Tiny, color = color)
    }
}

/** Type name for display, "Bro" for neutral moves. */
val BroType?.battleLabel: String get() = this?.label ?: "Bro"
