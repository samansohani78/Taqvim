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
 * Part 10 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_10: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.mabath"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "مبعث حضرت رسول اکرم صلی الله علیه و آله",
                        "en" to "Beginning of the prophetic mission of Muhammad (peace be upon him and his family)",
                    ),
                ),
            rule = EventRule.Fixed(month = 7, day = 27),
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "13",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "12",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.nature-day"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز طبیعت",
                        "en" to "Nature Day (Sizdah Bedar)",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 13),
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
            id = EventId("ir.holiday.nowruz-1"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "آغاز نوروز",
                        "ar" to "النوروز",
                        "az" to "Novruz",
                        "bn" to "নওরোজ",
                        "ckb" to "نەورۆز",
                        "de" to "Nouruz",
                        "en" to "Nowruz",
                        "es" to "Nowruz",
                        "fr" to "Norouz",
                        "hi" to "नवरोज़",
                        "id" to "Nowruz",
                        "ja" to "ノウルーズ",
                        "kmr" to "Newroz",
                        "ms" to "Nowruz",
                        "ne" to "नवरोज",
                        "prs" to "نوروز",
                        "ps" to "نوروز",
                        "ru" to "Навруз",
                        "ta" to "நவ்ரோஸ்",
                        "tg" to "Наврӯз",
                        "tr" to "Nevruz",
                        "ur" to "نوروز",
                        "uz" to "Navro‘z",
                        "zh" to "诺鲁孜节",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 1),
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
            id = EventId("ir.holiday.nowruz-2"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید نوروز",
                        "ar" to "النوروز",
                        "az" to "Novruz",
                        "bn" to "নওরোজ",
                        "ckb" to "نەورۆز",
                        "de" to "Nouruz",
                        "en" to "Nowruz",
                        "es" to "Nowruz",
                        "fr" to "Norouz",
                        "hi" to "नवरोज़",
                        "id" to "Nowruz",
                        "ja" to "ノウルーズ",
                        "kmr" to "Newroz",
                        "ms" to "Nowruz",
                        "ne" to "नवरोज",
                        "prs" to "نوروز",
                        "ps" to "نوروز",
                        "ru" to "Навруз",
                        "ta" to "நவ்ரோஸ்",
                        "tg" to "Наврӯз",
                        "tr" to "Nevruz",
                        "ur" to "نوروز",
                        "uz" to "Navro‘z",
                        "zh" to "诺鲁孜节",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 2),
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
    )
