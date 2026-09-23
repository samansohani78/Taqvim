# ADR-0044: The `Astronomical`/`FULL_MOON` rule — instant, time zone and tie-break

- **Status:** Accepted
- **Date:** 2026-09-23
- **Plan reference:** docs/PLAN.md §5 D-05; docs/DATA_TODO.md DT-040; ADR-0031, ADR-0036, ADR-0038, ADR-0042

## Context

DT-040 blocks Vesak, the UN's Day of the Full Moon (General Assembly resolution 54/115, 1999): "the day of the full
moon in the month of May". The schema already has an `EventRule.Astronomical` rule with a `FULL_MOON` kind
(`dataset/events.v1.json`, `core/events/.../EventRule.kt`), and `OccurrenceCalculator` already turns a kind's instants
into days through a `timeZone` field — but nothing ever used it: `NEW_MOON` and `FULL_MOON` happen about every 29.5
days, so the rule as it stood would return every full moon of the year, not the one the UN names, and no convention
said which time zone or which of a rare month's two full moons a record should use. `docs/data-todo/un-days-sourcing.md`
declined to guess one. This ADR is that decision, reasoned the same way ADR-0038 reconstructed Nepal's lunar
festival rule: from the astronomy already in the app, checked against an external authority.

ADR-0038 solved the same class of problem — an astronomical event mapped to a civil day — for Nepal's tithi
festivals, anchored to Kathmandu civil time because they are Nepal's own domestic holidays observed there. Vesak has
no such home: it is a UN international day observed worldwide, and the dataset's `Astronomical.timeZone` is a fixed
IANA zone chosen per record, not the viewer's device zone (ADR-0031's "own zone" pattern, not a display-time
conversion).

DT-040 originally named a second blocker: no official Persian source names Vesak, and the dataset's own rule (until
now) kept an INTERNATIONAL-source record out without one. ADR-0042 (landed the same day, numbered ahead of this one)
removed that blocker for every well-sourced UN day, Vesak included: a record may carry a machine-translated `fa`
title marked `titleReview: ["fa"]` instead. This ADR covers only the rule/time-zone/tie-break question; §5 below
records that the record itself now ships, using ADR-0042's marker.

## Decision

### 1. The instant

The event is the **geocentric full moon**: the moment the Moon's apparent geocentric ecliptic longitude minus the
Sun's reaches 180° (mod 360°) — `Sky.moonPhaseDegrees(instant) == 180.0`, found by `Sky.moonQuarters` in
`:core:astronomy`, the same cosinekitty/astronomy 2.1.19 (Meeus-based) façade every other Moon-phase and season
calendar rule in this app already uses. It is geocentric, not topocentric: no observer position enters the
computation, unlike Nepal's sunrise- or sunset-anchored tithis. `UsnoMoonPhasesTest` checks every principal phase this
library finds — full moons included — against the U.S. Naval Observatory API to within two minutes over 19,839
phases, 1700–2100 (`core/astronomy/src/test/resources/golden/usno/moon-phases-1700-2100.csv`).

### 2. The time zone: UTC

`EventRule.Astronomical.timeZone` names one IANA zone per record (already required by the schema before this ADR);
`OccurrenceCalculator.astronomicalDays` converts the instant to a civil day in that zone exactly as every other
instant-to-day conversion in the app does (`Instant.toLocalDateTime(zone).date`, `core/calendar/DateTimeBridge.kt`).
For Nepal's lunar festivals the right zone is Kathmandu's, because the festival belongs to Nepal. Vesak belongs to no
one country: dozens of Buddhist-majority and other states observe it, at different local times, and naming any one
of their zones (Colombo, Bangkok, Kathmandu) would privilege that country's civil day over the others' without the
UN's resolution saying so. **UTC** is the zone-agnostic choice already used by this exact codebase for globally
anchored instants with no inherent local zone: ADR-0031 records that imported iCalendar series in UTC are stored and
dated by their UTC day, unconverted, for the same reason. Once Vesak's day is fixed in UTC it is an ordinary all-day
dataset event, and ADR-0031 rule 1 ("all-day events keep their dates in every display zone") takes over: every viewer
sees the same Gregorian day regardless of device zone, including on opposite sides of the date line.

### 3. Selecting the month, and the tie-break

Equinoxes and solstices happen once a year, so `Astronomical.month` (new in this ADR, alongside `kind`, `offsetDays`
and `timeZone`) is left `null` for them and every instant in the calendar year is kept, as before. `NEW_MOON` and
`FULL_MOON` happen about once a lunar month, so naming `month` (1–12, in the record's own `calendar`, reusing the
schema's existing `month` `$def`) is how a record picks the one the UN names: `OccurrenceCalculator` keeps only the
earliest of the year's `kind` days that falls in that civil month.

"Earliest" is the tie-break for the one case a month can hold two: a "blue moon" month, when a full moon falls right
at the start and another right at the end (USNO's 1700–2100 range has 421 such Mays; the closest to today is 2026:
1 May 17:23 UTC and 31 May 08:45 UTC). The rule keeps 1 May, for the same reason a farmers'-almanac "blue moon" is
the *second* full moon of a month and not the first: the earlier one is the month's ordinary phase, the later one is
the extra. This needs no metadata beyond `month` and is order-independent to compute. A month with zero `FULL_MOON`
days is not reachable for `month = 5` — the synodic month (~29.53 days) cannot skip a 31-day May — so it does not
arise for Vesak; for a rule that could reach it (a different kind or month), `OccurrenceCalculator` returns no
occurrence that year, exactly as `EventRule.Fixed` does for a day a calendar doesn't have.

An instant exactly at civil midnight belongs to the day that begins then, not the day before, because
`Instant.toLocalDateTime(zone).date` already resolves it that way (the same rule every other instant-to-day
conversion in the app follows); no code change was needed for this case, only stating it here.

### 4. Validity

`Sky.moonQuarters` is checked against USNO to within two minutes over the full golden range, 1700–2100
(`UsnoMoonPhasesTest`). Outside that checked window the same computation still returns an answer — cosinekitty's
lunar theory is not restricted to a fitted range, unlike the tabulated calendars elsewhere in this app — but no
external authority has verified it here, exactly as for the equinox/solstice and apsis computations already in
`:core:astronomy`. The Vesak record's `validity.fromYear` is 1999, the year of resolution 54/115, and is otherwise
open-ended; nothing in the rule itself bounds the years it will compute.

### 5. The Vesak record

`un.vesak-day` (`dataset/international/un-international-days.json`) uses `Astronomical(kind = FULL_MOON,
offsetDays = 0, timeZone = "UTC", month = 5)`, `validity.fromYear = 1999` citing resolution 54/115, and the UN's own
English title "Vesak, the Day of the Full Moon" (`un.org/en/observances/list-days-weeks`,
`un.org/en/observances/vesak-day`). No official Persian source names Vesak (checked 2026-09-23: `iran.un.org` and
`unic-ir.org`, live and the archived 2012-10-05 UNIC Tehran page, name no Buddhist or Vesak observance;
`un.org/fa/observances/vesak-day` does not exist) — before ADR-0042 that would have kept the record out entirely, as
this ADR's first version recorded. It now ships with a machine-translated `fa` title marked `titleReview: ["fa"]`,
exactly as ADR-0042's other 132 records do; `UnInternationalDaysTest` accepts it on that basis. The rule mechanism
itself is exercised independently in `SkyAstronomicalEventSourceTest` (`:data:events`) against USNO-derived
full-moon dates for 2020–2030 (including the 2026 tie-break), so the rule and the dataset record were verified apart
from each other.

## Consequences

- Any future `Astronomical`/`NEW_MOON` or `FULL_MOON` record (e.g. an Islamic-calendar rule expressed this way) reuses
  the same `month` field and earliest-in-month tie-break; a record that truly wants every occurrence (unlikely, but
  not forbidden) leaves `month` unset.
- `data/events`'s default `AstronomicalEventSource` (`SkyAstronomicalEventSource`) now backs every production
  `EventLookup`/`OccurrenceCalculator`/`CalculatorOfficialEventSchedule` built from the dataset, so the day any
  `Astronomical` rule computes is available wherever dataset events are shown, searched or reminded — not only where
  a caller happened to supply one. Before this, every one of those call sites defaulted to `astronomy = null`, which
  `OccurrenceCalculator` turns into a thrown `IllegalStateException` the moment any `Astronomical` record exists; the
  schema already allowed such a record, so this was a live crash waiting on the first one, not a hypothetical.
- Vesak's `fa` title is machine-translated and unreviewed like the app's other MT strings; a human reviewer removes
  its `titleReview` tag once a native title (official or otherwise checked) replaces it, per ADR-0042.
