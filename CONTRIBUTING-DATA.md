# Contributing holiday and event data

Taqvim ships holidays and observances compiled from **primary official sources only** (docs/PLAN.md §5). This guide
covers changes under `dataset/`. Code contributions follow the pull-request template.

## Ground rules

1. **Primary sources only.** Examples: the University of Tehran Calendar Center's official calendar of Iran, a
   government gazette, the United Nations list of international days. Wikipedia, blogs, other calendar apps and
   aggregator sites are not primary sources.
2. **Every record carries a citation.** `citations` needs at least one `{ url, title, page, retrieved }`. The schema
   rejects a record without it (`tools/dataset/src/test/resources/dataset/invalid/13-missing-citation.json`). A
   `validity` range carries its own citation.
3. **Never guess.** If a date, name or rule cannot be verified from the cited page, leave it out. Add a row to
   [`docs/DATA_TODO.md`](docs/DATA_TODO.md) naming the missing data and the primary source that would settle it.
4. **Clean room.** Do not copy data, strings or fixtures from GPL/LGPL/AGPL projects. This includes
   `persian-calendar/*`, `avianey/Level`, `ilius/starcal` and any Persian-calendar or prayer-times library under those
   licences. Do not use them to "check" your work either.
5. **Keep the document.** When a source is a downloaded file (PDF, image), add it to `docs/sources/` and record its
   size, SHA-256 and content in [`docs/sources/MANIFEST.md`](docs/sources/MANIFEST.md). Cite it in the record's
   `title`, e.g. `Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)`.

## Workflow

1. Edit or add a file under `dataset/<region>/`. The record format is described in
   [`dataset/README.md`](dataset/README.md) and enforced by [`dataset/events.v1.json`](dataset/events.v1.json).
2. Validate against the schema and the cross-record rules (unique ids, references, day ranges, validity years):

   ```bash
   ./gradlew :tools:dataset:validate
   ```

3. Regenerate the typed Kotlin sources in `data/events` and format them:

   ```bash
   ./gradlew :tools:dataset:generateEvents spotlessApply
   ```

   Commit the regenerated files with the data. A test fails when they are stale.
4. Add or update the golden tests for the year(s) you touched. Iran uses the per-year holiday date sets in
   `tools/dataset/src/test/resources/golden/iran/` (with a `# source / url / retrieved / page / reviewer` header)
   and the daily fixtures of `:core:calendar`. Then run:

   ```bash
   ./gradlew :tools:dataset:test :data:events:test
   ```

5. Update [`docs/PROVENANCE.md`](docs/PROVENANCE.md) (Datasets section) and, if the change closes or opens a data gap,
   `docs/DATA_TODO.md`.
6. Open a pull request and complete the "Dataset changes" checklist. To report a wrong date without a pull request,
   use the *Holiday / event data correction* issue form. It also requires a primary source.

## Official calendars (Iran)

The daily tables of the Calendar Center's yearly calendars are imported, never typed. To add one:

1. Put the PDF in `docs/sources/iran/` as `Calendar-<solar year>.pdf` (for example `Calendar-1406.pdf`), and add the
   year's layout to `tools/sources/iran/official_calendar_sources.py` (an edition whose text layer cannot be read goes
   into `REJECTED` with the reason, never into the fixtures).
2. Add its row to [`docs/sources/iran/MANIFEST.md`](docs/sources/iran/MANIFEST.md). Running step 3 without one prints
   the row to paste — page count, byte size and SHA-256 — so nothing is measured by hand; fill in kind, layout, date
   supplied and the content description.

3. Import it (needs `poppler-utils`; `--check` only reports whether the fixtures are stale):

   ```bash
   tools/sources/iran/official_calendar_import.py
   ```

   This rewrites the daily fixture of every stored calendar (`golden/persian/official/<year>.csv`), the index
   `core/calendar/src/test/resources/golden/islamic-iran/official-calendars.csv`, the month-start history next to it
   (`official-month-starts-1381-1405.csv`), the override months and `dataset/iran/islamic-iran-overrides.json`, and
   the holiday goldens of `:tools:dataset`. It stops with an error rather than guessing if the pages read do not make
   up the whole year, a month name is unknown or a day does not follow the day above it; a misprint is corrected only
   through an `Erratum` that states the printed value and the reason, and only where the page prints exactly that.
   `tools/sources/iran/nowruz_instants_import.py` does the same for the list of past Nowruz instants
   (`tahvil-sal.txt`).
4. Refresh the calibration report and run the tests that read the fixtures:

   ```bash
   ./gradlew :core:astronomy:test -Ptaqvim.updateSnapshots=true
   ./gradlew :core:calendar:test :tools:dataset:test :tools:dataset:validate
   ```

   The tests iterate the index, so no test names a fixture and none has to be edited. Review the diff of
   [`docs/data-todo/islamic-calibration-report.md`](docs/data-todo/islamic-calibration-report.md): it is where the
   agreement per region changes (ADR-0040).

## Continuous integration

- **PR workflow, "Dataset validation" job:** `:tools:dataset:validate`, then the generator and golden tests
  (`:tools:dataset:test :data:events:test`). A pull request that removes a citation, breaks the schema or leaves
  generated code stale fails here.
- **Dataset citation links (weekly):** every citation and link URL must still resolve. Some official sites block
  data-centre networks, so a failure is checked by hand before any record changes.

## Reviewer sign-off checklist

A second person, other than the compiler of the record, checks each new or changed record against the cited page.
Only after that is `reviewedBy` changed from `pending` to their name.

- [ ] The cited URL or `docs/sources` file is the primary publication and the page number is right.
- [ ] Every holiday on the cited pages is present, and no record claims a holiday that the page does not mark.
- [ ] Dates, calendar, rule type and validity years match the page (check at least two years for recurring rules).
- [ ] The `fa` title matches the source's wording; other languages are translations, not new claims.
- [ ] `isHoliday`, `source` and `category` are correct.
- [ ] Golden tests cover the years in the citation and pass.
- [ ] Anything uncertain is in `docs/DATA_TODO.md`, not in the record.
