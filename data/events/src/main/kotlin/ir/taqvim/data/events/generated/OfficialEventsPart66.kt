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
 * Part 66 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_66: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.spanish-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان اسپانیایی",
                        "en" to "Spanish Language Day",
                        "es" to "Día del Idioma Español en las Naciones Unidas",
                        "ru" to "День испанского языка в ООН",
                        "zh" to "联合国西班牙文日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 23),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/russian-language-day",
                            title = "UN Language Days established 2010 by the Department of Global Communications for each of the six official languages",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/es/observances/spanish-language-day",
                        title = "Spanish Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/es/observances/spanish-language-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.sport-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ورزش برای توسعه و صلح",
                        "ar" to "اليوم الدولي للرياضة من أجل التنمية والسلام",
                        "en" to "International Day of Sport for Development and Peace",
                        "es" to "Día Internacional del Deporte para el Desarrollo y la Paz",
                        "fr" to "Journée internationale du sport au service du développement et de la paix",
                        "ru" to "Международный день спорта на благо мира и развития",
                        "zh" to "体育促进发展与和平国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/sport-day",
                        title = "International Day of Sport for Development and Peace (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20170507003152/http://www.unic-ir.org:80/index.php?option=com_content&view=article&id=2575:%D9%81%D8%B1%D9%88%D8%B1%D8%AF%DB%8C%D9%86-96-%DA%AF%D8%B1%D8%A7%D9%85%D9%8A%D8%AF%D8%A7%D8%B4%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D9%88%D8%B1%D8%B2%D8%B4-%D8%A8%D8%B1%D8%A7%DB%8C-%D8%AA%D9%88%D8%B3%D8%B9%D9%87-%D9%88-%D8%B5%D9%84%D8%AD&catid=8:%D8%B1%D9%88%DB%8C%D8%AF%D8%A7%D8%AF-%D9%87%D8%A7&Itemid=231&lang=fa",
                        title = "United Nations Information Centre Tehran — فروردین ۹۶ گرامیداشت روز جهانی ورزش برای توسعه و صلح",
                        page = "Wayback Machine snapshot of 2017-05-07 (original site unreachable 2026-09-18)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/67/296",
                    "un" to "https://www.un.org/en/observances/sport-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.srebrenica-genocide-commemoration-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تأمل و یادبود نسل‌کشی سال ۱۹۹۵ سربرنیتسا",
                        "en" to "International Day of Reflection and Commemoration of the 1995 Genocide in Srebrenica",
                        "es" to "Día Internacional de Reflexión y Conmemoración del Genocidio de Srebrenica de 1995",
                        "fr" to "Journée internationale de réflexion et de commémoration du génocide commis à Srebrenica en 1995",
                        "ru" to "Международный день размышления и памяти о геноциде 1995 года в Сребренице",
                        "zh" to "1995年斯雷布雷尼察灭绝种族罪国际反思和纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/srebrenica-genocide-commemoration-day",
                            title = "International Day of Reflection and Commemoration of the 1995 Genocide in Srebrenica — A/RES/78/282 (23 May 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/srebrenica-genocide-commemoration-day",
                        title = "International Day of Reflection and Commemoration of the 1995 Genocide in Srebrenica (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/srebrenica-genocide-commemoration-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.steelpan-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی استیل‌پن",
                        "ar" to "اليوم العالمي لطبول ’’ستيل بان‘‘",
                        "en" to "World Steelpan Day",
                        "es" to "Día Mundial de los Tambores Metálicos",
                        "fr" to "Journée mondiale du steel pan",
                        "ru" to "Всемирный день стального барабана",
                        "zh" to "世界钢鼓日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/steelpan-day",
                            title = "World Steelpan Day — A/RES/77/316 (24 July 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/steelpan-day",
                        title = "World Steelpan Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/steelpan-day",
                ),
        ),
    )
