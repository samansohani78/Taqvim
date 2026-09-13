# ADR-0011: Recurrence in non-Gregorian calendars

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-503 (e.g. yearly on 30 Esfand: skip non-leap years or move to 1 Farvardin,
  user-selectable policy)

## Context

RFC 5545 defines recurrence on the Gregorian calendar and skips occurrences whose day does not exist (e.g. the 31st in
a 30-day month). Personal events in Taqvim may be defined in the Persian or Islamic calendar, where month lengths vary
(29/30 Esfand, 29/30-day lunar months).

## Decision

- Months and years of a recurrence rule are counted in the event's own calendar (`CalendarArithmetic` from
  `:core:calendar`), not in the Gregorian calendar.
- When the target day does not exist in a period, a user-selectable `InvalidDayPolicy` applies: `SKIP` (the RFC 5545
  behaviour and the default), `NEXT_DAY` (the first day of the following month, e.g. 30 Esfand → 1 Farvardin) or
  `LAST_DAY_OF_MONTH` (e.g. 30 Esfand → 29 Esfand).
- Expansion is a lazy, strictly increasing sequence bounded by COUNT, UNTIL or an explicit window, and it stops after
  1 000 consecutive periods without an occurrence.

## Consequences

- ICS export of such events can only be exact for Gregorian rules; non-Gregorian rules are exported as explicit
  occurrences or with an X- property (to be decided in T-1003).
