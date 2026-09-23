/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.segmentTag
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.scheduler.AlarmStore
import ir.taqvim.feature.calendar.DayDetailsTab
import ir.taqvim.feature.events.PersonalEvent
import ir.taqvim.feature.events.PersonalEventStore
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * J12 (docs/device-journeys.md): a personal event with a reminder does not just open a screen — it persists to where
 * the plan promises it shows (its day, the agenda) and its reminder becomes a row [AlarmStore] holds, i.e. it was
 * actually scheduled, not merely requested. [DeviceReminderTest] already proves the last leg end to end (the system
 * fires the alarm and a notification appears); this test proves the first two legs and that scheduling itself is
 * recorded promptly, without paying for that test's multi-minute wait for delivery.
 *
 * A first version matched the scheduled row by `it.sourceId == id`, `id` being the saved event's row id
 * (`PersonalEventStore.save`'s return value); it timed out on every run (CI run 35854629904, API 33 and 36).
 * `PlannedReminder.sourceId`/`ScheduledAlarmEntity.sourceId`'s own doc ("reminder row id (personal) or official
 * reminder id") says why: a `REMINDER` alarm's source id is the *reminder rule's own row id* in the `reminders`
 * table (`ReminderRule.id` in `feature/notification/ReminderPlanner.kt`), a different table's primary key from the
 * personal event's id — the two are unrelated numbers on a device that already has other rows, so the match could
 * only ever succeed by coincidence. [DeviceReminderTest] never hit this because it waits for the *notification*
 * instead, which does not depend on that id at all. The fix below instead asks whether scheduling added a new
 * `REMINDER` row at all (comparing ids seen before the save against the ids seen while waiting), which needs no
 * knowledge of which table's id ends up as the source id, and additionally requires the new row's trigger time to
 * match the event's own reminder time so a reminder from an unrelated, concurrently-running alarm cannot pass this
 * check by accident. `AlarmInputWatcher` (`data/scheduler/RescheduleCoordinator.kt`) debounces personal-event
 * changes by 2 seconds before recomputing, so this is comfortably possible within the wait below.
 *
 * The day and agenda checks needed two more fixes, shared with [DeviceHolidayTest] and [DeviceBackupRestoreTest] and
 * found the same way while reproducing their CI failures locally on `d1api33`: `AppDestination.Day`'s day-details
 * pane defaults to the `CALENDARS` tab, not `EVENTS` (`DayDetailsPanel.kt`), fixed by selecting
 * `segmentTag(DayDetailsTab.EVENTS.ordinal)` first; and both the day's `EventChip` and the agenda's own event row
 * (`feature/agenda/AgendaRows.kt`'s `DayRow`, also built from `EventChip`) use
 * `.clearAndSetSemantics { contentDescription = ... }` (`core/ui/component/EventChip.kt`), which replaces the row's
 * text with a `contentDescription` starting with the title — `By.textContains` cannot see it, `By.descContains` can.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // useLanguage needs LocaleManager (ADR-0023)
class DeviceEventLifecycleTest {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun createdEventShowsOnItsDayAndAgendaAndSchedulesAReminder() {
        useLanguage("en")
        shell("appops set ${appContext.packageName} SCHEDULE_EXACT_ALARM allow")
        val store = GlobalContext.get().get<PersonalEventStore>()
        val alarms = GlobalContext.get().get<AlarmStore>()
        val start = (Clock.System.now() + LEAD_MINUTES.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        val date = CalendarDate(CalendarSystem.GREGORIAN, start.year, start.month.ordinal + 1, start.day)
        val minute = start.hour * MINUTES_PER_HOUR + start.minute
        val event =
            PersonalEvent(
                title = TITLE,
                calendar = CalendarSystem.GREGORIAN,
                start = date,
                end = date,
                startMinute = minute,
                endMinute = (minute + 1).coerceAtMost(LAST_MINUTE),
                timeZoneId = TimeZone.currentSystemDefault().id,
                reminderMinutes = listOf(1),
            )
        // Truncated to the minute, like the reminder pipeline reconstructs it from the stored minute-of-day.
        val startAtMinute = LocalDateTime(start.year, start.month, start.day, start.hour, start.minute)
        val expectedTriggerMillis =
            startAtMinute.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() - MINUTE_MILLIS
        val before = runBlocking { alarms.alarms() }.filter { it.kind == AlarmKind.REMINDER }.map { it.id }.toSet()
        val id = runBlocking { store.save(event) }
        try {
            waitFor("the reminder to be scheduled", SCHEDULING_WAIT_MILLIS) {
                runBlocking { alarms.alarms() }.any { alarm ->
                    val offMillis = alarm.triggerAtEpochMillis - expectedTriggerMillis
                    val onTime = offMillis in -TOLERANCE_MILLIS..TOLERANCE_MILLIS
                    alarm.kind == AlarmKind.REMINDER && alarm.id !in before && onTime
                }
            }
            openLink("taqvim://day/${date.year}-${date.month}-${date.day}?calendar=gregorian", "destination:Day")
            awaitTag(segmentTag(DayDetailsTab.EVENTS.ordinal)).click()
            device.waitForIdle()
            device.wait(Until.findObject(By.descContains(TITLE)), DEVICE_TIMEOUT_MILLIS)
                ?: error("'$TITLE' is not shown on its day's Events tab")
            openLink("taqvim://calendar", "destination:Calendar")
            awaitTag("tab:MORE").click()
            awaitTag("more:AGENDA").click()
            awaitTag("destination:Agenda")
            device.wait(Until.findObject(By.descContains(TITLE)), DEVICE_TIMEOUT_MILLIS)
                ?: error("'$TITLE' is not shown on the agenda")
        } finally {
            runBlocking { store.delete(id) }
        }
    }

    private companion object {
        const val TITLE = "Device lifecycle check"
        const val LEAD_MINUTES = 10
        const val MINUTES_PER_HOUR = 60
        const val LAST_MINUTE = 1439
        const val SCHEDULING_WAIT_MILLIS = 45_000L
        const val MINUTE_MILLIS = 60_000L

        /** How close a candidate alarm's trigger time must be to the expected one (clock/rounding slack). */
        const val TOLERANCE_MILLIS = 5_000L
    }
}
