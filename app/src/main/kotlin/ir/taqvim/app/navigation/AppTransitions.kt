/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import ir.taqvim.core.ui.motion.MotionSettings
import ir.taqvim.core.ui.motion.ProvideSharedScopes
import ir.taqvim.core.ui.motion.rememberSystemMotionSettings

/** Where the screen container animates (T-703): its motion settings, reading direction and shared-transition scope. */
internal class AppTransitions(
    val motion: MotionSettings,
    val rtl: Boolean,
    val shared: SharedTransitionScope,
)

/** [content] inside one shared-transition layout, with the motion settings and direction of this composition. */
@Composable
internal fun AppTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable (AppTransitions) -> Unit,
) {
    val motion = rememberSystemMotionSettings()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    SharedTransitionLayout(modifier) { content(AppTransitions(motion, rtl, this)) }
}

/** One screen of the back stack, with the shared-element scopes of its entry transition (ir.taqvim.core.ui.motion). */
@Composable
internal fun AnimatedScreen(
    transitions: AppTransitions,
    content: @Composable () -> Unit,
) {
    ProvideSharedScopes(transitions.shared, LocalNavAnimatedContentScope.current, content)
}
