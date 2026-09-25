# ADR-0048: `validity.fromYear` says when an observance began, not when we found a document

- **Status:** Accepted
- **Date:** 2026-09-25
- **Plan reference:** docs/PLAN.md D-03; DT-031, DT-032; ADR-0036 (events are rules)

## Context

Every record in `dataset/afghanistan/afghanistan-official-holidays.json` carried a `validity.fromYear` of the year the
announcement we happened to find was published — 1404 or 1405 SH, 1447 AH. The app therefore showed **no Afghan public
holiday at all before 2025**: an Afghan user looking at 1403, or at any earlier year, saw an empty calendar, including
on Eid al-Fitr and Eid al-Adha, which have been public holidays there for decades.

That was an artefact of how the data was gathered, not a statement about the world. The announcements are yearly
notices of *which days are off this year*; they say nothing about when the holiday began. Article 41 of the Labour Law
(No. 35 of 2007), found for DT-032, does: it lists the public holidays themselves.

Two facts fix the start date. Article 153 enforces the law "from the date, signed by the president and published in the
Official Gazette". ILO NATLEX record 78309 dates that publication **2007-02-04** — a date the PDF itself does not
carry, recovered from an archived copy of the NATLEX record. Converted with this project's own calendars, that is
**15 Dalw 1385 SH** and **Muharram 1428 AH**.

## Decision

1. **A record's `validity.fromYear` states when the observance began, as its citation evidences it** — not when the
   document reporting it was published.
2. **Law-fixed records start from the law**, in the era of their own rule: 1385 SH for 26 Dalw, **1386 SH** for
   28 Asad, and 1428 AH for the Eid spans. 28 Asad is the exception that proves the care needed: 28 Asad 1385 falls on
   2006-08-19, *before* the law took force, so its first covered occurrence is 28 Asad 1386 (2007-08-19).
3. **Announcement-only records keep their announced year**, because that is all their source establishes. 24 Asad
   (Kabul victory) stays at 1405 SH: Article 41 does not list it — it is an Islamic Emirate addition — so nothing
   extends it backwards.
4. **Where the law and a yearly announcement both cover a holiday, the law sets `validity` and both are cited.**

## Consequences

- An Afghan user now sees the holidays that existed in past years. For 1400 SH the app shows nine: Eid al-Fitr
  (13–15 May 2021), Arafa and Eid al-Adha (20–23 July 2021), Independence (19 August 2021) and the Soviet withdrawal
  (15 February 2022). Before this change it showed none.
- Extending a record backwards says only that *that* holiday existed then. It does not assert that the current list
  applied then, and the Emirate has demonstrably edited the list (it added 24 Asad). Records whose history is
  uncertain were deliberately left alone rather than reconstructed — the goal is to stop hiding holidays that
  certainly existed, not to invent a history.
- The golden now covers 1400 SH as well as 1404 and 1405, so a regression that re-hides the earlier years fails.
- One discrepancy is recorded rather than resolved: the dataset cites the Dari text as Official Gazette **966**, the
  number in the file the ministry publishes, while the archived NATLEX record for the same law says Official Gazette
  **914** of 2007-02-04. Both are cited; the in-force date used here is NATLEX's, and DT-032 keeps the discrepancy
  open.
