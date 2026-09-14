/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1503: exports and imports go through their ports, every failure is shown, and passphrases are always wiped. */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val operations = FakeOperations()
    private val languages = FakeLanguages(BackupFixtures.persian)

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): BackupViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return BackupViewModel(operations, languages, BackupFixtures.tehran)
    }

    private fun CharArray.wiped(): Boolean = all { it == Char.MIN_VALUE }

    @Test
    fun `encrypted exports carry the passphrase once, report the size and wipe it`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitItem().loading shouldBe true
                awaitItem().export shouldBe ExportState(encrypt = true)
            }
            val passphrase = BackupFixtures.PASSPHRASE.toCharArray()

            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, passphrase)
            advanceUntilIdle()

            operations.exports shouldBe listOf(BackupFixtures.DESTINATION_URI to BackupFixtures.PASSPHRASE)
            passphrase.wiped() shouldBe true
            viewModel.uiState.value.export shouldBe ExportState(encrypt = true, exportedSizeText = "۱۳")
            viewModel.onExportMessageDismissed()
            advanceUntilIdle()
            viewModel.uiState.value.export shouldBe ExportState(encrypt = true)
        }

    @Test
    fun `plain exports send no passphrase, failures are shown and short passphrases never leave`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onEncryptChanged(false)
            val ignored = "left over".toCharArray()
            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, ignored)
            advanceUntilIdle()
            operations.exports shouldBe listOf(BackupFixtures.DESTINATION_URI to null)
            ignored.wiped() shouldBe true

            operations.exportResult = BackupExportResult.Failed(BackupFailure.FILE_UNWRITABLE)
            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, CharArray(0))
            advanceUntilIdle()
            viewModel.uiState.value.export.failure shouldBe BackupFailure.FILE_UNWRITABLE

            viewModel.onEncryptChanged(true)
            advanceUntilIdle()
            viewModel.uiState.value.export shouldBe ExportState(encrypt = true)
            val short = "short".toCharArray()
            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, short)
            advanceUntilIdle()
            operations.exports.size shouldBe 2
            short.wiped() shouldBe true
            viewModel.uiState.value.export.failure shouldBe BackupFailure.PASSPHRASE_REQUIRED
        }

    @Test
    fun `a busy or cancelled export still wipes every passphrase`(): Unit =
        runTest {
            val viewModel = viewModel()
            operations.exportGate = CompletableDeferred()
            val first = BackupFixtures.PASSPHRASE.toCharArray()
            val second = BackupFixtures.PASSPHRASE.toCharArray()

            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, first)
            runCurrent()
            viewModel.uiState.value.export.working shouldBe true
            viewModel.onEncryptChanged(false)
            viewModel.onExportDestination(BackupFixtures.DESTINATION_URI, second)
            runCurrent()
            second.wiped() shouldBe true
            operations.exports.size shouldBe 1
            first.wiped() shouldBe false

            viewModel.viewModelScope.cancel()
            advanceUntilIdle()
            first.wiped() shouldBe true
        }

    @Test
    fun `plain backups open to a preview and are restored only after confirmation`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onImportSource(null)
            viewModel.onRestoreConfirmed()
            advanceUntilIdle()
            operations.opens shouldBe emptyList()

            viewModel.onImportSource(BackupFixtures.SOURCE_URI)
            advanceUntilIdle()
            val preview =
                viewModel.uiState.value.import
                    .shouldBeInstanceOf<ImportStage.Preview>()
            preview.confirming shouldBe false
            preview.preview.createdText shouldBe "یکشنبه ۲۲ شهریور ۱۴۰۵"
            preview.preview.languageName shouldBe BackupFixtures.persian.nativeName
            preview.preview.encrypted shouldBe false
            preview.preview.rows.map { it.table } shouldBe BackupTableKind.entries
            preview.preview.rows.map { it.countText } shouldBe listOf("۱۲", "۳", "۲۰", "۰", "۰", "۲", "۰")

            viewModel.onRestoreRequested()
            advanceUntilIdle()
            (viewModel.uiState.value.import as ImportStage.Preview).confirming shouldBe true
            viewModel.onRestoreDismissed()
            viewModel.onRestoreRequested()
            viewModel.onRestoreConfirmed()
            advanceUntilIdle()

            operations.restores shouldBe 1
            viewModel.uiState.value.import shouldBe ImportStage.Restored("۳۷")
            viewModel.onImportDismissed()
            advanceUntilIdle()
            viewModel.uiState.value.import shouldBe ImportStage.Idle
        }

    @Test
    fun `encrypted backups ask for the passphrase, allow retries and wipe every attempt`(): Unit =
        runTest {
            operations.encrypted()
            val viewModel = viewModel()
            val early = "early passphrase".toCharArray()
            viewModel.onImportPassphrase(early)
            early.wiped() shouldBe true

            viewModel.onImportSource(BackupFixtures.SOURCE_URI)
            advanceUntilIdle()
            viewModel.uiState.value.import shouldBe ImportStage.NeedsPassphrase(wrongPassphrase = false)

            val wrong = "not the passphrase".toCharArray()
            viewModel.onImportPassphrase(wrong)
            advanceUntilIdle()
            viewModel.uiState.value.import shouldBe ImportStage.NeedsPassphrase(wrongPassphrase = true)
            wrong.wiped() shouldBe true

            val right = BackupFixtures.PASSPHRASE.toCharArray()
            viewModel.onImportPassphrase(right)
            advanceUntilIdle()
            viewModel.uiState.value.import
                .shouldBeInstanceOf<ImportStage.Preview>()
                .preview.encrypted shouldBe true
            right.wiped() shouldBe true
            operations.opens.map { it.second } shouldBe listOf(null, "not the passphrase", BackupFixtures.PASSPHRASE)
            operations.passphraseArrays.all { it.wiped() } shouldBe true
        }

    @Test
    fun `every read and restore failure is shown and forgets the file`(): Unit =
        runTest {
            val viewModel = viewModel()
            val readFailures =
                BackupFailure.entries -
                    setOf(
                        BackupFailure.PASSPHRASE_REQUIRED,
                        BackupFailure.WRONG_PASSPHRASE,
                        BackupFailure.RESTORE_FAILED,
                        BackupFailure.FILE_UNWRITABLE,
                    )
            readFailures.forEach { failure ->
                operations.openResult = { BackupOpenResult.Failed(failure) }
                viewModel.onImportSource(BackupFixtures.SOURCE_URI)
                advanceUntilIdle()
                withClue(failure) { viewModel.uiState.value.import shouldBe ImportStage.Failed(failure) }
                viewModel.onImportPassphrase(BackupFixtures.PASSPHRASE.toCharArray())
                viewModel.onImportDismissed()
                advanceUntilIdle()
            }
            operations.opens.size shouldBe readFailures.size

            operations.openResult = { BackupOpenResult.Ready(FakeBackup(BackupFixtures.plainSummary)) }
            operations.restoreResult = BackupRestoreResult.Failed(BackupFailure.RESTORE_FAILED)
            viewModel.onImportSource(BackupFixtures.SOURCE_URI)
            advanceUntilIdle()
            viewModel.onRestoreConfirmed()
            advanceUntilIdle()
            viewModel.uiState.value.import shouldBe ImportStage.Failed(BackupFailure.RESTORE_FAILED)
            viewModel.onRestoreConfirmed()
            advanceUntilIdle()
            operations.restores shouldBe 1
        }

    @Test
    fun `a language change formats the preview again`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onImportSource(BackupFixtures.SOURCE_URI)
            advanceUntilIdle()

            languages.state.value = BackupFixtures.english
            advanceUntilIdle()

            val preview =
                viewModel.uiState.value.import
                    .shouldBeInstanceOf<ImportStage.Preview>()
                    .preview
            preview.createdText shouldBe "Sunday, September 13, 2026"
            preview.formatText shouldBe "1"
            preview.rows.first().countText shouldBe "12"
            BackupStateMapper.kilobytes(0) shouldBe 0
            BackupStateMapper.kilobytes(1) shouldBe 1
            BackupStateMapper.kilobytes(2_048) shouldBe 2
        }
}
