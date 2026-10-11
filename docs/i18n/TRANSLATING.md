# Translating Taqvim (T-1702)

How the 24 launch languages (ADR-0007) are translated, checked and signed off. Authoring rules for English source
strings are in [STRINGS.md](STRINGS.md).

## 1. Who translates

| Language | Translated by | Status |
|---|---|---|
| English (`en`) | Reference language, written with the code; AI corrections in October 2026 | Structurally complete; no human sign-off claimed by the AI pass |
| Persian (`fa`) | Added with English; AI corrections in October 2026 | Structurally complete; native reviewer sign-off pending |
| Dari (`prs`) | Machine translation from Persian, with Dari vocabulary; AI corrections | Structurally complete, needs native review |
| The other 21 languages | Machine translation from T-1702, with later AI corrections; Weblate is the human review workflow | Structurally complete, needs native review |

Machine translations are committed so that every launch language is usable and the checks in §4 run on real text.
Each machine-translated file starts with

```xml
<!-- MT: needs review — machine translation (<language>) generated 2026-09-17, T-1702; see docs/i18n/TRANSLATING.md. -->
```

A named human reviewer may remove that comment after reading the file end to end (§2a). An AI review must retain
it and record its own provenance. `fa` and `en` are the reference pair, not automatically human-reviewed. Language data
derived from Unicode CLDR (month and weekday names, date patterns, plural rules in `:core:i18n`) is generated from
ICU and never edited by translators.

Release criterion (docs/PLAN.md §8): `fa` and `en` 100 % translated, plus an RTL review. The other languages ship
as machine translations marked for review; a language loses its `MT: needs review` marker when its reviewer signs
off in the table above.

## 2. Workflow

0. The component list, file masks and language folders are kept in [`.weblate`](../../.weblate) at the repository
   root (the `wlc` CLI format); the Weblate server settings themselves are in STRINGS.md §4.
1. The owner creates the Weblate project with the settings in STRINGS.md §4 (Hosted Weblate, `https://hosted.weblate.org/projects/taqvim/`).
2. Translators work in Weblate on the English source files. Weblate pushes a `weblate/translations` branch.
3. A pull request from that branch runs every CI gate below. A language reviewer (the project owner for `fa` and `en`; one volunteer reviewer per other language, recorded in the sign-off table)
   approves it.
4. After the first complete review of a language, record the sign-off in the table above (date and reviewer).

The order languages are reviewed in, and how that fits the dataset's own review queue, is in
[../REVIEWING.md](../REVIEWING.md).

### 2a. Reviewing a machine-translated language

1. Open the language in Weblate (or edit `res/values-<qualifier>/strings.xml` directly) and read every string
   against the English source and its translator comment.
2. Fix wording. Only a human signing off the whole file may delete its `<!-- MT: needs review … -->` comment.
3. Record the human reviewer and date in the table in §1, or explicitly record AI review in the change ledger;
   keep the checks in §4 green. AI review is useful but does not replace a human sign-off.

Until a language is signed off its strings remain marked for review. Structural checks cover placeholders, plural
categories and punctuation; they cannot certify natural wording or cultural accuracy.

Translators read the XML comment above each English string (STRINGS.md §2): where it appears, whether it is a noun
or a verb, and what is inserted into placeholders.

## 3. Rules translators must follow

- **Placeholders** such as `%1$s` and `%2$d` must all be kept; their order may change.
- **Plurals** need exactly the CLDR categories of the language: for example `one` and `other` for Persian and
  English, `zero`, `one`, `two`, `few`, `many`, `other` for Arabic, and only `other` for Chinese and Japanese.
- **Search keywords** (`*_keywords` strings) are not shown: they are `|`-separated words users may type. Translate
  each word and keep the separators.
- **Arabic-script languages** (fa, prs, ps, ar, ckb, ur) use `،`, `؛` and `؟`, not ASCII `,` `;` `?`, except inside
  Latin text such as version numbers.
- **No direction marks** (LRM, RLM, embeddings, overrides) in translations; the app isolates mixed-direction text.
  Unicode isolates (U+2066–U+2069) are allowed where a source string already uses them.
- **Same as English** is allowed only for proper names. Such entries are listed with a reason in
  [`config/i18n/same-as-source.txt`](../../config/i18n/same-as-source.txt).

## 4. Automated checks

| Check | Where |
|---|---|
| `fa` complete in every module | `MissingFarsiTranslation` lint rule and `FarsiTranslationKonsistTest` |
| Placeholders match the English source | `TranslationKonsistTest` (`:konsist:test`) |
| Plural categories match the language's CLDR rules | `TranslationKonsistTest` |
| No translation identical to English outside the exception list | `TranslationKonsistTest` |
| ASCII `,` `;` `?` in Arabic-script text | `TranslationKonsistTest` |
| No bidi marks, embeddings or overrides in any resource | `TranslationKonsistTest` |
| Pseudo-locales `en-XA` and `ar-XB` in debug builds | `app/build.gradle.kts` |

The checks look at structure only. Whether a translation is correct and natural is the reviewer's job.

## 5. Completeness report

```bash
./gradlew :konsist:translationReport
```

writes `konsist/build/reports/translations/translations.md` and `translations.json`: translated strings per language
and per module, how many English strings have a translator comment, and how many short strings still lack one. CI
uploads the report from the static analysis job.

The broader inventory includes dataset titles and honest review counts:

```sh
python3 tools/i18n/localization_audit.py --output docs/i18n/localization-after.json
python3 -m unittest discover -s tools/i18n
```

See [LOCALIZATION_REPORT.md](LOCALIZATION_REPORT.md) and [DATASET_TITLE_DEBT.md](DATASET_TITLE_DEBT.md) for the
2026-10-10 before/after inventory. English/Persian UI completeness does not imply dataset completeness in every
language, nor does either imply translation quality.

## 6. Dataset title provenance and fallback

`config/i18n/title-provenance.json` is an editorial companion keyed by event ID and language. It does not alter the
runtime dataset schema or replace existing `reviewedBy`, `titleReview` or citations. Each entry stores the exact
`text` so a later title edit cannot silently inherit a stale review. Corrections may retain `previousText`.

- `origin`: `source-original` (inherited canonical wording), `source-verified` (matched target-language source),
  `ai-generated` (including AI corrections), or `human-translated` (requires a real credited contributor).
- `sourceLanguage`, `sources` and optional `terminologyReferences`: the translation's basis. A terminology
  reference does **not** prove that the complete target title appears there.
- `aiReviewed`: AI semantic/back-translation review, not human sign-off. This pass used one AI reviewer.
- `needsNativeReview`: explicit outstanding language/editorial review, independent of structural validity.
- Optional `humanReviewedBy`: only a real human's recorded sign-off; never set by AI on its own behalf.
- Optional `verification`: source retrieval date, matching method and source index SHA-256. Fetched source pages
  are not bundled into the application.

Absent entries are `legacy-unclassified`, not implicitly source verified or human reviewed. Source-original
classification identifies which wording was inherited; it does not claim this pass independently verified that
wording. Existing record-level review markers remain meaningful and are never cleared merely by filling titles.

`LocalizedText.forLanguage` resolves exact requested tag → base language / Android alias (`in` → `id`, `ku` → `kmr`,
`fa-AF` → `prs`) → Persian/Dari sibling → English → Persian → Nepali. The schema guarantees Persian or Nepali for
the last resort. Related Persian/Dari wording is preferred to English; unrelated languages use the shared English
reference. Sorani and Kurmanji are separate. This replaces the old requested → Persian → Nepali fallback, which
could show Persian in English UI. Proper names may remain untranslated intentionally; their origin still matters.
