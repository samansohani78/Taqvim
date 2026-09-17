# Security

Taqvim is an offline-first calendar. This document summarises what it protects, how, and how to report a
vulnerability. Decisions are recorded in [ADR-0017](adr/0017-app-security-baseline.md). The link and broadcast contract
is in [ADR-0016](adr/0016-automation-links-and-broadcasts.md).

## Reporting a vulnerability

Please report security problems privately, not in public issues.

- Contact: support@taqvim.app with the subject "Security", or GitHub private vulnerability reporting on
  [samansohani78/Taqvim](https://github.com/samansohani78/Taqvim/security/advisories/new). <!-- TODO(owner): confirm security contact -->
- Encryption key for reports (optional): none published; use GitHub private vulnerability reporting for confidential
  details. <!-- TODO(owner): confirm secure channel -->
- Expected first response: within 3 working days. <!-- TODO(owner): confirm security response time -->

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
| Another app starts Taqvim components or sends spoofed broadcasts | Only `MainActivity` (launcher, `taqvim://` links) and the `PROCESS_TEXT` alias are exported, plus library components guarded by system permissions. Links only open screens and are size-limited (ADR-0016). Receivers and services are unexported and ignore unknown actions or extras. An allowlist test covers the merged debug and release manifests. |
| Pending intents hijacked or modified | Every `PendingIntent` is immutable (Konsist rule). |
| Files exposed through a content provider | The only `FileProvider` shares `cache/shared/` for QR images, granted per share (T-1400). |
| Network interception | HTTPS only, system CAs only, cleartext refused (network security config). Subscriptions reject non-HTTPS URLs and scheme-changing redirects. The network is off until the user allows subscription refresh. |
| Backup file theft | Passphrase backups use AES-256-GCM with PBKDF2-HMAC-SHA256, 600 000 iterations (T-605). Passphrases stay in memory only and are wiped after use (T-1503). |
| Auto Backup or device transfer copying data in the clear | Cloud backup only when end-to-end encrypted (Android 9+). Android 8.x is excluded. |
| Personal data in logs or reports | No `android.util.Log` in Taqvim code. Problem reports and diagnostics are redacted: event titles, precise coordinates, links with tokens, e-mail addresses and passwords are removed (T-1504). Nothing is uploaded. |
| Vulnerable or non-permissive dependencies | License gate with an LGPL canary (T-001), CycloneDX SBOM (T-003). |
| Development regressions | StrictMode in debug builds. Manifest audit, Konsist security rules and the license gate run in CI. |

## For contributors

- Don't add an exported component, a permission or network access without updating
  `app/src/test/resources/security/exported-components.txt`, ADR-0017 and this file.
- Create `PendingIntent`s with `FLAG_IMMUTABLE`.
- Don't log personal data. Use the redacted diagnostics path.
- Keep `taqvim://` links navigation-only (ADR-0016).
