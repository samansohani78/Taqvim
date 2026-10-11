/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("NoHardcodedNonLatinText", "MaxLineLength")

package ir.taqvim.data.events.generated

import ir.taqvim.core.calendar.TithiObservance
import ir.taqvim.core.events.Citation
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventScope
import ir.taqvim.core.events.EventScopeLevel
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 16 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_16: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("np.holiday.eid-al-adha"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید قربان",
                        "ar" to "عيد الأضحى",
                        "az" to "Qurban bayramı",
                        "bn" to "ঈদুল আজহা",
                        "ckb" to "جەژنی قوربان",
                        "de" to "Opferfest",
                        "en" to "Eid al-Adha",
                        "es" to "Eid al-Adha",
                        "fr" to "Aïd el-Adha",
                        "hi" to "ईद-उल-अज़हा",
                        "id" to "Iduladha",
                        "ja" to "イード・アル＝アドハー",
                        "kmr" to "Cejna Qurbanê",
                        "ms" to "Hari Raya Aidiladha",
                        "ne" to "बकर ईद (ईद उल अजहा)",
                        "prs" to "عید قربان",
                        "ps" to "لوی اختر",
                        "ru" to "Курбан-байрам",
                        "ta" to "ஈதுல் அத்ஹா",
                        "tg" to "Иди Қурбон",
                        "tr" to "Kurban Bayramı",
                        "ur" to "عید الاضحیٰ",
                        "uz" to "Qurbon hayiti",
                        "zh" to "宰牲节",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(थ): इस्लाम धर्मावलम्बीको पर्व बकर ईद (ईद उल अजहा) का दिन (undated: the day of the festival) — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(थ): इस्लाम धर्मावलम्बीको पर्व बकर ईद (ईद उल अजहा) का दिन (undated: the day of the festival)",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(थ): इस्लाम धर्मावलम्बीको पर्व बकर ईद (ईद उल अजहा) का दिन (undated: the day of the festival)",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.eid-al-fitr"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید فطر",
                        "ar" to "عيد الفطر",
                        "az" to "Ramazan bayramı",
                        "bn" to "ঈদুল ফিতর",
                        "ckb" to "جەژنی ڕەمەزان",
                        "de" to "Fest des Fastenbrechens",
                        "en" to "Eid al-Fitr",
                        "es" to "Eid al-Fitr",
                        "fr" to "Aïd el-Fitr",
                        "hi" to "ईद-उल-फ़ित्र",
                        "id" to "Idulfitri",
                        "ja" to "イード・アル＝フィトル",
                        "kmr" to "Cejna Remezanê",
                        "ms" to "Hari Raya Aidilfitri",
                        "ne" to "ईद (ईद उल फित्र)",
                        "prs" to "عید فطر",
                        "ps" to "کوچنی اختر",
                        "ru" to "Ураза-байрам",
                        "ta" to "ஈதுல் ஃபித்ர்",
                        "tg" to "Иди Фитр",
                        "tr" to "Ramazan Bayramı",
                        "ur" to "عید الفطر",
                        "uz" to "Ramazon hayiti",
                        "zh" to "开斋节",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(त): इस्लाम धर्मावलम्बीको पर्व ईद (ईद उल फित्र) का दिन (undated: the day of the festival) — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(त): इस्लाम धर्मावलम्बीको पर्व ईद (ईद उल फित्र) का दिन (undated: the day of the festival)",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(त): इस्लाम धर्मावलम्बीको पर्व ईद (ईद उल फित्र) का दिन (undated: the day of the festival)",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.fagu-purnima-hill"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "فاگو پورنیما (ناحیه‌های کوهستانی و تپه‌ای)",
                        "en" to "Fagu Purnima (mountain and hill districts)",
                        "ne" to "फागु पूर्णिमा (हिमाली र पहाडी जिल्ला)",
                    ),
                ),
            rule = EventRule.LunarTithi(month = 11, tithi = 15, observance = TithiObservance.SUNSET),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                            page = "PDF p. 3, clause 2.1(द) — the rule reproduces both notices (2082 फागुन १८, 2083 चैत ७), so the record is valid from the first notice checked.",
                        ),
                ),
            aliases =
                listOf(
                    "होली",
                    "फागु",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 4 (Rajpatra p. 2), clause 2.1(द): फागुपूर्णिमा - हिमाली र पहाडी जिल्ला लगायतका ५६ जिल्लामा फागुन १८ गते सोमबार The record is not marked a day off: the notice makes each day a public holiday in its own districts only, and the app does not know which district a user is in, so marking either one nationwide would add a day off that most of the country does not have (DT-038).",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(द): फागुपूर्णिमा - हिमाली र पहाडी ५६ जिल्लाहरूमा चैत ७ गते आइतबार",
                    ),
                ),
            scope =
                EventScope(
                    level = EventScopeLevel.REGION,
                    label =
                        LocalizedText(
                            mapOf(
                                "en" to "The 56 hill and mountain districts",
                                "ne" to "हिमाली र पहाडी ५६ जिल्ला",
                            ),
                        ),
                    areas =
                        listOf(
                            "np.region.hill",
                            "np.region.mountain",
                        ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.fagu-purnima-terai"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "فاگو پورنیما (ناحیه‌های ترای)",
                        "en" to "Fagu Purnima (Terai districts)",
                        "ne" to "फागु पूर्णिमा (तराईका जिल्ला)",
                    ),
                ),
            rule = EventRule.LunarTithi(month = 11, tithi = 15, observance = TithiObservance.SUNRISE),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                            page = "PDF p. 3–4, clause 2.1(द) — the rule reproduces both notices (2082 फागुन १९, 2083 चैत ८), so the record is valid from the first notice checked.",
                        ),
                ),
            aliases =
                listOf(
                    "होली",
                    "फागु",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 4–5 (Rajpatra p. 2–3), clause 2.1(द): … र तराईका देहायका जिल्लामा फागुन १९ गते मङ्गलबार:- झापा, मोरङ, सुनसरी, सप्तरी, सिराहा, उदयपुर, धनुषा, महोत्तरी, सर्लाही, रौतहट, बारा, पर्सा, नवलपरासी (बर्दघाट सुस्ता पूर्व), नवलपरासी (बर्दघाट सुस्ता पश्चिम), रुपन्देही, कपिलवस्तु, दाङ, बाँके, बर्दिया, कैलाली र कञ्चनपुर The record is not marked a day off: the notice makes each day a public holiday in its own districts only, and the app does not know which district a user is in, so marking either one nationwide would add a day off that most of the country does not have (DT-038).",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3–4, clause 2.1(द): … र तराईका जिल्ला झापा, मोरङ, सुनसरी, सप्तरी, सिराहा, उदयपुर, धनुषा, महोत्तरी, सर्लाही, रौतहट, बारा, पर्सा, नवलपरासी (बर्दघाट सुस्ता पूर्व), नवलपरासी (बर्दघाट सुस्ता पश्चिम), रुपन्देही, कपिलवस्तु, दाङ, बाँके, बर्दिया, कैलाली र कञ्चनपुरमा चैत ८ गते सोमबार",
                    ),
                ),
            scope =
                EventScope(
                    level = EventScopeLevel.REGION,
                    label =
                        LocalizedText(
                            mapOf(
                                "en" to "The 21 named Terai districts",
                                "ne" to "तराईका २१ जिल्ला",
                            ),
                        ),
                    areas =
                        listOf(
                            "np.region.terai",
                        ),
                ),
        ),
    )
