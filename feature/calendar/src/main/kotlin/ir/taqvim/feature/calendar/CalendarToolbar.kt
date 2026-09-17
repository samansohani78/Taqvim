/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import ir.taqvim.core.calendar.HebrewCalendar
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.monthNamesOf
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerModel
import ir.taqvim.core.ui.component.DatePickerSheet
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.ui.component.TopBar
import ir.taqvim.core.ui.component.TopBarAction

/** Test tag of the toolbar menu (T-803). */
const val CALENDAR_MENU_TAG: String = "calendar_menu"

private const val MONTHS_IN_YEAR = 12
private const val FALLBACK_LANGUAGE = "en"

/**
 * The toolbar (T-803): the shown month in the primary calendar with its span in the secondary (and further) calendars,
 * then today, search and the menu.
 */
@Composable
internal fun CalendarTopBar(
    content: CalendarContent,
    heading: MonthHeading,
    onAction: (CalendarAction) -> Unit,
) {
    val actions =
        listOf(
            TopBarAction(
                icon = ImageVector.vectorResource(R.drawable.calendar_ic_today),
                label = stringResource(R.string.calendar_today),
                onClick = { onAction(CalendarAction.GoToToday) },
            ),
            TopBarAction(
                icon = ImageVector.vectorResource(R.drawable.calendar_ic_search),
                label = stringResource(R.string.calendar_action_search),
                onClick = { onAction(CalendarAction.OpenSearchScreen) },
            ),
            TopBarAction(
                icon = ImageVector.vectorResource(R.drawable.calendar_ic_more),
                label = stringResource(R.string.calendar_action_more),
                onClick = { onAction(CalendarAction.OpenMenu) },
            ),
        )
    Box {
        TopBar(heading.title, subtitle = heading.subtitle, actions = actions)
        Box(Modifier.align(Alignment.TopEnd)) { CalendarMenuPopup(content, onAction) }
    }
}

@Composable
private fun CalendarMenuPopup(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
) {
    val weekNumbers = content.showWeekNumbers
    DropdownMenu(
        expanded = content.menu.isOpen,
        onDismissRequest = { onAction(CalendarAction.DismissMenu) },
        modifier = Modifier.testTag(CALENDAR_MENU_TAG),
    ) {
        MenuItem(R.string.calendar_menu_pick_date) { onAction(CalendarAction.OpenDatePicker) }
        MenuItem(R.string.calendar_menu_shift_work) { onAction(CalendarAction.OpenShiftWork) }
        MenuItem(R.string.calendar_menu_print_month) { onAction(CalendarAction.PrintMonth) }
        MenuItem(R.string.calendar_menu_planetary_hours) { onAction(CalendarAction.OpenPlanetaryHours) }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.calendar_menu_week_numbers)) },
            onClick = { onAction(CalendarAction.ShowWeekNumbers(!weekNumbers)) },
            modifier = Modifier.semantics { toggleableState = ToggleableState(weekNumbers) },
            trailingIcon = { Checkbox(checked = weekNumbers, onCheckedChange = null) },
        )
        MenuItem(R.string.calendar_menu_secondary_calendar) { onAction(CalendarAction.OpenSecondaryCalendarChooser) }
    }
}

@Composable
private fun MenuItem(
    @StringRes label: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

/** The dialog opened from the menu, if any (T-803). */
@Composable
internal fun CalendarDialogs(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
) {
    when (content.menu.dialog) {
        CalendarDialog.DATE_PICKER -> GoToDateSheet(content, onAction)
        CalendarDialog.SECONDARY_CALENDAR -> SecondaryCalendarDialog(content, onAction)
        null -> Unit
    }
}

@Composable
private fun GoToDateSheet(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
) {
    val labels =
        DatePickerLabels(
            title = stringResource(R.string.calendar_menu_pick_date),
            year = stringResource(R.string.calendar_picker_year),
            month = stringResource(R.string.calendar_picker_month),
            day = stringResource(R.string.calendar_picker_day),
            confirm = stringResource(R.string.calendar_picker_confirm),
            cancel = stringResource(R.string.calendar_picker_cancel),
        )
    DatePickerSheet(
        model = goToDateModel(content, labels),
        onConfirm = { onAction(CalendarAction.PickDate(it.year, it.month, it.day)) },
        onDismiss = { onAction(CalendarAction.DismissDialog) },
    )
}

@Composable
private fun SecondaryCalendarDialog(
    content: CalendarContent,
    onAction: (CalendarAction) -> Unit,
) {
    val secondary = content.calendars.getOrNull(1)
    AlertDialog(
        onDismissRequest = { onAction(CalendarAction.DismissDialog) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onAction(CalendarAction.DismissDialog) }) {
                Text(stringResource(R.string.calendar_close))
            }
        },
        title = { Text(stringResource(R.string.calendar_menu_secondary_calendar)) },
        text = {
            Column(Modifier.selectableGroup()) {
                content.secondaryChoices.forEach { system ->
                    SecondaryCalendarOption(system, system == secondary) {
                        onAction(CalendarAction.ChooseSecondaryCalendar(system))
                    }
                }
            }
        },
    )
}

@Composable
private fun SecondaryCalendarOption(
    system: CalendarSystem,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(DayDetailsLabels.of(system)))
    }
}

/** The go-to-date picker in the primary calendar, starting at the selected day (T-803). */
internal fun goToDateModel(
    content: CalendarContent,
    labels: DatePickerLabels,
): DatePickerModel {
    val calendars = CalendarCalendars(content.settings())
    val language = languageOf(content.languageCode)
    val selected = content.selectedDates.first()
    return DatePickerModel(
        initial = DateSelection(selected.year, selected.month, selected.day),
        years = calendars.pagedYears(content.today),
        monthNames = monthNamesOf(language, selected.system, selected.year),
        daysInMonth = calendars::primaryMonthLength,
        formatNumber = { number(it.toLong(), language) },
        labels = labels,
        monthNamesIn = { year -> monthNamesOf(language, selected.system, year) },
    )
}

/**
 * Month names of [system]'s [year] in [language], else in English, else the month numbers in the language's digits
 * (13 in a Hebrew leap year, F07).
 */
internal fun monthNamesOf(
    language: LanguageSpec,
    system: CalendarSystem,
    year: Int,
): List<String> =
    language.monthNamesOf(system, year)
        ?: LanguageTable.forCode(FALLBACK_LANGUAGE)?.monthNamesOf(system, year)
        ?: (1..monthCount(system, year)).map { number(it.toLong(), language) }

private fun monthCount(
    system: CalendarSystem,
    year: Int,
): Int = if (system == CalendarSystem.HEBREW) HebrewCalendar.monthsInYear(year) else MONTHS_IN_YEAR
