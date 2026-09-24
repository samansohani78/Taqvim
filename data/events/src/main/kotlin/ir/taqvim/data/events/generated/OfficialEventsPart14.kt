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
 * Part 14 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_14: List<EventDefinition> =
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
            id = EventId("un.deafblindness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ناشنوایی-نابینایی",
                        "en" to "International Day of Deafblindness",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/deafblindness-day",
                            title = "International Day of Deafblindness — A/RES/79/294 (16 June 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/deafblindness-day",
                        title = "International Day of Deafblindness (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/deafblindness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.delegates-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی نمایندگان",
                        "en" to "International Delegate's Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/delegates-day",
                            title = "International Delegate's Day — resolution 73/286 (2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/delegates-day",
                        title = "International Delegate's Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/delegates-day",
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
            id = EventId("un.detained-staff-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همبستگی با کارکنان بازداشت‌شده و مفقود",
                        "en" to "International Day of Solidarity with Detained and Missing Staff Members",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_994,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/detained-staff-day",
                            title = "International Day of Solidarity with Detained and Missing Staff Members -- linked to GA resolution A/RES/49/59 (1994)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/detained-staff-day",
                        title = "International Day of Solidarity with Detained and Missing Staff Members (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/detained-staff-day",
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
    )
