/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection

/** A labelled user action shown as a button: [label] is the button text or, for icon buttons, its description. */
@Immutable
public data class ComponentAction(
    public val label: String,
    public val onClick: () -> Unit,
)

/** An icon button of a [TopBar]; [mirrorInRtl] flips direction-dependent icons such as a back arrow. */
@Immutable
public data class TopBarAction(
    public val icon: ImageVector,
    public val label: String,
    public val onClick: () -> Unit,
    public val mirrorInRtl: Boolean = false,
)

/**
 * The screen title bar (T-701): a single-line [title] announced as a heading, an optional [subtitle] (e.g. the date in
 * a secondary calendar), an optional [navigation] button and trailing [actions]. The container is transparent so the
 * theme background (gradient or image) stays visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: TopBarAction? = null,
    actions: List<TopBarAction> = emptyList(),
) {
    TopAppBar(
        title = { TopBarTitle(title, subtitle) },
        modifier = modifier,
        navigationIcon = { navigation?.let { TopBarActionButton(it) } },
        actions = { actions.forEach { TopBarActionButton(it) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
private fun TopBarTitle(
    title: String,
    subtitle: String?,
) {
    Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopBarActionButton(action: TopBarAction) {
    val mirrored = action.mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(onClick = action.onClick) {
        Icon(
            action.icon,
            contentDescription = action.label,
            modifier = Modifier.graphicsLayer { scaleX = if (mirrored) -1f else 1f },
        )
    }
}
