# Getting help with Taqvim

This page says where to report a problem, what to include, and how quickly each kind of problem is handled
(docs/PLAN.md T-1901). Holiday and event data corrections follow the same process, with the extra rules in
[CONTRIBUTING-DATA.md](CONTRIBUTING-DATA.md).

> **Owner defaults.** Values marked with a `TODO(owner)` comment are defaults filled in on 2026-09-17; the owner confirms
> or replaces them before the first public release (list: docs/MANUAL_TEST_CHECKLIST.md, *Owner defaults to confirm*).

## Channels

| Channel | Use it for |
|---|---|
| **In the app:** More → About → *Report a problem* | Crashes and wrong behaviour. The app prepares the report for you (see [Privacy](#privacy-of-reports)). |
| **GitHub issue forms** ([github.com/samansohani78/Taqvim/issues/new/choose](https://github.com/samansohani78/Taqvim/issues/new/choose) <!-- TODO(owner): confirm repository URL -->) | *Bug report*, *Holiday / event data correction*, *Feature request*. Blank issues are disabled. |
| **E-mail** support@taqvim.app <!-- TODO(owner): confirm support address --> | Reports from people without a GitHub account. The in-app report still opens the share sheet, so you choose the app that sends it. |

Reports are read in Persian (`fa`) and English (`en`); other languages are answered on a best-effort basis. <!-- TODO(owner): confirm languages answered -->

## What to include

**A bug or crash**

1. What happened, and what you expected instead.
2. Steps to reproduce, starting from opening the app.
3. App version, device model and Android version. The in-app report fills these in.
4. The anonymized diagnostics from *Report a problem*, if you can.
5. For a wrong date or prayer time: the city or coordinates, the calendar, the prayer method and the date shown.

**A wrong holiday or event**

1. The event and the date shown by Taqvim.
2. The correct date or title.
3. A **primary source** — the official publication, with a page number. Corrections without a primary source cannot be
   accepted, because every record in the dataset must cite one (see [CONTRIBUTING-DATA.md](CONTRIBUTING-DATA.md)).

## Severity and response times

| Severity | Examples | Target |
|---|---|---|
| **P0 — crash or data loss** | The app crashes on start or on a common screen; events, reminders or backups are lost; athan or reminders stop firing for everyone | Fix released within **48 hours** of confirmation |
| **Holiday data error** | An official holiday on the wrong day, missing or wrongly marked as a day off | Corrected within **72 hours** of confirming the primary source |
| **P1 — major feature broken** | A screen unusable for some devices or languages, wrong prayer times for a method | Fix released within **7 days** of confirmation <!-- TODO(owner): confirm P1 target --> |
| **P2 — minor problem** | Layout glitches, untranslated text, small inaccuracies | Fixed in the next scheduled release, within **30 days** <!-- TODO(owner): confirm P2 target --> |
| **Feature request** | New features and improvements | Considered for planning; no fixed time |

The 48-hour and 72-hour targets come from the plan. A report is *confirmed* when it has been reproduced, or when the
primary source for a data correction has been checked. New reports get a first response within **2 working days**.
<!-- TODO(owner): confirm first-response time -->

### Data-only corrections

Data corrections ship with app releases (ADR-0020): Taqvim does not download dataset updates. A confirmed data error
is fixed in the repository's dataset and released as a PATCH version; the 72-hour target applies to publishing that
corrected release.

## Triage

Each new issue carries the `triage` label until it is sorted:

1. **Reproduce or verify.** For data corrections, open the cited source and check the page.
2. **Set the severity** (table above) and replace `triage` with the matching label: `P0`, `P1`, `P2` or `data-error`. <!-- TODO(owner): confirm label names -->
3. **P0 or holiday data error:** start the fix at once; the response time is counted from this step.
4. **Missing information:** ask for it, and close the issue if there is no answer within
   **14 days**. <!-- TODO(owner): confirm stale-issue days -->
5. **Unverifiable data:** add the record to [docs/DATA_TODO.md](docs/DATA_TODO.md) instead of guessing, and link the
   issue.

## Privacy of reports

Taqvim has no analytics or crash-reporting service and never uploads anything on its own. *Report a problem* only
prepares a message; you choose whether to send it, and through which app.

The report contains the app version and build, device model, Android version, app language, and at most the 200 most
recent diagnostics entries. Before diagnostics are shown, copied, shared or reported, the app removes:

- the values of sensitive fields such as titles, names, notes, places, coordinates, addresses, links, passphrases and
  tokens;
- quoted text (for example event titles);
- e-mail addresses, and the path and query of links;
- coordinate digits beyond two decimals, and long runs of digits (phone numbers, identifiers).

Backup files and passphrases are never part of a report. Please check the text before sending it, and remove anything
else you do not want to share.
