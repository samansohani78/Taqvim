/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import android.content.res.AssetManager
import android.hardware.GeomagneticField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin bindings of the world map. `:app` provides [MapSettingsSource], [MapCitySource] and `kotlin.time.Clock`; the
 * outline asset
 * and the platform's World Magnetic Model come from this module.
 */
val mapFeatureModule: Module =
    module {
        single<WorldOutlineSource> { AssetWorldOutlineSource(androidContext().assets) }
        single<MagneticModel> { PlatformMagneticModel }
        viewModel { MapViewModel(get(), get(), get(), get(), citySource = get()) }
    }

/** The bundled outline, time-zone and plate lines read from the module's assets. */
class AssetWorldOutlineSource(
    private val assets: AssetManager,
) : WorldOutlineSource {
    override suspend fun load(): WorldOutline =
        withContext(Dispatchers.IO) {
            val texts =
                WorldOutlineParser.ASSETS.map { asset ->
                    assets.open(asset).bufferedReader().use { it.readText() }
                }
            WorldOutlineParser.parse(texts)
        }
}

/**
 * Declination, inclination and field strength from `android.hardware.GeomagneticField` (the platform's World Magnetic
 * Model, no bundled coefficients), at sea level.
 */
object PlatformMagneticModel : MagneticModel {
    override fun elements(
        place: Coordinates,
        instant: Instant,
    ): MagneticElements {
        val field =
            GeomagneticField(place.latitude.toFloat(), place.longitude.toFloat(), 0f, instant.toEpochMilliseconds())
        return MagneticElements(
            declinationDegrees = field.declination.toDouble(),
            inclinationDegrees = field.inclination.toDouble(),
            fieldStrengthNanotesla = field.fieldStrength.toDouble(),
        )
    }
}

/** The world map bound to its [MapViewModel]; [onLocationPicked] receives picked points and city markers. */
@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    onLocationPicked: (MapEffect.LocationPicked) -> Unit = {},
    viewModel: MapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onPicked by rememberUpdatedState(onLocationPicked)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MapEffect.LocationPicked -> onPicked(effect)
            }
        }
    }
    val actions =
        remember(viewModel) {
            MapActions(
                onToggleLayer = viewModel::onToggleLayer,
                onZoom = viewModel::onZoom,
                onPan = viewModel::onPan,
                onResize = viewModel::onResize,
                onPick = viewModel::onPick,
                onPickCenter = viewModel::onPickCenter,
                onPickCity = viewModel::onPickCity,
                onSelectMinute = viewModel::onSelectMinute,
                onStepDay = viewModel::onStepDay,
                onNow = viewModel::onNow,
                onSelectProjection = viewModel::onSelectProjection,
                onSelectCrescentCriterion = viewModel::onSelectCrescentCriterion,
            )
        }
    MapScreen(state, actions, modifier)
}
