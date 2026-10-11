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
 * Part 50 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_50: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.international-day-peace"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی صلح",
                        "ar" to "اليوم الدولي للسلام",
                        "en" to "International Day of Peace",
                        "es" to "Día Internacional de la Paz",
                        "fr" to "Journée internationale de la paix",
                        "ru" to "Международный день мира",
                        "zh" to "国际和平日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-peace",
                        title = "International Day of Peace (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/36/67",
                    "un" to "https://www.un.org/en/observances/international-day-peace",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-literacy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سوادآموزی",
                        "ar" to "اليوم الدولي لمحو الأمية",
                        "en" to "International Literacy Day",
                        "es" to "Día Internacional de la Alfabetización",
                        "fr" to "Journée internationale de l'alphabétisation",
                        "ru" to "Международный день грамотности",
                        "zh" to "国际扫盲日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/literacy",
                        title = "International Literacy Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000114048",
                    "un" to "https://www.unesco.org/en/days/literacy",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-nowruz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی نوروز",
                        "ar" to "يوم نوروز الدولي",
                        "en" to "International Day of Nowruz",
                        "es" to "Día Internacional del Novruz",
                        "fr" to "Journée internationale du Novruz",
                        "ru" to "Международный день Навруз",
                        "zh" to "国际诺鲁孜节",
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
                        url = "https://www.un.org/en/observances/international-nowruz-day",
                        title = "International Day of Nowruz (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/64/253",
                    "un" to "https://www.un.org/en/observances/international-nowruz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-tea-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی چای",
                        "en" to "International Tea Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/74/241",
                            title = "International Tea Day (A/RES/74/241, adopted 19 December 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/international-tea-day/en",
                        title = "International Tea Day (observance page)",
                    ),
                    Citation(
                        url = "https://docs.un.org/en/A/RES/74/241",
                        title = "General Assembly resolution A/RES/74/241 (19 December 2019): \"Decides to designate 21 May as International Tea Day\" — D-05 re-validation (2026-09-25) found the record's validity citation misidentified this resolution as A/RES/74/151 (an unrelated human-rights resolution of 18 December 2019); the 21 May date itself was already correct, only the cited resolution symbol was wrong.",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/international-tea-day/en",
                ),
        ),
    )
