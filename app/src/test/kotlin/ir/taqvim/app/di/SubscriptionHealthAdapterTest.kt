/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsCacheSummary
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.SubscriptionErrorCodes
import ir.taqvim.feature.settings.SubscriptionError
import ir.taqvim.feature.settings.SubscriptionHealthData
import ir.taqvim.feature.settings.SubscriptionItem
import org.junit.jupiter.api.Test

/** F03: stored subscriptions become the settings page's items with their health, and failure codes their kinds. */
class SubscriptionHealthAdapterTest {
    private val hour = 3_600_000L

    @Test
    fun `a stored subscription carries its checks, cache, problems and failure`() {
        val entity =
            IcsSubscriptionEntity(
                id = 3,
                url = "https://example.org/a.ics",
                displayName = "A",
                refreshIntervalMinutes = 5,
                lastFetchedAtEpochMillis = 10 * hour,
                lastCheckedAtEpochMillis = 11 * hour,
                lastError = SubscriptionErrorCodes.http(503),
                lastErrorAtEpochMillis = 12 * hour,
                problemCount = 4,
            )

        entity.toItem(IcsCacheSummary(3, 9, 1_000, 2_000)) shouldBe
            SubscriptionItem(
                id = 3,
                name = "A",
                url = "https://example.org/a.ics",
                enabled = true,
                lastFetchedAtEpochMillis = 10 * hour,
                health =
                    SubscriptionHealthData(
                        lastCheckedAtEpochMillis = 11 * hour,
                        // The refresh interval is never shorter than WorkManager's 15 minutes.
                        nextCheckAtEpochMillis = 11 * hour + 15 * 60_000,
                        refreshIntervalMinutes = 15,
                        cachedEvents = 9,
                        cachedFromEpochMillis = 1_000,
                        cachedUntilEpochMillis = 2_000,
                        problemCount = 4,
                        error = SubscriptionError.SERVER,
                        httpStatus = 503,
                        errorAtEpochMillis = 12 * hour,
                    ),
            )
    }

    @Test
    fun `a never checked subscription has no next check and an empty cache`() {
        val entity =
            IcsSubscriptionEntity(url = "https://example.org/b.ics", displayName = "B", refreshIntervalMinutes = 60)

        entity.toItem(null).health shouldBe SubscriptionHealthData(refreshIntervalMinutes = 60)
    }

    @Test
    fun `stored failure codes map to their kinds`() {
        mapOf(
            SubscriptionErrorCodes.NETWORK to SubscriptionError.NETWORK,
            SubscriptionErrorCodes.TIMEOUT to SubscriptionError.TIMEOUT,
            SubscriptionErrorCodes.TOO_LARGE to SubscriptionError.TOO_LARGE,
            SubscriptionErrorCodes.INSECURE to SubscriptionError.INSECURE,
            SubscriptionErrorCodes.INVALID_ADDRESS to SubscriptionError.INVALID_ADDRESS,
            SubscriptionErrorCodes.UNREADABLE to SubscriptionError.UNREADABLE,
            SubscriptionErrorCodes.http(500) to SubscriptionError.SERVER,
            SubscriptionErrorCodes.http(429) to SubscriptionError.SERVER,
            SubscriptionErrorCodes.http(404) to SubscriptionError.NOT_AVAILABLE,
            SubscriptionErrorCodes.http(410) to SubscriptionError.NOT_AVAILABLE,
            "SOMETHING_NEW" to SubscriptionError.NETWORK,
        ).forEach { (code, kind) -> subscriptionError(code) shouldBe kind }
    }
}
