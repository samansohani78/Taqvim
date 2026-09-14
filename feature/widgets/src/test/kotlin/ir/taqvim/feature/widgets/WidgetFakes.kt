/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.model.CalendarSystem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Thread-safe test doubles of the widget ports (receivers run their work on a background dispatcher). */
class FakeWidgetConfigStore(
    initial: Map<Int, WidgetConfig> = emptyMap(),
) : WidgetConfigStore {
    val stored: MutableMap<Int, WidgetConfig> = ConcurrentHashMap(initial)
    val deleted: MutableList<Set<Int>> = CopyOnWriteArrayList()

    @Volatile
    var failSaves: Boolean = false

    override suspend fun config(appWidgetId: Int): WidgetConfig? = stored[appWidgetId]

    override suspend fun save(
        appWidgetId: Int,
        config: WidgetConfig,
    ) {
        check(!failSaves) { "store unavailable" }
        stored[appWidgetId] = config
    }

    override suspend fun delete(appWidgetIds: Set<Int>) {
        deleted += appWidgetIds
        appWidgetIds.forEach { stored.remove(it) }
    }
}

class FakeWidgetViewStore(
    initial: Map<Int, WidgetView> = emptyMap(),
) : WidgetViewStore {
    val stored: MutableMap<Int, WidgetView> = ConcurrentHashMap(initial)

    override suspend fun view(appWidgetId: Int): WidgetView = stored[appWidgetId] ?: WidgetView()

    override suspend fun save(
        appWidgetId: Int,
        view: WidgetView,
    ) {
        stored[appWidgetId] = view
    }

    override suspend fun delete(appWidgetIds: Set<Int>) {
        appWidgetIds.forEach { stored.remove(it) }
    }
}

class FakeInstalledWidgets(
    @Volatile var widgets: InstalledWidgetIds = emptyMap(),
) : InstalledWidgets {
    override suspend fun installed(): InstalledWidgetIds = widgets
}

class RecordingUpdater : WidgetUpdater {
    val updates: MutableList<InstalledWidgetIds> = CopyOnWriteArrayList()

    override suspend fun update(targets: InstalledWidgetIds) {
        updates += targets
    }
}

/** Records schedules and cancellations; [awaitCall] waits for the next one (the last step of every refresh). */
class RecordingWakeUpScheduler : WidgetWakeUpScheduler {
    private val calls = LinkedBlockingQueue<WidgetWakeUp?>()
    val scheduled: MutableList<WidgetWakeUp> = CopyOnWriteArrayList()

    @Volatile
    var cancellations: Int = 0

    override fun schedule(wakeUp: WidgetWakeUp) {
        scheduled += wakeUp
        calls.put(wakeUp)
    }

    override fun cancel() {
        cancellations++
        calls.put(CANCELLED)
    }

    /** The next wake-up scheduled, or [CANCELLED]; fails after five seconds. */
    fun awaitCall(): WidgetWakeUp? = requireNotNull(calls.poll(5, TimeUnit.SECONDS)) { "no schedule or cancel call" }

    companion object {
        /** Marker of a cancellation in [awaitCall]. */
        val CANCELLED: WidgetWakeUp = WidgetWakeUp(Instant.DISTANT_PAST, emptySet())
    }
}

class FixedTimeline(
    private val timeline: WidgetTimeline,
) : WidgetTimelineSource {
    override suspend fun timeline(now: Instant): WidgetTimeline = timeline
}

class FakeCalendars(
    calendars: List<CalendarSystem>,
) : WidgetCalendarsSource {
    val flow: MutableStateFlow<List<CalendarSystem>> = MutableStateFlow(calendars)

    override fun calendars(): Flow<List<CalendarSystem>> = flow
}

object WidgetSamples {
    val tehran: TimeZone = TimeZone.of("Asia/Tehran")

    fun data(isHoliday: Boolean = false): WidgetData =
        WidgetData(
            date = LocalDate(2026, 9, 13),
            dayNumber = "22",
            title = "22 Shahrivar 1405",
            weekday = "Sunday",
            secondaryDate = "13 September 2026",
            isHoliday = isHoliday,
            events = persistentListOf(WidgetEventLine("World Day", isHoliday = false)),
            nextPrayer = WidgetPrayerLine("Maghrib", "19:12"),
        )

    fun refresher(
        installed: FakeInstalledWidgets,
        updater: RecordingUpdater = RecordingUpdater(),
        scheduler: RecordingWakeUpScheduler = RecordingWakeUpScheduler(),
        configs: FakeWidgetConfigStore = FakeWidgetConfigStore(),
        clock: Clock,
        nextPrayer: Instant? = null,
    ): WidgetRefresher =
        WidgetRefresher(
            installed,
            updater,
            scheduler,
            FixedTimeline(WidgetTimeline(tehran, nextPrayer)),
            configs,
            clock,
        )
}
