# ADR-0006: Umm al-Qura from an embedded, ICU4J-verified table instead of runtime ICU4J

- **Status:** Accepted
- **Date:** 2026-09-13
- **Plan reference:** docs/PLAN.md §6 A-04 ("ICU4J `IslamicCalendar(UMALQURA)` used directly"), §9 (APK ≤ 8 MB),
  §3.1 (`:core:*` KMP-ready)

## Context

A-04 allows ICU4J's Umm al-Qura implementation at runtime. ICU4J is one monolithic artifact (≈ 14 MB with its data
files), which conflicts with the release APK budget of 8 MB, and a JVM-only runtime dependency in
`:core:calendar` works against the KMP-ready goal for `:core:*`. Android's platform `android.icu` cannot be used
from `:core:*` (no `android.*` imports) and its Umm al-Qura data varies with the device's ICU version.

ICU4J's Umm al-Qura calendar is table-driven: for AH 1300–1600 it uses a table of month lengths, and outside that
range it falls back to the civil (tabular, type II) calendar. The data is licensed under the Unicode License v3,
which is on the ADR-0003 allow-list for runtime use.

## Decision

- `UmmAlQuraCalendar` embeds ICU4J 78.3's month lengths for AH 1300–1600 as 301 twelve-bit masks (one per year) and
  delegates to `TabularIslamicCalendar.TYPE_II` outside that range — the same behaviour as ICU4J. Both table
  boundaries coincide with the civil calendar (JDN 2 408 762 and 2 515 427), so there are no gaps or overlaps.
- The masks were produced by querying ICU4J's public API (not by copying ICU source code).
- `UmmAlQuraCalendarTest` is the golden test: it compares the start and length of **every** tabulated month, and
  100 000 random days across and beyond the table, with ICU4J (test scope). When ICU4J is upgraded (Renovate) and the
  data changes, the test fails and prints regenerated masks.
- The Unicode License notice is kept in `licenses/ICU-LICENSE.txt` and must be shown in the in-app open-source
  licenses screen (T-1504).

## Consequences

- No runtime ICU4J: zero APK cost, pure Kotlin, deterministic across devices.
- Data refreshes are explicit, reviewed code changes driven by the golden test.
- ICU4J remains a test-only dependency of `:core:calendar`.
