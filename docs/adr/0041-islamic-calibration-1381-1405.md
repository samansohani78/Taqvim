# ADR-0041: Islamic calibration on 1381–1405

- **Status:** Accepted
- **Date:** 2026-09-18
- **Plan reference:** docs/PLAN.md §5 D-07, §6 A-05, A-06; ADR-0027, ADR-0037, ADR-0040

## Context

ADR-0040 measured the Iranian crescent calibration (ADR-0027: a month has 29 days when Yallop's class is D or better
on the evening of day 29 at any of Tehran, Mashhad, Zahedan, Bandar Abbas or Shiraz) on 25 official months, AH
1446–1448. On 2026-09-18 the owner supplied the Calendar Center's official calendars for every year 1381–1405 SH.
The importer reads 21 of them; 1395, 1396, 1401 and 1402 have a text layer that maps several digits to one character
and would need OCR. The result is `golden/islamic-iran/official-month-starts-1381-1405.csv` in `:core:calendar`:
262 month starts over AH 1423–1448, of which 255 have a printed length.

The calendars are the Center's own **computed** calendars, published before each year. They are not sighting records,
except for four months (Ramadan 1423, Shawwal 1425, Ramadan 1426, Shawwal 1427) where the calendar notes that the month
began a day later or earlier by official announcement; the importer applies those shifts.

The owner asked for a refit on this history: fit on 1381–1400, hold out 1401–1405, compare Yallop, Odeh and a logistic
model, report the misses, and **not** switch the shipped criterion.

## Decision

1. **Two measures, both in `IslamicCalibrationReportTest`.**
   - *Evening decision:* each month with a printed length is one decision — given the official first day, is the
     crescent seen on the evening of day 29 at any of the five cities (29 days) or not (30)? Months are split by the
     calendar they were read from: **218 fit (1381–1400), 37 held out (1403–1405; 1401 and 1402 are unreadable)**.
   - *Chained starts:* the calendar each criterion produces from AH 1423 on, scored on all 262 official starts, as
     ADR-0040 does for the other regions. A day lost stays lost until a crescent check restores it.
2. **Criteria tried** at the five shipped cities: Yallop ≤ C, D (shipped), E; Odeh ≤ B, C, D; and a logistic model on
   the best site's Moon age, lag, ARCV, ARCL and W, standardised and fitted by Newton's method with a 10⁻³ ridge on
   the fit months only (test code, no dependency). The Moon's age is taken from cosinekitty/astronomy's new-moon search.
3. **Floors.** The shipped criterion must keep ≥ 90 % of the fit decisions, ≥ 89 % of the held-out ones (34/37 today;
   one more miss would be 33/37 = 89.2 %) and ≥ 90 % of the chained starts. `docs/data-todo/islamic-calibration-report.md`
   lists every miss of every criterion with the best Yallop q, the best Odeh V and a probable reason.
4. **The shipped criterion is not changed.** This ADR records the evidence and a recommendation only.

## Results (2026-09-18)

| Criterion | Fit (1381–1400) | Held out (1403–1405) | Chained, 262 starts |
|---|---|---|---|
| **Yallop ≤ D, five cities (shipped)** | **90.8 % (198/218)** | **91.9 % (34/37)** | **91.2 % (239/262)** |
| Yallop ≤ C, five cities | 89.0 % (194/218) | 86.5 % (32/37) | 88.2 % |
| Yallop ≤ E, five cities | 90.8 % (198/218) | 91.9 % (34/37) | — |
| Odeh ≤ B, five cities | 89.0 % (194/218) | 83.8 % (31/37) | 87.8 % |
| Odeh ≤ C, five cities | 81.2 % (177/218) | 81.1 % (30/37) | 81.7 % |
| Odeh ≤ D, five cities | 50.9 % (111/218) | 54.1 % (20/37) | — |
| Logistic (age, lag, ARCV, ARCL, W) | 92.2 % (201/218) | **97.3 % (36/37)** | **93.1 %** |
| Tabular type II / type I | — | — | 61.8 % / 62.2 % |

The shipped criterion's 23 missed decisions (the report has the full table):

| Month | Official | Shipped | Best q | Best V | Probable reason |
|---|---|---|---|---|---|
| 1423-08 | 30 | 29 | −0.210 | 1.333 | the next month (Ramadan 1423) was moved by official announcement |
| 1424-01 | 30 | 29 | −0.202 | 1.552 | marginal: q at the class D/E limit |
| 1424-11 | 30 | 29 | −0.211 | 1.354 | marginal; winter evening |
| 1425-04 | 30 | 29 | −0.083 | 2.723 | the calendar rejects a class C crescent |
| 1425-07 | 30 | 29 | 0.003 | 3.514 | the calendar rejects a class B crescent |
| 1425-12 | 30 | 29 | −0.170 | 1.724 | the calendar rejects a class D crescent; winter evening |
| 1426-10 | 30 | 29 | −0.216 | 1.347 | marginal; winter evening |
| 1428-08 | 29 | 30 | −0.379 | −0.159 | the calendar accepts a class F crescent |
| 1429-06 | 29 | 30 | −0.312 | 0.324 | the calendar accepts a class F crescent |
| 1429-09 | 29 | 30 | −0.274 | 0.883 | the calendar accepts a class E crescent |
| 1430-04 | 29 | 30 | −0.282 | 0.651 | the calendar accepts a class E crescent |
| 1430-09 | 29 | 30 | −0.654 | −2.927 | the calendar accepts a class F crescent |
| 1431-05 | 29 | 30 | −0.240 | 1.095 | marginal: q at the class D/E limit |
| 1431-10 | 29 | 30 | −0.601 | −2.430 | the calendar accepts a class F crescent |
| 1432-08 | 29 | 30 | −0.458 | −1.021 | marginal: V at Odeh's zone C/D limit |
| 1433-09 | 29 | 30 | −0.506 | −1.451 | the calendar accepts a class F crescent |
| 1434-10 | 29 | 30 | −0.325 | 0.346 | the calendar accepts a class F crescent |
| 1435-01 | 29 | 30 | −0.244 | 0.979 | marginal; winter evening |
| 1435-07 | 29 | 30 | −0.247 | 1.099 | marginal: q at the class D/E limit |
| 1436-10 | 29 | 30 | −0.436 | −0.715 | marginal: V at Odeh's zone C/D limit |
| 1446-04 (held out) | 29 | 30 | −0.451 | −0.861 | marginal: V at Odeh's zone C/D limit; winter evening |
| 1447-04 (held out) | 29 | 30 | −0.512 | −1.449 | the calendar accepts a class F crescent |
| 1448-04 (held out) | 29 | 30 | −0.613 | −2.473 | the calendar accepts a class F crescent |

**The misses change direction around AH 1428 (2007).** All 7 misses of AH 1423–1427 are months the calendar gives
30 days where the criterion gives 29: the Center was then *stricter* than Yallop D. All 16 misses of AH 1428–1448 are
the opposite: the Center gives 29 days to crescents Yallop rates E or F, several below the Danjon limit at all five
cities. The Center's rule was not constant over the period; a single fixed criterion cannot reproduce both eras, which
bounds any fit near 91–93 %.

## Recommendation

- **Keep Yallop ≤ D at five cities for now.** It is the best of the published criteria on both the fit and held-out
  months, and on the chained starts. Yallop ≤ E ties it and changes nothing.
- **The logistic model is the strongest candidate** (97.3 % held out, 93.1 % chained), but its held-out set is only
  37 decisions, one per month of three calendars, and all of it lies in the lenient era the fit also covers. Before it
  replaces the shipped rule: (a) import 1395, 1396, 1401 and 1402 (needs OCR or a clean copy of the PDFs), which adds
  about 48 decisions, most of them held out; (b) refit on AH 1428 onward only, since the earlier era follows a
  different practice; (c) re-run this report. If the logistic model still beats Yallop D by at least three held-out
  decisions, adopt it through a new ADR as a change of the default in `IranCrescentCalibration` — the optional
  official override (ADR-0037) keeps the published dates in either case.

## Consequences

- `IslamicCalibrationReportTest` asserts the three floors above; a regression in the ephemeris, the crescent geometry
  or the fixture fails it with the months that moved.
- The report grows from one Iranian region to two plus the evening section; the test still runs in a few seconds
  because every evening's geometry is computed once.
- The logistic weights live in test code only. Nothing in the app changes.
