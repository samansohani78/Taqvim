# Data audit — computed, not typed (Phase 1)

Audited 2026-09-17 against `main@4dc3a95`. Owner rule: the app must never need someone to type in new dates next
year. Everything that can be computed is computed at run time; bundled data is allowed only where computation is
impossible, and each such item is justified below with its maintenance action and how the app degrades without it.

Scope: every main source set, generated code (`data/events/.../generated/OfficialEvents*.kt`), `dataset/`, main
resources and assets of all modules (including `:wear`), Room (no prepopulated database: no `createFromAsset` /
`createFromFile` anywhere), override tables, `.properties` files and embedded constant arrays. A grep for 4-digit years
(13xx, 14xx, 19xx, 20xx) in main source sets found no per-year date data beyond the items listed here; the remaining
hits are comments, physical constants, epochs and definitional switch dates.

Legend for "Currently": **computed** (algorithm at run time), **bundled table** (static data read at run time),
**per-year manual** (data that must be extended every year).

## 1. Runtime data

### 1.1 Calendars

| Item | Location | Currently | Can it be computed? | Action |
|---|---|---|---|---|
| Persian year starts, leap years, Nowruz | `core/calendar/.../PersianYearStarts.kt`, `PersianCalendarSystem.kt`, `CalendarAstronomy.kt` (ADR-0026) | computed — true March equinox at the Tehran meridian for SH −3000…3000, exact mean-equinox continuation beyond; `PersianCalendarSystem.kt:16` confirms no year is overridden | yes | Done (main@c6f13e6): no runtime override; ADR-0026 addendum makes official data a golden oracle only, and `PersianComputedOnlyKonsistTest` fails if production code reads the official Persian fixtures or gains an override input. Official leap years 1206–1498, the 1404/1405 calendars and the Nowruz instants all agree with the computation. |
| Gregorian, Julian | `GregorianCalendarSystem.kt`, `JulianCalendar.kt` | computed (Fliegel & Van Flandern; Richards) | yes | None. |
| Islamic tabular (types I/II) | `TabularIslamicCalendar.kt` | computed | yes | None. |
| Iranian Islamic months — official table | optional asset `core/calendar/src/main/resources/ir/taqvim/core/calendar/islamic-iran-official.json` (byte copy of `dataset/iran/islamic-iran-overrides.json`), parser `IslamicMonthOverrides.kt`; the Kotlin table `IranOfficialMonthStarts.kt` is deleted | **optional override**, off by default (ADR-0037); the runtime default is the computed crescent calendar | **no** for the announced dates themselves: Iranian month starts are decided by moon-sighting announcements, which a criterion only predicts (23/25 agreement, ADR-0027). **yes** for every date the app shows: the calibrated crescent calendar computes every month. | Done (main@27950c5, ADR-0037): computed by default; Settings → Calendar → "Official Hijri dates" switches on the bundled official file, imports a newer file through the file picker (512 KiB limit, no network) or removes it. A malformed or expired override is ignored with a notice. Day details and the converter label each Hijri date "Computed", "Official announced date" or "Printed calendar". Without an override 2 of the 25 announced months (AH 1446–1448) start one day later than announced. Maintenance: optional, yearly after a new official calendar. |
| Iranian Islamic months — crescent calendar | `IranCrescentCalendar.kt`, `IranCrescentMonths.kt`, `IranCrescentSighting.kt` (ADR-0027) | computed (Yallop at five calibration sites, mean months beyond AH −3000…3000) | yes | None. The five sites (`IranCrescentSighting.kt:22–28`) are fixed calibration constants, not per-year data. |
| Umm al-Qura — historical years | `core/calendar/.../UmmAlQuraTable.kt` (120 masks, AH 1300–1419), `UmmAlQuraMonths.kt` | bundled table of **historical** published months (ADR-0028) | **no** before AH 1420: no rule fits before 1392 and the rule documented for 1392–1419 reproduces 61 of 335 months. **yes** from AH 1420: the moonset rule gives all 36 months of 1420–1422 and the criterion 334 of 336 of 1423–1450. | Done (main@9c33702, ADR-0028 addendum): AH 1420 onward computed at run time; only AH 1300–1419 bundled — a closed historical record that never needs extending. The printed 1420–1450 months are a golden (`PUBLISHED_1420_1450_MASKS`); the app differs from them in exactly two marginal months (1 Jumada II 1427 one day earlier, 1 Jumada II 1446 one day later). Maintenance: none. |
| Umm al-Qura — criterion | `UmmAlQuraCriterion.kt` | computed | yes | None. |
| Nepali (Bikram Sambat) | `NepaliCalendarSystem.kt`, `NepaliMonthStarts.kt`, `SuryaSiddhantaSun.kt`, `KathmanduDaylight.kt` (ADR-0030) | computed from Surya Siddhanta sankrantis with the Nepal-time day rule inferred from official data | yes | None. The plan assumed a 100-year month-length table (A-07); Taqvim needs none — every year is computed and checked against the official National Panchang 2082/2083 (24/24) and 84 of 89 NTA month starts. |
| Hebrew calendar | `HebrewCalendar.kt`, `HebrewCalendarSystem.kt` | computed (molad and postponements) | yes | None. |
| Easter and movable feasts | `GregorianComputus.kt`, `JulianComputus.kt`, `ChristianMovableFeasts.kt` | computed | yes | None. |
| US daylight-saving rules | `UsDaylightSavingRules.kt` | computed (statute eras) | yes | None. Future law changes would need a code change (not per-year). |
| Hijri user offset | `HijriDateResolver.kt` | user setting (±2 days, 30-day expiry) | n/a (user choice) | None. |
| Week numbers, month maths, day distances | `CalendarMath.kt`, `CalendarLimits.kt` | computed | yes | None. |
| Time-zone rules | platform tz database via kotlinx-datetime | platform data, updated by the OS | no (political decisions) | None in the app; Android updates tzdata. |

### 1.2 Events and holidays

| Item | Location | Currently | Can it be computed? | Action |
|---|---|---|---|---|
| Iran official holidays and observances | `dataset/iran/iran-official-holidays.json` → generated `OfficialEvents*.kt` | rules: 26 `Fixed` (Persian and Islamic months) + 1 `LastDayOfMonth`; regenerate for any year | yes (from law) | None. Islamic-dated holidays follow the source's Islamic calendar, which becomes computed by default (P1c). Maintenance: edit a rule only when the holiday law changes (rare, dataset PR, D-09). |
| Afghanistan official holidays | `dataset/afghanistan/afghanistan-official-holidays.json` | rules: 6 `Fixed` records with `validity.fromYear` (Soviet withdrawal 26 Dalw, Kabul victory 24 Asad, Independence 28 Asad; Arafah 9, Eid al-Adha 10 and 11 Dhu al-Hijjah) | **yes** for the recurring national days (Soviet withdrawal 26 Dalw, Victory 24 Asad, Independence 28 Asad) and for the Eid al-Adha/Arafa days (Dhu al-Hijjah 9–12 by rule); **no** for per-year announcement quirks (e.g. the 13th instead of the 12th in 1447) and the Afghan moon-sighting month starts (DT-033) | Done (main@d995b10, ADR-0036): the six recurring days are rules with their Bakhtar citations; the weekday-dependent 13 Dhu al-Hijjah 1447 day off was removed (DT-031). `Single` rules now require `oneOffReason`, none ship, and `NoPerYearManualDataTest` fails on any per-year date data outside a justified allow-list. Maintenance: rule edits only when Afghan law changes (DT-032). |
| UN international days | `dataset/international/un-international-days.json` | rules: 89 `Fixed` + 4 `NthWeekdayOfMonth` | yes (UN resolutions define rules) | None. New UN days: dataset PR when the UN adopts one (a few per year, optional). |
| Ancient Iranian festivals | `dataset/ancient-iran/ancient-iranian-festivals.json` | rules: 2 `Fixed` | yes | None. |
| Generated event code | `data/events/.../generated/OfficialEvents*.kt` (117 `Fixed`, 4 `NthWeekdayOfMonth`, 1 `LastDayOfMonth`, 7 `Single`) | generated from `dataset/` | n/a | Regenerated by D-08; the 7 `Single` rules disappear with P1e. |
| Occurrences, holidays, workdays, recurrence | `core/events` (`OccurrenceCalculator`, `RuleEngine`), `core/workdays/WorkdayCalculator.kt`, `core/ics` (`RecurrenceEngine`, `OccurrenceSeries`) | computed | yes | None. |
| Official-event reminders | `feature/notification` | computed from event rules each year (T-1002) | yes | None. |

### 1.3 Astronomy, prayer times, directions

| Item | Location | Currently | Can it be computed? | Action |
|---|---|---|---|---|
| Sun, Moon, phases, seasons, apsides, eclipses, libration, lunar tilt | `core/astronomy` (cosinekitty/astronomy, MIT) | computed | yes | None. |
| Zodiac (tropical, IAU), Moon in Scorpio | `Zodiac.kt` | computed; IAU boundaries are definitional constants inside the library | yes | None. |
| Planetary hours, tithi, houses, lots | `PlanetaryHours.kt`, `Tithi.kt`, `Houses.kt` | computed | yes | None. |
| Chinese New Year and animal year | `ChineseNewYear.kt`, `AnimalYear.kt:35` (2020 = Rat cycle anchor) | computed; the anchor is a fixed cycle epoch, not per-year data | yes | None. |
| Crescent visibility (Yallop, Odeh) | `CrescentVisibility.kt`, `OdehCriterion.kt` | computed; thresholds are the published criteria constants | yes | None. |
| Prayer times | `core/praytimes` (`SolarEphemeris`, `SunDay`, `HighLatitude`, `PrayerMethodParameters.kt:136–141`) | computed; the method table holds each authority's definitional angles and published adjustment minutes, not dates | yes | None. A method change by an authority would be a code change (rare). |
| Qibla, great-circle distances | `Qibla.kt`, `GreatCircle.kt` | computed | yes | None. |
| Magnetic declination / inclination | Android `GeomagneticField` (World Magnetic Model) | platform model with a validity period, updated by the OS | no (measured field model) | None in the app. |

### 1.4 Non-date data (cannot be computed)

| Item | Location | Currently | Can it be computed? | Action |
|---|---|---|---|---|
| City catalog (7 342 places, zones) | `data/location/src/main/resources/.../cities.tsv` | bundled table (Natural Earth, public domain) | no — geography | None per year. Refresh only if a newer Natural Earth release is wanted. Without it: coordinates entry and GPS still work. |
| World outline, time-zone bands, tectonic plates | `feature/map/src/main/assets/map/*.txt` | bundled geometry (Natural Earth; Matthews 2016, CC BY) | no — geography | None per year. Time-zone offsets are computed from the device tz rules (main@4a5c127); only the band shapes are bundled. |
| Language table, date/number formats | `core/i18n/src/main/resources/.../languages.properties`, `formats.properties` (CLDR 48) | bundled table | no — language data | None per year; regenerate on a new CLDR release (optional). |
| Hebrew and Bikram Sambat month names | `hebrew-months.properties` (CLDR 48), `bikram-sambat.properties` (official Panchang, NTA spellings) | bundled names | no — language data | None per year. Missing languages fall back to numbers or Latin names (DT-037, DT-007). |
| NLP lexicon | `core/nlp/src/main/resources/.../lexicon.properties` | bundled words | no — language data | None per year. |
| UI strings | `*/src/main/res/values*/strings.xml` (including `:wear`) | bundled | no | Translation workflow (T-1702). |
| Licence texts and third-party list | `feature/about/src/main/assets/licenses/` | generated from the dependency report | n/a | Regenerated by `tools/licenses/about_licenses.py`. |

## 2. Test-only fixtures (golden oracles, never read at run time)

| Directory | Contents | Used by |
|---|---|---|
| `core/calendar/src/test/resources/golden/persian/` | official leap years 1206–1498, Nowruz instants (1404/1405 title pages and the 1360–1403 list), the daily calendars of 21 years 1381–1405 (`official/<year>.csv`), century boundaries | Persian calendar tests |
| `core/calendar/src/test/resources/golden/islamic-iran/` | official month starts of the override calendars (1446–1448), the 262-month history 1381–1405 with its basis (printed, derived, announced) and the index of imported calendars, all written by `tools/sources/iran/official_calendar_import.py` (ADR-0040) | Iranian Islamic calendar tests |
| `core/calendar/src/test/resources/golden/nepal/` | National Panchang 2082/2083, NTA month starts | Nepali calendar tests |
| `core/calendar/src/test/resources/golden/usno/` | USNO Islamic, Jewish, Christian observances, US DST | calendar goldens (ADR-0025) |
| `core/astronomy/src/test/resources/golden/usno/`, `nasa/`, `iran/`, `yallop/`, `odeh/` | seasons, apsides, moon phases, eclipses, official Iranian eclipses and equinoxes, crescent observation facts | astronomy goldens |
| `core/praytimes/src/test/resources/golden/iran-prayer-times-1405/`, `noaa/` | 31 official city timetables, NOAA samples | prayer-time goldens |
| `core/ics/.../golden/ics/`, `data/events/.../golden/ics/` | 32 iCalendar fixtures | ICS goldens |
| `core/nlp/.../golden/nlp/` | phrase and snippet corpora | NLP goldens |
| `tools/dataset/src/test/resources/golden/` | Iran, Afghanistan, UN, ancient dataset goldens; the official Iranian holiday sets of 21 years 1381–1405 (`iran/iran-official-holidays-<year>.csv`, written by `tools/sources/iran/official_calendar_import.py`) | dataset goldens; `IranOfficialHolidayHistoryTest` checks the Iran rules and their `validity` ranges against every year |
| `core/testing/.../golden/sample/` | fixture-format self-test | test infrastructure |
| `docs/sources/` | primary source documents (Iranian sources in `docs/sources/iran/`, inventory in its MANIFEST.md) and the USNO archive | provenance only, never packaged |

## 3. Summary

| Category | Rows |
|---|---|
| Runtime, computed by the app | 22 |
| Runtime, platform-provided and OS-updated (tz database, magnetic model) | 2 |
| Runtime, user setting (Hijri offset) | 1 |
| Runtime, generated from the dataset rules | 1 |
| Runtime, bundled non-date data (geography, language, licences) | 7 |
| Runtime, per-year manual — converted in this change | 2 (Iranian official Islamic months → optional override; Afghanistan `Single` records → rules) |
| Runtime, historical table (not computable), justified | 1 (Umm al-Qura AH 1300–1419) |
| **Runtime rows total** | **36** |
| Test-only golden fixture groups | 11 |

## Conversions in this change

- [x] P1b (main@c6f13e6) — Persian: no runtime official override; the official table is a golden only (policy wording in
      ADR-0008/ADR-0026 updated).
- [x] P1c (main@27950c5, ADR-0037) — Iranian Islamic months: computed by default; the official months become an optional imported JSON
      override; every Hijri date labelled "computed" or "official override".
- [x] P1d (main@9c33702) — Umm al-Qura: computed from AH 1420 (moonset rule 1420–1422, criterion from 1423); bundled table only for AH 1300–1419.
- [x] P1e (main@d995b10, ADR-0036) — Afghanistan `Single` records converted to recurring rules; `NoPerYearManualDataTest` added.
- [x] P1f (main@9f23c0b) — completeness test: every day of SH 1380–1480 (36 890 days) in the Persian, Gregorian,
      Nepali and Hebrew calendars and all five Islamic variants, with every dataset rule, the day assembler under each
      language default, prayer times (Tehran, Kabul, Tromsø) and daily and yearly astronomy — no missing values, no
      exceptions (`CalendarCompletenessTest`, `SkyAndTimesCompletenessTest`, ~40 s in the normal test tasks). It found
      and fixed a planetary-hours exception on polar-day edges.
