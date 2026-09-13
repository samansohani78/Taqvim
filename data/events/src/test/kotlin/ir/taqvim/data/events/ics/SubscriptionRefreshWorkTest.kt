/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.TaqvimDatabase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** T-1003 (R): the periodic refresh work — network constraint, period, cancellation and retry decisions. */
@RunWith(AndroidJUnit4::class)
class SubscriptionRefreshWorkTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db =
        Room
            .inMemoryDatabaseBuilder(context, TaqvimDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Before
    fun initWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @After
    fun close() {
        db.close()
    }

    private fun subscription(
        intervalMinutes: Int,
        enabled: Boolean = true,
    ) = IcsSubscriptionEntity(
        url = "https://example.org/$intervalMinutes.ics",
        displayName = "Feed",
        enabled = enabled,
        refreshIntervalMinutes = intervalMinutes,
    )

    private fun workInfo(workManager: WorkManager): WorkInfo =
        workManager.getWorkInfosForUniqueWork(SubscriptionRefreshScheduler.WORK_NAME).get().single()

    @Test
    fun periodicWorkNeedsANetworkAndFollowsTheShortestInterval() {
        val workManager = WorkManager.getInstance(context)
        val scheduler = SubscriptionRefreshScheduler(workManager)

        scheduler.update(listOf(subscription(60), subscription(30), subscription(20, enabled = false)), true) shouldBe
            30.minutes
        workInfo(workManager).let {
            it.state shouldBe WorkInfo.State.ENQUEUED
            it.constraints.requiredNetworkType shouldBe NetworkType.CONNECTED
            it.periodicityInfo?.repeatIntervalMillis shouldBe 30.minutes.inWholeMilliseconds
        }

        scheduler.update(listOf(subscription(60)), networkAllowed = false).shouldBeNull()
        workInfo(workManager).state shouldBe WorkInfo.State.CANCELLED
        scheduler.update(emptyList(), networkAllowed = true).shouldBeNull()
    }

    @Test
    fun theWorkerRetriesOnlyAfterTransientFailures(): Unit =
        runTest {
            db.icsSubscriptionDao().insertSubscription(subscription(60))

            worker(FetchResult.Failed(FetchError.Timeout)).doWork() shouldBe ListenableWorker.Result.retry()
            worker(FetchResult.Failed(FetchError.HttpStatus(404))).doWork() shouldBe ListenableWorker.Result.success()
            worker(FetchResult.NotModified).doWork() shouldBe ListenableWorker.Result.success()
        }

    private fun worker(result: FetchResult): SubscriptionRefreshWorker {
        val fetcher =
            object : IcsFetcher {
                override suspend fun fetch(
                    url: String,
                    validators: HttpValidators,
                ): FetchResult = result
            }
        val policy = SubscriptionRefreshPolicy()
        val refresher = SubscriptionRefresher(db.icsSubscriptionDao(), fetcher, Clock.System, { TimeZone.UTC }, policy)
        return TestListenableWorkerBuilder<SubscriptionRefreshWorker>(context)
            .setWorkerFactory(SubscriptionRefreshWorkerFactory(refresher, policy))
            .build()
    }
}
