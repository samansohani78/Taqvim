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
 * Part 11 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_11: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.care-and-support-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مراقبت و حمایت",
                        "en" to "International Day of Care and Support",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/care-and-support-day",
                        title = "International Day of Care and Support (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/care-and-support-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.charity-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی نیکوکاری",
                        "en" to "International Day of Charity",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 5),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_012,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/charity-day",
                            title = "International Day of Charity: GA resolution A/RES/67/105 designated 5 September as the day (67th session, 2012)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/charity-day",
                        title = "International Day of Charity (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/charity-day",
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
            id = EventId("un.chernobyl-remembrance-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود فاجعه چرنوبیل",
                        "en" to "International Chernobyl Disaster Remembrance Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 26),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/chernobyl-remembrance-day",
                            title = "International Chernobyl Disaster Remembrance Day — resolution 71/125 (8 December 2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/chernobyl-remembrance-day",
                        title = "International Chernobyl Disaster Remembrance Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/chernobyl-remembrance-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.child-early-forced-marriage-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی ریشه‌کنی ازدواج کودکان، ازدواج زودهنگام و اجباری",
                        "en" to "International Day for the Elimination of Child, Early and Forced Marriage",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://docs.un.org/en/A/80/L.99",
                        title = "International Day for the Elimination of Child, Early and Forced Marriage (UN General Assembly draft resolution A/80/L.99)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://docs.un.org/en/A/80/L.99",
                ),
        ),
        EventDefinition(
            id = EventId("un.child-sexual-exploitation-prevention-and-healing-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پیشگیری و بهبود از بهره‌کشی جنسی، آزار و خشونت علیه کودکان",
                        "en" to "World Day for the Prevention of and Healing from Child Sexual Exploitation, Abuse and Violence",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 18),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_022,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/child-sexual-exploitation-prevention-and-healing-day",
                            title = "World Day for the Prevention of and Healing from Child Sexual Exploitation, Abuse and Violence — A/RES/77/8 (7 November 2022)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/child-sexual-exploitation-prevention-and-healing-day",
                        title = "World Day for the Prevention of and Healing from Child Sexual Exploitation, Abuse and Violence (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/child-sexual-exploitation-prevention-and-healing-day",
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
            id = EventId("un.chinese-language-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز زبان چینی",
                        "en" to "Chinese Language Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 20),
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
                        url = "https://www.un.org/zh/observances/chinese-language-day",
                        title = "Chinese Language Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/zh/observances/chinese-language-day",
                ),
        ),
    )
