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
 * Part 15 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_15: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.desertification-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مبارزه با گسترش کویر و خشکسالی",
                        "ar" to "اليوم العالمي لمكافحة التصحر والجفاف",
                        "en" to "World Day to Combat Desertification and Drought",
                        "es" to "Día Mundial de Lucha contra la Desertificación",
                        "fr" to "Journée mondiale de la lutte contre la désertification et la sécheresse",
                        "ru" to "Всемирный день борьбы с опустыниванием и засухой",
                        "zh" to "防治荒漠化和干旱世界日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/desertification-day",
                        title = "World Day to Combat Desertification and Drought (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/49/115",
                    "un" to "https://www.un.org/en/observances/desertification-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.detained-staff-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی همبستگی با کارکنان بازداشت‌شده و مفقود",
                        "en" to "International Day of Solidarity with Detained and Missing Staff Members",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 25),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_994,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/detained-staff-day",
                            title = "International Day of Solidarity with Detained and Missing Staff Members -- linked to GA resolution A/RES/49/59 (1994)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/detained-staff-day",
                        title = "International Day of Solidarity with Detained and Missing Staff Members (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/detained-staff-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.development-information-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اطلاعات توسعه",
                        "ar" to "اليوم العالمي للإعلام الإنمائي",
                        "en" to "World Development Information Day",
                        "es" to "Día Mundial de Información sobre el Desarrollo",
                        "fr" to "Journée mondiale d'information sur le développement",
                        "ru" to "Всемирный день информации о развитии",
                        "zh" to "世界发展宣传日",
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
                        url = "https://www.un.org/en/observances/development-information-day",
                        title = "World Development Information Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/development-information-day",
                ),
        ),
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
                        "en" to "Disarmament Week",
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
                        "fa" to "روز جهانی کاهش مصایب طبیعی",
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
        EventDefinition(
            id = EventId("un.down-syndrome-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی سندرم داون",
                        "en" to "World Down Syndrome Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_011,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/down-syndrome-day",
                            title = "World Down Syndrome Day (A/RES/66/149, adopted 19 December 2011; adoption-year convention, consistent with un.vesak-day/ADR-0044)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/down-syndrome-day",
                        title = "World Down Syndrome Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/down-syndrome-day",
                ),
        ),
    )
