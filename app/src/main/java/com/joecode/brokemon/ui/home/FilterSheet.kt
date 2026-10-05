package com.joecode.brokemon.ui.home

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

/** Name search first; everything else lives behind the filter button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    filter: BroFilter,
    onFilterChanged: (BroFilter) -> Unit,
    onOpenFilters: () -> Unit,
    onClear: () -> Unit,
    shown: Int,
    total: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search by name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Filled.Close, "Clear search") }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DexColors.DexRed,
                    unfocusedBorderColor = DexColors.Outline,
                ),
            )
            Spacer(Modifier.size(8.dp))
            Box {
                IconButton(
                    onClick = onOpenFilters,
                    modifier = Modifier
                        .size(56.dp)
                        .background(DexColors.Surface, CutCornerShape(6.dp))
                        .border(2.dp, if (filter.activeCount > 0) DexColors.LedYellow else DexColors.Outline, CutCornerShape(6.dp)),
                ) { Icon(Icons.Filled.Tune, "Filters", tint = if (filter.activeCount > 0) DexColors.LedYellow else DexColors.Text) }
                if (filter.activeCount > 0) {
                    Text(
                        filter.activeCount.toString(),
                        style = PixelText.Tiny,
                        color = Color(0xFF101014),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(DexColors.LedYellow, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                    )
                }
            }
        }
        // Active filters as removable chips, so nothing is hidden behind the sheet.
        if (filter.activeCount > 0) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                filter.types.forEach { t -> ActiveChip(t.label) { onFilterChanged(filter.copy(types = filter.types - t)) } }
                filter.rarities.forEach { r -> ActiveChip(r.label) { onFilterChanged(filter.copy(rarities = filter.rarities - r)) } }
                filter.stages.forEach { s -> ActiveChip(s.title) { onFilterChanged(filter.copy(stages = filter.stages - s)) } }
                if (filter.shinyOnly) ActiveChip("Shiny") { onFilterChanged(filter.copy(shinyOnly = false)) }
                if (filter.limitedOnly) ActiveChip("Limited") { onFilterChanged(filter.copy(limitedOnly = false)) }
                if (filter.tradeableOnly) ActiveChip("Tradeable") { onFilterChanged(filter.copy(tradeableOnly = false)) }
                if (filter.sort != SortOrder.DEX) ActiveChip("Sort: ${filter.sort.label}") { onFilterChanged(filter.copy(sort = SortOrder.DEX)) }
                TextButton(onClick = onClear) { Text("CLEAR ALL", style = PixelText.Tiny, color = DexColors.LedYellow) }
            }
        }
        if (query.isNotBlank() || filter.activeCount > 0) {
            Text("SHOWING $shown OF $total", style = PixelText.Tiny, color = DexColors.TextMuted)
        }
    }
}

@Composable
private fun ActiveChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.Close, "Remove $label", modifier = Modifier.size(16.dp)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    filter: BroFilter,
    onFilterChanged: (BroFilter) -> Unit,
    onClear: () -> Unit,
    resultCount: Int,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DexColors.Surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FILTER & SORT", style = PixelText.Header, color = DexColors.Text, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("RESET", style = PixelText.Tiny, color = DexColors.LedYellow) }
            }

            Section("Sort by")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortOrder.entries.forEach { order ->
                    Choice(order.label, filter.sort == order) { onFilterChanged(filter.copy(sort = order)) }
                }
            }

            Section("Rarity")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Rarity.entries.forEach { r ->
                    Choice(r.label, r in filter.rarities, r.color) { onFilterChanged(filter.copy(rarities = filter.rarities.toggle(r))) }
                }
            }

            Section("Bond level")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EvolutionStage.entries.forEach { s ->
                    Choice(s.title, s in filter.stages) { onFilterChanged(filter.copy(stages = filter.stages.toggle(s))) }
                }
            }

            Section("Special")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("Shiny", filter.shinyOnly, DexColors.Gold) { onFilterChanged(filter.copy(shinyOnly = !filter.shinyOnly)) }
                Choice("Limited frame", filter.limitedOnly) { onFilterChanged(filter.copy(limitedOnly = !filter.limitedOnly)) }
                Choice("Tradeable", filter.tradeableOnly) { onFilterChanged(filter.copy(tradeableOnly = !filter.tradeableOnly)) }
            }

            Section("Types")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BroType.entries.forEach { t ->
                    val selected = t in filter.types
                    Text(
                        t.label.uppercase(),
                        style = PixelText.Tiny,
                        color = if (selected) Color(0xFF101014) else t.color,
                        modifier = Modifier
                            .background(if (selected) t.color else Color.Transparent, CutCornerShape(4.dp))
                            .border(1.dp, t.color, CutCornerShape(4.dp))
                            .clickable { onFilterChanged(filter.copy(types = filter.types.toggle(t))) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PixelButton(
                text = if (resultCount == 1) "Show 1 bro" else "Show $resultCount bros",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text.uppercase(), style = PixelText.Label, color = DexColors.DexRedLight)
}

@Composable
private fun Choice(label: String, selected: Boolean, accent: Color = DexColors.LedBlue, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.copy(alpha = 0.25f),
            selectedLabelColor = DexColors.Text,
        ),
    )
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
