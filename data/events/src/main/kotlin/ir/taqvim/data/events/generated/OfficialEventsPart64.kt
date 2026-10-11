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
 * Part 64 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_64: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم الدولي للغات الإشارة",
                        "en" to "International Day of Sign Languages",
                        "es" to "Día Internacional de las Lenguas de Señas",
                        "fr" to "Journée internationale des langues des signes",
                        "ru" to "Международный день жестовых языков",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 23),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/sign-languages-day",
                            title = "International Day of Sign Languages — A/RES/72/161 (19 December 2017)",
                        ),
                ),
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
                        "fa" to "روز بین‌المللی لغو برده‌داری",
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
                        "es" to "Día Internacional del Leopardo de las Nieves",
                        "ru" to "Международный день снежного барса",
                        "zh" to "国际雪豹日",
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
    )
