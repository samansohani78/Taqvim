# ADR-0021: Compose stability and memory checks

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md T-1802 (compiler metrics in CI; all UI models `@Immutable`/stable; recomposition
  count tests for `DayCell`, ≤ 1 per state change), T-1803 (LeakCanary in debug; month screen heap budget < 80 MB),
  §9 budgets

## Context

The Compose compiler skips a composable only when its parameters are stable. Types from Taqvim's pure JVM `:core`
modules (`CalendarDate`, `Coordinates`, `LanguageSpec`, …) are compiled without the Compose compiler, so the reports
marked them unstable even though they are immutable data classes. A first report run found 15 unstable composable
parameters and unstable UI states such as `CalendarUiState`/`CalendarContent`, all caused by those core types.

## Decision

1. **Stability configuration** — `config/compose/stability.conf` lists immutable core value types (each commented)
   plus `kotlin.ranges.IntRange`. A type is added only if it has no `var` properties and no mutable state. Classes
   that live in Compose modules use `@Immutable` or immutable collections instead.
2. **Enforcement** — every Compose module has `composeStabilityCheck`. With `-Ptaqvim.composeMetrics=true` it reads the
   module's compiler reports and fails on any composable parameter marked `unstable` and on unstable `*UiState`,
   `*Content` or `*Model` classes (ViewModels excluded). Report generation is a compile-task input so reports are always
   regenerated. The CI static job runs it and uploads the reports. Accepted exceptions live in
   `config/compose/stability-exceptions.txt`, each with a reason (currently the Glance widget click `Intent`).
3. **Recomposition tests** — `RecompositionCounter` in `:core:ui-testing` counts executions of one composable through
   the Compose runtime's trace hooks (no test hooks in production code). `DayCell` must recompose once per model change,
   skip unrelated state and equal models, and moving the month-grid selection must recompose only the two affected
   cells.
4. **Memory** — LeakCanary runs in debug builds only (`debugImplementation`). Robolectric tests check that
   `MainActivity` is garbage-collected after it is destroyed and that process-lifetime objects are built from the
   Application context. The §9 month-screen budget (< 80 MB RSS) is a macrobenchmark with `MemoryUsageMetric`, checked
   by `compare_benchmarks.py --budgets benchmark/budgets.json` on a device or emulator. Existing caches are bounded by
   construction (astronomy LRU caches with fixed capacity, one-day map cache, paints cached per painter instance).
   Anything that outlives a screen (alarms, PendingIntents, process-lifetime watchers) is created from the Application
   context: the first leak test found `MainActivity` retained through the day-change alarm's PendingIntent, which was
   built with the Activity as its context (traced with a Shark heap analysis; fixed in `DayChangeAlarm`). LeakCanary
   brings two debug-only exported activities (allowlisted per ADR-0017) and legacy storage permissions, which the
   debug manifest removes.

## Consequences

- A new unstable parameter or UI model fails CI with the exact function or class named.
- Adding a core type to the stability list is a reviewed change; a wrong entry would hide missed recompositions, so
  entries must stay immutable value types.
- The heap budget is only enforced where a device runs the benchmarks; local and Robolectric runs cover leaks, not RSS.
