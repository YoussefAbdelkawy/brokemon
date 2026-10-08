package com.joecode.brokemon.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these instead of raw dp values for padding and gaps. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner "radii". Pixel UI uses chamfered corners, so these are chamfer sizes. */
object Radii {
    val xs = 2.dp
    val sm = 4.dp
    val md = 6.dp
    val lg = 8.dp
    val xl = 12.dp
    val card = 10.dp
}

/** Border widths (the chunky pixel look is 2-4 dp). */
object Borders {
    val thin = 1.dp
    val normal = 2.dp
    val chunky = 3.dp
    val heavy = 4.dp
}

/** Minimum touch target, even for small pixel buttons. */
val MinTouch = 48.dp

/** The one shape of the app: a chamfered rectangle. */
fun DexShape(size: Dp): CornerBasedShape = CutCornerShape(size)

fun DexShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): CornerBasedShape =
    CutCornerShape(topStart, topEnd, bottomEnd, bottomStart)
