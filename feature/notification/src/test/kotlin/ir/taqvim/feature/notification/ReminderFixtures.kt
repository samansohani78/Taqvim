/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Synthetic reminder setups and official events; no real personal data. */
internal object ReminderFixtures {
    val TEHRAN: TimeZone = TimeZone.of("Asia/Tehran")

    /** The day number of [date] in its own calendar. */
    fun jdnOf(date: CalendarDate): Jdn = requireNotNull(CalendarProvider.DEFAULT.calendarFor(date.system)).toJdn(date)

    fun persian(
        year: Int,
        month: Int,
        day: Int,
    ): CalendarDate = CalendarDate(CalendarSystem.PERSIAN, year, month, day)

    fun event(
        start: CalendarDate,
        id: Long = 1,
        startMinute: MinuteOfDay? = null,
        zone: String = "Asia/Tehran",
        recurrence: RecurrenceRule? = null,
        reminders: List<ReminderRule> = listOf(ReminderRule(id, 0)),
    ): ReminderEvent = ReminderEvent(id, "Event $id", start, startMinute, zone, recurrence, reminders)

    /** A schedule without official events. */
    val NO_OFFICIAL_EVENTS: OfficialEventSchedule =
        object : OfficialEventSchedule {
            override fun title(eventId: EventId): String? = null

            override fun days(
                eventId: EventId,
                from: Jdn,
                until: Jdn,
            ): List<Jdn> = emptyList()
        }

    fun setup(
        events: List<ReminderEvent>,
        officials: List<OfficialReminder> = emptyList(),
        schedule: OfficialEventSchedule = NO_OFFICIAL_EVENTS,
        zone: TimeZone = TEHRAN,
    ): ReminderSetup = ReminderSetup(events, officials, schedule, zone)

    /** Nowruz as the dataset defines it (1 Farvardin), with synthetic titles. */
    val NOWRUZ =
        EventDefinition(
            id = EventId("ir.holiday.nowruz-1"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "Nowruz (fa)", "en" to "Nowruz")),
            rule = EventRule.Fixed(month = 1, day = 1),
        )

    /** Nowruz 1405 = 21 March 2026. */
    val NOWRUZ_1405: Jdn = LocalDate(2026, 3, 21).toJdn()

    fun planned(
        kind: ReminderKind = ReminderKind.PERSONAL,
        sourceId: Long = 5,
        target: String = "42",
        daysBefore: Int = 0,
    ): PlannedReminder =
        PlannedReminder(
            kind = kind,
            sourceId = sourceId,
            target = target,
            title = "Dentist",
            occurrence = NOWRUZ_1405,
            daysBefore = daysBefore,
            at = Instant.parse("2026-03-18T05:30:00Z"),
        )
}

/** [ReminderNotifier] that records what it shows and accepts or refuses every reminder. */
internal class RecordingNotifier(
    private val accept: Boolean = true,
) : ReminderNotifier {
    val shown = mutableListOf<PlannedReminder>()

    override fun show(reminder: PlannedReminder): Boolean {
        if (accept) shown += reminder
        return accept
    }
}

/** [SnoozeScheduler] that records snoozes and counts down [done] for each. */
internal class RecordingSnoozer : SnoozeScheduler {
    val reminders = CopyOnWriteArrayList<Pair<PlannedReminder, Instant>>()
    val athans = CopyOnWriteArrayList<Pair<PlannedAthan, Instant>>()
    val done = CountDownLatch(1)

    override suspend fun snoozeReminder(
        reminder: PlannedReminder,
        at: Instant,
    ) {
        reminders += reminder to at
        done.countDown()
    }

    override suspend fun snoozeAthan(
        athan: PlannedAthan,
        at: Instant,
    ) {
        athans += athan to at
        done.countDown()
    }
}
