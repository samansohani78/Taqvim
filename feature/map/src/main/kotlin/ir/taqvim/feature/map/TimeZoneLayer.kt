/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Immutable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

/** The time-zone layer at one moment: the boundaries between bands whose offsets differ and the offset labels. */
@Immutable
data class TimeZoneOverlay(
    val boundaries: ImmutableList<FloatArray> = persistentListOf(),
    /** Largest band first. */
    val labels: ImmutableList<OffsetLabel> = persistentListOf(),
)

/**
 * A band's UTC offset [text] (e.g. "+3:30", in the language's digits) written at [point]; [mixed] when places inside the
 * band have other offsets at the shown moment, so the label is marked.
 */
@Immutable
data class OffsetLabel(
    val point: MapPoint,
    val text: String,
    val areaKm2: Long,
    val mixed: Boolean = false,
)

/**
 * The time-zone bands' offsets, computed by the app (T-1301). The bundled band shapes date from 2012, so no offset of
 * the source is shown: a band's offset is its representative zone's total UTC offset (standard time plus daylight
 * saving) at the shown moment, from the platform's tz rules. A boundary is drawn only where the two bands' offsets
 * differ at that moment. Bands without a zone (oceans, Antarctica, uninhabited islands), or with a zone the platform
 * does not know, get no label, and their boundaries are drawn where the 2012 offsets of both bands differ.
 *
 * This is an approximation (review I09): each band takes the zone of its most populous catalog city, so a 2012 band that
 * now holds regions with other offsets shows that city's offset throughout (for example a band with Cairo and Istanbul
 * follows Cairo). The asset lists, per band, the other zones of its catalog places whose offsets differed at generation
 * time; when any of them differs from the band's zone at the shown moment, the band's label is marked
 * ([OffsetLabel.mixed]) and the legend explains the mark. Geometry is never corrected. The tz
 * database's own conventions carry over: Europe/Dublin keeps Irish summer time as standard time (the total offset is
 * still right), and a band whose city is Urumqi would follow Asia/Urumqi's local time (UTC+6) rather than China's
 * official UTC+8; in the bundled asset China's band follows Asia/Shanghai.
 */
object TimeZoneOffsets {
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60
    private const val MINUS_SIGN = '−'

    /** [zoneId]'s total UTC offset at [instant] in minutes, or `null` when the platform does not know the zone. */
    fun minutesAt(
        zoneId: String,
        instant: Instant,
    ): Int? = runCatching { TimeZone.of(zoneId).offsetAt(instant).totalSeconds / SECONDS_PER_MINUTE }.getOrNull()

    /** The shown boundaries and labels of [bands] at [instant], labels written in [numerals]. */
    fun overlay(
        bands: TimeZoneBands,
        instant: Instant,
        numerals: NumeralSystem,
    ): TimeZoneOverlay {
        val offsets = offsets(bands.bands, instant)
        val boundaries = bands.boundaries.filter { isShown(it, bands.bands, offsets) }.map { it.line }
        val labels =
            bands.bands
                .mapIndexedNotNull { index, band ->
                    offsets[index]?.let {
                        OffsetLabel(band.label, text(it, numerals), band.areaKm2, isMixed(band, it, instant))
                    }
                }.sortedByDescending { it.areaKm2 }
        return TimeZoneOverlay(boundaries.toImmutableList(), labels.toImmutableList())
    }

    /** Each band's offset in minutes at [instant] (`null` without a known zone); every zone is looked up once. */
    fun offsets(
        bands: List<TimeZoneBand>,
        instant: Instant,
    ): List<Int?> {
        val known = HashMap<String, Int?>()
        return bands.map { band ->
            band.zoneId?.let { zone ->
                if (zone in known) known[zone] else minutesAt(zone, instant).also { known[zone] = it }
            }
        }
    }

    /** Whether any of [band]'s other zones has an offset other than [offset] at [instant] (unknown zones are skipped). */
    fun isMixed(
        band: TimeZoneBand,
        offset: Int,
        instant: Instant,
    ): Boolean = band.otherZoneIds.any { zone -> minutesAt(zone, instant)?.let { it != offset } ?: false }

    /** Whether [boundary] separates bands with different [offsets], or different 2012 offsets where one is unknown. */
    fun isShown(
        boundary: ZoneBoundary,
        bands: List<TimeZoneBand>,
        offsets: List<Int?>,
    ): Boolean {
        val first = offsets[boundary.first]
        val second = offsets[boundary.second]
        return if (first != null && second != null) {
            first != second
        } else {
            bands[boundary.first].offset2012Minutes != bands[boundary.second].offset2012Minutes
        }
    }

    /** An offset as "+3:30", "−4" or "+0", in [numerals]' digits. */
    fun text(
        minutes: Int,
        numerals: NumeralSystem,
    ): String {
        val sign = if (minutes < 0) MINUS_SIGN else '+'
        val hours = abs(minutes) / MINUTES_PER_HOUR
        val rest = abs(minutes) % MINUTES_PER_HOUR
        val clock = if (rest == 0) "$hours" else "$hours:" + rest.toString().padStart(2, '0')
        return sign + Numerals.localizeDigits(clock, numerals)
    }
}

/** Which plate boundaries the map draws at a zoom (T-1301). */
object PlateBoundaries {
    /**
     * The smallest plate whose boundaries show at zoom 1, in km². The limit falls with the square of the zoom (the area
     * a plate covers on screen), so at the maximum zoom of 8 it is about 1 400 km² and the smallest plate of the model
     * (about 1 440 km²) shows.
     */
    const val AREA_AT_ZOOM_ONE_KM2: Double = 90_000.0

    fun minimumAreaKm2(zoom: Double): Double {
        val shown = zoom.takeIf { it.isFinite() }?.coerceAtLeast(MapViewport.MIN_ZOOM) ?: MapViewport.MIN_ZOOM
        return AREA_AT_ZOOM_ONE_KM2 / (shown * shown)
    }

    fun isShown(
        boundary: PlateBoundary,
        zoom: Double,
    ): Boolean = boundary.smallerPlateAreaKm2 >= minimumAreaKm2(zoom)
}
