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
import ir.taqvim.core.model.Weekday

/**
 * Part 19 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_19: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.world-meteorological-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی هواشناسی",
                        "ar" to "اليوم العالمي للأرصاد الجوية",
                        "en" to "World Meteorological Day",
                        "es" to "Día Meteorológico Mundial",
                        "fr" to "Journée météorologique mondiale",
                        "ru" to "Всемирный метеорологический день",
                        "zh" to "世界气象日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://wmo.int/about-wmo/world-meteorological-day",
                        title = "World Meteorological Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://library.wmo.int/viewer/32802?medianame=99_en_#page=66&amp;viewer=picture&amp;o=bookmarks&amp;n=0&amp;q=",
                    "un" to "https://wmo.int/about-wmo/world-meteorological-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-no-tobacco-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خودداری از مصرف دخانیات",
                        "ar" to "اليوم العالمي للامتناع عن تعاطي التبغ",
                        "en" to "World No-Tobacco Day",
                        "es" to "Día Mundial Sin Tabaco",
                        "fr" to "Journée mondiale sans tabac",
                        "ru" to "Всемирный день без табака",
                        "zh" to "世界无烟日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 31),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-no-tobacco-day",
                        title = "World No-Tobacco Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://www.who.int/about/governance/world-health-assembly",
                    "un" to "https://www.who.int/campaigns/world-no-tobacco-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-philosophy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی فلسفه",
                        "en" to "World Philosophy Day",
                        "es" to "Día Mundial de la Filosofía",
                        "fr" to "Journée mondiale de la philosophie",
                        "ru" to "Всемирный день философии",
                        "zh" to "世界哲学日",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 11, weekday = Weekday.THURSDAY, n = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/philosophy",
                        title = "World Philosophy Day (observance page)",
                        page = "\"each year, on the third Thursday of November\"",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000142825.page=86",
                    "un" to "https://www.unesco.org/en/days/philosophy",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-poetry-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی شعر",
                        "ar" to "اليوم العالمي للشعر",
                        "en" to "World Poetry Day",
                        "es" to "Día Mundial de la Poesía",
                        "fr" to "Journée mondiale de la poésie",
                        "ru" to "Всемирный день поэзии",
                        "zh" to "世界诗歌日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/poetry",
                        title = "World Poetry Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000118514.page=70",
                    "un" to "https://www.unesco.org/en/days/poetry",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-population-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی جمعیت",
                        "ar" to "اليوم العالمي للسكان",
                        "en" to "World Population Day",
                        "es" to "Día Mundial de la Población",
                        "fr" to "Journée mondiale de la population",
                        "ru" to "Всемирный день народонаселения",
                        "zh" to "世界人口日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 11),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-population-day",
                        title = "World Population Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/45/216",
                    "un" to "https://www.un.org/en/observances/world-population-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-post-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پست",
                        "ar" to "اليوم العالمي للبريد",
                        "en" to "World Post Day",
                        "fr" to "Journée mondiale de la poste",
                        "ru" to "Всемирный день почты",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, fr, ru editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-post-day",
                        title = "World Post Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-post-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-science-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی علم در خدمت صلح و توسعه",
                        "ar" to "اليوم العالمي للعلوم من أجل السلام والتنمية",
                        "en" to "World Science Day for Peace and Development",
                        "es" to "Día Mundial de la Ciencia para la Paz y el Desarrollo",
                        "fr" to "Semaine mondiale de la science au service de la paix et du développement",
                        "ru" to "Всемирный день науки за мир и развитие",
                        "zh" to "争取和平与发展世界科学日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-science-day",
                        title = "World Science Day for Peace and Development (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000142825",
                    "un" to "https://www.un.org/en/observances/world-science-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-teachers-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آموزگاران",
                        "ar" to "اليوم العالمي للمعلمين",
                        "en" to "World Teachers’ Day",
                        "es" to "Día Mundial de los Docentes",
                        "fr" to "Journée mondiale des enseignants",
                        "ru" to "Всемирный день учителя",
                        "zh" to "世界教师日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/teachers",
                        title = "World Teachers’ Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000095720.page=5",
                    "un" to "https://www.unesco.org/en/days/teachers",
                ),
        ),
    )
