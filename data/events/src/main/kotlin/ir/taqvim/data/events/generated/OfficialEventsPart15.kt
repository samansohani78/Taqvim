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
 * Part 15 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_15: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.disarmament-non-proliferation-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از خلع سلاح و منع اشاعه",
                        "en" to "International Day for Disarmament and Non-Proliferation Awareness",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                            title = "International Day for Disarmament and Non-Proliferation Awareness -- GA resolution A/RES/77/51 (7 Dec 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                        title = "International Day for Disarmament and Non-Proliferation Awareness (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.disarmament-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته خلع سلاح",
                        "en" to "Disarmament Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 10, day = 24), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_978,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/disarmament-week",
                            title = "General Assembly special session on disarmament, Final Document (resolution S-10/2, 1978): Disarmament Week, 24-30 October",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disarmament-week",
                        title = "General Assembly special session on disarmament, Final Document (resolution S-10/2, 1978): Disarmament Week, 24-30 October",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/disarmament-week",
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
            id = EventId("un.down-syndrome-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی سندرم داون",
                        "en" to "World Down Syndrome Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/down-syndrome-day",
                            title = "World Down Syndrome Day (A/RES/66/149, adopted 19 December 2011; adoption-year convention, consistent with un.vesak-day/ADR-0044)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/down-syndrome-day",
                        title = "World Down Syndrome Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/down-syndrome-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.drowning-prevention-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پیشگیری از غرق‌شدگی",
                        "en" to "World Drowning Prevention Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/drowning-prevention-day",
                            title = "World Drowning Prevention Day — A/RES/75/273 (April 2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/drowning-prevention-day",
                        title = "World Drowning Prevention Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/drowning-prevention-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.duchenne-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آگاهی از دیستروفی عضلانی دوشن",
                        "en" to "World Duchenne Awareness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/duchenne-awareness-day",
                            title = "World Duchenne Awareness Day — A/RES/78/12 (2023, observed beginning 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/duchenne-awareness-day",
                        title = "World Duchenne Awareness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/duchenne-awareness-day",
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
            id = EventId("un.earthquake-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود قربانیان زلزله",
                        "en" to "International Day in Memory of the Victims of Earthquakes",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 29),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/earthquake-victims-day",
                            title = "International Day in Memory of the Victims of Earthquakes — A/RES/79/285 (April 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/earthquake-victims-day",
                        title = "International Day in Memory of the Victims of Earthquakes (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/earthquake-victims-day",
                ),
        ),
    )
