/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the compass and the level. `:app` provides [CompassSettingsSource], [LevelCalibrationStore] and
 * `kotlin.time.Clock`; the platform sensors and the World Magnetic Model come from this module.
 */
val compassFeatureModule: Module =
    module {
        single<MotionSensors> { PlatformMotionSensors(androidContext().getSystemService(SensorManager::class.java)) }
        single<DeclinationModel> { PlatformDeclination }
        viewModelOf(::CompassViewModel)
        viewModelOf(::LevelViewModel)
    }

/** The compass bound to its [CompassViewModel]; the screen orientation is locked while it is shown. */
@Composable
fun CompassRoute(
    modifier: Modifier = Modifier,
    viewModel: CompassViewModel = koinViewModel(),
) {
    LockScreenOrientation()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            CompassActions(onToggleFrozen = viewModel::onToggleFrozen, onToggleSunPath = viewModel::onToggleSunPath)
        }
    CompassScreen(state, actions, modifier)
}

/** The level and ruler bound to their [LevelViewModel]; the screen orientation is locked while it is shown. */
@Composable
fun LevelRoute(
    modifier: Modifier = Modifier,
    viewModel: LevelViewModel = koinViewModel(),
) {
    LockScreenOrientation()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            LevelActions(
                onSelectTab = viewModel::onSelectTab,
                onCalibrate = viewModel::onCalibrate,
                onResetCalibration = viewModel::onResetCalibration,
                onSelectUnit = viewModel::onSelectUnit,
            )
        }
    LevelScreen(state, actions, modifier)
}

/**
 * Keeps the display in the device's natural orientation while composed, so that the screen's axes stay aligned with
 * the sensor axes the compass and the level measure in; the previous request is restored afterwards.
 */
@Composable
internal fun LockScreenOrientation() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        onDispose { previous?.let { activity.requestedOrientation = it } }
    }
}

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
