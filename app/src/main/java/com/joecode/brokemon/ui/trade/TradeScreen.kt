package com.joecode.brokemon.ui.trade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.joecode.brokemon.domain.EvolutionStage
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.BroRow
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

@Composable
fun TradeScreen(
    onBack: () -> Unit,
    onShareBro: (Long) -> Unit,
    onOpenBro: (Long) -> Unit,
    /** Opened from the "Scan QR" app shortcut: jump straight into the scanner. */
    autoScan: Boolean = false,
    viewModel: TradeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    fun scan() {
        // Google's code scanner runs the camera UI inside Play services,
        // so this app never needs the CAMERA permission.
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options)
            .startScan()
            .addOnSuccessListener { viewModel.onScanned(it.rawValue) }
            .addOnFailureListener { viewModel.onScanFailed("Scanner unavailable. Make sure Google Play services is up to date.") }
    }

    var autoScanned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoScan) {
        if (autoScan && !autoScanned) {
            autoScanned = true
            scan()
        }
    }

    DexScaffold(title = "Trade", onBack = onBack, snackbarHostState = snackbar) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenPanel(title = "Receive") {
                    Text(
                        "Scan a friend's Brokemon QR to add their card to your dex. Only card data is shared, never photos.",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    PixelButton(
                        "Scan a card",
                        ::scan,
                        Modifier.fillMaxWidth(),
                        color = DexColors.LedBlue.copy(alpha = 0.85f),
                        leading = { Icon(Icons.Filled.QrCodeScanner, null, tint = DexColors.Text, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
            item {
                Text("SEND A CARD", style = PixelText.Label, color = DexColors.DexRedLight, modifier = Modifier.padding(top = 8.dp))
            }
            if (state.tradeable.isEmpty()) {
                item {
                    Text(
                        "No tradeable bros. Cards marked Locked stay private.",
                        color = DexColors.TextMuted,
                    )
                }
            }
            items(state.tradeable, key = { it.bro.id }) { entry ->
                BroRow(entry.bro, entry.stage, Modifier.clickable { onShareBro(entry.bro.id) }) {
                    Icon(Icons.Filled.ChevronRight, null, tint = DexColors.TextMuted)
                }
            }
        }
    }

    state.incoming?.let { bro ->
        AlertDialog(
            onDismissRequest = viewModel::declineIncoming,
            title = { Text("INCOMING CARD!", style = PixelText.Label, color = DexColors.LedYellow) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(true, enter = scaleIn() + fadeIn()) {
                        BroCard(bro = bro, stage = EvolutionStage.ROOKIE, modifier = Modifier.widthIn(max = 200.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Add ${bro.name} to your Brodex? Their bond starts fresh with you.",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptIncoming(onOpenBro) }) { Text("Add to dex") }
            },
            dismissButton = { TextButton(onClick = viewModel::declineIncoming) { Text("No thanks") } },
        )
    }
}
