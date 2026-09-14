/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings of the onboarding; `:app` provides [GeneralSettingsStore] and [OnboardingStore]. */
val onboardingFeatureModule: Module =
    module {
        viewModelOf(::OnboardingViewModel)
    }

/**
 * The first-run onboarding (T-1501) bound to its [OnboardingViewModel]; system back returns a page or, on the first
 * page, skips. [onFinished] is called once after completing or skipping; [location] is the location page's content.
 */
@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
    viewModel: OnboardingViewModel = koinViewModel(),
    location: @Composable (Modifier) -> Unit = { LocationSettingsRoute(it, embedded = true) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(state.finished) { if (state.finished) currentOnFinished() }
    BackHandler(enabled = !state.finished) { viewModel.onBack() }
    val actions =
        remember(viewModel) {
            OnboardingActions(
                onLanguageSelected = viewModel::onLanguageSelected,
                onSourceToggled = viewModel::onSourceToggled,
                onNext = viewModel::onNext,
                onBack = viewModel::onBack,
                onSkip = viewModel::onSkip,
            )
        }
    OnboardingScreen(state, actions, modifier, location)
}
