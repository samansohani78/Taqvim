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
 * Part 13 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_13: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.day-against-unilateral-coercive-measures"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مخالفت با اقدامات قهری یکجانبه",
                        "en" to "International Day Against Unilateral Coercive Measures",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 4),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                            title = "International Day against Unilateral Coercive Measures — resolution 79/293 (June 2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                        title = "International Day Against Unilateral Coercive Measures (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/day-against-unilateral-coercive-measures",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-for-eradicating-poverty"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه کنی فقر",
                        "ar" to "اليوم الدولي للقضاء على الفقر",
                        "en" to "International Day for the Eradication of Poverty",
                        "es" to "Día Internacional para la Erradicación de la Pobreza",
                        "fr" to "Journée internationale pour l'élimination de la pauvreté",
                        "ru" to "Международный день борьбы за ликвидацию нищеты",
                        "zh" to "消除贫穷国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-for-eradicating-poverty",
                        title = "International Day for the Eradication of Poverty (observance page)",
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
                    "un" to "https://www.un.org/en/observances/day-for-eradicating-poverty",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-of-combating-sand-and-dust-storms"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با طوفان‌های شن و گرد و غبار",
                        "en" to "International Day of Combating Sand and Dust Storms",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                            title = "International Day of Combating Sand and Dust Storms — A/RES/77/294 (8 June 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                        title = "International Day of Combating Sand and Dust Storms (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/day-of-combating-sand-and-dust-storms",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-of-persons-with-disabilities"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی معلولان",
                        "ar" to "اليوم الدولي للأشخاص ذوي الإعاقة",
                        "en" to "International Day of Persons with Disabilities",
                        "es" to "Día Internacional de las Personas con Discapacidad",
                        "fr" to "Journée internationale des personnes handicapées",
                        "ru" to "Международный день инвалидов",
                        "zh" to "国际残疾人日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                        title = "International Day of Persons with Disabilities (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/3",
                    "un" to "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                ),
        ),
    )
