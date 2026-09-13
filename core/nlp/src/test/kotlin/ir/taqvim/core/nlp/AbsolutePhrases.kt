/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem.GREGORIAN
import ir.taqvim.core.model.CalendarSystem.ISLAMIC
import ir.taqvim.core.model.CalendarSystem.PERSIAN
import ir.taqvim.core.nlp.CorpusKit.Companion.id

/** Written dates of the synthetic corpus: 200 with month names and 100 numeric. */
internal object AbsolutePhrases {
    private val PERSIAN_YEARS = 1300..1450
    private val ISLAMIC_YEARS = 1350..1500
    private val GREGORIAN_YEARS = 1950..2100

    fun rows(kit: CorpusKit): List<CorpusRow> =
        faPersian(kit) + faIslamic(kit) + faGregorian(kit) + enGregorian(kit) + enOther(kit) + numeric(kit)

    private fun faPersian(kit: CorpusKit): List<CorpusRow> =
        List(60) { i ->
            val date = kit.date(PERSIAN, PERSIAN_YEARS)
            val month = kit.monthName(kit.fa, PERSIAN, date.month)
            val weekday = kit.weekdayName(kit.fa, kit.jdn(date).weekday())
            val text =
                listOf(
                    "${date.day} $month ${date.year}",
                    "$weekday ${date.day} $month ${date.year}",
                    "جلسه در تاریخ ${date.day} $month ${date.year} برگزار می‌شود",
                    "${date.year} $month ${date.day}",
                )[i % 4]
            CorpusRow(id("fa-persian", i), "fa", PERSIAN, kit.reference(), kit.persianDigits(text), kit.jdn(date))
        }

    private fun faIslamic(kit: CorpusKit): List<CorpusRow> =
        List(30) { i ->
            val date = kit.date(ISLAMIC, ISLAMIC_YEARS)
            val month = kit.monthName(kit.fa, ISLAMIC, date.month)
            val weekday = kit.weekdayName(kit.fa, kit.jdn(date).weekday())
            val text =
                listOf(
                    "${date.day} $month ${date.year} ه.ق",
                    "${date.day} $month ${date.year} قمری",
                    "$weekday ${date.day} $month ${date.year}",
                )[i % 3]
            CorpusRow(id("fa-islamic", i), "fa", PERSIAN, kit.reference(), kit.persianDigits(text), kit.jdn(date))
        }

    private fun faGregorian(kit: CorpusKit): List<CorpusRow> =
        List(30) { i ->
            val date = kit.date(GREGORIAN, GREGORIAN_YEARS)
            val month = kit.monthName(kit.fa, GREGORIAN, date.month)
            val weekday = kit.weekdayName(kit.fa, kit.jdn(date).weekday())
            val text =
                listOf(
                    "${date.day} $month ${date.year} میلادی",
                    "${date.day} $month ${date.year}",
                    "$weekday ${date.day} $month ${date.year}",
                )[i % 3]
            CorpusRow(id("fa-gregorian", i), "fa", PERSIAN, kit.reference(), kit.persianDigits(text), kit.jdn(date))
        }

    private fun enGregorian(kit: CorpusKit): List<CorpusRow> =
        List(60) { i ->
            val date = kit.date(GREGORIAN, GREGORIAN_YEARS)
            val month = kit.monthName(kit.en, GREGORIAN, date.month)
            val weekday = kit.weekdayName(kit.en, kit.jdn(date).weekday())
            val (d, y) = date.day to date.year
            val text =
                listOf(
                    "$month $d, $y",
                    "$weekday, $month $d, $y",
                    "The meeting is on $month $d, $y.",
                    "$d $month $y",
                    "$weekday $d $month $y",
                    "due by ${month.lowercase()} $d $y",
                )[i % 6]
            CorpusRow(id("en-gregorian", i), "en", GREGORIAN, kit.reference(), text, kit.jdn(date))
        }

    private fun enOther(kit: CorpusKit): List<CorpusRow> =
        List(20) { i ->
            val persian = i % 2 == 0
            val date = if (persian) kit.date(PERSIAN, PERSIAN_YEARS) else kit.date(ISLAMIC, ISLAMIC_YEARS)
            val month = kit.monthName(kit.en, date.system, date.month)
            val text = if (persian) "$month ${date.day}, ${date.year} AP" else "${date.day} $month ${date.year} AH"
            CorpusRow(id("en-lunisolar", i), "en", GREGORIAN, kit.reference(), text, kit.jdn(date))
        }

    private fun numeric(kit: CorpusKit): List<CorpusRow> {
        val persian =
            List(40) { i ->
                val date = kit.date(PERSIAN, PERSIAN_YEARS)
                val text = if (i % 2 == 0) padded(date, "/") else "${date.year}/${date.month}/${date.day}"
                CorpusRow(id("fa-numeric", i), "fa", PERSIAN, kit.reference(), kit.persianDigits(text), kit.jdn(date))
            }
        val iso =
            List(30) { i ->
                val date = kit.date(GREGORIAN, GREGORIAN_YEARS)
                CorpusRow(id("iso", i), "en", GREGORIAN, kit.reference(), date.toIsoLikeString(), kit.jdn(date))
            }
        val monthFirst =
            List(30) { i ->
                val date = kit.date(GREGORIAN, GREGORIAN_YEARS)
                val text = "${date.month}/${date.day}/${date.year}"
                CorpusRow(id("en-numeric", i), "en", GREGORIAN, kit.reference(), text, kit.jdn(date))
            }
        return persian + iso + monthFirst
    }

    private fun padded(
        date: CalendarDate,
        separator: String,
    ): String = listOf(date.year, date.month, date.day).joinToString(separator) { it.toString().padStart(2, '0') }
}
