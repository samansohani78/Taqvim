# Provenance Register

Required by `docs/PLAN.md` §0.1 (clean-room policy). Every core algorithm, dataset, and
golden fixture added to this repository must have an entry here **in the same commit** that
introduces it.

## Attestation

By adding an entry, the author attests that the implementation was written solely from the
listed public references and `docs/PLAN.md`, and that **no** source code, data file, string
resource, icon, or test fixture from any GPL/LGPL/AGPL/MPL project was fetched, read, quoted,
paraphrased, or recalled. Explicitly forbidden inputs include (non-exhaustive):
`persian-calendar/*` (all repositories), `avianey/Level`, `ilius/starcal`, and any
Persian-calendar or prayer-times GPL/LGPL library.

## Entry template

```markdown
### <ID> — <Component>
- **Module / files:** `core/<module>/src/main/kotlin/...`
- **Task:** T-xxx
- **Spec:** docs/PLAN.md §6 <A-xx>
- **References used (public only):**
  1. <Author, Title, Publisher/Journal, Year, URL, retrieved YYYY-MM-DD>
- **Validation oracle:** <library/table + license>
- **Deviations from spec:** <none | ADR-xxxx>
- **Author / date:** <name, YYYY-MM-DD>
- **Reviewer attestation:** <name, YYYY-MM-DD — "no forbidden sources consulted">
```

## Algorithms

_No entries yet._

## Datasets

_No entries yet._

## Golden fixtures

_No entries yet._
