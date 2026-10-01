package com.joecode.brokemon.ui.trade

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.share.QrBitmap
import com.joecode.brokemon.share.QrCodec
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroCard
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

@Composable
fun ShareBroScreen(
    broId: Long,
    onBack: () -> Unit,
    viewModel: TradeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entry = state.tradeable.firstOrNull { it.bro.id == broId }
    val locked = entry == null && state.allBros.any { it.id == broId }

    DexScaffold(title = "Share card", onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                entry != null -> {
                    val payload = remember(entry.bro) { QrCodec.encode(entry.bro) }
                    val qr = remember(payload) {
                        QrBitmap.render(payload, Color(0xFF0B0B10).toArgb(), Color.White.toArgb()).asImageBitmap()
                    }
                    val glow by rememberInfiniteTransition(label = "qr").animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                        label = "qrGlow",
                    )
                    ScreenPanel(title = "Transmitting", modifier = Modifier.widthIn(max = 420.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(DexColors.LedGreen.copy(alpha = 0.25f * glow), CutCornerShape(6.dp))
                                .padding(10.dp)
                                .background(Color.White, CutCornerShape(4.dp))
                                .padding(8.dp),
                        ) {
                            Image(
                                bitmap = qr,
                                contentDescription = "QR code for ${entry.bro.name}",
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.None,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Have your friend open Brokemon → Trade → Scan a card.",
                            color = DexColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BroCard(entry.bro, entry.stage, Modifier.widthIn(max = 220.dp))
                    Text(
                        "Shared: name, types, stats, moves, rarity, look. Never shared: photos, videos, facts, dates.",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                locked -> Text("This card is locked. Unlock it from its detail page to share.", color = DexColors.TextMuted)
                else -> Text("LOADING...", style = PixelText.Label, color = DexColors.TextMuted)
            }
        }
    }
}
