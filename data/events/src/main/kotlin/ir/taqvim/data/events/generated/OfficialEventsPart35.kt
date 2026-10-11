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
 * Part 35 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_35: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم العالمي لمتلازمة داون",
                        "en" to "World Down Syndrome Day",
                        "es" to "Día Mundial del Síndrome de Down",
                        "fr" to "Journée mondiale de la trisomie 21",
                        "ru" to "Всемирный день людей с синдромом Дауна",
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
                        "ar" to "اليوم العالمي للوقاية من الغرق",
                        "en" to "World Drowning Prevention Day",
                        "es" to "Día Mundial para la Prevención de los Ahogamientos",
                        "fr" to "Journée mondiale de prévention de la noyade",
                        "ru" to "Всемирный день предотвращения утоплений",
                        "zh" to "世界预防溺水日",
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
                        "ar" to "اليوم العالمي للتوعية بمرض الحثل العضلي الدوشيني",
                        "en" to "World Duchenne Awareness Day",
                        "es" to "Día Mundial de Concienciación sobre la Distrofia de Duchenne",
                        "ru" to "Всемирный день распространения информации о мышечной дистрофии Дюшенна",
                        "zh" to "世界杜氏肌营养不良知晓日",
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
                        "fa" to "روز بین‌المللی مادر زمین",
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
    )
