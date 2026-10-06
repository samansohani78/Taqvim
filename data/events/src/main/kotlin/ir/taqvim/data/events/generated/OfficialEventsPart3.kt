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
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 3 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_3: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("christian.pentecost"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "پنطیکاست",
                        "en" to "Pentecost",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.PENTECOST, rite = ChristianRite.WESTERN),
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
            id = EventId("christian.trinity-sunday"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.CHRISTIAN,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "یکشنبهٔ تثلیث",
                        "en" to "Trinity Sunday",
                    ),
                ),
            rule = EventRule.ChristianFeastDate(feast = MovableFeast.TRINITY_SUNDAY, rite = ChristianRite.WESTERN),
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
