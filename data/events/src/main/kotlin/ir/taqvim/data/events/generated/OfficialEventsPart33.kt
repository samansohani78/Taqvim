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
 * Part 33 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_33: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.democracy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی دموکراسی",
                        "ar" to "اليوم الدولي للديمقراطية",
                        "en" to "International Day of Democracy",
                        "es" to "Día Internacional de la Democracia",
                        "fr" to "Journée internationale de la démocratie",
                        "ru" to "Международный день демократии",
                        "zh" to "国际民主日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/democracy-day",
                        title = "International Day of Democracy (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/62/7",
                    "un" to "https://www.un.org/en/observances/democracy-day",
                ),
        ),
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
                        "ar" to "اليوم العالمي للتضامن مع الموظفين المحتجزين والمفقودين",
                        "en" to "International Day of Solidarity with Detained and Missing Staff Members",
                        "es" to "Día internacional de Solidaridad con los miembros del personal detenidos o desaparecidos",
                        "fr" to "Journée de solidarité avec les fonctionnaires détenus ou portés disparus",
                        "ru" to "Международный день солидарности с сотрудниками, содержащимися под стражей и пропавшими без вести",
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
    )
