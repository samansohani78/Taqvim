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
 * Part 5 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_5: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.nowruz-3"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید نوروز")),
            rule = EventRule.Fixed(month = 1, day = 3),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "4",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.nowruz-4"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "عید نوروز")),
            rule = EventRule.Fixed(month = 1, day = 4),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "4",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "3",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.oil-nationalization"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "روز ملی شدن صنعت نفت ایران")),
            rule = EventRule.Fixed(month = 12, day = 29),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "15",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "14",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.prophet-demise-imam-hasan-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("fa" to "رحلت حضرت رسول اکرم صلی الله علیه و آله و شهادت حضرت امام حسن مجتبی علیه السلام")),
            rule = EventRule.Fixed(month = 2, day = 28),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "8",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "7",
                    ),
                ),
        ),
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
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "9",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
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
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "14",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
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
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "7",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "6",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("np.holiday.buddha-jayanti"),
            calendar = CalendarSystem.NEPALI,
            source = EventSource.NEPAL_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title = LocalizedText(mapOf("ne" to "बुद्ध जयन्ती")),
            rule = EventRule.LunarTithi(month = 1, tithi = 15, observance = TithiObservance.SUNRISE),
            validity =
                Validity(
                    calendar = CalendarSystem.NEPALI,
                    fromYear = 2_082,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                            title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                            page = "PDF p. 1 (Rajpatra p. 3), clause 2.1(ख), 7.1(क): चण्डी पूर्णिमा (वैशाख पूर्णिमा) - बुद्ध जयन्ति ... वैशाख २९ गते; PDF p. 4: बुद्ध जयन्ती - वैशाख २९ गते — Owner decision 2026-09-17 (ADR-0036, ADR-0038): the LunarTithi rule reproduces the dates of both notices; the record is valid from the first notice checked.",
                        ),
                ),
            aliases =
                listOf(
                    "चण्डी पूर्णिमा",
                    "वैशाख पूर्णिमा",
                    "उभौली पर्व",
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A8_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%AC%E0%A4%BF%E0%A4%A6%E0%A4%BE_%E0%A4%B8%E0%A4%AE%E0%A5%8D%E0%A4%AC%E0%A4%A8%E0%A5%8D%E0%A4%A7%E0%A5%80.pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2082, Nepal Rajpatra Part 5, Vol. 74, No. 59, 2081-11-15",
                        page = "PDF p. 1 (Rajpatra p. 3), clause 2.1(ख), 7.1(क): चण्डी पूर्णिमा (वैशाख पूर्णिमा) - बुद्ध जयन्ति ... वैशाख २९ गते; PDF p. 4: बुद्ध जयन्ती - वैशाख २९ गते",
                    ),
                    Citation(
                        url = "https://moha.gov.np/upload/e66443e81e8cc9c4fa5c099a1fb1bb87/files/%E0%A5%A8%E0%A5%A6%E0%A5%AE%E0%A5%A9_%E0%A4%B8%E0%A4%BE%E0%A4%B2%E0%A4%95%E0%A5%8B_%E0%A4%B8%E0%A4%BE%E0%A4%B0%E0%A5%8D%E0%A4%B5%E0%A4%9C%E0%A4%A8%E0%A4%BF%E0%A4%95_%E0%A4%B5%E0%A4%BF%E0%A4%A6%E0%A4%BE_(67_11_18).pdf",
                        title = "Government of Nepal, Ministry of Home Affairs — «सूचना» on public holidays of BS 2083, Nepal Rajpatra Part 5, Vol. 75, No. 67, 2082-11-18",
                        page = "PDF p. 2, clause 2.1(ख), 7.1(क): चण्डी पूर्णिमा (वैशाख पूर्णिमा) - बुद्ध जयन्ती ... वैशाख १८ गते; PDF p. 6: बुद्ध जयन्ती - वैशाख १८ गते",
                    ),
                ),
        ),
    )
