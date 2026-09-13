# ADR-0009: Iranian official lunar Hijri calendar — table, aligned estimate and user offset

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §6 A-05, A-06; T-104; D-07

## Context

A-05 defines the Iranian lunar calendar as an override table (D-07) on top of the tabular calendar (A-03), with a user
offset of ±2 days that expires, and precedence override > offset > tabular. The Calendar Center publishes month starts
only in its annual official calendars; today the repository holds those of 1404 and 1405 SH, which cover Ramadan 1446
to the start of Shawwal 1448.

A naive combination — table dates inside the covered range, plain tabular dates outside — creates impossible months
at both edges: official and tabular month starts differ by up to a couple of days, so the months either side of the
table would have 27–32 days.

## Decision

1. **Month table.** `IslamicMonthTable` stores the first published month and consecutive month lengths (29/30);
   `IranOfficialMonthStarts.TABLE` holds the published months and grows with D-07.
2. **Aligned estimate outside the table.** Before the table the tabular calendar is shifted by
   `officialStart(first) − tabularStart(first)`; after the table by `officialStart(next) − tabularStart(next)`. Shifting
   whole days keeps tabular month lengths, so every month on both sides still has 29 or 30 days and the calendar is
   continuous. Near the table this is the best available estimate; far from it, it is the tabular calendar with a
   constant day offset.
3. **Precedence and offset.** `HijriDateResolver` returns the table date where the table covers the day; otherwise an
   active `HijriOffset` (−2…+2 days, valid for 30 days from when it was set) shifts the estimate; otherwise the
   estimate is used. Each result carries a `HijriDateSource` for the "date source" explanation in the UI.
4. **A-06 deferred.** The calculated-observational variant (Yallop criterion at the user's location) needs Sun/Moon
   positions at sunset from the astronomy façade (T-400, T-403). It will be implemented as another estimate source
   after those tasks; T-104 stays WIP until then.

## Consequences

- Dates outside the published range may differ from later official announcements by a day or two; the UI must show the
  source, and the user offset exists for exactly that case.
- Adding a newly published year to the table can change estimated dates just outside the old range; tests pin only
  published data and structural invariants (29/30-day months, continuity, round-trips).

## Addendum (2026-09-13): A-06 implemented and calibrated

A-06 is implemented in `:core:astronomy` (`Yallop`, `ObservationalMonthStarts`): a month has 29 days when the crescent
passes Yallop's test on the evening of its 29th day, otherwise 30; the table feeds `IranIslamicCalendar`.

Measured against the 25 published Iranian month starts (Ramadan 1446 – Shawwal 1448), a single site at Tehran with
class C agrees on 19 (every miss is one day late); at class D 20. Requiring a sighting at any of five cities across
Iran (Tehran, Mashhad, Zahedan, Bandar Abbas, Shiraz) at class D or better agrees on 23 (92 %), which meets the plan's
90 % criterion and is published as `IranCrescentCalibration`.

This calibration is a fit to a small sample and does not describe the official procedure. It must be re-validated
(and the test threshold kept) whenever further official calendars are added (DT-002); generic single-site use keeps
class C as its default.

