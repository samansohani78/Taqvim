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
 * Part 4 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_4: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.prophet-imam-sadiq-birth"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "ولادت حضرت رسول اکرم صلی الله علیه و آله و ولادت حضرت امام جعفر صادق علیه السلام")),
            rule = EventRule.Fixed(month = 3, day = 17),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/Calendar-1404.pdf)",
                        page = "9",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/Calendar-1405.pdf)",
                        page = "8",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.revolution-victory"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "پیروزی انقلاب اسلامی ایران و سقوط نظام شاهنشاهی")),
            rule = EventRule.Fixed(month = 11, day = 22),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/Calendar-1404.pdf)",
                        page = "14",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/Calendar-1405.pdf)",
                        page = "13",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.tasua"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "تاسوعای حسینی")),
            rule = EventRule.Fixed(month = 1, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/Calendar-1404.pdf)",
                        page = "7",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/Calendar-1405.pdf)",
                        page = "6",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("un.africa-industrialization-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز صنعت گستری آفریقا",
                        "ar" to "يوم التصنيع في أفريقيا",
                        "en" to "Africa Industrialization Day",
                        "es" to "Día de la Industrialización de África",
                        "fr" to "Journée de l'industrialisation de l'Afrique",
                        "ru" to "День индустриализации Африки",
                        "zh" to "非洲工业化日",
                    ),
                ),
            rule = EventRule.Fixed(month = 11, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/africa-industrialization-day",
                        title = "Africa Industrialization Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/44/237",
                    "un" to "https://www.un.org/en/observances/africa-industrialization-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.anti-corruption-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین المللی مبارزه با فساد",
                        "ar" to "اليوم الدولي لمكافحة الفساد",
                        "en" to "International Anti-Corruption Day",
                        "es" to "Día Internacional contra la Corrupción",
                        "fr" to "Journée internationale contre la corruption",
                        "ru" to "Международный день борьбы с коррупцией",
                        "zh" to "国际反腐败日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/anti-corruption-day",
                        title = "International Anti-Corruption Day (observance page)",
                    ),
                    Citation(
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/4",
                    "un" to "https://www.un.org/en/observances/anti-corruption-day",
                ),
        ),
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
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
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
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
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
                        url = "https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy: https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm)",
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
