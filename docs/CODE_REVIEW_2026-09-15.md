# Taqvim: code review and prioritized backlog

Reviewed 2026-09-15 against HEAD `c8ee198`, plus the working-tree map changes present during review.

## Summary

The strongest parts are the separation of calendar arithmetic from Android, explicit calendar and date types,
source citations for datasets, transactional database operations, and extensive automated checks. The main weakness
is consistency between modules: independently reasonable implementations disagree about recurrence, time zones,
calendar variants, and the lifetime of an operation.

This review found **14 actionable findings: nine reproduced with focused probes and five traced in code**.
Prioritize date/time correctness and preservation of user data before adding more screens.

### Scope and verification

- Read production paths in calendar/events/ICS, preferences/database/backup, device calendars, scheduling,
  notifications, the editor, navigation, search, tools, widgets, maps, Wear, and related tests and release tooling.
- Ran `:core:ics:test`, `:data:events:testDebugUnitTest`, `:data:database:testDebugUnitTest`, and
  `:data:scheduler:testDebugUnitTest`: **282 tests in 45 suites; zero failures, errors, or skips**.
- Ran the benchmark utility tests: **9 passed**.
- Compiled the current recurrence, subscription expansion/import mapping, day assembly, subscription refresh, and
  official reminder calculation sources into a temporary Kotlin harness, using locally built dependencies. Eight
  application behavior probes reproduced the findings below. A separate Python probe reproduced the benchmark gap.
- The probes assert the observed faulty behavior; their successful execution is evidence of the bugs, not a passing
  correctness test. They should become regression tests asserting the corrected behavior when fixes are implemented.
- Did not run the full repository gate, device instrumentation, OEM alarm checks, or a full security audit. Source
  review across these areas is not a claim that every file or every scientific formula was independently validated.
- No application code was changed. This report is the only repository file added by this review.

**Priority:** P1 = fix before a dependable beta; P2 = important correctness/reliability follow-up.
**Evidence:** Reproduced = executed focused probe; Traced = reachable code path inspected, without the corresponding
device or storage-failure experiment.

## 1. Bugs

### B01 — P1 — Old recurring subscriptions disappear — Reproduced

**Status:** Fixed in main@2ecb37a (tests main@c9b0223).

**Where:** [IcsOccurrenceExpander.kt:115](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/ics/IcsOccurrenceExpander.kt:115).

`ruleStarts()` takes the first 1,000 occurrences from DTSTART before `expand()` filters by the requested window.
A daily event starting 2020-01-01 produced **0 rows** for September 15–17, 2026, instead of **3**. A feed can download
successfully while its long-running events silently vanish.

**Better:** seek to the relevant range, preserve COUNT/UNTIL semantics, and apply the output cap to occurrences that
overlap the window. Keep an independent computational budget, including time spent seeking.

**Verify:** old daily/weekly/monthly series, bounded COUNT, long events overlapping the lower boundary, EXDATE, and
moved overrides. A full re-download must not reproduce the disappearance.

### B02 — P1 — Daily recurrence ignores BYDAY and BYMONTHDAY — Reproduced

**Status:** Fixed in main@697cc1a.

**Where:** [RecurrenceEngine.kt:128](/home/ssohani/Lab/my/repo/Taqvim/core/ics/src/main/kotlin/ir/taqvim/core/ics/RecurrenceEngine.kt:128).

Starting Monday 2026-09-14, `FREQ=DAILY;BYDAY=MO;COUNT=3` yields September **14, 15, 16**, rather than
**14, 21, 28**. The daily branch never examines either filter. This affects imported events, subscriptions, and any
reminder path using that rule. Daily filters must restrict candidates before COUNT is evaluated under
[RFC 5545 §3.3.10](https://www.rfc-editor.org/rfc/rfc5545.html#section-3.3.10).

**Better:** implement frequency-specific filtering explicitly, including intersections of supported filters.

**Verify:** daily weekday-only rules, positive/negative month days, multiple filters, and COUNT after filtering.

### B03 — P1 — Personal events are assigned to the wrong day across time zones — Reproduced

**Status:** Fixed in main@2ecb37a and main@1413bc2 (ADR-0031).

**Where:** [DayEventsAssembler.kt:88](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/DayEventsAssembler.kt:88),
[TimelineAdapters.kt:109](/home/ssohani/Lab/my/repo/Taqvim/app/src/main/kotlin/ir/taqvim/app/di/TimelineAdapters.kt:109).

A personal meeting on September 15 at 00:30 in Tokyo belongs to **September 14 at 15:30 UTC**. The assembler instead
put it in September 15 and left September 14 empty. The timeline then converts the time and clips it against the day
it was given, so this meeting can disappear entirely. The probe reproduced the wrong day buckets; the disappearance
follows from the inspected timeline clipping path. Database range selection also uses the original event dates.

**Better:** keep recurrence dates in the event's zone, convert timed occurrences to instants, then assign them to
display-zone days. Widen source queries enough to include cross-date conversions. Keep all-day events date-based.

**Verify:** Tokyo→UTC, Los Angeles→Tehran, midnight endings, two-day events, and recurring overrides. Test repository
through timeline together, not just the minute-span helper with hand-assembled day inputs.

### B04 — P1 — Official reminders can disagree with the displayed holiday date — Reproduced

**Status:** Fixed in main@d604af6.

**Where:** [ReminderAdapters.kt:77](/home/ssohani/Lab/my/repo/Taqvim/app/src/main/kotlin/ir/taqvim/app/di/ReminderAdapters.kt:77),
[CalculatorOfficialEventSchedule.kt:22](/home/ssohani/Lab/my/repo/Taqvim/feature/notification/src/main/kotlin/ir/taqvim/feature/notification/CalculatorOfficialEventSchedule.kt:22).

The calendar display uses a source-specific Islamic variant: Iranian official events always use the Iranian official
calendar. Reminder setup passes the user's personal-calendar provider to all official events. With `TABULAR_16`,
`ir.holiday.imam-ali-birth` appears on **2026-01-03** in the calendar, while the reminder schedule includes
**2026-01-02** instead.

**Better:** calculate official reminder occurrences through the same source-aware service as the calendar. Keep the
user-selected variant for personal events and sources without an override.

**Verify:** every official reminder date equals its displayed event date across all selectable Islamic variants,
including year boundaries and validity ranges.

### B05 — P1 — Import changes a UTC recurrence into local wall-clock recurrence — Reproduced

**Status:** Fixed in main@7d38d7a.

**Where:** [IcsEventMapping.kt:153](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/ics/IcsEventMapping.kt:153).

A weekly event starting `2026-03-23T09:00:00Z`, imported in Berlin, is stored as 10:00 Europe/Berlin. Its next
occurrence becomes **2026-03-30T08:00:00Z**, an hour earlier than the original UTC series. Subscription expansion
preserves UTC, so importing and subscribing to the same event can yield different times.

**Better:** preserve UTC as the recurrence's time basis and convert only for display. Distinguish UTC, named-zone,
floating, and all-day values in storage where necessary.

**Verify:** import and subscription occurrence instants agree across both DST transitions for UTC and named-zone
series. Include export/re-import and seconds precision in the compatibility tests.

### B06 — P2 — Refresh can undo a user's subscription pause — Reproduced

**Status:** Fixed in main@2ecb37a (tests main@c9b0223).

**Where:** [SubscriptionRefresher.kt:160](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/ics/SubscriptionRefresher.kt:160).

Refresh captures the whole subscription before fetching and later writes that snapshot back. The probe paused the
subscription while a fetch was suspended; completing a 304 response changed `enabled=false` back to **true**.
The full-download path also updates the stale entity. Other editable metadata can be overwritten in the same way.

**Better:** update only fetch metadata with targeted SQL. Commit downloaded cache rows and their validators together,
and guard against a subscription being removed or changed during the request. Coordinate overlapping refreshes.

**Verify:** deterministic pause/edit/delete-during-fetch tests for 200 and 304 responses, plus two overlapping
refreshes completing in reverse order.

### B07 — P2 — Import drops the time component of UNTIL — Reproduced

**Status:** Fixed in main@7d38d7a.

**Where:** [IcsEventMapping.kt:204](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/ics/IcsEventMapping.kt:204).

A daily event at 10:00 UTC beginning September 14 with UNTIL September 15 at 09:00 UTC should occur once. Import
reduces UNTIL to a day and produces **two occurrences**, including the 10:00 event after the cutoff.

**Better:** retain an instant cutoff or translate it into the last eligible local occurrence day using DTSTART's
time. Do not treat all timestamps on the last day as permitted.

**Verify:** cutoff before, exactly at, and after the last start time; UTC and named-zone rules; subscription/import
parity. [RFC 5545 recurrence semantics](https://www.rfc-editor.org/rfc/rfc5545.html#section-3.3.10) define UNTIL as inclusive.

### B08 — P2 — Cancelled ordinary subscription events remain visible — Reproduced

**Status:** Fixed in main@2ecb37a (tests main@c9b0223).

**Where:** [IcsOccurrenceExpander.kt:50](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/ics/IcsOccurrenceExpander.kt:50).

The code excludes cancelled overrides and cancelled orphan overrides, but expands ordinary/master events without
checking `cancelled`. A cancelled standalone event produced **1 cache row**, rather than **0**.

**Better:** apply cancellation semantics to masters and standalone events as well as overrides. Preserve explicit
handling for replacement components and repeated UIDs.

**Verify:** cancelled standalone event, cancelled recurring master, one cancelled instance, and a refresh changing
an existing active event to cancelled. [RFC 5545 STATUS](https://www.rfc-editor.org/rfc/rfc5545.html#section-3.8.1.11)
defines event cancellation independently of whether the event is an override.

### B09 — P1 — Restore can fail after the database has already been replaced — Traced

**Status:** Fixed in main@c6cdb01 (ADR-0032); its two open points (screens during recovery, queued subscription refreshes) closed in main@d1f2863.

**Where:** [BackupService.kt:43](/home/ssohani/Lab/my/repo/Taqvim/data/database/src/main/kotlin/ir/taqvim/data/database/backup/BackupService.kt:43).

`replaceAll()` commits its Room transaction before `preferences.update()`. If the preference write fails, the
operation reports failure but leaves restored database rows with old preferences. Existing rollback testing fails
inside the database transaction and does not exercise this second boundary. Cancellation/process death between
stores needs consideration too.

**Better:** use a durable restore operation record with recovery/rollback semantics across both stores. Report
partial completion accurately until recovery finishes. Simply reversing the two writes moves the same problem.

**Verify:** preference-write failure after successful database replacement, process recreation at each boundary,
and alarm/subscription rescheduling only from a consistent restored state.

### B10 — P2 — System Back bypasses the editor's discard confirmation — Traced

**Status:** Fixed in main@a72f00c.

**Where:** [AppScreens.kt:74](/home/ssohani/Lab/my/repo/Taqvim/app/src/main/kotlin/ir/taqvim/app/navigation/AppScreens.kt:74),
[EventEditorScreen.kt:63](/home/ssohani/Lab/my/repo/Taqvim/feature/events/src/main/kotlin/ir/taqvim/feature/events/EventEditorScreen.kt:63).

The explicit Discard button checks `hasChanges` and opens a confirmation. Navigation Back calls `navigator.back`
directly; the editor route installs no corresponding unsaved-change handler. An accidental back gesture can discard
the same edits without the confirmation shown by the button.

**Better:** route all editor exits through one dirty-state decision, with a busy-state rule during save/delete and
support for predictive back.

**Verify:** dirty versus clean form, button versus system Back, cancelled back gesture, confirmation dismissal,
and Back during a pending save. This needs a navigation/device test, not only a ViewModel test.

### B11 — P2 — Unsaved event drafts do not survive process recreation — Traced

**Status:** Fixed in main@e9b99ec.

**Where:** [EventEditorViewModel.kt:34](/home/ssohani/Lab/my/repo/Taqvim/feature/events/src/main/kotlin/ir/taqvim/feature/events/EventEditorViewModel.kt:34),
[EventEditorRoute.kt:30](/home/ssohani/Lab/my/repo/Taqvim/feature/events/src/main/kotlin/ir/taqvim/feature/events/EventEditorRoute.kt:30).

The edited form exists only in `MutableStateFlow`. The route reconstructs the ViewModel from the event ID and initial
date/time draft; it does not restore the user's title, notes, recurrence changes, or other edits. Saveable dialog
flags do not save the form. ViewModels alone do not survive system-initiated process death, as documented in
[Android's state-saving guidance](https://developer.android.com/topic/libraries/architecture/saving-states).

**Better:** persist a compact draft with `SavedStateHandle`, or a draft table for larger/long-lived edits. Restore
inputs and recompute validation. Deliberately exclude backup passphrases from any such general mechanism.

**Verify:** edit a new/existing event, background the app, recreate the process with its task retained, and resume.
Rotation alone is insufficient because a ViewModel can survive it.

### B12 — P2 — Search dates all-day external events in the device zone — Traced

**Status:** Fixed in main@2ecb37a and main@1413bc2 (ADR-0031).

**Where:** [SearchAdapters.kt:126](/home/ssohani/Lab/my/repo/Taqvim/app/src/main/kotlin/ir/taqvim/app/di/SearchAdapters.kt:126).

Device and subscription cache rows carry `allDay`, but search ignores it and converts UTC midnight into the device
zone. In a negative-offset zone, an all-day September 15 event gets September 14 as its result date. The calendar's
existing `DeviceEventMapping.days()` correctly distinguishes all-day values, so the two screens disagree.

**Better:** reuse the established date mapping and keep all-day dates independent of time zone.

**Verify:** the result's date and navigation target match the calendar in Los Angeles, UTC, and Tehran for both
external sources. Include recurring results and events that start before the search window but overlap it.

### B13 — P2 — Privacy dashboard reports notifications allowed unconditionally before Android 13 — Traced

**Status:** Fixed in main@e6451ed (per-channel status not covered: the dashboard shows no channels).

**Where:** [BackupAdapters.kt:316](/home/ssohani/Lab/my/repo/Taqvim/app/src/main/kotlin/ir/taqvim/app/di/BackupAdapters.kt:316).

`notificationsAllowed()` returns `true` on every pre-Android-13 device. Users can still block this app's notifications
in system settings, leaving the dashboard inconsistent with actual delivery availability.

**Better:** check app-level notification availability through
[NotificationManagerCompat.areNotificationsEnabled](https://developer.android.com/reference/androidx/core/app/NotificationManagerCompat#areNotificationsEnabled()).
Keep runtime permission and individual channel states distinct if the screen intends to explain each separately.

**Verify:** app notifications blocked/unblocked on API 26–32, runtime permission denied/granted on API 33+, and
blocked reminder/athan channels where those are surfaced. Device behavior was not exercised in this review.

### B14 — P2 — Performance gate accepts missing required benchmarks — Reproduced

**Status:** Fixed in main@76d786f.

**Where:** [compare_benchmarks.py:47](/home/ssohani/Lab/my/repo/Taqvim/tools/benchmark/compare_benchmarks.py:47),
[compare_benchmarks.py:69](/home/ssohani/Lab/my/repo/Taqvim/tools/benchmark/compare_benchmarks.py:69).

Regression comparison iterates only current results. Budget checking explicitly skips a benchmark that is missing.
Removing the required startup benchmark while retaining an unrelated result produced **no regression or budget
failure**. The global empty-results check does not catch partial result loss.

**Better:** define required benchmark/metric sets per workflow/device and fail when an expected result disappears.
Keep an explicit optional category for benchmarks intentionally not run in that job. Detect renamed metrics too.

**Verify:** missing startup with remaining microbenchmark results, removed baseline metric, renamed test,
duplicated results, and documented optional exclusions.

## 2. Existing TODOs to finish

These are incomplete capabilities or validation obligations, not newly demonstrated runtime bugs. Status is based
on local code and documents; remote CI/account status was not checked.

| ID | Work | Evidence and why it matters | Completion condition |
|---|---|---|---|
| T01 | Broaden official Iranian calendar coverage | [DATA_TODO](DATA_TODO.md), DT-001/002; published data is narrower than the planned coverage. Estimates need a clear boundary. | Obtain cited missing years/months and add independent daily/holiday goldens. |
| T02 | Complete Afghanistan data and source-specific lunar dating | [DATA_TODO](DATA_TODO.md), DT-031/032/033; a handful of announcements is not a complete recurring holiday calendar. | Validate announced dates, establish the relevant month starts, and document coverage by year. |
| T03 | Implement Nepali arithmetic and dataset support | [CalendarProvider](../core/events/src/main/kotlin/ir/taqvim/core/events/OccurrenceCalculator.kt) returns null for NEPALI; T-105/D-04 remain open. | Authoritative month lengths, 32-day-month tests, round trips, language names, and feature wiring. |
| T04 | Complete human translations and review | [Translation workflow](i18n/TRANSLATING.md): 22 languages still pending; inspected feature/app/Wear strings are in default English and Persian directories. | Complete selected languages with reviewers; clearly identify fallback-language behavior. |
| T05 | Complete independent science/reference acceptance | [DATA_TODO](DATA_TODO.md), DT-013–016/034/036 and related entries. **Odeh Table VI testing now exists for 578 printed observations**; do not list it as absent. | Resolve remaining source gaps, especially published map comparisons and the 159 absent Table VI records if a complete primary source is found. |
| T06 | Run OEM alarm/athan acceptance | [Release checklist](RELEASE.md): per-OEM validation and checklist remain incomplete. JVM tests do not measure real background delivery. | Record exact/inexact alarms, reboot, battery restrictions, audio focus, DND, and permission-change results on the chosen devices. |
| T07 | Finish widget and Wear device acceptance | [PROGRESS](PROGRESS.md), T-1200+ and T-1600: implementations exist; interaction/render/update evidence remains incomplete. | Actual launcher placement/resizing/taps, midnight updates, watch tile/complication rendering, and timezone changes. |
| T08 | Finish manual accessibility/adaptive-layout checks | [PROGRESS](PROGRESS.md), T-1700/1701/T-806. Existing automated accessibility coverage excludes some surfaces. | TalkBack, large text, RTL, tablet/foldable navigation and touch targets, including map, app shell, widgets and Wear. |
| T09 | Generate profiles and establish performance baselines | No `benchmark/baselines` directory or app baseline profile was present. [Benchmark workflow](../.github/workflows/benchmark.yml) exists. | Reviewed baseline/startup profiles plus repeatable device measurements; fix B14 before relying on the regression gate. |
| T10 | Finish source coverage and release readiness records | UN/festival data remains partial; [RELEASE](RELEASE.md), [BETA](BETA.md), and [SUPPORT](../SUPPORT.md) still contain pending work/placeholders. | Record coverage, owners, support details, actual CI evidence, device sign-offs, and beta exit results. No release/upload work is implied by this review. |

## 3. Improvements: better approach and why

| ID / priority | Improvement | Why it is better | Starting point |
|---|---|---|---|
| I01 / High | Share one occurrence pipeline and explicit calendar/time context across display, reminders, search and export. Preserve original recurrence identity, time basis, source and zone. | Fixes the architectural cause of B03–B05/B07/B12: each surface should present the same event, not reinterpret it. Keep adapters thin. | `DayEventsAssembler`, `ReminderPlanner`, `IcsEventMapping`, app adapters. |
| I02 / High | Batch relational event reads into a consistent snapshot. | `RoomPersonalEventsSource` performs three additional DAO reads per event; reminder setup reads still more. This adds database round trips and can mix parent/child versions during concurrent writes. Use Room relations or batched queries inside a read transaction. | [EventInputs.kt:62](/home/ssohani/Lab/my/repo/Taqvim/data/events/src/main/kotlin/ir/taqvim/data/events/EventInputs.kt:62), `RoomReminderSetupSource.current()`. |
| I03 / High | Add seekable, computationally bounded recurrence expansion. | `monthAfter()` walks from the original year for every occurrence, despite calendars exposing constant-month-count arithmetic. Repeated historical scans and huge intervals can consume excessive CPU even with an output cap. Profile on representative histories; use arithmetic jumps and explicit limits. | [RecurrenceEngine.kt:166](/home/ssohani/Lab/my/repo/Taqvim/core/ics/src/main/kotlin/ir/taqvim/core/ics/RecurrenceEngine.kt:166). |
| I04 / High | Standardize cancellation and user-visible error handling. | Some paths rethrow cancellation correctly; editor `runCatching` treats it as ordinary failure, while subscription/location mutations can let exceptions escape and retain busy state. Use explicit operation results, cleanup on cancellation, and retryable UI states. | `EventEditorViewModel`, `SubscriptionsViewModel`, `LocationSettingsViewModel`; use `SearchViewModel`'s cancellation handling as one existing example. |
| I05 / Medium | Move Tools computation off the main dispatcher and avoid unnecessary work. | Parsing, date-distance workday scans, board building and QR encoding are collected in `viewModelScope` without a computation dispatcher. The combined content also collects inactive tool tabs. Profile long pasted text and QR input, then use a computation dispatcher and cancellation/debounce where useful. | [ToolsViewModel.kt:56](/home/ssohani/Lab/my/repo/Taqvim/feature/tools/src/main/kotlin/ir/taqvim/feature/tools/ToolsViewModel.kt:56). |
| I06 / High | Make device timezone changes explicit reactive inputs. | Several flows capture the zone at collection start; some settings flows reread it only after preferences change. A clock tick alone does not recreate those mappings. Share a zone-change stream and rebuild relevant date windows; distinguish selected-place time from device time. | `EventsRepository.days()`, `DeviceCalendarRepository.events()`, `WearGraph.setups`, Tools settings. |
| I07 / High | Unify scheduled alarms, snoozes, and delivery state. | Snoozes schedule directly through AlarmManager outside the persistent scheduler. Delivery logs claim before posting/starting succeeds. Model pending/delivered/failed states and defined retry rules; include snoozes in reboot and event-deletion reconciliation. These are reliability risks to verify with fault injection and device tests. | `ReminderSnooze`, `AthanSnooze`, [ReminderAlarms.kt:55](/home/ssohani/Lab/my/repo/Taqvim/feature/notification/src/main/kotlin/ir/taqvim/feature/notification/ReminderAlarms.kt:55), `AthanAlarms`. |
| I08 / High | Increase test independence and integration coverage. | The daily branch of the recurrence test oracle repeats B02's missing filters. Some astronomy acceptance rates are explicitly based on current output with a margin, useful for regression detection but insufficient as independent accuracy proof. Keep observation facts; add separate external reference checks and end-to-end contracts. | [RecurrenceOracleTest.kt:78](/home/ssohani/Lab/my/repo/Taqvim/core/ics/src/test/kotlin/ir/taqvim/core/ics/RecurrenceOracleTest.kt:78), `OdehTable6Test`, repository→timeline/reminder tests. |
| I09 / Medium | Make map offset approximations explicit and test representative conflicts. | `TimeZoneLayer` assigns each 2012 band one city's current offset. Updating that city's offset cannot repair geographic regions that have since diverged. Keep approved dataset choices; explain the approximation and track conflicting bands. More precise replacement geometry must satisfy the project's existing data requirements. | [TimeZoneLayer.kt:40](/home/ssohani/Lab/my/repo/Taqvim/feature/map/src/main/kotlin/ir/taqvim/feature/map/TimeZoneLayer.kt:40). |
| I10 / Medium | Separate implementation status, data coverage, and validation status in project tracking. | “DONE” frequently coexists with device/reference tests pending. Release notes still say widgets are not built, and the data tracker says Odeh is only tested against Table V despite the new Table VI suite. Separate columns make remaining work unambiguous. | `PROGRESS.md`, `RELEASE.md`, `DATA_TODO.md`. |

**Improvement status:** I02 done in main@6928708; I03 in main@e7cb1da; I04 in main@a9d2d73; I05 in main@9945456; I07 in main@762c432 (ADR-0033); I09 in main@a8041a3; I08 in main@cbd40b6 (three new bugs pinned as disabled tests; the reminder one fixed in main@37ac1b2, the two search ones in progress).

The 282 passing targeted tests show that the existing examples work. They do not invalidate these findings: the
missing cases are about combinations, failure boundaries, and agreement between surfaces. Raising a coverage
percentage alone will not address that gap.

## 4. Feature ideas

These are proposals, not current bugs or commitments. Effort is relative and excludes unresolved source acquisition.

| ID | Feature | Why users benefit | Foundation / effort |
|---|---|---|---|
| F01 | Edit or cancel one occurrence, with explicit “this occurrence” versus “whole series” choices | Recurring appointments need exceptions without rebuilding a series. | Exception/override tables and expansion already exist; editor UI is missing. Medium. “This and following” needs a separate series-splitting design. |
| F02 | Recurrence preview showing the next 10 occurrences | Makes Persian leap-day, weekday, invalid-date policy and timezone choices understandable before saving. | Reuse the corrected common occurrence pipeline. Small–medium. |
| F03 | Subscription health/details page | Users can distinguish an empty calendar from an old cache, failed download, unsupported recurrence, or partial import. | Surface last success/check, next attempt, cached range, and readable warnings. Persist the warning detail if it must survive restart. Medium. |
| F04 | Selective restore and a changes preview | Users may want events without replacing location/settings, or to recover a few events without overwriting recent work. | Build on backup preview and stable IDs; define merge/duplicate policies explicitly after fixing B09. Medium–large. |
| F05 | Timezone meeting planner with working-hour overlap | The existing timezone board becomes useful for choosing a shared time, including changes in DST on a future date. | Add date/time selection and working-hour constraints to the board; use instants internally. Medium. |
| F06 | Visible data-coverage and date-source panel | Shows which years/months are officially sourced, user-corrected or calculated, and why two lunar-calendar variants differ. | Build on citations, `ResolvedHijriDate`, and documented dataset ranges. Small–medium. |
| F07 | Expose the existing Hebrew calendar in conversion tools | Adds a useful calendar without starting the arithmetic from zero. | Domain implementation exists; enum/integration, names, formatting, settings and round-trip UI tests remain. Medium. |

**Feature status:** F01 done in main@60cca26 (ADR-0034); F02 in main@f31a07c; F07 in main@381f6c0 and main@d15b2a4 (screen-wide month names in progress); F03 in progress.

## 5. Recommended order

1. **Correct shared event semantics:** B01–B05 and B07–B08. Add the failing examples first, then verify display,
   reminders, import and subscription expansion agree. Start I01/I08 alongside these fixes.
2. **Protect user state:** B06, B09–B11. Cover concurrent refresh edits, restore interruption, accidental Back,
   and process recreation. Follow with I04/I07.
3. **Close smaller correctness and validation gaps:** B12–B14. Establish real performance baselines and required
   result sets before interpreting benchmark success as protection against regressions.
4. **Complete device/data acceptance:** T01–T10, grouped by the intended beta scope. Prioritize the languages,
   regions, widgets and devices that will actually ship.
5. **Add focused features:** F01/F02/F03 first; they build directly on the corrected event model. Selective restore,
   meeting planning and additional calendars can follow.

## Appendix: observed probe output

```text
Daily Mondays COUNT=3:
  actual   [2026-09-14, 2026-09-15, 2026-09-16]
  expected [2026-09-14, 2026-09-21, 2026-09-28]
Daily event from 2020, Sep 15–17 2026 window: actual 0 rows; expected 3
Cancelled standalone event: actual 1 row; expected 0
Tokyo Sep 15 00:30 meeting, UTC day buckets:
  actual [(2026-09-14, 0), (2026-09-15, 1)]
  expected start instant 2026-09-14T15:30:00Z
Official event ir.holiday.imam-ali-birth, TABULAR_16 preference:
  calendar 2026-01-03; reminder dates [2026-01-02, 2026-12-23]
Daily 10:00 start, imported UNTIL Sep 15 09:00:
  actual [2026-09-14, 2026-09-15]; expected [2026-09-14]
UTC weekly series imported in Berlin:
  actual [2026-03-23T09:00:00Z, 2026-03-30T08:00:00Z]
  expected second occurrence 2026-03-30T09:00:00Z
Disable subscription during refresh: actual enabled=true; expected false
Missing required startup benchmark: no budget failure
Removed baseline benchmark: no regression failure
```

The temporary harness files are `/tmp/taqvim-review-Ly34f8/ReviewProbe.kt`, `DeepProbe.kt`, and `deep.zsh`.
They compile source snapshots against cached local artifacts and are session evidence, not a portable replacement
for Gradle regression tests. The scenarios and outputs above remain usable after temporary files are removed.
