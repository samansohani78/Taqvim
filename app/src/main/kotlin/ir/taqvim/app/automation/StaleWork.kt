/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import android.app.job.JobScheduler
import android.content.Context

/**
 * Cancels background jobs left over from a version that had calendar subscriptions (ADR-0054).
 *
 * Until the subscription feature was removed, WorkManager held a periodic refresh job for it, scheduled through the
 * platform [JobScheduler]. Upgrading does not clear those: JobScheduler persists a job across an app update, and
 * WorkManager is no longer in the APK to cancel its own work. Taqvim now schedules no job of any kind — its only
 * background work is an [android.app.AlarmManager] alarm — so every pending job belongs to the removed feature and
 * can go unconditionally.
 *
 * This runs from [DayChangeReceiver] on boot and on `MY_PACKAGE_REPLACED`, which is the upgrade that strands them. It
 * is idempotent and, after the first run, finds nothing: [JobScheduler.getAllPendingJobs] returns only this app's
 * jobs, so it cannot touch another app's work.
 */
internal object StaleWork {
    fun cancelAll(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        if (runCatching { jobs.allPendingJobs }.getOrNull().isNullOrEmpty()) return
        runCatching { jobs.cancelAll() }
    }
}
