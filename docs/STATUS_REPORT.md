# Taqvim — Status report

Generated 2026-09-17 from `docs/PLAN.md`, `docs/PROGRESS.md`, `docs/DATA_TODO.md`, `git log` and the build reports, at
main@cbfb1da. Status mapping: PROGRESS `DONE` → DONE (open validation listed); `WIP` → PARTIAL; waiting on the owner or
outside data → BLOCKED; `TODO` → NOT STARTED. DATA_TODO `Resolved` → DONE, `Partly resolved`/`In progress` →
PARTIAL, `Open` → BLOCKED.

## 1. Tasks

### Plan tasks (T-xxx, D-xx) and owner-approved additions (F01, F02, F03, F07; T-108–T-110 per ADR-0025)

Summary: 119 rows — 105 DONE, 6 PARTIAL, 7 BLOCKED, 1 NOT STARTED.

| ID | Title | Status | Commit hash(es) | Tests added | Open item |
|---|---|---|---|---|---|
| T-000 | Repository bootstrap | DONE | f34ecdb | R: MainActivityTest · U: AppModuleTest (Koin graph verify), TaqvimIssueRegistryTest · L: ProjectStructureKonsistTest; spotless/detekt/lint/Kover … | — |
| T-001 | License gate | DONE | d6c7ac3 | U: AllowListParserTest, SpdxNormalizerTest, PomLicensesTest (XXE, parent inheritance, cache lookup), LicensePolicyTest, ClassificationAndCodecTest · … | — |
| T-002 | Architecture rules (Konsist) | DONE | 3ea66ba | Konsist: ArchitectureKonsistTest (10 rules on real code), ArchitectureRulesTest (17 unit), KonsistFixturesTest (8 end-to-end, planted violations) | — |
| T-003 | CI workflows | DONE | 8587d6b, 42f7228 | L: actionlint (4 workflows + composite action) · local: CycloneDX SBOM (192 runtime components, no test deps), signed release APK verified by … | CI green on GitHub |
| T-004 | Custom lint module | DONE | c3fc147 | L: SourceRuleDetectorsTest (6), StateTextAndBackDetectorsTest (8), TaqvimIssueRegistryTest (3) — LintDetectorTest positive/negative per rule; … | — |
| T-005 | Test infrastructure (`:core:testing`) | DONE | e10a0a2 | U: GoldenFilesTest, SnapshotAndTimeFakesTest, ScreenshotMatrixTest · R: ScreenshotEnvironmentRobolectricTest · S: ui_testing_self_test (1 reference) … | — |
| T-100 | Value types | DONE | e94b47d | U: JdnTest (8), CalendarTypesTest (7) · P: jdn+n−n==jdn, weekday(jdn+7)==weekday(jdn), weekday+7, MinuteOfDay wrap | — |
| T-101 | Gregorian ↔ JDN (A-01) | DONE | c266f45 | G: GregorianIcuOracleTest (1 000 000 random JDNs in [−1e6, 5e6] vs ICU4J proleptic) · U/P: GregorianCalendarSystemTest (1582-10-15, year 0, negative … | — |
| T-102 | Persian calendar (A-02) | DONE | 9b1b7bf, dfe6f17, c6f13e6 | G: PersianCalendarOfficialTest — official leap years 1206–1498 (293), every day of the official 1404/1405 calendars (730, incl. weekday), official … | data partial |
| T-103 | Islamic tabular (A-03) & UAQ (A-04) | DONE | 97c9da4, 2fed071, ef282be, 9c33702 | G: TabularIslamicCalendarTest — type II vs ICU4J civil on 100 000 random days; UmmAlQuraCalendarTest — every month AH 1300–1600 and 100 000 random … | — |
| T-104 | Islamic Iran official (A-05) + observational (A-06) | DONE | 28a2d16, ac90cf8, 842b720, 27950c5 | G: IranIslamicCalendarTest — every lunar date of the official 1404/1405 calendars (730), 26 published month starts, table edges, 29/30-day months … | data partial |
| T-105 | Nepali (A-07) | DONE | d47a10a, 1a08566, 58994dc | G: NepaliCalendarSystemTest (10) — official National Panchang 2082/2083 month starts and sankranti times ±1 min, night rules, Kathmandu … | — |
| T-106 | Calendar utilities | DONE | 5cbcf02, 44f4346, bf69a9f | U: CalendarMathTest — 135 rows over Gregorian, tabular Islamic (incl. Dhul-Hijjah 29/30) and Umm al-Qura; PersianCalendarMathTest — 109 rows (29/30 … | — |
| T-107 | `kotlinx.datetime` bridge | DONE | ba40f51 | U: DateTimeBridgeTest — LocalDate↔Jdn, out-of-range rejection, Asia/Tehran day boundaries before/after DST abolition (2022), Asia/Kabul fixed offset, … | — |
| T-108 | Hebrew calendar (A-15, ADR-0025) | DONE | b20c5c6, 381f6c0, d15b2a4, 3170881 | G: UsnoJewishObservancesTest (5) — all 57 839 USNO Jewish observances 360–9999 exact (Julian before 1582-10-15), year lengths from consecutive new … | — |
| T-109 | Gregorian Easter and movable feasts (A-16, ADR-0025) | DONE | f9d2140, 330d795 | G: ChristianMovableFeastsTest — all 67 336 USNO Christian observances 1583–9999 exact; Meeus ch. 8 examples · P: Easter a Sunday 22 Mar–25 Apr and … | not shown in the app |
| T-110 | US daylight saving time (ADR-0025) | DONE | 7b336c2 | G: UsDaylightSavingRulesTest (5) — all 16 066 USNO begin/end dates 1967–9999 exact, none before 1967, March 8–14/November 1–7 Sundays for random … | — |
| T-200 | Language table | DONE | cdf1a66, 7e4e798 | U: LanguageTableTest (29: approved list, every language has every field ×24, exact data-gap set, numeral/direction rules, lookup & and-joining, … | — |
| T-201 | Numerals | DONE | b05c636 | U: NumeralsTest — `parse("۱۲٫۵") == 12.5`, separators and grouping per system (incl. Indian grouping), lenient mixed-digit parsing, traditional Tamil … | — |
| T-202 | Date & duration formatting | DONE | 05bf3e6 | U + oracle: PluralRulesTest (27), FormatTableTest (4), DateFormatterTest (47), RelativeTimeFormatterTest (48) — against ICU4J PluralRules (n 0–1000), … | — |
| T-203 | Persian text normalization & fuzzy match | DONE | 94d2947 | U: PersianTextTest — 50 normalization + 50 fuzzy pairs, search keys, OSA distance with early exit · P: normalize/searchKey idempotent, distance … | — |
| T-204 | String resources plan | DONE | 1a4d047 | L: MissingFarsiTranslationDetectorTest (4: untranslated string/plurals/array reported, missing values-fa folder, complete fa clean, other locales may … | — |
| T-300 | Rule engine | DONE | d7cf58e, d995b10 | U: RuleEvaluationTest — 108 cases, ≥ 10 per rule (Fixed incl. 29 Feb and 30 Esfand, NthWeekday, LastWeekday ± offset, LastDayOfMonth, Single, … | — |
| T-301 | Year cache & day lookup | DONE | 6db021b | U: EventLookupTest (6) — Persian/Islamic/Gregorian merge and order, disabled source vs ALWAYS_DISPLAYED, offset crossing into the next year, cache … | — |
| T-302 | Policies | DONE | 5c701af | U: EventVisibilityPolicyTest — 192-row truth table (source × holiday-only × hide-abroad × abroad × validity none/in/out × holiday × ALWAYS_DISPLAYED) … | — |
| T-303 | Holiday determination & workday basics | BLOCKED | 12b54fa | U: HolidayCalendarTest (3) — reasons, enabled sources only, weekend parameter and per-language CLDR weekends · G: OfficialHolidaysGoldenTest … | official Iranian calendars 1403 and 1406 (owner files) |
| T-304 | Search index | DONE | c635f90 | U: EventSearchIndexTest (9) — نوروز / نوريز / nowruz, ranking exact > prefix > substring > fuzzy, two-edit fuzzy, short queries, ties, filters, … | — |
| T-305 | Repository impl (`:data:events`) | DONE | c8b422e, 2ecb37a, 1413bc2, cbd40b6, 5f1d9db, 7a90a76, 9f23c0b | U: EventsRepositoryTest (6) — Nowruz 1405 holiday, weekend and official Hijri date; Turbine re-emission on preference change, none for identical … | — |
| D-01 | Dataset JSON Schema + validator CLI | DONE | ad81460 | U: DatasetValidatorTest — valid sample covering all 8 rule types, 30 invalid fixtures each reporting exactly one issue (all 6 issue kinds: malformed … | — |
| D-02 | Iran official holidays 1403–1406 | BLOCKED | 945d0c5 | G: IranOfficialHolidaysTest — the dataset's rules reproduce exactly the 26 official holiday dates of 1404 and of 1405 (golden date sets and the … | official Iranian calendars 1403 and 1406 (owner files), reviewer sign-off |
| D-03 | Afghanistan set | DONE | e031285, d995b10 | G: AfghanistanOfficialHolidaysTest (5) — validator clean; golden per Solar year (1404: 1, 1405: 6) equals the records' dates; dataset ids = golden … | weekday-dependent one-off days (DT-031); further announcements and labour-law holidays (DT-032) |
| D-04 | Nepal set | NOT STARTED | — | — | Nepal holiday set |
| D-05 | UN international days | PARTIAL | 2648a53, 5c281e0 | G: UnInternationalDaysTest (4) — validator clean, golden count 93 (89 Fixed, 4 NthWeekdayOfMonth), 14 rules spot-checked against the cited pages … | 93 of ≥ 150 days; Persian titles missing (DT-019, DT-023, DT-024) |
| D-06 | Ancient Iranian festivals | PARTIAL | 05d9fc1 | G: AncientIranianFestivalsTest (3) — validator clean; golden count 2 and rules (Fixed 1/6, Fixed 9/30) against the cited pages; every record PERSIAN, … | 2 records; Persian titles and dates (DT-029), schema flag (DT-030) |
| D-07 | Islamic Iran override table 1390–1410 | DONE | 6dfb3be, 27950c5 | G: IslamicIranOverridesGoldenTest (5) — validator clean; table equals `IranOfficialMonthStarts` month for month; every printed start is day 1 of its … | official months beyond Ramadan 1446 – Shawwal 1448 (DT-002) |
| D-08 | Dataset code generator (KotlinPoet) | DONE | 49d1ead, fe060cc | S: EventsCodeGeneratorTest — snapshot of the synthetic sample (all 8 rule types, validity, flags, aliases, links), determinism/id order/file … | — |
| D-09 | Dataset CI + CONTRIBUTING-DATA.md | DONE | 5ff3a12 | L: actionlint (5 workflows) · local: `:tools:dataset:validate` (1 file, 0 issues); the existing D-01 fixture 13-missing-citation proves a record … | CI green on GitHub |
| T-400 | Solar position (A-09) | DONE | e408019 | G: NoaaSolarCalculatorTest — all 240 rows of NOAA's Solar Calculations spreadsheet (declination, equation of time, solar noon, sunrise, sunset, … | — |
| T-401 | Prayer times (A-10) | DONE | e6b65cb, 7846949 | G: PrayerTimesOfficialTest — Institute of Geophysics official 1405 timetables, 31 Iranian cities × 365 days: Fajr, sunrise, Dhuhr, sunset, Maghrib … | — |
| T-402 | Qibla & great-circle (A-11) | DONE | ce4d495 | U/oracle: GreatCircleTest (13) — 10 sample cities, bearing ±1e-6° and distance ±1 m against an independent vector formulation; antipode, coincident … | — |
| T-403 | Astronomy façade (A-13) | DONE | 2facefb, 456d49a, e3f4938, d30c4b3, e710000, e322bf1, 0c9557e, 5dea390 | G: EclipsesTest — NASA GSFC catalogs 2024–2030, all 16 solar + 16 lunar eclipses (type, peak ±5 min), local total eclipse 2024-04-08 and partial … | — |
| T-404 | Zodiac & moon-in-Scorpio | BLOCKED | 5ccdd5f | U: ZodiacTest (5) — tropical sign boundaries (0°, 30°, 210°–240°, negative and multi-turn angles), IAU constellation of bright stars far from … | published Moon-in-Scorpio golden (DT-013) |
| T-405 | Houses, lots, ascendant (A-14) | BLOCKED | 7c1946f | U: HousesTest (4) — ascendant on the eastern horizon and midheaven culminating (property, 1950–2050, latitudes ±60°, all longitudes), Placidus cusps … | published chart golden (DT-018) |
| T-406 | Tithi, planetary hours, Chinese/animal year, year names | BLOCKED | a332cf4, 88e16b2, 9f23c0b | U: PlanetaryHoursTest (3) — Chaldean order and weekday rulers for every weekday, unequal day/night hours, polar Unavailable · U: TithiTest (3) — 30 … | panchang golden (DT-014), Hijri-Persian year names (DT-015) |
| T-407 | Photography panel calculations (F-10) | DONE | cdb2902 | U: PhotographyPanelTest (3) — morning/evening golden hour (apparent altitude −4°…6°) and blue hour (−6°…−4°) ordering and durations, … | reference check pending |
| T-500 | NLP date parser (F-03) | DONE | f0c0af1 | G: 500-phrase synthetic fa/en corpus (`golden/nlp/date-phrases.tsv`; expected days from `:core:calendar`, never from the parser) · P: NUMERIC and ISO … | LONG gaps |
| T-501 | Text date detector (F-04) | DONE | ecb16ea, 5d4cf77 | G: TextSnippetCorpusTest — 200 synthetic everyday fa/en snippets (176 with dates: single, weekday-prefixed, yearless, numeric, Islamic/Gregorian … | real-world corpus pending |
| T-502 | ICS reader/writer (F-05) | DONE | 31f5c52 | G: 30 synthetic fixture calendars (Google/Outlook-style exports: PRODID, VTIMEZONE, X- properties, Windows zone names, folding, escaping) · U: … | — |
| T-503 | Recurrence engine | DONE | 4464dbb, 697cc1a, e7cb1da, 7a90a76 | G: RecurrenceOracleTest — 5 RFC 5545 examples + 50 Gregorian cases against an independent day-by-day java.time oracle (52) · U: RecurrenceEngineTest … | — |
| T-504 | Workday engine (F-07) | DONE | d19884b | U: WorkdayCalculatorTest — 100 cases (50 addWorkdays, 50 workdaysBetween) against an independent java.time loop, next/previous/zero semantics, … | — |
| T-600 | Proto DataStore | DONE | 83f9182 | U: UserPreferencesMappingTest — defaults for all 24 languages, proto ↔ model mapping, table-driven migrations · R: UserPreferencesRepositoryTest — … | — |
| T-601 | Room schema & DAOs | DONE | c2e5b80, 6928708 | R: PersonalDaoTest, WorkAndCacheDaoTest — in-memory DAO CRUD with Turbine flows · R: TaqvimDatabaseMigrationTest — MigrationTestHelper validates … | — |
| T-602 | Device calendar adapter | DONE | ca9a397, 5f1d9db | U: DeviceEventMappingTest (6) — all-day event keeps its date in UTC+3:30, UTC−8, Asia/Tehran, America/Los_Angeles; timed events in the device zone; … | — |
| T-603 | Location data | DONE | c2e1528 | U: CityTableParserTest (3) — any column order, empty localized column = English spelling, prs→fa fallback, bad rows rejected with line number · U: … | districts pending |
| T-604 | Scheduler | DONE | 4fbcf1e, 762c432 | U: AlarmPoliciesTest (5) — skip if fired > 15 min late (exactly 15 min is on time), dedup, reconcile, restore drops late alarms and duplicates · U: … | — |
| T-605 | Backup/restore (F-11) | DONE | d594bea, c6cdb01, d1f2863 | U: BackupCryptoTest (4) — PBKDF2-HMAC-SHA256 and SP 800-108 KDF against independently computed vectors (ASCII and UTF-8 passphrases), AES-GCM rejects … | — |
| T-700 | Theme | DONE | 99eac40 | U: SchemeContrastTest (2) — WCAG 2.2 contrast of 71 text/background role pairs ≥ 4.5:1 (standard) and ≥ 7:1 (high contrast) for random, default and … | — |
| T-701 | Components | DONE | 22d40c7 | U: ComponentGeometryTest (11) — wheel index/progress arithmetic, date-selection day clamping (29/30 Esfand), picker and month-grid model validation, … | — |
| T-702 | Painters for widgets | DONE | 909ed8b | P: PainterGeometryTest (7) — month cells tile every row without gaps and mirror exactly in RTL for any size/columns/rows (property), week column, … | — |
| T-703 | Motion & shared elements | DONE | 9b8d98b, a559999 | U: TaqvimMotionTest (4) — durations follow the animator scale (0/negative/NaN = no motion), reduced motion yields no transition, RTL mirroring and … | — |
| T-800 | CalendarViewModel & use cases | DONE | 720443c | U: CalendarViewModelTest (12, Turbine) — load today in every calendar with month and events; select day → month and events; month paging keeps the … | — |
| T-801 | Month pager | DONE | 41c189d, 5d4cf77 | UI: CalendarScreenTest (5, Robolectric) — swipe 12 months forward and 24 back shows each month's title; tap selects a day; long press opens the … | benchmark not run |
| T-802 | Day details tabs | DONE | 55ea31e | U: DayDetailsCalculatorTest (8) — Nowruz 1405: 3 days from 27 Esfand, day 1 of week/year, spring day 1 of 93, Sun in Aries; negative distances; … | — |
| T-803 | Toolbar & menu | DONE | a84ee78 | U: CalendarMenuTest (5, Turbine) — menu and dialogs; go to date selects the clamped primary-calendar day; week numbers and secondary calendar stored … | — |
| T-806 | Adaptive layout (F-13) | DONE | a84ee78 | U: CalendarLayoutTest (4) — stacked below 600 dp, two panes from 600 dp, tabletop at any width; picker range and month names · UI: … | tablet emulator run pending |
| T-804 | Search screen (F-12) | DONE | de0aacf, 2ecb37a, 1413bc2, 6be22a5, a4243f3 | UI: SearchScreenTest (4, Robolectric) — typing "نور" through the route shows Nowruz and opens it (1 Farvardin 1405); grouped headings, go-to-date, … | — |
| T-805 | Year view | DONE | 6a47034, 803842f, 5d4cf77 | U: YearViewModelTest (7, Turbine) — today's year in the first calendar with day flags; next/previous/chosen/today years (limit 3000); calendar switch … | macrobenchmark not run |
| T-900 | Timeline layout | DONE | 912a2b6, a4243f3 | U/P: IntervalColoringTest (5) — 33-case table (disjoint, touching, overlapping, nested, chained, day edges) + overlaps never share a column, cluster … | — |
| T-901 | Month list & agenda | DONE | 551aa61 | UI: AgendaScreenTest (6, Robolectric) — scroll 100 rows forward and back, both list ends request more months; mode, today, print, share, event and … | — |
| T-1000 | Events editor | DONE | 34cc9a9, 5d4cf77, a72f00c, e9b99ec, a9d2d73, f31a07c, 60cca26, 01ea742, a4243f3 | U: EventValidatorTest (8) — blank titles (property), title/notes/link limits, end before start (property), interval/count in Latin/Persian/Eastern … | — |
| T-1001 | Reminder notifications | DONE | a6b2edf, 762c432, 37ac1b2, a4243f3, 7a90a76 | P: ReminderPlannerTest — random events in Persian, Gregorian and Islamic calendars (all-day/timed; Tehran, Berlin, Los Angeles; … | — |
| T-1002 | Official-event reminders | DONE | a6b2edf, d604af6 | U: OfficialRemindersTest (3) — next Nowruz across the Persian year boundary (late Esfand 1404 → 1 Farvardin 1405 = 2026-03-21; after it → 1406); … | — |
| T-1003 | ICS import/export & subscriptions | DONE | 2a4a56b, 7d38d7a, 2ecb37a, c9b0223, 9a3ff36 | R: IcsRoomTest (4) — fixture import into Room (3 created, repeated UID skipped, reader problem reported), duplicates skipped or replaced by UID, … | — |
| T-1100 | Times tab & report | DONE | ea91bd3 | U: MonthlyReportTest (5) — one row per day for every month of Persian 1400–1410, Gregorian 2024–2026 and Umm al-Qura 1445–1447; HTML with … | — |
| T-1101 | Athan settings | DONE | df5f30f, 39f5e17 | U: AthanPreferencesMappingTest (5) — defaults identical for all 24 languages (every athan off); random settings round-trip through proto and … | — |
| T-1102 | Athan playback | DONE | c5c1492, 762c432 | P: AthanPlannerTest (5) — for random instants in 2026, gaps −60…60 and any set of enabled prayers, athans = A-10 times + gaps, strictly after now, … | OEM checklist not written |
| T-1103 | Automation broadcasts (F-14) | DONE | a2c4179, 609b23f | U/P: DeepLinksTest (5) — every documented link → destination, unreadable links → calendar, fuzz 1 000 arbitrary strings never throw, every Persian … | — |
| T-1200 | Widget framework | DONE | 163eed2 | U: WidgetUpdatePolicyTest (6) — each trigger reaches only dependent installed widgets (property); next day at local midnight in Tehran, São Paulo … | — |
| T-1201 | Widget: 1×1 date | DONE | b976f33, 1515e02, 0439373 | G: DayWidgetsGlanceTest — day number and weekday, tap opens today · U: WidgetContentBuilderTest — fa/en titles, weekdays and digits · U: … | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1202 | Widget: 4×1 date + clock | DONE | b976f33, 1515e02, 0439373 | G: full date and secondary date, no events | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1203 | Widget: 2×2 date + events + next prayer | DONE | b976f33, 1515e02, 0439373 | G: 2 events at MEDIUM and 4 at LARGE; holiday opens its day, personal event opens `taqvim://event/<id>`; next prayer opens times; no-events and … | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1204 | Widget: 4×2 with prayer strip | DONE | b976f33, 1515e02, 0439373 | G: six times with the next one bold; one-row widget keeps only the strip; no-place text opens times | screenshots, device tap tests and < 30 ms render benchmark pending |
| T-1205 | Widget: Month interactive | DONE | b0355f7, a438f8c, 5d4cf77 | G: previous/next/today steps kept per widget id; days open `taqvim://day`; holiday and weekend tones; secondary day at 4×4; RTL-mirrored controls; + … | screenshots and device tap tests pending |
| T-1206 | Widget: Month bitmap | DONE | b0355f7, a438f8c | G: T-702 month painter at the widget's pixel size; holidays, events and secondary days follow the widget's settings · B: best-of-5 render time under … | screenshots and < 30 ms device render benchmark pending |
| T-1207 | Widget: Week strip | DONE | b0355f7, a438f8c | G: seven days from the user's week start with weekday initial, tones and event dot; each day opens its day | screenshots and device tap tests pending |
| T-1208 | Widget: Schedule list | DONE | b0355f7, a438f8c | G: today and event days of the next 14 days as a LazyColumn; headers open the day, personal events the editor; empty message | screenshots and device scroll tests pending |
| T-1209 | Widget: Sun arc | DONE | b0355f7, a438f8c | G: T-702 sun-arc painter with daylight progress and the next prayer; without a place asks for one and opens times · U: Tehran sunrise/sunset, polar … | screenshots pending |
| T-1210 | Widget: Moon | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71 | G: SkyWidgetsGlanceTest — sizes, Persian digits, links · U: WidgetSkyBuilderTest (100 widget tests) | screenshots pending |
| T-1211 | Widget: Map | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71 | G: SkyWidgetsGlanceTest · U: WidgetSkyAdaptersTest (6) — outline loaded once and retried, 72×36 day/night grid, sun position, projection · U: … | screenshots pending |
| T-1212 | Widget: Countdown/age (F-09) | DONE | 08fa677, 2aaa59b, 711e0bc, 5128b71, 5d4cf77 | P: WidgetCountdownMathTest — Persian, Gregorian, tabular Islamic; 30 Esfand / 29 February move to the month's last day in common years · U: … | screenshots pending |
| T-1213 | Persistent notification | DONE | bf20f3c, f080650, be04534 | notification module 71 tests (29 new): content (date, weekday, next prayer, other calendars, holidays, prayer strip), setting on/off, re-post only on … | device checks pending |
| T-1214 | Dynamic launcher icon | DONE | bf20f3c, f080650, a4e7d4b | R: LauncherIconTest — exactly one entry enabled, new enabled before old disabled, nothing written while off, DONT_KILL_APP · allowlist + release … | device checks pending |
| T-1215 | QS tile, shortcuts, live wallpaper, daydream | DONE | be04534, 4db72a8, a4e7d4b | R: TodayTileTest (onDestroy skipped: Robolectric shadow bug); shortcut intents parse through DeepLinkParser; wallpaper redraw scheduling (14 … | device checks pending |
| T-1300 | Astronomy screen | DONE | e4c1222, 5d4cf77 | U: AstronomyHeaderCacheTest (4) — Moon and signs once per hour, seasons and eclipses once per UTC day, separate places, LRU eviction; next season, … | — |
| T-1301 | Map | DONE | ce9356a, 074ab4a, 50b537b, e216e41, 6abd6b9, ece6731, c8ee198, 4a5c127, a8041a3, cbd40b6 | U/P: MapGeometryTest (6) — projection round trip (property), longitude wrap, screen↔map round trip with zoom 1–8 and pan inside the map (property), … | DT-034 crescent-map comparison; globe frame time on a device |
| T-1302 | Compass | DONE | 8a40fcf | U: AnglesTest (5) — wrap and signed-turn properties, filter across north, circular mean, 15° announcements with hysteresis (property) · U: … | — |
| T-1303 | Bubble level | DONE | 8a40fcf | U: LevelMathTest (5) — classification (scale-invariant property), flat/portrait/landscape tilt (property, face-down/upside-down), calibration per … | — |
| T-1400 | Converter, distance, duration, time zones, QR | DONE | 6179966, 9945456, 5f1d9db | U: DurationExpressionTest (3) — 106-expression table: 78 values incl. Persian units/digits, decimals, nesting, ±100 000 days; 28 errors with … | — |
| T-1500 | Settings screens | DONE | 33060a6, 3fc4d09, a9d2d73, 9a3ff36 | U: AppSettingsMappingTest (5) — defaults identical for all 24 languages (week numbers off, ancient Iranian festivals off); random settings round-trip … | — |
| T-1501 | Language & first-run | DONE | c4273e2, fe9096e, f749b4a | U: LanguageSwitchTest (5) — 24×24 defaults; changed values kept (property); chosen values survive round trips; unknown → en; first run per device … | — |
| T-1502 | Location settings | DONE | 4d7cae6, 5980f0f, a9d2d73 | U: ChosenPlaceMappingTest (5) — city/device/coordinates places round-trip through proto and serializer (property); unusable stored values (unknown … | — |
| T-1503 | Backup/restore UI & privacy dashboard | DONE | 50d2730, e6451ed | U: BackupViewModelTest (7, Turbine) — encrypted export carries the passphrase once and wipes it; plain export sends none; unwritable destination; … | — |
| T-1504 | About, licenses, diagnostics & report | DONE | 803d63a, d9fb503 | P: DiagnosticsRedactorTest (5) — random Persian titles in quoted and key=value forms never survive; coordinates in Latin/Persian digits keep ≤ 2 … | — |
| T-1600 | Wear app | DONE | 4d9bcf4, 5d4cf77, 5f1d9db | U: WearTodayTest (5) — Nowruz 1405 fa date, holiday, other calendars, next prayer Dhuhr in Persian digits; after Isha → tomorrow's Fajr; property … | emulator smoke and tile screenshots pending |
| T-1700 | TalkBack pass | PARTIAL | 9511cd2, f02a7ac, 7eaf900, abdf955, 131eb76 | A11y: `AccessibilityAudit` (core/ui-testing; 11 tests with planted violations) runs on every screenshot state of core/ui, calendar, events, times, … | automated audit gaps (map, app shell, widgets, Wear, notification); manual TalkBack sign-off on a device |
| T-1701 | RTL & font scale | DONE | e55c0c0, 8aa8a63, 86b2592, 74a3b42, 0949d6c | L: `LayoutAudit` (core/ui-testing) runs in every captured screenshot — flags ellipsized, cut-off, over-limit or off-window text — enabled for core/ui … | manual pass pending |
| T-1702 | Translations | PARTIAL | 1e3c51b | L: TranslationKonsistTest (7) — placeholders match the source (`%%` literal), CLDR plural categories per language, no copies of English outside … | 22 languages untranslated (machine translation planned, Phase 3) |
| T-1800 | Baseline/startup profiles, R8, shrinking | DONE | 14d7fd0, 3718b74, f7b87f2 | build: `:app:verifyReleaseShrinking` — R8 mapping keeps back-stack and backup serializers, WorkManager worker name, proto field names … | device cold-start measurement (T-1801) |
| T-1801 | Macrobenchmarks | PARTIAL | 3718b74, e2d6c5c, 51fab98, 76d786f, 0439373, 2c8940d | B: StartupBenchmark (3), MonthPagerScrollBenchmark, ScreenScrollBenchmarks (timeline scroll, search typing), YearViewBenchmark (3, T-805, … | benchmarks not run on a device; no baselines |
| T-1802 | Compose stability | DONE | c5e943b | build: `composeStabilityCheck` per Compose module (reports via `-Ptaqvim.composeMetrics=true`, CI static job) fails on unstable composable parameters … | — |
| T-1803 | Memory & leaks | DONE | 1418c78 | R: MemoryLeakTest (2) — MainActivity collected after onDestroy (verified failing without the fix), process-lifetime objects use the Application … | month heap budget not run: needs device |
| T-1804 | Security | DONE | 5d933cf, f75f1f5 | L: ExportedComponentsTest (4) — merged debug unit-test manifest exports exactly the allowlist (`security/exported-components.txt`, reason per entry), … | — |
| T-1900 | Release engineering | PARTIAL | 63c2a68, f8759ce | L: actionlint (release.yml, release-dry-run.yml) · changelog parser simulation over git log (101 features, 5 fixes, 4 tests, 8 documentation, 3 build … | CI green on GitHub; signing secrets and Play upload |
| T-1901 | Support system | BLOCKED | c3416e4, 30706f2, 2e3eb8a | L: issue forms parsed and structure-checked (types, unique ids, labels, dropdown options) | owner confirms the flagged support defaults |
| T-1902 | Beta program | BLOCKED | 63c2a68, 407520d | — | Play Console account, testers, running the beta |
| F01 | Edit or cancel one occurrence (review idea, owner-approved, ADR-0034) | DONE | 60cca26, 01ea742 | OccurrenceEditTest (5), store and expansion tests, 4 screenshots | — |
| F02 | Recurrence preview in the editor (review idea, owner-approved) | DONE | f31a07c | RecurrencePreviewTest | — |
| F03 | Subscription health page (review idea, owner-approved) | DONE | 9a3ff36 | SubscriptionHealthTest (4), SubscriptionHealthScreenTest (3), SubscriptionHealthAdapterTest (3), SubscriptionHealthRecordTest (3), migration 6→7, 5 screenshots | — |
| F07 | Hebrew calendar in the converter and pickers (review idea, owner-approved) | DONE | 381f6c0, d15b2a4, 3170881 | HebrewMonthNamesTest (5), HebrewCalendarSystemTest (3), CalendarMonthNamesTest (3), 2 screenshots | Hebrew month names in 11 languages (DT-037) |

### Data gaps (DT-xx)

Summary: 37 rows — 6 DONE, 5 PARTIAL, 26 BLOCKED (each needs a primary source; the feature works from computation or a
documented fallback meanwhile).

| ID | Title | Status | Commit hash(es) | Tests added | Open item |
|---|---|---|---|---|---|
| DT-001 | Official leap years 1499–1500 SH and Nowruz instants 1390–1403, 1406–1420 | PARTIAL | — | — | T-102 acceptance (remaining years), T-104; Partly resolved 2026-09-13: official leap years 1206–1498 and Nowruz instants 1404–1405 are golden fixtures (docs/sources) |
| DT-002 | Official Iranian lunar month starts for the rest of PLAN D-07's range (1390–1410) | PARTIAL | — | — | T-104, D-07; Partly resolved: Ramadan 1446 – Shawwal 1448 in `dataset/iran/islamic-iran-overrides.json` (D-07, main@6dfb3be) and the core table; 1390–1403 and 1406–1410 still need official calendars |
| DT-003 | Bikram Sambat month lengths | DONE | — | — | T-105, D-04; Resolved by algorithm |
| DT-004 | Central Kurdish (`ckb`) "and" list pattern | BLOCKED | — | — | T-200 `andPattern`; primary source needed |
| DT-005 | Persian (Solar Hijri) month names in `ckb`, `kmr`, `ne`, `id`, `ms`, `zh` | BLOCKED | — | — | T-200 `monthNames.persian`; primary source needed |
| DT-006 | Islamic month names in `ckb`, `ne`, `zh` | BLOCKED | — | — | T-200 `monthNames.islamic`; primary source needed |
| DT-007 | Bikram Sambat month names in all 24 languages (at minimum `ne` and `en`) | DONE | — | — | T-200 `monthNames.nepali`, T-105; Resolved for ne; other languages use official Latin spellings |
| DT-008 | Era abbreviations — Persian calendar: ps, ckb, kmr, az, tr, ur, ne, hi, ta, bn, tg, de, es, id, ms, ja; … | BLOCKED | — | — | T-202 LONG dates (era omitted); primary source needed |
| DT-009 | Central Kurdish (ckb) relative-time, unit and list patterns; Tajik (tg) unit patterns | BLOCKED | — | — | T-202 relative phrases and durations return null; primary source needed |
| DT-010 | Bikram Sambat date patterns and era name | PARTIAL | — | — | T-202 (NEPALI uses the Gregorian pattern), T-105; Partly resolved (era; pattern pending) |
| DT-011 | Published prayer timetables for Kabul, Istanbul, Berlin and Sydney (12 months each) | BLOCKED | — | — | T-401 golden set (PLAN: six cities; Tehran and Mashhad plus 29 other Iranian cities are …; primary source needed |
| DT-012 | Equinox and solstice instants 2020–2040 | DONE | — | — | T-403 seasons golden (only 2025/2026 official instants so far); Resolved (USNO 1700–2100) |
| DT-013 | Published "Moon in Scorpio" (قمر در عقرب) periods for several years, with the zodiac convention used | BLOCKED | — | — | T-404 golden ("known dates"); primary source needed |
| DT-014 | Published panchang tithi start/end times for several months and a stated location | BLOCKED | — | — | T-406 tithi golden; primary source needed |
| DT-015 | Hijri-Persian (12-animal) year names and their alignment with Solar Hijri years | BLOCKED | — | — | T-406 year names; primary source needed |
| DT-016 | Official Chinese lunar new-year dates | DONE | — | — | T-406 (dates before new year belong to the previous animal); Resolved by algorithm (optional pre-1929 golden) |
| DT-017 | Published golden/blue hour times for fixed places and dates | BLOCKED | — | — | T-407 ±3 min golden; primary source needed |
| DT-018 | Published natal-chart cusps (Placidus), ascendant, midheaven and Part of Fortune for stated instants and … | BLOCKED | — | — | T-405 golden; primary source needed |
| DT-019 | Persian (`fa`) titles of 141 UN international days (list: `docs/data-todo/un-days-without-persian-title.tsv`) | PARTIAL | — | — | D-05 ≥ 150 entries (93 records so far); Partly resolved 2026-09-13: 86 titles from the archived UNIC list, 7 from United Nations in Iran pages |
| DT-020 | Place names in prs, ps, ckb, kmr, az, ne, ta, tg, uz, ms | BLOCKED | — | — | T-603 city names (Dari shows Persian names, the others English); primary source needed |
| DT-021 | Iran country divisions (provinces, counties, districts) with names and coordinates | BLOCKED | — | — | T-603 districts, T-1502 district picker; primary source needed |
| DT-022 | Real-world date-mention snippet corpus (fa/en) under a licence allowing reuse | BLOCKED | — | — | T-501 golden acceptance ("200 real-world snippets"; synthetic snippets used meanwhile); primary source needed |
| DT-023 | Persian title of World Telecommunication and Information Society Day (17 May) | BLOCKED | — | — | D-05; primary source needed |
| DT-024 | Official rules and Persian titles of World Maritime Day and World Migratory Bird Day | BLOCKED | — | — | D-05; primary source needed |
| DT-025 | Full (LONG) date patterns for the Persian and Islamic calendars in `ps`, `ckb`, `ne` — CLDR 48 only has the … | BLOCKED | — | — | T-202 LONG dates in those languages (kept as CLDR until sourced; fa/prs Persian …; primary source needed |
| DT-026 | Names of the 88 IAU constellations in the 24 app languages | BLOCKED | — | — | T-1300 Moon constellation label (IAU abbreviation shown meanwhile); primary source needed |
| DT-027 | Animal-year compatibility rules (PLAN T-1400, optional) | BLOCKED | — | — | T-1400 distance "animal-year compatibility"; primary source needed |
| DT-028 | A bundled athan recording with a licence allowing redistribution (optional; users can pick their own file … | BLOCKED | — | — | T-1101/T-1102 default athan sound (the default alarm sound is used until then); primary source needed |
| DT-029 | Persian titles and modern-calendar dates of Mehragān, Sada, Čahāršanba-sūrī, Tīragān, … | BLOCKED | — | — | D-06 (2 records so far); primary source needed |
| DT-030 | Schema support for the Zoroastrian 30-day-month convention flag (PLAN D-06) and for the eve of the last … | BLOCKED | — | — | D-06 records of those festivals; primary source needed |
| DT-031 | Afghanistan official holiday dates not covered by a found announcement: Eid al-Fitr 1447 (the 2026-03-21 … | PARTIAL | — | — | D-03 completeness; Partly resolved 2026-09-14: 7 announced holidays of 1404–1405 from Bakhtar |
| DT-032 | Current Afghanistan Labour Law (Islamic Emirate) text, especially the public-holidays article, from the … | BLOCKED | — | — | D-03 recurring national-day rules (currently one Single record per announced year); primary source needed |
| DT-033 | Afghanistan official Islamic month starts for 1447–1448 AH and later | BLOCKED | — | — | Placement of D-03 Islamic holidays independent of the user's variant (announcements show …; primary source needed |
| DT-034 | Published crescent visibility maps for 5 dates, with the criterion used | BLOCKED | — | — | T-1301 golden "crescent classifier vs published crescent maps (5 dates)"; primary source needed |
| DT-035 | Time-zone boundary polygons and tectonic plate boundaries under a licence compatible with ADR-0003 | DONE | — | — | T-1301 time-zone and tectonic-plate layers |
| DT-036 | Odeh 2004 Table VI crescent observations (the paper numbers 737; 578 are printed) extracted and checked from … | DONE | — | — | T-1301 Odeh criterion golden test against observations (now tested against all 578 …; Resolved (578 published records) |
| DT-037 | Hebrew month names in ps, ckb, kmr, az, ne, hi, tg, uz, id, ms and zh | BLOCKED | — | — | F07 Hebrew calendar names (main@381f6c0) — these languages fall back like other missing …; primary source needed |

## 2. Test inventory

Counted from the source at main@cbfb1da (`@Test` methods per kind; property tests by `checkAll`/`forAll` calls; screenshot
references by committed PNGs) and from the JUnit XML results of the last local run (3 699 JVM test cases in 41 modules,
0 failures before the regression noted in 3(a)).

| Kind | Count | Where |
|---|---|---|
| Unit (JVM, JUnit 6 + Kotest) | 1 347 test methods | `*/src/test` without Android runners |
| Golden (fixture-backed, cited) | 139 test methods | tests reading `golden/` resources (USNO, NASA, NOAA, official Iranian, Nepal, Odeh/Yallop, datasets) |
| Property (Kotest `checkAll`/`forAll`) | 160 property checks | calendars, rules, numerals, recurrence, NLP, map, reminders |
| Robolectric (incl. Compose UI tests on the JVM) | 509 test methods | feature, data and app modules |
| Screenshot (Roborazzi) | 379 reference images, captured by screenshot test classes in each UI module | `*/src/test/screenshots` |
| Instrumented UI (androidTest) | none committed yet on main | managed-device smoke tests in progress (Phase 3) |
| Benchmark (Macrobenchmark / micro, instrumented) | 26 test methods | `benchmark`, `benchmark/micro` |
| Timing (tagged, run apart) | 13 tests | `timingTests` |
| Fuzz (in-suite, 1 000–10 000 random inputs) | 4 named fuzz tests plus fuzzed validators | dataset validator, ICS, NLP, duration grammar, deep links |
| Konsist architecture | 63 test methods | `konsist` |
| Lint detector | 3 test classes (17 detector cases) | `lint` |
| Build-logic unit | 39 test methods | `build-logic` |
| Python | 35 tests | `tools/benchmark`, `tools/ci`, `tools/geodata` |

### Coverage per module (Kover, main@ae12f2f; cbfb1da since then only regenerates the licence list)

`:core:nlp` is missing because its tests fail on main (3(a)); the root merged report was not produced for the same
reason. Gates: `:core:*` ≥ 95 % line / ≥ 90 % branch, merged ≥ 85 %.

| Module | Line % | Branch % |
|---|---|---|
| `app` | 96.5 | 80.6 |
| `benchmark` | — | — |
| `benchmark/micro` | — | — |
| `core/astronomy` | 100.0 | 96.8 |
| `core/calendar` | 99.3 | 93.4 |
| `core/events` | 98.1 | 94.1 |
| `core/i18n` | 99.5 | 90.8 |
| `core/ics` | 98.1 | 92.8 |
| `core/model` | 98.0 | 100.0 |
| `core/praytimes` | 99.4 | 90.5 |
| `core/testing` | 100.0 | 94.0 |
| `core/ui-testing` | 99.0 | 92.6 |
| `core/ui` | 95.7 | 95.1 |
| `core/workdays` | 100.0 | 97.6 |
| `data/database` | 97.7 | 63.3 |
| `data/device-calendar` | 98.1 | 95.2 |
| `data/events` | 95.3 | 86.8 |
| `data/location` | 95.7 | 88.2 |
| `data/preferences` | 98.8 | 93.5 |
| `data/scheduler` | 96.9 | 95.2 |
| `feature/about` | 97.2 | 79.5 |
| `feature/agenda` | 93.9 | 87.7 |
| `feature/astronomy` | 95.6 | 78.5 |
| `feature/backup` | 97.9 | 89.8 |
| `feature/calendar` | 96.9 | 88.2 |
| `feature/compass` | 86.8 | 76.4 |
| `feature/events` | 97.2 | 88.1 |
| `feature/map` | 78.5 | 64.0 |
| `feature/month` | — | — |
| `feature/notification` | 96.9 | 78.6 |
| `feature/search` | 97.6 | 92.6 |
| `feature/settings` | 97.9 | 89.3 |
| `feature/timeline` | 95.6 | 85.1 |
| `feature/times` | 97.7 | 86.2 |
| `feature/tools` | 96.5 | 90.8 |
| `feature/wallpaper` | 94.2 | 75.0 |
| `feature/widgets` | 96.4 | 85.3 |
| `feature/year` | 90.2 | 85.2 |
| `konsist` | — | — |
| `lint` | 99.5 | 80.2 |
| `tools/dataset` | 97.4 | 79.5 |
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
| Baseline and startup profiles (Gradle Managed Device `pixel6Api34`, needs KVM) | `./gradlew :app:generateBaselineProfile` |
| Benchmarks (connected device) | `./gradlew :benchmark:connectedBenchmarkAndroidTest :benchmark:micro:connectedReleaseAndroidTest`, then `python3 tools/benchmark/compare_benchmarks.py --required benchmark/required.json …` (see `.github/workflows/benchmark.yml`) |
| Python tool tests | `python3 -m unittest discover -s tools/benchmark`, `python3 -m unittest discover -s tools/ci`, `python3 -m unittest discover -s tools/geodata` |
| USNO goldens up to date | `for g in tools/usno/*.py; do python3 "$g" --check; done` |
| Map layer assets up to date | `python3 tools/geodata/natural_earth_time_zones.py --check`, `python3 tools/geodata/matthews_plates.py --check` |

## 3. Blocked and partial work

### (a) Work Claude can finish (Phase 3, in progress)

| Item | What remains | Plan |
|---|---|---|
| T-1702 translations | 22 of 24 languages have no strings | Machine-translate every module's strings, mark each file `<!-- MT: needs review -->`, keep lint/Konsist green, add a Weblate config; fa and en stay at 100 % |
| Nepali feature-screen strings | Feature modules have no `values-ne` | Same machine-translation approach, marked for review |
| DT-013, DT-014, DT-034 | No published golden | Features already compute these (Moon in Scorpio, tithi, crescent visibility); label the values "source: computed" and keep the missing publication in list (b) |
| DT-015, DT-037, DT-005/006/008/009/025/026, DT-020 | Names or patterns missing in some languages | Machine-generate where the owner's MT policy allows (marked), otherwise keep the documented fallback; never empty or crashing |
| DT-002, DT-031–DT-033 | Official announcements beyond the covered years | Computed calendars already fill every date (labelled "computed"); the optional override (ADR-0037) takes newer official files |
| D-04 Nepal holiday set | Not started | Compile recurring rules from the reachable official Panchang / Home Ministry publications; otherwise list the missing file in (b) |
| D-05, D-06 | Fewer records than the plan | Add records only where a primary source gives the Persian title; the rest stays in (b) |
| T-1700, T-1801, T-1803, T-1600, widgets | Device-only checks | Run on the Gradle Managed Device (`pixel6Api34`, KVM works on the dev machine: baseline profiles were generated there, main@f7b87f2); record the rest in docs/MANUAL_TEST_CHECKLIST.md |
| `docs/RELEASE.md` owner defaults | Commit f7b87f2 overwrote the flagged defaults that 2e3eb8a filled; 8 `‹OWNER: …›` markers are back | Re-apply the defaults with `TODO(owner)` comments |
| `:core:nlp:test` fails on main | 27950c5 (computed Islamic default) moved some Hijri dates by a day; `TextSnippetCorpusTest` fixtures were not regenerated (e.g. `en-text-077`, "16 Jumada I 1447" now 2460988) | Regenerate the fixtures with `-Ptaqvim.updateSnapshots=true`, check the diff, re-run `:core:nlp:test` |
| T-003, D-09, T-1900 | "Green on GitHub" | CI fixes are being pushed (main@874506f, 42f7228, d8e96ab, 2c8940d); finish until every workflow passes, then tag `v1.0.0-rc1` |

### (b) Things only the owner can provide

Each line: what to provide → what happens if it is never provided.

- **Official Iran calendars for 1403 and 1406** (Calendar Center PDFs) → drop them into `docs/sources/`. Without them D-02 and T-303 stay golden-tested for 1404–1405 only; holidays for every year still come from the rules.
- **Newer official Iranian Hijri month tables (DT-002)** → optional: import through Settings → Official Hijri dates, or drop the Calendar Center calendar PDF into `docs/sources/` for a release. Without them the app shows computed months, labelled "computed" (23 of 25 recent months match the announcements).
- **Nepal government holiday gazette for the current and next BS year (D-04)** → drop the PDF into `docs/sources/nepal/`. Without it the Nepal holiday source stays empty; the Bikram Sambat calendar itself is computed and complete.
- **Current Afghanistan Labour Law, public-holidays article (DT-032)**, plus later Bakhtar announcements (DT-031, DT-033) → drop the Official Gazette scan into `docs/sources/afghanistan/`. Without it Afghanistan shows the six recurring holidays already sourced; Islamic dates are computed.
- **A published almanac listing Moon-in-Scorpio periods with its zodiac convention (DT-013)** → drop into `docs/sources/`. Without it the feature works from computation with no external golden.
- **A published panchang with tithi times and location (DT-014)** → same; tithi stays computed without an external golden.
- **A primary source for Hijri-Persian animal year names (DT-015)** → same; the names stay unshown.
- **A printed table of houses or published charts (DT-018)** → same; houses stay computed without an external golden.
- **Published golden/blue-hour tables (DT-017)** → same; the panel stays computed.
- **HM Nautical Almanac Office crescent visibility maps for 5 dates (DT-034)** → drop the PDFs into `docs/sources/crescent/`. Without them the crescent layer is validated only against the Yallop and Odeh observation records.
- **Hebrew month names for ps, ckb, kmr, az, ne, hi, tg, uz, id, ms, zh (DT-037)** — *optional human review* → provide or approve names. Without them those languages show machine-generated or numbered months.
- **Review of the machine-translated strings in 22 languages** — *optional human review* → through Weblate (`docs/i18n/TRANSLATING.md`). Without it those languages ship with strings marked "MT: needs review".
- **Persian titles of the remaining UN days (DT-019, DT-023, DT-024), ancient festival sources (DT-029), Persian place names and Iran divisions (DT-020, DT-021), a reusable real-world snippet corpus (DT-022), a redistributable athan recording (DT-028)** → drop the source files into `docs/sources/`. Without them those records stay out, place names fall back to Persian/English, and the default alarm sound is used.
- **Official prayer timetables for Kabul, Istanbul, Berlin, Sydney under reusable terms, or Diyanet's written permission (DT-011)** → drop into `docs/sources/`. Without them prayer times are validated against the 31 official Iranian city tables only.
- **Play Console account and app `ir.taqvim`** → create it. Without it nothing can be distributed through Play (GitHub releases still work).
- **Upload keystore and GitHub secrets** (`TAQVIM_KEYSTORE_BASE64`, `TAQVIM_KEYSTORE_PASSWORD`, `TAQVIM_KEY_ALIAS`, `TAQVIM_KEY_PASSWORD`, as named in `docs/RELEASE.md`) → add them in the repository settings. Without them the release workflow builds unsigned artifacts only.
- **Physical devices** → run the TalkBack pass and the OEM alarm matrix in `docs/MANUAL_TEST_CHECKLIST.md` (Samsung, Xiaomi, Huawei/Honor, Oppo/OnePlus/Realme, Pixel, Motorola, a low-end API 26 phone). Without them those checks stay unsigned and the beta exit criteria (T-1902) are not met.
- **Beta testers and a Google Group `taqvim-beta`** → create them. Without them the closed beta cannot start.
- **Weblate instance confirmation** (default `https://hosted.weblate.org/projects/taqvim/`) → confirm or replace. Without it translations stay file-based.
- **Confirm the flagged support defaults** (every `TODO(owner)` comment; list in `docs/MANUAL_TEST_CHECKLIST.md`, "Owner defaults to confirm"):
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
  Without confirmation the defaults stay in the documents; `support@taqvim.app` is not wired into the app (the in-app report opens the share sheet until an address is set).

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
| Machine translation for 22 languages, marked for review (plan T-1702 said human-only; owner changed this on 2026-09-17) | recorded in Phase 3 |

## 5. Open risks and next release steps

**Risks**

1. **JVM-only testing missed a launch crash.** An Android-only regex failure (main@f7b87f2) passed every JVM test. Until the managed-device suite runs in CI, device-only failures can slip through.
2. **CI is not yet green on GitHub.** The workflows only started running on 2026-09-16/17; several fixes landed and more may be needed.
3. **Computed Iranian Hijri dates can differ from announcements.** They differ by a day in some months (2 of 25 recent months). The optional official override exists, but users must switch it on.
4. **Machine-translated strings** in 22 languages can be wrong until reviewed.
5. **Umm al-Qura** differs from the printed calendar in two marginal months, and from ICU-based platforms after AH 1450.
6. **Map time-zone bands** come from 2012 geometry; 23 bands are marked "mixed" (I09).
7. **No crash reporting** by design, so the beta exit criterion (crash-free ≥ 99.9 %) relies on Android vitals.
8. **Release APK** is 5.67 MB with profiles (budget 8 MB); the widget and map assets are the largest growth.

**Recommended next steps**

1. Finish Phase 3: machine translations, data-gap labels, managed-device test runs, and a green CI on GitHub.
2. Run `docs/MANUAL_TEST_CHECKLIST.md` on at least one physical phone, one tablet and one Wear device.
3. Tag `v1.0.0-rc1`, build a signed release once the keystore secrets exist, and upload to the Play closed-testing track.
4. Run the two-week closed beta (`docs/BETA.md`); confirm the `TODO(owner)` defaults; collect official 1406 calendar data.
5. Promote through the staged rollout in `docs/RELEASE.md`.
