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
 * Part 35 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_35: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.womens-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی زنان",
                        "ar" to "اليوم الدولي للمرأة",
                        "en" to "International Women's Day",
                        "es" to "Día Internacional de la Mujer",
                        "fr" to "Journée internationale des femmes",
                        "ru" to "Международный женский день",
                        "zh" to "国际妇女节",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 8),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/womens-day",
                        title = "International Women's Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/womens-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.work-safety-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ایمنی و سلامت در محیط کار",
                        "ar" to "اليوم العالمي للصحة والسلامة في مكان العمل",
                        "en" to "World Day for Safety and Health at Work",
                        "es" to "Día Mundial de la Seguridad y Salud en el Trabajo",
                        "fr" to "Journée mondiale pour la sécurité et la santé au travail",
                        "ru" to "Всемирный день охраны труда",
                        "zh" to "世界工作安全与健康日",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 28),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/work-safety-day",
                        title = "World Day for Safety and Health at Work (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/work-safety-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-aids-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی ایدز",
                        "ar" to "اليوم العالمي للإيدز",
                        "en" to "World AIDS Day",
                        "es" to "Día Mundial del SIDA",
                        "fr" to "Journée mondiale de lutte contre le sida",
                        "ru" to "Всемирный день борьбы со СПИДом",
                        "zh" to "世界艾滋病日",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 1),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-aids-day",
                        title = "World AIDS Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-aids-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-basketball-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بسکتبال",
                        "en" to "World Basketball Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/world-basketball-day",
                            title = "World Basketball Day — A/RES/77/324 (25 August 2023)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/world-basketball-day",
                        title = "World Basketball Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/world-basketball-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.world-bee-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی زنبور عسل",
                        "en" to "World Bee Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 20),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_017,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.fao.org/world-bee-day/en",
                            title = "World Bee Day -- 2017, UN General Assembly proclamation",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.fao.org/world-bee-day/en",
                        title = "World Bee Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.fao.org/world-bee-day/en",
                ),
        ),
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
            id = EventId("un.world-chagas-disease-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی بیماری شاگاس",
                        "en" to "World Chagas Disease Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 4, day = 14),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-chagas-disease-day",
                            title = "World Chagas Disease Day (72nd World Health Assembly decision WHA72.20, May 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-chagas-disease-day",
                        title = "World Chagas Disease Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-chagas-disease-day",
                ),
        ),
    )
