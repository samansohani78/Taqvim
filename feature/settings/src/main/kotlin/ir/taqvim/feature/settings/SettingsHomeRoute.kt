/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the settings home, subscriptions and official Islamic dates; `:app` binds [GeneralSettingsStore],
 * [SubscriptionsStore] and [IslamicOverrideStore].
 */
val generalSettingsFeatureModule: Module =
    module {
        viewModel { params -> SettingsHomeViewModel(get(), params.getOrNull()) }
        viewModel { SubscriptionsViewModel(get(), get()) }
        viewModel { IslamicOverrideViewModel(get()) }
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
                onConfirmationAccepted = viewModel::onConfirmationAccepted,
                onConfirmationDismissed = viewModel::onConfirmationDismissed,
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
                onDetailsToggled = viewModel::onDetailsToggled,
            )
        }
    SubscriptionsScreen(state, actions, modifier)
}

/** Types the system file picker offers for an override file. */
private val OVERRIDE_FILE_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

/** The official Islamic dates page (ADR-0037) bound to its [IslamicOverrideViewModel]; files come from the picker. */
@Composable
fun IslamicOverrideRoute(
    modifier: Modifier = Modifier,
    viewModel: IslamicOverrideViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onImport(it.toString()) }
        }
    val actions =
        remember(viewModel) {
            IslamicOverrideActions(
                onOfficialChanged = viewModel::onOfficialChanged,
                onImport = viewModel::onImport,
                onRemove = viewModel::onRemove,
            )
        }
    IslamicOverrideScreen(state, actions, onPickFile = { picker.launch(OVERRIDE_FILE_TYPES) }, modifier = modifier)
}
