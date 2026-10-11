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
 * Part 2 of the dataset events.
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
internal val OFFICIAL_EVENTS_PART_2: List<EventDefinition> =
    listOf(
        EventDefinition(
            id = EventId("af.holiday.eid-al-fitr.1"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
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
                        "prs" to "عید سعید فطر",
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
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 1 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 1 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette No. 966 text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of the article translated above; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.eid-al-fitr.2"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
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
                        "prs" to "عید سعید فطر",
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
            rule = EventRule.Fixed(month = 10, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 2 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 2 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette No. 966 text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of the article translated above; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.eid-al-fitr.3"),
            calendar = CalendarSystem.ISLAMIC,
            source = EventSource.AFGHANISTAN_OFFICIAL,
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
                        "prs" to "عید سعید فطر",
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
            rule = EventRule.Fixed(month = 10, day = 3),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_428,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 3 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 3 Shawwal. In force from 2007-02-04 = Muharram 1428 AH (Article 153; date from ILO NATLEX record 78309).",
                    ),
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette No. 966 text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of the article translated above; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
        EventDefinition(
            id = EventId("af.holiday.independence"),
            calendar = CalendarSystem.PERSIAN,
            source = EventSource.AFGHANISTAN_OFFICIAL,
            category = EventCategory.NATIONAL,
            isHoliday = true,
            title =
                LocalizedText(
                    mapOf(
                        "fa" to "سالروز استرداد استقلال کشور",
                        "en" to "Independence anniversary",
                        "prs" to "سالروز استرداد استقلال کشور",
                    ),
                ),
            rule = EventRule.Fixed(month = 5, day = 28),
            validity =
                Validity(
                    calendar = CalendarSystem.PERSIAN,
                    fromYear = 1_386,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(3): «Twenty-eight of Assad … the day of restoration of independence of the country». The law took force 2007-02-04 = 15 Dalw 1385 SH (Article 153; date from ILO NATLEX record 78309), which is after 28 Asad 1385 (2006-08-19), so the first occurrence it covers is 28 Asad 1386 (2007-08-19).",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                        title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                        page = "Article 41(3): «Twenty-eight of Assad … the day of restoration of independence of the country». The law took force 2007-02-04 = 15 Dalw 1385 SH (Article 153; date from ILO NATLEX record 78309), which is after 28 Asad 1385 (2006-08-19), so the first occurrence it covers is 28 Asad 1386 (2007-08-19).",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%DB%8C%D9%87-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%DB%B2%DB%B8-%D8%A7%D8%B3%D8%AF-%D8%B1%D9%88%D8%B2-%D8%A7%D8%B3%D8%AA%D8%B1%D8%AF%D8%A7%D8%AF-%D8%A7%D8%B3%D8%AA%D9%82%D9%84%D8%A7%D9%84-%DA%A9%D8%B4%D9%88%D8%B1",
                        title = "Bakhtar News Agency — «اطلاعیه رخصتی ۲۸ اسد، روز استرداد استقلال کشور» (holiday announcement; issuing authority not named in the article), published 2026-08-17",
                        page = "article text: 5 Rabi al-Awwal 1448 AH = 28 Asad 1405 SH (Wednesday), public holiday",
                    ),
                    Citation(
                        url = "https://www.bakhtarnews.af/en/Afghanistan-Declares-August-19-Public-Holiday-to-Mark-Independence-Anniversary",
                        title = "Bakhtar News Agency (English) — «Afghanistan Declares August 19 Public Holiday to Mark Independence Anniversary» (official announcement), published 2026-08-17",
                        page = "article text: Asad 28 = 5 Rabi al-Awwal 1448 AH = August 19, 2026, government offices closed",
                    ),
                    Citation(
                        url = "https://molsa.gov.af/sites/default/files/2019-06/%D9%82%D8%A7%D9%86%D9%88%D9%86%20%DA%A9%D8%A7%D8%B1%20966.pdf",
                        title = "Ministry of Labour and Social Affairs of Afghanistan (molsa.gov.af) — «قانون کار ۹۶۶», the Dari Official Gazette text of the Labour Law, still published by the ministry under the Islamic Emirate. SHA-256 9ec90caa871c6ad537f7e0073bede5b2e1e6f0e84afe0bb59eb2022ec1b8686c",
                        page = "the Dari original of Article 41; its text layer is a custom encoding that does not extract, so the article is quoted from the ILO translation",
                    ),
                ),
        ),
    )
