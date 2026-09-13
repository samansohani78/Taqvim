# ADR-0012: Room schema versioning and preference defaults

- **Status:** Accepted (the Islamic-variant defaults are provisional)
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-600 (defaults derived from `LanguageSpec` on first run), T-601 (tables §4.3,
  migrations framework, exported schema JSON in VCS)

## Context

PLAN §4.3 names the personal-data tables but not their columns. The plan also asks for exported schema JSON in version
control and migration tests with `MigrationTestHelper`. `LanguageSpec` (ADR-0007) has no field that says which Islamic
calendar variant a language's users expect.

## Decision

1. **Columns** are designed in `:data:database`. Structured values reuse the core types: `RecurrenceRule` from
   `:core:ics` and `WorkdayProfile` from `:core:workdays`, stored through converters with round-trip tests. As a
   result, `:data:database` exposes `:core:ics`, `:core:events` and `:core:workdays` as `api` dependencies.
2. **Schema JSON** is committed under `data/database/schemas/` and read by the host tests from their assets. Every
   version bump must:
   - add the new schema file, which KSP writes *after* test assets are merged, so generate it before running the tests;
   - add a `Migration` to `TaqvimMigrations.ALL`.

   The migration test validates every committed schema up to the latest version. Until version 2 exists, the chain is
   1 → 1.
3. **Default Islamic variant** on first run is `IRAN_OFFICIAL` for `fa` and `UMM_AL_QURA` for every other language.
   This is an application default, not calendar data; users can change it. It stays provisional until the owner
   confirms it or `LanguageSpec` gains a region-based variant (for example for `prs`/`ps` in Afghanistan).

## Consequences

- Schema drift fails the migration test instead of reaching users.
- Changing the per-language default later only affects new installs; existing preferences are kept.
