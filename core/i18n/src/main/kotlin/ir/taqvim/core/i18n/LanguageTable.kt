/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import java.util.Properties

/**
 * The launch languages (T-200), loaded from the generated `languages.properties` resource. Native-script text lives
 * in that resource rather than in Kotlin sources; its CLDR-derived values are re-verified against ICU4J in tests.
 */
public object LanguageTable {
    private const val RESOURCE = "languages.properties"

    /** Every launch language in presentation order. */
    public val languages: List<LanguageSpec> by lazy { LanguageTableParser.parse(loadResource()) }

    /** The language with [code] (e.g. `fa`, `prs`, `kmr`), or `null`. */
    public fun forCode(code: String): LanguageSpec? = languages.firstOrNull { it.code == code }

    private fun loadResource(): Map<String, String> {
        val stream = checkNotNull(LanguageTable::class.java.getResourceAsStream(RESOURCE)) { "$RESOURCE is missing" }
        val properties = Properties()
        stream.reader(Charsets.UTF_8).use { properties.load(it) }
        return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
    }
}

/** Parses `key=value` entries of the language table: `languages=<codes>` and `<code>.<field>=<value>`. */
internal object LanguageTableParser {
    fun parse(entries: Map<String, String>): List<LanguageSpec> {
        val codes = requireNotNull(entries["languages"]) { "language table has no 'languages' entry" }.split(',')
        return codes.map { LanguageEntry(it, entries).toSpec() }
    }
}

private class LanguageEntry(
    private val code: String,
    private val entries: Map<String, String>,
) {
    fun toSpec(): LanguageSpec =
        LanguageSpec(
            code = code,
            localeTag = required("locale"),
            nativeName = required("nativeName"),
            script = required("script"),
            direction = enumOf(required("direction")),
            numerals = enumOf(required("numerals")),
            calendars = required("calendars").split(',').map { enumOf<CalendarSystem>(it) },
            weekStart = enumOf(required("weekStart")),
            weekend = required("weekend").split(',').map { enumOf<Weekday>(it) }.toSet(),
            prayerMethod = enumOf(required("prayerMethod")),
            asrJuristic = enumOf(required("asrJuristic")),
            monthNames =
                MonthNames(
                    gregorian = months("gregorian") ?: required("months.gregorian").split(MONTH_SEPARATOR),
                    persian = months("persian"),
                    islamic = months("islamic"),
                    nepali = months("nepali"),
                ),
            datePattern = DatePattern.fromCldr(required("datePattern")),
            dayPeriods = DayPeriodNames(required("am"), required("pm")),
            andPattern = entries["$code.andPattern"],
        )

    private fun required(field: String): String =
        requireNotNull(entries["$code.$field"]) { "language '$code' has no '$field'" }

    private fun months(calendar: String): List<String>? = entries["$code.months.$calendar"]?.split(MONTH_SEPARATOR)

    private inline fun <reified T : Enum<T>> enumOf(name: String): T =
        requireNotNull(enumValues<T>().firstOrNull { it.name == name }) {
            "language '$code': '$name' is not a ${T::class.simpleName}"
        }

    private companion object {
        const val MONTH_SEPARATOR = '|'
    }
}
