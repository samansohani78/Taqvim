# Dataset translation debt — 2026-10-10

Baseline: clean commit `a57ad1e20357b67534b26735018d690d1affb869`. All counts concern the same 329 events. Coverage is presence of a target-language title, **not** a quality score or human sign-off.

English and Persian reference coverage increased to 329/329. Missing target titles now fall back to English, except Dari/Persian which prefer one another. Original titles, citations, event IDs, dates, holiday flags and existing review markers are preserved. Forty-six Persian UN translations were corrected; their previous text is retained in the provenance ledger.

## Before / after by language

Each locale has 1,509/1,509 translatable UI keys (100% structural coverage). Ten non-translatable Android keys are excluded. No missing/fallback-only UI keys were found. UI wording is not thereby certified.

| Locale | Dataset before | Dataset after | Coverage | Missing / fallback | Source verified | AI generated | Native review marked | Legacy unclassified |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| en | 280 | 329 | 100.0% | 0 | 0 | 49 | 327 | 20 |
| fa | 299 | 329 | 100.0% | 0 | 0 | 76 | 260 | 224 |
| prs | 10 | 22 | 6.69% | 307 | 0 | 12 | 22 | 0 |
| ps | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| ar | 97 | 231 | 70.21% | 98 | 176 | 32 | 32 | 23 |
| ckb | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| kmr | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| az | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| tr | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| ur | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| ne | 30 | 43 | 13.07% | 286 | 0 | 13 | 43 | 0 |
| hi | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| ta | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| bn | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| tg | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| uz | 0 | 18 | 5.47% | 311 | 0 | 18 | 18 | 0 |
| ru | 98 | 243 | 73.86% | 86 | 181 | 32 | 32 | 30 |
| de | 0 | 32 | 9.73% | 297 | 0 | 32 | 32 | 0 |
| fr | 99 | 231 | 70.21% | 98 | 174 | 32 | 32 | 25 |
| es | 95 | 230 | 69.91% | 99 | 171 | 32 | 32 | 27 |
| id | 0 | 32 | 9.73% | 297 | 0 | 32 | 32 | 0 |
| ms | 0 | 32 | 9.73% | 297 | 0 | 32 | 32 | 0 |
| zh | 99 | 204 | 62.01% | 125 | 97 | 32 | 32 | 75 |
| ja | 0 | 32 | 9.73% | 297 | 0 | 32 | 32 | 0 |

Native-review counts include existing `titleReview` and explicit companion metadata. Legacy titles — those already in the dataset before this pass — now carry an explicit `legacy-unclassified` entry in the provenance ledger rather than being inferred from a missing one, so the audit checks their recorded text like any other entry and every one of the 2,188 title-language pairs is described. Unclassified means exactly that: this pass did not assess them, and they are not assumed human reviewed. Source verification is an AI match against an authoritative target-language source, not human review. Source-original means inherited canonical wording, not a newly verified quotation. All record-level `reviewedBy` values remain unchanged.

## By dataset

| Dataset | Events | English before → after | Persian before → after | All target titles before → after |
|---|---:|---:|---:|---:|
| dataset/afghanistan/afghanistan-official-holidays.json | 10 | 9 → 10 | 10 → 10 | 29 → 156 |
| dataset/ancient-iran/ancient-iranian-festivals.json | 2 | 0 → 2 | 2 → 2 | 2 → 4 |
| dataset/international/un-international-days.json | 246 | 246 → 246 | 246 → 246 | 980 → 1471 |
| dataset/iran/iran-official-holidays.json | 27 | 11 → 27 | 27 → 27 | 38 → 208 |
| dataset/nepal/nepal-official-holidays.json | 30 | 0 → 30 | 0 → 30 | 30 → 195 |
| dataset/religious/christian-movable-feasts.json | 8 | 8 → 8 | 8 → 8 | 16 → 88 |
| dataset/religious/jewish-observances.json | 6 | 6 → 6 | 6 → 6 | 12 → 66 |

## Explicit fallback

`LocalizedText.forLanguage`: exact requested tag → normalized base language (Android `in` → `id`, `ku` → `kmr`, `fa-AF` → `prs`) → Persian/Dari sibling → English → Persian → Nepali. The final two are the schema’s guaranteed original-language candidates. Sorani and Kurmanji do not fall back to each other. English is the shared reference for unrelated languages, not an assertion that it is an official title.

The former fallback was requested language → Persian → Nepali. It could show Persian in English and Nepali in Persian. All shipped events now have English and Persian; the final original-language branches remain for imported/constructed texts without those references.

## Reproduction and provenance

```sh
python3 tools/i18n/localization_audit.py --output docs/i18n/localization-after.json
python3 -m unittest discover -s tools/i18n
./gradlew :konsist:translationReport :tools:dataset:validate :core:events:test :data:events:test
```

[`localization-before.json`](localization-before.json) freezes the pre-edit inventory. [`localization-after.json`](localization-after.json) includes every event/title, missing target language, per-language fallback counts, UI leakage candidates and review counts. [`ui-review-changes.json`](ui-review-changes.json) records exact UI changes. [`title-provenance.json`](../../config/i18n/title-provenance.json) records title origin, previous text when corrected, sources, AI review and outstanding native review.

AI translations are permitted. Missing target titles remain visible debt, not a requirement to invent an official translation. The reference fallback makes them readable while culturally sensitive/local spellings receive further editorial review. Filling a title does not close its review debt.
