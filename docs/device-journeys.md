# Device journeys (R11)

`docs/PLAN.md` §8.3 ("Core UI scenarios (40)", line 735-736) requires the phone instrumented suite to demonstrate 40
core scenarios. The independent review (`docs/reviews/2026-09-19-adversarial-review.md`, R11) found that the suite
had 8 test methods (12 cases via fa/en parameterisation) that only open screens and rotate, and that the PLAN prose
lists 33 semicolon-separated groups, not 40, with no stable ids to track them against. This document gives every
scenario a stable id (J01–J40), and `app/src/androidTest/kotlin/ir/taqvim/app/device/DeviceJourneys.kt` is the
machine-readable form of the same table, checked by `DeviceJourneyCoverageTest` (see "Harness" below).

## From 33 groups to 40 ids

Splitting PLAN §8.3's paragraph on its semicolons gives exactly 33 groups (counted below). It reaches 40 by splitting
only the groups whose slash- or comma-joined items are genuinely different user actions — different screens or
different code paths — one id per item:

| Group (as written in PLAN) | Split into | Extra ids |
|---|---|---|
| "Install fresh (fa/en/ne/ckb/ar) → defaults" | J01 (fa) … J05 (ar), one id per language | +4 |
| "onboarding skip/complete" | J06 (skip), J07 (complete) | +1 |
| "edit/delete event" | J13 (edit), J14 (delete) | +1 |
| "ICS import/export" | J31 (import), J32 (export) | +1 |

33 groups − 4 groups replaced + (5 + 2 + 2 + 2) ids from those groups = 33 + 7 = **40**.

The install-fresh split follows R11's own wording directly ("Fresh-install ne/ckb/ar coverage is not supplied by its
fa/en parameterization" — i.e. the review already treats each language as a separate thing to cover). The other three
are literal alternatives ("X/Y") naming two distinct behaviours, not one behaviour repeated with different data.

Every other group that carries a quantity — "NLP input 10 phrases", "theme changes (6)", "deep links (10)",
"converter (each calendar)", "add each widget", "change location by city/GPS/coords" — is read as a required
**breadth** within one journey, not one id per item: the existing suite already tests such breadth as a single test
iterating a list (e.g. `DeviceSmokeTest`'s `LINKS`, `TABS` and `MORE_ENTRIES`), and splitting all of them literally
would overshoot 40 by dozens (10 + 10 + 6 alone is 26 ids from three groups). "change location by city/GPS/coords" is
the one boundary case kept as a single id (J24) despite being three distinct code paths, purely to make the count land
on exactly 40 with the smallest, most literally-justified set of splits (the four in the table above); a future
revision of this document is free to split it into three ids (city/GPS/coordinates) if that is judged more useful,
renumbering everything after it.

J40 ("wear smoke") is implemented by `:wear`'s own instrumented suite
(`wear/src/androidTest/kotlin/ir/taqvim/wear/device/WearDeviceSmokeTest.kt`), a different Gradle module with its own
test APK; it is tracked here for completeness but is out of scope for `:app`'s device tests and for the coverage
harness described below.

## The 40 journeys

Status: **covered** = the journey's specified outcome is asserted (not just that a screen opened); **partial** = a
test exercises part of the journey (usually navigation) without asserting the outcome; **missing** = no test touches
it. This column, and the "test" column, are the same data `DeviceJourneyCoverageTest` checks by reflection against
`DeviceJourneys.kt` — if a class or method named there is renamed or deleted without updating the registry, that test
fails.

| Id | Journey | Status | Test |
|---|---|---|---|
| J01 | Install fresh (fa) → first-run defaults | partial | `DeviceSmokeTest`, `DeviceAccessibilityTest` (open the app in fa; no defaults assertion) |
| J02 | Install fresh (en) → first-run defaults | partial | `DeviceSmokeTest`, `DeviceAccessibilityTest` |
| J03 | Install fresh (ne) → first-run defaults | missing | — |
| J04 | Install fresh (ckb) → first-run defaults | missing | — |
| J05 | Install fresh (ar) → first-run defaults | missing | — |
| J06 | Onboarding: skip | missing | — |
| J07 | Onboarding: complete | partial | `DeviceSmokeTest#onboardingPagesOpen` (walks the pages to the end; no assertion of the resulting defaults) |
| J08 | Month swipe changes the shown month and its title | partial | `DeviceSmokeTest` (opens the calendar and rotates it; no swipe gesture or title assertion) |
| J09 | Select a day → its tabs, with the day's real content | **covered** | `DeviceHolidayTest#nowruzIsMarkedAndItsTitleMatchesTheDataset` (new, this change) + `DeviceSmokeTest#everyLinkedScreenOpensAndSurvivesRotation` |
| J10 | Today button returns to the current day | missing | — |
| J11 | Long-press a day opens the event editor on it | missing | (`taqvim://event/new/...` opens the editor directly; no long-press gesture is exercised) |
| J12 | Create a personal event + reminder → it appears on its day and the agenda, the reminder is scheduled and fires | **covered** | `DeviceEventLifecycleTest#createdEventShowsOnItsDayAndAgendaAndSchedulesAReminder` (new) + `DeviceReminderTest#reminderNotificationFires` |
| J13 | Edit an existing event | missing | — |
| J14 | Delete an event | missing | — |
| J15 | An official-event reminder | missing | — |
| J16 | Search jumps to the matching day | partial | `DeviceSmokeTest` (opens Search with a query; no jump assertion) |
| J17 | Year view: select a day | partial | `DeviceSmokeTest#everyTabAndMoreEntryOpens` (opens Year; no selection assertion) |
| J18 | Timeline: drag-create an event | missing | — |
| J19 | Agenda scrolls through upcoming events | partial | `DeviceSmokeTest#everyTabAndMoreEntryOpens`; content strengthened by `DeviceEventLifecycleTest` (title shown), but no scroll gesture is exercised |
| J20 | Converter: each calendar | partial | `DeviceSmokeTest` (opens the Converter with a date; no per-calendar output assertion) |
| J21 | NLP input: 10 phrases | missing | (covered outside the device suite by `core/nlp` unit/property tests) |
| J22 | Distance and workdays tool | missing | — |
| J23 | Enable athan → alarm scheduled → fires → stop | missing | — |
| J24 | Change location by city, GPS or coordinates | missing | — |
| J25 | Add each widget, tap it, its deep link opens | partial | `DeviceSurfacesTest#everyWidgetIsBoundUpdatedAndDrawn` (bind/update/draw only; no tap-through to the deep link) |
| J26 | Notification content is correct | partial | `DeviceReminderTest#reminderNotificationFires` (title only, not the full content) |
| J27 | Theme changes (6) | missing | — |
| J28 | Font scale 2.0 | missing | (R10 also found the calendar's own font scale deliberately capped at 1.3) |
| J29 | RTL switch | partial | `DeviceSmokeTest`, `DeviceAccessibilityTest` (fa vs en exercises RTL vs LTR layout; no RTL-specific assertion) |
| J30 | Backup, wipe, restore: the event is back | **covered** | `DeviceBackupRestoreTest#backupWipeRestoreBringsTheEventBack` (new) |
| J31 | ICS import | missing | — |
| J32 | ICS export | missing | — |
| J33 | Subscription refresh | missing | — |
| J34 | Compass orientation lock | partial | `DeviceSmokeTest#everyTabAndMoreEntryOpens` (opens Compass; no lock assertion) |
| J35 | Map layers | partial | `DeviceSmokeTest#everyTabAndMoreEntryOpens` (`visitToggles` toggles every layer control on/off; no visual-effect assertion) |
| J36 | Astronomy slider | partial | `DeviceSmokeTest` (opens Astronomy; no slider interaction) |
| J37 | Settings are table-driven: a changed row survives a cold read and the calendar screen shows it | **covered** (one row) | `DeviceCalendarPersistenceTest#changedPrimaryCalendarSurvivesAColdReadAndShowsInTheMonthGrid` (new; the primary-calendar row only, not every settings row) |
| J38 | Deep links (10) open their destination | partial | `DeviceSmokeTest#everyLinkedScreenOpensAndSurvivesRotation` (opens 16 distinct links and asserts each reaches its destination and survives rotation — breadth exceeds the "10", but the assertion is open+rotate, not the link's semantic correctness) |
| J39 | PROCESS_TEXT detection | missing | — |
| J40 | Wear smoke | covered (different module) | `:wear`'s `WearDeviceSmokeTest` |

## The four journeys implemented in this change (T-1700)

Full behavioural implementations, asserting persistence rather than navigation, in
`app/src/androidTest/kotlin/ir/taqvim/app/device/`:

1. **J12 — `DeviceEventLifecycleTest`.** Saves a personal event with a reminder through the real
   `PersonalEventStore`, then: (a) polls `AlarmStore.alarms()` for a `REMINDER` row whose `sourceId` is the event's
   id, proving the reminder was actually scheduled (not just requested) without paying for `DeviceReminderTest`'s
   multi-minute wait for delivery; (b) opens the event's day (`taqvim://day/...?calendar=gregorian`) and asserts its
   title is shown; (c) opens the Agenda screen and asserts the same title is shown there.
2. **J30 — `DeviceBackupRestoreTest`.** Saves an event, exports a backup with the real `BackupService` (the same
   class `app/di/AppModule.kt` binds for the Backup screen), deletes the event ("wipe" — deleting only what this test
   created, not every table a full reset would touch, so other suites sharing this install keep their own data),
   restores from the exported backup, and asserts the event is back both in storage (`PersonalEventStore.load`) and
   on its day screen.
3. **J37 — `DeviceCalendarPersistenceTest`.** Changes the primary calendar (a `SettingsCatalog` table-driven row)
   through the real preferences repository, then proves durability the way a new process would experience it: this
   module's instrumented tests are self-instrumenting (test code runs inside the app's own process; no
   `android:targetProcess` split or Test Orchestrator is configured), so `am force-stop` would abort the
   instrumentation itself rather than exercise a clean restart. Instead the test opens a second, independent
   `UserPreferencesRepository` over `UserPreferencesRepository.createDataStore`, pointed at the same on-disk
   `user_prefs.pb` file, bypassing the running app's live singleton and in-memory `StateFlow` — a cold read of exactly
   the bytes a fresh process would open at start-up. Only then does it reopen the calendar screen and assert a day
   cell's accessibility description names today in the new (Gregorian) calendar, computed the same way
   `MonthPageBuilder` computes it (`DateFormatter.format` on the primary calendar's date), so the assertion is tied to
   production formatting rather than a hand-written expectation.
4. **J09 — `DeviceHolidayTest`.** Computes Nowruz (Persian New Year, the current Persian year's month 1 day 1) via
   `PersianCalendarSystem`, reads its `DayEvents` from the real `EventsRepository`, asserts it is marked as a holiday
   and reads the expected title from the dataset at run time (never hardcoded, per the project's "compute every
   value, tables only as goldens" rule). It then opens that day and asserts the title text is shown and that some day
   cell's accessibility description contains the localized "holiday" word (`R.string.calendar_holiday`), i.e. the
   month grid marks it.

### API level

All four new tests carry `@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)`, matching every existing class
in this package. They **cannot** run on API 26 or 30: every one of them calls the shared `useLanguage()` helper
(`DeviceApp.kt`) to make the language deterministic before asserting on text or content descriptions, and
`useLanguage()` reads `Context.getSystemService(LocaleManager::class.java)` — per-app language (ADR-0023) — which
does not exist before API 33. This is a pre-existing, repo-wide constraint of the shared harness, not something these
four tests introduce; avoiding it would mean accepting whatever language a fresh emulator boots with, which is not
compatible with asserting specific title and formatted-date text.

## The harness (declarative journeys, a missing one fails)

`app/src/androidTest/kotlin/ir/taqvim/app/device/DeviceJourneys.kt` is the registry: a `Journey` data class per id
with its `summary`, `status` and, when not `MISSING`, a `coveringTest` reference (`"ClassName#methodName"`, several
separated by `;`; a `module:` prefix for J40's out-of-module coverage).

`DeviceJourneyCoverageTest` is the check, mirroring how `benchmark/required.json` plus
`tools/benchmark/compare_benchmarks.py --required` fail a nightly run that is missing a required benchmark rather than
only comparing whatever happened to be reported (R08):

- every id is unique and the 40 ids run `J01`…`J40` with no gaps;
- every `COVERED` or `PARTIAL` journey's `coveringTest` is loaded by reflection (`Class.forName` in this package) and
  must have a `@Test`-annotated method of the named name — a renamed or deleted test leaves a stale claim that fails
  immediately instead of the suite silently covering fewer journeys than it claims to;
- a `MISSING` journey must not name a test (catches the opposite drift: a test written but the registry never
  updated).

This differs from the benchmark gate in one respect: `compare_benchmarks.py` is a host-side script that compares a
declared requirement against *emitted results* from a completed run, so it can also catch a test that silently never
executed. `DeviceJourneyCoverageTest` is a JUnit test living in the same `androidTest` source set as the journeys it
tracks (so it can reference their classes directly without a separate reporting/scraping step) and therefore runs
*with* the rest of the suite, on the same Gradle Managed Device, rather than as a separate host-side pass; it still
needs the emulator to execute (unlike the benchmark script), which is consistent with the rest of this module's
instrumented tests being unrunnable in this sandboxed change (see below). It is intentionally cheap (pure reflection
and set arithmetic, no Android APIs), so once it does run it costs a fraction of a second.

`RequiredJourneys` is not a separate list: `JourneyRegistry.REQUIRED_IN_THIS_MODULE` (all ids except J40) is exactly
"every id this module's harness is accountable for", and every one of those ids has an entry in `ALL` — there is
nothing to omit, because `MISSING` is itself a tracked, honest status rather than an absence from the registry. What
the harness fails on is a `COVERED`/`PARTIAL` claim that does not resolve, or an id that stops existing.

## What is compile-verified, not run

No emulator was available while making this change. `:app:compileDebugAndroidTestKotlin` was used to confirm the new
and existing device test sources compile (T-1700 depends on a real device/Gradle Managed Device, which this change
does not have access to); `:app:testDebugUnitTest`, `:konsist:test`, `spotlessCheck` and `detekt` were also run.
`DeviceJourneyCoverageTest`'s reflection checks (class exists, method exists, is `@Test`-annotated) were
cross-checked by hand against the actual source of every referenced class while writing this document, but the test
itself has not executed on a device.

## CI

`.github/workflows/*.yml` was not changed by this task, and no line in it needs to change to *run* the new suite:
`.github/workflows/instrumented.yml`'s "Run connected tests" step
(`script: ./gradlew connectedDebugAndroidTest -x :wear:connectedDebugAndroidTest --continue`) already runs every test
class in `app`'s `androidTest` source set on its API 26/30/33/36 matrix, so `DeviceJourneyCoverageTest`,
`DeviceEventLifecycleTest`, `DeviceBackupRestoreTest`, `DeviceCalendarPersistenceTest` and `DeviceHolidayTest` are
picked up automatically — AGP discovers `androidTest` classes by source set, not by an explicit list. On the API 26
and 30 legs every one of the five new classes is skipped, not failed, by its `@SdkSuppress(minSdkVersion =
Build.VERSION_CODES.TIRAMISU)`, the same as the four existing classes in this package.

R06 (release tags could bypass the quality gates) is already fixed on this branch, ahead of this task:
`.github/workflows/release.yml`'s `qualify` job calls `tools/ci/verify_required_checks.py` against
`.github/required-checks.txt` before anything builds or signs, and that file already lists "API 26", "API 30", "API
33" and "API 36" (`instrumented.yml`'s job names) as required checks. So once this suite runs clean on a PR, it is
already load-bearing for release with no further edit to either file — the new tests inherit the existing gate rather
than needing a new one.
