/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsCacheSummary
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.SubscriptionErrorCodes
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** F03 (R): refreshes record their failure, clear it on the next answer and store the reader's problem count. */
@RunWith(AndroidJUnit4::class)
class SubscriptionHealthRecordTest {
    private class ScriptedFetcher : IcsFetcher {
        val responses = ArrayDeque<FetchResult>()

        override suspend fun fetch(
            url: String,
            validators: HttpValidators,
        ): FetchResult = responses.removeFirst()
    }

    private val now = Instant.parse("2026-09-13T12:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = this@SubscriptionHealthRecordTest.now
        }
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Application>(),
                TaqvimDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
    private val dao = db.icsSubscriptionDao()
    private val fetcher = ScriptedFetcher()
    private val refresher = SubscriptionRefresher(dao, fetcher, clock, { TimeZone.UTC })

    /** One valid event and one without a start, which the reader skips with a problem. */
    private val feed =
        listOf(
            "BEGIN:VCALENDAR",
            "PRODID:-//Test//EN",
            "BEGIN:VEVENT",
            "UID:once@taqvim.test",
            "DTSTART:20260920T100000Z",
            "DTEND:20260920T110000Z",
            "SUMMARY:Once",
            "END:VEVENT",
            "BEGIN:VEVENT",
            "UID:broken@taqvim.test",
            "SUMMARY:No start",
            "END:VEVENT",
            "END:VCALENDAR",
        ).joinToString("\r\n", postfix = "\r\n")

    @After
    fun close() {
        db.close()
    }

    private suspend fun subscription(url: String = "https://example.org/cal.ics"): Long =
        dao.insertSubscription(IcsSubscriptionEntity(url = url, displayName = url, refreshIntervalMinutes = 60))

    private suspend fun stored(id: Long): IcsSubscriptionEntity? = dao.getSubscription(id)

    @Test
    fun failuresAreRecordedWithTheirKindAndClearedByTheNextAnswer(): Unit =
        runTest {
            val id = subscription()
            fetcher.responses += FetchResult.Failed(FetchError.HttpStatus(404))
            fetcher.responses += FetchResult.Failed(FetchError.Timeout)
            fetcher.responses += FetchResult.NotModified

            refresher.refresh(id)
            stored(id)?.lastError shouldBe SubscriptionErrorCodes.http(404)
            stored(id)?.lastErrorAtEpochMillis shouldBe now.toEpochMilliseconds()

            refresher.refresh(id)
            stored(id)?.lastError shouldBe SubscriptionErrorCodes.TIMEOUT

            refresher.refresh(id)
            stored(id)?.lastError shouldBe null
            stored(id)?.lastErrorAtEpochMillis shouldBe null
        }

    @Test
    fun aDownloadStoresTheProblemCountAndClearsTheFailure(): Unit =
        runTest {
            val id = subscription()
            fetcher.responses += FetchResult.Modified("not a calendar", HttpValidators())
            fetcher.responses += FetchResult.Modified(feed, HttpValidators())

            refresher.refresh(id)
            stored(id)?.lastError shouldBe SubscriptionErrorCodes.UNREADABLE

            refresher.refresh(id)
            val row = stored(id)
            row?.lastError shouldBe null
            row?.problemCount shouldBe 1
            dao.observeCacheSummaries().first() shouldBe
                listOf(
                    IcsCacheSummary(
                        subscriptionId = id,
                        eventCount = 1,
                        firstStartEpochMillis = Instant.parse("2026-09-20T10:00:00Z").toEpochMilliseconds(),
                        lastEndEpochMillis = Instant.parse("2026-09-20T11:00:00Z").toEpochMilliseconds(),
                    ),
                )
        }

    @Test
    fun addressFailuresAreRecordedAndAMissingSubscriptionWritesNothing(): Unit =
        runTest {
            val insecure = subscription("http://example.org/cal.ics")
            refresher.refresh(insecure)
            stored(insecure)?.lastError shouldBe SubscriptionErrorCodes.INSECURE

            fetcher.responses += FetchResult.Failed(FetchError.Network)
            refresher.refresh(404L)
            dao.observeSubscriptions().first().map { it.lastError } shouldBe listOf(SubscriptionErrorCodes.INSECURE)

            SubscriptionErrorCodes.httpStatus(SubscriptionErrorCodes.http(503)) shouldBe 503
            SubscriptionErrorCodes.httpStatus(SubscriptionErrorCodes.NETWORK) shouldBe null
        }
}
