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
 * Part 28 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_28: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.police-cooperation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همکاری پلیسی",
                        "en" to "International Day of Police Cooperation",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/police-cooperation-day",
                            title = "International Day of Police Cooperation — resolution 77/241 (2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/police-cooperation-day",
                        title = "International Day of Police Cooperation (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/police-cooperation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.press-freedom-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آزادی مطبوعاتی",
                        "ar" to "اليوم العالمي لحرية الصحافة",
                        "en" to "World Press Freedom Day",
                        "es" to "Día Mundial de la Libertad de Prensa",
                        "fr" to "Journée mondiale de la liberté de la presse",
                        "ru" to "Всемирный день свободы печати",
                        "zh" to "世界新闻自由日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/press-freedom-day",
                        title = "World Press Freedom Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000090448.page=76",
                    "un" to "https://www.un.org/en/observances/press-freedom-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.prevention-extremism-when-conducive-terrorism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پیشگیری از افراط‌گرایی خشونت‌آمیز در صورت منتهی‌شدن به تروریسم",
                        "en" to "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                            title = "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism — A/RES/77/243 (2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                        title = "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.protect-education-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حفاظت از آموزش در برابر حملات",
                        "ar" to "اليوم الدولي لحماية التعليم من الهجمات",
                        "en" to "International Day to Protect Education from Attack",
                        "es" to "Día Internacional para Proteger la Educación de Ataques",
                        "fr" to "Journée internationale pour la protection de l’éducation contre les attaques",
                        "ru" to "Международный день защиты образования от нападений",
                        "zh" to "保护教育免受攻击国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/protect-education-day",
                        title = "International Day to Protect Education from Attack (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/322427-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%D8%AD%D9%81%D8%A7%D8%B8%D8%AA-%D8%A7%D8%B2-%D8%A2%D9%85%D9%88%D8%B2%D8%B4-%D8%AF%D8%B1-%D8%A8%D8%B1%D8%A7%D8%A8%D8%B1-%D8%AD%D9%85%D9%84%D8%A7%D8%AA-%DB%B2%DB%B0%DB%B2%DB%B6",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی حفاظت از آموزش در برابر حملات ۲۰۲۶",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/74/275",
                    "un" to "https://www.un.org/en/observances/protect-education-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.public-service-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز خدمات عمومی ملل متحد",
                        "ar" to "يوم الأمم المتحدة للخدمة العامة",
                        "en" to "United Nations Public Service Day",
                        "es" to "Día de las Naciones Unidas para la Administración Pública",
                        "fr" to "Journée des Nations Unies pour la fonction publique",
                        "ru" to "День государственной службы ООН",
                        "zh" to "联合国公务员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/public-service-day",
                        title = "United Nations Public Service Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/277",
                    "un" to "https://www.un.org/en/observances/public-service-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.racism-solidarity-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته همبستگی با مردمان مبارز علیه نژادپرستی و تبعیض نژادی",
                        "en" to "Week of Solidarity with the Peoples Struggling against Racism and Racial Discrimination",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 3, day = 21), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_979,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/week-solidarity-peoples-struggling-against-racism-and-racial-discrimination",
                            title = "General Assembly resolution A/RES/34/24 (1979): Programme for the Decade for Action to Combat Racism and Racial Discrimination",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/week-solidarity-peoples-struggling-against-racism-and-racial-discrimination",
                        title = "General Assembly resolution A/RES/34/24 (1979): Programme for the Decade for Action to Combat Racism and Racial Discrimination",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/week-solidarity-peoples-struggling-against-racism-and-racial-discrimination",
                ),
        ),
        EventDefinition(
            id = EventId("un.radio-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی رادیو",
                        "ar" to "اليوم العالمي للإذاعة",
                        "en" to "World Radio Day",
                        "es" to "Día Mundial de la Radio",
                        "fr" to "Journée mondiale de la radio",
                        "ru" to "Всемирный день радио",
                        "zh" to "世界无线电日",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 13),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/radio-day",
                        title = "World Radio Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20150403105303/http://www.unic-ir.org:80/index.php?option=com_content&view=article&id=984:%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%B1%D8%A7%D8%AF%DB%8C%D9%88-%D8%AF%D8%A8%DB%8C%D8%B1-%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D8%AE%D9%88%D8%A7%D8%B3%D8%AA%D8%A7%D8%B1-%D8%B4%D9%86%DB%8C%D8%AF%D9%86-%D9%86%D8%AF%D8%A7%DB%8C-%D8%AC%D9%88%D8%A7%D9%86%D8%A7%D9%86-%D8%B4%D8%AF&catid=8:%D8%B1%D9%88%DB%8C%D8%AF%D8%A7%D8%AF-%D9%87%D8%A7&Itemid=231&lang=fa",
                        title = "United Nations Information Centre Tehran — به مناسبت روز جهانی رادیو دبیر کل سازمان ملل خواستار شنیدن ندای جوانان شد",
                        page = "Wayback Machine snapshot of 2015-04-03 (original site unreachable 2026-09-18)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/67/124",
                    "un" to "https://www.un.org/en/observances/radio-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.refugee-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پناهندگان",
                        "ar" to "اليوم العالمي للاجئين",
                        "en" to "World Refugee Day",
                        "es" to "Día Mundial de los Refugiados",
                        "fr" to "Journée mondiale des réfugiés",
                        "ru" to "Всемирный день беженцев",
                        "zh" to "世界难民日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/refugee-day",
                        title = "World Refugee Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/55/76",
                    "un" to "https://www.un.org/en/observances/refugee-day",
                ),
        ),
    )
