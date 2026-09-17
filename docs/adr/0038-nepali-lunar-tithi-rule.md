# ADR-0038: Nepal's lunar festivals are a `LunarTithi` rule computed with the Surya Siddhanta

- **Status:** Accepted
- **Date:** 2026-09-17
- **Plan reference:** docs/PLAN.md §5 D-04, §6 A-07; ADR-0007, ADR-0030, ADR-0036

## Context

D-04 adds Nepal's public holidays. The owner's directive of 2026-09-17 (ADR-0036) requires every event to be a rule
that regenerates for any year. About half of Nepal's national holidays are lunar festivals (Dashain, Tihar, Buddha
Jayanti, Janmashtami and others), whose Bikram Sambat and Gregorian dates change every year, so none of the existing
rule types can express them.

The Government of Nepal fixes each year's holidays in a Ministry of Home Affairs notice in the Nepal Rajpatra (Part 5):
BS 2082 (Vol. 74 No. 59) and BS 2083 (Vol. 75 No. 67), both on moha.gov.np. The dates in these notices follow the
national panchang, which ADR-0030 already models for the solar months with the Surya Siddhanta Sun.

The notices give the lunar festivals only as dates. The rule behind each date was reconstructed and checked against
every dated lunar holiday of both notices (21 entries, including the Dashain and Tihar periods, plus Teej and
Gyalpo Lhosar in both years):

- **Tithi.** Tithi n (1–30) runs while the Moon's elongation from the Sun is in [12(n − 1)°, 12n°). Tithis 1–15 are the
  bright half, 16–30 the dark half, and 30 ends at the new moon.
- **Month.** Months are amanta, running from new moon to new moon. Each is named by the solar (BS) month in which its
  opening new moon falls. When a solar month holds two new moons, the first month is adhika (intercalary) and the
  second carries the name; a solar month with no new moon (kshaya) names no month. The panchang's purnimanta names
  for the dark half map to the next amanta number: Bhadra Krishna Ashtami is month 4, tithi 23.
- **Day.** A festival falls on the first civil day (Nepal Standard Time) whose tithi, read at the festival's
  observance moment, is the festival's tithi:
  - sunrise in Kathmandu for most festivals;
  - sunset (pradosha) for Laxmi Puja;
  - the midnight that ends the day (nishitha) for Maha Shivaratri.

  If the tithi begins and ends between two such moments, the festival falls on the day before, when the tithi began.
- **Astronomy.** Modern ephemerides (cosinekitty/astronomy 2.1.19) miss two notice dates. Both are marginal: Chhath
  2082 has an elongation of 59.93° at sunrise, and Phulpati 2083 has 71.96°. The Surya Siddhanta gives 61.96° and
  73.21° and reproduces every date. The Moon (Burgess 1860) moves 57 753 336 revolutions per mahayuga. Its apsis moves
  488 203 revolutions per mahayuga and stands at 90° at the Kali epoch. Its epicycle is 32° less 20′ at the odd
  quadrants. The Sun and the counting of days are those of ADR-0030. Sunrise and sunset use `KathmanduDaylight`.

## Decision

1. **`EventRule.LunarTithi(month, tithi, observance = SUNRISE, endTithi = null, endOffsetDays = 0)`** in
   `:core:events`. A rule with `endTithi` covers a festival period: it runs from the festival day through the day of
   the next `endTithi`, read at sunrise and in the next lunar month when `endTithi` ≤ `tithi`, plus `endOffsetDays`.
   The rule has days only in the `NEPALI` calendar: the schema rejects any other calendar, and the engine returns no
   days for one.
2. **`NepaliLunarDays` and `TithiObservance`** in `:core:calendar` compute the days with the conventions above. They
   use the internal `SuryaSiddhantaMoon` together with the ADR-0030 sankrantis and `KathmanduDaylight`. Nothing is
   tabulated, and every `Int` year works: day counts are reduced modulo the mahayuga before multiplying.
3. **Year membership.** The lunar months named in the previous year and in this year are both evaluated, and only
   their days that fall in the requested BS year are kept. A festival can therefore skip a year: Ram Navami of 2083
   falls in Baisakh 2084, and the 2083 notice omits it.
4. **Holidays expressed with this rule:**

   | Holiday | Rule |
   |---|---|
   | Buddha Jayanti | (1, 15) |
   | Raksha Bandhan | (4, 15) |
   | Janmashtami | (4, 23) |
   | Ghatasthapana | (6, 1) |
   | Dashain | (6, 7) through tithi 12 |
   | Tihar | (6, 30, SUNSET) through tithi 2, plus 1 day |
   | Chhath | (7, 6) |
   | Dhanya Purnima | (8, 15) |
   | Sonam Lhochhar | (10, 1) |
   | Maha Shivaratri | (10, 29, MIDNIGHT) |
   | Ram Navami | (12, 9) |

   `NepalOfficialHolidaysTest` checks the dataset in both directions against the dated days of both notices, and
   `NepaliLunarDaysTest` checks the calendar computation on its own.
5. **Only validated festivals.** A festival is added only when both notices confirm its rule. Gyalpo Lhosar matches
   (11, 1) in both years but follows the Tibetan calendar, whose leap months differ, so it is left out, as are regional
   and group holidays (see D-04 in DATA_TODO).
6. **Nepali-only titles.** The records carry only the official Nepali titles of the notices, with no translations.
   `LocalizedText` therefore requires a Persian (`fa`) *or* a Nepali (`ne`) text, and `forLanguage` falls back from the
   requested language to `fa` and then to `ne`. The schema says the same: `fa` is required unless `ne` is present.
   Translations are left to the translation work. Default visibility follows ADR-0007: `NEPAL_OFFICIAL` is on for `ne`.
7. **Validity.** Every Nepal record is valid from BS 2082, the first notice checked. The Gen Z Martyrs' Day
   (जेनजी सहिद दिवस) first appears in the 2083 notice, so it is valid from BS 2083.

## Consequences

- The dates of future years come from the rule, not from a new notice. When a notice disagrees with the rule, fix
  the convention (and extend the golden test) instead of adding one-off records.
- Eid al-Fitr and Eid al-Adha are `Fixed` rules in the `ISLAMIC` calendar, which the notices leave undated ("the day of
  Eid"). Their day follows the viewer's Islamic variant; Nepal's own moon-sighting announcements are not modelled.
- Users of languages other than Persian and Nepali see Nepal's holidays in Nepali until translations are added.
- The computation costs a few hundred Sun and Moon evaluations for each rule and year, and nothing is cached.
