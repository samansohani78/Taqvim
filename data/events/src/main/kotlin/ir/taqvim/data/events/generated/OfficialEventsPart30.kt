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
 * Part 30 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_30: List<EventDefinition> =
    listOf(
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
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/snow-leopard-day",
                            title = "International Day of the Snow Leopard — resolution 79/143 (2024)",
                        ),
                ),
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
        EventDefinition(
            id = EventId("un.social-justice-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی عدالت اجتماعی",
                        "ar" to "اليوم العالمي للعدالة الاجتماعية",
                        "en" to "World Day of Social Justice",
                        "es" to "Día Mundial de la Justicia Social",
                        "fr" to "Journée mondiale de la justice sociale",
                        "ru" to "Всемирный день социальной справедливости",
                        "zh" to "世界社会公正日",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/social-justice-day",
                        title = "World Day of Social Justice (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/10",
                    "un" to "https://www.un.org/en/observances/social-justice-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.soil-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خاک",
                        "ar" to "اليوم العالمي للتربة",
                        "en" to "World Soil Day",
                        "es" to "Día Mundial del Suelo",
                        "fr" to "Journée mondiale des sols",
                        "ru" to "Всемирный день почв",
                        "zh" to "世界土壤日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-soil-day",
                        title = "World Soil Day (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/285250-%D8%A7%DB%8C%D8%B1%D8%A7%D9%86-%D8%A8%D8%B1%D9%86%D8%AF%D9%87-%D8%AC%D8%A7%DB%8C%D8%B2%D9%87-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%AE%D8%A7%DA%A9-2024",
                        title = "United Nations in the Islamic Republic of Iran — ایران برنده جایزه روز جهانی خاک ۲۰۲۴",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/68/232",
                    "un" to "https://www.un.org/en/observances/world-soil-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.solstice-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی گرامیداشت انقلاب تابستانی",
                        "en" to "International Day of the Celebration of the Solstice",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/solstice-day",
                            title = "International Day of the Celebration of the Solstice (A/RES/73/300, adopted 20 June 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/solstice-day",
                        title = "International Day of the Celebration of the Solstice (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/solstice-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.south-south-cooperation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز ملل متحد برای همکاری جنوب - جنوب",
                        "ar" to "يوم الأمم المتحدة للتعاون فيما بين بلدان الجنوب",
                        "en" to "United Nations Day for South-South Cooperation",
                        "es" to "Día de las Naciones Unidas para la Cooperación Sur-Sur",
                        "fr" to "Journée des Nations Unies pour la coopération Sud-Sud",
                        "ru" to "День сотрудничества Юг — Юг Организации Объединенных Наций",
                        "zh" to "联合国南南合作日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/south-south-cooperation-day",
                        title = "United Nations Day for South-South Cooperation (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/220",
                    "un" to "https://www.un.org/en/observances/south-south-cooperation-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.space-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی فضا",
                        "en" to "World Space Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 10, day = 4), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_999,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-space-week",
                            title = "General Assembly resolution A/RES/54/68 (1999): World Space Week",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-space-week",
                        title = "General Assembly resolution A/RES/54/68 (1999): World Space Week",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-space-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.spanish-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان اسپانیایی",
                        "en" to "Spanish Language Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 23),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_010,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/russian-language-day",
                            title = "UN Language Days established 2010 by the Department of Global Communications for each of the six official languages",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/es/observances/spanish-language-day",
                        title = "Spanish Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/es/observances/spanish-language-day",
                ),
        ),
    )
