/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** F03: a subscription's health state and the details rendered for it. */
class SubscriptionHealthTest {
    private val now = Instant.parse("2026-09-16T12:00:00Z")
    private val nowMillis = now.toEpochMilliseconds()
    private val hourly = SubscriptionHealthData(refreshIntervalMinutes = 60)

    private fun item(
        enabled: Boolean = true,
        fetched: Instant? = now - 1.hours,
        health: SubscriptionHealthData =
            hourly.copy(
                lastCheckedAtEpochMillis = (now - 30.minutes).toEpochMilliseconds(),
            ),
    ) = SubscriptionItem(7, "Feed", "https://example.org/a.ics", enabled, fetched?.toEpochMilliseconds(), health)

    @Test
    fun `every health state is recognised, most urgent first`() {
        SubscriptionHealthRules.of(item(), nowMillis) shouldBe SubscriptionHealth.OK
        SubscriptionHealthRules.of(item(fetched = null, health = hourly), nowMillis) shouldBe
            SubscriptionHealth.NEVER_FETCHED
        val oldCheck = hourly.copy(lastCheckedAtEpochMillis = (now - 3.hours).toEpochMilliseconds())
        SubscriptionHealthRules.of(item(health = oldCheck), nowMillis) shouldBe SubscriptionHealth.STALE
        val twoPeriods = hourly.copy(lastCheckedAtEpochMillis = (now - 2.hours).toEpochMilliseconds())
        SubscriptionHealthRules.of(item(health = twoPeriods), nowMillis) shouldBe SubscriptionHealth.OK
        val failed = oldCheck.copy(error = SubscriptionError.TIMEOUT)
        SubscriptionHealthRules.of(item(health = failed), nowMillis) shouldBe SubscriptionHealth.FAILED
        SubscriptionHealthRules.of(item(fetched = null, health = failed), nowMillis) shouldBe
            SubscriptionHealth.FAILED
        SubscriptionHealthRules.of(item(enabled = false, health = failed), nowMillis) shouldBe
            SubscriptionHealth.PAUSED
    }

    @Test
    fun `a feed without a known period or check time is judged by its download`() {
        val noPeriod = SubscriptionHealthData(lastCheckedAtEpochMillis = 0)
        SubscriptionHealthRules.of(item(health = noPeriod), nowMillis) shouldBe SubscriptionHealth.OK
        SubscriptionHealthRules.of(item(fetched = now - 5.hours, health = hourly), nowMillis) shouldBe
            SubscriptionHealth.STALE
    }

    @Test
    fun `details show times, cache, problems and the failure in the user's language`() {
        val health =
            hourly.copy(
                lastCheckedAtEpochMillis = (now - 30.minutes).toEpochMilliseconds(),
                nextCheckAtEpochMillis = (now + 30.minutes).toEpochMilliseconds(),
                cachedEvents = 12,
                cachedFromEpochMillis = Instant.parse("2026-08-16T00:00:00Z").toEpochMilliseconds(),
                cachedUntilEpochMillis = Instant.parse("2027-10-20T00:00:00Z").toEpochMilliseconds(),
                problemCount = 3,
                error = SubscriptionError.SERVER,
                httpStatus = 503,
                errorAtEpochMillis = (now - 5.minutes).toEpochMilliseconds(),
            )
        val english =
            SubscriptionHealthRules.details(item(health = health), LocationFixtures.english, TimeZone.UTC, nowMillis)

        english.lastSuccess?.time shouldBe "11:00"
        english.lastCheck?.time shouldBe "11:30"
        english.nextCheck?.time shouldBe "12:30"
        english.errorAt?.time shouldBe "11:55"
        english.lastSuccess?.date.orEmpty() shouldContain "2026"
        english.cachedFrom.orEmpty() shouldContain "2026"
        english.cachedUntil.orEmpty() shouldContain "2027"
        english.eventCount shouldBe "12"
        english.problems shouldBe "3"
        english.error shouldBe SubscriptionError.SERVER
        english.errorStatus shouldBe "503"

        val persian =
            SubscriptionHealthRules.details(
                item(health = health),
                LocationFixtures.persian,
                TimeZone.of("Asia/Tehran"),
                nowMillis,
            )
        persian.lastCheck?.time shouldBe "۱۵:۰۰"
        persian.eventCount shouldBe "۱۲"
        persian.errorStatus shouldBe "۵۰۳"
        persian.cachedFrom.orEmpty() shouldContain "۱۴۰۵"
    }

    @Test
    fun `nothing is shown for what never happened, a due feed or a paused one`() {
        val never =
            SubscriptionHealthRules.details(
                item(fetched = null, health = hourly),
                LocationFixtures.english,
                TimeZone.UTC,
                nowMillis,
            )
        never shouldBe SubscriptionDetails(eventCount = "0")

        val due = hourly.copy(nextCheckAtEpochMillis = (now - 1.minutes).toEpochMilliseconds())
        SubscriptionHealthRules
            .details(item(health = due), LocationFixtures.english, TimeZone.UTC, nowMillis)
            .nextCheck shouldBe null
        val later = hourly.copy(nextCheckAtEpochMillis = (now + 1.hours).toEpochMilliseconds())
        SubscriptionHealthRules
            .details(item(enabled = false, health = later), LocationFixtures.english, TimeZone.UTC, nowMillis)
            .nextCheck shouldBe null
    }
}
