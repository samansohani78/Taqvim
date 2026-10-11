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
 * Part 38 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_38: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.end-impunity-crimes-against-journalists"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی پایان دادن به معافیت از مجازات جرایم علیه روزنامه‌نگاران",
                        "ar" to "اليوم الدولي لإنهاء الإفلات من العقاب على الجرائم المرتكبة ضد الصحافيين",
                        "en" to "International Day to End Impunity for Crimes against Journalists",
                        "es" to "Día Internacional para Poner Fin a la Impunidad de los Crímenes contra Periodistas",
                        "fr" to "Journée internationale de la fin de l’impunité pour les crimes commis contre des journalistes",
                        "ru" to "Международный день прекращения безнаказанности за преступления против журналистов",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_013,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/end-impunity-crimes-against-journalists",
                            title = "International Day to End Impunity for Crimes against Journalists — A/RES/68/163 (2013)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-impunity-crimes-against-journalists",
                        title = "International Day to End Impunity for Crimes against Journalists (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-impunity-crimes-against-journalists",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-nuclear-tests-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی علیه آزمایش‌های هسته‌ای",
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
            id = EventId("un.end-sexual-violence-in-conflict-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه‌کنی خشونت جنسی در منازعات",
                        "ar" to "اليوم الدولي للقضاء على العنف الجنسي في حالات النزاع",
                        "en" to "International Day for the Elimination of Sexual Violence in Conflict",
                        "es" to "Día Internacional para la Eliminación de la Violencia Sexual en los Conflictos",
                        "fr" to "Journée internationale pour l'élimination de la violence sexuelle en temps de conflit",
                        "ru" to "Международный день борьбы с сексуальным насилием в условиях конфликта",
                        "zh" to "消除冲突中性暴力行为国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 19),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_015,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/end-sexual-violence-in-conflict-day",
                            title = "International Day for the Elimination of Sexual Violence in Conflict — A/RES/69/293 (19 June 2015)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-sexual-violence-in-conflict-day",
                        title = "International Day for the Elimination of Sexual Violence in Conflict (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-sexual-violence-in-conflict-day",
                ),
        ),
    )
