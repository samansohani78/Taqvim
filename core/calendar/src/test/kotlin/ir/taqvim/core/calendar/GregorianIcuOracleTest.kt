/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.GregorianCalendar
import com.ibm.icu.util.TimeZone
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import java.util.Date
import kotlin.random.Random
import org.junit.jupiter.api.Test

/**
 * Golden test for A-01 against the independent oracle required by docs/PLAN.md §6: ICU4J's Gregorian calendar
 * switched to proleptic mode (Unicode License, test scope only). One million random JDNs in [−1 000 000, 5 000 000].
 */
class GregorianIcuOracleTest {
    private fun icuProleptic(): GregorianCalendar =
        GregorianCalendar(TimeZone.GMT_ZONE).apply {
            gregorianChange = Date(Long.MIN_VALUE)
            isLenient = false
        }

    @Test
    fun `agrees with ICU4J proleptic Gregorian on one million random days`() {
        val icu = icuProleptic()
        val random = Random(SEED)
        var mismatches = 0
        var firstMismatch = ""
        repeat(SAMPLES) {
            val jdn = random.nextLong(MIN_JDN, MAX_JDN + 1)
            icu.clear()
            icu.set(Calendar.JULIAN_DAY, jdn.toInt())
            val expected =
                CalendarDate(
                    CalendarSystem.GREGORIAN,
                    icu.get(Calendar.EXTENDED_YEAR),
                    icu.get(Calendar.MONTH) + 1,
                    icu.get(Calendar.DAY_OF_MONTH),
                )
            val actual = GregorianCalendarSystem.fromJdn(Jdn(jdn))
            val back = GregorianCalendarSystem.toJdn(expected)
            if (actual != expected || back != Jdn(jdn)) {
                if (mismatches == 0) firstMismatch = "JDN $jdn: ICU=$expected ours=$actual back=$back"
                mismatches++
            }
        }

        firstMismatch shouldBe ""
        mismatches shouldBe 0
    }

    private companion object {
        const val SAMPLES = 1_000_000
        const val MIN_JDN = -1_000_000L
        const val MAX_JDN = 5_000_000L
        const val SEED = 20_260_913L
    }
}
