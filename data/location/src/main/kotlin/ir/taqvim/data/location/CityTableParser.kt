/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * Parses the bundled `cities.tsv` written by `tools/geodata/natural_earth_cities.py`: `#` header lines (provenance,
 * a `# layout:` note and a `# columns:` list), then the body in column-major layout (T-1800) — one tab-separated
 * line per column of the header, holding that column's value for every place in the same order. Localized name
 * columns are empty when the name equals the English one. Malformed input is rejected with the line number.
 */
internal object CityTableParser {
    private const val COLUMNS_PREFIX = "# columns: "
    private const val COMMENT = "#"
    private val BASE_COLUMNS = listOf("neId", "country", "region", "latitude", "longitude", "timeZone", "population")

    fun parse(lines: Sequence<String>): List<City> {
        var columns: List<String>? = null
        val body = mutableListOf<String>()
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            if (line.startsWith(COLUMNS_PREFIX)) {
                columns = columnNames(line.removePrefix(COLUMNS_PREFIX))
            } else if (!line.startsWith(COMMENT)) {
                requireNotNull(columns) { "line $lineNumber: a column before the columns header" }
                body += line
            }
        }
        val known = requireNotNull(columns) { "the columns header is missing" }
        // A column whose value is empty for every place is an empty line, so only lines past the last column can be
        // the blank one a reader adds for the file's final newline.
        while (body.size > known.size && body.last().isEmpty()) body.removeAt(body.lastIndex)
        require(body.size == known.size) { "expected ${known.size} column lines, found ${body.size}" }
        return Table(known.zip(body.map { it.split('\t') }).toMap()).cities()
    }

    private fun columnNames(header: String): List<String> {
        val names = header.split(',')
        val missing = (BASE_COLUMNS + City.PUBLISHED_LANGUAGES) - names.toSet()
        require(missing.isEmpty()) { "columns header lacks $missing" }
        require(names.distinct().size == names.size) { "columns header repeats a column: $names" }
        return names
    }

    /** The parsed columns, each holding one value per place. */
    private class Table(
        private val columns: Map<String, List<String>>,
    ) {
        private val size = columns.values.first().size

        init {
            val ragged = columns.entries.firstOrNull { it.value.size != size }
            require(ragged == null) {
                "column '${ragged?.key}' has ${ragged?.value?.size} places, expected $size"
            }
        }

        fun cities(): List<City> = (0 until size).map(::city)

        private fun field(
            name: String,
            place: Int,
        ): String = columns.getValue(name)[place]

        private fun optional(
            name: String,
            place: Int,
        ): String? = field(name, place).ifEmpty { null }

        private fun number(
            name: String,
            place: Int,
        ): Double =
            requireNotNull(field(name, place).toDoubleOrNull()) {
                "place ${place + 1}: invalid $name '${field(name, place)}'"
            }

        private fun city(place: Int): City {
            val english = field(City.ENGLISH, place)
            require(english.isNotBlank()) { "place ${place + 1}: missing English name" }
            return City(
                id =
                    requireNotNull(field("neId", place).toLongOrNull()) {
                        "place ${place + 1}: invalid id '${field("neId", place)}'"
                    },
                englishName = english,
                localizedNames =
                    City.PUBLISHED_LANGUAGES
                        .filter { it != City.ENGLISH }
                        .mapNotNull { code -> optional(code, place)?.let { code to it } }
                        .toMap(),
                countryCode = optional("country", place),
                region = optional("region", place),
                coordinates = coordinates(place),
                timeZoneId = optional("timeZone", place),
                population =
                    optional("population", place)?.let {
                        requireNotNull(it.toLongOrNull()?.takeIf { value -> value >= 0 }) {
                            "place ${place + 1}: invalid population '$it'"
                        }
                    },
            )
        }

        private fun coordinates(place: Int): Coordinates =
            runCatching { Coordinates(number("latitude", place), number("longitude", place)) }
                .getOrElse { throw IllegalArgumentException("place ${place + 1}: ${it.message}", it) }
    }
}
