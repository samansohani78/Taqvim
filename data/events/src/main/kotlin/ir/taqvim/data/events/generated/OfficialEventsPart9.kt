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
 * Part 9 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_9: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.autism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آگاهی درباره اوتیسم",
                        "ar" to "اليوم العالمي للتوعية بمرض التوحد",
                        "en" to "World Autism Awareness Day",
                        "es" to "Día Mundial de Concienciación sobre el Autismo",
                        "fr" to "Journée mondiale de sensibilisation à l'autisme",
                        "ru" to "Всемирный день распространения информации о проблеме аутизма",
                        "zh" to "世界提高自闭症意识日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 2),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/autism-day",
                        title = "World Autism Awareness Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/139",
                    "un" to "https://www.un.org/en/observances/autism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.biological-diversity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی تنوع زیستی",
                        "ar" to "اليوم الدولي للتنوع البيولوجي",
                        "en" to "International Day for Biological Diversity",
                        "es" to "Día Internacional de la Diversidad Biológica",
                        "fr" to "Journée internationale de la diversité biologique",
                        "ru" to "Международный день биологического разнообразия",
                        "zh" to "生物多样性国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/biological-diversity-day",
                        title = "International Day for Biological Diversity (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/55/201",
                    "un" to "https://www.un.org/en/observances/biological-diversity-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.child-victim-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی کودکان بی گناه قربانی تجاوز و تعرض",
                        "ar" to "اليوم الدولي لضحايا العدوان من الأطفال الأبرياء",
                        "en" to "International Day of Innocent Children Victims of Aggression",
                        "es" to "Día Internacional de los Niños Víctimas Inocentes de Agresión",
                        "fr" to "Journée internationale des enfants victimes innocentes de l'agression",
                        "ru" to "Международный день невинных детей — жертв агрессии",
                        "zh" to "受侵略戕害的无辜儿童国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 4),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/child-victim-day",
                        title = "International Day of Innocent Children Victims of Aggression (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/ES-7/8",
                    "un" to "https://www.un.org/en/observances/child-victim-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.civil-aviation-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی هواپیمایی غیر نظامی",
                        "ar" to "يوم الطيران المدني الدولي",
                        "en" to "International Civil Aviation Day",
                        "es" to "Día de la Aviación Civil Internacional",
                        "fr" to "Journée de l'aviation civile internationale",
                        "ru" to "Международный день гражданской авиации",
                        "zh" to "国际民航日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/civil-aviation-day",
                        title = "International Civil Aviation Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/51/33",
                    "un" to "https://www.un.org/en/observances/civil-aviation-day",
                ),
        ),
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
    )
