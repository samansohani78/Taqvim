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
 * Part 22 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_22: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.african-descent-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی افراد تبار آفریقایی",
                        "ar" to "اليوم الدولي للمنحدرين من أصل أفريقي",
                        "en" to "International Day for People of African Descent",
                        "es" to "Día Internacional de los Afrodescendientes",
                        "fr" to "Journée internationale des personnes d’ascendance africaine",
                        "ru" to "Международный день лиц африканского происхождения",
                        "zh" to "非洲人后裔国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 31),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_020,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://research.un.org/en/docs/ga/quick/regular/75",
                            title = "UN Dag Hammarskjold Library: A/RES/75/170, adopted 16 December 2020 (corroborated by un.org/en/observances/african-descent-day)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/african-descent-day",
                        title = "International Day for People of African Descent (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/african-descent-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.albinism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از آلبینیسم",
                        "ar" to "اليوم الدولي للتوعية بالمهق",
                        "en" to "International Albinism Awareness Day",
                        "es" to "Día Internacional de Sensibilización sobre el Albinismo",
                        "fr" to "Journée internationale de sensibilisation à l'albinisme",
                        "ru" to "Международный день распространения информации об альбинизме",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 13),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://research.un.org/en/docs/ga/quick/regular/69",
                            title = "International Albinism Awareness Day (A/RES/69/170, adopted 18 December 2014)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/albinism-day",
                        title = "International Albinism Awareness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/albinism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.amr-awareness-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی آگاهی از مقاومت ضدمیکروبی",
                        "en" to "World Antimicrobial Resistance Awareness Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 11, day = 18), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-amr-awareness-week",
                            title = "WHO — World AMR Awareness Week (campaign page: dates standardized to 18-24 November by 2018, after 2015's 16-22 November launch as World Antibiotic Awareness Week)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-amr-awareness-week",
                        title = "WHO — World AMR Awareness Week (campaign page: dates standardized to 18-24 November by 2018, after 2015's 16-22 November launch as World Antibiotic Awareness Week)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-amr-awareness-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.anti-colonialism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با استعمار در همه اشکال و مظاهر آن",
                        "ar" to "اليوم الدولي لمناهضة الاستعمار بجميع أشكاله ومظاهره",
                        "en" to "International Day against Colonialism in All its Forms and Manifestations",
                        "es" to "Día Internacional contra el Colonialismo en Todas sus Formas y Manifestaciones",
                        "ru" to "Международный день борьбы с колониализмом во всех его формах и проявлениях",
                        "zh" to "反对一切形式和表现的殖民主义国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 14),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/anti-colonialism-day",
                            title = "International Day against Colonialism in All its Forms and Manifestations — A/RES/80/106 (2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/anti-colonialism-day",
                        title = "International Day against Colonialism in All its Forms and Manifestations (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/anti-colonialism-day",
                ),
        ),
    )
