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
            id = EventId("un.chemical-warfare-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی یادبود قربانیان سلاح های شیمیایی",
                        "ar" to "يوم إحياء ذكرى جميع ضحايا الحرب الكيميائية",
                        "en" to "Day of Remembrance for all Victims of Chemical Warfare",
                        "es" to "Día de Conmemoración de todas las víctimas de la guerra química",
                        "fr" to "Journée du souvenir dédiée à toutes les victimes de la guerre chimique",
                        "ru" to "День памяти всех жертв применения химического оружия",
                        "zh" to "化学战受害者纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/chemical-warfare-victims-day",
                        title = "Day of Remembrance for all Victims of Chemical Warfare (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20140927025001/http://www.unic-ir.org/index.php?option=com_content&view=article&id=392:%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%DB%8C%D8%A7%D8%AF%D8%A8%D9%88%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86%DB%8C%D8%A7%D9%86-%D8%B3%D9%84%D8%A7%D8%AD-%D9%87%D8%A7%DB%8C-%D8%B4%DB%8C%D9%85%DB%8C%D8%A7%DB%8C%DB%8C&catid=8:eventpersian&Itemid=231&lang=fa",
                        title = "United Nations Information Centre Tehran — روز جهانی یادبود قربانیان سلاح های شیمیایی",
                        page = "Wayback Machine snapshot of 2014-09-27 (original site unreachable 2026-09-18)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/chemical-warfare-victims-day",
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
            id = EventId("un.clean-energy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی انرژی پاک",
                        "ar" to "اليوم الدولي للطاقة النظيفة",
                        "en" to "International Day of Clean Energy",
                        "es" to "Día Internacional de la Energía Limpia",
                        "fr" to "Journée internationale des énergies propres",
                        "ru" to "Международный день чистой энергии",
                        "zh" to "国际清洁能源日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/clean-energy-day",
                        title = "International Day of Clean Energy (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/259765-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D8%A7%D9%86%D8%B1%DA%98%DB%8C-%D9%BE%D8%A7%DA%A9",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز جهانی انرژی پاک",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/78/265",
                    "un" to "https://www.un.org/en/observances/clean-energy-day",
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
    )
