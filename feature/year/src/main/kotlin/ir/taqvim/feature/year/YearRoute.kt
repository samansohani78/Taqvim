/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/** The year view bound to its [YearViewModel]; one-shot effects go to [navigation]. */
@Composable
fun YearRoute(
    modifier: Modifier = Modifier,
    navigation: YearNavigation = YearNavigation(),
    viewModel: YearViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is YearEffect.NavigateToMonth -> currentNavigation.onOpenMonth(effect.firstDay)
            }
        }
    }
    YearScreen(state, viewModel::onAction, modifier)
}
