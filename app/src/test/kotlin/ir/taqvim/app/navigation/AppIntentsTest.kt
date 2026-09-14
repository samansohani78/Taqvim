/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.nlp.ParseContext
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/** T-1103: which intents ask for a screen — `taqvim://` links and selected text — and how selected text is read. */
@RunWith(AndroidJUnit4::class)
class AppIntentsTest {
    private val nowruz1405 = LocalDate(2026, 3, 21).toJdn()
    private val english = ParseContext.forLanguage(requireNotNull(LanguageTable.forCode("en")), nowruz1405)

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    private fun destination(intent: Intent?) = AppIntents.destination(intent) { english }

    @Test
    fun linksAndSelectedTextAskForTheirScreens() {
        val day = AppDestination.Day(nowruz1405.value)
        assertEquals(day, destination(Intent(Intent.ACTION_VIEW, Uri.parse("taqvim://day/1405-01-01"))))
        assertEquals(AppDestination.Times, destination(Intent(Intent.ACTION_VIEW, Uri.parse("TAQVIM://times"))))
        assertNull(destination(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/day/1405-01-01"))))
        assertNull(destination(Intent(Intent.ACTION_VIEW)))

        val selected = Intent(Intent.ACTION_PROCESS_TEXT).putExtra(Intent.EXTRA_PROCESS_TEXT, "Due 21 March 2026")
        assertEquals(day, destination(selected))
        assertNull(destination(Intent(Intent.ACTION_PROCESS_TEXT)))

        assertNull(destination(Intent(Intent.ACTION_MAIN)))
        assertNull(destination(null))
    }

    @Test
    fun selectedTextIsReadInTheDeviceLanguageRelativeToToday() {
        val now = Instant.parse("2026-09-14T21:00:00Z")

        val persian = AppIntents.parseContext(now, TimeZone.of("Asia/Tehran"), "fa")
        assertEquals(CalendarSystem.PERSIAN, persian.preferredCalendar)
        assertEquals(LocalDate(2026, 9, 15).toJdn(), persian.reference)

        val unsupported = AppIntents.parseContext(now, TimeZone.UTC, "xx")
        assertEquals(CalendarSystem.GREGORIAN, unsupported.preferredCalendar)
        assertEquals(LocalDate(2026, 9, 14).toJdn(), unsupported.reference)
    }
}
