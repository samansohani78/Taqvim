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
            id = EventId("un.day-against-unilateral-coercive-measures"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مخالفت با اقدامات قهری یکجانبه",
                        "en" to "International Day Against Unilateral Coercive Measures",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 4),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                            title = "International Day against Unilateral Coercive Measures — resolution 79/293 (June 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                        title = "International Day Against Unilateral Coercive Measures (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-for-eradicating-poverty"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه کنی فقر",
                        "ar" to "اليوم الدولي للقضاء على الفقر",
                        "en" to "International Day for the Eradication of Poverty",
                        "es" to "Día Internacional para la Erradicación de la Pobreza",
                        "fr" to "Journée internationale pour l'élimination de la pauvreté",
                        "ru" to "Международный день борьбы за ликвидацию нищеты",
                        "zh" to "消除贫穷国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-for-eradicating-poverty",
                        title = "International Day for the Eradication of Poverty (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/136",
                    "un" to "https://www.un.org/en/observances/day-for-eradicating-poverty",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-of-combating-sand-and-dust-storms"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با طوفان‌های شن و گرد و غبار",
                        "en" to "International Day of Combating Sand and Dust Storms",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                            title = "International Day of Combating Sand and Dust Storms — A/RES/77/294 (8 June 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                        title = "International Day of Combating Sand and Dust Storms (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-of-persons-with-disabilities"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی معلولان",
                        "ar" to "اليوم الدولي للأشخاص ذوي الإعاقة",
                        "en" to "International Day of Persons with Disabilities",
                        "es" to "Día Internacional de las Personas con Discapacidad",
                        "fr" to "Journée internationale des personnes handicapées",
                        "ru" to "Международный день инвалидов",
                        "zh" to "国际残疾人日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                        title = "International Day of Persons with Disabilities (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/3",
                    "un" to "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                ),
        ),
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
    )
