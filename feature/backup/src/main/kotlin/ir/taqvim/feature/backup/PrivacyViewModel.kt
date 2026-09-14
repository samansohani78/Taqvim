/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** State of the privacy dashboard (T-1503, F-15). */
data class PrivacyUiState(
    val loading: Boolean = true,
    val data: ImmutableList<StoredDataRow> = persistentListOf(),
    val permissions: ImmutableList<PermissionStatus> = persistentListOf(),
    /** Data whose clearing waits for confirmation, or `null`. */
    val confirmClear: StoredDataKind? = null,
    /** Data that could not be cleared, or `null`. */
    val clearFailed: StoredDataKind? = null,
)

/** One kind of stored data. */
data class StoredDataRow(
    val kind: StoredDataKind,
    /** Number of entries in the app's digits. */
    val countText: String,
    val empty: Boolean,
    /** Whether a clear action is offered: clearable kinds that hold something. */
    val canClear: Boolean,
)

/** User actions of the privacy dashboard. */
@Immutable
data class PrivacyActions(
    val onClearRequested: (StoredDataKind) -> Unit = {},
    val onClearConfirmed: () -> Unit = {},
    val onClearDismissed: () -> Unit = {},
    val onOpenPermissionSettings: (PermissionKind) -> Unit = {},
)

/**
 * The privacy dashboard: what Taqvim stores on this device, which permissions are granted, and clearing of caches, the
 * chosen location, diagnostics and recent searches after a confirmation.
 */
class PrivacyViewModel(
    private val data: PrivacyDataSource,
    private val permissions: PermissionStatusSource,
    languages: BackupLanguageSource,
) : ViewModel() {
    private val state = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = state.asStateFlow()

    private val confirm = MutableStateFlow<StoredDataKind?>(null)
    private val failed = MutableStateFlow<StoredDataKind?>(null)

    init {
        combine(languages.language(), data.storedData(), permissions.statuses(), confirm, failed) {
            language,
            stored,
            granted,
            confirming,
            failure,
            ->
            PrivacyUiState(
                loading = false,
                data = rows(language, stored),
                permissions =
                    PermissionKind.entries
                        .map { kind -> granted.firstOrNull { it.kind == kind } ?: PermissionStatus(kind, false) }
                        .toImmutableList(),
                confirmClear = confirming,
                clearFailed = failure,
            )
        }.onEach { state.value = it }
            .launchIn(viewModelScope)
    }

    fun onClearRequested(kind: StoredDataKind) {
        if (kind.clearable) {
            failed.value = null
            confirm.value = kind
        }
    }

    fun onClearDismissed() {
        confirm.value = null
    }

    fun onClearConfirmed() {
        val kind = confirm.value ?: return
        confirm.value = null
        viewModelScope.launch { failed.value = if (data.clear(kind)) null else kind }
    }

    /** The screen is shown again, for example after the system settings: permissions may have changed. */
    fun onResume() {
        permissions.refresh()
    }

    private fun rows(
        language: LanguageSpec,
        stored: List<StoredData>,
    ): ImmutableList<StoredDataRow> =
        StoredDataKind.entries
            .map { kind ->
                val count = stored.firstOrNull { it.kind == kind }?.count ?: 0
                StoredDataRow(
                    kind = kind,
                    countText = Numerals.localizeDigits(count.toString(), language.numerals),
                    empty = count == 0,
                    canClear = kind.clearable && count > 0,
                )
            }.toImmutableList()
}
