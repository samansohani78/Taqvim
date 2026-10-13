/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.ui.permission.LocalNotificationsAllowed
import ir.taqvim.core.ui.permission.rememberNotificationPermission
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the settings home, the calendar files page and the official Islamic dates page; `:app` binds
 * [GeneralSettingsStore], [IcsFilesStore] and [IslamicOverrideStore].
 */
val generalSettingsFeatureModule: Module =
    module {
        viewModel { params -> SettingsHomeViewModel(get(), params.getOrNull()) }
        viewModel { IcsFilesViewModel(get()) }
        viewModel { IslamicOverrideViewModel(get()) }
    }

/** Where the settings home leads; the app maps each [SettingsDestination] to its screen. */
@Immutable
data class SettingsNavigation(
    val onOpen: (SettingsDestination) -> Unit = {},
)

/**
 * The settings home bound to its [SettingsHomeViewModel]; [initialItem] opens its tab with the item highlighted.
 * Switching the date notification on asks for the notification permission, because that switch is the moment the
 * user asks to be shown something (T-1213).
 */
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
    val notifications = rememberNotificationPermission()
    val currentState by rememberUpdatedState(state)
    val actions =
        remember(viewModel, notifications) {
            SettingsHomeActions(
                onTabSelected = viewModel::onTabSelected,
                onQueryChanged = viewModel::onQueryChanged,
                onRowClicked = { id ->
                    val turningOn = currentState.isOff(id)
                    viewModel.onRowClicked(id)
                    if (id == SettingsItemId.PERSISTENT_NOTIFICATION && turningOn) notifications.request()
                },
                onOptionClicked = viewModel::onOptionClicked,
                onDialogDismissed = viewModel::onDialogDismissed,
                onConfirmationAccepted = viewModel::onConfirmationAccepted,
                onConfirmationDismissed = viewModel::onConfirmationDismissed,
            )
        }
    CompositionLocalProvider(LocalNotificationsAllowed provides notifications.allowed) {
        SettingsHomeScreen(state, actions, modifier)
    }
}

/** Whether [id] is a switch that is currently off, so clicking it turns it on. */
private fun SettingsHomeUiState.isOff(id: SettingsItemId): Boolean =
    rows.firstOrNull { it.id == id }?.value.let { it is RowValue.Switch && !it.checked }

/**
 * Types the system file picker offers for an iCalendar file.
 *
 * `text/calendar` is the registered type (RFC 5545 §8.1), but plenty of files arrive as `text/plain` or with no
 * type at all, and a picker that hides them would look broken to the user holding the file.
 */
private val ICS_FILE_TYPES = arrayOf("text/calendar", "text/plain", "application/octet-stream")

/** Name offered for a new export file; the user can change it in the picker. */
private const val ICS_EXPORT_NAME = "taqvim.ics"

/**
 * The calendar files page (T-1003) bound to its [IcsFilesViewModel]; both files come from the system picker, so
 * Taqvim touches one document the user chose and needs no storage permission for it (ADR-0054).
 */
@Composable
fun IcsFilesRoute(
    modifier: Modifier = Modifier,
    viewModel: IcsFilesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onImport(it.toString()) }
        }
    val exportPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
            uri?.let { viewModel.onExport(it.toString()) }
        }
    val actions =
        remember(viewModel) {
            IcsFilesActions(
                onDuplicatesChanged = viewModel::onDuplicatesChanged,
                onImport = viewModel::onImport,
                onExport = viewModel::onExport,
            )
        }
    IcsFilesScreen(
        state = state,
        actions = actions,
        onPickImport = { importPicker.launch(ICS_FILE_TYPES) },
        onPickExport = { exportPicker.launch(ICS_EXPORT_NAME) },
        modifier = modifier,
    )
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
