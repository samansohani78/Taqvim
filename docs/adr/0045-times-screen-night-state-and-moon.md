# ADR-0045: The Times screen keeps its countdown at night and shows the Moon

- **Status:** Accepted
- **Date:** 2026-09-23
- **Plan reference:** docs/PLAN.md §7 T-1100 (line 507); T-1700, T-1701

## Context

The owner reported from a device that on the Times screen "the sun state does not work" and "the moon state is not
shown", with a screenshot taken at Tehran after Isha. Both were reproduced on an API 33 emulator with the clock at
20:30 and explained:

1. **The screen goes blank at night.** `SunArc` draws the travelled arc and the sun disc only while the Sun is up
   (`SunArcGeometry.isAboveHorizon`), which is correct — but the countdown above it disappeared as well, so between
   Isha and dawn the screen showed a bare dashed arc and a static list. The countdown was not missing by design:
   `PrayerSchedule.next` already searches the neighbouring days and returned tomorrow's Fajr, and
   `TimesStateMapper` then discarded it with `takeIf { isToday && it.at.toJdn(zone) == day }` — the next time falls on
   the *next* date, so the condition could never hold after the day's last prayer. For Tehran in Mehr that is roughly
   19:05 to 04:29, over nine hours a day, on the app's most-used screen after the calendar.
2. **The Moon was never on this screen.** PLAN line 507 specifies Times as "sun arc, prayer list, next-time highlight,
   city, method" and puts the Moon in the Calendars overview, where `DayDetailsPanel` does show it. `feature/times`
   contained no moon code at all, so nothing was broken — the screen simply never had it, while the astronomy screen,
   the moon widget and the day details all did.

## Decision

1. **The countdown follows the next time, wherever it falls.** The shown day must still be today for a countdown to
   appear, but the time it counts to may belong to the following date. At night the screen therefore counts down to
   tomorrow's Fajr and highlights that row, which is also the answer to "when does the night end".
2. **The sun arc is unchanged.** A dashed track with no marker is a correct picture of a Sun that is below the
   horizon, and with the countdown restored the screen is no longer silent at night. Marking the arc itself as night
   is a visual design question for the owner, not a defect, and is deliberately left out of this change.
3. **The Times screen shows the Moon of the shown day**: its phase name, the illuminated percentage, and its rise and
   set in the place's time zone, drawn with the existing `MoonDisc`. It follows the day navigation rather than today,
   like every other row on the screen. The maths is `core/astronomy`'s `Sky.moonAppearance`, `Sky.moonPhaseDegrees`
   and `Sky.riseSetTransit` — the same routines the Calendars tab and the widgets already use; no new astronomy was
   written. The phase is read at local noon of the shown day, as `DayDetailsCalculator` does, so both screens agree.
4. **Rise and set may be absent.** The Moon rises about fifty minutes later each day, so on one day of most months it
   does not rise, or does not set, between one local midnight and the next; that part of the line is then omitted
   rather than shown as a placeholder. A search result that lands past the shown day is discarded.
5. **No new translated text.** The thirteen strings the section needs already existed, translated in all 24 locales,
   as `calendar_phase_*`, `calendar_moon`, `calendar_percent`, `calendar_separator`, `astronomy_mode_moon`,
   `astronomy_moonrise` and `astronomy_moonset`; they are copied under `times_*` names. Only a format-only
   `"%1$s %2$s"` pattern is new, identical in every locale, so nothing ships machine-translated.

## Consequences

- The Times screen is informative for the whole day: a live countdown at every hour, and the Moon on every date.
- `MoonPhaseName` now exists in `feature/times` as well as `feature/calendar` and `feature/astronomy`. That is the
  third copy of an eight-value enum and its phase-name arithmetic; a later change could lift one copy into a shared
  module, which this ADR does not do because it would touch three features and 72 string files at once.
- `feature/times` now depends on `:core:astronomy`.
- The screen is one row taller, so its seven recorded screenshots changed; at font scale 2.0 the Moon row sits below
  the fold of the scrolling column, which the layout audit accepts because nothing is clipped within its own bounds.
- The test that asserted "after the last time of today there is no countdown" encoded the defect and now asserts the
  countdown runs to tomorrow's Fajr.
