# ADR-0029: Prayer times are computed on a high-precision apparent Sun for any day and place

- **Status:** Accepted
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md §6 A-09, A-10, T-400, T-401; ADR-0002 (module rules), ADR-0003 (permissive
  dependencies), ADR-0026 (the same library in `:core:calendar`); DATA_TODO DT-011

## Context

T-401 computed prayer times on the NOAA Solar Calculator series (A-09). NOAA states them for 1901–2099 only; sunrise
used a fixed 0.833° zenith; and religious midnight ended at the same civil day's Fajr or sunrise rather than the next
morning's. PLAN's published-timetable check for Kabul, Istanbul, Berlin and Sydney (DT-011) cannot be done: Diyanet
reserves all rights to its timetables, Kabul's ministry publishes an image with no method, and Sydney has no official
timetable. The owner answered (2026-09-15): "write a algortim for better model", together with the standing rule that
every value comes from an algorithm that works for any year.

## Decision

1. **Ephemeris.** `:core:praytimes` depends on cosinekitty/astronomy 2.1.19 (MIT) as an `implementation` dependency,
   behind the internal `SolarEphemeris` (library types never leave it), as `:core:calendar` does (ADR-0026). The Sun is
   the library's apparent geocentric position of date — VSOP87 Earth, light time and aberration, precession and
   nutation to the true equator and equinox of date, the library's ΔT — with Greenwich apparent sidereal time.
   `:core:praytimes` does not use `:core:astronomy`, whose façade has no API for this.
2. **Any day.** Days within 10 Gregorian 400-year cycles of J2000 (civil years −2000…6000) use the ephemeris directly.
   A day further out is moved by whole cycles (146 097 days, exact in `Long`) into that range, which keeps its month,
   day and weekday and therefore its season; the library's own values drift without bound so far out (its ΔT is a
   parabola, and at ±100 000 years the declination leaves ±24°). Every `Jdn` whose distance from J2000 fits a `Long`
   works. NOAA is no longer used for prayer times; `NoaaSolarCalculator` stays public for the map's day/night layer.
3. **Events** (Meeus, *Astronomical Algorithms* 2nd ed., ch. 15). Right ascension and declination at local midnight,
   noon and the next midnight are interpolated quadratically; transit and each altitude event are refined until the
   Sun's local hour angle matches (steps below 0.1 ms). Twilights (Fajr, Isha, Maghrib angles) are depressions below
   the geometric horizon.
4. **Horizon** (`HorizonSettings`). Sunrise and sunset put the Sun's centre below the geometric horizon by 34′ of
   refraction scaled for pressure and temperature (Meeus ch. 16: P/1010 × 283/(273 + T)) plus the semi-diameter —
   16′ by default, or its value at the Sun's distance (959.63″ at 1 au) on request — plus, only when asked, the dip
   1.76′√h for the place's elevation (*The American Practical Navigator*, NGA Pub. 9). The 16′ default is measured: it
   reproduces the Institute of Geophysics' sunrise and sunset (99.8 % and 99.9 % of days to the minute) where the
   actual semi-diameter gives 98.5 % and 98.4 %.
5. **Asr.** The afternoon moment when the Sun's altitude is atan(1 / (k + tan|φ − δ|)), k = 1 or 2, with δ the apparent
   declination at transit, refined on the interpolated ephemeris.
6. **Nights.** A night runs from a sunset to the next sunrise. Fajr's high-latitude limit uses the night ending that
   morning (the previous civil day's sunset), Isha's the night starting that evening (the next day's sunrise), and
   midnight ends at the next morning's sunrise or Fajr. Ending at the next morning's Fajr is what the Institute does:
   99.9 % of its 11 315 published midnights then match to the minute, against 57 % for the same day's Fajr.
7. **High latitudes.** The existing rules stay (none, middle of the night, one seventh, angle-based, the Institute of
   Geophysics white-nights rule). `NEAREST_LATITUDE` is added from Resolution 6 of the Islamic Fiqh Council's ninth
   session (1406 AH), endorsed by the European Council for Fatwa and Research (quoted at
   https://islamicfiqh.net/en/articles/prayer-in-polar-areas-267, retrieved 2026-09-15): where the twilight never
   reaches the angle, Fajr and Isha follow the nearest latitude of the same meridian where it does on that day, and
   beyond 66° the 45° parallel, carried over as the same fraction of the night. At that nearest latitude the Sun
   reaches the angle exactly at lower culmination, which is what the rule uses, so the time moves smoothly from day to
   day. Polar days and nights stay `Unavailable`. ICOP's and the Muslim World League's "local relative estimation" (a
   yearly average fraction) is not implemented.
8. **Methods.** Parameters gain published minute adjustments and a rounding policy. Diyanet (Türkiye) is added from its
   public statements only: Fajr 18°, Isha 17° (press statement, 17 July 2013,
   https://www.diyanet.gov.tr/tr-TR/Content/PrintDetail/2921) and its temkin — sunrise −7, Dhuhr +5, Asr +4,
   Maghrib +7 minutes, none for Fajr and Isha (https://vakithesaplama.diyanet.gov.tr/temkin.php, retrieved
   2026-09-15). Diyanet publishes no Asr school, high-latitude rule, coordinates or rounding; those follow the user's
   settings and the nearest minute. No timetable is copied.
9. **Rounding.** Every method rounds to the nearest minute. For the Institute this is measured: floor and ceiling are
   half a minute off on average against its timetables.

## Consequences

- Against the Institute's 31 official 1405 timetables (11 315 days), share of times equal to the minute, NOAA model →
  this model: Fajr 98.3 → 99.9 %, sunrise 98.1 → 99.8 %, Dhuhr 98.3 → 99.9 %, sunset 97.9 → 99.9 %, Maghrib
  98.0 → 100.0 %, midnight 56.4 → 99.9 %. Every time is within one minute, so the official test's midnight tolerance
  tightens from 2 minutes to 1.
- Declination and equation of time differ from NOAA's by at most 0.0032° and 3.9 s over 1900–2100.
- A day costs nine ephemeris evaluations (the day and its two neighbours); a year of times for one city stays inside
  the 50 ms budget of its `timingTest`.
- Far outside −2000…6000 the times are those of the same calendar date inside the range: a stated continuation, not a
  prediction.
- `PrayerMethod.DIYANET` and `HighLatitudeRule.NEAREST_LATITUDE` are new stored values (preferences proto), shown in
  settings, day details and on Wear (en, fa).
- Day-details and timeline screenshots move by a minute where a prayer time changed; they are re-recorded.
