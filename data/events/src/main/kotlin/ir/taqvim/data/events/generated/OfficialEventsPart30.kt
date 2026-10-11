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
import ir.taqvim.core.model.Weekday

/**
 * Part 30 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_30: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.cooperatives-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تعاونی‌ها",
                        "ar" to "اليوم الدولي للتعاونيات",
                        "en" to "International Day of Cooperatives",
                        "es" to "Día Internacional de las Cooperativas",
                        "fr" to "Journée internationale des coopératives",
                        "ru" to "Международный день кооперативов",
                        "zh" to "国际合作社日",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 7, weekday = Weekday.SATURDAY, n = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cooperatives-day",
                        title = "International Day of Cooperatives (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html; rule: اولين شنبه ژوئيه",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/90",
                    "un" to "https://www.un.org/en/observances/cooperatives-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cotton-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پنبه",
                        "en" to "World Cotton Day",
                        "fr" to "Journée mondiale du coton",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/cotton-day",
                            title = "World Cotton Day — General Assembly proclamation, August 2021",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cotton-day",
                        title = "World Cotton Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/cotton-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.countering-hate-speech"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مقابله با نفرت‌پراکنی",
                        "ar" to "اليوم الدولي لمكافحة خطاب الكراهية",
                        "en" to "International Day for Countering Hate Speech",
                        "es" to "Día Internacional para Contrarrestar el Discurso de Odio",
                        "fr" to "Journée internationale de la lutte contre les discours de haine",
                        "ru" to "Международный день борьбы с языком ненависти",
                        "zh" to "打击仇恨言论国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/countering-hate-speech",
                            title = "International Day for Countering Hate Speech — A/RES/75/309 (July 2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/countering-hate-speech",
                        title = "International Day for Countering Hate Speech (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/countering-hate-speech",
                ),
        ),
        EventDefinition(
            id = EventId("un.creativity-and-innovation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خلاقیت و نوآوری",
                        "en" to "World Creativity and Innovation Day",
                        "es" to "Día Mundial de la Creatividad y la Innovación",
                        "fr" to "Journée mondiale de la créativité et de l’innovation",
                        "ru" to "Всемирный день творчества и инновационной деятельности",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/creativity-and-innovation-day",
                            title = "World Creativity and Innovation Day (UN General Assembly A/RES/71/284, adopted 27 Apr 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/creativity-and-innovation-day",
                        title = "World Creativity and Innovation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/creativity-and-innovation-day",
                ),
        ),
    )
