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
                        "prs" to "روز عرفه",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 9),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                            title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                            page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
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
                        "prs" to "عید سعید اضحی",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 10),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                            title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                            page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
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
                        "prs" to "عید سعید اضحی",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 11),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                            title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                            page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
                    Citation(
                        url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B9%D9%84%D8%A7%D9%85-%DA%86%D9%87%D8%A7%D8%B1-%D8%B1%D9%88%D8%B2-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%D8%B9%D9%85%D9%88%D9%85%DB%8C-%D8%A8%D9%87-%D9%85%D9%86%D8%A7%D8%B3%D8%A8%D8%AA-%D8%B1%D9%88%D8%B2-%D8%B9%D8%B1%D9%81%D9%87-%D9%88-%D8%B9%DB%8C%D8%AF-%D9%82%D8%B1%D8%A8%D8%A7%D9%86-%D8%AF%D8%B1%D8%A7%D9%81%D8%BA%D8%A7%D9%86%D8%B3%D8%AA%D8%A7%D9%86",
                        title = "Bakhtar News Agency — «اعلام چهار روز رخصتی عمومی به مناسبت روز عرفه و عید قربان درافغانستان» (Ministry of Labour and Social Affairs announcement), published 2026-05-21",
                        page = "summary and article text: Arafa = Tuesday 9 Dhu al-Hijjah 1447 AH; four working days of public holiday; offices resume Sunday 14 Dhu al-Hijjah 1447 AH",
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
                        "prs" to "عید سعید اضحی",
                    ),
                ),
            rule = EventRule.Fixed(month = 12, day = 12),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(7): «Four days of Eid-e-Said-e-Adhah and Arafat (Three days of Eid and one day of Arafat)» — Arafa 9 plus Eid 10, 11 and 12 Dhu al-Hijjah. The shipped Bakhtar announcement of 2026-05-21 likewise gives four days with offices resuming 14 Dhu al-Hijjah 1447; the dataset held only 10 and 11.",
                        ),
                ),
            citations =
                listOf(
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
                        "prs" to "عید سعید فطر",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 1),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                        ),
                ),
            citations =
                listOf(
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
                        "prs" to "عید سعید فطر",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 2),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                        ),
                ),
            citations =
                listOf(
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
                        "prs" to "عید سعید فطر",
                    ),
                ),
            rule = EventRule.Fixed(month = 10, day = 3),
            validity =
                Validity(
                    calendar = CalendarSystem.ISLAMIC,
                    fromYear = 1_447,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://natlex.ilo.org/dyn/natlex2/natlex2/files/download/78309/AFG78309.pdf",
                            title = "ILO NATLEX record 78309 — Afghanistan, Labour Law (No. 35 of 2007), Official Gazette No. 966; English translation published by the International Labour Organization. SHA-256 28cb8ba93a578d1ac4c52e97de11b722a6034957f488c87f2efcee59360ead22",
                            page = "Article 41(6): «The three days of Eid-e-Feter» — 1, 2 and 3 Shawwal. Article 41(6) fixes the span at three days; that Eid al-Fitr itself is a public holiday under the Islamic Emirate is recorded in DT-031 from the Bakhtar announcement of 2026-03-21, which names the holiday without giving its dates.",
                        ),
                ),
            citations =
                listOf(
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
                    fromYear = 1_405,
                    toYear = null,
                    citation =
                        Citation(
                            url = "https://www.bakhtarnews.af/dr/%D8%A7%D8%B7%D9%84%D8%A7%D8%B9%DB%8C%D9%87-%D8%B1%D8%AE%D8%B5%D8%AA%DB%8C-%DB%B2%DB%B8-%D8%A7%D8%B3%D8%AF-%D8%B1%D9%88%D8%B2-%D8%A7%D8%B3%D8%AA%D8%B1%D8%AF%D8%A7%D8%AF-%D8%A7%D8%B3%D8%AA%D9%82%D9%84%D8%A7%D9%84-%DA%A9%D8%B4%D9%88%D8%B1",
                            title = "Bakhtar News Agency — «اطلاعیه رخصتی ۲۸ اسد، روز استرداد استقلال کشور» (holiday announcement; issuing authority not named in the article), published 2026-08-17",
                            page = "article text: 5 Rabi al-Awwal 1448 AH = 28 Asad 1405 SH (Wednesday), public holiday — Owner decision 2026-09-17 (\"computed, not typed\", ADR-0036): the announced day recurs every year on this calendar date, so the record is a Fixed rule valid from the announced year; the citation is the announcement that established it.",
                        ),
                ),
            citations =
                listOf(
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
                ),
        ),
    )
