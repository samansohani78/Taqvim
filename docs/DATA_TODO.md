# Data TODO

Records that could not be verified from a primary source. Nothing listed here may be guessed. Each row names the
primary source that is needed and the code or data that stays empty until it is available.

| ID | Needed data | Blocks | Required primary source | Status |
|---|---|---|---|---|
| DT-001 | Official leap years 1499–1500 SH and Nowruz instants 1390–1403, 1406–1420 | T-102 acceptance (remaining years), T-104 | University of Tehran, Institute of Geophysics, Calendar Center (calendar.ut.ac.ir) | Partly resolved 2026-09-13: official leap years 1206–1498 and Nowruz instants 1404–1405 are golden fixtures (docs/sources) |
| DT-002 | Official Iranian lunar month starts for the rest of PLAN D-07's range (1390–1410) | T-104, D-07 | University of Tehran Calendar Center / Iran official calendar publications | Partly resolved 2026-09-13: Shawwal 1446 – Shawwal 1448 extracted from the official 1404 and 1405 calendars |
| DT-003 | Bikram Sambat month lengths | T-105, D-04 | Nepal government calendar publications (e.g. Nepal Panchang Nirnayak Samiti) | Waiting for owner-supplied files |
| DT-004 | Central Kurdish (`ckb`) "and" list pattern | T-200 `andPattern` | Unicode CLDR (future release) or a published Sorani grammar/style reference | Open |
| DT-005 | Persian (Solar Hijri) month names in `ckb`, `kmr`, `ne`, `id`, `ms`, `zh` | T-200 `monthNames.persian` | Unicode CLDR (future release) or an official calendar publication in each language | Open |
| DT-006 | Islamic month names in `ckb`, `ne`, `zh` | T-200 `monthNames.islamic` | Unicode CLDR (future release) or an official publication in each language | Open |
| DT-007 | Bikram Sambat month names in all 24 languages (at minimum `ne` and `en`) | T-200 `monthNames.nepali`, T-105 | Nepal government calendar publications; CLDR has no Bikram Sambat calendar | Open |
| DT-008 | Era abbreviations — Persian calendar: ps, ckb, kmr, az, tr, ur, ne, hi, ta, bn, tg, de, es, id, ms, ja; Islamic calendar: ps, ckb, az, ne, hi, ta, ru, de, fr, es, ja | T-202 LONG dates (era omitted) | Unicode CLDR (future release) or official calendar publications in each language | Open |
| DT-009 | Central Kurdish (ckb) relative-time, unit and list patterns; Tajik (tg) unit patterns | T-202 relative phrases and durations return null | Unicode CLDR (future release) or published style references | Open |
| DT-010 | Bikram Sambat date patterns and era name | T-202 (NEPALI uses the Gregorian pattern), T-105 | Nepal government calendar publications | Open |
| DT-011 | Published prayer timetables for Kabul, Istanbul, Berlin and Sydney (12 months each) | T-401 golden set (PLAN: six cities; Tehran and Mashhad plus 29 other Iranian cities are covered by the Institute of Geophysics 1405 tables) | The respective official religious authorities' published timetables (e.g. Afghanistan Ministry of Hajj and Religious Affairs, Türkiye Diyanet, local mosque councils) with the method they state | Open |
| DT-012 | Equinox and solstice instants 2020–2040 | T-403 seasons golden (only 2025/2026 official instants so far) | US Naval Observatory seasons data (aa.usno.navy.mil) — unreachable from the development network; or the Calendar Center's official calendars for further years | Open |
| DT-013 | Published "Moon in Scorpio" (قمر در عقرب) periods for several years, with the zodiac convention used | T-404 golden ("known dates") | A published almanac or calendar that lists these periods and states whether it uses tropical signs or constellation boundaries (the Calendar Center's official 1404/1405 calendars do not list them) | Open |
| DT-014 | Published panchang tithi start/end times for several months and a stated location | T-406 tithi golden | A government or institutional panchang (e.g. Nepal Panchang Nirnayak Samiti, Indian Astronomical Ephemeris) | Open |
| DT-015 | Hijri-Persian (12-animal) year names and their alignment with Solar Hijri years | T-406 year names | A primary Iranian calendar or almanac publication that lists them (the official 1404/1405 calendars do not) | Open |
| DT-016 | Official Chinese lunar new-year dates | T-406 (dates before new year belong to the previous animal) | Hong Kong Observatory or Purple Mountain Observatory published calendars | Open |
| DT-017 | Published golden/blue hour times for fixed places and dates | T-407 ±3 min golden | A published photographers' ephemeris or observatory twilight table with stated altitude thresholds | Open |
| DT-018 | Published natal-chart cusps (Placidus), ascendant, midheaven and Part of Fortune for stated instants and places | T-405 golden | A published ephemeris or table of houses (e.g. a printed Placidus table of houses) with its time and coordinate conventions | Open |
