/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** A unit word of the duration grammar and its length in seconds. */
enum class DurationUnitWord(
    val seconds: Long,
) {
    WEEK(604_800),
    DAY(86_400),
    HOUR(3_600),
    MINUTE(60),
    SECOND(1),
}

/** Why an expression has no value; [position] is the index of the offending character (the text length at its end). */
sealed interface DurationError {
    val position: Int

    /** A character or word that is not part of the grammar. */
    data class UnexpectedCharacter(
        override val position: Int,
    ) : DurationError

    /** A value or operator is missing or out of place. */
    data class UnexpectedToken(
        override val position: Int,
    ) : DurationError

    /** A number that should be followed by a unit. */
    data class MissingUnit(
        override val position: Int,
    ) : DurationError

    /** A parenthesis without its partner. */
    data class UnbalancedParenthesis(
        override val position: Int,
    ) : DurationError

    data class DivisionByZero(
        override val position: Int,
    ) : DurationError

    /** The value leaves ±[DurationExpression.MAX_DAYS] days. */
    data class TooLarge(
        override val position: Int,
    ) : DurationError

    /** The text is longer than [DurationExpression.MAX_LENGTH] or nested too deeply. */
    data class TooComplex(
        override val position: Int,
    ) : DurationError
}

/** The result of [DurationExpression.evaluate]. */
sealed interface DurationEvaluation {
    /** The value rounded to whole seconds. */
    data class Value(
        val seconds: Long,
    ) : DurationEvaluation

    data class Invalid(
        val error: DurationError,
    ) : DurationEvaluation
}

/**
 * Duration expressions (T-1400), e.g. `1d 2h + 30m`, `(1h - 5m) * 3` or `۲ ساعت و ۳۰ دقیقه`:
 *
 * ```
 * sum      ← product (("+" | "-") product)*
 * product  ← signed (("*" | "/") NUMBER)*
 * signed   ← "-" signed | NUMBER "*" signed | primary
 * primary  ← "(" sum ")" | (NUMBER UNIT)+
 * ```
 *
 * Numbers use the digits of any supported script with `.` or `٫` as decimal separator; `−`, `×` and `÷` are accepted
 * for `-`, `*` and `/`; the words "and"/"و" between quantities are ignored. Units: w/week(s)/هفته, d/day(s)/روز,
 * h/hr(s)/hour(s)/ساعت, m/min(s)/minute(s)/دقیقه, s/sec(s)/second(s)/ثانیه.
 */
object DurationExpression {
    const val MAX_LENGTH: Int = 256
    const val MAX_DAYS: Long = 100_000
    internal const val MAX_DEPTH: Int = 16
    internal val MAX_SECONDS: BigDecimal = BigDecimal.valueOf(MAX_DAYS * DurationUnitWord.DAY.seconds)

    fun evaluate(text: String): DurationEvaluation =
        if (text.length > MAX_LENGTH) {
            DurationEvaluation.Invalid(DurationError.TooComplex(MAX_LENGTH))
        } else {
            DurationTokenizer.tokenize(text).fold(
                onTokens = { DurationParser(it, text.length).evaluate() },
                onError = { DurationEvaluation.Invalid(it) },
            )
        }
}

/** Recursive-descent evaluation of [tokens]; the first error found wins. */
private class DurationParser(
    private val tokens: List<DurationToken>,
    private val end: Int,
) {
    private var index = 0
    private var error: DurationError? = null

    fun evaluate(): DurationEvaluation {
        val value = sum(depth = 0)
        val leftover = tokens.getOrNull(index)
        if (value != null && leftover != null) {
            fail<BigDecimal>(
                if (leftover.isSymbol(CLOSE)) {
                    DurationError.UnbalancedParenthesis(leftover.position)
                } else {
                    DurationError.UnexpectedToken(leftover.position)
                },
            )
        }
        return error?.let { DurationEvaluation.Invalid(it) }
            ?: DurationEvaluation.Value(requireNotNull(value).setScale(0, RoundingMode.HALF_UP).longValueExact())
    }

    private fun sum(depth: Int): BigDecimal? {
        var total = product(depth) ?: return null
        while (true) {
            val operator = take(ADDITIVE) ?: return total
            val right = product(depth)
            total = right?.let { checked(if (operator.char == PLUS) total + it else total - it, operator) }
                ?: return null
        }
    }

    private fun product(depth: Int): BigDecimal? {
        var value = signed(depth) ?: return null
        while (true) {
            val operator = take(MULTIPLICATIVE) ?: return value
            value = plainNumber()?.let { scale(value, operator, it) } ?: return null
        }
    }

    private fun signed(depth: Int): BigDecimal? {
        if (depth > DurationExpression.MAX_DEPTH) return fail(DurationError.TooComplex(position()))
        val minus = take(MINUS.toString())
        val scalar = if (minus == null) scalarPrefix() else null
        return when {
            minus != null -> signed(depth + 1)?.negate()
            scalar != null -> signed(depth + 1)?.let { checked(it * scalar.value, scalar) }
            take(OPEN.toString()) != null -> group(depth)
            else -> quantities()
        }
    }

    private fun group(depth: Int): BigDecimal? {
        val openedAt = tokens[index - 1].position
        val inner = sum(depth + 1) ?: return null
        return if (take(CLOSE.toString()) != null) inner else fail(DurationError.UnbalancedParenthesis(openedAt))
    }

    private fun quantities(): BigDecimal? {
        var total: BigDecimal? = null
        while (true) {
            val number =
                tokens.getOrNull(index) as? DurationToken.Number
                    ?: return total ?: fail(DurationError.UnexpectedToken(position()))
            val unit = tokens.getOrNull(index + 1) as? DurationToken.UnitWord
            index += if (unit == null) 1 else 2
            total =
                unit?.let { checked((total ?: BigDecimal.ZERO) + number.value * it.unit.seconds.toBigDecimal(), it) }
                    ?: return fail(if (unit == null) DurationError.MissingUnit(number.position) else null)
        }
    }

    private fun scalarPrefix(): DurationToken.Number? {
        val number = tokens.getOrNull(index) as? DurationToken.Number
        val isScalar = number != null && tokens.getOrNull(index + 1)?.isSymbol(TIMES) == true
        if (isScalar) index += 2
        return number.takeIf { isScalar }
    }

    private fun plainNumber(): BigDecimal? {
        val number =
            (tokens.getOrNull(index) as? DurationToken.Number)?.takeIf {
                tokens.getOrNull(index + 1) !is DurationToken.UnitWord
            }
        if (number != null) index++
        return number?.value ?: fail(DurationError.UnexpectedToken(position()))
    }

    private fun scale(
        value: BigDecimal,
        operator: DurationToken.Symbol,
        factor: BigDecimal,
    ): BigDecimal? =
        when {
            operator.char == TIMES -> checked(value * factor, operator)
            factor.signum() == 0 -> fail(DurationError.DivisionByZero(operator.position))
            else -> checked(value.divide(factor, MathContext.DECIMAL64), operator)
        }

    private fun checked(
        value: BigDecimal,
        at: DurationToken,
    ): BigDecimal? =
        if (value.abs() >
            DurationExpression.MAX_SECONDS
        ) {
            fail(DurationError.TooLarge(at.position))
        } else {
            value
        }

    private fun take(symbols: String): DurationToken.Symbol? =
        (tokens.getOrNull(index) as? DurationToken.Symbol)?.takeIf { it.char in symbols }?.also { index++ }

    private fun position(): Int = tokens.getOrNull(index)?.position ?: end

    /** Records [problem] unless an earlier error was recorded (a `null` problem keeps the earlier one). */
    private fun <T> fail(problem: DurationError?): T? {
        if (error == null) error = problem
        return null
    }

    private fun DurationToken.isSymbol(char: Char): Boolean = this is DurationToken.Symbol && this.char == char

    private companion object {
        const val PLUS = '+'
        const val MINUS = '-'
        const val TIMES = '*'
        const val OPEN = '('
        const val CLOSE = ')'
        const val ADDITIVE = "+-"
        const val MULTIPLICATIVE = "*/"
    }
}
