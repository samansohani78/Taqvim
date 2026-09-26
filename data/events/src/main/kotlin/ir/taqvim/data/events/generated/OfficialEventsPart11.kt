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
            id = EventId("un.bicycle-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی دوچرخه",
                        "en" to "World Bicycle Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 3),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/bicycle-day",
                            title = "World Bicycle Day (A/RES/72/272, adopted 12 April 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/bicycle-day",
                        title = "World Bicycle Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/bicycle-day",
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
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
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
            id = EventId("un.braille-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی خط بریل",
                        "en" to "World Braille Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 4),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/braille-day/background",
                            title = "World Braille Day — General Assembly resolution A/RES/73/161 (November 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/braille-day",
                        title = "World Braille Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/braille-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.breastfeeding-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی شیردهی",
                        "en" to "World Breastfeeding Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 8, day = 1), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-breastfeeding-week",
                            title = "WHO — World Breastfeeding Week (campaign page: a 2018 World Health Assembly resolution endorsed it as a WHO health-promotion observance)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-breastfeeding-week",
                        title = "WHO — World Breastfeeding Week (campaign page: a 2018 World Health Assembly resolution endorsed it as a WHO health-promotion observance)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-breastfeeding-week",
                ),
        ),
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
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_023,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://docs.un.org/en/A/RES/77/317",
                            title = "International Day of Care and Support — General Assembly resolution A/RES/77/317, \"Resolution adopted by the General Assembly on 24 July 2023\", para. 1: \"Decides to proclaim 29 October as the International Day of Care and Support\".",
                        ),
                ),
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
    )
