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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.permission.rememberNotificationPermissionRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The calendar screen bound to its [CalendarViewModel]; navigation effects go to [navigation] and the shown month is
 * printed through [printer] (T-803). [initialDay] is selected when the screen opens (T-1103 links, year, agenda and
 * search); `null` opens on today.
 */
@Composable
fun CalendarRoute(
    modifier: Modifier = Modifier,
    navigation: CalendarNavigation = CalendarNavigation(),
    printer: MonthPrinter = WebViewMonthPrinter,
    initialDay: Jdn? = null,
    viewModel: CalendarViewModel = koinViewModel(parameters = { parametersOf(initialDay) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    val currentPrinter by rememberUpdatedState(printer)
    val context = LocalContext.current
    val resources = LocalResources.current
    val requestNotifications = rememberNotificationPermissionRequest()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect == CalendarEffect.RequestNotificationPermission) {
                requestNotifications()
            } else if (effect == CalendarEffect.PrintMonth) {
                state.content?.let { content ->
                    val job = resources.getString(R.string.calendar_print_job)
                    currentPrinter.print(context, monthPrintHtml(content, resources), job)
                }
            } else {
                currentNavigation.handle(effect)
            }
        }
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
        CalendarEffect.NavigateToSearch -> onOpenSearch()
        CalendarEffect.NavigateToShiftWork -> onOpenShiftWork()
        is CalendarEffect.NavigateToPlanetaryHours -> onOpenPlanetaryHours(effect.day)
        CalendarEffect.PrintMonth, CalendarEffect.RequestNotificationPermission -> Unit
    }
}
