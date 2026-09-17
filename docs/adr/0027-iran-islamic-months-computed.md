# ADR-0027: Iranian lunar months are computed from the crescent rule for every year

- **Status:** Accepted (supersedes decision 2 of ADR-0009; completes its decision 4; decision 5 superseded by
  ADR-0037)
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md §6 A-05, A-06; T-104; ADR-0009 (addenda of 2026-09-13 and 2026-09-14), ADR-0010,
  ADR-0026

## Context

`IranIslamicCalendar` used the 25 months the Calendar Center has published (Ramadan 1446 – Shawwal 1448,
`IranOfficialMonthStarts`) and, everywhere else, the tabular calendar shifted by a constant number of days
(ADR-0009 decision 2). The shifted tabular calendar drifts from real crescent sightings a few months from the table.
The Iran calibration of A-06 (`IranCrescentCalibration` in `:core:astronomy`: Yallop's test at class D or better at
any of five cities) agrees with 23 of the 25 official month starts, but it only built finite tables and nothing used
it at run time. The calculated-observational variant (`IslamicVariant.CALCULATED_OBSERVATIONAL`) fell back to the
tabular calendar. On 2026-09-15 the owner asked that no calendar data be set by hand and that every value come from an
algorithm that works for any year.

## Decision

1. **Crescent months in `:core:calendar`.** `IranCrescentSighting` repeats the Iran calibration (same five cities,
   Yallop NAO TN 69 eq. 3.6, class D, best time Ts + 4/9 lag, airless geocentric arc of vision, topocentric width) with
   cosinekitty/astronomy through `CalendarAstronomy` (ADR-0026); `:core:astronomy` depends on
   `:core:calendar`, so the calculation cannot be called from there. `ObservationalMonthStartsTest` keeps the two
   implementations equal (AH 1440–1450).
2. **Sighting start.** For each month, the conjunction is searched around Meeus's mean new moon (Astronomical
   Algorithms, eq. 49.1; lunation k = month index − 17 037). The sighting start is the day after the first evening,
   from the conjunction's civil day in Iran (UTC+03:30) on, on which the crescent is seen; if it is not seen on four
   evenings, the day after the fourth.
3. **29 or 30 days (anchored months).** A month start is the sighting start kept within 29 and 30 days of the previous
   month start: when the crescent is first seen only on the evening of day 30 the month is completed to 30 days. The
   chain restarts on the sighting start at every anchor month, one whose two previous sighting months have 30 and 29
   days in either order; that pattern returns a start that was a day early or late to the sighting start, so each
   month is computed from its nearest anchor without any history. Checked with the same library (Python build
   2.1.19) over all 72 024 months of AH −3000…3000: sighting months had 29 or 30 days except three 31-day months
   (before AH −1500), the anchored months always have 29 or 30 days and equal the month-by-month rule "29 days if the
   crescent is seen on the evening of day 29, otherwise 30" in every month, and the nearest anchor is at most four
   months back.
4. **Range.** AH −3000…3000 (and 1 Muharram 3001) are computed from the crescent, a Hijri year at a time on first use,
   and kept in immutable arrays behind `lazy`, as ADR-0026 does. Outside that range the ephemeris and ΔT are
   extrapolations, so the months continue the exact mean month between the two edge months (whole days by integer
   division in `Long`): every month has 29 or 30 days and the edges meet exactly. Every `Int` Hijri year is supported;
   days outside them are rejected.
5. **Official months override.** `IranIslamicCalendar` uses the published table where it exists and the crescent
   months elsewhere. Next to the table each month start is the crescent start kept within 29 and 30 days of its
   neighbour towards the table, outwards month by month, until it equals the crescent start (joining rule). Today both
   edges already agree, so no month is adjusted. Custom tables (for example calibration tables in tests) are joined the
   same way.
6. **Variants and labels.** `IslamicVariant.CALCULATED_OBSERVATIONAL` now uses `IranCrescentCalendar` (the crescent
   months without official data). A location-specific A-06 calendar for the user's own place is not offered: the
   variant selection has no location, and the Iran calibration is the one validated against official data.
   `HijriDateSource.TABULAR_ESTIMATE` is renamed `CRESCENT_ESTIMATE`; the UI keeps showing official, user offset and
   estimate as distinct sources. The shifted tabular estimate is no longer used by any path.

## Consequences

- Dates outside the published months follow a crescent prediction instead of a tabular calendar; they can still differ
  from later announcements by a day (2 of the 25 published months start a day earlier than predicted), so the user
  offset stays.
- Adding newly published months can change predicted months only next to the table, through the joining rule.
- The first use of a year computes its twelve conjunctions and up to a few evenings at five sites; later conversions
  read cached arrays. Measured on the development machine (JVM, 2026-09-15): the first conversion in AH 1300 took
  43 ms, and 1 000 000 cached `fromJdn` calls over 200 years took 41 ms (timing test budget 1 s).
- Crescent months far in the past and future rest on extrapolated ΔT and lunar theory; beyond AH ±3000 they are the
  mean month by construction, and no one can know the true sightings of those months.
