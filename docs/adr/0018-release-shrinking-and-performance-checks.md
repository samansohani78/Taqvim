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

## Addendum (2026-09-18): APK headroom and a warning margin on the size gate (T-1800)

The release APK was 7 265 104 bytes on main@8a4d8ba (7 261 536 at v1.0.0-rc1), 86.6 % of the 8 MiB budget of plan
§9, and the gate said nothing until a build went over it. Both are fixed: the gate now warns from 90 % of the budget,
and four changes take 836 039 bytes (0.80 MiB, 11.5 %) out of the APK without changing what the app does. It is now
6 429 065 bytes, 76.6 % of the budget.

| part of the APK (compressed size) | before | after | change |
| --- | ---: | ---: | ---: |
| `resources.arsc` (stored) | 2 480 524 | 2 017 640 | −462 884 |
| `cities.tsv` | 713 910 | 536 911 | −176 999 |
| other Java resources | 127 165 | 30 745 | −96 420 |
| `assets/map` | 131 646 | 89 323 | −42 323 |
| `META-INF` | 34 587 | 106 | −34 481 |
| dex | 3 114 889 | 3 114 545 | −344 |
| `res/`, `lib/`, other assets | 466 613 | 466 613 | 0 |
| zip headers and alignment (134 fewer entries: 1 256 → 1 122) | | | −22 588 |

### The gate

`ApkSizeBudget` in `build-logic` turns a measured size into a verdict — within budget, near budget or over budget —
and formats the line the build prints for it. `:app:checkReleaseApkSize` prints that line at lifecycle level below
90 % of the budget, as a warning from 90 % up to the budget, and fails only above the budget. The budget itself
still passes, so the gate fails exactly where it did before. `ApkSizeBudgetTest` covers the boundaries (just under
90 %, exactly 90 %, one byte under the budget, the budget, one byte over) and the wording of each line, which a
Gradle task cannot be unit-tested for.

### What was cut

1. **Locale filters (462 884 bytes).** `androidResources.localeFilters` keeps only the 24 launch languages of
   `locales_config.xml`. The APK carried 88 locales, because appcompat and other AndroidX libraries ship strings for
   about ninety; each unreachable locale cost roughly 6 kB of resource-table offsets plus its string values, and no
   language picker in the app can select one. Plain qualifiers sit next to the BCP-47 ones (`zh-rCN` beside
   `b+zh+Hans`, `az` beside `b+az+Latn`) so the libraries' own translations survive the filter. The debug
   pseudo-locales `en-rXA` and `ar-rXB` are listed too, or the filter would drop them. `LocaleFiltersTest`
   fails if a language is added to `LanguageTable` without its filter, or a filter names a language the app does not
   offer.
2. **Build-time metadata (130 901 bytes over 134 entries).** `packaging.resources.excludes` drops the packaged
   `.proto` descriptors (protobuf-javalite, Glance and DataStore each ship their sources), `*.kotlin_builtins` (only
   kotlin-reflect reads them and nothing depends on it), `DebugProbesKt.bin`, the AndroidX `META-INF/*.version`
   markers and the `META-INF` licence copies. Apache-2.0 §4(a) is still met: the About screen serves the licence
   texts from `assets/licenses` (T-1504), which is where the app has always shown them.
3. **`cities.tsv` column by column (176 999 bytes).** The table is written one line per column instead of one line
   per place, with the places ordered by country, region and English name and the languages grouped by script.
   Every value is byte-identical to before — no place, column or decimal was dropped — but a column's values are now
   adjacent, so deflate finds them inside its 32 kB window. `CityTableParser` reads the new layout and reports
   problems by place instead of by line; `natural_earth_time_zones.py` reads the table through its `# columns:`
   header rather than fixed positions.
4. **Delta-encoded map geometry (42 323 bytes).** In `world-110m.txt`, `time-zones-10m.txt` and
   `plates-matthews-2016.txt` only the first pair of a line is a position; every later pair is the step from the
   point before it, still in hundredths of a degree. A band's label stays absolute, being a single point, not a line.
   The geometry is unchanged — the steps are whole numbers and add back up to the same positions — and small
   repetitive numbers are what deflate packs well. `line_layers.delta_pairs` does it for every generator and the
   assets carry their new `# body-sha256`. `time-zones-10m.txt` also records the SHA-256 of the cities table it was
   derived from; that table's values did not change, only their layout, so the recorded hash was updated rather than
   the bands recomputed.

### Decision: language splits stay off

`bundle.language.enableSplit` stays `false`. All 24 launch languages ship in the base module.

The measurement: **1 564 338 bytes** of the APK are the 23 languages a device does not use — 648 676 bytes of
per-locale entry chunks in the resource table and 988 416 bytes of string values reachable only from a non-default
locale, less the one language the device keeps (about 27 400 and 45 354). Language splits are the only way to take
that out; nothing else in the APK holds anything near it. That is the whole of the 1.5 MiB that was asked for, and
it is why the cuts above stop at 836 039 bytes.

It was rejected anyway. The language is chosen inside the app (T-1501, ADR-0023), not taken from the system locale,
because a Persian calendar is used by people whose phone runs in English or German; Play delivers a language split
only when the device's own language list asks for it, so a language picked in the app would fall back to English on
the devices most likely to pick it. The app cannot fetch the missing split either: it holds no `INTERNET` permission
(T-1804), and on-demand delivery would add Play Feature Delivery, a proprietary Google dependency that ADR-0003 does
not allow and that the APK published on GitHub could not use. Giving up in-app language choice to save 1.5 MiB of a
6.1 MiB app is the wrong trade.

The owner can flip `enableSplit` to `true` if they later accept that users whose system locale list names no launch
language see English until they install one from Play.

### Measured and left alone

- **Dropping appcompat** from the runtime classpath (koin-android pulls it in only for its `ScopeActivity` and
  `ScopeFragment` helpers, which a Compose app on a `ComponentActivity` never instantiates) was built and measured:
  21 096 bytes, because R8 and the resource shrinker had already removed nearly all of it. It was reverted for now,
  since it changes the checked-in `third-party.json` licence list and deserves its own change with the licence gate
  re-run.
- **CLDR rows.** Every key kind in `formats.properties` — date patterns, weekday, month and era names per calendar,
  plural rules, relative times, units and list patterns — is read by `FormatTable`, and the root and English
  fallbacks are already dropped at generation. What repeats (a language's weekday names are the same in all three
  calendars) costs almost nothing once deflated, so no row was removed.
- **`resources.arsc`** is stored uncompressed, as Android requires of an app targeting API 30 or later, so its 2 MB
  is 2 MB of APK. Sparse resource encoding would shrink its offset tables but needs `minSdk` 32 and Taqvim's is 26;
  resource-name collapsing has no AGP switch in 9.4.
- **Glance layouts.** 1 088 of the APK's 1 122 entries are the layouts `glance-appwidget` generates, about 366 kB.
  They are selected by id at runtime, so the resource shrinker keeps them all; they go when the widgets do.

### Sizes

- Universal release APK: 7 265 104 → **6 429 065 bytes** (76.6 % of the budget).
- Release AAB: **10 883 140 bytes** (an AAB stores its resources uncompressed; it is not what a device downloads).
- Per-device download from that AAB: about **6.37 MB** — the APK less the three unused ABI folders (53 128 bytes of
  the 73 584 in `lib/`); the app has no density-specific resources worth splitting. With language splits it would be
  about 4.81 MB.

### Three further levers, checked with numbers

- **Duplicate translated values.** The resource table's string pool already stores each distinct value once:
  32 027 string-valued entries across all configs point at 23 237 pool strings, so 8 790 duplicate references are
  collapsed and the 186 362 bytes they would have cost are already not being spent. `config/i18n/same-as-source.txt`
  is a 140-line review list for translators and is not packaged. What is *not* shared is the entry itself: 1 401
  localized `<string>` entries hold a value identical to the default one and each still costs about 20 bytes of
  resource table, **28 020 bytes** in total. Deleting them would let Android fall back to the default config, but it
  would also make `MissingTranslation` fire and would fight the Weblate round-trip, which expects a complete file per
  language. Not worth 28 kB.
- **Unreachable resources.** `shrinkResources` is already removing entries, not just blanking files: the linked
  resource table has 4 795 entries and the APK has 3 556, so **1 239 entries and 357 `res/` files are already gone**
  — 351 styles, 306 attrs, 123 dimens, 117 drawables, 112 strings, 103 colors, 55 styleables, 43 layouts and the
  rest. What is left is reachable: 1 162 layouts and 822 ids are the matrix `glance-appwidget` generates and selects
  by id at runtime, and the 1 302 strings are the app's own UI text. There is no unreachable string or drawable left
  to drop.
- **Translator comments and untranslatable strings.** The `<!-- MT: needs review -->` marker sits in 418 of the 437
  `values-*/strings.xml` files, and **none of them reach the APK**: `resources.arsc` contains the text zero times,
  because AAPT2 drops XML comments when it compiles a resource file. No `values-*` file carries
  `translatable="false"`; the 9 untranslatable strings live in the default `values/` only, as they should, so they
  are stored once rather than per language.

## Addendum (2026-09-19): the benchmark variant carries the app's baseline profile (T-1800)

Every macrobenchmark runs against the `benchmark` build type: release-like, R8-minified, debug-signed. That variant
shipped **no rule of the app's own code**. Its merged ART profile held 4 131 rules, all from the AndroidX libraries'
bundled profiles, against 37 419 rules and 6 269 app rules in `release`.

The cause is the consumer plugin, not AGP: `androidx.baselineprofile` adds the committed profile
(`app/src/main/generated/baselineProfiles`) to the variants it manages, by calling
`variant.sources.baselineProfiles.addStaticSourceDirectory` on each of them. A build type declared by hand is not one
of them, and `initWith(getByName("release"))` copies build-type settings, never a source directory.

Nothing failed, which is the point. `StartupBenchmark` uses `CompilationMode.DEFAULT`, that is
`Partial(BaselineProfileMode.Require)`, and `Require` is satisfied by *a* profile in the APK — the libraries' rules
qualified. So the nightly job measured an app compiled almost without its profile: cold start and first-frame numbers
were pessimistic, `startupCold` and `startupColdWithoutProfile` measured nearly the same thing, and a lost or empty
profile could never fail the gate.

**Decision.** `app/build.gradle.kts` adds the committed profile to the `benchmark` variant explicitly, and
`check<Variant>BaselineProfile` (both non-debuggable variants, in `check`) reads the merged profile text AGP hands to
R8 and fails when fewer than 1 000 rules name the app. The floor sits far below the real 6 269 so ordinary code
changes never move it and only broken wiring trips it. The check reads the merged *text* because the packaged
`assets/dexopt/baseline.prof` stores dex indices, not names: from the outside, a profile holding only library rules
looks much like a complete one.

**Effect.** The benchmark APK's profile went from 11 775 to 16 563 bytes, one dex profile key to two, and 47 074 to
72 650 bytes uncompressed — the same content as release. No committed benchmark result had to be discarded:
`benchmark/baselines` does not exist yet, so the regression gate has no recorded numbers, and none of the nine
absolute budgets in `benchmark/budgets.json` is startup- or frame-sensitive in the app process (one is month-screen
RSS, the other eight are `:benchmark:micro` widget and map-mask timings in a separate module). The first recorded
baseline must therefore be measured on this commit or later.

## Addendum 2026-09-19 — baselines are required, and startup has budgets (REVIEW R08)

An independent review showed the nightly gate passing a synthetic run whose startups took 60 000 ms: `benchmark/baselines`
did not exist, so every result was "recorded only", and `budgets.json` had no startup entry. Changes:

- **A required benchmark without a committed baseline fails** (`Missing baseline`). `--record-baseline` (the
  `record-baseline` input of `benchmark.yml`) accepts the missing baselines so a first set can be recorded, but such a
  run is **non-qualifying**: it exits 3 even when clean, so the job is red and the release gate cannot count it.
- **Startup and jank budgets.** Cold and warm start (`timeToInitialDisplayMs`, 350 ms, plan §9) and the 24-month scroll
  (`frameDurationCpuMs` P99 < 16 ms, the §9 "< 1 % of frames over 16 ms") are `physicalDeviceOnly`. On the hosted
  emulator they are checked against a `hostedMaximum` — three times the medians that emulator measured on run
  35432306382 (cold 796.7 ms → 2 400 ms, warm 752.2 ms → 2 250 ms, scroll P99 126.3 ms → 380 ms). A ceiling is a guard
  against absurd results, not a baseline and not a §9 budget. The P99 of each sampled metric feeds the budgets only; the
  10 % regression comparison still uses medians and P50s.
- **Not covered:** the low-end API 26 cold budget (800 ms) needs a low-end device; search (< 20 ms per query over
  10 000 events) has no benchmark reporting a per-query time.

**Consequence:** until a reviewed baseline set is committed, the nightly job fails on 24 missing baselines, and a
release tag is refused (the macrobenchmark is a required check). Next step: dispatch `benchmark.yml` with
`record-baseline`, review the `benchmark-results` artifact, and commit it under `benchmark/baselines` — from a run on a
commit that carries the app's baseline profile (main@19bc59c or later).
