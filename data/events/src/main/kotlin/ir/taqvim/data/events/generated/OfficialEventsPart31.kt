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
 * Part 31 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_31: List<EventDefinition> =
    listOf(
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
                        "en" to "World Steelpan Day",
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
        EventDefinition(
            id = EventId("un.sustainable-gastronomy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی گاسترونومی پایدار",
                        "en" to "Sustainable Gastronomy Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://research.un.org/en/docs/ga/quick/regular/71",
                            title = "Sustainable Gastronomy Day (A/RES/71/246, adopted 21 December 2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/sustainable-gastronomy-day/en",
                        title = "Sustainable Gastronomy Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/sustainable-gastronomy-day/en",
                ),
        ),
        EventDefinition(
            id = EventId("un.sustainable-transport-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی حمل‌ونقل پایدار",
                        "en" to "World Sustainable Transport Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 26),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/sustainable-transport-day",
                            title = "World Sustainable Transport Day — A/RES/77/286 (2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/sustainable-transport-day",
                        title = "World Sustainable Transport Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/sustainable-transport-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.telecommunication-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ارتباطات",
                        "ar" to "اليوم العالمي للاتصالات ومجتمع المعلومات",
                        "en" to "World Telecommunication and Information Society Day",
                        "es" to "Día Mundial de las Telecomunicaciones y la Sociedad de la Información",
                        "fr" to "Journée mondiale des télécommunications et de la société de l'information",
                        "zh" to "世界电信和信息社会日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/telecommunication-day",
                        title = "World Telecommunication and Information Society Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-18); copy in docs/sources/unic-tehran-f-event-20121005.html; row \"روز جهانی ارتباطات — 17 مه\"",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/60/252",
                    "un" to "https://www.un.org/en/observances/telecommunication-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.terrorism-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یاد بود و احترام به قربانیان تروریسم",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا الإرهاب وإجلالهم",
                        "en" to "International Day of Remembrance and Tribute to the Victims of Terrorism",
                        "es" to "Día Internacional de Conmemoración y Homenaje a las Víctimas del Terrorismo",
                        "fr" to "Journée internationale du souvenir, en hommage aux victimes du terrorisme",
                        "ru" to "Международный день памяти и поминовения жертв терроризма",
                        "zh" to "纪念和悼念恐怖主义受害者国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/terrorism-victims-day",
                        title = "International Day of Remembrance and Tribute to the Victims of Terrorism (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/141109-%D9%BE%DB%8C%D8%A7%D9%85-%D8%A2%D9%86%D8%AA%D9%88%D9%86%DB%8C%D9%88-%DA%AF%D9%88%D8%AA%D8%B1%D8%B4%D8%8C-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%DB%8C%D8%A7%D8%AF-%D8%A8%D9%88%D8%AF-%D9%88-%D8%A7%D8%AD%D8%AA%D8%B1%D8%A7%D9%85-%D8%A8%D9%87",
                        title = "United Nations in the Islamic Republic of Iran — پیام آنتونیو گوترش، دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی یاد بود و احترام به قربانیان تروریسم، ۲۱ آگوست ۲۰۲۱ برابر با ۳۰ مرداد ۱۴۰۰",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/72/165",
                    "un" to "https://www.un.org/en/observances/terrorism-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.toilet-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی سرویس بهداشتی",
                        "ar" to "اليوم العالمي لدورات المياه",
                        "en" to "World Toilet Day",
                        "es" to "Día Mundial del Retrete",
                        "fr" to "Journée mondiale des toilettes",
                        "ru" to "Всемирный день туалета",
                        "zh" to "世界厕所日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 19),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/toilet-day",
                        title = "World Toilet Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20160801193354/http://unic-ir.org/index.php?option=com_content&view=article&id=199:%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D9%86%D8%AE%D8%B3%D8%AA%DB%8C%D9%86-%DA%AF%D8%B1%D8%A7%D9%85%DB%8C%D8%AF%D8%A7%D8%B4%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%B3%D8%B1%D9%88%DB%8C%D8%B3-%D8%A8%D9%87%D8%AF%D8%A7%D8%B4%D8%AA%DB%8C-%D8%B9%D8%AF%D9%85-%D8%AF%D8%B3%D8%AA%D8%B1%D8%B3%DB%8C-%D8%AF%D9%88-%D9%85%DB%8C%D9%84%DB%8C%D8%A7%D8%B1%D8%AF-%D9%88-500-%D9%85%DB%8C%D9%84%DB%8C%D9%88%D9%86-%D9%86%D9%81%D8%B1-%D8%A7%D8%B2-%D9%85%D8%B1%D8%AF%D9%85-%D8%AC%D9%87%D8%A7%D9%86-%D8%A8%D9%87-%D8%AA%D8%A7%D8%B3%DB%8C%D8%B3%D8%A7%D8%AA-%D8%A8%D9%87%D8%AF%D8%A7%D8%B4%D8%AA%DB%8C-%D9%85%D9%86%D8%A7%D8%B3%D8%A8&catid=10:press-releases-persian&Itemid=185&lang=fa",
                        title = "United Nations Information Centre Tehran — به مناسبت نخستین گرامیداشت روز جهانی سرویس بهداشتی",
                        page = "Wayback Machine snapshot of 2016-08-01 (original site unreachable 2026-09-18)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/67/291",
                    "un" to "https://www.un.org/en/observances/toilet-day",
                ),
        ),
    )
