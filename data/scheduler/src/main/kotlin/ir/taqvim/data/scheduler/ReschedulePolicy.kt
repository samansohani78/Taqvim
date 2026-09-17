/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import android.content.Intent
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.preferences.UserPreferences

/** Why alarms have to be looked at again. */
sealed interface RescheduleEvent {
    /** The device booted; the system has dropped every alarm. */
    data object BootCompleted : RescheduleEvent

    /** The app was updated; its alarms are gone and the rules that produce them may have changed. */
    data object PackageReplaced : RescheduleEvent

    /** The wall clock was set by the user or the network. */
    data object TimeChanged : RescheduleEvent

    /** The device time zone changed. */
    data object TimeZoneChanged : RescheduleEvent

    /** The exact-alarm permission was granted or revoked (API 31+). */
    data object ExactAlarmPermissionChanged : RescheduleEvent

    /** Preferences changed from [previous] to [current]. */
    data class PreferencesChanged(
        val previous: UserPreferences,
        val current: UserPreferences,
    ) : RescheduleEvent

    /** Data that alarms of [kinds] are computed from changed, e.g. personal events or their reminders (T-1001). */
    data class AlarmInputsChanged(
        val kinds: Set<AlarmKind>,
    ) : RescheduleEvent

    companion object {
        /** `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, spelled out because it is API 31+. */
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED: String =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        /** The event announced by the system broadcast [action], or `null` for any other action. */
        fun forAction(action: String?): RescheduleEvent? =
            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> BootCompleted
                Intent.ACTION_MY_PACKAGE_REPLACED -> PackageReplaced
                Intent.ACTION_TIME_CHANGED -> TimeChanged
                Intent.ACTION_TIMEZONE_CHANGED -> TimeZoneChanged
                ACTION_EXACT_ALARM_PERMISSION_CHANGED -> ExactAlarmPermissionChanged
                else -> null
            }
    }
}

/** What a [RescheduleEvent] requires: [restore] re-registers stored alarms, [recompute] asks their sources again. */
data class ReschedulePlan(
    val restore: Set<AlarmKind>,
    val recompute: Set<AlarmKind>,
)

/** The reschedule matrix of T-604. */
class ReschedulePolicy {
    /**
     * Boot and app updates lose every system alarm: restore, then recompute because time has passed. Clock and
     * time-zone changes keep system alarms but move wall-clock times: recompute. A permission change only switches
     * between exact and inexact: restore. Preference changes recompute the kinds whose times depend on them, and
     * changed alarm inputs (personal events, reminders) recompute exactly their kinds.
     */
    fun planFor(event: RescheduleEvent): ReschedulePlan =
        when (event) {
            RescheduleEvent.BootCompleted, RescheduleEvent.PackageReplaced -> {
                ReschedulePlan(ALL_KINDS, ALL_KINDS)
            }

            RescheduleEvent.TimeChanged, RescheduleEvent.TimeZoneChanged -> {
                ReschedulePlan(emptySet(), ALL_KINDS)
            }

            RescheduleEvent.ExactAlarmPermissionChanged -> {
                ReschedulePlan(ALL_KINDS, emptySet())
            }

            is RescheduleEvent.PreferencesChanged -> {
                ReschedulePlan(emptySet(), affectedKinds(event.previous, event.current))
            }

            is RescheduleEvent.AlarmInputsChanged -> {
                ReschedulePlan(emptySet(), event.kinds)
            }
        }

    /**
     * Alarm kinds whose times depend on a preference that differs between [previous] and [current]: the prayer method,
     * Asr convention, chosen place and athan settings (T-1101) move or switch prayer alarms; the Islamic calendar
     * variant and Hijri offset move reminders of events dated in the Islamic calendar. Language, theme and display
     * preferences move nothing.
     */
    fun affectedKinds(
        previous: UserPreferences,
        current: UserPreferences,
    ): Set<AlarmKind> =
        buildSet {
            val prayerChanged =
                previous.prayerMethod != current.prayerMethod ||
                    previous.asrJuristic != current.asrJuristic ||
                    previous.place != current.place ||
                    previous.athan != current.athan
            if (prayerChanged) add(AlarmKind.PRAYER)
            val hijriChanged =
                previous.islamicVariant != current.islamicVariant ||
                    previous.islamicOverride != current.islamicOverride ||
                    previous.hijriOffsetDays != current.hijriOffsetDays ||
                    previous.hijriOffsetSetAtEpochMillis != current.hijriOffsetSetAtEpochMillis
            if (hijriChanged) add(AlarmKind.REMINDER)
        }

    private companion object {
        val ALL_KINDS: Set<AlarmKind> = AlarmKind.entries.toSet()
    }
}
