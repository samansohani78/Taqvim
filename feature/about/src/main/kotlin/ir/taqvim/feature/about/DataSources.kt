/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.annotation.StringRes

/** How the data of a [DataSource] may be used. */
enum class DataLicense(
    @param:StringRes val label: Int,
) {
    UNICODE_3_0(R.string.about_data_license_unicode),
    PUBLIC_DOMAIN(R.string.about_data_license_public_domain),
    CITED_PUBLICATION(R.string.about_data_license_cited),
}

/** Data shipped with or used to validate Taqvim, as recorded in docs/PROVENANCE.md, with its public link. */
enum class DataSource(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val url: String,
    val license: DataLicense,
) {
    CLDR(
        R.string.about_source_cldr,
        R.string.about_source_cldr_description,
        "https://cldr.unicode.org/",
        DataLicense.UNICODE_3_0,
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
