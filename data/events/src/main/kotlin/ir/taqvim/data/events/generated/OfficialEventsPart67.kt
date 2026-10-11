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
 * Part 67 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_67: List<EventDefinition> =
    listOf(
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
                        "es" to "Día Mundial del Transporte Sostenible",
                        "fr" to "Journée mondiale des transports durables",
                        "ru" to "Всемирный день устойчивого транспорта",
                        "zh" to "世界可持续交通运输日",
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
                        "fa" to "روز جهانی ارتباطات و جامعهٔ اطلاعاتی",
                        "ar" to "اليوم العالمي للاتصالات ومجتمع المعلومات",
                        "en" to "World Telecommunication and Information Society Day",
                        "es" to "Día Mundial de las Telecomunicaciones y la Sociedad de la Información",
                        "fr" to "Journée mondiale des télécommunications et de la société de l'information",
                        "ru" to "Всемирный день электросвязи и информационного общества",
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
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "University of Tehran, Institute of Geophysics, Calendar Center — official calendar of Iran, occasions approved by the Public Culture Council",
                        page = "Calendar-1382.pdf, Calendar-1383.pdf, Calendar-1384.pdf and Calendar-1385.pdf (docs/sources/iran/) print «روز جهانی ارتباطات» on the day whose Gregorian column reads 17 May (27 Ordibehesht 1382, 28 Ordibehesht 1383, 27 Ordibehesht 1384, 27 Ordibehesht 1385), which is the Persian title shipped here. From Calendar-1386.pdf on, after the day was renamed in 2006, these calendars print Iran's national «روز ارتباطات و روابط عمومی» on that date instead and no longer name the UN day",
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
                        "fa" to "روز بین‌المللی یادبود و احترام به قربانیان تروریسم",
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
    )
