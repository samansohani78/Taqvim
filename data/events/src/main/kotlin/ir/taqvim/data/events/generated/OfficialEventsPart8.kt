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
 * Part 8 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_8: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.humanitarian-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی انساندوستی",
                        "ar" to "اليوم العالمي للعمل الإنساني",
                        "en" to "World Humanitarian Day",
                        "es" to "Día Mundial de la Asistencia Humanitaria",
                        "fr" to "Journée mondiale de l'aide humanitaire",
                        "ru" to "Всемирный день гуманитарной помощи",
                        "zh" to "世界人道主义日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 19),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/humanitarian-day",
                        title = "World Humanitarian Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/63/139",
                    "un" to "https://www.un.org/en/observances/humanitarian-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.indigenous-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مردم بومی جهان",
                        "ar" to "اليوم الدولي للسكان الأصليين في العالم",
                        "en" to "International Day of the World's Indigenous Peoples",
                        "es" to "Día Internacional de los Pueblos Indígenas",
                        "fr" to "Journée internationale des peuples autochtones",
                        "ru" to "Международный день коренных народов мира",
                        "zh" to "世界土著人民国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/indigenous-day",
                        title = "International Day of the World's Indigenous Peoples (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/49/214",
                    "un" to "https://www.un.org/en/observances/indigenous-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-for-dialogue-among-civilizations"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی گفت‌وگوی تمدن‌ها",
                        "ar" to "اليوم الدولي للحوار بين الحضارات",
                        "en" to "International Day for Dialogue among Civilizations",
                        "es" to "Día Internacional para el Diálogo entre Civilizaciones",
                        "fr" to "Journée internationale pour le dialogue entre les civilisations",
                        "ru" to "Международный день диалога между цивилизациями",
                        "zh" to "文明对话国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 10),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-for-dialogue-among-civilizations",
                        title = "International Day for Dialogue among Civilizations (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/296020-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%A8%DB%8C%D9%86%E2%80%8C%D8%A7%D9%84%D9%85%D9%84%D9%84%DB%8C-%DA%AF%D9%81%D8%AA%E2%80%8C%D9%88%DA%AF%D9%88%DB%8C-%D8%AA%D9%85%D8%AF%D9%86%E2%80%8C%D9%87%D8%A7",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز بین‌المللی گفت‌وگوی تمدن‌ها",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://www.undocs.org/EN/A/RES/78/286",
                    "un" to "https://www.un.org/en/observances/international-day-for-dialogue-among-civilizations",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-for-the-remembrance-of-the-slave-trade"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی یادبود و الغاء تجارت برده",
                        "ar" to "اليوم الدولي لاحياء ذكرى تجارة الرقيق وذكرى إلغائها",
                        "en" to "International Day for the Remembrance of the Slave Trade and Its Abolition",
                        "es" to "Día Internacional del Recuerdo de la Trata de Esclavos y de su Abolición",
                        "fr" to "Journée internationale du souvenir de la traite négrière et de son abolition",
                        "ru" to "Международный день памяти о работорговле и ее ликвидации",
                        "zh" to "贩卖奴隶及其废除奴隶制国际纪念日",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/slave-trade-remembrance",
                        title = "International Day for the Remembrance of the Slave Trade and Its Abolition (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000110220.page=72",
                    "un" to "https://www.unesco.org/en/days/slave-trade-remembrance",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-for-tolerance"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی بردباری",
                        "ar" to "اليوم الدولي للتسامح",
                        "en" to "International Day for Tolerance",
                        "es" to "Día Internacional de la Tolerancia",
                        "fr" to "Journée mondiale de la tolérance",
                        "ru" to "Международный день, посвященный терпимости",
                        "zh" to "国际宽容日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/tolerance",
                        title = "International Day for Tolerance (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000101803_eng#page=75",
                    "un" to "https://www.unesco.org/en/days/tolerance",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-clean-air-for-blue-skies"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی هوای پاک برای آسمان آبی",
                        "ar" to "اليوم الدولي لنقاوة الهواء من أجل سماء زرقاء",
                        "en" to "International Day of Clean Air for Blue Skies",
                        "es" to "Día Internacional del Aire Limpio por un Cielo Azul",
                        "fr" to "Journée internationale de l’air pur pour des ciels bleus",
                        "ru" to "Международный день чистого воздуха для голубого неба (ЮНЕП)",
                        "zh" to "国际清洁空气蓝天日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.cleanairblueskies.org/",
                        title = "International Day of Clean Air for Blue Skies (observance page)",
                    ),
                    Citation(
                        url = "https://iran.un.org/fa/245526-%D9%BE%DB%8C%D8%A7%D9%85-%D8%AF%D8%A8%DB%8C%D8%B1%DA%A9%D9%84-%D8%B3%D8%A7%D8%B2%D9%85%D8%A7%D9%86-%D9%85%D9%84%D9%84-%D9%85%D8%AA%D8%AD%D8%AF-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%AC%D9%87%D8%A7%D9%86%DB%8C-%D9%87%D9%88%D8%A7%DB%8C-%D9%BE%D8%A7%DA%A9-%D8%A8%D8%B1%D8%A7%DB%8C-%D8%A2%D8%B3%D9%85%D8%A7%D9%86-%D8%A2%D8%A8%DB%8C-%DB%B2%DB%B0%DB%B2%DB%B3",
                        title = "United Nations in the Islamic Republic of Iran — پیام دبیرکل سازمان ملل متحد به مناسبت روز جهانی هوای پاک برای آسمان آبی ۲۰۲۳",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/74/212",
                    "un" to "https://www.cleanairblueskies.org/",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-families"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی خانواده‌ها",
                        "ar" to "اليوم الدولي للأسر",
                        "en" to "International Day of Families",
                        "es" to "Día Internacional de las Familias",
                        "fr" to "Journée internationale des familles",
                        "ru" to "Международный день семей",
                        "zh" to "国际家庭日",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/international-day-of-families",
                        title = "International Day of Families (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/47/237",
                    "un" to "https://www.un.org/en/observances/international-day-of-families",
                ),
        ),
        EventDefinition(
            id = EventId("un.international-day-of-reflection-on-the-1994-genocide"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز یادبود قربانیان نسل کشی رواندا",
                        "ar" to "اليوم الدولي للتفكر في الإبادة الجماعية التي وقعت في عام 1994 ضد التوتسي في رواندا",
                        "en" to "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda",
                        "es" to "Día Mundial de la Salud",
                        "fr" to "Journée internationale de réflexion sur le génocide des Tutsis au Rwanda en 1994",
                        "ru" to "Международный день памяти о геноциде тутси в Руанде в 1994 году",
                        "zh" to "1994年卢旺达境内针对图西人实施的灭绝种族罪国际反思日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                        title = "International Day of Reflection on the 1994 Genocide against the Tutsi in Rwanda (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/234",
                    "un" to "http://www.un.org/en/preventgenocide/rwanda/commemoration/annualcommemoration.shtml",
                ),
        ),
    )
