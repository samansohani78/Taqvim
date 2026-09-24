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
 * Part 20 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_20: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.human-rights-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی حقوق بشر",
                        "ar" to "يوم حقوق الإنسان",
                        "en" to "Human Rights Day",
                        "es" to "Día de los Derechos Humanos",
                        "fr" to "Journée des droits de l'homme",
                        "ru" to "День прав человека",
                        "zh" to "人权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-rights-day",
                        title = "Human Rights Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-rights-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-solidarity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی همبستگی بشر",
                        "ar" to "اليوم الدولي للتضامن الإنساني",
                        "en" to "International Human Solidarity Day",
                        "es" to "Día Internacional de la Solidaridad Humana",
                        "fr" to "Journée internationale de la solidarité humaine",
                        "ru" to "Международный день солидарности людей",
                        "zh" to "国际人类团结日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-solidarity-day",
                        title = "International Human Solidarity Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/60/209",
                    "un" to "https://www.un.org/en/observances/human-solidarity-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-spaceflight-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پرواز فضایی انسان",
                        "en" to "International Day of Human Space Flight",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/human-spaceflight-day",
                            title = "International Day of Human Space Flight — A/RES/65/271 (7 April 2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-spaceflight-day",
                        title = "International Day of Human Space Flight (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-spaceflight-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.humanitarian-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی انساندوستی",
                        "ar" to "اليوم العالمي للعمل الإنساني",
                        "en" to "World Humanitarian Day",
                        "es" to "Día Mundial de la Asistencia Humanitaria",
                        "fr" to "Journée mondiale de l'aide humanitaire",
                        "ru" to "Всемирный день гуманитарной помощи",
                        "zh" to "世界人道主义日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 19),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/humanitarian-day",
                        title = "World Humanitarian Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/63/139",
                    "un" to "https://www.un.org/en/observances/humanitarian-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.immunization-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی ایمن‌سازی",
                        "en" to "World Immunization Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 4, day = 24), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-immunization-week",
                            title = "WHO — World Immunization Week (campaign page; WHO's own event archive for this week begins in 2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-immunization-week",
                        title = "WHO — World Immunization Week (campaign page; WHO's own event archive for this week begins in 2012)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-immunization-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.indigenous-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مردم بومی جهان",
                        "ar" to "اليوم الدولي للسكان الأصليين في العالم",
                        "en" to "International Day of the World's Indigenous Peoples",
                        "es" to "Día Internacional de los Pueblos Indígenas",
                        "fr" to "Journée internationale des peuples autochtones",
                        "ru" to "Международный день коренных народов мира",
                        "zh" to "世界土著人民国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/indigenous-day",
                        title = "International Day of the World's Indigenous Peoples (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/49/214",
                    "un" to "https://www.un.org/en/observances/indigenous-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.indigenous-women-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران بومی جهان",
                        "en" to "International Day of the World's Indigenous Women and Girls",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/indigenous-women-day",
                            title = "International Day of the World's Indigenous Women and Girls — A/RES/80/10 (26 November 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/indigenous-women-day",
                        title = "International Day of the World's Indigenous Women and Girls (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/indigenous-women-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.information-access-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دسترسی همگانی به اطلاعات",
                        "en" to "International Day for Universal Access to Information",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 28),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/information-access-day",
                            title = "International Day for Universal Access to Information — General Assembly, 28 September 2019 (following UNESCO's 2015 declaration)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/information-access-day",
                        title = "International Day for Universal Access to Information (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/information-access-day",
                ),
        ),
    )
