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
 * Part 32 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_32: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.victims-enforced-disappearance"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قربانیان ناپدیدشدگان اجباری",
                        "en" to "International Day of the Victims of Enforced Disappearances",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/victims-enforced-disappearance",
                        title = "International Day of the Victims of Enforced Disappearances (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/victims-enforced-disappearance",
                ),
        ),
        EventDefinition(
            id = EventId("un.volunteer-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی داوطلبان برای توسعه اقتصادی و اجتماعی",
                        "ar" to "اليوم الدولي للمتطوعين من أجل التنمية الاقتصادية والاجتماعية",
                        "en" to "International Volunteer Day for Economic and Social Development",
                        "es" to "Día Internacional de los Voluntarios",
                        "fr" to "Journée internationale des volontaires",
                        "ru" to "Международный день добровольцев во имя экономического и социального развития",
                        "zh" to "国际志愿人员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/volunteer-day",
                        title = "International Volunteer Day for Economic and Social Development (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/40/212",
                    "un" to "https://www.un.org/en/observances/volunteer-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.water-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آب",
                        "ar" to "اليوم العالمي للمياه",
                        "en" to "World Water Day",
                        "es" to "Día Mundial del Agua",
                        "fr" to "Journée mondiale de l'eau",
                        "ru" to "Всемирный день водных ресурсов",
                        "zh" to "世界水日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/water-day",
                        title = "World Water Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/47/193",
                    "un" to "https://www.un.org/en/observances/water-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.wellness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تندرستی",
                        "en" to "International Wellness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/wellness-day",
                        title = "International Wellness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/wellness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.widows-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی بیوه ها",
                        "ar" to "اليوم الدولي للأرامل",
                        "en" to "International Widows' Day",
                        "es" to "Día Internacional de las Viudas",
                        "fr" to "Journée internationale des veuves",
                        "ru" to "Международный день вдов",
                        "zh" to "国际丧偶妇女日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/widows-day",
                        title = "International Widows' Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/65/189",
                    "un" to "https://www.un.org/en/observances/widows-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-and-girls-in-science-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران در علم",
                        "en" to "International Day of Women and Girls in Science",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 11),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                        title = "International Day of Women and Girls in Science (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-girls-african-descent"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران تبار آفریقایی",
                        "en" to "International Day of Women and Girls of African Descent",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-girls-african-descent",
                        title = "International Day of Women and Girls of African Descent (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-girls-african-descent",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-in-diplomacy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان در دیپلماسی",
                        "en" to "International Day of Women in Diplomacy",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-in-diplomacy-day",
                        title = "International Day of Women in Diplomacy (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-in-diplomacy-day",
                ),
        ),
    )
