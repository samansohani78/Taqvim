/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
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
 * A personal event's reminder (T-1001) is scheduled through the persistent scheduler (T-604, ADR-0033) and fires as a
 * notification on a real device's AlarmManager, with exact alarms allowed as the permission screen would allow them.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // per-app language and POST_NOTIFICATIONS need 13+
class DeviceReminderTest {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun reminderNotificationFires() {
        useLanguage("en")
        shell("appops set ${appContext.packageName} SCHEDULE_EXACT_ALARM allow")
        val store = GlobalContext.get().get<PersonalEventStore>()
        val id = runBlocking { store.save(eventStartingSoon()) }
        val manager = appContext.getSystemService(NotificationManager::class.java)
        try {
            waitFor("the reminder notification", REMINDER_WAIT_MILLIS) {
                manager.activeNotifications.any {
                    it.notification.extras
                        .getCharSequence(Notification.EXTRA_TITLE)
                        ?.contains(TITLE) == true
                }
            }
        } finally {
            runBlocking { store.delete(id) }
        }
    }

    /** A timed event today that starts [LEAD_MINUTES] minutes from now, reminded one minute before. */
    private fun eventStartingSoon(): PersonalEvent {
        val zone = TimeZone.currentSystemDefault()
        val start = (Clock.System.now() + LEAD_MINUTES.minutes).toLocalDateTime(zone)
        val date = CalendarDate(CalendarSystem.GREGORIAN, start.year, start.month.ordinal + 1, start.day)
        val minute = start.hour * MINUTES_PER_HOUR + start.minute
        return PersonalEvent(
            title = TITLE,
            calendar = CalendarSystem.GREGORIAN,
            start = date,
            end = date,
            startMinute = minute,
            endMinute = (minute + 1).coerceAtMost(LAST_MINUTE),
            timeZoneId = zone.id,
            reminderMinutes = listOf(1),
        )
    }

    private companion object {
        const val TITLE = "Device reminder check"
        const val LEAD_MINUTES = 3
        const val MINUTES_PER_HOUR = 60
        const val LAST_MINUTE = 1439
        const val REMINDER_WAIT_MILLIS = 5 * 60_000L
    }
}
