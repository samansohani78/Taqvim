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
        EventDefinition(
            id = EventId("un.water-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آب",
                        "ar" to "اليوم العالمي للمياه",
                        "en" to "World Water Day",
                        "es" to "Día Mundial del Agua",
                        "fr" to "Journée mondiale de l'eau",
                        "ru" to "Всемирный день водных ресурсов",
                        "zh" to "世界水日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/water-day",
                        title = "World Water Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/47/193",
                    "un" to "https://www.un.org/en/observances/water-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.wellness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تندرستی",
                        "en" to "International Wellness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_026,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/wellness-day",
                            title = "International Wellness Day (UN General Assembly A/RES/80/249, adopted 10 Mar 2026)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/wellness-day",
                        title = "International Wellness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/wellness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.widows-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی بیوه ها",
                        "ar" to "اليوم الدولي للأرامل",
                        "en" to "International Widows' Day",
                        "es" to "Día Internacional de las Viudas",
                        "fr" to "Journée internationale des veuves",
                        "ru" to "Международный день вдов",
                        "zh" to "国际丧偶妇女日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/widows-day",
                        title = "International Widows' Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/65/189",
                    "un" to "https://www.un.org/en/observances/widows-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.women-and-girls-in-science-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان و دختران در علم",
                        "en" to "International Day of Women and Girls in Science",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/list-days-weeks",
                            title = "International Day of Women and Girls in Science -- GA resolution A/RES/70/212 (22 Dec 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                        title = "International Day of Women and Girls in Science (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/women-and-girls-in-science-day/",
                ),
        ),
    )
