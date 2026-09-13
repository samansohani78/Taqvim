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
