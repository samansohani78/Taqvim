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
 * Part 12 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_12: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.prophet-imam-sadiq-birth"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "ولادت حضرت رسول اکرم صلی الله علیه و آله و ولادت حضرت امام جعفر صادق علیه السلام",
                        "en" to "Birth of Prophet Muhammad (peace be upon him and his family) and Imam Jafar al-Sadiq (peace be upon him)",
                    ),
                ),
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
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "پیروزی انقلاب اسلامی ایران و سقوط نظام شاهنشاهی",
                        "en" to "Victory of the Islamic Revolution of Iran and fall of the monarchy",
                    ),
                ),
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
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "تاسوعای حسینی",
                        "en" to "Tasua",
                    ),
                ),
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
                        "ar" to "حانوكا",
                        "de" to "Chanukka",
                        "en" to "Hanukkah",
                        "es" to "Janucá",
                        "fr" to "Hanoucca",
                        "id" to "Hanukkah",
                        "ja" to "ハヌカー",
                        "ms" to "Hanukkah",
                        "ru" to "Ханука",
                        "zh" to "光明节",
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
