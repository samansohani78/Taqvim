/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn
import kotlin.math.abs

/** A date the detector must report: character [span] and the first (and, for a range, last) day. */
internal data class ExpectedDate(
    val span: IntRange,
    val start: Jdn,
    val end: Jdn? = null,
) {
    override fun toString(): String = "${span.first}-${span.last}:${start.value}" + (end?.let { "..${it.value}" } ?: "")

    companion object {
        fun parse(text: String): ExpectedDate {
            val (span, days) = text.split(':')
            val (first, last) = span.split('-').map(String::toInt)
            val jdns = days.split("..").map { Jdn(it.toLong()) }
            return ExpectedDate(first..last, jdns.first(), jdns.getOrNull(1))
        }
    }
}

/** One snippet of the T-501 corpus with its reading context and expected detections. */
internal data class SnippetRow(
    val id: String,
    val language: String,
    val calendar: CalendarSystem,
    val reference: Jdn,
    val text: String,
    val expected: List<ExpectedDate>,
) {
    fun toLine(): String {
        val referenceDate = GregorianCalendarSystem.fromJdn(reference).toIsoLikeString()
        return listOf(id, "$language;${calendar.name};$referenceDate", text, describe(expected)).joinToString("\t")
    }

    fun context(): ParseContext =
        ParseContext.forLanguage(requireNotNull(LanguageTable.forCode(language)), reference, calendar)

    companion object {
        private const val NONE = "-"

        fun describe(dates: List<ExpectedDate>): String = dates.joinToString("|").ifEmpty { NONE }

        fun parse(line: String): SnippetRow {
            val columns = line.split('\t')
            val (language, calendar, reference) = columns[1].split(';')
            val (year, month, day) = reference.split('-').map(String::toInt)
            return SnippetRow(
                id = columns[0],
                language = language,
                calendar = CalendarSystem.valueOf(calendar),
                reference = GregorianCalendarSystem.toJdn(CalendarDate(GREGORIAN, year, month, day)),
                text = columns[2],
                expected =
                    columns[3]
                        .takeIf { it != NONE }
                        ?.split('|')
                        ?.map(ExpectedDate::parse)
                        .orEmpty(),
            )
        }
    }
}

/** A phrase to insert into a template; the reported span starts [spanStart] characters into [text]. */
internal data class Phrase(
    val text: String,
    val start: Jdn,
    val end: Jdn? = null,
    val spanStart: Int = 0,
)

/**
 * Generator of the T-501 golden snippets: everyday messages, notices and e-mails written for this project (no copied
 * text), with date phrases whose days come from `:core:calendar`, and number-heavy snippets that contain no date.
 */
internal object TextSnippets {
    private const val POSITIVE = 88
    private const val YEARS_AROUND = 5
    private const val MAX_RANGE_DAYS = 40
    private const val MAX_YEARLESS_DAY = 29
    private val SLOT = Regex("""\{[dr]}""")

    fun rows(kit: CorpusKit): List<SnippetRow> =
        positives(kit, "fa", PERSIAN, SnippetTemplates.FA) + negatives(kit, "fa", PERSIAN, SnippetTemplates.FA_NONE) +
            positives(kit, "en", GREGORIAN, SnippetTemplates.EN) +
            negatives(kit, "en", GREGORIAN, SnippetTemplates.EN_NONE)

    private fun positives(
        kit: CorpusKit,
        language: String,
        calendar: CalendarSystem,
        templates: List<String>,
    ): List<SnippetRow> =
        List(POSITIVE) { i ->
            val reference = kit.reference()
            val template = templates[i % templates.size]
            val phrases =
                SLOT.findAll(template).toList().map { slot ->
                    val variant = kit.number(0..DateRenderers.VARIANTS)
                    when {
                        slot.value == "{r}" && language == "fa" -> DateRenderers.faRange(kit, reference, variant)
                        slot.value == "{r}" -> DateRenderers.enRange(kit, reference, variant)
                        language == "fa" -> DateRenderers.faSingle(kit, reference, variant)
                        else -> DateRenderers.enSingle(kit, reference, variant)
                    }
                }
            val (text, expected) = fill(template, phrases)
            val localized = if (language == "fa" && i % 3 != 0) kit.persianDigits(text) else text
            SnippetRow(CorpusKit.id("$language-text", i), language, calendar, reference, localized, expected)
        }

    private fun negatives(
        kit: CorpusKit,
        language: String,
        calendar: CalendarSystem,
        texts: List<String>,
    ): List<SnippetRow> =
        texts.mapIndexed { i, text ->
            val localized = if (language == "fa" && i % 2 == 0) kit.persianDigits(text) else text
            SnippetRow(CorpusKit.id("$language-none", i), language, calendar, kit.reference(), localized, emptyList())
        }

    /** [template] with its `{d}`/`{r}` slots replaced by [phrases], and where each phrase's span ends up. */
    fun fill(
        template: String,
        phrases: List<Phrase>,
    ): Pair<String, List<ExpectedDate>> {
        val parts = template.split(SLOT)
        val text = StringBuilder(parts.first())
        val expected =
            phrases.mapIndexed { i, phrase ->
                val first = text.length + phrase.spanStart
                text.append(phrase.text)
                val last = text.length - 1
                text.append(parts[i + 1])
                ExpectedDate(first..last, phrase.start, phrase.end)
            }
        return text.toString() to expected
    }

    /** A random date within [YEARS_AROUND] years of [reference] in [system]. */
    fun near(
        kit: CorpusKit,
        system: CalendarSystem,
        reference: Jdn,
    ): CalendarDate {
        val year = kit.calendar(system).fromJdn(reference).year
        return kit.date(system, year - YEARS_AROUND..year + YEARS_AROUND)
    }

    fun rangeDays(kit: CorpusKit): Int = kit.number(1..MAX_RANGE_DAYS)

    fun yearlessDay(kit: CorpusKit): Int = kit.number(1..MAX_YEARLESS_DAY)

    /** The occurrence of Persian [month]/[day] closest to [reference] (the later one on a tie). */
    fun nearest(
        kit: CorpusKit,
        reference: Jdn,
        month: Int,
        day: Int,
    ): Jdn {
        val year = kit.calendar(PERSIAN).fromJdn(reference).year
        return (year - 1..year + 1)
            .map { kit.jdn(CalendarDate(PERSIAN, it, month, day)) }
            .minWith(compareBy({ abs(it - reference) }, { if (it >= reference) 0 else 1 }))
    }
}
