# ADR-0005: Test-scope license findings from the first license-gate scan

- **Status:** Accepted — the EPL-1.0 exception is **pending owner confirmation** because it widens the
  fixed allow-list (ADR-0003).
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §0.1 (dependency gate), T-001

## Context

The first full `licenseCheck` scan (428 external modules across every module's runtime, build and test
classpaths) found three dependencies whose licenses are not on the ADR-0003 allow-list. All three are
**test-scope only**; none reaches an app artifact.

| Dependency | License | Pulled in by |
|---|---|---|
| `junit:junit:4.13.2` | EPL-1.0 | Robolectric, Compose UI Test (`ui-test-junit4`), Macrobenchmark (`benchmark-macro-junit4`), `androidx.test.ext:junit`, lint `lint-tests` |
| `javax.annotation:javax.annotation-api:1.3.2` | CDDL-1.1 OR GPL-2.0 with Classpath Exception | Android test tooling in every Android module |
| `org.jetbrains.intellij.deps:trove4j` | LGPL-2.1 | Konsist's embedded Kotlin compiler (`:konsist` only) |

The fixed rule is: remove non-permissive dependencies and pick an alternative or implement it ourselves.

## Decisions

### 1. `javax.annotation-api` → substituted, no exception
Every module substitutes `javax.annotation:javax.annotation-api` with
`jakarta.annotation:jakarta.annotation-api:1.3.5`. The 1.3.x Jakarta line ships the identical
`javax.annotation` package and is licensed EPL-2.0 OR GPL-2.0 with Classpath Exception; EPL-2.0 is
already allowed for tests. Implemented in `QualityConventionPlugin.substituteNonPermissiveTestDependencies`.

### 2. `trove4j` → excluded, no exception
`:konsist` excludes `org.jetbrains.intellij.deps:trove4j` from its test classpaths. Konsist only parses
sources; the Konsist suite is run without it to prove it is not needed.

### 3. `junit:junit` (EPL-1.0) → test-only exception
There is no alternative: JUnit 4 was never relicensed, and it is a hard transitive requirement of tools
that are themselves fixed decisions (Robolectric, Compose UI Test, Macrobenchmark, Android Lint tests).
Replacing those tools would contradict the fixed tech stack.

EPL-1.0 is the predecessor of EPL-2.0 — which the allow-list already admits for tests — with the same
weak-copyleft model: obligations attach only to distributing modified EPL-licensed code. Test
dependencies are never distributed in Taqvim artifacts.

The allow-list therefore admits **EPL-1.0 in the `test` scope only**. The gate still fails if EPL-1.0
ever appears on a runtime or build classpath.

## Consequences

- `config/license/allowed-licenses.json` gains `{ "id": "EPL-1.0", "scopes": ["test"] }`.
- If the owner rejects the EPL-1.0 exception, the alternatives are to drop Robolectric/Compose UI
  Test/Macrobenchmark (conflicts with the fixed stack) or accept a red license gate; that trade-off needs
  an owner decision.
- New substitutions or exclusions for license reasons must be added here.
