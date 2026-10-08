package com.joecode.brokemon.ui.pack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.domain.Cosmetic
import com.joecode.brokemon.domain.CosmeticKind
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.PixelProgressBar
import com.joecode.brokemon.ui.components.pixelBox
import com.joecode.brokemon.ui.theme.Borders
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing

/** Everything the Daily Pack can give, owned ones lit up and the rest as locked silhouettes. */
@Composable
fun CollectionScreen(onBack: () -> Unit, viewModel: PackViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val total = Cosmetic.packable.size
    val have = Cosmetic.packable.count { it.id in state.owned }
    DexScaffold(title = "Collection", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text("$have OF $total COLLECTED", style = PixelText.Label, color = DexColors.LedYellow)
                    Spacer4()
                    PixelProgressBar(have / total.toFloat(), DexColors.LedYellow, Modifier.fillMaxWidth())
                    Spacer4()
                    Text("Cosmetics only. They never change stats or battles.", color = DexColors.TextMuted)
                }
            }
            CosmeticKind.entries.forEach { kind ->
                val items = Cosmetic.packable.filter { it.kind == kind }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(kind.label.uppercase() + "S", style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.padding(top = Spacing.sm))
                }
                items(items, key = { it.id }) { item ->
                    val owned = item.id in state.owned
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) { contentDescription = if (owned) "${item.label}, collected" else "Locked ${kind.label}" }
                            .pixelBox(DexColors.Surface, if (owned) DexColors.LedYellow else DexColors.Outline, Borders.normal, 2.dp)
                            .padding(Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) { CosmeticPreview(item, 56.dp, locked = !owned) }
                        Text(
                            if (owned) item.label.uppercase() else "???",
                            style = PixelText.Tiny,
                            color = if (owned) DexColors.Text else DexColors.TextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer4() = androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.xs))
