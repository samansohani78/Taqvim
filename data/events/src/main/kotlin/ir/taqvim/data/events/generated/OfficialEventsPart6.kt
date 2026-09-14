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
 * Part 6 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_6: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.day-of-the-seafarer"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز دریانوردان",
                        "ar" to "اليوم الدولي للبحارة",
                        "en" to "Day of the Seafarer",
                        "es" to "Día de la Gente de Mar (OIM)",
                        "fr" to "Journée des gens de mer",
                        "ru" to "День моряка",
                        "zh" to "海员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.imo.org/en/About/Events/Pages/Day-of-the-Seafarer-2025.aspx",
                        title = "Day of the Seafarer (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://www.un.org/sites/un2.un.org/files/dayoftheseafarer-res19.pdf",
                    "un" to "https://www.imo.org/en/About/Events/Pages/Day-of-the-Seafarer-2025.aspx",
                ),
        ),
        EventDefinition(
            id = EventId("un.democracy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی دموکراسی",
                        "ar" to "اليوم الدولي للديمقراطية",
                        "en" to "International Day of Democracy",
                        "es" to "Día Internacional de la Democracia",
                        "fr" to "Journée internationale de la démocratie",
                        "ru" to "Международный день демократии",
                        "zh" to "国际民主日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/democracy-day",
                        title = "International Day of Democracy (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/62/7",
                    "un" to "https://www.un.org/en/observances/democracy-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.desertification-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مبارزه با گسترش کویر و خشکسالی",
                        "ar" to "اليوم العالمي لمكافحة التصحر والجفاف",
                        "en" to "World Day to Combat Desertification and Drought",
                        "es" to "Día Mundial de Lucha contra la Desertificación",
                        "fr" to "Journée mondiale de la lutte contre la désertification et la sécheresse",
                        "ru" to "Всемирный день борьбы с опустыниванием и засухой",
                        "zh" to "防治荒漠化和干旱世界日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/desertification-day",
                        title = "World Day to Combat Desertification and Drought (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/49/115",
                    "un" to "https://www.un.org/en/observances/desertification-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.development-information-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اطلاعات توسعه",
                        "ar" to "اليوم العالمي للإعلام الإنمائي",
                        "en" to "World Development Information Day",
                        "es" to "Día Mundial de Información sobre el Desarrollo",
                        "fr" to "Journée mondiale d'information sur le développement",
                        "ru" to "Всемирный день информации о развитии",
                        "zh" to "世界发展宣传日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/development-information-day",
                        title = "World Development Information Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/development-information-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.diabetes-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دیابت",
                        "ar" to "اليوم العالمي لمرضى السكري",
                        "en" to "World Diabetes Day",
                        "es" to "Día Mundial de la Diabetes",
                        "fr" to "Journée mondiale du diabète",
                        "ru" to "Всемирный день борьбы с диабетом",
                        "zh" to "世界糖尿病日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 14),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/diabetes-day",
                        title = "World Diabetes Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/61/225",
                    "un" to "https://www.un.org/en/observances/diabetes-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.disaster-reduction-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی کاهش مصایب طبیعی",
                        "ar" to "اليوم الدولي للحد من الكوارث",
                        "en" to "International Day for Disaster Risk Reduction",
                        "es" to "Día Internacional para la Reducción de los Desastres",
                        "fr" to "Journée internationale pour la réduction des risques de catastrophes",
                        "ru" to "Международный день по снижению риска бедствий",
                        "zh" to "国际减少灾害风险日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 13),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disaster-reduction-day",
                        title = "International Day for Disaster Risk Reduction (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/64/200",
                    "un" to "https://www.un.org/en/observances/disaster-reduction-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.earth-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مادر زمین",
                        "ar" to "اليوم الدولي لأمنا الأرض",
                        "en" to "International Mother Earth Day",
                        "es" to "Día Internacional de la Madre Tierra",
                        "fr" to "Journée internationale de la Terre nourricière",
                        "ru" to "Международный день Матери-Земли",
                        "zh" to "国际地球母亲日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/earth-day",
                        title = "International Mother Earth Day (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/45428-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86-%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%D9%85%D8%A7%D8%AF%D8%B1-%D8%B2%D9%85%DB%8C%D9%86-%D9%87%D8%B4%D8%AF%D8%A7%D8%B1-%DA%AF%D9%88%D8%AA%D8%B1%D8%B4-%D8%AF%D8%B1-%D9%85%D9%88%D8%B1%D8%AF-%D8%AE%D8%B7%D8%B1-%D9%88%DB%8C%D8%B1%D9%88%D8%B3%E2%80%8C%D9%87%D8%A7-%D9%88-%DA%AF%D8%A7%D8%B2%D9%87%D8%A7%DB%8C-%DA%AF%D9%84%D8%AE%D8%A7%D9%86%D9%87%E2%80%8C%D8%A7%DB%8C",
                        title = "United Nations in the Islamic Republic of Iran — به مناسبت روز بین المللی مادر زمین: هشدار گوترش در مورد خطر ویروس‌ها و گازهای گلخانه‌ای",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/63/278",
                    "un" to "https://www.un.org/en/observances/earth-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.education-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آموزش",
                        "ar" to "اليوم الدولي للتعليم",
                        "en" to "International Day of Education",
                        "es" to "Día Internacional de la Educación",
                        "fr" to "Journée internationale de l'éducation",
                        "ru" to "Международный день образования",
                        "zh" to "国际教育日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/education-day",
                        title = "International Day of Education (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/169386-%D9%BE%DB%8C%D8%A7%D9%85-%D8%A2%D9%86%D8%AA%D9%88%D9%86%DB%8C%D9%88-%DA%AF%D9%88%D8%AA%D8%B1%D8%B4%D8%8C-%D8%AF%D8%A8%DB%8C%D8%B1-%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF%D8%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%D8%A2%D9%85%D9%88%D8%B2%D8%B4",
                        title = "United Nations in the Islamic Republic of Iran — پیام آنتونیو گوترش، دبیر کل سازمان ملل متحد، به مناسبت روز بین‌المللی آموزش",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/73/25",
                    "un" to "https://www.un.org/en/observances/education-day",
                ),
        ),
    )
