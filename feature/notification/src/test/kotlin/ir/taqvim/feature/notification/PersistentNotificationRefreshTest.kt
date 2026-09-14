/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.feature.notification.TodayFixtures.MAGHRIB
import ir.taqvim.feature.notification.TodayFixtures.MIDNIGHT
import ir.taqvim.feature.notification.TodayFixtures.NOW
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * T-1213 (R): the persistent notification follows the setting, shows today's content on its low-importance channel,
 * opens today, is posted only when its content changes and wakes up at the next prayer or the day change.
 */
@RunWith(AndroidJUnit4::class)
class PersistentNotificationRefreshTest {
    private class CountingStore : PostedNotificationStore {
        var key: String? = null
        var remembered = 0

        override fun postedKey(): String? = key

        override fun remember(key: String) {
            this.key = key
            remembered++
        }

        override fun forget() {
            key = null
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = requireNotNull(context.getSystemService(NotificationManager::class.java))
    private val store = CountingStore()
    private var summary = TodayFixtures.summary()
    private var options = PersistentNotificationOptions(enabled = true)
    private val refresh = PersistentNotificationRefresh(context, { summary }, { options }, store, DayIconCache())

    private fun grant() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun shown(): Notification? = shadowOf(notifications).getNotification(PersistentNotifications.ID)

    private fun Notification.text(key: String): String? = extras.getCharSequence(key)?.toString()

    @Test
    fun anOngoingSilentNotificationShowsTodayAndOpensTheCalendar(): Unit =
        runTest {
            grant()

            refresh.refresh(NOW) shouldBe MAGHRIB

            val notification = shown().shouldNotBeNull()
            notification.channelId shouldBe PersistentNotifications.CHANNEL
            (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) shouldBe true
            notification.category shouldBe NotificationCompat.CATEGORY_STATUS
            notification.visibility shouldBe Notification.VISIBILITY_PUBLIC
            notification.text(Notification.EXTRA_TITLE) shouldBe "22 Shahrivar 1405"
            notification.text(Notification.EXTRA_TEXT) shouldBe "Maghrib 19:10"
            notification.text(Notification.EXTRA_SUB_TEXT) shouldBe "Sunday"
            val lines = notification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            lines?.map { it.toString() } shouldBe
                listOf(
                    "13 September 2026",
                    "1 Rabi al-Awwal 1448",
                    "Holiday",
                    "Fajr 04:52 · Dhuhr 13:07 · Maghrib 19:10",
                )
            notification.smallIcon.type shouldBe Icon.TYPE_BITMAP
            notification.getLargeIcon().shouldBeNull()
            val open = shadowOf(notification.contentIntent)
            open.isActivity shouldBe true
            open.savedIntent.action shouldBe Intent.ACTION_VIEW
            open.savedIntent.data.toString() shouldBe TodaySurfaces.TODAY_LINK
            open.savedIntent.`package` shouldBe context.packageName

            val channel = notifications.getNotificationChannel(PersistentNotifications.CHANNEL).shouldNotBeNull()
            channel.importance shouldBe NotificationManager.IMPORTANCE_LOW
            channel.lockscreenVisibility shouldBe Notification.VISIBILITY_PUBLIC
            channel.sound.shouldBeNull()
            channel.name.toString() shouldBe "Today's date"
        }

    @Test
    fun theNotificationIsPostedAgainOnlyWhenItsContentChanges(): Unit =
        runTest {
            grant()

            refresh.refresh(NOW)
            refresh.refresh(NOW)
            store.remembered shouldBe 1

            summary = TodayFixtures.summary(holidays = emptyList())
            refresh.refresh(NOW)
            store.remembered shouldBe 2
            shown()
                .shouldNotBeNull()
                .extras
                .getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.size shouldBe 3

            options = options.copy(largeNumber = true)
            refresh.refresh(NOW)
            store.remembered shouldBe 3
            shown().shouldNotBeNull().getLargeIcon().shouldNotBeNull()
        }

    @Test
    fun aNotificationThatIsNoLongerShownIsPostedAgain(): Unit =
        runTest {
            grant()
            refresh.refresh(NOW)

            notifications.cancel(PersistentNotifications.ID)
            shown().shouldBeNull()
            refresh.refresh(NOW)

            shown().shouldNotBeNull()
            store.remembered shouldBe 2
        }

    @Test
    fun turningTheSettingOffRemovesTheNotificationAndItsWakeUp(): Unit =
        runTest {
            grant()
            refresh.refresh(NOW)

            options = PersistentNotificationOptions(enabled = false)

            refresh.refresh(NOW).shouldBeNull()
            shown().shouldBeNull()
            store.key.shouldBeNull()
        }

    @Test
    fun withoutPermissionNothingIsPostedButTheDayChangeIsStillFollowed(): Unit =
        runTest {
            summary = TodayFixtures.summary(withPlace = false)

            refresh.refresh(NOW) shouldBe MIDNIGHT

            shown().shouldBeNull()
            store.remembered shouldBe 0
        }
}
