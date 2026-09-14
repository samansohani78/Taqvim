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
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.datetime.TimeZone

/** Synthetic inputs for the map tests; Tehran's coordinates are a rounded sample, not official data. */
object MapFixtures {
    val TEHRAN = Coordinates(35.7, 51.42)
    val NEW_YORK = Coordinates(40.71, -74.01)

    /** 12:00 in Tehran on 2026-09-14. */
    val NOON: Instant = Instant.parse("2026-09-14T08:30:00Z")

    val TEHRAN_ZONE: TimeZone = TimeZone.of("Asia/Tehran")

    /** A synthetic declination field: 1° per 6° of longitude, none near the poles. */
    val MAGNETIC = MagneticModel { place, _ -> if (place.latitude > POLAR) Double.NaN else place.longitude / 6 }

    /** Synthetic crescent classes by longitude band, none beyond 60° latitude. */
    val CRESCENT =
        CrescentObserver { place, _ ->
            if (kotlin.math.abs(place.latitude) > POLAR) {
                null
            } else {
                CrescentVisibilityClass.entries[((place.longitude + 180) / 61).toInt()]
            }
        }

    /** A small triangle "continent" and one boundary line. */
    val OUTLINE: WorldOutline = WorldOutlineParser.parse("L -9000,0 0,6000 9000,0\nB 0,0 1000,1000\n")

    private const val POLAR = 60.0

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
    ): MapUiState {
        val settings = settings(code, place)
        val computed = MapOverlayBuilder(magnetic, crescent).build(MapInputs(settings, instant, true, layers, picked))
        return MapUiState(
            outline = OutlineState.Ready(outline),
            layers = layers.toPersistentSet(),
            viewport = viewport,
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
