/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {
    private val apacheText = mapOf("licenses/texts/Apache-2.0.txt" to "Apache License text")

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        licenses: LicenseCatalogSource = FakeLicenses(AboutFixtures.catalog, apacheText),
        entries: MutableStateFlow<List<DiagnosticEntry>> = MutableStateFlow(AboutFixtures.entries),
    ): AboutViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return AboutViewModel(
            { MutableStateFlow(AboutFixtures.info) },
            { entries },
            licenses,
            { AboutFixtures.device },
        )
    }

    private suspend fun ReceiveTurbine<AboutUiState>.awaitUntil(predicate: (AboutUiState) -> Boolean): AboutUiState {
        var state = awaitItem()
        while (!predicate(state)) state = awaitItem()
        return state
    }

    @Test
    fun `shows the app facts and redacted diagnostics filtered by level`(): Unit =
        runTest {
            val vm = viewModel()
            vm.uiState.test {
                val loaded = awaitUntil { it.info != null }
                loaded.info shouldBe AboutFixtures.info
                loaded.page shouldBe AboutPage.HOME
                loaded.canGoBack shouldBe false
                loaded.diagnostics.rows shouldHaveSize 4
                loaded.diagnostics.rows[2].message shouldBe "Fix at 35.68,51.38 from gps"
                val shown = loaded.diagnostics.rows.joinToString { it.tag + it.message }
                AboutFixtures.personalData.forEach { shown shouldNotContain it }

                vm.onMinimumLevel(DiagnosticLevel.WARN)
                val filtered = awaitUntil { it.diagnostics.minimum == DiagnosticLevel.WARN }
                filtered.diagnostics.rows.map { it.level } shouldBe listOf(DiagnosticLevel.ERROR, DiagnosticLevel.WARN)
                filtered.diagnostics.hidden shouldBe 2
                val text = vm.diagnosticsText()
                text.lines() shouldHaveSize 2
                AboutFixtures.personalData.forEach { text shouldNotContain it }
            }
        }

    @Test
    fun `licenses load once when opened, texts open and back returns page by page`(): Unit =
        runTest {
            val licenses = FakeLicenses(AboutFixtures.catalog, apacheText)
            val vm = viewModel(licenses)
            vm.uiState.test {
                awaitUntil { it.info != null }
                licenses.catalogCalls.get() shouldBe 0
                vm.onOpenLicenses()
                val ready = awaitUntil { it.licenses is LicensesContent.Ready }
                ready.page shouldBe AboutPage.LICENSES
                ready.canGoBack shouldBe true
                val content = ready.licenses.shouldBeInstanceOf<LicensesContent.Ready>()
                content.componentCount shouldBe 3
                content.groups.map { it.license.id } shouldBe listOf("Apache-2.0", "MIT")
                vm.onOpenLicenses()

                vm.onOpenLicense("Apache-2.0")
                awaitUntil { it.licenseText?.text != null }.licenseText?.text shouldBe "Apache License text"
                vm.onOpenLicense("MIT")
                val mit = awaitUntil { it.licenseText?.license?.id == "MIT" }.licenseText.shouldNotBeNull()
                mit.loading shouldBe false
                mit.text.shouldBeNull()
                licenses.catalogCalls.get() shouldBe 1

                vm.onBack() shouldBe true
                awaitUntil { it.page == AboutPage.LICENSES }
                vm.onBack() shouldBe true
                awaitUntil { it.page == AboutPage.HOME }.canGoBack shouldBe false
                vm.onBack() shouldBe false
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `unavailable catalog, unknown licenses and the data license`(): Unit =
        runTest {
            val unicode = mapOf("licenses/texts/Unicode-3.0.txt" to "UNICODE LICENSE V3")
            val vm = viewModel(FakeLicenses(catalog = null, texts = unicode))
            vm.uiState.test {
                awaitUntil { it.info != null }
                vm.onOpenLicenses()
                awaitUntil { it.licenses == LicensesContent.Unavailable }
                vm.onOpenLicense("Apache-2.0")
                vm.onOpenDiagnostics()
                awaitUntil { it.page == AboutPage.DIAGNOSTICS }.licenseText.shouldBeNull()

                vm.onOpenDataSources()
                awaitUntil { it.page == AboutPage.DATA_SOURCES }
                vm.onOpenDataLicense()
                awaitUntil { it.licenseText?.text != null }.licenseText?.license shouldBe DataSource.UNICODE_LICENSE
                vm.onBack()
                awaitUntil { it.page == AboutPage.DATA_SOURCES }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `report is composed only from known facts and confirmation closes`(): Unit =
        runTest {
            val vm = viewModel()
            vm.report(AboutFixtures.texts).shouldBeNull()
            vm.uiState.test {
                awaitUntil { it.info != null }
                vm.onRequestReport()
                awaitUntil { it.confirmReport }
                val report = vm.report(AboutFixtures.texts).shouldNotBeNull()
                report.recipient shouldBe AboutFixtures.SUPPORT
                report.body shouldContain "Device: Google Pixel 8"
                AboutFixtures.personalData.forEach { report.body shouldNotContain it }

                vm.onReportSent()
                awaitUntil { !it.confirmReport }
                vm.onRequestReport()
                awaitUntil { it.confirmReport }
                vm.onBack() shouldBe false
                awaitUntil { !it.confirmReport }
                vm.onRequestReport()
                awaitUntil { it.confirmReport }
                vm.onDismissReport()
                awaitUntil { !it.confirmReport }
            }
        }
}
