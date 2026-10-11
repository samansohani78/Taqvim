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
 * Part 42 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_42: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.friendship-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دوستی",
                        "ar" to "اليوم الدولي للصداقة",
                        "en" to "International Day of Friendship",
                        "es" to "Día Internacional de la Amistad",
                        "fr" to "Journée internationale de l'amitié",
                        "ru" to "Международный день дружбы",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/friendship-day",
                            title = "International Day of Friendship — A/RES/65/275 (2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/friendship-day",
                        title = "International Day of Friendship (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/friendship-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.genocide-prevention-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود و کرامت قربانیان جنایت نسل‌کشی و پیشگیری از این جنایت",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا جريمة الإبادة الجماعية وتكريمهم ومنع هذه الجريمة",
                        "en" to "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide and of the Prevention of this Crime",
                        "es" to "Día Internacional para la Conmemoración y Dignificación de las Víctimas del Crimen de Genocidio y para la Prevención de ese Crimen",
                        "fr" to "Journée internationale de commémoration des victimes du crimede génocide, d’affirmation de leur dignité et de prévention de ce crime",
                        "zh" to "缅怀灭绝种族罪受害者、受害者尊严和防止此种罪行国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/genocide-prevention-day",
                            title = "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide — A/RES/69/323 (29 September 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/genocide-prevention-day",
                        title = "International Day of Commemoration and Dignity of the Victims of the Crime of Genocide and of the Prevention of this Crime (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/genocide-prevention-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.girl-child-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دختران",
                        "ar" to "اليوم الدولي للطفلة",
                        "en" to "International Day of the Girl Child",
                        "es" to "Día Internacional de la Niña",
                        "fr" to "Journée internationale de la fille",
                        "ru" to "Международный день девочек",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/girl-child-day",
                            title = "International Day of the Girl Child — resolution 66/170 (19 December 2011)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/girl-child-day",
                        title = "International Day of the Girl Child (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/girl-child-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.girls-in-ict-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دختران در فناوری اطلاعات و ارتباطات",
                        "en" to "International Girls in ICT Day",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 4, weekday = Weekday.THURSDAY, n = 4),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.itu.int/women-and-girls/girls-in-ict/about-international-girls-in-ict-day/",
                            title = "International Girls in ICT Day (ITU Plenipotentiary Conference Resolution 70, 2010)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.itu.int/women-and-girls/girls-in-ict/",
                        title = "International Girls in ICT Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.itu.int/women-and-girls/girls-in-ict/about-international-girls-in-ict-day/",
                        title = "ITU — About International Girls in ICT Day: \"Since 8 April 2011, the ITU has celebrated Girls in ICT Day annually on the fourth Thursday in April\" — D-05 re-validation (2026-09-25) found this record was shipped as a Fixed April 23 date, which is only the fourth Thursday in 3 of 25 years (2015, 2020, 2026); ITU's own page states a floating weekday-ordinal rule, so the rule type was corrected from Fixed to NthWeekdayOfMonth.",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.itu.int/women-and-girls/girls-in-ict/",
                ),
        ),
    )
