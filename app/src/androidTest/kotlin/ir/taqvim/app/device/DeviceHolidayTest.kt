/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.feature.calendar.R as CalendarR
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * J09 (docs/device-journeys.md): a holiday-bearing day is marked in the month grid, and the title shown on its day
 * matches [EventsRepository] — the exact source the UI reads from — instead of only asserting that the day's screen
 * opens (R11). Nowruz (Persian New Year, `PERSIAN` year/1/1) is used because every dataset year Taqvim supports
 * carries it; the expected title is read from the dataset at run time, never hardcoded here, per the project's
 * "compute every value, tables only as goldens" rule.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // useLanguage needs LocaleManager (ADR-0023)
class DeviceHolidayTest {
    @Test
    fun nowruzIsMarkedAndItsTitleMatchesTheDataset() {
        useLanguage("fa")
        val events = GlobalContext.get().get<EventsRepository>()
        val today = Clock.System.now().toJdn(TimeZone.currentSystemDefault())
        val year = PersianCalendarSystem.fromJdn(today).year
        val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))

        val dayEvents = runBlocking { events.day(nowruz).first() }
        check(dayEvents.isHoliday) { "Nowruz $year is not marked as a holiday by the dataset" }
        val occurrence =
            dayEvents.official.firstOrNull { it.isHoliday }
                ?: error("Nowruz $year has no holiday occurrence in the dataset")
        val title = occurrence.definition.title.forLanguage("fa")
        val holidayWord = appContext.getString(CalendarR.string.calendar_holiday)

        openLink("taqvim://day/$year-1-1", "destination:Day")
        device.wait(Until.findObject(By.textContains(title)), DEVICE_TIMEOUT_MILLIS)
            ?: error("'$title' is not shown on Nowruz's day screen")
        device.wait(Until.findObject(By.descContains(holidayWord)), DEVICE_TIMEOUT_MILLIS)
            ?: error(
                "no day cell's description marks a holiday ('$holidayWord'); Nowruz is not shown as one in the grid",
            )
    }
}
