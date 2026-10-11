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
 * Part 29 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_29: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.cleanup-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پاکسازی",
                        "ar" to "اليوم العالمي لتنظيف البيئة",
                        "en" to "World Cleanup Day",
                        "es" to "Día Mundial de la Limpieza",
                        "ru" to "Всемирный день чистоты",
                        "zh" to "世界清洁日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/cleanup-day",
                            title = "World Cleanup Day — resolution 78/122 (8 December 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cleanup-day",
                        title = "World Cleanup Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/cleanup-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.commemoration-holocaust-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی بزرگداشت خاطره قربانیان هولوکاست",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا محرقة اليهود",
                        "en" to "International Day of Commemoration in Memory of the Victims of the Holocaust",
                        "es" to "Día Internacional de Conmemoración anual en memoria de las víctimas del Holocausto",
                        "fr" to "Journée internationale dédiée à la mémoire des victimes de l'Holocauste",
                        "ru" to "Международный день памяти жертв Холокоста",
                        "zh" to "缅怀大屠杀受难者国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/commemoration-holocaust-victims-day",
                        title = "International Day of Commemoration in Memory of the Victims of the Holocaust (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/60/7",
                    "un" to "https://www.un.org/en/observances/commemoration-holocaust-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.conjoined-twins-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دوقلوهای به‌هم‌چسبیده",
                        "ar" to "اليوم العالمي للتوائم الملتصقة",
                        "en" to "World Conjoined Twins Day",
                        "es" to "Día Mundial de los Gemelos Unidos",
                        "ru" to "Всемирный день сросшихся близнецов",
                        "zh" to "世界连体双胞胎日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/conjoined-twins-day",
                            title = "World Conjoined Twins Day — A/RES/78/313 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/conjoined-twins-day",
                        title = "World Conjoined Twins Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/conjoined-twins-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.conscience-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی وجدان",
                        "ar" to "اليوم الدولي للضمير",
                        "en" to "International Day of Conscience",
                        "es" to "Día Internacional de la Conciencia",
                        "fr" to "Journée internationale de la conscience",
                        "ru" to "Международный день нравственности",
                        "zh" to "国际良心日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/conscience-day",
                            title = "International Day of Conscience (UN General Assembly A/RES/73/329, adopted 25 Jul 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/conscience-day",
                        title = "International Day of Conscience (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/conscience-day",
                ),
        ),
    )
