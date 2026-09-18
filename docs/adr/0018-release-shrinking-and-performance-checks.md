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

## Addendum (2026-09-15): widget render and map benchmarks (T-1801)

- **Map screen:** `MapScreenBenchmark` in `:benchmark` opens `taqvim://map` and measures `FrameTimingMetric` while
  pinch-zooming and panning. The map fills the screen, so gestures go to the app's root node and no test tag is
  needed.
- **In-process timings in `:benchmark:micro`:** widget bitmap renders and map masks are plain function calls, so a
  macrobenchmark cannot isolate them. A new library module `:benchmark:micro` has instrumented tests only
  (`androidTest`, `testBuildType = "release"` so the code under test is not debuggable). `WidgetRenderBenchmark`
  paints each T-702 widget drawing (4 × 4 month, sun arc, Moon, map thumbnail with the bundled outline, countdown
  ring) at its widget's pixel size. It also covers the whole map widget update, twilight grid included.
  `MapMaskBenchmark` times the day/night and Moon visibility grids off the main thread and records the crescent grid
  for regressions only.
- **Interim harness:** Jetpack Microbenchmark (`androidx.benchmark:benchmark-junit4`, Apache-2.0) was the intended
  harness, but it could not be resolved while Google Maven returned 404s. Until then a small `Timing` helper runs
  10 warm-up and 30 timed calls and writes the median as `metrics.timeNs` in the AndroidX Benchmark JSON format. It
  lacks Microbenchmark's CPU and thermal controls, so its numbers are coarser. Replacing it with `BenchmarkRule`
  keeps the class names, test names and metric name, so baselines and budgets stay valid.
- **Budgets:** `benchmark/budgets.json` holds the plan §9 limits on the `timeNs` medians: 30 ms per widget render and
  150 ms per map mask. The nightly job runs both modules, collects their results into one directory and applies the
  same regression and budget gate.
- **Not measured:** Glance composition of the 4 × 4 month widget. It runs through `RemoteViews` translation in the
  host process and has no stable in-process entry point outside a `GlanceAppWidget` session. The bitmap it shows is
  the measured `monthBitmap4x4`.

## Addendum (2026-09-15): timing tests run alone (T-1801)

- **Problem:** eight JVM and Robolectric tests assert a wall-clock budget (best of several runs) next to their
  correctness tests. In the parallel `test` run other modules' test JVMs share the CPU, and
  `TextSnippetCorpusTest` took 108 ms against its 50 ms budget on an unchanged module, while it passes alone. A gate
  that fails under load is not deterministic, and relaxing budgets would hide real regressions.
- **Policy:** a unit or Robolectric test that asserts elapsed time is a timing test. It carries
  `@Tag(TimingTest.TAG)` (JUnit 5) or `@Category(TimingTest::class)` (JUnit 4, which the Vintage engine reports as the
  tag `ir.taqvim.core.testing.TimingTest`); `TimingTest` lives in `:core:testing`. Only the timing assertion is
  tagged; correctness checks stay in untagged tests.
- **Build:** every `Test` task excludes both tags, so `test`, `testDebugUnitTest`, `koverVerify` and the screenshot
  tasks never run a timing test. Each module has `timingTest` with the same test classes and classpath as its unit
  test task (`test`, or `testDebugUnitTest` on Android), only the timing tags, one fork, no Kover instrumentation, and a
  build service that lets one `timingTest` run at a time. `./gradlew timingTests` runs them all; use
  `--max-workers=1` so compilation does not overlap them either.
- **CI:** the `unit` job of `pr.yml` runs `./gradlew timingTests --max-workers=1` after the unit tests and coverage
  gate, when everything is already compiled. Budgets are still enforced on every PR, just not under load.
- **Kover:** timing tests are not needed for coverage; the module gates are checked without them.

## Addendum (2026-09-17): baseline profiles from a Gradle Managed Device (T-1800)

- **Plugin:** `androidx.baselineprofile` (`androidx.benchmark:benchmark-baseline-profile-gradle-plugin`, Apache-2.0,
  same version as `benchmark` in the catalog) sits on the `build-logic` classpath next to AGP, so modules apply it by
  id without a version. It is build-time only: nothing from it reaches an APK, the SBOM or the in-app licence list,
  and the licence gate (which scans module configurations) is unaffected.
- **Producer:** `:benchmark` applies the plugin and declares the Gradle Managed Device `pixel6Api34` (Pixel 6,
  API 34, `aosp-atd`). `baselineProfile { managedDevices += "pixel6Api34"; useConnectedDevices = false }` runs
  `BaselineProfileGenerator` on it. The variant filter keeps the nightly `benchmark` variant and adds the plugin's
  `nonMinified*`/`benchmark*` variants.
- **Consumer:** `:app` applies the plugin with `baselineProfile(projects.benchmark)`,
  `automaticGenerationDuringBuild = false` (normal builds never start an emulator), `saveInSrc = true`,
  `mergeIntoMain = true` (files land in `app/src/main/generated/baselineProfiles`) and `dexLayoutOptimization = true`
  (the generator's `includeInStartupProfile = true` startup profile drives dex layout). A build without generated
  files still works; the app then ships only what `profileinstaller` and the libraries provide.
- **Command:** `./gradlew :app:generateBaselineProfile` downloads the emulator and system image on first use (needs
  `/dev/kvm` and access to `dl.google.com`), boots the managed device headless, runs the generator and writes the
  reviewed profiles into `app/src/main/generated/baselineProfiles`. The files are committed after review
  (docs/RELEASE.md, "Baseline profile").
- **Network note:** on 2026-09-17 Google Maven first returned 404 for every artifact and later answered 200 for the
  same URLs (plugin metadata, `repository2-3.xml`), so the earlier failures were an outage on Google's side, not a
  local block. The repositories already list `google()` first with content filters.

## Addendum (2026-09-17): timing budgets on CI runners (T-1801)

GitHub's shared runners are slower and noisier than a developer machine: `TextSnippetCorpusTest` took 56 ms against its
50 ms budget there (run 35187540605) while passing locally. Timing tests now compare with `TimingTest.budget(value)`,
which multiplies the plan's budget by the `taqvim.timingScale` system property (Gradle property
`-Ptaqvim.timingScale`, default 1; values below 1 or non-numbers are ignored). The PR workflow runs `timingTests` with
a factor of 2. Local runs and the device benchmarks keep the §9 budgets unchanged, so a real regression still shows
there; CI only catches gross slowdowns.

## Addendum (2026-09-18): what a GitHub-hosted emulator can measure (T-1801)

The nightly Benchmarks job runs on `ubuntu-latest` with a software-rendered emulator, which measures less than a
phone. Three findings from the first runs that reached the gate (35300935866, 35302152223):

- **Frame timing.** `FrameTimingMetric` reported `frameCount` alone; `frameDurationCpuMs` and `frameOverrunMs` need
  SurfaceFlinger's frame timeline, which the emulator does not provide. `benchmark/required.json` therefore requires
  `frameCount` for the frame journeys; the §9 jank budget (under 1 % of frames over 16 ms) is a physical-device check.
- **Memory.** The month screen reported 261 MB of RSS against the 80 MB of plan §9. A budget may now carry
  `physicalDeviceOnly: true`; `compare_benchmarks.py` skips it unless `--physical-device` is passed and prints which
  budgets it skipped. The memory budget is marked that way and is checked on a device before a release
  (docs/RELEASE.md), not by the nightly job.
- **Glance compositions.** `GlanceWidgetBenchmark` measured ~403 ms for every widget, within 1 % of each other,
  because `GlanceAppWidget.compose` includes Glance's own session setup whatever the widget draws. The 30 ms render
  budget of §9 is about the drawing, which `WidgetRenderBenchmark` measures (1–9 ms on the same emulator), so the
  Glance entries carry no budget; they stay required, so a regression against the recorded results still shows.

Journeys that open their screen in `setupBlock` use `StartupMode.WARM`: with `COLD` the framework kills the app
between the setup and the measured block, so the map, timeline and search journeys measured an empty screen and failed
on a missing tag. A journey that wants a cold start opens the app inside the measured block, as
`MonthScreenMemoryBenchmark` does.
