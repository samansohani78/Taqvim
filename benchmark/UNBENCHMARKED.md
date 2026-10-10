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
