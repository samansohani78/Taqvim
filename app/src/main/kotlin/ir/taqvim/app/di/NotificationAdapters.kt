/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.scheduler.AlarmDelivery
import ir.taqvim.data.scheduler.AlarmKey
import ir.taqvim.data.scheduler.AlarmSource
import ir.taqvim.feature.notification.AthanAlertRule
import ir.taqvim.feature.notification.AthanPlanSettings
import ir.taqvim.feature.notification.AthanPlayback
import ir.taqvim.feature.notification.AthanPrayer as PlannedPrayer
import ir.taqvim.feature.notification.AthanSetup
import ir.taqvim.feature.notification.AthanSetupSource
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** [AthanSetupSource] (T-1102) from the stored preferences: athan settings (T-1101) and the chosen place (T-1502). */
internal class PreferencesAthanSetupSource(
    private val preferences: UserPreferencesRepository,
) : AthanSetupSource {
    override suspend fun current(): AthanSetup? = athanSetup(preferences.preferences.first())
}

/**
 * The athan setup under [preferences], or `null` without a chosen place or with a zone this device does not know. The
 * prayer settings use the stored method and Asr convention with the default high-latitude rule (no preference yet).
 */
internal fun athanSetup(preferences: UserPreferences): AthanSetup? {
    val place = preferences.place ?: return null
    val zone = runCatching { TimeZone.of(place.zoneId) }.getOrNull() ?: return null
    val athan = preferences.athan
    return AthanSetup(
        plan =
            AthanPlanSettings(
                alerts =
                    athan.alerts.entries.associate { (prayer, alert) ->
                        PlannedPrayer.valueOf(prayer.name) to AthanAlertRule(alert.enabled, alert.gapMinutes)
                    },
                place = place.coordinates,
                timeZone = zone,
                prayer = PrayerSettings(method = preferences.prayerMethod, asr = preferences.asrJuristic),
                useIranTime = athan.useIranTime,
            ),
        playback = AthanPlayback(athan.sound?.uri, athan.volumePercent, athan.vibrate, athan.bypassDndForFajr),
    )
}

/** The scheduler's prayer [AlarmSource] (T-604) over the athans due after a given instant (T-1102). */
internal class AthanAlarmSource(
    private val upcoming: suspend (now: Instant) -> List<Instant>,
) : AlarmSource {
    override val kind: AlarmKind = AlarmKind.PRAYER

    override suspend fun upcomingAlarms(now: Instant): List<AlarmKey> =
        upcoming(now).map { AlarmKey(AlarmKind.PRAYER, sourceId = null, triggerAt = it) }
}

/** The scheduler's prayer [AlarmDelivery] (T-604): plays the athan planned at the alarm's trigger instant (T-1102). */
internal class AthanAlarmDelivery(
    private val onAlarm: suspend (triggerAt: Instant) -> Unit,
) : AlarmDelivery {
    override val kind: AlarmKind = AlarmKind.PRAYER

    override suspend fun deliver(alarm: ScheduledAlarmEntity) {
        onAlarm(Instant.fromEpochMilliseconds(alarm.triggerAtEpochMillis))
    }
}
