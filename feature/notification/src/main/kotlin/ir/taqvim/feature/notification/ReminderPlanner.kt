/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventId
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.ics.seriesInstances
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** One reminder of a personal event: [minutesBefore] each occurrence starts; [id] is its reminder row (T-601). */
data class ReminderRule(
    val id: Long,
    val minutesBefore: Int,
) {
    init {
        require(id > 0) { "reminder id must be positive (was $id)" }
        require(minutesBefore in 0..MAX_MINUTES_BEFORE) { "minutesBefore must be within 0..$MAX_MINUTES_BEFORE" }
    }

    companion object {
        /** Four weeks, the longest reminder the editor offers (T-1000). */
        const val MAX_MINUTES_BEFORE: Int = 40_320
    }
}

/** A personal event with its reminders (T-601/T-1000), dated in its own calendar. */
data class ReminderEvent(
    val id: Long,
    val title: String,
    val start: CalendarDate,
    /** Start time in [timeZoneId]; `null` for an all-day event. */
    val startMinute: MinuteOfDay?,
    val timeZoneId: String,
    /** Repetition counted in the event's calendar (ADR-0011), or `null` for a one-off event. */
    val recurrence: RecurrenceRule?,
    val reminders: List<ReminderRule>,
    /** Days on which the event does not occur (T-1003). */
    val exceptions: Set<Jdn> = emptySet(),
    /** Changed occurrences (T-1003). */
    val overrides: List<ReminderOverride> = emptyList(),
)

/**
 * The occurrence of a personal event that would start on [original], changed (T-1003): it starts on [day] at
 * [startMinute] (`null` all day) with [title], or does not take place when [cancelled].
 */
data class ReminderOverride(
    val original: Jdn,
    val day: Jdn,
    val startMinute: MinuteOfDay?,
    val title: String,
    val cancelled: Boolean = false,
)

/** A reminder attached to an official event (T-1002): [daysBefore] each occurrence, at the all-day reminder time. */
data class OfficialReminder(
    val id: Long,
    val eventId: EventId,
    val daysBefore: Int,
) {
    init {
        require(id > 0) { "official reminder id must be positive (was $id)" }
        require(daysBefore in 0..MAX_DAYS_BEFORE) { "daysBefore must be within 0..$MAX_DAYS_BEFORE" }
    }

    companion object {
        /** The longest lead time offered for official events (product choice). */
        const val MAX_DAYS_BEFORE: Int = 30
    }
}

/** Titles and occurrence days of official events; see [CalculatorOfficialEventSchedule]. */
interface OfficialEventSchedule {
    /** The title of [eventId] in the app language, or `null` when the event is not known. */
    fun title(eventId: EventId): String?

    /** Days from [from] to [until] (inclusive) on which [eventId] occurs, ascending. */
    fun days(
        eventId: EventId,
        from: Jdn,
        until: Jdn,
    ): List<Jdn>
}

/** Everything that decides when reminders are due. */
data class ReminderSetup(
    val events: List<ReminderEvent>,
    val officials: List<OfficialReminder>,
    val schedule: OfficialEventSchedule,
    /** The device zone; all-day events and official reminders sound at [allDayTime] in it. */
    val zone: TimeZone,
    val allDayTime: MinuteOfDay = DEFAULT_ALL_DAY_TIME,
    /** Calendars of personal events, with the user's Islamic variant. */
    val calendars: CalendarProvider = CalendarProvider.DEFAULT,
) {
    companion object {
        /** When all-day and official reminders sound unless the user picks another time (product choice). */
        val DEFAULT_ALL_DAY_TIME: MinuteOfDay = MinuteOfDay.of(9, 0)
    }
}

/** What a reminder belongs to; each kind has its own notification channel. */
enum class ReminderKind {
    PERSONAL,
    OFFICIAL,
}

/**
 * One reminder to show at [at] for the occurrence of [target] that takes place on [occurrence]. [original] is the day
 * the occurrence belongs to in its series (the day an override moved it from; [occurrence] otherwise), which identifies
 * it even when a moved occurrence lands on the day of another one.
 */
data class PlannedReminder(
    val kind: ReminderKind,
    /** Reminder row id (personal) or official reminder id. */
    val sourceId: Long,
    /** The personal event id, or the official [EventId] value. */
    val target: String,
    val title: String,
    val occurrence: Jdn,
    /** Days between the reminder and the occurrence of an official event; 0 for personal reminders. */
    val daysBefore: Int,
    val at: Instant,
    val original: Jdn = occurrence,
    /**
     * Whether this is the occurrence the planner kept before series identity was used (the one with the earliest
     * [original] among those of its reminder taking place on [occurrence]), so [legacyKey] refers to it.
     */
    val legacyKeyed: Boolean = true,
) {
    /** Identifies the reminder of one occurrence by its series day, so it is shown at most once (ADR-0033 addendum). */
    val key: String
        get() = "${kind.name}:$sourceId#${original.value}"

    /**
     * The key used before 2026-09-17 (`kind:source@day the occurrence takes place`), under which reminders delivered
     * by older versions are recorded; only meaningful when [legacyKeyed].
     */
    val legacyKey: String
        get() = "${kind.name}:$sourceId@${occurrence.value}"

    /** Whether an override moved this occurrence to another day. */
    val moved: Boolean
        get() = original != occurrence

    /** The scheduler's source id: personal reminders keep their id, official ones are negated, so they never clash. */
    val alarmSourceId: Long
        get() = alarmSourceIdOf(kind, sourceId)

    companion object {
        /** The scheduler source id of reminder [sourceId] of [kind]. */
        fun alarmSourceIdOf(
            kind: ReminderKind,
            sourceId: Long,
        ): Long = if (kind == ReminderKind.PERSONAL) sourceId else -sourceId
    }
}

/**
 * When reminders are due (T-1001, T-1002); pure, so the alarm source and the delivery agree on every instant. Personal
 * occurrences are expanded in the event's own calendar (ADR-0011); timed events start in their zone, all-day events and
 * official reminders at [ReminderSetup.allDayTime] in the device zone.
 */
object ReminderPlanner {
    /** How far ahead reminders are planned; yearly events stay covered until the next replanning. */
    const val HORIZON_DAYS: Int = 400

    /** The most alarms handed to the scheduler at once (the nearest ones); the rest follow at the next replanning. */
    const val MAX_REMINDERS: Int = 50

    private const val LEAD_DAYS = 31
    private const val ZONE_SLACK_DAYS = 2

    /** Up to [MAX_REMINDERS] reminders strictly after [now] and within [HORIZON_DAYS], in time order. */
    fun upcoming(
        now: Instant,
        setup: ReminderSetup,
    ): List<PlannedReminder> = candidates(now, setup) { true }.take(MAX_REMINDERS)

    /** The first reminder of scheduler source [alarmSourceId] due exactly at [instant], if any. */
    fun at(
        alarmSourceId: Long,
        instant: Instant,
        setup: ReminderSetup,
    ): PlannedReminder? = allAt(alarmSourceId, instant, setup).firstOrNull()

    /**
     * Every reminder of scheduler source [alarmSourceId] due exactly at [instant]: more than one when an override moves
     * an occurrence onto the start time of another occurrence of the same event.
     */
    fun allAt(
        alarmSourceId: Long,
        instant: Instant,
        setup: ReminderSetup,
    ): List<PlannedReminder> =
        candidates(instant - 1.milliseconds, setup) { it == alarmSourceId }.filter { it.at == instant }

    private fun candidates(
        now: Instant,
        setup: ReminderSetup,
        wanted: (Long) -> Boolean,
    ): List<PlannedReminder> {
        val today = now.toJdn(setup.zone)
        val from = today - ZONE_SLACK_DAYS
        val until = today + HORIZON_DAYS + LEAD_DAYS + ZONE_SLACK_DAYS
        val last = now + HORIZON_DAYS.days
        val personal = setup.events.flatMap { personal(it, setup, from, until, wanted) }
        val official =
            setup.officials
                .filter { wanted(PlannedReminder.alarmSourceIdOf(ReminderKind.OFFICIAL, it.id)) }
                .flatMap { official(it, setup, from, until) }
        return (personal + official)
            .filter { it.at > now && it.at <= last }
            .distinctBy { it.key }
            .sortedWith(compareBy<PlannedReminder> { it.at }.thenBy { it.key })
    }

    private fun personal(
        event: ReminderEvent,
        setup: ReminderSetup,
        from: Jdn,
        until: Jdn,
        wanted: (Long) -> Boolean,
    ): List<PlannedReminder> {
        val rules = event.reminders.filter { wanted(it.id) }
        if (rules.isEmpty()) return emptyList()
        val instances = instances(event, setup.calendars, from, until)
        val firstOriginal = instances.groupBy { it.day }.mapValues { (_, same) -> same.minOf { it.original } }
        return instances.flatMap { instance ->
            val start = startOf(event, instance, setup)
            rules.map { rule ->
                PlannedReminder(
                    kind = ReminderKind.PERSONAL,
                    sourceId = rule.id,
                    target = event.id.toString(),
                    title = instance.title,
                    occurrence = instance.day,
                    daysBefore = 0,
                    at = start - rule.minutesBefore.minutes,
                    original = instance.original,
                    legacyKeyed = firstOriginal[instance.day] == instance.original,
                )
            }
        }
    }

    /**
     * An occurrence of a personal event as it takes place: on [day] at [startMinute] with [title]; [original] is its day
     * in the series.
     */
    private data class Instance(
        val original: Jdn,
        val day: Jdn,
        val startMinute: MinuteOfDay?,
        val title: String,
    )

    private fun official(
        reminder: OfficialReminder,
        setup: ReminderSetup,
        from: Jdn,
        until: Jdn,
    ): List<PlannedReminder> {
        val title = setup.schedule.title(reminder.eventId) ?: return emptyList()
        return setup.schedule.days(reminder.eventId, from, until).map { day ->
            PlannedReminder(
                kind = ReminderKind.OFFICIAL,
                sourceId = reminder.id,
                target = reminder.eventId.value,
                title = title,
                occurrence = day,
                daysBefore = reminder.daysBefore,
                at = instantAt(day - reminder.daysBefore, setup.allDayTime, setup.zone),
            )
        }
    }

    /**
     * Occurrences of [event] taking place from [from] to [until], without exception days and cancelled occurrences and
     * with changed ones moved (T-1003); none when its calendar is unavailable or its start invalid.
     */
    private fun instances(
        event: ReminderEvent,
        calendars: CalendarProvider,
        from: Jdn,
        until: Jdn,
    ): List<Instance> {
        val calendar = calendars.calendarFor(event.start.system) ?: return emptyList()
        val first = runCatching { calendar.toJdn(event.start) }.getOrNull() ?: return emptyList()
        val all = event.recurrence?.let { RecurrenceEngine(calendar).occurrences(event.start, it) } ?: sequenceOf(first)
        val (cancelled, kept) = event.overrides.partition { it.cancelled }
        val excluded = event.exceptions + cancelled.map { it.original }
        return seriesInstances(all, excluded, kept.associateBy { it.original }, from, until)
            .map { instance ->
                instance.override?.let { Instance(instance.original, it.day, it.startMinute, it.title) }
                    ?: Instance(instance.original, instance.original, event.startMinute, event.title)
            }.filter { it.day in from..until }
    }

    private fun startOf(
        event: ReminderEvent,
        instance: Instance,
        setup: ReminderSetup,
    ): Instant {
        val minute = instance.startMinute ?: return instantAt(instance.day, setup.allDayTime, setup.zone)
        val zone = runCatching { TimeZone.of(event.timeZoneId) }.getOrDefault(setup.zone)
        return instantAt(instance.day, minute, zone)
    }

    /** [time] on [day] in [zone]; a time skipped by a daylight-saving change moves forward by the gap. */
    private fun instantAt(
        day: Jdn,
        time: MinuteOfDay,
        zone: TimeZone,
    ): Instant = day.toLocalDate().atTime(time.hour, time.minute).toInstant(zone)
}
