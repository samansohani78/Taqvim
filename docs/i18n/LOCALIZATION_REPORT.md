# Localization implementation report — 2026-10-10

Work began from clean HEAD `a57ad1e20357b67534b26735018d690d1affb869`. This is a localization pass, not a general engineering audit. No algorithms, official dates, holiday flags, source IDs, event visibility, signing or release settings were changed.

## A. Persian

- Corrected tithi from a twelfth of a turn to **12°** of lunar elongation; the implementation already used 12°. Its traditional Surya Siddhanta computation is no longer presented as identical to every modern Nepali calendar.
- Updated obsolete FAQ claims that Nepali calendars and widgets were unavailable, and removed the claim that only Persian source titles can be displayed.
- Standardized جست‌وجو, نقشهٔ جهان, بیکرم سمبت, فاز ماه, کار شیفتی and برنامه‌های روز کاری. Backup terminology now matches the workday/shift screens. Religious categories retain مناسبت‌های مذهبی and the established category wording.
- Removed the misleading “official” from the Iranian calculated Hijri method label; official overrides retain their explicit source/status text. No method computation changed.
- Wrote coordinate bounds with منفی / مثبت instead of ambiguous RTL minus-sign placement; isolated each three-date widget line using the existing bidi utility.
- Added Persian reference titles to all 30 Nepali records; corrected 46 Persian UN titles (12 meaning changes, the remainder spacing). Caste discrimination, political descriptions and religious honorifics were not silently neutralized. Nepali transliterations remain flagged for native/editorial review.

## B. English

Added the 49 missing reference titles: 16 Iranian, two ancient Iranian, one Afghan and 30 Nepali. Preserved the wording’s political qualifiers and religious honorifics rather than replacing them with an invented neutral holiday. Corrected the four FAQ/astronomy explanations, “Nearest latitude with a twilight”, the time-zone hint and the misleading “Iran official” label.

## C. Other UI locales

All 24 resource sets were inventoried across 20 modules, including widgets, notifications, accessibility strings, Wear, plurals and arrays. The new audit checks types, duplicate keys, per-item placeholders, plural `other`, inline XML markup, array length, newlines and potential script leakage. Existing Konsist additionally checks CLDR plural categories and source-identical exceptions. No structural completeness claim is a semantic certification.

Every locale received corrected tithi guidance, the three obsolete FAQ answers and the Iranian method label. Azerbaijani **Mədəni münasibətlər** (“cultural relations”) became **Mədəni tədbirlər**. Five Arabic-script Sorani crash-report strings in the Latin-script Kurmanji set were replaced with Kurmanji. Tajik occasion labels that used муносибат (relationship) now use calendar wording such as санаҳои муҳим / санаҳои расмӣ. [Khovar’s official events calendar](https://khovar.tj/wp-content/uploads/2026/02/Ta-vimi-chorabinihoi-navr-z-.pdf) informed the terminology check; these corrections remain AI-reviewed, not official UI translations. Intentional numeral samples remain permitted.

| Locale | UI edit operations | Remaining uncertainty |
|---|---:|---|
| en | 7 | Human/editorial sign-off is not claimed. |
| fa | 50 | Human/editorial sign-off is not claimed. |
| prs | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ps | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ar | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ckb | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| kmr | 10 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| az | 6 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| tr | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ur | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ne | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| hi | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ta | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| bn | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| tg | 19 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| uz | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ru | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| de | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| fr | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| es | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| id | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ms | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| zh | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |
| ja | 5 | Native review of idiom, accessibility phrasing and religious names remains; MT marker retained. |

The edit ledger classifies corrections as DEFINITELY WRONG, UNNATURAL or TERMINOLOGY INCONSISTENT. Unchanged strings are not automatically LIKELY CORRECT. Low-confidence culturally specific wording remains NEEDS NATIVE REVIEW. The broader MT corpus has structural checking and targeted semantic correction, not a claimed exhaustive native-level review of all 36,216 entries.

## D. Dataset localization

See the per-dataset and per-language before/after tables in [DATASET_TITLE_DEBT.md](DATASET_TITLE_DEBT.md). Every event is listed in the machine-readable after report with its available and missing languages.

- Iran / Ancient Iran: completed English reference names, including the full Yalda hospitality clause and Shia honorifics. Added common Eid/Nowruz/Ashura names across other locales.
- Afghanistan: completed the remaining English title from the original wording; retained Dari originals and political qualifications. No claim that these are newly sourced official English translations.
- Nepal: all 30 now have English and Persian references; preserved all original Nepali names. Festival proper names use established spellings rather than literal English inventions.
- International: fetched six official UN indexes and matched target-language anchor text by the observance URL slug. Added 491 titles and independently matched 308 existing ones; 799 are source-verified. English titles and dates were untouched.
- Christian / Jewish: added 126 names across Arabic, German, French, Spanish, Russian, Chinese, Japanese, Indonesian and Malay. These are AI-generated/AI-reviewed, with references supporting terminology; not all target-language spellings are source-verified. Denominational preferences remain a native/editorial review item.

## E. Important web research

| Source | Decision supported |
|---|---|
| [UN language indexes](https://www.un.org/en/observances/list-days-weeks) ([Arabic](https://www.un.org/ar/observances/list-days-weeks), [Chinese](https://www.un.org/zh/observances/list-days-weeks), [French](https://www.un.org/fr/observances/list-days-weeks), [Russian](https://www.un.org/ru/observances/list-days-weeks), [Spanish](https://www.un.org/es/observances/list-days-weeks)) | Exact official target titles; provenance records source URLs and fetched index SHA-256. |
| [UN Iran: truth and justice](https://iran.un.org/fa/46444-فراخوان-گوترش-برای-حفاظت-از-افراد-حامی-حقیقت-و-عدالت) | حق بر حقیقت, not “accuracy/correctness”. The revised title is an AI rendering, not a claimed verbatim official quotation. |
| [UN solstice day](https://www.un.org/en/observances/solstice-day) | Solstice is not restricted to summer; removed the hemisphere-specific mistranslation. |
| [UNESCO Yalda/Chella](https://ich.unesco.org/en/RL/yalda-chella-01877) | Established name and family/hospitality context; retained the original title’s full clause. |
| [Nepal Tourism Board: Dashain](https://ntb.gov.np/dashain), [festival listings](https://trade.ntb.gov.np/tourist-activities/events-festivals/), [cultural guide](https://trade.ntb.gov.np/wp-content/uploads/2023/04/cultural-treasures-of-nepal-inside.pdf) | Dashain, Tihar, Fagu Purnima/Holi and Lhosar names; no date changes inferred from tourism pages. |
| [Nepal Home Ministry](https://moha.gov.np/en/page/martyrs-and-wounded-of-the-gen-z-movement) | Gen Z martyr terminology, without inferring the observance date. |
| [Al-Islam: Imam Hasan Askari](https://al-islam.org/imamate-and-imams-ibrahim-amini/eleventh-imam-hasan-askari), [Imam Mahdi](https://al-islam.org/fr/imamate-and-imams-ibrahim-amini/twelfth-imam-al-mahdi) | Names and honorific context; translations remain AI-marked. |
| [Azerbaijan culture law](https://frameworks.e-qanun.az/25/c_f_25303.html), [Baku State University](https://bdu.info.az/sosial-fealiyyet/medeniyyet-yaradiciliq-merkezi) | Natural cultural-event terminology: mədəni tədbirlər. |
| [Chabad French holiday names](https://fr.chabad.org/holidays/default_cdo/jewish/holidays.htm), [Spanish Sucot](https://es.chabad.org/library/article_cdo/aid/5361/jewish/Sucot.htm), [Vatican German](https://www.vatican.va/content/paul-vi/de/homilies/1972.html), [Vatican French liturgical directory](https://www.vatican.va/roman_curia/congregations/ccdds/documents/rc_con_ccdds_doc_20020513_vers-direttorio_fr.html) | Established Jewish names and Christian terminology; no invented literal translations of festival names. |
| [Muslim World League](https://www.themwl.org/ar/contact) | Institution identity behind the shared Persian prayer-method wording. |

### Three-date widget rendering

A new RemoteViews test reproduced clipping at the old 110×40 dp minimum: only the first date was visible. Merely widening the widget still clipped Persian/Arabic at 2× font scale. The provider now requests 180×180 dp (3×3 launcher cells), uses the compact 13/10 sp text sizes and permits two lines per date. Each date is directionally isolated. Six measured-bitmap screenshots cover English, Persian with a Latin Gregorian date, and Arabic, at 1× and 2× font scale. They were visually inspected; assertions reject vertical clipping and ellipsis. This is a visible sizing change, not a calendar or data-loading change. Real-launcher placement and migration of already-pinned widgets still need a device.

Generated event files now hold four records rather than eight because expanded translations exceeded the existing 400-line source limit. No generation semantics changed.

## F. Fallback

Old: requested → Persian → Nepali. Final: exact requested → base/Android alias → Persian/Dari sibling → English reference → Persian → Nepali. The choice is deterministic and covered by core unit tests, runtime dataset tests and the inventory’s fallback report. Arabic is not treated as a Persian sibling; Sorani is not treated as Latin-script Kurmanji. Original source titles are never removed.

## G. Provenance

- source-original: **329** title-language entries.
- source-verified: **799** title-language entries.
- ai-generated: **636** title-language entries.
- ai-reviewed: **1435** title-language entries.
- human-reviewed: **0** title-language entries.
- needs-native-review: **1389** title-language entries.
- legacy-unclassified: **424** title-language entries — titles that were in the dataset before this pass and that it did not assess. They now carry an explicit entry in `config/i18n/title-provenance.json` rather than being inferred from a missing one, so the audit checks their recorded text against the dataset like every other entry and the file describes all 2 188 title-language pairs.

Counts overlap: origin and review are separate axes. `source-original` is an inventory classification, not newly verified source wording. There are no new human review claims. All original `reviewedBy`, `titleReview`, citations and MT comments remain. AI review is one model’s semantic/back-translation pass, not an independent human or second-model review.

## H. Verification

Final command results are recorded below after the verification run. Screenshot changes must be visually inspected before acceptance; a successful record operation is not evidence of correctness.

## I. Remaining judgement

- Native/editorial review of the MT UI corpus in the 22 non-reference locales, especially Pashto, Sorani, Kurmanji, Tajik, Tamil and Nepali cultural/religious terminology. Persian and English also have no fabricated human sign-off.
- Editorial choice of Nepali proper-name transliterations in Persian; local/denominational preferences for Christian/Jewish names; retention/abbreviation of honorifics in compact UI. Full titles were retained rather than shortened silently.
- Direct target-language official wording where it is absent from the fetched UN index or inaccessible at an original citation. Remaining title gaps are explicitly counted in the debt report; English fallback is a product choice, not a claim those gaps require a native translator.

## J. Git and scope

No release was tagged, signed or published. The final commit and working-tree status are reported in the delivery message. Dataset invariants are compared against the starting commit, not merely against regenerated code.
