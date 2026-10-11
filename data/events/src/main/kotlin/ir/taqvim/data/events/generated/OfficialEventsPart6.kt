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
 * Part 6 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_6: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("ir.holiday.arbaeen"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "اربعین حسینی",
                        "en" to "Arbaeen",
                    ),
                ),
            rule = EventRule.Fixed(month = 2, day = 20),
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
            id = EventId("ir.holiday.ashura"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عاشورای حسینی",
                        "ar" to "عاشوراء",
                        "az" to "Aşura",
                        "bn" to "আশুরা",
                        "ckb" to "عاشوورا",
                        "de" to "Aschura",
                        "en" to "Ashura",
                        "es" to "Ashura",
                        "fr" to "Achoura",
                        "hi" to "आशूरा",
                        "id" to "Asyura",
                        "ja" to "アーシューラー",
                        "kmr" to "Aşûra",
                        "ms" to "Asyura",
                        "ne" to "आशुरा",
                        "prs" to "عاشورا",
                        "ps" to "عاشورا",
                        "ru" to "Ашура",
                        "ta" to "ஆஷூரா",
                        "tg" to "Ошуро",
                        "tr" to "Aşure Günü",
                        "ur" to "عاشورا",
                        "uz" to "Ashuro",
                        "zh" to "阿舒拉节",
                    ),
                ),
            rule = EventRule.Fixed(month = 1, day = 10),
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
            id = EventId("ir.holiday.eid-al-adha"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید سعید قربان",
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
                        "ne" to "ईद उल अजहा",
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
            citations =
                listOf(
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1404 SH (docs/sources/iran/Calendar-1404.pdf)",
                        page = "6",
                    ),
                    Citation(
                        url = "https://calendar.ut.ac.ir/Fa/",
                        title = "Official calendar of Iran 1405 SH (docs/sources/iran/Calendar-1405.pdf)",
                        page = "5",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("ir.holiday.eid-al-fitr"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.IRAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید سعید فطر",
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
                        "ne" to "ईद उल फित्र",
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
                        page = "3, 14",
                    ),
                ),
        ),
    )
