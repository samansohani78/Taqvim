/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.testing.FakeClock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1200 refresh orchestration and per-session state loading. */
class WidgetRefresherTest {
    private val clock = FakeClock(Instant.parse("2026-09-14T10:00:30Z"))
    private val midnight = Instant.parse("2026-09-14T20:30:00Z")

    @Test
    fun `a trigger redraws only stale installed widgets and plans the next wake-up`(): Unit =
        runTest {
            val installed = FakeInstalledWidgets(mapOf(WidgetKind.DATE_1X1 to setOf(1), WidgetKind.MAP to setOf(4)))
            val updater = RecordingUpdater()
            val scheduler = RecordingWakeUpScheduler()
            val refresher = WidgetSamples.refresher(installed, updater, scheduler, clock = clock)

            refresher.refresh(WidgetUpdateTrigger.DayChanged)

            updater.updates shouldBe listOf(mapOf(WidgetKind.DATE_1X1 to setOf(1)))
            scheduler.scheduled shouldBe
                listOf(WidgetWakeUp(Instant.parse("2026-09-14T10:01:00Z"), setOf(WidgetUpdateTrigger.MinuteTick)))
        }

    @Test
    fun `nothing stale means no redraw, and no widgets means no wake-up`(): Unit =
        runTest {
            val installed = FakeInstalledWidgets(mapOf(WidgetKind.DATE_1X1 to setOf(1), WidgetKind.MOON to emptySet()))
            val updater = RecordingUpdater()
            val scheduler = RecordingWakeUpScheduler()
            val refresher = WidgetSamples.refresher(installed, updater, scheduler, clock = clock)

            refresher.refresh(setOf(WidgetUpdateTrigger.EventsChanged, WidgetUpdateTrigger.MinuteTick))
            updater.updates.shouldBeEmpty()
            scheduler.scheduled shouldBe listOf(WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged)))

            installed.widgets = mapOf(WidgetKind.MOON to emptySet())
            refresher.refresh(WidgetUpdateTrigger.Everything)
            updater.updates.shouldBeEmpty()
            scheduler.cancellations shouldBe 1
        }

    @Test
    fun `placing and removing widgets re-plans the wake-up`(): Unit =
        runTest {
            val installed = FakeInstalledWidgets(mapOf(WidgetKind.DAY_SUMMARY_2X2 to setOf(6)))
            val updater = RecordingUpdater()
            val scheduler = RecordingWakeUpScheduler()
            val configs = FakeWidgetConfigStore(mapOf(6 to WidgetConfig(), 7 to WidgetConfig()))
            val maghrib = Instant.parse("2026-09-14T15:12:00Z")
            val refresher = WidgetSamples.refresher(installed, updater, scheduler, configs, clock, maghrib)

            refresher.reschedule()
            scheduler.scheduled.last() shouldBe WidgetWakeUp(maghrib, setOf(WidgetUpdateTrigger.PrayerTimeReached))

            installed.widgets = emptyMap()
            refresher.onDeleted(setOf(6, 7))
            configs.deleted shouldBe listOf(setOf(6, 7))
            configs.stored shouldBe emptyMap()
            scheduler.cancellations shouldBe 1
            updater.updates.shouldBeEmpty()
        }

    @Test
    fun `a session loads the stored or default configuration, normalized, and the content`(): Unit =
        runTest {
            val requested = mutableListOf<WidgetConfig>()
            val source =
                WidgetDataSource { _, config, _, _ ->
                    requested += config
                    WidgetSamples.data()
                }
            val configs = FakeWidgetConfigStore(mapOf(2 to WidgetConfig(transparencyPercent = 95, scalePercent = 130)))
            val loader = WidgetStateLoader(configs, source, clock, StandardTestDispatcher(testScheduler))

            val fresh = loader.load(WidgetKind.DATE_1X1, 1)
            fresh shouldBe WidgetContentState.Ready(WidgetConfig.defaultFor(WidgetKind.DATE_1X1), WidgetSamples.data())

            val stored = loader.load(WidgetKind.DATE_1X1, 2).config
            stored.transparencyPercent shouldBe 90
            stored.scalePercent shouldBe 125
            stored.contents shouldBe WidgetKind.DATE_1X1.contents
            requested shouldBe listOf(fresh.config, stored)
        }

    @Test
    fun `a session loads its widget's view and removed widgets forget theirs`(): Unit =
        runTest {
            val views = FakeWidgetViewStore(mapOf(5 to WidgetView(monthOffset = 2), 6 to WidgetView(monthOffset = -1)))
            val seen = mutableListOf<WidgetView>()
            val source =
                WidgetDataSource { _, _, _, view ->
                    seen += view
                    WidgetSamples.data()
                }
            val loader =
                WidgetStateLoader(FakeWidgetConfigStore(), source, clock, StandardTestDispatcher(testScheduler), views)

            loader.load(WidgetKind.MONTH_INTERACTIVE, 5)
            loader.load(WidgetKind.MONTH_INTERACTIVE, 7)
            seen shouldBe listOf(WidgetView(monthOffset = 2), WidgetView())

            val installed = FakeInstalledWidgets()
            val refresher =
                WidgetRefresher(
                    installed,
                    RecordingUpdater(),
                    RecordingWakeUpScheduler(),
                    FixedTimeline(WidgetTimeline(WidgetSamples.tehran, null)),
                    FakeWidgetConfigStore(),
                    clock,
                    views = views,
                )
            refresher.onDeleted(setOf(5))
            views.stored shouldBe mapOf(6 to WidgetView(monthOffset = -1))
        }

    @Test
    fun `a failing content source gives the failure state but cancellation propagates`(): Unit =
        runTest {
            val configs = FakeWidgetConfigStore()
            val failing =
                WidgetStateLoader(
                    configs,
                    { _, _, _, _ -> error("offline") },
                    clock,
                    StandardTestDispatcher(testScheduler),
                )
            failing.load(WidgetKind.MOON, 3).shouldBeInstanceOf<WidgetContentState.Failed>().config shouldBe
                WidgetConfig.defaultFor(WidgetKind.MOON)

            val cancelled =
                WidgetStateLoader(
                    configs,
                    { _, _, _, _ -> throw CancellationException("stopped") },
                    clock,
                    StandardTestDispatcher(testScheduler),
                )
            shouldThrow<CancellationException> { cancelled.load(WidgetKind.MOON, 3) }
        }
}
