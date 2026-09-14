/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.feature.notification.TodayFixtures.MAGHRIB
import ir.taqvim.feature.notification.TodayFixtures.MIDNIGHT
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1213 (U): the persistent notification's texts in each state, and a key that changes exactly with the content. */
class PersistentNotificationContentTest {
    private val format: (SummaryPrayer) -> String = { "${it.name} at ${it.time}" }

    @Test
    fun `with a place the next prayer is the text and the prayer strip closes the expanded lines`() {
        val content = PersistentNotificationContents.of(TodayFixtures.summary(), largeNumber = true, format)

        content shouldBe
            PersistentNotificationContent(
                title = "22 Shahrivar 1405",
                subText = "Sunday",
                text = "Maghrib at 19:10",
                lines =
                    listOf(
                        "13 September 2026",
                        "1 Rabi al-Awwal 1448",
                        "Holiday",
                        "Fajr 04:52 · Dhuhr 13:07 · Maghrib 19:10",
                    ),
                iconText = "22",
                largeNumber = true,
            )
        TodayFixtures.summary().staleAt shouldBe MAGHRIB
    }

    @Test
    fun `without a place the first other date is the text and there is no prayer strip`() {
        val summary = TodayFixtures.summary(withPlace = false, holidays = emptyList())

        val content = PersistentNotificationContents.of(summary, largeNumber = false, format)

        content.text shouldBe "13 September 2026"
        content.lines shouldBe listOf("13 September 2026", "1 Rabi al-Awwal 1448")
        summary.nextPrayer shouldBe null
        summary.staleAt shouldBe MIDNIGHT
    }

    @Test
    fun `with a single calendar and no place the weekday is the text`() {
        val summary = TodayFixtures.summary(withPlace = false, otherDates = emptyList(), holidays = emptyList())

        val content = PersistentNotificationContents.of(summary, largeNumber = false, format)

        content.text shouldBe "Sunday"
        content.lines shouldBe emptyList()
    }

    @Test
    fun `a prayer after the day ends does not delay the day change`() {
        val summary = TodayFixtures.summary().copy(nextPrayerAt = MIDNIGHT + DailyRefreshCoordinator.RETRY)

        summary.staleAt shouldBe MIDNIGHT
    }

    @Test
    fun `property - the key is equal exactly when the content is equal`(): Unit =
        runTest {
            val text = Arb.string(0..6, Codepoint.alphanumeric())
            val lines = Arb.list(text, 0..3)
            checkAll(PropertyTesting.iterations, text, text, lines, lines, Arb.boolean(), Arb.boolean()) {
                title,
                other,
                first,
                second,
                large,
                otherLarge,
                ->
                val a = PersistentNotificationContent(title, "Sunday", other, first, "22", large)
                val b = PersistentNotificationContent(other, "Sunday", title, second, "22", otherLarge)

                if (a == b) a.key shouldBe b.key else a.key shouldNotBe b.key
            }
        }
}
