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
 * Part 1 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_1: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("af.holiday.arafa"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "روز عرفه",
                        "en" to "Day of Arafah",
                        "prs" to "روز عرفه",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(7): «Four days of Eid-e-Said-e-Adhah and Arafat (Three days of Eid and one day of Arafat)» — Arafa 9 Dhu al-Hijjah. The law took force 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309), so 1428 is the first Islamic year it covers.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(7): «Four days of Eid-e-Said-e-Adhah and Arafat (Three days of Eid and one day of Arafat)» — Arafa 9 Dhu al-Hijjah. The law took force 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309), so 1428 is the first Islamic year it covers.",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of Article 41; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.eid-al-adha.1"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید سعید اضحی",
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
                        "prs" to "عید سعید اضحی",
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
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(7): the first day of Eid al-Adha, 10 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(7): the first day of Eid al-Adha, 10 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of Article 41; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.eid-al-adha.2"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید سعید اضحی",
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
                        "prs" to "عید سعید اضحی",
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
            rule = EventRule.Fixed(month = 12, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(7): the second day of Eid al-Adha, 11 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(7): the second day of Eid al-Adha, 11 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of Article 41; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.eid-al-adha.3"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.RELIGIOUS,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "عید سعید اضحی",
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
                        "prs" to "عید سعید اضحی",
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
            rule = EventRule.Fixed(month = 12, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(7): the third day of Eid al-Adha, 12 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(7): the third day of Eid al-Adha, 12 Dhu al-Hijjah. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(7): «Four days of Eid-e-Said-e-Adhah and Arafat (Three days of Eid and one day of Arafat)» — Arafa 9 plus Eid 10, 11 and 12 Dhu al-Hijjah. The shipped Bakhtar announcement of 2026-05-21 likewise gives four days with offices resuming 14 Dhu al-Hijjah 1447; the dataset held only 10 and 11.",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette No. 966 text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of the article translated above; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
    )
