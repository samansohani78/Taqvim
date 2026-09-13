/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin bindings of the Astronomy screen; `:app` provides [AstronomySettingsSource] and `kotlin.time.Clock`. */
val astronomyFeatureModule: Module =
    module {
        viewModel { AstronomyViewModel(get(), get()) }
    }

/** The Astronomy screen bound to its [AstronomyViewModel]. */
@Composable
fun AstronomyRoute(
    modifier: Modifier = Modifier,
    viewModel: AstronomyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            AstronomyActions(
                onMode = viewModel::onMode,
                onStepDays = viewModel::onStepDays,
                onStepYears = viewModel::onStepYears,
                onMinuteOfDay = viewModel::onMinuteOfDay,
                onNow = viewModel::onNow,
                onPickDate = viewModel::onPickDate,
                onDatePicked = viewModel::onDatePicked,
                onDismissPicker = viewModel::onDismissPicker,
                onOpenDialog = viewModel::onOpenDialog,
                onDismissDialog = viewModel::onDismissDialog,
            )
        }
    AstronomyScreen(state, actions, modifier)
}
