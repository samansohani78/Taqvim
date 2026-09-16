/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.taqvim.data.database.backup.RestoreState

/** The test tag of the screen shown instead of the app while a restore is settled (ADR-0032). */
internal const val RESTORE_HOLD_TAG: String = "screen:restore-hold"

/**
 * Shown instead of the app while [state] is not settled, so no screen reads a partly restored database: a progress
 * indicator while the previous process's restore is finished or undone, and an explanation if it stays incomplete.
 */
@Composable
internal fun RestoreHold(
    state: RestoreState,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize().testTag(RESTORE_HOLD_TAG)) {
        Column(
            Modifier.padding(HOLD_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(HOLD_PADDING.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state == RestoreState.RECOVERING) CircularProgressIndicator()
            val text = if (state == RestoreState.INCOMPLETE) R.string.restore_incomplete else R.string.restore_finishing
            Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

private const val HOLD_PADDING = 24
