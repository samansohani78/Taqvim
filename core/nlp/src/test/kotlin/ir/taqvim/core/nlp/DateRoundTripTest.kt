/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import io.kotest.matchers.collections.shouldBeEmpty
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import org.junit.jupiter.api.Test

/**
 * P tests of T-500: T-202 [DateFormatter] output parses back to the same day. NUMERIC and ISO must round-trip in all
 * 24 languages and the three available calendars (no Nepali arithmetic yet); LONG must round-trip in fa and en, and
 * its coverage in the other languages is recorded in a snapshot rather than asserted.
 */
class DateRoundTripTest {
    private val start = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 1950, 1, 3))
    private val samples: List<Jdn> = List(SAMPLES) { start + it * STEP_DAYS }

    private fun failure(
        language: LanguageSpec,
        system: CalendarSystem,
        jdn: Jdn,
        style: DateStyle,
    ): String? {
        val date = ParseContext.DEFAULT_CALENDARS.getValue(system).fromJdn(jdn)
        val text = DateFormatter.format(date, jdn.weekday(), language, style)
        val context = ParseContext.forLanguage(language, jdn + REFERENCE_OFFSET, system)
        val parsed = DateParser.parseBest(text, context)
        return if (parsed?.jdn == jdn) null else "${language.code} $system $style '$text' -> ${parsed?.date}"
    }

    @Test
    fun `numeric and ISO output parses back in every language`() {
        val failures =
            LanguageTable.languages.flatMap { language ->
                SYSTEMS.flatMap { system ->
                    samples.flatMap { jdn ->
                        listOf(DateStyle.NUMERIC, DateStyle.ISO).mapNotNull { failure(language, system, jdn, it) }
                    }
                }
            }
        failures.shouldBeEmpty()
    }

    @Test
    fun `long output parses back in Persian and English and its coverage is recorded`() {
        val results =
            LanguageTable.languages.flatMap { language ->
                SYSTEMS.map { system ->
                    Triple(language, system, samples.mapNotNull { failure(language, system, it, DateStyle.LONG) })
                }
            }
        val table =
            listOf("# language<TAB>calendar<TAB>LONG samples parsed back (of $SAMPLES)") +
                results.map { (language, system, failures) -> "${language.code}\t$system\t${SAMPLES - failures.size}" }
        val snapshot = File("src/test/resources/snapshots/long-round-trip.tsv")
        SnapshotVerifier().verify(snapshot, table.joinToString("\n"), "DateRoundTripTest").getOrThrow()
        results.filter { it.first.code in ASSERTED }.flatMap { it.third }.shouldBeEmpty()
    }

    private companion object {
        const val SAMPLES = 60
        const val STEP_DAYS = 919
        const val REFERENCE_OFFSET = 400
        val SYSTEMS = listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)
        val ASSERTED = setOf("fa", "en")
    }
}
