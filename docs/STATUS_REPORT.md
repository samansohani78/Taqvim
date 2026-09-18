# Taqvim — Status report

Generated 2026-09-18 from `docs/PLAN.md`, `docs/PROGRESS.md`, `docs/DATA_TODO.md`, `docs/DATA_AUDIT.md`, `git log` and
the build reports, at main@41a6b04 (release-candidate refresh of the 2026-09-17 report at main@cbfb1da). Status mapping:
PROGRESS `DONE` → DONE (open validation listed); `WIP` → PARTIAL, or BLOCKED when it waits on the owner or on outside
data; `TODO` → NOT STARTED. DATA_TODO `Resolved` → DONE, `Partly resolved`/`In progress` → PARTIAL, `Open` → BLOCKED.

## 1. Tasks

### Plan tasks (T-xxx, D-xx) and owner-approved additions (F01, F02, F03, F07; T-108–T-110 per ADR-0025)

Summary: 115 rows — 103 DONE, 3 PARTIAL, 9 BLOCKED, 0 NOT STARTED.

| ID | Title | Status | Commit hash(es) | Tests added | Open item |
|---|---|---|---|---|---|
| T-000 | Repository bootstrap | DONE | f34ecdb | R: MainActivityTest · U: AppModuleTest (Koin graph verify), TaqvimIssueRegistryTest · L: ProjectStructureKonsistTest; spotless/detekt/lint/Kover gates green | — |
| T-001 | License gate | DONE | d6c7ac3 | U: AllowListParserTest, SpdxNormalizerTest, PomLicensesTest (XXE, parent inheritance, cache lookup), LicensePolicyTest, ClassificationAndCodecTest · Negative: LGPL … | — |
| T-002 | Architecture rules (Konsist) | DONE | 3ea66ba | Konsist: ArchitectureKonsistTest (10 rules on real code), ArchitectureRulesTest (17 unit), KonsistFixturesTest (8 end-to-end, planted violations) | — |
| T-003 | CI workflows | DONE | 8587d6b, 42f7228, 9a152e5 | L: actionlint (4 workflows + composite action) · local: CycloneDX SBOM (192 runtime components, no test deps), signed release APK verified by apksigner (v2), full gate … | local |
| T-004 | Custom lint module | DONE | c3fc147 | L: SourceRuleDetectorsTest (6), StateTextAndBackDetectorsTest (8), TaqvimIssueRegistryTest (3) — LintDetectorTest positive/negative per rule; Konsist: string-literal … | — |
| T-005 | Test infrastructure (`:core:testing`) | DONE | e10a0a2 | U: GoldenFilesTest, SnapshotAndTimeFakesTest, ScreenshotMatrixTest · R: ScreenshotEnvironmentRobolectricTest · S: ui_testing_self_test (1 reference) · Konsist: … | — |
| T-100 | Value types | DONE | e94b47d | U: JdnTest (8), CalendarTypesTest (7) · P: jdn+n−n==jdn, weekday(jdn+7)==weekday(jdn), weekday+7, MinuteOfDay wrap | — |
| T-101 | Gregorian ↔ JDN (A-01) | DONE | c266f45 | G: GregorianIcuOracleTest (1 000 000 random JDNs in [−1e6, 5e6] vs ICU4J proleptic) · U/P: GregorianCalendarSystemTest (1582-10-15, year 0, negative years, round-trip, … | — |
| T-102 | Persian calendar (A-02) | DONE | 9b1b7bf, dfe6f17, c6f13e6 | G: PersianCalendarOfficialTest — official leap years 1206–1498 (293), every day of the official 1404/1405 calendars (730, incl. weekday), official Nowruz instants … | data partial |
| T-103 | Islamic tabular (A-03) & UAQ (A-04) | DONE | 97c9da4, 2fed071, ef282be, 9c33702 | G: TabularIslamicCalendarTest — type II vs ICU4J civil on 100 000 random days; UmmAlQuraCalendarTest — every month AH 1300–1600 and 100 000 random days (AH 1200–1700) vs … | — |
| T-104 | Islamic Iran official (A-05) + observational (A-06) | DONE | 28a2d16, ac90cf8, 842b720, 27950c5 | G: IranIslamicCalendarTest — every lunar date of the official 1404/1405 calendars (730), 26 published month starts, table edges, 29/30-day months 1440–1455, 100 000 … | data partial |
| T-105 | Nepali (A-07) | DONE | d47a10a, 1a08566, 58994dc | G: NepaliCalendarSystemTest (10) — official National Panchang 2082/2083 month starts and sankranti times ±1 min, night rules, Kathmandu sunrise/sunset ±3 min, cached … | — |
| T-106 | Calendar utilities | DONE | 5cbcf02, 44f4346, bf69a9f | U: CalendarMathTest — 135 rows over Gregorian, tabular Islamic (incl. Dhul-Hijjah 29/30) and Umm al-Qura; PersianCalendarMathTest — 109 rows (29/30 Esfand from the … | — |
| T-107 | `kotlinx.datetime` bridge | DONE | ba40f51 | U: DateTimeBridgeTest — LocalDate↔Jdn, out-of-range rejection, Asia/Tehran day boundaries before/after DST abolition (2022), Asia/Kabul fixed offset, Europe/Berlin & … | — |
| T-108 | Hebrew calendar (A-15, ADR-0025) | DONE | b20c5c6, 381f6c0, d15b2a4, 3170881, 76c6ddd | G: UsnoJewishObservancesTest (5) — all 57 839 USNO Jewish observances 360–9999 exact (Julian before 1582-10-15), year lengths from consecutive new years · P/U: … | — |
| T-109 | Gregorian Easter and movable feasts (A-16, ADR-0025) | DONE | f9d2140, 330d795 | G: ChristianMovableFeastsTest — all 67 336 USNO Christian observances 1583–9999 exact; Meeus ch. 8 examples · P: Easter a Sunday 22 Mar–25 Apr and Advent a Sunday 27 … | not shown in the app |
| T-110 | US daylight saving time (ADR-0025) | DONE | 7b336c2 | G: UsDaylightSavingRulesTest (5) — all 16 066 USNO begin/end dates 1967–9999 exact, none before 1967, March 8–14/November 1–7 Sundays for random years to 10 000 000, any … | — |
| T-200 | Language table | DONE | cdf1a66 (model: 7e4e798) | U: LanguageTableTest (29: approved list, every language has every field ×24, exact data-gap set, numeral/direction rules, lookup & and-joining, snapshot … | — |
| T-201 | Numerals | DONE | b05c636 | U: NumeralsTest — `parse("۱۲٫۵") == 12.5`, separators and grouping per system (incl. Indian grouping), lenient mixed-digit parsing, traditional Tamil (18 rows + … | — |
| T-202 | Date & duration formatting | DONE | 05bf3e6 | U + oracle: PluralRulesTest (27), FormatTableTest (4), DateFormatterTest (47), RelativeTimeFormatterTest (48) — against ICU4J PluralRules (n 0–1000), full date formats … | — |
| T-203 | Persian text normalization & fuzzy match | DONE | 94d2947 | U: PersianTextTest — 50 normalization + 50 fuzzy pairs, search keys, OSA distance with early exit · P: normalize/searchKey idempotent, distance symmetric and zero iff … | — |
| T-204 | String resources plan | DONE | 1a4d047, 577c1f3 | L: MissingFarsiTranslationDetectorTest (4: untranslated string/plurals/array reported, missing values-fa folder, complete fa clean, other locales may be incomplete), … | — |
| T-300 | Rule engine | DONE | d7cf58e, d995b10, 8676be7 | U: RuleEvaluationTest — 108 cases, ≥ 10 per rule (Fixed incl. 29 Feb and 30 Esfand, NthWeekday, LastWeekday ± offset, LastDayOfMonth, Single, NthDayOfYear, … | — |
| T-301 | Year cache & day lookup | DONE | 6db021b | U: EventLookupTest (6) — Persian/Islamic/Gregorian merge and order, disabled source vs ALWAYS_DISPLAYED, offset crossing into the next year, cache hits/misses/eviction, … | — |
| T-302 | Policies | DONE | 5c701af, 76c6ddd | U: EventVisibilityPolicyTest — 192-row truth table (source × holiday-only × hide-abroad × abroad × validity none/in/out × holiday × ALWAYS_DISPLAYED) + 4 cases … | — |
| T-303 | Holiday determination & workday basics | BLOCKED | 12b54fa | U: HolidayCalendarTest (3) — reasons, enabled sources only, weekend parameter and per-language CLDR weekends · G: OfficialHolidaysGoldenTest (`:data:events`) — generated … | — |
| T-304 | Search index | DONE | c635f90 | U: EventSearchIndexTest (9) — نوروز / نوريز / nowruz, ranking exact > prefix > substring > fuzzy, two-edit fuzzy, short queries, ties, filters, limits, best matching … | — |
| T-305 | Repository impl (`:data:events`) | DONE | c8b422e, 2ecb37a, 1413bc2, cbd40b6, 5f1d9db, 7a90a76, 9f23c0b | U: EventsRepositoryTest (6) — Nowruz 1405 holiday, weekend and official Hijri date; Turbine re-emission on preference change, none for identical results; Hijri … | — |
| D-01 | Dataset JSON Schema + validator CLI | DONE | ad81460 | U: DatasetValidatorTest — valid sample covering all 8 rule types, 30 invalid fixtures each reporting exactly one issue (all 6 issue kinds: malformed JSON, schema, … | — |
| D-02 | Iran official holidays 1403–1406 | BLOCKED | 945d0c5 | G: IranOfficialHolidaysTest — the dataset's rules reproduce exactly the 26 official holiday dates of 1404 and of 1405 (golden date sets and the official_holiday column … | — |
| D-03 | Afghanistan set | DONE | e031285, d995b10 | G: AfghanistanOfficialHolidaysTest (5) — validator clean; golden per Solar year (1404: 1, 1405: 6) equals the records' dates; dataset ids = golden ids; every record … | weekday-dependent one-off days (DT-031); further announcements and labour-law holidays (DT-032) |
| D-04 | Nepal set | DONE | 8676be7 | G: NepalOfficialHolidaysTest — rules equal every dated day of the MoHA 2082 and 2083 notices in both directions; NepaliLunarDaysTest — 26 lunar dates incl. intercalary … | Gyalpo Lhosar, Fagu Purnima, regional and community holidays, Eid dates by sighting (see DATA_TODO) |
| D-05 | UN international days | BLOCKED | 2648a53, 5c281e0, c77c571 | G: UnInternationalDaysTest (4) — validator clean, golden count 102 (96 Fixed, 5 NthWeekdayOfMonth, 1 LastWeekdayOfMonth), 23 rules spot-checked against the cited pages … | 102 of ≥ 150 (blocked on Persian titles, DT-019) |
| D-06 | Ancient Iranian festivals | BLOCKED | 05d9fc1 | G: AncientIranianFestivalsTest (3) — validator clean; golden count 2 and rules (Fixed 1/6, Fixed 9/30) against the cited pages; every record PERSIAN, ANCIENT_IRAN, … | 2 records |
| D-07 | Islamic Iran override table 1390–1410 | DONE | 6dfb3be, 27950c5 | G: IslamicIranOverridesGoldenTest (5) — validator clean; table equals `IranOfficialMonthStarts` month for month; every printed start is day 1 of its Hijri month on the … | official months beyond Ramadan 1446 – Shawwal 1448 (DT-002) |
| D-08 | Dataset code generator (KotlinPoet) | DONE | 49d1ead, fe060cc | S: EventsCodeGeneratorTest — snapshot of the synthetic sample (all 8 rule types, validity, flags, aliases, links), determinism/id order/file splitting, empty dataset · … | — |
| D-09 | Dataset CI + CONTRIBUTING-DATA.md | DONE | 5ff3a12 | L: actionlint (5 workflows) · local: `:tools:dataset:validate` (1 file, 0 issues); the existing D-01 fixture 13-missing-citation proves a record without a citation is … | local |
| T-400 | Solar position (A-09) | DONE | e408019 | G: NoaaSolarCalculatorTest — all 240 rows of NOAA's Solar Calculations spreadsheet (declination, equation of time, solar noon, sunrise, sunset, zenith, refraction, … | — |
| T-401 | Prayer times (A-10) | DONE | e6b65cb, 7846949 | G: PrayerTimesOfficialTest — Institute of Geophysics official 1405 timetables, 31 Iranian cities × 365 days: Fajr, sunrise, Dhuhr, sunset, Maghrib within 1 min, midnight … | — |
| T-402 | Qibla & great-circle (A-11) | DONE | ce4d495 | U/oracle: GreatCircleTest (13) — 10 sample cities, bearing ±1e-6° and distance ±1 m against an independent vector formulation; antipode, coincident points and poles → … | — |
| T-403 | Astronomy façade (A-13) | DONE | 2facefb, 456d49a, e3f4938, d30c4b3, e710000, e322bf1, 0c9557e, 5dea390 | G: EclipsesTest — NASA GSFC catalogs 2024–2030, all 16 solar + 16 lunar eclipses (type, peak ±5 min), local total eclipse 2024-04-08 and partial 2029-01-14 at catalog … | — |
| T-404 | Zodiac & moon-in-Scorpio | BLOCKED | 5ccdd5f | U: ZodiacTest (5) — tropical sign boundaries (0°, 30°, 210°–240°, negative and multi-turn angles), IAU constellation of bright stars far from boundaries (Sco, Leo, Vir, … | — |
| T-405 | Houses, lots, ascendant (A-14) | BLOCKED | 7c1946f | U: HousesTest (4) — ascendant on the eastern horizon and midheaven culminating (property, 1950–2050, latitudes ±60°, all longitudes), Placidus cusps 11/12 at ⅓ and ⅔ of … | — |
| T-406 | Tithi, planetary hours, Chinese/animal year, year names | BLOCKED | a332cf4, 88e16b2, 9f23c0b | U: PlanetaryHoursTest (3) — Chaldean order and weekday rulers for every weekday, unequal day/night hours, polar Unavailable · U: TithiTest (3) — 30 tithis per synodic … | — |
| T-407 | Photography panel calculations (F-10) | DONE | cdb2902 | U: PhotographyPanelTest (3) — morning/evening golden hour (apparent altitude −4°…6°) and blue hour (−6°…−4°) ordering and durations, moonrise/moonset, polar cases empty | reference check pending |
| T-500 | NLP date parser (F-03) | DONE | f0c0af1 | G: 500-phrase synthetic fa/en corpus (`golden/nlp/date-phrases.tsv`; expected days from `:core:calendar`, never from the parser) · P: NUMERIC and ISO format → parse … | LONG gaps |
| T-501 | Text date detector (F-04) | DONE | ecb16ea, 5d4cf77 | G: TextSnippetCorpusTest — 200 synthetic everyday fa/en snippets (176 with dates: single, weekday-prefixed, yearless, numeric, Islamic/Gregorian month names, ranges; 24 … | real-world corpus pending |
| T-502 | ICS reader/writer (F-05) | DONE | 31f5c52 | G: 30 synthetic fixture calendars (Google/Outlook-style exports: PRODID, VTIMEZONE, X- properties, Windows zone names, folding, escaping) · U: content lines (6), values … | — |
| T-503 | Recurrence engine | DONE | 4464dbb, 697cc1a, e7cb1da, 7a90a76 | G: RecurrenceOracleTest — 5 RFC 5545 examples + 50 Gregorian cases against an independent day-by-day java.time oracle (52) · U: RecurrenceEngineTest — invalid-day … | — |
| T-504 | Workday engine (F-07) | DONE | d19884b | U: WorkdayCalculatorTest — 100 cases (50 addWorkdays, 50 workdaysBetween) against an independent java.time loop, next/previous/zero semantics, profile options (sources, … | — |
| T-600 | Proto DataStore | DONE | 83f9182 | U: UserPreferencesMappingTest — defaults for all 24 languages, proto ↔ model mapping, table-driven migrations · R: UserPreferencesRepositoryTest — DataStore read/write … | — |
| T-601 | Room schema & DAOs | DONE | c2e5b80, 6928708 | R: PersonalDaoTest, WorkAndCacheDaoTest — in-memory DAO CRUD with Turbine flows · R: TaqvimDatabaseMigrationTest — MigrationTestHelper validates every exported schema up … | — |
| T-602 | Device calendar adapter | DONE | ca9a397, 5f1d9db | U: DeviceEventMappingTest (6) — all-day event keeps its date in UTC+3:30, UTC−8, Asia/Tehran, America/Los_Angeles; timed events in the device zone; property: every … | — |
| T-603 | Location data | DONE | c2e1528 | U: CityTableParserTest (3) — any column order, empty localized column = English spelling, prs→fa fallback, bad rows rejected with line number · U: CityCatalogTest (4) — … | districts pending |
| T-604 | Scheduler | DONE | 4fbcf1e, 762c432 | U: AlarmPoliciesTest (5) — skip if fired > 15 min late (exactly 15 min is on time), dedup, reconcile, restore drops late alarms and duplicates · U: ReschedulePolicyTest … | — |
| T-605 | Backup/restore (F-11) | DONE | d594bea, c6cdb01, d1f2863 | U: BackupCryptoTest (4) — PBKDF2-HMAC-SHA256 and SP 800-108 KDF against independently computed vectors (ASCII and UTF-8 passphrases), AES-GCM rejects wrong … | — |
| T-700 | Theme | DONE | 99eac40 | U: SchemeContrastTest (2) — WCAG 2.2 contrast of 71 text/background role pairs ≥ 4.5:1 (standard) and ≥ 7:1 (high contrast) for random, default and extreme seeds in … | — |
| T-701 | Components | DONE | 22d40c7 | U: ComponentGeometryTest (11) — wheel index/progress arithmetic, date-selection day clamping (29/30 Esfand), picker and month-grid model validation, gap-free column … | — |
| T-702 | Painters for widgets | DONE | 909ed8b | P: PainterGeometryTest (7) — month cells tile every row without gaps and mirror exactly in RTL for any size/columns/rows (property), week column, event dots ≤ 3, text … | — |
| T-703 | Motion & shared elements | DONE | 9b8d98b, a559999 | U: TaqvimMotionTest (4) — durations follow the animator scale (0/negative/NaN = no motion), reduced motion yields no transition, RTL mirroring and push/pop reversal … | — |
| T-800 | CalendarViewModel & use cases | DONE | 720443c | U: CalendarViewModelTest (12, Turbine) — load today in every calendar with month and events; select day → month and events; month paging keeps the selection; today … | — |
| T-801 | Month pager | DONE | 41c189d, 5d4cf77 | UI: CalendarScreenTest (5, Robolectric) — swipe 12 months forward and 24 back shows each month's title; tap selects a day; long press opens the editor on that day; week … | benchmark not run |
| T-802 | Day details tabs | DONE | 55ea31e | U: DayDetailsCalculatorTest (8) — Nowruz 1405: 3 days from 27 Esfand, day 1 of week/year, spring day 1 of 93, Sun in Aries; negative distances; southern seasons swapped … | — |
| T-803 | Toolbar & menu | DONE | a84ee78 | U: CalendarMenuTest (5, Turbine) — menu and dialogs; go to date selects the clamped primary-calendar day; week numbers and secondary calendar stored and followed; failed … | — |
| T-806 | Adaptive layout (F-13) | DONE | a84ee78 | U: CalendarLayoutTest (4) — stacked below 600 dp, two panes from 600 dp, tabletop at any width; picker range and month names · UI: CalendarAdaptiveTest (3, Robolectric) … | tablet emulator run pending |
| T-804 | Search screen (F-12) | DONE | de0aacf, 2ecb37a, 1413bc2, 6be22a5, a4243f3 | UI: SearchScreenTest (4, Robolectric) — typing "نور" through the route shows Nowruz and opens it (1 Farvardin 1405); grouped headings, go-to-date, filters, clear; … | — |
| T-805 | Year view | DONE | 6a47034, 803842f, 5d4cf77 | U: YearViewModelTest (7, Turbine) — today's year in the first calendar with day flags; next/previous/chosen/today years (limit 3000); calendar switch keeps the same … | macrobenchmark not run |
| T-900 | Timeline layout | DONE | 912a2b6, a4243f3 | U/P: IntervalColoringTest (5) — 33-case table (disjoint, touching, overlapping, nested, chained, day edges) + overlaps never share a column, cluster width = deepest … | — |
| T-901 | Month list & agenda | DONE | 551aa61 | UI: AgendaScreenTest (6, Robolectric) — scroll 100 rows forward and back, both list ends request more months; mode, today, print, share, event and day actions; today … | — |
| T-1000 | Events editor | DONE | 34cc9a9, 5d4cf77, a72f00c, e9b99ec, a9d2d73, f31a07c, 60cca26, 01ea742, a4243f3 | U: EventValidatorTest (8) — blank titles (property), title/notes/link limits, end before start (property), interval/count in Latin/Persian/Eastern Arabic digits … | — |
| T-1001 | Reminder notifications | DONE | a6b2edf, 762c432, 37ac1b2, a4243f3, 7a90a76, 9a152e5 | P: ReminderPlannerTest — random events in Persian, Gregorian and Islamic calendars (all-day/timed; Tehran, Berlin, Los Angeles; DAILY/WEEKLY/MONTHLY/YEARLY with every … | — |
| T-1002 | Official-event reminders | DONE | a6b2edf, d604af6 | U: OfficialRemindersTest (3) — next Nowruz across the Persian year boundary (late Esfand 1404 → 1 Farvardin 1405 = 2026-03-21; after it → 1406); titles in the language … | — |
| T-1003 | ICS import/export & subscriptions | DONE | 2a4a56b, 7d38d7a, 2ecb37a, c9b0223, 9a3ff36 | R: IcsRoomTest (4) — fixture import into Room (3 created, repeated UID skipped, reader problem reported), duplicates skipped or replaced by UID, unreadable text stores … | — |
| T-1100 | Times tab & report | DONE | ea91bd3 | U: MonthlyReportTest (5) — one row per day for every month of Persian 1400–1410, Gregorian 2024–2026 and Umm al-Qura 1445–1447; HTML with `dir`/`lang`, Persian digits, … | — |
| T-1101 | Athan settings | DONE | df5f30f, 39f5e17 | U: AthanPreferencesMappingTest (5) — defaults identical for all 24 languages (every athan off); random settings round-trip through proto and serializer (property); … | — |
| T-1102 | Athan playback | DONE | c5c1492, 762c432 | P: AthanPlannerTest (5) — for random instants in 2026, gaps −60…60 and any set of enabled prayers, athans = A-10 times + gaps, strictly after now, ordered, one per … | OEM checklist not written |
| T-1103 | Automation broadcasts (F-14) | DONE | a2c4179, 609b23f | U/P: DeepLinksTest (5) — every documented link → destination, unreadable links → calendar, fuzz 1 000 arbitrary strings never throw, every Persian y-m-d link → exactly … | — |
| T-1200 | Widget framework | DONE | 163eed2, 9a152e5 | U: WidgetUpdatePolicyTest (6) — each trigger reaches only dependent installed widgets (property); next day at local midnight in Tehran, São Paulo 2018 skipped midnight, … | — |
| T-1201 | Widget: 1×1 date | DONE | b976f33, 1515e02, 0439373 | G: DayWidgetsGlanceTest — day number and weekday, tap opens today · U: WidgetContentBuilderTest — fa/en titles, weekdays and digits · U: WidgetCatalogTest — registration … | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1202 | Widget: 4×1 date + clock | DONE | b976f33, 1515e02, 0439373 | G: full date and secondary date, no events | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1203 | Widget: 2×2 date + events + next prayer | DONE | b976f33, 1515e02, 0439373 | G: 2 events at MEDIUM and 4 at LARGE; holiday opens its day, personal event opens `taqvim://event/<id>`; next prayer opens times; no-events and no-place texts; content … | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1204 | Widget: 4×2 with prayer strip | DONE | b976f33, 1515e02, 0439373 | G: six times with the next one bold; one-row widget keeps only the strip; no-place text opens times | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1205 | Widget: Month interactive | DONE | b0355f7, a438f8c, 5d4cf77 | G: previous/next/today steps kept per widget id; days open `taqvim://day`; holiday and weekend tones; secondary day at 4×4; RTL-mirrored controls; + opens … | screenshots and device tap tests pending |
| T-1206 | Widget: Month bitmap | DONE | b0355f7, a438f8c | G: T-702 month painter at the widget's pixel size; holidays, events and secondary days follow the widget's settings · B: best-of-5 render time under a loose 250 ms … | screenshots and < 30 ms device render benchmark pending |
| T-1207 | Widget: Week strip | DONE | b0355f7, a438f8c | G: seven days from the user's week start with weekday initial, tones and event dot; each day opens its day | screenshots and device tap tests pending |
| T-1208 | Widget: Schedule list | DONE | b0355f7, a438f8c | G: today and event days of the next 14 days as a LazyColumn; headers open the day, personal events the editor; empty message | screenshots and device scroll tests pending |
| T-1209 | Widget: Sun arc | DONE | b0355f7, a438f8c | G: T-702 sun-arc painter with daylight progress and the next prayer; without a place asks for one and opens times · U: Tehran sunrise/sunset, polar night | screenshots pending |
| T-1210 | Widget: Moon | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71 | G: SkyWidgetsGlanceTest — sizes, Persian digits, links · U: WidgetSkyBuilderTest (100 widget tests) | screenshots pending |
| T-1211 | Widget: Map | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71 | G: SkyWidgetsGlanceTest · U: WidgetSkyAdaptersTest (6) — outline loaded once and retried, 72×36 day/night grid, sun position, projection · U: ShadeAndRingPainterTest … | screenshots pending |
| T-1212 | Widget: Countdown/age (F-09) | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71, 5d4cf77 | P: WidgetCountdownMathTest — Persian, Gregorian, tabular Islamic; 30 Esfand / 29 February move to the month's last day in common years · U: WidgetCountdownBuilderTest | screenshots pending |
| T-1213 | Persistent notification | DONE | bf20f3c, f080650, be04534, 9a152e5 | notification module 71 tests (29 new): content (date, weekday, next prayer, other calendars, holidays, prayer strip), setting on/off, re-post only on change or when … | device checks pending |
| T-1214 | Dynamic launcher icon | DONE | bf20f3c, f080650, a4e7d4b | R: LauncherIconTest — exactly one entry enabled, new enabled before old disabled, nothing written while off, DONT_KILL_APP · allowlist + release audit (35 new lines) | device checks pending |
| T-1215 | QS tile, shortcuts, live wallpaper, daydream | DONE | be04534, 4db72a8, a4e7d4b, 9a152e5 | R: TodayTileTest (onDestroy skipped: Robolectric shadow bug); shortcut intents parse through DeepLinkParser; wallpaper redraw scheduling (14 wallpaper tests); daydream … | device checks pending |
| T-1300 | Astronomy screen | DONE | e4c1222, 5d4cf77, 76c6ddd | U: AstronomyHeaderCacheTest (4) — Moon and signs once per hour, seasons and eclipses once per UTC day, separate places, LRU eviction; next season, total solar eclipse … | — |
| T-1301 | Map | DONE | ce9356a, 074ab4a, 50b537b, e216e41, 6abd6b9, ece6731, c8ee198, 4a5c127, a8041a3, cbd40b6, 76c6ddd | U/P: MapGeometryTest (6) — projection round trip (property), longitude wrap, screen↔map round trip with zoom 1–8 and pan inside the map (property), zoom keeps the point … | DT-034 crescent-map comparison; globe frame time on a device |
| T-1302 | Compass | DONE | 8a40fcf | U: AnglesTest (5) — wrap and signed-turn properties, filter across north, circular mean, 15° announcements with hysteresis (property) · U: RotationMathTest (7) — … | — |
| T-1303 | Bubble level | DONE | 8a40fcf | U: LevelMathTest (5) — classification (scale-invariant property), flat/portrait/landscape tilt (property, face-down/upside-down), calibration per orientation, tolerance, … | — |
| T-1400 | Converter, distance, duration, time zones, QR | DONE | 6179966, 9945456, 5f1d9db | U: DurationExpressionTest (3) — 106-expression table: 78 values incl. Persian units/digits, decimals, nesting, ±100 000 days; 28 errors with positions · P: … | — |
| T-1500 | Settings screens | DONE | 33060a6, 3fc4d09, a9d2d73, 9a3ff36 | U: AppSettingsMappingTest (5) — defaults identical for all 24 languages (week numbers off, ancient Iranian festivals off); random settings round-trip through proto and … | — |
| T-1501 | Language & first-run | DONE | c4273e2, fe9096e, f749b4a | U: LanguageSwitchTest (5) — 24×24 defaults; changed values kept (property); chosen values survive round trips; unknown → en; first run per device language … | — |
| T-1502 | Location settings | DONE | 4d7cae6, 5980f0f, a9d2d73 | U: ChosenPlaceMappingTest (5) — city/device/coordinates places round-trip through proto and serializer (property); unusable stored values (unknown source, out-of-range … | — |
| T-1503 | Backup/restore UI & privacy dashboard | DONE | 50d2730, e6451ed | U: BackupViewModelTest (7, Turbine) — encrypted export carries the passphrase once and wipes it; plain export sends none; unwritable destination; short passphrase never … | — |
| T-1504 | About, licenses, diagnostics & report | DONE | 803d63a, d9fb503 | P: DiagnosticsRedactorTest (5) — random Persian titles in quoted and key=value forms never survive; coordinates in Latin/Persian digits keep ≤ 2 decimals; tokens in … | — |
| T-1600 | Wear app | DONE | 4d9bcf4, 5d4cf77, 5f1d9db, 9a152e5 | U: WearTodayTest (5) — Nowruz 1405 fa date, holiday, other calendars, next prayer Dhuhr in Persian digits; after Isha → tomorrow's Fajr; property 2026: next prayer after … | emulator smoke and tile screenshots pending |
| T-1700 | TalkBack pass | PARTIAL | 9511cd2, f02a7ac, 7eaf900, abdf955, 131eb76, 9a152e5 | A11y: `AccessibilityAudit` (core/ui-testing; 11 tests with planted violations) runs on every screenshot state of core/ui, calendar, events, times, astronomy, compass, … | manual TalkBack device pass and sign-off pending |
| T-1701 | RTL & font scale | DONE | e55c0c0, 8aa8a63, 86b2592, 74a3b42, 0949d6c | L: `LayoutAudit` (core/ui-testing) runs in every captured screenshot — flags ellipsized, cut-off, over-limit or off-window text — enabled for core/ui and every feature … | manual pass pending |
| T-1702 | Translations | DONE | 1e3c51b, b579b37, 6e3a842, bc2e422, 577c1f3 | L: TranslationKonsistTest (7) — placeholders match the source (`%%` literal), CLDR plural categories per language, no copies of English outside … | optional human review of the 22 machine-translated languages; Weblate instance URL and per-language reviewers confirmed 2026-09-18 (main@b70b58f) |
| T-1800 | Baseline/startup profiles, R8, shrinking | DONE | 14d7fd0, 3718b74, f7b87f2 | build: `:app:verifyReleaseShrinking` — R8 mapping keeps back-stack and backup serializers, WorkManager worker name, proto field names (`shrinking-requirements.txt`) · … | device cold-start measurement (T-1801) |
| T-1801 | Macrobenchmarks | PARTIAL | 3718b74, e2d6c5c, 51fab98, 76d786f, 0439373, 2c8940d | B: StartupBenchmark (3), MonthPagerScrollBenchmark, ScreenScrollBenchmarks (timeline scroll, search typing), YearViewBenchmark (3, T-805, main@803842f) — compile only · … | not run on a device; no baselines |
| T-1802 | Compose stability | DONE | c5e943b | build: `composeStabilityCheck` per Compose module (reports via `-Ptaqvim.composeMetrics=true`, CI static job) fails on unstable composable parameters and … | — |
| T-1803 | Memory & leaks | DONE | 1418c78 | R: MemoryLeakTest (2) — MainActivity collected after onDestroy (verified failing without the fix), process-lifetime objects use the Application context · B: … | month heap budget not run: needs device |
| T-1804 | Security | DONE | 5d933cf, f75f1f5 | L: ExportedComponentsTest (4) — merged debug unit-test manifest exports exactly the allowlist (`security/exported-components.txt`, reason per entry), no implicit … | — |
| T-1900 | Release engineering | PARTIAL | 63c2a68, f8759ce | L: actionlint (release.yml, release-dry-run.yml) · changelog parser simulation over git log (101 features, 5 fixes, 4 tests, 8 documentation, 3 build & CI, 68 … | CI unrun until first push |
| T-1901 | Support system | BLOCKED | c3416e4, 30706f2, 2e3eb8a | L: issue forms parsed and structure-checked (types, unique ids, labels, dropdown options) | — |
| T-1902 | Beta program | BLOCKED | 63c2a68, 407520d | — | docs only |

### Data gaps (DT-xx)

Summary: 38 rows — 7 DONE, 7 PARTIAL, 24 BLOCKED. Every blocked row needs a primary source; each feature meanwhile works from computation or a documented fallback.

| ID | Needed data | Status | Blocks | Note |
|---|---|---|---|---|
| DT-001 | Official leap years 1499–1500 SH and Nowruz instants 1390–1403, 1406–1420 | PARTIAL | T-102 acceptance (remaining years), T-104 | Partly resolved 2026-09-13: official leap years 1206–1498 and Nowruz instants 1404–1405 are golden fixtures (docs/sources) |
| DT-002 | Official Iranian lunar month starts for the rest of PLAN D-07's range (1390–1410) | PARTIAL | T-104, D-07 | Partly resolved: Ramadan 1446 – Shawwal 1448 in `dataset/iran/islamic-iran-overrides.json` (D-07, main@6dfb3be) and the core table; 1390–1403 and … Since 2026-09-18 (ADR-0040) importing one more calendar is a single command, and the calibration agreement is reported per region in `docs/data-todo/islamic-calibration-report.md` (Iran 92.0 %, Saudi Arabia 99.5 %, Afghanistan 100 %) |
| DT-003 | Bikram Sambat month lengths | DONE | T-105, D-04 | Resolved by algorithm |
| DT-004 | Central Kurdish (`ckb`) "and" list pattern | BLOCKED | T-200 `andPattern` | Open |
| DT-005 | Persian (Solar Hijri) month names in `ckb`, `kmr`, `ne`, `id`, `ms`, `zh` | BLOCKED | T-200 `monthNames.persian` | Open |
| DT-006 | Islamic month names in `ckb`, `ne`, `zh` | BLOCKED | T-200 `monthNames.islamic` | Open |
| DT-007 | Bikram Sambat month names in all 24 languages (at minimum `ne` and `en`) | DONE | T-200 `monthNames.nepali`, T-105 | Resolved for ne; other languages use official Latin spellings |
| DT-008 | Era abbreviations — Persian calendar: ps, ckb, kmr, az, tr, ur, ne, hi, ta, bn, tg, de, es, id, ms, ja; Islamic … | BLOCKED | T-202 LONG dates (era omitted) | Open |
| DT-009 | Central Kurdish (ckb) relative-time, unit and list patterns; Tajik (tg) unit patterns | BLOCKED | T-202 relative phrases and durations return null | Open |
| DT-010 | Bikram Sambat date patterns and era name | PARTIAL | T-202 (NEPALI uses the Gregorian pattern), T-105 | Partly resolved (era; pattern pending) |
| DT-011 | Published prayer timetables for Kabul, Istanbul, Berlin and Sydney (12 months each) | BLOCKED | T-401 golden set (PLAN: six cities; Tehran and Mashhad plus 29 other Iranian cities are … | Open |
| DT-012 | Equinox and solstice instants 2020–2040 | DONE | T-403 seasons golden (only 2025/2026 official instants so far) | Resolved (USNO 1700–2100) |
| DT-013 | Published "Moon in Scorpio" (قمر در عقرب) periods for several years, with the zodiac convention used | BLOCKED | T-404 golden ("known dates") | Open |
| DT-014 | Published panchang tithi start/end times for several months and a stated location | BLOCKED | T-406 tithi golden | Open |
| DT-015 | Hijri-Persian (12-animal) year names and their alignment with Solar Hijri years | BLOCKED | T-406 year names | Open |
| DT-016 | Official Chinese lunar new-year dates | DONE | T-406 (dates before new year belong to the previous animal) | Resolved by algorithm (optional pre-1929 golden) |
| DT-017 | Published golden/blue hour times for fixed places and dates | BLOCKED | T-407 ±3 min golden | Open |
| DT-018 | Published natal-chart cusps (Placidus), ascendant, midheaven and Part of Fortune for stated instants and places | BLOCKED | T-405 golden | Open |
| DT-019 | Persian (`fa`) titles of 130 UN international days (list: `docs/data-todo/un-days-without-persian-title.tsv`) | PARTIAL | D-05 ≥ 150 entries (102 records so far) | Partly resolved 2026-09-18: 86 titles from the archived UNIC list, 9 from United Nations in Iran pages, 4 from archived UNIC Persian articles, 3 more … |
| DT-020 | Place names in prs, ps, ckb, kmr, az, ne, ta, tg, uz, ms | BLOCKED | T-603 city names (Dari shows Persian names, the others English) | Open |
| DT-021 | Iran country divisions (provinces, counties, districts) with names and coordinates | BLOCKED | T-603 districts, T-1502 district picker | Open |
| DT-022 | Real-world date-mention snippet corpus (fa/en) under a licence allowing reuse | BLOCKED | T-501 golden acceptance ("200 real-world snippets"; synthetic snippets used meanwhile) | Open |
| DT-023 | Current Persian title of World Telecommunication and Information Society Day (17 May) | PARTIAL | D-05 | Partly resolved |
| DT-024 | October occurrence of World Migratory Bird Day, and IMO's own wording of the World Maritime Day rule | PARTIAL | D-05 | Partly resolved |
| DT-025 | Full (LONG) date patterns for the Persian and Islamic calendars in `ps`, `ckb`, `ne` — CLDR 48 only has the root … | BLOCKED | T-202 LONG dates in those languages (kept as CLDR until sourced; fa/prs Persian … | Open |
| DT-026 | Names of the 88 IAU constellations in the 24 app languages | BLOCKED | T-1300 Moon constellation label (IAU abbreviation shown meanwhile) | Open |
| DT-027 | Animal-year compatibility rules (PLAN T-1400, optional) | BLOCKED | T-1400 distance "animal-year compatibility" | Open |
| DT-028 | A bundled athan recording with a licence allowing redistribution (optional; users can pick their own file meanwhile) | BLOCKED | T-1101/T-1102 default athan sound (the default alarm sound is used until then) | Open |
| DT-029 | Persian titles and modern-calendar dates of Mehragān, Sada, Čahāršanba-sūrī, Tīragān, Esfandegān/Sepandārmazgān, … | BLOCKED | D-06 (2 records so far) | Open |
| DT-030 | Schema support for the Zoroastrian 30-day-month convention flag (PLAN D-06) and for the eve of the last Wednesday of … | BLOCKED | D-06 records of those festivals | Open |
| DT-031 | Afghanistan official holiday dates not covered by a found announcement: Eid al-Fitr 1447 (the 2026-03-21 Bakhtar … | PARTIAL | D-03 completeness | Partly resolved 2026-09-14: 7 announced holidays of 1404–1405 from Bakhtar |
| DT-032 | Current Afghanistan Labour Law (Islamic Emirate) text, especially the public-holidays article, from the Official Gazette | BLOCKED | D-03 recurring national-day rules (currently one Single record per announced year) | Open |
| DT-033 | Afghanistan official Islamic month starts for 1447–1448 AH and later | BLOCKED | Placement of D-03 Islamic holidays independent of the user's variant (announcements show … | Open |
| DT-034 | Published crescent visibility maps for 5 dates, with the criterion used | BLOCKED | T-1301 golden "crescent classifier vs published crescent maps (5 dates)" | Open |
| DT-035 | Time-zone boundary polygons and tectonic plate boundaries under a licence compatible with ADR-0003 | DONE | T-1301 time-zone and tectonic-plate layers | Resolved |
| DT-036 | Odeh 2004 Table VI crescent observations (the paper numbers 737; 578 are printed) extracted and checked from the paper | DONE | T-1301 Odeh criterion golden test against observations (now tested against all 578 … | Resolved (578 published records) |
| DT-037 | Hebrew month names in ps, ckb, kmr, az, ne, hi, tg, uz, id, ms and zh | DONE | F07 Hebrew calendar names (main@381f6c0) — these languages fall back like other missing … | Resolved with machine translations (review optional) |
| DT-038 | Nepal holidays not expressed as rules: Gyalpo Lhosar (fits lunar 11/1 but follows the Tibetan calendar), Fagu Purnima … | BLOCKED | D-04 completeness | Open |

## 2. Test inventory

Counted from the source at main@41a6b04 (`@Test` methods per kind; property tests by `checkAll`/`forAll` call sites;
screenshot references by committed PNGs) and from the JUnit XML of the last full local run on that commit:
**3 970 JVM test cases in 486 result files, 0 failures, 0 skipped**, plus 12 instrumented cases on the Gradle Managed
Device `pixel6Api34`.

| Kind | Count | Where |
|---|---|---|
| Unit (JVM, JUnit 6 + Kotest) | 1 549 test methods | `*/src/test` without Android runners |
| Golden (fixture-backed, cited) | 148 test methods in 38 files | tests reading `golden/` resources (USNO, NASA, NOAA, official Iranian, Nepal MoHA/Panchang, Odeh/Yallop, ICU4J oracles, datasets) |
| Property (Kotest `checkAll`/`forAll`) | 162 property checks in 103 files | calendars, rules, numerals, recurrence, NLP, map, reminders, workdays |
| Robolectric (incl. Compose UI tests on the JVM) | 519 test methods | feature, data, wear and app modules |
| Screenshot (Roborazzi) | 379 reference images | `*/src/test/screenshots` |
| Instrumented UI (androidTest, Gradle Managed Device) | 8 test methods / 12 cases (fa + en) | `app/src/androidTest/.../device`: `DeviceSmokeTest` (6), `DeviceSurfacesTest` (3), `DeviceAccessibilityTest` (2), `DeviceReminderTest` (1) |
| Benchmark (Macrobenchmark / micro, instrumented) | 26 test methods (13 + 13) | `benchmark`, `benchmark/micro` |
| Timing (tagged, run apart) | 15 tests | `./gradlew timingTests` |
| Fuzz (in-suite, 1 000–10 000 random inputs) | 4 named fuzz tests plus fuzzed validators | dataset validator, ICS, NLP, duration grammar, deep links |
| Konsist architecture | 60 test methods | `konsist` |
| Lint detector | 3 test classes (17 detector cases) | `lint` |
| Build-logic unit | 42 test methods | `build-logic` |
| Python | 35 tests in 4 files | `tools/benchmark`, `tools/ci`, `tools/geodata` |
| Calendar completeness (per-year, 1380–1480 SH) | `CalendarCompletenessTest` (:data:events), `SkyAndTimesCompletenessTest` (:app) — 36 890 days each | proves no per-year manual data is needed (Phase 1, ADR-0026/0027/0028) |

### Coverage per module (Kover, main@41a6b04)

Gates: `:core:*` ≥ 95 % line / ≥ 90 % branch, merged ≥ 85 %. `koverVerify` is green on this commit. `wear`,
`feature/map`, `feature/compass` and `data/database` are below the merged average and carry the largest untested
branches (device sensors, map gestures, Room migrations); they are not gated individually.

| Module | Line % | Branch % |
|---|---|---|
| **merged (root, gate ≥ 85 %)** | **96.1** | **85.3** |
| `app` | 96.5 | 80.6 |
| `benchmark`, `benchmark/micro` (instrumented only) | — | — |
| `core/astronomy` | 100.0 | 96.8 |
| `core/calendar` | 99.3 | 93.6 |
| `core/events` | 98.1 | 94.7 |
| `core/i18n` | 99.5 | 90.8 |
| `core/ics` | 98.1 | 92.8 |
| `core/model` | 98.0 | 100.0 |
| `core/nlp` | 99.1 | 92.7 |
| `core/praytimes` | 99.4 | 91.1 |
| `core/testing` | 100.0 | 94.0 |
| `core/ui` | 95.7 | 95.1 |
| `core/ui-testing` | 99.0 | 92.6 |
| `core/workdays` | 100.0 | 97.6 |
| `data/database` | 97.7 | 63.3 |
| `data/device-calendar` | 98.1 | 95.2 |
| `data/events` | 95.3 | 86.7 |
| `data/location` | 95.7 | 88.2 |
| `data/preferences` | 98.8 | 93.5 |
| `data/scheduler` | 96.9 | 95.2 |
| `feature/about` | 97.2 | 79.5 |
| `feature/agenda` | 93.9 | 87.7 |
| `feature/astronomy` | 95.6 | 78.5 |
| `feature/backup` | 97.9 | 89.8 |
| `feature/calendar` | 96.9 | 88.0 |
| `feature/compass` | 86.8 | 76.4 |
| `feature/events` | 97.2 | 88.1 |
| `feature/map` | 78.5 | 64.0 |
| `feature/month` (UI only; screenshot- and Robolectric-tested, no Kover report) | — | — |
| `feature/notification` | 96.9 | 78.6 |
| `feature/search` | 97.6 | 92.6 |
| `feature/settings` | 97.9 | 89.3 |
| `feature/timeline` | 95.6 | 85.1 |
| `feature/times` | 97.7 | 86.2 |
| `feature/tools` | 96.5 | 90.8 |
| `feature/wallpaper` | 94.2 | 75.0 |
| `feature/widgets` | 96.3 | 85.1 |
| `feature/year` | 90.2 | 85.2 |
| `konsist` (rules, not product code) | — | — |
| `lint` | 99.5 | 80.2 |
| `tools/dataset` | 97.7 | 80.3 |
| `wear` | 71.0 | 48.0 |

### Commands

| Suite | Command |
|---|---|
| Format | `./gradlew spotlessCheck` |
| Static analysis | `./gradlew detekt` |
| Android Lint + custom rules | `./gradlew lint` |
| Architecture (Konsist) | `./gradlew :konsist:test --rerun` |
| Unit, golden, property, Robolectric | `./gradlew test` |
| Coverage gates | `./gradlew koverVerify` (reports: `koverXmlReport`, `koverLog`) |
| Timing tests (one fork each) | `./gradlew timingTests` |
| Screenshots | `./gradlew verifyRoborazziDebug` (record: `recordRoborazziDebug`) |
| Compose stability | `./gradlew composeStabilityCheck -Ptaqvim.composeMetrics=true` |
| Dataset | `./gradlew :tools:dataset:validate :tools:dataset:test` |
| License gate | `./gradlew licenseCheck` |
| App build, audit, size, shrinking | `./gradlew :app:assembleDebug :app:auditReleaseExportedComponents :app:checkReleaseApkSize :app:verifyReleaseShrinking` |
| Wear build | `./gradlew :wear:assembleDebug` |
| **Instrumented device tests (Gradle Managed Device `pixel6Api34`, API 34 aosp-atd; needs KVM, downloads the image on first run)** | `./gradlew :app:pixel6Api34DebugAndroidTest` — 12 cases, ~8 min |
| Baseline and startup profiles (same managed device) | `./gradlew :app:generateBaselineProfile` |
| Benchmarks (connected device) | `./gradlew :benchmark:connectedBenchmarkAndroidTest :benchmark:micro:connectedReleaseAndroidTest`, then `python3 tools/benchmark/compare_benchmarks.py --required benchmark/required.json …` (see `.github/workflows/benchmark.yml`) |
| Python tool tests | `python3 -m unittest discover -s tools/benchmark`, `python3 -m unittest discover -s tools/ci`, `python3 -m unittest discover -s tools/geodata` |
| USNO goldens up to date | `for g in tools/usno/*.py; do python3 "$g" --check; done` (8 generators) |
| Map layer assets up to date | `python3 tools/geodata/natural_earth_time_zones.py --check`, `python3 tools/geodata/matthews_plates.py --check` |

Local full-suite result on main@41a6b04 (2026-09-18, 18 min wall clock): static, konsist, lint, tests, kover, timing,
screenshots, app and wear builds, exported-component audit, APK size, Compose stability, dataset validate, Python tool
tests and all 8 USNO `--check` runs — every one exit 0.

## 3. Blocked and partial work

### (a) Work Claude can finish

Phase 3 emptied most of this list. What is left:

| Item | What remains | Plan |
|---|---|---|
| T-1900 / T-1901 / T-003 CI | Done: all four workflows green on main@dd8cd65, tagged `v1.0.0-rc2` (run URLs in §6) | Keep them green; the signed release build waits for the keystore secrets |
| T-1902 beta exit criteria | Documented, not exercised | Nothing more to automate; needs the beta itself (list (b)) |
| T-1600 Wear device checks | Done except the run itself (main@cf87a75): `wearApi34` managed device, 7 device test methods in 11 runs, and two tile screenshots recorded by Roborazzi from the real ProtoLayout renderer. The Wear system image cannot be fetched here — every `dl.google.com` path returns 404 — so the suite has never executed | Run `./gradlew :wear:wearApi34DebugAndroidTest` on a machine with network and `/dev/kvm`; watch-face tile/complication placement stays manual (`docs/MANUAL_TEST_CHECKLIST.md` §7) |
| T-1801 device measurements | Cold-start and jank numbers from a real phone | Macrobenchmarks run on a connected device only; the managed device gives a first figure but is not a release baseline |
| D-05 / D-06 records | 102 UN days of the planned ≥ 150; 2 historical records | Every further record needs a primary source with a Persian title (DT-019, DT-023, DT-024, DT-029) — see list (b) |
| DT-004–DT-010, DT-015, DT-020, DT-025, DT-026, DT-037 language data | Names and patterns missing in some languages | Machine-generated where the owner's MT policy allows (marked `MT: needs review`), otherwise the documented fallback; never empty, never crashing |

Everything else previously in this list is done: translations for all 24 languages (T-1702, 1 352 entries each),
Nepali feature strings, the Nepal holiday rules (D-04, 27 rules), the computed-data labels for DT-013/DT-014/DT-034,
the `docs/RELEASE.md` owner defaults, the `:core:nlp` fixture regeneration, baseline and startup profiles
(33 287 / 28 493 rules) and the 12 instrumented device tests.

### (b) Things only the owner can provide

Each line: what to provide → what happens if it is never provided. The owner only downloads or supplies files; no
engineering is expected.

- **Persian titles for the 130 UN international days that have none (DT-019)** → the list is in `docs/data-todo/un-days-without-persian-title.tsv`; a UN Information Centre Tehran publication or an iran.un.org page per day. Every reachable archive was already searched (937 UNIC articles, 576 Persian pages), giving 102 of the ≥ 150 records the plan asks for. Without them D-05 stays at 102 days and those observances are not shown in Persian.
- **Official Iran calendars for 1403 and 1406** (Calendar Center PDFs) → drop them into `docs/sources/`. Without them D-02 and T-303 stay golden-tested for 1404–1405 only; holidays for every year still come from the rules.
- **Newer official Iranian Hijri month tables (DT-002)** → optional: import through Settings → Official Hijri dates, or drop the Calendar Center calendar PDF into `docs/sources/` for a release. Without them the app shows computed months, labelled "computed" (23 of 25 recent months match the announcements).
- **Nepal government holiday gazette for the current and next BS year (D-04)** → drop the PDF into `docs/sources/nepal/`. Without it Nepal keeps the 27 computed rules checked against the 2082 and 2083 MoHA notices; the holidays that are not expressible as rules (DT-038: Gyalpo Lhosar, Fagu Purnima, regional and community days, Eid by sighting) stay out.
- **Current Afghanistan Labour Law, public-holidays article (DT-032)**, plus later Bakhtar announcements (DT-031, DT-033) → drop the Official Gazette scan into `docs/sources/afghanistan/`. Without it Afghanistan shows the six recurring holidays already sourced; Islamic dates are computed.
- **A published almanac listing Moon-in-Scorpio periods with its zodiac convention (DT-013)** → drop into `docs/sources/`. Without it the feature works from computation with no external golden.
- **A published panchang with tithi times and location (DT-014)** → same; tithi stays computed without an external golden.
- **A primary source for Hijri-Persian animal year names (DT-015)** → same; the names stay unshown.
- **A printed table of houses or published charts (DT-018)** → same; houses stay computed without an external golden.
- **Published golden/blue-hour tables (DT-017)** → same; the panel stays computed.
- **HM Nautical Almanac Office crescent visibility maps for 5 dates (DT-034)** → drop the PDFs into `docs/sources/crescent/`. Without them the crescent layer is validated only against the Yallop and Odeh observation records.
- **Hebrew month names for ps, ckb, kmr, az, ne, hi, tg, uz, id, ms, zh (DT-037)** — *optional human review* → approve or replace the machine-generated names now shipping in `hebrew-months-mt.properties`. Without review those languages keep the generated names.
- **Review of the machine-translated strings in 22 languages** — *optional human review* → through Weblate (`docs/i18n/TRANSLATING.md`). Without it those languages ship with strings marked "MT: needs review".
- **Ancient festival sources (DT-029), Persian place names and Iran divisions (DT-020, DT-021), a reusable real-world snippet corpus (DT-022), a redistributable athan recording (DT-028)** → drop the source files into `docs/sources/`. Without them those records stay out, place names fall back to Persian/English, and the default alarm sound is used.
- **Official prayer timetables for Kabul, Istanbul, Berlin, Sydney under reusable terms, or Diyanet's written permission (DT-011)** → drop into `docs/sources/`. Without them prayer times are validated against the 31 official Iranian city tables only.
- **Play Console account and app `ir.taqvim`** → create it. Without it nothing can be distributed through Play (GitHub releases still work).
- **Upload keystore and GitHub secrets** (`TAQVIM_KEYSTORE_BASE64`, `TAQVIM_KEYSTORE_PASSWORD`, `TAQVIM_KEY_ALIAS`, `TAQVIM_KEY_PASSWORD`, as named in `docs/RELEASE.md`) → add them in the repository settings. Without them the release workflow builds unsigned artifacts only.
- **Physical devices, including a Wear OS watch** → run the TalkBack pass (T-1700), the Wear tile and complication checks (T-1600, §7) and the OEM alarm matrix in `docs/MANUAL_TEST_CHECKLIST.md` (Samsung, Xiaomi, Huawei/Honor, Oppo/OnePlus/Realme, Pixel, Motorola, a low-end API 26 phone). Without them those checks stay unsigned and the beta exit criteria (T-1902) are not met.
- **Beta testers and a Google Group `taqvim-beta`** → create them. Without them the closed beta cannot start.
- **Weblate instance confirmation** (default `https://hosted.weblate.org/projects/taqvim/`) → confirm or replace. Without it translations stay file-based.
- **Support defaults — confirmed by the owner on 2026-09-18** (the `TODO(owner)` markers are removed, main@b70b58f; the list stays in `docs/MANUAL_TEST_CHECKLIST.md`, "Owner defaults confirmed on 2026-09-18"):
  - `SUPPORT.md` — Repository URL for the issue forms: `https://github.com/samansohani78/Taqvim/issues/new/choose`
  - `SUPPORT.md` — Support e-mail: `support@taqvim.app`
  - `SUPPORT.md` — Other languages answered: Persian and English only; other languages answered on a best-effort basis
  - `SUPPORT.md` — P1 target: Fix released within 7 days of confirmation
  - `SUPPORT.md` — P2 target: Fix in the next scheduled release (within 30 days)
  - `SUPPORT.md` — First-response time: 2 working days
  - `SUPPORT.md` — Severity label names: `P0`, `P1`, `P2` (plus `data-error`)
  - `SUPPORT.md` — Days to wait for missing information: 14 days
  - `docs/SECURITY.md` — Security contact: `support@taqvim.app` (subject "Security") or GitHub private vulnerability reporting
  - `docs/SECURITY.md` — Encryption key: None published; use GitHub private vulnerability reporting for confidential details
  - `docs/SECURITY.md` — First response: 3 working days
  - `docs/RELEASE.md` — Keystore backup holder and Play Console account: The project owner (Saman Sohani)
  - `docs/RELEASE.md` — Required reviewers for `release`: The project owner
  - `docs/RELEASE.md` — OEM device matrix: The matrix in this checklist, §6.4
  - `docs/RELEASE.md` — Play Console app, rollout percentages and hold times: `ir.taqvim`; 1 % → 5 % → 20 % → 50 % → 100 %, at least 24 h per step
  - `docs/RELEASE.md` — Store listing copy in `fa` and `en`: Not written yet; to be drafted from the README before the first upload
  - `docs/RELEASE.md` — Play service account: None; uploads stay manual
  - `docs/RELEASE.md` — Repository URL: `https://github.com/samansohani78/Taqvim`
  - `docs/BETA.md` — Closed-testing track and tester group: Play *Closed testing – Beta* track with a Google Group `taqvim-beta`
  - `docs/BETA.md` — Opt-in URL: Created by Play Console when the track is set up
  - `docs/BETA.md` — Tester list: At least 2 testers per listed language, recruited through the tester group
  - `docs/BETA.md` — Beta start date: The day the first beta build reaches testers
  - `docs/i18n/TRANSLATING.md` — Weblate instance URL: Hosted Weblate (`https://hosted.weblate.org/projects/taqvim/`)
  - `docs/i18n/TRANSLATING.md` — Reviewer per language: The project owner for `fa` and `en`; one volunteer reviewer per other language, recorded in the sign-off table
  All 29 markers are gone from the seven files that carried them. `support@taqvim.app` is now the recipient of the in-app problem report: `AboutIntents.report` builds an `ACTION_SENDTO mailto:` intent with the redacted body when an e-mail app is installed and falls back to the share sheet otherwise, always through a chooser (main@b70b58f).

## 4. Known deviations from the plan

| Deviation | ADR |
|---|---|
| Module map and toolchain choices (JDK 21 toolchain, Robolectric API 36, budgeted builds) | [ADR-0004](adr/0004-module-and-toolchain-deviations.md) |
| EPL-1.0 `junit:junit` allowed as test-only | [ADR-0005](adr/0005-test-scope-license-exceptions.md) |
| Umm al-Qura from an embedded table instead of runtime ICU4J, now computed from AH 1420 | [ADR-0006](adr/0006-umm-al-qura-table.md), [ADR-0028](adr/0028-umm-al-qura-computed.md) |
| Persian year starts: table replaced by a run-time equinox computation; official data is a golden only | [ADR-0008](adr/0008-persian-year-start-table.md), [ADR-0026](adr/0026-persian-year-starts-computed.md) |
| Iranian Hijri calendar: computed crescent months by default; official dates are an optional override (plan A-05 put the override table first) | [ADR-0009](adr/0009-iran-islamic-calendar.md), [ADR-0027](adr/0027-iran-islamic-months-computed.md), [ADR-0037](adr/0037-optional-islamic-override.md) |
| Islamic calendar per event source | [ADR-0010](adr/0010-islamic-calendar-per-event-source.md) |
| Recurrence and iCalendar export in non-Gregorian calendars | [ADR-0011](adr/0011-recurrence-in-non-gregorian-calendars.md), [ADR-0013](adr/0013-ics-export-of-non-gregorian-recurrence.md) |
| Persian long date pattern for fa/prs overrides CLDR | [ADR-0014](adr/0014-persian-long-date-pattern.md) |
| Standalone Wear OS app | [ADR-0019](adr/0019-standalone-wear-app.md) |
| No signed data-only update path; dataset fixes ship with app releases (plan T-1901 asked for one) | [ADR-0020](adr/0020-dataset-updates-via-app-releases.md) |
| Dynamic launcher icon through activity aliases | [ADR-0022](adr/0022-dynamic-launcher-icon.md) |
| 3D globe drawn on the Compose canvas instead of OpenGL ES / AGSL | [ADR-0024](adr/0024-canvas-orthographic-globe.md) |
| Scope added: Hebrew calendar, Easter and movable feasts, US DST, full USNO reference data | [ADR-0025](adr/0025-usno-reference-data-and-scope.md) |
| Prayer times on a high-precision Sun instead of the NOAA formulas; tithi on a modern ephemeris (plan A-15 named Surya Siddhanta) | [ADR-0029](adr/0029-prayer-times-precise-sun.md) |
| Bikram Sambat computed from Surya Siddhanta sankrantis instead of a 2000–2100 BS month-length table (plan A-07) | [ADR-0030](adr/0030-bikram-sambat-computed.md) |
| Event day assignment, restore journal, alarm delivery state, one-occurrence edits, shared occurrence pipeline (review follow-ups) | [ADR-0031](adr/0031-event-day-assignment.md), [ADR-0032](adr/0032-restore-journal.md), [ADR-0033](adr/0033-alarm-delivery-state.md), [ADR-0034](adr/0034-edit-one-occurrence.md), [ADR-0035](adr/0035-shared-occurrence-pipeline.md) |
| Every dataset event is a rule; `Single` records need an owner decision (plan §4.2 lists `Single`) | [ADR-0036](adr/0036-events-as-rules.md) |
| Release shrinking, profiles (generated on a Gradle Managed Device) and benchmark gates; timing tests run apart from `test` | [ADR-0018](adr/0018-release-shrinking-and-performance-checks.md) |
| Nepali lunar holidays expressed as a `LunarTithi` rule instead of a bundled Panchang table | [ADR-0038](adr/0038-nepali-lunar-tithi-rule.md) |
| CC BY 4.0 admitted for bundled **data** only (ADR-0003 allows it for code nowhere); a CC BY dependency still fails `licenseCheck` | [ADR-0039](adr/0039-cc-by-data-license.md) |
| Islamic month starts are calibrated against official calendars by a test-time report; the shipped criteria stay unless a candidate beats them | [ADR-0040](adr/0040-islamic-calibration-refit.md), [ADR-0041](adr/0041-islamic-calibration-1381-1405.md) |
| All calendar data computed at run time; official tables demoted to golden oracles or to the optional override; no per-year manual data (enforced by `NoPerYearManualDataTest`) | [ADR-0026](adr/0026-persian-year-starts-computed.md), [ADR-0027](adr/0027-iran-islamic-months-computed.md), [ADR-0028](adr/0028-umm-al-qura-computed.md), [ADR-0030](adr/0030-bikram-sambat-computed.md), [ADR-0037](adr/0037-optional-islamic-override.md), `docs/DATA_AUDIT.md` |
| Machine translation for 22 languages, marked for review (plan T-1702 said human-only; owner changed this on 2026-09-17) | recorded in Phase 3; workflow in `docs/i18n/TRANSLATING.md` |

## 5. Open risks and next release steps

**Risks**

1. **CI has never been green on GitHub.** The workflows first ran on 2026-09-16/17; eight fixes have landed (KVM on
   modern runners, per-tool SARIF upload, debug-only licence entries, runner-scaled timing budgets, a pinned
   `pixel_6` AVD profile, Glance test timeout, benchmark report upload). Until a full run passes, the release tag
   waits.
2. **JVM-only testing can miss Android-only failures.** One launch crash (an ICU-rejected regex, main@f7b87f2) passed
   every JVM test and was caught only on a device. 12 instrumented cases now run on `pixel6Api34`; that is a thin
   net over a 30-screen app.
3. **22 machine-translated languages are unreviewed.** Strings are marked `MT: needs review`; placeholders, plural
   categories and "no English left" are enforced by `TranslationKonsistTest`, but wording is not.
4. **Computed Iranian Hijri dates can differ from the announcement** by a day (2 of the 25 most recent months). The
   official override (ADR-0037) fixes this but is optional and off by default; the UI labels each date "computed" or
   "official override".
5. **Umm al-Qura** differs from the printed calendar in two marginal months (Jumada II 1427 and 1446) and from
   ICU-based platforms after AH 1450.
6. **APK size.** The release APK is 6 429 065 bytes against the 8 388 608-byte budget — 76.6 %, **1.87 MiB left**
   (main@d9dd4da cut 0.80 MiB; the gate now warns from 90 %). The owner's 1.5 MiB target was not reached: the other
   23 languages take 1.49 MiB and only AAB language splits remove them, which would break in-app language choice
   for users whose phone language is not one of the app's (ADR-0018 addendum). The owner can switch the split on.
7. **Wear and OEM coverage is unproven.** The Wear device suite exists and compiles (main@cf87a75) but has never
   executed — the Wear system image cannot be downloaded in this environment — and none of the seven OEM
   alarm/battery profiles in `docs/MANUAL_TEST_CHECKLIST.md` §6.4 has been exercised. Tile rendering is covered by
   two Roborazzi screenshots, which need no emulator.
8. **Map time-zone bands** come from 2012 Natural Earth geometry; 23 bands are marked "mixed" (I09).
9. **No crash reporting** by design, so the beta exit criterion (crash-free ≥ 99.9 %) depends on Play vitals.

**Next release steps**

1. `v1.0.0-rc2` is tagged and pushed on main@dd8cd65 (2026-09-18), the first commit on which all four workflows passed
   (run URLs in §6). `v1.0.0-rc1` (main@54f9299) was cut before that: its Release run failed on missing signing
   secrets and its PR and Instrumented runs were cancelled by a later push. The device-only budgets stay unverified.
2. Add the keystore secrets and build a signed release; without them the workflow produces unsigned artifacts.
3. Run `docs/MANUAL_TEST_CHECKLIST.md` on at least one phone, one tablet and one Wear device, including the TalkBack
   pass (T-1700) and the OEM matrix (T-1102).
4. Create the Play Console app and the closed-testing track, upload the RC, run the two-week beta (`docs/BETA.md`).
5. Collect the official 1406 Iranian calendar and the current Nepal gazette, then promote through the staged rollout in
   `docs/RELEASE.md`. (The `TODO(owner)` defaults were confirmed on 2026-09-18, main@b70b58f.)


### Fixed after the report was first written (2026-09-18)

Two bugs that only a real Android runtime could show, both found by running the app on an emulator:

- **The release build crashed at launch** (main@1a79898). R8 full mode repackages classes, so three relative
  `Class.getResourceAsStream` lookups — the i18n language tables, the city list and the official Iranian months —
  read nothing in any minified build. `ClasspathResourceKonsistTest` now fails the build on a relative lookup.
- **The app crashed at launch on every device** (main@f7b87f2). A regex the JVM accepts is rejected by Android's ICU;
  `AndroidRegexKonsistTest` guards it. Found while generating the baseline profile.

Both were invisible to the JVM and Robolectric suites, which is why the managed-device runs (main@9a152e5) and the
nightly benchmark job matter as gates.

### Official Iranian calendars 1381–1405 (2026-09-18, main@33d0418, main@7493b47, main@c309fd4, main@c92223b)

The owner supplied the Calendar Center's official calendar for every year 1381–1405. They are inventoried in
`docs/sources/iran/MANIFEST.md` (82 files: 25 yearly calendars, 31 city prayer timetables for 1405, the leap-year
table, the Nowruz-instant list, a holiday list, a year overview, 3 notices and 19 papers — the papers are cited in
PROVENANCE and kept out of Git). One command imports them: `tools/sources/iran/official_calendar_import.py`.

- **Imported: all 25 years, 9 131 days.** Four editions (1395, 1396, 1401, 1402) have a broken text layer that maps
  several digits to one character; their digits are read from the rendered glyphs instead, matched against glyphs
  whose value the document itself fixes (Solar Hijri day, Gregorian day and year). Each year had to read 100 % of
  its ~1 300 known digits before any Hijri digit was trusted. Two misprints (1381, 1383) are explicit errata.
- **Persian calendar: 0 mismatches.** Every day equals the computed calendar in date, weekday and Gregorian day. The
  leap-year table 1206–1498 matches, and the 44 Nowruz instants of 1360–1403 lie within **42.8 s** of the computed
  March equinox (limit ±1 min).
- **Official holidays: 0 rule errors.** Every year matches its official set exactly with the printed Hijri dates.
  Two holidays changed by law and now carry `validity` with the page cited: Imam Hasan Askari's martyrdom from
  AH 1440 (absent in `Calendar-1396.pdf`, present in `Calendar-1397.pdf` p. 13) and 2 Shawwal from AH 1433
  (`Calendar-1391.pdf` p. 8). With the app's computed lunar calendar, 40 holidays in 1382–1393 fall a day off because
  the month started a day off, and 10 in 1383–1385 follow later official announcements — all explained in
  `docs/data-todo/iran-holiday-history.md`.
- **Islamic calibration on 310 official month starts** (ADR-0041 and addendum). Fit on 1381–1400 (244 decisions),
  held out 1401–1405 (61):

| Criterion | Fit | Held out | Chained over 310 starts |
|---|---|---|---|
| **Yallop ≤ D (shipped)** | 91.4 % | 95.1 % (58/61) | 92.3 % |
| Yallop ≤ C | 89.3 % | 88.5 % | 88.7 % |
| Odeh ≤ B | 89.3 % | 86.9 % | 88.4 % |
| Odeh ≤ C | 82.0 % | 83.6 % | 82.6 % |
| Logistic (age, lag, ARCV, ARCL, W), fitted on 1381–1400 | 92.6 % | 96.7 % (59/61) | 93.5 % |
| Logistic, fitted on AH 1428 onward | 93.0 % | **100 % (61/61)** | — |

  The Calendar Center changed practice around AH 1428: before it, all misses are months it made longer than
  Yallop ≤ D; after it, all misses are crescents Yallop rates E or F that it accepted. The logistic model fitted on
  AH 1428 onward beats the shipped criterion by 3 held-out decisions, which meets ADR-0041's adoption bar. It is
  **not switched** (the owner's brief said not to); the addendum recommends adopting it through a new ADR, with the
  caveat that every held-out decision lies in the era it was fitted on. Other regions: Saudi Umm al-Qura 99.5 %
  (370/372), Afghanistan 100 % (5/5, two anchors loose to a week).

## 6. Ready for release: what I need from you

**Where the release stands.** Every plan task that can be finished without a physical device, a Play account or an
outside publication is done and tested: 103 of 115 task rows DONE, 3 PARTIAL, 9 BLOCKED on data or on accounts only.
The full local gate suite is green on main@41a6b04 — 3 970 JVM test cases and 12 instrumented cases, 0 failures,
96.1 % line / 85.3 % branch merged coverage, release APK 6.1 MiB of an 8 MiB budget (main@d9dd4da). All calendar and astronomy data
is computed for any year (proved for every day of 1380–1480 SH), so the app never needs a yearly data drop. The tag
`v1.0.0-rc2` marks main@dd8cd65, on which every GitHub workflow is green:

| Workflow | Commit | Result | Run |
|---|---|---|---|
| PR | dd8cd65 | success | https://github.com/samansohani78/Taqvim/actions/runs/35327888666 |
| Benchmarks | dd8cd65 | success | https://github.com/samansohani78/Taqvim/actions/runs/35327888908 |
| Instrumented tests (API 26, 30, 33, 36) | dd8cd65 | success | https://github.com/samansohani78/Taqvim/actions/runs/35327888637 |
| Release dry run | dd8cd65 | success | https://github.com/samansohani78/Taqvim/actions/runs/35327892318 |

Getting there took three CI fixes after rc1: release-candidate tags now build unsigned instead of failing on missing
secrets (main@c5bacfb), and the emulators cold-boot on every API level, because resuming the cached AVD snapshot left
the framework without system services — API 26 hung until the 90-minute timeout and API 30 failed in two minutes
(main@bcd23b4, main@dd8cd65).

**What only you can do, most valuable first.** Each line: what to provide, where it goes, and what happens if it never
arrives.

| # | What to provide | Where it goes | If it never arrives |
|---|---|---|---|
| 1 | Play Console account for `ir.taqvim`, plus an upload keystore and the four GitHub secrets named in `docs/RELEASE.md` | Play Console; repository → Settings → Secrets | No Play distribution and no signed build; GitHub releases of unsigned APKs still work |
| 2 | One Android phone, one tablet and one Wear watch for `docs/MANUAL_TEST_CHECKLIST.md` (TalkBack pass, OEM alarm matrix, Wear tiles) | you run the checklist and tick the boxes | T-1700, T-1102, T-1600 device checks and the T-1902 beta criteria stay unsigned |
| 3 | Beta testers and a `taqvim-beta` Google Group | Play closed testing (`docs/BETA.md`) | The closed beta cannot start, so the release stays a candidate |
| 4 | The 1406 official calendar when the Calendar Center publishes it, and a decision on adopting the AH 1428+ logistic crescent model (ADR-0041 addendum) | `docs/sources/iran/` (then `tools/sources/iran/official_calendar_import.py`); a yes/no | Computed dates continue past 1405 with the shipped Yallop ≤ D criterion, which matched 95.1 % of the held-out months |
| 5 | Nepal MoHA holiday gazette for the current and next BS year | `docs/sources/nepal/` | The 27 computed Nepal rules stand; sighting-based and community holidays (DT-038) stay out |
| 6 | Afghanistan Labour Law holidays article and later Bakhtar announcements | `docs/sources/afghanistan/` | Afghanistan shows the six sourced recurring holidays only |
| 7 | Persian titles for the 130 remaining UN days | list in `docs/data-todo/un-days-without-persian-title.tsv` | D-05 stays at 102 of ≥ 150 days |
| 8 | Optional: human review of the 22 machine-translated languages, and Hebrew month names in 11 of them | Weblate (`docs/i18n/TRANSLATING.md`) | Those languages ship marked `MT: needs review` |
| 9 | Optional goldens: crescent-visibility maps (DT-034), a panchang with tithi times (DT-014), a Moon-in-Scorpio almanac (DT-013), house tables (DT-018), golden/blue-hour tables (DT-017), reusable prayer timetables outside Iran (DT-011), a redistributable athan recording (DT-028) | `docs/sources/` | Those features keep working from computation, with no external cross-check and the stock alarm sound |

**What I do next, without asking:** confirm the last Benchmarks workflow run and keep
`docs/PROGRESS.md`, `docs/DATA_TODO.md` and this report current.
