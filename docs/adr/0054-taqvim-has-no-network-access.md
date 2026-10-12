# ADR-0054: Taqvim has no network access

- **Status:** Accepted
- **Date:** 2026-10-13
- **Plan reference:** docs/PLAN.md T-1003 (subscriptions withdrawn), T-1804, T-1503; ADR-0017 (manifest audit),
  ADR-0020 (dataset updates ship with releases), ADR-0032 (restore gate), ADR-0051 (the device calendar)

## Context

Remote iCalendar subscriptions (T-1003) were the only feature in Taqvim that opened a socket. A user added an
`https:` or `webcal:` address, a WorkManager job fetched it periodically through `HttpURLConnection`, and the
expanded occurrences were cached in `ics_events_cache` and shown beside Taqvim's own events on the calendar, day
details, agenda, timeline, search and the event widgets.

The feature was already hedged about. It needed `INTERNET`, which WorkManager extended with `ACCESS_NETWORK_STATE`
for its constraints. It was off twice over: until the user added a feed, and until they also turned on
`subscriptions_network_allowed`, which defaulted to false. It carried a URL validator, a redirect check, a size
cap, conditional requests, a failure-code vocabulary, a striped lock, a health page and a retry policy — roughly
1 700 lines of production code and its tests, for a feature whose data the app could not verify and whose address
list said where the user's attention was.

The cost that mattered was not the code. It was the permission. `INTERNET` cannot be scoped: a dependency that
wants to phone home, an analytics library added by accident, a crash reporter in a transitive upgrade — all of them
work silently as long as the app holds it. While the permission is in the manifest, "Taqvim does not send your data
anywhere" is a promise about what the code happens to do today, checked by reading the code. Without it, it is a
property the operating system enforces on every line of every dependency, now and after every future change.

## Decision

**Taqvim requests no network permission.** `INTERNET` and `ACCESS_NETWORK_STATE` are removed from every manifest,
and no module may declare either again.

Remote subscriptions are removed with them: the fetcher, the refresher, the worker and its scheduler, the
subscription settings page and its health view, the network opt-in preference, the subscription event source, and
the `ics_subscriptions` and `ics_events_cache` tables.

WorkManager goes too. It was there only to schedule the refresh, and it is what brought `ACCESS_NETWORK_STATE`.
Taqvim's remaining background work is one `AlarmManager` alarm, which needs neither.

**Local iCalendar import and export stay.** They are the half of T-1003 that never needed the network: the user
picks a file through the Storage Access Framework and Taqvim reads or writes that document. This is now the only
way an `.ics` file reaches the app, and it reaches it because the user handed it over.

**Opening a citation is not network access.** Tapping a source link starts an `ACTION_VIEW` intent, and the user's
browser — a different app, with its own permission — fetches the page. Taqvim passes a URL to the system and
learns nothing about what happens next. This distinction is stated wherever the claim is made, because "no network
access" would otherwise be read as "no link ever leaves the app".

## Consequences

**Users who had subscriptions.** Their feeds stop being listed and their cached events stop being shown. Schema
migration 9 → 10 drops both tables rather than orphaning them: a copy of somebody else's calendar that nothing can
ever refresh is only a liability, and the subscription rows held the feed URLs. Personal events are untouched —
they live in `personal_events`, which the migration does not read. An `.ics` file is still the way to bring an
external calendar in; it is now a file instead of a URL.

Backups made by an earlier version still restore. `BackupDocument` reads with `ignoreUnknownKeys`, so the
`icsSubscriptions` array and the `subscriptionsNetworkAllowed` flag in an old file are ignored rather than rejected,
and everything else in it is restored as before. New backups do not write either field.

The stored preference is retired by reserving its proto tag (`reserved 8`) instead of deleting the line, so the tag
can never be reused and an older install's stored value is dropped on read.

**Stale background work is cancelled, not left to expire.** JobScheduler keeps a job across an app update, and
WorkManager is no longer in the APK to cancel its own. `StaleWork.cancelAll` runs from `DayChangeReceiver` on
`MY_PACKAGE_REPLACED` and `BOOT_COMPLETED` and cancels every pending job the app owns. That is safe to state
absolutely: after this change Taqvim schedules no job of any kind, so any job found belongs to the removed feature.

**The pipeline gets smaller again.** `EventsRepository` reads one input instead of two, so a day assembly no longer
combines two flows and waits for both. `DayEvents` loses `ics`, `EventInputs` loses a source, `DayEventKind`,
`AgendaEventKind`, `TimelineEventKind` and `SearchEventKind` each lose a case, and the month grid's indicator
palette loses a colour. Search reads the bundled dataset and the user's own rows, and nothing else.

**It is enforced, not just done.** `SecurityKonsistTest` fails if any source manifest requests `INTERNET` or
`ACCESS_NETWORK_STATE`, and separately if any main Kotlin source names an HTTP client type, so a fetch path cannot
be written and left dormant waiting for someone to add the permission back. `auditReleasePermissions` checks the
merged **release** manifest against `app/src/test/resources/security/requested-permissions.txt`, which must never
list either. All of it has planted-violation coverage, so none of it can pass by reading nothing.

**What this does not claim.** It does not mean nothing the user does can reach the internet. They can open a
citation in their browser, pick a document from a cloud provider through the Storage Access Framework, or export a
backup into one. Each is an app they chose, started deliberately, and can see. The claim is about Taqvim's own
process: it has no permission to open a socket, so it does not.

**What it costs.** Subscribed calendars no longer refresh themselves. A user who tracked a team or a sports feed
must now export an `.ics` file and import it, and do it again when the feed changes. That is a real loss of
convenience, accepted deliberately: the feature was opt-in twice and little used, and keeping it meant keeping a
permission that weakens every other privacy claim the app makes. Dataset updates were already unaffected — they
ship with releases (ADR-0020), never over the air.
