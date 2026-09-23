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

/**
 * Part 16 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_16: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.end-fistula-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پایان دادن به فیستول مامایی",
                        "en" to "International Day to End Obstetric Fistula",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-fistula-day",
                        title = "International Day to End Obstetric Fistula (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-fistula-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-food-waste-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از تلفات و ضایعات غذایی",
                        "en" to "International Day of Awareness of Food Loss and Waste",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-food-waste-day/",
                        title = "International Day of Awareness of Food Loss and Waste (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-food-waste-day/",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-human-trafficking-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مبارزه با قاچاق انسان",
                        "ar" to "اليوم العالمي لمكافحة الاتجار بالأشخاص",
                        "en" to "World Day against Trafficking in Persons",
                        "es" to "Día Mundial contra la Trata",
                        "fr" to "Journée mondiale de la lutte contre la traite d’êtres humains",
                        "ru" to "Всемирный день борьбы с торговлей людьми",
                        "zh" to "世界打击贩运人口日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-human-trafficking-day",
                        title = "World Day against Trafficking in Persons (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/86445-%D9%BE%DB%8C%D8%A7%D9%85-%D8%BA%D8%A7%D8%AF%D8%A7-%D9%81%D8%AA%D8%AD%DB%8C-%D9%88%D8%A7%D9%84%DB%8C-%D9%85%D8%AF%DB%8C%D8%B1-%DA%A9%D9%84-%D8%AF%D9%81%D8%AA%D8%B1-%D9%85%D9%82%D8%A7%D8%A8%D9%84%D9%87-%D8%A8%D8%A7-%D9%85%D9%88%D8%A7%D8%AF-%D9%85%D8%AE%D8%AF%D8%B1-%D9%88-%D8%AC%D8%B1%D9%85-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF%D8%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2",
                        title = "United Nations in the Islamic Republic of Iran — پیام غادا فتحی والی مدیر کل دفتر مقابله با مواد مخدر و جرم سازمان ملل متحد، به مناسبت روز جهانی مبارزه با قاچاق انسان",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/68/192",
                    "un" to "https://www.un.org/en/observances/end-human-trafficking-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-illegal-fishing-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با صید غیرقانونی، گزارش‌نشده و بی‌ضابطه",
                        "en" to "International Day for the Fight against Illegal, Unreported and Unregulated Fishing",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-illegal-fishing-day",
                        title = "International Day for the Fight against Illegal, Unreported and Unregulated Fishing (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-illegal-fishing-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-impunity-crimes-against-journalists"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پایان دادن به معافیت از مجازات جرایم علیه روزنامه‌نگاران",
                        "en" to "International Day to End Impunity for Crimes against Journalists",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-impunity-crimes-against-journalists",
                        title = "International Day to End Impunity for Crimes against Journalists (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-impunity-crimes-against-journalists",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-nuclear-tests-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی علیه آزمایش های هسته ای",
                        "ar" to "اليوم الدولي لمناهضة التجارب النووية",
                        "en" to "International Day against Nuclear Tests",
                        "es" to "Día Internacional contra los Ensayos Nucleares",
                        "fr" to "Journée internationale contre les essais nucléaires",
                        "ru" to "Международный день действий против ядерных испытаний",
                        "zh" to "禁止核试验国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-nuclear-tests-day",
                        title = "International Day against Nuclear Tests (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/64/35",
                    "un" to "https://www.un.org/en/observances/end-nuclear-tests-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-racism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفع تبعیض نژادی",
                        "ar" to "اليوم الدولي للقضاء على التمييز العنصري",
                        "en" to "International Day for the Elimination of Racial Discrimination",
                        "es" to "Día Internacional de la Eliminación de la Discriminación Racial",
                        "fr" to "Journée internationale pour l'élimination de la discrimination raciale",
                        "ru" to "Международный день борьбы за ликвидацию расовой дискриминации",
                        "zh" to "消除种族歧视国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-racism-day",
                        title = "International Day for the Elimination of Racial Discrimination (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-racism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-sexual-violence-in-conflict-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه‌کنی خشونت جنسی در منازعات",
                        "en" to "International Day for the Elimination of Sexual Violence in Conflict",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 19),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-sexual-violence-in-conflict-day",
                        title = "International Day for the Elimination of Sexual Violence in Conflict (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-sexual-violence-in-conflict-day",
                ),
        ),
    )
