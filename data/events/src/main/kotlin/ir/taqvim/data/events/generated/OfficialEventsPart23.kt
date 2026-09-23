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
 * Part 23 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_23: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.jazz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی جاز",
                        "en" to "International Jazz Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/jazz-day",
                        title = "International Jazz Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/jazz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.judicial-well-being-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفاه قضایی",
                        "en" to "International Day for Judicial Well-being",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/judicial-well-being-day",
                        title = "International Day for Judicial Well-being (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/judicial-well-being-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.kiswahili-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی زبان سواحیلی",
                        "en" to "World Kiswahili Language Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/kiswahili-day",
                        title = "World Kiswahili Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/kiswahili-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.landlocked-developing-countries-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از نیازها و چالش‌های ویژه توسعه‌ای کشورهای درحال‌توسعه محصور در خشکی",
                        "en" to "International Day of Awareness of the Special Development Needs and Challenges of Landlocked Developing Countries",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/landlocked-developing-countries-day",
                        title = "International Day of Awareness of the Special Development Needs and Challenges of Landlocked Developing Countries (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/landlocked-developing-countries-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.living-in-peace-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زندگی مسالمت‌آمیز در کنار هم",
                        "en" to "International Day of Living Together in Peace",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/living-in-peace-day",
                        title = "International Day of Living Together in Peace (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/living-in-peace-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.maritime-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دریانوردی",
                        "ar" to "يوم الملاحة البحرية العالمي",
                        "en" to "World Maritime Day",
                        "es" to "Día Marítimo Mundial",
                        "fr" to "Journée mondiale de la mer",
                        "zh" to "世界海事日",
                    ),
                ),
            rule = EventRule.LastWeekdayOfMonth(month = 9, weekday = Weekday.THURSDAY),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/maritime-day",
                        title = "World Maritime Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-18); copy in docs/sources/unic-tehran-f-event-20121005.html; row \"روز جهانی دریانوردی — 29 سپتامبر (هفته آخر سپتامبر)\"",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/maritime-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.markhor-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مارخور",
                        "en" to "International Day of the Markhor",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/markhor-day",
                        title = "International Day of the Markhor (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/markhor-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.meditation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مراقبه",
                        "en" to "World Meditation Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/meditation-day",
                        title = "World Meditation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/meditation-day",
                ),
        ),
    )
