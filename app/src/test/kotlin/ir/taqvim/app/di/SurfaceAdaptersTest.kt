/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.notification.DailyRefresh
import ir.taqvim.feature.notification.DailyRefreshCoordinator
import ir.taqvim.feature.notification.DailyRefreshScheduler
import ir.taqvim.feature.notification.PersistentNotificationOptions
import ir.taqvim.feature.notification.SummaryPrayer
import ir.taqvim.feature.widgets.WidgetData
import ir.taqvim.feature.widgets.WidgetDataSource
import ir.taqvim.feature.widgets.WidgetEventLine
import ir.taqvim.feature.widgets.WidgetPrayerLine
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1213…T-1215: the surfaces outside the app share the widgets' day, and follow the preferences they depend on. */
class SurfaceAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")
    private val berlin = TimeZone.of("Europe/Berlin")
    private val noonInTehran = Instant.parse("2026-09-13T08:30:00Z")
    private val tehranMidnight = Instant.parse("2026-09-13T20:30:00Z")

    private val widgetDay =
        WidgetData(
            date = LocalDate(2026, 9, 13),
            dayNumber = "۲۲",
            title = "۲۲ شهریور ۱۴۰۵",
            weekday = "یکشنبه",
            secondaryDate = "13 September 2026",
            isHoliday = true,
            events = persistentListOf(WidgetEventLine("Holiday", isHoliday = true), WidgetEventLine("Mom", false, 7)),
            prayers = persistentListOf(WidgetPrayerLine("Fajr", "04:52"), WidgetPrayerLine("Maghrib", "19:10", true)),
        )

    private val fixedDay = WidgetDataSource { _, _, _, _ -> widgetDay }

    private val events = { range: JdnRange ->
        flowOf(
            range.map {
                DayEvents(
                    jdn = it,
                    islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1448, 3, 1),
                    hijri = null,
                    official = emptyList(),
                    isHoliday = false,
                    isWeekend = false,
                    personal = emptyList(),
                    device = emptyList(),
                    ics = emptyList(),
                )
            },
        )
    }

    @Test
    fun `today's summary is the widget day in the place's zone with its holidays and prayers`(): Unit =
        runTest {
            val preferences = repositoryOf(persian.copy(place = tehran))
            val timeline = PreferencesWidgetTimelineSource(preferences) { berlin }

            val summary = WidgetTodaySummarySource(preferences, fixedDay, timeline).load(noonInTehran)

            summary.dayOfMonth shouldBe 22
            summary.dayNumber shouldBe "۲۲"
            summary.otherDates shouldBe listOf("13 September 2026")
            summary.holidays shouldBe listOf("Holiday")
            summary.prayers shouldBe listOf(SummaryPrayer("Fajr", "04:52"), SummaryPrayer("Maghrib", "19:10", true))
            summary.nextDayAt shouldBe tehranMidnight
            (summary.nextPrayerAt.shouldNotBeNull() > noonInTehran) shouldBe true
        }

    @Test
    fun `the notification options follow both switches`(): Unit =
        runTest {
            PreferencesPersistentNotificationOptions(repositoryOf(persian)).options() shouldBe
                PersistentNotificationOptions(enabled = persian.app.persistentNotification, largeNumber = false)
            val on =
                persian.copy(
                    app = persian.app.copy(persistentNotification = true, persistentNotificationLargeNumber = true),
                )
            PreferencesPersistentNotificationOptions(repositoryOf(on)).options() shouldBe
                PersistentNotificationOptions(enabled = true, largeNumber = true)
        }

    @Test
    fun `the wallpaper shows the month under the day sky until sunset at the place`(): Unit =
        runTest {
            val preferences = repositoryOf(persian.copy(place = tehran))
            val data = PreferencesWidgetDataSource(preferences, events, { it.name }) { berlin }
            val timeline = PreferencesWidgetTimelineSource(preferences) { berlin }

            val content = WidgetWallpaperContentSource(preferences, data, timeline).load(noonInTehran)

            content.title shouldBe "۲۲ شهریور ۱۴۰۵"
            content.month.cells.shouldNotBeEmpty()
            content.month.cells.count { it.isToday } shouldBe 1
            content.daylight shouldBe true
            (content.nextChangeAt > noonInTehran && content.nextChangeAt < tehranMidnight) shouldBe true
            (content.moon.illuminatedFraction in 0f..1f) shouldBe true
        }

    @Test
    fun `without a place the sky follows local day hours and changes at 18 and midnight`() {
        val morning = WallpaperSky.at(noonInTehran, null, berlin)
        morning.daylight shouldBe true
        morning.nextChangeAt shouldBe Instant.parse("2026-09-13T16:00:00Z")

        val night = WallpaperSky.at(Instant.parse("2026-09-13T20:00:00Z"), null, berlin)
        night.daylight shouldBe false
        night.nextChangeAt shouldBe Instant.parse("2026-09-13T22:00:00Z")

        val tehranNight =
            WallpaperSky.at(
                Instant.parse("2026-09-13T20:00:00Z"),
                tehran.coordinates,
                TimeZone.of("Asia/Tehran"),
            )
        tehranNight.daylight shouldBe false
        tehranNight.nextChangeAt shouldBe tehranMidnight
    }

    @Test
    fun `the daydream shows the weekday and date until the day changes`(): Unit =
        runTest {
            val timeline = PreferencesWidgetTimelineSource(repositoryOf(persian.copy(place = tehran))) { berlin }

            val content = WidgetDreamContentSource(fixedDay, timeline).load(noonInTehran)

            content.date shouldBe "یکشنبه ۲۲ شهریور ۱۴۰۵"
            content.otherDates shouldBe "13 September 2026"
            content.nextChangeAt shouldBe tehranMidnight
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `surfaces refresh at start and when a preference they show changes, not for others`(): Unit =
        runTest {
            val preferences = MutableStateFlow(persian)
            var runs = 0
            val scheduler =
                object : DailyRefreshScheduler {
                    override fun schedule(at: Instant) = Unit

                    override fun cancel() = Unit
                }
            val coordinator =
                DailyRefreshCoordinator(
                    listOf(
                        DailyRefresh {
                            runs++
                            null
                        },
                    ),
                    scheduler,
                )
            val watcher = SurfaceRefreshWatcher(preferences, coordinator)
            backgroundScope.launch { watcher.watch() }
            runCurrent()
            runs shouldBe 1

            preferences.value = persian.copy(app = persian.app.copy(showWeekNumbers = !persian.app.showWeekNumbers))
            runCurrent()
            runs shouldBe 1

            preferences.value = persian.copy(app = persian.app.copy(dynamicLauncherIcon = true))
            runCurrent()
            runs shouldBe 2

            preferences.value = preferences.value.copy(place = tehran)
            runCurrent()
            runs shouldBe 3
        }
}
