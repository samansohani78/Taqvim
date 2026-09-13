/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1400 duration grammar fuzzing: arbitrary text never crashes, and generated sums evaluate exactly. */
class DurationPropertiesTest {
    private val maxSeconds = DurationExpression.MAX_DAYS * DurationUnitWord.DAY.seconds
    private val alphabet = "0123456789.٫۰۱۲۳ +-*/()−×÷ dhmsw\tو ساعتروزدقیقه x:$".toList()
    private val unitWords =
        mapOf(
            DurationUnitWord.WEEK to listOf("w", "weeks", "هفته"),
            DurationUnitWord.DAY to listOf("d", "day", "روز"),
            DurationUnitWord.HOUR to listOf("h", "hours", "ساعت"),
            DurationUnitWord.MINUTE to listOf("m", "min", "دقیقه"),
            DurationUnitWord.SECOND to listOf("s", "secs", "ثانیه"),
        )

    private data class Term(
        val amount: Int,
        val unit: DurationUnitWord,
        val word: String,
        val negative: Boolean,
        val numerals: NumeralSystem,
    )

    private val terms =
        Arb.bind(
            Arb.int(0..999),
            Arb.element(DurationUnitWord.entries),
            Arb.int(0..2),
            Arb.boolean(),
            Arb.element(NumeralSystem.entries),
        ) { amount, unit, alias, negative, numerals ->
            Term(amount, unit, unitWords.getValue(unit)[alias], negative, numerals)
        }

    @Test
    fun `arbitrary text never throws and stays within the limit`(): Unit =
        runBlocking {
            val texts = Arb.list(Arb.element(alphabet), 0..60).map { it.joinToString("") }
            checkAll(PropertyTesting.iterations * 10, texts) {
                when (val result = DurationExpression.evaluate(it)) {
                    is DurationEvaluation.Value -> (abs(result.seconds) <= maxSeconds) shouldBe true
                    is DurationEvaluation.Invalid -> (result.error.position in 0..it.length) shouldBe true
                }
            }
        }

    @Test
    fun `generated sums of quantities evaluate to the sum of their parts`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.list(terms, 1..6)) { list ->
                val text =
                    list
                        .mapIndexed { index, term ->
                            val sign = if (term.negative) " - " else " + "
                            val number = Numerals.localizeDigits(term.amount.toString(), term.numerals)
                            (if (index == 0) "" else sign) + "$number ${term.word}"
                        }.joinToString("")
                val expected =
                    list.withIndex().sumOf { (index, term) ->
                        val value = term.amount * term.unit.seconds
                        if (index > 0 && term.negative) -value else value
                    }
                withClue(text) { DurationExpression.evaluate(text) shouldBe DurationEvaluation.Value(expected) }
            }
        }

    @Test
    fun `the presenter splits values and totals and keeps errors`() {
        val english = ToolsFixtures.language("en")
        val value = DurationPresenter.present("1d 2h 3m 4s", english).shouldBeInstanceOf<DurationState.Value>()
        value.isNegative shouldBe false
        listOf(value.days, value.hours, value.minutes, value.seconds) shouldBe listOf("1", "2", "3", "4")
        value.totalHours shouldBe "26.05"
        value.totalMinutes shouldBe "1563.07"
        (value.text?.contains("day") == true) shouldBe true
        DurationPresenter.present("-90m", english).shouldBeInstanceOf<DurationState.Value>().let {
            it.isNegative shouldBe true
            it.totalHours shouldBe "1.5"
        }
        val persian = ToolsFixtures.language("fa")
        DurationPresenter.present("2h", persian).shouldBeInstanceOf<DurationState.Value>().hours shouldBe "۲"
        DurationPresenter.present("0s", english).shouldBeInstanceOf<DurationState.Value>().totalMinutes shouldBe "0"
        DurationPresenter.present("  ", english) shouldBe DurationState.Empty
        DurationPresenter.present("5", english) shouldBe DurationState.Invalid(DurationError.MissingUnit(0))
        val kurdish = ToolsFixtures.language("ckb")
        DurationPresenter
            .present("1h", kurdish)
            .shouldBeInstanceOf<DurationState.Value>()
            .text
            .shouldBeNull()
    }
}
