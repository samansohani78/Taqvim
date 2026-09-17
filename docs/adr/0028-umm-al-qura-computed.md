# ADR-0028: Umm al-Qura months are computed from the calendar's criterion for every year

- **Status:** Accepted (amends ADR-0006)
- **Date:** 2026-09-15
- **Plan reference:** docs/PLAN.md §6 A-04, T-103; ADR-0006, ADR-0025, ADR-0026

## Context

ADR-0006 embedded ICU4J 78.3's Umm al-Qura month lengths for AH 1300–1600 and used the civil tabular calendar
(type II) outside them, exactly as ICU4J does. The repository documented no Umm al-Qura rule, so every year outside
the table silently showed a different calendar. The owner asked on 2026-09-15 that no calendar data be set by hand
and that every value come from an algorithm that works for any year.

R. H. van Gent (Utrecht University) documents the rules the calendar has used
(https://webspace.science.uu.nl/~gent0113/islam/ummalqura.htm, retrieved 2026-09-15; cited only, SHA-256 in
`docs/sources/MANIFEST.md`):

- before AH 1392: uncertain (reconstructions do not match the published dates);
- AH 1392–1419: a rule on the time of the new moon relative to 0h UTC;
- AH 1420–1422: on the 29th day the next day starts the month if the Moon sets after the Sun at Mecca;
- since AH 1423 (15 March 2002): on the 29th day the next day starts the month if **the geocentric conjunction occurs
  before sunset and the Moon sets after the Sun**; otherwise the month has 30 days. The Kaʿba in the Great Mosque
  defines the place. The dates are computed by KACST from modern theories of the Sun and Moon.

The printed calendars of the Saudi Ministry of Finance cover AH 1300–1429 and 1420–1450 (van Gent's bibliography).

Checked against ICU4J's data with cosinekitty/astronomy 2.1.19 at 21.4225° N, 39.8262° E, civil days in UTC+3
(`UmmAlQuraCriterionTest`):

- AH 1423–1450: the rule gives 334 of 336 published month starts. The two others are marginal: Jumada II 1427
  (conjunction 24 s before sunset, published 30 days) and Jumada II 1446 (moonset 4 s before sunset, published
  29 days). An observer height of 0 m or 277 m changes nothing.
- AH 1420–1422: the moonset rule gives all 36 months.
- AH 1392–1419: the page's rule, read as "first day = UTC date of the new moon + 1", gives only 61 of 335 months, and
  the best simple variant about 85 %; before 1392 no rule fits. These years are known only from the printed tables.
- AH 1451–1600: ICU4J's table is a projection, not the criterion. In about a third of the months it starts one day
  later than the criterion, and never earlier, including clear cases such as Rabi II 1451 (conjunction 14 h before
  sunset, moonset 10 min after it).

## Decision

1. **Published years stay as data.** AH 1300–1450, the years of the printed calendars, keep the published month
   lengths (151 masks). They are the official record, including the two marginal months and the years whose rule is
   unknown.
2. **The criterion since AH 1423 for every other year** of AH −3000…3000 (`UmmAlQuraCriterion`): after AH 1450 it is the
   calendar's own rule; before AH 1300 it is applied proleptically (the calendar did not exist then). Each month
   follows the rule from the start of the month before it, which is either the published start (right after 1450)
   or found from its own conjunction (the day after the first evening, from the conjunction on, on which both
   conditions hold). Starts are computed on first use, 120 months at a time, and kept in immutable arrays behind
   `lazy`, as in ADR-0026. The month before 1 Muharram 1300 is kept at 29 or 30 days so the two parts join (the rule
   already gives that; the clamp is a safeguard).
3. **Mean lunar months beyond AH −3000…3000.** The ephemeris and ΔT are extrapolations there, so the interval between
   the first and last astronomical month starts is spread evenly in whole days (`MeanLunarMonths`, shared with the
   Iranian crescent months since main@d59a920). Every month has 29
   or 30 days, every year 354 or 355, and both edges meet the astronomical months exactly. Arithmetic is in `Long`.
4. **Range:** every `Int` year; `fromJdn` rejects days outside them with `IllegalArgumentException`.
5. **No civil-calendar fallback.** ICU4J remains a test-only oracle for the published years.
6. **Library use.** The criterion gets the conjunction and Sun and Moon rise/set at the Kaʿba through the internal
   `CalendarAstronomy` object (ADR-0026), the only `:core:calendar` main file that imports cosinekitty/astronomy
   (main@d59a920); library types stay inside it.

## Consequences

- Umm al-Qura dates after AH 1450 now follow the published rule instead of ICU4J's projection, so they differ from
  ICU4J and from platforms that use ICU data in about a third of the months (always one day earlier).
- Dates before AH 1300 are a proleptic Umm al-Qura calendar, not the civil tabular calendar.
- The first conversion in a 120-month block pays for about 600 ephemeris searches; the published years cost nothing.
- Religious authorities may still announce Ramadan, Shawwal or Dhu al-Hijja by sighting; those announcements are not
  part of the computed calendar (van Gent, "Adjustment of the Umm al-Qura Calendar").

## Addendum 2026-09-17 — computed from AH 1420; only AH 1300–1419 bundled

The owner's "computed, not typed" directive (2026-09-17) allows bundled calendar data only where computation is
impossible, and published tables of computable years only as test oracles. Decision 1 is revised:

- **AH 1423 onward is computed at run time** by the criterion, including AH 1423–1450, which decision 1 used to take from
  the printed calendar. The published months of AH 1420–1450 are kept only as a golden oracle in
  `UmmAlQuraCalendarTest` (`PUBLISHED_1420_1450_MASKS`, cross-checked against ICU4J).
- **AH 1420–1422 are computed** by that period's documented moonset-only rule, chained month by month from the end of
  the bundled years; this reproduces all 36 published months, and 1 Muharram 1423 lands on the published day.
- **AH 1300–1419 stay bundled** (120 masks). They are not computable: no rule fits the months before 1392, and the rule
  documented for 1392–1419 reproduces only 61 of 335 months (about 85 % for the best simple variant). They are finite
  historical dates of the printed calendar that will never change, so they need no maintenance. The public API names
  them `BUNDLED_FIRST_YEAR`, `BUNDLED_LAST_YEAR` and `isBundled` (formerly `PUBLISHED_*` and `isPublished`).

**Consequence:** within AH 1420–1450 the app's dates now differ from the printed calendar in exactly two months, the
marginal cases above. 1 Jumada II 1427 falls one day earlier (conjunction 24 s before sunset, so the rule ends Jumada I
after 29 days) and 1 Jumada II 1446 one day later (moonset 4 s before sunset, so the rule gives Jumada I 30 days). The
months after each start on the published days again. Every other month of AH 1420–1450, and every month of AH
1300–1419, matches the printed calendar exactly. Users who need the announced dates can use the Iranian official or
another variant; Saudi sighting announcements were never part of this calendar.
