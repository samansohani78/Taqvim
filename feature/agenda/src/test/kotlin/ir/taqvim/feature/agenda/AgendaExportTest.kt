/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

/** T-901 print/export: the loaded rows as shareable text and as a printable HTML document. */
class AgendaExportTest {
    private val texts =
        AgendaTexts(
            monthTitle = { name, year -> "$name $year" },
            monthRange = { first, last -> "$first – $last" },
            separator = " · ",
            today = "Today",
            holiday = "Holiday",
            noEvents = "No events",
            emptyMonth = "No events this month",
            kinds = mapOf(AgendaEventKind.OFFICIAL to "Official"),
            exportTitle = { from, to -> "Agenda: $from – $to" },
        )

    @Test
    fun `text lists months, days with flags and events, and empty months`() {
        val text = AgendaExport.text(sampleContent(), texts)
        text shouldStartWith "Agenda: Thursday, September 3, 2026 – Friday, October 23, 2026"
        text shouldContain "\nAugust 2026\nNo events this month\n"
        text shouldContain "\nSeptember 2026\nThursday, September 3, 2026: Dentist\n"
        text shouldContain "Sunday, September 13, 2026 · Today: No events"
        text shouldContain "Tuesday, September 15, 2026 · Holiday: Holiday event · Birthday"
        text shouldContain "\nOctober 2026\nFriday, October 23, 2026: Match\n"
        text.endsWith("November 2026\nNo events this month") shouldBe true
    }

    @Test
    fun `html follows the language direction and escapes texts`() {
        val content = sampleContent(PERSIAN_FA, events = mapOf(TODAY to listOf(event("x", "<b>&\"'", holiday = true))))
        val html = AgendaExport.html(content, texts)
        html shouldStartWith "<!DOCTYPE html><html lang=\"fa-IR\" dir=\"rtl\">"
        html shouldContain "<p class=\"holiday\">&lt;b&gt;&amp;&quot;&#39;</p>"
        html shouldContain "<h2>شهریور ۱۴۰۵</h2>"
        html shouldContain "<p>No events this month</p>"
        html shouldNotContain "<b>&"
        AgendaExport.html(sampleContent(), texts) shouldContain "dir=\"ltr\""
    }

    @Test
    fun `month subtitles join the other calendars' months`() {
        val header = sampleContent(PERSIAN_FA).items.filterIsInstance<AgendaMonthHeader>()[1]
        val subtitle = texts.subtitle(header).orEmpty()
        subtitle shouldContain " – "
        subtitle shouldContain " · "
        texts.subtitle(header.copy(otherCalendars = kotlinx.collections.immutable.persistentListOf())) shouldBe null
        texts.kind(event("x", "X")) shouldBe "Official"
        texts.kind(event("y", "Y", AgendaEventKind.DEVICE)) shouldBe ""
    }
}
