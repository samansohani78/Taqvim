/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The registry of shared-bounds keys (T-703). Features name shared elements only through these typed keys, so two
 * screens meet on the same element by value and keys of different kinds can never collide the way free-form strings
 * could.
 */
@Immutable
public sealed interface SharedKey {
    /** A civil day, by its Julian day number. */
    @Immutable
    public data class Day(
        public val jdn: Long,
    ) : SharedKey

    /** A month page, by its offset from today's month in the primary calendar. */
    @Immutable
    public data class Month(
        public val offset: Int,
    ) : SharedKey

    /** A personal event, by its stored id. */
    @Immutable
    public data class Event(
        public val id: String,
    ) : SharedKey
}

/** The shared-transition scope of the screen container and the animated visibility scope of the current screen. */
@Stable
public class SharedScopes(
    public val transition: SharedTransitionScope,
    public val visibility: AnimatedVisibilityScope,
)

/** The [SharedScopes] of the enclosing screen, or `null` outside an animated screen container (e.g. in widgets). */
public val LocalSharedScopes: ProvidableCompositionLocal<SharedScopes?> = staticCompositionLocalOf { null }

/** Provides [transition] and [visibility] as [LocalSharedScopes] to [content]. */
@Composable
public fun ProvideSharedScopes(
    transition: SharedTransitionScope,
    visibility: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    val scopes = remember(transition, visibility) { SharedScopes(transition, visibility) }
    CompositionLocalProvider(LocalSharedScopes provides scopes, content = content)
}

/**
 * Calls [content] with a modifier that morphs the bounds of the element named [key] into the element with the same key
 * on the next screen. The modifier is plain when [key] is `null` or there is no shared transition around (e.g. in
 * previews and screenshots), so settled frames never change.
 */
@Composable
public fun WithSharedBounds(
    key: SharedKey?,
    content: @Composable (Modifier) -> Unit,
) {
    val scopes = LocalSharedScopes.current
    if (key == null || scopes == null) {
        content(Modifier)
        return
    }
    with(scopes.transition) {
        content(Modifier.sharedBounds(rememberSharedContentState(key), scopes.visibility))
    }
}
