/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.os.Build
import androidx.datastore.dataStoreFile
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
import ir.taqvim.data.preferences.UserPrefsSerializer
import ir.taqvim.data.preferences.toDomain
import kotlin.time.Clock
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
 * abort the instrumentation itself rather than exercise a clean "restart". A second, independent
 * [UserPreferencesRepository]/`DataStore` over the same file was tried first and rejected at run time:
 * `IllegalStateException: There are multiple DataStores active for the same file` — DataStore enforces a single
 * active instance per file via its coordinator/file lock, so that approach cannot work in-process at all, on any API
 * level (CI run 35854629904). Durability is instead proven by reading the exact bytes on disk directly, bypassing
 * DataStore (and its coordinator) entirely: [UserPrefsSerializer] — the same `Serializer` `UserPreferencesRepository`
 * itself uses — parses the file `UserPreferencesRepository.createDataStore` points at, with no `DataStore` instance
 * involved and therefore no conflict with the running app's own singleton. `updatePreferences` only returns after its
 * suspend `dataStore.updateData` call has committed, so by the time the write returns, these are the literal
 * committed bytes a fresh process would open at start-up, not an in-memory value.
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

    /** The preferences parsed straight from the on-disk file, without creating a second `DataStore` over it. */
    private fun coldRead() =
        runBlocking {
            val file = appContext.dataStoreFile(UserPreferencesRepository.FILE_NAME)
            file.inputStream().use { UserPrefsSerializer.readFrom(it) }.toDomain()
        }
}
