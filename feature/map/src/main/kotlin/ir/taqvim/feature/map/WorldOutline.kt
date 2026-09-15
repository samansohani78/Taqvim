/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable
import java.security.MessageDigest

/**
 * The bundled vector lines of the map, each as flattened (x, y) pairs in map units: Natural Earth 1:110m land rings and
 * country boundary lines (public domain; `assets/map/world-110m.txt`), Natural Earth 1:10m time-zone band boundaries
 * (public domain, 2012 content; `assets/map/time-zones-10m.txt`) and the plate-id boundaries of the Matthews et al.
 * (2016) present-day static plate polygons (CC BY 4.0, simplified; `assets/map/plates-matthews-2016.txt`). Generated
 * by the scripts in `tools/geodata/`.
 */
@Immutable
class WorldOutline(
    val land: List<FloatArray>,
    val borders: List<FloatArray>,
    val timeZones: List<FloatArray> = emptyList(),
    val plates: List<FloatArray> = emptyList(),
)

/**
 * Reads the outline assets: `#` header lines, then `L` (land ring), `B` (country boundary), `Z` (time-zone boundary)
 * or `P` (plate boundary) lines of "lon,lat" pairs in hundredths of a degree.
 */
object WorldOutlineParser {
    const val ASSET: String = "map/world-110m.txt"
    const val TIME_ZONES_ASSET: String = "map/time-zones-10m.txt"
    const val PLATES_ASSET: String = "map/plates-matthews-2016.txt"

    /** Every asset of the map, read together by [parse]. */
    val ASSETS: List<String> = listOf(ASSET, TIME_ZONES_ASSET, PLATES_ASSET)

    private const val HASH_PREFIX = "# body-sha256: "
    private const val HUNDREDTHS = 100f
    private const val HALF_TURN = 180f
    private const val FULL_TURN = 360f
    private const val RIGHT_ANGLE = 90f
    private const val KINDS = "LBZP"

    /** The parts of one asset [text]. */
    fun parse(text: String): WorldOutline = parse(listOf(text))

    /** The parts of every text in [texts], in order. */
    fun parse(texts: List<String>): WorldOutline {
        val parts =
            texts
                .asSequence()
                .flatMap { text -> text.lineSequence().withIndex() }
                .filter { (_, line) -> line.isNotBlank() && !line.startsWith("#") }
                .map { (index, line) -> line.first() to points(line, index + 1) }
                .toList()
        parts.forEach { (kind, _) -> require(kind in KINDS) { "unknown outline part '$kind'" } }

        fun kind(tag: Char) = parts.filter { it.first == tag }.map { it.second }
        return WorldOutline(land = kind('L'), borders = kind('B'), timeZones = kind('Z'), plates = kind('P'))
    }

    /** Whether the header's `body-sha256` equals the SHA-256 of the body lines joined with "\n". */
    fun bodyHashMatches(text: String): Boolean {
        val lines = text.lines().filter { it.isNotEmpty() }
        val declared = lines.firstOrNull { it.startsWith(HASH_PREFIX) }?.removePrefix(HASH_PREFIX)
        val body = lines.filterNot { it.startsWith("#") }.joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.UTF_8))
        return declared == digest.joinToString("") { "%02x".format(it) }
    }

    private fun points(
        line: String,
        number: Int,
    ): FloatArray {
        val pairs = line.substringAfter(' ', "").split(' ').filter { it.isNotEmpty() }
        require(line.getOrNull(1) == ' ' && pairs.size >= 2) { "line $number: an outline part needs two points" }
        val values = FloatArray(pairs.size * 2)
        pairs.forEachIndexed { index, pair ->
            val longitude = pair.substringBefore(',').toIntOrNull()
            val latitude = pair.substringAfter(',', "").toIntOrNull()
            require(longitude != null && latitude != null) { "line $number: '$pair' is not lon,lat" }
            values[2 * index] = (longitude / HUNDREDTHS + HALF_TURN) / FULL_TURN
            values[2 * index + 1] = (RIGHT_ANGLE - latitude / HUNDREDTHS) / HALF_TURN
        }
        return values
    }
}
