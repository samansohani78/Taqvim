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
import ir.taqvim.core.model.Weekday

/**
 * Part 7 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_7: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.end-nuclear-tests-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی علیه آزمایش های هسته ای",
                        "ar" to "اليوم الدولي لمناهضة التجارب النووية",
                        "en" to "International Day against Nuclear Tests",
                        "es" to "Día Internacional contra los Ensayos Nucleares",
                        "fr" to "Journée internationale contre les essais nucléaires",
                        "ru" to "Международный день действий против ядерных испытаний",
                        "zh" to "禁止核试验国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-nuclear-tests-day",
                        title = "International Day against Nuclear Tests (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/64/35",
                    "un" to "https://www.un.org/en/observances/end-nuclear-tests-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-racism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفع تبعیض نژادی",
                        "ar" to "اليوم الدولي للقضاء على التمييز العنصري",
                        "en" to "International Day for the Elimination of Racial Discrimination",
                        "es" to "Día Internacional de la Eliminación de la Discriminación Racial",
                        "fr" to "Journée internationale pour l'élimination de la discrimination raciale",
                        "ru" to "Международный день борьбы за ликвидацию расовой дискриминации",
                        "zh" to "消除种族歧视国际日",
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
                        url = "https://www.un.org/en/observances/end-racism-day",
                        title = "International Day for the Elimination of Racial Discrimination (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-racism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.ending-violence-against-women-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حذف خشونت علیه زنان",
                        "ar" to "اليوم الدولي للقضاء على العنف ضد المرأة",
                        "en" to "International Day for the Elimination of Violence against Women",
                        "es" to "Día Internacional de la Eliminación de la Violencia contra la Mujer",
                        "fr" to "Journée internationale pour l'élimination de la violence à l'égard des femmes",
                        "ru" to "Международный день борьбы за ликвидацию насилия в отношении женщин",
                        "zh" to "制止暴力侵害妇女行为国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/ending-violence-against-women-day",
                        title = "International Day for the Elimination of Violence against Women (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/32/40",
                    "un" to "https://www.un.org/en/observances/ending-violence-against-women-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.environment-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی محیط زیست",
                        "ar" to "اليوم العالمي للبيئة",
                        "en" to "World Environment Day",
                        "es" to "Día Mundial del Medio Ambiente",
                        "fr" to "Journée mondiale de l'environnement",
                        "ru" to "Всемирный день окружающей среды",
                        "zh" to "世界环境日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/environment-day",
                        title = "World Environment Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/environment-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.environment-in-war-protection-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی پیشگیری از سوء استفاده از محیط زیست در جنگ و مناقشه مسلحانه",
                        "ar" to "اليوم الدولي لمنع استخدام البيئة في الحروب والصراعات العسكرية",
                        "en" to "International Day for Preventing the Exploitation of the Environment in War and Armed Conflict",
                        "es" to "Día Internacional para la Prevención de la Explotación del Medio Ambiente en la Guerra y los Conflictos Armados",
                        "fr" to "Journée internationale pour la prévention de l'exploitation de l'environnement en temps de guerre et de conflit armé",
                        "ru" to "Международный день предотвращения эксплуатации окружающей среды во время войны и вооруженных конфликтов",
                        "zh" to "防止战争和武装冲突糟蹋环境国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/environment-in-war-protection-day",
                        title = "International Day for Preventing the Exploitation of the Environment in War and Armed Conflict (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/56/4",
                    "un" to "https://www.un.org/en/observances/environment-in-war-protection-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.habitat-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اسکان بشری",
                        "en" to "World Habitat Day",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 10, weekday = Weekday.MONDAY, n = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/habitat-day",
                        title = "World Habitat Day (observance page)",
                        page = "Background: \"the first Monday of October of every year\"",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/40/202",
                    "un" to "https://www.un.org/en/observances/habitat-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-rights-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی حقوق بشر",
                        "ar" to "يوم حقوق الإنسان",
                        "en" to "Human Rights Day",
                        "es" to "Día de los Derechos Humanos",
                        "fr" to "Journée des droits de l'homme",
                        "ru" to "День прав человека",
                        "zh" to "人权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-rights-day",
                        title = "Human Rights Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/human-rights-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.human-solidarity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی همبستگی بشر",
                        "ar" to "اليوم الدولي للتضامن الإنساني",
                        "en" to "International Human Solidarity Day",
                        "es" to "Día Internacional de la Solidaridad Humana",
                        "fr" to "Journée internationale de la solidarité humaine",
                        "ru" to "Международный день солидарности людей",
                        "zh" to "国际人类团结日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/human-solidarity-day",
                        title = "International Human Solidarity Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/60/209",
                    "un" to "https://www.un.org/en/observances/human-solidarity-day",
                ),
        ),
    )
