# ADR-0031: One rule decides which days an event is shown on

- **Status:** Accepted
- **Date:** 2026-09-16
- **Plan reference:** docs/PLAN.md T-305 (events repository), T-602 (device calendar), T-804 (search), T-900
  (timeline), T-1003 (iCalendar import); docs/CODE_REVIEW_2026-09-15.md findings B03 and B12

## Context

Events reach the screens from four sources: the dataset, personal events, device calendars and iCalendar feeds. The
device and feed paths already dated rows with `DeviceEventMapping.days`: all-day rows by their UTC dates (both stores
keep UTC-midnight bounds) and timed rows by the days their instants fall on in the device zone. Two paths did not:

- **B03:** personal events were filed under the dates the user typed, in the event's own zone. A meeting at 00:30 in
  Tokyo belongs to 15:30 UTC the previous day, but was filed under the Tokyo date. The timeline then converted the
  time and clipped it to that day, so the meeting disappeared from both days. The repository also read personal
  events only for the shown days, so a conversion into the range from a neighbouring date was never read.
- **B12:** search dated device and feed rows by their start instant in the device zone, ignoring `allDay`. In a zone
  behind UTC, an all-day event on 15 September showed as 14 September, unlike the calendar.

Imported iCalendar series in UTC are now stored with the zone `UTC` (main@7d38d7a), so their stored dates are UTC
dates and must be moved into the display zone the same way.

## Decision

`EventDays` (`:data:events`) states the rule once, and every path uses it or `DeviceEventMapping.days`, which it
delegates to:

1. **All-day events keep their dates** in every display zone: dataset and personal events by their own dates, device
   and feed rows by their UTC dates.
2. **Timed events are dated by their instants.** The wall-clock start and end are read in the event's own zone (an
   unknown zone falls back to the display zone), and the occurrence covers the days that interval touches in the
   display zone. The end is exclusive, so an event ending at midnight stays on its day, and a zero-length event keeps
   the day it starts on.
3. **Recurrence stays in the event's own calendar and zone.** Occurrences are expanded first, and overrides and
   exceptions are applied by their own dates; only then is each occurrence dated by rule 2.
4. **Sources are read one day wider** than the shown range (`EventDays.WIDENING`), because offsets up to ±14 h can
   move an occurrence into the range from a neighbouring date. Results are narrowed again by the assigned days.
5. **Search uses the same dates** and navigates to the first shown day that is not before today, so an event that
   started earlier and still continues opens on today.

The personal occurrence keeps its own dates and wall-clock times: the timeline rebuilds the instants from them, and
the editor shows them as the user entered them.

## Consequences

- The calendar, timeline, widgets, agenda and search agree on an event's day in every zone.
- Personal events are read over two extra days per request; the query is indexed by date, so the cost is small.
- A device zone change is picked up when a day flow is collected again, as for the other sources.
- Reminders are scheduled from instants and are not affected.
