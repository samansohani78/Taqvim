# ADR-0036: Every dataset event is a rule; single-year instances are removed

- **Status:** Accepted
- **Date:** 2026-09-17
- **Plan reference:** docs/PLAN.md §4.2 (`EventRule`), §5 (dataset program), D-01, D-03, D-08, D-09, T-300; owner
  directive of 2026-09-17 ("computed, not typed")

## Context

The owner asked that nobody should ever have to type next year's dates: "every event must be expressed as a rule
(Fixed, NthWeekdayOfMonth, RelativeToEvent, Astronomical, …) so it regenerates for any year. Any event that exists as a
single hard-coded year instance must be converted to a rule or removed", guarded by a CI test that fails on per-year
date instances a rule could express.

The Iranian, international and ancient-Iranian datasets were already rule-based. The Afghanistan set (D-03) held seven
`Single` records, one per announced day, because the only accepted source is Bakhtar News Agency announcements for
specific years. Six of them are annually recurring days (three national anniversaries, Arafa and the first two days of
Eid al-Adha). The seventh, 13 Dhu al-Hijjah 1447, was a holiday only because the 1447 announcement granted "four working
days" from Arafa and Friday 12 Dhu al-Hijjah was not counted; which day completes the four depends on the weekday each
year, and no rule type expresses that.

## Decision

1. **Recurring days are rules.** A day an announcement names as an annual occasion becomes a `Fixed` rule in the
   announcement's calendar with `validity.fromYear` = the announced year, citing that announcement. Earlier years are
   not asserted; later years regenerate without data entry. The six Afghan records now read `af.holiday.soviet-withdrawal`
   (26 Dalw), `af.holiday.kabul-victory` (24 Asad), `af.holiday.independence` (28 Asad), `af.holiday.arafa`
   (9 Dhu al-Hijjah), `af.holiday.eid-al-adha.1` and `.2` (10 and 11 Dhu al-Hijjah); the year suffix is dropped from
   their ids.
2. **Single-year instances are removed from the shipped data.** The 1447 fourth Eid day (13 Dhu al-Hijjah 1447) is
   removed, as the owner's directive requires ("converted to a rule or removed"); it is recorded under DATA_TODO DT-031.
   12 Dhu al-Hijjah is not added as a recurring holiday either: no source states it, and the Afghan labour-law holiday
   list that would (DT-032) is not available. The `Single` rule type remains in the schema (PLAN §4.2) for validator
   fixtures and for contributions, guarded as follows: the schema gains an optional `oneOffReason`, and the validator
   (`OneOffChecks`, issue kind `ONE_OFF_RULE`) rejects a `Single` rule without it, a `oneOffReason` on any other rule,
   and `Single` records on the same calendar day in different years (those must be one recurring rule). Shipping a
   `Single` record needs a new owner decision, since `NoPerYearManualDataTest` fails on any.
3. **`NoPerYearManualDataTest`** (`:tools:dataset`) guards all runtime data: every dataset event file validates with
   no one-off issue; neither the shipped dataset files nor the generated `:data:events` sources contain any `Single`
   rule;
   no module's `src/main` assets, resources or raw resources contain a calendar date outside comment lines; and every
   table-like source in `src/main/kotlin` (names with Table, MonthStarts, Override or Leap) is on a justified
   allow-list. The allow-lists name the optional Iranian official month starts and their override parser (ADR-0027 and
   the optional-override change of 2026-09-17), the historical Umm al-Qura calendar of AH 1300–1419 (ADR-0028
   addendum), computed caches and CLDR name tables.

## Consequences

- Afghan holidays appear in every year from their announced year on, instead of only in the announced year.
- Reminders attached to the old year-suffixed Afghan ids (F-02) no longer match; the app had not been released.
- The announced 13 Dhu al-Hijjah 1447 is no longer shown as a holiday. 12 Dhu al-Hijjah in years without a Friday in the
  Eid span, and holidays in years before the announcements, stay unknown until a source states them (DT-031, DT-032).
- A new per-year table or dated resource fails CI until it is computed instead or justified on the allow-list.
