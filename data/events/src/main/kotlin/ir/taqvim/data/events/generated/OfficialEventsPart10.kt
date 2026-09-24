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
 * Part 10 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_10: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.argania-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی درخت آرگان",
                        "en" to "International Day of Argania",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_021,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/argania-day",
                            title = "International Day of Argania — A/RES/75/262 (2021)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/argania-day",
                        title = "International Day of Argania (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/argania-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.asteroid-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی سیارک",
                        "en" to "International Asteroid Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 30),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_016,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/asteroid-day",
                            title = "International Asteroid Day — A/RES/71/90 (December 2016)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/asteroid-day",
                        title = "International Asteroid Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/asteroid-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.audiovisual-heritage"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی میراث سمعی و بصری",
                        "en" to "World Day for Audiovisual Heritage",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 27),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 1_980,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.unesco.org/en/days/audiovisual-heritage",
                            title = "World Day for Audiovisual Heritage: UNESCO's 21st General Conference adopted the Recommendation for the Safeguarding and Preservation of Moving Images (1980)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.unesco.org/en/days/audiovisual-heritage",
                        title = "World Day for Audiovisual Heritage (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.unesco.org/en/days/audiovisual-heritage",
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
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
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
    )
