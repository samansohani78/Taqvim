/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * Parses the bundled `iran-divisions.tsv` written by `tools/geodata/geonames_iran_divisions.py`: `#` header lines
 * (provenance and a `# columns:` list), then one tab-separated row per division. The base columns (`code`, `level`,
 * `parentCode`, `en`, `latitude`, `longitude`) and [IranDivision.PUBLISHED_LANGUAGES] must all appear in the header,
 * in any order; `parentCode` is empty for a province and an empty localized-name field means no published name.
 * Malformed input is rejected with the line number.
 */
internal object IranDivisionTableParser {
    private const val COLUMNS_PREFIX = "# columns: "
    private const val COMMENT = "#"
    private val BASE_COLUMNS = listOf("code", "level", "parentCode", "en", "latitude", "longitude")

    fun parse(lines: Sequence<String>): List<IranDivision> {
        var columns: List<String>? = null
        val divisions = mutableListOf<IranDivision>()
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            if (line.startsWith(COLUMNS_PREFIX)) {
                columns = columnNames(line.removePrefix(COLUMNS_PREFIX), lineNumber)
            } else if (!line.startsWith(COMMENT) && line.isNotEmpty()) {
                val known = requireNotNull(columns) { "line $lineNumber: a row before the columns header" }
                divisions += division(line, known, lineNumber)
            }
        }
        requireNotNull(columns) { "the columns header is missing" }
        return divisions
    }

    private fun columnNames(
        header: String,
        lineNumber: Int,
    ): List<String> {
        val names = header.split(',')
        val missing = (BASE_COLUMNS + IranDivision.PUBLISHED_LANGUAGES) - names.toSet()
        require(missing.isEmpty()) { "line $lineNumber: columns header lacks $missing" }
        require(names.distinct().size == names.size) { "line $lineNumber: columns header repeats a column: $names" }
        return names
    }

    private fun division(
        line: String,
        columns: List<String>,
        lineNumber: Int,
    ): IranDivision {
        val fields = line.split('\t')
        require(fields.size == columns.size) {
            "line $lineNumber: ${fields.size} fields, expected ${columns.size}"
        }
        val values = columns.zip(fields).toMap()

        fun field(name: String) = values.getValue(name)
        val parentCode = field("parentCode")
        val english = field("en")
        return IranDivision(
            code = field("code"),
            level = level(field("level"), lineNumber),
            parentCode = parentCode.ifEmpty { null },
            englishName = english.ifBlank { error("line $lineNumber: missing English name") },
            localizedNames =
                IranDivision.PUBLISHED_LANGUAGES
                    .mapNotNull { code -> field(code).ifEmpty { null }?.let { code to it } }
                    .toMap(),
            coordinates = coordinates(field("latitude"), field("longitude"), lineNumber),
        )
    }

    private fun level(
        name: String,
        lineNumber: Int,
    ): IranDivisionLevel =
        runCatching { IranDivisionLevel.valueOf(name) }
            .getOrElse { throw IllegalArgumentException("line $lineNumber: invalid level '$name'", it) }

    private fun coordinates(
        latitude: String,
        longitude: String,
        lineNumber: Int,
    ): Coordinates =
        runCatching {
            Coordinates(
                requireNotNull(latitude.toDoubleOrNull()) { "invalid latitude '$latitude'" },
                requireNotNull(longitude.toDoubleOrNull()) { "invalid longitude '$longitude'" },
            )
        }.getOrElse { throw IllegalArgumentException("line $lineNumber: ${it.message}", it) }
}
