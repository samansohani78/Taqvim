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
 * Part 34 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_34: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.diabetes-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دیابت",
                        "ar" to "اليوم العالمي لمرضى السكري",
                        "en" to "World Diabetes Day",
                        "es" to "Día Mundial de la Diabetes",
                        "fr" to "Journée mondiale du diabète",
                        "ru" to "Всемирный день борьбы с диабетом",
                        "zh" to "世界糖尿病日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 14),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/diabetes-day",
                        title = "World Diabetes Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/61/225",
                    "un" to "https://www.un.org/en/observances/diabetes-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.disarmament-non-proliferation-awareness-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از خلع سلاح و منع اشاعه",
                        "en" to "International Day for Disarmament and Non-Proliferation Awareness",
                        "es" to "Día Internacional para Concienciar sobre el Desarme y la No Proliferación",
                        "fr" to "Journée internationale de sensibilisation au désarmement et à la non-prolifération",
                        "ru" to "Международный день просвещения по вопросам разоружения и нераспространения",
                        "zh" to "裁军和不扩散宣传国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                            title = "International Day for Disarmament and Non-Proliferation Awareness -- GA resolution A/RES/77/51 (7 Dec 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                        title = "International Day for Disarmament and Non-Proliferation Awareness (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/disarmament-non-proliferation-awareness-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.disarmament-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته خلع سلاح",
                        "ar" to "أسبوع نزع السلاح",
                        "en" to "Disarmament Week",
                        "es" to "Semana del Desarme",
                        "fr" to "Semaine du désarmement",
                        "ru" to "Неделя разоружения",
                        "zh" to "裁军周",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 10, day = 24), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_978,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/disarmament-week",
                            title = "General Assembly special session on disarmament, Final Document (resolution S-10/2, 1978): Disarmament Week, 24-30 October",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disarmament-week",
                        title = "General Assembly special session on disarmament, Final Document (resolution S-10/2, 1978): Disarmament Week, 24-30 October",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/disarmament-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.disaster-reduction-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی کاهش خطر بلایا",
                        "ar" to "اليوم الدولي للحد من الكوارث",
                        "en" to "International Day for Disaster Risk Reduction",
                        "es" to "Día Internacional para la Reducción de los Desastres",
                        "fr" to "Journée internationale pour la réduction des risques de catastrophes",
                        "ru" to "Международный день по снижению риска бедствий",
                        "zh" to "国际减少灾害风险日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 13),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/disaster-reduction-day",
                        title = "International Day for Disaster Risk Reduction (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/64/200",
                    "un" to "https://www.un.org/en/observances/disaster-reduction-day",
                ),
        ),
    )
