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
import ir.taqvim.core.model.Weekday

/**
 * Part 13 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_13: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.commemoration-holocaust-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی بزرگداشت خاطره قربانیان هولوکاست",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا محرقة اليهود",
                        "en" to "International Day of Commemoration in Memory of the Victims of the Holocaust",
                        "es" to "Día Internacional de Conmemoración anual en memoria de las víctimas del Holocausto",
                        "fr" to "Journée internationale dédiée à la mémoire des victimes de l'Holocauste",
                        "ru" to "Международный день памяти жертв Холокоста",
                        "zh" to "缅怀大屠杀受难者国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/commemoration-holocaust-victims-day",
                        title = "International Day of Commemoration in Memory of the Victims of the Holocaust (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/60/7",
                    "un" to "https://www.un.org/en/observances/commemoration-holocaust-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.conjoined-twins-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دوقلوهای به‌هم‌چسبیده",
                        "en" to "World Conjoined Twins Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 24),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/conjoined-twins-day",
                            title = "World Conjoined Twins Day — A/RES/78/313 (2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/conjoined-twins-day",
                        title = "World Conjoined Twins Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/conjoined-twins-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.conscience-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی وجدان",
                        "en" to "International Day of Conscience",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/conscience-day",
                            title = "International Day of Conscience (UN General Assembly A/RES/73/329, adopted 25 Jul 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/conscience-day",
                        title = "International Day of Conscience (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/conscience-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cooperatives-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی تعاونیها",
                        "en" to "International Day of Cooperatives",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 7, weekday = Weekday.SATURDAY, n = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cooperatives-day",
                        title = "International Day of Cooperatives (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html; rule: اولين شنبه ژوئيه",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/90",
                    "un" to "https://www.un.org/en/observances/cooperatives-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cotton-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پنبه",
                        "en" to "World Cotton Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/cotton-day",
                            title = "World Cotton Day — General Assembly proclamation, August 2021",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cotton-day",
                        title = "World Cotton Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/cotton-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.countering-hate-speech"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مقابله با نفرت‌پراکنی",
                        "en" to "International Day for Countering Hate Speech",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/countering-hate-speech",
                            title = "International Day for Countering Hate Speech — A/RES/75/309 (July 2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/countering-hate-speech",
                        title = "International Day for Countering Hate Speech (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/countering-hate-speech",
                ),
        ),
        EventDefinition(
            id = EventId("un.creativity-and-innovation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خلاقیت و نوآوری",
                        "en" to "World Creativity and Innovation Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/creativity-and-innovation-day",
                            title = "World Creativity and Innovation Day (UN General Assembly A/RES/71/284, adopted 27 Apr 2017)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/creativity-and-innovation-day",
                        title = "World Creativity and Innovation Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/creativity-and-innovation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.cultural-diversity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی تنوع فرهنگی برای گفت و گو و توسعه",
                        "ar" to "اليوم العالمي للتنوع الثقافي من أجل الحوار والتنمية",
                        "en" to "World Day for Cultural Diversity for Dialogue and Development",
                        "es" to "Día Mundial de la Diversidad Cultural para el Diálogo y el Desarrollo",
                        "fr" to "Journée mondiale de la diversité culturelle pour le dialogue et le développement",
                        "ru" to "Всемирный день культурного разнообразия во имя диалога и развития",
                        "zh" to "世界文化多样性促进对话和发展日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/cultural-diversity-day",
                        title = "World Day for Cultural Diversity for Dialogue and Development (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/249",
                    "un" to "https://www.un.org/en/observances/cultural-diversity-day",
                ),
        ),
    )
