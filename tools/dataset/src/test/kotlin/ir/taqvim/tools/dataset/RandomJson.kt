/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import kotlin.random.Random

/** Random JSON-ish text for fuzzing: well-formed trees biased towards dataset keys, plus text-level mutations. */
internal class RandomJson(
    private val random: Random,
) {
    fun document(): String = value(depth = 0)

    /** [text] with a few random characters replaced, inserted or deleted (often no longer valid JSON). */
    fun mutate(text: String): String =
        (1..random.nextInt(1, MAX_MUTATIONS)).fold(text) { current, _ ->
            val at = random.nextInt(0, current.length.coerceAtLeast(1))
            when (random.nextInt(3)) {
                0 -> current.take(at) + TOKENS.random(random) + current.drop(at + 1)
                1 -> current.take(at) + TOKENS.random(random) + current.drop(at)
                else -> current.take(at) + current.drop(at + 1)
            }
        }

    private fun value(depth: Int): String =
        when (if (depth >= MAX_DEPTH) random.nextInt(4) else random.nextInt(6)) {
            0 -> {
                random.nextInt(-5, 400).toString()
            }

            1 -> {
                "\"${STRINGS.random(random)}\""
            }

            2 -> {
                listOf("true", "false", "null").random(random)
            }

            3 -> {
                random.nextDouble(-1e6, 1e6).toString()
            }

            4 -> {
                (0 until random.nextInt(0, MAX_WIDTH)).joinToString(",", "[", "]") { value(depth + 1) }
            }

            else -> {
                (0 until random.nextInt(0, MAX_WIDTH)).joinToString(",", "{", "}") {
                    "\"${KEYS.random(random)}\":${value(depth + 1)}"
                }
            }
        }

    private companion object {
        const val MAX_DEPTH = 5
        const val MAX_WIDTH = 5
        const val MAX_MUTATIONS = 6
        val KEYS =
            listOf(
                "schemaVersion",
                "events",
                "id",
                "calendar",
                "source",
                "category",
                "isHoliday",
                "title",
                "fa",
                "en",
                "rule",
                "type",
                "month",
                "day",
                "year",
                "n",
                "weekday",
                "eventId",
                "offsetDays",
                "kind",
                "timeZone",
                "validity",
                "fromYear",
                "toYear",
                "citation",
                "citations",
                "url",
                "page",
                "flags",
                "aliases",
                "links",
                "updated",
                "reviewedBy",
                "",
            )
        val STRINGS =
            listOf(
                "PERSIAN",
                "ISLAMIC",
                "GREGORIAN",
                "NEPALI",
                "Fixed",
                "RelativeToEvent",
                "Single",
                "NthDayOfYear",
                "test.base",
                "https://example.org",
                "2026-09-13",
                "FRIDAY",
                "IRAN_OFFICIAL",
                "\\u0000",
                "",
                "\\\"",
            )
        val TOKENS = listOf("{", "}", "[", "]", ",", ":", "\"", "0", "-1", "null", "\\", "x")
    }
}
