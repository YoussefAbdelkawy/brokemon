package com.joecode.brokemon.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.RegionalDex
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

/** Name a regional dex ("Uni Dex", "Gym Dex"...). With [onDelete] it also offers deleting it. */
@Composable
fun DexNameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase(), style = PixelText.Label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(RegionalDex.MAX_NAME) },
                    label = { Text("Dex name") },
                    placeholder = { Text("Uni Dex") },
                    singleLine = true,
                )
                Text(
                    if (confirmDelete) "Delete this dex? Your bros stay in the National Dex." else "Each dex has its own numbers and counter.",
                    color = if (confirmDelete) DexColors.LedRed else DexColors.TextMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(if (confirmDelete) "Yes, delete" else "Delete", color = DexColors.LedRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** Checkbox list of bros. Returns the chosen ids. */
@Composable
fun BroPickerDialog(
    title: String,
    bros: List<Bro>,
    initial: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title.uppercase(), style = PixelText.Label) },
        text = {
            if (bros.isEmpty()) {
                Text("No bros yet. Catch one first.", color = DexColors.TextMuted)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(bros, key = { it.id }) { bro ->
                        val checked = bro.id in chosen
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { chosen = if (checked) chosen - bro.id else chosen + bro.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { chosen = if (it) chosen + bro.id else chosen - bro.id },
                                colors = CheckboxDefaults.colors(checkedColor = DexColors.DexRed),
                            )
                            Box(Modifier.size(36.dp)) { BroSprite(bro, 0, Modifier.size(36.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Text("${bro.dexNumber} ${bro.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen) }) { Text("Done (${chosen.size})") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
