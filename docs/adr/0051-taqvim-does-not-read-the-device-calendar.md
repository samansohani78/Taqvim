# ADR-0051: Taqvim does not read the device calendar

- **Status:** Accepted
- **Date:** 2026-10-12
- **Plan reference:** docs/PLAN.md T-602 (withdrawn), T-1804, T-1503; ADR-0017 (manifest audit), ADR-0031 (event day
  assignment), ADR-0019 (the watch's manifest)

## Context

T-602 read the user's calendar through `CalendarContract.Instances`, cached the rows in `device_events_cache`, and
showed them beside Taqvim's own events on the calendar, the day details, the agenda, the timeline, search and the
event widgets. It asked for `READ_CALENDAR` at runtime and worked without it, showing nothing.

`READ_CALENDAR` is one of Android's most far-reaching permissions. It is not scoped to a calendar, a date range or a
field: granting it hands over every event in every account on the device — titles, times, locations, attendees,
recurrences, and the accounts themselves — to be read at any time, in the background, for as long as the grant
lasts. For a calendar app it is also the permission a user is least likely to question, because showing their
existing events is exactly what they expect a calendar to do. That combination is the problem: the permission is
easy to obtain, enormous in scope, and impossible for the user to verify the use of.

What Taqvim did with it was narrow. It copied a window of instances into its own database so widgets and search
could read them without touching the provider, and it drew them in a different colour. None of that is worth the
grant, and the copy was the single most sensitive thing the app stored — a duplicate of the user's calendar held
under Taqvim's own retention rules rather than their calendar app's.

## Decision

**Taqvim does not read the device calendar.** `READ_CALENDAR` is removed from every manifest, the provider code and
its cache are deleted, and no module may declare a calendar permission again.

`WRITE_CALENDAR` is not introduced as a replacement. Neither permission may appear.

Interoperability is kept where it needs no access to the user's calendar database:

- **iCalendar import and export** (T-1003) continues unchanged. It is file-based: the user picks a file, or
  subscribes to a URL they typed. Nothing reads the provider.
- **Nothing else is added.** There is no "add to system calendar" action today, and this decision does not create
  one. `ACTION_INSERT` would be a legitimate way to offer that later — the calendar app's own editor opens
  pre-filled and the user confirms, with no permission involved — but adding a user-facing action is a product
  change and not part of a privacy removal.

`ACTION_VIEW` on `CalendarContract.Events.CONTENT_URI` is removed with the rest. It opened a device event in the
calendar app, which required the provider row id, which required having read it.

## Consequences

**Users who had it.** Nothing breaks and nothing is asked. The permission simply stops being requested; Android
leaves an existing grant in place until the user revokes it or the app is reinstalled, and Taqvim never calls the
provider again. The copy of their calendar is deleted by schema migration 8 → 9, which drops `device_events_cache`.
Dropping it in the migration is deliberate: an upgrade removes the copy rather than orphaning it.

**What disappears from the interface.** The device-calendar event kind on the calendar, day details, agenda,
timeline, search and the event widgets, its colour in the month grid, its row on the privacy dashboard, and the
calendar entry in the permissions list. There is no stale setting, because the feature never had one: it was on
whenever the permission was granted.

**The pipeline gets smaller.** `EventsRepository` combines two inputs instead of three, so every day assembly waits
on one fewer flow. `DayEvents` loses a field, `EventInputs` a source, and the widget invalidation set a table. The
`:data:device-calendar` module is deleted; `DeviceTimeZone` moves to `:data:preferences` and the instant-to-civil-day
rules to `:data:events` as `EventWindows`, where the subscription path still needs them.

**It is enforced, not just done.** `SecurityKonsistTest` fails if any source manifest requests a calendar permission
or any main Kotlin source names `CalendarContract`, the provider authority or either permission constant.
`auditReleasePermissions` checks the merged **release** manifest against
`app/src/test/resources/security/requested-permissions.txt`, which must never list one. Both have planted-violation
coverage, so neither can pass by reading nothing.

**What this does not claim.** Taqvim still handles calendar data: the user's own events, and whatever an .ics file
or a subscribed feed contains. "Taqvim does not read your device calendar" is the claim, and it is the one the
privacy screen and docs/SECURITY.md make. "No calendar integration" would be false.
