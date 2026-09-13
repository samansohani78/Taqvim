/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Receives the app's own alarm broadcasts (explicit, not exported) and hands them to [SchedulerEvents]. */
class AlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val events: SchedulerEvents by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val id = intent.getLongExtra(EXTRA_ALARM_ID, NO_ALARM)
        if (intent.action == ACTION_ALARM && id != NO_ALARM) {
            handleAsync { events.onAlarmFired(id) }
        }
    }

    companion object {
        const val ACTION_ALARM: String = "ir.taqvim.data.scheduler.action.ALARM"
        const val EXTRA_ALARM_ID: String = "ir.taqvim.data.scheduler.extra.ALARM_ID"
        private const val NO_ALARM = -1L

        /** The broadcast that alarm [requestCode] sends. */
        fun intent(
            context: Context,
            requestCode: Int,
        ): Intent =
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_ALARM_ID, requestCode.toLong())
    }
}

/** Receives boot, app-update, clock, time-zone and exact-alarm-permission broadcasts. */
class RescheduleReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val events: SchedulerEvents by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        RescheduleEvent.forAction(intent.action)?.let { event -> handleAsync { events.handle(event) } }
    }
}

/** Runs [block] off the main thread and keeps the broadcast alive until it completes; failures are rethrown. */
private fun BroadcastReceiver.handleAsync(block: suspend () -> Unit) {
    val pending: BroadcastReceiver.PendingResult? = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        val result = runCatching { block() }
        pending?.finish()
        result.getOrThrow()
    }
}
