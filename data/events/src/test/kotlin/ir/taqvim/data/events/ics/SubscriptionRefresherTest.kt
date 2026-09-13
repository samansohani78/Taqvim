/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** T-1003 (R): subscription refresh over Room with a scripted fetcher (conditional requests, failures, due checks). */
@RunWith(AndroidJUnit4::class)
class SubscriptionRefresherTest {
    private class ScriptedFetcher : IcsFetcher {
        val responses = ArrayDeque<FetchResult>()
        val calls = mutableListOf<Pair<String, HttpValidators>>()

        override suspend fun fetch(
            url: String,
            validators: HttpValidators,
        ): FetchResult {
            calls += url to validators
            return responses.removeFirst()
        }
    }

    private class SteppingClock(
        var current: Instant,
    ) : Clock {
        override fun now(): Instant = current
    }

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = db.icsSubscriptionDao()
    private val fetcher = ScriptedFetcher()
    private val clock = SteppingClock(Instant.parse("2026-09-13T12:00:00Z"))
    private val refresher = SubscriptionRefresher(dao, fetcher, clock, { TimeZone.of("Asia/Tehran") })
    private val validators = HttpValidators("\"v1\"", "Sun, 13 Sep 2026 10:00:00 GMT")
    private val feed =
        listOf(
            "BEGIN:VCALENDAR",
            "PRODID:-//Test//EN",
            "BEGIN:VEVENT",
            "UID:daily@taqvim.test",
            "DTSTART;VALUE=DATE:20260914",
            "RRULE:FREQ=DAILY;COUNT=3",
            "SUMMARY:Daily",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:once@taqvim.test",
            "DTSTART:20260920T100000Z",
            "DTEND:20260920T110000Z",
            "SUMMARY:Once",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n", postfix = "\r\n")

    @After
    fun close() {
        db.close()
    }

    private suspend fun subscription(
        url: String = "webcal://example.org/cal.ics",
        enabled: Boolean = true,
        lastChecked: Instant? = null,
    ): Long =
        dao.insertSubscription(
            IcsSubscriptionEntity(
                url = url,
                displayName = url,
                enabled = enabled,
                refreshIntervalMinutes = 60,
                lastCheckedAtEpochMillis = lastChecked?.toEpochMilliseconds(),
            ),
        )

    private suspend fun cached(): List<IcsEventCacheEntity> = dao.observeEvents(0, Long.MAX_VALUE).first()

    @Test
    fun aChangedFeedReplacesTheCacheAndStoresItsValidators(): Unit =
        runTest {
            val id = subscription()
            dao.insertEvents(listOf(IcsEventCacheEntity(id, "old", 0, 1, false, "Old")))
            fetcher.responses += FetchResult.Modified(feed, validators)

            refresher.refresh(id) shouldBe RefreshOutcome.Updated(id, occurrences = 4, problems = 0)
            fetcher.calls shouldBe listOf("https://example.org/cal.ics" to HttpValidators())
            cached().map { it.uid } shouldBe List(3) { "daily@taqvim.test" } + "once@taqvim.test"
            dao.getSubscription(id).shouldNotBeNull().let {
                listOf(it.etag, it.lastModified) shouldBe listOf(validators.etag, validators.lastModified)
                listOf(it.lastFetchedAtEpochMillis, it.lastCheckedAtEpochMillis) shouldBe
                    List(2) { clock.current.toEpochMilliseconds() }
            }
        }

    @Test
    fun notModifiedKeepsTheCacheAndValidatorsExpireAfterAWeek(): Unit =
        runTest {
            val id = subscription()
            fetcher.responses += FetchResult.Modified(feed, validators)
            refresher.refresh(id)
            val fetchedAt = clock.current.toEpochMilliseconds()

            clock.current += 2.hours
            fetcher.responses += FetchResult.NotModified
            refresher.refresh(id) shouldBe RefreshOutcome.Unchanged(id)
            fetcher.calls.last().second shouldBe validators
            cached().size shouldBe 4
            dao.getSubscription(id).shouldNotBeNull().let {
                listOf(it.lastFetchedAtEpochMillis, it.lastCheckedAtEpochMillis) shouldBe
                    listOf(fetchedAt, clock.current.toEpochMilliseconds())
            }

            clock.current += 8.days
            fetcher.responses += FetchResult.NotModified
            refresher.refresh(id)
            fetcher.calls.last().second shouldBe HttpValidators()
        }

    @Test
    fun failuresKeepTheCachedEvents(): Unit =
        runTest {
            val id = subscription()
            fetcher.responses += FetchResult.Modified(feed, validators)
            refresher.refresh(id)
            val checkedAt = dao.getSubscription(id)?.lastCheckedAtEpochMillis

            clock.current += 2.hours
            fetcher.responses += FetchResult.Failed(FetchError.Timeout)
            refresher.refresh(id) shouldBe RefreshOutcome.Failed(id, RefreshError.Fetch(FetchError.Timeout))
            fetcher.responses += FetchResult.Modified("garbage", HttpValidators("\"v2\""))
            refresher.refresh(id) shouldBe RefreshOutcome.Failed(id, RefreshError.Unreadable)

            cached().size shouldBe 4
            dao.getSubscription(id).shouldNotBeNull().let {
                listOf(it.etag, it.lastCheckedAtEpochMillis) shouldBe listOf(validators.etag, checkedAt)
            }
        }

    @Test
    fun badUrlsAndMissingSubscriptionsFailWithoutFetching(): Unit =
        runTest {
            val insecure = subscription(url = "http://example.org/cal.ics")
            val invalid = subscription(url = "no url")

            refresher.refresh(insecure) shouldBe RefreshOutcome.Failed(insecure, RefreshError.InsecureUrl)
            refresher.refresh(invalid) shouldBe RefreshOutcome.Failed(invalid, RefreshError.InvalidUrl)
            refresher.refresh(999) shouldBe RefreshOutcome.Failed(999, RefreshError.NotFound)
            fetcher.calls.shouldBeEmpty()
        }

    @Test
    fun onlyDueEnabledSubscriptionsAreRefreshed(): Unit =
        runTest {
            val due = subscription(url = "https://example.org/due.ics")
            subscription(url = "https://example.org/disabled.ics", enabled = false)
            subscription(url = "https://example.org/recent.ics", lastChecked = clock.current - 10.minutes)
            fetcher.responses += FetchResult.NotModified

            refresher.refreshDue() shouldBe listOf(RefreshOutcome.Unchanged(due))
            fetcher.calls.map { it.first } shouldBe listOf("https://example.org/due.ics")
        }
}
