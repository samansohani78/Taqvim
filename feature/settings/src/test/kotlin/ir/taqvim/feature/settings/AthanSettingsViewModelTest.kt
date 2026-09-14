/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** T-1101: athan settings are shown in the app's digits, and every control stores its value. */
@OptIn(ExperimentalCoroutinesApi::class)
class AthanSettingsViewModelTest {
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(
        store: FakeAthanStore,
        exact: FakeExactAlarms = FakeExactAlarms(),
        preview: FakePreview = FakePreview(),
    ): AthanSettingsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return AthanSettingsViewModel(store, exact, AthanFixtures.library, preview)
    }

    @Test
    fun `stored settings are shown in the language's digits with the exact alarm warning`(): Unit =
        runTest {
            val store = FakeAthanStore(LocationFixtures.persian, AthanFixtures.someOn.copy(volumePercent = 55))
            val exact = FakeExactAlarms(allowed = true)
            val viewModel = viewModel(store, exact)

            viewModel.uiState.test {
                awaitItem().loading shouldBe true
                val loaded = awaitItem()
                loaded.loading shouldBe false
                loaded.alerts.map { it.prayer } shouldBe AthanPrayerKind.entries
                loaded.alerts.first() shouldBe
                    PrayerAlertRow(AthanPrayerKind.FAJR, true, -10, "۱۰", canMoveEarlier = true, canMoveLater = true)
                loaded.volumeText shouldBe "۵۵"
                loaded.bypassAvailable shouldBe true
                loaded.exactAlarmsBlocked shouldBe false
                loaded.sound shouldBe SoundState()

                exact.state.value = false
                awaitItem().exactAlarmsBlocked shouldBe true
                viewModel.onAlertToggled(AthanPrayerKind.FAJR, false)
                awaitItem().exactAlarmsBlocked shouldBe true
                viewModel.onAlertToggled(AthanPrayerKind.MAGHRIB, false)
                awaitItem().let {
                    it.exactAlarmsBlocked shouldBe false
                    it.bypassAvailable shouldBe false
                }
            }
        }

    @Test
    fun `every switch and step is stored, and gaps stay within an hour`(): Unit =
        runTest {
            val store = FakeAthanStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            advanceUntilIdle()

            viewModel.onAlertToggled(AthanPrayerKind.ASR, true)
            repeat(3) { viewModel.onGapStep(AthanPrayerKind.ASR, 1) }
            viewModel.onGapStep(AthanPrayerKind.ISHA, -70)
            viewModel.onGapStep(AthanPrayerKind.DHUHR, 90)
            viewModel.onVibrateChanged(false)
            viewModel.onBypassDndChanged(true)
            viewModel.onIranTimeChanged(true)
            advanceUntilIdle()

            store.current.alerts.getValue(AthanPrayerKind.ASR) shouldBe PrayerAlert(true, 3)
            store.current.alerts.getValue(AthanPrayerKind.ISHA) shouldBe PrayerAlert(false, -60)
            store.current.alerts.getValue(AthanPrayerKind.DHUHR) shouldBe PrayerAlert(false, 60)
            store.current.vibrate shouldBe false
            store.current.bypassDndForFajr shouldBe true
            store.current.useIranTime shouldBe true
            viewModel.uiState.value.alerts
                .first { it.prayer == AthanPrayerKind.DHUHR }
                .let {
                    it.canMoveLater shouldBe false
                    it.canMoveEarlier shouldBe true
                    it.gapText shouldBe "60"
                }
        }

    @Test
    fun `picked sounds are adopted, unusable files are flagged and cancelled picks change nothing`(): Unit =
        runTest {
            val store = FakeAthanStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            advanceUntilIdle()

            viewModel.onSoundPicked(null)
            advanceUntilIdle()
            store.updates shouldBe 0

            viewModel.onSoundPicked(AthanFixtures.SOUND_URI)
            advanceUntilIdle()
            store.current.sound shouldBe AthanSoundChoice(AthanFixtures.SOUND_URI, "Tehran athan")
            viewModel.uiState.value.sound shouldBe SoundState(custom = true, name = "Tehran athan")

            viewModel.onSoundPicked("file:///sdcard/unknown.bin")
            advanceUntilIdle()
            store.current.sound shouldBe AthanSoundChoice(AthanFixtures.SOUND_URI, "Tehran athan")
            viewModel.uiState.value.sound.unreadable shouldBe true

            viewModel.onUseDefaultSound()
            advanceUntilIdle()
            store.current.sound shouldBe null
            viewModel.uiState.value.sound shouldBe SoundState()
        }

    @Test
    fun `dragging the volume shows every value but stores it once the drag pauses`(): Unit =
        runTest {
            val store = FakeAthanStore(LocationFixtures.english)
            val viewModel = viewModel(store)
            advanceUntilIdle()

            listOf(10, 20, 150).forEach { volume ->
                viewModel.onVolumeChanged(volume)
                advanceTimeBy(100)
                viewModel.uiState.value.volumePercent shouldBe volume.coerceAtMost(100)
            }
            store.updates shouldBe 0
            advanceTimeBy(1_000)

            store.updates shouldBe 1
            store.current.volumePercent shouldBe 100
            viewModel.onVolumeChanged(-5)
            advanceUntilIdle()
            store.current.volumePercent shouldBe 0
        }

    @Test
    fun `the preview plays the chosen sound at the shown volume and stops`(): Unit =
        runTest {
            val store = FakeAthanStore(LocationFixtures.english, AthanFixtures.defaults.copy(volumePercent = 40))
            val preview = FakePreview()
            val viewModel = viewModel(store, preview = preview)
            viewModel.onPreview()
            preview.plays shouldBe emptyList()
            advanceUntilIdle()

            viewModel.onVolumeChanged(65)
            viewModel.onPreview()
            advanceUntilIdle()
            preview.plays shouldBe listOf(null to 65)
            viewModel.uiState.value.sound.previewing shouldBe true

            viewModel.onPreview()
            advanceUntilIdle()
            preview.stops shouldBe 1
            viewModel.uiState.value.sound.previewing shouldBe false

            viewModel.onPreview()
            preview.finish()
            advanceUntilIdle()
            viewModel.uiState.value.sound.previewing shouldBe false

            val silent = FakePreview(starts = false)
            val other = AthanSettingsViewModel(store, FakeExactAlarms(), AthanFixtures.library, silent)
            advanceUntilIdle()
            other.onPreview()
            advanceUntilIdle()
            silent.plays.size shouldBe 1
            other.uiState.value.sound.previewing shouldBe false
        }
}
