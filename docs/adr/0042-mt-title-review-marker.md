# ADR-0042: A missing Persian source no longer excludes a well-sourced UN day; titles are machine-translated and marked for review

- **Status:** Accepted
- **Date:** 2026-09-23
- **Plan reference:** docs/PLAN.md §5 D-05; REVIEW R07; ADR-0007; docs/i18n/TRANSLATING.md

## Context

REVIEW finding R07 / PLAN D-05 requires at least 150 UN international days; the dataset shipped 102. A 2026-09-18
pass and a 2026-09-23 re-check (`docs/data-todo/un-days-sourcing.md`) found 132 further days on the UN's own list
(`https://www.un.org/en/observances/list-days-weeks`), each with an official un.org, UNESCO, WHO, FAO or UN General
Assembly citation for its existence and date. None of them could be added, because the repository's standing rule
(`UnInternationalDaysTest`, "days without a primary Persian title are listed in docs/DATA_TODO.md, not in the
dataset") required a **primary Persian-language UN source** for the title specifically, and the reachable Persian
corpus — 576 pages of iran.un.org and the 937-article Internet Archive copy of unic-ir.org, both read in full — names
none of these 132 days.

This blocked D-05 on a title-language technicality, not on sourcing: the day, its date and its existence were never
in doubt. The app already ships every other language (`docs/i18n/TRANSLATING.md`, T-1702) with machine-translated
strings marked `<!-- MT: needs review -->`, reviewed later through Weblate; `fa` and `en` are the only pair held to
a stricter "translated at authoring time" standard, and that standard exists for the *app's UI strings*, not for a
dataset citation.

## Decision

1. **A missing Persian title no longer excludes a well-sourced day.** A record may enter
   `dataset/international/un-international-days.json` on the strength of its official (English-language) citation
   alone. The day's date and existence still need a primary source, exactly as before; only the requirement that the
   *title itself* be corroborated by a Persian-language UN source is dropped.
2. **Its Persian title is machine-translated from the official English name**, by whoever adds the record (an agent
   or a translator), following the same practice already approved for the app's 22 machine-translated languages.
3. **Schema marker: `titleReview`.** `dataset/events.v1.json` gains an optional array field on every event,
   `titleReview: [languageTag, ...]`, naming the languages of `title` whose text is machine-translated and not yet
   reviewed — the dataset's equivalent of the app's `MT: needs review` string comment. A reviewer removes the
   language's tag from the array once its text has been checked against a primary source (a native Persian
   publication, for `fa`).
4. **Validator enforcement (`SemanticChecks`, `IssueKind.TITLE_REVIEW_UNKNOWN_LANGUAGE`).** Every tag in
   `titleReview` must name a language actually present in the record's `title`; a tag for a language the title
   doesn't carry is a dataset error, not a silent no-op.
5. **`UnInternationalDaysTest` is relaxed accordingly.** A record with `source`/`category` `INTERNATIONAL`, on the UN
   list, needs either a citation from `unic-ir.org` / `iran.un.org` (a primary Persian source, as before) **or**
   `"fa" in titleReview` (a marked machine translation). A record with neither is still rejected. A second test
   pins the count of `fa`-marked records (132, this pass) and checks each carries both an English and a Persian
   title.
6. **132 days added.** Every day of the 2026-09-18/2026-09-23 sourcing passes that can be expressed as a rule is
   added as a `Fixed` (month, day) record: none of the 132 needed a weekday-ordinal rule. Each carries the UN list
   citation, its specific observance/resolution citation, a `links` entry (`un` for an observance page, `resolution`
   for a citation that is itself a resolution or proclamation document), `titleReview: ["fa"]`, and a machine-translated
   Persian title. `validity.fromYear` is set only for the two days whose proclamation year the sourcing pass already
   established (World Turkic Language Family Day, 2025; International Day of Recognition for Women Searchers of
   Missing Persons, 2026); researching a proclamation year for the other 130 is out of scope of this pass, matching
   the existing 102 records, none of which carry `validity` either.
7. **Vesak stays out.** Its blocker is not a missing Persian title but a missing rule convention (DT-040: "the day of
   the full moon in May" needs a timezone and a two-full-moons tie-break the UN never states); this ADR does not
   change that.
8. **`docs/data-todo/un-days-without-persian-title.tsv` becomes a review queue.** It no longer lists days excluded
   from the dataset; it lists the 132 shipped ids whose `fa` title is machine-translated, for a human reviewer (or a
   future Persian source) to check off one at a time. `docs/DATA_TODO.md` DT-019 is reworded to match: it stays open
   until every queued id has a native Persian title and its `titleReview` tag is removed.

## Consequences

- The dataset ships 234 UN international days (102 natively Persian-sourced, 132 machine-translated and marked),
  past the ≥150 target of D-05.
- `EventsCodeGenerator` does not read `titleReview`: like `reviewedBy`, it is dataset-review metadata, not part of
  the runtime `EventDefinition` the app displays. Nothing in `:core:events` or `:data:events` changes.
- A future record in *any* dataset file (not just UN days) may use the same marker for the same reason: an
  officially sourced day or holiday whose title needs translating rather than transcribing from a primary source in
  that language.
- Reviewing a title means editing its text in place and deleting its tag from `titleReview` (and its row from the
  tsv) — never adding a second, "reviewed" copy of the record.
- This does not relax sourcing for the *date* of a record: a day whose date itself is undocumented, or whose date
  needs a rule convention the source never states (Vesak), still cannot be added.
