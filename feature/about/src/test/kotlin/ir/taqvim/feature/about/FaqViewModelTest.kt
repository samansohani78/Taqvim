/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1901: opening the FAQ, searching it, expanding answers and opening an answer's screen of the app. */
@OptIn(ExperimentalCoroutinesApi::class)
class FaqViewModelTest {
    private val texts: Map<FaqEntry, FaqText> =
        FaqEntry.entries.associateWith { entry ->
            FaqText("topic", "question ${entry.name.lowercase()}", "answer")
        } +
            (FaqEntry.PRIVACY to FaqText("حریم خصوصی", "آیا داده‌ها فرستاده می‌شوند؟", "خیر"))

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): AboutViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return AboutViewModel(
            { MutableStateFlow(AboutFixtures.info) },
            { MutableStateFlow(emptyList()) },
            FakeLicenses(AboutFixtures.catalog),
            { AboutFixtures.device },
            FakeCrashes(),
        )
    }

    private suspend fun ReceiveTurbine<AboutUiState>.awaitUntil(predicate: (AboutUiState) -> Boolean): AboutUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    @Test
    fun `the FAQ opens from the home page, filters by query and returns on back`(): Unit =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                awaitUntil { it.info != null }
                vm.onOpenFaq(texts)
                val opened = awaitUntil { it.page == AboutPage.FAQ }
                opened.canGoBack shouldBe true
                opened.faq.groups
                    .flatMap { it.entries }
                    .size shouldBe FaqEntry.entries.size

                vm.onFaqQuery("داده ها فرستاده ميشوند")
                val filtered = awaitUntil { it.faq.query.isNotEmpty() }
                filtered.faq.groups.flatMap { it.entries } shouldContainExactly listOf(FaqEntry.PRIVACY)

                vm.onFaqQuery("nothing like this")
                awaitUntil { it.faq.query == "nothing like this" }.faq.noResults shouldBe true

                vm.onBack() shouldBe true
                awaitUntil { it.page == AboutPage.HOME }.canGoBack shouldBe false
                vm.onBack() shouldBe false
            }
        }

    @Test
    fun `answers expand and collapse one by one`(): Unit =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                vm.onOpenFaq(texts)
                awaitUntil { it.page == AboutPage.FAQ }
                vm.onToggleFaq(FaqEntry.ATHAN)
                vm.onToggleFaq(FaqEntry.NEPALI)
                awaitUntil { it.faq.expanded.size == 2 }.faq.expanded shouldContainExactly
                    setOf(FaqEntry.ATHAN, FaqEntry.NEPALI)
                vm.onToggleFaq(FaqEntry.ATHAN)
                awaitUntil { it.faq.expanded.size == 1 }.faq.expanded shouldContainExactly setOf(FaqEntry.NEPALI)
            }
        }

    @Test
    fun `an answer link opens its screen of the app once and answers without a link do nothing`(): Unit =
        runTest {
            val vm = viewModel()
            vm.effects.test {
                vm.onOpenFaqLink(FaqEntry.NEPALI)
                vm.onOpenFaqLink(FaqEntry.PRIVACY)
                awaitItem() shouldBe AboutEffect.OpenInApp("taqvim://settings/privacy")
                vm.onOpenFaqLink(FaqEntry.HIGH_LATITUDE)
                awaitItem() shouldBe AboutEffect.OpenInApp("taqvim://settings/high-latitude")
                expectNoEvents()
            }
        }
}
