/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.nlp.DateParser
import ir.taqvim.core.nlp.ParseContext
import ir.taqvim.core.ui.component.DateSelection

/** A change made in the editor form (T-1000). */
sealed interface EditorIntent

/** Title, notes, color and source link. */
sealed interface DetailsIntent : EditorIntent {
    data class Title(
        val text: String,
    ) : DetailsIntent

    data class Notes(
        val text: String,
    ) : DetailsIntent

    data class Color(
        val argb: Int?,
    ) : DetailsIntent

    data class Link(
        val text: String,
    ) : DetailsIntent
}

/** Calendar, dates and times. */
sealed interface ScheduleIntent : EditorIntent {
    /** Shows the same days in [system]. */
    data class ChangeCalendar(
        val system: CalendarSystem,
    ) : ScheduleIntent

    /** Moves the start, keeping the event's length in days. */
    data class StartDate(
        val date: DateSelection,
    ) : ScheduleIntent

    data class EndDate(
        val date: DateSelection,
    ) : ScheduleIntent

    data class AllDay(
        val allDay: Boolean,
    ) : ScheduleIntent

    /** Moves the start time, keeping the duration of a one-day event. */
    data class StartTime(
        val minute: Int,
    ) : ScheduleIntent

    data class EndTime(
        val minute: Int,
    ) : ScheduleIntent
}

/** Turns repetition on with [frequency], or off when it is `null`. */
data class SetRepeat(
    val frequency: Frequency?,
) : EditorIntent

/** A change to the repetition; ignored while the event does not repeat. */
sealed interface RepeatIntent : EditorIntent {
    data class Interval(
        val text: String,
    ) : RepeatIntent

    data class Ends(
        val end: RecurrenceEnd,
    ) : RepeatIntent

    data class Count(
        val text: String,
    ) : RepeatIntent

    data class Until(
        val date: DateSelection,
    ) : RepeatIntent

    data class ToggleWeekday(
        val weekday: Weekday,
    ) : RepeatIntent

    data class InvalidDays(
        val policy: InvalidDatePolicy,
    ) : RepeatIntent
}

/** Reminder offsets in minutes before the start. */
sealed interface ReminderIntent : EditorIntent {
    data class Add(
        val minutes: Int,
    ) : ReminderIntent

    data class Remove(
        val minutes: Int,
    ) : ReminderIntent
}

/** Applies [EditorIntent]s to an [EditorForm]; pure. */
internal object EditorReducer {
    fun reduce(
        form: EditorForm,
        intent: EditorIntent,
        settings: EditorSettings,
    ): EditorForm =
        when (intent) {
            is DetailsIntent -> {
                details(form, intent)
            }

            is ScheduleIntent -> {
                schedule(form, intent, settings)
            }

            is SetRepeat -> {
                form.copy(repeat = intent.frequency?.let { frequency(form, it, settings) })
            }

            is RepeatIntent -> {
                val calendar = settings.arithmeticOf(form.calendar)
                form.repeat?.let { form.copy(repeat = repeat(it, intent, calendar)) } ?: form
            }

            is ReminderIntent -> {
                reminders(form, intent)
            }
        }

    private fun details(
        form: EditorForm,
        intent: DetailsIntent,
    ): EditorForm =
        when (intent) {
            is DetailsIntent.Title -> form.copy(title = intent.text)
            is DetailsIntent.Notes -> form.copy(notes = intent.text)
            is DetailsIntent.Color -> form.copy(colorArgb = intent.argb)
            is DetailsIntent.Link -> form.copy(sourceLink = intent.text)
        }

    private fun schedule(
        form: EditorForm,
        intent: ScheduleIntent,
        settings: EditorSettings,
    ): EditorForm {
        val calendar = settings.arithmeticOf(form.calendar)
        return when (intent) {
            is ScheduleIntent.ChangeCalendar -> changeCalendar(form, calendar, settings.arithmeticOf(intent.system))
            is ScheduleIntent.StartDate -> moveStart(form, calendar.dateOf(intent.date), calendar)
            is ScheduleIntent.EndDate -> form.copy(end = calendar.dateOf(intent.date))
            is ScheduleIntent.AllDay -> form.copy(allDay = intent.allDay)
            is ScheduleIntent.StartTime -> moveStartTime(form, intent.minute)
            is ScheduleIntent.EndTime -> form.copy(endMinute = intent.minute)
        }
    }

    private fun changeCalendar(
        form: EditorForm,
        from: CalendarArithmetic,
        to: CalendarArithmetic,
    ): EditorForm {
        fun convert(date: CalendarDate): CalendarDate = to.fromJdn(from.toJdn(date))
        return form.copy(
            calendar = to.system,
            start = convert(form.start),
            end = convert(form.end),
            repeat = form.repeat?.let { it.copy(until = convert(it.until)) },
        )
    }

    private fun moveStart(
        form: EditorForm,
        start: CalendarDate,
        calendar: CalendarArithmetic,
    ): EditorForm {
        val length = (calendar.toJdn(form.end) - calendar.toJdn(form.start)).coerceAtLeast(0)
        return form.copy(start = start, end = calendar.fromJdn(calendar.toJdn(start) + length))
    }

    private fun moveStartTime(
        form: EditorForm,
        minute: Int,
    ): EditorForm {
        val duration = (form.endMinute - form.startMinute).coerceAtLeast(0)
        val endMinute =
            if (form.start == form.end) (minute + duration).coerceAtMost(EditorForm.LAST_MINUTE) else form.endMinute
        return form.copy(startMinute = minute, endMinute = endMinute)
    }

    private fun frequency(
        form: EditorForm,
        frequency: Frequency,
        settings: EditorSettings,
    ): RepeatForm {
        val current = form.repeat ?: return RepeatForm.create(frequency, form.start, settings.language.numerals)
        val weekdays = if (frequency == Frequency.WEEKLY) current.weekdays else emptySet()
        return current.copy(frequency = frequency, weekdays = weekdays)
    }

    private fun repeat(
        repeat: RepeatForm,
        intent: RepeatIntent,
        calendar: CalendarArithmetic,
    ): RepeatForm =
        when (intent) {
            is RepeatIntent.Interval -> {
                repeat.copy(intervalText = intent.text)
            }

            is RepeatIntent.Ends -> {
                repeat.copy(end = intent.end)
            }

            is RepeatIntent.Count -> {
                repeat.copy(countText = intent.text)
            }

            is RepeatIntent.Until -> {
                repeat.copy(until = calendar.dateOf(intent.date))
            }

            is RepeatIntent.InvalidDays -> {
                repeat.copy(invalidDates = intent.policy)
            }

            is RepeatIntent.ToggleWeekday -> {
                val weekdays = repeat.weekdays
                val toggled = if (intent.weekday in weekdays) weekdays - intent.weekday else weekdays + intent.weekday
                repeat.copy(weekdays = toggled)
            }
        }

    private fun reminders(
        form: EditorForm,
        intent: ReminderIntent,
    ): EditorForm {
        val minutes =
            when (intent) {
                is ReminderIntent.Add -> (form.reminderMinutes + intent.minutes).distinct().sorted()
                is ReminderIntent.Remove -> form.reminderMinutes.filterNot { it == intent.minutes }
            }
        return form.copy(reminderMinutes = minutes)
    }
}

/** Applies a typed date phrase (T-500, F-03): the start, and a range's end, in the form's calendar. */
internal object DatePhrase {
    /** The form with the date of [text], or `null` when [text] contains no date. */
    fun apply(
        form: EditorForm,
        text: String,
        settings: EditorSettings,
        today: Jdn,
    ): EditorForm? {
        val calendar = settings.arithmeticOf(form.calendar)
        val context =
            ParseContext(
                reference = today,
                preferredCalendar = form.calendar,
                numericOrder = settings.language.datePattern.order,
                anchors = settings.anchors,
                calendars = ParseContext.calendarsFor(settings.calendars + form.calendar, settings.arithmetic),
            )
        val result = text.takeIf { it.isNotBlank() }?.let { DateParser.parseBest(it, context) } ?: return null
        val length = (calendar.toJdn(form.end) - calendar.toJdn(form.start)).coerceAtLeast(0)
        val end = result.end?.jdn ?: (result.jdn + length)
        return form.copy(start = calendar.fromJdn(result.jdn), end = calendar.fromJdn(end))
    }
}

/** The date of [selection] in this calendar. */
internal fun CalendarArithmetic.dateOf(selection: DateSelection): CalendarDate =
    date(selection.year, selection.month, selection.day)
