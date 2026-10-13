# Interface strings flagged for a native speaker (2026-10-10)

Every machine-translated language already carries `MT: needs review` at the top of each of its `strings.xml`, so
nothing listed here is claimed as reviewed. This file exists so a reviewer who has an hour, not a week, knows
which strings to spend it on: these are the ones where a specific doubt was raised, not a general one.

A line stays here until a native speaker of that language has looked at it. Removing a line means someone did.

## az — Azerbaijani (Latin)

| Key | Current | Doubt |
|---|---|---|
| `settings_category_cultural` | `Mədəni münasibətlər` | `münasibət` carries both "occasion" and "relation". `mədəni münasibətlər` is the ordinary collocation for *cultural relations* in the diplomatic sense, which is not what this setting means — it switches cultural **occasions** on and off. Raised by an independent review. Not changed here: "occasion" has more than one defensible Azerbaijani rendering and picking one without the language is how the problem was created. |
| `settings_category_religious` | `Dini mərasimlər` | `mərasim` is a ceremony or rite. The English is "Religious observances", which includes days that are observed without a ceremony. Possibly narrower than intended. |

The other three in that group read naturally against the English: `Milli günlər`, `Beynəlxalq günlər`,
`Astronomik hadisələr`.

## The calendar files page (2026-10-13, all 22 machine-translated languages)

T-1003's import and export page reached the interface on 2026-10-13 (ADR-0054), and its 21 strings were written in
English and Persian and machine-translated into the other 22 languages in the same pass. They have had no native
reading at all — less than the rest of the file, which at least went through the 2026-09-17 generation and the
structural checks since. The structural checks do pass for them (placeholders, punctuation, bidi, same-as-source),
so what is open is wording, not mechanics.

The two to look at first in any language are `settings_ics_files_replace` ("Update events already here") and its
detail, because they describe what happens to an event the user already has, and a reader who takes them the wrong
way round will overwrite something. `settings_ics_files_import_heading` is listed in
`config/i18n/same-as-source.txt` for Malay and Uzbek, where "Import" is the ordinary word.

## The notification-permission warning (2026-10-13, all 22 machine-translated languages)

`shared_notifications_blocked` and `shared_notifications_allow` in `:core:ui` are shown by four screens when the
system will not display this app's notifications. They were written in English and machine-translated in the same
pass, so they have had no native reading. The sentence has to carry one idea precisely — *the feature works, but
it cannot alert you* — and a translation that turns it into *the feature is off* would be wrong in a way the
structural checks cannot see.

## Read by a model, not by a speaker (2026-10-13)

Before v1.3.0 every string of the calendar files page, the two privacy answers and the two notification strings
above were read again in all 24 languages, by a model, against the English. Twelve were changed:

| Language | Key | Was | Why |
|---|---|---|---|
| ta | `settings_ics_files_import_detail` | `…பாதிக்கப்படா.` | truncated negative verb; `பாதிக்கப்படாது` |
| ta | `settings_ics_files_export_detail` | `…எழுதப்படா.` | same |
| hi | `settings_ics_files_intro` | `अपने अपने कार्यक्रम` | duplicated word |
| ps | `settings_ics_files_intro` | `خپل پېښې` | `پېښې` is feminine plural: `خپلې` |
| ps | `settings_ics_files_import_detail` | `…نه اغېزمن کېږي` | the negation particle belongs before the verb |
| ps | `settings_ics_files_replace_detail` | `کله چې مړ وي` | `مړ` is "dead"; a switch is `بند` |
| ku | `settings_ics_files_import_detail` | `bûyerên we yên xwe` | double possessive |
| ku | `settings_ics_files_export_detail` | `Bûyerên we yên xwe` | double possessive |
| tg | `settings_ics_files_replace` | `дар ин ҷобударо` | two words run together |
| ja | `settings_ics_files_replace_detail` | `ファイルが重ねた…` | `重ねる` is to stack, not to duplicate |
| ckb | `about_faq_privacy_a` | `لەسەر هەمان ئامێر` | "the same device" where the English says "this device" |
| ckb | `about_faq_privacy_a` | `پاڵپشت ناکرێت` | `پاڵپشتی` is support, not backup |

**This is not a native review and does not remove anything from this file.** A model reading its own output finds
the mechanical faults above and is exactly the wrong instrument for the ones that matter — register, whether a
word means to a speaker what the dictionary says, whether a sentence sounds like a warning or like a setting. The
`MT: needs review` header stays on all 22 files, and the two groups above stay open.

## Every other machine-translated language

No specific doubt has been raised, which is not the same as being right. The structural checks (placeholders,
plural categories, punctuation, bidi controls, same-as-source) pass for all of them, and those catch the mechanical
faults, not the wrong word.

## What was checked and found sound

- **fa Persian** — reviewed in full for the event-settings group on 2026-10-10. The wording is the owner's:
  `روزهای ملی`, `مناسبت‌های مذهبی`, `مناسبت‌های فرهنگی`, `روزهای جهانی`, `رویدادهای نجومی`. These read as
  occasions, not as the names of internal categories, which is the point of the group.
- **en English** — reviewed in full for the same group.
- **Structural validation of all 460 locale files across 20 modules** — 0 duplicate keys, 0 keys with no source
  string, 0 parse errors, and the existing `TranslationKonsistTest` rules pass.

## Corrected, not flagged

`settings_islamic_override_intro` said in every language that Hijri dates are "computed from the crescent
visibility rule". Only one of the five Islamic calculations the app offers works that way
(`CALCULATED_OBSERVATIONAL`, a Yallop-criterion estimate), and it is not the default: Persian users get
`IRAN_OFFICIAL`, which is published official months on top of the tabular calendar, and others get
`UMM_AL_QURA`. The Persian wording was worse than the English — `رؤیت هلال` reads as an actual sighting.

English and Persian now say the dates are calculated with the method you choose, which is true for all five and
is what the setting is about. **All 22 remaining languages were refreshed from the corrected English on
2026-10-12**, before v1.3.0: only the first clause changed in each, and the sentence that follows it is still the
wording the original translator chose. The replacements are machine translations like the files that hold them —
they are a factual correction, not a claim of review, and every one of those files still carries its
`MT: needs review` header.
