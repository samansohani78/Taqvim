/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

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
                    fromYear = 1_404,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%DB%8C%D9%87-%D9%88%D8%B2%D8%A7%D8%B1%D8%AA-%DA%A9%D8%A7%D8%B1-%D9%88-%D8%A7%D9%85%D9%88%D8%B1-%D8%A7%D8%AC%D8%AA%D9%85%D8%A7%D8%B9%DB%8C",
                            title = "Bakhtar News Agency — «اطلاعیه‌ وزارت کار و امور اجتماعی» (Ministry of Labour and Social Affairs holiday announcement), published 2026-02-11",
                            page = "article text: 27 Sha'ban 1447 AH = 26 Dalw 1404 SH, public holiday — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%DB%8C%D9%87-%D9%88%D8%B2%D8%A7%D8%B1%D8%AA-%DA%A9%D8%A7%D8%B1-%D9%88-%D8%A7%D9%85%D9%88%D8%B1-%D8%A7%D8%AC%D8%AA%D9%85%D8%A7%D8%B9%DB%8C",
                        title = "Bakhtar News Agency — «اطلاعیه‌ وزارت کار و امور اجتماعی» (Ministry of Labour and Social Affairs holiday announcement), published 2026-02-11",
                        page = "article text: 27 Sha'ban 1447 AH = 26 Dalw 1404 SH, public holiday",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.ancient.yalda"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.ANCIENT_IRAN,
            category = EventCategory.CULTURAL,
            isHoliday = false,
            title = LocalizedText(mapOf("fa" to "شب یلدا و ترویج فرهنگ میهمانی و پیوند با خویشان")),
            rule = EventRule.Fixed(month = 9, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf) — 30 Azar",
                        page = "12",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf) — 30 Azar",
                        page = "11",
                    ),
                    Citation(
                        url = "https://iranicaonline.org/articles/cella",
                        title = "Encyclopaedia Iranica — ČELLA (Vol. V, Fasc. 2): the great čella begins on 1 Dey; its night is called šab-e čella or šab-e yaldā",
                        page = "123-125",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.ancient.zoroaster-birthday"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.ANCIENT_IRAN,
            category = EventCategory.CULTURAL,
            isHoliday = false,
            title = LocalizedText(mapOf("fa" to "زادروز زرتشت پیامبر")),
            rule = EventRule.Fixed(month = 1, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf) — 6 Farvardin",
                        page = "4",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf) — 6 Farvardin",
                        page = "3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.arbaeen"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "اربعین حسینی")),
            rule = EventRule.Fixed(month = 2, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "8",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "7",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.ashura"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عاشورای حسینی")),
            rule = EventRule.Fixed(month = 1, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "7",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "6",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.eid-al-adha"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید سعید قربان")),
            rule = EventRule.Fixed(month = 12, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "6",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "5",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.eid-al-fitr"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید سعید فطر")),
            rule = EventRule.Fixed(month = 10, day = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "4",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "3, 14",
                    ),
                ),
        ),
    )
