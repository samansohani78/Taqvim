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
 * Part 21 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_21: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.interfaith-harmony-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی هماهنگی بین ادیان",
                        "en" to "World Interfaith Harmony Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 2, day = 1), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://undocs.org/en/A/RES/65/5",
                            title = "General Assembly resolution A/RES/65/5 (2010): World Interfaith Harmony Week",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/interfaith-harmony-week",
                        title = "General Assembly resolution A/RES/65/5 (2010): World Interfaith Harmony Week",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/65/5",
                    "un" to "https://www.un.org/en/observances/interfaith-harmony-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-coffee-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قهوه",
                        "en" to "International Coffee Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_026,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/80/248",
                            title = "International Coffee Day (A/RES/80/248, adopted 10 March 2026)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://docs.un.org/en/A/RES/80/248",
                        title = "International Coffee Day (UN General Assembly resolution A/RES/80/248)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://docs.un.org/en/A/RES/80/248",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-for-dialogue-among-civilizations"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی گفت‌وگوی تمدن‌ها",
                        "ar" to "اليوم الدولي للحوار بين الحضارات",
                        "en" to "International Day for Dialogue among Civilizations",
                        "es" to "Día Internacional para el Diálogo entre Civilizaciones",
                        "fr" to "Journée internationale pour le dialogue entre les civilisations",
                        "ru" to "Международный день диалога между цивилизациями",
                        "zh" to "文明对话国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-for-dialogue-among-civilizations",
                        title = "International Day for Dialogue among Civilizations (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/296020-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%DA%AF%D9%81%D8%AA%E2%80%8C%D9%88%DA%AF%D9%88%DB%8C-%D8%AA%D9%85%D8%AF%D9%86%E2%80%8C%D9%87%D8%A7",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی گفت‌وگوی تمدن‌ها",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://www.undocs.org/EN/A/RES/78/286",
                    "un" to "https://www.un.org/en/observances/international-day-for-dialogue-among-civilizations",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-for-the-remembrance-of-the-slave-trade"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی یادبود و الغاء تجارت برده",
                        "ar" to "اليوم الدولي لاحياء ذكرى تجارة الرقيق وذكرى إلغائها",
                        "en" to "International Day for the Remembrance of the Slave Trade and Its Abolition",
                        "es" to "Día Internacional del Recuerdo de la Trata de Esclavos y de su Abolición",
                        "fr" to "Journée internationale du souvenir de la traite négrière et de son abolition",
                        "ru" to "Международный день памяти о работорговле и ее ликвидации",
                        "zh" to "贩卖奴隶及其废除奴隶制国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/slave-trade-remembrance",
                        title = "International Day for the Remembrance of the Slave Trade and Its Abolition (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000110220.page=72",
                    "un" to "https://www.unesco.org/en/days/slave-trade-remembrance",
                ),
        ),
    )
