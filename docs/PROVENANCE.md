# Provenance Register

Required by `docs/PLAN.md` §0.1 (clean-room policy). Every core algorithm, dataset, and
golden fixture added to this repository must have an entry here **in the same commit** that
introduces it.

## Attestation

By adding an entry, the author attests that the implementation was written solely from the
listed public references and `docs/PLAN.md`, and that **no** source code, data file, string
resource, icon, or test fixture from any GPL/LGPL/AGPL/MPL project was fetched, read, quoted,
paraphrased, or recalled. Explicitly forbidden inputs include (non-exhaustive):
`persian-calendar/*` (all repositories), `avianey/Level`, `ilius/starcal`, and any
Persian-calendar or prayer-times GPL/LGPL library.

## Entry template

```markdown
### <ID> — <Component>
- **Module / files:** `core/<module>/src/main/kotlin/...`
- **Task:** T-xxx
- **Spec:** docs/PLAN.md §6 <A-xx>
- **References used (public only):**
  1. <Author, Title, Publisher/Journal, Year, URL, retrieved YYYY-MM-DD>
- **Validation oracle:** <library/table + license>
- **Deviations from spec:** <none | ADR-xxxx>
- **Author / date:** <name, YYYY-MM-DD>
- **Reviewer attestation:** <name, YYYY-MM-DD — "no forbidden sources consulted">
```

## Algorithms

### A-01 — JDN ↔ Gregorian
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/GregorianCalendarSystem.kt`
- **Task:** T-101
- **Spec:** docs/PLAN.md §6 A-01
- **References used (public only):**
  1. H. F. Fliegel and T. C. Van Flandern, "A Machine Algorithm for Processing Calendar Dates",
     *Communications of the ACM* 11(10):657, 1968. https://doi.org/10.1145/364096.364097 (retrieved 2026-09-13)
  2. ISO 8601-1:2019, *Date and time — Representations for information interchange* (proleptic Gregorian
     calendar, year 0000 = 1 BC).
  3. J. Meeus, *Astronomical Algorithms*, 2nd ed., Willmann-Bell, 1998, ch. 7 (Julian Day epoch, day of week).
- **Implementation note:** the 1968 integer formulas are valid only for positive day counts; dates are shifted
  by whole 400-year Gregorian cycles (146 097 days) before conversion (own derivation from the cycle length).
- **Validation oracle:** ICU4J 78.3 `GregorianCalendar` in proleptic mode (Unicode-3.0, test scope only),
  1 000 000 random JDNs in [−1 000 000, 5 000 000] with a fixed seed.
- **Deviations from spec:** none.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-02 — Persian (Solar Hijri) calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/` — `PersianCalendarSystem.kt`,
  `PersianLeapTable.kt` (generated), `PersianYearStartRule.kt`, `BirashkArithmetic.kt`
- **Task:** T-102
- **Spec:** docs/PLAN.md §6 A-02; deviation ADR-0008 (generated leap table instead of a runtime equinox search)
- **References used (public only):**
  1. cosinekitty/astronomy 2.1.19 (MIT), `Astronomy.seasons(year).marchEquinox`, https://github.com/cosinekitty/astronomy
     — equinox instants for the table generator and its test (library API only; no GPL source involved).
  2. C. Tøndering, "The Persian Calendar", *Calendar FAQ*, https://www.tondering.dk/claus/cal/persian.php (retrieved
     2026-09-13) — 2820-year cycle structure, leap positions within a cycle, and the AP 475 anchor (fallback only).
  3. Wikipedia, "Solar Hijri calendar", https://en.wikipedia.org/wiki/Solar_Hijri_calendar (retrieved 2026-09-13) —
     cross-check of the 29/33/37-year and 128/132-year grouping; its "1925" anchor statement was not used.
- **Implementation note:** the year-start rule, the table encoding and the fallback counting (closed-form leap counts
  per period, grand cycle and cycle, anchored at the table edges) are own work from the definitions above.
- **Generator:** a one-off program run on 2026-09-13 applied the rule to equinoxes for SH −3000…3000 (183 ms);
  `PersianCalendarAstronomyTest` is the executable specification and prints the regenerated constant on mismatch.
- **Validation:** Calendar Center official leap years 1206–1498 (293 years), official calendars 1404 and 1405
  (730 days, weekday included), official Nowruz instants 1404/1405, the Calendar Center century note, and ICU4J
  78.3 `PersianCalendar` for 1200–1500 (first day of every year plus 100 000 random days).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-05 — Iranian official lunar Hijri calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/` — `IranIslamicCalendar.kt`,
  `IslamicMonthTable.kt`, `IranOfficialMonthStarts.kt`, `HijriDateResolver.kt`
- **Task:** T-104 (A-06 deferred, ADR-0009)
- **Spec:** docs/PLAN.md §6 A-05; design ADR-0009 (tabular estimate aligned at the table edges; precedence official >
  user offset > estimate; ±2-day offset expiring after 30 days)
- **Data:** month starts Ramadan 1446 – Shawwal 1448 from the Calendar Center's official calendars of 1404 and 1405 SH
  (see the "Iran official calendar sources" dataset entry). Ramadan 1446's start is derived from the 1404 calendar
  printing 20 Ramadan on 1 Farvardin 1404; its 29-day length from 1 Shawwal on 11 Farvardin.
- **Validation:** every day of the official 1404/1405 calendars (730 lunar dates), the 26 published month starts and
  lengths, structural invariants for 1440–1455, 100 000 random round-trips.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-300 — Event rule engine
- **Module / files:** `core/events/src/main/kotlin/ir/taqvim/core/events/` — `EventDefinition.kt`, `EventRule.kt`,
  `OccurrenceCalculator.kt`
- **Task:** T-300
- **Spec:** docs/PLAN.md §4.2 (`EventDefinition`, `EventRule`, `Occurrence`) and T-300; own implementation on top of
  the `:core:calendar` utilities (T-106) — no external code or data.
- **Behaviour choices:** a `Fixed`/`Single` day that does not exist in a year (e.g. 30 Esfand in a common year) has no
  occurrence; `LastWeekdayOfMonth` offsets and `Astronomical` offsets may leave the month but keep the rule year;
  `RelativeToEvent` resolves across calendars and rejects cycles, self references and unknown targets up front;
  astronomical instants come from an injected `AstronomicalEventSource` (implemented by T-403).
- **Validation oracles:** java.time (Gregorian weekday/day-of-year rules), ICU4J 78.3 `IslamicCalendar(ISLAMIC_CIVIL)`
  and `PersianCalendar` (test scope only); the official 1404 calendar's "last Friday of Ramadan" for the Iranian lunar
  calendar.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-09 — Solar position (NOAA)
- **Module / files:** `core/praytimes/src/main/kotlin/ir/taqvim/core/praytimes/NoaaSolarCalculator.kt`
- **Task:** T-400
- **Spec:** docs/PLAN.md §6 A-09
- **References used (public only):**
  1. NOAA Global Monitoring Laboratory, "NOAA Solar Calculations" spreadsheet (day version),
     https://gml.noaa.gov/grad/solcalc/NOAA_Solar_Calculations_day.ods (retrieved 2026-09-13; US government work,
     public domain). Formulas transcribed column by column: Julian century, geometric mean longitude/anomaly,
     eccentricity, equation of centre, apparent longitude, obliquity, declination, equation of time, sunrise hour angle
     (zenith 90.833°), solar noon, true solar time, hour angle, zenith, refraction, azimuth.
  2. NOAA GML, "Solar Calculation Details", https://gml.noaa.gov/grad/solcalc/calcdetails.html (retrieved 2026-09-13) —
     method after J. Meeus, *Astronomical Algorithms*; stated validity 1901–2099.
- **Implementation note:** own Kotlin transcription; additions beyond the spreadsheet are argument clamping (no NaN),
  a defined azimuth when the Sun is at the zenith or the observer at a pole, and `null` rise/set on polar days/nights.
- **Validation oracle:** the spreadsheet's own cached results for its sample (40°N, 105°W, UTC−7, 2010-06-21, 240 rows).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-202 — CLDR plural-rule evaluator
- **Module / files:** `core/i18n/src/main/kotlin/ir/taqvim/core/i18n/PluralRules.kt`
- **Task:** T-202
- **References used (public only):** Unicode Technical Standard #35, Part 3 "Numbers", section "Language Plural Rules"
  (https://www.unicode.org/reports/tr35/tr35-numbers.html) — rule syntax and operands; integer operands only.
- **Validation oracle:** ICU4J 78.3 `PluralRules` for all 24 launch languages, n = 0…1000 plus large values.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-301 / T-302 — Event lookup cache and visibility policy
- **Module / files:** `core/events/src/main/kotlin/ir/taqvim/core/events/EventLookup.kt`, `EventVisibilityPolicy.kt`
- **Origin:** own work from docs/PLAN.md T-301/T-302 on the T-300 rule engine; no external code or data. The Iranian
  lunar date used in the lookup test comes from the A-05 official table.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-11 — Qibla and great-circle distance
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/GreatCircle.kt`
- **Task:** T-402
- **Spec:** docs/PLAN.md §6 A-11 — great-circle initial bearing to the Kaaba (21.4225°N, 39.8262°E)
- **Implementation note:** own spherical formulas (haversine distance, atan2 initial bearing, IUGG mean Earth radius);
  validated against an independent vector (cross-product) formulation; city coordinates in tests are rounded sample
  inputs, not official values.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-13 — Astronomy façade
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/` — `Sky.kt`, `SkyTypes.kt`, `Eclipses.kt`
- **Task:** T-403
- **References:** cosinekitty/astronomy 2.1.19 (MIT), public API only, https://github.com/cosinekitty/astronomy; kept as
  an implementation detail behind Taqvim types.
- **Validation:** NASA GSFC eclipse catalogs 2024–2030; official Iranian equinox instants 1404/1405 (see fixtures).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-14 — Houses, lots and ascendant
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/Houses.kt`
- **Task:** T-405
- **References used (public only):**
  1. J. Meeus, *Astronomical Algorithms*, 2nd ed. (Willmann-Bell, 1998) — ecliptic ↔ equatorial conversion (ch. 13)
     and the standard expressions for the ascendant and midheaven from the local sidereal angle, latitude and
     obliquity.
  2. NOAA Global Monitoring Laboratory, Solar Calculations spreadsheet (as used for A-09) — mean obliquity series and
     the 0.00256·cos Ω correction.
  3. The Placidus definition: each intermediate cusp is the ecliptic point lying one or two thirds of its diurnal
     (houses 11–12) or nocturnal (houses 2–3) semi-arc from the meridian — general astrological-astronomy knowledge; no
     code or tables copied.
  4. cosinekitty/astronomy 2.1.19 (MIT), public API only: `siderealTime` and the A-13 façade (Sun/Moon longitudes, Sun
     altitude for day/night charts).
- **Implementation note:** own work. Cusps are found by fixed-point iteration on the point's declination (30
  iterations); Placidus is undefined (`null`) where |latitude| ≥ 90° − obliquity. Lots: day Fortune = ASC + Moon − Sun,
  Spirit = ASC − Moon + Sun; the formulas swap at night.
- **Validation:** property tests against the definitions (ascendant on the eastern horizon, midheaven hour angle 0,
  cusps 11/12 at ⅓ and ⅔ semi-arc, ordering and oppositions) for 1950–2050 and latitudes ±60°. Published charts are
  pending (DT-018).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-500 — Date phrase parser
- **Module / files:** `core/nlp/src/main/kotlin/ir/taqvim/core/nlp/`; golden `core/nlp/src/test/resources/golden/nlp/date-phrases.tsv`
- **Origin:** own work from docs/PLAN.md T-500 on `:core:calendar` and the CLDR-derived names in `:core:i18n`.
- **Golden corpus:** synthetic, not a primary source. Phrases are built from `:core:i18n` month and weekday names and
  invented event names. Expected days come from the `:core:calendar` calendars and month arithmetic, never from the
  parser.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-700 — Theme colors, contrast and custom fonts
- **Module / files:** `core/ui/src/main/kotlin/ir/taqvim/core/ui/theme/` — `ColorMath.kt`, `ColorRole.kt`,
  `SchemeGenerator.kt`, `ColorSchemes.kt`, `ThemeSettings.kt`, `Typography.kt`, `CustomFonts.kt`, `TaqvimTheme.kt`
- **Task:** T-700
- **References used (public only):**
  1. IEC 61966-2-1 (sRGB): transfer function and D65 RGB ↔ XYZ matrices.
  2. CIE 15:2004 Colorimetry: CIE 1976 L\*a\*b\* and LCh(ab).
  3. W3C WCAG 2.2, https://www.w3.org/TR/WCAG22/ — definitions of relative luminance and contrast ratio; SC 1.4.3
     (4.5:1) and 1.4.6 (7:1).
  4. Material 3 color-role names as exposed by `androidx.compose.material3.ColorScheme` (Apache-2.0), and
     `dynamicLightColorScheme`/`dynamicDarkColorScheme`.
  5. OpenType specification (Microsoft Typography), "The OpenType Font File": sfnt version tags.
- **Implementation note:** own work; no Material color-utilities (HCT/CAM16) code or GPL/LGPL theming code used or
  consulted. Palette chroma values and the per-role tone table are Taqvim's own choices.
- **Validation:** WCAG reference contrasts (#767676 on white = 4.54:1), sRGB ↔ LCh round trip within 1/255, contrast
  property tests for all text roles, Roborazzi theme matrix.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-603 — City catalog, collation and device location
- **Module / files:** `data/location/src/main/kotlin/ir/taqvim/data/location/` — `CityCatalog.kt`,
  `CityTableParser.kt`, `DeviceLocator.kt`, `PlatformGeocoder.kt`
- **Task:** T-603
- **References used (public only):** Unicode Collation Algorithm and CLDR tailorings via the platform
  `android.icu.text.Collator`; haversine great-circle distance; Android `LocationManager` and `Geocoder` public APIs;
  search keys from T-203 `PersianText.searchKey` after NFD with U+0300–U+036F removed.
- **Implementation note:** own work. Collation test expectations follow the published alphabets (German DIN 5007-1,
  Turkish, Persian, Arabic) and collation strength levels.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-604 — Alarm scheduler
- **Module / files:** `data/scheduler/src/main/kotlin/ir/taqvim/data/scheduler/`
- **Origin:** own work from docs/PLAN.md T-604. References: Android developer documentation for `AlarmManager`
  (`setExactAndAllowWhileIdle`, `setAndAllowWhileIdle`, `canScheduleExactAlarms`,
  `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`), `BroadcastReceiver.goAsync`, and the system broadcasts
  `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`. No data.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-10 — Prayer times
- **Module / files:** `core/praytimes/src/main/kotlin/ir/taqvim/core/praytimes/` — `PrayerTimesCalculator.kt`,
  `PrayerMethodParameters.kt` (on the A-09 `NoaaSolarCalculator`)
- **Task:** T-401
- **Spec:** docs/PLAN.md §6 A-10 — method angles (MWL 18/17, ISNA 15/15, Egypt 19.5/17.5, Makkah 18.5 and Isha
  90 min after Maghrib, Karachi 18/18, Tehran 17.7/14 with Maghrib 4.5°, Jafari 16/14 with Maghrib 4°, Singapore
  20/18, France 12/12, Russia 16/15), Asr shadow factor 1 or 2, high-latitude options and midnight intervals.
- **References used (public only):**
  1. The plan's method table above.
  2. University of Tehran Institute of Geophysics / Calendar Center, note "Determining Fajr on white nights"
     (`docs/sources/اذان صبح در شب_های سفید.pdf`, retrieved 2026-09-13): where Fajr cannot be computed, Imsak is
     12 hours after Dhuhr and Fajr half an hour after Imsak (`HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS`).
- **Implementation note:** own work. Events are found from the NOAA hour-angle equation and refined by re-evaluating the
  Sun at the event time (3 iterations); times are rounded to the nearest minute. The high-latitude portions are
  defined here from the option names in the plan: the night runs from sunset to sunrise; Fajr is no earlier than
  sunrise − portion × night and Isha no later than sunset + portion × night, with portion ½, ⅐ or angle/60. No
  prayer-time library code (GPL/LGPL or otherwise) was consulted.
- **Validation:** Institute of Geophysics official 1405 timetables for 31 Iranian cities (365 days each): Fajr,
  sunrise, Dhuhr, sunset and Maghrib within 1 minute and midnight (middle of sunset→Fajr) within 2 minutes with the
  TEHRAN method; polar day/night, high-latitude rules, Makkah Isha, Hanafi Asr, midnight modes and time ordering.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-06 — Calculated-observational Islamic months (Yallop)
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/` — `CrescentVisibility.kt`,
  `ObservationalMonthStarts.kt` (tables consumed by `IranIslamicCalendar`, A-05)
- **Task:** T-104
- **Spec:** docs/PLAN.md §6 A-06 — Yallop criterion evaluated at the chosen location on day 29; tabular fallback
- **References used (public only):**
  1. B. D. Yallop, "A Method for Predicting the First Sighting of the New Crescent Moon", NAO Technical Note No. 69,
     HM Nautical Almanac Office, https://astronomycenter.net/pdf/yallop_1997.pdf (retrieved 2026-09-13): definitions
     of ARCL/ARCV (§2), eq. (3.6) and the topocentric width (3.8)–(3.10), best time Tb = Ts + 4/9 lag (4.1), classes
     A–F (Table 5).
  2. cosinekitty/astronomy 2.1.19 (MIT), public API only: geocentric vectors, rotation to the equator of date,
     airless horizon coordinates, rise/set searches. Earth equatorial radius 6378.137 km (WGS 84) for the parallax.
- **Implementation note:** own work. Month rule: a month has 29 days when the crescent is rated at the chosen class or
  better on the evening of its 29th day at the chosen place, otherwise 30; counting starts two months earlier from the
  tabular calendar so an off-by-one start settles before the first returned month.
- **Validation:** Yallop Table 4 (295 observations: q from ARCV and W′, class groups, width from parallax and ARCL);
  real evenings around a new moon; agreement with the official Iranian month starts (A-05) and within a day of
  Umm al-Qura (A-04).
- **Calibration:** `IranCrescentCalibration` (any of five cities, class D) was chosen by measuring agreement with
  the 25 published Iranian month starts (23/25); see the ADR-0009 addendum.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-303 / T-304 — Holiday determination and event search index
- **Module / files:** `core/events/src/main/kotlin/ir/taqvim/core/events/HolidayCalendar.kt`, `EventSearchIndex.kt`,
  `SearchTypes.kt`; golden test `data/events/src/test/.../OfficialHolidaysGoldenTest.kt`
- **Origin:** own work from docs/PLAN.md T-303/T-304 on the T-300 engine and T-203 normalization; search tests use
  synthetic data only. The holiday golden test uses the Calendar Center daily fixtures (T-102) and the D-02 dataset.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-404 — Zodiac signs, IAU constellations and "Moon in Scorpio"
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/Zodiac.kt`
- **Task:** T-404
- **References used (public only):** tropical signs as 30° bins of ecliptic longitude from the March equinox point;
  IAU constellation boundaries through cosinekitty/astronomy 2.1.19 (MIT) `constellation(ra, dec)` with geocentric
  J2000 coordinates (public API only).
- **Implementation note:** own work. The Moon-in-Scorpio search scans the window hourly and bisects each boundary to
  one minute; visits shorter than an hour may be missed (documented in the API).
- **Validation:** sign-boundary unit tests; constellation lookups for bright stars far from boundaries (rounded J2000
  positions used only as test inputs); internal-consistency checks of the 2026 intervals. No published list of
  Moon-in-Scorpio dates is available (DT-013), so the plan's "known dates" golden is pending.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-504 — Workday engine
- **Module / files:** `core/workdays/src/main/kotlin/ir/taqvim/core/workdays/WorkdayCalculator.kt`; golden test
  `data/events/src/test/kotlin/ir/taqvim/data/events/NowruzWorkdaysGoldenTest.kt`
- **Origin:** own work from docs/PLAN.md T-504 on HolidayCalendar (T-303). The synthetic test profile contains no
  real-world data; the golden test uses the D-02/D-08 generated official events and the Calendar Center daily
  fixtures (T-102). Its Friday weekend is a test setting, not official data.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-502 / T-503 — iCalendar reader/writer and recurrence engine
- **Module / files:** `core/ics/src/main/kotlin/ir/taqvim/core/ics/` — `IcsModel.kt`, `ContentLines.kt`, `IcsValues.kt`,
  `IcsReader.kt`, `IcsWriter.kt`, `RecurrenceEngine.kt`
- **Tasks:** T-502, T-503
- **References used (public only):** IETF RFC 5545, https://www.rfc-editor.org/rfc/rfc5545 — §3.1 (content lines,
  folding), §3.2 (parameters), §3.3.4–3.3.6 (DATE, DATE-TIME), §3.3.10 (RECUR), §3.3.11 (TEXT escaping), §3.6.1
  (VEVENT), §3.6.6 (VALARM), §3.8.5 (recurrence properties and examples).
- **Implementation note:** own work; no iCalendar library code was used or consulted. Recurrence in non-Gregorian
  calendars follows ADR-0011.
- **Validation:** 30 synthetic fixtures (`core/ics/src/test/resources/golden/ics/*.ics`, each marked synthetic with the
  RFC sections exercised; no personal data), RFC 5545 §3.8.5.3 examples and an independent java.time oracle.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-406 / T-407 — Tithi, planetary hours, animal year and photography panel
- **Module / files:** `core/astronomy/src/main/kotlin/ir/taqvim/core/astronomy/` — `Tithi.kt`, `PlanetaryHours.kt`,
  `AnimalYear.kt`, `IntervalSearch.kt`, `PhotographyPanel.kt`
- **Tasks:** T-406, T-407
- **References used (public only):**
  1. Animal-year cycle anchor (2020 = Rat): Hong Kong Observatory, Gregorian–lunar calendar conversion tables,
     https://www.hko.gov.hk/en/gts/time/conversion.htm (retrieved 2026-09-13).
  2. Planetary hours: the traditional Chaldean order (Saturn, Jupiter, Mars, Sun, Venus, Mercury, Moon) and weekday
     rulers — general astronomical-history knowledge; no code or data copied.
  3. cosinekitty/astronomy 2.1.19 (MIT), public API via the A-13 façade.
- **Implementation note:** own work. Tithi is computed from modern geocentric longitudes (a documented deviation from
  the plan's Surya Siddhanta method); golden and blue hours use the plan's apparent solar altitude bands.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-100 — `Jdn.weekday()`
- **Module / files:** `core/model/src/main/kotlin/ir/taqvim/core/model/Jdn.kt`
- **Task:** T-100
- **References used (public only):** J. Meeus, *Astronomical Algorithms*, 2nd ed., 1998, ch. 7 — day of week
  from the Julian Day (JDN 0 is a Monday).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-03 — Islamic tabular calendar (types I and II)
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/TabularIslamicCalendar.kt`
- **Task:** T-103
- **Spec:** docs/PLAN.md §6 A-03 (30-year cycle, type II "16" and type I "15", civil epoch JD 1 948 439.5)
- **References used (public only):**
  1. R. H. van Gent, "The Islamic Calendar — Tabular Islamic calendars", Utrecht University,
     https://webspace.science.uu.nl/~gent0113/islam/islam_tabcal.htm (leap-year patterns of the arithmetic
     variants; civil epoch 16 July 622 Julian).
  2. E. M. Reingold and N. Dershowitz, *Calendrical Calculations*, 3rd ed., Cambridge University Press, 2008,
     ch. 6 (structure of the arithmetic Islamic calendar: 354/355-day years, alternating 30/29-day months).
- **Implementation note:** all arithmetic is derived directly from the definition (leap-year set per cycle,
  cumulative leap counts, month alternation); no closed-form formula was copied.
- **Validation oracle:** ICU4J 78.3 `IslamicCalendar(ISLAMIC_CIVIL)` for type II (100 000 random days). ICU has
  no type I variant; type I is validated through its exact relationship to type II (dates differ by one day
  only in cycle year 16) and round-trip properties.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### A-04 — Umm al-Qura calendar
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/UmmAlQuraCalendar.kt`
- **Task:** T-103
- **Spec:** docs/PLAN.md §6 A-04; deviation ADR-0006 (embedded table instead of runtime ICU4J)
- **Data origin:** month lengths for AH 1300–1600 obtained through ICU4J 78.3's public API
  (`IslamicCalendar(ISLAMIC_UMALQURA)`, license Unicode-3.0); notice in `licenses/ICU-LICENSE.txt`, text retrieved
  from https://www.unicode.org/license.txt on 2026-09-13. No ICU source code was copied.
- **Validation oracle:** ICU4J 78.3 — every tabulated month (start and length) and 100 000 random days across
  AH 1200–1700; published Umm al-Qura tables are ICU's own source.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-107 — kotlinx.datetime bridge
- **Module / files:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/DateTimeBridge.kt`
- **References used (public only):** kotlinx-datetime API documentation (epoch day 0 = 1970-01-01);
  JDN 2 440 588 for 1970-01-01 follows from A-01 (verified by `GregorianCalendarSystem` and a property test).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### T-201 — Numerals
- **Module / files:** `core/i18n/src/main/kotlin/ir/taqvim/core/i18n/Numerals.kt`
- **Task:** T-201
- **References used (public only):**
  1. The Unicode Standard 16.0, code charts for Arabic (U+0600), Arabic Extended digits (U+06F0), Devanagari
     (U+0900) and Tamil (U+0B80), including the Tamil number signs ௰ ௱ ௲ (U+0BF0–U+0BF2),
     https://www.unicode.org/charts/ (retrieved 2026-09-13).
  2. Unicode CLDR number symbols (decimal and group separators per locale; Indian grouping pattern
     `#,##,##0`), https://cldr.unicode.org/ (Unicode License v3).
- **Implementation note:** own implementation from the digit code-point layout; traditional Tamil uses a
  multiplier-sign decomposition derived from the sign values above.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-203 — Persian text normalization & fuzzy matching
- **Module / files:** `core/i18n/src/main/kotlin/ir/taqvim/core/i18n/PersianText.kt`
- **Task:** T-203
- **References used (public only):**
  1. The Unicode Standard 16.0, Arabic block chart and chapter 9.2 "Arabic" (yeh/kaf letter variants, tatweel,
     harakat, ZWNJ/ZWJ behaviour), https://www.unicode.org/charts/PDF/U0600.pdf (retrieved 2026-09-13).
  2. Institute of Standards and Industrial Research of Iran, ISIRI 6219 *Information Technology — Persian
     Information Interchange and Display Mechanism* (Farsi yeh U+06CC and keheh U+06A9 as the canonical letters).
  3. F. J. Damerau, "A technique for computer detection and correction of spelling errors", *Communications of
     the ACM* 7(3):171–176, 1964, https://doi.org/10.1145/363958.363994; V. I. Levenshtein, "Binary codes
     capable of correcting deletions, insertions, and reversals", *Soviet Physics Doklady* 10(8):707, 1966.
- **Implementation note:** optimal string alignment (restricted Damerau–Levenshtein) dynamic programme with
  three rolling rows and a row-minimum early exit; own implementation from the published recurrence.
- **Test data:** the 100 normalization/fuzzy pairs were written for this repository; they contain no external data.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

## Datasets

### T-200 — Language table (`languages.properties`)
- **Module / files:** `core/i18n/src/main/resources/ir/taqvim/core/i18n/languages.properties`
- **Task:** T-200; decisions in docs/adr/0007-launch-languages.md
- **Source:** Unicode CLDR 48 via the ICU4J 78.3 public API (Unicode-3.0; notice in `licenses/ICU-LICENSE.txt`),
  retrieved 2026-09-13. Fields: native name, likely script, direction, region week data (first day, weekend), short
  date pattern, AM/PM, two-item "and" list pattern, standalone wide month names (Gregorian, Persian, Islamic).
- **Extraction:** a one-off Java program (`LanguageTableGen.java`, run from the session scratchpad against
  `icu4j-78.3.jar` on JDK 21) that calls only public ICU4J APIs and writes the resource. Its logic is reproduced
  in `LanguageTableCldrOracleTest`, which re-verifies every CLDR-derived value against ICU4J on each test run.
- **Not from CLDR:** numerals, calendar order, prayer method and Asr convention are product defaults (ADR-0007).
- **Gaps:** values CLDR only provides as generic or English fallbacks were omitted (docs/DATA_TODO.md DT-004…DT-007).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### T-603 — City list (`cities.tsv`)
- **Files:** `data/location/src/main/resources/ir/taqvim/data/location/cities.tsv` (7342 places), generated by
  `tools/geodata/natural_earth_cities.py`
- **Source:** Natural Earth 1:10m Cultural Vectors, Populated Places (`ne_10m_populated_places.geojson`, version
  5.2.0-pre), retrieved 2026-09-13 from
  https://raw.githubusercontent.com/nvkelso/natural-earth-vector/ca96624a56bd078437bca8184e78163e5039ad19/geojson/ne_10m_populated_places.geojson
  (SHA-256 `9b8e3de09048ef00dfc70357dbb9fa324493f214b5e0ae4daf1aa79a8d10116b`).
- **Licence:** public domain (https://www.naturalearthdata.com/about/terms-of-use/; repository LICENSE.md), allowed
  by ADR-0003.
- **Transformation:** kept id, ISO_A2, ADM1NAME, POP_MAX, TIMEZONE, NAME_EN and names in ar, bn, de, es, fa, fr, hi,
  id, ja, ru, tr, ur, zh. Coordinates from the point geometry rounded to 5 decimals (the LATITUDE/LONGITUDE attributes
  differ for 244 places, by up to 0.76°). A localized name equal to the English one is left empty; −99 and missing
  time zones become empty. Nothing translated or added; missing languages in DT-020.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13
- **Reviewer attestation:** pending — no forbidden sources consulted.

### Iran official calendar sources (University of Tehran, Institute of Geophysics, Calendar Center)
- **Files:** `docs/sources/` — inventory with page counts and SHA-256 in `docs/sources/MANIFEST.md`
- **Obtained:** downloaded by the repository owner in a browser and added on 2026-09-13 (calendar.ut.ac.ir blocks
  non-browser clients); publisher site https://calendar.ut.ac.ir/Fa/. Exact download URLs to be added by the owner.
- **Extraction (T-102):** poppler `pdftotext` 2026-09-13. The leap-year table was read from the layout text and
  self-checked (293 consecutive years, no duplicates). The daily tables of the 1404/1405 calendars were parsed by word
  coordinates (`pdftotext -bbox`): each row is anchored on its weekday, values are assigned by column order, and ditto
  marks carry the previous month/year. Every row was validated for consecutive Solar Hijri days, weekday cycle,
  lunar-month progression (29/30-day months) and consecutive Gregorian dates — 730 rows, 0 errors. Holiday flags
  come from the "(تعطیل)" marker in the occasion text nearest the row (26 per year; the fixed national holidays were
  checked by hand).
- **Interpretation:** the leap table's Gregorian column is the civil date of the March equinox in Iran time, not
  1 Farvardin (it differs from the official calendars' 1 Farvardin 1404 and 1405); confirmed by computation for all
  293 years.
- **Used by:** `core/calendar/src/test/resources/golden/persian/*` (T-102); lunar-month columns reserved for T-104/D-07;
  holidays for D-02.

### D-01 — Dataset schema and validator
- **Files:** `dataset/events.v1.json`, `dataset/README.md`, `tools/dataset/`
- **Origin:** own work from docs/PLAN.md §4.2 (`EventDefinition`) and §5.2–5.3; no external dataset, schema or code
  was used. Test fixtures under `tools/dataset/src/test/resources/dataset/` are synthetic (`example.org` citations,
  placeholder titles) and contain no real-world facts.
- **Dependency:** networknt json-schema-validator 3.0.7 (Apache-2.0), used through its public API only.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### D-02 — Iran official holidays (`dataset/iran/iran-official-holidays.json`)
- **Source:** University of Tehran Calendar Center, official calendars of 1404 and 1405 SH (`docs/sources`, checksums in
  `docs/sources/MANIFEST.md`); days marked "(تعطیل)".
- **Method:** titles are the official Persian wording; only PDF text-layer artefacts were corrected (lam-alef ligature,
  ی/ک, spacing, words split across lines). Where one cell held several occasions, the holiday was attributed using the
  other year (lunar holidays move by about 11 days) or the rendered page. Rules were derived from both years and checked
  date by date against the extracted daily rows (26/26 holidays in each year). No translations: there is no primary
  source for them.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending (sign-off checklist in D-02).

### core/i18n — `formats.properties` (T-202)
- **Source:** Unicode CLDR 48 through the ICU4J 78.3 public API (Unicode-3.0), retrieved 2026-09-13: full date
  patterns, weekday and month names, era abbreviations (Gregorian, Persian, Islamic), plural rules, relative-time
  patterns, day/hour/minute unit patterns and "and" list patterns for the 24 launch languages.
- **Generator:** a one-off program (scratchpad `FormatTableGen.java`, same approach as T-200) that omits values ICU only
  provides by root/English fallback; every stored value is re-checked against ICU4J by the T-202 oracle tests.

### D-08 — Generated official events (`data/events/.../generated/OfficialEvents*.kt`)
- **Origin:** generated by `:tools:dataset:generateEvents` (own code from docs/PLAN.md D-08 and the T-300 model, using
  KotlinPoet 2.4.0, Apache-2.0) exclusively from `dataset/`; the data's provenance is that of D-02.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

## Golden fixtures

Format: every file under `src/test/resources/golden/` starts with a `# source/url/retrieved/page/reviewer`
header (see `core/testing/README.md`); `FixtureProvenanceKonsistTest` fails the build otherwise.

### core/testing — `golden/sample/valid-fixture.csv`
- **Task:** T-005 (self-test of the fixture loader)
- **Source:** synthetic data written for this repository; contains no external facts.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13

### core/calendar — `golden/persian/*` (T-102)
- `official-leap-years-1206-1498.csv` — official leap markers and equinox dates, 293 rows (`Kabise Shamsi 1206-1498.pdf`, pp. 1–11).
- `iran-official-1404-days.csv`, `iran-official-1405-days.csv` — one row per day: Solar Hijri date, ISO weekday,
  Iran official lunar Hijri date, Gregorian date, official holiday flag, page (`Calendar-1404.pdf`, `Calendar-1405.pdf`).
- `official-nowruz-instants.csv` — moment of the vernal equinox printed on the title page of each official calendar.
- `century-boundaries.csv` — weekdays of 1 Farvardin 1, 101, 1301, 1401 and 29 Esfand 100, 200, 1400 (`century15th.pdf`).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/calendar — `golden/islamic-iran/official-month-starts-1446-1448.csv` (T-104)
- First day (Gregorian and Solar Hijri) and length of each official Iranian lunar month, Ramadan 1446 – Shawwal 1448,
  from `Calendar-1404.pdf` and `Calendar-1405.pdf`; the Ramadan 1446 row is derived as described in A-05.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/praytimes — `golden/noaa/noaa-solar-day-2010-06-21.csv` (T-400)
- NOAA Solar Calculations spreadsheet (day), cached results of its sample inputs; no values were computed by Taqvim.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/astronomy — `golden/nasa/*`, `golden/iran/official-equinox-instants.csv` (T-403)
- `solar-eclipses-2024-2030.csv`, `lunar-eclipses-2024-2030.csv`: NASA GSFC Five Millennium Catalogs of Solar/Lunar
  Eclipses, https://eclipse.gsfc.nasa.gov/SEcat5/SE2001-2100.html and https://eclipse.gsfc.nasa.gov/LEcat5/LE2001-2100.html
  (retrieved 2026-09-13; public domain; page SHA-256 in the headers).
- `official-equinox-instants.csv`: vernal equinox instants printed in the Calendar Center's official calendars 1404 and
  1405 (`docs/sources`, SHA-256 in the header).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/praytimes — `golden/iran-prayer-times-1405/*.csv` (T-401)
- One file per city (31): the Institute of Geophysics' official religious times for 1405 SH (`docs/sources/<City>1405.pdf`,
  SHA-256 and the coordinates stated in each document in the header), extracted with `pdftotext -layout` and validated
  (365 days per city, time order, identical coordinates on every monthly page).
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.

### core/astronomy — `golden/yallop/yallop-1997-table4.csv` (T-104, A-06)
- Yallop, NAO Technical Note 69, Table 4 (295 observations, pages 5–10), extracted with `pdftotext -layout`; PDF SHA-256 in
  the header. Entries 251/252 (q = −0.014) keep the note's own group B.
- **Author / date:** Saman Sohani (via Claude Code), 2026-09-13; **reviewer:** pending.
