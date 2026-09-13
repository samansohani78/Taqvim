/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import ir.taqvim.feature.tools.DurationError.DivisionByZero
import ir.taqvim.feature.tools.DurationError.MissingUnit
import ir.taqvim.feature.tools.DurationError.TooComplex
import ir.taqvim.feature.tools.DurationError.TooLarge
import ir.taqvim.feature.tools.DurationError.UnbalancedParenthesis
import ir.taqvim.feature.tools.DurationError.UnexpectedCharacter
import ir.taqvim.feature.tools.DurationError.UnexpectedToken
import org.junit.jupiter.api.Test

/** T-1400 duration grammar: a table of valid expressions with their values and invalid ones with their errors. */
class DurationExpressionTest {
    private val valid: List<Pair<String, Long>> =
        listOf(
            "1d" to 86_400,
            "2h" to 7_200,
            "30m" to 1_800,
            "45s" to 45,
            "1w" to 604_800,
            "1d 2h" to 93_600,
            "1d 2h + 30m" to 95_400,
            "1d2h30m" to 95_400,
            "2h - 30m" to 5_400,
            "30m - 2h" to -5_400,
            "-1h" to -3_600,
            "--1h" to 3_600,
            "- 1h + 2h" to 3_600,
            "1.5h" to 5_400,
            "0.5d" to 43_200,
            "1h * 3" to 10_800,
            "3 * 1h" to 10_800,
            "3 × 1h" to 10_800,
            "1h × 3" to 10_800,
            "(1h - 5m) * 3" to 9_900,
            "(1h − 5m) × 3" to 9_900,
            "1h / 2" to 1_800,
            "1h ÷ 4" to 900,
            "1d / 3" to 28_800,
            "10m / 3" to 200,
            "1m / 7" to 9,
            "1s / 2" to 1,
            "1s / 3" to 0,
            "2 * (1h + 30m)" to 10_800,
            "(2h)" to 7_200,
            "((2h))" to 7_200,
            "(1h + (2h - 30m)) / 2" to 4_500,
            "1 hour" to 3_600,
            "2 hours 15 minutes" to 8_100,
            "1 day and 2 hours" to 93_600,
            "3 days" to 259_200,
            "1 week 1 day" to 691_200,
            "10 min" to 600,
            "5 mins + 5 secs" to 305,
            "90 sec" to 90,
            "1 hr 1 min 1 sec" to 3_661,
            "2 wk" to 1_209_600,
            "1 Hour" to 3_600,
            "1H 1M" to 3_660,
            "۲ ساعت" to 7_200,
            "۲ ساعت و ۳۰ دقیقه" to 9_000,
            "۱ روز" to 86_400,
            "۳ هفته" to 1_814_400,
            "۱۰ ثانیه" to 10,
            "۱٫۵ ساعت" to 5_400,
            "٢ ساعت" to 7_200,
            "۲ساعت" to 7_200,
            "۵ دقيقه" to 300,
            "१ h" to 3_600,
            "1d + 1d + 1d" to 259_200,
            "1h - 1h" to 0,
            "0s" to 0,
            "0d 0h" to 0,
            "1h * 0" to 0,
            "1h * 1.5" to 5_400,
            "1h * 0.25" to 900,
            "100000d" to 8_640_000_000,
            "-100000d" to -8_640_000_000,
            "  1h  " to 3_600,
            "1h\t+\n30m" to 5_400,
            "2h+30m" to 9_000,
            "2h -30m" to 5_400,
            "1d - 1s" to 86_399,
            "3 * 2 * 1h" to 21_600,
            "1h * 2 / 4" to 1_800,
            "(30m + 30m) * 24" to 86_400,
            "- (1h + 1h)" to -7_200,
            "1h -(-1h)" to 7_200,
            "1.25 m" to 75,
            "0.001s" to 0,
            "1h 1h" to 7_200,
            "1h and" to 3_600,
            "1 day and" to 86_400,
        ).map { (text, seconds) -> text to seconds.toLong() }

    private val invalid: List<Pair<String, DurationError>> =
        listOf(
            "" to UnexpectedToken(0),
            "1" to MissingUnit(0),
            "h" to UnexpectedToken(0),
            "1x" to UnexpectedCharacter(1),
            "1h +" to UnexpectedToken(4),
            "+1h" to UnexpectedToken(0),
            "1h 2" to MissingUnit(3),
            "(1h" to UnbalancedParenthesis(0),
            "1h)" to UnbalancedParenthesis(2),
            "()" to UnexpectedToken(1),
            "1h / 0" to DivisionByZero(3),
            "1h / 0.0" to DivisionByZero(3),
            "1h * h" to UnexpectedToken(5),
            "1h * 2h" to UnexpectedToken(5),
            "2 / 1h" to MissingUnit(0),
            "1h $" to UnexpectedCharacter(3),
            "100001d" to TooLarge(6),
            "99999d + 2d" to TooLarge(7),
            "1d * 100001" to TooLarge(3),
            "1..5h" to UnexpectedCharacter(1),
            "1.h" to UnexpectedCharacter(1),
            "and" to UnexpectedToken(3),
            "(".repeat(20) + "1h" + ")".repeat(20) to TooComplex(17),
            "x".repeat(300) to TooComplex(DurationExpression.MAX_LENGTH),
            "1h * 2 * " to UnexpectedToken(9),
            "1h - - " to UnexpectedToken(7),
            "1h (2h)" to UnexpectedToken(3),
            "1:30" to UnexpectedCharacter(1),
        )

    @Test
    fun `the table has at least a hundred expressions`() {
        (valid + invalid) shouldHaveAtLeastSize 100
    }

    @Test
    fun `valid expressions evaluate to their values in seconds`() {
        valid.forEach { (text, seconds) ->
            withClue(text) { DurationExpression.evaluate(text) shouldBe DurationEvaluation.Value(seconds) }
        }
    }

    @Test
    fun `invalid expressions report the first problem and where it is`() {
        invalid.forEach { (text, error) ->
            withClue(text.take(40)) { DurationExpression.evaluate(text) shouldBe DurationEvaluation.Invalid(error) }
        }
    }
}
