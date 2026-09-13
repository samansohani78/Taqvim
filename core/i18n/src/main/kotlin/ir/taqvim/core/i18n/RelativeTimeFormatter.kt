/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import kotlin.time.Duration

/** Whether a relative phrase points backwards ("3 days ago") or forwards ("in 3 days"). */
public enum class RelativeDirection {
    PAST,
    FUTURE,
}

/**
 * Relative phrases (T-202) from CLDR relative-time patterns selected by CLDR plural rules. Every function returns
 * `null` when the language has no verified relative-time data (docs/DATA_TODO.md).
 */
public object RelativeTimeFormatter {
    /** "in [count] days" or "[count] days ago"; [count] must not be negative. */
    public fun format(
        count: Long,
        direction: RelativeDirection,
        unit: RelativeUnit,
        language: LanguageSpec,
        numerals: NumeralSystem = language.numerals,
    ): String? {
        require(count >= 0) { "count must not be negative (was $count)" }
        val formats = FormatTable.of(language)
        val relative = formats.relative ?: return null
        val byCategory = if (direction == RelativeDirection.PAST) relative.past[unit] else relative.future[unit]
        val category = formats.pluralRules.select(count)
        val pattern = byCategory?.get(category) ?: byCategory?.get(PluralCategory.OTHER) ?: return null
        return pattern.replace(PLACEHOLDER, Numerals.format(count, numerals))
    }

    /** "yesterday", "today" or "tomorrow" (or last/this/next [unit]) for [offset] −1, 0, 1; otherwise [format]. */
    public fun formatOffset(
        offset: Long,
        unit: RelativeUnit,
        language: LanguageSpec,
        numerals: NumeralSystem = language.numerals,
    ): String? {
        require(offset != Long.MIN_VALUE) { "offset is out of range" }
        val named =
            FormatTable
                .of(language)
                .relative
                ?.named
                ?.get(unit)
        return when {
            named != null && offset == -1L -> named.previous
            named != null && offset == 0L -> named.current
            named != null && offset == 1L -> named.next
            offset < 0 -> format(-offset, RelativeDirection.PAST, unit, language, numerals)
            else -> format(offset, RelativeDirection.FUTURE, unit, language, numerals)
        }
    }

    private const val PLACEHOLDER = "{0}"
}

/**
 * Durations such as "2 hours and 5 minutes" (T-202): CLDR unit patterns for days, hours and minutes joined with the
 * language's "and" list pattern. Zero parts are left out; a zero duration is "0 minutes". Returns `null` when the
 * language lacks unit or list data.
 */
public object DurationFormatter {
    private const val PLACEHOLDER = "{0}"

    /** [parts] (non-negative amounts per unit) in day, hour, minute order. */
    public fun format(
        parts: Map<DurationUnit, Long>,
        language: LanguageSpec,
        numerals: NumeralSystem = language.numerals,
    ): String? {
        require(parts.values.all { it >= 0 }) { "duration parts must not be negative: $parts" }
        val formats = FormatTable.of(language)
        val units = formats.durationUnits ?: return null
        val present = DurationUnit.entries.filter { (parts[it] ?: 0L) > 0 }.ifEmpty { listOf(DurationUnit.MINUTE) }
        val texts =
            present.map { unit ->
                val count = parts[unit] ?: 0L
                val byCategory = units.getValue(unit)
                val pattern = byCategory[formats.pluralRules.select(count)] ?: byCategory[PluralCategory.OTHER]
                pattern?.replace(PLACEHOLDER, Numerals.format(count, numerals)) ?: return null
            }
        return join(texts, formats.lists)
    }

    /** [duration] rounded down to whole minutes, split into days, hours and minutes. */
    public fun format(
        duration: Duration,
        language: LanguageSpec,
        numerals: NumeralSystem = language.numerals,
    ): String? {
        require(!duration.isNegative()) { "duration must not be negative (was $duration)" }
        return duration.toComponents { days, hours, minutes, _, _ ->
            format(
                mapOf(
                    DurationUnit.DAY to days,
                    DurationUnit.HOUR to hours.toLong(),
                    DurationUnit.MINUTE to minutes.toLong(),
                ),
                language,
                numerals,
            )
        }
    }

    private fun join(
        texts: List<String>,
        lists: ListPatterns?,
    ): String? =
        when (texts.size) {
            1 -> {
                texts.single()
            }

            2 -> {
                lists?.two?.replace("{0}", texts[0])?.replace("{1}", texts[1])
            }

            else -> {
                lists
                    ?.three
                    ?.replace("{0}", texts[0])
                    ?.replace("{1}", texts[1])
                    ?.replace("{2}", texts[2])
            }
        }
}
