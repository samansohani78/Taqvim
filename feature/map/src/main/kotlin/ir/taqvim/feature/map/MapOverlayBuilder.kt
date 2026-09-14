/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import ir.taqvim.core.astronomy.Qibla
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Everything one computation of the map depends on. */
internal data class MapInputs(
    val settings: MapSettings,
    val instant: Instant,
    val live: Boolean,
    val layers: Set<MapLayer>,
    val picked: Coordinates?,
)

/** The computed time, overlays and pick of one [MapInputs]. */
internal data class MapComputed(
    val time: MapTime,
    val overlays: MapOverlays,
    val picked: MapPick?,
)

/**
 * Builds the map's overlays. Grids that change within a day only slowly (crescent visibility, declination) are kept
 * per local day; day/night and the Moon are recomputed for every moment.
 */
internal class MapOverlayBuilder(
    private val magnetic: MagneticModel,
    private val crescentObserver: CrescentObserver,
) {
    private val crescentCache = DayCache<ShadeGrid>()
    private val declinationCache = DayCache<ShadeGrid>()

    fun build(inputs: MapInputs): MapComputed {
        val settings = inputs.settings
        val instant = inputs.instant
        val layers = inputs.layers
        val picked = inputs.picked
        val text = MapText(settings)
        val sun = SubPoints.sun(instant)
        val moon = SubPoints.moon(instant)
        val overlays =
            MapOverlays(
                illumination = if (MapLayer.DAY_NIGHT in layers) LayerGrids.illumination(sun) else null,
                moon = if (MapLayer.MOON_VISIBILITY in layers) LayerGrids.moonVisibility(moon) else null,
                crescent = crescentCache.grid(MapLayer.CRESCENT_VISIBILITY in layers, settings, instant, ::crescent),
                declination =
                    declinationCache.grid(
                        MapLayer.MAGNETIC_DECLINATION in layers,
                        settings,
                        instant,
                        ::declination,
                    ),
                sun = Equirectangular.project(sun),
                moonPoint = Equirectangular.project(moon),
                place = settings.place?.let(Equirectangular::project),
                qibla = path(settings.place, Qibla.KAABA.takeIf { MapLayer.QIBLA in layers }),
                directPath = path(settings.place, picked.takeIf { MapLayer.DIRECT_PATH in layers }),
            )
        val pick = picked?.let { MapPick(Equirectangular.project(it), text.coordinates(it)) }
        return MapComputed(text.time(instant, inputs.live), overlays, pick)
    }

    private fun crescent(dayStart: Instant): ShadeGrid = LayerGrids.crescent(dayStart, crescentObserver)

    private fun declination(dayStart: Instant): ShadeGrid = LayerGrids.declination(magnetic, dayStart)

    private fun DayCache<ShadeGrid>.grid(
        enabled: Boolean,
        settings: MapSettings,
        instant: Instant,
        compute: (Instant) -> ShadeGrid,
    ): ShadeGrid? {
        if (!enabled) return null
        val day = instant.toJdn(settings.timeZone)
        return get(day) { compute(day.toLocalDate().atStartOfDayIn(settings.timeZone)) }
    }

    private fun path(
        from: Coordinates?,
        to: Coordinates?,
    ): ImmutableList<ImmutableList<MapPoint>> =
        if (from == null || to == null) {
            emptyList<ImmutableList<MapPoint>>().toImmutableList()
        } else {
            GreatCirclePath.polylines(GreatCirclePath.points(from, to)).map { it.toImmutableList() }.toImmutableList()
        }
}

/** The last value computed for one day. */
internal class DayCache<T : Any> {
    private var day: Jdn? = null
    private var value: T? = null

    fun get(
        key: Jdn,
        compute: () -> T,
    ): T {
        val cached = value?.takeIf { day == key }
        return cached ?: compute().also {
            day = key
            value = it
        }
    }
}

/** Localized texts of the map: date, time and coordinates in the settings' language, calendar and zone. */
internal class MapText(
    private val settings: MapSettings,
) {
    private val numerals = settings.language.numerals

    fun time(
        instant: Instant,
        live: Boolean,
    ): MapTime {
        val local = instant.toLocalDateTime(settings.timeZone)
        val day = local.date.toJdn()
        val dateText =
            DateFormatter.format(settings.calendar.fromJdn(day), day.weekday(), settings.language, DateStyle.LONG)
        val clock = local.hour.toString().padStart(2, '0') + ":" + local.minute.toString().padStart(2, '0')
        val minute = local.hour * MINUTES_PER_HOUR + local.minute
        return MapTime(instant, minute, dateText, Numerals.localizeDigits(clock, numerals), live)
    }

    fun coordinates(place: Coordinates): String = decimal(place.latitude) + separator() + decimal(place.longitude)

    private fun decimal(value: Double): String =
        Numerals.format(BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP), numerals) + DEGREE

    private fun separator(): String =
        if (numerals == NumeralSystem.PERSIAN || numerals == NumeralSystem.EASTERN_ARABIC) {
            Char(ARABIC_COMMA) + " "
        } else {
            ", "
        }

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val DECIMALS = 2
        const val DEGREE = "°"
        const val ARABIC_COMMA = 0x060C
    }
}
