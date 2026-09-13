# PROJECT "TAQVIM" — Master Implementation Plan
## A brand-new, clean-room, permissively-licensed Persian/Multi-calendar Android application

Version 1.0 of this plan · Status: DRAFT FOR EXECUTION

---

# 0. Ground Rules (read before anything else)

## 0.1 Licensing & clean-room policy (non-negotiable)

| Rule | Detail |
|---|---|
| Target license | Project code is owned by the author. Default: **proprietary (All Rights Reserved)** with the option to relicense to MIT/Apache-2.0 later. Nothing in the dependency graph may force copyleft. |
| Forbidden inputs | No source code, no data files, no string resources, no icons, no test fixtures from any GPL/LGPL/AGPL/MPL project. Explicitly forbidden: `persian-calendar/persian-calendar`, `persian-calendar/calendar`, `persian-calendar/praytimes`, `persian-calendar/events`, `avianey/Level`, `ilius/starcal`. |
| Allowed inputs | Public mathematical formulas, published standards (ISO 8601, RFC 5545, Unicode CLDR), official government publications (facts), permissive libraries (MIT, Apache-2.0, BSD, Unicode, CC0), and this document. |
| Clean-room procedure | Every algorithm is implemented from a **written specification** (Section 7) that cites only public references. Engineers write code from the spec, never from another implementation. Each core algorithm PR must include a `PROVENANCE.md` entry listing the references used. |
| Data | The holiday/event dataset is compiled **directly from primary sources** (official calendar PDFs, government gazettes, UN observances list) into our own schema (Section 6). Each record carries a citation. |
| Dependency gate | CI runs a license scanner (Gradle License Report + custom allow-list). Any dependency not in the allow-list fails the build. |

Allow-list: Apache-2.0, MIT, BSD-2/3, ISC, Unicode-DFS, CC0, EPL-2.0 (test-only), Public Domain.

## 0.2 Engineering non-negotiables

- Kotlin only. No Java. No XML layouts. Compose everywhere (phone, tablet, foldable, Wear).
- Zero global mutable state. All state is owned by ViewModels/Repositories and exposed as `StateFlow`/`Flow`.
- Zero `!!`, zero raw `try/catch` in production code (use `runCatching`/`Result`), zero unsafe `as`, zero hard-coded user-facing text in Kotlin.
- Every public function in `:core:*` has KDoc and at least one test.
- Every task in this plan ends with its acceptance criteria and tests green in CI. **No task is "done" without its tests merged.**
- Minimum SDK 26 (Android 8.0). Target/compile SDK = latest stable at project start.

## 0.3 Definitions

- **JDN**: Julian Day Number (integer day count, noon-based epoch). Internal canonical day representation.
- **Calendar system**: PERSIAN (Solar Hijri), ISLAMIC (Lunar Hijri, multiple variants), GREGORIAN, NEPALI (Bikram Sambat).
- **Occurrence**: a concrete (calendar-day, event-definition) instance produced by the rule engine.
- **Golden test**: test asserting against externally published reference data stored as fixture files with citations.

---

# 1. Product Vision

A fast, beautiful, privacy-first calendar for Persian-speaking users (Iran, Afghanistan, Tajikistan) and the wider region, that:
1. Shows Persian, Islamic, Gregorian and Nepali dates with official holidays and observances from cited sources.
2. Provides prayer times, athan, Qibla, and rich astronomy.
3. Lets users create their own events and reminders **without** depending on Google Calendar, while still integrating with the device calendar.
4. Works entirely offline; never sends data anywhere.
5. Runs on phone, tablet, foldable, Wear OS; widgets are first-class.
6. Is accessible (TalkBack, high contrast, large fonts) and fully RTL.

Non-goals: cloud sync servers, accounts, ads, analytics SDKs.

---

# 2. Feature Scope

## 2.1 Parity features (must exist at v1.0)

| Area | Features |
|---|---|
| Calendar | Month pager; day selection; today button; secondary calendar in cells; week numbers; weekend/holiday coloring; year view with zoom & year picker; week timeline; day timeline; horizontal month list; agenda list; swipe-up/down actions; keyboard shortcuts; shift-work overlay; search in events |
| Events | Official holidays/observances (Iran, Afghanistan, Nepal, international, ancient Iranian); device calendar events (read, open, create via intent); event source tooltip with citation; equinox countdown |
| Times | Prayer times (10+ calculation methods, Hanafi/Standard Asr, high-latitude methods, midnight methods); athan alarms per prayer with gap, sound picker, vibration, DND bypass for Fajr; monthly printable report; sun arc view |
| Astronomy | Sun/Moon/Earth views; zodiac (tropical & IAU); moon phase & names; moon-in-Scorpio; seasons/equinox times; eclipses; horoscope (houses, lots, ascendant); planetary hours; Chinese/animal year names; Tithi |
| Tools | Date converter; day-distance with workdays; duration calculator; time zones; QR generator; compass with Qibla & true north; bubble level; world map (day/night, moon visibility, magnetic field, time zones, tectonic plates, crescent visibility) with 3D globe |
| Widgets | 1×1, 4×1, 2×2, 4×2, month (interactive), month-view, week, schedule list, sun, moon, map, age/countdown; per-widget color, transparency, scale; dynamic colors |
| Notification | Persistent date notification with day icon, other calendars, holidays, prayer times, lock-screen visibility |
| Settings | 24 languages; numerals; themes (system/light/dark/black/custom colors) with dynamic color; gradient; bold/custom font; background image; calendars priority; week start/weekends; holiday sources; hijri offset; location (city list, districts, GPS, coordinates); wallpaper/screensaver |
| Wear | Date app, month tile, complications (date, month progress) |
| Platform | Dynamic launcher icon (day number); Quick Settings tile; app shortcuts; live wallpaper; daydream; backup rules |

## 2.2 New "killer" features (v1.0 unless marked)

| ID | Feature | Value |
|---|---|---|
| F-01 | **Native personal events & reminders** (Room-backed, recurrence in any calendar system, colors, notes, attachments as URIs) | No Google Calendar dependency |
| F-02 | **Reminders on official events** ("1 day before Eid al-Fitr") via stable event IDs | Unique |
| F-03 | **Natural-language date input** (`fa`/`en`): "سه روز قبل از نوروز", "next Friday", "۱۵ خرداد ۱۴۰۵" → date | Fast entry |
| F-04 | **Text date detection**: share/select any text → app detects Persian/Gregorian/Hijri dates and offers "open in calendar" (Android `PROCESS_TEXT`) | Delight |
| F-05 | **ICS import/export + WebCal subscription** (read-only subscriptions refreshed by WorkManager) | Interop |
| F-06 | **Observational Islamic calendar by region** (Iran official, Umm al-Qura, Turkey, calculated) with per-year override tables and transparent "source of this date" | Accuracy |
| F-07 | **Workday engine**: custom holiday sets, half-days, personal leave; "how many workdays until X"; export | Business |
| F-08 | **Multiple shift-work rotations** with per-rotation color, exceptions, and calendar export | Workers |
| F-09 | **Countdown widgets & live tiles** to any event (personal/official) with progress rings | Engagement |
| F-10 | **Golden hour / photography panel**: sunrise/sunset/blue hour/golden hour, moonrise/moonset, sun & moon azimuth chart | Niche delight |
| F-11 | **Full backup/restore** to a single encrypted file (settings + personal data), and plain JSON export | Trust |
| F-12 | **In-app search across everything** (events, settings, tools) with fuzzy Persian matching (ي/ی, ك/ک, ZWNJ-insensitive) | Speed |
| F-13 | **Adaptive layouts** for tablets/foldables (list-detail, two-pane calendar+day) | Modern |
| F-14 | **Automation API**: documented intents/deep links (`taqvim://day/1405-01-01`, `taqvim://convert?...`), Tasker-compatible broadcasts for athan | Power users |
| F-15 | **Privacy dashboard**: shows exactly what permissions are used and why; permission-free mode | Trust |
| F-16 | **Diagnostics & support**: in-app log ring buffer, "report a problem" that composes an email/GitHub issue with anonymized diagnostics; no network SDK | Support |
| F-17 (v1.1) | Desktop/KMP build of `:core:*` + a JVM CLI (`taqvim-cli convert 1405-01-01`) | Ecosystem |
| F-18 (v1.1) | Home-screen "Now" widget: next prayer, next event, day progress ring | Engagement |

---

# 3. Architecture

## 3.1 Layers & rules
```
feature/*  →  domain (core/*)  ←  data/*
app  → wires everything via DI
```
- `core/*` are pure Kotlin/JVM (KMP-ready): no `android.*` imports (enforced by Konsist).
- `data/*` implement `core` repository interfaces using Android APIs.
- `feature/*` contain Compose UI + ViewModels; depend only on `core` and `core:ui`; never on other features or `data` (DI binds them).
- Unidirectional data flow: `UiState` (immutable data class) + `UiAction` (sealed) + effects channel.

## 3.2 Module map

| Module | Responsibility |
|---|---|
| `:app` | Application, DI graph (Koin), NavHost, Manifest, deep links |
| `:build-logic` | Gradle convention plugins, code generators (events, cities), license gate |
| `:core:model` | Shared value types: `Jdn`, `CalendarSystem`, `CalendarDate`, `Coordinates`, `Language`, `Numeral`, `Theme` |
| `:core:calendar` | Calendar arithmetic & conversions (Section 7.1–7.4) |
| `:core:events` | Event definitions, rule engine, occurrence calculator, policies, search index |
| `:core:praytimes` | Solar position, prayer time computation, high-latitude/midnight methods |
| `:core:astronomy` | Wrapper over permissive astronomy engine; zodiac, phases, houses, lots, tithi, planetary hours, eclipses |
| `:core:i18n` | Language specs (data table), numerals, date/duration formatting, Persian text normalization |
| `:core:nlp` | Natural-language date parser (F-03) and text date detector (F-04) |
| `:core:ics` | RFC 5545 parser/writer subset |
| `:core:workdays` | Workday engine (F-07) |
| `:core:testing` | Fixtures, fakes, test rules, golden-file helpers |
| `:core:ui` | Design system: theme, typography, components, painters, motion |
| `:data:preferences` | Proto DataStore schema + accessors |
| `:data:database` | Room: personal events, reminders, alarms, shift-work, subscriptions, caches |
| `:data:events` | Repository impl over generated dataset + overrides |
| `:data:device-calendar` | `CalendarContract` adapter, observer, cache |
| `:data:location` | Cities DB, districts, GPS, geocoder |
| `:data:scheduler` | Alarm/Work scheduling single source of truth |
| `:feature:calendar` `:feature:timeline` `:feature:month` `:feature:year` `:feature:agenda` `:feature:events` `:feature:times` `:feature:astronomy` `:feature:map` `:feature:compass` `:feature:tools` `:feature:settings` `:feature:about` `:feature:widgets` `:feature:notification` `:feature:wallpaper` `:feature:search` `:feature:backup` | One feature each |
| `:wear` | Wear OS app |
| `:cli` (v1.1) | JVM CLI |
| `:lint` | Custom lint rules |
| `:benchmark` | Macrobenchmark & baseline profile |
| `:tools:dataset` | Dataset validator/compiler (JVM CLI) |

## 3.3 Tech stack (pin latest stable at kickoff; Renovate keeps them current)

Kotlin (K2) · Coroutines · `kotlinx.serialization` · `kotlinx.datetime` · `kotlinx.collections.immutable` · Compose BOM + Material 3 (Expressive) · Navigation 3 · Adaptive layouts · Lifecycle/ViewModel · Room (KSP) · Proto DataStore · WorkManager · Glance · `androidx.core`, `activity`, `browser` · Koin · **Astronomy**: `io.github.cosinekitty:astronomy` (MIT) · **Calendar reference/validation only**: ICU4J (Unicode license) · **QR**: ZXing core (Apache-2.0) · **Testing**: JUnit 5, Kotest (assertions + property), Turbine, Robolectric, Roborazzi, Compose UI Test, Macrobenchmark, MockK (sparingly) · **Quality**: ktlint via Spotless, detekt, Android Lint + `:lint`, Konsist · **CI**: GitHub Actions.

No LGPL/GPL anywhere (including transitive) — enforced by the license gate.

---

# 4. Data Model

## 4.1 Core value types (`:core:model`)
```kotlin
@JvmInline value class Jdn(val value: Long)
enum class CalendarSystem { PERSIAN, ISLAMIC, GREGORIAN, NEPALI }
enum class IslamicVariant { IRAN_OFFICIAL, UMM_AL_QURA, TABULAR_16, TABULAR_15, CALCULATED_OBSERVATIONAL }
data class CalendarDate(val system: CalendarSystem, val year: Int, val month: Int, val day: Int)
enum class Weekday { MONDAY..SUNDAY } // ISO order; presentation applies weekStart
data class Coordinates(val latitude: Double, val longitude: Double, val elevationMeters: Double = 0.0)
@JvmInline value class MinuteOfDay(val value: Int) // 0..1439, no floating error
```

## 4.2 Events (`:core:events`)
```kotlin
data class EventDefinition(
  val id: EventId,                 // stable slug, e.g. "ir.nowruz.1"
  val calendar: CalendarSystem,
  val source: EventSource,         // IRAN_OFFICIAL, AFGHANISTAN_OFFICIAL, NEPAL_OFFICIAL, INTERNATIONAL, ANCIENT_IRAN, USER
  val category: EventCategory,     // NATIONAL, RELIGIOUS, INTERNATIONAL, CULTURAL, ASTRONOMICAL, PERSONAL
  val isHoliday: Boolean,
  val title: LocalizedText,        // map<langTag, String>, "fa" mandatory
  val rule: EventRule,
  val validity: Validity?,         // fromYear/toYear in a named calendar + citation
  val flags: Set<EventFlag>,       // ALWAYS_DISPLAYED, HALF_DAY, ...
  val aliases: List<String>,
  val citations: List<Citation>,   // url/title/page
  val links: Map<String, String>,  // "wikipedia" -> url
)
sealed interface EventRule {
  data class Fixed(month: Int, day: Int)
  data class NthWeekdayOfMonth(month: Int, weekday: Weekday, n: Int)
  data class LastWeekdayOfMonth(month: Int, weekday: Weekday, offsetDays: Int = 0)
  data class LastDayOfMonth(month: Int)
  data class Single(year: Int, month: Int, day: Int)
  data class NthDayOfYear(n: Int)
  data class RelativeToEvent(eventId: EventId, offsetDays: Int)
  data class Astronomical(kind: AstroKind /*MARCH_EQUINOX, JUNE_SOLSTICE, ..., NEW_MOON, FULL_MOON*/, offsetDays: Int, timeZone: String)
}
```
Occurrences: `data class Occurrence(val definition: EventDefinition, val jdn: Jdn, val date: CalendarDate, val isHoliday: Boolean, val year: Int)`.

## 4.3 Personal data (Room)
Tables: `personal_events`, `event_recurrences` (RRULE-lite: FREQ, INTERVAL, BYDAY, COUNT/UNTIL, calendar system), `reminders`, `scheduled_alarms`, `shift_rotations`, `shift_rotation_records`, `ics_subscriptions`, `ics_events_cache`, `device_events_cache`, `workday_profiles`, `diagnostics_log` (ring, max 2 000 rows).

## 4.4 Preferences (Proto DataStore)
One `UserPrefs` message with typed enums; `schema_version`; migrations tested table-driven.

---

# 5. Dataset Program (holidays & observances)

Because we cannot reuse any existing dataset, the dataset is a first-class deliverable.

## 5.1 Primary sources (facts, cited per record)
- Iran: annual official calendar published by the University of Tehran Institute of Geophysics (Calendar Center) for each Persian year; Iranian legal holidays from Majlis law texts.
- Afghanistan: government calendar publications (Ministry of Justice / Urban Development annual calendar).
- Nepal: Government of Nepal official holiday gazette per year.
- International: United Nations list of international days & weeks.
- Ancient Iranian festivals: encyclopedic sources (citations), marked category CULTURAL, off by default.
- Islamic observational dates for Iran: official published Hijri↔Persian tables per year (Calendar Center), stored as `islamic_iran_overrides.json`.

## 5.2 Schema (`dataset/events.v1.json`) — JSON Schema draft 2020-12 in repo
Fields as §4.2 plus `updated`, `reviewedBy`. Compiled at build time by `:build-logic` into typed Kotlin.

## 5.3 Dataset tasks

| ID | Task | Acceptance | Tests |
|---|---|---|---|
| D-01 | Write JSON Schema + validator CLI (`:tools:dataset validate`) | Rejects: bad ranges, duplicate ids, missing `fa`, unknown source, invalid rule params, missing citation for holiday | Unit tests with 30 invalid fixture files (each one error), fuzz 10k random JSON docs → no crash |
| D-02 | Compile Iran official holidays & observances for 1403–1406 from primary source | 100% of holidays in the source present; each row has citation with page | Golden: `iran_holidays_1403.json`…`1406.json` count & set equality; reviewer sign-off checklist |
| D-03 | Compile Afghanistan set | idem | Golden per year |
| D-04 | Compile Nepal set | idem | Golden per year |
| D-05 | Compile UN international days (with rule types where needed) | ≥ 150 entries | Golden count; each URL resolves (weekly CI job) |
| D-06 | Compile ancient Iranian festivals (with Jalali 30-day-month convention flag) | Each entry cited | Golden |
| D-07 | Islamic Iran override table 1390–1410 (Persian years) | Every Hijri month start present | Golden vs published table |
| D-08 | Code generator (KotlinPoet) from dataset to `:data:events` | Generated code compiles; sealed rules typed | Snapshot test of generated file |
| D-09 | Dataset CI: validate → generate → golden tests; `CONTRIBUTING-DATA.md` | PRs to dataset require citation | CI |

---

# 6. Algorithm Specifications (clean-room references)

Each spec cites only public references. Implementations must be written from these specs.

| ID | Component | Specification / references | Validation oracle |
|---|---|---|---|
| A-01 | JDN ↔ Gregorian | Fliegel & Van Flandern (1968) integer algorithm; proleptic Gregorian | ICU4J `GregorianCalendar` over 1 000 000 random days; ISO 8601 |
| A-02 | Persian (Solar Hijri) | **Astronomical**: year begins on the day (Tehran meridian, 52.5°E) whose noon follows the March equinox instant; computed via `:core:astronomy` equinox search. **Arithmetic fallback** for years outside ±3000 using the 2820-year cycle (public formula). Month lengths: 31×6, 30×5, 29/30 | ICU4J `PersianCalendar` (arithmetic) for agreement except known divergent years; official leap years (1403, 1408, 1412, 1416, …) from published tables |
| A-03 | Islamic tabular | Standard 30-year cycle (type II/16 and type I/15), epoch JDN 1948439.5 | ICU4J `IslamicCalendar(CIVIL/TBLA)` |
| A-04 | Islamic Umm al-Qura | ICU4J `IslamicCalendar(UMALQURA)` used **directly** (Unicode license) | Published Umm al-Qura tables |
| A-05 | Islamic Iran official | Override table (D-07) on top of A-03; user offset ±2 with expiry | Published Iran tables |
| A-06 | Islamic calculated-observational | Yallop criterion evaluated at chosen location on day 29; fallback tabular | Compare with Iran/UAQ tables ≥ 90% agreement |
| A-07 | Nepali (Bikram Sambat) | Table of month lengths per year from public government data (2000–2100 BS); epoch mapping | Government calendar samples |
| A-08 | Week numbering | ISO-8601 week rule generalized to configurable week start; week 1 contains first day of year (configurable "first 4-day week") | Hand-computed fixtures |
| A-09 | Solar position for prayer times | NOAA Solar Calculator equations (public domain): Julian century, mean longitude, equation of time, declination | NOAA published sample values |
| A-10 | Prayer times | Standard angles per method (MWL 18/17, ISNA 15/15, Egypt 19.5/17.5, Makkah 18.5/90min, Karachi 18/18, Tehran 17.7/14 & maghrib 4.5, Jafari 16/14 & maghrib 4, Singapore 20/18, France 12/12, Russia 16/15); Asr shadow factor 1 or 2; high-latitude: none, middle-of-night, one-seventh, angle-based; midnight: mid sunset→sunrise, sunset→fajr, maghrib→sunrise, maghrib→fajr | Published timetables for Tehran, Mashhad, Kabul, Istanbul, Berlin, Sydney (±2 min) |
| A-11 | Qibla | Great-circle initial bearing to Kaaba (21.4225°N, 39.8262°E) | Known values (Tehran ≈ 217.6°) |
| A-12 | Magnetic declination | Android `GeomagneticField` (platform) | — |
| A-13 | Astronomy | `cosinekitty/astronomy` (MIT) for positions, rise/set, phases, equinoxes, eclipses | Library's own accuracy claims + NASA eclipse tables |
| A-14 | Houses (Placidus) & lots | Standard Placidus iteration; Arabic parts formulas (public) | Published charts |
| A-15 | Tithi | Standard Surya Siddhanta mean/true longitude method | Published panchang samples |
| A-16 | Persian NLP date grammar | Own PEG grammar (Section 8, E-NLP) | Fixture corpus of 500 phrases |

---

# 7. Work Breakdown — Epics & Tasks

Format per task: **ID · Title · Goal · Deliverables · Depends · Acceptance criteria · Tests**.
Test kinds: U = unit, P = property, G = golden, R = Robolectric, S = screenshot (Roborazzi), UI = Compose instrumented, B = benchmark, L = static/lint.

---

## EPIC 0 — Foundation & Delivery Pipeline

### T-000 Repository bootstrap
- Goal: empty multi-module project building on CI.
- Deliverables: module skeleton (§3.2), `libs.versions.toml`, convention plugins, `.editorconfig`, Renovate, CODEOWNERS, issue/PR templates, ADR folder with ADR-0001 (architecture), ADR-0002 (DI: Koin), ADR-0003 (license policy).
- Acceptance: `./gradlew build` green in < 5 min on CI cold; configuration cache on.
- Tests: L (spotless, detekt, lint pass on empty project); Konsist test skeleton.

### T-001 License gate
- Deliverables: Gradle task `licenseCheck` with allow-list; fails on GPL/LGPL/MPL/unknown; report artifact.
- Acceptance: adding a GPL dependency in a test branch fails CI.
- Tests: U for allow-list parser; CI negative test.

### T-002 Architecture rules (Konsist)
- Rules: core has no `android.*`; feature→feature forbidden; ViewModels end with `ViewModel` and expose `StateFlow<*UiState>`; no `!!`, no `try`, no `as ` (non-safe) via custom lint; file ≤ 400 lines; function ≤ 50 lines; cyclomatic ≤ 12 (detekt).
- Tests: Konsist suite runs in `check`.

### T-003 CI workflows
- `pr.yml`: spotless, detekt, lint, konsist, unit, Robolectric, screenshot verify, license gate, dataset validate; `instrumented.yml` (API 26/30/33/latest emulator matrix, KVM); `benchmark.yml` nightly; `release.yml` on tag: signed AAB/APK, changelog from conventional commits, SBOM (CycloneDX).
- Acceptance: all workflows green on main.

### T-004 Custom lint module
- Rules: NoDoubleBang, NoTryCatch, NoUnsafeCast, NoHardcodedNonLatinText, NoGlobalMutableState (top-level `var`/`mutableStateOf`), UseRunCatching, PreferPredictiveBack.
- Tests: `LintDetectorTest` per rule with positive/negative cases.

### T-005 Test infrastructure (`:core:testing`)
- Golden-file helpers, `FakeClock`, `FakeTimeZone`, fixture loaders, Compose test rules with locale/direction/font-scale params, Roborazzi config (devices: phone 412×915, tablet 1280×800, fold 673×841).
- Tests: self-tests.

---

## EPIC 1 — `:core:model` & `:core:calendar`

### T-100 Value types
- `Jdn`, `CalendarDate`, `Weekday`, `MinuteOfDay`, `Coordinates`, `CalendarSystem`, `IslamicVariant`; arithmetic operators; ranges; `Jdn.weekday()`.
- Acceptance: KDoc 100%.
- Tests: U; P: `jdn + n - n == jdn`; `weekday(jdn+7)==weekday(jdn)`.

### T-101 Gregorian ↔ JDN (A-01)
- Tests: G vs ICU4J 1e6 random JDNs in [−1e6, 5e6]; P round-trip; edge: 1582-10-15, year 0, negative years.

### T-102 Persian calendar (A-02)
- Deliverables: `PersianCalendarSystem` with astronomical year start (Tehran) + arithmetic fallback; leap-year predicate; month lengths.
- Acceptance: agrees with official leap years 1300–1500; conversions for all days 1300–1500 match reference table.
- Tests: G: official Nowruz instants 1390–1420 → 1 Farvardin JDN; G: ICU4J agreement 1200–1500 with documented exception list; P round-trip 1e5 random days; B: 1e6 conversions < 300 ms.

### T-103 Islamic tabular (A-03) & UAQ (A-04)
- Tests: G vs ICU4J for 1e5 days each variant; P round-trip.

### T-104 Islamic Iran official (A-05) + observational (A-06)
- Deliverables: override table loader; offset with expiry; "date source" explanation API.
- Tests: G vs D-07 tables; U expiry (30 days) with `FakeClock`; U precedence override > offset > tabular.

### T-105 Nepali (A-07)
- Tests: G vs government samples (20 dates); P round-trip within supported range; U out-of-range error.

### T-106 Calendar utilities
- Month length, months in year, `nthWeekdayOfMonth`, `lastWeekdayOfMonth(offset)`, months distance, add months, day-of-year, week-of-year (A-08), position in season, date-part difference (y/m/d), days between.
- Tests: U table (≥ 200 rows incl. 29/30 Esfand, 32-day Nepali months, Dhul-Hijjah 29/30); P: `addMonths(d, k)` then `-k` returns month start.

### T-107 `kotlinx.datetime` bridge
- `Jdn ↔ LocalDate`, `Instant → Jdn(zone)`; "today" provider interface.
- Tests: U DST transitions (Tehran 2022 abolished DST, Kabul none, Europe/Berlin), P.

---

## EPIC 2 — `:core:i18n`

### T-200 Language table
- `LanguageSpec` records for 24 languages: code, native name, script, direction, default numeral, default calendars order, week start, weekends, prayer method, Asr juristic default, month-name variants, date pattern (dmy/ymd/mdy, separator, zero-pad), AM/PM strings, conjunctions.
- Tests: U every language has all fields; snapshot JSON of table.

### T-201 Numerals
- Persian, Eastern-Arabic, Devanagari, Tamil, Latin; format/parse; grouping & decimal separators; Tamil special 10/100/1000.
- Tests: P round-trip; U separators; U `parse("۱۲٫۵")==12.5`.

### T-202 Date & duration formatting
- Pattern engine; ISO option; calendar abbreviations; "and" joiner; relative phrases ("3 days ago").
- Tests: S-style text snapshot for 24 languages × 4 calendars × {long, numeric, iso} (fixture file); U plural rules via ICU.

### T-203 Persian text normalization & fuzzy match
- Normalize ي→ی, ك→ک, remove tatweel, unify ZWNJ/space, diacritics strip; Damerau-Levenshtein ≤ 2 matcher.
- Tests: U 100 pairs; P idempotent normalization.

### T-204 String resources plan
- All UI text in `strings.xml` (source `en`, `fa` complete before v1); Weblate project; lint for untranslated `fa`.
- Tests: L: `fa` completeness test = 100%.

---

## EPIC 3 — `:core:events` + `:data:events`

### T-300 Rule engine
- `OccurrenceCalculator.occurrences(definition, year): List<Occurrence>` for every `EventRule`, including `RelativeToEvent` (topological resolution, cycle detection) and `Astronomical`.
- Tests: U per rule ≥ 10 cases; G: last Friday of Ramadan 1440–1450, 3rd Thursday of November 2020–2035, day 256 of year, last Tuesday of Persian year; U cycle detection error; P: occurrences sorted & unique.

### T-301 Year cache & day lookup
- Per-(calendar, year) occurrence cache (LRU 64) keyed by enabled sources; `eventsOn(jdn)` merges 4 calendars.
- Tests: U cache hit/miss; B: `eventsOn` 10k days < 50 ms warm.

### T-302 Policies
- `EventVisibilityPolicy` data-driven: source enablement, holiday-only, "hide non-holiday religious outside home timezone", validity ranges, ALWAYS_DISPLAYED, Islamic variant selection by source.
- Tests: U truth table (source × holidayOnly × tz × validity) ≥ 60 rows.

### T-303 Holiday determination & workday basics
- `isHoliday(jdn)`, `isWeekend(jdn)`, `holidayReasons(jdn)`.
- Tests: G: every official Iran holiday 1403–1406 is `isHoliday==true`; U weekends by language.

### T-304 Search index
- In-memory index over titles/aliases (normalized), sources, categories; query API with filters & ranking.
- Tests: U fuzzy "نوروز"/"نوريز"/"nowruz"; B 10k queries < 200 ms.

### T-305 Repository impl (`:data:events`)
- Loads generated dataset; exposes `Flow<DayEvents>` reacting to prefs; combines official + device + personal + ICS.
- Tests: U with fakes; Turbine emission on pref change.

---

## EPIC 4 — `:core:praytimes` & `:core:astronomy`

### T-400 Solar position (A-09)
- Tests: G NOAA samples (declination, EoT) ±0.01°; P continuity.

### T-401 Prayer times (A-10)
- All methods, Asr factors, high-latitude, midnight; `MinuteOfDay` output; no NaN (returns `Result`).
- Tests: G six cities × 12 months (fixture from published timetables) ±2 min; U polar day/night returns explicit `Unavailable`; P monotonic order fajr<sunrise<dhuhr<asr<sunset≤maghrib<isha.

### T-402 Qibla & great-circle (A-11)
- Tests: G 10 cities ±0.2°; U antipode handling.

### T-403 Astronomy façade (A-13)
- Sun/Moon ecliptic & horizon, rise/set/transit, phases & phase times, seasons, eclipses (solar/lunar, local), libration, lunar tilt.
- Tests: G: equinox/solstice instants 2020–2040 vs published (±5 min); G: NASA eclipse catalog 2024–2030; U southern hemisphere phase mirroring.

### T-404 Zodiac & moon-in-Scorpio
- Tropical & IAU boundaries; scorpio entry/exit search.
- Tests: U boundaries; G known dates.

### T-405 Houses, lots, ascendant (A-14)
- Tests: G 20 published charts (ascendant ±0.5°).

### T-406 Tithi (A-15), planetary hours, Chinese/animal year, Hijri-Persian year names
- Tests: G panchang samples; U planetary hour order for each weekday; U animal year mapping table.

### T-407 Photography panel calculations (F-10)
- Golden/blue hour definitions (sun altitude −4°..6°, −6°..−4°), moonrise/set.
- Tests: G vs published photographers' ephemeris samples ±3 min.

---

## EPIC 5 — `:core:nlp`, `:core:ics`, `:core:workdays`

### T-500 NLP date parser (F-03)
- PEG grammar for `fa` and `en`: absolute dates in 3 calendars (numerals in any script), relative ("فردا", "سه روز بعد", "next Friday"), anchored ("۲ روز قبل از عید فطر" via event search), ranges. Returns `ParseResult(date, confidence, span)`.
- Tests: G corpus 500 phrases → expected JDN (fixture); P: formatted date → parse returns same date for 24 languages; U ambiguity ranking.

### T-501 Text date detector (F-04)
- Regex/PEG scanner over arbitrary text; overlapping candidates resolved by score.
- Tests: G 200 real-world snippets; B 100 KB text < 50 ms.

### T-502 ICS reader/writer (F-05)
- RFC 5545 subset: VEVENT, DTSTART/DTEND (date & date-time, TZID), RRULE (FREQ, INTERVAL, COUNT, UNTIL, BYDAY, BYMONTHDAY), EXDATE, SUMMARY, DESCRIPTION, UID, VALARM (display); line folding; escaping.
- Tests: G 30 fixture calendars (incl. Google/Outlook exports); P write→read round-trip; U malformed input → error not crash; fuzz.

### T-503 Recurrence engine (personal events)
- RRULE-lite in any calendar system (e.g. yearly on 30 Esfand → skip non-leap years or move to 1 Farvardin, user-selectable policy).
- Tests: P generated instances strictly increasing; U leap policies; G 50 cases.

### T-504 Workday engine (F-07)
- Profiles: weekend set, holiday sources, half-days, personal leave; `workdaysBetween`, `addWorkdays`, `nextWorkday`.
- Tests: U/G 100 cases incl. Nowruz week; P `addWorkdays(d,n)` lands on workday.

---

## EPIC 6 — Data layer

### T-600 Proto DataStore
- `UserPrefs` proto, typed accessors, defaults derived from `LanguageSpec` on first run.
- Tests: R read/write; U defaults per language.

### T-601 Room schema & DAOs
- Tables §4.3; migrations framework; exported schema JSON in VCS.
- Tests: R DAO CRUD; migration tests from schema 1→N with `MigrationTestHelper`.

### T-602 Device calendar adapter
- Query instances in range; `ContentObserver` → Flow; cache table; permission-aware (no permission → empty, no crash); color parsing; all-day handling across time zones.
- Tests: R with `ShadowContentResolver` fixtures (all-day, multi-day, deleted, invisible calendars); U all-day event dated correctly in UTC+3:30 and UTC−8.

### T-603 Location data
- Cities dataset (own compilation from public geodata, permissive: GeoNames CC-BY or Natural Earth public domain), Iran districts; GPS via `FusedLocation`-free `LocationManager` with timeout; Geocoder wrapper.
- Tests: U sorting per language collation; R timeout path.

### T-604 Scheduler (single source of truth)
- `ScheduledAlarm` table; `AlarmScheduler` uses exact alarms when permitted else inexact + banner; reschedule on boot, timezone change, time change, prefs change; idempotent; dedup by key.
- Tests: R `ShadowAlarmManager` set/cancel/replace; U reschedule matrix; U "skip if fired > 15 min late".

### T-605 Backup/restore (F-11)
- Single file: JSON inside AES-GCM (user passphrase, PBKDF2/Argon2 via platform) or plaintext export; version header; restore preview.
- Tests: U round-trip; U wrong passphrase → error; U forward-compat unknown fields ignored.

---

## EPIC 7 — Design system (`:core:ui`)

### T-700 Theme
- Schemes: system/light/dark/black + dynamic color + user custom seed; gradient toggle; high contrast; custom font (TTF import) & bold; background image; RTL from app language.
- Tests: S theme matrix 6 themes × RTL/LTR; U contrast ratio ≥ 4.5 for text roles (compute).

### T-701 Components
- `TopBar`, `ScreenSurface`, `SegmentedTabs`, `NumberWheel`, `DatePickerSheet`, `EventChip`, `DayCell`, `MonthGrid` (custom Layout, single measure pass), `MoonDisc`, `SunArc`, `ProgressRing`, `EmptyState`, `TooltipCard`.
- Tests: S each × {light, dark, RTL, fontScale 1.0/1.3/2.0}; UI semantics tests.

### T-702 Painters for widgets
- Canvas painters producing bitmaps for month grid, sun arc, moon, map thumbnail; cached `Paint`s.
- Tests: S bitmap snapshots; B render month bitmap < 8 ms.

### T-703 Motion & shared elements
- Standard specs; shared-bounds keys registry.
- Tests: UI: no crash on rapid navigation (monkey 500 events).

---

## EPIC 8 — Feature: Calendar (home)

### T-800 CalendarViewModel & use cases
- State: selected day, month offset, tabs, day details, search; actions; effects (navigate, snackbar).
- Tests: U with Turbine: select/navigate/today/search/prefs change; state is immutable data class (Konsist).

### T-801 Month pager
- `HorizontalPager` with precomputed `MonthUiModel` (cells, holidays, indicators, secondary dates, shift labels) built off-main; ±1 page prefetch; selection animation; long-press → new event; week number column optional & clickable → timeline.
- Tests: UI: swipe ±12 months shows correct titles; UI: long-press opens editor; S 12 months of 1405 in fa/en/ne; B scroll 24 months jank < 1%.

### T-802 Day details tabs
- Calendars overview (all calendars, day distance, week/season progress, zodiac, moon), Events list (chips, source tooltip with citation, open device event), Times (sun arc, prayer list, next-time highlight, city, method).
- Tests: UI each tab; S; a11y: each chip has contentDescription.

### T-803 Toolbar & menu
- Title with secondary calendar; today button; search; menu (pick date, shift work, print month, planetary hours, week numbers, secondary calendar).
- Tests: UI menu actions; S.

### T-804 Search screen (F-12)
- Unified search (events/settings/tools) with fuzzy matching, grouped results, jump-to.
- Tests: UI type "نور" → Nowruz appears; U ranking.

### T-805 Year view
- Zoomable grid, year selection mode, calendar switch by swipe, tap month → jump.
- Tests: UI; S; B.

### T-806 Adaptive layout (F-13)
- Compact: stacked; Medium/Expanded: two-pane; foldable posture aware.
- Tests: S at 3 window classes; UI on tablet emulator.

---

## EPIC 9 — Feature: Timeline (week/day), Month list, Agenda

### T-900 Timeline layout
- Custom `Layout` placing timed events with interval-graph coloring (greedy by start), all-day row, now line, prayer lines, zoom (0.5–2×), 15-minute drag box to create events (move/resize handles), keyboard a11y.
- Tests: U interval coloring (≥ 30 cases incl. nested); UI drag creates event with correct times; S week & day.

### T-901 Month list & agenda
- Infinite lazy lists anchored at today; month headers; print/export.
- Tests: UI scroll 100 items and back; S.

---

## EPIC 10 — Feature: Events editor & reminders (F-01, F-02)

### T-1000 Editor
- Title, calendar system, date (picker or NLP field), time/all-day, recurrence, color, notes, reminder offsets, source link.
- Tests: UI create/edit/delete; U validation.

### T-1001 Reminder notifications
- Channel per type; actions: done/snooze; deep link to event.
- Tests: R scheduled via T-604; UI notification content (`NotificationManagerCompat` shadow).

### T-1002 Official-event reminders
- Attach reminders to `EventId`; recalc yearly.
- Tests: U next occurrence resolution across year boundary.

### T-1003 ICS import/export & subscriptions (F-05)
- SAF import/export; WebCal subscription with periodic refresh (WorkManager, network only when user enables); ETag caching.
- Tests: R import fixture → events in DB; U refresh policy; UI subscription screen.

---

## EPIC 11 — Feature: Times & Athan

### T-1100 Times tab & report
- Expand/collapse; monthly HTML/PDF report via `PrintManager`.
- Tests: UI; S; U report contains 29–31 rows.

### T-1101 Athan settings
- Per-prayer toggles, gap minutes, sound (SAF picker, preview), vibration, DND bypass (Fajr only), volume, "Iran time" toggle.
- Tests: UI each control persists; R scheduling matrix.

### T-1102 Athan playback
- Foreground service with `MediaPlayer` (works regardless of channel sound), stop/snooze actions, respects silent mode unless bypass, stops after max 5 min.
- Tests: R service lifecycle; U dedup (never twice/day/prayer); manual OEM checklist.

### T-1103 Automation broadcasts (F-14)
- `ACTION_ATHAN_STARTED`, `ACTION_DAY_CHANGED` documented; deep links.
- Tests: R broadcast emitted; UI deep link opens day.

---

## EPIC 12 — Feature: Widgets & Notification (Glance)

### T-1200 Widget framework
- Base `GlanceAppWidget` with shared state provider, size-aware layouts, per-widget config screen (color, transparency, scale, content toggles, secondary calendar), update policy (targeted, only installed widgets).
- Tests: Glance unit tests; U update policy.

### T-1201–T-1212 Twelve widgets
- 1×1 date; 4×1 date+clock; 2×2 date+events+next prayer; 4×2 with prayer strip; Month interactive (prev/next/today/add); Month bitmap; Week strip; Schedule list (LazyColumn, 14 days); Sun arc; Moon; Map (day/night, cached bitmap, refresh ≥ 1 min); Countdown/age (F-09) with progress ring.
- Tests per widget: S 3 sizes × light/dark; UI tap actions; B render < 30 ms.

### T-1213 Persistent notification
- Day icon (vector-generated bitmap cached per day/numeral/font), title, other calendars, holidays, prayer strip, lock-screen visibility, large-number mode; posts only on change.
- Tests: R content per state; S big/small views; U icon cache.

### T-1214 Dynamic launcher icon (optional feature)
- Implemented via 31 aliases; toggled with warning; disabled by default.
- Tests: R component state after day change.

### T-1215 Quick Settings tile, app shortcuts, live wallpaper, daydream
- Tests: R tile state; UI shortcuts; wallpaper smoke.

---

## EPIC 13 — Feature: Astronomy, Map, Compass, Level

### T-1300 Astronomy screen
- Modes Earth/Moon/Sun; time slider (±day, ±year) with animation; date picker; header (zodiac, phase, distances, eclipses, seasons); dialogs: horoscope (chart with houses/lots), year horoscope, planetary hours, moon-in-Scorpio.
- Tests: UI; S per mode; U header cache.

### T-1301 Map
- World map (own vector from Natural Earth, public domain); layers: day/night (mask computed off-main, cached), moon visibility, magnetic field/declination/inclination, time zones (own compiled from public tz boundaries CC0), tectonic plates (public domain source), crescent visibility (Yallop/Odeh); grid; location; direct path; Qibla; 3D globe (OpenGL ES 3 / AGSL on 13+).
- Tests: S 6 layers; B mask < 150 ms; U crescent classifier vs published crescent maps (5 dates).

### T-1302 Compass (own implementation)
- Rotation-vector sensor primary, accelerometer+magnetometer fallback, low-pass filter, true north via declination, Qibla arrow, sun/moon/planets azimuth ring, 24-hour path animation, stopped mode, a11y announcements every 15°.
- Tests: U filter & wrap-around; R sensor injection; UI orientation lock.

### T-1303 Bubble level (own implementation)
- Orientation classification (landing/portrait/landscape), calibration offset stored, ruler (cm/in) with screen DPI.
- Tests: U classification; UI calibrate persists.

---

## EPIC 14 — Feature: Tools

### T-1400 Converter, distance, duration calculator, time zones, QR
- Converter with NLP input; distance with workdays (F-07) and animal-year compatibility (optional); duration calculator with own expression grammar (`1d 2h + 30m`); time-zone board; QR (ZXing) with share.
- Tests: U grammar (100 expressions, fuzz); UI; S.

---

## EPIC 15 — Feature: Settings, About, Backup, Privacy, Diagnostics

### T-1500 Settings screens
- Three tabs (Interface & Calendar / Widgets & Notification / Location & Athan) + search within settings; deep-link to item with highlight; every item bound to DataStore.
- Tests: table-driven UI test: for each of N settings, interact → DataStore value changes; S.

### T-1501 Language & first-run
- Language chooser applies `LanguageSpec` defaults; onboarding (3 screens: language, location, events sources) skippable.
- Tests: UI first-run matrix fa/en/ne/ckb → defaults asserted.

### T-1502 Location settings
- City picker (searchable), districts, GPS, coordinates with live geocode, map pick.
- Tests: UI each path persists coordinates.

### T-1503 Backup/restore UI (F-11) & privacy dashboard (F-15)
- Tests: UI round-trip; S.

### T-1504 About, licenses (auto-generated from dependency report), device info, diagnostics & report (F-16)
- Tests: U log ring buffer; UI report intent built without PII (asserted).

---

## EPIC 16 — Wear OS

### T-1600 Wear app
- Screens: today, month, converter, settings (DataStore); tiles: month, next event/prayer; complications: date, month progress, next prayer.
- Tests: U shared core; UI smoke on Wear emulator; S tiles.

---

## EPIC 17 — Accessibility & Internationalization

### T-1700 TalkBack pass
- Every interactive element labeled; day cells summarize date+events; live regions for time; custom actions.
- Tests: Accessibility checks enabled in all UI tests; manual TalkBack script (20 steps) per release.

### T-1701 RTL & font scale
- Tests: S matrix RTL × fontScale 2.0 for all screens; no clipping (pixel-diff on text bounds).

### T-1702 Translations
- `fa` 100% at v1; others via Weblate; pseudo-locale test build.
- Tests: L completeness.

---

## EPIC 18 — Performance, Reliability, Security

### T-1800 Baseline & startup profiles; R8 full mode; resource shrinking
- Tests: B cold start; APK size check ≤ 8 MB (or 90% of first measured).

### T-1801 Macrobenchmarks
- Startup, month scroll, timeline scroll, widget render, map mask, search.
- Acceptance: budgets §9; nightly regression > 10% fails.

### T-1802 Compose stability
- Compiler metrics in CI; all UI models `@Immutable`/stable; recomposition count tests for `DayCell`.
- Tests: U recomposition counter ≤ 1 per state change.

### T-1803 Memory & leaks
- LeakCanary debug; heap budget test on month screen < 80 MB.

### T-1804 Security
- No network permission in v1 except optional ICS subscriptions (declared, off by default); `FileProvider` scoped; backup encryption; exported components audited; StrictMode in debug.
- Tests: L manifest audit test (no unexpected `exported=true`).

---

## EPIC 19 — Release & Support

### T-1900 Release engineering
- Reproducible builds; signing; Play internal→closed→open→staged; GitHub Releases with APK + SBOM; F-Droid-compatible metadata (if license allows); changelog automation.
- Tests: release dry-run in CI.

### T-1901 Support system
- In-app FAQ (`fa`/`en`), "report a problem" (F-16), GitHub issue forms, dataset correction form, SLA doc: P0 crash fix ≤ 48 h, holiday data error ≤ 72 h (data-only release path without app update via bundled override + optional signed dataset update file).
- Tests: U dataset override loader (signed JSON verification).

### T-1902 Beta program
- 2-week closed beta with checklist (Section 10.5); exit criteria crash-free ≥ 99.9%.

---

# 8. Test Strategy (strict)

## 8.1 Pyramid & gates

| Level | Tooling | Scope | Gate (CI fails if…) |
|---|---|---|---|
| Static | ktlint, detekt, Android Lint, `:lint`, Konsist, license gate | all | any error/warning |
| Unit | JUnit 5 + Kotest | `core/*`, ViewModels, use cases | coverage `core` < 95% line / 90% branch; overall < 85% |
| Property | Kotest property | conversions, rules, numerals, recurrence, NLP | any failing property (1 000 iterations default, 10 000 nightly) |
| Golden | JUnit 5 fixtures with citations | calendars, holidays, prayer times, astronomy, NLP corpus | any mismatch |
| Robolectric | Robolectric | DataStore, Room, scheduler, content resolver, notifications, services | any failure |
| Screenshot | Roborazzi | components, screens, widgets × theme × RTL × font scale × device class | any pixel diff (threshold 0.1%) |
| Instrumented UI | Compose test + Accessibility checks | 40 core scenarios | any failure on any API in matrix |
| Benchmark | Macrobenchmark | budgets §9 | regression > 10% (nightly) |
| Fuzz | Jazzer (nightly) | dataset validator, ICS, NLP, expression grammar, backup parser | any crash |
| Mutation (monthly) | Pitest on `core` | — | mutation score < 80% reported |
| Manual | Release checklist | OEM/athan/widgets/TalkBack | sign-off required |

## 8.2 Fixture governance
- Every golden fixture file has a header: source URL/title, retrieval date, page, reviewer. Fixtures without provenance fail a repo test.
- Fixtures are immutable; updates require a PR with justification.

## 8.3 Core UI scenarios (40)
Install fresh (fa/en/ne/ckb/ar) → defaults; onboarding skip/complete; month swipe & titles; select day → tabs; today button; long-press → editor; create personal event + reminder → notification fires (Robolectric clock); edit/delete event; official-event reminder; search jump; year view select; timeline drag-create; agenda scroll; converter (each calendar); NLP input 10 phrases; distance & workdays; enable athan → alarm scheduled → fires → stop; change location by city/GPS/coords; add each widget → tap → deep link; notification content; theme changes (6); font scale 2.0; RTL switch; backup → wipe → restore equals; ICS import/export; subscription refresh; compass orientation lock; map layers; astronomy slider; settings table-driven; deep links (10); PROCESS_TEXT detection; wear smoke.

---

# 9. Quality Budgets

| Metric | Budget |
|---|---|
| Cold start to first month frame | ≤ 350 ms mid-range (Pixel 6a class), ≤ 800 ms low-end API 26 |
| Month scroll jank (24 months) | < 1% frames > 16 ms |
| `eventsOn(jdn)` warm | < 5 µs |
| Month model build | < 3 ms |
| Widget render (any) | < 30 ms |
| Map mask | < 150 ms off-main |
| Search 10k events | < 20 ms/query |
| Memory (month screen) | < 80 MB RSS |
| APK (arm64, release) | ≤ 8 MB |
| Battery | 0 wakeups if no widget/notification/athan; ≤ 1 wakeup/hour otherwise |
| Crash-free sessions | ≥ 99.9% |
| a11y | 0 Accessibility Scanner errors |

---

# 10. Timeline (single senior engineer; parallelizable to ~14 weeks with 2)

| Weeks | Epics |
|---|---|
| 1 | E0 |
| 2–3 | E1, E2 |
| 4–5 | E3 + Dataset D-01..D-09 (data work parallel with a reviewer) |
| 6–7 | E4, E5 |
| 8 | E6 |
| 9 | E7 |
| 10–11 | E8 |
| 12 | E9 ‖ E11 |
| 13–14 | E10 ‖ E12 |
| 15–16 | E13 ‖ E14 |
| 17 | E15 |
| 18 | E16 ‖ E17 |
| 19 | E18 |
| 20–22 | E19 (beta) |

---

# 11. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Astronomical Persian year start disagrees with official calendar in edge years | Official leap-year table overrides computed value; golden tests per year; ADR documents policy |
| Dataset accuracy | Two-person review per year; citations; fast data-only fix path (T-1901) |
| OEM background limits kill athan | Foreground service, exact alarm permission flow, battery-optimization guidance, OEM test matrix |
| Glance limitations for graphical widgets | Bitmap-based widgets rendered by painters (T-702) |
| Clean-room contamination | PROVENANCE.md per algorithm, reviewer attests no reference to forbidden repos, license gate |
| Scope creep | v1.0 = §2.1 + F-01…F-16; F-17/F-18 deferred |

---

# 12. Definition of Done (project)

1. All tasks T-000…T-1902 merged with tests; CI green on main for 7 consecutive days.
2. Coverage gates met; screenshot suite complete for every screen/widget.
3. Dataset years 1403–1406 (Iran), current+next year (Afghanistan, Nepal), UN list — all golden tests green with citations.
4. Budgets §9 met on reference devices; benchmark history stored.
5. Zero forbidden licenses in SBOM; PROVENANCE complete for every `core` algorithm.
6. `fa` and `en` 100% translated; RTL/TalkBack sign-off.
7. Docs: ARCHITECTURE.md, ADRs, DATASET.md (schema + contribution), ALGORITHMS.md (specs §6), AUTOMATION.md (intents/deep links), SUPPORT.md (SLA), RELEASE.md.
8. Beta exit criteria met; staged rollout to 100%.

---

# 13. Deliverables Checklist

- [ ] Android app (phone/tablet/foldable), API 26+
- [ ] Wear OS app
- [ ] 12 widgets, notification, tile, shortcuts, wallpaper, daydream
- [ ] Dataset repo with validator & CI
- [ ] Test suites (unit/property/golden/Robolectric/screenshot/UI/benchmark/fuzz)
- [ ] Documentation set (§12.7)
- [ ] Release pipeline & support process
