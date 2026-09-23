# UN international days — sourcing pass (R07 / D-05, 2026-09-23)

Review finding R07: the dataset ships 102 of the 150 UN international days PLAN D-05 asks for. This is a fresh
sourcing pass against **official UN-family domains only** (un.org observance pages, UN General Assembly resolution
documents, and UNESCO/WHO/FAO official pages for the days each owns), five days after the previous exhaustive pass
recorded in `docs/DATA_TODO.md` DT-019 (2026-09-18). No record was invented, copied from an aggregator, or
machine-translated into Persian; the rules below are the ones this pass could and could not act on.

## Method

1. Re-fetched `https://www.un.org/en/observances/list-days-weeks` (2026-09-23) and listed every day and week shown.
2. Diffed that list (day-only entries; the dataset has no week rule, per the existing tsv note) against the 102
   titles already in `dataset/international/un-international-days.json` and the 130 titles already catalogued in
   `docs/data-todo/un-days-without-persian-title.tsv`.
3. For the 130 already-catalogued days, re-checked whether iran.un.org has published anything new since the
   2026-09-18 pass: its most recent posts (through 2026-09-21, the newest on the site as of 2026-09-23) were message
   pages for International Day of Peace, International Day for the Preservation of the Ozone Layer, and UN Day for
   South-South Cooperation — all three are days already shipped in the dataset (`un.peace-day`... — checked by id),
   so none of them close a gap. Targeted searches for several of the highest-profile still-open days (Equal Pay Day,
   World Cleanup Day, International Day of Sign Languages, World Patient Safety Day, World Hepatitis Day, World Bee
   Day) turned up only third-party Iranian news coverage, never an iran.un.org, unic-ir.org or FAO/WHO Persian
   (`/fa/`) page. **No new Persian source was found for any of the 130 already-catalogued days.**
4. The diff found exactly 3 titles on the current UN list that are in neither the dataset nor the to-do file. Each
   is documented below.

## Already covered (102 shipped, unchanged)

All 102 records in `dataset/international/un-international-days.json` still match a current entry on the UN list
(name and date checked); nothing was renamed or discontinued since the last pass. No changes made.

## Already catalogued, still without a Persian source (130 → 132)

The 130 rows of `docs/data-todo/un-days-without-persian-title.tsv` remain accurate; two more days were added to that
file by this pass (see below), bringing it to 132. Per the owner's standing rule and the dataset's own test
(`UnInternationalDaysTest`, "days without a primary Persian title are listed in docs/DATA_TODO.md, not in the
dataset"), none of these can enter `un-international-days.json` until an official Persian-language UN source is
found — that would have to be a new UNIC Tehran or United Nations in Iran publication, since the reachable Persian
corpus (576 iran.un.org pages, the 937-article unic-ir.org archive) was already read in full on 2026-09-18 and named
none of them, and staying re-checked here on 2026-09-23 with no change.

## Newly identified (not previously on either list)

Three days were proclaimed too recently to have appeared in the previous pass. None could be added to the dataset.

### World Turkic Language Family Day (15 December)

- Proclaimed by UNESCO's 43rd General Conference (Samarkand, 2025), document `43 C/57`:
  https://unesdoc.unesco.org/ark:/48223/pf0000396088
- Date commemorates the 1893 decipherment of the Orkhon inscriptions.
- Blocked: UNESCO has not yet published a dedicated `unesco.org/en/days/...` observance page (checked
  `https://www.unesco.org/en/days` 2026-09-23 — not listed among current or upcoming days), and no Persian-language
  UN source exists for a day this new.
- Added to `docs/data-todo/un-days-without-persian-title.tsv` (12-15) citing the UNESCO document, since the blocker
  is the missing Persian title, same as the other 130 rows.

### International Day of Recognition for Women Searchers of Missing Persons (19 December)

- Adopted by the UN General Assembly in September 2026 (Syria-sponsored), document `A/RES/80/301` (draft
  `A/80/L.108`): https://docs.un.org/en/A/RES/80/301 (the UN's document viewer returns only its JavaScript shell to
  a plain fetch, matching the HTTP 403/PDF-viewer behavior already logged for un.org/docs.un.org in DT-012 and
  DT-019; the resolution's existence and date are independently confirmed by UN News
  https://news.un.org/en/story/2026/09/1168264 and UN Meetings Coverage https://press.un.org/en/2026/ga12775.doc.htm).
- Blocked: no dedicated `un.org/en/observances/...` page yet, and — being a matter of weeks old — no Persian source.
- Added to `docs/data-todo/un-days-without-persian-title.tsv` (12-19) citing the resolution document.

### Vesak, the Day of the Full Moon (day of the full moon in May)

- UN GA resolution 54/115 (1999): https://www.un.org/en/observances/vesak-day
- **Not a Persian-title gap** — its blocker is that it has no fixed Gregorian date; the UN's own wording is "the day
  of the full moon in the month of May", which needs an astronomical rule, not a Fixed/NthWeekdayOfMonth rule.
- The schema already defines an `Astronomical` rule with a `FULL_MOON` kind (`dataset/events.v1.json`), but it is
  unused anywhere in the dataset today — no record, golden test, or documented timezone/tie-break convention exists
  for it (unlike the equinox-based rules, which have the Tehran-meridian convention and USNO goldens from DT-012).
  Guessing a timezone (e.g. UTC) or a tie-break rule for the rare year with two May full moons (or none) would be
  inventing a convention the UN never states, which the review's rules forbid.
- Tracked as `docs/DATA_TODO.md` DT-040 instead of the Persian-title tsv, pending an owner decision on the
  convention. Not added to either the dataset or the to-do tsv (it would misrepresent it as a Persian-sourcing gap).

## Result of this pass

- 0 records added to `dataset/international/un-international-days.json` — every day this pass could newly identify
  either lacks a Persian source (the dataset's own enforced rule) or lacks a schema-expressible rule with a stated
  convention (Vesak).
- Dataset stays at **102 of the ≥150 target** set by PLAN D-05.
- `docs/data-todo/un-days-without-persian-title.tsv`: 130 → **132** rows (added World Turkic Language Family Day and
  International Day of Recognition for Women Searchers of Missing Persons).
- `docs/DATA_TODO.md`: DT-019 updated to 132 and this pass's date; new DT-040 opened for the Vesak rule-type gap.
- Full reconciliation: 102 shipped + 132 Persian-title gap + 1 rule-type gap (Vesak) = 235 unique day titles, which
  is every day title on the current UN list (236 raw list entries; "World Migratory Bird Day" is listed twice, for
  its May and October dates — the May occurrence is the shipped rule, per DT-024, and the October occurrence is
  already tracked there, not duplicated here).

## Resolution (2026-09-23 addendum, ADR-0042)

The owner decided the same day: a missing *Persian title* should not keep a well-sourced day out of the dataset. All
132 rows above (every one of them a `Fixed` month/day; none needed a weekday-ordinal rule) are now records in
`dataset/international/un-international-days.json`, each keeping its official citation from this pass and carrying
a machine-translated Persian title marked `titleReview: ["fa"]` — the dataset's equivalent of the app's
`MT: needs review` string marker (docs/i18n/TRANSLATING.md). See ADR-0042 for the schema/validator change and the
policy; `docs/data-todo/un-days-without-persian-title.tsv` is now the review queue for these 132 titles, not a list
of days excluded from the app. Vesak is unaffected by this change: its blocker (DT-040) is a missing rule convention,
not a missing Persian title, and it remains out of the dataset. The dataset now ships **234** UN international days,
past the ≥150 target of D-05.
