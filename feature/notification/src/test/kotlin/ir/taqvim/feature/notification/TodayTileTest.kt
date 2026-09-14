/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Looper
import android.service.quicksettings.Tile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** T-1215 (R): the Quick Settings tile shows today's date and weekday with the day number icon, and opens today. */
@RunWith(AndroidJUnit4::class)
class TodayTileTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(ATTEMPTS) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MILLIS)
        }
        condition() shouldBe true
    }

    @Test
    fun theTileStateComesFromTodaysSummary() {
        TodayTileStates.of(TodayFixtures.summary()) { weekday, date -> "$weekday / $date" } shouldBe
            TodayTileState(
                label = "22 Shahrivar 1405",
                subtitle = "Sunday",
                description = "Sunday / 22 Shahrivar 1405",
                iconText = "22",
            )
    }

    @Test
    fun listeningShowsTodayOnTheTile() {
        startKoin {
            modules(
                module {
                    single { TodaySummarySource { TodayFixtures.summary() } }
                    single { DayIconCache() }
                },
            )
        }
        val controller = Robolectric.buildService(TodayTileService::class.java).create()
        val service = controller.get()

        service.onStartListening()
        waitFor { service.qsTile.label?.toString() == "22 Shahrivar 1405" }

        val tile = service.qsTile
        tile.subtitle?.toString() shouldBe "Sunday"
        tile.contentDescription?.toString() shouldBe "Sunday, 22 Shahrivar 1405"
        tile.state shouldBe Tile.STATE_ACTIVE
        tile.icon?.type shouldBe Icon.TYPE_BITMAP
    }

    @Test
    fun withoutTheAppGraphOrWithAFailingSourceTheTileKeepsItsLabel() {
        val bare = Robolectric.buildService(TodayTileService::class.java).create().get()
        bare.onStartListening()
        bare.qsTile.label.shouldBeNull()

        startKoin {
            modules(
                module {
                    single { TodaySummarySource { error("no preferences") } }
                    single { DayIconCache() }
                },
            )
        }
        val failing = Robolectric.buildService(TodayTileService::class.java).create().get()
        failing.onStartListening()
        repeat(FAILING_POLLS) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MILLIS)
        }
        failing.qsTile.label.shouldBeNull()
    }

    @Test
    fun theTileOpensTodayInTheApp() {
        val open = shadowOf(TodaySurfaces.openTodayPendingIntent(context, 1))

        open.isActivity shouldBe true
        open.savedIntent.action shouldBe Intent.ACTION_VIEW
        open.savedIntent.data.toString() shouldBe "taqvim://calendar"
        open.savedIntent.`package` shouldBe context.packageName
        (open.savedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) shouldBe true
    }

    private companion object {
        const val ATTEMPTS = 300
        const val FAILING_POLLS = 10
        const val POLL_MILLIS = 10L
    }
}
