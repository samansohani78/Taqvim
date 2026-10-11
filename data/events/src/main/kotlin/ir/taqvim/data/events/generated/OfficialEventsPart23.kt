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
 * Part 23 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_23: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.anti-corruption-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با فساد",
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
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/79/243",
                            title = "International Anti-Cybercrime Day — General Assembly resolution A/RES/79/243, \"Resolution adopted by the General Assembly on 24 December 2024\", para. 12: \"Decides that, in order to raise awareness of cybercrime and of the role of the Convention in combating and preventing it, 24 December should be designated International Anti-Cybercrime Day.\"",
                        ),
                ),
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
                        "ar" to "اليوم الدولي لمكافحة كراهية الإسلام",
                        "en" to "International Day to Combat Islamophobia",
                        "es" to "Día Internacional para Combatir la Islamofobia",
                        "fr" to "Journée internationale de lutte contre l’islamophobie",
                        "ru" to "Международный день борьбы с исламофобией",
                        "zh" to "打击伊斯兰恐惧症国际日",
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
                        "ar" to "اليوم الدولي للنمر العربي",
                        "en" to "International Day of the Arabian Leopard",
                        "es" to "Día Internacional del Leopardo Árabe",
                        "fr" to "Journée internationale du léopard d’Arabie",
                        "ru" to "Международный день арабского леопарда",
                        "zh" to "国际阿拉伯豹日",
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
    )
