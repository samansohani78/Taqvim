## Summary

<!-- What does this change and why? Reference the task ID from docs/PLAN.md, e.g. T-102. -->

Task: T-

## Checklist

- [ ] Conventional commit title including the task ID
- [ ] `./gradlew spotlessCheck detekt lint konsistTest licenseCheck test` is green locally
- [ ] Tests listed in the plan for this task are added (unit / property / golden / Robolectric / screenshot / UI / benchmark)
- [ ] Golden fixtures carry a provenance header (source, URL, retrieval date)
- [ ] `docs/PROVENANCE.md` updated for any new algorithm or dataset
- [ ] `docs/PROGRESS.md` updated
- [ ] No new dependency, or its license is on the allow-list (ADR-0003)

## Clean-room attestation

- [ ] I did not consult any GPL/LGPL/AGPL/MPL source (including `persian-calendar/*`, `avianey/Level`, `ilius/starcal`) while writing this change.
