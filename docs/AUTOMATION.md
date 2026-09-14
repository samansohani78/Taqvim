# Automating Taqvim

Taqvim can be opened at a given screen with a `taqvim://` link, reacts to text you select in other apps, and announces
athans and the start of each day to automation apps such as Tasker. The contract is recorded in
[ADR-0016](adr/0016-automation-links-and-broadcasts.md); names and extras are only ever added to, never renamed.

## Links

Links only open screens. They never create, change or delete anything, so it is safe to put them in notes, widgets or
web pages. A link Taqvim cannot read opens the calendar.

| Link | Opens |
|---|---|
| `taqvim://calendar` | The calendar on today |
| `taqvim://day/1405-01-01` | The calendar on 1 Farvardin 1405 (Persian calendar by default) |
| `taqvim://day/2026-03-21?calendar=gregorian` | The calendar on 21 March 2026 |
| `taqvim://day/1447-10-01?calendar=islamic` | The calendar on 1 Shawwal 1447 (Iran's official Islamic calendar) |
| `taqvim://event/42` | Your event number 42 in the event editor |
| `taqvim://occasion/<event id>?day=2461121` | The calendar on that day (used by official-event reminders) |
| `taqvim://convert?date=1405-01-01&from=persian` | The date converter with 1 Farvardin 1405 |
| `taqvim://convert?date=next%20friday` | The date converter with any text it can read |
| `taqvim://times` | Prayer times |
| `taqvim://astronomy` | Astronomy |
| `taqvim://search?q=%D9%86%D9%88%D8%B1%D9%88%D8%B2` | Search for "نوروز" |
| `taqvim://settings` | Settings |
| `taqvim://settings/main-calendar` | Settings, at the main calendar (any settings item, lower case with `-`) |
| `taqvim://settings/backup` | Backup and restore |
| `taqvim://settings/privacy` | The privacy dashboard |

Rules:

- Dates are `year-month-day` with Latin digits; the calendar is `persian` (default), `islamic` or `gregorian`. A date
  that does not exist in its calendar (such as `1405-07-31`) is not opened.
- `day` in `occasion` is a Julian day number; days outside the Gregorian years 1–9999 are ignored.
- Texts (`date`, `q`) are percent-encoded and cut at 500 characters.

Example (Android shell): `adb shell am start -a android.intent.action.VIEW -d "taqvim://day/1405-01-01"`.

## Selected text

Select text in any app and choose **Open in Taqvim** (in Persian: **باز کردن در تقویم**). If the text contains a date,
the calendar opens on the first one, read in the device language; otherwise the date converter opens with the text.

## Broadcasts

Taqvim sends these as ordinary broadcasts. Apps that register a receiver for them while running (Tasker's "Intent
Received" event does) receive them; Android does not deliver them to receivers declared only in a manifest.

### `ir.taqvim.action.ATHAN_STARTED`

Sent when an athan starts playing.

| Extra | Type | Value |
|---|---|---|
| `prayer` | string | `FAJR`, `DHUHR`, `ASR`, `MAGHRIB` or `ISHA` |
| `time` | long | When the athan was due, in milliseconds since 1970-01-01T00:00Z |
| `jdn` | long | The day, as a Julian day number |

### `ir.taqvim.action.DAY_CHANGED`

Sent shortly after local midnight (a few minutes late is possible while the device is idle).

| Extra | Type | Value |
|---|---|---|
| `jdn` | long | The new day, as a Julian day number |
| `date` | string | The new day as an ISO date, e.g. `2026-03-21` |

Example (Tasker): Profile → Event → Intent Received → Action `ir.taqvim.action.ATHAN_STARTED`; the task can read
`%prayer`.
