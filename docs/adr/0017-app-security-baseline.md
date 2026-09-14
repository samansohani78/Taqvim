# ADR-0017: App security baseline

- **Status:** Accepted
- **Date:** 2026-09-14
- **Plan reference:** docs/PLAN.md T-1804 (no network permission in v1 except optional ICS subscriptions, declared and
  off by default; `FileProvider` scoped; backup encryption; exported components audited; StrictMode in debug; manifest
  audit test)

## Context

By T-1804 the app merges receivers, services, providers and activity aliases from several modules and libraries,
exposes `taqvim://` links and `PROCESS_TEXT` (ADR-0016), keeps personal data in Room and DataStore, and refreshes
iCalendar subscriptions over HTTPS (T-1003). The audit found that the merged manifest did not request `INTERNET` at all
(subscriptions could not work), subscriptions defaulted to "may use the network", and Android Auto Backup copied the
database and preferences without requiring encryption.

## Decision

1. **Network.**
   - `android.permission.INTERNET` is declared only by `:data:events`, the subscriptions module.
   - `AppSettings.subscriptionsNetworkAllowed` defaults to `false`: nothing touches the network until the user
     allows subscriptions to refresh.
   - The app's network security config refuses cleartext and trusts only system certificate authorities. It has no
     user CAs, no domain exceptions and no debug overrides.
   - `IcsFetcher` already accepts HTTPS only and rejects redirects to other schemes.
2. **Exported components.**
   - `app/src/test/resources/security/exported-components.txt` lists every exported component with its guarding
     permission, the builds it appears in, and why.
   - `ExportedComponentsTest` compares it with the merged debug unit-test manifest.
   - `:app:auditReleaseExportedComponents` (part of `check`) compares it with the merged release manifest.
   - Every source manifest component must declare `android:exported` (Konsist).
   - Taqvim's own receivers and services stay unexported and ignore actions or extras they do not handle.
   - Exported entries:
     - `MainActivity`: launcher and navigation-only links.
     - `ProcessTextActivity`: reads text.
     - Library components guarded by `BIND_JOB_SERVICE` or `DUMP`.
     - Debug-only Compose tooling and test hosts.
3. **Pending intents and files.**
   - Every `PendingIntent` is created with `FLAG_IMMUTABLE`, checked by Konsist.
   - `FileProvider`s share a dedicated cache sub-directory only: no root, external or whole-directory paths
     (Konsist).
4. **Backups.**
   - Taqvim's own backup file (T-605) is the supported way to move data. It can be encrypted with a passphrase.
   - Android Auto Backup may copy the database, DataStore and delivery logs only when end-to-end encrypted:
     - Android 12+: `disableIfNoEncryptionCapabilities="true"`.
     - Android 9–11: `requireFlags="clientSideEncryption"`.
     - Android 8.x: excluded entirely, because encryption cannot be required there.
   - A user-initiated device-to-device transfer copies the same data.
5. **StrictMode.** Debug builds install thread and VM policies with `detectAll` and `penaltyLog`. They log rather than
   crash, because library code outside Taqvim's control also violates them.
6. **Logs.** Taqvim code does not use `android.util.Log`. Diagnostics go through the redacted T-1504 path. No new rule
   is added because the plan asks for none; `docs/SECURITY.md` records the expectation.

## Consequences

- Adding an exported component, a permission or a network-using module fails the tests until the allowlist or this
  ADR is updated deliberately.
- Existing installs keep a stored `subscriptionsNetworkAllowed` value. Only new installs start with the network off.
- Users on Android 8.x rely on Taqvim's own backup file only.
