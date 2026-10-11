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
 * Part 61 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_61: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("un.refugee-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز جهانی پناهندگان",
                        "ar" to "اليوم العالمي للاجئين",
                        "en" to "World Refugee Day",
                        "es" to "Día Mundial de los Refugiados",
                        "fr" to "Journée mondiale des réfugiés",
                        "ru" to "Всемирный день беженцев",
                        "zh" to "世界难民日",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 20),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/refugee-day",
                        title = "World Refugee Day (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/55/76",
                    "un" to "https://www.un.org/en/observances/refugee-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.religious-based-violence-victims-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی یادبود قربانیان اعمال خشونت بر اساس دین یا باور",
                        "ar" to "اليوم الدولي لإحياء ذكرى ضحايا أعمال العنف القائمة على أساس الدين أو المعتقد",
                        "en" to "International Day Commemorating the Victims of Acts of Violence Based on Religion or Belief",
                        "es" to "Día de Conmemoración de las Víctimas de Actos de Violencia basados en la Religión o las Creencias",
                        "fr" to "Journée internationale de commémoration des personnes victimes de violences en raison de leur religion ou de leurs convictions",
                        "ru" to "Международный день памяти жертв актов насилия на основе религии или убеждений",
                    ),
                ),
            rule = EventRule.Fixed(month = 8, day = 22),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_019,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/religious-based-violence-victims-day",
                            title = "Religious violence victims day — A/RES/73/296 (28 May 2019)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/religious-based-violence-victims-day",
                        title = "International Day Commemorating the Victims of Acts of Violence Based on Religion or Belief (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/religious-based-violence-victims-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.remittances-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حواله‌های خانوادگی",
                        "ar" to "اليوم الدولي للتحويلات المالية العائلية",
                        "en" to "International Day of Family Remittances",
                        "es" to "Día Internacional de las Remesas Familiares",
                        "fr" to "Journée internationale des envois de fonds familiaux",
                        "ru" to "Международный день семейных денежных переводов",
                    ),
                ),
            rule = EventRule.Fixed(month = 6, day = 16),
            validity =
                Validity(
                    calendar = CalendarSystem.GREGORIAN,
                    fromYear = 2_018,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.un.org/en/observances/remittances-day",
                            title = "International Day of Family Remittances (A/RES/72/281, adopted 12 June 2018)",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions of the same page for the titles in those languages",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/remittances-day",
                        title = "International Day of Family Remittances (observance page)",
                    ),
                ),
            links =
                mapOf(
                    "un" to "https://www.un.org/en/observances/remittances-day",
                ),
        ),
        EventDefinition(
            id = EventId("un.right-to-truth-day"),
            calendar = CalendarSystem.GREGORIAN,
            source = EventSource.INTERNATIONAL,
            category = EventCategory.INTERNATIONAL,
            isHoliday = false,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز بین‌المللی حق دانستن حقیقت دربارهٔ نقض‌های فاحش حقوق بشر و کرامت قربانیان",
                        "ar" to "اليوم العالمي للحق في معرفة الحقيقة فيما يتعلق بالانتهاكات الجسيمة لحقوق الإنسان ولاحترام كرامة الضحايا",
                        "en" to "International Day for the Right to the Truth concerning Gross Human Rights Violations and for the Dignity of Victims",
                        "es" to "Día Internacional del Derecho a la Verdad en relación con Violaciones Graves de los Derechos Humanos y de la Dignidad de las Víctimas",
                        "fr" to "Journée internationale pour le droit à la vérité en ce qui concerne les violations flagrantes des droits de l’homme et pour la dignité des victimes",
                        "ru" to "Международный день права на установление истины в отношении грубых нарушений прав человека и достоинства жертв",
                        "zh" to "了解严重侵犯人权行为真相权利和维护受害者尊严国际日",
                    ),
                ),
            rule = EventRule.Fixed(month = 3, day = 24),
            citations =
                listOf(
                    Citation(
                        url = "https://www.un.org/en/observances/list-days-weeks",
                        title = "United Nations — List of International Days and Weeks — also the ar, zh, fr, ru, es editions (https://www.un.org/<language>/observances/list-days-weeks)",
                    ),
                    Citation(
                        url = "https://www.un.org/en/observances/right-to-truth-day",
                        title = "International Day for the Right to the Truth concerning Gross Human Rights Violations and for the Dignity of Victims (observance page)",
                    ),
                    Citation(
                        url = "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
                        title = "United Nations Information Centre Tehran — مناسبت های ویژه سازمان ملل متحد (Internet Archive copy of https://www.unic-ir.org/event/f-event.htm)",
                        page = "Wayback Machine snapshot of 2012-10-05 (original site unreachable 2026-09-13); copy in docs/sources/unic-tehran-f-event-20121005.html",
                    ),
                ),
            links =
                mapOf(
                    "resolution" to "http://undocs.org/en/A/RES/67/200",
                    "un" to "https://www.un.org/en/observances/right-to-truth-day",
                ),
        ),
    )
