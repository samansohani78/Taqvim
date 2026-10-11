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
 * Part 45 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_45: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.humanitarian-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی انسان‌دوستی",
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
                        "es" to "Día Internacional de las Mujeres y las Niñas Indígenas del Mundo",
                        "fr" to "Journée internationale des femmes et des filles autochtones",
                        "zh" to "世界土著妇女和女童国际日",
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
    )
