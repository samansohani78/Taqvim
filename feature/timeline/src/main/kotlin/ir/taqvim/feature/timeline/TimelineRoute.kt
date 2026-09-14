/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.model.Jdn
import org.koin.androidx.compose.koinViewModel

/**
 * The timeline bound to its [TimelineViewModel]. [initialDay], when given (the month pager's week number), shows the
 * week containing it once; one-shot effects go to [navigation].
 */
@Composable
fun TimelineRoute(
    modifier: Modifier = Modifier,
    initialDay: Jdn? = null,
    navigation: TimelineNavigation = TimelineNavigation(),
    viewModel: TimelineViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    var initialDayShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel, initialDay) {
        if (initialDay != null && !initialDayShown) {
            viewModel.onAction(TimelineAction.ShowWeekOf(initialDay))
            initialDayShown = true
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TimelineEffect.CreateEvent -> {
                    currentNavigation.onCreateEvent(effect.day, effect.startMinute, effect.endMinute)
                }

                is TimelineEffect.NavigateToEvent -> {
                    currentNavigation.onOpenEvent(effect.id, effect.kind)
                }
            }
        }
    }
    TimelineScreen(state, viewModel::onAction, modifier)
}
