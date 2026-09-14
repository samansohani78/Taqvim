# ADR-0007: Launch languages and per-language defaults

- **Status:** Accepted. The language list was chosen by the owner on 2026-09-13.
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-200 ("`LanguageSpec` records for 24 languages"), T-204, T-1702

## Context

The plan requires 24 languages but does not name them. The owner chose the "regional + global" set: Taqvim's
home markets (Iran, Afghanistan, Kurdish-speaking regions, South Asia, Nepal, Central Asia) plus widely used
global languages. Each language needs locale conventions (script, direction, week, date layout, AM/PM,
"and" pattern, month names) and a few product defaults.

## Decision

### 1. Languages (presentation order)

| Code | Language | CLDR locale used for conventions |
|---|---|---|
| `fa` | Persian (Iran) | `fa-IR` |
| `prs` | Dari | `fa-AF` |
| `ps` | Pashto | `ps-AF` |
| `ar` | Arabic | `ar-SA` |
| `ckb` | Central Kurdish (Sorani) | `ckb-IQ` |
| `kmr` | Northern Kurdish (Kurmanji) | `ku-TR` |
| `az` | Azerbaijani (Latin) | `az-Latn-AZ` |
| `tr` | Turkish | `tr-TR` |
| `ur` | Urdu | `ur-PK` |
| `ne` | Nepali | `ne-NP` |
| `hi` | Hindi | `hi-IN` |
| `ta` | Tamil | `ta-IN` |
| `bn` | Bengali | `bn-BD` |
| `tg` | Tajik | `tg-TJ` |
| `uz` | Uzbek (Latin) | `uz-Latn-UZ` |
| `en` | English | `en-US` |
| `ru` | Russian | `ru-RU` |
| `de` | German | `de-DE` |
| `fr` | French | `fr-FR` |
| `es` | Spanish | `es-ES` |
| `id` | Indonesian | `id-ID` |
| `ms` | Malay | `ms-MY` |
| `zh` | Chinese (Simplified) | `zh-Hans-CN` |
| `ja` | Japanese | `ja-JP` |

Azerbaijani uses the Latin script because CLDR 48 has no usable `az-Arab-IR` data (ICU falls back to English).

### 2. Locale conventions come from CLDR

Native name, script, direction, week start, weekend, short date pattern (field order, separator, zero padding),
AM/PM, the two-item "and" list pattern, and standalone wide month names for the Gregorian, Persian and Islamic
calendars are taken from Unicode CLDR 48 through ICU4J 78.3's public API. They are stored in the generated resource
`core/i18n/src/main/resources/ir/taqvim/core/i18n/languages.properties`, and `LanguageTableCldrOracleTest`
re-checks every value against ICU4J. ICU4J stays test-only.

Where CLDR has no real localized data (the generic or English fallback), the value is left **absent**, never
guessed, and recorded in docs/DATA_TODO.md: the "and" pattern for `ckb`; Persian month names for `ckb`, `kmr`,
`ne`, `id`, `ms`, `zh`; Islamic month names for `ckb`, `ne`, `zh`; and Nepali (Bikram Sambat) month names for
every language (CLDR has no such calendar).

### 3. Product defaults (user-changeable)

These are product choices for a sensible first-run experience. They make no claim about which convention any
authority or community uses, and each one can be changed in settings (T-1500).

| Default | Rule |
|---|---|
| Numerals | Native digits where Taqvim supports them and the script's everyday use favors them: `fa`, `prs`, `ps` → Persian; `ar`, `ckb` → Eastern Arabic; `ne` → Devanagari. Every other language → Latin. |
| Calendar order | Persian first for `fa`, `prs`, `ps`; Islamic first for `ar`; Nepali first for `ne`; Gregorian first otherwise. Persian is also offered for `ckb`, `kmr`, `az`, `tg` and `en`; Islamic for every language except `ne`. |
| Prayer method | `fa` → TEHRAN; `prs`, `ps`, `ur`, `hi`, `ta`, `bn` → KARACHI; `ar` → MAKKAH; `az` → JAFARI; `ru` → RUSSIA; `fr` → FRANCE; `id`, `ms` → SINGAPORE; all others → MWL. |
| Asr convention | HANAFI for `prs`, `ps`, `tr`, `ur`, `hi`, `bn`, `tg`, `uz`; STANDARD otherwise. |

Once a location is known, the prayer method may be re-suggested from it (T-1502); the language default only
applies before that.

## Consequences

- Adding or removing a language requires amending this ADR, regenerating `languages.properties`, and updating
  the snapshot `core/i18n/src/test/resources/snapshots/language-table.json`.
- Translations for these languages come through Weblate (docs/i18n/STRINGS.md); only `fa` must be complete.
- Gaps in §2 are tracked in docs/DATA_TODO.md and asserted exactly by `LanguageTableTest`, so filling a gap must
  update that test.

## Addendum (2026-09-14): default event sources

Once Afghanistan's official holidays joined the dataset (D-03), the T-305/T-1500 default of "every source except
ancient Iranian festivals" showed Afghan public holidays as holidays to every user, including in Iran. The default is
now a product default per language, in the spirit of §3:

| Default | Rule |
|---|---|
| Event sources | International days for every language, plus the national official holidays of the language's main country: `fa` → IRAN_OFFICIAL; `prs`, `ps` → AFGHANISTAN_OFFICIAL; `ne` → NEPAL_OFFICIAL; no national source otherwise. Ancient Iranian festivals are off for everyone (PLAN §5.1). |

- A day is marked a holiday only by enabled sources (`HolidayCalendar`, T-303); personal events are unaffected.
- `AppSettings.defaultEventSources` is the single definition, used by the first-run preferences, the stored
  preferences, backups and `EventsSettings`.
- The choice is explicit: `AppSettingsProto.event_sources_chosen` (field 15) is set once the user changes the sources
  in the settings. Until then the sources follow the current language, so a language change updates them. Stores and
  backups written before the flag ignore their stored list and take the language default; they came only from
  development builds of the same day, so no released user choice is lost.
