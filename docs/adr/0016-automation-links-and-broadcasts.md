# ADR-0016: Automation contract — links, broadcasts and selected text

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md F-14 (documented intents/deep links such as `taqvim://day/1405-01-01` and
  `taqvim://convert?...`, Tasker-compatible broadcasts for athan), T-1103 (`ACTION_ATHAN_STARTED`,
  `ACTION_DAY_CHANGED` documented; deep links), §9 E2E ("deep links (10)", "PROCESS_TEXT detection"); builds on
  ADR-0015 (navigation host)

## Context

Automation apps (Tasker, launchers, notes with links, other calendars) need a stable way to open Taqvim screens and to
react to athans and to the change of day. The plan names the link shape and the two broadcasts but not their extras,
their security properties or how selected text is handled.

## Decision

1. **Links** use the `taqvim` scheme and are handled by `MainActivity` (`singleTop`, `ACTION_VIEW`, `DEFAULT` and
   `BROWSABLE`). A pure parser (`DeepLinks`) maps them onto ADR-0015 destinations:
   `calendar`, `day/<y-m-d>[?calendar=persian|islamic|gregorian]` (Persian by default), `event/<id>`,
   `occasion/<event id>?day=<jdn>`, `convert?date=<text>[&from=<calendar>]`, `times`, `astronomy`, `search?q=<text>`,
   `settings[/<item>]` (including `backup` and `privacy`). Links **only open screens**: none creates, changes or
   deletes data, so a link from a web page cannot act without the user. Dates are checked against their calendar and
   Julian days are limited to Gregorian years 1…9999; texts are limited to 500 characters. Anything unreadable opens
   the calendar.
2. **Broadcasts** are implicit (`Context.sendBroadcast`) so dynamically registered receivers such as Tasker's receive
   them: `ir.taqvim.action.ATHAN_STARTED` (extras `prayer` = `FAJR`…`ISHA`, `time` = epoch milliseconds, `jdn`) when an
   athan starts playing (T-1102 `AthanEventHook`), and `ir.taqvim.action.DAY_CHANGED` (extras `jdn`, `date` = ISO
   Gregorian) shortly after local midnight. The day change comes from an inexact alarm kept by `DayChangeReceiver`
   (re-armed at app start, boot, app update, time and time-zone changes). Names and extras are only ever added to.
3. **Selected text** (`ACTION_PROCESS_TEXT`, "Open in Taqvim") is read with the T-501 detector in the device
   language: the first date opens that day, other text opens the date converter.
4. **Entry parameters** the destinations need were added to the features without cross-feature dependencies:
   calendar initial day, editor draft (day and minutes), astronomy entry dialog, converter text, search query.

## Consequences

- The contract is documented for users in docs/AUTOMATION.md.
- Broadcasts are on for everyone: the plan asks for no switch. An athan broadcast reveals prayer times to apps that
  listen for it; these are public astronomical facts for the chosen place, and the place itself is not included. A
  future setting (T-1500) can turn broadcasts off without changing the contract.
- `DAY_CHANGED` may arrive a few minutes after midnight when the device is idle; exact timing would need the exact-alarm
  permission, which Taqvim reserves for athans and reminders.
- Links carry no signature: they are safe because they only navigate.

## Addendum (2026-09-14)

- `settings/about` opens the About screen (T-1504): version, open-source licenses, data sources, diagnostics and
  problem reports. Like every link it only opens a screen.
