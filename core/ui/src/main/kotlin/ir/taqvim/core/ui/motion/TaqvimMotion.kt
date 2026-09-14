/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * The Material 3 motion tokens Taqvim uses (T-703): durations and easing curves. Every duration goes through [scaled],
 * so the system animation scale (developer options, "remove animations") also slows or removes Taqvim's motion.
 */
public object TaqvimMotion {
    /** Material 3 `short3`: small fades. */
    public const val SHORT_MILLIS: Int = 150

    /** Material 3 `medium2`: screen transitions. */
    public const val MEDIUM_MILLIS: Int = 300

    /** Material 3 `long1`: large, emphasized movements. */
    public const val LONG_MILLIS: Int = 450

    /** Material 3 emphasized decelerate: elements entering the screen. */
    public val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Material 3 emphasized accelerate: elements leaving the screen. */
    public val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Material 3 standard: elements moving within the screen. */
    public val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** [millis] under the system animation [scale]; no scale (0, negative or NaN) means no motion at all. */
    public fun scaled(
        millis: Int,
        scale: Float,
    ): Int = if (scale > 0f) (millis * scale).roundToInt() else 0

    /**
     * The side incoming content slides in from, as the sign of its horizontal offset: pushed screens come from the end
     * of the reading direction (the right in LTR, the left in RTL) and popped screens from the start.
     */
    public fun enterOffsetSign(
        push: Boolean,
        rtl: Boolean,
    ): Int = if (push != rtl) 1 else -1
}

/** How much motion the user allows: the system animator [durationScale] (1 by default, 0 when animations are off). */
@Immutable
public data class MotionSettings(
    public val durationScale: Float = 1f,
) {
    /** Whether animations are removed. */
    public val reduced: Boolean
        get() = !(durationScale > 0f)
}

/** The system animator duration scale of this device, read once per composition context. */
@Composable
public fun rememberSystemMotionSettings(): MotionSettings {
    val context = LocalContext.current
    return remember(context) {
        MotionSettings(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

/**
 * Screen-to-screen transitions (Material 3 shared X axis): the new screen slides a little and fades in while the old
 * one slides the other way and fades out, mirrored in RTL. With [MotionSettings.reduced] screens switch without motion.
 */
public object NavigationMotion {
    private const val OFFSET_DIVISOR = 10

    /** Opening a screen on top of the current one. */
    public fun push(
        rtl: Boolean,
        motion: MotionSettings,
    ): ContentTransform = transform(push = true, rtl, motion)

    /** Returning to the screen below, including predictive back. */
    public fun pop(
        rtl: Boolean,
        motion: MotionSettings,
    ): ContentTransform = transform(push = false, rtl, motion)

    private fun transform(
        push: Boolean,
        rtl: Boolean,
        motion: MotionSettings,
    ): ContentTransform {
        if (motion.reduced) return EnterTransition.None togetherWith ExitTransition.None
        val sign = TaqvimMotion.enterOffsetSign(push, rtl)
        val slide = TaqvimMotion.scaled(TaqvimMotion.MEDIUM_MILLIS, motion.durationScale)
        val fade = TaqvimMotion.scaled(TaqvimMotion.SHORT_MILLIS, motion.durationScale)
        val enterSlide = tween<IntOffset>(slide, easing = TaqvimMotion.EmphasizedDecelerate)
        val exitSlide = tween<IntOffset>(slide, easing = TaqvimMotion.EmphasizedAccelerate)
        val enter =
            slideInHorizontally(enterSlide) { sign * it / OFFSET_DIVISOR } +
                fadeIn(tween(fade, delayMillis = fade / 2, easing = TaqvimMotion.Standard))
        val exit =
            slideOutHorizontally(exitSlide) { -sign * it / OFFSET_DIVISOR } +
                fadeOut(tween(fade, easing = TaqvimMotion.Standard))
        return enter togetherWith exit
    }
}
