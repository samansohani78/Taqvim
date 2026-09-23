/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.ui.component.segmentTag
import ir.taqvim.data.database.backup.BackupMetadata
import ir.taqvim.data.database.backup.BackupProtection
import ir.taqvim.data.database.backup.BackupReadResult
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.database.backup.RestoreResult
import ir.taqvim.feature.calendar.DayDetailsTab
import ir.taqvim.feature.events.PersonalEvent
import ir.taqvim.feature.events.PersonalEventStore
import kotlin.time.Clock
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/**
 * J30 (docs/device-journeys.md): backup, wipe, restore brings personal data back (F-11, T-605), driven through the
 * real [BackupService] the settings screen uses (`app/src/main/kotlin/ir/taqvim/app/di/AppModule.kt`), not a copy of
 * its logic. "Wipe" deletes the one event this test created, rather than every table `BackupService.restore` can
 * touch: a full-database reset would also erase whatever the device already had before this test ran, which other
 * suites sharing this install (no `clearPackageData`, ADR-... none configured) may still need — the smallest
 * destructive action that only a real restore, not the app simply staying alive, can undo is enough to prove the
 * journey.
 *
 * A first version asserted the title right after opening the day; it failed on CI (run 35854629904, API 33 and 36)
 * for the same two reasons [DeviceHolidayTest] did, confirmed by reproducing both locally on `d1api33` — neither is
 * the restore's fault:
 * - `AppDestination.Day`'s day-details pane defaults to the `CALENDARS` tab, and an event's title lives on the
 *   `EVENTS` tab (`DayDetailsPanel.kt`), which nothing had selected. Fixed by selecting
 *   `segmentTag(DayDetailsTab.EVENTS.ordinal)` first.
 * - `By.textContains(TITLE)` still found nothing (confirmed with a window-hierarchy dump): the event row is an
 *   `EventChip` (`core/ui/component/EventChip.kt`), which uses `.clearAndSetSemantics { contentDescription = ... }`
 *   to expose only a `contentDescription` (starting with the title; `DayEventsTab.kt`'s `chipDescription`), not
 *   plain text. Fixed by matching `By.descContains(TITLE)` instead. The restored event was on screen the whole time;
 *   the selector could not see it.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU) // useLanguage needs LocaleManager (ADR-0023)
class DeviceBackupRestoreTest {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun backupWipeRestoreBringsTheEventBack() {
        useLanguage("en")
        val store = GlobalContext.get().get<PersonalEventStore>()
        val backup = GlobalContext.get().get<BackupService>()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val date = CalendarDate(CalendarSystem.GREGORIAN, today.year, today.month.ordinal + 1, today.day)
        val event =
            PersonalEvent(
                title = TITLE,
                calendar = CalendarSystem.GREGORIAN,
                start = date,
                end = date,
                timeZoneId = TimeZone.currentSystemDefault().id,
            )
        val id = runBlocking { store.save(event) }
        try {
            val metadata = BackupMetadata(appVersion = "device-test", createdAtEpochMillis = System.currentTimeMillis())
            val snapshot = runBlocking { backup.export(metadata, BackupProtection.None) }

            // Wipe: remove the event the backup carries, simulating the data loss a restore is meant to undo.
            runBlocking { store.delete(id) }
            check(runBlocking { store.load(id) } == null) { "the event was not wiped before the restore" }

            val opened = runBlocking { backup.read(snapshot) }
            check(opened is BackupReadResult.Ready) { "the backup this test just wrote does not read back: $opened" }
            val restored = runBlocking { backup.restore(opened.backup) }
            check(restored is RestoreResult.Restored) { "restore failed: $restored" }
            check(runBlocking { store.load(id) }?.title == TITLE) { "the event is not back in storage after restore" }

            openLink("taqvim://day/${date.year}-${date.month}-${date.day}?calendar=gregorian", "destination:Day")
            awaitTag(segmentTag(DayDetailsTab.EVENTS.ordinal)).click()
            device.waitForIdle()
            device.wait(Until.findObject(By.descContains(TITLE)), DEVICE_TIMEOUT_MILLIS)
                ?: error("'$TITLE' is not shown on its day's Events tab after restore")
        } finally {
            runBlocking { runCatching { store.delete(id) } }
        }
    }

    private companion object {
        const val TITLE = "Device backup roundtrip check"
    }
}
