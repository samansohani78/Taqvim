/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.NumeralSystem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Marks the first-run onboarding (T-1501) as finished; bound in `:app` over the user preferences. */
fun interface OnboardingStore {
    /** Stores that the onboarding was completed or skipped, so it is not shown again. */
    suspend fun complete()
}

/** The onboarding pages, in order (docs/PLAN.md T-1501: language, location, event sources). */
enum class OnboardingStep {
    LANGUAGE,
    LOCATION,
    EVENT_SOURCES,
}

/** A launch language offered on the language page, named in its own language. */
@Immutable
data class LanguageOption(
    val code: String,
    val nativeName: String,
    val selected: Boolean,
)

/** An event source offered on the event sources page. */
@Immutable
data class SourceOption(
    val source: EventSource,
    @param:StringRes val label: Int,
    val checked: Boolean,
)

/** State of the first-run onboarding (T-1501). */
data class OnboardingUiState(
    val loading: Boolean = true,
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val languages: ImmutableList<LanguageOption> = persistentListOf(),
    val sources: ImmutableList<SourceOption> = persistentListOf(),
    /** Digits of the chosen language, for the page counter. */
    val numerals: NumeralSystem = NumeralSystem.LATIN,
    /** Whether the onboarding was completed or skipped. */
    val finished: Boolean = false,
)

/** What the onboarding reports to its [OnboardingViewModel]. */
@Immutable
data class OnboardingActions(
    val onLanguageSelected: (String) -> Unit = {},
    val onSourceToggled: (EventSource) -> Unit = {},
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
)

/** Test tags of the onboarding controls. */
internal object OnboardingTags {
    const val NEXT = "onboarding:next"
    const val BACK = "onboarding:back"
    const val SKIP = "onboarding:skip"

    fun language(code: String): String = "onboarding:language:$code"

    fun source(name: String): String = "onboarding:source:$name"
}
