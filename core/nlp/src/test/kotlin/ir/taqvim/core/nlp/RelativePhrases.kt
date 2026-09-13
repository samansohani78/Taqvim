/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.nlp.CorpusKit.Companion.id

/** Relative (120), anchored (50) and range (30) phrases of the synthetic corpus. */
internal object RelativePhrases {
    private val FA_WORDS = listOf("یک", "دو", "سه", "چهار", "پنج")
    private val EN_WORDS = listOf("a", "two", "three", "four", "five")

    /** Random inputs of one relative template. */
    private data class Seed(
        val kit: CorpusKit,
        val index: Int,
        val reference: Jdn,
        val count: Int,
        val day: Weekday,
    ) {
        val small = count % FA_WORDS.size + 1
    }

    private val faTemplates: List<(Seed) -> Pair<String, Jdn>> =
        listOf(
            { s -> "امروز" to s.reference },
            { s -> "فردا" to s.reference + 1 },
            { s -> "پس‌فردا" to s.reference + 2 },
            { s -> "دیروز" to s.reference - 1 },
            { s -> "پریروز" to s.reference - 2 },
            { s -> "${s.count} روز بعد" to s.reference + s.count },
            { s -> "${s.count} روز قبل" to s.reference - s.count },
            { s -> "${s.small} هفته بعد" to s.reference + s.small * CorpusKit.DAYS_PER_WEEK },
            { s -> "${s.count} ماه بعد" to s.kit.months(PERSIAN, s.reference, s.count) },
            { s -> "${s.small} سال قبل" to s.kit.months(PERSIAN, s.reference, -12 * s.small) },
            { s -> "${s.kit.weekdayName(s.kit.fa, s.day)} آینده" to s.kit.next(s.reference, s.day) },
            { s -> "${s.kit.weekdayName(s.kit.fa, s.day)} گذشته" to s.kit.previous(s.reference, s.day) },
            { s -> "این ${s.kit.weekdayName(s.kit.fa, s.day)}" to s.kit.upcoming(s.reference, s.day) },
            { s -> "${FA_WORDS[s.small - 1]} روز دیگر" to s.reference + s.small },
            { s ->
                listOf(
                    "هفته بعد" to s.reference + CorpusKit.DAYS_PER_WEEK,
                    "ماه آینده" to s.kit.months(PERSIAN, s.reference, 1),
                    "سال گذشته" to s.kit.months(PERSIAN, s.reference, -12),
                )[s.index % 3]
            },
        )

    private val enTemplates: List<(Seed) -> Pair<String, Jdn>> =
        listOf(
            { s -> "today" to s.reference },
            { s -> "tomorrow" to s.reference + 1 },
            { s -> "the day after tomorrow" to s.reference + 2 },
            { s -> "yesterday" to s.reference - 1 },
            { s -> "the day before yesterday" to s.reference - 2 },
            { s -> "in ${s.count} days" to s.reference + s.count },
            { s -> "${s.count} days ago" to s.reference - s.count },
            { s -> "${s.small} weeks later" to s.reference + s.small * CorpusKit.DAYS_PER_WEEK },
            { s -> "${s.count} months from now" to s.kit.months(GREGORIAN, s.reference, s.count) },
            { s ->
                val unit = if (s.small == 1) "year" else "years"
                "${EN_WORDS[s.small - 1]} $unit ago" to s.kit.months(GREGORIAN, s.reference, -12 * s.small)
            },
            { s -> "next ${s.kit.weekdayName(s.kit.en, s.day)}" to s.kit.next(s.reference, s.day) },
            { s -> "last ${s.kit.weekdayName(s.kit.en, s.day)}" to s.kit.previous(s.reference, s.day) },
            { s -> "this ${s.kit.weekdayName(s.kit.en, s.day)}" to s.kit.upcoming(s.reference, s.day) },
            { s -> "see you in ${s.count} days" to s.reference + s.count },
            { s ->
                listOf(
                    "next week" to s.reference + CorpusKit.DAYS_PER_WEEK,
                    "next month" to s.kit.months(GREGORIAN, s.reference, 1),
                    "last year" to s.kit.months(GREGORIAN, s.reference, -12),
                )[s.index % 3]
            },
        )

    fun rows(kit: CorpusKit): List<CorpusRow> = relative(kit) + anchored(kit) + ranges(kit)

    private fun relative(kit: CorpusKit): List<CorpusRow> {
        val fa =
            List(60) { i ->
                val seed = Seed(kit, i / faTemplates.size, kit.reference(), kit.number(2..29), kit.weekday())
                val (text, jdn) = faTemplates[i % faTemplates.size](seed)
                CorpusRow(id("fa-relative", i), "fa", PERSIAN, seed.reference, kit.persianDigits(text), jdn)
            }
        val en =
            List(60) { i ->
                val seed = Seed(kit, i / enTemplates.size, kit.reference(), kit.number(2..29), kit.weekday())
                val (text, jdn) = enTemplates[i % enTemplates.size](seed)
                CorpusRow(id("en-relative", i), "en", GREGORIAN, seed.reference, text, jdn)
            }
        return fa + en
    }

    private fun anchored(kit: CorpusKit): List<CorpusRow> {
        val fa =
            List(30) { i ->
                val reference = kit.reference()
                val event = SyntheticEvents.persian[i % 2]
                val day = SyntheticEvents.occurrence(event, reference)
                val n = kit.number(1..20)
                val (text, jdn) =
                    listOf(
                        "$n روز قبل از ${event.name}" to day - n,
                        "$n روز بعد از ${event.name}" to day + n,
                        "$n هفته پیش از ${event.name}" to day - n * CorpusKit.DAYS_PER_WEEK,
                        "$n روز پس از ${event.name}" to day + n,
                        event.name to day,
                    )[i % 5]
                CorpusRow(id("fa-anchored", i), "fa", PERSIAN, reference, kit.persianDigits(text), jdn)
            }
        val en =
            List(20) { i ->
                val reference = kit.reference()
                val event = SyntheticEvents.english[i % 2]
                val day = SyntheticEvents.occurrence(event, reference)
                val n = kit.number(2..20)
                val (text, jdn) =
                    listOf(
                        "$n days before ${event.name}" to day - n,
                        "$n weeks after ${event.name}" to day + n * CorpusKit.DAYS_PER_WEEK,
                        event.name to day,
                        "1 day after ${event.name}" to day + 1,
                    )[i % 4]
                CorpusRow(id("en-anchored", i), "en", GREGORIAN, reference, text, jdn)
            }
        return fa + en
    }

    private fun ranges(kit: CorpusKit): List<CorpusRow> = faRanges(kit) + enRanges(kit)

    private fun faRanges(kit: CorpusKit): List<CorpusRow> =
        List(20) { i ->
            val reference = kit.reference()
            val calendar = kit.calendar(PERSIAN)
            val first = kit.date(PERSIAN, 1380..1420)
            val month = kit.monthName(kit.fa, PERSIAN, first.month)
            val row =
                if (i < 10) {
                    val last = calendar.fromJdn(kit.jdn(first) + kit.number(1..90))
                    val lastMonth = kit.monthName(kit.fa, PERSIAN, last.month)
                    val text = "از ${first.day} $month ${first.year} تا ${last.day} $lastMonth ${last.year}"
                    CorpusRow(id("fa-range", i), "fa", PERSIAN, reference, text, kit.jdn(first), kit.jdn(last))
                } else {
                    val lastDay = kit.number(2..calendar.monthLength(first.year, first.month))
                    val startDay = kit.number(1 until lastDay)
                    val prefix = if (i % 2 == 0) "از " else ""
                    val text = "$prefix$startDay تا $lastDay $month ${first.year}"
                    val start = kit.jdn(first.copy(day = startDay))
                    val end = kit.jdn(first.copy(day = lastDay))
                    CorpusRow(id("fa-range", i), "fa", PERSIAN, reference, text, start, end)
                }
            row.copy(phrase = kit.persianDigits(row.phrase))
        }

    private fun enRanges(kit: CorpusKit): List<CorpusRow> =
        List(10) { i ->
            val reference = kit.reference()
            val first = kit.date(GREGORIAN, 1990..2040)
            val last = kit.calendar(GREGORIAN).fromJdn(kit.jdn(first) + kit.number(0..120))
            val firstMonth = kit.monthName(kit.en, GREGORIAN, first.month)
            val lastMonth = kit.monthName(kit.en, GREGORIAN, last.month)
            val text =
                if (i < 5) {
                    "from $firstMonth ${first.day}, ${first.year} to $lastMonth ${last.day}, ${last.year}"
                } else {
                    "between ${first.day} $firstMonth ${first.year} and ${last.day} $lastMonth ${last.year}"
                }
            CorpusRow(id("en-range", i), "en", GREGORIAN, reference, text, kit.jdn(first), kit.jdn(last))
        }
}
