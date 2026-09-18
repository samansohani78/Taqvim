# Taqvim manual test checklist

The JVM, Robolectric and Roborazzi suites (docs/STATUS_REPORT.md, test inventory) cover the app's logic and layouts,
but some behaviour can only be judged on a real device or emulator: launchers, TalkBack, OEM power management, sound,
real timing and memory. This checklist is the residual manual work for a release. It is referenced by
[RELEASE.md](RELEASE.md) (*Release checklist*) and [BETA.md](BETA.md) (*Beta checklist*).

**How to record a result.** Copy the relevant sections into the draft GitHub release notes and fill every
*Sign-off* line as `name · date · build (versionName / versionCode) · device (model, Android version) · pass/fail`.
A failure is filed as an issue with the severity rules of [SUPPORT.md](../SUPPORT.md).

**Common preconditions** (assumed below unless a section says otherwise):

- A release-type build (`assembleRelease`, signed or debug-signed) installed fresh, unless the step says "update".
- App language set in the step; test at least `fa` (RTL, Persian digits) and `en` (LTR).
- A place chosen (Settings → Location & Athan → city), e.g. Tehran.
- Notifications allowed; exact alarms allowed unless the step tests the opposite.
- Device clock on automatic time; the device zone is noted in the sign-off.

Deep links used below are listed in [AUTOMATION.md](AUTOMATION.md) and can be sent with
`adb shell am start -a android.intent.action.VIEW -d "<link>"`.

---

## 0. Automated on a Gradle Managed Device

Much of what used to be manual now runs on a real Android 14 emulator (Gradle Managed Device `pixel6Api34`: Pixel 6,
API 34, `aosp-atd`, x86_64), declared in `app/build.gradle.kts`, and on a real Wear OS 5 watch emulator (Gradle Managed
Device `wearApi34`: Wear OS Small Round, API 34, `android-wear`, x86_64), declared in `wear/build.gradle.kts`. Each
needs `/dev/kvm`, about 4 GB of free RAM and access to `dl.google.com` for the first system-image download
(`system-images;android-34;aosp_atd;x86_64` and `system-images;android-34;android-wear;x86_64`). Run one at a time.

| Command | What it runs |
|---|---|
| `./gradlew :app:pixel6Api34DebugAndroidTest` | All phone device tests below (`app/src/androidTest`) |
| `./gradlew :wear:wearApi34DebugAndroidTest` | All watch device tests below (`wear/src/androidTest`) |
| `./gradlew :app:generateBaselineProfile` | Baseline and startup profiles on the `:benchmark` managed device (RELEASE.md) |

Covered automatically (tick the manual item only where the table in each section still says *manual*):

- **Every screen opens, in `fa` and `en`** (`DeviceSmokeTest`): each documented `taqvim://` link, the four top-level
  tabs and every More entry open their screen; every tab of each screen is selected; linked screens are rotated to
  landscape and back; the world map's layer and globe toggles are switched; the onboarding pages open and rotate.
  A crash, ANR or missing screen fails the run.
- **Accessibility Test Framework** (`DeviceAccessibilityTest`): ATF errors on the calendar, times, tools, More and
  every More screen, in `fa` and `en`.
- **Widgets** (`DeviceSurfacesTest`): all 12 widget providers are bound in a widget host, updated and drawn without
  the launcher's error view (the device part of §1.1 up to placement; resize and taps stay manual).
- **Persistent notification** (§7.1, first sentence): posted when enabled, removed when disabled.
- **Quick Settings tile and shortcuts** (§7.3, §7.4): the tile service and every manifest and dynamic shortcut
  resolve.
- **Reminders** (§6.1 step 1–2, the on-time part): a personal event's reminder, scheduled through the persistent
  scheduler with exact alarms allowed, is posted as a notification on time.
- **Watch app** (§5 steps 1–2, `WearDeviceSmokeTest`): with the watch language set to `fa` and to `en`, today opens
  the month, the converter and settings, each swipe back returns, the month steps forward and back, every converter
  stepper is used and each of the four settings choice lists opens.
- **Watch tiles** (§5 step 3, first half, `WearTileDeviceTest`): both tile services are bound as the system binds
  them, answer a tile request, and their layouts are rendered by the ProtoLayout renderer; the rendered text is
  checked against what the watch computes. Each rendered tile is stored as a PNG in
  `wear/build/outputs/managed_device_android_test_additional_output/` (`wear_tile_month.png`, `wear_tile_next.png`).
  The same layouts are rendered on the JVM by `WearTileScreenshotTest`, whose committed screenshots are
  `wear/src/test/screenshots/wear_tile_month/round_fa.png` and `.../wear_tile_next/round_fa.png`
  (`./gradlew :wear:verifyRoborazziDebug`).
- **Watch complications** (§5 step 4, the provider half, `WearComplicationDeviceTest`): every complication provider
  on the device binds and declares the complication types a watch face may ask for. Their values are checked on the
  JVM by `WearTilesAndComplicationsTest`.

Not automatable here, so still manual:

- **Wear OS (§5 steps 3–6, the watch-face and time half):** adding a tile or a complication to a watch face, tapping
  a tile to open the app, and what tiles and complications show after a time-zone change or past midnight need a real
  watch face, which an instrumentation test cannot drive.
- **Real launchers and OEM behaviour (§1, §6.4):** placement by drag, resizing, OEM power management, audio focus and
  Do Not Disturb need real devices.
- **TalkBack by ear (§2), physical sensors (compass, level), sound (athan), battery and memory budgets (§8).**

---

## 1. Widgets (T-1200 – T-1212)

Twelve widgets exist (`WidgetKind`): 1×1 date, 4×1 date + clock, 2×2 day summary, 4×2 prayer strip, month
(interactive), month (bitmap), week strip, schedule, sun arc, Moon, map, countdown. Each opens a configuration screen
when placed (colour, transparency, scale, content toggles, secondary calendar where supported).

### 1.1 Placement, configuration and resize — each of the 12 widgets

Preconditions: stock launcher (Pixel) and one OEM launcher (see §6.4 matrix).

1. Long-press the home screen → Widgets → Taqvim → drag the widget onto the home screen.
2. The configuration screen opens. Change the colour, transparency and scale; toggle each content option offered;
   choose a secondary calendar if offered. Confirm.
3. Check the widget shows the chosen options, in the app language, with the right digits and direction.
4. Resize the widget to its smallest and largest allowed size (where the launcher allows).
5. Remove the widget and add it again; the previous configuration is not reused for the new instance.

Expected: no blank, clipped or overlapping content at any size; text follows the app language and RTL; colours and
transparency match the configuration.

| Widget | Sign-off (Pixel launcher) | Sign-off (OEM launcher) |
|---|---|---|
| 1×1 date | | |
| 4×1 date + clock | | |
| 2×2 day summary (date, events, next prayer) | | |
| 4×2 prayer strip | | |
| Month (interactive) | | |
| Month (bitmap) | | |
| Week strip | | |
| Schedule (14 days) | | |
| Sun arc | | |
| Moon | | |
| Map (day/night) | | |
| Countdown / age | | |

### 1.2 Taps and actions

1. Tap the body of each date, day-summary, prayer-strip, week and schedule widget → the app opens on the tapped day
   (or today).
2. Month (interactive): tap *previous*, *next*, *today*; tap a day; tap *add* → the event editor opens on that day.
3. Schedule: scroll the list to the end (14 days) and back; tap an item → the calendar opens on that day.
4. Countdown: tap → the app opens on the target day.
5. Sun, Moon and map widgets: tap → Astronomy / the world map opens.

Expected: each tap opens exactly the stated screen once; month navigation changes only that widget instance.

Sign-off: ______

### 1.3 Updates

1. Place the date, month, prayer-strip, clock, sun and map widgets. Set the device clock to 23:58 (manual time).
2. Wait past midnight. The date widgets, month and week widgets move to the new day within about a minute.
3. With a prayer time 2 minutes ahead, wait for it: the next-prayer line moves to the following prayer.
4. Clock and sun/map widgets advance at least once per minute while the screen is on.
5. Change the app language, the theme, the main calendar and the place: every widget redraws accordingly.
6. Change the device time zone (e.g. Tehran → Tokyo): dates and times follow the new zone.
7. Reboot: widgets show current content after the boot completes.

Expected: no stale day after midnight; no widget stuck on a loading state; updates stop costing battery when no
widget is placed (Android battery usage shows no wake-ups from Taqvim; PLAN §9).

Sign-off: ______

---

## 2. TalkBack (T-1700)

Automated accessibility checks run in the screenshot tests; this is the PLAN's 20-step manual script, run on a device
with TalkBack on, once in `fa` and once in `en`.

1. Launch Taqvim from the launcher; TalkBack announces the app and the month title.
2. Swipe through the top bar: title with the secondary calendar, *today*, *search* and *more* each have a spoken label.
3. Move focus into the month grid; a day cell announces the full date, weekday, holiday status and event count.
4. Double-tap a day; the day details open and the selected date is announced.
5. Swipe through the day details tabs (Calendars, Events, Times); each tab announces its name and selected state.
6. In Events, each event chip reads its title, time and source; its citation tooltip is reachable.
7. In Times, each prayer reads its name and time; the next prayer is announced as next.
8. Use the TalkBack custom actions on the month grid to move to the next and previous month.
9. Open *today* from the top bar; the focus returns to today's cell.
10. Open search, type "nowruz" (or "نوروز"); results are announced with their group.
11. Open the event editor (long-press a day via TalkBack actions); every field has a label; save with no title →
    the validation error is announced.
12. Create an event and save; the confirmation is announced.
13. Open the year view; month tiles announce their names; select a month.
14. Open the week timeline; events read time and title; the now line is not announced repeatedly.
15. Open the converter; change the calendar; the result is read as a live region.
16. Open the compass; the heading is announced at most every 15°.
17. Open the world map; zoom, pick-centre and per-city actions are offered as custom actions.
18. Open Settings; each item reads its title, value and role (switch, choice); change one.
19. Open Backup; the passphrase field is announced as a password field; the privacy dashboard reads each permission.
20. Open About → Report a problem; the prepared report and the share action are reachable. *Continue* opens the
    e-mail app addressed to support@taqvim.app, or the share sheet on a device without one.

Expected: no unlabeled control, no focus trap, reading order follows the visual order (RTL in `fa`).

Sign-off (`fa`): ______ Sign-off (`en`): ______

---

## 3. RTL and font scale (T-1701)

Automated screenshot tests cover RTL × font scale 2.0 per screen. Manually, on a phone, a tablet and a foldable:

1. Set the app language to `fa`; set the system font size to the largest step and display size to the largest.
2. Visit every screen reachable from the bottom navigation and More: calendar, day details, year, agenda, timeline,
   events editor, times, astronomy, map, compass, level, tools (converter, distance, duration, time zones, QR), search,
   settings (all three tabs), backup, privacy, about, FAQ.
3. Repeat in `en` at the same scale, and in both languages at the default scale.

Expected: no clipped or overlapping text, no truncated buttons without an ellipsis, icons mirrored where
directional, numbers in the language's digits.

| Device class | `fa` scale 2.0 | `en` scale 2.0 | `fa` scale 1.0 | `en` scale 1.0 |
|---|---|---|---|---|
| Phone | | | | |
| Tablet | | | | |
| Foldable | | | | |

---

## 4. Tablet and foldable layouts (T-806)

1. On a tablet (≥ 600 dp width), open the calendar: the month and the day details show side by side and scroll
   independently.
2. Rotate the device; the selected day is kept.
3. On a foldable, open the app folded (compact) and unfold: the layout switches to two panes without losing the
   selected day.
4. Half-fold (tabletop posture): the month sits above the fold and the details below.
5. Use a hardware keyboard: arrow keys move the selection, Enter opens the day (where supported).

Sign-off (tablet): ______ Sign-off (foldable): ______

---

## 5. Wear OS (T-1600)

Standalone watch app (ADR-0019): no phone sync; its own language, main calendar, prayer method and city.

Steps 1–2, the tile content of step 3 and the provider half of step 4 run on the `wearApi34` managed device
(`./gradlew :wear:wearApi34DebugAndroidTest`, §0); the rest needs a watch face and the clock.

1. *(automated)* Install the Wear build on a watch or Wear emulator (API 30+). Open the app: today's date in the main
   and secondary calendars is shown.
2. *(automated)* Open *month*, scroll to the next month and back; open the *converter* and convert a date; open
   *settings* and change the language, calendar, prayer method and city.
3. Add the *month* tile and the *next prayer / occasion* tile; both show current content *(automated)* and open the
   app on tap *(manual)*.
4. Add each complication (date, month progress, next prayer) to a watch face; each shows correct values (the
   providers themselves are *automated*; the watch face is manual).
5. Change the watch time zone; the app, tiles and complications follow within a minute.
6. Wait past midnight and past a prayer time; tiles and complications update.

Sign-off: ______

---

## 6. Alarms, reminders and athan (T-604, T-1001, T-1102)

Scheduling rules: exact alarms when allowed, otherwise inexact with a banner; reschedule on boot, update, time and
zone change and preference changes; an alarm more than 15 minutes late is skipped; fired alarms stay pending until
delivered and are retried (ADR-0033); athan plays in a foreground service for at most 5 minutes, stops on audio focus
loss, respects silent mode and Do Not Disturb unless the Fajr bypass is on.

### 6.1 Reminders

1. Create a personal event 3 minutes ahead with a reminder at the start time. Lock the phone.
2. The notification appears on time with *Done* and *Snooze*. Tap *Snooze* → it returns after the snooze interval.
3. Tap *Done* → the notification is removed and does not return.
4. Create an official-event reminder ("1 day before" an official occasion) and move the clock to fire it.
5. Create a daily repeating event; edit one occurrence to move it past midnight; both that occurrence and the regular
   one on the next day get their reminders.

Sign-off: ______

### 6.2 Athan

1. Settings → Location & Athan: enable athan for all prayers with a custom sound (SAF picker), vibration on, gap
   minutes set.
2. Set the clock so a prayer is 2 minutes ahead; the athan plays, the notification shows *Stop* and *Snooze*.
3. *Stop* ends playback; *Snooze* replays after 10 minutes.
4. Start a phone call during playback → playback stops (audio focus loss).
5. Silent mode on: athan does not sound. Do Not Disturb on: athan does not sound, except Fajr when the Fajr bypass is on
   and notification-policy access is granted.
6. Let it play: it stops after at most 5 minutes.
7. Each prayer plays at most once per day, including after the app is killed and reopened.

Sign-off: ______

### 6.3 Scheduling robustness

1. Revoke *Alarms & reminders* (exact alarms) in system settings → the app shows the inexact-alarm banner; reminders
   still fire within the platform's inexact window. Grant it again → exact timing returns.
2. Enable battery optimisation (restricted) for Taqvim → note whether reminders and athan still fire; the app shows
   its battery-optimisation guidance.
3. Reboot with pending reminders and a snoozed reminder → all fire after boot.
4. Update the app (install a newer build over it) → pending reminders survive.
5. Change the time zone and the clock manually → alarms are recomputed for the new zone.
6. Revoke the notification permission (Android 13+) → no crash; the privacy dashboard shows notifications blocked.
7. Force-stop the app → note the behaviour (Android cancels alarms of force-stopped apps until the next launch).

Sign-off: ______

### 6.4 OEM matrix

Run §6.1–§6.3 on each device family; note any vendor setting required (auto-start, background activity, battery
saver exemptions) in the notes column.

| OEM / device family | Android version | §6.1 | §6.2 | §6.3 | Notes |
|---|---|---|---|---|---|
| Samsung (One UI) | | | | | |
| Xiaomi / Redmi / POCO (HyperOS/MIUI) | | | | | |
| Huawei / Honor (EMUI/MagicOS) | | | | | |
| Oppo / OnePlus / Realme (ColorOS/OxygenOS) | | | | | |
| Google Pixel | | | | | |
| Motorola | | | | | |
| Low-end API 26 device | 8.0 | | | | |

---

## 7. Notification and platform surfaces (T-1213 – T-1215)

1. **Persistent notification:** enable it in Settings → Widgets & Notification. The status-bar icon shows today's
   number; the notification shows the other calendars, holidays and prayer strip; it is visible on the lock screen;
   the large-number option changes the icon; it updates after midnight; tapping it opens the calendar.
2. **Dynamic launcher icon:** enable it (a warning is shown). After the next midnight (or a clock change) the launcher
   icon shows the day number. Note the launcher's behaviour (icon may move, pinned shortcuts may be disabled;
   ADR-0022). Disable it → the normal icon returns.
3. **Quick Settings tile:** add the Taqvim tile; it shows today's date; tapping it opens the app.
4. **App shortcuts:** long-press the launcher icon → *today*, *new event*, *prayer times*, *converter*; each opens its
   screen. Pin one shortcut and use it.
5. **Live wallpaper:** set Taqvim as live wallpaper → month, Moon and day/night sky are drawn; it redraws when content
   changes and does not drain the battery while the screen is off.
6. **Daydream / screen saver:** select Taqvim as screen saver and start it → a dim clock with dates is shown.

Sign-off: ______

---

## 8. Performance on a device (T-1801, T-1803, T-1800)

Run on the reference devices of PLAN §9 (a mid-range Pixel 6a class phone and a low-end API 26 phone) with a
release-type build. The macrobenchmark and microbenchmark modules (`:benchmark`, `:benchmark:micro`) measure these;
`benchmark.yml` runs them nightly on an emulator and compares with `benchmark/baselines`.

1. `./gradlew :benchmark:connectedBenchmarkAndroidTest` (device connected) → startup (`StartupBenchmark`), month scroll
   (`MonthPagerScrollBenchmark`), timeline and search (`ScreenScrollBenchmarks`), year view (`YearViewBenchmark`),
   map (`MapScreenBenchmark`), month-screen memory (`MonthScreenMemoryBenchmark`).
2. `./gradlew :benchmark:micro:connectedReleaseAndroidTest` → widget render (`WidgetRenderBenchmark`, `GlanceWidgetBenchmark`)
   and map mask (`MapMaskBenchmark`).
3. Compare with the budgets.

| Metric (PLAN §9) | Budget | Mid-range result | Low-end result |
|---|---|---|---|
| Cold start to first month frame | ≤ 350 ms / ≤ 800 ms | | |
| Month scroll jank (24 months) | < 1 % frames > 16 ms | | |
| Widget render (any) | < 30 ms | | |
| Map mask | < 150 ms | | |
| Search 10k events | < 20 ms / query | | |
| Month screen memory | < 80 MB RSS | | |
| Release APK (arm64) | ≤ 8 MB | | |

4. Baseline profile: run `BaselineProfileGenerator` on an API 33+ device (the `benchmark.yml` on-demand job, or
   `:app:generateBaselineProfile` once the Baseline Profile plugin is applied) and commit the generated profile
   (T-1800; see STATUS_REPORT for its current state).

Sign-off: ______

---

## 9. Backup, restore and calendars on a device (T-605, T-1503, T-1003, T-602)

1. Create personal events with reminders, change several settings and pick a place.
2. Settings → Backup → export with a passphrase to a file (SAF); export a plain JSON copy as well.
3. Clear the app's data (system settings) or reinstall.
4. Restore the passphrase backup: the preview lists what will be restored; after restoring, events, reminders,
   settings and place equal the originals and reminders are rescheduled.
5. Try a wrong passphrase → a clear error, nothing changed.
6. Restore a backup made with the previous release (release checklist *Backup*).
7. Kill the app during a restore (from recents, while the restore runs) and reopen it → a "finishing restore" screen
   is shown until recovery completes; data is either fully restored or fully the previous state.
8. ICS: import an `.ics` file exported from Google Calendar and one from Outlook → events appear; export Taqvim events
   and open the file in another calendar app.
9. Subscriptions: add a WebCal/HTTPS feed with network use allowed → events appear; the health page shows the last
   download; pause it and refresh; turn network use off → no fetch happens.
10. Device calendar: grant calendar permission → device events appear with their colours; all-day events stay on their
    date after changing the device zone; revoke the permission → no crash, device events disappear.

Sign-off: ______

---

## 10. Core UI scenarios on a device (PLAN §8.3)

The 40 scenarios are covered by Robolectric/Compose tests on the JVM, and the screen walk of every scenario runs on the
managed device (§0). On a device, walk through each scenario once in `fa` and once in `en`; tick it when it
behaves as described.

| # | Scenario | Expected | `fa` | `en` |
|---|---|---|---|---|
| 1 | Fresh install in fa / en / ne / ckb / ar | Defaults (calendars, weekend, digits, prayer method) follow the language | | |
| 2 | Onboarding: skip | Lands on the calendar with language defaults | | |
| 3 | Onboarding: complete (language, location, event sources) | Choices are applied | | |
| 4 | Month swipe | Titles change month by month, secondary calendar shown | | |
| 5 | Select a day | Day details tabs show that day | | |
| 6 | Today button | Returns to today | | |
| 7 | Long-press a day | Event editor opens on that day | | |
| 8 | Create a personal event with a reminder | Reminder notification fires | | |
| 9 | Edit and delete an event | Changes shown; deleted event and its reminder gone | | |
| 10 | Edit one occurrence of a series | Only that occurrence changes | | |
| 11 | Official-event reminder | Fires on the configured day | | |
| 12 | Search and jump | Result opens the right day or screen | | |
| 13 | Year view: select a month | Calendar opens on that month | | |
| 14 | Timeline drag-create | New event with the dragged times | | |
| 15 | Agenda scroll | Scrolls far ahead and back without gaps | | |
| 16–19 | Converter in each calendar (Persian, Islamic, Gregorian, Nepali/Hebrew) | Correct conversions both ways | | |
| 20 | NLP input: 10 phrases (e.g. "سه روز قبل از نوروز", "next Friday") | Parsed to the right dates | | |
| 21 | Distance and workdays | Counts exclude weekends and holidays | | |
| 22 | Enable athan → alarm scheduled → fires → stop | As §6.2 | | |
| 23 | Location by city | Prayer times change | | |
| 24 | Location by GPS | Place found or a clear error | | |
| 25 | Location by coordinates / map pick | Place saved after confirmation | | |
| 26 | Add each widget → tap → deep link | As §1.2 | | |
| 27 | Persistent notification content | As §7 | | |
| 28–33 | Theme changes: system, light, dark, black, dynamic colour, custom colour | Applied everywhere, readable contrast | | |
| 34 | Font scale 2.0 | As §3 | | |
| 35 | RTL switch (change language fa ↔ en) | Layout mirrors without restart issues | | |
| 36 | Backup → wipe → restore | As §9 | | |
| 37 | ICS import / export; subscription refresh | As §9 | | |
| 38 | Compass orientation lock; map layers; astronomy slider | Compass stays usable; each map layer draws; slider moves time | | |
| 39 | Deep links (10 from AUTOMATION.md) and PROCESS_TEXT ("Open in Taqvim") | Each opens the documented screen | | |
| 40 | Wear smoke (watch face: tile tap, complications, time zone, midnight) | As §5 steps 3–6; steps 1–2 run on `wearApi34` | | |

---

## Owner defaults confirmed on 2026-09-18

These support-document values were filled with defaults on 2026-09-17 (owner directive, Phase 3) and **confirmed by
the owner on 2026-09-18**. The table is the record of what was confirmed; the documents now state each value without a
marker. Changing one means changing it in the file named beside it, and here.

| File | Placeholder | Default filled in |
|---|---|---|
| SUPPORT.md | Repository URL for the issue forms | `https://github.com/samansohani78/Taqvim/issues/new/choose` |
| SUPPORT.md | Support e-mail | `support@taqvim.app` |
| SUPPORT.md | Other languages answered | Persian and English only; other languages answered on a best-effort basis |
| SUPPORT.md | P1 target | Fix released within 7 days of confirmation |
| SUPPORT.md | P2 target | Fix in the next scheduled release (within 30 days) |
| SUPPORT.md | First-response time | 2 working days |
| SUPPORT.md | Severity label names | `P0`, `P1`, `P2` (plus `data-error`) |
| SUPPORT.md | Days to wait for missing information | 14 days |
| docs/SECURITY.md | Security contact | `support@taqvim.app` (subject "Security") or GitHub private vulnerability reporting |
| docs/SECURITY.md | Encryption key | None published; use GitHub private vulnerability reporting for confidential details |
| docs/SECURITY.md | First response | 3 working days |
| docs/RELEASE.md | Keystore backup holder and Play Console account | The project owner (Saman Sohani) |
| docs/RELEASE.md | Required reviewers for `release` | The project owner |
| docs/RELEASE.md | OEM device matrix | The matrix in this checklist, §6.4 |
| docs/RELEASE.md | Play Console app, rollout percentages and hold times | `ir.taqvim`; 1 % → 5 % → 20 % → 50 % → 100 %, at least 24 h per step |
| docs/RELEASE.md | Store listing copy in `fa` and `en` | Not written yet; to be drafted from the README before the first upload |
| docs/RELEASE.md | Play service account | None; uploads stay manual |
| docs/RELEASE.md | Repository URL | `https://github.com/samansohani78/Taqvim` |
| docs/BETA.md | Closed-testing track and tester group | Play *Closed testing – Beta* track with a Google Group `taqvim-beta` |
| docs/BETA.md | Opt-in URL | Created by Play Console when the track is set up |
| docs/BETA.md | Tester list | At least 2 testers per listed language, recruited through the tester group |
| docs/BETA.md | Beta start date | The day the first beta build reaches testers |
| docs/i18n/TRANSLATING.md | Weblate instance URL | Hosted Weblate (`https://hosted.weblate.org/projects/taqvim/`) |
| docs/i18n/TRANSLATING.md | Reviewer per language | The project owner for `fa` and `en`; one volunteer reviewer per other language, recorded in the sign-off table |
