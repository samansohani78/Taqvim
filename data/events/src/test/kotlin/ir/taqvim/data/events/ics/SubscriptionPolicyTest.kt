/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.devicecalendar.InstantWindow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-1003 (U): subscription URLs and the refresh policy. */
class SubscriptionPolicyTest {
    private val policy = SubscriptionRefreshPolicy()
    private val now = Instant.parse("2026-09-13T12:00:00Z")

    private fun subscription(
        intervalMinutes: Int = 60,
        enabled: Boolean = true,
        checkedAgo: Duration? = null,
        fetchedAgo: Duration? = null,
    ) = IcsSubscriptionEntity(
        id = 1,
        url = "https://example.org/cal.ics",
        displayName = "Feed",
        enabled = enabled,
        refreshIntervalMinutes = intervalMinutes,
        lastCheckedAtEpochMillis = checkedAgo?.let { (now - it).toEpochMilliseconds() },
        lastFetchedAtEpochMillis = fetchedAgo?.let { (now - it).toEpochMilliseconds() },
        etag = "\"v1\"",
        lastModified = "Sun, 13 Sep 2026 10:00:00 GMT",
    )

    @Test
    fun `webcal URLs are read as HTTPS and insecure or malformed URLs are refused`() {
        mapOf(
            "webcal://example.org/cal.ics" to SubscriptionUrl.Valid("https://example.org/cal.ics"),
            " WEBCALS://Example.org/a?b=1 " to SubscriptionUrl.Valid("https://Example.org/a?b=1"),
            "https://example.org/x" to SubscriptionUrl.Valid("https://example.org/x"),
            "http://example.org/cal.ics" to SubscriptionUrl.Insecure,
            "ftp://example.org/cal.ics" to SubscriptionUrl.Invalid,
            "example.org/cal.ics" to SubscriptionUrl.Invalid,
            "https:///no-host" to SubscriptionUrl.Invalid,
            "not a url" to SubscriptionUrl.Invalid,
        ).forEach { (text, expected) -> SubscriptionUrls.normalize(text) shouldBe expected }
    }

    @Test
    fun `a subscription is due one interval after its last check and never more often than 15 minutes`() {
        policy.isDue(subscription(), now) shouldBe true
        policy.isDue(subscription(checkedAgo = 59.minutes), now) shouldBe false
        policy.isDue(subscription(checkedAgo = 60.minutes), now) shouldBe true
        policy.isDue(subscription(enabled = false), now) shouldBe false
        policy.isDue(subscription(intervalMinutes = 5, checkedAgo = 10.minutes), now) shouldBe false
        policy.isDue(subscription(intervalMinutes = 5, checkedAgo = 15.minutes), now) shouldBe true
    }

    @Test
    fun `validators are sent only while the last full download is recent`() {
        policy.validatorsFor(subscription(fetchedAgo = 6.days), now) shouldBe
            HttpValidators("\"v1\"", "Sun, 13 Sep 2026 10:00:00 GMT")
        policy.validatorsFor(subscription(fetchedAgo = 7.days), now) shouldBe HttpValidators()
        policy.validatorsFor(subscription(), now) shouldBe HttpValidators()
        policy.window(now) shouldBe
            InstantWindow((now - 31.days).toEpochMilliseconds(), (now + 400.days).toEpochMilliseconds())
    }

    @Test
    fun `the work period is the shortest enabled interval`() {
        policy.workPeriod(listOf(subscription(60), subscription(5), subscription(1, enabled = false))) shouldBe
            15.minutes
        policy.workPeriod(listOf(subscription(120), subscription(90))) shouldBe 90.minutes
        policy.workPeriod(listOf(subscription(enabled = false))).shouldBeNull()
        policy.workPeriod(emptyList()).shouldBeNull()
    }

    @Test
    fun `only transient failures are retried`() {
        fun failed(error: FetchError) = RefreshOutcome.Failed(1, RefreshError.Fetch(error))

        listOf(FetchError.Timeout, FetchError.Network, FetchError.HttpStatus(503), FetchError.HttpStatus(429))
            .forEach { policy.shouldRetry(listOf(RefreshOutcome.Unchanged(2), failed(it))) shouldBe true }
        listOf(FetchError.HttpStatus(404), FetchError.TooLarge, FetchError.InsecureRedirect, FetchError.InvalidUrl)
            .forEach { policy.shouldRetry(listOf(failed(it))) shouldBe false }
        val permanent = listOf(RefreshOutcome.Failed(1, RefreshError.Unreadable), RefreshOutcome.Updated(2, 3, 0))
        policy.shouldRetry(permanent) shouldBe false
        policy.shouldRetry(emptyList()) shouldBe false
    }
}
