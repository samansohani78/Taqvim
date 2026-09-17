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
 * Part 10 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_10: List<EventDefinition> =
    listOf(
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
        EventDefinition(
            id = EventId("un.day-of-persons-with-disabilities"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی معلولان",
                        "ar" to "اليوم الدولي للأشخاص ذوي الإعاقة",
                        "en" to "International Day of Persons with Disabilities",
                        "es" to "Día Internacional de las Personas con Discapacidad",
                        "fr" to "Journée internationale des personnes handicapées",
                        "ru" to "Международный день инвалидов",
                        "zh" to "国际残疾人日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                        title = "International Day of Persons with Disabilities (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/47/3",
                    "un" to "https://www.un.org/en/observances/day-of-persons-with-disabilities",
                ),
        ),
        EventDefinition(
            id = EventId("un.day-of-the-seafarer"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز دریانوردان",
                        "ar" to "اليوم الدولي للبحارة",
                        "en" to "Day of the Seafarer",
                        "es" to "Día de la Gente de Mar (OIM)",
                        "fr" to "Journée des gens de mer",
                        "ru" to "День моряка",
                        "zh" to "海员日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.imo.org/en/About/Events/Pages/Day-of-the-Seafarer-2025.aspx",
                        title = "Day of the Seafarer (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://www.un.org/sites/un2.un.org/files/dayoftheseafarer-res19.pdf",
                    "un" to "https://www.imo.org/en/About/Events/Pages/Day-of-the-Seafarer-2025.aspx",
                ),
        ),
        EventDefinition(
            id = EventId("un.democracy-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی دموکراسی",
                        "ar" to "اليوم الدولي للديمقراطية",
                        "en" to "International Day of Democracy",
                        "es" to "Día Internacional de la Democracia",
                        "fr" to "Journée internationale de la démocratie",
                        "ru" to "Международный день демократии",
                        "zh" to "国际民主日",
                    ),
                ),
            rule = EventRule.Fixed(month = 9, day = 15),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/democracy-day",
                        title = "International Day of Democracy (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/62/7",
                    "un" to "https://www.un.org/en/observances/democracy-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.desertification-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مبارزه با گسترش کویر و خشکسالی",
                        "ar" to "اليوم العالمي لمكافحة التصحر والجفاف",
                        "en" to "World Day to Combat Desertification and Drought",
                        "es" to "Día Mundial de Lucha contra la Desertificación",
                        "fr" to "Journée mondiale de la lutte contre la désertification et la sécheresse",
                        "ru" to "Всемирный день борьбы с опустыниванием и засухой",
                        "zh" to "防治荒漠化和干旱世界日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/desertification-day",
                        title = "World Day to Combat Desertification and Drought (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/49/115",
                    "un" to "https://www.un.org/en/observances/desertification-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.development-information-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اطلاعات توسعه",
                        "ar" to "اليوم العالمي للإعلام الإنمائي",
                        "en" to "World Development Information Day",
                        "es" to "Día Mundial de Información sobre el Desarrollo",
                        "fr" to "Journée mondiale d'information sur le développement",
                        "ru" to "Всемирный день информации о развитии",
                        "zh" to "世界发展宣传日",
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
                        url = "https://www.un.org/en/observances/development-information-day",
                        title = "World Development Information Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/development-information-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.diabetes-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دیابت",
                        "ar" to "اليوم العالمي لمرضى السكري",
                        "en" to "World Diabetes Day",
                        "es" to "Día Mundial de la Diabetes",
                        "fr" to "Journée mondiale du diabète",
                        "ru" to "Всемирный день борьбы с диабетом",
                        "zh" to "世界糖尿病日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 14),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/diabetes-day",
                        title = "World Diabetes Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/61/225",
                    "un" to "https://www.un.org/en/observances/diabetes-day",
                ),
        ),
    )
