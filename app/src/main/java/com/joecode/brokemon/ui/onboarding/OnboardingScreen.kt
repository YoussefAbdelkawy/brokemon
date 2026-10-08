package com.joecode.brokemon.ui.onboarding

import com.joecode.brokemon.ui.theme.Spacing
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.BroLook
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.BroFullBody
import com.joecode.brokemon.ui.components.BroSprite
import com.joecode.brokemon.ui.components.Led
import com.joecode.brokemon.ui.components.LedCluster
import com.joecode.brokemon.ui.components.LensLed
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.scanlines
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText
import com.joecode.brokemon.ui.trainer.TrainerEditor
import com.joecode.brokemon.ui.trainer.TrainerViewModel
import kotlinx.coroutines.delay

private enum class IntroStep { TALK, CONSENT, TRAINER }

/** The professor: an original character in a white lab coat. */
internal val ProfessorLook = BroLook(
    skin = 1, hair = 8, hairColor = 6, expression = 0, facialHair = 2, glasses = 1, outfit = 3, outfitColor = 9,
)

private val introPages = listOf(
    listOf(
        "Yo! You made it. Welcome to the wild world of BROS!",
        "My name is BROMLEY. People around here call me the Bro Prof.",
    ),
    listOf(
        "This world is full of BROS: Yappers, Gym Rats, Road Ragers... and that one friend who's always late.",
        "Some people text their bros. Some ghost them. Me? I study them!",
    ),
    listOf(
        "Your job: catch your bros as cards, check in on them, and watch your friendships evolve.",
        "But first... let's start with YOU!",
    ),
)

/**
 * First launch: a short pixel-dialogue intro from the Bro Prof (skippable,
 * shown once), the privacy promise, then making your own Trainer Card.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    trainerViewModel: TrainerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var step by rememberSaveable { mutableStateOf(IntroStep.TALK) }
    BackHandler(enabled = step != IntroStep.TALK) {
        step = if (step == IntroStep.TRAINER) IntroStep.CONSENT else IntroStep.TALK
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(DexColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(240)) { -it / 3 } + fadeOut(tween(240)))
            },
            label = "intro",
        ) { current ->
            when (current) {
                IntroStep.TALK -> ProfessorTalk(onDone = { step = IntroStep.CONSENT })
                IntroStep.CONSENT -> ConsentStep(onAgree = { step = IntroStep.TRAINER })
                IntroStep.TRAINER -> Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LensLed()
                        Spacer(Modifier.width(12.dp))
                        Text("TRAINER REGISTRATION", style = PixelText.Label, color = DexColors.Text)
                    }
                    Spacer(Modifier.height(16.dp))
                    com.joecode.brokemon.ui.components.DexySays("Time to make your own card. You're the first Bro in the Brodex!")
                    Spacer(Modifier.height(16.dp))
                    TrainerEditor(
                        initial = null,
                        saveLabel = "Create my Trainer Card",
                        onSave = { trainerViewModel.saveTrainer(it, onSaved = onFinish) },
                        onSkip = onFinish,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessorTalk(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var line by rememberSaveable { mutableIntStateOf(0) }
    val text = introPages[page][line]
    var shown by remember(page, line) { mutableIntStateOf(0) }
    LaunchedEffect(page, line) {
        while (shown < text.length) {
            delay(24)
            shown++
        }
    }
    val finished = shown >= text.length
    fun advance() {
        when {
            !finished -> shown = text.length
            line < introPages[page].lastIndex -> line++
            page < introPages.lastIndex -> { page++; line = 0 }
            else -> onDone()
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LensLed()
            Spacer(Modifier.width(12.dp))
            LedCluster()
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("SKIP", style = PixelText.Label, color = DexColors.TextMuted) }
        }
        // The lab: tap anywhere to advance, like an old handheld.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Next",
                    onClick = ::advance,
                ),
            contentAlignment = Alignment.Center,
        ) {
            LabScene(page)
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            repeat(introPages.size) { Led(if (it == page) DexColors.LedGreen else DexColors.Outline, 1f, 10.dp) }
        }
        DialogueBox(
            speaker = "PROF. BROMLEY",
            text = text.take(shown),
            fullText = text,
            showNext = finished,
            onClick = ::advance,
        )
    }
}

@Composable
private fun LabScene(page: Int) {
    val bob by rememberInfiniteTransition(label = "prof").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "bob",
    )
    val bounce by rememberInfiniteTransition(label = "bros").animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "bounce",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.heightIn(max = 300.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            // Platform under everyone's feet.
            Canvas(Modifier.fillMaxWidth(0.8f).height(36.dp).align(Alignment.BottomCenter)) {
                drawOval(Brush.radialGradient(listOf(DexColors.ScreenText.copy(alpha = 0.25f), Color.Transparent)), Offset.Zero, size)
                drawOval(DexColors.ScreenBorder, Offset(size.width * 0.12f, size.height * 0.3f), Size(size.width * 0.76f, size.height * 0.5f))
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = Spacing.lg)) {
                BroFullBody(
                    ProfessorLook,
                    stage = 0,
                    shiny = false,
                    modifier = Modifier
                        .size(width = 150.dp, height = 244.dp)
                        .graphicsLayer { translationY = bob * 3f }
                        .semantics { contentDescription = "Professor Bromley" },
                )
                when (page) {
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val parade = listOf(
                            BroLook(skin = 4, hair = 11, hairColor = 0, expression = 1, outfit = 2, outfitColor = 1),
                            BroLook(skin = 1, hair = 2, hairColor = 3, expression = 3, glasses = 3, outfit = 1, outfitColor = 6),
                            BroLook(skin = 2, hair = 15, hairColor = 0, expression = 0, hat = 5, outfit = 0, outfitColor = 3),
                        )
                        parade.forEachIndexed { i, look ->
                            BroSprite(
                                look,
                                stage = 0,
                                shiny = false,
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer { translationY = if (i % 2 == 0) bounce else -bounce - 10f },
                            )
                        }
                    }
                    2 -> Box(Modifier.offset(y = (-8).dp), contentAlignment = Alignment.Center) {
                        // "Let's start with YOU": a mystery silhouette, waiting to be you.
                        BroFullBody(BroLook(), 0, false, Modifier.size(width = 110.dp, height = 180.dp), tint = DexColors.Outline)
                        Text("?", style = PixelText.Title, color = DexColors.LedYellow, modifier = Modifier.graphicsLayer { translationY = bounce })
                    }
                }
            }
        }
    }
}

/** Classic two-line text box with a name tag and a blinking "more" arrow. */
@Composable
private fun DialogueBox(speaker: String, text: String, fullText: String, showNext: Boolean, onClick: () -> Unit) {
    val blink by rememberInfiniteTransition(label = "next").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "blink",
    )
    val paper = DexColors.Paper
    val ink = DexColors.Ink
    val outer = DexShape(6.dp)
    Column {
        Text(
            speaker,
            style = PixelText.Tiny,
            color = DexColors.Text,
            modifier = Modifier
                .offset(x = 12.dp, y = 4.dp)
                .background(DexColors.DexRed, DexShape(topStart = 4.dp, topEnd = 4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .background(paper, outer)
                .border(4.dp, ink, outer)
                .padding(Spacing.xs)
                .border(2.dp, DexColors.ScreenBorder, DexShape(4.dp))
                .clickable(role = Role.Button, onClickLabel = "Next", onClick = onClick)
                .semantics { contentDescription = "$speaker says: $fullText" }
                .padding(horizontal = 14.dp, vertical = Spacing.md),
        ) {
            Text(text, color = ink, style = PixelText.Label.copy(lineHeight = PixelText.Header.lineHeight))
            if (showNext) {
                Canvas(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp, 10.dp)
                        .graphicsLayer { translationY = blink * 3.dp.toPx() },
                ) {
                    val px = size.width / 7f
                    for (r in 0 until 4) drawRect(DexColors.DexRed, Offset(r * px, r * px), Size(size.width - 2 * r * px, px))
                }
            }
        }
    }
}

@Composable
private fun ConsentStep(onAgree: () -> Unit) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .size(112.dp)
                .background(DexColors.Screen, DexShape(8.dp))
                .border(2.dp, DexColors.ScreenBorder, DexShape(8.dp))
                .scanlines(),
            contentAlignment = Alignment.Center,
        ) {
            BroSprite(ProfessorLook, 0, false, Modifier.fillMaxSize(0.9f))
        }
        Spacer(Modifier.height(16.dp))
        com.joecode.brokemon.ui.components.DexySays("Hi, I'm Dexy, your pocket guide! I'll pop up with tips. Tap me any time.")
        Spacer(Modifier.height(16.dp))
        Text("ONE RULE IN MY LAB", style = PixelText.Header, color = DexColors.LedYellow, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        ScreenPanel(title = "Privacy first") {
            Text(
                "Your Brodex lives only on this phone. No account, no cloud, no ads, no tracking. " +
                    "Cards leave your phone only when you show someone a QR code, and photos never do.",
                color = DexColors.ScreenText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = DexColors.DexRed),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "I'll only add photos, videos and details of friends who are cool with it.",
                color = DexColors.Text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        PixelButton("I promise", onAgree, Modifier.fillMaxWidth(), enabled = agreed)
    }
}
