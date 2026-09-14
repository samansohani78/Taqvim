/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ir.taqvim.app.R
import ir.taqvim.core.model.Jdn
import ir.taqvim.feature.agenda.AgendaRoute
import ir.taqvim.feature.astronomy.AstronomyRoute
import ir.taqvim.feature.calendar.CalendarRoute
import ir.taqvim.feature.compass.CompassRoute
import ir.taqvim.feature.compass.LevelRoute
import ir.taqvim.feature.events.EventEditorRoute
import ir.taqvim.feature.search.SearchRoute
import ir.taqvim.feature.settings.AthanSettingsRoute
import ir.taqvim.feature.settings.LocationSettingsRoute
import ir.taqvim.feature.settings.SettingsHomeRoute
import ir.taqvim.feature.settings.SettingsItemId
import ir.taqvim.feature.settings.SubscriptionsRoute
import ir.taqvim.feature.timeline.TimelineRoute
import ir.taqvim.feature.times.TimesRoute
import ir.taqvim.feature.tools.ToolsRoute
import ir.taqvim.feature.year.YearRoute

/** The test tag of the screen shown for [destination]. */
internal fun destinationTag(destination: AppDestination): String =
    "destination:" + (destination::class.simpleName ?: "unknown")

/** The screens of [navigator]'s back stack, with predictive back, per-screen saved state and per-screen ViewModels. */
@Composable
internal fun AppNavDisplay(
    navigator: AppNavigator,
    router: AppRouter,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = navigator.backStack.entries,
        modifier = modifier,
        onBack = navigator::back,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = { destination -> NavEntry(destination) { AppScreen(it, navigator, router) } },
    )
}

@Composable
private fun AppScreen(
    destination: AppDestination,
    navigator: AppNavigator,
    router: AppRouter,
) {
    val modifier = Modifier.fillMaxSize().testTag(destinationTag(destination))
    when (destination) {
        is AppDestination.Timeline -> {
            TimelineRoute(modifier, destination.initialDay?.let(::Jdn), router.timeline())
        }

        is AppDestination.EventEditor -> {
            EventEditorRoute(destination.eventId, onClose = { navigator.back() }, modifier = modifier)
        }

        is AppDestination.Pending -> {
            PendingScreen(destination.feature.notice, modifier)
        }

        is AppDestination.Settings -> {
            val item = SettingsItemId.entries.firstOrNull { it.name == destination.initialItem }
            SettingsHomeRoute(modifier, item, router.settings())
        }

        else -> {
            SimpleScreen(destination, navigator, router, modifier)
        }
    }
}

/** Screens without arguments. */
@Composable
private fun SimpleScreen(
    destination: AppDestination,
    navigator: AppNavigator,
    router: AppRouter,
    modifier: Modifier,
) {
    when (destination) {
        AppDestination.Calendar -> CalendarRoute(modifier, router.calendar())
        AppDestination.Times -> TimesRoute(modifier)
        AppDestination.Tools -> ToolsRoute(modifier)
        AppDestination.More -> MoreScreen(navigator::navigate, modifier)
        AppDestination.Year -> YearRoute(modifier, router.year())
        AppDestination.Agenda -> AgendaRoute(modifier, router.agenda())
        else -> SettingsAndInstrumentScreen(destination, router, modifier)
    }
}

@Composable
private fun SettingsAndInstrumentScreen(
    destination: AppDestination,
    router: AppRouter,
    modifier: Modifier,
) {
    when (destination) {
        AppDestination.Astronomy -> AstronomyRoute(modifier)
        AppDestination.Compass -> CompassRoute(modifier)
        AppDestination.Level -> LevelRoute(modifier)
        AppDestination.Search -> SearchRoute(modifier, router.search())
        AppDestination.LocationSettings -> LocationSettingsRoute(modifier)
        AppDestination.AthanSettings -> AthanSettingsRoute(modifier)
        AppDestination.Subscriptions -> SubscriptionsRoute(modifier)
        else -> PendingScreen(R.string.pending_settings, modifier)
    }
}

/** The screens reached from the "More" tab, in order. */
internal enum class MoreEntry(
    val destination: AppDestination,
    @param:StringRes val label: Int,
) {
    YEAR(AppDestination.Year, R.string.more_year),
    AGENDA(AppDestination.Agenda, R.string.more_agenda),
    SEARCH(AppDestination.Search, R.string.more_search),
    ASTRONOMY(AppDestination.Astronomy, R.string.more_astronomy),
    COMPASS(AppDestination.Compass, R.string.more_compass),
    LEVEL(AppDestination.Level, R.string.more_level),
    SETTINGS(AppDestination.Settings(), R.string.more_settings),
}

/** The list of [MoreEntry] screens. */
@Composable
internal fun MoreScreen(
    onOpen: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = WindowInsets.safeDrawing.asPaddingValues()) {
        items(MoreEntry.entries) { entry ->
            ListItem(
                headlineContent = { Text(stringResource(entry.label)) },
                modifier = Modifier.testTag("more:" + entry.name).clickable { onOpen(entry.destination) },
            )
        }
    }
}

/** A notice in place of a screen that is not built yet. */
@Composable
private fun PendingScreen(
    @StringRes notice: Int,
    modifier: Modifier,
) {
    Box(modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Text(stringResource(notice), style = MaterialTheme.typography.bodyLarge)
    }
}
