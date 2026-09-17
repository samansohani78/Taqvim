/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/**
 * Hebrew calendar month names (F07, T-108) from Unicode CLDR 48, generated into `hebrew-months.properties` by
 * `tools/i18n/HebrewMonthsGen.java`. Names follow the Hebrew calendar's numbering from Tishri: a common year has 12
 * months with a single Adar, a leap year 13 with Adar I and Adar II in sixth and seventh place.
 *
 * Languages whose CLDR data is only the root (English) fallback, or only generic numbered months ([omittedLanguages]),
 * get machine-translated names from `hebrew-months-mt.properties` (DT-037, marked "MT: needs review"; see
 * [isMachineTranslated]). A language with neither has no names, and callers show month numbers.
 */
public object HebrewMonthNames {
    private const val RESOURCE = "hebrew-months.properties"
    private const val MT_RESOURCE = "hebrew-months-mt.properties"
    private const val CLDR_MONTHS = 14
    private const val LEAP_FACTOR = 7L
    private const val CYCLE = 19L
    private const val LEAP_YEARS = 7L

    // CLDR order: Tishri, Heshvan, Kislev, Tevet, Shevat, Adar I, Adar, Nisan … Elul, Adar II.
    private val commonYear = listOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12)
    private val leapYear = listOf(0, 1, 2, 3, 4, 5, 13, 7, 8, 9, 10, 11, 12)

    private val entries: Map<String, String> by lazy { loadPropertiesResource(RESOURCE) }
    private val machineTranslated: Map<String, String> by lazy { loadPropertiesResource(MT_RESOURCE) }

    /** Languages CLDR offers no real Hebrew month names for; their names are machine-translated. */
    public val omittedLanguages: Set<String> by lazy {
        entries["omitted"]
            .orEmpty()
            .split(',')
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /**
     * The month names of a common (12 names) or [leap] (13 names) Hebrew year in language [code], in the calendar's
     * order from Tishri: CLDR's names, else the machine-translated ones, else `null`.
     */
    public fun months(
        code: String,
        leap: Boolean,
    ): List<String>? {
        val line = entries["$code.months"] ?: machineTranslated["$code.months"] ?: return null
        val names = line.split('|')
        require(names.size == CLDR_MONTHS) { "Hebrew month names need $CLDR_MONTHS entries for '$code'" }
        return (if (leap) leapYear else commonYear).map(names::get)
    }

    /**
     * The name of [month] (numbered from Tishri) of Hebrew [year] in language [code], or `null` when the language has
     * no names or the year has no such month.
     */
    public fun name(
        code: String,
        year: Int,
        month: Int,
    ): String? = months(code, isLeapYear(year))?.getOrNull(month - 1)

    /** The 19-year cycle's leap years 3, 6, 8, 11, 14, 17 and 19 (A-15): `(7 × year + 1) mod 19 < 7`. */
    internal fun isLeapYear(year: Int): Boolean = Math.floorMod(LEAP_FACTOR * year.toLong() + 1, CYCLE) < LEAP_YEARS

    /** Whether the names of [code] are machine-translated (DT-037) rather than taken from CLDR. */
    public fun isMachineTranslated(code: String): Boolean =
        entries["$code.months"] == null && machineTranslated["$code.months"] != null

    /** The CLDR locale the names of [code] were taken from, or `null` when the language is omitted. */
    public fun locale(code: String): String? = entries["$code.locale"]
}
