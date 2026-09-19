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
| Personal events, reminders, official-day reminder choices, work profiles | Room database | In Taqvim backup files the user creates (T-605), and in encrypted Auto Backup |
| Preferences, chosen place, athan and app settings | DataStore | Same as above |
| Device calendar copy, subscription caches, diagnostics | Room database | Diagnostics only inside a problem report the user shares, after redaction (T-1504) |
| Subscription URLs | Room database | Fetched over HTTPS only while the user allows network use (T-1003) |

Taqvim has no accounts, analytics, advertising or server of its own.

## Threat model summary

| Threat | Mitigation |
|---|---|
| Another app starts Taqvim components or sends spoofed broadcasts | The release manifest exports the 54 components listed under [Exported components](#exported-components-release) below — entry points, launcher aliases, widget receivers and system-bound services — each with the protection stated there. Links only open screens and are size-limited (ADR-0016). Every other receiver and service is unexported and ignores unknown actions or extras. An allowlist test covers the merged debug and release manifests. |
| Pending intents hijacked or modified | Every `PendingIntent` is immutable (Konsist rule). |
| Files exposed through a content provider | The only `FileProvider` shares `cache/shared/` for QR images, granted per share (T-1400). |
| Network interception | HTTPS only, system CAs only, cleartext refused (network security config). Subscriptions reject non-HTTPS URLs and scheme-changing redirects. The network is off until the user allows subscription refresh. |
| Backup file theft | Passphrase backups use AES-256-GCM with PBKDF2-HMAC-SHA256, 600 000 iterations (T-605). Passphrases stay in memory only and are wiped after use (T-1503). |
| Auto Backup or device transfer copying data in the clear | Cloud backup only when end-to-end encrypted (Android 9+). Android 8.x is excluded. |
| Personal data in logs or reports | One `android.util.Log` call in Taqvim code, documented under [Logging](#logging). Problem reports and diagnostics are redacted: event titles, precise coordinates, links with tokens, e-mail addresses and passwords are removed (T-1504). Nothing is uploaded. |
| Vulnerable or non-permissive dependencies | License gate with an LGPL canary (T-001), CycloneDX SBOM (T-003). |
| Development regressions | StrictMode in debug builds. Manifest audit, Konsist security rules and the license gate run in CI. |

## Exported components (release)

The release manifest exports **54** components (REVIEW R18). They are listed with their reasons, line by line, in
`app/src/test/resources/security/exported-components.txt`, which `ExportedComponentsTest` checks against the merged
manifest and `SecurityDocumentTest` checks against this section. Grouped:

| Group | Count | Components | Protection |
|---|---|---|---|
| App entry points | 2 | `ir.taqvim.app.MainActivity`, `ir.taqvim.feature.widgets.WidgetConfigActivity` | `MainActivity` accepts `taqvim://` links and launcher shortcuts, which only open screens and are size-limited (ADR-0016). `WidgetConfigActivity` answers `APPWIDGET_CONFIGURE` for a Taqvim widget the user is placing; it only changes that widget's appearance and saves nothing without the user. |
| Activity aliases | 33 | `ir.taqvim.app.ProcessTextActivity`; `ir.taqvim.app.LauncherDefault` and `ir.taqvim.app.LauncherDay01` … `LauncherDay31` | All target `MainActivity`. `ProcessTextActivity` is "Open in Taqvim" for selected text and only reads that text. The 32 launcher aliases are one `MAIN`/`LAUNCHER` entry per icon; all but the chosen one are disabled (T-1214, ADR-0022). |
| Widget receivers | 12 | `ir.taqvim.feature.widgets.DateWidget1x1Receiver`, `DateClockWidget4x1Receiver`, `DaySummaryWidget2x2Receiver`, `PrayerStripWidget4x2Receiver`, `MonthInteractiveWidgetReceiver`, `MonthBitmapWidgetReceiver`, `WeekStripWidgetReceiver`, `ScheduleWidgetReceiver`, `SunArcWidgetReceiver`, `MoonWidgetReceiver`, `MapWidgetReceiver`, `CountdownWidgetReceiver` | Must be exported for the launcher to deliver `APPWIDGET_UPDATE`; each receiver only redraws Taqvim's own widgets. |
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
