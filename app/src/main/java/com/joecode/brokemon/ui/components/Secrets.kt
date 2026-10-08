package com.joecode.brokemon.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalContext
import com.joecode.brokemon.BrokemonApplication
import com.joecode.brokemon.domain.Cosmetic
import com.joecode.brokemon.ui.feedback.LocalFeedback
import com.joecode.brokemon.ui.feedback.Sfx
import com.joecode.brokemon.ui.feedback.ToastKind
import kotlinx.coroutines.launch
import kotlin.math.abs

/** The old arcade code, as swipes: up, up, down, down, left, right, left, right. */
private val SECRET_CODE = listOf(0, 0, 1, 1, 2, 3, 2, 3)

/**
 * A hidden gesture on the header lens. Swipe the code and a secret card frame is added to your
 * collection. Nothing points at it; it's there for people who poke around.
 */
@Composable
fun rememberSecretSwipe(): Modifier {
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    val owned = LocalOwnedCosmetics.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf(0) }
    return Modifier.pointerInput(owned) {
        var dx = 0f
        var dy = 0f
        detectDragGestures(
            onDragStart = { dx = 0f; dy = 0f },
            onDrag = { change, drag -> change.consume(); dx += drag.x; dy += drag.y },
            onDragEnd = {
                if (maxOf(abs(dx), abs(dy)) < 12f) return@detectDragGestures
                val dir = if (abs(dy) >= abs(dx)) (if (dy < 0) 0 else 1) else (if (dx < 0) 2 else 3)
                progress = if (dir == SECRET_CODE[progress]) progress + 1 else if (dir == SECRET_CODE[0]) 1 else 0
                if (progress == SECRET_CODE.size) {
                    progress = 0
                    if (Cosmetic.FR_SECRET.id in owned) {
                        feedback?.toast("You already found the secret frame", ToastKind.INFO)
                    } else {
                        scope.launch {
                            (context.applicationContext as BrokemonApplication).container.prefs.addCosmetic(Cosmetic.FR_SECRET.id)
                        }
                        feedback?.play(Sfx.SECRET, 0.7f)
                        feedback?.reward("Secret frame unlocked! Find it in Card frame.")
                    }
                }
            },
        )
    }
}
