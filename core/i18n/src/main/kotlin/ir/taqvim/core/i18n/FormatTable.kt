/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import ir.taqvim.core.model.CalendarSystem

/** Units of relative phrases such as "3 days ago". */
public enum class RelativeUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

/** Units of formatted durations such as "2 hours and 5 minutes". */
public enum class DurationUnit {
    DAY,
    HOUR,
    MINUTE,
}

/** Named relative days/weeks/…: "yesterday", "today", "tomorrow" (or "last week", "this week", "next week"). */
public data class NamedRelative(
    public val previous: String,
    public val current: String,
    public val next: String,
)

/** Relative-time patterns with a `{0}` count placeholder per plural category, plus the named forms. */
public data class RelativePatterns(
    public val future: Map<RelativeUnit, Map<PluralCategory, String>>,
    public val past: Map<RelativeUnit, Map<PluralCategory, String>>,
    public val named: Map<RelativeUnit, NamedRelative>,
)

/** "And" list patterns for two (`{0}`, `{1}`) and three (`{0}`, `{1}`, `{2}`) items. */
public data class ListPatterns(
    public val two: String,
    public val three: String,
)

/**
 * CLDR formatting data of one launch language (T-202). Nullable or missing entries are data gaps listed in
 * docs/DATA_TODO.md; formatters degrade as documented instead of inventing text.
 *
 * @property weekdays wide weekday names in ISO order (Monday first) per calendar; CLDR names can differ by calendar.
 * @property datePatterns full date pattern per calendar: CLDR for Gregorian, Persian and Islamic, the documented
 *   Bikram Sambat pattern (or the Gregorian one) for Nepali.
 * @property eras abbreviated name of the current era per calendar.
 * @property monthNames format-context wide month names per calendar.
 * @property durationUnits unit patterns with `{0}` per plural category.
 */
public data class LanguageFormats(
    public val code: String,
    public val weekdays: Map<CalendarSystem, List<String>>,
    public val datePatterns: Map<CalendarSystem, String>,
    public val eras: Map<CalendarSystem, String>,
    public val monthNames: Map<CalendarSystem, List<String>>,
    public val pluralRules: PluralRules,
    public val relative: RelativePatterns?,
    public val durationUnits: Map<DurationUnit, Map<PluralCategory, String>>?,
    public val lists: ListPatterns?,
)

/** Formatting data for every launch language, loaded from the generated `formats.properties` resource. */
public object FormatTable {
    private const val RESOURCE = "formats.properties"

    /**
     * Product overrides of CLDR full date patterns (docs/adr/0014-persian-long-date-pattern.md). CLDR 48 gives `fa`
     * and `prs` the Persian-calendar pattern `y MMMM d, EEEE` (year first, Latin comma); Taqvim uses the language's own
     * CLDR Gregorian full pattern instead. Every other pattern is CLDR data as generated.
     */
    internal val PRODUCT_DATE_PATTERNS: Map<Pair<String, CalendarSystem>, String> =
        mapOf(
            ("fa" to CalendarSystem.PERSIAN) to "EEEE d MMMM y",
            ("prs" to CalendarSystem.PERSIAN) to "EEEE d MMMM y",
        )

    /** Era abbreviations CLDR has none for, by language code and calendar; cited in [SourcedEras]. */
    internal val PRODUCT_ERAS: Map<Pair<String, CalendarSystem>, String> get() = SourcedEras.all

    /** Formatting data by language code, for every language of [LanguageTable]. */
    public val formats: Map<String, LanguageFormats> by lazy {
        FormatTableParser
            .parse(loadPropertiesResource(RESOURCE), LanguageTable.languages.map { it.code })
            .mapValues { (code, formats) ->
                withHebrew(withBikramSambat(code, withProductEras(code, withProductPatterns(code, formats))))
            }
    }

    private fun withProductPatterns(
        code: String,
        formats: LanguageFormats,
    ): LanguageFormats =
        formats.copy(
            datePatterns =
                formats.datePatterns.mapValues { (calendar, cldr) ->
                    PRODUCT_DATE_PATTERNS[code to calendar] ?: cldr
                },
        )

    /** [formats] with the sourced era abbreviations of [SourcedEras]; CLDR leaves those pairs without one. */
    private fun withProductEras(
        code: String,
        formats: LanguageFormats,
    ): LanguageFormats {
        val own = SourcedEras.of(code)
        return if (own.isEmpty()) formats else formats.copy(eras = formats.eras + own)
    }

    /**
     * [formats] with Bikram Sambat names (T-105): CLDR has none, so month names, era and pattern come from
     * [BikramSambatNames]; languages without an official pattern write these dates with their Gregorian pattern.
     */
    private fun withBikramSambat(
        code: String,
        formats: LanguageFormats,
    ): LanguageFormats {
        val nepali = CalendarSystem.NEPALI
        val pattern = BikramSambatNames.pattern(code) ?: formats.datePatterns.getValue(CalendarSystem.GREGORIAN)
        val era = BikramSambatNames.era(code)
        return formats.copy(
            datePatterns = formats.datePatterns + (nepali to pattern),
            eras = if (era == null) formats.eras else formats.eras + (nepali to era),
            monthNames = formats.monthNames + (nepali to BikramSambatNames.months(code)),
        )
    }

    /**
     * [formats] with a Hebrew date pattern (F07): the language's Gregorian pattern. Month names depend on the year
     * (13 in a leap year), so they come from [HebrewMonthNames] rather than [LanguageFormats.monthNames].
     */
    private fun withHebrew(formats: LanguageFormats): LanguageFormats =
        formats.copy(
            datePatterns =
                formats.datePatterns +
                    (CalendarSystem.HEBREW to formats.datePatterns.getValue(CalendarSystem.GREGORIAN)),
        )

    /** Formatting data of [language]. */
    public fun of(language: LanguageSpec): LanguageFormats =
        requireNotNull(formats[language.code]) { "no formatting data for '${language.code}'" }
}

/** Parses `<code>.<key>=<value>` entries of the formatting table. */
internal object FormatTableParser {
    private const val WEEK_DAYS = 7
    private const val MONTHS = 12
    private val PATTERN_CALENDARS = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC)

    fun parse(
        entries: Map<String, String>,
        codes: List<String>,
    ): Map<String, LanguageFormats> = codes.associateWith { parseLanguage(it, entries) }

    private fun parseLanguage(
        code: String,
        entries: Map<String, String>,
    ): LanguageFormats {
        val own = entries.filterKeys { it.startsWith("$code.") }.mapKeys { it.key.removePrefix("$code.") }

        fun required(key: String) = requireNotNull(own[key]) { "formats of '$code' have no '$key'" }
        val weekdays =
            PATTERN_CALENDARS.associateWith { calendar ->
                required("weekdays.${calendar.key}").split('|').also { names ->
                    require(names.size == WEEK_DAYS) { "formats of '$code' need 7 ${calendar.key} weekdays" }
                }
            }
        return LanguageFormats(
            code = code,
            weekdays = weekdays,
            datePatterns = PATTERN_CALENDARS.associateWith { required("pattern.${it.key}") },
            eras =
                PATTERN_CALENDARS
                    .mapNotNull { calendar ->
                        own["era.${calendar.key}"]?.let { calendar to it }
                    }.toMap(),
            monthNames = PATTERN_CALENDARS.mapNotNull { calendar -> months(code, own, calendar) }.toMap(),
            pluralRules = PluralRules.parse(categoryMap(own, "plural.")),
            relative = relative(own),
            durationUnits = durationUnits(own),
            lists = own["list.2"]?.let { two -> ListPatterns(two, required("list.3")) },
        )
    }

    private fun months(
        code: String,
        own: Map<String, String>,
        calendar: CalendarSystem,
    ): Pair<CalendarSystem, List<String>>? =
        own["months.${calendar.key}"]?.split('|')?.let { names ->
            require(names.size == MONTHS) { "formats of '$code' need 12 ${calendar.key} months" }
            calendar to names
        }

    private fun relative(own: Map<String, String>): RelativePatterns? {
        if (RelativeUnit.entries.none { own.containsKey("relative.${it.key}.current") }) return null
        return RelativePatterns(
            future = RelativeUnit.entries.associateWith { categoryMap(own, "relative.${it.key}.future.") },
            past = RelativeUnit.entries.associateWith { categoryMap(own, "relative.${it.key}.past.") },
            named =
                RelativeUnit.entries.associateWith { unit ->
                    fun named(which: String) =
                        requireNotNull(own["relative.${unit.key}.$which"]) { "missing relative.${unit.key}.$which" }
                    NamedRelative(named("previous"), named("current"), named("next"))
                },
        )
    }

    private fun durationUnits(own: Map<String, String>): Map<DurationUnit, Map<PluralCategory, String>>? =
        DurationUnit.entries
            .associateWith { categoryMap(own, "unit.${it.key}.") }
            .takeIf { units -> units.values.all { it.isNotEmpty() } }

    private fun categoryMap(
        own: Map<String, String>,
        prefix: String,
    ): Map<PluralCategory, String> =
        own
            .filterKeys { it.startsWith(prefix) }
            .mapKeys { (key, _) ->
                val name = key.removePrefix(prefix)
                requireNotNull(PluralCategory.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }) {
                    "unknown plural category '$name' in '$key'"
                }
            }

    private val CalendarSystem.key: String get() = name.lowercase()

    private val RelativeUnit.key: String get() = name.lowercase()

    private val DurationUnit.key: String get() = name.lowercase()
}
