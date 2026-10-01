package com.joecode.brokemon.ui.catchbro

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Rarity
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.AvatarBuilder
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.rarityGlow
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color

@Composable
fun CatchBroScreen(
    onBack: () -> Unit,
    onViewBro: (Long) -> Unit,
    viewModel: BroViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        DexScaffold(title = "Catch a bro", onBack = onBack) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AvatarPreview(state, onLookChanged = viewModel::onLookChanged, onRandomize = viewModel::randomizeLook)
                IdentitySection(state, viewModel::onNameChanged, viewModel::onLocationChanged)
                TypeSection(state, viewModel::onType1Selected, viewModel::onType2Selected)
                RaritySection(state.rarity, viewModel::onRarityChanged)
                StatsSection(state, viewModel::onStatChanged, viewModel::rerollStats)
                MovesSection(
                    state = state,
                    onMoveToggled = viewModel::onMoveToggled,
                    onCustomChanged = viewModel::onCustomMoveInputChanged,
                    onAddCustom = viewModel::addCustomMove,
                )
                PixelButton(
                    text = if (state.isSaving) "Throwing..." else "Throw the cube!",
                    onClick = viewModel::saveBro,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!state.canSave && !state.isSaving) {
                    Text(
                        "A bro needs a name and a type before you can catch them.",
                        color = DexColors.TextMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        state.caught?.let { result ->
            CatchOverlay(
                result = result,
                onViewBro = { onViewBro(result.id) },
                onDone = onBack,
            )
        }
    }
}

@Composable
private fun AvatarPreview(state: BroState, onLookChanged: (BroLook) -> Unit, onRandomize: () -> Unit) {
    val bob = rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "bobY",
    )
    ScreenPanel(title = "Character", modifier = Modifier.rarityGlow(state.rarity)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(132.dp)
                    .background(
                        Brush.radialGradient(
                            listOf((state.type1?.color ?: DexColors.ScreenBorder).copy(alpha = 0.35f), Color.Transparent),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BroSprite(
                    look = state.look,
                    stage = 0,
                    shiny = false,
                    modifier = Modifier
                        .size(124.dp)
                        .graphicsLayer { translationY = bob.value },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.name.ifBlank { "???" }.uppercase(),
                    style = PixelText.Header,
                    color = DexColors.ScreenText,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Make them look like the real thing.",
                    color = DexColors.TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRandomize) {
                    Icon(Icons.Filled.Casino, contentDescription = null, tint = DexColors.LedYellow)
                    Spacer(Modifier.width(6.dp))
                    Text("RANDOM", style = PixelText.Tiny, color = DexColors.LedYellow)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AvatarBuilder(look = state.look, onLookChange = onLookChanged)
    }
}

@Composable
private fun IdentitySection(
    state: BroState,
    onNameChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DexColors.DexRed,
        unfocusedBorderColor = DexColors.Outline,
        focusedLabelColor = DexColors.DexRedLight,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Identity")
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            label = { Text("Bro name") },
            singleLine = true,
            colors = colors,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            supportingText = { Text("${state.name.length}/${BroState.MAX_NAME}") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.catchLocation,
            onValueChange = onLocationChanged,
            label = { Text("Where'd you catch them? (optional)") },
            singleLine = true,
            colors = colors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeSection(
    state: BroState,
    onType1Selected: (BroType) -> Unit,
    onType2Selected: (BroType?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Primary type")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BroType.entries.forEach { type ->
                TypeChoice(type, selected = state.type1 == type, enabled = true) { onType1Selected(type) }
            }
        }
        state.type1?.let { Text(it.blurb, color = DexColors.TextMuted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
        SectionTitle("Second type (optional)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BroType.entries.forEach { type ->
                TypeChoice(
                    type,
                    selected = state.type2 == type,
                    enabled = state.type1 != null && state.type1 != type,
                ) { onType2Selected(type) }
            }
        }
    }
}

@Composable
private fun TypeChoice(type: BroType, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = CutCornerShape(4.dp)
    val alpha = if (enabled) 1f else 0.3f
    Row(
        Modifier
            .graphicsLayer { this.alpha = alpha }
            .background(if (selected) type.color else Color.Transparent, shape)
            .border(2.dp, type.color, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF101014), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            type.label.uppercase(),
            style = PixelText.Tiny,
            color = if (selected) Color(0xFF101014) else type.color,
        )
    }
}

@Composable
private fun RaritySection(rarity: Rarity, onRarityChanged: (Rarity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Rarity")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Rarity.entries.forEachIndexed { i, r ->
                SegmentedButton(
                    selected = rarity == r,
                    onClick = { onRarityChanged(r) },
                    shape = SegmentedButtonDefaults.itemShape(i, Rarity.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = r.color.copy(alpha = 0.25f),
                        activeContentColor = r.color,
                        activeBorderColor = r.color,
                    ),
                ) {
                    Text(r.label.uppercase(), style = PixelText.Tiny)
                }
            }
        }
        Text(
            when (rarity) {
                Rarity.COMMON -> "A solid everyday bro."
                Rarity.RARE -> "Hard to find, harder to replace."
                Rarity.LEGENDARY -> "Once-in-a-lifetime. Choose wisely."
            },
            color = DexColors.TextMuted,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatsSection(state: BroState, onStatChanged: (Int, Int) -> Unit, onReroll: () -> Unit) {
    val color = (state.type1 ?: BroType.CHILL_GUY).color
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Base stats", Modifier.weight(1f))
            TextButton(onClick = onReroll) {
                Icon(Icons.Filled.Casino, contentDescription = null, tint = DexColors.LedYellow)
                Spacer(Modifier.width(6.dp))
                Text("ROLL", style = PixelText.Tiny, color = DexColors.LedYellow)
            }
        }
        state.stats.asList().forEachIndexed { i, (info, value) ->
            Text(
                info.blurb,
                color = DexColors.TextMuted,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.label, style = PixelText.Tiny, color = DexColors.ScreenText, modifier = Modifier.width(64.dp))
                Text(value.toString(), style = PixelText.Tiny, color = DexColors.Text, modifier = Modifier.width(32.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onStatChanged(i, it.toInt()) },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color,
                        inactiveTrackColor = DexColors.SurfaceHigh,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            "TOTAL ${state.stats.total}",
            style = PixelText.Tiny,
            color = DexColors.TextMuted,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovesSection(
    state: BroState,
    onMoveToggled: (String) -> Unit,
    onCustomChanged: (String) -> Unit,
    onAddCustom: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Signature moves ${state.selectedMoves.size}/${BroState.MAX_MOVES}")
        val customMoves = state.selectedMoves.filterNot { it in PresetMoves.all }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (customMoves + PresetMoves.all).forEach { move ->
                val selected = move in state.selectedMoves
                FilterChip(
                    selected = selected,
                    onClick = { onMoveToggled(move) },
                    enabled = selected || !state.movesFull,
                    label = { Text(move) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DexColors.DexRedDark,
                        selectedLabelColor = DexColors.Text,
                        selectedLeadingIconColor = DexColors.Text,
                    ),
                )
            }
        }
        OutlinedTextField(
            value = state.customMoveInput,
            onValueChange = onCustomChanged,
            label = { Text("Custom move") },
            singleLine = true,
            enabled = !state.movesFull,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onAddCustom() }),
            trailingIcon = {
                IconButton(onClick = onAddCustom, enabled = state.customMoveInput.isNotBlank() && !state.movesFull) {
                    Icon(Icons.Filled.Add, contentDescription = "Add move")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DexColors.DexRed,
                unfocusedBorderColor = DexColors.Outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = PixelText.Label, color = DexColors.DexRedLight, modifier = modifier)
}
