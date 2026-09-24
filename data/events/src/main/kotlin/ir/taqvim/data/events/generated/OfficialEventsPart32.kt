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
 * Part 32 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_32: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.torture-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حمایت از قربانیان شکنجه",
                        "ar" to "اليوم الدولي للأمم المتحدة لمساندة ضحايا التعذيب",
                        "en" to "United Nations International Day in Support of Victims of Torture",
                        "es" to "Día Internacional en Apoyo de las Víctimas de la Tortura",
                        "fr" to "Journée internationale pour le soutien aux victimes de la torture",
                        "ru" to "Международный день в поддержку жертв пыток",
                        "zh" to "支援酷刑受害者国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/torture-victims-day",
                        title = "United Nations International Day in Support of Victims of Torture (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/52/149",
                    "un" to "https://www.un.org/en/observances/torture-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.tourism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی گردشگری",
                        "ar" to "يوم السياحة العالمي",
                        "en" to "World Tourism Day",
                        "es" to "Día Mundial del Turismo",
                        "fr" to "Journée mondiale du tourisme",
                        "ru" to "Всемирный день туризма",
                        "zh" to "世界旅游日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tourism-day",
                        title = "World Tourism Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tourism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.tourism-resilience-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی تاب‌آوری گردشگری",
                        "en" to "Global Tourism Resilience Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 17),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/tourism-resilience-day",
                            title = "Global Tourism Resilience Day — A/RES/77/269 (2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tourism-resilience-day",
                        title = "Global Tourism Resilience Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tourism-resilience-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.transatlantic-slave-trade"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی یاد بود قربانیان بردگی و تجارت برده از آن سوی اقیانوس اطلس",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا الرق وتجارة الرقيق عبر المحيط الأطلسي",
                        "en" to "International Day of Remembrance of the Victims of Slavery and the Transatlantic Slave Trade",
                        "es" to "Día Internacional de Recuerdo de las Víctimas de la Esclavitud y la Trata Transatlántica de Esclavos",
                        "fr" to "Journée internationale de commémoration des victimes de l’esclavage et de la traite transatlantique des esclaves",
                        "ru" to "Международный день памяти жертв рабства и трансатлантической работорговли",
                        "zh" to "奴隶制和跨大西洋贩卖奴隶行为受害者国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/transatlantic-slave-trade",
                        title = "International Day of Remembrance of the Victims of Slavery and the Transatlantic Slave Trade (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/122",
                    "un" to "https://www.un.org/en/observances/transatlantic-slave-trade",
                ),
        ),
        EventDefinition(
            id = EventId("un.tropics-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مناطق حاره‌ای",
                        "en" to "International Day of the Tropics",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 29),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/tropics-day",
                            title = "International Day of the Tropics — A/RES/70/267 (2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tropics-day",
                        title = "International Day of the Tropics (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tropics-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.tsunami-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آگاهی از سونامی",
                        "en" to "World Tsunami Awareness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/tsunami-awareness-day",
                            title = "World Tsunami Awareness Day — resolution 70/203 (December 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tsunami-awareness-day",
                        title = "World Tsunami Awareness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tsunami-awareness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.tuna-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ماهی تن",
                        "en" to "World Tuna Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/tuna-day",
                            title = "World Tuna Day (UN General Assembly A/RES/71/124, adopted 7 Dec 2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tuna-day",
                        title = "World Tuna Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tuna-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.un-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز ملل متحد",
                        "ar" to "يوم الأمم المتحدة",
                        "en" to "United Nations Day",
                        "es" to "Día de las Naciones Unidas",
                        "fr" to "Journée des Nations Unies",
                        "ru" to "День Организации Объединенных Наций",
                        "zh" to "联合国日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/un-day",
                        title = "United Nations Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/un-day",
                ),
        ),
    )
