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
    /** The countdown section's choices (T-1212), or `null` for other widgets or while they are unavailable. */
    val countdown: WidgetCountdownChoices? = null,
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
    val countdown: WidgetCountdownActions = WidgetCountdownActions(),
)

/** What the countdown section of the configuration screen reports (T-1212). */
data class WidgetCountdownActions(
    val onMode: (CountdownMode) -> Unit = {},
    val onTitle: (String) -> Unit = {},
    val onCalendar: (CalendarSystem) -> Unit = {},
    val onDate: (year: Int, month: Int, day: Int) -> Unit = { _, _, _ -> },
    val onRepeats: (Boolean) -> Unit = {},
    val onOccasion: (WidgetOccasion) -> Unit = {},
    /** Days in a month of the countdown's calendar, for the date picker. */
    val daysInMonth: (year: Int, month: Int) -> Int = { _, _ -> DEFAULT_MONTH_LENGTH },
)

private const val DEFAULT_MONTH_LENGTH = 30

private enum class SaveStatus { IDLE, SAVING, FAILED, SAVED }

/**
 * Per-widget configuration (T-1200): background, transparency, scale, optional parts and secondary calendar of widget
 * [appWidgetId] of [kind], and the countdown widget's date (T-1212) from [countdownSource]. Saving stores the
 * normalized configuration and redraws only that widget.
 */
class WidgetConfigViewModel(
    private val appWidgetId: Int,
    private val kind: WidgetKind,
    private val store: WidgetConfigStore,
    calendarsSource: WidgetCalendarsSource,
    private val refresher: WidgetRefresher,
    private val countdownSource: WidgetCountdownSource? = null,
) : ViewModel() {
    private val draft = MutableStateFlow<WidgetConfig?>(null)
    private val status = MutableStateFlow(SaveStatus.IDLE)
    private val options = MutableStateFlow<WidgetCountdownOptions?>(null)

    val uiState: StateFlow<WidgetConfigUiState> =
        combine(
            draft,
            calendarsSource.calendars(),
            status,
            options,
        ) { config, calendars, saveStatus, countdownOptions ->
            val shown = config ?: WidgetConfig.defaultFor(kind)
            WidgetConfigUiState(
                kind = kind,
                config = shown,
                calendars = calendars.toImmutableList(),
                countdown = shown.countdown?.let { countdownOptions?.choices(it) },
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
            val loaded = if (kind == WidgetKind.COUNTDOWN) loadOptions() else null
            options.value = loaded
            val countdown = stored.countdown ?: loaded?.defaultCountdown()
            draft.compareAndSet(null, stored.copy(countdown = countdown).normalizedFor(kind))
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

    fun onCountdownMode(mode: CountdownMode) {
        editCountdown { copy(mode = mode) }
    }

    fun onCountdownTitle(title: String) {
        editCountdown { copy(title = title) }
    }

    /** Moves the chosen day into [calendar], keeping the same civil day. */
    fun onCountdownCalendar(calendar: CalendarSystem) {
        val current = options.value ?: return
        editCountdown { current.inCalendar(this, calendar) }
    }

    /** A new date restarts a one-off countdown's progress from today. */
    fun onCountdownDate(
        year: Int,
        month: Int,
        day: Int,
    ) {
        editCountdown { copy(year = year, month = month, day = day, startJdn = today(startJdn)) }
    }

    fun onCountdownRepeats(repeats: Boolean) {
        editCountdown { copy(repeatsYearly = repeats) }
    }

    fun onCountdownOccasion(occasion: WidgetOccasion) {
        editCountdown {
            copy(
                calendar = occasion.calendar,
                year = occasion.year,
                month = occasion.month,
                day = occasion.day,
                startJdn = today(startJdn),
                mode = CountdownMode.UNTIL,
                repeatsYearly = occasion.repeatsYearly,
                title = occasion.title,
            )
        }
    }

    /** Days in [month] of [year] in the countdown's calendar, for the date picker; 30 when it cannot be computed. */
    fun countdownDaysInMonth(
        year: Int,
        month: Int,
    ): Int {
        val current = options.value
        val countdown = draft.value?.countdown
        if (current == null || countdown == null) return FALLBACK_MONTH_LENGTH
        val calendar = current.arithmetic(countdown.calendar)
        return runCatching { calendar.monthLength(year, month.coerceIn(1, calendar.monthsInYear(year))) }
            .getOrDefault(FALLBACK_MONTH_LENGTH)
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

    /** The countdown choices, or `null` without a source or when it fails (the widget then asks for a date). */
    private suspend fun loadOptions(): WidgetCountdownOptions? {
        val source = countdownSource ?: return null
        val result = runCatching { source.options() }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        return result.getOrNull()
    }

    private fun today(fallback: Long): Long = options.value?.today?.value ?: fallback

    private fun editCountdown(change: WidgetCountdown.() -> WidgetCountdown) {
        edit { copy(countdown = countdown?.change()) }
    }

    private fun edit(change: WidgetConfig.() -> WidgetConfig) {
        draft.update { current -> current?.change()?.normalizedFor(kind) }
        status.update { if (it == SaveStatus.FAILED) SaveStatus.IDLE else it }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val FALLBACK_MONTH_LENGTH = 30
    }
}
