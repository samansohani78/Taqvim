# ADR-0014: Persian-calendar long date pattern for `fa` and `prs`

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-202 (CLDR-based long, numeric and ISO dates for the launch languages)

## Context

T-202 formats long dates with CLDR full date patterns (CLDR 48 via ICU4J 78.3, generated into
`core/i18n/src/main/resources/ir/taqvim/core/i18n/formats.properties`). For the Persian calendar CLDR gives `fa` and
`prs` the pattern `y MMMM d, EEEE`, confirmed against ICU4J `DateFormat.getDateInstance(FULL,
"fa@calendar=persian")`. It renders "۱۴۰۵ شهریور ۲۲, یکشنبه": the year first and a Latin comma. That is not how
Persian dates are written, and `fa` is Taqvim's primary language with the Persian calendar first.

CLDR's own `fa` and `prs` Gregorian full pattern is `EEEE d MMMM y` ("یکشنبه ۲۲ شهریور ۱۴۰۵"). The Islamic pattern is
`EEEE d MMMM y G`, in the same order.

## Decision

- The Persian-calendar LONG pattern of `fa` and `prs` is the language's own CLDR Gregorian full pattern,
  `EEEE d MMMM y`. No era is added, because the Persian calendar is the default calendar for these languages.
- The override lives in code, in `FormatTable.PRODUCT_DATE_PATTERNS`, applied after the generated table is parsed.
  `formats.properties` stays generated CLDR data.
- Every other pattern stays CLDR. The ICU4J oracle test skips exactly the overridden pairs. A test pins the override set
  to {fa, prs} × PERSIAN and checks that each override equals the language's generated Gregorian pattern.
- `ps`, `ckb` and `ne` have only CLDR's root fallback `G y MMMM d, EEEE` for the Persian and Islamic calendars. They are
  not changed here; the gap is recorded in docs/DATA_TODO.md until a primary source for written dates exists.

## Consequences

- Snapshots showing fa/prs Persian long dates change: `date-formats.tsv` and the Times tab screenshots. The T-500
  parser reads the new order.
- A future CLDR update that fixes the `fa` Persian pattern should retire this override. The test that compares the
  generated pattern to `y MMMM d, EEEE` fails when that happens.
