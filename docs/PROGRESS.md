# Taqvim — Progress Tracker

Source of truth: [`docs/PLAN.md`](PLAN.md). Deviations: [`docs/adr/`](adr/).
Status legend: `TODO` · `WIP` · `DONE` · `BLOCKED` (see Notes).

| Task | Title | Status | Branch/Commit | Tests added | Notes |
|---|---|---|---|---|---|
| T-000 | Repository bootstrap | DONE | main@f34ecdb | R: MainActivityTest · U: AppModuleTest (Koin graph verify), TaqvimIssueRegistryTest · L: ProjectStructureKonsistTest; spotless/detekt/lint/Kover gates green | ADR-0001–0004. Tests & detekt on JDK 21 toolchain; build resource budget after local OOM crashes (ADR-0004 §9) |
| T-001 | License gate | DONE | main@d6c7ac3 | U: AllowListParserTest, SpdxNormalizerTest, PomLicensesTest (XXE, parent inheritance, cache lookup), LicensePolicyTest, ClassificationAndCodecTest · Negative: LGPL canary (`-Ptaqvim.licenseGate.canary=true`) rejected | 427 modules, 0 violations. ADR-0005: jakarta.annotation-api substitution, trove4j excluded, **EPL-1.0 test-only exception pending owner confirmation**; kxml2 MIT override. Google Maven unreachable from dev network → POMs read cache-first |
| T-002 | Architecture rules (Konsist) | DONE | main@3ea66ba | Konsist: ArchitectureKonsistTest (10 rules on real code), ArchitectureRulesTest (17 unit), KonsistFixturesTest (8 end-to-end, planted violations) | package↔module, layering (imports + build scripts), Android-free pure core, files ≤400 / functions ≤50 lines, ViewModel/UiState contract, no global mutable state. `!!`/`try`/`as` → detekt + T-004 lint; complexity ≤12 → detekt. Rule 10 (main@0b13c78): expression-bodied `@Test` must declare `: Unit` |
| T-003 | CI workflows | DONE (local) | main@8587d6b | L: actionlint (4 workflows + composite action) · local: CycloneDX SBOM (192 runtime components, no test deps), signed release APK verified by apksigner (v2), full gate green | pr.yml (static+SARIF, unit+Kover, Roborazzi verify, license gate + LGPL canary negative test, dataset, assemble, actionlint), instrumented.yml (API 26/30/33/36, KVM), benchmark.yml (nightly), release.yml (signed AAB/APK, git-cliff changelog, SBOM). **"Green on main" pending first push to GitHub** |
| T-004 | Custom lint module | DONE | main@c3fc147 | L: SourceRuleDetectorsTest (6), StateTextAndBackDetectorsTest (8), TaqvimIssueRegistryTest (3) — LintDetectorTest positive/negative per rule; Konsist: string-literal unit test | NoDoubleBang, NoTryCatch, UseRunCatching (quick fix), NoUnsafeCast, NoHardcodedNonLatinText, HardcodedComposeText (added: quality bar forbids hard-coded UI text), NoGlobalMutableState, PreferPredictiveBack (Kotlin + manifest); active in every module, full gate green |
| T-005 | Test infrastructure (`:core:testing`) | DONE | main@e10a0a2 | U: GoldenFilesTest, SnapshotAndTimeFakesTest, ScreenshotMatrixTest · R: ScreenshotEnvironmentRobolectricTest · S: ui_testing_self_test (1 reference) · Konsist: FixtureProvenanceKonsistTest | Golden fixtures with provenance header + repo audit, snapshots, FakeClock/FakeTimeZone, 54-environment screenshot matrix (phone/tablet/fold at mdpi), `:core:ui-testing` (ADR-0004 §2). Composables excluded from Kover (ADR-0004 §10) |
| T-100 | Value types | DONE | main@e94b47d | U: JdnTest (8), CalendarTypesTest (7) · P: jdn+n−n==jdn, weekday(jdn+7)==weekday(jdn), weekday+7, MinuteOfDay wrap | Overflow-checked Jdn + JdnRange, CalendarDate, ISO Weekday, validated Coordinates, integer MinuteOfDay; explicit API + KDoc 100%. **Correction (main@0b13c78):** its property tests were silently skipped by JUnit (expression-bodied `@Test` returning a value) until that fix; they now run and a Konsist rule prevents recurrence |
| T-101 | Gregorian ↔ JDN (A-01) | DONE | main@c266f45 | G: GregorianIcuOracleTest (1 000 000 random JDNs in [−1e6, 5e6] vs ICU4J proleptic) · U/P: GregorianCalendarSystemTest (1582-10-15, year 0, negative years, round-trip, successor) · U: PropertyTestingTest | Fliegel & Van Flandern + 400-year cycle shifting (exact for all Int years); CalendarArithmetic contract. **Correction (main@0b13c78):** its property tests were silently skipped by JUnit (expression-bodied `@Test` returning a value) until that fix; they now run and a Konsist rule prevents recurrence |
| T-102 | Persian calendar (A-02) | BLOCKED | | | Needs official leap years 1300–1500 and Nowruz instants 1390–1420 from the University of Tehran Calendar Center; calendar.ut.ac.ir answers non-browser clients with a JavaScript bot challenge, so the primary source cannot be retrieved. Owner input requested. Astronomy engine (cosinekitty 2.1.19, MIT) `Astronomy.seasons(year)` verified. |
| T-103 | Islamic tabular (A-03) & UAQ (A-04) | DONE | main@97c9da4 | G: TabularIslamicCalendarTest — type II vs ICU4J civil on 100 000 random days; UmmAlQuraCalendarTest — every month AH 1300–1600 and 100 000 random days (AH 1200–1700) vs ICU4J, table boundaries · P: round-trip for both tabular variants and UAQ across/beyond the table · U: epoch, leap cycles, month alternation, 10 631-day cycle, type I/II one-day difference, invalid input (15 tests) | ADR-0006: UAQ month masks embedded (obtained via ICU4J public API, Unicode-3.0 notice in `licenses/`); ICU4J remains test-only; civil tabular fallback outside AH 1300–1600 |
| T-104 | Islamic Iran official (A-05) + observational (A-06) | TODO | | | |
| T-105 | Nepali (A-07) | TODO | | | |
| T-106 | Calendar utilities | WIP | main@5cbcf02 | U: CalendarMathTest table — 135 rows over Gregorian, tabular Islamic and Umm al-Qura · P: `addMonths(k)` then `−k` returns the month start; y/m/d period components rebuild the target date | Calendar-agnostic over `CalendarArithmetic`. Remaining for acceptance (≥ 200 rows): Persian rows (29/30 Esfand) after T-102, Nepali 32-day months after T-105 |
| T-107 | `kotlinx.datetime` bridge | DONE | main@ba40f51 | U: DateTimeBridgeTest — LocalDate↔Jdn, out-of-range rejection, Asia/Tehran day boundaries before/after DST abolition (2022), Asia/Kabul fixed offset, Europe/Berlin & America/Los_Angeles DST, TodayProvider follows clock and zone · P: agrees with A-01 and round-trips; a zone shifts the civil day by ≤ 1 (8 tests) | `api(kotlinx-datetime 0.8.0)`; uses `kotlin.time.Clock`/`Instant` |
| T-200 | Language table | TODO | | | |
| T-201 | Numerals | DONE | main@b05c636 | U: NumeralsTest — `parse("۱۲٫۵") == 12.5`, separators and grouping per system (incl. Indian grouping), lenient mixed-digit parsing, traditional Tamil (18 rows + malformed input) · P: Long and BigDecimal round-trip through every system × grouping, traditional Tamil round-trip (35 tests) | Latin, Persian, Eastern Arabic, Devanagari, Tamil; CLDR separators; Tamil ௰/௱/௲ signs |
| T-202 | Date & duration formatting | TODO | | | |
| T-203 | Persian text normalization & fuzzy match | DONE | main@94d2947 | U: PersianTextTest — 50 normalization + 50 fuzzy pairs, search keys, OSA distance with early exit · P: normalize/searchKey idempotent, distance symmetric and zero iff equal (104 tests) | Restricted Damerau–Levenshtein (OSA) ≤ 2 on search keys; hamza carriers folded in search keys only |
| T-204 | String resources plan | TODO | | | |
| T-300 | Rule engine | TODO | | | |
| T-301 | Year cache & day lookup | TODO | | | |
| T-302 | Policies | TODO | | | |
| T-303 | Holiday determination & workday basics | TODO | | | |
| T-304 | Search index | TODO | | | |
| T-305 | Repository impl (`:data:events`) | TODO | | | |
| D-01 | Dataset JSON Schema + validator CLI | TODO | | | |
| D-02 | Iran official holidays 1403–1406 | TODO | | | |
| D-03 | Afghanistan set | TODO | | | |
| D-04 | Nepal set | TODO | | | |
| D-05 | UN international days | TODO | | | |
| D-06 | Ancient Iranian festivals | TODO | | | |
| D-07 | Islamic Iran override table 1390–1410 | TODO | | | |
| D-08 | Dataset code generator (KotlinPoet) | TODO | | | |
| D-09 | Dataset CI + CONTRIBUTING-DATA.md | TODO | | | |
| T-400 | Solar position (A-09) | TODO | | | |
| T-401 | Prayer times (A-10) | TODO | | | |
| T-402 | Qibla & great-circle (A-11) | TODO | | | |
| T-403 | Astronomy façade (A-13) | TODO | | | |
| T-404 | Zodiac & moon-in-Scorpio | TODO | | | |
| T-405 | Houses, lots, ascendant (A-14) | TODO | | | |
| T-406 | Tithi, planetary hours, Chinese/animal year, year names | TODO | | | |
| T-407 | Photography panel calculations (F-10) | TODO | | | |
| T-500 | NLP date parser (F-03) | TODO | | | |
| T-501 | Text date detector (F-04) | TODO | | | |
| T-502 | ICS reader/writer (F-05) | TODO | | | |
| T-503 | Recurrence engine | TODO | | | |
| T-504 | Workday engine (F-07) | TODO | | | |
| T-600 | Proto DataStore | TODO | | | |
| T-601 | Room schema & DAOs | TODO | | | |
| T-602 | Device calendar adapter | TODO | | | |
| T-603 | Location data | TODO | | | |
| T-604 | Scheduler | TODO | | | |
| T-605 | Backup/restore (F-11) | TODO | | | |
| T-700 | Theme | TODO | | | |
| T-701 | Components | TODO | | | |
| T-702 | Painters for widgets | TODO | | | |
| T-703 | Motion & shared elements | TODO | | | |
| T-800 | CalendarViewModel & use cases | TODO | | | |
| T-801 | Month pager | TODO | | | |
| T-802 | Day details tabs | TODO | | | |
| T-803 | Toolbar & menu | TODO | | | |
| T-804 | Search screen (F-12) | TODO | | | |
| T-805 | Year view | TODO | | | |
| T-806 | Adaptive layout (F-13) | TODO | | | |
| T-900 | Timeline layout | TODO | | | |
| T-901 | Month list & agenda | TODO | | | |
| T-1000 | Events editor | TODO | | | |
| T-1001 | Reminder notifications | TODO | | | |
| T-1002 | Official-event reminders | TODO | | | |
| T-1003 | ICS import/export & subscriptions | TODO | | | |
| T-1100 | Times tab & report | TODO | | | |
| T-1101 | Athan settings | TODO | | | |
| T-1102 | Athan playback | TODO | | | |
| T-1103 | Automation broadcasts (F-14) | TODO | | | |
| T-1200 | Widget framework | TODO | | | |
| T-1201 | Widget: 1×1 date | TODO | | | |
| T-1202 | Widget: 4×1 date + clock | TODO | | | |
| T-1203 | Widget: 2×2 date + events + next prayer | TODO | | | |
| T-1204 | Widget: 4×2 with prayer strip | TODO | | | |
| T-1205 | Widget: Month interactive | TODO | | | |
| T-1206 | Widget: Month bitmap | TODO | | | |
| T-1207 | Widget: Week strip | TODO | | | |
| T-1208 | Widget: Schedule list | TODO | | | |
| T-1209 | Widget: Sun arc | TODO | | | |
| T-1210 | Widget: Moon | TODO | | | |
| T-1211 | Widget: Map | TODO | | | |
| T-1212 | Widget: Countdown/age (F-09) | TODO | | | |
| T-1213 | Persistent notification | TODO | | | |
| T-1214 | Dynamic launcher icon | TODO | | | |
| T-1215 | QS tile, shortcuts, live wallpaper, daydream | TODO | | | |
| T-1300 | Astronomy screen | TODO | | | |
| T-1301 | Map | TODO | | | |
| T-1302 | Compass | TODO | | | |
| T-1303 | Bubble level | TODO | | | |
| T-1400 | Converter, distance, duration, time zones, QR | TODO | | | |
| T-1500 | Settings screens | TODO | | | |
| T-1501 | Language & first-run | TODO | | | |
| T-1502 | Location settings | TODO | | | |
| T-1503 | Backup/restore UI & privacy dashboard | TODO | | | |
| T-1504 | About, licenses, diagnostics & report | TODO | | | |
| T-1600 | Wear app | TODO | | | |
| T-1700 | TalkBack pass | TODO | | | |
| T-1701 | RTL & font scale | TODO | | | |
| T-1702 | Translations | TODO | | | |
| T-1800 | Baseline/startup profiles, R8, shrinking | TODO | | | |
| T-1801 | Macrobenchmarks | TODO | | | |
| T-1802 | Compose stability | TODO | | | |
| T-1803 | Memory & leaks | TODO | | | |
| T-1804 | Security | TODO | | | |
| T-1900 | Release engineering | TODO | | | |
| T-1901 | Support system | TODO | | | |
| T-1902 | Beta program | TODO | | | |

## Epic summaries

### Epic 0 — Foundation & Delivery Pipeline (completed 2026-09-13)

**Shipped:** ~45-module Gradle skeleton with build-logic conventions and latest-stable catalog (T-000);
dependency license gate with scope-aware allow-list, cache-first POM resolution and LGPL canary negative test
(T-001); Konsist architecture suite (T-002); PR / instrumented / benchmark / release workflows with signed
builds, changelog and CycloneDX SBOM (T-003); 8 custom lint rules active in every module (T-004); test
infrastructure — cited golden fixtures with repo audit, snapshots, time fakes, 54-environment screenshot
matrix (T-005). ADR-0001…0005.

**Tests by type**

| Type | Count |
|---|---|
| Unit (JVM / host, JUnit 6 + Kotest) | 101 |
| Robolectric | 3 |
| Lint detector (`LintDetectorTest`) | 17 |
| Konsist architecture | 33 |
| Screenshot references (Roborazzi) | 1 |
| Static gates | spotless, detekt, Android Lint + custom rules, Konsist, license gate, Kover |

**Coverage:** every module with code ≥ 99.4 % line (Kover); root merged gate (≥ 85 %) and `:core:*` gates
(≥ 95 % line / ≥ 90 % branch) pass. **Benchmarks:** none yet (startup Macrobenchmark skeleton; T-1801).
**License gate:** 437 external modules, 0 violations.

**Open risks**
1. EPL-1.0 test-only exception for `junit:junit` (ADR-0005) awaits owner confirmation.
2. T-003 acceptance ("workflows green on main") requires pushing to GitHub; not yet pushed.
3. Google Maven (dl.google.com) is unreachable from the development network without a proxy/VPN; new
   AndroidX/Google dependencies cannot be fetched locally until it is reachable.
4. The development machine (14 GB) crashed from OOM during unbounded builds; builds are now budgeted and run
   inside a capped systemd scope (ADR-0004 §9). Full gates take ~2–3 min warm.
5. detekt 1.23 and Robolectric 4.17 need a JDK 21 toolchain (ADR-0004 §6, §8); Robolectric emulates API 36.
