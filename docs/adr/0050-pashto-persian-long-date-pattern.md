# ADR-0050: Pashto writes Persian-calendar dates with its own CLDR Gregorian pattern

- **Status:** Accepted
- **Date:** 2026-09-26
- **Plan reference:** docs/PLAN.md T-202; ADR-0014 (which this extends); DT-008, DT-025

## Context

ADR-0014 gave `fa` and `prs` the language's own CLDR Gregorian full pattern for Persian-calendar dates, because CLDR
stores only the root fallback for that pair. It deliberately stopped there:

> `ps`, `ckb` and `ne` have only CLDR's root fallback `G y MMMM d, EEEE` for the Persian and Islamic calendars. They
> are not changed here; the gap is recorded in docs/DATA_TODO.md until a primary source for written dates exists.

That condition has now been met for `ps`. Seven covers of Afghanistan's Official Gazette (رسمي جریده), spanning
1393–1401 SH and both the Islamic Republic and the Islamic Emirate, were read for DT-008. Every one writes its solar
date as a genitive formula running **year → month → day**, with `د` before the year and before the month, and with no
weekday:

```
د خپرېدو نېټه: د ۱۴۰۱ هـ.ش کال د لړم د میاشتې (۱۴)
```

Until this decision the app rendered `ps` Persian-calendar long dates as `هـ.ش ۱۴۰۵ وږی ۲۲, يونۍ`. Three things are
wrong with that, and none of them is a matter of taste:

1. a **Latin comma** in a Pashto date — the same defect ADR-0014 was written to remove from `fa` and `prs`;
2. **year before month before day with the weekday last**, contradicted both by the gazette and by CLDR's own reviewed
   `ps` Gregorian pattern;
3. the **era first**, where the gazette puts it immediately after the year.

CLDR's `ps` Gregorian full pattern, `EEEE د y د MMMM d`, is real reviewed data in the language, uses the `د` genitives,
and orders the fields year → month → day exactly as the gazette does.

## Decision

1. **`ps` joins `fa` and `prs`**: its Persian-calendar full pattern is its own CLDR Gregorian full pattern. `ps`
   Persian long dates now read `يونۍ د ۱۴۰۵ د وږی ۲۲`.
2. **The override is a rule, not a stored pattern.** `FormatTable.PRODUCT_DATE_PATTERNS` (a map of pairs to literal
   pattern strings) becomes `FormatTable.GREGORIAN_PATTERN_OVERRIDES`, a set of language/calendar pairs; the pattern
   is read from the language's own Gregorian entry at load time. The pattern text is therefore never copied into
   Kotlin. This also removes a latent problem: `ps`'s pattern contains non-Latin characters, which the
   `NoHardcodedNonLatinText` rule forbids in production sources, so the old map could not have expressed it at all.
3. **The Pashto era is not shown in Persian-calendar long dates**, for ADR-0014's stated reason — the solar Hijri
   calendar is Afghanistan's default civil calendar, as the Persian calendar is Iran's. This is the same treatment
   `prs`, Afghanistan's other official language, has had since ADR-0014.
4. **The Islamic calendar is not changed.** `ps` keeps CLDR's root pattern there and still shows `هـ.ق`. An Afghan
   Islamic-calendar date needs its era precisely because two calendars are in official use — which is why the gazette
   prints both on every cover — and no source gives a renderable Pashto pattern for it. That stays in DT-025.

## Consequences

- One line of the T-202 snapshot changes; no other language moves.
- The `ps` era sourced in DT-008 still renders, in Islamic-calendar dates, and remains available in `eras` for any
  caller that wants it. Point 3 is the one debatable part of this decision and is the part to revisit first if the
  owner disagrees: showing `هـ.ش` in Pashto solar dates is a one-line change to the pattern rule.
- `ckb` and `ne` are still on the root fallback. ADR-0014's condition has not been met for them: no primary source for
  a written date in either language was found (DT-025).
- The gazette corroborates **field order**, not a pattern. Its Pashto line is a labelled genitive formula, and the
  Pashto and Dari lines on the same cover disagree on field order, so nothing here renders the gazette's own wording.
