# Provenance Register

Required by `docs/PLAN.md` §0.1 (clean-room policy). Every core algorithm, dataset, and
golden fixture added to this repository must have an entry here **in the same commit** that
introduces it.

## Attestation

By adding an entry, the author attests that the implementation was written solely from the
listed public references and `docs/PLAN.md`, and that **no** source code, data file, string
resource, icon, or test fixture from any GPL/LGPL/AGPL/MPL project was fetched, read, quoted,
paraphrased, or recalled. Explicitly forbidden inputs include (non-exhaustive):
`persian-calendar/*` (all repositories), `avianey/Level`, `ilius/starcal`, and any
Persian-calendar or prayer-times GPL/LGPL library.

## Entry template

```markdown
### <ID> — <Component>
- **Module / files:** `core/<module>/src/main/kotlin/...`
- **Task:** T-xxx
- **Spec:** docs/PLAN.md §6 <A-xx>
- **References used (public only):**
  1. <Author, Title, Publisher/Journal, Year, URL, retrieved YYYY-MM-DD>
- **Validation oracle:** <library/table + license>
- **Deviations from spec:** <none | ADR-xxxx>
- **Author / date:** <name, YYYY-MM-DD>
- **Reviewer attestation:** <name, YYYY-MM-DD — "no forbidden sources consulted">
```

## Algorithms

### A-01 — JDN ↔ Gregorian
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/GregorianCalendarSystem.kt`
- **Task:** T-101
- **Spec:** docs/PLAN.md §6 A-01
- **References used (public only):**
  1. H. F. Fliegel and T. C. Van Flandern, "A Machine Algorithm for Processing Calendar Dates",
     *Communications of the ACM* 11(10):657, 1968. https://doi.org/10.1145/364096.364097 (retrieved 2026-09-13)
  2. ISO 8601-1:2019, *Date and time — Representations for information interchange* (proleptic Gregorian
     calendar, year 0000 = 1 BC).
  3. J. Meeus, *Astronomical Algorithms*, 2nd ed., Willmann-Bell, 1998, ch. 7 (Julian Day epoch, day of week).
- **Implementation note:** the 1968 integer formulas are valid only for positive day counts; dates are shifted
  by whole 400-year Gregorian cycles (146 097 days) before conversion (own derivation from the cycle length).
- **Validation oracle:** ICU4J 78.3 `GregorianCalendar` in proleptic mode (Unicode-3.0, test scope only),
  1 000 000 random JDNs in [−1 000 000, 5 000 000] with a fixed seed.
- **Deviations from spec:** none.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-100 — `Jdn.weekday()`
- **Module / files:** `core/model/src/main/kotlin/ir/taqvim/core/model/Jdn.kt`
- **Task:** T-100
- **References used (public only):** J. Meeus, *Astronomical Algorithms*, 2nd ed., 1998, ch. 7 — day of week
  from the Julian Day (JDN 0 is a Monday).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

## Datasets

_No entries yet._

## Golden fixtures

Format: every file under `src/test/resources/golden/` starts with a `# source/url/retrieved/page/reviewer`
header (see `core/testing/README.md`); `FixtureProvenanceKonsistTest` fails the build otherwise.

### core/testing — `golden/sample/valid-fixture.csv`
- **Task:** T-005 (self-test of the fixture loader)
- **Source:** synthetic data written for this repository; contains no external facts.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
