/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * First-run onboarding (T-1501): three skippable pages — language, location and event sources. Choosing a language
 * stores it through [store], whose `:app` binding applies that language's defaults to everything the user has not
 * changed (ADR-0023); the location page is the T-1502 location settings. Finishing or skipping stores completion in
 * [onboarding].
 */
class OnboardingViewModel(
    private val store: GeneralSettingsStore,
    private val onboarding: OnboardingStore,
) : ViewModel() {
    private val state = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    init {
        store
            .settings()
            .onEach { data ->
                val settings = data.settings
                state.update { current ->
                    current.copy(
                        loading = false,
                        numerals = data.language.numerals,
                        languages =
                            LanguageTable.languages
                                .map { LanguageOption(it.code, it.nativeName, it.code == settings.languageCode) }
                                .toImmutableList(),
                        sources =
                            SettingsLabels.eventSources
                                .map { (source, label) ->
                                    SourceOption(source, label, source in settings.enabledEventSources)
                                }.toImmutableList(),
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onLanguageSelected(code: String) {
        if (state.value.languages.none { it.code == code && !it.selected }) return
        viewModelScope.launch { store.update { it.copy(languageCode = code) } }
    }

    fun onSourceToggled(source: EventSource) {
        viewModelScope.launch {
            store.update { settings ->
                val sources = settings.enabledEventSources
                settings.copy(enabledEventSources = if (source in sources) sources - source else sources + source)
            }
        }
    }

    /** The next page, or finishing on the last one. */
    fun onNext() {
        val next = OnboardingStep.entries.getOrNull(state.value.step.ordinal + 1)
        if (next == null) finish() else state.update { it.copy(step = next) }
    }

    /** The previous page; on the first page back skips the onboarding. */
    fun onBack() {
        val previous = OnboardingStep.entries.getOrNull(state.value.step.ordinal - 1)
        if (previous == null) finish() else state.update { it.copy(step = previous) }
    }

    fun onSkip() {
        finish()
    }

    private fun finish() {
        if (state.value.finished) return
        state.update { it.copy(finished = true) }
        viewModelScope.launch { onboarding.complete() }
    }
}
