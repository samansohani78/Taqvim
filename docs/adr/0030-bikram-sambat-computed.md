# ADR-0030: Bikram Sambat months are computed from Surya Siddhanta sankrantis

- **Status:** Accepted
- **Date:** 2026-09-16
- **Plan reference:** docs/PLAN.md §6 A-07, T-105, T-106; ADR-0002, ADR-0025, ADR-0026; DATA_TODO DT-003, DT-007,
  DT-010

## Context

The plan expected Bikram Sambat (BS) to be table-driven from Nepal government month-length tables supplied by the
owner (DT-003). None were supplied, and the owner asked on 2026-09-15 for every calendar value to come from an
algorithm for any year ("i want all data in this with algoritm not manuly from table or get from api").

BS months are solar: each begins with a sankranti, the Sun's entry into a sidereal zodiac sign, so months have 29 to
32 days. Nepal's official calendar is decided by the Nepal Panchanga Nirnayak Vikas Samiti, which publishes the
yearly National Panchang (https://npns.gov.np/). No published statement of its calculation rules was found. The rules
below were therefore established from its data:

- **Solar theory.** The panchang prints the Sun's sidereal longitude at sunrise every day and the time of each
  sankranti. For BS 2082 and 2083, all 24 printed sankranti times agree within 0.5 minutes with the Sun of the Surya
  Siddhanta (translation of E. Burgess, 1860):
  - 4 320 000 revolutions in 1 577 917 828 civil days (i.29–37);
  - the apsis makes 387 revolutions in a kalpa (i.41);
  - an epicycle of 14°, 20′ smaller at the odd quadrants (ii.34, ii.38);
  - days are counted from the Kali epoch, midnight at Ujjain (i.45–53).

  The modern ephemeris with a fixed ayanamsa does not fit: the implied ayanamsa varies by about 0.5° over the year.
- **Civil day.** The month begins on the Nepal Standard Time (UTC+05:45) day of the sankranti, with two night
  exceptions:
  - a Makara sankranti after sunset starts Magh the next day (BS 2082: 14 January 2026, 21:10, so Magh 1 is
    15 January);
  - a Karka sankranti before sunrise starts Shrawan the previous day (Nepal Telecommunications Authority reports for
    2074 and 2078).
- **Midnight boundary.** For sankrantis just after midnight, the Nepal Telecommunications Authority's monthly reports
  (BS 2070–2078) support the Nepal-midnight boundary in two self-consistent cases:
  - Asar 2072, sankranti at 00:16;
  - Mangsir 2074, sankranti at 00:05.

  They contradict it in one: Falgun 2073, sankranti at 00:01, which is within the model's precision of midnight.

## Decision

1. `NepaliCalendarSystem` implements `CalendarArithmetic` for every `Int` year. Month m of year y starts with the
   sankranti numbered 12 × (y + 3044) + m − 1 after the Kali epoch (`SuryaSiddhantaSun`). The start day is set by the
   day rule and night exceptions above (`NepaliMonthStarts`). Sunrise and sunset at Kathmandu use Meeus' low-precision
   solar formulas (`KathmanduDaylight`); they agree with the panchang's printed times within three minutes.
2. The Surya Siddhanta is a closed-form theory, so there is no ephemeris range. Mean motions are exact in `Long`
   (whole days and a fraction) for every year. Instants far from the present keep about 10-second resolution. The
   Meeus formulas behind the night rules are evaluated within ±20 000 years of J2000 and clamped beyond.
3. Month starts for BS −3000…3000 are computed on first use in immutable 32-year blocks behind `lazy` (as ADR-0026).
   Other years are computed on each call.
4. No astronomy library is used. `CalendarAstronomy` stays the only file that imports cosinekitty.
5. Official data is verification only:
   - the National Panchang 2082/2083 (24 months: first day, weekday, sankranti time);
   - the Telecommunications Authority's printed month ranges (89 reports, BS 2070–2078; later reports use another format).

   Both PDFs are cited by URL and SHA-256, not stored. If a future official calendar differs from the computed one,
   that month is added as an override, as ADR-0026 decision 5 describes.
6. The calendar is not yet shown in the app. The CLDR has no Bikram Sambat month names, and none are verified for
   the 24 languages (DT-007), so screens keep treating `NEPALI` as unavailable until the names exist.

## Consequences

- Agreement:
  - Official panchang: 24/24 month starts, sankranti times within 0.5 minutes.
  - Telecommunications Authority: 84/89 month starts. Four of the five differences contradict the authority's own
    adjacent reports or misprint the year; one is a sankranti one minute after midnight.
- Months whose sankranti falls within a few minutes of Nepal midnight, or of Kathmandu sunrise or sunset for Karka
  and Makara, depend on the model's minute-level precision. A disagreement with a future official calendar there is
  handled by an override.
- The day rule and night exceptions are inferred from official data, not quoted from a published rulebook. The
  committee's written rules, if published, should replace this inference (DT-003).

## Addendum (2026-09-16): names, date format and wiring

**Context.** CLDR has no Bikram Sambat calendar, so the app had no month names, era or date pattern for it
(DT-007, DT-010), and every screen skipped the calendar. The owner decided on 2026-09-16: Nepali script for `ne`, a
documented Latin spelling of the official names for every other language until human translators supply names, and
the calendar in every screen now.

**Decision.**

- **Nepali names** (`core/i18n` `bikram-sambat.properties`, cited there): the civil names printed in the planet-table
  headings of the official Rashtriya Panchangam BS 2083 — वैशाख, जेठ, असार, साउन, भदौ, असोज, कात्तिक, मङ्सिर, पुस,
  माघ, फागुन, चैत. The book prints no heading for the first month, so वैशाख is the spelling of its "वैशाखसंक्रान्ति"
  entry and "वै." day label. The sankranti entries use the Sanskrit forms (ज्येष्ठ, श्रावण, …); the headings' civil
  forms are the ones people use for dates.
- **Latin names:** the Government of Nepal's own English spellings rather than an academic transliteration scheme
  (ISO 15919 or IAST would give forms such as "Vaiśākha" that no Nepali date uses). For each month, the most frequent
  spelling printed on page 1 of the 89 Nepal Telecommunications Authority monthly reports BS 2070–2080: Baishakh,
  Jestha, Ashad, Shrawan, Bhadra, Ashwin, Kartik, Mangsir, Poush, Magh, Falgun, Chaitra. Every language except `ne`
  shows these.
- **Era:** "वि.सं." for `ne`, as printed on the Panchang's cover and headers. The English reports print no era, so
  other languages show none.
- **Date pattern:** no official document shows a full written date format. `ne` uses CLDR's `ne` pattern for its
  other non-Gregorian calendars (`G y MMMM d, EEEE`, e.g. "वि.सं. २०८३ भदौ २८, आइतबार"); other languages write Bikram
  Sambat dates with their own Gregorian pattern and the Latin names (e.g. "Sunday, Bhadra 28, 2083").
- **Wiring:** `IslamicCalendarSelection.arithmeticFor` (`core/events`) is the one lookup from `CalendarSystem` to
  arithmetic, used by the calendar, year, agenda, timeline and Wear screens and by `:app`; `OccurrenceCalculator`,
  the date parser and `taqvim://` links (`nepali`) know the calendar, and settings offer it.

**Consequences.** Non-Nepali users see romanised month names, including in right-to-left and CJK languages, until
translations exist (DT-007 stays open for those). A written Bikram Sambat date format from an official source would
replace the `ne` pattern (DT-010).
