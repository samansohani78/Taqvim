/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

class FormatTableTest {
    private val calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC)

    @Test
    fun `every launch language has weekdays, patterns and plural rules`() {
        FormatTable.formats.keys shouldBe LanguageTable.languages.map { it.code }.toSet()
        FormatTable.formats.values.forEach { formats ->
            formats.weekdays.keys shouldBe calendars.toSet()
            formats.weekdays.values.forEach { it shouldHaveSize 7 }
            formats.datePatterns.keys shouldBe calendars.toSet() + CalendarSystem.NEPALI + CalendarSystem.HEBREW
            formats.monthNames[CalendarSystem.GREGORIAN].shouldNotBeNull()
        }
    }

    @Test
    fun `data gaps are exactly the ones listed in DATA_TODO`() {
        val gaps =
            FormatTable.formats.values.flatMap { formats ->
                calendars.flatMap { calendar ->
                    val name = calendar.name.lowercase()
                    listOfNotNull(
                        "${formats.code}:era.$name".takeIf { calendar !in formats.eras },
                        "${formats.code}:months.$name".takeIf { calendar !in formats.monthNames },
                    )
                } +
                    listOfNotNull(
                        "${formats.code}:relative".takeIf { formats.relative == null },
                        "${formats.code}:units".takeIf { formats.durationUnits == null },
                        "${formats.code}:list".takeIf { formats.lists == null },
                    )
            }

        gaps.toSet() shouldBe EXPECTED_GAPS
    }

    @Test
    fun `a minimal entry parses with every optional part absent`() {
        val formats = FormatTableParser.parse(minimal(), listOf("xx")).getValue("xx")

        formats.relative.shouldBeNull()
        formats.durationUnits.shouldBeNull()
        formats.lists.shouldBeNull()
        formats.eras shouldBe emptyMap()
        formats.monthNames shouldBe emptyMap()
        FormatTableParser
            .parse(minimal() + unitsFor("day"), listOf("xx"))
            .getValue("xx")
            .durationUnits
            .shouldBeNull()
    }

    @Test
    fun `incomplete or inconsistent entries are rejected`() {
        listOf(
            minimal() - "xx.pattern.persian",
            minimal() + ("xx.weekdays.persian" to "a|b|c"),
            minimal() + ("xx.months.persian" to "a|b"),
            minimal() + ("xx.plural.dual" to "n = 2"),
            minimal() + ("xx.relative.day.current" to "today"),
            minimal() + ("xx.list.2" to "{0} & {1}"),
        ).forEach { entries ->
            shouldThrow<IllegalArgumentException> { FormatTableParser.parse(entries, listOf("xx")) }
        }
    }

    private fun minimal(): Map<String, String> =
        mapOf(
            "xx.weekdays.gregorian" to "1|2|3|4|5|6|7",
            "xx.weekdays.persian" to "1|2|3|4|5|6|7",
            "xx.weekdays.islamic" to "1|2|3|4|5|6|7",
            "xx.pattern.gregorian" to "y-M-d",
            "xx.pattern.persian" to "y-M-d",
            "xx.pattern.islamic" to "y-M-d",
        )

    private fun unitsFor(unit: String): Map<String, String> = mapOf("xx.unit.$unit.other" to "{0} $unit")

    private companion object {
        val EXPECTED_GAPS =
            setOf(
                "ps:era.persian",
                "ps:era.islamic",
                "ckb:era.persian",
                "ckb:months.persian",
                "ckb:era.islamic",
                "ckb:months.islamic",
                "ckb:relative",
                "ckb:units",
                "ckb:list",
                "kmr:era.persian",
                "kmr:months.persian",
                "az:era.persian",
                "az:era.islamic",
                "tr:era.persian",
                "ur:era.persian",
                "ne:era.persian",
                "ne:months.persian",
                "ne:era.islamic",
                "ne:months.islamic",
                "hi:era.persian",
                "hi:era.islamic",
                "ta:era.persian",
                "ta:era.islamic",
                "bn:era.persian",
                "tg:era.persian",
                "tg:units",
                "ru:era.islamic",
                "de:era.persian",
                "de:era.islamic",
                "fr:era.islamic",
                "es:era.persian",
                "es:era.islamic",
                "id:era.persian",
                "id:months.persian",
                "ms:era.persian",
                "ms:months.persian",
                "zh:months.persian",
                "zh:months.islamic",
                "ja:era.persian",
                "ja:era.islamic",
            )
    }
}
