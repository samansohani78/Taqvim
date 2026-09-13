/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import android.icu.text.Collator
import android.icu.util.ULocale
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.PersianText
import ir.taqvim.core.model.Coordinates
import java.text.Normalizer
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** The city list with lookup by id, search, nearest place and per-language sorting (T-603). */
class CityCatalog(
    val cities: List<City>,
) {
    private val byId: Map<Long, City> = cities.associateBy(City::id)

    /** Search keys of every published name of each city, index-aligned with [cities]. */
    private val searchKeys: List<Set<String>> by lazy {
        cities.map { city -> (city.localizedNames.values + city.englishName).mapTo(mutableSetOf(), ::searchKey) }
    }

    fun city(id: Long): City? = byId[id]

    /**
     * Cities with a published name (in any language) containing [query], ignoring case, Latin diacritics, Arabic
     * diacritics and Arabic/Persian letter variants. Names that start with the query come first, then larger
     * populations; at most [limit] results.
     */
    fun search(
        query: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
    ): List<City> {
        require(limit > 0) { "limit must be positive (was $limit)" }
        val key = searchKey(query)
        if (key.isEmpty()) return emptyList()
        return cities.indices
            .mapNotNull { index -> matchRank(searchKeys[index], key)?.let { rank -> index to rank } }
            .sortedWith(compareBy({ it.second }, { -(cities[it.first].population ?: 0L) }))
            .take(limit)
            .map { cities[it.first] }
    }

    /** The city closest to [coordinates] along the great circle, or `null` for an empty catalog. */
    fun nearest(coordinates: Coordinates): City? = cities.minByOrNull { haversine(coordinates, it.coordinates) }

    /** [cities] ordered by [City.name] in [language] with that language's collation (CLDR tailoring via ICU). */
    fun sortedByName(language: LanguageSpec): List<City> {
        val collator = Collator.getInstance(ULocale.forLanguageTag(language.localeTag))
        return cities.sortedWith(compareBy(collator) { it.name(language.code) })
    }

    companion object {
        const val DEFAULT_SEARCH_LIMIT: Int = 50
        private const val RESOURCE = "cities.tsv"
        private const val PREFIX_MATCH = 0
        private const val INNER_MATCH = 1
        private val LATIN_COMBINING_MARKS = '̀'..'ͯ'

        /** Loads the bundled list (about 1.7 MB of text); call it off the main thread. */
        fun loadBundled(): CityCatalog {
            val stream = checkNotNull(CityCatalog::class.java.getResourceAsStream(RESOURCE)) { "$RESOURCE is missing" }
            return CityCatalog(stream.bufferedReader(Charsets.UTF_8).useLines(CityTableParser::parse))
        }

        internal fun searchKey(text: String): String =
            PersianText.searchKey(
                Normalizer.normalize(text, Normalizer.Form.NFD).filterNot { it in LATIN_COMBINING_MARKS },
            )

        private fun matchRank(
            names: Set<String>,
            key: String,
        ): Int? =
            when {
                names.any { it.startsWith(key) } -> PREFIX_MATCH
                names.any { key in it } -> INNER_MATCH
                else -> null
            }

        /** Haversine of the central angle; monotonic in distance, so it orders places without the arcsine. */
        private fun haversine(
            a: Coordinates,
            b: Coordinates,
        ): Double {
            val latitudeA = Math.toRadians(a.latitude)
            val latitudeB = Math.toRadians(b.latitude)
            val deltaLatitude = latitudeB - latitudeA
            val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
            return sin(deltaLatitude / 2).pow(2) + cos(latitudeA) * cos(latitudeB) * sin(deltaLongitude / 2).pow(2)
        }
    }
}
