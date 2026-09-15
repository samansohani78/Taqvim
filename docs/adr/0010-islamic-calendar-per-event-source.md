# ADR-0010: Islamic calendar per event source

- **Status:** Accepted (the calculated-observational variant uses the crescent calendar, ADR-0027)
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-302 ("Islamic variant selection by source"), A-03…A-06

## Context

Lunar Hijri events from different sources must be placed on days computed with a suitable Islamic calendar: an
Iranian official holiday on the Iranian official calendar, while other sources may follow the user's preferred
variant. The plan asks for the selection to be data-driven.

## Decision

Product default, implemented as a parameter of the event lookup and policy (`:core:events`):

- `IRAN_OFFICIAL` events always use the Iranian official lunar calendar (A-05, `IranIslamicCalendar`).
- All other sources use the user's preferred `IslamicVariant`: `UMM_AL_QURA` → `UmmAlQuraCalendar`, `TABULAR_16` →
  tabular type II, `TABULAR_15` → tabular type I.
- `IRAN_OFFICIAL` as a user preference maps to `IranIslamicCalendar`; `CALCULATED_OBSERVATIONAL` falls back to tabular
  type II until A-06 is implemented (T-104).

## Consequences

- The mapping is configuration, not a statement about which calendar any country officially uses; sources such as
  Afghanistan's official events can be pointed at a different calendar once a primary source documents it.
- Changing the preferred variant only affects non-Iranian lunar events.
