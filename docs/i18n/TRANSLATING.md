# Translating Taqvim (T-1702)

How the 24 launch languages (ADR-0007) are translated, checked and signed off. Authoring rules for English source
strings are in [STRINGS.md](STRINGS.md).

## 1. Who translates

| Language | Translated by | Status |
|---|---|---|
| English (`en`) | Source language, written with the code | Complete |
| Persian (`fa`) | Added in the same change as each English string (STRINGS.md §1) | Complete; native reviewer sign-off pending |
| Dari (`prs`) | Machine translation derived from the reviewed Persian text with Dari vocabulary | Complete, needs review |
| The other 21 languages | Machine translation committed 2026-09-17 (T-1702), then human review through Weblate | Complete, needs review |

Machine translations are committed so that every launch language is usable and the checks in §4 run on real text.
Each machine-translated file starts with

```xml
<!-- MT: needs review — machine translation (<language>) generated 2026-09-17, T-1702; see docs/i18n/TRANSLATING.md. -->
```

A reviewer removes that comment for the file once the language has been read end to end (§2a). Human review of the
machine-translated languages is optional for the release: `fa` and `en` remain the reviewed pair. Language data
derived from Unicode CLDR (month and weekday names, date patterns, plural rules in `:core:i18n`) is generated from
ICU and never edited by translators.

Release criterion (docs/PLAN.md §8): `fa` and `en` 100 % translated, plus an RTL review. The other languages ship
as machine translations marked for review; a language loses its `MT: needs review` marker when its reviewer signs
off in the table above.

## 2. Workflow

0. The component list, file masks and language folders are kept in [`.weblate`](../../.weblate) at the repository
   root (the `wlc` CLI format); the Weblate server settings themselves are in STRINGS.md §4.
1. The owner creates the Weblate project with the settings in STRINGS.md §4 (Hosted Weblate, `https://hosted.weblate.org/projects/taqvim/` <!-- TODO(owner): confirm Weblate instance -->).
2. Translators work in Weblate on the English source files. Weblate pushes a `weblate/translations` branch.
3. A pull request from that branch runs every CI gate below. A language reviewer (the project owner for `fa` and `en`; one volunteer reviewer per other language, recorded in the sign-off table <!-- TODO(owner): confirm reviewers per language -->)
   approves it.
4. After the first complete review of a language, record the sign-off in the table above (date and reviewer).

### 2a. Reviewing a machine-translated language

1. Open the language in Weblate (or edit `res/values-<qualifier>/strings.xml` directly) and read every string
   against the English source and its translator comment.
2. Fix wording, then delete the `<!-- MT: needs review … -->` comment from the top of each file of that language.
3. Record the reviewer and date in the table in §1 and keep the checks in §4 green.

Until a language is reviewed its strings are machine output: they are structurally correct (placeholders, plural
categories, punctuation) but the wording is unverified.

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
