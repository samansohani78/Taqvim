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
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Validity
import ir.taqvim.core.model.CalendarSystem

/**
 * Part 8 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_8: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("np.holiday.tamu-lhochhar"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.CULTURAL,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "तमू ल्होछार")),
            rule = EventRule.Fixed(month = 9, day = 15),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ट): तमू ल्होछार - पुस १५ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(ट): तमू ल्होछार - पुस १५ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 3, clause 2.1(ट): तमू ल्होछार - पुस १५ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.tihar"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "तिहार बिदा")),
            rule = EventRule.LunarTithi(month = 6, tithi = 30, observance = TithiObservance.SUNSET, endTithi = 2, endOffsetDays = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(छ): तिहार बिदा - कात्तिक ३ गतेदेखि कात्तिक ७ गतेसम्म (लक्ष्मीपूजा देखि भाइटिकाको भोलिपल्टसम्म) — Owner decision 2026-09-17 (ADR-0036, ADR-0038): the LunarTithi rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            aliases =
                listOf(
                    "तिहार",
                    "लक्ष्मीपूजा",
                    "भाइटिका",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 2 (Rajpatra p. 4), clause 2.1(छ): तिहार बिदा - कात्तिक ३ गतेदेखि कात्तिक ७ गतेसम्म (लक्ष्मीपूजा देखि भाइटिकाको भोलिपल्टसम्म)",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 2, clause 2.1(छ): तिहार बिदा - कात्तिक २२ गतेदेखि कात्तिक २६ गतेसम्म (लक्ष्मीपूजा देखि भाइटिकाको भोलिपल्टसम्म), continued on PDF p. 3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.womens-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "अन्तर्राष्ट्रिय महिला दिवस")),
            rule = EventRule.Fixed(month = 3, day = 8),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 4 (Rajpatra p. 6), clause 6.1(च): अन्तर्राष्ट्रिय महिला दिवस (मार्च ८) - फागुन २४ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 4 (Rajpatra p. 6), clause 6.1(च): अन्तर्राष्ट्रिय महिला दिवस (मार्च ८) - फागुन २४ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 5, clause 6.1(च): अन्तर्राष्ट्रिय महिला दिवस (मार्च ८) - फागुन २४ गते",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.observance.anti-untouchability-day"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = false,
            title = LocalizedText(mapOf("ne" to "जातीय भेदभाव तथा छुवाछुत उन्मूलन राष्ट्रिय दिवस")),
            rule = EventRule.Fixed(month = 2, day = 21),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 5 (Rajpatra p. 7), clause 8(क): national day, offices open: जातीय भेदभाव तथा छुवाछुत उन्मूलन राष्ट्रिय दिवस - जेठ २१ गते — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 5 (Rajpatra p. 7), clause 8(क): national day, offices open: जातीय भेदभाव तथा छुवाछुत उन्मूलन राष्ट्रिय दिवस - जेठ २१ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 6, clause 8(क): national day, offices open: जातीय भेदभाव तथा छुवाछुत उन्मूलन राष्ट्रिय दिवस - जेठ २१ गते",
                    ),
                ),
        ),
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
    )
