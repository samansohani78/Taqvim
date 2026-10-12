# ADR-0056: Automatic cloud backup is off; the user makes their own

- **Status:** Accepted
- **Date:** 2026-10-13
- **Plan reference:** docs/PLAN.md T-605 (backup and restore), T-1503 (privacy dashboard), T-1804;
  ADR-0017 (security baseline), ADR-0054 (no network access), ADR-0055 (no device location)

## Context

Android's Auto Backup copies an app's private files to the user's cloud account unless the app says otherwise.
Taqvim allowed it, with the strongest condition the platform offers: `disableIfNoEncryptionCapabilities="true"` on
Android 12+, and `requireFlags="clientSideEncryption"` on 9–11, so the copy was made only when it would be
end-to-end encrypted with the device lock. Android 8.x could not promise that, so nothing was copied there.

That was a defensible position, and it is no longer the right one for this app. Three things changed around it:

- Taqvim now has no network permission at all (ADR-0054). "This app cannot send your data anywhere" became a
  property the operating system enforces — except for one path the app itself had switched on, where the platform
  uploaded the database on its behalf.
- It asks for no location and reads no calendar (ADR-0055, ADR-0051). The remaining store is the user's own
  events, reminders, shift rotations, workday profiles and settings: what is left is the most personal part.
- The app already has its own backup (T-605): one file, AES-256-GCM with PBKDF2-HMAC-SHA256 at 600 000 iterations,
  written where the user chooses and restored by them.

The condition Auto Backup offers is also not the one that matters here. "Encrypted in transit and at rest with a
key derived from your device lock" is a statement about how the copy is protected, not about whether it is made.
A user who never opened Taqvim's backup screen still had their calendar on a server, and nothing in the app told
them so.

## Decision

**Automatic cloud backup copies nothing.** Every domain is excluded, on every Android version Taqvim supports:
`xml/backup_rules.xml` (8.0–8.1), `xml-v28/backup_rules.xml` (9–11) and the `cloud-backup` section of
`xml/data_extraction_rules.xml` (12+). The watch does the same.

**Device-to-device transfer is kept on Android 12+**, where the platform separates it from cloud backup. It is a
different thing: the user starts it while setting up their next phone, it moves the data between two devices they
are holding, and nothing is stored on a server along the way. Without it, changing phone would silently lose
everything the app has, which is not a privacy win for anyone.

On Android 9–11 the two cannot be separated — one setting governs both — so the transfer goes with the backup, and
a user on those versions carries their data in Taqvim's own backup file instead.

**`android:allowBackup` stays `true`.** It is what lets the device transfer run at all; what is copied is decided
by the rules files, which copy nothing to the cloud.

## Consequences

**Users who relied on it.** Someone who changes phone on Android 12 or later is unaffected: the transfer carries
everything as before. Someone restoring onto a wiped device from a cloud backup, or on Android 9–11, now gets a
fresh Taqvim and has to restore from a backup file they made. That is a real loss, and the reason the backup
screen exists; the privacy screen and docs/SECURITY.md say so rather than leaving the user to find out.

**What this is not.** It is not encryption, and it is not a claim that the data is safer on the device than in an
end-to-end encrypted backup. It is a narrower claim: Taqvim's data leaves the device when the user decides it
does, and at no other time. That is checkable — the rules files say so and a test reads them — in a way "it is
encrypted somewhere you cannot see" is not.

**It is enforced, not just done.** `SecurityConfigurationTest` reads all three rules files and fails if any of
them declares an `include` or drops one of the five exclusions, and separately fails if the device transfer stops
carrying the data. All three are asserted rather than the newest, because a rule left out of one file is a silent
upload on whichever Android versions read that file.
