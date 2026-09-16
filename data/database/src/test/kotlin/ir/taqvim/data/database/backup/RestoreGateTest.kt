/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database.backup

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** ADR-0032: readers are held while an unfinished restore is settled, and only then. */
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreGateTest {
    @Test
    fun `a start without a journal never waits`(): Unit =
        runTest {
            val gate = RestoreGate(recorded = false)
            gate.state.value shouldBe RestoreState.SETTLED
            val waiter = async { gate.awaitSettled() }
            runCurrent()
            waiter.isCompleted.shouldBeTrue()
        }

    @Test
    fun `a recorded restore holds readers until it is finished or undone`(): Unit =
        runTest {
            listOf(RecoveryResult.Completed(emptyMap()), RecoveryResult.RolledBack, RecoveryResult.NothingPending)
                .forEach { result ->
                    val gate = RestoreGate(recorded = true)
                    gate.state.value shouldBe RestoreState.RECOVERING
                    val waiter = async { gate.awaitSettled() }
                    runCurrent()
                    waiter.isCompleted.shouldBeFalse()

                    gate.settle(result)
                    runCurrent()
                    waiter.isCompleted.shouldBeTrue()
                    gate.state.value shouldBe RestoreState.SETTLED
                }
        }

    @Test
    fun `a restore that stays incomplete keeps readers held`(): Unit =
        runTest {
            val gate = RestoreGate(recorded = true)
            val waiter = async { gate.awaitSettled() }
            gate.settle(RecoveryResult.StillPending)
            runCurrent()
            gate.state.value shouldBe RestoreState.INCOMPLETE
            waiter.isCompleted.shouldBeFalse()
            waiter.cancel()
        }

    @Test
    fun `a restore that fails during the session holds readers again`() {
        val gate = RestoreGate(recorded = false)
        gate.settle(RecoveryResult.StillPending)
        gate.state.value shouldBe RestoreState.INCOMPLETE
    }
}
