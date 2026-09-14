/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/** The calendar screen bound to its [CalendarViewModel]; one-shot effects go to [navigation]. */
@Composable
fun CalendarRoute(
    modifier: Modifier = Modifier,
    navigation: CalendarNavigation = CalendarNavigation(),
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { currentNavigation.handle(it) }
    }
    CalendarScreen(state, viewModel::onAction, modifier)
}

private fun CalendarNavigation.handle(effect: CalendarEffect) {
    when (effect) {
        is CalendarEffect.NavigateToEventEditor -> onOpenEventEditor(effect.day)
        is CalendarEffect.NavigateToEvent -> onOpenEvent(effect.event)
        is CalendarEffect.NavigateToTimeline -> onOpenTimeline(effect.firstDay)
        is CalendarEffect.ShowSnackbar -> onMessage(effect.message)
        is CalendarEffect.OpenUrl -> onOpenUrl(effect.url)
    }
}
