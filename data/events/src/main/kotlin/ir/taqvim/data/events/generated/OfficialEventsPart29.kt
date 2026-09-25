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
 * Part 29 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_29: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.religious-based-violence-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود قربانیان اعمال خشونت بر اساس دین یا باور",
                        "en" to "International Day Commemorating the Victims of Acts of Violence Based on Religion or Belief",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 22),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/religious-based-violence-victims-day",
                            title = "Religious violence victims day — A/RES/73/296 (28 May 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/religious-based-violence-victims-day",
                        title = "International Day Commemorating the Victims of Acts of Violence Based on Religion or Belief (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/religious-based-violence-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.remittances-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حواله‌های خانوادگی",
                        "en" to "International Day of Family Remittances",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/remittances-day",
                            title = "International Day of Family Remittances (A/RES/72/281, adopted 12 June 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/remittances-day",
                        title = "International Day of Family Remittances (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/remittances-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.right-to-truth-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حق بر صحت و درستی درباره نقض فاحش حقوق بشر و منزلت قربانیان",
                        "ar" to "اليوم العالمي للحق في معرفة الحقيقة فيما يتعلق بالانتهاكات الجسيمة لحقوق الإنسان ولاحترام كرامة الضحايا",
                        "en" to "International Day for the Right to the Truth concerning Gross Human Rights Violations and for the Dignity of Victims",
                        "es" to "Día Internacional del Derecho a la Verdad en relación con Violaciones Graves de los Derechos Humanos y de la Dignidad de las Víctimas",
                        "fr" to "Journée internationale pour le droit à la vérité en ce qui concerne les violations flagrantes des droits de l’homme et pour la dignité des victimes",
                        "ru" to "Международный день права на установление истины в отношении грубых нарушений прав человека и достоинства жертв",
                        "zh" to "了解严重侵犯人权行为真相权利和维护受害者尊严国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/right-to-truth-day",
                        title = "International Day for the Right to the Truth concerning Gross Human Rights Violations and for the Dignity of Victims (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/67/200",
                    "un" to "https://www.un.org/en/observances/right-to-truth-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.road-traffic-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی یادآوری قربانیان ترافیک جاده ها",
                        "ar" to "اليوم العالمي لإحياء ذكرى ضحايا حوادث الطرق",
                        "en" to "World Day of Remembrance for Road Traffic Victims",
                        "es" to "Día mundial en recuerdo de las víctimas de los accidentes de tráfico",
                        "ru" to "Всемирный день памяти жертв дорожно-транспортных происшествий",
                        "zh" to "世界道路交通事故受害者纪念日",
                    ),
                ),
            rule = EventRule.NthWeekdayOfMonth(month = 11, weekday = Weekday.SUNDAY, n = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/road-traffic-victims-day",
                        title = "World Day of Remembrance for Road Traffic Victims (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html; rule: سومین یکشنبه نوامبر",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/60/5",
                    "un" to "https://www.un.org/en/observances/road-traffic-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.rural-development-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی توسعه روستایی",
                        "en" to "World Rural Development Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 6),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_024,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/rural-development-day",
                            title = "World Rural Development Day — A/RES/78/326 (6 September 2024)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/rural-development-day",
                        title = "World Rural Development Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/rural-development-day",
                ),
        ),
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
            id = EventId("un.science-and-peace-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته بین‌المللی علم و صلح",
                        "en" to "International Week of Science and Peace",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 11, day = 9), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_988,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/international-week-science-and-peace",
                            title = "General Assembly resolution 43/61 (1988): International Week of Science and Peace",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-week-science-and-peace",
                        title = "General Assembly resolution 43/61 (1988): International Week of Science and Peace",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/international-week-science-and-peace",
                ),
        ),
    )
