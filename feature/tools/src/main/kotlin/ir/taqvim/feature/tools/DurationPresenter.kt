/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/** The duration calculator's state for an expression (T-1400). */
internal object DurationPresenter {
    private const val TOTAL_DECIMALS = 2

    fun present(
        input: String,
        language: LanguageSpec,
    ): DurationState =
        if (input.isBlank()) {
            DurationState.Empty
        } else {
            when (val result = DurationExpression.evaluate(input)) {
                is DurationEvaluation.Invalid -> DurationState.Invalid(result.error)
                is DurationEvaluation.Value -> value(result.seconds, language)
            }
        }

    private fun value(
        seconds: Long,
        language: LanguageSpec,
    ): DurationState.Value {
        val magnitude = abs(seconds)
        val numerals = language.numerals
        val day = DurationUnitWord.DAY.seconds
        val hour = DurationUnitWord.HOUR.seconds
        val minute = DurationUnitWord.MINUTE.seconds

        fun number(value: Long) = Numerals.format(value, numerals)

        fun total(unit: Long) =
            Numerals.format(
                BigDecimal
                    .valueOf(magnitude)
                    .divide(BigDecimal.valueOf(unit), TOTAL_DECIMALS, RoundingMode.HALF_UP)
                    .stripTrailingZeros(),
                numerals,
            )
        return DurationState.Value(
            isNegative = seconds < 0,
            text = DurationFormatter.format(magnitude.seconds, language),
            days = number(magnitude / day),
            hours = number(magnitude % day / hour),
            minutes = number(magnitude % hour / minute),
            seconds = number(magnitude % minute),
            totalHours = total(hour),
            totalMinutes = total(minute),
        )
    }
}
