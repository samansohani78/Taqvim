# ADR-0013: iCalendar export of non-Gregorian recurrence, UIDs and subscriptions

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md T-1003 (SAF import/export, WebCal subscriptions refreshed by WorkManager, ETag
  caching), F-05; follows ADR-0011, which left the export of non-Gregorian rules to T-1003

## Context

Personal events can recur in the Persian or Islamic calendar, or use an invalid-day policy other than "skip"
(ADR-0011). The RRULE of RFC 5545 §3.3.10 always counts Gregorian months and years and skips days that do not exist.
RFC 7529 adds an `RSCALE` rule part for other calendars, but few calendar clients support it. A file written by Taqvim
has to show the right dates in other apps, and it should import back into Taqvim without loss or duplicates.

Personal events had no iCalendar identity, so importing a file twice would duplicate every event. Subscriptions stored
only an ETag, and there was no way to tell a "not modified" answer apart from a full download.

## Decision

1. **Export.** A rule is written as an `RRULE` only when RFC 5545 expands it exactly as Taqvim does: Gregorian
   calendar, `SKIP` policy and weeks starting on Monday. Any other rule is written as:
   - **explicit `RDATE` values** after `DTSTART`, with the same value type as `DTSTART`. Rules with COUNT or UNTIL are
     expanded completely. Open-ended rules are expanded for 3 653 days from today, or from the event start if that is
     later. The limit is 1 000 dates either way; both limits are set in `ExportLimits`.
   - **the rule itself** in the non-standard property `X-TAQVIM-RECURRENCE` (RFC 5545 §3.8.8.2). Its parts are
     `CALENDAR`, `FREQ`, `INTERVAL`, `COUNT`, `UNTIL` (a Julian day number), `BYDAY`, `BYMONTHDAY`, `INVALID` and
     `WKST`.

   Other apps show the explicit dates. When Taqvim imports the file, it restores the rule from the property and ignores
   the `RDATE` values. RFC 7529 `RSCALE` is not written.
2. **Import.**
   - An `RRULE` becomes a Gregorian rule. A date-time `UNTIL` of a timed event is converted to its day in the event's
     time zone.
   - `RDATE` values without a Taqvim rule, `EXDATE` values and alarms other than display alarms before the start
     cannot be stored. Each is reported as an `ImportWarning`.
   - Floating and UTC times are placed in the device time zone.
   - Events are matched by UID. A file that repeats a UID keeps its first event.
3. **Schema version 2** (migration 1 → 2):
   - `personal_events.ics_uid` is a nullable, unique column. Export stores a generated UID for events that have none,
     so importing the exported file back replaces events instead of duplicating them.
   - `ics_subscriptions` gains `last_modified` and `last_checked_at_epoch_millis`. `last_fetched_at_epoch_millis` now
     means the last full download.
   - The backup document carries `icsUid`.
4. **Subscriptions.**
   - **URLs:** HTTPS only. `webcal:` and `webcals:` URLs are read as `https:`; `http:` feeds and redirects that change
     the scheme are refused.
   - **Requests:** the platform `HttpURLConnection` is used, with a 5 MiB limit and connect/read timeouts of 15 s/30 s.
   - **Conditional requests:** `If-None-Match` and `If-Modified-Since` are sent only while the last full download is
     less than 7 days old. Feeds are expanded from 31 days before the download to 400 days after it, so a feed that
     keeps answering "not modified" is expanded again weekly and its cached occurrences never run out.
   - **Periodic work:**
     - It runs at the shortest interval among enabled subscriptions, and never more often than every 15 minutes.
     - It needs a connected network and only runs while the caller says network use is allowed.
     - It retries after timeouts, network errors, HTTP 429 and 5xx responses.

## Consequences

- An open-ended Persian or Islamic rule shows only its first ten years in other apps.
- Recurrence overrides (`RECURRENCE-ID`) and `EXDATE` values are not stored for personal events. Subscriptions do
  honour `EXDATE`, but ignore overrides.
- The user preference that allows network use for subscriptions does not exist yet. Settings (T-1500) must add it and
  call `SubscriptionRefreshScheduler.update`.
- `:data:events` depends on WorkManager (Apache-2.0).
