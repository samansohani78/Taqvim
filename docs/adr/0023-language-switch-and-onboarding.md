# ADR-0023: Language switching and first-run onboarding

- **Status:** Accepted
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md T-1501 (per-app language via `LocaleManager` and `localeConfig` with a fallback
  for older APIs; language-derived defaults re-applied for anything not explicitly chosen; skippable onboarding shown
  once), E2E "Install fresh (fa/en/ne/ckb/ar) → defaults; onboarding skip/complete"; ADR-0007 (defaults per language
  and default event sources), ADR-0015 (app navigation), ADR-0017 (security baseline)

## Context

Taqvim derives calendars, numerals, week start, weekend, prayer method, Asr convention, Islamic variant and event
sources from the language (ADR-0007). Before T-1501 these defaults were applied only on the first run, from
`Locale.getDefault().language`, and the settings stored a new language code without touching anything else. The
preferences store has no per-field "chosen" flag except `eventSourcesChosen`. Android 13+ keeps a per-app language
itself (`LocaleManager`); older versions do not.

## Decision

1. **Explicit choices** — when the language changes, a language-derived value (calendars, numerals, week start,
   weekend, prayer method, Asr convention, Islamic variant) counts as *explicitly chosen* when it differs from the
   *current* language's default; it is kept. A value equal to that default takes the new language's default. Event
   sources follow the new language until `eventSourcesChosen` is set (unchanged from ADR-0007). Theme, place, time
   format, adjustments, reminders and every other setting are not language-derived and are never touched. An unknown
   language code switches to the fallback language `en`. `UserPreferences.withLanguage` implements this; the settings
   language item (T-1500) and the onboarding both go through it.
   *Limitation:* without per-field flags, a choice that happens to equal the old language's default is treated as not
   chosen (e.g. a Persian user who picks weekend Saturday–Sunday and then switches from English back to Persian).
   Adding a flag per field was rejected: it needs a proto field and a settings touchpoint for each of seven values, and
   the heuristic gives the expected result whenever the user deliberately differs from the language's convention.
2. **Device language** — the first-run language is the launch language matching the device locale
   (`DeviceLanguages`): language and region first (`fa-AF` → `prs`), then the language code, then the primary language
   of a launch locale tag (`ku` → `kmr`); anything else is `en`.
3. **Per-app language** — `res/xml/locales_config.xml` lists the locale tags of the 24 launch languages (a test
   compares it with `LanguageTable`). The stored language is the source of truth. At process start
   `AppLanguageSync` first adopts a language set outside the app (system per-app language settings) if it maps to a
   launch language, then applies every stored language that the platform does not already show. On Android 13+ it
   sets `LocaleManager.applicationLocales` (the system recreates activities). Before Android 13 the tag is stored in
   private SharedPreferences, `MainActivity.attachBaseContext` wraps its context with that locale and layout direction,
   and the activity recreates itself when a new tag is applied. No AppCompat dependency is added.
4. **Onboarding** — three skippable pages as T-1501 lists them: language, location (the location settings embedded)
   and event sources. Completing, skipping or backing out of the first page sets `onboarding_completed` (proto field
   16; older stores read `false`, so it is shown once). The flag is device-only: backups do not carry it and a restore
   keeps the device's value, since a restored device has already been through its own first run.
5. **Placement** — the onboarding is a gate in `TaqvimAppShell`, not a back-stack destination: until the preferences
   load the shell shows an empty surface, then the onboarding or the navigation frame. `taqvim://` links wait until
   the frame is shown. This keeps the tab bar hidden during onboarding and keeps ADR-0015's rule that the calendar is
   the root of the back stack. Macrobenchmark journeys skip the onboarding when it is shown.

## Consequences

- Changing the language in the settings now changes the defaults that follow it, as the plan requires.
- Tests: all 24 × 24 language switches, a property over the languages that changed values are kept, device-locale
  matching, backup exclusion, the onboarding ViewModel and screen, per-app language on both paths, and a first-run
  matrix (fa/en/ne/ckb/ar) in the running app.
- App UI tests that expect the calendar must mark the onboarding done first (`finishOnboarding` in app tests).
- The onboarding cannot be opened again from the settings; every choice it offers is available there.
