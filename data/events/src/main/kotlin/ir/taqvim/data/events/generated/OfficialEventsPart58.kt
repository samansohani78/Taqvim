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
 * Part 58 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_58: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.ozone-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حفظ لایه اوزون",
                        "ar" to "اليوم الدولي لحفظ طبقة الأوزون",
                        "en" to "International Day for the Preservation of the Ozone Layer",
                        "es" to "Día Internacional de la Preservación de la Capa de Ozono",
                        "fr" to "Journée internationale de la protection de la couche d'ozone",
                        "ru" to "Международный день охраны озонового слоя",
                        "zh" to "保护臭氧层国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/ozone-day",
                        title = "International Day for the Preservation of the Ozone Layer (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/49/114",
                    "un" to "https://www.un.org/en/observances/ozone-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.parents-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی والدین",
                        "ar" to "اليوم العالمي للوالدين",
                        "en" to "Global Day of Parents",
                        "es" to "Día Mundial de las Madres y los Padres",
                        "fr" to "Journée mondiale des parents",
                        "ru" to "Всемирный день родителей",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/parents-day",
                            title = "Global Day of Parents — A/RES/66/292 (2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/parents-day",
                        title = "Global Day of Parents (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/parents-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.parliamentarism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پارلمانتاریسم",
                        "ar" to "اليوم الدولي للعمل البرلماني",
                        "en" to "International Day of Parliamentarism",
                        "fr" to "Journée internationale du parlementarisme",
                        "ru" to "Международный день парламентаризма",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/parliamentarism-day",
                            title = "International Day of Parliamentarism — A/RES/72/278 (2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/parliamentarism-day",
                        title = "International Day of Parliamentarism (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/parliamentarism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.peaceful-coexistence-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همزیستی مسالمت‌آمیز",
                        "ar" to "اليوم الدولي للتعايش السلمي",
                        "en" to "International Day of Peaceful Coexistence",
                        "es" to "Día Internacional de la Coexistencia Pacífica",
                        "fr" to "Journée internationale de la coexistence pacifique",
                        "ru" to "Международный день мирного сосуществования",
                        "zh" to "和平共处国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 28),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/peaceful-coexistence-day",
                            title = "International Day of Peaceful Coexistence — A/RES/79/269 (4 March 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/peaceful-coexistence-day",
                        title = "International Day of Peaceful Coexistence (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/peaceful-coexistence-day",
                ),
        ),
    )
