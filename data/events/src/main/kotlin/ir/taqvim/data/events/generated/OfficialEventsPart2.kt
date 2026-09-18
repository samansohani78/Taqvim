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
        EventDefinition(
            id = EventId("ir.holiday.eid-al-fitr-holiday"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "تعطیل به مناسبت عید سعید فطر")),
            rule = EventRule.Fixed(month = 10, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_433,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://calendar.ut.ac.ir/Fa/",
                            title = "Official calendar of Iran 1391 SH (docs/sources/iran/Calendar-1391.pdf)",
                            page = "8: 2 Shawwal 1433 (1391-05-30) is printed \"تعطیل به مناسبت عید سعید فطر\", the first year it is a holiday. The official calendars of 1381–1390 print 2 Shawwal without (تعطیل), e.g. Calendar-1390.pdf page 10 (2 Shawwal 1432 = 1390-06-10).",
                        ),
                ),
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
        EventDefinition(
            id = EventId("ir.holiday.eid-al-ghadir"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید سعید غدیر خم")),
            rule = EventRule.Fixed(month = 12, day = 18),
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
            id = EventId("ir.holiday.fatima-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "شهادت حضرت فاطمه زهرا سلام الله علیها")),
            rule = EventRule.Fixed(month = 6, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "12",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "10",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.imam-ali-birth"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "ولادت حضرت امام علی علیه السلام")),
            rule = EventRule.Fixed(month = 7, day = 13),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "13",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "12",
                    ),
                ),
        ),
    )
