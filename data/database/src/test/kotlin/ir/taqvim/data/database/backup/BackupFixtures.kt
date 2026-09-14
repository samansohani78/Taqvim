/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import ir.taqvim.core.events.EventSource
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.InvalidDatePolicy
import ir.taqvim.core.ics.WeekdayNum
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.LeaveRange
import ir.taqvim.data.database.EventRecurrenceEntity
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.OfficialReminderEntity
import ir.taqvim.data.database.PersonalData
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.database.ReminderEntity
import ir.taqvim.data.database.ShiftRotationEntity
import ir.taqvim.data.database.ShiftRotationRecordEntity
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.preferences.AppSettings
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanPreferences
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.ThemeMode
import ir.taqvim.data.preferences.UserPreferences

/** Synthetic personal data covering every backed-up table and every nullable column (no real user data). */
internal object BackupFixtures {
    private const val NOWRUZ_1405 = 2_461_120L

    val metadata = BackupMetadata(appVersion = "0.1.0", createdAtEpochMillis = 1_789_000_000_000)

    val preferences: UserPreferences =
        UserPreferences.defaultsFor("fa").copy(
            prayerMethod = PrayerMethod.JAFARI,
            themeMode = ThemeMode.BLACK,
            hijriOffsetDays = -1,
            hijriOffsetSetAtEpochMillis = 1_788_000_000_000,
            place = ChosenPlace(PlaceSource.CITY, 112_931, "تهران", Coordinates(35.6892, 51.389), "Asia/Tehran"),
            app =
                AppSettings.DEFAULT.copy(
                    showWeekNumbers = true,
                    enabledEventSources = setOf(EventSource.IRAN_OFFICIAL, EventSource.ANCIENT_IRAN),
                    eventSourcesChosen = true,
                    highLatitudeRule = HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS,
                    subscriptionsNetworkAllowed = false,
                    timeZoneBoard = listOf("Asia/Kabul", "Europe/Berlin"),
                    allDayReminderMinute = 480,
                    persistentNotificationLargeNumber = true,
                    dynamicLauncherIcon = true,
                ),
            athan =
                AthanPreferences.DEFAULT.copy(
                    alerts =
                        AthanPrayer.entries.associateWith { prayer ->
                            val gap = if (prayer == AthanPrayer.FAJR) -10 else 0
                            AthanAlert(enabled = prayer != AthanPrayer.ASR, gapMinutes = gap)
                        },
                    vibrate = false,
                    bypassDndForFajr = true,
                    volumePercent = 60,
                    useIranTime = true,
                ),
        )

    val data =
        PersonalData(
            events =
                listOf(
                    PersonalEventEntity(
                        id = 3,
                        title = "تولد مادر",
                        notes = "کیک \"شکلاتی\"\nو گل",
                        calendarSystem = CalendarSystem.PERSIAN,
                        startJdn = NOWRUZ_1405,
                        endJdn = NOWRUZ_1405,
                        timeZoneId = "Asia/Tehran",
                        createdAtEpochMillis = 1,
                        updatedAtEpochMillis = 2,
                    ),
                    PersonalEventEntity(
                        id = 9,
                        title = "Dentist",
                        calendarSystem = CalendarSystem.GREGORIAN,
                        startJdn = NOWRUZ_1405 + 4,
                        startMinute = 540,
                        endJdn = NOWRUZ_1405 + 4,
                        endMinute = 600,
                        timeZoneId = "Europe/Berlin",
                        colorArgb = -16_711_936,
                        createdAtEpochMillis = 3,
                        updatedAtEpochMillis = 4,
                        sourceLink = "https://example.org/dentist",
                    ),
                ),
            recurrences =
                listOf(
                    EventRecurrenceEntity(
                        eventId = 3,
                        frequency = Frequency.YEARLY,
                        interval = 1,
                        count = null,
                        untilJdn = NOWRUZ_1405 + 3_650,
                        byDay = listOf(WeekdayNum(Weekday.FRIDAY, -1), WeekdayNum(Weekday.MONDAY)),
                        byMonthDay = listOf(1, -1),
                        calendarSystem = CalendarSystem.PERSIAN,
                        invalidDates = InvalidDatePolicy.NEXT_DAY,
                        weekStart = Weekday.SATURDAY,
                    ),
                ),
            reminders = listOf(ReminderEntity(5, 3, 1_440), ReminderEntity(6, 9, 30, enabled = false)),
            shiftRotations = listOf(ShiftRotationEntity(2, "Hospital", NOWRUZ_1405, listOf("D", "N", "", "Off"))),
            shiftRecords = listOf(ShiftRotationRecordEntity(2, NOWRUZ_1405 + 1, "N", "swap")),
            icsSubscriptions =
                listOf(
                    IcsSubscriptionEntity(
                        id = 4,
                        url = "https://example.org/feed.ics",
                        displayName = "Team",
                        colorArgb = 255,
                        refreshIntervalMinutes = 360,
                    ),
                ),
            workdayProfiles =
                listOf(
                    WorkdayProfileEntity(
                        id = 1,
                        name = "Office",
                        weekend = setOf(Weekday.FRIDAY),
                        holidaySources = setOf(EventSource.IRAN_OFFICIAL, EventSource.USER),
                        halfDays = HalfDayPolicy.FULL_WORKDAY,
                        personalLeave = listOf(LeaveRange(Jdn(NOWRUZ_1405 + 10), Jdn(NOWRUZ_1405 + 12))),
                        isDefault = true,
                    ),
                ),
            officialReminders =
                listOf(
                    OfficialReminderEntity(8, "ir.holiday.nowruz-1", 3),
                    OfficialReminderEntity(12, "ir.ancient.yalda", 0, enabled = false),
                ),
        )
}
