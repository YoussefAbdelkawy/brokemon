package com.joecode.brokemon.ui.trainer

import com.joecode.brokemon.ui.theme.Spacing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.data.model.Bro
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.data.model.BroType
import com.joecode.brokemon.data.model.Trainer
import com.joecode.brokemon.ui.catchbro.TypeChoice
import com.joecode.brokemon.ui.components.AvatarBuilder
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.color
import kotlin.random.Random

/**
 * Build-your-own Trainer Card: name, pixel character, your "type" and a motto.
 * Used by the professor intro and by "Edit Trainer Card".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerEditor(
    initial: Trainer?,
    saveLabel: String,
    onSave: (Trainer) -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var motto by rememberSaveable { mutableStateOf(initial?.motto.orEmpty()) }
    var typeName by rememberSaveable { mutableStateOf(initial?.type1 ?: BroType.MAIN_CHARACTER.name) }
    var lookValues by rememberSaveable {
        mutableStateOf((initial?.look ?: BroLook.random(Random.nextLong()).copy(hat = 0)).toList().toIntArray())
    }
    val look = BroLook.fromList(lookValues.toList())
    val type = BroType.from(typeName) ?: BroType.MAIN_CHARACTER
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "bobY",
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DexColors.DexRed,
        unfocusedBorderColor = DexColors.Outline,
        focusedLabelColor = DexColors.DexRedLight,
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        ScreenPanel(title = "This is you") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(124.dp)
                        .background(Brush.radialGradient(listOf(type.color.copy(alpha = 0.4f), Color.Transparent))),
                    contentAlignment = Alignment.Center,
                ) {
                    BroSprite(look, 0, false, Modifier.size(116.dp).graphicsLayer { translationY = bob })
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { "YOU" }.uppercase(), style = PixelText.Header, color = DexColors.ScreenText)
                    Spacer(Modifier.height(6.dp))
                    Text("TRAINER LV.1", style = PixelText.Tiny, color = DexColors.LedYellow)
                    TextButton(onClick = { lookValues = BroLook.random(Random.nextLong()).copy(hat = 0).toList().toIntArray() }) {
                        Icon(Icons.Filled.Casino, contentDescription = null, tint = DexColors.LedYellow)
                        Spacer(Modifier.width(6.dp))
                        Text("RANDOM", style = PixelText.Tiny, color = DexColors.LedYellow)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AvatarBuilder(look = look, onLookChange = { lookValues = it.toList().toIntArray() })
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(Trainer.MAX_NAME) },
            label = { Text("Your trainer name") },
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            supportingText = { Text("${name.length}/${Trainer.MAX_NAME}") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("WHAT TYPE OF BRO ARE YOU?", style = PixelText.Label, color = DexColors.TextMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BroType.entries.forEach { t -> TypeChoice(t, selected = t == type, enabled = true) { typeName = t.name } }
        }
        Text(type.blurb, color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = motto,
            onValueChange = { motto = it.take(Bro.MAX_FLAVOR) },
            label = { Text("Your motto (optional)") },
            placeholder = { Text("Replies in 3-5 business days.") },
            colors = fieldColors,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        PixelButton(
            text = saveLabel,
            enabled = name.isNotBlank(),
            onClick = {
                onSave(
                    Trainer(
                        name = name.trim(),
                        look = look,
                        type1 = type.name,
                        motto = motto.trim(),
                        frame = initial?.frame ?: com.joecode.brokemon.data.model.TrainerFrame.BASIC.name,
                        createdAt = initial?.createdAt ?: 0,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (onSkip != null) {
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("I'LL DO THIS LATER", style = PixelText.Tiny, color = DexColors.TextMuted)
            }
        }
    }
}
