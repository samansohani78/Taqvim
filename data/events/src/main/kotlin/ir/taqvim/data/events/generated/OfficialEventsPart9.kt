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

/**
 * Part 9 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_9: List<EventDefinition> =
    listOf(
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
                        "en" to "International Albinism Awareness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 13),
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
            id = EventId("un.anti-colonialism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با استعمار در همه اشکال و مظاهر آن",
                        "en" to "International Day against Colonialism in All its Forms and Manifestations",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 14),
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
        EventDefinition(
            id = EventId("un.anti-corruption-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مبارزه با فساد",
                        "ar" to "اليوم الدولي لمكافحة الفساد",
                        "en" to "International Anti-Corruption Day",
                        "es" to "Día Internacional contra la Corrupción",
                        "fr" to "Journée internationale contre la corruption",
                        "ru" to "Международный день борьбы с коррупцией",
                        "zh" to "国际反腐败日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/anti-corruption-day",
                        title = "International Anti-Corruption Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/4",
                    "un" to "https://www.un.org/en/observances/anti-corruption-day",
                ),
        ),
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
    )
