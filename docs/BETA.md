# Taqvim closed beta

The plan's beta program (docs/PLAN.md T-1902): a **two-week closed beta** with a checklist, exited only when
crash-free sessions are **≥ 99.9%**. Release mechanics are in [RELEASE.md](RELEASE.md); how testers report problems is
in [SUPPORT.md](../SUPPORT.md).

> **Not started.** No beta build has been distributed. The release mechanism itself is proven — v1.0.0 went from tag
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

## Exit criteria

The beta ends, and the release moves to the open track and staged rollout (RELEASE.md), only when all of these hold:

1. At least **14 days** have passed since the first beta build reached testers.
2. **No crash reported by any tester in the last 7 days**, and every tester has confirmed at least one session on
   the current build. This replaces the former "crash-free sessions ≥ 99.9% from Android vitals": vitals do not exist
   outside Play and the app has no crash reporter, so the former criterion was unmeasurable. It is a weaker
   guarantee, and deliberately so — it states what a small hand-recruited group can actually evidence rather than
   implying a fleet statistic. Recruit enough testers that silence is informative; a handful of devices cannot
   substantiate a rate.
3. **No open P0** and no holiday data error older than 72 hours (SUPPORT.md).
4. §9 budgets met on the reference devices, with the benchmark results stored (§12.4).
5. The manual sign-offs of RELEASE.md are recorded, including TalkBack and RTL (§12.6).
6. `fa` and `en` are 100% translated.

If a tester reports a crash, the beta continues with a fixed build and the 7-day window restarts with it.

## Owner defaults (confirmed 2026-09-18)

Confirmed by the owner on 2026-09-18 (record: [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md), *Owner defaults
confirmed on 2026-09-18*) and revised 2026-10-05 when the distribution channel changed: the tester list and the beta
start date, which is the day the first beta build reaches testers. The closed-testing track, tester Google Group and
Play opt-in URL no longer apply. **Two things are now the owner's to supply and cannot be automated:** how testers
are recruited and told that a new build exists — there is no store to notify them — and how their reports are
collected (SUPPORT.md's in-app report produces text the tester must send). The device-level checks
for the beta are in MANUAL_TEST_CHECKLIST.md.
