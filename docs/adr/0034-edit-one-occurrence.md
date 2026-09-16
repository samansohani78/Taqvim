# ADR-0034: Editing or cancelling one occurrence of a repeating personal event

- **Status:** Proposed (design only; implementation pending)
- **Date:** 2026-09-16
- **Plan reference:** owner-approved review features F01 and F02 (docs/CODE_REVIEW_2026-09-15.md §4, approved
  2026-09-16); T-1000 editor, T-1003 exceptions and overrides, ADR-0011 (recurrence in the event's calendar), ADR-0031
  (day assignment), ADR-0033 (alarm delivery)

## Context

Repeating personal events can already carry per-occurrence data: `event_exceptions` (an EXDATE day) and
`event_overrides` (a RECURRENCE-ID instance with its own title, notes, days, times and colour, or `cancelled`). The
calendar (`DayEventsAssembler`), reminders (`ReminderAdapters` reads exceptions and overrides) and ICS export
(`IcsExportMapping`: cancelled overrides become EXDATEs, the others become instances) already honour them, but the
editor can only change the whole series, so users cannot move or skip one meeting without rebuilding the series.

## Decision

1. **Where the choice is made.** Opening a personal event from a day (day details, timeline, agenda, search) passes
   the occurrence's original start day (`originalJdn`) along with the event id. When the event repeats, the editor
   first asks "This occurrence" or "All occurrences"; opening from anywhere without a day (deep link, event list)
   edits the whole series as today.
2. **This occurrence.** The form opens with the override for `originalJdn` if one exists, otherwise with the series
   values moved to that day. The repeat section is hidden, and so are reminders (they stay per series). Saving
   upserts one `event_overrides` row; the detached fields are title, notes, start and end days and times (in the
   series' zone) and colour (`null` keeps the series colour). Delete becomes "Cancel this occurrence" and stores a
   cancelled override (not an exception row), so undoing it later only needs the override to be removed.
3. **All occurrences.** The existing editor, unchanged. Saving a changed series keeps overrides whose `originalJdn`
   is still an occurrence of the new rule and drops the others, in the same transaction; deleting the series deletes
   its overrides and exceptions (existing cascade).
4. **Not included:** "This and following occurrences", which needs a series split (a new event plus an UNTIL on the
   old one) and its own decision.
5. **Preview (F02, main commit with this ADR).** The repeat section lists the next ten occurrences from today,
   expanded by the same `RecurrenceEngine` as the calendar and shown in the user's zone, so leap-day policy, weekday
   choices and zone/DST effects are visible before saving. It ignores overrides of an existing series (it previews the
   rule being edited).

## Consequences

- Store port: `PersonalEventStore` gains `loadOccurrence(eventId, originalJdn)`, `saveOccurrence(...)` and
  `cancelOccurrence(eventId, originalJdn)`, implemented in `:app` over the existing DAOs; no schema change.
- Navigation: `AppDestination.EventEditor` gains an optional `occurrence` day, restored with the back stack
  (ADR-0015); drafts (B11) record which mode they belong to.
- Reminders are rescheduled after an occurrence change through the existing reconciliation (ADR-0033); export and the
  calendar need no change beyond tests that cover a moved and a cancelled occurrence.
