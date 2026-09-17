# Taqvim closed beta

The plan's beta program (docs/PLAN.md T-1902): a **two-week closed beta** with a checklist, exited only when
crash-free sessions are **≥ 99.9%**. Release mechanics are in [RELEASE.md](RELEASE.md); how testers report problems is
in [SUPPORT.md](../SUPPORT.md).

> **Not started.** No build has been distributed and no store account is configured. Values marked with a `TODO(owner)`
> comment are defaults filled in on 2026-09-17 for the owner to confirm. Nothing is uploaded without the owner's approval.

## Tracks and builds

- **Google Play closed testing** is the beta track (RELEASE.md, *Distribution*). Testers join through an opt-in link.
  Track: Play *Closed testing – Beta*; testers join through the Google Group `taqvim-beta`. <!-- TODO(owner): confirm track and tester group -->
- Beta builds are tagged `vX.Y.Z-beta.N` and carry that `versionName`, so every report names its build. The GitHub
  release for a beta tag is a draft pre-release (`release.yml`).
- A new beta build is uploaded when a P0 or holiday data error is fixed, or at the end of the first week.

## Joining (tester instructions)

1. Open the opt-in link that Play Console creates for the beta track <!-- TODO(owner): confirm opt-in URL --> with the Google account you use on your phone and accept.
2. Install or update Taqvim from Google Play. It may take a few hours before the beta version appears.
3. Use Taqvim as your everyday calendar for two weeks, in your usual language (Persian and English are fully supported).
4. To leave, open the same link and choose to leave the program, then reinstall from the public listing when it exists.

## Giving feedback

- **In the app:** More → About → *Report a problem*. It prepares a redacted report with the app version, device and
  diagnostics; you choose where to send it ([SUPPORT.md](../SUPPORT.md), *Privacy of reports*).
- **Wrong holiday or date:** include the primary source, as described in SUPPORT.md.
- Severity and response times are the ones in SUPPORT.md: P0 fix within 48 hours, holiday data error within 72 hours.

Taqvim does not upload crash reports itself (no analytics, [SECURITY.md](SECURITY.md)). Crash-free sessions are
measured with Google Play's Android vitals for the beta track.

## Beta checklist

The plan points to a "Section 10.5" checklist that does not exist in PLAN.md; this checklist is assembled from the
plan's manual release checklist (§8.1), core UI scenarios (§8.3) and budgets (§9).

Before the beta starts:

- [ ] The build passed every release blocker in RELEASE.md except the manual sign-offs.
- [ ] Testers cover `fa` and `en`, and at least one each of `prs`, `ar`, `ckb` and `ne` users; at least two testers per listed language, recruited
      through the tester group. <!-- TODO(owner): confirm tester list -->
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
2. **Crash-free sessions ≥ 99.9%** over the last 7 days of the beta, from Android vitals (§9).
3. **No open P0** and no holiday data error older than 72 hours (SUPPORT.md).
4. §9 budgets met on the reference devices, with the benchmark results stored (§12.4).
5. The manual sign-offs of RELEASE.md are recorded, including TalkBack and RTL (§12.6).
6. `fa` and `en` are 100% translated.

If crash-free sessions fall below 99.9%, the beta continues with a fixed build; the 7-day window restarts with it.

## Owner defaults to confirm

Filled with defaults on 2026-09-17 (see [MANUAL_TEST_CHECKLIST.md](MANUAL_TEST_CHECKLIST.md), *Owner defaults to
confirm*): the closed-testing track and tester group, the opt-in URL (created by Play Console), the tester list, and
the beta start date, which is the day the first beta build reaches testers. The device-level checks for the beta are
in MANUAL_TEST_CHECKLIST.md.
