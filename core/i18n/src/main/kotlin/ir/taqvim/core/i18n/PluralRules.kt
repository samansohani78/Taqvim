/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/** CLDR plural categories (Unicode TR35, Part 3, "Language Plural Rules"). */
public enum class PluralCategory {
    ZERO,
    ONE,
    TWO,
    FEW,
    MANY,
    OTHER,
}

/**
 * CLDR plural rules evaluated for integer counts. Each category except [PluralCategory.OTHER] has a condition such as
 * `v = 0 and i % 10 = 2..4 and i % 100 != 12..14`; a count belongs to the first category whose condition holds, and to
 * OTHER when none does. For an integer the operands are `n = i = |count|` and `v = w = f = t = c = e = 0`.
 */
public class PluralRules private constructor(
    private val conditions: List<Pair<PluralCategory, Condition>>,
) {
    /** Categories this language distinguishes for integers, always including [PluralCategory.OTHER]. */
    public val categories: Set<PluralCategory> = conditions.map { it.first }.toSet() + PluralCategory.OTHER

    /** The category of [count]. */
    public fun select(count: Long): PluralCategory {
        require(count != Long.MIN_VALUE) { "count is out of range" }
        val magnitude = Math.abs(count)
        return conditions.firstOrNull { (_, condition) -> condition.matches(magnitude) }?.first ?: PluralCategory.OTHER
    }

    public companion object {
        private val RELATION =
            Regex("""^([nivwftce])\s*(?:%\s*(\d+)\s*)?(=|!=)\s*(\d+(?:\.\.\d+)?(?:\s*,\s*\d+(?:\.\.\d+)?)*)$""")
        private const val INTEGER_OPERANDS = "ni"
        private const val OPERAND_GROUP = 1
        private const val MODULUS_GROUP = 2
        private const val OPERATOR_GROUP = 3
        private const val RANGES_GROUP = 4

        /** Rules from CLDR rule texts per category; OTHER may be absent or blank and is ignored. */
        public fun parse(rules: Map<PluralCategory, String>): PluralRules =
            PluralRules(
                rules
                    .filterKeys { it != PluralCategory.OTHER }
                    .filterValues { it.isNotBlank() }
                    .toSortedMap()
                    .map { (category, text) -> category to parseCondition(text) },
            )

        private fun parseCondition(text: String): Condition =
            Condition(text.split(" or ").map { alternative -> alternative.split(" and ").map(::parseRelation) })

        private fun parseRelation(text: String): Relation {
            val match = requireNotNull(RELATION.matchEntire(text.trim())) { "unsupported plural relation '$text'" }
            val groups = match.groupValues
            return Relation(
                usesCount = groups[OPERAND_GROUP].single() in INTEGER_OPERANDS,
                modulus = groups[MODULUS_GROUP].toLongOrNull(),
                negated = groups[OPERATOR_GROUP] == "!=",
                ranges = groups[RANGES_GROUP].split(',').map(::parseRange),
            )
        }

        private fun parseRange(text: String): LongRange {
            val bounds = text.trim().split("..").map(String::toLong)
            return bounds.first()..bounds.last()
        }
    }

    /** Alternatives joined by `or`, each a list of relations joined by `and`. */
    private class Condition(
        private val alternatives: List<List<Relation>>,
    ) {
        fun matches(count: Long): Boolean = alternatives.any { relations -> relations.all { it.matches(count) } }
    }

    /** `operand [% modulus] (= | !=) ranges`. */
    private class Relation(
        private val usesCount: Boolean,
        private val modulus: Long?,
        private val negated: Boolean,
        private val ranges: List<LongRange>,
    ) {
        fun matches(count: Long): Boolean {
            val operand = if (usesCount) count else 0L
            val value = modulus?.let { operand % it } ?: operand
            return ranges.any { value in it } != negated
        }
    }
}
