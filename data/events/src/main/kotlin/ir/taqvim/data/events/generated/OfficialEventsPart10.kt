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
 * Part 10 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_10: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.international-day-of-reflection-on-the-1994-genocide"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز یادبود قربانیان نسل کشی رواندا",
                        "ar" to "اليوم الدولي للتفكر في الإبادة الجماعية التي وقعت في عام 1994 ضد التوتسي في رواندا",
                        "en" to "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda",
                        "es" to "Día Mundial de la Salud",
                        "fr" to "Journée internationale de réflexion sur le génocide des Tutsis au Rwanda en 1994",
                        "ru" to "Международный день памяти о геноциде тутси в Руанде в 1994 году",
                        "zh" to "1994年卢旺达境内针对图西人实施的灭绝种族罪国际反思日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                        title = "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/234",
                    "un" to "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-solidarity-with-the-palestinian-people"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همبستگی با مردم فلسطین",
                        "ar" to "اليوم الدولي للتضامن مع الشعب الفلسطيني",
                        "en" to "International Day of Solidarity with the Palestinian People",
                        "es" to "Día Internacional de Solidaridad con el Pueblo Palestino",
                        "fr" to "Journée internationale de solidarité avec le peuple palestinien",
                        "ru" to "Международный день солидарности с палестинским народом",
                        "zh" to "声援巴勒斯坦人民国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-solidarity-with-the-palestinian-people",
                        title = "International Day of Solidarity with the Palestinian People (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/32/40",
                    "un" to "https://www.un.org/en/observances/international-day-of-solidarity-with-the-palestinian-people",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-peace"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی صلح",
                        "ar" to "اليوم الدولي للسلام",
                        "en" to "International Day of Peace",
                        "es" to "Día Internacional de la Paz",
                        "fr" to "Journée internationale de la paix",
                        "ru" to "Международный день мира",
                        "zh" to "国际和平日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-peace",
                        title = "International Day of Peace (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/36/67",
                    "un" to "https://www.un.org/en/observances/international-day-peace",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-literacy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی سواد آموزی",
                        "ar" to "اليوم الدولي لمحو الأمية",
                        "en" to "International Literacy Day",
                        "es" to "Día Internacional de la Alfabetización",
                        "fr" to "Journée internationale de l'alphabétisation",
                        "ru" to "Международный день грамотности",
                        "zh" to "国际扫盲日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/literacy",
                        title = "International Literacy Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000114048.xml=http://www.unesco.org/ulis/cgi-bin/ulis.pl",
                    "un" to "https://www.unesco.org/en/days/literacy",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-nowruz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی نوروز",
                        "ar" to "يوم نوروز الدولي",
                        "en" to "International Day of Nowruz",
                        "es" to "Día Internacional del Novruz",
                        "fr" to "Journée internationale du Novruz",
                        "ru" to "Международный день Навруз",
                        "zh" to "国际诺鲁孜节",
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
                        url = "https://www.un.org/en/observances/international-nowruz-day",
                        title = "International Day of Nowruz (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/64/253",
                    "un" to "https://www.un.org/en/observances/international-nowruz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.migrants-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مهاجران",
                        "ar" to "اليوم الدولي للمهاجرين",
                        "en" to "International Migrants Day",
                        "es" to "Día Internacional del Migrante",
                        "fr" to "Journée internationale des migrants",
                        "ru" to "Международный день мигранта",
                        "zh" to "国际移民日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 18),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/migrants-day",
                        title = "International Migrants Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/55/93",
                    "un" to "https://www.un.org/en/observances/migrants-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.mine-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی آگاهی از مین و کمک به اقدام علیه مین",
                        "ar" to "اليوم الدولي للتوعية بخطر الألغام",
                        "en" to "International Day for Mine Awareness and Assistance in Mine Action",
                        "es" to "Día Internacional de información sobre el peligro de las minas y de asistencia para las actividades relativas a las minas",
                        "fr" to "Journée internationale pour la sensibilisation aux mines et l'assistance à la lutte antimines",
                        "ru" to "Международный день просвещения по вопросам минной опасности и помощи в деятельности, связанной с разминированием",
                        "zh" to "国际提高地雷意识和协助地雷行动日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 4),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/mine-awareness-day",
                        title = "International Day for Mine Awareness and Assistance in Mine Action (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/60/97",
                    "un" to "https://www.un.org/en/observances/mine-awareness-day",
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
    )
