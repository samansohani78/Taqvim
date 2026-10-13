# Commits released without a performance measurement

`benchmark.yml` reports two different things, and they are not the same:

- **Macrobenchmark (nightly)** — the job ran to completion.
- **Performance qualified** — the run actually measured this commit: the control benchmarks behaved, so the timings
  mean something, and they stayed inside their budgets.

The second is the one `.github/required-checks.txt` should hold a release to. Until 2026-10-10 only the first
existed, and because the gate exits 0 on an inconclusive comparison — deliberately, so that a runner which cannot
measure does not block a release — the check concluded `success` and a release tag qualified on a commit that
nothing had measured. v1.0.0, v1.1.0 and v1.1.1 all shipped that way, each saying so in its own release notes.

## Waiving a commit

A hosted runner is often unable to measure this app at all, so there has to be a way through that is not "switch the
gate off". A waiver is that way: a reviewed sentence in the history saying "we shipped this one without measuring
it", rather than a gate quietly reporting success.

`tools/ci/check_benchmark_waiver.py` decides, and it is tested rather than inlined in the workflow. The rules:

- **`### <full 40-character SHA>`** waives exactly that commit, on any run.
- **`### <tag>`** waives a release, and is read **only** on a run whose ref is that very tag and whose resolved
  commit is the one being judged. A run on `main` never consumes a tag waiver, and a tag that has been moved cannot
  speak for a commit it no longer points at.
- A waiver must carry **`Accepted by:`** and **`Why:`**. One that does not is not a waiver.
- Only headings inside **## Waived commits** count. A SHA mentioned anywhere else in this file authorizes nothing —
  the first version of this check was `grep` over the whole file, so writing a commit into a sentence was enough.

Naming the commit is exact. Naming the tag exists because a waiver written for a release changes the very SHA it is
trying to name: the waiver has to be committed, and committing it moves the commit. A tag is decided before it is
pushed, so it can be written down in advance, and it reads better afterwards besides.

The consequence for sequencing: dispatch `benchmark.yml` on `main` before tagging as usual, and if that run comes
back inconclusive, push the tag and dispatch `benchmark.yml` again **on the tag** before re-running `release.yml`.
The second run is what reads the waiver. `release.yml` takes the most recent run of each check, so it supersedes
the first.

## Waived commits

<!-- One entry per commit or release:

### <full 40-character SHA, or the tag, e.g. v1.2.0>
Accepted by: <name>, <date>
Why: <reason the run could not measure, and what was done instead>

-->

### v1.3.0
Accepted by: Saman Sohani, 2026-10-12
Why: the hosted runner still cannot measure this app, for the reason this file was written for — its control
benchmarks time fixed synthetic work the app cannot affect and move by hundreds of percent inside one run, so the
comparison says nothing either way. The nightly runs of 2026-10-11 and 2026-10-12 both concluded `Macrobenchmark
(nightly)` green and `Performance qualified` red on exactly that.

What this waiver does **not** say is that 1.3.0 was measured somewhere else. It was not. The physical-device
numbers quoted for v1.2.0 — OnePlus 15 (CPH2745, Android 16), 2026-10-07 and 2026-10-10 — were taken against the
1.2.0 code and are not reused here as if they were fresh. One change since then plausibly moves one of them: the
dataset's generated source went from 42 files to 83 and its title entries from roughly 1 760 to 2 188, and the
first read of `OfficialEvents.ALL` was the slowest component measured on that device at 148 ms. Nothing has
re-measured it. The widget change (main@d3f78a05) removes work rather than adding it, and the search change
(main@b454a49a) walks a lazy sequence that stops at the caller's limit, so neither is expected to cost more; that
is reasoning, not measurement, and it is written here as such.

What was checked on this commit, on the host rather than on a phone: `timingTests` green at `-Ptaqvim.timingScale=2`
including the dataset module's own cost bounds; release APK 6 830 544 bytes, 81.4 % of the 8 388 608 budget;
`:app:checkReleaseBaselineProfile` and `:app:checkBenchmarkBaselineProfile` green, so the 41 generated classes the
dataset gained are in the baseline profile rather than left to the interpreter.

The right close for this entry is a device run against the tagged commit, compared against the v1.2.0 figures, and
`OfficialEvents.ALL` is the number to look at first.

Re-checked on 2026-10-12 against the tree the tag will point at, which is several commits later than the one this
entry was written on: the release APK is 6 800 904 bytes, 81.1 % of the budget — 29 640 bytes smaller than the
figure above, because the device-calendar module was removed after this waiver was written. `timingTests` at
`-Ptaqvim.timingScale=2`, `:app:checkReleaseBaselineProfile` and `:app:checkBenchmarkBaselineProfile` are green on
it. This is a correction of the host figures, not a new acceptance and not a measurement: the reason for the waiver
is unchanged, and so is what it does not claim.

Updated 2026-10-13, after the owner attached the phone: **most of this entry's "not measured" no longer holds.**
The macro and micro suites both ran on the OnePlus 15 (CPH2745, Android 16, arm64) against `main@4121f5e3`,
followed by the `dumpsys meminfo` check. Cold start 155.67 ms and warm start 43.11 ms against a 350 ms budget;
month-screen TOTAL PSS 65.1 MB against 80 MB; every painter render under 30 ms and every map mask under 150 ms.
So v1.3.0 does **not** ship unmeasured, and the waiver below covers only what it always covered: the hosted
runner's silence.

What the phone run did **not** establish, and what this waiver therefore still carries: the §9 jank budgets.
`FrameTimingMetric` reported `frameCount` alone for all six frame journeys, so `frameDurationCpuMs.P99` — the
metric the ≤ 16 ms budgets are written against — is absent from the results. This same phone reported 13.05 ms
P99 for v1.2.0 on 2026-10-07, so the device is not the limitation and the cause is unresolved. Frame counts are
not jank measurements and are not recorded as such. Month scroll, timeline, search, year view and map pan/zoom are
accordingly **unverified against §9 for v1.3.0**, and that is accepted here rather than hidden.

Cold start also moved: 134 ms for v1.2.0 to 155.67 ms here, +16 %, at 44 % of budget. The dataset's generated
source grew from 42 files to 83 between the releases and `OfficialEvents.ALL` was the slowest component ever
measured on this phone; that remains the first thing to look at, and this run did not isolate it.

Two tooling findings, both in MANUAL_TEST_CHECKLIST §8a: `compare_benchmarks.py` refuses to compare these results
because every committed baseline was recorded on an emulator, so there is still no physical-device baseline in the
repository; and `ScreenScrollBenchmarks.searchTyping` failed once with `search_field is not shown` and passed on
an immediate re-run, which is a journey flake and not a regression.

### v1.2.0
Accepted by: Saman Sohani, 2026-10-10
Why: the hosted runner cannot measure this app — its control benchmarks, which time fixed synthetic work the app
cannot affect, move by hundreds of percent within a single run, so the comparison says nothing either way. What this
release does rest on is a physical-device run on the owner's OnePlus 15 (CPH2745, Android 16) on 2026-10-07 and
2026-10-10: cold start 134 ms against a 350 ms budget, month scroll frame CPU P99 13.05 ms, timeline 5.33 ms,
search 11.64 ms, map 5.88 ms, the year view down from 62.2 ms to 26.9 ms, and month-screen memory 67 MB PSS
against an 80 MB budget. Those numbers are in docs/STATUS_REPORT.md with the traces they came from. The waiver
covers the hosted gate's silence, not an absence of measurement.

Corrected 2026-10-10, after an independent review: this entry first also claimed "every Glance widget composing in
1.45-10.57 ms against 30 ms". Both halves of that were wrong. The figures were differences between separately
measured runs of GlanceWidgetBenchmark, which times `GlanceAppWidget.compose` end to end and is dominated by a
fixed per-call cost of roughly 405 ms; a 1-to-11 ms difference on that base is smaller than the harness's own
run-to-run spread, so it is an incremental wall-clock estimate and not a measured composition cost. And the 30 ms
budget it was compared against belongs to WidgetRenderBenchmark, which times the painters with no Glance harness,
not to GlanceWidgetBenchmark, which has no budget at all. The widget render budget was and remains met by the
benchmarks it actually applies to. Nothing else in this entry changes.
