# Taqvim event dataset

Holidays and observances shipped with Taqvim (docs/PLAN.md §5). Every record is compiled from a primary source and
carries at least one citation. Records that cannot be verified go to `docs/DATA_TODO.md`, never into this directory.
How to add or correct records, CI and the reviewer sign-off checklist: [`CONTRIBUTING-DATA.md`](../CONTRIBUTING-DATA.md).

## Files

- `events.v1.json` — JSON Schema (draft 2020-12) for one dataset file.
- `<region>/*.json` — dataset files (added by D-02 … D-06), each `{ "schemaVersion": 1, "events": [ … ] }`.
- `islamic-iran-overrides.v1.json` and `iran/islamic-iran-overrides.json` — the D-07 table of official Iranian Hijri
  month starts, one record per month with its Persian first day and calendar page. `*-overrides.json` files are not
  event files; `./gradlew :tools:dataset:validate` also runs `validateOverrides` (no duplicate months, no gaps, 29/30-day
  months). Generated, never edited by hand: `tools/iran/official_calendar_import.py` writes it from the official
  calendars in `docs/sources` together with the `:core:calendar` golden fixtures (ADR-0040).

## Record format

Fields follow `EventDefinition` (docs/PLAN.md §4.2) plus `updated` (ISO date) and `reviewedBy`:

| Field | Notes |
|---|---|
| `id` | Stable dot-separated slug, unique across all files (`ir.nowruz.1`) |
| `calendar` | `PERSIAN`, `ISLAMIC`, `GREGORIAN`, `NEPALI` |
| `source` | `IRAN_OFFICIAL`, `AFGHANISTAN_OFFICIAL`, `NEPAL_OFFICIAL`, `INTERNATIONAL`, `ANCIENT_IRAN` (`USER` is app-only) |
| `category` | `NATIONAL`, `RELIGIOUS`, `INTERNATIONAL`, `CULTURAL`, `ASTRONOMICAL` (`PERSONAL` is app-only) |
| `isHoliday` | Official day off |
| `title` | Language tag → text; `fa` is mandatory, or `ne` for records with only an official Nepali title (ADR-0038) |
| `rule` | Object with a `type` discriminator: `Fixed`, `NthWeekdayOfMonth`, `LastWeekdayOfMonth`, `LastDayOfMonth`, `Single`, `NthDayOfYear`, `RelativeToEvent`, `Astronomical`, `LunarTithi` (Bikram Sambat lunar festivals, `NEPALI` only; ADR-0038) |
| `validity` | Optional `fromYear` / `toYear` in a named calendar, with its own citation |
| `oneOffReason` | Required for `Single` rules, forbidden otherwise: why the day is a one-off decision no recurring rule can express (ADR-0036). A day that recurs is a `Fixed` (or other) rule with `validity.fromYear`, never one `Single` per year |
| `flags`, `aliases`, `links` | Optional |
| `citations` | At least one `{ url, title, page?, retrieved? }` |

## Validation

```bash
./gradlew :tools:dataset:validate
```

The validator checks each file against the schema, then all schema-valid files together: unique ids, existing
`RelativeToEvent` targets (not the event itself), days that exist in the record's calendar (Persian 31/30, Islamic 30,
Gregorian by month with 29 February, Nepali 32, Islamic years ≤ 355 days) and validity years. Exit code 0 means valid,
1 means issues were printed, 2 means wrong arguments.
