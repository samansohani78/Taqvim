/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.annotation.StringRes

/**
 * How the data of a [DataSource] may be used. [spdxId] is the identifier of the `dataLicenses` section of
 * `config/license/allowed-licenses.json` (ADR-0039), and [deedUrl] links the license text when it lives on the web
 * instead of in the bundled assets.
 */
enum class DataLicense(
    @param:StringRes val label: Int,
    val spdxId: String,
    val deedUrl: String? = null,
) {
    UNICODE_3_0(R.string.about_data_license_unicode, "Unicode-3.0"),
    PUBLIC_DOMAIN(R.string.about_data_license_public_domain, "LicenseRef-Public-Domain"),
    CITED_PUBLICATION(R.string.about_data_license_cited, "LicenseRef-Cited-Publication"),
    CC_BY_4_0(R.string.about_data_license_cc_by, "CC-BY-4.0", "https://creativecommons.org/licenses/by/4.0/"),
}

/**
 * Data shipped with or used to validate Taqvim, as recorded in docs/PROVENANCE.md, with its public link.
 * [attribution] is the credit line a license such as CC BY 4.0 obliges us to show (ADR-0039): creators, year,
 * license name and URI, disclaimer and what we changed. It is required for every license whose allow-list entry
 * says `"attribution": "required"` and is checked by `DataSourceLicenseTest`.
 */
enum class DataSource(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val url: String,
    val license: DataLicense,
    @param:StringRes val attribution: Int? = null,
) {
    CLDR(
        R.string.about_source_cldr,
        R.string.about_source_cldr_description,
        "https://cldr.unicode.org/",
        DataLicense.UNICODE_3_0,
        R.string.about_source_cldr_attribution,
    ),
    CALENDAR_CENTER(
        R.string.about_source_calendar_center,
        R.string.about_source_calendar_center_description,
        "https://calendar.ut.ac.ir/Fa/",
        DataLicense.CITED_PUBLICATION,
    ),
    UNITED_NATIONS(
        R.string.about_source_un,
        R.string.about_source_un_description,
        "https://www.un.org/en/observances/list-days-weeks",
        DataLicense.CITED_PUBLICATION,
    ),
    UNIC_TEHRAN(
        R.string.about_source_unic,
        R.string.about_source_unic_description,
        "https://web.archive.org/web/20121005003400/https://www.unic-ir.org/event/f-event.htm",
        DataLicense.CITED_PUBLICATION,
    ),
    IRANICA(
        R.string.about_source_iranica,
        R.string.about_source_iranica_description,
        "https://iranicaonline.org/",
        DataLicense.CITED_PUBLICATION,
    ),
    NATURAL_EARTH(
        R.string.about_source_natural_earth,
        R.string.about_source_natural_earth_description,
        "https://www.naturalearthdata.com/",
        DataLicense.PUBLIC_DOMAIN,
    ),
    MATTHEWS_PLATES(
        R.string.about_source_matthews,
        R.string.about_source_matthews_description,
        "https://zenodo.org/records/10526157",
        DataLicense.CC_BY_4_0,
        R.string.about_source_matthews_attribution,
    ),
    NOAA(
        R.string.about_source_noaa,
        R.string.about_source_noaa_description,
        "https://gml.noaa.gov/grad/solcalc/NOAA_Solar_Calculations_day.ods",
        DataLicense.PUBLIC_DOMAIN,
    ),
    ;

    companion object {
        /** The Unicode License v3 of CLDR data, bundled verbatim. */
        val UNICODE_LICENSE: LicenseInfo =
            LicenseInfo(
                id = "Unicode-3.0",
                name = "Unicode License v3",
                url = "https://www.unicode.org/license.txt",
                textAsset = "licenses/texts/Unicode-3.0.txt",
            )
    }
}
