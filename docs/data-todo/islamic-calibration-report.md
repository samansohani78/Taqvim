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
