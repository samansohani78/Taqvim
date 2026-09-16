/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.praytimes.PrayerSettings
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** Synthetic athan setups; coordinates are rounded sample inputs, not official data. */
internal object AthanFixtures {
    val TEHRAN = Coordinates(35.69, 51.42)
    val TROMSO = Coordinates(69.65, 18.96)

    fun alerts(
        enabled: Set<AthanPrayer> = AthanPrayer.entries.toSet(),
        gap: (AthanPrayer) -> Int = { 0 },
    ): Map<AthanPrayer, AthanAlertRule> = AthanPrayer.entries.associateWith { AthanAlertRule(it in enabled, gap(it)) }

    fun tehran(
        alerts: Map<AthanPrayer, AthanAlertRule> = alerts(),
        useIranTime: Boolean = false,
    ): AthanPlanSettings = AthanPlanSettings(alerts, TEHRAN, TimeZone.of("Asia/Tehran"), PrayerSettings(), useIranTime)

    fun tromso(): AthanPlanSettings =
        AthanPlanSettings(alerts(), TROMSO, TimeZone.of("Europe/Oslo"), PrayerSettings(), useIranTime = false)

    val PLAYBACK = AthanPlayback(soundUri = null, volumePercent = 80, vibrate = true, bypassDndForFajr = false)

    fun request(
        prayer: AthanPrayer = AthanPrayer.DHUHR,
        playback: AthanPlayback = PLAYBACK,
    ): AthanRequest =
        AthanRequest(PlannedAthan(prayer, Jdn(2_461_297), Instant.parse("2026-09-13T08:50:00Z")), playback)
}

/** In-memory [DeliveryLog] on [DeliveryHistory], shared by the athan and reminder tests. */
internal class HistoryDeliveryLog(
    capacity: Int = SharedPreferencesDeliveryLog.ATHAN_CAPACITY,
) : DeliveryLog {
    var history = DeliveryHistory(emptyList(), capacity)
        private set

    override suspend fun state(key: String): DeliveryState? = history.stateOf(key)

    override suspend fun record(
        key: String,
        state: DeliveryState,
    ) {
        history = history.with(key, state)
    }
}
