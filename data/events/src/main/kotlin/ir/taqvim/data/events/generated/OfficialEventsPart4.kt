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
 * Part 4 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_4: List<EventDefinition> =
    listOf(
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
                        "ar" to "عيد القيامة",
                        "de" to "Ostern",
                        "en" to "Easter",
                        "es" to "Pascua de Resurrección",
                        "fr" to "Pâques",
                        "id" to "Paskah",
                        "ja" to "復活祭",
                        "ms" to "Paskah",
                        "ru" to "Пасха",
                        "zh" to "复活节",
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
                        "ar" to "الأحد الأول من زمن المجيء",
                        "de" to "Erster Adventssonntag",
                        "en" to "First Sunday of Advent",
                        "es" to "Primer domingo de Adviento",
                        "fr" to "Premier dimanche de l’Avent",
                        "id" to "Minggu Adven Pertama",
                        "ja" to "待降節第1主日",
                        "ms" to "Ahad Adven Pertama",
                        "ru" to "Первое воскресенье Адвента",
                        "zh" to "将临期第一主日",
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
                        "ar" to "الجمعة العظيمة",
                        "de" to "Karfreitag",
                        "en" to "Good Friday",
                        "es" to "Viernes Santo",
                        "fr" to "Vendredi saint",
                        "id" to "Jumat Agung",
                        "ja" to "聖金曜日",
                        "ms" to "Jumaat Agung",
                        "ru" to "Страстная пятница",
                        "zh" to "耶稣受难日",
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
                        "ar" to "أحد الشعانين",
                        "de" to "Palmsonntag",
                        "en" to "Palm Sunday",
                        "es" to "Domingo de Ramos",
                        "fr" to "Dimanche des Rameaux",
                        "id" to "Minggu Palma",
                        "ja" to "枝の主日",
                        "ms" to "Ahad Palma",
                        "ru" to "Вербное воскресенье",
                        "zh" to "圣枝主日",
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
