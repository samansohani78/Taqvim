/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Whether the database and the preferences can be read as one consistent state (ADR-0032). */
enum class RestoreState {
    /** Nothing is unfinished; readers may proceed. */
    SETTLED,

    /** A restore left unfinished by the previous process is being finished or undone. */
    RECOVERING,

    /** The unfinished restore could not be settled; the stores may be partly replaced until the next start. */
    INCOMPLETE,
}

/**
 * Holds readers back while a restore journal is being settled (ADR-0032). One instance is shared by the process: the
 * start-up recovery [settle]s it, the app shell shows a waiting screen until [state] is [RestoreState.SETTLED], and
 * background work such as subscription refreshes calls [awaitSettled] first.
 *
 * [recorded] is whether a restore journal existed when the process started; without one the gate starts settled, so a
 * normal start never waits.
 */
class RestoreGate(
    recorded: Boolean,
) {
    private val current = MutableStateFlow(if (recorded) RestoreState.RECOVERING else RestoreState.SETTLED)

    /** The current state; starts at [RestoreState.RECOVERING] only when a journal was found. */
    val state: StateFlow<RestoreState> = current.asStateFlow()

    /** Records the outcome of settling a journal: [RecoveryResult.StillPending] keeps readers held. */
    fun settle(result: RecoveryResult) {
        current.value = if (result == RecoveryResult.StillPending) RestoreState.INCOMPLETE else RestoreState.SETTLED
    }

    /** Suspends until the stores are settled; while a restore stays incomplete this keeps waiting. */
    suspend fun awaitSettled() {
        current.first { it == RestoreState.SETTLED }
    }
}
