/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import org.junit.jupiter.api.Test

/**
 * R05 (review 2026-09-19): Isha never comes before Maghrib. At Tromsø the Sun reaches the Tehran method's 4.5° Maghrib
 * depression long after sunset but never the 14° Isha depression, so the high-latitude rule estimates Isha from a
 * portion of the night after sunset — which could fall before the observed Maghrib.
 */
class MaghribBeforeIshaTest {
    private fun day(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, dayOfMonth))

    @Test
    fun `at Tromso on 19 April 2001 Isha does not come before Maghrib`() {
        // Found by the completeness test (REVIEW R14): maghrib 22:06, Isha 22:04 under the default Tehran method and the
        // nearest-latitude rule.
        val settings = PrayerSettings(highLatitude = HighLatitudeRule.NEAREST_LATITUDE)

        val result =
            PrayerTimesCalculator.exact(day(2001, 4, 19), TROMSO, UTC_PLUS_2, settings, settings.method.parameters())

        val t = result.shouldBeInstanceOf<ExactResult.Times>().times
        t.isha.shouldNotBeNull() shouldBeGreaterThanOrEqual t.maghrib.shouldNotBeNull()
    }

    @Test
    fun `under every high-latitude rule and every method Isha follows Maghrib at Tromso through 2001-2101`() {
        val first = day(2001, 1, 1).value
        val last = day(2101, 12, 31).value
        val defects =
            HighLatitudeRule.entries.flatMap { rule ->
                PrayerMethod.entries.flatMap { method ->
                    val settings = PrayerSettings(method, highLatitude = rule)
                    val parameters = settings.method.parameters()
                    (first..last step SAMPLE_STEP_DAYS).mapNotNull { jdn ->
                        val result = PrayerTimesCalculator.exact(Jdn(jdn), TROMSO, UTC_PLUS_1, settings, parameters)
                        val t = (result as? ExactResult.Times)?.times ?: return@mapNotNull null
                        val maghrib = t.maghrib ?: return@mapNotNull null
                        val isha = t.isha ?: return@mapNotNull null
                        "$rule $method JDN $jdn: maghrib $maghrib, isha $isha".takeIf { isha < maghrib }
                    }
                }
            }

        defects.shouldBeEmpty()
    }

    private companion object {
        const val UTC_PLUS_1 = 60
        const val UTC_PLUS_2 = 120

        /** Every third day keeps the sweep at about 12 000 days per rule and method while crossing every season. */
        const val SAMPLE_STEP_DAYS = 3L
        val TROMSO = Coordinates(69.6492, 18.9553)
    }
}
