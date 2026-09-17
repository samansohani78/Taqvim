# Translating Taqvim (T-1702)

How the 24 launch languages (ADR-0007) are translated, checked and signed off. Authoring rules for English source
strings are in [STRINGS.md](STRINGS.md).

## 1. Who translates

| Language | Translated by | Status |
|---|---|---|
| English (`en`) | Source language, written with the code | Complete |
| Persian (`fa`) | Added in the same change as each English string (STRINGS.md §1) | Complete; native reviewer sign-off pending |
| The other 22 languages | Human translators through Weblate (docs/PLAN.md T-1702) | Not started |

No machine translation is committed. Language data derived from Unicode CLDR (month and weekday names, date
patterns, plural rules in `:core:i18n`) is generated from ICU and never edited by translators.

Release criterion (docs/PLAN.md §8): `fa` and `en` 100 % translated, plus an RTL review. Other languages ship when
their translators and reviewers sign off; until then Android falls back to English.

## 2. Workflow

1. The owner creates the Weblate project with the settings in STRINGS.md §4 (Hosted Weblate, `https://hosted.weblate.org/projects/taqvim/` <!-- TODO(owner): confirm Weblate instance -->).
2. Translators work in Weblate on the English source files. Weblate pushes a `weblate/translations` branch.
3. A pull request from that branch runs every CI gate below. A language reviewer (the project owner for `fa` and `en`; one volunteer reviewer per other language, recorded in the sign-off table <!-- TODO(owner): confirm reviewers per language -->)
   approves it.
4. After the first complete review of a language, record the sign-off in the table above (date and reviewer).

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
