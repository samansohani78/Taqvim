/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reads the current [ReminderSetup]; bound in `:app` over personal events, official opt-ins and preferences. */
fun interface ReminderSetupSource {
    suspend fun current(): ReminderSetup
}

/** Shows a reminder notification ([SystemReminderNotifier] in the app). */
fun interface ReminderNotifier {
    /** Shows [reminder]; returns whether it could be posted. */
    fun show(reminder: PlannedReminder): Boolean
}

/** One scheduler alarm of a reminder: the scheduler's [sourceId] (see [PlannedReminder.alarmSourceId]) and time. */
data class ReminderAlarm(
    val sourceId: Long,
    val at: Instant,
)

/**
 * The reminder side of the scheduler (T-604, ADR-0033): the alarms reminders need, and what happens when one fires.
 * `:app` adapts [upcoming] and [isPlanned] to the `REMINDER` `AlarmSource` and [onAlarm] and [onGaveUp] to its
 * `AlarmDelivery`; the scheduler has already dropped alarms that fired more than 15 minutes late.
 */
class ReminderAlarms(
    private val setup: ReminderSetupSource,
    private val log: DeliveryLog,
    private val notifier: ReminderNotifier,
) {
    private val mutex = Mutex()

    /** The alarms of the reminders due strictly after [now]. */
    suspend fun upcoming(now: Instant): List<ReminderAlarm> =
        ReminderPlanner.upcoming(now, setup.current()).map { ReminderAlarm(it.alarmSourceId, it.at) }

    /** Whether a reminder of scheduler source [sourceId] is still planned at [plannedAt], e.g. for a snooze of it. */
    suspend fun isPlanned(
        sourceId: Long,
        plannedAt: Instant,
    ): Boolean = ReminderPlanner.at(sourceId, plannedAt, setup.current()) != null

    /**
     * Shows the reminder of scheduler source [sourceId] planned exactly at [plannedAt], at most once per occurrence
     * unless it is shown again after a [snoozed] alarm. It is recorded as delivered only after it was posted, so a
     * failure leaves it pending for the scheduler to retry.
     */
    suspend fun onAlarm(
        sourceId: Long,
        plannedAt: Instant,
        snoozed: Boolean = false,
    ): AlarmDeliveryResult =
        mutex.withLock {
            val results =
                ReminderPlanner.allAt(sourceId, plannedAt, setup.current()).map { reminder ->
                    when {
                        !snoozed && isDelivered(reminder) -> {
                            AlarmDeliveryResult.SKIPPED
                        }

                        !notifier.show(reminder) -> {
                            AlarmDeliveryResult.FAILED
                        }

                        else -> {
                            AlarmDeliveryResult.DELIVERED.also {
                                log.record(reminder.key, DeliveryState.DELIVERED)
                            }
                        }
                    }
                }
            when {
                AlarmDeliveryResult.FAILED in results -> AlarmDeliveryResult.FAILED
                AlarmDeliveryResult.DELIVERED in results -> AlarmDeliveryResult.DELIVERED
                else -> AlarmDeliveryResult.SKIPPED
            }
        }

    /** Whether [reminder] was delivered, also under the key older versions recorded it with. */
    private suspend fun isDelivered(reminder: PlannedReminder): Boolean =
        log.state(reminder.key) == DeliveryState.DELIVERED ||
            (reminder.legacyKeyed && log.state(reminder.legacyKey) == DeliveryState.DELIVERED)

    /** Records that the reminder planned at [plannedAt] was given up after repeated failures. */
    suspend fun onGaveUp(
        sourceId: Long,
        plannedAt: Instant,
    ) {
        ReminderPlanner.allAt(sourceId, plannedAt, setup.current()).forEach { reminder ->
            if (!isDelivered(reminder)) log.record(reminder.key, DeliveryState.FAILED)
        }
    }
}
