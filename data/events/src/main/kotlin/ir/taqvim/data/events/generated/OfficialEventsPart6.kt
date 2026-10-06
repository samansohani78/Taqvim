/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

import ir.taqvim.core.calendar.JewishObservance
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 6 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_6: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.nowruz-3"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید نوروز")),
            rule = EventRule.Fixed(month = 1, day = 3),
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
                        page = "3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.nowruz-4"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید نوروز")),
            rule = EventRule.Fixed(month = 1, day = 4),
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
                        page = "3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.oil-nationalization"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "روز ملی شدن صنعت نفت ایران")),
            rule = EventRule.Fixed(month = 12, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "15",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "14",
                    ),
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/99002",
                        title = "لایحه قانونی تعیین تعطیلات رسمی کشور, approved 1359/04/08: the base list of official holidays, which includes «29 اسفند تا 4 فروردین».",
                    ),
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/93217",
                        title = "قانون اصلاح لایحه قانونی تعیین تعطیلات رسمی کشور, approved 1378/05/25: «…و تعطیلی روز بیست و نهم اسفند ماه لغو می‌گردد» — the 29 Esfand holiday was abolished.",
                    ),
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/93237",
                        title = "قانون راجع به تعطیل روز ملی شدن صنعت نفت, approved 1378/08/18, Guardian Council 1378/08/19: «روز ملی شدن صنعت نفت که مصادف با 29 اسفند ماه هر سال می‌باشد کماکان جزء تعطیلات رسمی کشور محسوب می‌شود» — restored three months after the abolition, which is why every official calendar from 1381 SH prints it as (تعطیل).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.prophet-demise-imam-hasan-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "رحلت حضرت رسول اکرم صلی الله علیه و آله و شهادت حضرت امام حسن مجتبی علیه السلام")),
            rule = EventRule.Fixed(month = 2, day = 28),
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
            id = EventId("ir.holiday.prophet-imam-sadiq-birth"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "ولادت حضرت رسول اکرم صلی الله علیه و آله و ولادت حضرت امام جعفر صادق علیه السلام")),
            rule = EventRule.Fixed(month = 3, day = 17),
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
                        page = "8",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.revolution-victory"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "پیروزی انقلاب اسلامی ایران و سقوط نظام شاهنشاهی")),
            rule = EventRule.Fixed(month = 11, day = 22),
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
            id = EventId("ir.holiday.tasua"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "تاسوعای حسینی")),
            rule = EventRule.Fixed(month = 1, day = 9),
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
            id = EventId("jewish.hanukkah"),
            calendar = CalendarSystem.HEBREW,
            source = EventSource.JEWISH,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "حنوکا",
                        "en" to "Hanukkah",
                    ),
                ),
            rule = EventRule.JewishObservanceDate(observance = JewishObservance.HANUKKAH),
            aliases =
                listOf(
                    "חנוכה",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://aa.usno.navy.mil/data/api.html",
                        title = "U.S. Naval Observatory, Astronomical Applications Department — API v4.0.1, Jewish observances (religious holidays), Hebrew years 360–9999",
                        page = "Column of the archived responses, byte-for-byte in docs/sources/usno/jewish-observances-raw.json; all 57 839 records are the golden of core/calendar's UsnoJewishObservancesTest, which the engine this record's rule calls reproduces exactly. US government work, public domain (17 U.S.C. 105).",
                    ),
                ),
        ),
    )
