# ADR-0033: Fired alarms stay pending until delivered, and snoozes are scheduler alarms

- **Status:** Accepted
- **Date:** 2026-09-16
- **Plan reference:** docs/PLAN.md T-604 (scheduler, reschedule matrix, "skip if fired more than 15 minutes late"),
  T-1001 (reminder notifications with Done and Snooze), T-1102 (athan playback with Stop and Snooze); code review
  2026-09-15, improvement I07; ADR-0012 (Room schema)

## Context

The scheduler (T-604) keeps every alarm in `scheduled_alarms` and re-registers it after a reboot, an app update or an
exact-alarm permission change. Two paths bypassed that model:

- **Delivery.** `onFired` deleted the row before delivery, and the reminder and athan delivery logs recorded an
  occurrence as delivered *before* the notification was posted or the athan service started. When posting failed (no
  notification permission, a foreground-service start refused) or the process died in between, the occurrence was
  marked delivered but never shown, and nothing retried it.
- **Snoozes.** Snooze actions called `AlarmManager` directly. Those alarms were not stored, so a reboot lost them, and
  deleting an event did not cancel its snoozed reminder, which then showed a stale copy carried in the intent.

## Decision

1. **A fired alarm stays pending until its delivery reports back.** `AlarmScheduler.onFired` no longer deletes an
   alarm it delivers. It *leases* the alarm instead: `retry_at_epoch_millis` is set to one minute later and the system
   alarm is registered for that time. If the process dies before the outcome is recorded, the lease fires the alarm
   again. `AlarmDelivery.deliver` returns a `DeliveryOutcome`, and `AlarmScheduler.complete` records it:
   - `DELIVERED` or `SKIPPED` (nothing planned any more, or already shown): the row and its system alarm are removed;
   - `FAILED`: `attempts` is incremented and the alarm fires again one minute later, until three deliveries have failed
     or the retry would fall outside the 15-minute lateness window of the planned trigger time. Then it is given up
     (`Completion.GAVE_UP`) and the delivery's `onGaveUp` records the failure.
   When several deliveries serve one kind, any failure retries the alarm (`combined()`); the delivery logs keep a retry
   from showing an occurrence twice.
2. **States.** An occurrence is *pending* while its row exists, *delivered* or *failed* once the feature's
   `DeliveryLog` records it. The log is written only after posting or starting succeeded, so a failure or process death
   leaves the occurrence pending. Both logs share one implementation (`SharedPreferencesDeliveryLog`, entries
   `key<TAB>STATE`); entries written before this change (only the key) read as delivered.
3. **Recomputing never replaces an alarm in flight.** `AlarmReconciler.reconcile` keeps stored alarms that are due and
   still within their lateness window, since their delivery may be under way; sources only return future alarms, so
   such an alarm would otherwise be cancelled by the recomputation that follows every fired alarm.
4. **Snoozes are scheduler alarms** of the new kinds `REMINDER_SNOOZE` and `PRAYER_SNOOZE`. The row stores the snoozed
   occurrence's planned instant in `snoozed_from_epoch_millis`. Consequences:
   - they are restored after a reboot, an app update or a permission change like every other kind, exact or inexact
     according to the permission;
   - a snooze is delivered by the delivery of the kind it snoozes (`deliveryKind`), which re-plans the occurrence from
     current data and shows it again even though it was already delivered; a fired snooze triggers no recomputation;
   - whenever a kind is recomputed (its inputs or preferences changed, the clock moved, a boot), its sources are asked
     whether each snooze still shows something (`AlarmSource.keepsSnooze`); snoozes of deleted or changed occurrences
     are cancelled;
   - snoozing the same occurrence again replaces its earlier snooze.
   Features snooze through the `SnoozeScheduler` port, bound in `:app` to the scheduler (ADR-0002). Without the app's
   graph (bare receivers in tests) the old one-off system alarm remains as a fallback.
5. **Schema 6.** `scheduled_alarms` gains `attempts` (default 0), `retry_at_epoch_millis` and
   `snoozed_from_epoch_millis` (both nullable). Migration 5 → 6 adds the columns; existing rows become pending alarms
   without a retry.

## Consequences

- A notification that could not be posted is retried twice within a few minutes and then recorded as failed instead
  of silently counted as shown. Delivery is at least once within the lateness window: a process death after posting
  but before recording can show a reminder again, which replaces the same notification id.
- Snoozes survive reboots and follow edits and deletions of their events. A snoozed athan plays with the current athan
  settings rather than the ones captured when it was snoozed.
- The athan service writes its snooze on a background coroutine after it stops; the write is a single row.
- Device checks (T-1102 OEM checklist) still need to confirm delivery under battery restrictions and Do Not Disturb.
