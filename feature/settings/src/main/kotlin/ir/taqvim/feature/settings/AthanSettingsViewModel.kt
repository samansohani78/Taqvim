/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Athan settings (T-1101): per-prayer athan and gap, sound (picked file or default, with a preview), volume, vibration,
 * Do Not Disturb bypass for Fajr and Iran time. Every change is stored immediately; dragging the volume is stored once
 * the drag pauses.
 */
@OptIn(FlowPreview::class)
class AthanSettingsViewModel(
    private val store: AthanSettingsStore,
    exactAlarms: ExactAlarmAccess,
    private val soundLibrary: AthanSoundLibrary,
    private val preview: AthanPreview,
) : ViewModel() {
    private val state = MutableStateFlow(AthanSettingsUiState())
    val uiState: StateFlow<AthanSettingsUiState> = state.asStateFlow()

    private val latest = MutableStateFlow<AthanSettingsData?>(null)
    private val previewing = MutableStateFlow(false)
    private val soundUnreadable = MutableStateFlow(false)
    private val volumeDraft = MutableStateFlow<Int?>(null)

    init {
        val data = store.settings().onEach { latest.value = it }
        combine(data, exactAlarms.allowed(), previewing, soundUnreadable, volumeDraft, ::AthanScreenInputs)
            .onEach { state.value = AthanStateMapper.state(it) }
            .launchIn(viewModelScope)
        volumeDraft
            .filterNotNull()
            .debounce(VOLUME_DEBOUNCE_MILLIS)
            .onEach { volume -> store.update { it.copy(volumePercent = volume) } }
            .launchIn(viewModelScope)
    }

    fun onAlertToggled(
        prayer: AthanPrayerKind,
        enabled: Boolean,
    ) {
        updateAlert(prayer) { it.copy(enabled = enabled) }
    }

    fun onGapStep(
        prayer: AthanPrayerKind,
        minutes: Int,
    ) {
        updateAlert(prayer) { it.copy(gapMinutes = (it.gapMinutes + minutes).coerceIn(PrayerAlert.GAP_RANGE)) }
    }

    /** A file picked in the system picker; `null` when the picker was cancelled. */
    fun onSoundPicked(uri: String?) {
        if (uri == null) return
        viewModelScope.launch {
            val sound = soundLibrary.adopt(uri)
            soundUnreadable.value = sound == null
            if (sound != null) store.update { it.copy(sound = sound) }
        }
    }

    fun onUseDefaultSound() {
        soundUnreadable.value = false
        updateSettings { it.copy(sound = null) }
    }

    /** Starts the preview, or stops it while it plays. */
    fun onPreview() {
        if (previewing.value) {
            preview.stop()
            previewing.value = false
            return
        }
        val settings = latest.value?.settings ?: return
        val volume = volumeDraft.value ?: settings.volumePercent
        previewing.value = preview.play(settings.sound, volume) { previewing.value = false }
    }

    fun onVolumeChanged(percent: Int) {
        volumeDraft.value = percent.coerceIn(VOLUME_RANGE)
    }

    fun onVibrateChanged(enabled: Boolean) {
        updateSettings { it.copy(vibrate = enabled) }
    }

    fun onBypassDndChanged(enabled: Boolean) {
        updateSettings { it.copy(bypassDndForFajr = enabled) }
    }

    fun onIranTimeChanged(enabled: Boolean) {
        updateSettings { it.copy(useIranTime = enabled) }
    }

    override fun onCleared() {
        preview.stop()
    }

    private fun updateAlert(
        prayer: AthanPrayerKind,
        transform: (PrayerAlert) -> PrayerAlert,
    ) {
        updateSettings { settings ->
            settings.copy(alerts = settings.alerts + (prayer to transform(AthanStateMapper.alertOf(settings, prayer))))
        }
    }

    private fun updateSettings(transform: (AthanSettings) -> AthanSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    private companion object {
        const val VOLUME_DEBOUNCE_MILLIS = 400L
        val VOLUME_RANGE = 0..100
    }
}
