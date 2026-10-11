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
 * Part 13 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_13: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("jewish.pesach"),
            calendar = CalendarSystem.HEBREW,
            source = EventSource.JEWISH,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "پسح",
                        "ar" to "عيد الفصح اليهودي",
                        "de" to "Pessach",
                        "en" to "Pesach",
                        "es" to "Pésaj",
                        "fr" to "Pessa’h",
                        "id" to "Pesakh",
                        "ja" to "過越祭",
                        "ms" to "Pesakh",
                        "ru" to "Песах",
                        "zh" to "逾越节",
                    ),
                ),
            rule = EventRule.JewishObservanceDate(observance = JewishObservance.PESACH),
            aliases =
                listOf(
                    "פסח",
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
        EventDefinition(
            id = EventId("jewish.rosh-hashanah"),
            calendar = CalendarSystem.HEBREW,
            source = EventSource.JEWISH,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روش هَشانا",
                        "ar" to "رأس السنة العبرية",
                        "de" to "Rosch Haschana",
                        "en" to "Rosh Hashanah",
                        "es" to "Rosh Hashaná",
                        "fr" to "Roch Hachana",
                        "id" to "Rosh Hashanah",
                        "ja" to "ロシュ・ハシャナ",
                        "ms" to "Rosh Hashanah",
                        "ru" to "Рош ха-Шана",
                        "zh" to "犹太新年",
                    ),
                ),
            rule = EventRule.JewishObservanceDate(observance = JewishObservance.ROSH_HASHANAH),
            aliases =
                listOf(
                    "ראש השנה",
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
        EventDefinition(
            id = EventId("jewish.shavuot"),
            calendar = CalendarSystem.HEBREW,
            source = EventSource.JEWISH,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "شاووعوت",
                        "ar" to "عيد الأسابيع",
                        "de" to "Schawuot",
                        "en" to "Shavuot",
                        "es" to "Shavuot",
                        "fr" to "Chavouot",
                        "id" to "Shavuot",
                        "ja" to "シャブオット",
                        "ms" to "Shavuot",
                        "ru" to "Шавуот",
                        "zh" to "七七节",
                    ),
                ),
            rule = EventRule.JewishObservanceDate(observance = JewishObservance.SHAVUOT),
            aliases =
                listOf(
                    "שבועות",
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
        EventDefinition(
            id = EventId("jewish.succoth"),
            calendar = CalendarSystem.HEBREW,
            source = EventSource.JEWISH,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "سوکوت",
                        "ar" to "عيد المظال",
                        "de" to "Sukkot",
                        "en" to "Succoth",
                        "es" to "Sucot",
                        "fr" to "Souccot",
                        "id" to "Sukkot",
                        "ja" to "仮庵の祭り",
                        "ms" to "Sukkot",
                        "ru" to "Суккот",
                        "zh" to "住棚节",
                    ),
                ),
            rule = EventRule.JewishObservanceDate(observance = JewishObservance.SUCCOTH),
            aliases =
                listOf(
                    "סוכות",
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
