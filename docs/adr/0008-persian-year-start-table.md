# ADR-0008: Persian year starts from a generated table, with an arithmetic fallback

- **Status:** Accepted; decisions 2 and 4 superseded by ADR-0026 (year starts computed at run time for every year)
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §6 A-02, T-102

## Context

A-02 defines the Solar Hijri year start astronomically: 1 Farvardin is the day, at the Tehran meridian (52.5°E),
whose noon follows the March equinox; the plan computes it with the `:core:astronomy` equinox search and falls back
to the 2820-year arithmetic cycle outside ±3000 years. Conversions are on the hottest path of the app (month grids,
widgets, search) and have a budget of 1 000 000 conversions in 300 ms.

The owner supplied the University of Tehran Calendar Center's official leap-year table for 1206–1498 and the official
calendars for 1404 and 1405 (docs/sources/MANIFEST.md).

## Decision

1. **Rule.** `PersianYearStartRule` implements A-02 exactly: local time at the Tehran meridian is UTC+03:30 (mean solar
   time at 52.5°E, which is also Iran Standard Time); an equinox strictly before 12:00 starts the year that day,
   otherwise the next day.
2. **Generated table instead of a runtime search.** `PersianLeapTable` stores one leap bit per year for SH −3000…3000
   (751 bytes as hex), generated from the rule with cosinekitty/astronomy 2.1.19 equinoxes. `PersianCalendarAstronomyTest`
   recomputes all 6 001 year starts on every build and prints the regenerated constant on mismatch, so the table cannot
   drift from the rule or from the library version. Reasons: no ephemeris computation on the hot path, identical
   results on every device and build, and `:core:calendar` needs the astronomy library only in tests.
3. **Official data wins.** The official table (1206–1498) agrees with the rule for all 293 years, so no year is
   overridden today. If a future official publication disagrees, that year's bit is set from the publication and
   listed here.
4. **Fallback.** Outside the table, leap years follow the 2820-year cycle as described by C. Tøndering, "The Persian
   Calendar" (cycles of 29, 33, 33, 33 years, the final cycle 37; years divisible by 4 within a cycle, except year 0,
   are leap; the current period began in AP 475). Year starts are counted from the table edges, so every year has 365
   or 366 days and there is no seam. Wikipedia states a different anchor ("began in 1925"); Tøndering's explicit anchor
   and leap positions are used. The fallback only affects dates before 2379 BC or after AD 3621.

## Consequences

- 31 tabulated years have an equinox within two minutes of noon (for example 1309, which is exactly at noon to the
  minute, and 1635, 1701, 1734, 2060). Their start depends on ephemeris and ΔT precision. For 1206–1498 the official
  table confirms the computed result; later years are best effort until official tables exist.
- ICU4J's arithmetic Persian calendar agrees with the table on every 1 Farvardin in 1200–1500
  (`PersianIcuOracleTest`); disagreements outside that range are expected and not tested.
- Upgrading cosinekitty/astronomy may change near-noon years; the astronomy test fails and prints the new table, which
  must be reviewed against official data before it is accepted.
