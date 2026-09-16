# ADR-0032: A restore is journalled so it can be finished or undone across the database and the preferences

- **Status:** Accepted
- **Date:** 2026-09-16
- **Plan reference:** docs/PLAN.md T-605 (backup/restore), T-1503 (backup UI); ADR-0012 (Room schema and preference
  defaults); code review finding B09 (docs/CODE_REVIEW_2026-09-15.md)

## Context

A restore replaces the personal tables (Room) and the preferences (Proto DataStore). The two stores cannot share one
transaction. `BackupService.restore` committed the Room transaction first and then wrote the preferences, so a failing
preference write, a cancellation or a process death between the two left restored rows next to the old preferences
while the screen reported that nothing had changed. Swapping the order only moves the same gap.

## Decision

1. **Journal first.** Before anything is written, `RestoreJournal` stores two encoded snapshots in
   `noBackupFilesDir/restore-journal/` — the backup to apply and the data and preferences it replaces — and then a
   `direction` file (`FORWARD`). Each file is written beside its final name and renamed; the `direction` file is written
   last and deleted first, so a partial record is never read. The files use the backup codec without encryption, like
   the database itself, and the directory is excluded from Android backups.
2. **Apply, then undo on failure.** Both stores are written outside cancellation. If either write fails, the direction
   becomes `ROLLBACK` and the previous snapshot is written back; the result is `RestoreFailed` (nothing changed).
3. **Report what is true.** If the undo also fails, the journal stays and the result is `RestorePending`
   (`BackupFailure.RESTORE_INCOMPLETE`: "may be partly replaced; finished or undone at the next start"). A new restore
   first tries to finish the pending one and refuses while it cannot.
4. **Recover at start-up.** `TaqvimApplication` runs `RestoreRecovery` before the preference and reminder watchers
   start: a `FORWARD` record is applied again (the process died mid-restore, so the user's intent stands), a
   `ROLLBACK` record is undone. Writes are idempotent (the tables are replaced wholesale, device-only preference values
   are always kept from the current store), so repeating a step is safe. Only after `Completed` or `RolledBack` is the
   scheduler told to recompute every alarm; `ServiceBackupOperations` does so only after `Restored`.

No Room schema or preference proto change is needed.

## Consequences

- Data and preferences always end as either the backup or the previous state; an interrupted restore is reported as
  incomplete until the next start settles it.
- A restore temporarily writes up to two copies of the personal data in private storage; they are deleted when the
  restore finishes.
- Alarms and subscriptions are only rescheduled from a settled state.

## Addendum 2026-09-16 — holding readers until the stores are settled

The first version left two gaps: screens opened before recovery finished could read the interrupted state, and
subscription refreshes already queued in WorkManager were not delayed. A process-wide `RestoreGate` closes both:

- It starts settled unless a journal existed at start (`BackupService.isRestoreRecorded`, one file lookup), so a normal
  start never waits.
- `RestoreRecovery` settles it with the recovery result. `RecoveryResult.StillPending`, and a restore during the session
  that can be neither finished nor undone, leave it `INCOMPLETE`, which keeps everything held until the next start.
- `TaqvimAppShell` shows a waiting screen (`RestoreHold`: progress while recovering, an explanation when incomplete)
  instead of any screen until the gate is settled.
- `SubscriptionRefresher` awaits the gate before every refresh, so queued or requested refreshes write only after
  recovery; a refresh held by an incomplete restore waits until WorkManager stops the worker and retries later.
- The widget triggers, the persistent notification and launcher icon refresh and the language sync now start after
  recovery, like the preference and reminder watchers.

Not held: system-initiated widget updates and scheduler broadcasts (time zone, time set, boot) received before recovery
finishes still read the stores; they are recomputed once recovery requests `AlarmInputsChanged` and the watchers start.
