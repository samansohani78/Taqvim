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
 * Part 26 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_26: List<EventDefinition> =
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
                        "en" to "Global Day of Parents",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 1),
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
                        "en" to "International Day of Parliamentarism",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 30),
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
                        "en" to "International Day of Peaceful Coexistence",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 28),
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
        EventDefinition(
            id = EventId("un.peacekeepers-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حافظان صلح ملل متحد",
                        "ar" to "اليوم الدولي لحفظة السلام",
                        "en" to "International Day of UN Peacekeepers",
                        "es" to "Día Internacional del Personal de Paz de las Naciones Unidas",
                        "fr" to "Journée internationale des Casques bleus des Nations Unies",
                        "ru" to "Международный день миротворцев ООН",
                        "zh" to "联合国维持和平人员国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/peacekeepers-day",
                        title = "International Day of UN Peacekeepers (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/129",
                    "un" to "https://www.un.org/en/observances/peacekeepers-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.police-cooperation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همکاری پلیسی",
                        "en" to "International Day of Police Cooperation",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/police-cooperation-day",
                        title = "International Day of Police Cooperation (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/police-cooperation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.press-freedom-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آزادی مطبوعاتی",
                        "ar" to "اليوم العالمي لحرية الصحافة",
                        "en" to "World Press Freedom Day",
                        "es" to "Día Mundial de la Libertad de Prensa",
                        "fr" to "Journée mondiale de la liberté de la presse",
                        "ru" to "Всемирный день свободы печати",
                        "zh" to "世界新闻自由日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/press-freedom-day",
                        title = "World Press Freedom Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000090448.page=76",
                    "un" to "https://www.un.org/en/observances/press-freedom-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.prevention-extremism-when-conducive-terrorism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پیشگیری از افراط‌گرایی خشونت‌آمیز در صورت منتهی‌شدن به تروریسم",
                        "en" to "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                        title = "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                ),
        ),
    )
