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
gate off". Add the commit's full SHA below with who accepted it and why. The job then passes and says in its summary
that the commit was released unbenchmarked on purpose.

A waiver names one commit. It is reviewed like any other change, and it stays in the history, so "we shipped this
one without measuring it" is a sentence someone wrote rather than a silence.

## Waived commits

<!-- One entry per commit:

### <full 40-character SHA>
Accepted by: <name>, <date>
Why: <reason the run could not measure, and what was done instead>

-->

_None yet._
