/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Composable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Synthetic places and fakes of the location settings ports (rounded sample coordinates, not official data). */
internal object LocationFixtures {
    val english: LanguageSpec = requireNotNull(LanguageTable.forCode("en"))
    val persian: LanguageSpec = requireNotNull(LanguageTable.forCode("fa"))

    val tehran = CityOption(112_931, "Tehran", "Iran", Coordinates(35.6892, 51.389), "Asia/Tehran")
    val karaj = CityOption(128_747, "Karaj", "Alborz, Iran", Coordinates(35.8327, 50.9915), "Asia/Tehran")
    val nowhere = CityOption(7, "Nowhere Island", null, Coordinates(-10.5, 170.25), null)
    val cities = listOf(tehran, karaj, nowhere)

    fun choice(city: CityOption): PlaceChoice =
        PlaceChoice(PlaceKind.CITY, city.id, city.name, city.coordinates, requireNotNull(city.timeZoneId))

    val describer = PlaceDescriber { PlaceDescription("Shiraz", "Asia/Tehran") }
}

/** Remembers every stored place and emits it back. */
internal class FakeLocationStore(
    language: LanguageSpec,
    place: PlaceChoice? = null,
) : LocationSettingsStore {
    private val state = MutableStateFlow(LocationSettings(language, place))
    val chosen = mutableListOf<PlaceChoice>()

    override fun settings(): Flow<LocationSettings> = state

    override suspend fun choose(place: PlaceChoice) {
        chosen += place
        state.update { it.copy(place = place) }
    }
}

/** Cities whose name contains the query, in catalog order; remembers the queries. */
internal class FakeCitySearch(
    private val cities: List<CityOption> = LocationFixtures.cities,
) : CitySearch {
    val queries = mutableListOf<String>()

    override suspend fun search(query: String): List<CityOption> {
        queries += query
        return cities.filter { it.name.contains(query, ignoreCase = true) }
    }
}

/** The app theme with fixed colors for tests and screenshots. */
@Composable
internal fun LocationTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
