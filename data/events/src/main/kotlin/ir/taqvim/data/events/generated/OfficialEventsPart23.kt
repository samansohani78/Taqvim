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
            id = EventId("un.international-literacy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی سواد آموزی",
                        "ar" to "اليوم الدولي لمحو الأمية",
                        "en" to "International Literacy Day",
                        "es" to "Día Internacional de la Alfabetización",
                        "fr" to "Journée internationale de l'alphabétisation",
                        "ru" to "Международный день грамотности",
                        "zh" to "国际扫盲日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/literacy",
                        title = "International Literacy Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000114048.xml=http://www.unesco.org/ulis/cgi-bin/ulis.pl",
                    "un" to "https://www.unesco.org/en/days/literacy",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-nowruz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی نوروز",
                        "ar" to "يوم نوروز الدولي",
                        "en" to "International Day of Nowruz",
                        "es" to "Día Internacional del Novruz",
                        "fr" to "Journée internationale du Novruz",
                        "ru" to "Международный день Навруз",
                        "zh" to "国际诺鲁孜节",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-nowruz-day",
                        title = "International Day of Nowruz (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/64/253",
                    "un" to "https://www.un.org/en/observances/international-nowruz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-tea-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی چای",
                        "en" to "International Tea Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.fao.org/new-york/news/news-detail/UN-adopts-resolution-showcasing-tea/en",
                            title = "International Tea Day (A/RES/74/151, adopted December 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/international-tea-day/en",
                        title = "International Tea Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/international-tea-day/en",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-translation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ترجمه",
                        "en" to "International Translation Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/international-translation-day",
                            title = "International Translation Day — resolution 71/288 (24 May 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-translation-day",
                        title = "International Translation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/international-translation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.interventional-cardiology-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی قلب و عروق مداخله‌ای",
                        "en" to "International Day for Interventional Cardiology",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/interventional-cardiology-day",
                            title = "International Day for Interventional Cardiology — resolution 76/302 (September 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/interventional-cardiology-day",
                        title = "International Day for Interventional Cardiology (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/interventional-cardiology-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.jazz-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی جاز",
                        "en" to "International Jazz Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/jazz-day",
                            title = "International Jazz Day — UNESCO General Conference, November 2011",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/jazz-day",
                        title = "International Jazz Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/jazz-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.judicial-well-being-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفاه قضایی",
                        "en" to "International Day for Judicial Well-being",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/judicial-well-being-day",
                            title = "International Day for Judicial Well-being — A/RES/79/266 (4 March 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/judicial-well-being-day",
                        title = "International Day for Judicial Well-being (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/judicial-well-being-day",
                ),
        ),
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
                        "en" to "World Kiswahili Language Day",
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
    )
