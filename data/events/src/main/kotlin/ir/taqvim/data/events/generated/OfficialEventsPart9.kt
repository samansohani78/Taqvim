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
 * Part 9 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_9: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("np.observance.civil-service-day"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = false,
            title = LocalizedText(mapOf("ne" to "निजामती सेवा दिवस")),
            rule = EventRule.Fixed(month = 5, day = 22),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 5 (Rajpatra p. 7), clause 8(ख): national day, offices open: निजामती सेवा दिवस- भदौ २२ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 5 (Rajpatra p. 7), clause 8(ख): national day, offices open: निजामती सेवा दिवस- भदौ २२ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 6, clause 8(ख): national day, offices open: निजामती सेवा दिवस - भदौ २२ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.observance.genz-martyrs-day"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = false,
            title = LocalizedText(mapOf("ne" to "जेनजी सहिद दिवस")),
            rule = EventRule.Fixed(month = 5, day = 23),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_083,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                            page = "PDF p. 6, clause 8(ग): national day, offices open: जेनजी सहिद दिवस - भदौ २३ गते (first listed in the notice for 2083) — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 6, clause 8(ग): national day, offices open: जेनजी सहिद दिवस - भदौ २३ गते (first listed in the notice for 2083)",
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
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
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
            id = EventId("un.african-descent-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی افراد تبار آفریقایی",
                        "en" to "International Day for People of African Descent",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 31),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_020,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://research.un.org/en/docs/ga/quick/regular/75",
                            title = "UN Dag Hammarskjold Library: A/RES/75/170, adopted 16 December 2020 (corroborated by un.org/en/observances/african-descent-day)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/african-descent-day",
                        title = "International Day for People of African Descent (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/african-descent-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.albinism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی آگاهی از آلبینیسم",
                        "en" to "International Albinism Awareness Day",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 13),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_014,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://research.un.org/en/docs/ga/quick/regular/69",
                            title = "International Albinism Awareness Day (A/RES/69/170, adopted 18 December 2014)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/albinism-day",
                        title = "International Albinism Awareness Day (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/albinism-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.amr-awareness-week"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "هفته جهانی آگاهی از مقاومت ضدمیکروبی",
                        "en" to "World Antimicrobial Resistance Awareness Week",
                    ),
                ),
            rule = EventRule.Week(start = EventRule.Fixed(month = 11, day = 18), lengthDays = 7),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.who.int/campaigns/world-amr-awareness-week",
                            title = "WHO — World AMR Awareness Week (campaign page: dates standardized to 18-24 November by 2018, after 2015's 16-22 November launch as World Antibiotic Awareness Week)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.who.int/campaigns/world-amr-awareness-week",
                        title = "WHO — World AMR Awareness Week (campaign page: dates standardized to 18-24 November by 2018, after 2015's 16-22 November launch as World Antibiotic Awareness Week)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.who.int/campaigns/world-amr-awareness-week",
                ),
        ),
        EventDefinition(
            id = EventId("un.anti-colonialism-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی مبارزه با استعمار در همه اشکال و مظاهر آن",
                        "en" to "International Day against Colonialism in All its Forms and Manifestations",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 14),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_025,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/anti-colonialism-day",
                            title = "International Day against Colonialism in All its Forms and Manifestations — A/RES/80/106 (2025)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/anti-colonialism-day",
                        title = "International Day against Colonialism in All its Forms and Manifestations (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/anti-colonialism-day",
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
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "https://undocs.org/en/A/RES/58/4",
                    "un" to "https://www.un.org/en/observances/anti-corruption-day",
                ),
        ),
    )
