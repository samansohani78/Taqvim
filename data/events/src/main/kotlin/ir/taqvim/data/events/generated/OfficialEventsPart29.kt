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
 * Part 29 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_29: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.social-justice-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی عدالت اجتماعی",
                        "ar" to "اليوم العالمي للعدالة الاجتماعية",
                        "en" to "World Day of Social Justice",
                        "es" to "Día Mundial de la Justicia Social",
                        "fr" to "Journée mondiale de la justice sociale",
                        "ru" to "Всемирный день социальной справедливости",
                        "zh" to "世界社会公正日",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/social-justice-day",
                        title = "World Day of Social Justice (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/10",
                    "un" to "https://www.un.org/en/observances/social-justice-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.soil-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خاک",
                        "ar" to "اليوم العالمي للتربة",
                        "en" to "World Soil Day",
                        "es" to "Día Mundial del Suelo",
                        "fr" to "Journée mondiale des sols",
                        "ru" to "Всемирный день почв",
                        "zh" to "世界土壤日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-soil-day",
                        title = "World Soil Day (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/285250-%D8%A7%DB%8C%D8%B1%D8%A7%D9%86-%D8%A8%D8%B1%D9%86%D8%AF%D9%87-%D8%AC%D8%A7%DB%8C%D8%B2%D9%87-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%AE%D8%A7%DA%A9-2024",
                        title = "United Nations in the Islamic Republic of Iran — ایران برنده جایزه روز جهانی خاک ۲۰۲۴",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/68/232",
                    "un" to "https://www.un.org/en/observances/world-soil-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.solstice-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی گرامیداشت انقلاب تابستانی",
                        "en" to "International Day of the Celebration of the Solstice",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/solstice-day",
                        title = "International Day of the Celebration of the Solstice (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/solstice-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.south-south-cooperation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز ملل متحد برای همکاری جنوب - جنوب",
                        "ar" to "يوم الأمم المتحدة للتعاون فيما بين بلدان الجنوب",
                        "en" to "United Nations Day for South-South Cooperation",
                        "es" to "Día de las Naciones Unidas para la Cooperación Sur-Sur",
                        "fr" to "Journée des Nations Unies pour la coopération Sud-Sud",
                        "ru" to "День сотрудничества Юг — Юг Организации Объединенных Наций",
                        "zh" to "联合国南南合作日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/south-south-cooperation-day",
                        title = "United Nations Day for South-South Cooperation (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/220",
                    "un" to "https://www.un.org/en/observances/south-south-cooperation-day",
                ),
        ),
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
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 23),
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
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 11),
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
                        "en" to "World Steelpan Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 11),
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
