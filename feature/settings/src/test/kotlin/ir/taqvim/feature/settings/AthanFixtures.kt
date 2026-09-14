/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.LanguageSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Synthetic athan settings and fakes of the athan settings ports. */
internal object AthanFixtures {
    const val SOUND_URI: String = "content://media/external/audio/media/9"

    val defaults: AthanSettings =
        AthanSettings(
            alerts = AthanPrayerKind.entries.associateWith { PrayerAlert(enabled = false, gapMinutes = 0) },
            sound = null,
            vibrate = true,
            bypassDndForFajr = false,
            volumePercent = 80,
            useIranTime = false,
        )

    /** Fajr 10 minutes early and Maghrib on time, the other prayers off. */
    val someOn: AthanSettings =
        defaults.copy(
            alerts =
                defaults.alerts +
                    mapOf(
                        AthanPrayerKind.FAJR to PrayerAlert(enabled = true, gapMinutes = -10),
                        AthanPrayerKind.MAGHRIB to PrayerAlert(enabled = true, gapMinutes = 0),
                    ),
        )

    /** Content URIs are readable and named; anything else cannot be used. */
    val library =
        AthanSoundLibrary { uri ->
            if (uri.startsWith("content://")) AthanSoundChoice(uri, "Tehran athan") else null
        }
}

/** Remembers the settings after every update and emits them back. */
internal class FakeAthanStore(
    language: LanguageSpec,
    settings: AthanSettings = AthanFixtures.defaults,
) : AthanSettingsStore {
    private val state = MutableStateFlow(AthanSettingsData(language, settings))
    var updates: Int = 0
        private set

    val current: AthanSettings
        get() = state.value.settings

    override fun settings(): Flow<AthanSettingsData> = state

    override suspend fun update(transform: (AthanSettings) -> AthanSettings) {
        updates++
        state.update { it.copy(settings = transform(it.settings)) }
    }
}

internal class FakeExactAlarms(
    allowed: Boolean = true,
) : ExactAlarmAccess {
    val state = MutableStateFlow(allowed)

    override fun allowed(): Flow<Boolean> = state
}

/** Records previews; [finish] ends the one playing. */
internal class FakePreview(
    private val starts: Boolean = true,
) : AthanPreview {
    val plays = mutableListOf<Pair<AthanSoundChoice?, Int>>()
    var stops: Int = 0
        private set
    private var onFinished: (() -> Unit)? = null

    override fun play(
        sound: AthanSoundChoice?,
        volumePercent: Int,
        onFinished: () -> Unit,
    ): Boolean {
        plays += sound to volumePercent
        this.onFinished = onFinished
        return starts
    }

    override fun stop() {
        stops++
    }

    fun finish() {
        onFinished?.invoke()
    }
}
