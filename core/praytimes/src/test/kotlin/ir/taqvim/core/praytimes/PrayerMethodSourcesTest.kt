/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.PrayerMethod
import org.junit.jupiter.api.Test

/**
 * T-601: every shipped method's parameters, pinned to the authority that defines them.
 *
 * A wrong angle here is invisible: it produces plausible times all year for everyone who selects that method, and no
 * other test can catch it, because the rest of the suite checks the calculator against these same constants. This
 * test is therefore the one place where the numbers are compared with their sources rather than with themselves — the
 * citation for each lives beside it here and in docs/PROVENANCE.md A-10.
 *
 * Two of them are checked against the authority's own published output rather than a statement of parameters:
 * - `SINGAPORE`: MUIS publishes a timetable, not angles. Its official 2025 timetable for Singapore (1.3521° N,
 *   103.8198° E, UTC+8) gives, over all 365 days, a Subuh depression with median 19.88° (19.67‥20.10) and an Isyak
 *   depression with median 18.13° (17.90‥18.37) — the spread being its rounding to the whole minute. So 20° and 18°.
 * - `TEHRAN`: validated against the Institute of Geophysics' own 1405 timetables for 31 cities (11 315 days), same
 *   minute for 99.8–100 % of every prayer (A-10 validation).
 */
class PrayerMethodSourcesTest {
    @Test
    fun `every method keeps the parameters its authority publishes`() {
        // Muslim World League.
        expect(PrayerMethod.MWL, fajr = 18.0, isha = IshaRule.Angle(17.0))

        // Fiqh Council of North America, General Body Meeting, Dallas, 27–29 October 2017: "15° for both Fajr and
        // Isha in the USA". (Its 13° for Canada is a separate regional value this app does not ship.)
        expect(PrayerMethod.ISNA, fajr = 15.0, isha = IshaRule.Angle(15.0))

        // Egyptian General Authority of Survey.
        expect(PrayerMethod.EGYPT, fajr = 19.5, isha = IshaRule.Angle(17.5))

        // Umm al-Qura, Makkah: Isha is an interval after Maghrib, not an angle. See the Ramadan note in
        // docs/PROVENANCE.md A-10 — the app does not lengthen it to 120 minutes in Ramadan.
        expect(PrayerMethod.MAKKAH, fajr = 18.5, isha = IshaRule.MinutesAfterMaghrib(90))

        // University of Islamic Sciences, Karachi.
        expect(PrayerMethod.KARACHI, fajr = 18.0, isha = IshaRule.Angle(18.0))

        // Institute of Geophysics, University of Tehran: Maghrib is a twilight, not sunset.
        expect(PrayerMethod.TEHRAN, fajr = 17.7, isha = IshaRule.Angle(14.0), maghrib = MaghribRule.Angle(4.5))

        // Shia Ithna-Ashari (Jafari): likewise a twilight Maghrib.
        expect(PrayerMethod.JAFARI, fajr = 16.0, isha = IshaRule.Angle(14.0), maghrib = MaghribRule.Angle(4.0))

        // Majlis Ugama Islam Singapura, derived from its official timetable (see the class comment).
        expect(PrayerMethod.SINGAPORE, fajr = 20.0, isha = IshaRule.Angle(18.0))

        // Union des Organisations Islamiques de France.
        expect(PrayerMethod.FRANCE, fajr = 12.0, isha = IshaRule.Angle(12.0))

        // Spiritual Administration of Muslims of Russia.
        expect(PrayerMethod.RUSSIA, fajr = 16.0, isha = IshaRule.Angle(15.0))

        // Diyanet İşleri Başkanlığı: Fajr 18° and Isha 17° (press statement, 17 July 2013), with the temkin of
        // https://vakithesaplama.diyanet.gov.tr/temkin.php — re-read 2026-09-25, which states 7 minutes at sunrise
        // and sunset, 4 at Asr, and Dhuhr 5 minutes after the Sun is overhead.
        expect(PrayerMethod.DIYANET, fajr = 18.0, isha = IshaRule.Angle(17.0))
        PrayerMethod.DIYANET.parameters().adjustments shouldBe
            TimeAdjustments(sunrise = -7, dhuhr = 5, asr = 4, maghrib = 7)
    }

    @Test
    fun `only the Shia methods move Maghrib off sunset, and only they measure midnight to Fajr`() {
        // The two conventions that a previous defect turned on (REVIEW R05): a twilight Maghrib belongs to the Tehran
        // and Jafari methods alone, and every Sunni method here keeps Maghrib at sunset.
        val twilightMaghrib = PrayerMethod.entries.filter { it.parameters().maghrib is MaghribRule.Angle }
        twilightMaghrib shouldBe listOf(PrayerMethod.TEHRAN, PrayerMethod.JAFARI)

        val toFajr = PrayerMethod.entries.filter { it.parameters().midnight == MidnightMode.SUNSET_TO_FAJR }
        toFajr shouldBe listOf(PrayerMethod.TEHRAN, PrayerMethod.JAFARI)
    }

    private fun expect(
        method: PrayerMethod,
        fajr: Double,
        isha: IshaRule,
        maghrib: MaghribRule = MaghribRule.AtSunset,
    ) {
        val parameters = method.parameters()
        parameters.fajrAngle shouldBe fajr
        parameters.isha shouldBe isha
        parameters.maghrib shouldBe maghrib
    }
}
