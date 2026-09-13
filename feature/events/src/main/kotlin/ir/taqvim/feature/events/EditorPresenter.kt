/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

/** The editor's session state before localization. */
internal sealed interface EditorSession {
    data object Loading : EditorSession

    data object NotFound : EditorSession

    data class Editing(
        val form: EditorForm,
        /** The form as opened, to tell whether anything changed. */
        val original: EditorForm,
        val dateText: String = "",
        val dateTextUnrecognized: Boolean = false,
        val showErrors: Boolean = false,
        val busy: Boolean = false,
        val storeFailed: Boolean = false,
    ) : EditorSession

    data class Finished(
        val outcome: EditorOutcome,
    ) : EditorSession
}

/** Turns an [EditorSession] into the localized [EventEditorUiState]. */
internal object EditorPresenter {
    private const val MINUTES_PER_HOUR = 60
    private const val PICKER_YEARS = 100

    /** At the start, 5, 15 and 30 minutes, 1 hour, 1 day and 1 week before. */
    private val REMINDER_PRESETS = listOf(0, 5, 15, 30, 60, 1_440, 10_080)

    /** Event colors (ARGB): red, orange, amber, green, teal, blue, indigo and purple. */
    @Suppress("MagicNumber") // Palette values.
    val COLORS =
        persistentListOf(
            0xFFD32F2F.toInt(),
            0xFFEF6C00.toInt(),
            0xFFF9A825.toInt(),
            0xFF2E7D32.toInt(),
            0xFF00897B.toInt(),
            0xFF1565C0.toInt(),
            0xFF3949AB.toInt(),
            0xFF7B1FA2.toInt(),
        )

    fun present(
        session: EditorSession,
        settings: EditorSettings?,
    ): EventEditorUiState =
        EventEditorUiState(
            when (session) {
                EditorSession.Loading -> EditorContent.Loading
                EditorSession.NotFound -> EditorContent.NotFound
                is EditorSession.Finished -> EditorContent.Finished(session.outcome)
                is EditorSession.Editing -> settings?.let { editing(session, it) } ?: EditorContent.Loading
            },
        )

    private fun editing(
        session: EditorSession.Editing,
        settings: EditorSettings,
    ): EditorContent.Editing {
        val form = session.form
        val calendar = settings.arithmeticOf(form.calendar)
        val errors =
            if (session.showErrors) EventValidator.validate(form, calendar).toImmutableSet() else persistentSetOf()
        return EditorContent.Editing(
            form = form,
            display = display(form, calendar, settings),
            errors = errors,
            isNew = form.id == null,
            hasChanges = form != session.original,
            dateText = session.dateText,
            dateTextUnrecognized = session.dateTextUnrecognized,
            busy = session.busy,
            storeFailed = session.storeFailed,
        )
    }

    private fun display(
        form: EditorForm,
        calendar: CalendarArithmetic,
        settings: EditorSettings,
    ): EditorDisplay {
        val language = settings.language
        return EditorDisplay(
            calendars = (settings.calendars + form.calendar).distinct().toImmutableList(),
            startDate = longDate(form.start, calendar, language),
            endDate = longDate(form.end, calendar, language),
            startTime = clockTime(form.startMinute, language),
            endTime = clockTime(form.endMinute, language),
            untilDate = form.repeat?.let { longDate(it.until, calendar, language) },
            weekdays = weekdays(form, language),
            reminders = form.reminderMinutes.map { reminder(it, language) }.toImmutableList(),
            reminderChoices =
                REMINDER_PRESETS
                    .filterNot { it in form.reminderMinutes }
                    .map {
                        reminder(
                            it,
                            language,
                        )
                    }.toImmutableList(),
            colors = COLORS,
            picker = picker(form, calendar, language),
        )
    }

    private fun longDate(
        date: CalendarDate,
        calendar: CalendarArithmetic,
        language: LanguageSpec,
    ): String = DateFormatter.format(date, calendar.toJdn(date).weekday(), language, DateStyle.LONG)

    private fun clockTime(
        minute: Int,
        language: LanguageSpec,
    ): String {
        val hours = (minute / MINUTES_PER_HOUR).toString().padStart(2, '0')
        val minutes = (minute % MINUTES_PER_HOUR).toString().padStart(2, '0')
        return Numerals.localizeDigits("$hours:$minutes", language.numerals)
    }

    private fun weekdays(
        form: EditorForm,
        language: LanguageSpec,
    ): ImmutableList<WeekdayOption> {
        val all = FormatTable.of(language).weekdays
        val names = all[form.calendar] ?: all.getValue(CalendarSystem.GREGORIAN)
        val selected = form.repeat?.weekdays.orEmpty()
        return List(Weekday.entries.size) { Weekday.entries[(language.weekStart.ordinal + it) % Weekday.entries.size] }
            .map { WeekdayOption(it, names[it.ordinal], it in selected) }
            .toImmutableList()
    }

    private fun reminder(
        minutes: Int,
        language: LanguageSpec,
    ): ReminderOption {
        val amount =
            if (minutes == 0) {
                ""
            } else {
                DurationFormatter.format(minutes.minutes, language)
                    ?: Numerals.format(minutes.toLong(), language.numerals)
            }
        return ReminderOption(minutes, amount)
    }

    private fun picker(
        form: EditorForm,
        calendar: CalendarArithmetic,
        language: LanguageSpec,
    ): CalendarPickerData {
        val months = calendar.monthsInYear(form.start.year)
        val names =
            FormatTable.of(language).monthNames[form.calendar]?.takeIf { it.size == months }
                ?: List(months) { Numerals.format(it + 1L, language.numerals) }
        val years = (form.start.year - PICKER_YEARS)..(form.start.year + PICKER_YEARS)
        return CalendarPickerData(calendar, names.toImmutableList(), years, language.numerals)
    }
}
