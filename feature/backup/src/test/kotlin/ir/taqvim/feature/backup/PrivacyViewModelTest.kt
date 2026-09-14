/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.backup

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
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

/** T-1503: the dashboard lists every kind of data and permission, and clears only after confirmation. */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyViewModelTest {
    private val data = FakePrivacyData()
    private val permissions = FakePermissions()

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): PrivacyViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return PrivacyViewModel(data, permissions, FakeLanguages(BackupFixtures.persian))
    }

    @Test
    fun `every kind of data and permission is listed in order, missing ones as empty or not allowed`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                awaitItem().loading shouldBe true
                val loaded = awaitItem()
                loaded shouldBe BackupFixtures.privacyState(BackupFixtures.persian)
                loaded.data.first { it.kind == StoredDataKind.DEVICE_CALENDAR_CACHE }.countText shouldBe "۱۴۰"
                loaded.data.first { it.kind == StoredDataKind.PERSONAL_EVENTS }.canClear shouldBe false
                loaded.data.first { it.kind == StoredDataKind.DIAGNOSTICS }.canClear shouldBe false
                loaded.permissions.filter { it.granted }.map { it.kind } shouldBe
                    listOf(PermissionKind.LOCATION, PermissionKind.NOTIFICATIONS)
            }
        }

    @Test
    fun `data is cleared only after confirmation, and failures are shown`(): Unit =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onClearRequested(StoredDataKind.PERSONAL_EVENTS)
            viewModel.onClearConfirmed()
            advanceUntilIdle()
            viewModel.uiState.value.confirmClear shouldBe null
            data.cleared shouldBe emptyList()

            viewModel.onClearRequested(StoredDataKind.RECENT_SEARCHES)
            advanceUntilIdle()
            viewModel.uiState.value.confirmClear shouldBe StoredDataKind.RECENT_SEARCHES
            viewModel.onClearDismissed()
            advanceUntilIdle()
            viewModel.uiState.value.confirmClear shouldBe null

            data.clearSucceeds = false
            viewModel.onClearRequested(StoredDataKind.LOCATION)
            viewModel.onClearConfirmed()
            advanceUntilIdle()
            viewModel.uiState.value.clearFailed shouldBe StoredDataKind.LOCATION

            data.clearSucceeds = true
            viewModel.onClearRequested(StoredDataKind.LOCATION)
            advanceUntilIdle()
            viewModel.uiState.value.clearFailed shouldBe null
            viewModel.onClearConfirmed()
            advanceUntilIdle()
            data.cleared shouldBe listOf(StoredDataKind.LOCATION, StoredDataKind.LOCATION)
            viewModel.uiState.value.data
                .first { it.kind == StoredDataKind.LOCATION }
                .let {
                    it.empty shouldBe true
                    it.canClear shouldBe false
                }
        }

    @Test
    fun `permissions are checked again when the dashboard is shown`(): Unit =
        runTest {
            val viewModel = viewModel()
            viewModel.onResume()
            permissions.state.value = PermissionKind.entries.map { PermissionStatus(it, true) }
            advanceUntilIdle()

            permissions.refreshes shouldBe 1
            viewModel.uiState.value.permissions
                .all { it.granted } shouldBe true
        }
}
