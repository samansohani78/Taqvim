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
gate off". Add the commit's full SHA below, or the tag being released, with who accepted it and why. The job then
passes and says in its summary that the commit was released unbenchmarked on purpose.

Naming the commit is exact. Naming the tag exists because a waiver written for a release changes the very SHA it is
trying to name: the waiver has to be committed, and committing it moves the commit. A tag is decided before it is
pushed, so it can be written down in advance, and it reads better afterwards besides.

A waiver names one commit or one release. It is reviewed like any other change, and it stays in the history, so "we
shipped this one without measuring it" is a sentence someone wrote rather than a silence.

## Waived commits

<!-- One entry per commit or release:

### <full 40-character SHA, or the tag, e.g. v1.2.0>
Accepted by: <name>, <date>
Why: <reason the run could not measure, and what was done instead>

-->

### v1.2.0
Accepted by: Saman Sohani, 2026-10-10
Why: the hosted runner cannot measure this app — its control benchmarks, which time fixed synthetic work the app
cannot affect, move by hundreds of percent within a single run, so the comparison says nothing either way. What this
release does rest on is a physical-device run on the owner's OnePlus 15 (CPH2745, Android 16) on 2026-10-07 and
2026-10-10: cold start 134 ms against a 350 ms budget, month scroll frame CPU P99 13.05 ms, timeline 5.33 ms,
search 11.64 ms, map 5.88 ms, the year view down from 62.2 ms to 26.9 ms, month-screen memory 67 MB PSS against an
80 MB budget, and every Glance widget composing in 1.45-10.57 ms against 30 ms. Those numbers are in
docs/STATUS_REPORT.md with the traces they came from. The waiver covers the hosted gate's silence, not an absence
of measurement.
