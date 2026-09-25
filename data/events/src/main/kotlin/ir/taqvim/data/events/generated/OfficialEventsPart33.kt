/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

import ir.taqvim.core.events.AstroKind
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
 * Part 33 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_33: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.universal-health-coverage-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پوشش همگانی سلامت",
                        "en" to "International Universal Health Coverage Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/universal-health-coverage-day",
                            title = "International Universal Health Coverage Day — resolution 72/138 (2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/universal-health-coverage-day",
                        title = "International Universal Health Coverage Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/universal-health-coverage-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.vesak-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "ویساک، روز ماه کامل",
                        "en" to "Vesak, the Day of the Full Moon",
                    ),
                ),
            rule = EventRule.Astronomical(kind = AstroKind.FULL_MOON, offsetDays = 0, timeZone = "UTC", month = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_999,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://undocs.org/en/A/RES/54/115",
                            title = "General Assembly resolution 54/115: International recognition of the Day of Vesak at United Nations Headquarters and other United Nations offices (1999)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/vesak-day",
                        title = "Vesak Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/54/115",
                    "un" to "https://www.un.org/en/observances/vesak-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.victims-enforced-disappearance"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قربانیان ناپدیدشدگان اجباری",
                        "en" to "International Day of the Victims of Enforced Disappearances",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/victims-enforced-disappearance",
                            title = "International Day of the Victims of Enforced Disappearances (A/RES/65/209, adopted 21 December 2010; adoption-year convention, consistent with un.vesak-day/ADR-0044)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/victims-enforced-disappearance",
                        title = "International Day of the Victims of Enforced Disappearances (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/victims-enforced-disappearance",
                ),
        ),
        EventDefinition(
            id = EventId("un.volunteer-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی داوطلبان برای توسعه اقتصادی و اجتماعی",
                        "ar" to "اليوم الدولي للمتطوعين من أجل التنمية الاقتصادية والاجتماعية",
                        "en" to "International Volunteer Day for Economic and Social Development",
                        "es" to "Día Internacional de los Voluntarios",
                        "fr" to "Journée internationale des volontaires",
                        "ru" to "Международный день добровольцев во имя экономического и социального развития",
                        "zh" to "国际志愿人员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/volunteer-day",
                        title = "International Volunteer Day for Economic and Social Development (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/40/212",
                    "un" to "https://www.un.org/en/observances/volunteer-day",
                ),
        ),
    )
