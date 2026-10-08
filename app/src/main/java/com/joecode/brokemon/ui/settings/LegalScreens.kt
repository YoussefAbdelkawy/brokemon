package com.joecode.brokemon.ui.settings

import com.joecode.brokemon.ui.theme.Spacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors

/**
 * In-app copy of the privacy policy. Keep this in sync with docs/privacy-policy.md,
 * which is the version you host publicly and link from the Play Console.
 */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalPage(
        title = "Privacy",
        sections = listOf(
            "Summary" to "Brokemon is local-first. Everything you create stays on your device. We (the developer) never " +
                "receive, see or store any of it. There are no accounts, ads, analytics or trackers.",
            "What the app stores" to "Bro cards (names, types, stats, moves, rarity, dates, catch location text you type), " +
                "facts you add, squads, and photos/videos you capture or pick for the memory log. Media is copied into the " +
                "app's private storage, not your gallery.",
            "Where it lives" to "On this device, inside Brokemon's private app storage. Uninstalling the app deletes it.",
            "Backups" to "Settings → Backup & restore exports a .zip (photos included) to wherever you choose. Your bros and " +
                "settings, but not photos, are also part of your phone's Android backup to your own Google account, only when " +
                "that backup is end-to-end encrypted with your screen lock. The developer can't access either.",
            "Reminders and widget" to "Birthday and weekly check-in reminders are created on your device and can be turned off " +
                "in Settings. The Bro of the Day widget shows one bro on your home screen; remove it any time.",
            "Sharing images" to "\"Post\" makes a picture of a card or your Wrapped and opens the share menu. Only that image " +
                "is shared, and only if you choose an app. Locked cards can't be shared.",
            "Sharing" to "Data only leaves your phone when you choose to show a card's QR code. The QR holds the card's name, " +
                "types, stats, moves, rarity, shiny flag and avatar look. It never contains photos, videos, facts or dates. " +
                "Locked cards can't be shared at all.",
            "Microphone" to "Used only while you record a voice memory or voice line. Recordings stay in Brokemon's " +
                "private storage and are never included in QR codes.",
            "Nearby battles" to "Battles between two phones use Google Nearby Connections (Bluetooth / Wi-Fi, no internet). " +
                "Only a battle card is sent: names, looks, types, level, stats and 4 moves, plus your trainer name. Never photos, " +
                "voice, facts or dates. Location permission (old Android only) is required by Android for Bluetooth scanning; " +
                "Brokemon never reads or stores your location.",
            "Camera and photos" to "Brokemon uses your device's camera app and the system photo picker. It does not request " +
                "camera or storage permissions and can only see the specific items you capture or pick.",
            "QR scanning" to "Scanning uses Google Play services' code scanner, which runs in Google Play services rather than " +
                "in Brokemon. Google may process limited diagnostic information under Google's own privacy policy.",
            "Photos of friends" to "Only add photos, videos and details of people who are okay with it. You can delete any memory, " +
                "card or everything at once from Settings.",
            "Children" to "Brokemon is not directed at children under 13.",
            "Deleting your data" to "Delete a single memory, a single bro, or use Settings → Delete all data. Deletion is immediate and permanent.",
            "Contact" to "Questions? Contact the developer through the email listed on the Google Play store page.",
        ),
        onBack = onBack,
    )
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    LegalPage(
        title = "Licenses",
        sections = listOf(
            "Press Start 2P" to "Copyright 2012 The Press Start 2P Project Authors (cody@zone38.net), with Reserved Font Name " +
                "\"Press Start 2P\". Licensed under the SIL Open Font License, Version 1.1 (scripts.sil.org/OFL).",
            "ZXing" to "QR code generation by ZXing (\"Zebra Crossing\"). Licensed under the Apache License 2.0.",
            "Gson" to "Copyright Google LLC. Licensed under the Apache License 2.0.",
            "AndroidX & Jetpack Compose" to "Copyright The Android Open Source Project. Licensed under the Apache License 2.0.",
            "Google Play services code scanner" to "Subject to the Google APIs Terms of Service.",
            "Original art" to "All sprites, the Bro Cube, icons and UI art in Brokemon are original and procedurally generated. " +
                "Brokemon is not affiliated with or endorsed by any game company.",
        ),
        onBack = onBack,
    )
}

@Composable
private fun LegalPage(title: String, sections: List<Pair<String, String>>, onBack: () -> Unit) {
    DexScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            sections.forEach { (heading, body) ->
                ScreenPanel(title = heading) {
                    Text(body, color = DexColors.Text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
