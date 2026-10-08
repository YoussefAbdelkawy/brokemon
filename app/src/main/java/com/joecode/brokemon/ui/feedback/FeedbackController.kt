package com.joecode.brokemon.ui.feedback

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import com.joecode.brokemon.data.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ToastKind { SUCCESS, ERROR, INFO, REWARD }

data class PixelToastData(val text: String, val kind: ToastKind, val id: Long = System.nanoTime())

/**
 * "Did that work?" answers: every action gets a small pixel toast, a haptic tick
 * and an 8-bit sound. Each part can be switched off in Settings.
 */
class FeedbackController(
    private val appContext: Context,
    scope: CoroutineScope,
    prefs: UserPrefs,
) {
    private val sound = SoundFx()
    private val _toast = MutableStateFlow<PixelToastData?>(null)
    val toast: StateFlow<PixelToastData?> = _toast.asStateFlow()
    @Volatile private var haptics = true
    @Volatile private var userReduceMotion = false
    var view: View? = null

    private val _reduceMotion = MutableStateFlow(false)
    /** True when the user turned on Reduce motion or the system "remove animations" setting is on. */
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    init {
        scope.launch { prefs.soundsEnabled.collect { sound.effectsOn = it } }
        scope.launch {
            prefs.musicEnabled.collect {
                sound.musicOn = it
                if (it) sound.startMusic() else sound.stopMusic()
            }
        }
        scope.launch { prefs.hapticsEnabled.collect { haptics = it } }
        scope.launch { prefs.reduceMotion.collect { userReduceMotion = it; refreshMotion() } }
    }

    /** Re-reads the system animation scale (it can change while the app is open). */
    fun refreshMotion() {
        val systemOff = runCatching {
            Settings.Global.getFloat(appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
        _reduceMotion.value = userReduceMotion || systemOff
    }

    fun toast(text: String, kind: ToastKind = ToastKind.SUCCESS) {
        _toast.value = PixelToastData(text, kind)
        when (kind) {
            ToastKind.SUCCESS -> { haptic(true); sound.play(Sfx.SUCCESS) }
            ToastKind.ERROR -> { haptic(false); sound.play(Sfx.ERROR) }
            ToastKind.REWARD -> { haptic(true); sound.play(Sfx.LEVEL_UP) }
            ToastKind.INFO -> sound.play(Sfx.TAP)
        }
    }

    fun dismissToast(id: Long) {
        if (_toast.value?.id == id) _toast.value = null
    }

    fun success(text: String) = toast(text, ToastKind.SUCCESS)
    fun error(text: String) = toast(text, ToastKind.ERROR)
    fun reward(text: String) = toast(text, ToastKind.REWARD)

    /** A light tick plus a blip, for buttons and tabs. */
    fun tap() {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK).takeIf { haptics }
        sound.play(Sfx.TAP, 0.35f)
    }

    fun play(sfx: Sfx, volume: Float = 0.5f) = sound.play(sfx, volume)

    private fun haptic(positive: Boolean) {
        if (!haptics) return
        val kind = when {
            Build.VERSION.SDK_INT >= 30 -> if (positive) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
            else -> if (positive) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
        }
        view?.performHapticFeedback(kind)
    }

    /** Heavier buzz for big moments (shiny, level up, hits). */
    fun thump() {
        if (haptics) view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun startMusic() = sound.startMusic()
    fun stopMusic() = sound.stopMusic()

    /** Called when the app goes to the background / returns. Music only plays while the app is visible. */
    fun pauseMusic() = sound.stopMusic()
    fun resumeMusic() { if (sound.musicOn) sound.startMusic() }

    companion object {
        /** Used in previews and tests: does nothing. */
        val NONE: FeedbackController? = null
    }
}

/** The app-wide feedback controller. Null means "silent" (previews, tests). */
val LocalFeedback = staticCompositionLocalOf<FeedbackController?> { null }

/** True when animations should be off (Reduce motion setting or the system's remove-animations setting). */
val LocalReduceMotion = staticCompositionLocalOf { false }
