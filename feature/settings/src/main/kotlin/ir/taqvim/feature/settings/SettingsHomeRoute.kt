/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/** Koin bindings of the settings home and subscriptions; `:app` binds [GeneralSettingsStore], [SubscriptionsStore]. */
val generalSettingsFeatureModule: Module =
    module {
        viewModel { params -> SettingsHomeViewModel(get(), params.getOrNull()) }
        viewModelOf(::SubscriptionsViewModel)
    }

/** Where the settings home leads; the app maps each [SettingsDestination] to its screen. */
@Immutable
data class SettingsNavigation(
    val onOpen: (SettingsDestination) -> Unit = {},
)

/** The settings home bound to its [SettingsHomeViewModel]; [initialItem] opens its tab with the item highlighted. */
@Composable
fun SettingsHomeRoute(
    modifier: Modifier = Modifier,
    initialItem: SettingsItemId? = null,
    navigation: SettingsNavigation = SettingsNavigation(),
    viewModel: SettingsHomeViewModel =
        koinViewModel(key = "settings-${initialItem?.name}") { parametersOf(initialItem) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(viewModel) { viewModel.effects.collect { currentNavigation.onOpen(it.destination) } }
    val actions =
        remember(viewModel) {
            SettingsHomeActions(
                onTabSelected = viewModel::onTabSelected,
                onQueryChanged = viewModel::onQueryChanged,
                onRowClicked = viewModel::onRowClicked,
                onOptionClicked = viewModel::onOptionClicked,
                onDialogDismissed = viewModel::onDialogDismissed,
            )
        }
    SettingsHomeScreen(state, actions, modifier)
}

/** The calendar subscriptions page bound to its [SubscriptionsViewModel]. */
@Composable
fun SubscriptionsRoute(
    modifier: Modifier = Modifier,
    viewModel: SubscriptionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            SubscriptionsActions(
                onDraftChanged = viewModel::onDraftChanged,
                onAdd = viewModel::onAdd,
                onRefresh = viewModel::onRefresh,
                onRemove = viewModel::onRemove,
                onEnabledChanged = viewModel::onEnabledChanged,
                onNetworkAllowedChanged = viewModel::onNetworkAllowedChanged,
            )
        }
    SubscriptionsScreen(state, actions, modifier)
}
