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
 * Part 19 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_19: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.world-blood-donor-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی اهداکنندگان خون",
                        "ar" to "اليوم العالمي للمتبرّعين بالدم",
                        "en" to "World Blood Donor Day",
                        "es" to "Día Mundial del Donante de Sangre",
                        "fr" to "Journée mondiale du donneur de sang",
                        "ru" to "Всемирный день донора крови",
                        "zh" to "世界献血者日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 14),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-blood-donor-day",
                        title = "World Blood Donor Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://apps.who.int/gb/ebwha/pdf_files/WHA58/WHA58_13-en.pdf",
                    "un" to "https://www.who.int/campaigns/world-blood-donor-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-book-and-copyright-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی کتاب و حق پدید آورنده",
                        "ar" to "اليوم العالمي للكتاب وحقوق المؤلف",
                        "en" to "World Book and Copyright Day",
                        "es" to "Día Mundial del Libro y del Derecho de Autor",
                        "fr" to "Journée mondiale du livre et du droit d'auteur",
                        "ru" to "Всемирный день книги и авторского права",
                        "zh" to "世界图书与版权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 23),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/world-book-and-copyright",
                        title = "World Book and Copyright Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://unesdoc.unesco.org/ark:/48223/pf0000101803.page=56",
                    "un" to "https://www.unesco.org/en/days/world-book-and-copyright",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-childrens-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی کودکان",
                        "ar" to "اليوم العالمي للطفل",
                        "en" to "World Children's Day",
                        "es" to "Día Mundial de la Infancia",
                        "fr" to "Journée mondiale de l'enfance",
                        "ru" to "Всемирный день ребенка",
                        "zh" to "世界儿童日",
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
                        url = "https://www.un.org/en/observances/world-childrens-day",
                        title = "World Children's Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/836(IX)",
                    "un" to "https://www.un.org/en/observances/world-childrens-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-day-against-child-labour"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی علیه کار کودک",
                        "ar" to "اليوم العالمي لمكافحة عمل الأطفال",
                        "en" to "World Day Against Child Labour",
                        "es" to "Día Mundial contra el Trabajo Infantil",
                        "fr" to "Journée mondiale contre le travail des enfants",
                        "ru" to "Всемирный день борьбы с детским трудом",
                        "zh" to "世界无童工日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 12),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-day-against-child-labour",
                        title = "World Day Against Child Labour (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-day-against-child-labour",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-food-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی غذا",
                        "ar" to "يوم الأغذية العالمي",
                        "en" to "World Food Day",
                        "es" to "Día Mundial de la Alimentación",
                        "fr" to "Journée mondiale de l'alimentation",
                        "ru" to "Всемирный день продовольствия",
                        "zh" to "世界粮食日",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 16),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.fao.org/world-food-day",
                        title = "World Food Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/35/70",
                    "un" to "http://www.fao.org/world-food-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-health-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بهداشت",
                        "en" to "World Health Day",
                        "fr" to "Journée mondiale de la santé",
                        "ru" to "Всемирный день здоровья",
                        "zh" to "世界卫生日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 7),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-health-day",
                        title = "World Health Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://apps.who.int/iris/bitstream/handle/10665/86099/WHA2.35_eng.pdf",
                    "un" to "https://www.who.int/campaigns/world-health-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-intellectual-property-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مالکیت معنوی",
                        "en" to "World Intellectual Property Day",
                        "es" to "Día Mundial de la Propiedad Intelectual",
                        "fr" to "Journée mondiale de la propriété intellectuelle",
                        "ru" to "Международный день интеллектуальной собственности",
                        "zh" to "世界知识产权日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 26),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "http://www.wipo.int/ip-outreach/en/ipday/",
                        title = "World Intellectual Property Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "http://www.wipo.int/ip-outreach/en/ipday/",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-malaria-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی مالاریا",
                        "ar" to "اليوم العالمي للملاريا",
                        "en" to "World Malaria Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 25),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-malaria-day/world-malaria-day-2021",
                        title = "World Malaria Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-malaria-day/world-malaria-day-2021",
                ),
        ),
    )
