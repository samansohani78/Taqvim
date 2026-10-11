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
 * Part 40 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_40: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.epidemic-preparedness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آمادگی در برابر بیماری‌های همه‌گیر",
                        "ar" to "اليوم الدولي للتأهب للأوبئة",
                        "en" to "International Day of Epidemic Preparedness",
                        "es" to "Día Internacional de la Preparación ante las Epidemias",
                        "fr" to "Journée internationale de la préparation aux épidémies",
                        "ru" to "Международный день противоэпидемической готовности",
                        "zh" to "防范流行病国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_020,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/75/27",
                            title = "International Day of Epidemic Preparedness — General Assembly resolution A/RES/75/27, \"Resolution adopted by the General Assembly on 7 December 2020\", para. 1: \"Decides to proclaim 27 December as the International Day of Epidemic Preparedness\".",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/epidemic-preparedness-day",
                        title = "International Day of Epidemic Preparedness (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/epidemic-preparedness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.equal-pay-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی برابری دستمزد",
                        "ar" to "اليوم الدولي للمساواة في الأجر",
                        "en" to "International Equal Pay Day",
                        "es" to "Día Internacional de la Igualdad Salarial",
                        "fr" to "Journée internationale de l’égalité de rémunération",
                        "ru" to "Международный день равной оплаты труда",
                        "zh" to "国际同工同酬日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/equal-pay-day",
                            title = "International Equal Pay Day (A/RES/74/142, 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/equal-pay-day",
                        title = "International Equal Pay Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/equal-pay-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.fair-play-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بازی منصفانه",
                        "ar" to "اليوم العالمي للعب النظيف",
                        "en" to "World Fair Play Day",
                        "es" to "Día Mundial del Juego Limpio",
                        "fr" to "Journée mondiale du fair-play",
                        "ru" to "Всемирный день честной игры",
                        "zh" to "世界公平比赛日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 19),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/fair-play-day",
                            title = "World Fair Play Day — A/RES/78/310 (1 July 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/fair-play-day",
                        title = "World Fair Play Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/fair-play-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.female-genital-mutilation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی عدم تحمل ختنه زنان",
                        "ar" to "اليوم الدولي لعدم التسامح مطلقا إزاء تشويه الأعضاء التناسلية الأنثوية",
                        "en" to "International Day of Zero Tolerance to Female Genital Mutilation",
                        "es" to "Día Internacional de Tolerancia Cero con la Mutilación Genital Femenina",
                        "fr" to "Journée internationale de tolérance zéro à l'égard des mutilations génitales féminines",
                        "ru" to "Международный день нетерпимого отношения к калечащим операциям на женских половых органах",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 6),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/female-genital-mutilation-day",
                            title = "Zero Tolerance to FGM Day — A/RES/67/146 (2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/female-genital-mutilation-day",
                        title = "International Day of Zero Tolerance to Female Genital Mutilation (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/female-genital-mutilation-day",
                ),
        ),
    )
