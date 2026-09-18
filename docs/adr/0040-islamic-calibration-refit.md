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

1. **One-command importer.** `tools/sources/iran/official_calendar_import.py` reads every `Calendar-<solar year>.pdf` in
   `docs/sources/iran` (moved there on 2026-09-18), verifies its SHA-256 against `docs/sources/iran/MANIFEST.md`, and extracts the daily
   Solar Hijri / lunar Hijri / Gregorian table with `pdftotext -bbox`. It writes
   - `core/calendar/src/test/resources/golden/persian/official/<year>.csv`, one row per printed day (named
     `iran-official-<year>-days.csv` until 2026-09-18);
   - `core/calendar/src/test/resources/golden/islamic-iran/official-calendars.csv`, the index of imported calendars;
   - `core/calendar/src/test/resources/golden/islamic-iran/official-month-starts.csv`, every lunar month start the
     calendars establish;
   - `dataset/iran/islamic-iran-overrides.json`, the optional official override of ADR-0037.

   The reading itself is `tools/sources/iran/official_calendar_pdf.py`. The tool supports `--check`, is idempotent, and fails
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
- Adding an older Calendar Center calendar is: drop `Calendar-<year>.pdf` in `docs/sources/iran`, add its manifest row,
  run `tools/sources/iran/official_calendar_import.py`, then
  `./gradlew :core:astronomy:test -Ptaqvim.updateSnapshots=true`. The daily fixtures, the month-start fixture, the
  override table, the tests that iterate them and the agreement figures all follow.
- The importer needs `pdftotext` and `pdfinfo` (poppler-utils), like the other PDF generators in `tools/`. CI does not
  run it; `--check` is available for a job that wants to.
- The Afghan anchors are the only Afghan evidence found so far. Two of the five constrain the month start only within
  the week, so 100 % there is weaker than the same figure for Iran.
- ICU4J stays a test-scope oracle in `:core:astronomy` as well as `:core:calendar`; no runtime dependency changes.

## Addendum 2026-09-18 — every official calendar 1381–1405

The owner supplied the Calendar Center's calendars for every Solar Hijri year 1381–1405 and moved all Iranian sources
to `docs/sources/iran/` (inventory: `docs/sources/iran/MANIFEST.md`). The importer moved to `tools/sources/iran/` and
now reads four layouts (ids in `official_calendar_sources.py`):

- **21 years imported** (1381–1394, 1397–1400, 1403–1405; 7 670 days). Every day agrees with the computed Persian
  calendar — date, weekday and Gregorian day — in `PersianCalendarOfficialTest`.
- **4 years not imported** (1395, 1396, 1401, 1402): their text layer maps several digit glyphs to one character (in
  1401, 0, 7, 8 and 9 all extract as 1), so no date can be read without OCR. They are listed in the index, not guessed.
- **Two misprints corrected from the documents themselves**, each as an `Erratum` that applies only where the page
  prints the stated value: the 1381 Shahrivar page prints lunar days 6–21 for 15–30 Jumada II (the first row and the
  printed 1 Rajab fix them), and the 1383 Esfand page prints Muharram 1427 for 1426.
- **Official announcements.** The 1381 and 1383–1385 editions are the Calendar Center's computed calendar with a notice
  that one month began a day later (Ramadan 1423) or earlier (Shawwal 1425, Ramadan 1426, Shawwal 1427) by official
  announcement. The daily fixtures keep the printed dates; the month-start history
  (`golden/islamic-iran/official-month-starts-1381-1405.csv`, 262 months) moves the announced month, gives the month
  before it the changed length, and leaves the announced month's own length empty. Its `basis` column says which
  months are printed, derived or announced.
- **The optional override stays on 1404–1405** (`OVERRIDE_YEARS`): extending the shipped override to the older years is
  a separate decision, and the other years are test oracles only.
- **Refit not done here.** The calibration report still scores the override months; refitting on the 262-month
  history is the next step (brief item 4).
