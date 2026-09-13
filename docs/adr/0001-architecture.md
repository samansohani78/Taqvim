# ADR-0001: Layered multi-module architecture

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §3

## Context

Taqvim targets phone, tablet, foldable and Wear OS, and plans a JVM CLI / KMP build of the domain
(F-17). The domain (calendar arithmetic, prayer times, astronomy, events rule engine) is large,
algorithm-heavy and must be tested to ≥ 95 % line coverage without an Android runtime.

## Decision

1. Three layers, dependencies pointing inward:
   `feature/*` → `core/*` (domain) ← `data/*`; `:app` wires everything with DI.
2. `:core:*` domain modules are pure Kotlin/JVM (`taqvim.jvm.library`) with strict explicit-API
   mode. They must not import `android.*` (Konsist rule, T-002). They are KMP-ready: no JVM-only APIs
   beyond `kotlin.*`/`kotlinx.*` without an `expect`-able seam.
3. `:data:*` implement `core` repository interfaces using Android APIs.
4. `:feature:*` contain Compose UI + ViewModels and depend only on `:core:*` (never on other features
   or `:data:*`); enforced by Konsist.
5. Unidirectional data flow: immutable `UiState` data class + sealed `UiAction` + one-shot effects
   channel; ViewModels expose `StateFlow<*UiState>`.
6. Gradle convention plugins in the `build-logic` included build define every module type
   (`taqvim.jvm.library`, `taqvim.android.library`, `taqvim.android.compose`, `taqvim.android.feature`,
   `taqvim.android.application`, `taqvim.android.test`, `taqvim.lint.checks`, `taqvim.root`).
   Module build files contain only dependencies and module-specific settings.
7. All versions live in `gradle/libs.versions.toml`; configuration cache, build cache and parallel
   configuration are on.

## Consequences

- Domain logic is testable with fast JVM tests; Android modules only need Robolectric at the edges.
- The module count (~45) raises configuration cost; convention plugins and configuration cache keep
  the cold CI build within the T-000 budget.
- Android-bound modules under the `core` namespace (`:core:ui`, `:core:ui-testing`) are exceptions
  to rule 2 — see ADR-0004.
