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
 * Part 52 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_52: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم العالمي للغة السواحلية",
                        "en" to "World Kiswahili Language Day",
                        "es" to "Día Mundial de la Lengua Kiswahili",
                        "ru" to "Всемирный день языка суахили",
                        "zh" to "世界斯瓦希里语日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.unesco.org/en/days/kiswahili-language",
                            title = "World Kiswahili Language Day — UNESCO 41st General Conference, resolution 41 C/61 (November 2021)",
                        ),
                ),
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
                        "es" to "Día Internacional de Concienciación sobre las Necesidades y los Desafíos Especiales de Desarrollo de los Países en Desarrollo sin Litoral",
                        "fr" to "Journée internationale de sensibilisation aux besoins et enjeux de développement propres aux pays en développement sans littoral",
                        "ru" to "Международный день распространения информации об особых потребностях развивающихся стран, не имеющих выхода к морю, и стоящих перед ними особых проблемах в области развития",
                        "zh" to "内陆发展中国家特殊发展需要和挑战国际宣传日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 6),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/landlocked-developing-countries-day",
                            title = "Landlocked Developing Countries Day — A/RES/79/320 (2024)",
                        ),
                ),
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
                        "ar" to "اليوم الدولي للعيش معاً في سلام",
                        "en" to "International Day of Living Together in Peace",
                        "es" to "Día Internacional de la Convivencia en Paz",
                        "fr" to "Journée internationale du vivre-ensemble en paix",
                        "ru" to "Международный день жизни вместе в мире друг с другом",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/living-in-peace-day",
                            title = "International Day of Living Together in Peace (UN General Assembly A/RES/72/130, adopted 8 Dec 2017)",
                        ),
                ),
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
                    Citation(
                        url = "https://www.imo.org/en/about/events/worldmaritimeday",
                        title = "IMO's own World Maritime Day page (DT-024): \"culminating in the annual celebration on the final Thursday of September\" — the same rule as this record's, in the International Maritime Organization's own words (imo.org returned HTTP 500 on 2026-09-17; reachable again on 2026-09-25)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/maritime-day",
                ),
        ),
    )
