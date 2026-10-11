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
 * Part 82 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_82: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم العالمي لمهارات الشباب",
                        "en" to "World Youth Skills Day",
                        "es" to "Día Mundial de las Habilidades de la Juventud",
                        "fr" to "Journée mondiale des compétences des jeunes",
                        "ru" to "Всемирный день навыков молодежи",
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
                        "ar" to "اليوم الدولي لليوغا",
                        "en" to "International Day of Yoga",
                        "es" to "Día Internacional del Yoga",
                        "fr" to "Journée internationale du Yoga",
                        "ru" to "Международный день йоги",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/yoga-day",
                            title = "International Day of Yoga — resolution 69/131 (11 December 2014)",
                        ),
                ),
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
                        "fa" to "روز بین‌المللی جوانان",
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
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.unaids.org/en/resources/presscentre/pressreleaseandstatementarchive/2014/february/20140227zerodiscrimination",
                            title = "Zero Discrimination Day launched by UNAIDS, first observed 1 March 2014",
                        ),
                ),
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
    )
