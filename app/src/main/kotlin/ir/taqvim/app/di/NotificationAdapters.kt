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
import ir.taqvim.data.scheduler.AlarmScheduler
import ir.taqvim.data.scheduler.AlarmSource
import ir.taqvim.data.scheduler.DeliveryOutcome
import ir.taqvim.data.scheduler.snoozedFrom
import ir.taqvim.data.scheduler.toKey
import ir.taqvim.feature.notification.AlarmDeliveryResult
import ir.taqvim.feature.notification.AthanAlarms
import ir.taqvim.feature.notification.AthanAlertRule
import ir.taqvim.feature.notification.AthanPlanSettings
import ir.taqvim.feature.notification.AthanPlayback
import ir.taqvim.feature.notification.AthanPrayer as PlannedPrayer
import ir.taqvim.feature.notification.AthanSetup
import ir.taqvim.feature.notification.AthanSetupSource
import ir.taqvim.feature.notification.PlannedAthan
import ir.taqvim.feature.notification.PlannedReminder
import ir.taqvim.feature.notification.SnoozeScheduler
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
                prayer = preferences.prayerSettings(),
                useIranTime = athan.useIranTime,
            ),
        playback = AthanPlayback(athan.sound?.uri, athan.volumePercent, athan.vibrate, athan.bypassDndForFajr),
    )
}

/** The scheduler's prayer [AlarmSource] (T-604) over the athans due after a given instant (T-1102). */
internal class AthanAlarmSource(
    private val alarms: AthanAlarms,
) : AlarmSource {
    override val kind: AlarmKind = AlarmKind.PRAYER

    override suspend fun upcomingAlarms(now: Instant): List<AlarmKey> =
        alarms.upcoming(now).map { AlarmKey(AlarmKind.PRAYER, sourceId = null, triggerAt = it) }

    override suspend fun keepsSnooze(snooze: ScheduledAlarmEntity): Boolean =
        snooze.snoozedFrom()?.let { alarms.isPlanned(it) } ?: false
}

/**
 * The scheduler's prayer [AlarmDelivery] (T-604, ADR-0033): plays the athan planned at the alarm's trigger instant, or
 * at the instant a snooze repeats (T-1102).
 */
internal class AthanAlarmDelivery(
    private val alarms: AthanAlarms,
) : AlarmDelivery {
    override val kind: AlarmKind = AlarmKind.PRAYER

    override suspend fun deliver(alarm: ScheduledAlarmEntity): DeliveryOutcome =
        alarms.onAlarm(alarm.plannedAt(), snoozed = alarm.snoozedFrom() != null).toOutcome()

    override suspend fun onGaveUp(alarm: ScheduledAlarmEntity) {
        alarms.onGaveUp(alarm.plannedAt())
    }
}

/** [SnoozeScheduler] on the persistent alarm scheduler (ADR-0033). */
internal class SchedulerSnoozeScheduler(
    private val scheduler: AlarmScheduler,
) : SnoozeScheduler {
    override suspend fun snoozeReminder(
        reminder: PlannedReminder,
        at: Instant,
    ) {
        scheduler.snooze(AlarmKind.REMINDER_SNOOZE, reminder.alarmSourceId, reminder.at, at)
    }

    override suspend fun snoozeAthan(
        athan: PlannedAthan,
        at: Instant,
    ) {
        scheduler.snooze(AlarmKind.PRAYER_SNOOZE, sourceId = null, snoozedFrom = athan.at, at = at)
    }
}

/** The instant whose reminder or athan [this] alarm shows: the snoozed one for snoozes, else its trigger time. */
internal fun ScheduledAlarmEntity.plannedAt(): Instant = snoozedFrom() ?: toKey().triggerAt

/** The scheduler's view of a feature delivery result. */
internal fun AlarmDeliveryResult.toOutcome(): DeliveryOutcome =
    when (this) {
        AlarmDeliveryResult.DELIVERED -> DeliveryOutcome.DELIVERED
        AlarmDeliveryResult.SKIPPED -> DeliveryOutcome.SKIPPED
        AlarmDeliveryResult.FAILED -> DeliveryOutcome.FAILED
    }
