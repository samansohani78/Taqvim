/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import androidx.compose.runtime.Composable
import ir.taqvim.core.astronomy.CrescentVisibilityClass
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import java.io.File
import kotlin.time.Instant
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.datetime.TimeZone

/** Synthetic inputs for the map tests; Tehran's coordinates are a rounded sample, not official data. */
object MapFixtures {
    val TEHRAN = Coordinates(35.7, 51.42)
    val NEW_YORK = Coordinates(40.71, -74.01)

    /** 12:00 in Tehran on 2026-09-14. */
    val NOON: Instant = Instant.parse("2026-09-14T08:30:00Z")

    val TEHRAN_ZONE: TimeZone = TimeZone.of("Asia/Tehran")

    /**
     * A synthetic magnetic field, none beyond 60° N: declination 1° per 6° of longitude, inclination equal to the
     * latitude, strength 30 000 nT plus 200 nT per degree of latitude.
     */
    val MAGNETIC =
        MagneticModel { place, _ ->
            if (place.latitude > POLAR) {
                MagneticElements(Double.NaN, Double.NaN, Double.NaN)
            } else {
                MagneticElements(place.longitude / 6, place.latitude, BASE_FIELD + FIELD_PER_DEGREE * place.latitude)
            }
        }

    /** Synthetic crescent classes by longitude band (up to the criterion's last class), none beyond 60° latitude. */
    val CRESCENT =
        CrescentObserver { place, _, criterion ->
            if (kotlin.math.abs(place.latitude) > POLAR) {
                null
            } else {
                val last = if (criterion == CrescentCriterion.YALLOP) CrescentVisibilityClass.F.ordinal else 3
                minOf(((place.longitude + 180) / 61).toInt(), last)
            }
        }

    /** Sample cities with rounded coordinates and populations, not catalog data; most populous first. */
    fun cities(code: String = "en"): List<MapCity> =
        listOf(
            city(1, "Tehran", "تهران", TEHRAN, 9_000_000),
            city(2, "Istanbul", "استانبول", Coordinates(41.01, 28.98), 8_900_000),
            city(3, "New York", "نیویورک", NEW_YORK, 8_300_000),
            city(4, "Baghdad", "بغداد", Coordinates(33.31, 44.36), 7_200_000),
            city(5, "Riyadh", "ریاض", Coordinates(24.71, 46.68), 6_900_000),
            city(6, "Karaj", "کرج", Coordinates(35.83, 50.99), 1_600_000),
            city(7, "Kabul", "کابل", Coordinates(34.53, 69.17), 4_400_000),
            city(8, "Dubai", "دبی", Coordinates(25.20, 55.27), 3_300_000),
        ).map { if (code == "fa") it.second else it.first }
            .sortedByDescending { it.population }

    private fun city(
        id: Long,
        english: String,
        persian: String,
        coordinates: Coordinates,
        population: Long,
    ): Pair<MapCity, MapCity> =
        MapCity(id, english, coordinates, population) to MapCity(id, persian, coordinates, population)

    /** A small triangle "continent" and one boundary line. */
    val OUTLINE: WorldOutline = WorldOutlineParser.parse("L -9000,0 0,6000 9000,0\nB 0,0 1000,1000\n")

    private const val POLAR = 60.0
    private const val BASE_FIELD = 30_000.0
    private const val FIELD_PER_DEGREE = 200.0

    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    fun settings(
        code: String = "en",
        place: Coordinates? = TEHRAN,
    ): MapSettings = MapSettings(language(code), TEHRAN_ZONE, place = place)

    /** The bundled Natural Earth outline, read from the module's assets. */
    fun worldOutline(): WorldOutline = WorldOutlineParser.parse(File("src/main/assets/map/world-110m.txt").readText())

    /** A state computed by the real overlay builder. */
    fun state(
        code: String = "en",
        layers: Set<MapLayer> = MapUiState.DEFAULT_LAYERS,
        picked: Coordinates? = null,
        outline: WorldOutline = OUTLINE,
        instant: Instant = NOON,
        viewport: MapViewport = MapViewport(),
        magnetic: MagneticModel = MAGNETIC,
        crescent: CrescentObserver = CRESCENT,
        place: Coordinates? = TEHRAN,
        criterion: CrescentCriterion = CrescentCriterion.YALLOP,
        globe: GlobeView? = null,
        cities: List<MapCity> = emptyList(),
    ): MapUiState {
        val settings = settings(code, place)
        val inputs = MapInputs(settings, instant, true, layers, picked, criterion)
        val computed = MapOverlayBuilder(magnetic, crescent).build(inputs)
        return MapUiState(
            outline = OutlineState.Ready(outline),
            layers = layers.toPersistentSet(),
            viewport = viewport,
            projection = if (globe == null) MapProjection.FLAT else MapProjection.GLOBE,
            globe = globe ?: GlobeView(),
            crescentCriterion = criterion,
            cities = cities.toPersistentList(),
            time = computed.time,
            overlays = computed.overlays,
            picked = computed.picked,
            hasPlace = place != null,
        )
    }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun MapTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
