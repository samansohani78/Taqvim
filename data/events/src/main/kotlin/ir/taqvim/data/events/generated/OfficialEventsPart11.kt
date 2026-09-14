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
 * Part 11 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_11: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.press-freedom-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آزادی مطبوعاتی",
                        "ar" to "اليوم العالمي لحرية الصحافة",
                        "en" to "World Press Freedom Day",
                        "es" to "Día Mundial de la Libertad de Prensa",
                        "fr" to "Journée mondiale de la liberté de la presse",
                        "ru" to "Всемирный день свободы печати",
                        "zh" to "世界新闻自由日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/press-freedom-day",
                        title = "World Press Freedom Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000090448.page=76",
                    "un" to "https://www.un.org/en/observances/press-freedom-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.protect-education-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حفاظت از آموزش در برابر حملات",
                        "ar" to "اليوم الدولي لحماية التعليم من الهجمات",
                        "en" to "International Day to Protect Education from Attack",
                        "es" to "Día Internacional para Proteger la Educación de Ataques",
                        "fr" to "Journée internationale pour la protection de l’éducation contre les attaques",
                        "ru" to "Международный день защиты образования от нападений",
                        "zh" to "保护教育免受攻击国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/protect-education-day",
                        title = "International Day to Protect Education from Attack (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/322427-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%D8%AD%D9%81%D8%A7%D8%B8%D8%AA-%D8%A7%D8%B2-%D8%A2%D9%85%D9%88%D8%B2%D8%B4-%D8%AF%D8%B1-%D8%A8%D8%B1%D8%A7%D8%A8%D8%B1-%D8%AD%D9%85%D9%84%D8%A7%D8%AA-%DB%B2%DB%B0%DB%B2%DB%B6",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی حفاظت از آموزش در برابر حملات ۲۰۲۶",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/74/275",
                    "un" to "https://www.un.org/en/observances/protect-education-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.public-service-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز خدمات عمومی ملل متحد",
                        "ar" to "يوم الأمم المتحدة للخدمة العامة",
                        "en" to "United Nations Public Service Day",
                        "es" to "Día de las Naciones Unidas para la Administración Pública",
                        "fr" to "Journée des Nations Unies pour la fonction publique",
                        "ru" to "День государственной службы ООН",
                        "zh" to "联合国公务员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/public-service-day",
                        title = "United Nations Public Service Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/57/277",
                    "un" to "https://www.un.org/en/observances/public-service-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.refugee-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پناهندگان",
                        "ar" to "اليوم العالمي للاجئين",
                        "en" to "World Refugee Day",
                        "es" to "Día Mundial de los Refugiados",
                        "fr" to "Journée mondiale des réfugiés",
                        "ru" to "Всемирный день беженцев",
                        "zh" to "世界难民日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/refugee-day",
                        title = "World Refugee Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/55/76",
                    "un" to "https://www.un.org/en/observances/refugee-day",
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
            id = EventId("un.second-world-war-remembrance-days"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "زمان بزرگداشت و آشتی برای کسانی که زندگی خود را در جریان جنگ جهانی دوم از دست دادند",
                        "en" to "Time of Remembrance and Reconciliation for Those Who Lost Their Lives During the Second World War",
                        "fr" to "Journées du souvenir et de la réconciliation en l'honneur des morts de la Seconde Guerre mondiale",
                        "ru" to "Дни памяти и примирения, посвященные погибшим во Второй мировой войне",
                        "zh" to "缅怀第二次世界大战的所有死难者的悼念与和解的时刻",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/second-world-war-remembrance-days",
                        title = "Time of Remembrance and Reconciliation for Those Who Lost Their Lives During the Second World War (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/59/26",
                    "un" to "https://www.un.org/en/observances/second-world-war-remembrance-days",
                ),
        ),
    )
