# ADR-0004: Module map and toolchain deviations from the plan

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §3.1, §3.2, §3.3, T-002, T-005

## Context

While bootstrapping (T-000) a few places in the plan turned out to be either internally inconsistent
or out of date with respect to the "latest stable" rule. This ADR records each deviation.

## Decisions

### 1. Android modules in the `core` namespace
§3.1 says `core/*` are pure Kotlin/JVM with no `android.*` imports, but §3.2 places the Compose design
system in `:core:ui` (theme, components, canvas painters producing bitmaps). A design system cannot be
Android-free. **`:core:ui` is an Android library** and is excluded from the "no `android.*`" Konsist
rule. All other `:core:*` modules remain pure JVM.

### 2. `:core:testing` split
T-005 puts both pure-JVM helpers (golden files, `FakeClock`, fixture loaders) and Android/Compose
helpers (Compose test rules, Roborazzi device config) into `:core:testing`. Pure JVM `:core:*` modules
must be able to depend on the test helpers without pulling in Android. Therefore:
- `:core:testing` — pure JVM helpers (golden files with provenance headers, fake clock/time zone,
  fixture loaders, JUnit 5 extensions).
- **`:core:ui-testing`** (new) — Android: Compose environment rules (locale, layout direction, font
  scale, theme), Roborazzi device/theme matrix.

### 3. `:konsist` module
T-002 requires a Konsist suite that runs in `check`. It lives in a dedicated JVM module **`:konsist`**
(tests only) so it can scan the whole repository. A root `konsistTest` task aliases `:konsist:test`.

### 4. `:cli` deferred
`:cli` is marked v1.1 in §3.2 and is not created until F-17 is scheduled.

### 5. JUnit 6 (Jupiter) instead of "JUnit 5"
The latest stable JUnit release is 6.x, the direct successor of JUnit 5 with the same Jupiter
programming model (`org.junit.jupiter.api.*`). We use the JUnit 6 BOM. Robolectric, Compose UI tests,
Macrobenchmark and lint `LintDetectorTest` are JUnit 4 based and run on the JUnit Platform through the
Vintage engine.

### 6. detekt 1.23.8 run as a forked CLI on JDK 21
The only stable detekt line is 1.23.x (2.0 is still alpha). detekt 1.23.8 embeds the Kotlin 2.0.21
compiler, which **crashes on JDK 25** (`IllegalArgumentException: 25.0.4` while parsing the runtime
version), and the detekt Gradle plugin always runs inside the Gradle daemon. We therefore do not apply
the detekt Gradle plugin. Instead, `build-logic` registers a cacheable `detekt` `JavaExec` task per
module that runs `io.gitlab.arturbosch.detekt:detekt-cli` on a **JDK 21 toolchain** (auto-provisioned
through the Foojay toolchain resolver). The detekt classpath is pinned to Kotlin 2.0.21 so KGP 2.4's
version alignment cannot upgrade it. detekt runs without type resolution. `check` depends on `detekt`.
We re-evaluate when detekt 2.0 is stable.

### 7. Android Gradle Plugin 9 built-in Kotlin
AGP 9 compiles Kotlin natively, so Android modules do not apply `org.jetbrains.kotlin.android`.
Android-free modules use `org.jetbrains.kotlin.jvm` with the `com.android.lint` plugin so the custom
lint rules also cover `:core:*`.

### 8. JDK, toolchains and bytecode target
Gradle and the Kotlin/Android compilers run on JDK 25 (latest LTS). All modules target Java 21
bytecode, which D8/R8 fully support and which keeps `:core:*` usable from any JVM 21+ consumer (F-17).
Two tool families do not support JDK 25 yet and run on a **JDK 21 toolchain** auto-provisioned by the
Foojay resolver:
- detekt 1.23.x (see §6);
- all `Test` tasks — Robolectric 4.17 officially supports JDKs up to 21, so JVM and Android host tests
  share that one supported runtime.

Robolectric's SDK 36 `ApplicationSharedMemory` interceptor reflects into `java.io.FileDescriptor` via
`jdk.internal.access.SharedSecrets` on every modern JDK (including 21), so test JVMs additionally get
`--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` and `--add-opens=java.base/java.io=ALL-UNNAMED`.

We move tests and detekt to JDK 25 once Robolectric and detekt support it.

### 9. Build resource budget
On 2026-09-13 an unbounded local build (6 GB Gradle daemon, one forked test/detekt JVM per module under
`org.gradle.parallel`) exhausted the 14 GB development machine; the kernel OOM killer terminated the
Gradle daemon (5.5 GB RSS) and the desktop session was lost. The build is therefore explicitly budgeted:
- Gradle daemon `-Xmx3g`, Kotlin daemon `-Xmx1536m`, `org.gradle.workers.max=4`, daemons exit after
  20 idle minutes.
- A shared `ForkedJvmLimiter` build service allows at most `taqvim.maxForkedJvms` (default 2)
  concurrently running forked JVMs; every `Test` task (`maxParallelForks = 1`, 1 GB heap) and every
  `detekt` task (768 MB heap, no internal `--parallel`) must acquire it.
- Local agent-driven builds run inside a `systemd-run --user --scope` with `MemoryMax` and `CPUQuota`
  so a runaway build is killed inside its own cgroup instead of taking down the desktop.

CI runners (4 vCPU / 16 GB) may raise the limits with `-Dorg.gradle.workers.max` and
`-Ptaqvim.maxForkedJvms`.

### 10. Composables are excluded from Kover coverage
The `:core:*` gate requires 95 % line and 90 % branch coverage. The Compose compiler adds
recomposition-skipping branches (`$changed` bitmasks, `skipToGroupEnd`) to every `@Composable` function; a
JVM test that composes once cannot reach them. The first Android core module (`:core:ui-testing`) had every
hand-written branch covered, yet reached only 78.6 % branch coverage because of three generated branches in
one composable. Functions annotated `@Composable` are therefore excluded from Kover (module and merged
reports). They are verified by what the plan prescribes for UI: Roborazzi screenshot matrices, Compose UI
tests and Robolectric tests. Non-composable UI logic (state holders, formatters, painters) stays under the gate.

## Consequences

- The Konsist rules must list `:core:ui` and `:core:ui-testing` as the only Android modules under
  `core`.
- If detekt 2.0 becomes stable, item 6 is superseded.
