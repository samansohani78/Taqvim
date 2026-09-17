# ADR-0026: Persian year starts are computed at run time for every year

- **Status:** Accepted (supersedes decisions 2 and 4 of ADR-0008; the 2026-09-17 addendum supersedes ADR-0008 decision 3
  and this ADR's decision 5)
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md §6 A-02, T-102; ADR-0002 (module rules), ADR-0003 (permissive dependencies),
  ADR-0008, ADR-0025

## Context

ADR-0008 shipped the A-02 rule as a generated table: one leap bit per year for SH −3000…3000, computed offline from
cosinekitty/astronomy equinoxes, with the 2820-year arithmetic cycle (Birashk, as described by Tøndering) outside it.
The owner asked on 2026-09-15 that no calendar data be set by hand and that every value come from an algorithm that
works for any year ("for all data in this app dont use manuly set write algoritm for inifiny use"). The table was data
derived from the rule, and the fallback followed a different rule from A-02.

## Decision

1. **Rule unchanged.** `PersianYearStartRule` (A-02): 1 Farvardin is the day, at the Tehran meridian (UTC+03:30), whose
   noon follows the March equinox.
2. **True equinox at run time for SH −3000…3000.** `PersianYearStarts` applies the rule to the March equinox found by
   `CalendarAstronomy.marchEquinox` (the same search, window and millisecond conversion the table was generated
   with). Year starts are computed on first use in blocks of 64 years and kept in immutable arrays behind `lazy`;
   nothing is written after a block is built, so the calendar holds no mutable state that callers can observe.
3. **Mean equinox for every other year.** Outside that range the ephemeris and ΔT are extrapolations: the library's
   own search fails between SH 10 000 and 20 000, and ΔT grows as a parabola. So no true equinox is claimed there.
   The same rule is applied to the mean March equinox, continued from the nearer edge by the mean interval between
   the two edge equinoxes: (E(3000) − E(−3000)) / 6000, about 365.2423 days. The interval is kept exactly (whole days,
   milliseconds and a remainder in 1/6000 ms), so every `Int` year, and the year after `Int.MAX_VALUE`, is computed in
   `Long` without overflow or rounding. At both edges the continuation gives exactly the astronomical start, so there
   is no seam, and every year has 365 or 366 days. The Birashk cycle is removed.
4. **Dependency.** `:core:calendar` now depends on cosinekitty/astronomy 2.1.19 (MIT, already used by
   `:core:astronomy`) as an `implementation` dependency. Its types stay inside the internal `CalendarAstronomy`
   object, which is also the extension point for other rule-defining events (the conjunction, sunset and moonset at a
   site) needed by lunar calendars. `:core:astronomy` keeps depending on `:core:calendar`, so there is no cycle.
5. **Official data still wins.** The Calendar Center's official leap years (1206–1498) and the 1404/1405 calendars
   agree with the computed starts, so no year is overridden. A future disagreement would be applied as a per-year
   override of the start, as ADR-0008 decision 3 states.
6. **Range.** Every `Int` year is supported; `fromJdn` rejects days before 1 Farvardin `Int.MIN_VALUE` or after the
   last day of `Int.MAX_VALUE` with `IllegalArgumentException`.

## Consequences

- The SH −3000…3000 table is kept only as a test snapshot (`PersianLeapTableSnapshot`); the computed starts must equal
  it for all 6 001 years, and must equal the library's `seasons` search. A library upgrade that moves a near-noon year
  fails the test and needs review against official data.
- 44 years of SH −3000…3000 have an equinox within 5 minutes of Tehran noon (20 within 2 minutes); their start depends
  on ephemeris and ΔT precision, and only 1206–1498 are confirmed by official data. With the mean equinox the time of
  day steps by about 5 h 49 min a year, so about 0.7 % of far years fall within 5 minutes of noon (694 of the 100 000
  years beyond each edge).
- Far from the present the calendar follows the mean equinox, not the true one; no one can know the true Nowruz of
  those years, and the rule used there is stated in code and here.
- Performance (JVM, development machine, 2026-09-15): 1 000 000 `fromJdn` within ±200 years of today in 24 ms, and
  over random days across ±100 000 years in 55 ms (63 ms with `toJdn`), within ADR-0008's budget of 300 ms. Building
  all 94 blocks of the astronomical range took under 40 ms; the first conversion in a block pays for its 64 equinox
  searches.
- The standalone Wear app, which uses `:core:calendar` but not `:core:astronomy`, now ships the astronomy library
  (a 214 KB jar before shrinking).

## Addendum 2026-09-17 — official data is a golden oracle only

The owner directed on 2026-09-17 that Persian leap years and Nowruz come from the astronomical equinox computation at
the Tehran meridian, with the official leap-year table allowed only as a golden test oracle, never as a runtime
override. This replaces decision 5 above and ADR-0008 decision 3: there is no per-year override mechanism, and none is
added if a publication ever disagrees — the disagreement is reported by the golden tests and investigated instead.

No runtime override existed after main@dfe6f17, so nothing was removed from the app. The rule is now enforced:
`PersianComputedOnlyKonsistTest` fails if any production file references the official Persian fixtures
(`golden/persian`, the leap-year and Nowruz tables, the 1404/1405 daily calendars, the former leap table) or if
`PersianCalendarSystem`/`PersianYearStarts` gain an override input. The official data stays in
`core/calendar/src/test/resources/golden/persian/` and is checked by `PersianCalendarOfficialTest` (leap years
1206–1498, every day of 1404 and 1405, Nowruz instants), `PersianCalendarAstronomyTest` and `PersianIcuOracleTest`.
