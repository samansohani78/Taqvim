# String Resources Plan (T-204)

Source of truth for how user-facing text is written, translated and verified. Plan references:
docs/PLAN.md T-204 (resources), T-1702 (translations), §quality bar ("no hard-coded user-facing text").

## 1. Rules

1. **All UI text lives in Android string resources.** Kotlin and Compose code never contain user-facing literals;
   enforced by the custom lint rules `HardcodedComposeText` and `NoHardcodedNonLatinText` (T-004).
2. **Source locale is English** (`res/values/`). **Persian (`res/values-fa/`) is mandatory and must be 100 %
   complete at every commit**, stricter than the plan's "before v1", so that `fa` never drifts.
3. Other languages are optional per commit and are filled through Weblate (§4). Missing strings fall back to
   English at runtime.
4. Strings belong to the module that displays them (`feature/*/src/main/res`, `core/ui` for shared components).
   The app module only holds app-level strings (name, launcher shortcuts).

## 2. Authoring conventions

| Topic | Convention |
|---|---|
| Names | `snake_case`, prefixed by the screen or component: `month_title_today`, `settings_language_title` |
| Placeholders | Positional only (`%1$s`, `%2$d`); never build sentences by concatenation, since word order differs in RTL languages |
| Plurals | `<plurals>` with CLDR categories; the category set per language comes from CLDR plural rules (T-202) |
| Numbers and dates | Never embed digits in strings; format with `:core:i18n` (`Numerals`, date formatting) and pass as `%1$s` |
| Translator context | An XML comment directly above each non-obvious string: where it appears and its length limit |
| Non-translatable | `translatable="false"` only for identifiers, units defined by standards, or brand names |
| Direction | No LRM/RLM characters in resources; bidi isolation is applied by the UI layer |
| Accessibility | Content descriptions are separate strings (`*_cd` suffix), never reused visible labels |

## 3. Automated gates

| Gate | What it checks | Where |
|---|---|---|
| `MissingFarsiTranslation` (custom lint, error) | Every translatable `string` / `plurals` / `string-array` in `values` exists in `values-fa`, per module | `lint/…/MissingFarsiTranslationDetector.kt`; runs in `./gradlew lint` |
| `FarsiTranslationKonsistTest` | Repository-wide `fa` completeness = 100 % with no Android toolchain, including modules without lint | `konsist/…/FarsiTranslationKonsistTest.kt`; runs in `:konsist:test` |
| `TranslationKonsistTest` (T-1702) | Placeholders, CLDR plural categories, copies of English, ASCII punctuation in Arabic-script text, bidi controls — every language | `konsist/…/TranslationKonsistTest.kt`; runs in `:konsist:test`; details in [TRANSLATING.md](TRANSLATING.md) |
| Completeness report (T-1702) | Translated strings per language and module; translator-comment coverage | `./gradlew :konsist:translationReport` |
| Pseudo-locales | `en-XA` (accented, about 30 % longer) and `ar-XB` (mirrored RTL) in debug builds reveal clipped text and hard-coded direction | `app/build.gradle.kts` (`isPseudoLocalesEnabled` on `debug`) |
| Screenshot matrix | RTL × font scale 1.0/1.3/2.0 screenshots for each screen (T-005, T-1701) | Roborazzi |

## 4. Weblate project

Weblate is an external hosted service. The project is created by the repository owner; the settings below are
the intended configuration and are not applied from this repository.

| Setting | Value |
|---|---|
| Project | `taqvim` |
| Components | one per module that has resources: `app`, `wear`, `core-ui`, `feature-<name>` |
| File format | Android String Resource (monolingual) |
| Monolingual base file | `<module>/src/main/res/values/strings.xml` |
| File mask | `<module>/src/main/res/values-*/strings.xml` |
| Source language | English |
| Repository access | Push translations to a `weblate/translations` branch; merged by PR so every CI gate runs |
| Checks | Enable "Same as source", "Mismatched placeholders" (`%1$s`), "Plurals", "Maximum length" |
| License shown to translators | Proprietary, contributions under the contributor agreement |

Persian translations may be edited in Weblate, but because `fa` must be complete at every commit, new source
strings are added together with their Persian translation in the same PR.

## 5. Supported locales

The 24 launch languages (docs/PLAN.md T-200) and their folder qualifiers are defined together with the
`LanguageSpec` table in T-200. Per-app language selection (Android 13+ `LocaleManager`, `locales_config`) is
implemented in T-1501.
