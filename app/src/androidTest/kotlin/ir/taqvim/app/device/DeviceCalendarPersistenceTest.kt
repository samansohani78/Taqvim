/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.preferences.UserPreferencesRepository
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.Test

/**
 * J37 (docs/device-journeys.md): changing the primary calendar (`SettingsCatalog.SettingsItemId.MAIN_CALENDAR`, a
 * table-driven settings row) durably persists and the calendar screen shows it, not only for as long as the running
 * process happens to keep it in memory.
 *
 * This module's instrumented tests are self-instrumenting: the test code runs inside the app's own process (no
 * `android:targetProcess` split, no Test Orchestrator in `app/build.gradle.kts`), so `am force-stop` on the app would
 * abort the instrumentation itself rather than exercise a clean "restart". Durability is instead proven the way a new
 * process would actually experience it: a brand-new [UserPreferencesRepository] is built over
 * [UserPreferencesRepository.createDataStore] pointed at the same on-disk file (`user_prefs.pb`), bypassing the
 * running app's live Koin singleton and its in-memory `StateFlow` entirely — a cold read of the same bytes a fresh
 * process would open at start-up. Only after that cold read succeeds is the app asked to show the day, so the UI
 * assertion also exercises the ordinary (still-running) read path.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // useLanguage needs LocaleManager (ADR-0023)
class DeviceCalendarPersistenceTest {
    @Test
    fun changedPrimaryCalendarSurvivesAColdReadAndShowsInTheMonthGrid() {
        useLanguage("fa")
        val original = runBlocking { preferences.preferences.first() }.calendars
        try {
            val changed =
                updatePreferences { current ->
                    current.copy(
                        calendars =
                            listOf(CalendarSystem.GREGORIAN) + (current.calendars - CalendarSystem.GREGORIAN),
                    )
                }
            check(changed.calendars.first() == CalendarSystem.GREGORIAN) { "the write itself did not take effect" }

            val cold = coldRead()
            check(cold.calendars.first() == CalendarSystem.GREGORIAN) {
                "a fresh read of the preferences file still has ${cold.calendars.first()} as the primary calendar"
            }

            val today = Clock.System.now().toJdn(TimeZone.currentSystemDefault())
            val language = requireNotNull(LanguageTable.forCode("fa")) { "language table lacks fa" }
            val expected =
                DateFormatter.format(
                    GregorianCalendarSystem.fromJdn(today),
                    today.weekday(),
                    language,
                    DateStyle.LONG,
                )

            openLink("taqvim://calendar", "destination:Calendar")
            device.wait(Until.findObject(By.descContains(expected)), DEVICE_TIMEOUT_MILLIS)
                ?: error(
                    "no day cell describes today as '$expected'; " +
                        "the month grid did not pick up the new primary calendar",
                )
        } finally {
            runCatching { updatePreferences { it.copy(calendars = original) } }
        }
    }

    /** The preferences read from a second, independent [UserPreferencesRepository] over the same on-disk file. */
    private fun coldRead() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob())
            try {
                val store = UserPreferencesRepository.createDataStore(appContext, scope) { "fa" }
                UserPreferencesRepository(store).preferences.first()
            } finally {
                scope.cancel()
            }
        }
}
