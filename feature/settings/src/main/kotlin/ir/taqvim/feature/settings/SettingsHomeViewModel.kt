/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings home (T-1500): three tabs of table-driven items (see [SettingsCatalog]), a search across all of them and
 * deep links that open an item's tab with the item highlighted. Every change is stored immediately.
 */
class SettingsHomeViewModel(
    private val store: GeneralSettingsStore,
    initialItem: SettingsItemId? = null,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            SettingsHomeUiState(tab = initialItem?.tab ?: SettingsTab.INTERFACE_CALENDAR, highlighted = initialItem),
        )
    val uiState: StateFlow<SettingsHomeUiState> = state.asStateFlow()

    private val navigation = Channel<SettingsEffect>(Channel.BUFFERED)

    /** Pages to open, each delivered once. */
    val effects: Flow<SettingsEffect> = navigation.receiveAsFlow()

    private var latest: GeneralSettings? = null

    init {
        store
            .settings()
            .onEach { data ->
                latest = data.settings
                state.update { current ->
                    current.copy(
                        loading = false,
                        rows = SettingsStateMapper.rows(data.settings),
                        dialog = current.dialog?.let { SettingsStateMapper.dialog(it.id, data.settings) },
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onTabSelected(tab: SettingsTab) {
        state.update { it.copy(tab = tab, query = "", highlighted = null) }
    }

    fun onQueryChanged(query: String) {
        state.update { it.copy(query = query, highlighted = null) }
    }

    fun onRowClicked(id: SettingsItemId) {
        val settings = latest ?: return
        state.update { it.copy(highlighted = null) }
        when (val control = SettingsCatalog.control(id)) {
            is SettingsControl.Toggle -> {
                update { control.write(it, !control.read(it)) }
            }

            is SettingsControl.Choice, is SettingsControl.MultiChoice, is SettingsControl.TimeOfDay -> {
                state.update { it.copy(dialog = SettingsStateMapper.dialog(id, settings)) }
            }

            is SettingsControl.Link -> {
                navigation.trySend(SettingsEffect(control.destination))
            }

            SettingsControl.ClearRecentSearches -> {
                if (settings.hasRecentSearches) viewModelScope.launch { store.clearRecentSearches() }
            }
        }
    }

    /** An option of the open dialog: a choice is stored and closes it; a multiple choice toggles the option. */
    fun onOptionClicked(key: String) {
        val dialog = state.value.dialog ?: return
        when (val control = SettingsCatalog.control(dialog.id)) {
            is SettingsControl.Choice -> {
                state.update { it.copy(dialog = null) }
                update { control.write(it, key) }
            }

            is SettingsControl.TimeOfDay -> {
                state.update { it.copy(dialog = null) }
                key.toIntOrNull()?.let { minute -> update { control.write(it, minute) } }
            }

            is SettingsControl.MultiChoice -> {
                update { settings ->
                    val chosen = control.read(settings)
                    val next = if (key in chosen) chosen - key else chosen + key
                    if (next.isEmpty() && !control.allowEmpty) settings else control.write(settings, next)
                }
            }

            else -> {
                state.update { it.copy(dialog = null) }
            }
        }
    }

    fun onDialogDismissed() {
        state.update { it.copy(dialog = null) }
    }

    private fun update(transform: (GeneralSettings) -> GeneralSettings) {
        viewModelScope.launch { store.update(transform) }
    }
}
