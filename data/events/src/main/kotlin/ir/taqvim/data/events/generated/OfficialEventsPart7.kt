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
 * Part 7 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_7: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("np.holiday.fagu-purnima-hill"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = false,
            title = LocalizedText(mapOf("ne" to "फागु पूर्णिमा (हिमाली र पहाडी जिल्ला)")),
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
            title = LocalizedText(mapOf("ne" to "फागु पूर्णिमा (तराईका जिल्ला)")),
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
        EventDefinition(
            id = EventId("np.holiday.gai-jatra"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.CULTURAL,
            isHoliday = false,
            title = LocalizedText(mapOf("ne" to "गाईजात्रा")),
            rule = EventRule.LunarTithi(month = 4, tithi = 16, observance = TithiObservance.SUNRISE),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                            page = "PDF p. 4, clause 2.2(क) — the rule reproduces both notices (2082 साउन २५, 2083 भदौ १३), so the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 5 (Rajpatra p. 3), clause 2.2(क): गाईजात्रा- साउन २५ गते आइतबार (देशभरका नेवार समुदायका लागि मात्र). The record is not marked a day off: the notice makes each day a public holiday in its own districts only, and the app does not know which district a user is in, so marking either one nationwide would add a day off that most of the country does not have (DT-038).",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 4, clause 2.2(क): गाईजात्रा - भदौ १३ गते शनिबार (देशभरका नेवार समुदायका लागि मात्र).",
                    ),
                ),
            scope =
                EventScope(
                    level = EventScopeLevel.COMMUNITY,
                    label =
                        LocalizedText(
                            mapOf(
                                "en" to "The Newar community, nationwide",
                                "ne" to "देशभरका नेवार समुदायका लागि मात्र",
                            ),
                        ),
                    areas =
                        listOf(
                            "np.community.newar",
                        ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.ghatasthapana"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "घटस्थापना")),
            rule = EventRule.LunarTithi(month = 6, tithi = 1, observance = TithiObservance.SUNRISE),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ङ): घटस्थापना - असोज ६ गते — Owner decision 2026-09-17 (ADR-0036, ADR-0038): the LunarTithi rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ङ): घटस्थापना - असोज ६ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 2, clause 2.1(ङ): घटस्थापना - असोज २५ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.krishna-janmashtami"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "श्रीकृष्ण जन्माष्टमी")),
            rule = EventRule.LunarTithi(month = 4, tithi = 23, observance = TithiObservance.SUNRISE),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 1 (Rajpatra p. 3), clause 2.1(घ): श्रीकृष्ण जन्माष्टमी - साउन ३१ गते — Owner decision 2026-09-17 (ADR-0036, ADR-0038): the LunarTithi rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 1 (Rajpatra p. 3), clause 2.1(घ): श्रीकृष्ण जन्माष्टमी - साउन ३१ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 2, clause 2.1(घ): श्रीकृष्ण जन्माष्टमी - भदौ १९ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.labour-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "विश्व मजदुर दिवस")),
            rule = EventRule.Fixed(month = 5, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 4 (Rajpatra p. 6), clause 6.1(क): विश्व मजदुर दिवस - (मे १) वैशाख १८ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 4 (Rajpatra p. 6), clause 6.1(क): विश्व मजदुर दिवस - (मे १) वैशाख १८ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 5, clause 6.1(क): विश्व मजदुर दिवस - (मे १) वैशाख १८ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.maghe-sankranti"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.CULTURAL,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "माघी पर्व/माघे सङ्क्रान्ति")),
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
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ठ): माघी पर्व/माघे सङ्क्रान्ति - माघ १ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            aliases =
                listOf(
                    "माघी",
                    "माघे सङ्क्रान्ति",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ठ): माघी पर्व/माघे सङ्क्रान्ति - माघ १ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(ठ): माघी पर्व/माघे सङ्क्रान्ति - माघ १ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.maha-shivaratri"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "महाशिवरात्री")),
            rule = EventRule.LunarTithi(month = 10, tithi = 29, observance = TithiObservance.MIDNIGHT),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ढ): महाशिवरात्री - फागुन ३ गते — Owner decision 2026-09-17 (ADR-0036, ADR-0038): the LunarTithi rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ढ): महाशिवरात्री - फागुन ३ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(ढ): महाशिवरात्री - फागुन २२ गते",
                    ),
                ),
        ),
    )
