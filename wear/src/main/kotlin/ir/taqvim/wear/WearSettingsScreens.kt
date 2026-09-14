/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.composable
import ir.taqvim.core.model.PrayerMethod

/** Settings and its choice lists; every choice is stored on the watch and returns to the summary. */
internal fun NavGraphBuilder.settingsDestinations(
    graph: WearGraph,
    onOpen: (String) -> Unit,
    onDone: () -> Unit,
) {
    composable(WearRoutes.SETTINGS) { SettingsRoute(graph) { _, state -> SettingsScreen(state, onOpen) } }
    composable(WearRoutes.LANGUAGE) { SettingsRoute(graph) { model, state -> LanguageChoice(model, state, onDone) } }
    composable(WearRoutes.CALENDAR) { SettingsRoute(graph) { model, state -> CalendarChoice(model, state, onDone) } }
    composable(WearRoutes.METHOD) { SettingsRoute(graph) { model, state -> MethodChoice(model, state, onDone) } }
    composable(WearRoutes.CITY) { SettingsRoute(graph) { model, state -> CityChoice(model, state, onDone) } }
}

@Composable
private fun LanguageChoice(
    model: WearSettingsViewModel,
    state: WearSettingsUiState,
    onDone: () -> Unit,
) {
    ChoiceScreen(
        title = stringResource(R.string.wear_settings_language),
        choices = state.languages.map { WearChoice(it.code, it.name) },
        selected = state.languageCode,
    ) { code ->
        model.chooseLanguage(code)
        onDone()
    }
}

@Composable
private fun CalendarChoice(
    model: WearSettingsViewModel,
    state: WearSettingsUiState,
    onDone: () -> Unit,
) {
    ChoiceScreen(
        title = stringResource(R.string.wear_settings_primary_calendar),
        choices = WearSettingsModel.PRIMARY_CALENDARS.map { WearChoice(it.name, stringResource(calendarLabel(it))) },
        selected = state.primaryCalendar?.name,
    ) { key ->
        WearSettingsModel.PRIMARY_CALENDARS.firstOrNull { it.name == key }?.let(model::choosePrimaryCalendar)
        onDone()
    }
}

@Composable
private fun MethodChoice(
    model: WearSettingsViewModel,
    state: WearSettingsUiState,
    onDone: () -> Unit,
) {
    ChoiceScreen(
        title = stringResource(R.string.wear_settings_prayer_method),
        choices = PrayerMethod.entries.map { WearChoice(it.name, stringResource(methodLabel(it))) },
        selected = state.prayerMethod?.name,
    ) { key ->
        PrayerMethod.entries.firstOrNull { it.name == key }?.let(model::choosePrayerMethod)
        onDone()
    }
}

@Composable
private fun CityChoice(
    model: WearSettingsViewModel,
    state: WearSettingsUiState,
    onDone: () -> Unit,
) {
    ChoiceScreen(
        title = stringResource(R.string.wear_settings_city),
        choices = state.cities.map { WearChoice(it.id.toString(), it.name) },
        selected = null,
    ) { key ->
        key.toLongOrNull()?.let(model::chooseCity)
        onDone()
    }
}

@Composable
private fun SettingsRoute(
    graph: WearGraph,
    content: @Composable (WearSettingsViewModel, WearSettingsUiState) -> Unit,
) {
    val model = viewModel { WearSettingsViewModel(graph) }
    val state by model.uiState.collectAsStateWithLifecycle()
    content(model, state)
}

/** Settings summary (T-1600): the current language, main calendar, prayer method and city. */
@Composable
fun SettingsScreen(
    state: WearSettingsUiState,
    onOpen: (String) -> Unit,
) {
    WearList {
        item { ListHeader { Text(stringResource(R.string.wear_settings)) } }
        item {
            val language =
                state.languages
                    .firstOrNull { it.code == state.languageCode }
                    ?.name
                    .orEmpty()
            WideButton(stringResource(R.string.wear_settings_language), { onOpen(WearRoutes.LANGUAGE) }, language)
        }
        item {
            val calendar = state.primaryCalendar?.let { stringResource(calendarLabel(it)) }
            WideButton(
                stringResource(R.string.wear_settings_primary_calendar),
                { onOpen(WearRoutes.CALENDAR) },
                calendar,
            )
        }
        item {
            val method = state.prayerMethod?.let { stringResource(methodLabel(it)) }
            WideButton(stringResource(R.string.wear_settings_prayer_method), { onOpen(WearRoutes.METHOD) }, method)
        }
        item {
            val place = state.placeName ?: stringResource(R.string.wear_settings_city_none)
            WideButton(stringResource(R.string.wear_settings_city), { onOpen(WearRoutes.CITY) }, place)
        }
    }
}

/** A list of [choices]; the [selected] one is marked, tapping one calls [onChoose] with its key. */
@Composable
fun ChoiceScreen(
    title: String,
    choices: List<WearChoice>,
    selected: String?,
    onChoose: (String) -> Unit,
) {
    WearList {
        item { ListHeader { Text(title) } }
        choices.forEach { choice ->
            item {
                val mark = if (choice.key == selected) stringResource(R.string.wear_selected) else null
                WideButton(choice.label, { onChoose(choice.key) }, mark)
            }
        }
    }
}
