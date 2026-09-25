# Primary source documents

## Iranian sources (`docs/sources/iran/`)

The Calendar Center's official calendars (1381–1405 SH), prayer timetables, Nowruz instants, leap-year table,
notices and the papers the owner supplied are listed, with their SHA-256, kind, year(s) and layout, in
[`docs/sources/iran/MANIFEST.md`](iran/MANIFEST.md) (moved there from this file on 2026-09-18).

## Other documents

| File | Pages | Bytes | SHA-256 | Content |
|---|---|---|---|---|
| `nepal/moha-public-holidays-2083.pdf` | 12 | 8618093 | `ffbabf83cad254b485504627f9e47fce6c1a7a28e2693a399ae88bf9c0279a29` | Nepal Rajpatra Part 5, Vol. 75 No. 67 (18 Falgun 2082): the Ministry of Home Affairs notice of public holidays for BS 2083, cited by D-04 and DT-038. Scanned images with no text layer, so it is read page by page; the hash matches the one PROVENANCE already cited for https://moha.gov.np/page/government-and-public-holidays-in-2083. |
| `unic-tehran-f-event-20121005.html` | — | 78362 | `0e15f1085c6d991536f67c4344d568170060389a738fa1a809592b931cda281d` (as committed, LF line endings; the downloaded CRLF file was 79037 bytes, `27ec144172049388c4a9f9f63e8e7b8fe1ce8646eef84a89a42bb2af759fa8b7`) | United Nations Information Centre Tehran, "مناسبت های ویژه سازمان ملل متحد" (UN special days, Persian), https://www.unic-ir.org/event/f-event.htm — Internet Archive copy https://web.archive.org/web/20121005003400id_/https://www.unic-ir.org/event/f-event.htm (retrieved 2026-09-13; the original site returns HTTP 522/403). Persian titles for D-05. |

Nepal (Bikram Sambat) sources: none supplied yet (T-105, D-04 remain blocked).

## USNO Astronomical Applications API (`docs/sources/usno/`)

Downloaded by the repository owner on 2026-09-15 from the U.S. Naval Observatory API v4.0.1
(https://aa.usno.navy.mil/data/api.html; unreachable from the development network). US Government work, public
domain. Each file is the owner's combined list of raw per-year JSON responses, archived byte for byte; checksums
are also in `docs/sources/usno/SHA256SUMS`. Use: ADR-0025.

| File | Pages | Bytes | SHA-256 | Content |
|---|---|---|---|---|
| `usno/seasons-raw.json` | — | 188750 | `8732302eb29bb6d939a97a6930f39a0e04a0787ae34524f091fb537b76407950` | Earth's seasons and apsides, 1700–2100 (401 responses) |
| `usno/moon-phases-raw.json` | — | 1403584 | `d5e58f840683a3283c0e546aab5ac7a2f46d300cf2903c3914892848be5b7439` | Moon phases, 1700–2100 (401 responses) |
| `usno/solar-eclipses-raw.json` | — | 62093 | `85806f51a6f99f1bf34cdbc8ef54be7ab5e63e8a80f2f0b625e049ada41a6e52` | Solar eclipses of each year, 1800–2050 (251 responses) |
| `usno/islamic-observances-raw.json` | — | 2807329 | `ec020d97e23d0eab3f80a21083fabc37160e5455223616e97188a457a93d65e3` | Islamic New Year, 1 Ramadan, 1 Shawwal, 622–9999 (9 378 responses) |
| `usno/jewish-observances-raw.json` | — | 6130697 | `6522a5c8b970c2f5928f5f99da89708a344ca8622e4956b23a35ba821655e053` | Six Jewish observances, 360–9999 (9 640 responses) |
| `usno/christian-observances-raw.json` | — | 4515320 | `6cdda8a16d3804c34af2f7d16c656eb5a0f87885d7dcb73fbdc93c9e760945a6` | Eight Christian movable observances, 1583–9999 (8 417 responses) |
| `usno/us-daylight-saving-raw.json` | — | 1379397 | `601c14a65dfc271b05f7c5c41077009f5a327cee9b91f53db155c106e699bdb6` | US daylight saving time begin/end, 1967–9999 (8 033 responses) |

## R. H. van Gent, "The Umm al-Qura Calendar of Saudi Arabia" (cited, not archived)

Pages of R. H. van Gent's website (Mathematical Institute, Utrecht University;
https://webspace.science.uu.nl/~gent0113/islam/ummalqura.htm, a frameset), retrieved 2026-09-15 with `curl`. Each
page carries "© R.H. van Gent (June 2026)" in its menu frame and grants no permission to copy, so on the owner's
decision (2026-09-15) the copies are not kept in the repository: only the URL, retrieval date, size and SHA-256 of
the retrieved copy (LF-normalised, and as downloaded with CRLF) are recorded. Use: ADR-0028 (Umm al-Qura criterion
and published years).

| File | Pages | Bytes | SHA-256 | Content |
|---|---|---|---|---|
| `ummalqura_rules.htm` (not archived) | — | 5963 | `9d89cf478c68e87997b62a73959b11de31aabc4e274b2826ed822190f80efc9f` (downloaded CRLF file 6046 bytes, `94ff721b170a2da40ba3b63396140aaad32158898027a66aaab9a4bc0594248b`) | "The Astronomical Rules Governing the Umm al-Qura Calendar": rules before 1392 AH (uncertain), 1392–1419, 1420–1422 and since 1423 AH (conjunction before sunset and moonset after sunset at Mecca), https://webspace.science.uu.nl/~gent0113/islam/ummalqura_rules.htm |
| `ummalqura_introduction.htm` (not archived) | — | 3735 | `011fcbe3f747fcdb9f5d72ce7a42ec4ccbbb4a57c92973bd58bf6722d922c5f5` (downloaded CRLF file 3791 bytes, `58875f5e53eee64496801804c5e4b19994d82485936452c1f8b309e135d3239e`) | Introduction: the Kaʿba in the Great Mosque of Mecca defines the latitude and longitude for which the calendar is calculated, https://webspace.science.uu.nl/~gent0113/islam/ummalqura_introduction.htm |
| `ummalqura_bibliography.htm` (not archived) | — | 9750 | `beb5307c16683379d7663dc682a5977b33911b4e1bcbf2e0dcc2724fbb7146c6` (downloaded CRLF file 9894 bytes, `2e77df35512aabd4ae196985d3407fa8391febb8479a69434ba4fedf34ff9aeb`) | Literature: the Saudi Ministry of Finance's printed comparison calendars *Taqwīm Umm al-Qurá al-Muqāran* 1300–1429 AH (1992/93) and 1420–1450 AH (2003), KFUPM comparison calendar 1356–1411 AH, al-Mostafa (2005) "Lunar Calendars: The New Saudi Arabian Criterion", https://webspace.science.uu.nl/~gent0113/islam/ummalqura_bibliography.htm |
