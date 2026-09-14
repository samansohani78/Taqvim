# ADR-0022: Dynamic launcher icon through activity aliases

- **Status:** Accepted
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md T-1214 (launcher icon showing today's day number; "toggled with warning; disabled by
  default"; R test of the component state after a day change), T-1213/T-1215 (shared daily refresh and shortcuts)

## Context

Android has no API for an app to change its launcher icon at run time. The only portable mechanism is to declare
several launcher entries (`<activity-alias>` with `MAIN`/`LAUNCHER`) with different icons and enable exactly one with
`PackageManager.setComponentEnabledSetting`. Launchers react to such changes in different ways: many move the new entry
to the app drawer or the end of a home screen page, some drop the home screen icon, and some cache icons. Disabling the
component a dynamic shortcut is attached to removes that shortcut. A component change also stops the app's process
unless `DONT_KILL_APP` is passed.

## Decision

1. **Entries.** `MainActivity` keeps the `taqvim://` link filter but no longer has the launcher filter. The manifest
   declares `LauncherDefault` (enabled, the normal icon) and `LauncherDay01`…`LauncherDay31` (disabled, each with an
   adaptive icon showing that number on the Taqvim calendar page). Every alias targets `MainActivity`, is exported (a
   launcher entry must be) and is listed in the T-1804 allowlist.
2. **Icons.** `tools/icons/launcher_day_icons.py` generates the 31 adaptive icons and the aliases. Digits are stroked
   vector paths designed for Taqvim: Latin digits in `drawable/`, Persian digits in `drawable-fa/`. Monochrome (themed)
   icons use the same foreground.
3. **Off by default, with a warning.** The `dynamicLauncherIcon` setting (T-1500) is off by default. Turning it on asks
   for confirmation with a warning that the launcher may move or remove the icon and its shortcuts; turning it off is
   immediate and restores the default entry.
4. **Switching.** `LauncherIconSwitcher` compares each entry's effective state (the manifest default counts) and only
   changes what differs, enabling the new entry before disabling the others, always with `DONT_KILL_APP`. With the
   setting off nothing is ever written, so users who never turn it on see no package changes.
5. **When.** `LauncherIconRefresh` is a `DailyRefresh` run by the shared `DailyRefreshCoordinator` (T-1213): at the local
   day change (one inexact alarm, re-armed after reboot, update and clock, zone or locale changes) and whenever a
   relevant preference changes. The day is the day of the month in the user's primary calendar. Days outside 1…31 (a
   32nd day in some calendars) show the default icon.
6. **Shortcuts.** Launcher shortcuts (T-1215) open `taqvim://` links resolved by `MainActivity`, never an alias. They are
   attached to the enabled entry and published again after every switch.

## Consequences

- Where the launcher supports it, the icon shows today's day number. Accepted limits, stated in the settings warning:
  - after a switch, some launchers move the icon, drop it from the home screen or show the old icon until they refresh;
  - pinned shortcuts may be disabled by the system when their entry is disabled (dynamic ones are republished);
  - the switch lands shortly after midnight, not exactly at it (inexact alarm, no foreground service).
- The digits follow the system locale, which the launcher uses to load resources, not the in-app language: Persian
  digits appear only when the system language is Persian.
- The number of launcher-related manifest entries grows by 32; the release manifest audit and `ExportedComponentsTest`
  check each one. A future change of the icon design regenerates all 93 files through the script.

## Alternatives considered

- **A single icon with a changing badge or widget:** a notification badge cannot show an arbitrary number, and a widget
  is a different feature (T-1201).
- **Always on:** rejected because of the launcher side effects above; the PLAN asks for off by default.
- **Switching at every app start instead of an alarm:** the icon would stay stale for days on devices where Taqvim is
  rarely opened.
