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
 * Part 10 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_10: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.anti-cybercrime-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با جرایم سایبری",
                        "en" to "International Anti-Cybercrime Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.unodc.org/unodc/en/cybercrime/day.html",
                        title = "International Anti-Cybercrime Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.unodc.org/unodc/en/cybercrime/day.html",
                ),
        ),
        EventDefinition(
            id = EventId("un.anti-islamophobia-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با اسلام‌هراسی",
                        "en" to "International Day to Combat Islamophobia",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/anti-islamophobia-day",
                            title = "International Day to Combat Islamophobia — A/RES/76/254 (2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/anti-islamophobia-day",
                        title = "International Day to Combat Islamophobia (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/anti-islamophobia-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.arabian-leopard-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پلنگ عربی",
                        "en" to "International Day of the Arabian Leopard",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/arabian-leopard-day",
                            title = "International Day of the Arabian Leopard — resolution 77/295 (June 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/arabian-leopard-day",
                        title = "International Day of the Arabian Leopard (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/arabian-leopard-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.arabic-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان عربی",
                        "en" to "Arabic Language Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/russian-language-day",
                            title = "UN Language Days established 2010 by the Department of Global Communications for each of the six official languages",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/arabiclanguageday",
                        title = "Arabic Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/arabiclanguageday",
                ),
        ),
        EventDefinition(
            id = EventId("un.argania-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی درخت آرگان",
                        "en" to "International Day of Argania",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/argania-day",
                            title = "International Day of Argania — A/RES/75/262 (2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/argania-day",
                        title = "International Day of Argania (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/argania-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.asteroid-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سیارک",
                        "en" to "International Asteroid Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/asteroid-day",
                            title = "International Asteroid Day — A/RES/71/90 (December 2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/asteroid-day",
                        title = "International Asteroid Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/asteroid-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.audiovisual-heritage"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی میراث سمعی و بصری",
                        "en" to "World Day for Audiovisual Heritage",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_980,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.unesco.org/en/days/audiovisual-heritage",
                            title = "World Day for Audiovisual Heritage: UNESCO's 21st General Conference adopted the Recommendation for the Safeguarding and Preservation of Moving Images (1980)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/audiovisual-heritage",
                        title = "World Day for Audiovisual Heritage (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.unesco.org/en/days/audiovisual-heritage",
                ),
        ),
        EventDefinition(
            id = EventId("un.autism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آگاهی درباره اوتیسم",
                        "ar" to "اليوم العالمي للتوعية بمرض التوحد",
                        "en" to "World Autism Awareness Day",
                        "es" to "Día Mundial de Concienciación sobre el Autismo",
                        "fr" to "Journée mondiale de sensibilisation à l'autisme",
                        "ru" to "Всемирный день распространения информации о проблеме аутизма",
                        "zh" to "世界提高自闭症意识日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/autism-day",
                        title = "World Autism Awareness Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/139",
                    "un" to "https://www.un.org/en/observances/autism-day",
                ),
        ),
    )
