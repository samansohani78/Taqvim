/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.astronomy.AstronomySettings
import ir.taqvim.feature.compass.CompassPlace
import ir.taqvim.feature.compass.DeviceOrientation
import ir.taqvim.feature.compass.LevelCalibration
import ir.taqvim.feature.compass.Tilt
import ir.taqvim.feature.times.TimesSettings
import ir.taqvim.feature.tools.ToolsSettings
import ir.taqvim.feature.year.YearDay
import ir.taqvim.feature.year.YearSettings
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** Wiring of the Astronomy (T-1300), compass/level (T-1302/T-1303), tools (T-1400) and year (T-805) ports. */
class FeatureAdaptersTest {
    private val persian = UserPreferences.defaultsFor("fa")
    private val tehran =
        TimesSettings(
            placeName = "تهران",
            place = Coordinates(35.69, 51.42),
            timeZone = TimeZone.of("Asia/Tehran"),
            prayer = PrayerSettings(method = persian.prayerMethod, asr = persian.asrJuristic),
            language = persian.languageSpec(),
            calendar = PersianCalendarSystem,
        )

    @Test
    fun `astronomy settings follow the Times tab's chosen place`(): Unit =
        runTest {
            val times = MutableStateFlow<TimesSettings?>(null)
            val source = TimesAstronomySettingsSource { times }

            source.settings().first().shouldBeNull()
            times.value = tehran
            source.settings().first() shouldBe
                AstronomySettings(
                    placeName = "تهران",
                    place = tehran.place,
                    timeZone = tehran.timeZone,
                    language = tehran.language,
                    calendar = PersianCalendarSystem,
                    scorpioSystem = ZodiacSystem.IAU_CONSTELLATION,
                )
        }

    @Test
    fun `compass settings carry the app language and the chosen place`(): Unit =
        runTest {
            val times = MutableStateFlow<TimesSettings?>(null)
            val source = PreferencesCompassSettingsSource(repositoryOf(persian)) { times }

            val withoutPlace = source.settings().first()
            withoutPlace.language.code shouldBe "fa"
            withoutPlace.place.shouldBeNull()

            times.value = tehran
            source.settings().first().place shouldBe CompassPlace("تهران", tehran.place, tehran.timeZone)

            PreferencesCompassSettingsSource(repositoryOf(persian.copy(languageCode = "xx"))) { times }
                .settings()
                .first()
                .language.code shouldBe UserPreferences.FALLBACK_LANGUAGE
        }

    @Test
    fun `the session level calibration keeps the last saved offsets`(): Unit =
        runTest {
            val store = SessionLevelCalibrationStore()
            val calibration = LevelCalibration(persistentMapOf(DeviceOrientation.FLAT to Tilt(1.5, -0.5)))

            store.calibration().first() shouldBe LevelCalibration()
            store.save(calibration)
            store.calibration().first() shouldBe calibration
        }

    @Test
    fun `tools settings use the user's calendars, the device zone and the default workday profile`(): Unit =
        runTest {
            val profile = MutableStateFlow<WorkdayProfile?>(null)
            var lookups = 0
            val source =
                PreferencesToolsSettingsSource(
                    preferences = repositoryOf(persian.copy(calendars = listOf(CalendarSystem.NEPALI))),
                    workdayProfile = profile,
                    zone = { TimeZone.of("Asia/Kabul") },
                    loadLookup = {
                        lookups++
                        EventLookup(emptyList())
                    },
                )

            val first = source.settings().first()
            first.language.code shouldBe "fa"
            first.homeZone shouldBe TimeZone.of("Asia/Kabul")
            first.calendars shouldBe listOf(NepaliCalendarSystem)
            first.boardZones shouldBe emptyList()
            first.workdays.shouldBeNull()
            lookups shouldBe 0

            profile.value = FRIDAY_WEEKEND
            source
                .settings()
                .first()
                .workdays
                .shouldNotBeNull()
                .profile shouldBe FRIDAY_WEEKEND
            lookups shouldBe 1

            PreferencesToolsSettingsSource(repositoryOf(persian), flowOf(null))
                .settings()
                .first()
                .calendars
                .first() shouldBeSameInstanceAs PersianCalendarSystem
        }

    @Test
    fun `only the profile marked default is used for workdays`() {
        val other = profileEntity(id = 1, isDefault = false)
        val chosen = profileEntity(id = 2, isDefault = true)

        listOf(other, chosen).defaultProfile() shouldBe FRIDAY_WEEKEND
        listOf(other).defaultProfile().shouldBeNull()
    }

    @Test
    fun `year settings and day flags come from the preferences and the events repository`(): Unit =
        runTest {
            val variant = persian.copy(islamicVariant = IslamicVariant.UMM_AL_QURA)
            PreferencesYearSettingsSource(repositoryOf(variant)).settings().first() shouldBe
                YearSettings(variant.calendars, variant.weekStart, IslamicVariant.UMM_AL_QURA, "fa")

            val nowruz = LocalDate(2026, 3, 21).toJdn()
            val day =
                DayEvents(
                    jdn = nowruz,
                    islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1),
                    hijri = null,
                    official = emptyList(),
                    isHoliday = true,
                    isWeekend = false,
                    personal = emptyList(),
                    device = emptyList(),
                    ics = emptyList(),
                )
            RepositoryYearDaysSource { flowOf(listOf(day, day.copy(jdn = nowruz + 1, isHoliday = false))) }
                .days(nowruz..nowruz + 1)
                .first() shouldContainExactly
                listOf(YearDay(nowruz, isHoliday = true, isWeekend = false), YearDay(nowruz + 1, false, false))
        }

    @Test
    fun `the year view's today ticks and emits only day changes`(): Unit =
        runTest {
            val days = ArrayDeque(listOf(Jdn(10), Jdn(10), Jdn(11)))
            val provider = TodayProvider { days.removeFirstOrNull() ?: Jdn(11) }

            TickingYearTodaySource(provider, 1.minutes).today().take(2).toList() shouldBe listOf(Jdn(10), Jdn(11))
        }

    private fun profileEntity(
        id: Long,
        isDefault: Boolean,
    ) = WorkdayProfileEntity(
        id = id,
        name = "p$id",
        weekend = if (isDefault) FRIDAY_WEEKEND.weekend else setOf(Weekday.SUNDAY),
        holidaySources = FRIDAY_WEEKEND.holidaySources,
        halfDays = FRIDAY_WEEKEND.halfDays,
        personalLeave = emptyList(),
        isDefault = isDefault,
    )

    private companion object {
        val FRIDAY_WEEKEND =
            WorkdayProfile(
                weekend = setOf(Weekday.FRIDAY),
                holidaySources = setOf(EventSource.IRAN_OFFICIAL),
                halfDays = HalfDayPolicy.HALF,
            )
    }
}
