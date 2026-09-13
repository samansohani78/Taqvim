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
 * Part 12 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_12: List<EventDefinition> =
    listOf(
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
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
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
            id = EventId("un.terrorism-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یاد بود و احترام به قربانیان تروریسم",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا الإرهاب وإجلالهم",
                        "en" to "International Day of Remembrance and Tribute to the Victims of Terrorism",
                        "es" to "Día Internacional de Conmemoración y Homenaje a las Víctimas del Terrorismo",
                        "fr" to "Journée internationale du souvenir, en hommage aux victimes du terrorisme",
                        "ru" to "Международный день памяти и поминовения жертв терроризма",
                        "zh" to "纪念和悼念恐怖主义受害者国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 21),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/terrorism-victims-day",
                        title = "International Day of Remembrance and Tribute to the Victims of Terrorism (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/141109-%D9%BE%DB%8C%D8%A7%D9%85-%D8%A2%D9%86%D8%AA%D9%88%D9%86%DB%8C%D9%88-%DA%AF%D9%88%D8%AA%D8%B1%D8%B4%D8%8C-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%DB%8C%D8%A7%D8%AF-%D8%A8%D9%88%D8%AF-%D9%88-%D8%A7%D8%AD%D8%AA%D8%B1%D8%A7%D9%85-%D8%A8%D9%87",
                        title = "United Nations in the Islamic Republic of Iran — پیام آنتونیو گوترش، دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی یاد بود و احترام به قربانیان تروریسم، ۲۱ آگوست ۲۰۲۱ برابر با ۳۰ مرداد ۱۴۰۰",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/72/165",
                    "un" to "https://www.un.org/en/observances/terrorism-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.torture-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی حمایت از قربانیان شکنجه",
                        "ar" to "اليوم الدولي للأمم المتحدة لمساندة ضحايا التعذيب",
                        "en" to "United Nations International Day in Support of Victims of Torture",
                        "es" to "Día Internacional en Apoyo de las Víctimas de la Tortura",
                        "fr" to "Journée internationale pour le soutien aux victimes de la torture",
                        "ru" to "Международный день в поддержку жертв пыток",
                        "zh" to "支援酷刑受害者国际日",
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
                        url = "https://www.un.org/en/observances/torture-victims-day",
                        title = "United Nations International Day in Support of Victims of Torture (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/52/149",
                    "un" to "https://www.un.org/en/observances/torture-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.tourism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی گردشگری",
                        "ar" to "يوم السياحة العالمي",
                        "en" to "World Tourism Day",
                        "es" to "Día Mundial del Turismo",
                        "fr" to "Journée mondiale du tourisme",
                        "ru" to "Всемирный день туризма",
                        "zh" to "世界旅游日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/tourism-day",
                        title = "World Tourism Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/tourism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.transatlantic-slave-trade"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی یاد بود قربانیان بردگی و تجارت برده از آن سوی اقیانوس اطلس",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا الرق وتجارة الرقيق عبر المحيط الأطلسي",
                        "en" to "International Day of Remembrance of the Victims of Slavery and the Transatlantic Slave Trade",
                        "es" to "Día Internacional de Recuerdo de las Víctimas de la Esclavitud y la Trata Transatlántica de Esclavos",
                        "fr" to "Journée internationale de commémoration des victimes de l’esclavage et de la traite transatlantique des esclaves",
                        "ru" to "Международный день памяти жертв рабства и трансатлантической работорговли",
                        "zh" to "奴隶制和跨大西洋贩卖奴隶行为受害者国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/transatlantic-slave-trade",
                        title = "International Day of Remembrance of the Victims of Slavery and the Transatlantic Slave Trade (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/62/122",
                    "un" to "https://www.un.org/en/observances/transatlantic-slave-trade",
                ),
        ),
        EventDefinition(
            id = EventId("un.un-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز ملل متحد",
                        "ar" to "يوم الأمم المتحدة",
                        "en" to "United Nations Day",
                        "es" to "Día de las Naciones Unidas",
                        "fr" to "Journée des Nations Unies",
                        "ru" to "День Организации Объединенных Наций",
                        "zh" to "联合国日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/un-day",
                        title = "United Nations Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/un-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.volunteer-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی داوطلبان برای توسعه اقتصادی و اجتماعی",
                        "ar" to "اليوم الدولي للمتطوعين من أجل التنمية الاقتصادية والاجتماعية",
                        "en" to "International Volunteer Day for Economic and Social Development",
                        "es" to "Día Internacional de los Voluntarios",
                        "fr" to "Journée internationale des volontaires",
                        "ru" to "Международный день добровольцев во имя экономического и социального развития",
                        "zh" to "国际志愿人员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 5),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/volunteer-day",
                        title = "International Volunteer Day for Economic and Social Development (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/40/212",
                    "un" to "https://www.un.org/en/observances/volunteer-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.water-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی آب",
                        "ar" to "اليوم العالمي للمياه",
                        "en" to "World Water Day",
                        "es" to "Día Mundial del Agua",
                        "fr" to "Journée mondiale de l'eau",
                        "ru" to "Всемирный день водных ресурсов",
                        "zh" to "世界水日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/water-day",
                        title = "World Water Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/47/193",
                    "un" to "https://www.un.org/en/observances/water-day",
                ),
        ),
    )
