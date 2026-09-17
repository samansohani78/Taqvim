# ADR-0037: The Iranian Hijri calendar is computed by default; official dates are an optional override

- **Status:** Accepted (supersedes decision 5 of ADR-0027)
- **Date:** 2026-09-17
- **Plan reference:** docs/PLAN.md §6 A-05, T-104; ADR-0009, ADR-0027, ADR-0028; owner directive of 2026-09-17
  ("computed, not typed")

## Context

ADR-0027 computed the Iranian lunar months from the crescent rule but still let the 25 months published by the Calendar
Center (Ramadan 1446 – Shawwal 1448, the Kotlin table `IranOfficialMonthStarts`) win wherever they existed; the owner
had chosen "calculate, official wins" earlier. On 2026-09-17 the owner asked that no calendar value be typed by hand:
the Islamic calendar must default to the computed crescent calendar, the Iranian per-year table must become an optional,
user-updatable JSON override, and the UI must say whether a date is computed or an official override. Hand-typed tables
may survive only as test goldens.

## Decision

1. **Computed by default.** `IranIslamicCalendar()` with no table is the crescent calendar of ADR-0027 for every year.
   `IslamicVariant.IRAN_OFFICIAL` means "the Iranian calendar, plus the user's override if one is set". The Kotlin table
   `IranOfficialMonthStarts` is deleted; its months survive only in `dataset/iran/islamic-iran-overrides.json`, the
   golden that the tests compare with the computed months (23 of 25 agree).
2. **Override format.** A UTF-8 JSON file, at most 512 KiB: `schemaVersion` 1 and `months`, each with `hijriYear`,
   `hijriMonth`, `persianStart` {`year`, `month`, `day`} and a `citation` {`url`, `title`, `page`, `retrieved`}.
   `IslamicMonthOverrides.parse` never throws; it returns a failure with an `OverrideProblem` (not JSON, unsupported
   version, fewer than two months, months not consecutive or invalid, a month not 29 or 30 days, a missing citation).
   Joining with the computed months keeps ADR-0027 decision 5's rule, now applied only to an override.
3. **Bundled official file, off by default.** The dataset file is also shipped as the class-path resource
   `core/calendar/src/main/resources/ir/taqvim/core/calendar/islamic-iran-official.json`;
   `IslamicIranOverridesGoldenTest` keeps the two byte-identical. Settings › Calendar › "Official Hijri dates" has a
   switch for it (default off), an "Import a dates file…" button (Storage Access Framework `OpenDocument`, no storage
   permission) and "Use computed dates only", which removes any override.
4. **Stored with the preferences.** `UserPreferences.islamicOverride` keeps the origin (none, bundled official,
   imported) and the file text (proto fields 17 and 18), so an import does not depend on the picked document staying
   readable. A stored override that no longer parses is ignored (the app computes every date) and the screen shows a
   notice; an override whose last month has ended is still used for its months and the screen says so. Backups carry the
   override; reminders are rescheduled and widgets and surfaces refresh when it changes.
5. **Origin labels.** `CalendarArithmetic.originOf(jdn)` gives `DateOrigin.COMPUTED`, `OFFICIAL_OVERRIDE` (a day inside
   an Iranian override) or `PUBLISHED_CALENDAR` (an Umm al-Qura year bundled from the printed calendar, ADR-0028). Day
   details and the date converter show the label next to the Hijri date.
6. **Every consumer gets the same table.** Month, year, agenda, timeline, search, reminders, widgets and the converter
   build their Iranian calendar from `IslamicCalendarSelection.calendarFor(variant, overrides)`. Natural-language
   parsing, deep links and the standalone Wear app have no override and stay computed.

## Consequences

- **Behaviour change:** with no override, Iranian users see computed dates in AH 1446–1448. Two of the 25 published
  months start a day later in the app than announced until the user turns the official file on or imports one.
- New announcements need no app release: a user (or a later bundled file) can import an updated JSON file.
- A malformed or unsupported file is rejected with a specific message and changes nothing; the app works identically
  with no override.
- The bundled file is data, not code, so `NoPerYearManualDataTest` allow-lists its path.
