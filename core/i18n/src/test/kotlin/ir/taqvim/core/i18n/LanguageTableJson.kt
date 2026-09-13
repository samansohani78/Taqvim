/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/** Stable, dependency-free JSON rendering of the language table for the T-200 snapshot. */
internal object LanguageTableJson {
    fun render(languages: List<LanguageSpec>): String =
        languages.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { spec -> "  " + objectOf(spec) }

    private fun objectOf(spec: LanguageSpec): String =
        listOf(
            "code" to string(spec.code),
            "localeTag" to string(spec.localeTag),
            "nativeName" to string(spec.nativeName),
            "script" to string(spec.script),
            "direction" to string(spec.direction.name),
            "numerals" to string(spec.numerals.name),
            "calendars" to array(spec.calendars.map { it.name }),
            "weekStart" to string(spec.weekStart.name),
            "weekend" to array(spec.weekend.map { it.name }),
            "prayerMethod" to string(spec.prayerMethod.name),
            "asrJuristic" to string(spec.asrJuristic.name),
            "datePattern" to
                "{\"order\": ${string(spec.datePattern.order.name)}, " +
                "\"separator\": ${string(spec.datePattern.separator.toString())}, " +
                "\"zeroPad\": ${spec.datePattern.zeroPad}}",
            "dayPeriods" to array(listOf(spec.dayPeriods.am, spec.dayPeriods.pm)),
            "andPattern" to (spec.andPattern?.let(::string) ?: "null"),
            "months.gregorian" to array(spec.monthNames.gregorian),
            "months.persian" to (spec.monthNames.persian?.let(::array) ?: "null"),
            "months.islamic" to (spec.monthNames.islamic?.let(::array) ?: "null"),
            "months.nepali" to (spec.monthNames.nepali?.let(::array) ?: "null"),
        ).joinToString(separator = ",\n    ", prefix = "{\n    ", postfix = "\n  }") { (key, value) ->
            "${string(key)}: $value"
        }

    private fun array(values: List<String>): String = values.joinToString(", ", "[", "]", transform = ::string)

    private fun string(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
