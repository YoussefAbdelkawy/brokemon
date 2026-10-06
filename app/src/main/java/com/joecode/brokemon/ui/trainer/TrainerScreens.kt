package com.joecode.brokemon.ui.trainer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.joecode.brokemon.ui.theme.DexShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joecode.brokemon.data.model.TrainerFrame
import com.joecode.brokemon.domain.Quest
import com.joecode.brokemon.domain.QuestStatus
import com.joecode.brokemon.domain.Reward
import com.joecode.brokemon.domain.RewardKind
import com.joecode.brokemon.domain.TrainerLevel
import com.joecode.brokemon.ui.AppViewModelProvider
import com.joecode.brokemon.ui.components.DexScaffold
import com.joecode.brokemon.ui.components.FrameIcon
import com.joecode.brokemon.ui.components.PixelButton
import com.joecode.brokemon.ui.components.RewardIcon
import com.joecode.brokemon.ui.components.ScreenPanel
import com.joecode.brokemon.ui.components.SegmentedBar
import com.joecode.brokemon.ui.components.Sparkles
import com.joecode.brokemon.ui.components.dragToTilt
import com.joecode.brokemon.ui.components.rememberTiltState
import com.joecode.brokemon.ui.components.rememberDeviceTilt
import com.joecode.brokemon.ui.theme.DexColors
import com.joecode.brokemon.ui.theme.PixelText

@Composable
fun TrainerScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onJournal: () -> Unit,
    viewModel: TrainerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val tilt = rememberTiltState()
    val scope = rememberCoroutineScope()
    val p = progress
    val trainer = p?.trainer

    DexScaffold(title = "Trainer Card", onBack = onBack) { padding ->
        when {
            p == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = DexColors.DexRed)
            }
            // No card yet: build it right here.
            trainer == null -> TrainerEditor(
                initial = null,
                saveLabel = "Create Trainer Card",
                onSave = { viewModel.saveTrainer(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.widthIn(max = 420.dp).fillMaxWidth().dragToTilt(tilt, scope)) {
                            TrainerCard(trainer, p.level, p.totals, p.unlocked, tilt = tilt, deviceTilt = rememberDeviceTilt(), trophies = p.trophies)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("HOLD & DRAG TO TILT", style = PixelText.Tiny, color = DexColors.Outline)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PixelButton(
                            "Edit card",
                            onEdit,
                            Modifier.weight(1f),
                            color = DexColors.SurfaceHigh,
                            stacked = true,
                            leading = { Icon(Icons.Filled.Edit, null, tint = DexColors.LedYellow, modifier = Modifier.size(20.dp)) },
                        )
                        PixelButton(
                            if (p.rewardsReady > 0) "Claim rewards" else "Journal",
                            onJournal,
                            Modifier.weight(1f),
                            stacked = true,
                            leading = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = DexColors.Text, modifier = Modifier.size(20.dp)) },
                        )
                    }
                }
                item { LevelPanel(p) }
                item { FramePanel(p.frames, trainer.resolvedFrame, viewModel::setFrame) }
                item { RewardsPanel(p.unlocked) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun LevelPanel(p: TrainerProgress) {
    ScreenPanel(title = "Trainer level") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LV.${p.level.level}", style = PixelText.Title, color = DexColors.LedYellow)
            Spacer(Modifier.weight(1f))
            Text("${p.level.xp} XP", style = PixelText.Label, color = DexColors.ScreenText)
        }
        Spacer(Modifier.height(8.dp))
        SegmentedBar(p.level.progress, DexColors.LedBlue, Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            p.level.toNext?.let { "$it XP to Lv.${p.level.level + 1}" } ?: "Max level. Absolute legend.",
            color = DexColors.TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        XpLine("Catch a bro", TrainerLevel.CATCH_XP)
        XpLine("Check in", TrainerLevel.CHECK_IN_XP)
        XpLine("Log a memory", TrainerLevel.MEMORY_XP)
        XpLine("Add a fact", TrainerLevel.FACT_XP)
        XpLine("Claim a quest", TrainerLevel.QUEST_XP)
        XpLine("Win a battle", TrainerLevel.BATTLE_WIN_XP)
        XpLine("Daily battle quest", TrainerLevel.DAILY_BATTLE_XP)
    }
}

@Composable
private fun XpLine(label: String, xp: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label.uppercase(), style = PixelText.Tiny, color = DexColors.TextMuted, modifier = Modifier.weight(1f))
        Text("+$xp XP", style = PixelText.Tiny, color = DexColors.ScreenText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FramePanel(unlocked: List<TrainerFrame>, current: TrainerFrame, onPick: (TrainerFrame) -> Unit) {
    ScreenPanel(title = "Card frame") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TrainerFrame.entries.forEach { frame ->
                val owned = frame in unlocked
                val selected = frame == current
                val shape = DexShape(4.dp)
                Column(
                    Modifier
                        .width(72.dp)
                        .semantics {
                            contentDescription = "${frame.label} frame" + when {
                                selected -> ", equipped"
                                !owned -> ", locked"
                                else -> ""
                            }
                        }
                        .border(2.dp, if (selected) DexColors.LedYellow else Color.Transparent, shape)
                        .clickable(enabled = owned, role = Role.Button) { onPick(frame) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(48.dp, 60.dp), contentAlignment = Alignment.Center) {
                        FrameIcon(frame, owned, Modifier.fillMaxSize())
                        if (!owned) Icon(Icons.Filled.Lock, null, tint = DexColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        frame.label.uppercase(),
                        style = PixelText.Tiny,
                        color = if (selected) DexColors.LedYellow else if (owned) DexColors.Text else DexColors.TextMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Earn new frames in the Trainer's Journal.", color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RewardsPanel(unlocked: Set<Reward>) {
    ScreenPanel(title = "Rewards ${unlocked.size}/${Reward.entries.size}") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Reward.entries.forEach { reward ->
                val owned = reward in unlocked
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RewardIcon(reward, owned, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (owned) reward.label.uppercase() else "???",
                            style = PixelText.Tiny,
                            color = if (owned) DexColors.Text else DexColors.TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (owned) reward.description else reward.kind.label,
                            color = DexColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrainerEditScreen(
    onBack: () -> Unit,
    viewModel: TrainerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    DexScaffold(title = "Edit Trainer Card", onBack = onBack) { padding ->
        val p = progress
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = DexColors.DexRed) }
        } else {
            TrainerEditor(
                initial = p.trainer,
                saveLabel = if (p.trainer == null) "Create Trainer Card" else "Save",
                onSave = { viewModel.saveTrainer(it, onSaved = onBack) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }
}

// --- Trainer's Journal -------------------------------------------------------------

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    onGo: (Quest) -> Unit,
    viewModel: TrainerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val claimed by viewModel.justClaimed.collectAsStateWithLifecycle()
    val p = progress

    DexScaffold(title = "Trainer's Journal", onBack = onBack) { padding ->
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = DexColors.DexRed) }
            return@DexScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenPanel(title = "Starter quests") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (p.journalComplete) "JOURNAL COMPLETE!" else "${p.questsDone}/${p.quests.size} DONE",
                            style = PixelText.Header,
                            color = if (p.journalComplete) DexColors.Gold else DexColors.ScreenText,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("+${TrainerLevel.QUEST_XP} XP EACH", style = PixelText.Tiny, color = DexColors.LedYellow)
                    }
                    Spacer(Modifier.height(10.dp))
                    SegmentedBar(p.questsDone / p.quests.size.toFloat(), DexColors.LedGreen, Modifier.fillMaxWidth().height(12.dp), segments = p.quests.size * 3)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Each quest teaches you a part of the Brodex and pays out a frame, a badge or a new sprite part.",
                        color = DexColors.TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(p.quests, key = { it.quest.name }) { status ->
                QuestRow(
                    number = status.quest.ordinal + 1,
                    status = status,
                    onClaim = { viewModel.claim(status.quest) },
                    onGo = { onGo(status.quest) },
                    modifier = Modifier.animateItem(),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    claimed?.let { reward ->
        RewardDialog(
            reward = reward,
            masterToo = p?.journalComplete == true && reward != Reward.MASTER_BADGE,
            onEquip = reward.frame?.let { frame -> { viewModel.setFrame(frame); viewModel.dismissClaimed() } },
            onDismiss = viewModel::dismissClaimed,
        )
    }
}

@Composable
private fun QuestRow(number: Int, status: QuestStatus, onClaim: () -> Unit, onGo: () -> Unit, modifier: Modifier = Modifier) {
    val shape = DexShape(6.dp)
    val pulse by rememberInfiniteTransition(label = "claim").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "scale",
    )
    val border = when {
        status.claimable -> DexColors.LedYellow
        status.claimed -> DexColors.LedGreen.copy(alpha = 0.5f)
        else -> DexColors.Outline
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(DexColors.Surface, shape)
            .border(2.dp, border, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (status.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (status.done) "Done" else "Not done",
            tint = if (status.done) DexColors.LedGreen else DexColors.Outline,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("%02d ".format(number) + status.quest.title.uppercase(), style = PixelText.Tiny, color = DexColors.Text)
            Spacer(Modifier.height(4.dp))
            Text(status.quest.hint, color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RewardIcon(status.quest.reward, unlocked = status.claimed || status.claimable, size = 22.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "REWARD: " + status.quest.reward.label.uppercase(),
                    style = PixelText.Tiny,
                    color = if (status.claimed) DexColors.LedGreen else DexColors.LedYellow,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            status.claimable -> PixelButton(
                "Claim",
                onClaim,
                Modifier.width(92.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
                color = DexColors.DexRed,
            )
            status.claimed -> Text("CLAIMED", style = PixelText.Tiny, color = DexColors.LedGreen)
            else -> PixelButton("Go", onGo, Modifier.width(72.dp), color = DexColors.SurfaceHigh)
        }
    }
}

@Composable
private fun RewardDialog(reward: Reward, masterToo: Boolean, onEquip: (() -> Unit)?, onDismiss: () -> Unit) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(reward) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Dialog(onDismissRequest = onDismiss) {
        ScreenPanel(title = "Quest complete") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REWARD GET!", style = PixelText.Title, color = DexColors.Gold)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Sparkles(Modifier.fillMaxSize(), seed = reward.ordinal)
                    RewardIcon(
                        reward,
                        unlocked = true,
                        size = 88.dp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            rotationZ = (1f - pop.value) * -40f
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(reward.label.uppercase(), style = PixelText.Label, color = DexColors.Text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(reward.description, color = DexColors.TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Text("+${TrainerLevel.QUEST_XP} XP", style = PixelText.Tiny, color = DexColors.LedBlue, modifier = Modifier.padding(top = 8.dp))
                if (masterToo) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RewardIcon(Reward.MASTER_BADGE, true, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("JOURNAL COMPLETE: BRO MASTER BADGE + POW! BACKDROP", style = PixelText.Tiny, color = DexColors.Gold)
                    }
                }
                if (reward.kind == RewardKind.PART) {
                    Spacer(Modifier.height(8.dp))
                    Text("Find it under HAT in any character builder.", color = DexColors.ScreenText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
                if (onEquip != null) {
                    PixelButton("Equip frame", onEquip, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                }
                PixelButton("Nice!", onDismiss, Modifier.fillMaxWidth(), color = DexColors.SurfaceHigh)
            }
        }
    }
}
