# ADR-0046: A `Week` rule for multi-day observances, and the ten UN international weeks

- **Status:** Accepted
- **Date:** 2026-09-24
- **Plan reference:** docs/PLAN.md §5 D-05; docs/data-todo/un-days-sourcing.md; ADR-0038, ADR-0042, ADR-0044

## Context

`docs/data-todo/un-days-without-persian-title.tsv` has carried this note since the 2026-09-18 sourcing pass: "the 10
international weeks on the UN list are not included: the dataset has no week rule." `un.org/en/observances/list-days-weeks`
lists ten observances that span several days rather than one: World Interfaith Harmony Week, the Week of Solidarity
with the Peoples Struggling against Racism and Racial Discrimination, World Immunization Week, the Week of Solidarity
with the Peoples of Non-Self-Governing Territories, World Breastfeeding Week, the International Week of Science and
Peace, World Antimicrobial Resistance Awareness Week, Global Media and Information Literacy Week, World Space Week
and Disarmament Week. None of the existing rule types can express "several consecutive days," and none had a record.

The schema already has a precedent for a multi-day occurrence: `LunarTithi` with `endTithi` set (ADR-0038) returns
every day from its start tithi through its end tithi as a separate `Jdn`, each becoming its own `Occurrence` of the
same record — this is how the app already shows a multi-day Nepali festival (Tihar, Dashain) on each of its days,
with no change needed to `EventLookup`, `DayEventsAssembler` or any UI code. A `Week` rule follows the same shape:
compute a start day and a length, then return every day of that span.

## Decision

### 1. The rule: a start plus a length

`EventRule.Week(start: EventRule, lengthDays: Int)` (`core/events/EventRule.kt`) returns `lengthDays` consecutive
days beginning wherever `start` resolves that year. `start` is itself an `EventRule`, restricted by a constructor
`require` to `Fixed`, `NthWeekdayOfMonth` or `LastWeekdayOfMonth` — the three rule types PLAN's own review named
("a fixed date, or an ordinal weekday") and the only three that resolve to a single day without needing another
event (`RelativeToEvent`) or astronomy (`Astronomical`) to already be resolved first. Reusing the existing rule types
as `start` means no new "day" vocabulary was invented: a week that starts on a fixed date is `Week(Fixed(m, d),
n)`, and a week that starts on an ordinal weekday (e.g. a hypothetical "the week of the third Friday of March") is
`Week(NthWeekdayOfMonth(3, FRIDAY, 3), n)`, with no other change to either rule.

`lengthDays` is bounded 2–14 (schema `minimum`/`maximum`, and the same bounds in the constructor): 2 as the smallest
span that isn't just a `Fixed` day, 14 as twice the longest UN week actually found (Global Media and Information
Literacy Week's 8 days) with headroom, so a mis-typed `lengthDays` of, say, 365 is rejected rather than silently
producing a year-long "week."

`OccurrenceCalculator.directDays` (`core/events/OccurrenceCalculator.kt`) handles `Week` by resolving `start` through
the same `directDays` function (recursion of one level deep, since `start` cannot itself be a `Week`, `RelativeToEvent`
or `Astronomical`), then returning the JDN range `[start, start + lengthDays)`. A year in which `start` has no
occurrence (e.g. an ordinal-weekday start whose ordinal doesn't exist that month) gives the week no occurrence either,
exactly as `Fixed` gives no occurrence for a day a calendar doesn't have. A week may cross into the next calendar
month or year (none of the ten UN weeks do, but the rule doesn't forbid it); no double-counting results, because each
year's `Week` occurrence is computed from that year's own `start`, independently of the neighbouring year's.

`data/events`: no new evaluation code was needed beyond `OccurrenceCalculator` itself — `SkyAstronomicalEventSource`
and the rest of `:data:events` are untouched, since `Week` needs neither astronomy nor cross-event lookups. The one
`:data:events` test that independently re-derives every rule's expected dates, `CalendarCompletenessTest`, gained a
`Week` case (recursing into its own `expectedDates` for `start`, then expanding the range) so the ten new records are
checked against a second, independent implementation over 1380–1480 SH, the same as every other rule.

### 2. Schema and validator

`dataset/events.v1.json` adds `"Week"` to the rule `type` enum and a `ruleWeek` `$def` (`start`, `lengthDays`).
`start` is its own `$def`, `weekStart`, restricted by its own `type` enum to `Fixed` / `NthWeekdayOfMonth` /
`LastWeekdayOfMonth` and dispatching to the same `ruleFixed` / `ruleNthWeekdayOfMonth` / `ruleLastWeekdayOfMonth`
`$def`s the top-level rule already uses — a nested rule reusing existing `$def`s, not a parallel schema.
`tools/dataset/SemanticChecks.kt`'s day-range check (`DAY_OUT_OF_RANGE`, e.g. Persian day 31 in a 30-day month) is
made recursive so it also runs on a `Week`'s `start` when `start` is `Fixed`. Two invalid fixtures were added:
`35-week-length-too-short` (schema: `lengthDays` below the minimum) and `36-week-start-day-out-of-range` (semantic:
a `Fixed` start naming a day its month doesn't have). `DatasetValidatorTest`'s fixture-count and rule-type-coverage
assertions were updated accordingly, and `sample.json` gained a `test.week` record.

### 3. Code generator

`Week`'s `start` is a nested rule object, not a flat parameter like every other rule's arguments, so
`EventsCodeGenerator.ruleCode` special-cases `"Week"`: it recurses into `ruleCode` for `start` and emits
`EventRule.Week(start = <generated start>, lengthDays = <n>)`. `EventsCodeGeneratorTest` checks the nested call is
generated correctly, and the `generated-sample.kt.txt` snapshot was regenerated (`-Ptaqvim.updateSnapshots=true`)
to include `test.week`.

### 4. The ten records

All ten are `GREGORIAN`/`INTERNATIONAL`/`INTERNATIONAL` records with a `Fixed` start (none of the ten needs an
ordinal-weekday start — PLAN's third option, "the week containing X," never came up either: see the Science and
Peace week below). Each carries a machine-translated `fa` title marked `titleReview: ["fa"]` (ADR-0042), since none
has an official Persian-language UN source (the same 2026-09-18/23 searches of `iran.un.org` and the UNIC Tehran
archive that covered the 132 days found nothing for these ten either).

| id | span | since | primary source |
|---|---|---|---|
| `un.interfaith-harmony-week` | 1–7 Feb | 1910→2010¹ | A/RES/65/5 (2010) |
| `un.racism-solidarity-week` | 21–27 Mar | 1979 | A/RES/34/24 (1979) |
| `un.immunization-week` | 24–30 Apr | 2012 | WHO campaign page (its own event archive begins 2012) |
| `un.non-self-governing-week` | 25–31 May | 1999 | A/RES/54/91 (1999) |
| `un.breastfeeding-week` | 1–7 Aug | 2018 | WHO campaign page (2018 World Health Assembly endorsement) |
| `un.science-and-peace-week` | 9–15 Nov | 1988 | A/RES/43/61 (1988) |
| `un.amr-awareness-week` | 18–24 Nov | 2018 | WHO campaign page (dates fixed to 18–24 Nov "by 2018") |
| `un.media-information-literacy-week` | 24–31 Oct (8 days) | 2021 | A/RES/75/267 (2021) |
| `un.space-week` | 4–10 Oct | 1999 | A/RES/54/68 (1999) |
| `un.disarmament-week` | 24–30 Oct | 1978 | GA special session, Final Document (resolution S-10/2, 1978) |

¹ typo guard: 2010 is correct (A/RES/65/5, proposed by King Abdullah II of Jordan); there is no 1910 date anywhere in
this record.

Two of the ten needed a decision beyond "read the date off the page":

- **International Week of Science and Peace.** A/RES/43/61 (1988) proclaims it "each year during the week in which
  11 November falls" — a genuinely relative rule ("the week containing X") that this ADR's `start` vocabulary
  (`Fixed`/`NthWeekdayOfMonth`/`LastWeekdayOfMonth`) cannot express without guessing which weekday starts the UN's
  notion of "week" (Saturday, Sunday or Monday all give different answers in most years). Rather than guess a
  convention the resolution never states, the ten years checked (`un.org/en/observances/international-week-science-and-peace`,
  and every one of 2022–2026 independently) show the UN's own current listing fixing this at **9–15 November every
  year**, regardless of which weekday 11 November falls on that year (Fri in 2022, Sat in 2023, Mon in 2024, Tue in
  2025, Wed in 2026 — all report 9–15 November). This ADR follows the UN's own published practice — a `Fixed` start
  on 9 November, 7 days — rather than the 1988 resolution's un-followed original wording, on the same principle
  ADR-0044 used for Vesak: the record follows what the authority actually publishes, not a convention this project
  would otherwise have to invent.
- **World Antimicrobial Resistance Awareness Week.** Its dates moved every year from the 2015 launch (16–22
  November, as "World Antibiotic Awareness Week") through at least 2017 (13–19 November) before WHO's own campaign
  history page states the 18–24 November dates were standardized "by 2018" and have held every year since. `fromYear`
  is 2018, not the 2015 launch year, because a record with `fromYear = 2015` computing 18–24 November would claim a
  date the observance did not actually use in 2015–2017 — the same reasoning ADR-0044 §4 applied to a rule's
  checked range.

### 5. What was not added

No week was added on a guessed convention. If a future UN week's official wording is genuinely ambiguous (a "the
week containing X" whose weekday convention the source never states, and whose own current practice — unlike Science
and Peace week above — doesn't resolve the ambiguity either), it stays out of the dataset and is listed in
`docs/DATA_TODO.md`, per the same rule that already governs Vesak (DT-040) and the still-Persian-title-less 132 days
(DT-019).

## Consequences

- A future multi-day observance (UN or otherwise) whose start is a fixed date or an ordinal weekday is a `Week`
  record; one whose start needs another event or an astronomical instant is not yet expressible and would need a
  further ADR (the same gap `LunarTithi` and `Astronomical` each closed for their own single-day case).
- `docs/data-todo/un-days-without-persian-title.tsv`'s "the dataset has no week rule" line is removed: all ten weeks
  the UN currently lists are now records, each already carrying its `titleReview` entry in the same review queue as
  the 132 days.
- The dataset's UN-day count grows by ten (to 246), still counted as UN *international days and weeks* records
  toward D-05's ≥150 target.
