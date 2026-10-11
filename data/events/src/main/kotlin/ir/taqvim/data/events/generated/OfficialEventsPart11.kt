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
 * Part 11 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_11: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.nowruz-3"),
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
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز ملی شدن صنعت نفت ایران",
                        "en" to "Nationalization of the Iranian oil industry",
                    ),
                ),
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
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/99002",
                        title = "لایحه قانونی تعیین تعطیلات رسمی کشور, approved 1359/04/08: the base list of official holidays, which includes «29 اسفند تا 4 فروردین».",
                    ),
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/93217",
                        title = "قانون اصلاح لایحه قانونی تعیین تعطیلات رسمی کشور, approved 1378/05/25: «…و تعطیلی روز بیست و نهم اسفند ماه لغو می‌گردد» — the 29 Esfand holiday was abolished.",
                    ),
                    Citation(
                        url = "https://rc.majlis.ir/fa/law/show/93237",
                        title = "قانون راجع به تعطیل روز ملی شدن صنعت نفت, approved 1378/08/18, Guardian Council 1378/08/19: «روز ملی شدن صنعت نفت که مصادف با 29 اسفند ماه هر سال می‌باشد کماکان جزء تعطیلات رسمی کشور محسوب می‌شود» — restored three months after the abolition, which is why every official calendar from 1381 SH prints it as (تعطیل).",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.prophet-demise-imam-hasan-martyrdom"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "رحلت حضرت رسول اکرم صلی الله علیه و آله و شهادت حضرت امام حسن مجتبی علیه السلام",
                        "en" to "Death anniversary of Prophet Muhammad (peace be upon him and his family) and martyrdom of Imam Hasan al-Mujtaba (peace be upon him)",
                    ),
                ),
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
    )
