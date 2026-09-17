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

/**
 * Part 11 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_11: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.education-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آموزش",
                        "ar" to "اليوم الدولي للتعليم",
                        "en" to "International Day of Education",
                        "es" to "Día Internacional de la Educación",
                        "fr" to "Journée internationale de l'éducation",
                        "ru" to "Международный день образования",
                        "zh" to "国际教育日",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/education-day",
                        title = "International Day of Education (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/169386-%D9%BE%DB%8C%D8%A7%D9%85-%D8%A2%D9%86%D8%AA%D9%88%D9%86%DB%8C%D9%88-%DA%AF%D9%88%D8%AA%D8%B1%D8%B4%D8%8C-%D8%AF%D8%A8%DB%8C%D8%B1-%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF%D8%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%D8%A2%D9%85%D9%88%D8%B2%D8%B4",
                        title = "United Nations in the Islamic Republic of Iran — پیام آنتونیو گوترش، دبیر کل سازمان ملل متحد، به مناسبت روز بین‌المللی آموزش",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/73/25",
                    "un" to "https://www.un.org/en/observances/education-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-drug-abuse-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مبارزه با سوء استفاده و قاچاق مواد مخدر",
                        "ar" to "اليوم الدولي لمكافحة إساءة استعمال المخدرات والاتجار غير المشروع بها",
                        "en" to "International Day against Drug Abuse and Illicit Trafficking",
                        "es" to "Día Internacional de la Lucha contra el Uso Indebido y el Tráfico Ilícito de Drogas",
                        "fr" to "Journée internationale contre l'abus et le trafic de drogues",
                        "ru" to "Международный день борьбы со злоупотреблением наркотическими средствами и их незаконным оборотом",
                        "zh" to "禁止药物滥用和非法贩运国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-drug-abuse-day",
                        title = "International Day against Drug Abuse and Illicit Trafficking (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/42/112",
                    "un" to "https://www.un.org/en/observances/end-drug-abuse-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-human-trafficking-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مبارزه با قاچاق انسان",
                        "ar" to "اليوم العالمي لمكافحة الاتجار بالأشخاص",
                        "en" to "World Day against Trafficking in Persons",
                        "es" to "Día Mundial contra la Trata",
                        "fr" to "Journée mondiale de la lutte contre la traite d’êtres humains",
                        "ru" to "Всемирный день борьбы с торговлей людьми",
                        "zh" to "世界打击贩运人口日",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 30),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-human-trafficking-day",
                        title = "World Day against Trafficking in Persons (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/86445-%D9%BE%DB%8C%D8%A7%D9%85-%D8%BA%D8%A7%D8%AF%D8%A7-%D9%81%D8%AA%D8%AD%DB%8C-%D9%88%D8%A7%D9%84%DB%8C-%D9%85%D8%AF%DB%8C%D8%B1-%DA%A9%D9%84-%D8%AF%D9%81%D8%AA%D8%B1-%D9%85%D9%82%D8%A7%D8%A8%D9%84%D9%87-%D8%A8%D8%A7-%D9%85%D9%88%D8%A7%D8%AF-%D9%85%D8%AE%D8%AF%D8%B1-%D9%88-%D8%AC%D8%B1%D9%85-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF%D8%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2",
                        title = "United Nations in the Islamic Republic of Iran — پیام غادا فتحی والی مدیر کل دفتر مقابله با مواد مخدر و جرم سازمان ملل متحد، به مناسبت روز جهانی مبارزه با قاچاق انسان",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/68/192",
                    "un" to "https://www.un.org/en/observances/end-human-trafficking-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-nuclear-tests-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی علیه آزمایش های هسته ای",
                        "ar" to "اليوم الدولي لمناهضة التجارب النووية",
                        "en" to "International Day against Nuclear Tests",
                        "es" to "Día Internacional contra los Ensayos Nucleares",
                        "fr" to "Journée internationale contre les essais nucléaires",
                        "ru" to "Международный день действий против ядерных испытаний",
                        "zh" to "禁止核试验国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-nuclear-tests-day",
                        title = "International Day against Nuclear Tests (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/64/35",
                    "un" to "https://www.un.org/en/observances/end-nuclear-tests-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.end-racism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی رفع تبعیض نژادی",
                        "ar" to "اليوم الدولي للقضاء على التمييز العنصري",
                        "en" to "International Day for the Elimination of Racial Discrimination",
                        "es" to "Día Internacional de la Eliminación de la Discriminación Racial",
                        "fr" to "Journée internationale pour l'élimination de la discrimination raciale",
                        "ru" to "Международный день борьбы за ликвидацию расовой дискриминации",
                        "zh" to "消除种族歧视国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/end-racism-day",
                        title = "International Day for the Elimination of Racial Discrimination (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/end-racism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.ending-violence-against-women-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حذف خشونت علیه زنان",
                        "ar" to "اليوم الدولي للقضاء على العنف ضد المرأة",
                        "en" to "International Day for the Elimination of Violence against Women",
                        "es" to "Día Internacional de la Eliminación de la Violencia contra la Mujer",
                        "fr" to "Journée internationale pour l'élimination de la violence à l'égard des femmes",
                        "ru" to "Международный день борьбы за ликвидацию насилия в отношении женщин",
                        "zh" to "制止暴力侵害妇女行为国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/ending-violence-against-women-day",
                        title = "International Day for the Elimination of Violence against Women (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/32/40",
                    "un" to "https://www.un.org/en/observances/ending-violence-against-women-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.environment-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی محیط زیست",
                        "ar" to "اليوم العالمي للبيئة",
                        "en" to "World Environment Day",
                        "es" to "Día Mundial del Medio Ambiente",
                        "fr" to "Journée mondiale de l'environnement",
                        "ru" to "Всемирный день окружающей среды",
                        "zh" to "世界环境日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/environment-day",
                        title = "World Environment Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/environment-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.environment-in-war-protection-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی پیشگیری از سوء استفاده از محیط زیست در جنگ و مناقشه مسلحانه",
                        "ar" to "اليوم الدولي لمنع استخدام البيئة في الحروب والصراعات العسكرية",
                        "en" to "International Day for Preventing the Exploitation of the Environment in War and Armed Conflict",
                        "es" to "Día Internacional para la Prevención de la Explotación del Medio Ambiente en la Guerra y los Conflictos Armados",
                        "fr" to "Journée internationale pour la prévention de l'exploitation de l'environnement en temps de guerre et de conflit armé",
                        "ru" to "Международный день предотвращения эксплуатации окружающей среды во время войны и вооруженных конфликтов",
                        "zh" to "防止战争和武装冲突糟蹋环境国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 6),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/environment-in-war-protection-day",
                        title = "International Day for Preventing the Exploitation of the Environment in War and Armed Conflict (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/56/4",
                    "un" to "https://www.un.org/en/observances/environment-in-war-protection-day",
                ),
        ),
    )
