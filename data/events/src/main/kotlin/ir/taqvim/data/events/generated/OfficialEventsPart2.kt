/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

import ir.taqvim.core.calendar.MovableFeast
import ir.taqvim.core.events.ChristianRite
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 2 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_2: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("af.holiday.kabul-victory"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز فتح کابل",
                        "en" to "Day of Victory",
                        "prs" to "روز فتح کابل",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.PERSIAN,
                    fromYear = 1_405,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%B1%D9%88%D8%B2-%D8%B4%D9%86%D8%A8%D9%87-%D8%A2%DB%8C%D9%86%D8%AF%D9%87-%D8%AF%D8%B1-%D8%B3%D8%B1%D8%A7%D8%B3%D8%B1-%DA%A9%D8%B4%D9%88%D8%B1-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A7%D8%B3%D8%AA",
                            title = "Bakhtar News Agency — «روز شنبه آینده در سراسر کشور رخصتی عمومی است» (Ministry of Labour and Social Affairs announcement), published 2026-08-11",
                            page = "article text: 1 Rabi al-Awwal 1448 AH = 24 Asad 1405 SH (Saturday), public holiday — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%B1%D9%88%D8%B2-%D8%B4%D9%86%D8%A8%D9%87-%D8%A2%DB%8C%D9%86%D8%AF%D9%87-%D8%AF%D8%B1-%D8%B3%D8%B1%D8%A7%D8%B3%D8%B1-%DA%A9%D8%B4%D9%88%D8%B1-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A7%D8%B3%D8%AA",
                        title = "Bakhtar News Agency — «روز شنبه آینده در سراسر کشور رخصتی عمومی است» (Ministry of Labour and Social Affairs announcement), published 2026-08-11",
                        page = "article text: 1 Rabi al-Awwal 1448 AH = 24 Asad 1405 SH (Saturday), public holiday",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/en/General-Holiday-Declared-Across-Afghanistan-on-Saturday",
                        title = "Bakhtar News Agency (English) — «General Holiday Declared Across Afghanistan on Saturday» (Ministry of Labor and Social Affairs), published 2026-08-11",
                        page = "article text: Saturday, 1 Rabi al-Awwal 1448 AH = August 15, 2026, public holiday",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.soviet-withdrawal"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "سالروز شکست و اخراج قشون سرخ اتحاد شوروی از افغانستان",
                        "prs" to "سالروز شکست و اخراج قشون سرخ اتحاد شوروی از افغانستان",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 26),
            validity =
                Validity(
                    calendar = CalendarSystem.PERSIAN,
                    fromYear = 1_385,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(10): «26th of month of Dalwe return of former Soviet Union forces». Article 153 enforces the law from its signature and publication in the Official Gazette, which ILO NATLEX record 78309 dates 2007-02-04 = 15 Dalw 1385 SH; 26 Dalw 1385 (2007-02-15) is the first occurrence after it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(10): «26th of month of Dalwe return of former Soviet Union forces». Article 153 enforces the law from its signature and publication in the Official Gazette, which ILO NATLEX record 78309 dates 2007-02-04 = 15 Dalw 1385 SH; 26 Dalw 1385 (2007-02-15) is the first occurrence after it.",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%DB%8C%D9%87-%D9%88%D8%B2%D8%A7%D8%B1%D8%AA-%DA%A9%D8%A7%D8%B1-%D9%88-%D8%A7%D9%85%D9%88%D8%B1-%D8%A7%D8%AC%D8%AA%D9%85%D8%A7%D8%B9%DB%8C",
                        title = "Bakhtar News Agency — «اطلاعیه‌ وزارت کار و امور اجتماعی» (Ministry of Labour and Social Affairs holiday announcement), published 2026-02-11",
                        page = "article text: 27 Sha'ban 1447 AH = 26 Dalw 1404 SH, public holiday",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of Article 41; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.ascension-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز عروج",
                        "en" to "Ascension Day",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.ASCENSION_DAY, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.ash-wednesday"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "چهارشنبهٔ خاکستر",
                        "en" to "Ash Wednesday",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.ASH_WEDNESDAY, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.easter"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید پاک",
                        "en" to "Easter",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.EASTER, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.first-sunday-of-advent"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "نخستین یکشنبهٔ ظهور",
                        "en" to "First Sunday of Advent",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.FIRST_SUNDAY_OF_ADVENT, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.good-friday"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "جمعهٔ نیک",
                        "en" to "Good Friday",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.GOOD_FRIDAY, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("christian.palm-sunday"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "یکشنبهٔ نخل",
                        "en" to "Palm Sunday",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.PALM_SUNDAY, rite = ChristianRite.WESTERN),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Christian observances, Gregorian years 1583–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/christian-observances-raw.json; all 67 336 records are the golden of core/calendar's ChristianMovableFeastsTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
    )
