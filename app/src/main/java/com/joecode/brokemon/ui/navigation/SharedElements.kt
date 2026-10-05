package com.joecode.brokemon.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Provided by the nav graph so a card on Home can morph into its room. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

fun cardKey(broId: Long) = "bro-card-$broId"

/** Shared bounds between a bro's card and their room: the card grows into the room. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCard(broId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val anim = LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedCard.sharedBounds(
            sharedContentState = rememberSharedContentState(cardKey(broId)),
            animatedVisibilityScope = anim,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            boundsTransform = { _, _ -> tween(520) },
            clipInOverlayDuringTransition = OverlayClip(CutCornerShape(10.dp)),
        )
    }
}
