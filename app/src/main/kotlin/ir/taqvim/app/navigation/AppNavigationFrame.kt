/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource

/** The test tag of [tab]'s navigation item. */
internal fun tabTag(tab: TopLevelTab): String = "tab:" + tab.name

/**
 * The app frame (ADR-0015): the top-level tabs in a navigation bar on compact windows and a rail on wider ones
 * (Material 3 adaptive navigation suite), the [content] beside them and calendar messages in [snackbar].
 */
@Composable
internal fun AppNavigationFrame(
    selected: TopLevelTab,
    onSelect: (TopLevelTab) -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelTab.entries.forEach { tab ->
                item(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(ImageVector.vectorResource(tab.icon), contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                    modifier = Modifier.testTag(tabTag(tab)),
                )
            }
        },
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
    }
}
