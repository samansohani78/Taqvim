/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

/** The time-zone board (T-1400): the current time in the home zone and in each chosen zone. */
internal object TimeZoneBoardBuilder {
    /** At most this many zones are suggested for a search. */
    const val MAX_SUGGESTIONS: Int = 8
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val TWO_DIGITS = 2

    /** Every IANA zone id known to the platform, sorted. */
    private val zoneIds: List<String> by lazy { TimeZone.availableZoneIds.sorted() }

    fun build(
        now: Instant,
        settings: ToolsSettings,
        zones: List<String>,
        query: String,
    ): TimeZoneBoard {
        val home = settings.homeZone
        val homeDate = now.toLocalDateTime(home).date
        val others = zones.filter { it != home.id && it in zoneIds }.distinct()
        val rows =
            (listOf(home) + others.map(TimeZone::of)).map { zone ->
                val local = now.toLocalDateTime(zone)
                ZoneRow(
                    id = zone.id,
                    city = cityOf(zone.id),
                    name = displayName(zone.id, now, settings.language),
                    time = clock(local.hour, local.minute, settings.language),
                    dayShift = (local.date.toEpochDays() - homeDate.toEpochDays()).toInt(),
                    offset = offset(zone.offsetAt(now).totalSeconds, settings.language),
                    isHome = zone == home,
                )
            }
        return TimeZoneBoard(rows.toImmutableList(), suggestions(query, others + home.id))
    }

    /** Zone ids matching [query] (case-insensitive, spaces for underscores) that are not on the board yet. */
    fun suggestions(
        query: String,
        shown: List<String>,
    ) = if (query.isBlank()) {
        emptyList<String>().toImmutableList()
    } else {
        val needle = query.trim().replace(' ', '_')
        zoneIds
            .filter { it.contains(needle, ignoreCase = true) && it !in shown }
            .take(MAX_SUGGESTIONS)
            .toImmutableList()
    }

    /** The last part of an IANA id with spaces, e.g. `America/New_York` → `New York`. */
    fun cityOf(id: String): String = id.substringAfterLast('/').replace('_', ' ')

    private fun displayName(
        id: String,
        now: Instant,
        language: LanguageSpec,
    ): String {
        val zone = java.util.TimeZone.getTimeZone(id)
        val daylight = zone.inDaylightTime(java.util.Date(now.toEpochMilliseconds()))
        return zone.getDisplayName(daylight, java.util.TimeZone.LONG, Locale.forLanguageTag(language.localeTag))
    }

    private fun clock(
        hour: Int,
        minute: Int,
        language: LanguageSpec,
    ): String = Numerals.localizeDigits("${pad(hour)}:${pad(minute)}", language.numerals)

    /** `+03:30`, `-11:00` or `+00:00` in the language's digits. */
    fun offset(
        totalSeconds: Int,
        language: LanguageSpec,
    ): String {
        val minutes = abs(totalSeconds) / SECONDS_PER_MINUTE
        val sign = if (totalSeconds < 0) "-" else "+"
        val text = "$sign${pad(minutes / MINUTES_PER_HOUR)}:${pad(minutes % MINUTES_PER_HOUR)}"
        return Numerals.localizeDigits(text, language.numerals)
    }

    private fun pad(value: Int): String = value.toString().padStart(TWO_DIGITS, '0')
}
