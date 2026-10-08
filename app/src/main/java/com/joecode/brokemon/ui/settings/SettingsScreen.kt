package com.joecode.brokemon.ui.settings

import com.joecode.brokemon.ui.theme.Spacing
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PrivacyTip
import com.joecode.brokemon.ui.components.PixelAlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import com.joecode.brokemon.data.EventClock
import com.joecode.brokemon.domain.SeasonEvent
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.detail.formatDate
import java.time.LocalDate
import com.joecode.brokemon.BuildConfigInfo
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var confirmWipe by rememberSaveable { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDebug = remember { context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::export)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) scope.launch { snackbar.showSnackbar("Notifications are off. You can turn them on in system settings.") }
    }
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    DexScaffold(title = "Settings", onBack = onBack, snackbarHostState = snackbar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
            ScreenPanel(title = "Backup & restore") {
                Text(
                    "Lost phone = lost bros, unless you back up. Export saves your whole Brodex, photos and videos " +
                        "included, as one .zip file. Keep it somewhere safe (Google Drive, your laptop).",
                    color = DexColors.ScreenText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    state.lastExport?.let { "Last export: ${formatDate(it)}" } ?: "Never exported yet.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Your bros and settings (not photos) are also included in your phone's encrypted Android backup.",
                    color = DexColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = DexColors.LedGreen)
                    Spacer(Modifier.height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelButton(
                        "Export",
                        { exportLauncher.launch("brodex-${LocalDate.now()}.zip") },
                        Modifier.weight(1f),
                        color = DexColors.LedGreen.copy(alpha = 0.75f),
                        enabled = !state.busy,
                    )
                    PixelButton(
                        "Restore",
                        { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        Modifier.weight(1f),
                        color = DexColors.SurfaceHigh,
                        enabled = !state.busy,
                    )
                }
            }
            ScreenPanel(title = "Reminders") {
                ReminderToggle(
                    title = "Birthdays",
                    body = "A heads-up on the day for any bro with a Birthday fact.",
                    checked = state.birthdayReminders,
                    onChange = { on -> viewModel.setBirthdayReminders(on); if (on) ensureNotificationPermission() },
                )
                ReminderToggle(
                    title = "Weekly check-in",
                    body = "Sunday evening nudge to check on a bro you haven't talked to in a while.",
                    checked = state.weeklyNudge,
                    onChange = { on -> viewModel.setWeeklyNudge(on); if (on) ensureNotificationPermission() },
                )
            }
            ScreenPanel(title = "Battle text speed") {
                Text("How fast battle messages type out.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf("Slow", "Normal", "Fast").forEachIndexed { i, label ->
                        val selected = state.textSpeed == i
                        Text(
                            label.uppercase(),
                            style = PixelText.Tiny,
                            color = if (selected) DexColors.OnBright else DexColors.ScreenText,
                            modifier = Modifier
                                .semantics { this.selected = selected }
                                .background(if (selected) DexColors.ScreenText else Color.Transparent, DexShape(3.dp))
                                .border(1.dp, DexColors.ScreenBorder, DexShape(3.dp))
                                .clickable { viewModel.setTextSpeed(i) }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        )
                    }
                }
            }
            if (isDebug) {
                ScreenPanel(title = "Debug: event preview") {
                    Text(
                        "Force a limited event to test frames. Release builds always use the real date.",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val options = listOf<Pair<String?, String>>(null to "Real date", EventClock.NONE to "No event") +
                            SeasonEvent.entries.map { it.name to it.label }
                        options.forEach { (value, label) ->
                            FilterChip(
                                selected = state.debugEvent == value,
                                onClick = { viewModel.setDebugEvent(value) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
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

    pendingRestore?.let { uri ->
        PixelAlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("RESTORE BACKUP?", style = PixelText.Label, color = DexColors.LedYellow) },
            text = { Text("This replaces everything currently in your Brodex with the backup. Export first if you want to keep what's here.") },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; viewModel.restore(uri) }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }

    if (confirmWipe) {
        var typed by rememberSaveable { mutableStateOf("") }
        PixelAlertDialog(
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
    val shape = DexShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, DexColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(14.dp))
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.Text, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DexColors.TextMuted)
    }
}

@Composable
private fun ReminderToggle(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = PixelText.Tiny, color = DexColors.Text)
            Spacer(Modifier.height(4.dp))
            Text(body, color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = DexColors.LedGreen.copy(alpha = 0.6f)),
        )
    }
}
