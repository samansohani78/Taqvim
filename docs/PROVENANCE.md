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

### A-02 — Persian (Solar Hijri) calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/` — `PersianCalendarSystem.kt`,
  `PersianLeapTable.kt` (generated), `PersianYearStartRule.kt`, `BirashkArithmetic.kt`
- **Task:** T-102
- **Spec:** docs/PLAN.md §6 A-02; deviation ADR-0008 (generated leap table instead of a runtime equinox search)
- **References used (public only):**
  1. cosinekitty/astronomy 2.1.19 (MIT), `Astronomy.seasons(year).marchEquinox`, https://github.com/cosinekitty/astronomy
     — equinox instants for the table generator and its test (library API only; no GPL source involved).
  2. C. Tøndering, "The Persian Calendar", *Calendar FAQ*, https://www.tondering.dk/claus/cal/persian.php (retrieved
     2026-09-13) — 2820-year cycle structure, leap positions within a cycle, and the AP 475 anchor (fallback only).
  3. Wikipedia, "Solar Hijri calendar", https://en.wikipedia.org/wiki/Solar_Hijri_calendar (retrieved 2026-09-13) —
     cross-check of the 29/33/37-year and 128/132-year grouping; its "1925" anchor statement was not used.
- **Implementation note:** the year-start rule, the table encoding and the fallback counting (closed-form leap counts
  per period, grand cycle and cycle, anchored at the table edges) are own work from the definitions above.
- **Generator:** a one-off program run on 2026-09-13 applied the rule to equinoxes for SH −3000…3000 (183 ms);
  `PersianCalendarAstronomyTest` is the executable specification and prints the regenerated constant on mismatch.
- **Validation:** Calendar Center official leap years 1206–1498 (293 years), official calendars 1404 and 1405
  (730 days, weekday included), official Nowruz instants 1404/1405, the Calendar Center century note, and ICU4J
  78.3 `PersianCalendar` for 1200–1500 (first day of every year plus 100 000 random days).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-05 — Iranian official lunar Hijri calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/` — `IranIslamicCalendar.kt`,
  `IslamicMonthTable.kt`, `IranOfficialMonthStarts.kt`, `HijriDateResolver.kt`
- **Task:** T-104 (A-06 deferred, ADR-0009)
- **Spec:** docs/PLAN.md §6 A-05; design ADR-0009 (tabular estimate aligned at the table edges; precedence official >
  user offset > estimate; ±2-day offset expiring after 30 days)
- **Data:** month starts Ramadan 1446 – Shawwal 1448 from the Calendar Center's official calendars of 1404 and 1405 SH
  (see the "Iran official calendar sources" dataset entry). Ramadan 1446's start is derived from the 1404 calendar
  printing 20 Ramadan on 1 Farvardin 1404; its 29-day length from 1 Shawwal on 11 Farvardin.
- **Validation:** every day of the official 1404/1405 calendars (730 lunar dates), the 26 published month starts and
  lengths, structural invariants for 1440–1455, 100 000 random round-trips.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-300 — Event rule engine
- **Module / files:** `core/events/src/main/kotlin/ir/taqvim/core/events/` — `EventDefinition.kt`, `EventRule.kt`,
  `OccurrenceCalculator.kt`
- **Task:** T-300
- **Spec:** docs/PLAN.md §4.2 (`EventDefinition`, `EventRule`, `Occurrence`) and T-300; own implementation on top of
  the `:core:calendar` utilities (T-106) — no external code or data.
- **Behaviour choices:** a `Fixed`/`Single` day that does not exist in a year (e.g. 30 Esfand in a common year) has no
  occurrence; `LastWeekdayOfMonth` offsets and `Astronomical` offsets may leave the month but keep the rule year;
  `RelativeToEvent` resolves across calendars and rejects cycles, self references and unknown targets up front;
  astronomical instants come from an injected `AstronomicalEventSource` (implemented by T-403).
- **Validation oracles:** java.time (Gregorian weekday/day-of-year rules), ICU4J 78.3 `IslamicCalendar(ISLAMIC_CIVIL)`
  and `PersianCalendar` (test scope only); the official 1404 calendar's "last Friday of Ramadan" for the Iranian lunar
  calendar.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-100 — `Jdn.weekday()`
- **Module / files:** `core/model/src/main/kotlin/ir/taqvim/core/model/Jdn.kt`
- **Task:** T-100
- **References used (public only):** J. Meeus, *Astronomical Algorithms*, 2nd ed., 1998, ch. 7 — day of week
  from the Julian Day (JDN 0 is a Monday).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-03 — Islamic tabular calendar (types I and II)
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/TabularIslamicCalendar.kt`
- **Task:** T-103
- **Spec:** docs/PLAN.md §6 A-03 (30-year cycle, type II "16" and type I "15", civil epoch JD 1 948 439.5)
- **References used (public only):**
  1. R. H. van Gent, "The Islamic Calendar — Tabular Islamic calendars", Utrecht University,
     https://webspace.science.uu.nl/~gent0113/islam/islam_tabcal.htm (leap-year patterns of the arithmetic
     variants; civil epoch 16 July 622 Julian).
  2. E. M. Reingold and N. Dershowitz, *Calendrical Calculations*, 3rd ed., Cambridge University Press, 2008,
     ch. 6 (structure of the arithmetic Islamic calendar: 354/355-day years, alternating 30/29-day months).
- **Implementation note:** all arithmetic is derived directly from the definition (leap-year set per cycle,
  cumulative leap counts, month alternation); no closed-form formula was copied.
- **Validation oracle:** ICU4J 78.3 `IslamicCalendar(ISLAMIC_CIVIL)` for type II (100 000 random days). ICU has
  no type I variant; type I is validated through its exact relationship to type II (dates differ by one day
  only in cycle year 16) and round-trip properties.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-04 — Umm al-Qura calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/UmmAlQuraCalendar.kt`
- **Task:** T-103
- **Spec:** docs/PLAN.md §6 A-04; deviation ADR-0006 (embedded table instead of runtime ICU4J)
- **Data origin:** month lengths for AH 1300–1600 obtained through ICU4J 78.3's public API
  (`IslamicCalendar(ISLAMIC_UMALQURA)`, license Unicode-3.0); notice in `licenses/ICU-LICENSE.txt`, text retrieved
  from https://www.unicode.org/license.txt on 2026-09-13. No ICU source code was copied.
- **Validation oracle:** ICU4J 78.3 — every tabulated month (start and length) and 100 000 random days across
  AH 1200–1700; published Umm al-Qura tables are ICU's own source.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-107 — kotlinx.datetime bridge
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/DateTimeBridge.kt`
- **References used (public only):** kotlinx-datetime API documentation (epoch day 0 = 1970-01-01);
  JDN 2 440 588 for 1970-01-01 follows from A-01 (verified by `GregorianCalendarSystem` and a property test).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### T-201 — Numerals
- **Module / files:** `core/i18n/src/main/kotlin/ir/taqvim/core/i18n/Numerals.kt`
- **Task:** T-201
- **References used (public only):**
  1. The Unicode Standard 16.0, code charts for Arabic (U+0600), Arabic Extended digits (U+06F0), Devanagari
     (U+0900) and Tamil (U+0B80), including the Tamil number signs ௰ ௱ ௲ (U+0BF0–U+0BF2),
     https://www.unicode.org/charts/ (retrieved 2026-09-13).
  2. Unicode CLDR number symbols (decimal and group separators per locale; Indian grouping pattern
     `#,##,##0`), https://cldr.unicode.org/ (Unicode License v3).
- **Implementation note:** own implementation from the digit code-point layout; traditional Tamil uses a
  multiplier-sign decomposition derived from the sign values above.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-203 — Persian text normalization & fuzzy matching
- **Module / files:** `core/i18n/src/main/kotlin/ir/taqvim/core/i18n/PersianText.kt`
- **Task:** T-203
- **References used (public only):**
  1. The Unicode Standard 16.0, Arabic block chart and chapter 9.2 "Arabic" (yeh/kaf letter variants, tatweel,
     harakat, ZWNJ/ZWJ behaviour), https://www.unicode.org/charts/PDF/U0600.pdf (retrieved 2026-09-13).
  2. Institute of Standards and Industrial Research of Iran, ISIRI 6219 *Information Technology — Persian
     Information Interchange and Display Mechanism* (Farsi yeh U+06CC and keheh U+06A9 as the canonical letters).
  3. F. J. Damerau, "A technique for computer detection and correction of spelling errors", *Communications of
     the ACM* 7(3):171–176, 1964, https://doi.org/10.1145/363958.363994; V. I. Levenshtein, "Binary codes
     capable of correcting deletions, insertions, and reversals", *Soviet Physics Doklady* 10(8):707, 1966.
- **Implementation note:** optimal string alignment (restricted Damerau–Levenshtein) dynamic programme with
  three rolling rows and a row-minimum early exit; own implementation from the published recurrence.
- **Test data:** the 100 normalization/fuzzy pairs were written for this repository; they contain no external data.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

## Datasets

### T-200 — Language table (`languages.properties`)
- **Module / files:** `core/i18n/src/main/resources/ir/taqvim/core/i18n/languages.properties`
- **Task:** T-200; decisions in docs/adr/0007-launch-languages.md
- **Source:** Unicode CLDR 48 via the ICU4J 78.3 public API (Unicode-3.0; notice in `licenses/ICU-LICENSE.txt`),
  retrieved 2026-09-13. Fields: native name, likely script, direction, region week data (first day, weekend), short
  date pattern, AM/PM, two-item "and" list pattern, standalone wide month names (Gregorian, Persian, Islamic).
- **Extraction:** a one-off Java program (`LanguageTableGen.java`, run from the session scratchpad against
  `icu4j-78.3.jar` on JDK 21) that calls only public ICU4J APIs and writes the resource. Its logic is reproduced
  in `LanguageTableCldrOracleTest`, which re-verifies every CLDR-derived value against ICU4J on each test run.
- **Not from CLDR:** numerals, calendar order, prayer method and Asr convention are product defaults (ADR-0007).
- **Gaps:** values CLDR only provides as generic or English fallbacks were omitted (docs/DATA_TODO.md DT-004…DT-007).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### Iran official calendar sources (University of Tehran, Institute of Geophysics, Calendar Center)
- **Files:** `docs/sources/` — inventory with page counts and SHA-256 in `docs/sources/MANIFEST.md`
- **Obtained:** downloaded by the repository owner in a browser and added on 2026-09-13 (calendar.ut.ac.ir blocks
  non-browser clients); publisher site https://calendar.ut.ac.ir/Fa/. Exact download URLs to be added by the owner.
- **Extraction (T-102):** poppler `pdftotext` 2026-09-13. The leap-year table was read from the layout text and
  self-checked (293 consecutive years, no duplicates). The daily tables of the 1404/1405 calendars were parsed by word
  coordinates (`pdftotext -bbox`): each row is anchored on its weekday, values are assigned by column order, and ditto
  marks carry the previous month/year. Every row was validated for consecutive Solar Hijri days, weekday cycle,
  lunar-month progression (29/30-day months) and consecutive Gregorian dates — 730 rows, 0 errors. Holiday flags
  come from the "(تعطیل)" marker in the occasion text nearest the row (26 per year; the fixed national holidays were
  checked by hand).
- **Interpretation:** the leap table's Gregorian column is the civil date of the March equinox in Iran time, not
  1 Farvardin (it differs from the official calendars' 1 Farvardin 1404 and 1405); confirmed by computation for all
  293 years.
- **Used by:** `core/calendar/src/test/resources/golden/persian/*` (T-102); lunar-month columns reserved for T-104/D-07;
  holidays for D-02.

### D-01 — Dataset schema and validator
- **Files:** `dataset/events.v1.json`, `dataset/README.md`, `tools/dataset/`
- **Origin:** own work from docs/PLAN.md §4.2 (`EventDefinition`) and §5.2–5.3; no external dataset, schema or code
  was used. Test fixtures under `tools/dataset/src/test/resources/dataset/` are synthetic (`example.org` citations,
  placeholder titles) and contain no real-world facts.
- **Dependency:** networknt json-schema-validator 3.0.7 (Apache-2.0), used through its public API only.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### D-02 — Iran official holidays (`dataset/iran/iran-official-holidays.json`)
- **Source:** University of Tehran Calendar Center, official calendars of 1404 and 1405 SH (`docs/sources`, checksums in
  `docs/sources/MANIFEST.md`); days marked "(تعطیل)".
- **Method:** titles are the official Persian wording; only PDF text-layer artefacts were corrected (lam-alef ligature,
  ی/ک, spacing, words split across lines). Where one cell held several occasions, the holiday was attributed using the
  other year (lunar holidays move by about 11 days) or the rendered page. Rules were derived from both years and checked
  date by date against the extracted daily rows (26/26 holidays in each year). No translations: there is no primary
  source for them.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending (sign-off checklist in D-02).

## Golden fixtures

Format: every file under `src/test/resources/golden/` starts with a `# source/url/retrieved/page/reviewer`
header (see `core/testing/README.md`); `FixtureProvenanceKonsistTest` fails the build otherwise.

### core/testing — `golden/sample/valid-fixture.csv`
- **Task:** T-005 (self-test of the fixture loader)
- **Source:** synthetic data written for this repository; contains no external facts.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### core/calendar — `golden/persian/*` (T-102)
- `official-leap-years-1206-1498.csv` — official leap markers and equinox dates, 293 rows (`Kabise Shamsi 1206-1498.pdf`, pp. 1–11).
- `iran-official-1404-days.csv`, `iran-official-1405-days.csv` — one row per day: Solar Hijri date, ISO weekday,
  Iran official lunar Hijri date, Gregorian date, official holiday flag, page (`Calendar-1404.pdf`, `Calendar-1405.pdf`).
- `official-nowruz-instants.csv` — moment of the vernal equinox printed on the title page of each official calendar.
- `century-boundaries.csv` — weekdays of 1 Farvardin 1, 101, 1301, 1401 and 29 Esfand 100, 200, 1400 (`century15th.pdf`).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/calendar — `golden/islamic-iran/official-month-starts-1446-1448.csv` (T-104)
- First day (Gregorian and Solar Hijri) and length of each official Iranian lunar month, Ramadan 1446 – Shawwal 1448,
  from `Calendar-1404.pdf` and `Calendar-1405.pdf`; the Ramadan 1446 row is derived as described in A-05.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.
