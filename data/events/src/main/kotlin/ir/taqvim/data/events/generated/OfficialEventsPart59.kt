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
 * Part 59 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_59: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.peacekeepers-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حافظان صلح ملل متحد",
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
                        "ar" to "اليوم الدولي للتعاون بين أجهزة الشرطة",
                        "en" to "International Day of Police Cooperation",
                        "es" to "Día Internacional de la Cooperación Policial",
                        "fr" to "Journée internationale de la coopération policière",
                        "ru" to "Международный день полицейского сотрудничества",
                        "zh" to "警务合作国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/police-cooperation-day",
                            title = "International Day of Police Cooperation — resolution 77/241 (2022)",
                        ),
                ),
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
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000090448#page=76",
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
                        "ar" to "اليوم الدولي لمنع التطرف العنيف عندما يفضي إلى الإرهاب",
                        "en" to "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism",
                        "es" to "Día Internacional para la Prevención del Extremismo Violento cuando Conduzca al Terrorismo",
                        "fr" to "Journée internationale pour la prévention de l’extrémisme violent pouvant conduire au terrorisme",
                        "ru" to "Международный день предупреждения насильственного экстремизма, создающего питательную среду для терроризма",
                        "zh" to "防止助长恐怖主义的暴力极端主义国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/prevention-extremism-when-conducive-terrorism-day",
                            title = "International Day for the Prevention of Violent Extremism as and when Conducive to Terrorism — A/RES/77/243 (2022)",
                        ),
                ),
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
