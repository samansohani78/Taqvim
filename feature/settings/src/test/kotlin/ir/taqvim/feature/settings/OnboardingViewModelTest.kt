/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1501: the onboarding pages, the language and source choices, completing and skipping. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val onboarding = FakeOnboardingStore()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(store: FakeGeneralSettingsStore): OnboardingViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return OnboardingViewModel(store, onboarding)
    }

    @Test
    fun `the language page lists every launch language with the stored one selected`(): Unit =
        runTest {
            val model = viewModel(FakeGeneralSettingsStore(LocationFixtures.persian))
            advanceUntilIdle()

            val state = model.uiState.value
            state.loading shouldBe false
            state.step shouldBe OnboardingStep.LANGUAGE
            state.numerals shouldBe NumeralSystem.PERSIAN
            state.languages.map { it.code } shouldBe LanguageTable.languages.map { it.code }
            state.languages.filter { it.selected }.map { it.code } shouldBe listOf("fa")
            state.languages.first { it.code == "ne" }.nativeName shouldBe
                requireNotNull(LanguageTable.forCode("ne")).nativeName
            state.sources.map { it.source } shouldBe SettingsLabels.eventSources.keys.toList()
            state.sources
                .filter { it.checked }
                .map { it.source }
                .toSet() shouldBe
                setOf(EventSource.IRAN_OFFICIAL, EventSource.INTERNATIONAL)
        }

    @Test
    fun `choosing a language and toggling sources are stored`(): Unit =
        runTest {
            val store = FakeGeneralSettingsStore(LocationFixtures.persian)
            val model = viewModel(store)
            advanceUntilIdle()

            model.onLanguageSelected("ckb")
            model.onSourceToggled(EventSource.ANCIENT_IRAN)
            model.onSourceToggled(EventSource.IRAN_OFFICIAL)
            advanceUntilIdle()

            store.current.languageCode shouldBe "ckb"
            store.current.enabledEventSources shouldBe setOf(EventSource.INTERNATIONAL, EventSource.ANCIENT_IRAN)
            model.uiState.value.languages
                .single { it.selected }
                .code shouldBe "ckb"

            val before = store.current
            model.onLanguageSelected("ckb")
            model.onLanguageSelected("xx")
            advanceUntilIdle()
            store.current shouldBe before
        }

    @Test
    fun `next walks the pages, back returns and completing stores it once`(): Unit =
        runTest {
            val model = viewModel(FakeGeneralSettingsStore(LocationFixtures.english))
            advanceUntilIdle()

            model.uiState.test {
                awaitItem().step shouldBe OnboardingStep.LANGUAGE
                model.onNext()
                awaitItem().step shouldBe OnboardingStep.LOCATION
                model.onNext()
                awaitItem().step shouldBe OnboardingStep.EVENT_SOURCES
                model.onBack()
                awaitItem().step shouldBe OnboardingStep.LOCATION
                model.onNext()
                awaitItem().step shouldBe OnboardingStep.EVENT_SOURCES
                model.onNext()
                awaitItem().finished shouldBe true
                model.onNext()
                model.onSkip()
                advanceUntilIdle()
                expectNoEvents()
            }
            onboarding.completions shouldBe 1
        }

    @Test
    fun `skipping or going back from the first page finishes the onboarding`(): Unit =
        runTest {
            val skipped = viewModel(FakeGeneralSettingsStore(LocationFixtures.english))
            advanceUntilIdle()
            skipped.onSkip()
            advanceUntilIdle()
            skipped.uiState.value.finished shouldBe true
            skipped.uiState.value.step shouldBe OnboardingStep.LANGUAGE

            val backed = OnboardingViewModel(FakeGeneralSettingsStore(LocationFixtures.english), onboarding)
            advanceUntilIdle()
            backed.onBack()
            advanceUntilIdle()
            backed.uiState.value.finished shouldBe true
            onboarding.completions shouldBe 2
        }
}

/** Counts completions. */
internal class FakeOnboardingStore : OnboardingStore {
    var completions: Int = 0
        private set

    override suspend fun complete() {
        completions++
    }
}
