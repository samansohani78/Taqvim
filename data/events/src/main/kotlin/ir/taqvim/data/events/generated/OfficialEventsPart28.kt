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
 * Part 28 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_28: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.rural-women-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی زنان روستایی",
                        "ar" to "اليوم الدولي للمرأة الريفية",
                        "en" to "International Day of Rural Women",
                        "es" to "Día Internacional de las Mujeres Rurales",
                        "fr" to "Journée internationale des femmes rurales",
                        "ru" to "Международный день сельских женщин",
                        "zh" to "国际农村妇女日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/rural-women-day",
                        title = "International Day of Rural Women (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/136",
                    "un" to "https://www.un.org/en/observances/rural-women-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.russian-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان روسی",
                        "en" to "Russian Language Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/russian-language-day",
                        title = "Russian Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/russian-language-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.science-technology-and-innovation-for-south-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی علم، فناوری و نوآوری برای کشورهای جنوب",
                        "en" to "International Day of Science, Technology and Innovation for the South",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/science-technology-and-innovation-for-south-day",
                        title = "International Day of Science, Technology and Innovation for the South (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/science-technology-and-innovation-for-south-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.seagrass-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی علف‌های دریایی",
                        "en" to "World Seagrass Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/seagrass-day",
                        title = "World Seagrass Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/seagrass-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.second-world-war-remembrance-days"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "زمان بزرگداشت و آشتی برای کسانی که زندگی خود را در جریان جنگ جهانی دوم از دست دادند",
                        "en" to "Time of Remembrance and Reconciliation for Those Who Lost Their Lives During the Second World War",
                        "fr" to "Journées du souvenir et de la réconciliation en l'honneur des morts de la Seconde Guerre mondiale",
                        "ru" to "Дни памяти и примирения, посвященные погибшим во Второй мировой войне",
                        "zh" to "缅怀第二次世界大战的所有死难者的悼念与和解的时刻",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/second-world-war-remembrance-days",
                        title = "Time of Remembrance and Reconciliation for Those Who Lost Their Lives During the Second World War (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/59/26",
                    "un" to "https://www.un.org/en/observances/second-world-war-remembrance-days",
                ),
        ),
        EventDefinition(
            id = EventId("un.sign-languages-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زبان‌های اشاره",
                        "en" to "International Day of Sign Languages",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/sign-languages-day",
                        title = "International Day of Sign Languages (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/sign-languages-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.slavery-abolition-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی لغو برده داری",
                        "ar" to "اليوم الدولي لإلغاء الرق",
                        "en" to "International Day for the Abolition of Slavery",
                        "es" to "Día Internacional para la Abolición de la Esclavitud",
                        "fr" to "Journée internationale pour l'abolition de l'esclavage",
                        "ru" to "Международный день борьбы за отмену рабства",
                        "zh" to "废除奴隶制国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/slavery-abolition-day",
                        title = "International Day for the Abolition of Slavery (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/317(IV)",
                    "un" to "https://www.un.org/en/observances/slavery-abolition-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.snow-leopard-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پلنگ برفی",
                        "en" to "International Day of the Snow Leopard",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/snow-leopard-day",
                        title = "International Day of the Snow Leopard (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/snow-leopard-day",
                ),
        ),
    )
