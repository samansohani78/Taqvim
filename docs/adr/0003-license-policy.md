# ADR-0003: Proprietary license, permissive-only dependencies, clean-room policy

- **Status:** Accepted (fixed decision)
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §0.1

## Decision

### Project license
Taqvim is **proprietary, All Rights Reserved** (`LICENSE`). Every Kotlin source file carries the
copyright header enforced by Spotless. Relicensing to MIT/Apache-2.0 later must remain possible, so
nothing in the dependency graph may impose copyleft obligations.

### Dependency allow-list
Allowed licenses (SPDX), per dependency scope:

| License | runtime | build tooling | test |
|---|---|---|---|
| Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, Unicode-3.0 / Unicode-DFS-2016 (ICU), CC0-1.0, Public Domain | ✅ | ✅ | ✅ |
| EPL-2.0 | ❌ | ❌ | ✅ |

Everything else — GPL, LGPL, AGPL, MPL, CDDL, EPL in runtime, unknown or missing license — fails the
build. This table is about **dependencies**; data files bundled with the app follow the separate `dataLicenses`
section of the same file (ADR-0039), which also admits CC BY 4.0 — for data only, never for a dependency. The machine-readable allow-list is `config/license/allowed-licenses.json`; the gate is the
`licenseCheck` task (T-001), which scans every resolved dependency (direct and transitive) of every
module and configuration.

When a module declares several licenses, it passes if **at least one** declared license is allowed
for its scope (dual licensing lets us choose). Modules whose metadata lacks license information may
only be admitted through an explicit override entry with an evidence URL; overrides are reviewed like
code.

### Third-party repositories
Only Google Maven, Maven Central and the Gradle Plugin Portal are used, plus JitPack restricted by
exclusive content filtering to `io.github.cosinekitty` (the MIT-licensed astronomy engine, published
only there).

### Clean-room rules
- Never fetch, read, quote, paraphrase or recall code, data, strings, icons or fixtures from any
  GPL/LGPL/AGPL/MPL project. Explicitly forbidden: `persian-calendar/*` (all repositories),
  `avianey/Level`, `ilius/starcal`, and any Persian-calendar or prayer-times GPL/LGPL library.
- Algorithms are implemented only from the specifications in docs/PLAN.md §6 and the public
  references listed there; each gets a `docs/PROVENANCE.md` entry in the same commit.
- Holiday/event data is compiled from primary official sources with a citation on every record.
  Unverifiable records are left out and logged in `docs/DATA_TODO.md`.
- Launcher icons and artwork are drawn from scratch for this project.

## Consequences

- Adding a dependency may require a license override with evidence; the gate makes the cost visible.
- Test-only tooling that ships with a weak-copyleft license is limited to what the allow-list admits;
  exceptions require a new ADR.
