# ADR-0018: Release shrinking, keep rules, profiles and benchmark gates

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md T-1800 (baseline and startup profiles, R8 full mode, resource shrinking; B cold
  start; APK size ≤ 8 MB or 90 % of the first measurement), T-1801 (macrobenchmarks: startup, month scroll, timeline
  scroll, widget render, map mask, search; budgets §9; nightly regression > 10 % fails), §9 quality budgets; builds on
  ADR-0004 (toolchain), ADR-0015 (navigation), ADR-0016 (links), ADR-0017 (security baseline)

## Context

The application convention plugin already builds the release variant with R8 (full mode is the AGP default) and
resource shrinking. What was missing: rules for code R8 cannot see through, a check that those rules hold, the size
budget, profile generation and benchmarks for every journey the plan names. The development network cannot download
new artifacts (Google Maven returns HTTP 404), so only libraries already in the Gradle cache are used: Macrobenchmark
1.5.0, UiAutomator 2.4.0 and ProfileInstaller 1.4.1. The Baseline Profile Gradle plugin is not in the cache.

## Decision

1. **Keep rules only where no library ships one**, each commented in `app/proguard-rules.pro`. There is one:
   protobuf-javalite 4.x generated message fields (Proto DataStore, T-600). The lite runtime reads them by name and its
   jar ships no R8 rules; the first shrunk release renamed them (`languageCode_ -> g`), which would have broken reading
   and writing preferences in every release build. Nothing is added for kotlinx.serialization (its jar ships R8 rules
   and serial names are compiled into descriptors), Koin (no reflection in the DSL), Room, Compose, Navigation 3 or
   WorkManager (its consumer rules already keep worker class names, which WorkManager stores).
2. **`:app:verifyReleaseShrinking`** reads the R8 mapping and checks `app/shrinking-requirements.txt`: the back-stack
   and backup serializers are kept, the worker keeps its name and the proto messages keep their field names.
3. **`:app:checkReleaseApkSize`** fails when a release APK is over 8 MiB (plan §9). Both checks run in the PR build
   job after `assemble`, not in `check`, so unit-test jobs do not pay for an R8 build.
4. **Profiles:** `ir.taqvim.benchmark.BaselineProfileGenerator` records cold start → calendar → month swipes → day
   details → times → search with `BaselineProfileRule` (startup profile included). It needs an API 33+ device or
   emulator; its output is reviewed and committed as `app/src/main/baseline-prof.txt`, installed by ProfileInstaller.
   No profile is written by hand. The on-demand `baseline-profile` job of `benchmark.yml` produces one on an emulator.
   Dex layout optimisation from the startup profile needs the Baseline Profile Gradle plugin and waits for it.
5. **Benchmarks** (`:benchmark`): cold and warm startup, cold startup without ahead-of-time compilation, month pager
   (T-801), timeline scroll and search typing, all driven through test tags exposed as resource ids by `MainActivity`
   and through `taqvim://` links. A `timeline` link was added to ADR-0016 for this. Widget render (T-1200, blocked)
   and map mask (T-1301, in progress) benchmarks wait for their features.
6. **Budgets and regressions:** plan §9 budgets are device budgets (Pixel 6a class and low-end API 26). Emulator runs
   are only compared with committed baselines: `tools/benchmark/compare_benchmarks.py` fails the nightly job when a
   median or P50 grows by more than 10 %. Baselines are committed from reviewed results under `benchmark/baselines/`;
   until then benchmarks are recorded only.

## Consequences

- A library update that starts needing reflection shows up as a failing `verifyReleaseShrinking` only if it touches a
  listed class; runtime smoke tests on a device remain necessary before a release (T-1900 checklist).
- The size budget is measured on the universal release APK; per-ABI splits would only lower the number.
- No generated baseline profile is committed yet, so cold start runs without one until the generator has run on a
  device.
