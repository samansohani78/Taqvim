/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.OfficialReminderDao
import ir.taqvim.data.database.OfficialReminderEntity
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderDao
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.database.toRule
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.AlarmDelivery
import ir.taqvim.data.scheduler.AlarmKey
import ir.taqvim.data.scheduler.AlarmSource
import ir.taqvim.feature.calendar.OfficialReminderStore
import ir.taqvim.feature.notification.CalculatorOfficialEventSchedule
import ir.taqvim.feature.notification.OfficialReminder
import ir.taqvim.feature.notification.ReminderAlarm
import ir.taqvim.feature.notification.ReminderEvent
import ir.taqvim.feature.notification.ReminderOverride
import ir.taqvim.feature.notification.ReminderRule
import ir.taqvim.feature.notification.ReminderSetup
import ir.taqvim.feature.notification.ReminderSetupSource
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [ReminderSetupSource] (T-1001, T-1002) over the Room personal events and their enabled reminders (T-601) and the
 * enabled official reminder opt-ins, with the user's calendars and Islamic variant, official event titles in the app
 * language, the stored all-day reminder time and the device [zone].
 */
internal class RoomReminderSetupSource(
    private val events: PersonalEventDao,
    private val reminders: ReminderDao,
    private val officialReminders: OfficialReminderDao,
    private val preferences: UserPreferencesRepository,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val definitions: List<EventDefinition> = OfficialEvents.ALL,
) : ReminderSetupSource {
    override suspend fun current(): ReminderSetup {
        val current = preferences.preferences.first()
        val arithmetic = current.availableArithmetic()
        val calendars = CalendarProvider { arithmetic[it] }
        val personal =
            events.all().mapNotNull { entity ->
                reminderEvent(
                    entity,
                    arithmetic,
                    events.getRecurrence(entity.id)?.toRule(),
                    reminders.reminders(entity.id),
                    events.exceptionDays(entity.id),
                    events.overrides(entity.id),
                )
            }
        return ReminderSetup(
            events = personal,
            officials = officialReminders.all().mapNotNull(::officialReminder),
            schedule = CalculatorOfficialEventSchedule(definitions, current.languageSpec().code, calendars),
            zone = zone(),
            allDayTime = MinuteOfDay(current.app.allDayReminderMinute),
            calendars = calendars,
        )
    }
}

/** [entity] for the reminder planner, or `null` when it is off or unusable (no event id, lead time beyond 30 days). */
internal fun officialReminder(entity: OfficialReminderEntity): OfficialReminder? =
    entity
        .takeIf {
            it.enabled && it.id > 0 && it.eventId.isNotBlank() && it.daysBefore in 0..OfficialReminder.MAX_DAYS_BEFORE
        }?.let { OfficialReminder(it.id, EventId(it.eventId), it.daysBefore) }

/** [OfficialReminderStore] (T-1002) over the `official_reminders` table. */
internal class RoomOfficialReminderStore(
    private val dao: OfficialReminderDao,
) : OfficialReminderStore {
    override fun daysBefore(eventId: String): Flow<Set<Int>> =
        dao.observeAll().map { rows ->
            rows.filter { it.enabled && it.eventId == eventId }.map { it.daysBefore }.toSet()
        }

    override suspend fun setReminder(
        eventId: String,
        daysBefore: Int,
        enabled: Boolean,
    ) {
        require(eventId.isNotBlank()) { "official reminders need an event id" }
        require(daysBefore in 0..OfficialReminderEntity.MAX_DAYS_BEFORE) { "lead time must be 0..30 days" }
        if (enabled) {
            dao.insert(OfficialReminderEntity(eventId = eventId, daysBefore = daysBefore))
        } else {
            dao.delete(eventId, daysBefore)
        }
    }
}

/** Tables that reminder alarms are computed from (T-1001, T-1002). */
internal val REMINDER_INPUT_TABLES: Set<String> =
    setOf(
        "personal_events",
        "event_recurrences",
        "event_exceptions",
        "event_overrides",
        "reminders",
        "official_reminders",
    )

/**
 * Emits the changed table names whenever personal events, their rules, exceptions, overrides and reminders or official
 * opt-ins change.
 */
internal fun reminderInputChanges(database: TaqvimDatabase): Flow<Set<String>> =
    database.invalidationTracker.createFlow(
        "personal_events",
        "event_recurrences",
        "event_exceptions",
        "event_overrides",
        "reminders",
        "official_reminders",
        emitInitialState = false,
    )

/**
 * [entity] with its enabled [reminders], [exceptionDays] and [overrides] (T-1003) for the reminder planner, or `null`
 * when it has no reminders or its calendar cannot be computed (Nepali, T-105).
 */
internal fun reminderEvent(
    entity: PersonalEventEntity,
    arithmetic: Map<CalendarSystem, CalendarArithmetic>,
    recurrence: RecurrenceRule?,
    reminders: List<ReminderEntity>,
    exceptionDays: List<Long> = emptyList(),
    overrides: List<EventOverrideEntity> = emptyList(),
): ReminderEvent? {
    val calendar = arithmetic[entity.calendarSystem] ?: return null
    val rules =
        reminders
            .filter { it.enabled && it.id > 0 && it.minutesBefore in 0..ReminderRule.MAX_MINUTES_BEFORE }
            .map { ReminderRule(it.id, it.minutesBefore) }
    if (rules.isEmpty()) return null
    return ReminderEvent(
        id = entity.id,
        title = entity.title,
        start = calendar.fromJdn(Jdn(entity.startJdn)),
        startMinute = entity.startMinute?.let(::MinuteOfDay),
        timeZoneId = entity.timeZoneId,
        recurrence = recurrence,
        reminders = rules,
        exceptions = exceptionDays.map(::Jdn).toSet(),
        overrides =
            overrides.map {
                ReminderOverride(
                    original = Jdn(it.originalJdn),
                    day = Jdn(it.startJdn),
                    startMinute = it.startMinute?.let(::MinuteOfDay),
                    title = it.title,
                    cancelled = it.cancelled,
                )
            },
    )
}

/** The scheduler's reminder [AlarmSource] (T-604): one alarm per planned reminder, keyed by its source id. */
internal class ReminderAlarmSource(
    private val upcoming: suspend (now: Instant) -> List<ReminderAlarm>,
) : AlarmSource {
    override val kind: AlarmKind = AlarmKind.REMINDER

    override suspend fun upcomingAlarms(now: Instant): List<AlarmKey> =
        upcoming(now).map { AlarmKey(AlarmKind.REMINDER, it.sourceId, it.at) }
}

/** The scheduler's reminder [AlarmDelivery] (T-604): shows the reminder planned for the alarm's source and instant. */
internal class ReminderAlarmDelivery(
    private val onAlarm: suspend (sourceId: Long, triggerAt: Instant) -> Unit,
) : AlarmDelivery {
    override val kind: AlarmKind = AlarmKind.REMINDER

    override suspend fun deliver(alarm: ScheduledAlarmEntity) {
        val sourceId = alarm.sourceId ?: return
        onAlarm(sourceId, Instant.fromEpochMilliseconds(alarm.triggerAtEpochMillis))
    }
}
