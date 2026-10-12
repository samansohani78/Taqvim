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
