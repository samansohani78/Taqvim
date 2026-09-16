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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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

    /** A fetcher whose [answer] runs while the request is in flight, so a change during a fetch can be scripted. */
    private class ActionFetcher(
        private val answer: suspend () -> FetchResult,
    ) : IcsFetcher {
        override suspend fun fetch(
            url: String,
            validators: HttpValidators,
        ): FetchResult = answer()
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

    private fun refresherWith(answer: suspend () -> FetchResult) =
        SubscriptionRefresher(dao, ActionFetcher(answer), clock, { TimeZone.of("Asia/Tehran") })

    private suspend fun pause(id: Long) {
        dao.updateSubscription(dao.getSubscription(id).shouldNotBeNull().copy(enabled = false, displayName = "Mine"))
    }

    @Test
    fun aPauseDuringAFetchSurvivesBothAnswers(): Unit =
        runTest {
            val unchanged = subscription(url = "https://example.org/304.ics")
            refresherWith {
                pause(unchanged)
                FetchResult.NotModified
            }.refresh(unchanged) shouldBe RefreshOutcome.Unchanged(unchanged)

            val downloaded = subscription(url = "https://example.org/200.ics")
            refresherWith {
                pause(downloaded)
                FetchResult.Modified(feed, validators)
            }.refresh(downloaded) shouldBe RefreshOutcome.Updated(downloaded, occurrences = 4, problems = 0)

            listOf(unchanged, downloaded).forEach { id ->
                dao.getSubscription(id).shouldNotBeNull().let {
                    listOf(it.enabled, it.displayName) shouldBe listOf(false, "Mine")
                    it.lastCheckedAtEpochMillis shouldBe clock.current.toEpochMilliseconds()
                }
            }
            cached().shouldBeEmpty()
            val resumed = dao.getSubscription(downloaded).shouldNotBeNull().copy(enabled = true)
            dao.updateSubscription(resumed)
            cached().filter { it.subscriptionId == downloaded } shouldHaveSize 4
        }

    @Test
    fun aDeleteDuringAFetchWritesNothing(): Unit =
        runTest {
            val downloaded = subscription(url = "https://example.org/200.ics")
            refresherWith {
                dao.deleteSubscription(downloaded)
                FetchResult.Modified(feed, validators)
            }.refresh(downloaded) shouldBe RefreshOutcome.Failed(downloaded, RefreshError.NotFound)

            val unchanged = subscription(url = "https://example.org/304.ics")
            refresherWith {
                dao.deleteSubscription(unchanged)
                FetchResult.NotModified
            }.refresh(unchanged) shouldBe RefreshOutcome.Failed(unchanged, RefreshError.NotFound)

            cached().shouldBeEmpty()
        }

    @Test
    fun overlappingRefreshesOfOneSubscriptionDoNotInterleave(): Unit =
        runTest {
            val id = subscription()
            val steps = mutableListOf<String>()
            val bodies = ArrayDeque(listOf(feed, feed.replace("SUMMARY:Daily", "SUMMARY:Second")))
            val serialized =
                refresherWith {
                    steps += "start"
                    delay(1.seconds)
                    steps += "end"
                    FetchResult.Modified(bodies.removeFirst(), validators)
                }

            val first = async { serialized.refresh(id) }
            val second = async { serialized.refresh(id) }
            listOf(first.await(), second.await()) shouldBe List(2) { RefreshOutcome.Updated(id, 4, 0) }

            steps shouldBe listOf("start", "end", "start", "end")
            cached().map { it.summary }.toSet() shouldBe setOf("Second", "Once")
        }

    @Test
    fun aFeedThatCancelsItsEventsEmptiesTheCache(): Unit =
        runTest {
            val id = subscription()
            fetcher.responses += FetchResult.Modified(feed, validators)
            refresher.refresh(id)
            cached() shouldHaveSize 4

            clock.current += 2.hours
            val cancelled = feed.replace("SUMMARY:", "STATUS:CANCELLED\r\nSUMMARY:")
            fetcher.responses += FetchResult.Modified(cancelled, HttpValidators("\"v2\""))
            refresher.refresh(id) shouldBe RefreshOutcome.Updated(id, occurrences = 0, problems = 0)
            cached().shouldBeEmpty()
        }

    @Test
    fun aSeriesThatBeganYearsAgoKeepsItsRowsAcrossDownloads(): Unit =
        runTest {
            val id = subscription()
            val old =
                listOf(
                    "BEGIN:VCALENDAR",
                    "PRODID:-//Test//EN",
                    "BEGIN:VEVENT",
                    "UID:since-2020@taqvim.test",
                    "DTSTART;VALUE=DATE:20200101",
                    "RRULE:FREQ=DAILY",
                    "SUMMARY:Every day",
                    "END:VEVENT",
                    "END:VCALENDAR",
                ).joinToString("\r\n", postfix = "\r\n")
            // 2026-08-13 … 2027-10-18: every day overlapping 31 days before to 400 days after 2026-09-13T12:00Z.
            repeat(2) {
                fetcher.responses += FetchResult.Modified(old, validators)
                refresher.refresh(id) shouldBe RefreshOutcome.Updated(id, occurrences = 432, problems = 0)
                cached().first().startEpochMillis shouldBe Instant.parse("2026-08-13T00:00:00Z").toEpochMilliseconds()
                clock.current += 1.hours
            }
        }
}
