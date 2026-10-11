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
 * Part 28 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_28: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.chinese-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان چینی",
                        "en" to "Chinese Language Day",
                        "ru" to "День китайского языка в ООН",
                        "zh" to "联合国中文日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/russian-language-day",
                            title = "UN Language Days established 2010 by the Department of Global Communications for each of the six official languages",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/zh/observances/chinese-language-day",
                        title = "Chinese Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/zh/observances/chinese-language-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cities-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی شهرها",
                        "ar" to "اليوم العالمي للمدن",
                        "en" to "World Cities Day",
                        "es" to "Día Mundial de las Ciudades",
                        "fr" to "Journée mondiale des villes",
                        "ru" to "Всемирный день городов",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 31),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/cities-day",
                            title = "World Cities Day -- GA resolution 68/239, first celebrated in 2014",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cities-day",
                        title = "World Cities Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/cities-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.civil-aviation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی هواپیمایی غیرنظامی",
                        "ar" to "يوم الطيران المدني الدولي",
                        "en" to "International Civil Aviation Day",
                        "es" to "Día de la Aviación Civil Internacional",
                        "fr" to "Journée de l'aviation civile internationale",
                        "ru" to "Международный день гражданской авиации",
                        "zh" to "国际民航日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/civil-aviation-day",
                        title = "International Civil Aviation Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/51/33",
                    "un" to "https://www.un.org/en/observances/civil-aviation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.clean-energy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی انرژی پاک",
                        "ar" to "اليوم الدولي للطاقة النظيفة",
                        "en" to "International Day of Clean Energy",
                        "es" to "Día Internacional de la Energía Limpia",
                        "fr" to "Journée internationale des énergies propres",
                        "ru" to "Международный день чистой энергии",
                        "zh" to "国际清洁能源日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/clean-energy-day",
                        title = "International Day of Clean Energy (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/259765-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%A7%D9%86%D8%B1%DA%98%DB%8C-%D9%BE%D8%A7%DA%A9",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز جهانی انرژی پاک",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/78/265",
                    "un" to "https://www.un.org/en/observances/clean-energy-day",
                ),
        ),
    )
