/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferencesRepository
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import org.koin.dsl.module

/** Periodic refresh of the due subscriptions; retried with back-off after transient network failures. */
class SubscriptionRefreshWorker(
    context: Context,
    parameters: WorkerParameters,
    private val refresher: SubscriptionRefresher,
    private val policy: SubscriptionRefreshPolicy,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result =
        if (policy.shouldRetry(refresher.refreshDue())) Result.retry() else Result.success()
}

/** Creates [SubscriptionRefreshWorker]s with their dependencies; `null` for other workers. */
class SubscriptionRefreshWorkerFactory(
    private val refresher: SubscriptionRefresher,
    private val policy: SubscriptionRefreshPolicy,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == SubscriptionRefreshWorker::class.java.name) {
            SubscriptionRefreshWorker(appContext, workerParameters, refresher, policy)
        } else {
            null
        }
}

/** Schedules the refresh work: on a connected network only, and only while the user allows it (F-05). */
class SubscriptionRefreshScheduler(
    private val workManager: WorkManager,
    private val policy: SubscriptionRefreshPolicy = SubscriptionRefreshPolicy(),
) {
    /**
     * Enqueues or updates the periodic work for [subscriptions], or cancels it when none is enabled or [networkAllowed]
     * is false; returns the scheduled period.
     */
    fun update(
        subscriptions: List<IcsSubscriptionEntity>,
        networkAllowed: Boolean,
    ): Duration? {
        val period = policy.workPeriod(subscriptions)?.takeIf { networkAllowed }
        if (period == null) {
            workManager.cancelUniqueWork(WORK_NAME)
        } else {
            val request =
                PeriodicWorkRequestBuilder<SubscriptionRefreshWorker>(period.inWholeMinutes, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
        return period
    }

    companion object {
        /** Unique name of the periodic work. */
        const val WORK_NAME: String = "ir.taqvim.data.events.ics-subscription-refresh"
    }
}

/**
 * Koin bindings of iCalendar import, export and subscriptions (ADR-0002). Other modules provide the DAOs,
 * `TaqvimDatabase`, `UserPreferencesRepository`, `WorkManager`, `ContentResolver` and `kotlin.time.Clock`.
 */
val icsDataModule =
    module {
        single<TransactionRunner> { RoomTransactionRunner(get<TaqvimDatabase>()) }
        single { SubscriptionRefreshPolicy() }
        single<IcsFetcher> { HttpIcsFetcher() }
        single { IcsDocuments(get()) }
        single { IcsImporter(get(), get(), get(), get(), IcsZones.system) }
        single {
            val preferences = get<UserPreferencesRepository>().preferences
            IcsExporter(get(), get(), { IcsZones.personalCalendars(preferences) }, get(), IcsZones.system)
        }
        single { SubscriptionRefresher(get(), get(), get(), IcsZones.system, get()) }
        single { SubscriptionRefreshWorkerFactory(get(), get()) }
        single { SubscriptionRefreshScheduler(get(), get()) }
    }
