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
 * Part 27 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_27: List<EventDefinition> =
    listOf(
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
                        "ar" to "اليوم الدولي لإحياء ذكرى كارثة تشيرنوبيل",
                        "en" to "International Chernobyl Disaster Remembrance Day",
                        "es" to "Día Internacional de Recordación del Desastre de Chernóbyl",
                        "fr" to "Journée internationale du souvenir de la catastrophe de Chornobyl",
                        "ru" to "Международный день памяти о чернобыльской катастрофе",
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
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_026,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/80/309",
                            title = "International Day for the Elimination of Child, Early and Forced Marriage — General Assembly resolution A/RES/80/309, \"Resolution adopted by the General Assembly on 4 September 2026 [without reference to a Main Committee (A/80/L.99)]\", para. 1: \"Decides to proclaim 27 November of each year as the International Day for the Elimination of Child, Early and Forced Marriage\".",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://docs.un.org/en/A/RES/80/309",
                        title = "International Day for the Elimination of Child, Early and Forced Marriage (UN General Assembly resolution A/RES/80/309, adopted 4 September 2026; draft A/80/L.99)",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://docs.un.org/en/A/RES/80/309",
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
                        "ar" to "اليوم العالمي لمنع ممارسات الاستغلال والانتهاك والعنف الجنسي ضد الأطفال والتشافي منها",
                        "en" to "World Day for the Prevention of and Healing from Child Sexual Exploitation, Abuse and Violence",
                        "es" to "Día Mundial para Prevenir la Explotación, los Abusos y la Violencia Sexuales contra los Niños y Promover la Sanación",
                        "fr" to "Journée mondiale pour la prévention et la guérison de l’exploitation, des atteintes et des violences sexuelles visant les enfants",
                        "ru" to "Всемирный день предотвращения актов сексуальной эксплуатации детей, жестокого обращения с ними и насилия в их отношении и исцеления детей, пострадавших от таких актов",
                        "zh" to "防止儿童性剥削、性虐待和性暴力及促进受害者疗愈世界日",
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
                        "fa" to "روز بین‌المللی کودکان بی گناه قربانی تجاوز و تعرض",
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
    )
