/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** Top-level app shell. Replaced by the Navigation 3 host in Epic 8. */
@Composable
fun TaqvimAppShell(modifier: Modifier = Modifier) {
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.app_shell_placeholder),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}
