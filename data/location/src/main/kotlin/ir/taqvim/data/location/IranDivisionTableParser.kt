/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import ir.taqvim.core.model.Coordinates

/**
 * Parses the bundled `iran-divisions.tsv` written by `tools/geodata/geonames_iran_divisions.py`: `#` header lines
 * (provenance and a `# columns:` list), then one tab-separated row per division in `code,level,parentCode,en,fa,
 * latitude,longitude` order. `parentCode` is empty for a province. Malformed input is rejected with the line number.
 */
internal object IranDivisionTableParser {
    private const val COLUMNS_PREFIX = "# columns: "
    private const val COMMENT = "#"
    private val EXPECTED_COLUMNS = listOf("code", "level", "parentCode", "en", "fa", "latitude", "longitude")

    // Field positions within a row, matching EXPECTED_COLUMNS.
    private const val CODE = 0
    private const val LEVEL = 1
    private const val PARENT_CODE = 2
    private const val ENGLISH_NAME = 3
    private const val PERSIAN_NAME = 4
    private const val LATITUDE = 5
    private const val LONGITUDE = 6

    fun parse(lines: Sequence<String>): List<IranDivision> {
        var columns: List<String>? = null
        val divisions = mutableListOf<IranDivision>()
        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            if (line.startsWith(COLUMNS_PREFIX)) {
                columns = columnNames(line.removePrefix(COLUMNS_PREFIX), lineNumber)
            } else if (!line.startsWith(COMMENT) && line.isNotEmpty()) {
                requireNotNull(columns) { "line $lineNumber: a row before the columns header" }
                divisions += division(line, lineNumber)
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
        require(names == EXPECTED_COLUMNS) { "line $lineNumber: columns header was $names, expected $EXPECTED_COLUMNS" }
        return names
    }

    private fun division(
        line: String,
        lineNumber: Int,
    ): IranDivision {
        val fields = line.split('\t')
        require(fields.size == EXPECTED_COLUMNS.size) {
            "line $lineNumber: ${fields.size} fields, expected ${EXPECTED_COLUMNS.size}"
        }
        val code = fields[CODE]
        val parentCode = fields[PARENT_CODE]
        val english = fields[ENGLISH_NAME]
        val persian = fields[PERSIAN_NAME]
        return IranDivision(
            code = code,
            level = level(fields[LEVEL], lineNumber),
            parentCode = parentCode.ifEmpty { null },
            englishName = english.ifBlank { error("line $lineNumber: missing English name") },
            persianName = persian.ifEmpty { null },
            coordinates = coordinates(fields[LATITUDE], fields[LONGITUDE], lineNumber),
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
