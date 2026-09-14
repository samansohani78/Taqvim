/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.CalendarSystem
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The configuration screen of one placed widget. */
data class WidgetConfigUiState(
    val kind: WidgetKind,
    val config: WidgetConfig,
    /** Calendars offered as the secondary calendar. */
    val calendars: ImmutableList<CalendarSystem> = persistentListOf(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    /** The configuration was stored and the widget redrawn; the screen closes. */
    val saved: Boolean = false,
)

/** What the configuration screen reports. */
data class WidgetConfigActions(
    val onBackground: (WidgetBackground) -> Unit = {},
    val onTransparency: (Int) -> Unit = {},
    val onScale: (Int) -> Unit = {},
    val onContent: (WidgetContent, Boolean) -> Unit = { _, _ -> },
    val onSecondaryCalendar: (CalendarSystem?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
)

private enum class SaveStatus { IDLE, SAVING, FAILED, SAVED }

/**
 * Per-widget configuration (T-1200): background, transparency, scale, optional parts and secondary calendar of widget
 * [appWidgetId] of [kind]. Saving stores the normalized configuration and redraws only that widget.
 */
class WidgetConfigViewModel(
    private val appWidgetId: Int,
    private val kind: WidgetKind,
    private val store: WidgetConfigStore,
    calendarsSource: WidgetCalendarsSource,
    private val refresher: WidgetRefresher,
) : ViewModel() {
    private val draft = MutableStateFlow<WidgetConfig?>(null)
    private val status = MutableStateFlow(SaveStatus.IDLE)

    val uiState: StateFlow<WidgetConfigUiState> =
        combine(draft, calendarsSource.calendars(), status) { config, calendars, saveStatus ->
            WidgetConfigUiState(
                kind = kind,
                config = config ?: WidgetConfig.defaultFor(kind),
                calendars = calendars.toImmutableList(),
                loading = config == null,
                saving = saveStatus == SaveStatus.SAVING,
                saveFailed = saveStatus == SaveStatus.FAILED,
                saved = saveStatus == SaveStatus.SAVED,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            WidgetConfigUiState(kind, WidgetConfig.defaultFor(kind)),
        )

    init {
        viewModelScope.launch {
            val stored = store.config(appWidgetId) ?: WidgetConfig.defaultFor(kind)
            draft.compareAndSet(null, stored.normalizedFor(kind))
        }
    }

    fun onBackground(background: WidgetBackground) {
        edit { copy(background = background) }
    }

    fun onTransparency(percent: Int) {
        edit { copy(transparencyPercent = percent) }
    }

    fun onScale(percent: Int) {
        edit { copy(scalePercent = percent) }
    }

    fun onContent(
        content: WidgetContent,
        shown: Boolean,
    ) {
        edit { copy(contents = (if (shown) contents + content else contents - content).toImmutableSet()) }
    }

    fun onSecondaryCalendar(calendar: CalendarSystem?) {
        edit { copy(secondaryCalendar = calendar) }
    }

    fun onSave() {
        val config = draft.value ?: return
        if (status.value == SaveStatus.SAVING || status.value == SaveStatus.SAVED) return
        status.value = SaveStatus.SAVING
        viewModelScope.launch {
            val result =
                runCatching {
                    store.save(appWidgetId, config)
                    refresher.refresh(WidgetUpdateTrigger.ConfigChanged(appWidgetId))
                }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            status.value = if (result.isSuccess) SaveStatus.SAVED else SaveStatus.FAILED
        }
    }

    private fun edit(change: WidgetConfig.() -> WidgetConfig) {
        draft.update { current -> current?.change()?.normalizedFor(kind) }
        status.update { if (it == SaveStatus.FAILED) SaveStatus.IDLE else it }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
