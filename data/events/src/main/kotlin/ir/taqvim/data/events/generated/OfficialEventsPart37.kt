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
 * Part 37 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_37: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.yoga-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی یوگا",
                        "en" to "International Day of Yoga",
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
                        url = "https://www.un.org/en/observances/yoga-day",
                        title = "International Day of Yoga (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/yoga-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.youth-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی جوانان",
                        "ar" to "يوم الشباب الدولي",
                        "en" to "International Youth Day",
                        "es" to "Día Internacional de la Juventud",
                        "fr" to "Journée internationale de la jeunesse",
                        "ru" to "Международный день молодежи",
                        "zh" to "国际青年日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/youth-day",
                        title = "International Youth Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/54/120",
                    "un" to "https://www.un.org/en/observances/youth-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.zero-discrimination-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز عدم تبعیض",
                        "en" to "Zero Discrimination Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.unaids.org/en/zero-discrimination-day",
                        title = "Zero Discrimination Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.unaids.org/en/zero-discrimination-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.zero-waste-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پسماند صفر",
                        "en" to "International Day of Zero Waste",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/zero-waste-day",
                        title = "International Day of Zero Waste (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/zero-waste-day",
                ),
        ),
    )
