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
 * Part 38 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_38: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.world-television-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی تلویزیون",
                        "ar" to "اليوم العالمي للتلفزيون",
                        "en" to "World Television Day",
                        "es" to "Día Mundial de la Televisión",
                        "fr" to "Journée mondiale de la télévision",
                        "ru" to "Всемирный день телевидения",
                        "zh" to "世界电视日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-television-day",
                        title = "World Television Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/51/205",
                    "un" to "https://www.un.org/en/observances/world-television-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-tuberculosis-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بیماری سل",
                        "ar" to "اليوم العالمي للسل",
                        "en" to "World Tuberculosis Day",
                        "es" to "Día Mundial de la Tuberculosis",
                        "fr" to "Journée mondiale de la lutte contre la tuberculose",
                        "ru" to "Всемирный день борьбы против туберкулеза",
                        "zh" to "世界防治结核病日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-tb-day/",
                        title = "World Tuberculosis Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-tb-day/",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-turkic-language-family-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خانواده زبان‌های ترکی",
                        "en" to "World Turkic Language Family Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://unesdoc.unesco.org/ark:/48223/pf0000396088",
                            title = "World Turkic Language Family Day (UNESCO General Conference, 43rd session, resolution 43 C/57, Samarkand, 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://unesdoc.unesco.org/ark:/48223/pf0000396088",
                        title = "World Turkic Language Family Day (UNESCO General Conference, 43rd session, resolution 43 C/57, Samarkand, 2025)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000396088",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-wetlands-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی تالاب‌ها",
                        "en" to "World Wetlands Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-wetlands-day/background",
                            title = "World Wetlands Day — General Assembly proclamation, 30 August 2021",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-wetlands-day",
                        title = "World Wetlands Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-wetlands-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-wildlife-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی حیات وحش",
                        "en" to "World Wildlife Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 3),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_013,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-wildlife-day/background",
                            title = "World Wildlife Day — General Assembly decision, 20 December 2013",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-wildlife-day",
                        title = "World Wildlife Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-wildlife-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-youth-skills-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مهارت‌های جوانان",
                        "en" to "World Youth Skills Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-youth-skills-day",
                            title = "World Youth Skills Day — General Assembly resolution, 18 December 2014",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-youth-skills-day",
                        title = "World Youth Skills Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-youth-skills-day",
                ),
        ),
    )
