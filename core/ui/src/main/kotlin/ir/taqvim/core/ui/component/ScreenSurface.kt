/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ir.taqvim.core.ui.theme.TaqvimBackground

/**
 * The root of every screen (T-701): the theme background (color, gradient or veiled image) behind a transparent
 * scaffold with [topBar] and [bottomBar]. [content] receives the insets it must pad by.
 */
@Composable
public fun ScreenSurface(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    TaqvimBackground(modifier.fillMaxSize()) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
