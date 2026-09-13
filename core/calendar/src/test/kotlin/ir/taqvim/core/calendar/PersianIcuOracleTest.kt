/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.PersianCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import kotlin.random.Random
import org.junit.jupiter.api.Test

/** A-02 against ICU4J's arithmetic Persian calendar (Unicode License, test scope only) for 1200‥1500. */
@Suppress("DEPRECATION")
class PersianIcuOracleTest {
    private val persian = PersianCalendarSystem

    /** Years where ICU4J's arithmetic rule is expected to differ from A-02. None do in 1200‥1500. */
    private val documentedExceptions = emptyList<Int>()

    private fun icu() = PersianCalendar(TimeZone.GMT_ZONE, ULocale.ROOT)

    private fun PersianCalendar.firstDayJdn(year: Int): Long {
        clear()
        set(year, 0, 1)
        return Math.floorDiv(timeInMillis, MILLIS_PER_DAY) + JDN_OF_UNIX_EPOCH_DAY
    }

    private fun PersianCalendar.agreesWith(date: CalendarDate): Boolean =
        get(Calendar.EXTENDED_YEAR) == date.year &&
            get(Calendar.MONTH) + 1 == date.month &&
            get(Calendar.DAY_OF_MONTH) == date.day

    @Test
    fun `first days of years 1200 to 1500 agree except documented years`() {
        val icu = icu()

        (FIRST_YEAR..LAST_YEAR).filter { icu.firstDayJdn(it) != persian.firstDayOfYear(it).value } shouldBe
            documentedExceptions
    }

    @Test
    fun `100 000 random days in 1200 to 1500 agree outside documented years`() {
        val icu = icu()
        val random = Random(SEED)
        val first = persian.firstDayOfYear(FIRST_YEAR).value
        val end = persian.firstDayOfYear(LAST_YEAR + 1).value

        (1..SAMPLES)
            .map { random.nextLong(first, end) }
            .filterNot { jdn ->
                val date = persian.fromJdn(Jdn(jdn))
                icu.timeInMillis = (jdn - JDN_OF_UNIX_EPOCH_DAY) * MILLIS_PER_DAY
                date.year in documentedExceptions || icu.agreesWith(date)
            }.shouldBeEmpty()
    }

    private companion object {
        const val FIRST_YEAR = 1200
        const val LAST_YEAR = 1500
        const val SAMPLES = 100_000
        const val SEED = 1300
        const val MILLIS_PER_DAY = 86_400_000L
        const val JDN_OF_UNIX_EPOCH_DAY = 2_440_588L
    }
}
