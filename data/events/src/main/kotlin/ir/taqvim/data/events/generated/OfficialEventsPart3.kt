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
 * Part 3 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_3: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("ir.holiday.imam-ali-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "شهادت حضرت امام علی علیه السلام")),
            rule = EventRule.Fixed(month = 9, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "4, 15",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "14",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.imam-hasan-askari-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "شهادت حضرت امام حسن عسکری علیه السلام و آغاز امامت حضرت ولی عصر (عجل الله تعالی فرجه)")),
            rule = EventRule.Fixed(month = 3, day = 8),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_440,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://calendar.ut.ac.ir/Fa/",
                            title = "Official calendar of Iran 1397 SH (docs/sources/iran/Calendar-1397.pdf)",
                            page = "13: 8 Rabi al-Awwal 1440 (1397-08-25), martyrdom of Imam Hasan Askari and the beginning of the Imamate of the Twelfth Imam, marked (تعطیل) — the first year it is a holiday. The official calendars of 1381–1396 print 8 Rabi al-Awwal without (تعطیل), the last three at Calendar-1394.pdf page 12 (1437), Calendar-1395.pdf page 10 (1438) and Calendar-1396.pdf page 14 (1439); the 1395 and 1396 editions were read from their occasion text only, since their digits do not extract.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "9",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "7",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.imam-mahdi-birth"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "ولادت حضرت قائم عجل الله تعالی فرجه")),
            rule = EventRule.Fixed(month = 8, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "14",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "13",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.imam-reza-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "شهادت حضرت امام رضا علیه السلام")),
            rule = EventRule.LastDayOfMonth(month = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "9",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "7",
                    ),
                ),
        ),
    )
