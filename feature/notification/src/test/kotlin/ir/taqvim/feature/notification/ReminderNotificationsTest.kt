/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** T-1001 (R/UI): reminder notifications per channel, their Done and Snooze actions, links and persistence. */
@RunWith(AndroidJUnit4::class)
class ReminderNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = requireNotNull(context.getSystemService(NotificationManager::class.java))

    private fun grant() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun posted(reminder: PlannedReminder): Notification? =
        shadowOf(notifications).getNotification(ReminderNotifications.idOf(reminder))

    @Test
    fun eachKindPostsOnItsChannelWithDoneSnoozeAndALink() {
        grant()
        val personal = ReminderFixtures.planned()
        val nowruz = "ir.holiday.nowruz-1"
        val official = ReminderFixtures.planned(ReminderKind.OFFICIAL, sourceId = 7, target = nowruz, daysBefore = 3)
        val today = official.copy(sourceId = 8, daysBefore = 0)

        listOf(personal, official, today).forEach { SystemReminderNotifier(context).show(it) shouldBe true }

        val first = posted(personal).shouldNotBeNull()
        first.channelId shouldBe ReminderNotifications.CHANNEL_PERSONAL
        first.extras.getCharSequence(Notification.EXTRA_TITLE).toString() shouldBe "Dentist"
        first.extras.getCharSequence(Notification.EXTRA_TEXT).toString() shouldBe "Upcoming event"
        first.actions.map { it.title.toString() } shouldBe listOf("Done", "Snooze")
        val open = shadowOf(first.contentIntent)
        open.isActivity shouldBe true
        open.savedIntent.action shouldBe Intent.ACTION_VIEW
        open.savedIntent.data.toString() shouldBe "taqvim://event/42"
        open.savedIntent.`package` shouldBe context.packageName

        val second = posted(official).shouldNotBeNull()
        second.channelId shouldBe ReminderNotifications.CHANNEL_OFFICIAL
        second.extras.getCharSequence(Notification.EXTRA_TEXT).toString() shouldBe "In 3 days"
        shadowOf(second.contentIntent).savedIntent.data.toString() shouldBe
            "taqvim://occasion/ir.holiday.nowruz-1?day=${ReminderFixtures.NOWRUZ_1405.value}"
        posted(today)
            .shouldNotBeNull()
            .extras
            .getCharSequence(Notification.EXTRA_TEXT)
            .toString() shouldBe "Today"
        val single = official.copy(sourceId = 9, daysBefore = 1)
        SystemReminderNotifier(context).show(single)
        posted(single)
            .shouldNotBeNull()
            .extras
            .getCharSequence(Notification.EXTRA_TEXT)
            .toString() shouldBe "In 1 day"
        listOf(ReminderNotifications.CHANNEL_PERSONAL, ReminderNotifications.CHANNEL_OFFICIAL).forEach { channel ->
            val importance = notifications.getNotificationChannel(channel).shouldNotBeNull().importance
            importance shouldBe NotificationManager.IMPORTANCE_HIGH
        }
    }

    @Test
    fun doneCancelsAndSnoozeShowsTheReminderAgainLater() {
        grant()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val reminder = ReminderFixtures.planned()
        val notifier = SystemReminderNotifier(context)
        val receiver = ReminderActionReceiver()

        notifier.show(reminder)
        receiver.onReceive(context, shadowOf(posted(reminder).shouldNotBeNull().actions[0].actionIntent).savedIntent)
        posted(reminder).shouldBeNull()

        notifier.show(reminder)
        val snooze = shadowOf(posted(reminder).shouldNotBeNull().actions[1].actionIntent)
        snooze.isBroadcast shouldBe true
        val before = Clock.System.now()
        receiver.onReceive(context, snooze.savedIntent)
        posted(reminder).shouldBeNull()

        val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))
        val alarm = shadowOf(alarms).scheduledAlarms.single()
        val due = before + ReminderSnooze.SNOOZE
        (alarm.triggerAtMs in due.toEpochMilliseconds()..(due + 5.seconds).toEpochMilliseconds()) shouldBe true
        @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
        val operation = shadowOf(requireNotNull(alarm.operation))
        operation.isBroadcast shouldBe true
        operation.savedIntent.action shouldBe ReminderIntents.ACTION_SHOW
        ReminderIntents.reminderOf(operation.savedIntent) shouldBe reminder

        receiver.onReceive(context, operation.savedIntent)
        posted(reminder).shouldNotBeNull()
    }

    @Test
    fun remindersTravelInBroadcastsAndInvalidOnesAreIgnored() {
        grant()
        val reminder = ReminderFixtures.planned(ReminderKind.OFFICIAL, sourceId = 2, target = "ir.holiday.nowruz-1")
        val intent = ReminderIntents.of(context, ReminderIntents.ACTION_SHOW, reminder)

        intent.component?.className shouldBe ReminderActionReceiver::class.java.name
        ReminderIntents.reminderOf(intent) shouldBe reminder
        ReminderIntents.reminderOf(Intent(intent).putExtra(KIND, "SHIFT")).shouldBeNull()
        ReminderIntents.reminderOf(Intent(intent).apply { removeExtra(AT) }).shouldBeNull()
        ReminderIntents.reminderOf(Intent(intent).putExtra(DAYS_BEFORE, -1)).shouldBeNull()

        ReminderActionReceiver().onReceive(context, Intent(ReminderIntents.ACTION_SHOW))
        ReminderActionReceiver().onReceive(context, Intent(intent).setAction("other"))
        shadowOf(notifications).allNotifications.shouldBeEmpty()
    }

    @Test
    fun withoutTheNotificationPermissionNothingIsPosted() {
        SystemReminderNotifier(context).show(ReminderFixtures.planned()) shouldBe false
        shadowOf(notifications).allNotifications.shouldBeEmpty()
    }

    @Test
    fun deliveriesAreRememberedAcrossInstancesAndTheModuleProvidesTheAlarms(): Unit =
        runTest {
            SharedPreferencesReminderDeliveryLog(context).claim("PERSONAL:1@5") shouldBe true
            SharedPreferencesReminderDeliveryLog(context).claim("PERSONAL:1@5") shouldBe false
            SharedPreferencesReminderDeliveryLog(context).claim("OFFICIAL:1@5") shouldBe true

            val koin =
                koinApplication {
                    androidContext(context)
                    modules(
                        notificationFeatureModule,
                        module {
                            single<AthanSetupSource> { AthanSetupSource { null } }
                            single<ReminderSetupSource> { ReminderSetupSource { ReminderFixtures.setup(emptyList()) } }
                        },
                    )
                }.koin
            koin.get<ReminderAlarms>().upcoming(Clock.System.now()).shouldBeEmpty()
            koin.get<ReminderNotifier>().shouldNotBeNull()
        }

    private companion object {
        const val KIND = "ir.taqvim.feature.notification.extra.REMINDER_KIND"
        const val AT = "ir.taqvim.feature.notification.extra.REMINDER_AT"
        const val DAYS_BEFORE = "ir.taqvim.feature.notification.extra.REMINDER_DAYS_BEFORE"
    }
}
