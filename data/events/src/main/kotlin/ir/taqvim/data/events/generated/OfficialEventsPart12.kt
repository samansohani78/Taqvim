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
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday

/**
 * Part 12 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_12: List<EventDefinition> =
    listOf(
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
                        "en" to "World Cleanup Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 20),
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
                        "fa" to "روز بین المللی بزرگداشت خاطره قربانیان هولوکاست",
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
                        "en" to "World Conjoined Twins Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 24),
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
                        "en" to "International Day of Conscience",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 5),
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
        EventDefinition(
            id = EventId("un.cooperatives-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تعاونیها",
                        "en" to "International Day of Cooperatives",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 7, weekday = Weekday.SATURDAY, n = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cooperatives-day",
                        title = "International Day of Cooperatives (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html; rule: اولين شنبه ژوئيه",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/90",
                    "un" to "https://www.un.org/en/observances/cooperatives-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cotton-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پنبه",
                        "en" to "World Cotton Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cotton-day",
                        title = "World Cotton Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/cotton-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.countering-hate-speech"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مقابله با نفرت‌پراکنی",
                        "en" to "International Day for Countering Hate Speech",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 18),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/countering-hate-speech",
                        title = "International Day for Countering Hate Speech (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/countering-hate-speech",
                ),
        ),
    )
