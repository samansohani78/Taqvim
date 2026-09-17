# ADR-0035: One occurrence pipeline for personal events

- **Status:** Accepted
- **Date:** 2026-09-17
- **Plan reference:** docs/PLAN.md T-305, T-503, T-804, T-1000, T-1001, T-1003; review improvement I01
  (docs/CODE_REVIEW_2026-09-15.md); ADR-0011 (recurrence in the event's calendar), ADR-0031 (day assignment),
  ADR-0033 (reminder keys), ADR-0034 (editing one occurrence)

## Context

The review found that the calendar, reminders, search and export each turned a personal event's recurrence rule into
occurrences their own way, and that several bugs (B02–B05, B07, B12, the I08 findings) came from surfaces disagreeing
about the same event. Since then the day-assignment rule (ADR-0031), occurrence-identity reminder keys (ADR-0033
addendum) and cross-surface contract tests have converged the results, but the calendar (`PersonalExpansion`), the
reminder planner, ICS export, the editor's one-occurrence edits and its recurrence preview still called
`RecurrenceEngine` and `seriesInstances` directly, and the reminder planner still walked every rule from its first
occurrence on each recompute.

Features may not depend on data modules (ADR-0002), so a shared step must live in core.

## Decision

- `OccurrenceSeries` in `:core:ics` is the start of the pipeline: a series in its own calendar (start, rule or none,
  length in days, excluded days, overrides by original day). It offers the occurrence days sought to a day
  (`starts`), whether a day is an occurrence (`isOccurrence`), and the instances that can take place in a day range
  (`instances`: exclusions removed, instances overlapping the range kept, overridden instances kept wherever their
  original day lies). Every instance carries its original day, which is the occurrence's identity.
- Every personal-event surface consumes it through a thin adapter and never expands rules itself:
  - calendar, agenda, timeline, widgets and search: `PersonalExpansion` (`:data:events`) maps instances to
    `PersonalOccurrence` (series id, original day, days, times, zone, override applied); `EventDays` dates them in the
    display zone (ADR-0031); search uses the same path through `PersonalEventDays`;
  - reminders: `ReminderPlanner` maps instances to planned reminders keyed by the original day (ADR-0033);
  - export: `IcsExportMapping` lists the occurrence days (RDATE) from `starts`;
  - editor: one-occurrence edits check `isOccurrence`; the recurrence preview takes `starts`.
- Subscription feeds are not personal series: their instant-based RRULE, RECURRENCE-ID and UNTIL semantics are
  expanded once by `IcsOccurrenceExpander` into cached rows that every surface reads, so they already have a single
  pipeline. Official events use the dataset rule engine (`EventLookup`), shared by display and reminders since B04.

## Consequences

- A rule is expanded the same way everywhere; a new surface gets moved and cancelled occurrences, exclusions and the
  occurrence identity by construction. The cross-surface contract tests (`CrossSurfaceContractTest`,
  `ExportExpansionContractTest`) cover calendar, reminders, search and export.
- The reminder planner now seeks each rule to the planning window, so recomputing reminders no longer grows with the
  age of a series.
- `OccurrenceSeries` holds no display zone; dating in a zone stays with each surface's adapter (ADR-0031), because the
  device zone, the selected place and all-day reminder times differ per surface.
