/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

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
    // The app bar has a fixed height: title and subtitle keep one line each, capped at a moderate font scale, and
    // shrink to fit rather than being cut off (T-1701).
    val density = LocalDensity.current
    val barDensity = Density(density.density, density.fontScale.coerceAtMost(MAX_BAR_FONT_SCALE))
    CompositionLocalProvider(LocalDensity provides barDensity) {
        TitleLines(title, subtitle)
    }
}

@Composable
private fun TitleLines(
    title: String,
    subtitle: String?,
) {
    Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
        val titleStyle = MaterialTheme.typography.titleLarge
        Text(title, style = titleStyle, maxLines = 1, autoSize = fitting(titleStyle))
        if (subtitle != null) {
            val subtitleStyle = MaterialTheme.typography.labelMedium
            Text(
                subtitle,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                autoSize = fitting(subtitleStyle),
            )
        }
    }
}

private val MIN_TITLE_TEXT_SIZE = 8.sp
private const val MAX_BAR_FONT_SCALE = 1.3f

private fun fitting(style: TextStyle): TextAutoSize =
    TextAutoSize.StepBased(minFontSize = MIN_TITLE_TEXT_SIZE, maxFontSize = style.fontSize)

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
