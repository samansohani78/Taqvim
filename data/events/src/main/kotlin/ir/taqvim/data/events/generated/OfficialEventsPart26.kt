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
 * Part 26 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_26: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.moon-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ماه",
                        "en" to "International Moon Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/moon-day",
                            title = "International Moon Day — A/RES/76/76 (9 December 2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/moon-day",
                        title = "International Moon Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/moon-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.mother-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی زبان مادری",
                        "ar" to "اليوم الدولي للغة الأم",
                        "en" to "International Mother Language Day",
                        "es" to "Día Internacional de la Lengua Materna",
                        "fr" to "Journée internationale de la langue maternelle",
                        "ru" to "Международный день родного языка",
                        "zh" to "国际母语日",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/mother-language-day",
                        title = "International Mother Language Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/56/262",
                    "un" to "https://www.un.org/en/observances/mother-language-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.mountain-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی کوهستان",
                        "ar" to "اليوم الدولي للجبال",
                        "en" to "International Mountain Day",
                        "es" to "Día Internacional de las Montañas",
                        "fr" to "Journée internationale de la montagne",
                        "ru" to "Международный день гор",
                        "zh" to "国际山岳日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 11),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/mountain-day",
                        title = "International Mountain Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/245",
                    "un" to "https://www.un.org/en/observances/mountain-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.multilateralism-for-peace-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی چندجانبه‌گرایی و دیپلماسی برای صلح",
                        "en" to "International Day of Multilateralism and Diplomacy for Peace",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/multilateralism-for-peace-day",
                            title = "International Day of Multilateralism and Diplomacy for Peace — A/RES/73/127 (12 December 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/Multilateralism-for-Peace-day",
                        title = "International Day of Multilateralism and Diplomacy for Peace (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/Multilateralism-for-Peace-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.nelson-mandela-international-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی نلسون ماندلا",
                        "ar" to "اليوم الدولي لنيلسون مانديلا",
                        "en" to "Nelson Mandela International Day",
                        "es" to "Día Internacional de Nelson Mandela",
                        "fr" to "Journée internationale Nelson Mandela",
                        "ru" to "Международный день Нельсона Манделы",
                        "zh" to "纳尔逊·曼德拉国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 18),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.un.org/en/events/mandeladay/",
                        title = "Nelson Mandela International Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/64/13",
                    "un" to "http://www.un.org/en/events/mandeladay/",
                ),
        ),
        EventDefinition(
            id = EventId("un.neutrality-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی بی‌طرفی",
                        "en" to "International Day of Neutrality",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/neutrality-day",
                            title = "International Day of Neutrality — resolution 71/275 (2 February 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/neutrality-day",
                        title = "International Day of Neutrality (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/neutrality-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.non-self-governing-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته همبستگی با مردمان سرزمین‌های غیرخودمختار",
                        "en" to "Week of Solidarity with the Peoples of Non-Self-Governing Territories",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 5, day = 25), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_999,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/non-self-governing-week",
                            title = "General Assembly resolution A/RES/54/91 (1999): Week of Solidarity with the Peoples of Non-Self-Governing Territories",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/non-self-governing-week",
                        title = "General Assembly resolution A/RES/54/91 (1999): Week of Solidarity with the Peoples of Non-Self-Governing Territories",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/non-self-governing-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.non-violence-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی عدم خشونت",
                        "ar" to "اليوم الدولي للاعنف",
                        "en" to "International Day of Non-Violence",
                        "es" to "Día Internacional de la No Violencia",
                        "fr" to "Journée internationale de la non-violence",
                        "ru" to "Международный день ненасилия",
                        "zh" to "国际非暴力日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/non-violence-day",
                        title = "International Day of Non-Violence (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/61/271",
                    "un" to "https://www.un.org/en/observances/non-violence-day",
                ),
        ),
    )
