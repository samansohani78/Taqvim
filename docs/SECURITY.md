# Security

Taqvim is an offline-first calendar. This document summarises what it protects, how, and how to report a
vulnerability. Decisions are recorded in [ADR-0017](adr/0017-app-security-baseline.md). The link and broadcast contract
is in [ADR-0016](adr/0016-automation-links-and-broadcasts.md).

## Reporting a vulnerability

Please report security problems privately, not in public issues.

- Contact: support@taqvim.app with the subject "Security", or GitHub private vulnerability reporting on
  [samansohani78/Taqvim](https://github.com/samansohani78/Taqvim/security/advisories/new).
- Encryption key for reports (optional): none published; use GitHub private vulnerability reporting for confidential
  details.
- Expected first response: within 3 working days.

Include the app version (More → About), the Android version, and steps to reproduce. Do not include other people's
personal data.

## What Taqvim holds

| Data | Where | Leaves the device |
|---|---|---|
| Personal events, reminders, official-day reminder choices, work profiles | Room database | Only in Taqvim backup files the user creates (T-605), and in a device-to-device transfer they start (ADR-0056) |
| Preferences, chosen place, athan and app settings | DataStore | Same as above |
| Diagnostics | Room database | Only inside a problem report the user shares, after redaction (T-1504) |

Taqvim has no accounts, analytics, advertising or server of its own.

### Nothing is backed up unless you back it up

Android's Auto Backup copies an app's private files to your cloud account unless the app opts out. Taqvim opts
out — on every version it supports, and for every kind of data it holds. If you want a copy of your events,
reminders, shifts and settings, you make one in Settings → Backup: a single file, encrypted with a passphrase you
choose, saved where you choose. Changing phone on Android 12 or later still carries everything, because the
device-to-device transfer you start is kept; it moves data between two devices you hold and stores nothing on a
server. [ADR-0056](adr/0056-no-automatic-cloud-backup.md) has the reasoning and what it costs.

### Taqvim has no network access

Taqvim requests neither `INTERNET` nor `ACCESS_NETWORK_STATE`, so its process cannot open a socket at all. Remote
calendar subscriptions were the only feature that did, and they were removed with the permission; upgrading drops
the feeds and their cached events outright (schema migration 9 → 10). The decision and its reasoning are in
[ADR-0054](adr/0054-taqvim-has-no-network-access.md).

Without the permission this is not a property of the code anyone has to re-check: no dependency, present or future,
can reach the network, however it is written. What remains are things the user starts in another app — opening a
cited source in their browser, or picking a file through the Storage Access Framework, possibly from a cloud
provider. Taqvim hands over a URL or reads a document; the fetching is done by an app the user chose.

### Taqvim does not ask where your device is

Taqvim requests neither `ACCESS_FINE_LOCATION` nor `ACCESS_COARSE_LOCATION`, and no code in it reads the platform
location service or the geocoder. Prayer times, Qibla and the sky need coordinates, not a permission: you give them
by choosing a city from the bundled list of 7 342, tapping a point on the map, or typing them. A place you had
already saved from your device position keeps working — its coordinates and time zone were stored with it. The
decision and its reasoning are in [ADR-0055](adr/0055-taqvim-does-not-ask-where-the-device-is.md).

The platform geocoder went with it. It needs no permission and usually resolves over the network, so it was the one
way coordinates could still leave the device; the nearest bundled city names them instead.

### Taqvim does not read your device calendar

Taqvim requests neither `READ_CALENDAR` nor `WRITE_CALENDAR`, and no code in it reaches Android's calendar
provider. It cannot see the events in your calendar app, and it keeps no copy of them: an earlier version did
(T-602), and upgrading deletes that copy outright (schema migration 8 → 9). The decision and its reasoning are in
[ADR-0051](adr/0051-taqvim-does-not-read-the-device-calendar.md).

This is not a claim that Taqvim has no calendar interoperability. It still reads and writes **.ics files** you
choose, in Settings → Calendar files (T-1003). That is a file you hand it through the system file picker; it never
touches the calendar database on your device, and needs no storage permission to read or write the one document.

Two checks keep this true rather than merely stated: `auditReleasePermissions` compares the merged **release**
manifest against `app/src/test/resources/security/requested-permissions.txt`, which may never list a calendar
permission, and a Konsist rule fails if any source manifest requests one or any production source names
`CalendarContract`, the provider authority or either permission constant.

## Threat model summary

| Threat | Mitigation |
|---|---|
| The app learns the user's whereabouts | It holds no location permission and reads no position or geocoder (ADR-0055). The place is whatever the user chose, stored in the app's own settings. |
| Another app starts Taqvim components or sends spoofed broadcasts | The release manifest exports the 56 components listed under [Exported components](#exported-components-release) below — entry points, launcher aliases, widget receivers and system-bound services — each with the protection stated there. Links only open screens and are size-limited (ADR-0016). Every other receiver and service is unexported and ignores unknown actions or extras. An allowlist test covers the merged debug and release manifests. |
| Pending intents hijacked or modified | Every `PendingIntent` is immutable (Konsist rule). |
| Files exposed through a content provider | The only `FileProvider` shares `cache/shared/` for QR images, granted per share (T-1400). |
| Network interception | Taqvim requests no network permission, so it has no traffic to intercept (ADR-0054). The network security config stays as the standing answer if a dependency ever brings the permission back: cleartext refused, system CAs only, no user-installed CAs. |
| Backup file theft | Passphrase backups use AES-256-GCM with PBKDF2-HMAC-SHA256, 600 000 iterations (T-605). Passphrases stay in memory only and are wiped after use (T-1503). |
| Automatic cloud backup copying data the user did not ask to copy | Auto Backup copies nothing, on every supported Android version (ADR-0056). The device-to-device transfer the user starts when setting up a new phone is kept on Android 12+, where the platform separates the two. |
| Personal data in logs or reports | One `android.util.Log` call in Taqvim code, documented under [Logging](#logging). Problem reports and diagnostics are redacted: event titles, precise coordinates, links with tokens, e-mail addresses and passwords are removed (T-1504). Nothing is uploaded. |
| A dependency adds a permission nobody asked for | `auditReleasePermissions` compares the merged release manifest with the documented permission list and fails on any difference, in either direction. Calendar permissions (ADR-0051) and network permissions (ADR-0054) are additionally refused by name. |
| Vulnerable or non-permissive dependencies | License gate with an LGPL canary (T-001), CycloneDX SBOM (T-003). |
| Development regressions | StrictMode in debug builds. Manifest audit, Konsist security rules and the license gate run in CI. |

## Exported components (release)

The release manifest exports **56** components (REVIEW R18). They are listed with their reasons, line by line, in
`app/src/test/resources/security/exported-components.txt`, which `ExportedComponentsTest` checks against the merged
manifest and `SecurityDocumentTest` checks against this section. Grouped:

| Group | Count | Components | Protection |
|---|---|---|---|
| App entry points | 2 | `ir.taqvim.app.MainActivity`, `ir.taqvim.feature.widgets.WidgetConfigActivity` | `MainActivity` accepts `taqvim://` links and launcher shortcuts, which only open screens and are size-limited (ADR-0016). `WidgetConfigActivity` answers `APPWIDGET_CONFIGURE` for a Taqvim widget the user is placing; it only changes that widget's appearance and saves nothing without the user. |
| Activity aliases | 34 | `ir.taqvim.app.ProcessTextActivity`; `ir.taqvim.app.ShareTextActivity`; `ir.taqvim.app.LauncherDefault` and `ir.taqvim.app.LauncherDay01` … `LauncherDay31` | All target `MainActivity`. `ProcessTextActivity` is "Open in Taqvim" for selected text and `ShareTextActivity` the same for text shared from another app (`ACTION_SEND`, `text/plain`); both only read that text — it is parsed for a date and then discarded, never stored and never sent anywhere, and the app holds no `INTERNET` permission to send it with. Both are size-limited like links (ADR-0016). The 32 launcher aliases are one `MAIN`/`LAUNCHER` entry per icon; all but the chosen one are disabled (T-1214, ADR-0022). |
| Widget receivers | 13 | `ir.taqvim.feature.widgets.DateWidget1x1Receiver`, `ThreeDatesWidgetReceiver`, `DateClockWidget4x1Receiver`, `DaySummaryWidget2x2Receiver`, `PrayerStripWidget4x2Receiver`, `MonthInteractiveWidgetReceiver`, `MonthBitmapWidgetReceiver`, `WeekStripWidgetReceiver`, `ScheduleWidgetReceiver`, `SunArcWidgetReceiver`, `MoonWidgetReceiver`, `MapWidgetReceiver`, `CountdownWidgetReceiver` | Must be exported for the launcher to deliver `APPWIDGET_UPDATE`; each receiver only redraws Taqvim's own widgets. `ThreeDatesWidgetReceiver` also declares the `keyguard` widget category, so a host that offers lock-screen widgets may place it: it draws three calendar dates and reads nothing personal, so nothing private can reach a locked screen. |
| Library receivers | 2 | `androidx.work.impl.diagnostics.DiagnosticsReceiver`, `androidx.profileinstaller.ProfileInstallReceiver` | Guarded by `android.permission.DUMP`, a signature/privileged permission. |
| System-bound services | 5 | `ir.taqvim.feature.notification.TodayTileService`, `ir.taqvim.feature.wallpaper.TaqvimWallpaperService`, `ir.taqvim.feature.wallpaper.TaqvimDreamService`, `androidx.work.impl.background.systemjob.SystemJobService`, `androidx.glance.appwidget.GlanceRemoteViewsService` | Only the system can bind: `BIND_QUICK_SETTINGS_TILE`, `BIND_WALLPAPER`, `BIND_DREAM_SERVICE`, `BIND_JOB_SERVICE` and `BIND_REMOTEVIEWS` respectively. |

Debug builds add Compose tooling, the Compose test host and LeakCanary's two activities; unit tests add the androidx.test
hosts. None of them is in a release build.

## Logging

Taqvim code logs through `android.util.Log` in one place: `feature/calendar/…/CalendarRangeGuard.kt` writes a
warning when a day falls outside what a shown calendar can express (BUG-1). The message names the operation and a
Julian day number or a month offset — for example "day 2461299 is outside the supported calendar range" — followed by
the `CalendarRangeException` and its stack trace, whose message names only calendar numbers (such as a Hebrew month).
No event title, place, account or other personal data can reach it. The log is kept so a range failure on a device can
be diagnosed from logcat; it is not collected by the app. Everything else goes through the redacted diagnostics path.

## For contributors

- Don't add an exported component, a permission or network access without updating
  `app/src/test/resources/security/exported-components.txt`, ADR-0017 and this file.
- Create `PendingIntent`s with `FLAG_IMMUTABLE`.
- Don't log personal data. Use the redacted diagnostics path.
- Keep `taqvim://` links navigation-only (ADR-0016).
