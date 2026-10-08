package com.joecode.brokemon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.domain.ReleaseNote
import com.joecode.brokemon.ui.components.Dexy
import com.joecode.brokemon.ui.components.DexyMood
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.PixelDialog
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.theme.Spacing

/** The pixel "What's new" popup, shown once after an update and from Settings. */
@Composable
fun WhatsNewDialog(note: ReleaseNote, onDismiss: () -> Unit) {
    PixelDialog(
        onDismissRequest = onDismiss,
        title = "What's new in ${note.versionName}",
        buttons = { PixelButton("Nice!", onClick = onDismiss) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dexy(DexyMood.WOW, size = 56.dp)
            Spacer(Modifier.width(Spacing.md))
            Text(note.headline.uppercase(), style = PixelText.Label, color = DexColors.LedYellow)
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(
            Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            note.bullets.forEach { line ->
                Row {
                    Text("+", style = PixelText.Label, color = DexColors.LedGreen)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(line, color = DexColors.Text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
