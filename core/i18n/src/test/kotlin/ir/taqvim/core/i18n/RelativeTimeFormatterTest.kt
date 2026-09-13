/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.ListFormatter
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class RelativeTimeFormatterTest {
    private fun language(code: String) = requireNotNull(LanguageTable.forCode(code))

    @Test
    fun `offsets use named days and numeric phrases`() {
        val english = language("en")

        RelativeTimeFormatter.formatOffset(-1, RelativeUnit.DAY, english) shouldBe "yesterday"
        RelativeTimeFormatter.formatOffset(0, RelativeUnit.DAY, english) shouldBe "today"
        RelativeTimeFormatter.formatOffset(1, RelativeUnit.DAY, english) shouldBe "tomorrow"
        RelativeTimeFormatter.formatOffset(-3, RelativeUnit.DAY, english) shouldBe "3 days ago"
        RelativeTimeFormatter.formatOffset(5, RelativeUnit.WEEK, english) shouldBe "in 5 weeks"
        RelativeTimeFormatter.format(1, RelativeDirection.PAST, RelativeUnit.YEAR, english) shouldBe "1 year ago"
    }

    @Test
    fun `languages without relative data return null and bad counts are rejected`() {
        val kurdish = language("ckb")

        RelativeTimeFormatter.format(3, RelativeDirection.PAST, RelativeUnit.DAY, kurdish).shouldBeNull()
        RelativeTimeFormatter.formatOffset(-3, RelativeUnit.DAY, kurdish).shouldBeNull()
        RelativeTimeFormatter.formatOffset(0, RelativeUnit.DAY, kurdish).shouldBeNull()
        shouldThrow<IllegalArgumentException> {
            RelativeTimeFormatter.format(-1, RelativeDirection.PAST, RelativeUnit.DAY, language("en"))
        }
        shouldThrow<IllegalArgumentException> {
            RelativeTimeFormatter.formatOffset(Long.MIN_VALUE, RelativeUnit.DAY, language("en"))
        }
    }

    @TestFactory
    fun `relative phrases for 0 to 1000 match ICU4J`(): List<DynamicTest> =
        LanguageTable.languages.filter { FormatTable.of(it).relative != null }.map { language ->
            DynamicTest.dynamicTest(language.code) {
                val icu =
                    RelativeDateTimeFormatter.getInstance(
                        language.icuLocale,
                        null,
                        RelativeDateTimeFormatter.Style.LONG,
                        DisplayContext.CAPITALIZATION_NONE,
                    )
                val mismatches =
                    UNITS.flatMap { (unit, icuUnit, absoluteUnit) ->
                        namedMismatches(language, icu, unit, absoluteUnit) +
                            (0L..MAX_COUNT).flatMap { count ->
                                DIRECTIONS.mapNotNull { (direction, icuDirection) ->
                                    val expected =
                                        normalizeForOracle(icu.format(count.toDouble(), icuDirection, icuUnit))
                                    val actual =
                                        RelativeTimeFormatter
                                            .format(
                                                count,
                                                direction,
                                                unit,
                                                language,
                                            )?.let(::normalizeForOracle)
                                    "$unit $direction $count: '$expected' vs '$actual'".takeIf { expected != actual }
                                }
                            }
                    }
                withClue(mismatches.take(5).joinToString("\n")) { mismatches.shouldBeEmpty() }
            }
        }

    private fun namedMismatches(
        language: LanguageSpec,
        icu: RelativeDateTimeFormatter,
        unit: RelativeUnit,
        absoluteUnit: RelativeDateTimeFormatter.AbsoluteUnit,
    ): List<String> =
        listOf(
            -1L to RelativeDateTimeFormatter.Direction.LAST,
            0L to RelativeDateTimeFormatter.Direction.THIS,
            1L to RelativeDateTimeFormatter.Direction.NEXT,
        ).mapNotNull { (offset, direction) ->
            val expected = icu.format(direction, absoluteUnit)
            val actual = RelativeTimeFormatter.formatOffset(offset, unit, language)
            "$unit named $offset: '$expected' vs '$actual'".takeIf { expected != actual }
        }

    @Test
    fun `durations join unit phrases with the language's and pattern`() {
        val english = language("en")

        DurationFormatter.format(mapOf(DurationUnit.HOUR to 2L, DurationUnit.MINUTE to 5L), english) shouldBe
            "2 hours and 5 minutes"
        DurationFormatter.format(2.days + 3.hours + 1.minutes + 30.seconds, english) shouldBe
            "2 days, 3 hours, and 1 minute"
        DurationFormatter.format(0.seconds, english) shouldBe "0 minutes"
        DurationFormatter.format(21.days, english) shouldBe "21 days"
        DurationFormatter.format(3.hours, language("ckb")).shouldBeNull()
        DurationFormatter.format(3.hours + 2.minutes, language("tg")).shouldBeNull()
        shouldThrow<IllegalArgumentException> { DurationFormatter.format(mapOf(DurationUnit.DAY to -1L), english) }
        shouldThrow<IllegalArgumentException> { DurationFormatter.format((-1).minutes, english) }
    }

    @TestFactory
    fun `durations match ICU4J unit and list formatting`(): List<DynamicTest> =
        LanguageTable.languages
            .filter { FormatTable.of(it).durationUnits != null && FormatTable.of(it).lists != null }
            .map { language ->
                DynamicTest.dynamicTest(language.code) {
                    val units = MeasureFormat.getInstance(language.icuLocale, MeasureFormat.FormatWidth.WIDE)
                    val lists =
                        ListFormatter.getInstance(
                            language.icuLocale,
                            ListFormatter.Type.AND,
                            ListFormatter.Width.WIDE,
                        )
                    val mismatches =
                        DURATIONS.mapNotNull { (days, hours, minutes) ->
                            val parts =
                                listOf(
                                    days to MeasureUnit.DAY,
                                    hours to MeasureUnit.HOUR,
                                    minutes to MeasureUnit.MINUTE,
                                ).filter { it.first > 0 }
                                    .ifEmpty { listOf(0L to MeasureUnit.MINUTE) }
                            val expected =
                                normalizeForOracle(
                                    lists.format(parts.map { units.format(Measure(it.first, it.second)) }),
                                )
                            val actual =
                                DurationFormatter
                                    .format(
                                        mapOf(
                                            DurationUnit.DAY to days,
                                            DurationUnit.HOUR to hours,
                                            DurationUnit.MINUTE to minutes,
                                        ),
                                        language,
                                    )?.let(::normalizeForOracle)
                            "$days d $hours h $minutes m: '$expected' vs '$actual'".takeIf { expected != actual }
                        }
                    withClue(mismatches.take(5).joinToString("\n")) { mismatches.shouldBeEmpty() }
                }
            }

    private companion object {
        const val MAX_COUNT = 1_000L
        val UNITS =
            listOf(
                Triple(
                    RelativeUnit.DAY,
                    RelativeDateTimeFormatter.RelativeUnit.DAYS,
                    RelativeDateTimeFormatter.AbsoluteUnit.DAY,
                ),
                Triple(
                    RelativeUnit.WEEK,
                    RelativeDateTimeFormatter.RelativeUnit.WEEKS,
                    RelativeDateTimeFormatter.AbsoluteUnit.WEEK,
                ),
                Triple(
                    RelativeUnit.MONTH,
                    RelativeDateTimeFormatter.RelativeUnit.MONTHS,
                    RelativeDateTimeFormatter.AbsoluteUnit.MONTH,
                ),
                Triple(
                    RelativeUnit.YEAR,
                    RelativeDateTimeFormatter.RelativeUnit.YEARS,
                    RelativeDateTimeFormatter.AbsoluteUnit.YEAR,
                ),
            )
        val DIRECTIONS =
            listOf(
                RelativeDirection.PAST to RelativeDateTimeFormatter.Direction.LAST,
                RelativeDirection.FUTURE to RelativeDateTimeFormatter.Direction.NEXT,
            )
        val DURATIONS =
            listOf(
                Triple(0L, 0L, 0L),
                Triple(0L, 0L, 1L),
                Triple(0L, 2L, 5L),
                Triple(1L, 0L, 0L),
                Triple(2L, 1L, 0L),
                Triple(3L, 1L, 1L),
                Triple(0L, 11L, 59L),
                Triple(21L, 23L, 2L),
                Triple(101L, 0L, 3L),
                Triple(1000L, 12L, 45L),
            )
    }
}
