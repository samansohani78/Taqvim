/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.StoredWidgetConfig
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.widgets.InstalledWidgetIds
import ir.taqvim.feature.widgets.WidgetBackground
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetConfigStore
import ir.taqvim.feature.widgets.WidgetContent
import ir.taqvim.feature.widgets.WidgetDependency
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetRefresher
import ir.taqvim.feature.widgets.WidgetTimeline
import ir.taqvim.feature.widgets.WidgetView
import ir.taqvim.feature.widgets.WidgetWakeUp
import ir.taqvim.feature.widgets.WidgetWakeUpScheduler
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1201…T-1204 wiring: widget content, timeline, configurations and update triggers from the stored preferences. */
class WidgetAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")
    private val berlin = TimeZone.of("Europe/Berlin")
    private val day = LocalDate(2026, 9, 13).toJdn()

    private fun dayEvents(jdn: Jdn): DayEvents =
        DayEvents(
            jdn = jdn,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1448, 3, 1),
            hijri = null,
            official = emptyList(),
            isHoliday = true,
            isWeekend = false,
            personal =
                listOf(
                    PersonalOccurrence(
                        eventId = 7,
                        title = "Mom",
                        notes = "",
                        calendarSystem = CalendarSystem.PERSIAN,
                        days = jdn..jdn,
                        startMinute = null,
                        endMinute = null,
                        timeZoneId = "Asia/Tehran",
                        colorArgb = null,
                        recurring = false,
                    ),
                ),
            device = emptyList(),
            ics = emptyList(),
        )

    @Test
    fun `widget content uses the place's day, the user's calendars and the day's events`(): Unit =
        runTest {
            val preferences = repositoryOf(persian.copy(place = tehran))
            val events = { range: JdnRange -> flowOf(range.map { dayEvents(it) }) }
            val source = PreferencesWidgetDataSource(preferences, events, { it.name }) { berlin }

            // 22:30 UTC on the 12th is already the 13th in Tehran.
            val at = Instant.parse("2026-09-12T22:30:00Z")
            val data = source.load(WidgetKind.DAY_SUMMARY_2X2, WidgetConfig(), at, WidgetView())

            data.date shouldBe LocalDate(2026, 9, 13)
            data.title shouldBe "۲۲ شهریور ۱۴۰۵"
            data.isHoliday shouldBe true
            data.events.single().eventId shouldBe 7
            data.nextPrayer.shouldNotBeNull()
            data.month.shouldBeNull()
        }

    @Test
    fun `the secondary calendar follows the configuration but never repeats the primary one`() {
        val events = dayEvents(day)
        val gregorianSecond = persian.copy(calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN))

        widgetInputs(gregorianSecond, WidgetConfig(), day, events, null).secondary shouldBe GregorianCalendarSystem
        widgetInputs(gregorianSecond, WidgetConfig(secondaryCalendar = CalendarSystem.PERSIAN), day, events, null)
            .secondary
            .shouldBeNull()
        widgetInputs(persian.copy(calendars = listOf(CalendarSystem.PERSIAN)), WidgetConfig(), day, events, null)
            .secondary
            .shouldBeNull()
    }

    @Test
    fun `without a place the device zone is used and there is no next prayer`(): Unit =
        runTest {
            val preferences = repositoryOf(persian)
            val source = PreferencesWidgetTimelineSource(preferences) { berlin }

            source.timeline(Instant.parse("2026-09-13T10:00:00Z")) shouldBe WidgetTimeline(berlin, null)
            persian.widgetPlace().shouldBeNull()
            val withPlace = PreferencesWidgetTimelineSource(repositoryOf(persian.copy(place = tehran))) { berlin }
            val next = withPlace.timeline(Instant.parse("2026-09-13T10:00:00Z"))
            next.timeZone shouldBe TimeZone.of("Asia/Tehran")
            (next.nextPrayer.shouldNotBeNull() > Instant.parse("2026-09-13T10:00:00Z")) shouldBe true
        }

    @Test
    fun `stored configurations map both ways and unknown names fall back`() {
        val config =
            WidgetConfig(
                background = WidgetBackground.BLACK,
                transparencyPercent = 40,
                scalePercent = 150,
                contents = persistentSetOf(WidgetContent.EVENTS, WidgetContent.WEEKDAY),
                secondaryCalendar = CalendarSystem.GREGORIAN,
            )

        config.toStored().toWidgetConfig() shouldBe config
        StoredWidgetConfig("PURPLE", 0, 100, setOf("EVENTS", "RETIRED"), null).toWidgetConfig() shouldBe
            WidgetConfig(contents = persistentSetOf(WidgetContent.EVENTS))
    }

    @Test
    fun `preference changes mark only the widget content they affect`() {
        widgetDependenciesChanged(persian, persian).shouldBeEmpty()
        widgetDependenciesChanged(persian, persian.copy(languageCode = "en")) shouldBe
            setOf(WidgetDependency.APPEARANCE)
        widgetDependenciesChanged(persian, persian.copy(place = tehran)) shouldBe
            setOf(WidgetDependency.LOCATION, WidgetDependency.PRAYER_TIMES)
        widgetDependenciesChanged(persian, persian.copy(prayerMethod = PrayerMethod.MWL)) shouldBe
            setOf(WidgetDependency.PRAYER_TIMES)
        val noSources = persian.copy(app = persian.app.copy(enabledEventSources = emptySet()))
        widgetDependenciesChanged(persian, noSources) shouldBe setOf(WidgetDependency.EVENTS)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the watcher redraws affected widgets on preference and event changes`(): Unit =
        runTest {
            val preferences = MutableSharedFlow<UserPreferences>(replay = 1)
            val changes = MutableSharedFlow<Set<String>>()
            val updates = mutableListOf<InstalledWidgetIds>()
            val installed = mapOf(WidgetKind.DATE_1X1 to setOf(1), WidgetKind.DAY_SUMMARY_2X2 to setOf(2))
            val refresher =
                WidgetRefresher(
                    installed = { installed },
                    updater = { updates += it },
                    scheduler = NoWakeUps,
                    timeline = { WidgetTimeline(berlin, null) },
                    configs = NoConfigs,
                    clock = FixedClock,
                )
            val watching = launch { WidgetTriggerWatcher(preferences, changes, refresher, 10.milliseconds).watch() }

            preferences.emit(persian)
            advanceUntilIdle()
            updates.shouldBeEmpty()
            preferences.emit(persian.copy(place = tehran))
            advanceUntilIdle()
            updates shouldBe listOf(mapOf(WidgetKind.DAY_SUMMARY_2X2 to setOf(2)))
            changes.emit(setOf("personal_events"))
            advanceUntilIdle()
            updates.last() shouldBe mapOf(WidgetKind.DAY_SUMMARY_2X2 to setOf(2))
            watching.cancel()
        }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.parse("2026-09-13T10:00:00Z")
    }

    private object NoWakeUps : WidgetWakeUpScheduler {
        override fun schedule(wakeUp: WidgetWakeUp) = Unit

        override fun cancel() = Unit
    }

    private object NoConfigs : WidgetConfigStore {
        override suspend fun config(appWidgetId: Int): WidgetConfig? = null

        override suspend fun save(
            appWidgetId: Int,
            config: WidgetConfig,
        ) = Unit

        override suspend fun delete(appWidgetIds: Set<Int>) = Unit
    }
}
