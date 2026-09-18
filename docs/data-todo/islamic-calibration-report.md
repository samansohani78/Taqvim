# generated-by: core/astronomy IslamicCalibrationReportTest
# Islamic calendar calibration — agreement with the official months

Produced by `IslamicCalibrationReportTest` in `:core:astronomy`. It scores the shipped calendar of each region, and every calendar the refit tries, against the official months stored in the repository today. Import new official calendars with `tools/sources/iran/official_calendar_import.py`, then rerun the test with `-Ptaqvim.updateSnapshots=true` to refresh this page.

## Iran (Calendar Center, official calendars in docs/sources)

25 official facts, AH 1446–1448. One anchor per lunar month the official calendars print (their first days). The month table is rebuilt from the tabular calendar two months before the range, so a candidate that is a day out stays a day out until a crescent check corrects it.

**Shipped: five cities, Yallop ≤ D (shipped, ADR-0027) — 92.0 % (23/25).**

Missed: 1447-05 (+1 d), 1448-05 (+1 d).

| Calendar tried | Agreement | Missed |
|---|---|---|
| five cities, Yallop ≤ D (shipped, ADR-0027) | 92.0 % | 1447-05 (+1 d), 1448-05 (+1 d) |
| Yallop ≤ D, five cities | 92.0 % | 1447-05 (+1 d), 1448-05 (+1 d) |
| Yallop ≤ D, south-east | 92.0 % | 1447-05 (+1 d), 1448-05 (+1 d) |
| Odeh ≤ C, Tehran | 80.0 % | 1446-09 (-1 d), 1447-05 (+1 d), 1447-08 (-1 d), 1447-10 (-1 d), 1448-05 (+1 d) |
| Yallop ≤ C, five cities | 80.0 % | 1447-05 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Yallop ≤ D, Tehran | 80.0 % | 1447-05 (+1 d), 1447-06 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Odeh ≤ B, five cities | 76.0 % | 1446-12 (+1 d), 1447-05 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Odeh ≤ B, south-east | 76.0 % | 1446-12 (+1 d), 1447-05 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Odeh ≤ C, five cities | 76.0 % | 1446-09 (-1 d), 1447-05 (+1 d), 1447-08 (-1 d), 1447-10 (-1 d), 1448-05 (+1 d), 1448-08 (-1 d) |
| Odeh ≤ C, south-east | 76.0 % | 1446-09 (-1 d), 1447-05 (+1 d), 1447-08 (-1 d), 1447-10 (-1 d), 1448-05 (+1 d), 1448-08 (-1 d) |
| Yallop ≤ C, Tehran | 76.0 % | 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Yallop ≤ C, south-east | 76.0 % | 1446-12 (+1 d), 1447-05 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), 1448-06 (+1 d) |
| Odeh ≤ B, Tehran | 72.0 % | 1446-12 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), 1448-05 (+1 d), … |
| Yallop ≤ B, five cities | 68.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Yallop ≤ B, south-east | 68.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Odeh ≤ A, five cities | 60.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Odeh ≤ A, south-east | 60.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Yallop ≤ A, five cities | 60.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Yallop ≤ A, south-east | 60.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Yallop ≤ B, Tehran | 60.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), 1448-01 (+1 d), 1448-03 (+1 d), … |
| Odeh ≤ A, Tehran | 48.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-03 (+1 d), 1447-04 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), … |
| Yallop ≤ A, Tehran | 48.0 % | 1446-12 (+1 d), 1447-02 (+1 d), 1447-03 (+1 d), 1447-04 (+1 d), 1447-05 (+1 d), 1447-06 (+1 d), … |
| type I | 44.0 % | 1446-09 (-1 d), 1446-12 (+1 d), 1447-02 (+1 d), 1447-07 (-1 d), 1447-08 (-1 d), 1447-09 (-1 d), … |
| type II | 44.0 % | 1446-09 (-1 d), 1446-12 (+1 d), 1447-02 (+1 d), 1447-07 (-1 d), 1447-08 (-1 d), 1447-09 (-1 d), … |

No candidate does better than the shipped calendar, so it stays as it is.

## Iran 1381–1405 SH (Calendar Center, every readable official calendar)

310 official facts, AH 1423–1448. One anchor per lunar month start the calendars establish (printed, counted back, or moved by an announcement the calendar notes); `*` marks the held-out calendars 1401–1405. The months are chained from AH 1423, so a day lost stays lost until a crescent check restores it; the evening-decision section below scores each month on its own.

**Shipped: five cities, Yallop ≤ D (shipped, ADR-0027) — 92.3 % (286/310).**

Missed: 1423-09 (-1 d), 1424-02 (-1 d), 1424-12 (-1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1426-01 (-1 d), 1426-11 (-1 d), 1428-09 (+1 d), 1429-07 (+1 d), 1429-10 (+1 d), 1430-05 (+1 d), 1430-10 (+1 d), 1431-06 (+1 d), 1431-11 (+1 d), 1432-09 (+1 d), 1433-10 (+1 d), 1434-11 (+1 d), 1435-02 (+1 d), 1435-08 (+1 d), 1436-11 (+1 d), 1437-11 (+1 d), 1446-05* (+1 d), 1447-05* (+1 d), 1448-05* (+1 d).

| Calendar tried | Agreement | Missed |
|---|---|---|
| logistic (ADR-0041), five cities | 93.5 % | 1423-09 (-1 d), 1423-11 (-1 d), 1424-02 (-1 d), 1424-12 (-1 d), 1425-05 (-1 d), 1425-08 (-1 d), … |
| five cities, Yallop ≤ D (shipped, ADR-0027) | 92.3 % | 1423-09 (-1 d), 1424-02 (-1 d), 1424-12 (-1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1426-01 (-1 d), … |
| Yallop ≤ D, five cities | 92.3 % | 1423-09 (-1 d), 1424-02 (-1 d), 1424-12 (-1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1426-01 (-1 d), … |
| Yallop ≤ C, five cities | 88.7 % | 1423-02 (+1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1428-06 (+1 d), 1428-09 (+1 d), 1428-10 (+1 d), … |
| Odeh ≤ B, five cities | 88.4 % | 1423-02 (+1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1428-06 (+1 d), 1428-09 (+1 d), 1428-10 (+1 d), … |
| Yallop ≤ D, Tehran | 86.8 % | 1423-02 (+1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1425-10 (+1 d), 1426-09 (+1 d), 1427-10 (+1 d), … |
| Odeh ≤ C, Tehran | 83.2 % | 1423-04 (-1 d), 1423-07 (-1 d), 1423-09 (-1 d), 1423-11 (-1 d), 1424-02 (-1 d), 1424-12 (-1 d), … |
| Yallop ≤ C, Tehran | 83.2 % | 1423-02 (+1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1425-10 (+1 d), 1426-09 (+1 d), 1427-10 (+1 d), … |
| Odeh ≤ B, Tehran | 82.6 % | 1423-02 (+1 d), 1425-05 (-1 d), 1425-08 (-1 d), 1425-10 (+1 d), 1426-09 (+1 d), 1427-10 (+1 d), … |
| Odeh ≤ C, five cities | 82.6 % | 1423-04 (-1 d), 1423-07 (-1 d), 1423-09 (-1 d), 1423-11 (-1 d), 1424-02 (-1 d), 1424-08 (-1 d), … |
| Yallop ≤ B, five cities | 79.7 % | 1423-02 (+1 d), 1425-08 (-1 d), 1425-10 (+1 d), 1426-03 (+1 d), 1426-05 (+1 d), 1426-09 (+1 d), … |
| Yallop ≤ B, Tehran | 73.5 % | 1423-02 (+1 d), 1425-10 (+1 d), 1426-03 (+1 d), 1426-05 (+1 d), 1426-09 (+1 d), 1427-10 (+1 d), … |
| Yallop ≤ A, five cities | 65.8 % | 1423-02 (+1 d), 1423-06 (+1 d), 1424-03 (+1 d), 1424-05 (+1 d), 1424-07 (+1 d), 1424-09 (+1 d), … |
| Odeh ≤ A, five cities | 65.5 % | 1423-02 (+1 d), 1423-06 (+1 d), 1424-03 (+1 d), 1424-05 (+1 d), 1424-07 (+1 d), 1424-09 (+1 d), … |
| type I | 61.9 % | 1423-01 (-1 d), 1423-03 (-1 d), 1423-04 (-1 d), 1423-05 (-1 d), 1423-07 (-1 d), 1423-09 (-1 d), … |
| type II | 61.6 % | 1423-01 (-1 d), 1423-03 (-1 d), 1423-04 (-1 d), 1423-05 (-1 d), 1423-07 (-1 d), 1423-09 (-1 d), … |
| Odeh ≤ A, Tehran | 59.0 % | 1423-02 (+1 d), 1423-06 (+1 d), 1424-03 (+1 d), 1424-05 (+1 d), 1424-07 (+1 d), 1424-09 (+1 d), … |
| Yallop ≤ A, Tehran | 59.0 % | 1423-02 (+1 d), 1423-06 (+1 d), 1424-03 (+1 d), 1424-05 (+1 d), 1424-07 (+1 d), 1424-09 (+1 d), … |

**logistic (ADR-0041), five cities fits better (93.5 %).** Reported, not adopted: the shipped calendar changes only by a decision recorded in an ADR (ADR-0040, ADR-0041).

## Saudi Arabia (printed Umm al-Qura calendar, ICU4J as the oracle)

372 official facts, AH 1420–1450. One anchor per printed month of AH 1420–1450, the years the Umm al-Qura calendar has been published for. The shipped calendar computes them from the Umm al-Qura criterion (ADR-0028); the two months it misses are the marginal ones `UmmAlQuraCriterionTest` names.

**Shipped: Umm al-Qura criterion (shipped, ADR-0028) — 99.5 % (370/372).**

Missed: 1427-06 (-1 d), 1446-06 (+1 d).

| Calendar tried | Agreement | Missed |
|---|---|---|
| Umm al-Qura criterion (shipped, ADR-0028) | 99.5 % | 1427-06 (-1 d), 1446-06 (+1 d) |
| Odeh ≤ C, Makkah | 55.1 % | 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-07 (+1 d), 1420-12 (+1 d), 1421-02 (+1 d), … |
| type II | 44.4 % | 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), 1420-07 (+1 d), 1420-08 (+1 d), … |
| type I | 42.5 % | 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), 1420-07 (+1 d), 1420-08 (+1 d), … |
| Yallop ≤ D, Makkah | 37.9 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-07 (+1 d), 1420-08 (+1 d), … |
| Yallop ≤ C, Makkah | 32.0 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-07 (+1 d), 1420-08 (+1 d), … |
| Odeh ≤ B, Makkah | 30.4 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), 1420-07 (+1 d), … |
| Yallop ≤ B, Makkah | 23.9 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), 1420-07 (+1 d), … |
| Yallop ≤ A, Makkah | 10.8 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-03 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), … |
| Odeh ≤ A, Makkah | 10.2 % | 1420-01 (+1 d), 1420-02 (+1 d), 1420-03 (+1 d), 1420-04 (+1 d), 1420-05 (+1 d), 1420-06 (+1 d), … |

No candidate does better than the shipped calendar, so it stays as it is.

## Afghanistan (Bakhtar News Agency announcements)

5 official facts, AH 1447–1448. One anchor per lunar Hijri date an announcement states together with its Solar Hijri day or its weekday; a weekday-only anchor constrains the month start within the week only.

**Shipped: tabular type II (shipped, ADR-0010) — 100.0 % (5/5).**

| Calendar tried | Agreement | Missed |
|---|---|---|
| tabular type II (shipped, ADR-0010) | 100.0 % | — |
| type I | 100.0 % | — |
| Odeh ≤ A, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Odeh ≤ B, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Yallop ≤ A, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Yallop ≤ B, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Yallop ≤ C, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Yallop ≤ D, Kabul | 80.0 % | soviet-withdrawal (+1 d) |
| Odeh ≤ C, Kabul | 60.0 % | kabul-victory (-1 d), independence (-1 d) |

No candidate does better than the shipped calendar, so it stays as it is.

## Iran 1381–1405 SH: the evening of day 29 (ADR-0041)

Each official month whose length the calendars print is one decision: given the official first day, does the criterion give the month 29 days (crescent seen on the evening of day 29 at any of the five cities) or 30? 305 decisions: 244 fit (calendars 1381–1400) and 61 held out (calendars 1401–1405; the digits of 1395, 1396, 1401 and 1402 are read from their glyphs). The logistic models are fitted on the fit months only, the second on those of AH 1428 onward.

| Criterion | Fit | Held out |
|---|---|---|
| Yallop ≤ D, five cities (shipped, ADR-0027) | 91.4 % (223/244) | 95.1 % (58/61) |
| Yallop ≤ C, five cities | 89.3 % (218/244) | 88.5 % (54/61) |
| Yallop ≤ E, five cities | 91.4 % (223/244) | 95.1 % (58/61) |
| Odeh ≤ B, five cities | 89.3 % (218/244) | 86.9 % (53/61) |
| Odeh ≤ C, five cities | 82.0 % (200/244) | 83.6 % (51/61) |
| Odeh ≤ D, five cities | 50.4 % (123/244) | 50.8 % (31/61) |
| logistic (age, lag, ARCV, ARCL, W), five cities | 92.6 % (226/244) | 96.7 % (59/61) |
| logistic, fitted on AH 1428 onward, five cities | 93.0 % (227/244) | 100.0 % (61/61) |

Logistic weights (intercept, age, lag, ARCV, ARCL, W; standardised): -0.75, -2.01, 4.99, -2.59, 12.95, -6.87.

Fitted on AH 1428 onward: 0.64, -2.49, 7.83, -2.86, 11.92, -0.63.

- AH 1423–1427: 7 shipped misses — 7 where the calendar has 30 days and the criterion 29 (the calendar is stricter), 0 where it has 29 and the criterion 30 (the calendar is more lenient).
- AH 1428–1448: 17 shipped misses — 0 where the calendar has 30 days and the criterion 29 (the calendar is stricter), 17 where it has 29 and the criterion 30 (the calendar is more lenient).

### Missed by Yallop ≤ D, five cities (shipped, ADR-0027)

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1428-08 | 2007-08-15 | 29 d | 30 d | -0.379 | -0.159 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-06 | 2008-06-05 | 29 d | 30 d | -0.312 | 0.324 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-09 | 2008-09-02 | 29 d | 30 d | -0.274 | 0.883 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-04 | 2009-03-28 | 29 d | 30 d | -0.282 | 0.651 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-05 | 2010-04-16 | 29 d | 30 d | -0.240 | 1.095 | marginal: best q -0.240 is at Yallop's class D/E limit (−0.232) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1432-08 | 2011-07-03 | 29 d | 30 d | -0.458 | -1.021 | marginal: best V -1.021 is at Odeh's zone C/D limit (−0.96) |
| 1433-09 | 2012-07-21 | 29 d | 30 d | -0.506 | -1.451 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1434-10 | 2013-08-09 | 29 d | 30 d | -0.325 | 0.346 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1435-01 | 2013-11-05 | 29 d | 30 d | -0.244 | 0.979 | marginal: best q -0.244 is at Yallop's class D/E limit (−0.232); winter evening |
| 1435-07 | 2014-05-01 | 29 d | 30 d | -0.247 | 1.099 | marginal: best q -0.247 is at Yallop's class D/E limit (−0.232) |
| 1436-10 | 2015-07-18 | 29 d | 30 d | -0.436 | -0.715 | marginal: best V -0.715 is at Odeh's zone C/D limit (−0.96) |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1446-04 (held out) | 2024-10-05 | 29 d | 30 d | -0.451 | -0.861 | marginal: best V -0.861 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1447-04 (held out) | 2025-09-24 | 29 d | 30 d | -0.512 | -1.449 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |

### Missed by Yallop ≤ C, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-01 | 2002-03-16 | 29 d | 30 d | -0.202 | 1.548 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1428-05 | 2007-05-18 | 29 d | 30 d | -0.213 | 1.325 | marginal: best q -0.213 is at Yallop's class D/E limit (−0.232) |
| 1428-08 | 2007-08-15 | 29 d | 30 d | -0.379 | -0.159 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-06 | 2008-06-05 | 29 d | 30 d | -0.312 | 0.324 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-09 | 2008-09-02 | 29 d | 30 d | -0.274 | 0.883 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-04 | 2009-03-28 | 29 d | 30 d | -0.282 | 0.651 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-05 | 2010-04-16 | 29 d | 30 d | -0.240 | 1.095 | marginal: best q -0.240 is at Yallop's class D/E limit (−0.232) |
| 1431-07 | 2010-06-14 | 29 d | 30 d | -0.218 | 1.272 | marginal: best q -0.218 is at Yallop's class D/E limit (−0.232) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-12 | 2010-11-08 | 29 d | 30 d | -0.213 | 1.386 | marginal: best q -0.213 is at Yallop's class D/E limit (−0.232); winter evening |
| 1432-08 | 2011-07-03 | 29 d | 30 d | -0.458 | -1.021 | marginal: best V -1.021 is at Odeh's zone C/D limit (−0.96) |
| 1433-03 | 2012-01-25 | 29 d | 30 d | -0.177 | 1.762 | the calendar accepts a fainter crescent than the criterion (best class D); winter evening |
| 1433-07 | 2012-05-23 | 29 d | 30 d | -0.178 | 1.792 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1433-09 | 2012-07-21 | 29 d | 30 d | -0.506 | -1.451 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1434-07 | 2013-05-12 | 29 d | 30 d | -0.227 | 1.330 | marginal: best q -0.227 is at Yallop's class D/E limit (−0.232) |
| 1434-10 | 2013-08-09 | 29 d | 30 d | -0.325 | 0.346 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1435-01 | 2013-11-05 | 29 d | 30 d | -0.244 | 0.979 | marginal: best q -0.244 is at Yallop's class D/E limit (−0.232); winter evening |
| 1435-07 | 2014-05-01 | 29 d | 30 d | -0.247 | 1.099 | marginal: best q -0.247 is at Yallop's class D/E limit (−0.232) |
| 1436-10 | 2015-07-18 | 29 d | 30 d | -0.436 | -0.715 | marginal: best V -0.715 is at Odeh's zone C/D limit (−0.96) |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1438-11 | 2017-07-25 | 29 d | 30 d | -0.162 | 1.863 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1442-09 | 2021-04-14 | 29 d | 30 d | -0.212 | 1.458 | marginal: best q -0.212 is at Yallop's class D/E limit (−0.232) |
| 1443-01 | 2021-08-10 | 29 d | 30 d | -0.224 | 1.239 | marginal: best q -0.224 is at Yallop's class D/E limit (−0.232) |
| 1443-09 (held out) | 2022-04-03 | 29 d | 30 d | -0.209 | 1.459 | marginal: best q -0.209 is at Yallop's class D/E limit (−0.232) |
| 1445-07 (held out) | 2024-01-13 | 29 d | 30 d | -0.193 | 1.480 | the calendar accepts a fainter crescent than the criterion (best class D); winter evening |
| 1446-04 (held out) | 2024-10-05 | 29 d | 30 d | -0.451 | -0.861 | marginal: best V -0.861 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1447-04 (held out) | 2025-09-24 | 29 d | 30 d | -0.512 | -1.449 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1447-12 (held out) | 2026-05-18 | 29 d | 30 d | -0.166 | 1.735 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1448-02 (held out) | 2026-07-16 | 29 d | 30 d | -0.188 | 1.599 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |

### Missed by Yallop ≤ E, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-06 | 2002-08-10 | 30 d | 29 d | -0.253 | 0.889 | marginal: best q -0.253 is at Yallop's class D/E limit (−0.232) |
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-01 | 2006-01-31 | 30 d | 29 d | -0.251 | 0.899 | marginal: best q -0.251 is at Yallop's class D/E limit (−0.232); winter evening |
| 1428-08 | 2007-08-15 | 29 d | 30 d | -0.379 | -0.159 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-06 | 2008-06-05 | 29 d | 30 d | -0.312 | 0.324 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1432-06 | 2011-05-05 | 30 d | 29 d | -0.234 | 1.189 | marginal: best q -0.234 is at Yallop's class D/E limit (−0.232) |
| 1432-08 | 2011-07-03 | 29 d | 30 d | -0.458 | -1.021 | marginal: best V -1.021 is at Odeh's zone C/D limit (−0.96) |
| 1433-09 | 2012-07-21 | 29 d | 30 d | -0.506 | -1.451 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1434-10 | 2013-08-09 | 29 d | 30 d | -0.325 | 0.346 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1436-02 | 2014-11-24 | 30 d | 29 d | -0.232 | 1.123 | marginal: best q -0.232 is at Yallop's class D/E limit (−0.232); winter evening |
| 1436-10 | 2015-07-18 | 29 d | 30 d | -0.436 | -0.715 | marginal: best V -0.715 is at Odeh's zone C/D limit (−0.96) |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1441-12 | 2020-07-22 | 30 d | 29 d | -0.260 | 0.848 | marginal: best q -0.260 is at Yallop's class D/E limit (−0.232) |
| 1446-04 (held out) | 2024-10-05 | 29 d | 30 d | -0.451 | -0.861 | marginal: best V -0.861 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1447-04 (held out) | 2025-09-24 | 29 d | 30 d | -0.512 | -1.449 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |

### Missed by Odeh ≤ B, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-01 | 2002-03-16 | 29 d | 30 d | -0.202 | 1.548 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1428-05 | 2007-05-18 | 29 d | 30 d | -0.213 | 1.325 | marginal: best q -0.213 is at Yallop's class D/E limit (−0.232) |
| 1428-08 | 2007-08-15 | 29 d | 30 d | -0.379 | -0.159 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-06 | 2008-06-05 | 29 d | 30 d | -0.312 | 0.324 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1429-09 | 2008-09-02 | 29 d | 30 d | -0.274 | 0.883 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-04 | 2009-03-28 | 29 d | 30 d | -0.282 | 0.651 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-05 | 2010-04-16 | 29 d | 30 d | -0.240 | 1.095 | marginal: best q -0.240 is at Yallop's class D/E limit (−0.232) |
| 1431-07 | 2010-06-14 | 29 d | 30 d | -0.218 | 1.272 | marginal: best q -0.218 is at Yallop's class D/E limit (−0.232) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-12 | 2010-11-08 | 29 d | 30 d | -0.213 | 1.386 | marginal: best q -0.213 is at Yallop's class D/E limit (−0.232); winter evening |
| 1432-08 | 2011-07-03 | 29 d | 30 d | -0.458 | -1.021 | marginal: best V -1.021 is at Odeh's zone C/D limit (−0.96) |
| 1433-03 | 2012-01-25 | 29 d | 30 d | -0.177 | 1.762 | the calendar accepts a fainter crescent than the criterion (best class D); winter evening |
| 1433-07 | 2012-05-23 | 29 d | 30 d | -0.178 | 1.792 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1433-09 | 2012-07-21 | 29 d | 30 d | -0.506 | -1.451 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1434-07 | 2013-05-12 | 29 d | 30 d | -0.227 | 1.330 | marginal: best q -0.227 is at Yallop's class D/E limit (−0.232) |
| 1434-10 | 2013-08-09 | 29 d | 30 d | -0.325 | 0.346 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1435-01 | 2013-11-05 | 29 d | 30 d | -0.244 | 0.979 | marginal: best q -0.244 is at Yallop's class D/E limit (−0.232); winter evening |
| 1435-07 | 2014-05-01 | 29 d | 30 d | -0.247 | 1.099 | marginal: best q -0.247 is at Yallop's class D/E limit (−0.232) |
| 1436-10 | 2015-07-18 | 29 d | 30 d | -0.436 | -0.715 | marginal: best V -0.715 is at Odeh's zone C/D limit (−0.96) |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1438-11 | 2017-07-25 | 29 d | 30 d | -0.162 | 1.863 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1442-09 | 2021-04-14 | 29 d | 30 d | -0.212 | 1.458 | marginal: best q -0.212 is at Yallop's class D/E limit (−0.232) |
| 1443-01 | 2021-08-10 | 29 d | 30 d | -0.224 | 1.239 | marginal: best q -0.224 is at Yallop's class D/E limit (−0.232) |
| 1443-09 (held out) | 2022-04-03 | 29 d | 30 d | -0.209 | 1.459 | marginal: best q -0.209 is at Yallop's class D/E limit (−0.232) |
| 1445-07 (held out) | 2024-01-13 | 29 d | 30 d | -0.193 | 1.480 | the calendar accepts a fainter crescent than the criterion (best class D); winter evening |
| 1446-04 (held out) | 2024-10-05 | 29 d | 30 d | -0.451 | -0.861 | marginal: best V -0.861 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1446-11 (held out) | 2025-04-29 | 29 d | 30 d | -0.147 | 1.932 | the calendar accepts a fainter crescent than the criterion (best class C) |
| 1447-04 (held out) | 2025-09-24 | 29 d | 30 d | -0.512 | -1.449 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1447-12 (held out) | 2026-05-18 | 29 d | 30 d | -0.166 | 1.735 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1448-02 (held out) | 2026-07-16 | 29 d | 30 d | -0.188 | 1.599 | the calendar accepts a fainter crescent than the criterion (best class D) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |

### Missed by Odeh ≤ C, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-03 | 2002-05-14 | 30 d | 29 d | -0.339 | 0.147 | the calendar rejects a crescent the criterion counts (best class F) |
| 1423-06 | 2002-08-10 | 30 d | 29 d | -0.253 | 0.889 | marginal: best q -0.253 is at Yallop's class D/E limit (−0.232) |
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1423-10 | 2002-12-06 | 30 d | 29 d | -0.323 | 0.296 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-07 | 2003-08-29 | 30 d | 29 d | -0.435 | -0.863 | marginal: best V -0.863 is at Odeh's zone C/D limit (−0.96) |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-01 | 2004-02-22 | 30 d | 29 d | -0.307 | 0.480 | the calendar rejects a crescent the criterion counts (best class F) |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-06 | 2004-07-19 | 30 d | 29 d | -0.314 | 0.416 | the calendar rejects a crescent the criterion counts (best class F) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-06 | 2005-07-08 | 30 d | 29 d | -0.425 | -0.632 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-07 | 2005-08-07 | 30 d | 29 d | -0.319 | 0.409 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-01 | 2006-01-31 | 30 d | 29 d | -0.251 | 0.899 | marginal: best q -0.251 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-04 | 2006-04-29 | 30 d | 29 d | -0.383 | -0.304 | the calendar rejects a crescent the criterion counts (best class F) |
| 1427-07 | 2006-07-27 | 30 d | 29 d | -0.379 | -0.171 | the calendar rejects a crescent the criterion counts (best class F) |
| 1427-08 | 2006-08-26 | 30 d | 29 d | -0.343 | 0.201 | the calendar rejects a crescent the criterion counts (best class F) |
| 1428-02 | 2007-02-19 | 30 d | 29 d | -0.350 | -0.067 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-03 | 2008-03-09 | 30 d | 29 d | -0.350 | -0.046 | the calendar rejects a crescent the criterion counts (best class F) |
| 1430-02 | 2009-01-28 | 30 d | 29 d | -0.426 | -0.684 | marginal: best V -0.684 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1432-06 | 2011-05-05 | 30 d | 29 d | -0.234 | 1.189 | marginal: best q -0.234 is at Yallop's class D/E limit (−0.232) |
| 1432-08 | 2011-07-03 | 29 d | 30 d | -0.458 | -1.021 | marginal: best V -1.021 is at Odeh's zone C/D limit (−0.96) |
| 1433-09 | 2012-07-21 | 29 d | 30 d | -0.506 | -1.451 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1433-12 | 2012-10-17 | 30 d | 29 d | -0.337 | 0.071 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1435-12 | 2014-09-26 | 30 d | 29 d | -0.432 | -0.745 | marginal: best V -0.745 is at Odeh's zone C/D limit (−0.96) |
| 1436-02 | 2014-11-24 | 30 d | 29 d | -0.232 | 1.123 | marginal: best q -0.232 is at Yallop's class D/E limit (−0.232); winter evening |
| 1437-03 | 2015-12-13 | 30 d | 29 d | -0.313 | 0.368 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1437-05 | 2016-02-10 | 30 d | 29 d | -0.355 | -0.110 | the calendar rejects a crescent the criterion counts (best class F) |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1438-04 | 2016-12-31 | 30 d | 29 d | -0.388 | -0.327 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1439-05 | 2018-01-19 | 30 d | 29 d | -0.298 | 0.588 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1440-01 | 2018-09-11 | 30 d | 29 d | -0.354 | -0.044 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-05 | 2019-01-08 | 30 d | 29 d | -0.375 | -0.139 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1440-08 | 2019-04-07 | 30 d | 29 d | -0.351 | 0.044 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-11 | 2019-07-04 | 30 d | 29 d | -0.417 | -0.710 | marginal: best V -0.710 is at Odeh's zone C/D limit (−0.96) |
| 1441-02 | 2019-09-30 | 30 d | 29 d | -0.382 | -0.346 | the calendar rejects a crescent the criterion counts (best class F) |
| 1441-05 | 2019-12-28 | 30 d | 29 d | -0.400 | -0.407 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1441-12 | 2020-07-22 | 30 d | 29 d | -0.260 | 0.848 | marginal: best q -0.260 is at Yallop's class D/E limit (−0.232) |
| 1442-11 | 2021-06-12 | 30 d | 29 d | -0.334 | 0.231 | the calendar rejects a crescent the criterion counts (best class F) |
| 1443-03 | 2021-10-08 | 30 d | 29 d | -0.296 | 0.486 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1443-11 (held out) | 2022-06-01 | 30 d | 29 d | -0.397 | -0.358 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-02 (held out) | 2022-08-29 | 30 d | 29 d | -0.333 | 0.207 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-11 (held out) | 2023-05-21 | 30 d | 29 d | -0.410 | -0.515 | the calendar rejects a crescent the criterion counts (best class F) |
| 1445-10 (held out) | 2024-04-10 | 30 d | 29 d | -0.324 | 0.231 | the calendar rejects a crescent the criterion counts (best class F) |
| 1446-08 (held out) | 2025-01-31 | 30 d | 29 d | -0.305 | 0.393 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1447-04 (held out) | 2025-09-24 | 29 d | 30 d | -0.512 | -1.449 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1447-07 (held out) | 2025-12-22 | 30 d | 29 d | -0.294 | 0.613 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1447-09 (held out) | 2026-02-19 | 30 d | 29 d | -0.317 | 0.312 | the calendar rejects a crescent the criterion counts (best class F) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1448-07 (held out) | 2026-12-11 | 30 d | 29 d | -0.433 | -0.703 | marginal: best V -0.703 is at Odeh's zone C/D limit (−0.96); winter evening |

### Missed by Odeh ≤ D, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-02 | 2002-04-14 | 30 d | 29 d | -1.027 | -6.543 | the calendar rejects a crescent the criterion counts (best class F) |
| 1423-03 | 2002-05-14 | 30 d | 29 d | -0.339 | 0.147 | the calendar rejects a crescent the criterion counts (best class F) |
| 1423-06 | 2002-08-10 | 30 d | 29 d | -0.253 | 0.889 | marginal: best q -0.253 is at Yallop's class D/E limit (−0.232) |
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1423-10 | 2002-12-06 | 30 d | 29 d | -0.323 | 0.296 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1423-12 | 2003-02-03 | 30 d | 29 d | -0.585 | -2.211 | the calendar rejects a crescent the criterion counts (best class F) |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-03 | 2003-05-03 | 30 d | 29 d | -0.614 | -2.496 | the calendar rejects a crescent the criterion counts (best class F) |
| 1424-05 | 2003-07-01 | 30 d | 29 d | -0.456 | -1.004 | marginal: best V -1.004 is at Odeh's zone C/D limit (−0.96) |
| 1424-07 | 2003-08-29 | 30 d | 29 d | -0.435 | -0.863 | marginal: best V -0.863 is at Odeh's zone C/D limit (−0.96) |
| 1424-09 | 2003-10-27 | 30 d | 29 d | -0.512 | -1.628 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-01 | 2004-02-22 | 30 d | 29 d | -0.307 | 0.480 | the calendar rejects a crescent the criterion counts (best class F) |
| 1425-03 | 2004-04-21 | 30 d | 29 d | -0.608 | -2.429 | the calendar rejects a crescent the criterion counts (best class F) |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-06 | 2004-07-19 | 30 d | 29 d | -0.314 | 0.416 | the calendar rejects a crescent the criterion counts (best class F) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-03 | 2005-04-10 | 30 d | 29 d | -0.748 | -3.842 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-05 | 2005-06-08 | 30 d | 29 d | -0.653 | -2.872 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-06 | 2005-07-08 | 30 d | 29 d | -0.425 | -0.632 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-07 | 2005-08-07 | 30 d | 29 d | -0.319 | 0.409 | the calendar rejects a crescent the criterion counts (best class F) |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-01 | 2006-01-31 | 30 d | 29 d | -0.251 | 0.899 | marginal: best q -0.251 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-04 | 2006-04-29 | 30 d | 29 d | -0.383 | -0.304 | the calendar rejects a crescent the criterion counts (best class F) |
| 1427-06 | 2006-06-27 | 30 d | 29 d | -0.481 | -1.204 | marginal: best V -1.204 is at Odeh's zone C/D limit (−0.96) |
| 1427-07 | 2006-07-27 | 30 d | 29 d | -0.379 | -0.171 | the calendar rejects a crescent the criterion counts (best class F) |
| 1427-08 | 2006-08-26 | 30 d | 29 d | -0.343 | 0.201 | the calendar rejects a crescent the criterion counts (best class F) |
| 1427-12 | 2006-12-22 | 30 d | 29 d | -0.704 | -3.454 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1428-02 | 2007-02-19 | 30 d | 29 d | -0.350 | -0.067 | the calendar rejects a crescent the criterion counts (best class F) |
| 1428-06 | 2007-06-16 | 30 d | 29 d | -0.752 | -3.918 | the calendar rejects a crescent the criterion counts (best class F) |
| 1428-07 | 2007-07-16 | 30 d | 29 d | -0.498 | -1.389 | the calendar rejects a crescent the criterion counts (best class F) |
| 1428-10 | 2007-10-13 | 30 d | 29 d | -0.973 | -5.969 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1428-11 | 2007-11-12 | 30 d | 29 d | -0.520 | -1.551 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1429-01 | 2008-01-10 | 30 d | 29 d | -0.616 | -2.573 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1429-03 | 2008-03-09 | 30 d | 29 d | -0.350 | -0.046 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-07 | 2008-07-04 | 30 d | 29 d | -0.933 | -5.708 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-08 | 2008-08-03 | 30 d | 29 d | -0.601 | -2.400 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-10 | 2008-10-01 | 30 d | 29 d | -0.992 | -6.167 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-11 | 2008-10-31 | 30 d | 29 d | -0.560 | -1.927 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1430-01 | 2008-12-29 | 30 d | 29 d | -0.898 | -5.272 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1430-02 | 2009-01-28 | 30 d | 29 d | -0.426 | -0.684 | marginal: best V -0.684 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1430-05 | 2009-04-26 | 30 d | 29 d | -0.763 | -4.071 | the calendar rejects a crescent the criterion counts (best class F) |
| 1430-07 | 2009-06-24 | 30 d | 29 d | -0.583 | -2.325 | the calendar rejects a crescent the criterion counts (best class F) |
| 1430-11 | 2009-10-20 | 30 d | 29 d | -0.633 | -2.678 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1431-01 | 2009-12-18 | 30 d | 29 d | -0.860 | -4.897 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1431-02 | 2010-01-17 | 30 d | 29 d | -0.534 | -1.700 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1431-04 | 2010-03-17 | 30 d | 29 d | -0.891 | -5.219 | the calendar rejects a crescent the criterion counts (best class F) |
| 1431-06 | 2010-05-15 | 30 d | 29 d | -0.867 | -5.060 | the calendar rejects a crescent the criterion counts (best class F) |
| 1431-08 | 2010-07-13 | 30 d | 29 d | -0.862 | -5.021 | the calendar rejects a crescent the criterion counts (best class F) |
| 1432-01 | 2010-12-07 | 30 d | 29 d | -0.911 | -5.429 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1432-02 | 2011-01-06 | 30 d | 29 d | -0.479 | -1.177 | marginal: best V -1.177 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1432-04 | 2011-03-06 | 30 d | 29 d | -1.024 | -6.482 | the calendar rejects a crescent the criterion counts (best class F) |
| 1432-05 | 2011-04-05 | 30 d | 29 d | -0.658 | -2.934 | the calendar rejects a crescent the criterion counts (best class F) |
| 1432-06 | 2011-05-05 | 30 d | 29 d | -0.234 | 1.189 | marginal: best q -0.234 is at Yallop's class D/E limit (−0.232) |
| 1432-11 | 2011-09-29 | 30 d | 29 d | -0.473 | -1.223 | marginal: best V -1.223 is at Odeh's zone C/D limit (−0.96) |
| 1433-02 | 2011-12-26 | 30 d | 29 d | -0.675 | -3.154 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1433-04 | 2012-02-23 | 30 d | 29 d | -1.048 | -6.732 | the calendar rejects a crescent the criterion counts (best class F) |
| 1433-05 | 2012-03-24 | 30 d | 29 d | -0.732 | -3.642 | the calendar rejects a crescent the criterion counts (best class F) |
| 1433-06 | 2012-04-23 | 30 d | 29 d | -0.458 | -0.961 | marginal: best V -0.961 is at Odeh's zone C/D limit (−0.96) |
| 1433-08 | 2012-06-21 | 30 d | 29 d | -1.001 | -6.290 | the calendar rejects a crescent the criterion counts (best class F) |
| 1433-12 | 2012-10-17 | 30 d | 29 d | -0.337 | 0.071 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1434-03 | 2013-01-13 | 30 d | 29 d | -0.606 | -2.503 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1434-05 | 2013-03-13 | 30 d | 29 d | -0.854 | -4.865 | the calendar rejects a crescent the criterion counts (best class F) |
| 1434-06 | 2013-04-12 | 30 d | 29 d | -0.483 | -1.208 | marginal: best V -1.208 is at Odeh's zone C/D limit (−0.96) |
| 1434-09 | 2013-07-10 | 30 d | 29 d | -0.851 | -4.788 | the calendar rejects a crescent the criterion counts (best class F) |
| 1434-11 | 2013-09-07 | 30 d | 29 d | -0.784 | -4.207 | the calendar rejects a crescent the criterion counts (best class F) |
| 1435-02 | 2013-12-04 | 30 d | 29 d | -0.770 | -4.153 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1435-04 | 2014-02-01 | 30 d | 29 d | -0.686 | -3.319 | the calendar rejects a crescent the criterion counts (best class F) |
| 1435-06 | 2014-04-01 | 30 d | 29 d | -0.732 | -3.704 | the calendar rejects a crescent the criterion counts (best class F) |
| 1435-09 | 2014-06-29 | 30 d | 29 d | -0.855 | -4.819 | the calendar rejects a crescent the criterion counts (best class F) |
| 1435-10 | 2014-07-29 | 30 d | 29 d | -0.497 | -1.311 | the calendar rejects a crescent the criterion counts (best class F) |
| 1435-12 | 2014-09-26 | 30 d | 29 d | -0.432 | -0.745 | marginal: best V -0.745 is at Odeh's zone C/D limit (−0.96) |
| 1436-02 | 2014-11-24 | 30 d | 29 d | -0.232 | 1.123 | marginal: best q -0.232 is at Yallop's class D/E limit (−0.232); winter evening |
| 1436-05 | 2015-02-20 | 30 d | 29 d | -0.852 | -4.951 | the calendar rejects a crescent the criterion counts (best class F) |
| 1436-07 | 2015-04-20 | 30 d | 29 d | -0.658 | -3.003 | the calendar rejects a crescent the criterion counts (best class F) |
| 1436-09 | 2015-06-18 | 30 d | 29 d | -0.863 | -4.931 | the calendar rejects a crescent the criterion counts (best class F) |
| 1436-11 | 2015-08-16 | 30 d | 29 d | -0.994 | -6.200 | the calendar rejects a crescent the criterion counts (best class F) |
| 1436-12 | 2015-09-15 | 30 d | 29 d | -0.540 | -1.759 | the calendar rejects a crescent the criterion counts (best class F) |
| 1437-02 | 2015-11-13 | 30 d | 29 d | -0.698 | -3.349 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1437-03 | 2015-12-13 | 30 d | 29 d | -0.313 | 0.368 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1437-05 | 2016-02-10 | 30 d | 29 d | -0.355 | -0.110 | the calendar rejects a crescent the criterion counts (best class F) |
| 1437-08 | 2016-05-08 | 30 d | 29 d | -0.637 | -2.813 | the calendar rejects a crescent the criterion counts (best class F) |
| 1437-11 | 2016-08-04 | 30 d | 29 d | -1.001 | -6.298 | the calendar rejects a crescent the criterion counts (best class F) |
| 1437-12 | 2016-09-03 | 30 d | 29 d | -0.483 | -1.208 | marginal: best V -1.208 is at Odeh's zone C/D limit (−0.96) |
| 1438-02 | 2016-11-01 | 30 d | 29 d | -0.747 | -3.781 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1438-03 | 2016-12-01 | 30 d | 29 d | -0.653 | -2.887 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1438-04 | 2016-12-31 | 30 d | 29 d | -0.388 | -0.327 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1438-06 | 2017-02-28 | 30 d | 29 d | -0.467 | -1.167 | marginal: best V -1.167 is at Odeh's zone C/D limit (−0.96) |
| 1438-09 | 2017-05-27 | 30 d | 29 d | -0.627 | -2.733 | the calendar rejects a crescent the criterion counts (best class F) |
| 1438-12 | 2017-08-23 | 30 d | 29 d | -0.595 | -2.368 | the calendar rejects a crescent the criterion counts (best class F) |
| 1439-02 | 2017-10-21 | 30 d | 29 d | -0.711 | -3.443 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1439-03 | 2017-11-20 | 30 d | 29 d | -0.672 | -3.048 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1439-04 | 2017-12-20 | 30 d | 29 d | -0.601 | -2.360 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1439-05 | 2018-01-19 | 30 d | 29 d | -0.298 | 0.588 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1439-07 | 2018-03-19 | 30 d | 29 d | -0.454 | -1.003 | marginal: best V -1.003 is at Odeh's zone C/D limit (−0.96) |
| 1439-10 | 2018-06-15 | 30 d | 29 d | -0.564 | -2.139 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-01 | 2018-09-11 | 30 d | 29 d | -0.354 | -0.044 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-03 | 2018-11-09 | 30 d | 29 d | -0.717 | -3.527 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1440-04 | 2018-12-09 | 30 d | 29 d | -0.599 | -2.343 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1440-05 | 2019-01-08 | 30 d | 29 d | -0.375 | -0.139 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1440-07 | 2019-03-08 | 30 d | 29 d | -0.906 | -5.356 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-08 | 2019-04-07 | 30 d | 29 d | -0.351 | 0.044 | the calendar rejects a crescent the criterion counts (best class F) |
| 1440-11 | 2019-07-04 | 30 d | 29 d | -0.417 | -0.710 | marginal: best V -0.710 is at Odeh's zone C/D limit (−0.96) |
| 1441-02 | 2019-09-30 | 30 d | 29 d | -0.382 | -0.346 | the calendar rejects a crescent the criterion counts (best class F) |
| 1441-04 | 2019-11-28 | 30 d | 29 d | -0.782 | -4.185 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1441-05 | 2019-12-28 | 30 d | 29 d | -0.400 | -0.407 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1441-07 | 2020-02-25 | 30 d | 29 d | -0.948 | -5.739 | the calendar rejects a crescent the criterion counts (best class F) |
| 1441-08 | 2020-03-26 | 30 d | 29 d | -0.614 | -2.480 | the calendar rejects a crescent the criterion counts (best class F) |
| 1441-10 | 2020-05-24 | 30 d | 29 d | -0.749 | -3.860 | the calendar rejects a crescent the criterion counts (best class F) |
| 1441-12 | 2020-07-22 | 30 d | 29 d | -0.260 | 0.848 | marginal: best q -0.260 is at Yallop's class D/E limit (−0.232) |
| 1442-03 | 2020-10-18 | 30 d | 29 d | -0.644 | -2.922 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1442-05 | 2020-12-16 | 30 d | 29 d | -0.793 | -4.310 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1442-07 | 2021-02-13 | 30 d | 29 d | -0.984 | -6.114 | the calendar rejects a crescent the criterion counts (best class F) |
| 1442-08 | 2021-03-15 | 30 d | 29 d | -0.603 | -2.373 | the calendar rejects a crescent the criterion counts (best class F) |
| 1442-10 | 2021-05-13 | 30 d | 29 d | -0.927 | -5.550 | the calendar rejects a crescent the criterion counts (best class F) |
| 1442-11 | 2021-06-12 | 30 d | 29 d | -0.334 | 0.231 | the calendar rejects a crescent the criterion counts (best class F) |
| 1443-02 | 2021-09-08 | 30 d | 29 d | -0.740 | -3.833 | the calendar rejects a crescent the criterion counts (best class F) |
| 1443-03 | 2021-10-08 | 30 d | 29 d | -0.296 | 0.486 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1443-06 | 2022-01-04 | 30 d | 29 d | -0.751 | -3.928 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1443-08 | 2022-03-04 | 30 d | 29 d | -0.757 | -3.928 | the calendar rejects a crescent the criterion counts (best class F) |
| 1443-10 (held out) | 2022-05-02 | 30 d | 29 d | -0.919 | -5.474 | the calendar rejects a crescent the criterion counts (best class F) |
| 1443-11 (held out) | 2022-06-01 | 30 d | 29 d | -0.397 | -0.358 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-01 (held out) | 2022-07-30 | 30 d | 29 d | -0.562 | -2.007 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-02 (held out) | 2022-08-29 | 30 d | 29 d | -0.333 | 0.207 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-04 (held out) | 2022-10-27 | 30 d | 29 d | -0.589 | -2.349 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1444-07 (held out) | 2023-01-23 | 30 d | 29 d | -0.756 | -4.004 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1444-09 (held out) | 2023-03-23 | 30 d | 29 d | -0.558 | -2.028 | the calendar rejects a crescent the criterion counts (best class F) |
| 1444-11 (held out) | 2023-05-21 | 30 d | 29 d | -0.410 | -0.515 | the calendar rejects a crescent the criterion counts (best class F) |
| 1445-01 (held out) | 2023-07-19 | 30 d | 29 d | -0.631 | -2.653 | the calendar rejects a crescent the criterion counts (best class F) |
| 1445-02 (held out) | 2023-08-18 | 30 d | 29 d | -0.574 | -2.095 | the calendar rejects a crescent the criterion counts (best class F) |
| 1445-03 (held out) | 2023-09-17 | 30 d | 29 d | -0.472 | -1.107 | marginal: best V -1.107 is at Odeh's zone C/D limit (−0.96) |
| 1445-05 (held out) | 2023-11-15 | 30 d | 29 d | -0.679 | -3.197 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1445-08 (held out) | 2024-02-11 | 30 d | 29 d | -0.848 | -4.910 | the calendar rejects a crescent the criterion counts (best class F) |
| 1445-10 (held out) | 2024-04-10 | 30 d | 29 d | -0.324 | 0.231 | the calendar rejects a crescent the criterion counts (best class F) |
| 1446-01 (held out) | 2024-07-07 | 30 d | 29 d | -0.696 | -3.310 | the calendar rejects a crescent the criterion counts (best class F) |
| 1446-02 (held out) | 2024-08-06 | 30 d | 29 d | -0.625 | -2.598 | the calendar rejects a crescent the criterion counts (best class F) |
| 1446-03 (held out) | 2024-09-05 | 30 d | 29 d | -0.612 | -2.443 | the calendar rejects a crescent the criterion counts (best class F) |
| 1446-06 (held out) | 2024-12-03 | 30 d | 29 d | -0.546 | -1.864 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1446-08 (held out) | 2025-01-31 | 30 d | 29 d | -0.305 | 0.393 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1446-12 (held out) | 2025-05-28 | 30 d | 29 d | -0.573 | -2.201 | the calendar rejects a crescent the criterion counts (best class F) |
| 1447-02 (held out) | 2025-07-26 | 30 d | 29 d | -0.776 | -4.119 | the calendar rejects a crescent the criterion counts (best class F) |
| 1447-03 (held out) | 2025-08-25 | 30 d | 29 d | -0.664 | -2.977 | the calendar rejects a crescent the criterion counts (best class F) |
| 1447-06 (held out) | 2025-11-22 | 30 d | 29 d | -0.870 | -4.986 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1447-07 (held out) | 2025-12-22 | 30 d | 29 d | -0.294 | 0.613 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1447-09 (held out) | 2026-02-19 | 30 d | 29 d | -0.317 | 0.312 | the calendar rejects a crescent the criterion counts (best class F) |
| 1448-01 (held out) | 2026-06-16 | 30 d | 29 d | -0.688 | -3.349 | the calendar rejects a crescent the criterion counts (best class F) |
| 1448-03 (held out) | 2026-08-14 | 30 d | 29 d | -0.926 | -5.598 | the calendar rejects a crescent the criterion counts (best class F) |
| 1448-06 (held out) | 2026-11-11 | 30 d | 29 d | -0.886 | -5.136 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1448-07 (held out) | 2026-12-11 | 30 d | 29 d | -0.433 | -0.703 | marginal: best V -0.703 is at Odeh's zone C/D limit (−0.96); winter evening |
| 1448-09 (held out) | 2027-02-08 | 30 d | 29 d | -0.864 | -4.963 | the calendar rejects a crescent the criterion counts (best class F) |

### Missed by logistic (age, lag, ARCV, ARCL, W), five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1423-10 | 2002-12-06 | 30 d | 29 d | -0.323 | 0.296 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-08 | 2006-08-26 | 30 d | 29 d | -0.343 | 0.201 | the calendar rejects a crescent the criterion counts (best class F) |
| 1429-06 | 2008-06-05 | 29 d | 30 d | -0.312 | 0.324 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1430-04 | 2009-03-28 | 29 d | 30 d | -0.282 | 0.651 | the calendar accepts a fainter crescent than the criterion (best class E) |
| 1430-09 | 2009-08-22 | 29 d | 30 d | -0.654 | -2.927 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1431-10 | 2010-09-10 | 29 d | 30 d | -0.601 | -2.430 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1432-06 | 2011-05-05 | 30 d | 29 d | -0.234 | 1.189 | marginal: best q -0.234 is at Yallop's class D/E limit (−0.232) |
| 1435-01 | 2013-11-05 | 29 d | 30 d | -0.244 | 0.979 | marginal: best q -0.244 is at Yallop's class D/E limit (−0.232); winter evening |
| 1436-02 | 2014-11-24 | 30 d | 29 d | -0.232 | 1.123 | marginal: best q -0.232 is at Yallop's class D/E limit (−0.232); winter evening |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1443-01 | 2021-08-10 | 29 d | 30 d | -0.224 | 1.239 | marginal: best q -0.224 is at Yallop's class D/E limit (−0.232) |
| 1443-09 (held out) | 2022-04-03 | 29 d | 30 d | -0.209 | 1.459 | marginal: best q -0.209 is at Yallop's class D/E limit (−0.232) |
| 1448-04 (held out) | 2026-09-13 | 29 d | 30 d | -0.613 | -2.473 | the calendar accepts a fainter crescent than the criterion (best class F) |

### Missed by logistic, fitted on AH 1428 onward, five cities

| Month | First day | Official | Criterion | Best Yallop q | Best Odeh V | Probable reason |
|---|---|---|---|---|---|---|
| 1423-06 | 2002-08-10 | 30 d | 29 d | -0.253 | 0.889 | marginal: best q -0.253 is at Yallop's class D/E limit (−0.232) |
| 1423-08 | 2002-10-08 | 30 d | 29 d | -0.210 | 1.333 | length set by the official announcement of the next month (a sighting report); winter evening |
| 1423-10 | 2002-12-06 | 30 d | 29 d | -0.323 | 0.296 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
| 1424-01 | 2003-03-05 | 30 d | 29 d | -0.202 | 1.552 | marginal: best q -0.202 is at Yallop's class D/E limit (−0.232) |
| 1424-11 | 2003-12-25 | 30 d | 29 d | -0.211 | 1.354 | marginal: best q -0.211 is at Yallop's class D/E limit (−0.232); winter evening |
| 1425-04 | 2004-05-21 | 30 d | 29 d | -0.083 | 2.723 | the calendar rejects a crescent the criterion counts (best class C) |
| 1425-07 | 2004-08-18 | 30 d | 29 d | 0.003 | 3.514 | the calendar rejects a crescent the criterion counts (best class B) |
| 1425-12 | 2005-01-12 | 30 d | 29 d | -0.170 | 1.724 | the calendar rejects a crescent the criterion counts (best class D); winter evening |
| 1426-10 | 2005-11-04 | 30 d | 29 d | -0.216 | 1.347 | marginal: best q -0.216 is at Yallop's class D/E limit (−0.232); winter evening |
| 1427-08 | 2006-08-26 | 30 d | 29 d | -0.343 | 0.201 | the calendar rejects a crescent the criterion counts (best class F) |
| 1432-06 | 2011-05-05 | 30 d | 29 d | -0.234 | 1.189 | marginal: best q -0.234 is at Yallop's class D/E limit (−0.232) |
| 1432-11 | 2011-09-29 | 30 d | 29 d | -0.473 | -1.223 | marginal: best V -1.223 is at Odeh's zone C/D limit (−0.96) |
| 1435-10 | 2014-07-29 | 30 d | 29 d | -0.497 | -1.311 | the calendar rejects a crescent the criterion counts (best class F) |
| 1436-02 | 2014-11-24 | 30 d | 29 d | -0.232 | 1.123 | marginal: best q -0.232 is at Yallop's class D/E limit (−0.232); winter evening |
| 1437-10 | 2016-07-06 | 29 d | 30 d | -0.529 | -1.690 | the calendar accepts a fainter crescent than the criterion (best class F) |
| 1441-12 | 2020-07-22 | 30 d | 29 d | -0.260 | 0.848 | marginal: best q -0.260 is at Yallop's class D/E limit (−0.232) |
| 1443-03 | 2021-10-08 | 30 d | 29 d | -0.296 | 0.486 | the calendar rejects a crescent the criterion counts (best class F); winter evening |
