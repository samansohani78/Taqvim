# ADR-0040: The Islamic calibration is refitted from the imported official calendars

- **Status:** Accepted
- **Date:** 2026-09-18
- **Plan reference:** docs/PLAN.md §5 D-07, §6 A-04, A-05, A-06; ADR-0009, ADR-0010, ADR-0027, ADR-0028, ADR-0037

## Context

ADR-0027 calibrates the calculated-observational Iranian lunar months on the only official months the repository
held: the Calendar Center's calendars for 1404 and 1405 SH. Its addendum promises a refit when more official
calendars arrive. Two things were missing for that promise to be keepable:

1. The daily tables of those calendars were extracted once, by hand, with `pdftotext -bbox`. Nothing in the
   repository could repeat the extraction, so a calendar the owner drops into `docs/sources` would have to be
   transcribed again, against the owner's directive of 2026-09-17 that no calendar data is typed.
2. Nothing measured how well each region's shipped calendar reproduces the official months, so "the calibration is
   still right" was a claim without a number, and a better-fitting rule could not be recognised.

## Decision

1. **One-command importer.** `tools/iran/official_calendar_import.py` reads every `Calendar-<solar year>.pdf` in
   `docs/sources`, verifies its SHA-256 against `docs/sources/MANIFEST.md`, and extracts the daily
   Solar Hijri / lunar Hijri / Gregorian table with `pdftotext -bbox`. It writes
   - `core/calendar/src/test/resources/golden/persian/iran-official-<year>-days.csv`, one row per printed day;
   - `core/calendar/src/test/resources/golden/islamic-iran/official-calendars.csv`, the index of imported calendars;
   - `core/calendar/src/test/resources/golden/islamic-iran/official-month-starts.csv`, every lunar month start the
     calendars establish;
   - `dataset/iran/islamic-iran-overrides.json`, the optional official override of ADR-0037.

   The reading itself is `tools/iran/official_calendar_pdf.py`. The tool supports `--check`, is idempotent, and fails
   loudly — an unknown month name, a day that does not follow the day above it or a printed weekday that disagrees
   with the Gregorian date all stop the run instead of producing a guess. A calendar with no manifest row stops it
   too, with the row to paste (pages, bytes, SHA-256) printed; the tool never writes the checksum it then verifies. Reading the two stored calendars reproduces the hand-extracted fixtures row for row
   and the override file byte for byte, which is how the extraction was validated.

   `pdftotext -bbox` reads a page in visual order, so a word arrives with its letters reversed and a lam-alef
   ligature, which poppler expands in logical order inside that run, arrives with its two letters swapped. Names are
   therefore compared on a key that sorts each run of alef and lam; the 1405 calendar's text layer also drops the ayn
   of ربیع, so that spelling is listed among the accepted ones.

2. **Fixtures are found, not named.** Tests read `official-calendars.csv` and iterate the calendars listed there
   (`OfficialIranCalendars` in `:core:calendar`), so importing another calendar extends
   `PersianCalendarOfficialTest`, `IranIslamicCalendarTest` and the calibration report without editing a test. The
   month-start fixture therefore has a fixed name instead of the year range it used to carry.

3. **A refit that is run, not described.** `CrescentSighting` in `:core:astronomy` is the rule a
   calculated-observational month table is built on, and `ObservationalMonthStarts.table` takes one. The refit tries
   Yallop's classes A–D and Odeh's zones A–C over named sets of observing places, plus both tabular calendars, and
   scores each against the official facts of the region. Yallop and Odeh answers are cached per place and evening, so
   the whole search costs one pass of crescent geometry.

4. **Agreement is measured per region, against primary data only.**

   | Region | Official facts | Oracle |
   |---|---|---|
   | Iran | first day of every lunar month the imported calendars print | `dataset/iran/islamic-iran-overrides.json` (ADR-0037) |
   | Saudi Arabia | every month of the printed Umm al-Qura calendar, AH 1420–1450 | ICU4J (Unicode License, test scope only) |
   | Afghanistan | every lunar date a Bakhtar announcement states with its civil day or weekday | `golden/islamic-afghanistan/bakhtar-announced-dates.csv` |

5. **`IslamicCalibrationReportTest`** asserts each region's documented threshold (Iran 0.90, Saudi Arabia 0.99,
   Afghanistan 1.00), asserts that no tabular calendar beats a shipped calculated one, and regenerates
   `docs/data-todo/islamic-calibration-report.md`. The report is a snapshot: new official calendars change the
   anchors, the test then fails with the line that changed, and `-Ptaqvim.updateSnapshots=true` rewrites it for
   review.

6. **Shipped defaults stand.** With the data available on 2026-09-18 the refit finds nothing better than what is
   shipped, so nothing changes: Iran keeps five cities at Yallop class D, Saudi Arabia keeps the Umm al-Qura
   criterion, Afghanistan keeps the tabular type II calendar. A candidate that fits better is reported, not adopted:
   25 months are too few to move the calibration on, and the decision waits for more official calendars (§7).

## Consequences

- Agreement on 2026-09-18: **Iran 23/25 = 92.0 %** (Jumada I 1447 and Jumada I 1448 are one day late),
  **Saudi Arabia 370/372 = 99.5 %** (the two marginal months of ADR-0028), **Afghanistan 5/5 = 100 %**.
- Adding an older Calendar Center calendar is: drop `Calendar-<year>.pdf` in `docs/sources`, add its manifest row,
  run `tools/iran/official_calendar_import.py`, then
  `./gradlew :core:astronomy:test -Ptaqvim.updateSnapshots=true`. The daily fixtures, the month-start fixture, the
  override table, the tests that iterate them and the agreement figures all follow.
- The importer needs `pdftotext` and `pdfinfo` (poppler-utils), like the other PDF generators in `tools/`. CI does not
  run it; `--check` is available for a job that wants to.
- The Afghan anchors are the only Afghan evidence found so far. Two of the five constrain the month start only within
  the week, so 100 % there is weaker than the same figure for Iran.
- ICU4J stays a test-scope oracle in `:core:astronomy` as well as `:core:calendar`; no runtime dependency changes.
