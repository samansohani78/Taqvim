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
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.scheduler.AlarmStore
import ir.taqvim.feature.events.PersonalEvent
import ir.taqvim.feature.events.PersonalEventStore
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
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
        val id = runBlocking { store.save(event) }
        try {
            waitFor("the reminder to be scheduled", SCHEDULING_WAIT_MILLIS) {
                runBlocking { alarms.alarms() }.any { it.kind == AlarmKind.REMINDER && it.sourceId == id }
            }
            openLink("taqvim://day/${date.year}-${date.month}-${date.day}?calendar=gregorian", "destination:Day")
            device.wait(Until.findObject(By.textContains(TITLE)), DEVICE_TIMEOUT_MILLIS)
                ?: error("'$TITLE' is not shown on its day")
            openLink("taqvim://calendar", "destination:Calendar")
            awaitTag("tab:MORE").click()
            awaitTag("more:AGENDA").click()
            awaitTag("destination:Agenda")
            device.wait(Until.findObject(By.textContains(TITLE)), DEVICE_TIMEOUT_MILLIS)
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
        const val SCHEDULING_WAIT_MILLIS = 30_000L
    }
}
