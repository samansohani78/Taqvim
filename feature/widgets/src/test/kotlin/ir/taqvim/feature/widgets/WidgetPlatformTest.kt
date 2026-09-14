/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.testing.FakeClock
import kotlin.time.Clock
import kotlin.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** T-1200 (R): the wake-up alarm, the update receiver with the Koin module, widget receivers, the config activity. */
@RunWith(AndroidJUnit4::class)
class WidgetPlatformTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms: ShadowAlarmManager = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val clock = FakeClock(Instant.parse("2026-09-14T10:00:30Z"))
    private val installed = FakeInstalledWidgets(mapOf(WidgetKind.DATE_1X1 to setOf(1), WidgetKind.MAP to setOf(4)))
    private val updater = RecordingUpdater()
    private val scheduler = RecordingWakeUpScheduler()
    private val configs = FakeWidgetConfigStore(mapOf(1 to WidgetConfig(), 4 to WidgetConfig()))

    @Before
    fun startGraph() {
        startKoin {
            androidContext(context)
            modules(
                widgetsFeatureModule,
                module {
                    single<Clock> { clock }
                    single<WidgetDataSource> { WidgetDataSource { _, _, _, _ -> WidgetSamples.data() } }
                    single<WidgetConfigStore> { configs }
                    single<WidgetTimelineSource> { FixedTimeline(WidgetTimeline(WidgetSamples.tehran, null)) }
                    single<WidgetCalendarsSource> { FakeCalendars(listOf(CalendarSystem.PERSIAN)) }
                    single<InstalledWidgets> { installed }
                    single<WidgetUpdater> { updater }
                    single<WidgetWakeUpScheduler> { scheduler }
                    single<WidgetKindResolver> { WidgetKindResolver { id -> WidgetKind.DATE_1X1.takeIf { id == 1 } } }
                },
            )
        }
    }

    @After
    fun stopGraph() {
        stopKoin()
    }

    @Test
    fun wakeUpAlarmsAreSetReplacedAndCancelled() {
        val wakeUps = AlarmWidgetWakeUpScheduler(context)
        val midnight = Instant.parse("2026-09-14T20:30:00Z")
        wakeUps.schedule(WidgetWakeUp(Instant.parse("2026-09-14T10:01:00Z"), setOf(WidgetUpdateTrigger.MinuteTick)))
        wakeUps.schedule(WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged)))

        val alarm = alarms.scheduledAlarms.single()
        alarm.triggerAtMs shouldBe midnight.toEpochMilliseconds()
        alarm.getType() shouldBe AlarmManager.RTC
        @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
        val intent = shadowOf(requireNotNull(alarm.operation)).savedIntent
        intent.component?.className shouldBe WidgetUpdateReceiver::class.java.name
        intent.action shouldBe WidgetBroadcasts.ACTION_WAKE_UP
        intent.getStringArrayExtra(WidgetBroadcasts.EXTRA_TRIGGERS)?.toList() shouldBe listOf("DayChanged")

        wakeUps.cancel()
        alarms.scheduledAlarms.shouldBeEmpty()
    }

    @Test
    fun theUpdateReceiverRefreshesTargetsAndReplansTheWakeUp() {
        val receiver = WidgetUpdateReceiver()
        receiver.onReceive(
            context,
            Intent(WidgetBroadcasts.ACTION_WAKE_UP).putExtra(WidgetBroadcasts.EXTRA_TRIGGERS, arrayOf("MinuteTick")),
        )
        scheduler.awaitCall() shouldBe
            WidgetWakeUp(Instant.parse("2026-09-14T10:01:00Z"), setOf(WidgetUpdateTrigger.MinuteTick))
        updater.updates shouldBe listOf(mapOf(WidgetKind.MAP to setOf(4)))

        receiver.onReceive(context, Intent("android.intent.action.TIMEZONE_CHANGED"))
        scheduler.awaitCall()
        updater.updates.last() shouldBe installed.widgets

        receiver.onReceive(context, WidgetUpdateReceiver.rescheduleIntent(context))
        scheduler.awaitCall()
        updater.updates.size shouldBe 2

        installed.widgets = mapOf(WidgetKind.DATE_1X1 to setOf(1))
        receiver.onReceive(context, WidgetUpdateReceiver.deletedIntent(context, intArrayOf(4)))
        scheduler.awaitCall()
        configs.deleted shouldBe listOf(setOf(4))
        configs.stored.keys shouldBe setOf(1)

        receiver.onReceive(context, Intent("android.intent.action.BOOT_COMPLETED"))
        updater.updates.size shouldBe 2
    }

    @Test
    fun placingAWidgetAsksTheFrameworkToReplan() {
        val widgetReceiver = SampleReceiver()
        context.registerReceiver(
            widgetReceiver,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
            Context.RECEIVER_NOT_EXPORTED,
        )
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setPackage(context.packageName)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(5)),
        )
        shadowOf(Looper.getMainLooper()).idle()

        val sent = shadowOf(context as Application).broadcastIntents.map { it.action }
        (WidgetBroadcasts.ACTION_RESCHEDULE in sent).shouldBeTrue()
        context.unregisterReceiver(widgetReceiver)
    }

    @Test
    fun theConfigActivityAnswersCanceledForUnknownWidgets() {
        val unknown = Robolectric.buildActivity(WidgetConfigActivity::class.java, configure(77)).setup().get()
        unknown.isFinishing.shouldBeTrue()
        shadowOf(unknown).resultCode shouldBe Activity.RESULT_CANCELED

        val missingId = Robolectric.buildActivity(WidgetConfigActivity::class.java, Intent()).setup().get()
        missingId.isFinishing.shouldBeTrue()

        val placed = Robolectric.buildActivity(WidgetConfigActivity::class.java, configure(1)).setup().get()
        placed.isFinishing.shouldBeFalse()
        shadowOf(placed).resultCode shouldBe Activity.RESULT_CANCELED
    }

    private fun configure(appWidgetId: Int) =
        Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private class SampleWidget : TaqvimGlanceWidget(WidgetKind.DATE_1X1) {
        @androidx.compose.runtime.Composable
        override fun Content(
            data: WidgetData,
            config: WidgetConfig,
            style: WidgetStyle,
            size: WidgetSize,
        ) {
            WidgetDateLines(data, config, style, compact = size == WidgetSize.SMALL)
        }
    }

    private class SampleReceiver : TaqvimWidgetReceiver() {
        override val glanceAppWidget = SampleWidget()
    }
}
