/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.os.StrictMode
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.backup.BackupService
import ir.taqvim.data.database.backup.RecoveryResult
import ir.taqvim.data.database.backup.RestoreGate
import ir.taqvim.data.scheduler.RescheduleEvent
import ir.taqvim.data.scheduler.SchedulerEvents

/**
 * Finishes a restore the previous process left unfinished (B09) and, once data and preferences match again, tells the
 * scheduler to recompute every alarm and releases the [gate] that holds the screens and background work (ADR-0032).
 * Run at start-up before the watchers that read or schedule from the data.
 */
internal class RestoreRecovery(
    private val service: BackupService,
    private val events: SchedulerEvents,
    private val gate: RestoreGate = RestoreGate(recorded = false),
) {
    suspend fun run(): RecoveryResult =
        service.recover().also { result ->
            gate.settle(result)
            if (result is RecoveryResult.Completed || result == RecoveryResult.RolledBack) {
                events.handle(RescheduleEvent.AlarmInputsChanged(AlarmKind.entries.toSet()))
            }
        }
}

/**
 * The process's [RestoreGate]: starts held only when a restore journal was left by the previous process. The check is
 * one file lookup, allowed on the main thread because it runs once while the process starts.
 */
internal fun restoreGateAtStart(service: BackupService): RestoreGate {
    val policy = StrictMode.allowThreadDiskReads()
    val recorded = service.isRestoreRecorded()
    StrictMode.setThreadPolicy(policy)
    return RestoreGate(recorded)
}
