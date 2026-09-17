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

## Addendum 2026-09-17 — Afghan official events use tabular type II (DT-002, DT-031–DT-033)

No Afghan primary source documents which Hijri calendar the country uses, and the owner's directive of 2026-09-17 asks
for a computed calendar wherever published data is missing. Four Afghan announcements in the dataset name a Hijri day
(27 Shaban 1447, 9 Dhu al-Hijjah 1447, 1 and 5 Rabi al-Awwal 1448, all from Bakhtar News Agency). Placing them with
each computed calendar:

| Calendar | Announced days matched |
|---|---|
| Tabular type II (`TABULAR_16`) | 4 of 4 |
| Umm al-Qura (ADR-0028) | 2 of 4 |
| Iranian crescent calendar (ADR-0037) | 1 of 4 |

Decision:

- `AFGHANISTAN_OFFICIAL` events always use tabular type II (`DEFAULT_SOURCE_VARIANTS`), whatever the user's preferred
  variant; other non-Iranian sources still follow the preference.
- Every official occurrence placed by the Islamic calendar carries the `DateOrigin` of its day (ADR-0037):
  `DayEvents.officialOrigins`, shown in the event's source card as "Date: Computed", "Date: Official announced date" or
  "Date: Printed calendar". Afghan dates are always "Computed"; Iranian dates are "Computed" unless an imported
  official override covers the month.

Consequences: Afghan Islamic holidays no longer move when the user changes the preferred variant. The match rests on
four dates; a published Afghan calendar or month announcements (DATA_TODO DT-033) would replace this evidence, and an
announced date can be applied through the Islamic month overrides without code changes.
