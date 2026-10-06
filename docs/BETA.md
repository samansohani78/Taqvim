# Taqvim closed beta

The plan's beta program (docs/PLAN.md T-1902): a **two-week closed beta** with a checklist and practical exit
criteria. The plan's original "crash-free sessions ≥ 99.9%" is not used and cannot be: it is an Android vitals
number, there is no Play Console here, and the app collects no telemetry of its own (F-15). Nothing in this file
reports a fleet statistic the app does not measure. Release mechanics are in [RELEASE.md](RELEASE.md); how testers report problems is
in [SUPPORT.md](../SUPPORT.md).

> **Not started.** No beta build has been distributed, and no tester has run anything. Everything in this file is
> preparation; the tester recruitment and the fourteen days are the owner's to perform. The release mechanism itself is proven — v1.0.0 went from tag
> to published, signed, checksum-verified GitHub release on 2026-09-26 — so what a beta now needs is testers, not
> infrastructure. The owner confirmed the track,
> tester group and tester counts below on 2026-09-18. Nothing is uploaded without the owner's approval.

## Tracks and builds

- **A GitHub pre-release** is the beta track (RELEASE.md, *Distribution: GitHub Releases, not Google Play*). There is
  no Play Console, so there is no closed-testing track and no opt-in link. A `vX.Y.Z-beta.N` tag runs the same
  qualified, signed pipeline as a release and lands as a GitHub **pre-release** carrying the universal APK, its
  `SHA256SUMS`, the SBOM and the licence report.
- Beta builds are tagged `vX.Y.Z-beta.N` and carry that `versionName`, so every report names its build. The GitHub
  release for a beta tag is a draft pre-release (`release.yml`).
- A new beta build is uploaded when a P0 or holiday data error is fixed, or at the end of the first week.

## Joining (tester instructions)

1. Open the pre-release on the repository's Releases page and download `app-release.apk`.
2. Open the file on your phone and allow your browser or file manager to install unknown apps. There is no store, so
   **updates are not automatic**: to move to the next beta build, download the newer APK and install it over this one.
   Android accepts it because every build is signed with the same key; it never asks you to uninstall.
3. Optional but useful: check the download against the `SHA256SUMS` line in the release notes before installing.
3. Use Taqvim as your everyday calendar for two weeks, in your usual language (Persian and English are fully supported).
4. To leave, open the same link and choose to leave the program, then reinstall from the public listing when it exists.

## Giving feedback

- **In the app:** More → About → *Report a problem*. It prepares a redacted report with the app version, device and
  diagnostics; you choose where to send it ([SUPPORT.md](../SUPPORT.md), *Privacy of reports*).
- **Wrong holiday or date:** include the primary source, as described in SUPPORT.md.
- Severity and response times are the ones in SUPPORT.md: P0 fix within 48 hours, holiday data error within 72 hours.

Taqvim does not upload crash reports itself (no analytics, [SECURITY.md](SECURITY.md)). Crash-free sessions are
reported by testers. **There are no Android vitals outside Play and this app ships no crash reporting of its own**,
so stability is measured by what testers send in, not by a dashboard — see *Exit criteria*.

## Beta checklist

The plan points to a "Section 10.5" checklist that does not exist in PLAN.md; this checklist is assembled from the
plan's manual release checklist (§8.1), core UI scenarios (§8.3) and budgets (§9).

Before the beta starts:

- [ ] The build passed every release blocker in RELEASE.md except the manual sign-offs.
- [ ] Testers cover `fa` and `en`, and at least one each of `prs`, `ar`, `ckb` and `ne` users; at least two testers per listed language, recruited
      through the tester group.
- [ ] Devices cover low-end API 26, a mid-range phone, a tablet or foldable, and the OEM matrix for athan.

During the two weeks, testers and the team confirm:

- [ ] Fresh install and onboarding in each tested language picks the right defaults (calendars, holidays, prayer method).
- [ ] Month swipe, day details, today button, search, year view, agenda and timeline.
- [ ] Creating, editing and deleting personal events; reminders and official-day reminders notify on time.
- [ ] Athan: enabling it schedules alarms that fire and stop correctly, including after a reboot.
- [ ] Location by city, GPS and coordinates; prayer times match the official tables for Iranian cities.
- [ ] Converter, distance and workdays, time zones, compass, level, astronomy and map.
- [ ] Backup → wipe → restore returns the same data; ICS import, export and subscription refresh.
- [ ] Theme changes, RTL, font scale 2.0 and TalkBack on a real device.
- [ ] Holidays shown for Iran (and Afghanistan for Dari/Pashto users) match the official announcements for the beta
      period.

## Testers

**10 to 20 people**, recruited by the owner, covering the devices Taqvim's users actually hold:

| Make | Why it is on the list | Testers wanted |
|---|---|---|
| Samsung | One UI changes alarm and notification behaviour more than any other skin; the largest share in Iran. | 3–6 |
| Xiaomi | MIUI's aggressive background limits are the most common cause of a missed athan. | 3–6 |
| Pixel | Stock Android: the behaviour every other device is a deviation from. | 2–4 |
| OnePlus | OxygenOS doze handling, and the owner's own reference device for benchmarks (§12.4). | 2–4 |

Spread across at least three Android versions, including the oldest supported (API 26) and the newest. At least
two testers must use the app in Persian with RTL and a large font scale, and at least one with TalkBack.

Each tester is asked to confirm, once per build: that it installed, that they used it for a day, and what broke.
Silence from a tester is not evidence of anything — it is recorded as "no report", not as a pass.

## Exit criteria

The beta ends, and the release moves to a public tag (RELEASE.md), only when all of these hold. They are stated as
things a named person can attest, because that is all this project can actually observe:

1. At least **14 days** have passed since the first beta build reached testers.
2. **No blocker open**: nothing that stops the app being used for its purpose.
3. **No reproducible crash** reported by any tester on the current build, with "reproducible" meaning a second
   person or a second attempt produced it.
4. **No data loss** of any kind: no personal event, reminder, profile, rotation or backup lost or corrupted.
5. **No serious alarm or notification failure**: an athan or reminder that did not fire, fired at the wrong time, or
   could not be dismissed, on any tester's device.
6. **No major RTL or accessibility failure**: unreadable layout at font scale 2.0, a screen TalkBack cannot
   navigate, or text that reads in the wrong direction.
7. **No open P0** and no holiday data error older than 72 hours (SUPPORT.md).
8. §9 budgets met on the reference devices, with the benchmark results stored (§12.4).
9. The manual sign-offs of RELEASE.md are recorded, including TalkBack and RTL (§12.6).
10. `fa` and `en` are 100% translated.

Every criterion above is judged from tester reports and the owner's own device testing. **No fleet percentage is
computed, claimed or implied anywhere**, because the app measures none.

If a tester reports a crash, the beta continues with a fixed build and the 7-day window restarts with it.

## Owner defaults (confirmed 2026-09-18)

Confirmed by the owner on 2026-09-18 (record: [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md), *Owner defaults
confirmed on 2026-09-18*) and revised 2026-10-05 when the distribution channel changed: the tester list and the beta
start date, which is the day the first beta build reaches testers. The closed-testing track, tester Google Group and
Play opt-in URL no longer apply. **Two things are now the owner's to supply and cannot be automated:** how testers
are recruited and told that a new build exists — there is no store to notify them — and how their reports are
collected (SUPPORT.md's in-app report produces text the tester must send). The device-level checks
for the beta are in MANUAL_TEST_CHECKLIST.md.
