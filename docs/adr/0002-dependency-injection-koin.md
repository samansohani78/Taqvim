# ADR-0002: Dependency injection with Koin

- **Status:** Accepted (fixed decision)
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §3.2 (`:app` — DI graph (Koin)), §3.3

## Context

Features must not depend on `:data:*` implementations; something in `:app` has to bind
`core` repository interfaces to `data` implementations. Candidates: Hilt/Dagger (compile-time,
annotation processing, Android-centric), Koin (runtime DSL, Kotlin-first, KMP-capable), manual DI.

## Decision

Use **Koin** (Apache-2.0), versions aligned through `io.insert-koin:koin-bom`.

- `:app` owns `startKoin` and the module list; each `:data:*` and `:feature:*` module exposes one
  top-level immutable `val xxxModule = module { … }`.
- ViewModels are obtained with `koinViewModel()` in Compose.
- `:core:*` modules never reference Koin; they use constructor injection only, which keeps them
  KMP-ready and framework-free.
- A `koin-test` `verify()` test in `:app` checks the whole graph at unit-test time, compensating for
  the lack of compile-time graph validation.

## Consequences

- No KSP/kapt for DI → faster builds; runtime resolution errors are caught by the graph verification
  test instead of the compiler.
- Koin's global context is framework-owned state, not application global mutable state; application
  code must not hold its own singletons outside Koin.
