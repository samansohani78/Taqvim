/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * Parses the bundled `cities.tsv` written by `tools/geodata/natural_earth_cities.py`: `#` header lines (provenance
 * and a `# columns:` list), then one tab-separated row per place. Localized name columns are empty when the name
 * equals the English one. Malformed input is rejected with the line number.
 */
internal object CityTableParser {
    private const val COLUMNS_PREFIX = "# columns: "
    private const val COMMENT = "#"
    private val BASE_COLUMNS = listOf("neId", "country", "region", "latitude", "longitude", "timeZone", "population")

    fun parse(lines: Sequence<String>): List<City> {
        var columns: Map<String, Int>? = null
        val cities = mutableListOf<City>()
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            if (line.startsWith(COLUMNS_PREFIX)) {
                columns = columnIndex(line.removePrefix(COLUMNS_PREFIX))
            } else if (!line.startsWith(COMMENT) && line.isNotBlank()) {
                val known = requireNotNull(columns) { "line $lineNumber: row before the columns header" }
                cities += Row(lineNumber, line, known).toCity()
            }
        }
        return cities
    }

    private fun columnIndex(header: String): Map<String, Int> {
        val names = header.split(',')
        val missing = (BASE_COLUMNS + City.PUBLISHED_LANGUAGES) - names.toSet()
        require(missing.isEmpty()) { "columns header lacks $missing" }
        require(names.distinct().size == names.size) { "columns header repeats a column: $names" }
        return names.withIndex().associate { (index, name) -> name to index }
    }

    private class Row(
        private val lineNumber: Int,
        line: String,
        private val columns: Map<String, Int>,
    ) {
        private val fields = line.split('\t')

        init {
            require(fields.size == columns.size) {
                "line $lineNumber: expected ${columns.size} fields, found ${fields.size}"
            }
        }

        private fun field(name: String): String = fields[columns.getValue(name)]

        private fun optional(name: String): String? = field(name).ifEmpty { null }

        private fun number(name: String): Double =
            requireNotNull(field(name).toDoubleOrNull()) { "line $lineNumber: invalid $name '${field(name)}'" }

        fun toCity(): City {
            val english = field(City.ENGLISH)
            require(english.isNotBlank()) { "line $lineNumber: missing English name" }
            val coordinates =
                runCatching { Coordinates(number("latitude"), number("longitude")) }
                    .getOrElse { throw IllegalArgumentException("line $lineNumber: ${it.message}", it) }
            return City(
                id = requireNotNull(field("neId").toLongOrNull()) { "line $lineNumber: invalid id '${field("neId")}'" },
                englishName = english,
                localizedNames =
                    City.PUBLISHED_LANGUAGES
                        .filter { it != City.ENGLISH }
                        .mapNotNull { code -> optional(code)?.let { code to it } }
                        .toMap(),
                countryCode = optional("country"),
                region = optional("region"),
                coordinates = coordinates,
                timeZoneId = optional("timeZone"),
                population =
                    optional("population")?.let {
                        requireNotNull(it.toLongOrNull()?.takeIf { value -> value >= 0 }) {
                            "line $lineNumber: invalid population '$it'"
                        }
                    },
            )
        }
    }
}
