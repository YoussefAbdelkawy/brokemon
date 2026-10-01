package com.joecode.brokemon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.BuildConfigInfo
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var confirmWipe by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DexScaffold(title = "Settings", onBack = onBack, snackbarHostState = snackbar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenPanel(title = "Your data") {
                Text(
                    "Everything in Brokemon lives only on this phone: your bros, facts, photos and videos. " +
                        "No account, no cloud, no ads, no tracking. The only way a card leaves your phone is " +
                        "when you show its QR code to someone.",
                    color = DexColors.ScreenText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingsRow("Privacy policy", Icons.Filled.PrivacyTip, DexColors.LedBlue, onPrivacy)
            SettingsRow("Open-source licenses", Icons.AutoMirrored.Filled.Article, DexColors.LedGreen, onLicenses)
            SettingsRow("Delete all data", Icons.Filled.DeleteForever, DexColors.LedRed) { confirmWipe = true }
            Spacer(Modifier.height(12.dp))
            Text(
                "BROKEMON v${BuildConfigInfo.versionName(androidx.compose.ui.platform.LocalContext.current)}",
                style = PixelText.Tiny,
                color = DexColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                "Brokemon is a fan-made friendship tracker. Not affiliated with, endorsed or sponsored by any game company.",
                style = MaterialTheme.typography.bodySmall,
                color = DexColors.TextMuted,
            )
        }
    }

    if (confirmWipe) {
        var typed by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("WIPE THE BRODEX?", style = PixelText.Label, color = DexColors.LedRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This permanently deletes every bro, squad, fact, photo and video stored by Brokemon on this phone.")
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = typed.trim() == "DELETE",
                    onClick = {
                        confirmWipe = false
                        viewModel.wipeEverything { scope.launch { snackbar.showSnackbar("All Brokemon data deleted.") } }
                    },
                ) { Text("Delete everything", color = DexColors.LedRed) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsRow(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val shape = CutCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(14.dp))
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.Text, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DexColors.TextMuted)
    }
}
