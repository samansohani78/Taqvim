<!-- Archived from REVIEW.md, the independent adversarial review of main@98260a4, 19 September 2026.
     Kept verbatim as the record; what was done about each finding is in docs/STATUS_REPORT.md §"Independent review". -->

# Independent adversarial review

**DO NOT SHIP — reproducible calendar, prayer-time, parser and privacy defects remain, and the required release evidence is incomplete.**

Reviewed commit: `98260a41a8383a61b0ab520684e2baa34e9f153b`, 19 September 2026. `docs/PLAN.md` was read first and used as the specification; ADRs were examined for explicit deviations. Repository reports were treated as claims. No application, test, data or configuration fixes were made; no commit was created.

The review began before an interruption that erased `/tmp` and ended its running checks. The checkout subsequently advanced. Findings below were checked against the commit above; fresh measurements take precedence over the earlier observations. Two shared-checkout Gradle invocations received an external **“stop command received”**. Their partial reports are not presented as completed suites. Remaining checks used a `git archive` copy of the reviewed commit and a separate Gradle daemon directory. The commands appendix distinguishes failures, incomplete runs and successful execution.

## Findings, sorted by severity

| ID | Severity | Area | Location | Summary |
|---|---|---|---|---|
| R01 | P1 | B | `core/ics/src/main/kotlin/ir/taqvim/core/ics/RecurrenceEngine.kt:349,388` | Yearly recurrence from Hebrew month 13 crashes in the next common year. |
| R02 | P1 | B | `feature/tools/src/main/kotlin/ir/taqvim/feature/tools/DateTools.kt:82`; `app/src/main/kotlin/ir/taqvim/app/navigation/AppIntents.kt:49` | An unresolved Nowruz anchor becomes “three days ago,” silently returning the wrong date. |
| R03 | P1 | F | `feature/about/src/main/kotlin/ir/taqvim/feature/about/DiagnosticsRedactor.kt:22` | Multiword titles and authorization secrets survive redaction. |
| R04 | P1 | F, G | `core/ics/src/main/kotlin/ir/taqvim/core/ics/ContentLines.kt:31` | Sub-megabyte folded ICS input takes seconds through quadratic string copying. |
| R05 | P1 | B, E | `core/praytimes/src/test/kotlin/ir/taqvim/core/praytimes/PrayerModelTest.kt:218,242` | Fresh suite finds Asr after sunset; randomly sampled tests can miss the same defect. |
| R06 | P1 | I | `.github/workflows/release.yml:52,96` | Tag release does not require tests, lint, coverage, screenshots or green checks for that SHA. |
| R07 | P1 | C | `docs/PLAN.md:220`; `dataset/international/un-international-days.json` | Mandatory dataset acceptance is unmet: 102 UN records versus at least 150. |
| R08 | P2 | G, I | `tools/benchmark/compare_benchmarks.py:166`; `benchmark/budgets.json` | Missing baselines allow a synthetic 60-second startup to pass. |
| R09 | P2 | C | `tools/dataset/src/test/kotlin/ir/taqvim/tools/dataset/NoPerYearManualDataTest.kt:94,132` | The no-per-year-data gate misses runtime XML and structured date objects. |
| R10 | P2 | H | `core/ui/src/main/kotlin/ir/taqvim/core/ui/component/DayCell.kt:90,144` | Requested 2.0 font scaling is capped at 1.3 in calendar cells/header. |
| R11 | P2 | E | `docs/PLAN.md:735`; `app/src/androidTest/kotlin/ir/taqvim/app/device/DeviceSmokeTest.kt:23` | Device smoke tests do not implement the specified 40 behavioral journeys across the API/language matrix. |
| R12 | P2 | A | `docs/sources/iran/MANIFEST.md:81,85`; `docs/sources/iran/.gitignore:1` | Two third-party paper PDFs are tracked; paper exclusion is a finite filename list. |
| R13 | P2 | A | `docs/PROVENANCE.md:196`; `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/CalendarMath.kt` | Generalized week numbering lacks an algorithm entry; Placidus has no precise reference for its defining construction. |
| R14 | P2 | C, E | `data/events/src/test/kotlin/ir/taqvim/data/events/CalendarCompletenessTest.kt:184,242,268` | “Every field/every language” completeness is broader than what the tests assert. |
| R15 | P2 | D, G | `data/events/src/main/kotlin/ir/taqvim/data/events/ics/SubscriptionRefresher.kt:135,241` | Subscription locks accumulate for the lifetime of the singleton. |
| R16 | P2 | D | `feature/notification/src/main/kotlin/ir/taqvim/feature/notification/AthanService.kt:73` | Snooze persistence runs in a detached coroutine after the service stops. |
| R17 | P2 | H, C | `feature/about/src/main/res/values/strings.xml:107` | FAQ describes official-table use without explaining the opt-in override. |
| R18 | P3 | F | `docs/SECURITY.md:35,41` | Security document contradicts actual exported components and production logging. |

P0 = blocks release; P1 = fix before beta; P2 = should fix; P3 = nit/documentation. A missing external verification is not automatically a proven defect.

## Findings and reproduction evidence

### R01 — Hebrew yearly recurrence throws

**Severity: P1. Location:** `core/ics/src/main/kotlin/ir/taqvim/core/ics/RecurrenceEngine.kt:349,388`.

**Evidence:** Running the production engine with `HebrewCalendarSystem`, start `CalendarDate(HEBREW, 5784, 13, 1)` and `RecurrenceRule(YEARLY, count=3)` throws `IllegalArgumentException: Hebrew month must be in 1..12 (was 13)`. `inYear` reuses `start.month`; `resolve` asks `monthLength` before applying an invalid-date policy. The following common Hebrew year has only 12 months. This is a valid start date, not malformed input.

**Impact:** A supported calendar can crash occurrence expansion. Integer month reuse also needs a separate semantic audit around inserted Adar, because an ordinal month can denote a different named month in common/leap years.

**Fix:** Give Hebrew recurrence an explicit named-month/leap-month policy, validate the target month before querying its length, and apply the chosen skip/next/last-day behavior. Add leap-to-common regression cases, including months after Adar. Do not merely catch and silently drop the series.

### R02 — Unresolved anchored NLP silently changes meaning

**Severity: P1. Locations:** `DateTools.kt:82` and `AppIntents.kt:49` at the paths in the table; `core/nlp/src/main/kotlin/ir/taqvim/core/nlp/RelativeRules.kt`.

**Evidence:** With reference Gregorian `2026-09-18`, production `DateParser.parse("سه روز قبل از نوروز", ParseContext(reference))` returns `PERSIAN 1405-06-24`, JDN `2461299`, confidence `0.85`, span `0..9`, kind `RELATIVE`. It accepts the “three days before” prefix and ignores “Nowruz.” The tools and PROCESS_TEXT contexts do not supply the official-event anchor lookup. The editor has a separate anchor-aware context, so this finding does not claim every entry point fails.

**Impact:** The exact phrase promised in PLAN F-03 yields a plausible, confidently wrong date in affected entry points.

**Fix:** Supply the event anchor lookup consistently and reject a relative prefix followed by an unresolved anchor. Test the complete phrase through each user entry point, not only the parser with a custom lookup.

### R03 — Redaction leaks sensitive values

**Severity: P1. Location:** `feature/about/src/main/kotlin/ir/taqvim/feature/about/DiagnosticsRedactor.kt:22–45`; report integration in `ProblemReport.kt`.

**Evidence:** Direct production calls returned:

```text
title=Meeting with oncologist tomorrow -> title=[redacted] with oncologist tomorrow
Authorization: Bearer abcdefSECRET -> Authorization: Bearer abcdefSECRET
عنوان=ملاقات خصوصی -> عنوان=ملاقات خصوصی
```

The sensitive-key pattern consumes only `\S+` for an unquoted value and omits authorization/localized keys. E-mail and URL-token examples did redact; those successes do not cover the bypasses. Coordinates are intentionally reduced to two decimal places, not removed completely. New crash capture also supplies exception text to the reporting path (`app/.../diagnostics/CrashLog.kt`, `error.stackTraceToString()`), increasing the importance of treating arbitrary text as unsafe.

**Impact:** A user sharing a supposedly redacted problem report can disclose event-title fragments or tokens. This reproduces a redactor failure; it does not demonstrate automatic network exfiltration.

**Fix:** Prefer structured, allow-listed diagnostic facts. Exclude arbitrary exception messages or redact entire sensitive values and authorization headers before persistence/reporting. Add multiword, Unicode-key, multiline and mixed-format adversarial cases.

### R04 — Folded-line parser has quadratic cost

**Severity: P1. Locations:** `core/ics/src/main/kotlin/ir/taqvim/core/ics/ContentLines.kt:31–43`; `data/events/src/main/kotlin/ir/taqvim/data/events/ics/IcsDocuments.kt` (`DEFAULT_MAX_BYTES`).

**Evidence:** A valid VCALENDAR/VEVENT with `SUMMARY:a` followed by repeated ` a\n` continuation lines was accepted by the production `IcsReader`:

| Continuations | Input bytes | Wall time on this host | Outcome |
|---:|---:|---:|---|
| 80,000 | 240,110 | 753 ms | Success |
| 160,000 | 480,110 | 2,071 ms | Success |
| 320,000 | 960,110 | 8,297 ms | Success |

Each continuation does `previous.second + physical.substring(1)`, repeatedly copying the accumulated logical line. All examples are below the 5 MiB document/fetch limit. Host contention makes the exact milliseconds non-portable; the implementation and scaling establish the quadratic behavior.

**Impact:** A subscribed/imported file can monopolize a worker and create allocation/GC pressure with a small payload. This is not an XML entity attack: the parser is not XML.

**Fix:** Unfold into a streaming `StringBuilder`, bound logical-line size and component/property counts, and check cancellation during parsing/expansion. Add a bounded-cost folded-line regression and huge-RRULE cases.

### R05 — Prayer-time ordering fails a fresh property test

**Severity: P1. Locations:** `core/praytimes/src/test/kotlin/ir/taqvim/core/praytimes/PrayerModelTest.kt:218,242`; `core/praytimes/src/main/kotlin/ir/taqvim/core/praytimes/PrayerTimesCalculator.kt` (Asr calculation/order).

**Evidence:** The first fresh `./gradlew test` failed after 705 generated attempts:

```text
latitude=-69, longitude=-171, JDN=3342886
seed=8171453473268066201
753.4632346923836 should be < 752.4507963282219
```

A separate call with UTC offset −660 minutes, `MWL`, `ANGLE_BASED` returned for Gregorian `4440-05-29`: sunrise `12:18`, dhuhr `12:26`, Asr `12:33`, sunset/maghrib `12:32`, as an `Available` result. This is a far-future polar-boundary case; the review did **not** reproduce an equivalent modern-date ordering failure. A separate modern scan found only legitimate sunset-after-midnight wrap cases, which are excluded from the findings.

**Impact:** The documented all-year/latitude ordering property is false, and ordinary random seeds can let the gate pass without exercising the failing input. This does not invalidate the Tehran/Kabul sample comparison below.

**Fix:** Add the exact case as a deterministic regression. Solve/validate Asr within the actual daylight interval and return an explicit unavailable result when the target altitude cannot occur. Preserve legitimate next-day sunset semantics instead of comparing wrapped wall-clock values blindly.

### R06 — Release can bypass the quality gates

**Severity: P1. Location:** `.github/workflows/release.yml:52,96`.

**Evidence:** The tag jobs build/sign/package, run license/version/manifest/page-alignment checks and produce an SBOM. They have no dependency on the PR test, lint, coverage, screenshot or device jobs and do not verify successful checks for the tagged SHA or its ancestry on the accepted branch. `release-dry-run.yml` is a separate workflow triggered by selected build-file paths or manually.

**Impact:** An untested tagged commit can produce release artifacts despite failing the plan's gates. A draft release reduces accidental public publication but does not establish artifact qualification.

**Fix:** Reuse a required validation workflow for the exact tagged SHA, or verify the complete required check set and approved ancestry before signing/artifact promotion. Make missing/in-progress/skipped checks fail closed.

### R07 — Mandatory dataset acceptance remains unmet

**Severity: P1. Location:** `docs/PLAN.md:220`; `dataset/international/un-international-days.json`.

**Evidence:** Counting parsed records gives Iran 27, Afghanistan 6, Nepal 27, UN 102 and ancient Iran 2: **164 total**. D-05 explicitly requires at least 150 UN entries: **48 are missing from that minimum**. All 164 `reviewedBy` values are `pending`. The documents acknowledge unresolved Afghanistan/Nepal sources and the future 1406 official calendar; acknowledgement is not fulfillment of the acceptance criteria.

**Impact:** The release is incomplete against the requested spec. “Computed for every day” cannot demonstrate completeness of the holiday inventory or accuracy of uncited future observances.

**Fix:** Complete the primary-source records and actual review attestations, or explicitly revise/approve the v1 scope before release. Keep unresolved observances visible as dataset limitations; do not invent recurring holidays from unsupported one-year announcements.

### R08 — Startup regression gate accepts 60 seconds

**Severity: P2. Locations:** `tools/benchmark/compare_benchmarks.py:166–178`; `benchmark/budgets.json`; `.github/workflows/benchmark.yml`.

**Evidence:** `benchmark/baselines` does not exist. The comparator substitutes `{}` and prints “No baseline … recorded only.” A synthetic AndroidX result file containing **every required benchmark and metric**, startup medians `60000` ms, and other medians `1`, exits **0** under the nightly comparator command. The absolute budget file has no startup budget. RSS is `physicalDeviceOnly`; the emulator job cannot enforce it. Frame-count presence is not the <1% jank budget.

**Impact:** A green benchmark workflow does not establish the 350/800 ms startup, jank, search or physical-device memory budgets.

**Fix:** Commit reviewed baselines, fail on a missing baseline for an established required benchmark, and enforce absolute startup/jank/search/RSS budgets on the intended hardware. Keep baseline-recording mode explicit and non-qualifying for release.

### R09 — Runtime date-data gate has reproducible holes

**Severity: P2. Location:** `tools/dataset/src/test/kotlin/ir/taqvim/tools/dataset/NoPerYearManualDataTest.kt:94–148`.

**Evidence:** The actual test method was invoked with its repository/dataset system properties pointing to a temporary miniature module, without changing repository files:

```text
app/src/main/res/xml/year_1406.xml: <dates>1406-01-01</dates> -> PASSED
app/src/main/resources/year_1406.json: {"year":1406,"month":1,"day":1} -> PASSED
app/src/main/resources/iso.txt: 1406-01-01 -> REJECTED
```

It scans module `src/main/assets`, `src/main/resources`, `src/main/res/raw`, plus dataset override files, for a four-digit ISO-date regex. It does not scan all Android resource directories/qualifiers or other runtime source sets. Kotlin scrutiny is filename-based; only names matching the table/override pattern are inspected. Generated event checks inspect direct `.kt` files and `EventRule.Single` text.

**Impact:** The gate can stay green when prohibited per-year runtime data is added. The negative control establishes that the test really executed.

**Fix:** Enumerate packaged runtime inputs from Android/JVM source sets, parse structured date-bearing formats, and use explicit resource exceptions with provenance. Add planted canaries for XML, qualified resources, numeric JSON dates and innocuously named Kotlin tables.

### R10 — Large-font setting is deliberately overridden

**Severity: P2. Locations:** `core/ui/src/main/kotlin/ir/taqvim/core/ui/component/DayCell.kt:90,144`; `MonthGrid.kt:224`.

**Evidence:** `MAX_CELL_FONT_SCALE = 1.3f`; both components replace the incoming density with `fontScale.coerceAtMost(MAX_CELL_FONT_SCALE)`. A system scale of 2.0 therefore renders calendar text at at most 1.3. `docs/PROVENANCE.md` calls this a product choice; PLAN still requires large-font accessibility.

**Impact:** A screenshot named `fs200` can pass while the key calendar text ignores the requested size. `MonthGrid` also has a 40 dp minimum cell height; disjoint 48 dp touch targets on narrow windows require device/layout verification, not inference from a Pixel 6 screenshot.

**Fix:** Reflow or offer an accessible alternate month/day presentation that honors the requested scale. Verify semantics bounds and clipping at 2.0 on narrow supported windows; do not use a capped screenshot as evidence of uncapped large-font support.

### R11 — The 40 UI journeys are not demonstrated

**Severity: P2. Locations:** `docs/PLAN.md:735`; `app/src/androidTest/kotlin/ir/taqvim/app/device/DeviceSmokeTest.kt:23`; scenario map below.

**Evidence:** The phone instrumentation has eight test methods, expanded to 12 cases by the fa/en parameterization. Navigation/rotation and presence checks are not event creation→delivery, backup→wipe→restore, timeline drag→persist or subscription→display journeys. `DeviceSmokeTest` is suppressed below API 33. Fresh-install ne/ckb/ar coverage is not supplied by its fa/en parameterization. Numerous local tests cover pieces; the map distinguishes them from complete journeys. The prose heading says 40 but lists 33 semicolon-separated groups, with embedded variants and no stable IDs.

**Impact:** “12 instrumented cases” cannot establish the plan's 40 journeys on API 26/30/33/36. UI integration and lower-API behavior remain unproven.

**Fix:** Give the scenarios stable IDs, implement each complete behavior and assert persistence/output, parameterize the specified languages/APIs, and fail when a required scenario is skipped. Retain local tests as component coverage rather than substituting them for device acceptance.

### R12 — Third-party papers are actually archived

**Severity: P2. Locations:** `docs/sources/iran/MANIFEST.md:81,85`; `docs/sources/iran/.gitignore:1`.

**Evidence:** `git ls-files` includes `6th-abstract.pdf` (7 pages, 182,433 bytes) and `Workshop4-report.pdf` (5 pages, 752,766 bytes); the manifest classifies both as `paper`. Text extraction confirms authored conference abstracts/workshop text, not merely a citation or hash. The ignore file lists 17 named papers. `git check-ignore --no-index` rejects those names but returns exit 1 for these two and `new-paper.pdf`.

**Impact:** The requested “papers cited, not copied” boundary is not met, and a future paper can be accidentally staged. A public URL or internal `LicenseRef-Cited-Publication` label does not itself demonstrate permission to redistribute complete authored text. This is **not** a claim of GPL contamination or a legal conclusion of infringement.

**Fix:** Remove authored paper copies from the distributable/source archive unless permission is documented, retain bibliographic citations/checksums, and enforce a reviewed allow-list for source artifacts. Audit history separately if removal from repository history is required.

### R13 — Algorithm provenance is incomplete

**Severity: P2. Locations:** `core/calendar/src/main/kotlin/ir/taqvim/core/calendar/CalendarMath.kt`; `docs/PROVENANCE.md:196–216`.

**Evidence:** The provenance ledger references T-106 from other entries but has no dedicated entry for generalized week numbering (PLAN A-08). Placidus reference 3 is “general astrological-astronomy knowledge”; no title/page/equation identifies the construction and convergence choices. Published chart validation is explicitly pending. A-15/A-16 labels also refer to different subjects between PLAN and the provenance ledger (tithi/NLP versus Hebrew/Easter), so ID-only traceability is unsafe.

**Impact:** A reviewer cannot reconstruct the complete written-source chain or independently check the house algorithm from the ledger alone. No forbidden-source use has been established.

**Fix:** Add a file-to-algorithm reference map, precise public formulas/edition/pages, assumptions and convergence/boundary rules; reconcile IDs and add an independent published chart oracle. Obtain real reviewer attestations instead of accepting `pending` as completion.

### R14 — Completeness assertions are narrower than the claim

**Severity: P2. Locations:** `data/events/src/test/kotlin/ir/taqvim/data/events/CalendarCompletenessTest.kt:184,242,268`; `app/src/test/kotlin/ir/taqvim/app/completeness/SkyAndTimesCompletenessTest.kt:99–140`; `docs/DATA_AUDIT.md:112`.

**Evidence:** The tests do build and iterate every day in SH 1380–1480; they are not merely a sample. However, the day assembler runs five national defaults (`fa`, `prs`, `ps`, `ne`, `en`), not all 24 languages. A null `day.hijri` is accepted by the `hijri != null && invalid` check. Lunar-tithi expected dates invoke the same `NepaliLunarDays.days` routine used by production. The Arctic prayer assertion only checks four values are in `0..1439`; it does not establish chronological ordering. The astronomy test selects certain fields and deliberately permits missing polar/lunar rise/set results; houses and every UI field are not covered.

**Impact:** These tests establish useful iteration/structural properties, but cannot substantiate the blanket “every field non-null/no missing values” or independent astronomical correctness claim. The circular tithi oracle can reproduce its own error.

**Fix:** Define required/optional fields per place and state, assert required fields explicitly, enumerate all intended language/default combinations, check chronological times with day offsets, and compare tithi results with an independent source. Preserve legitimate unavailable polar states.

### R15 — Subscription mutex map has no eviction

**Severity: P2. Location:** `data/events/src/main/kotlin/ir/taqvim/data/events/ics/SubscriptionRefresher.kt:135,241`; singleton binding in `SubscriptionRefreshWork.kt`.

**Evidence:** `private val locks = mutableMapOf<Long, Mutex>()`; acquisition uses `locks.getOrPut(subscriptionId) { Mutex() }`. There is no deletion, size cap or expiry. The comment explicitly keeps a lock for the lifetime of the refresher, assuming subscriptions are few. Repeated add/delete churn accumulates different IDs even when the active set is small.

**Impact:** Unbounded process-lifetime growth. This is a source-proven bound failure; a device OOM was not reproduced, and normal small workloads may never notice it.

**Fix:** Use reference-counted lock entries with safe removal after the last holder/waiter, or a fixed striped-lock set. Test add/refresh/delete churn and concurrent refresh serialization.

### R16 — Detached snooze write can outlive its service

**Severity: P2. Location:** `feature/notification/src/main/kotlin/ir/taqvim/feature/notification/AthanService.kt:65–74`.

**Evidence:** The service creates `CoroutineScope(Dispatchers.Default).launch { snoozer.snoozeAthan(...) }`; its comment says the service stops at once. The scope has no service-owned completion, persistent work handoff or explicit failure reporting.

**Impact:** A process exit after the foreground service stops can lose an acknowledged snooze before persistence/scheduling finishes. **UNVERIFIED — needs device process-death/fault-injection testing** to quantify the window; this is a lifecycle risk, not a reproduced missed alarm.

**Fix:** Finish the durable write before stopping the service, or hand it to an appropriate persistent work mechanism while maintaining user-visible failure handling. Test interruption before, during and after persistence.

### R17 — FAQ misstates the Islamic date source

**Severity: P2. Locations:** `feature/about/src/main/res/values/strings.xml:107`, `values-fa/strings.xml:92`, and translated `about_faq_islamic_date_a` entries.

**Evidence:** The FAQ says the Iran official method uses published University of Tehran month starts and estimates unpublished months. Production `IranIslamicCalendar()` defaults to no table, and official overrides are opt-in. The same unconditional explanation appeared in the sampled Arabic, German and French translations.

**Impact:** Users can believe a computed religious date is an official announcement. The wording conceals the setting that changes the authority of the date, even where the date-source UI is otherwise present.

**Fix:** Explain computed-by-default behavior and the optional official override, including its finite coverage. Update all translations from the corrected source text and test FAQ/source-label consistency.

### R18 — Security document is stale

**Severity: P3. Locations:** `docs/SECURITY.md:35,41`; `feature/calendar/src/main/kotlin/ir/taqvim/feature/calendar/CalendarRangeGuard.kt:31`.

**Evidence:** The merged release manifest has **54 exported components**: 2 activities, 33 activity aliases, 14 receivers, 5 services. This includes launcher aliases, widget configuration/receivers and system-bound surfaces, beyond the document's MainActivity/PROCESS_TEXT description. These components are not automatically vulnerabilities. The document also claims no `android.util.Log`; production `CalendarRangeGuard` imports it and calls `Log.w(..., failure)`.

**Impact:** Reviewers cannot use the threat-model summary as an accurate inventory. The log call bypasses the stated redacted-diagnostics route.

**Fix:** Generate/synchronize the component summary from the merged-manifest allow-list and document permission/action protections. Route range warnings through the intended diagnostics policy or explicitly document the exception.
