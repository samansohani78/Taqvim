/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.Collections
import java.util.IdentityHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Locks per subscription (review R15): the refresher kept one Mutex per subscription id it had ever seen, so adding
 * and deleting subscriptions grew the map for the life of the process. A fixed set of stripes bounds it.
 */
class StripedLocksTest {
    @Test
    fun `any number of ids share a fixed set of locks`() {
        val locks = StripedLocks()
        val distinct = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        (1L..CHURN).forEach { distinct += locks.lockFor(it) }
        distinct.size shouldBeLessThanOrEqual StripedLocks.STRIPES
    }

    @Test
    fun `one id always gets the same lock, negative and large ids included`() {
        val locks = StripedLocks()
        listOf(0L, 7L, -7L, Long.MAX_VALUE, Long.MIN_VALUE).forEach { id ->
            locks.lockFor(id) shouldBeSameInstanceAs locks.lockFor(id)
        }
    }

    @Test
    fun `two refreshes of one subscription never overlap`(): Unit =
        runTest {
            val locks = StripedLocks()
            var inside = 0
            var most = 0
            List(PARALLEL) {
                async {
                    locks.lockFor(SUBSCRIPTION).withLock {
                        inside++
                        most = maxOf(most, inside)
                        delay(1)
                        inside--
                    }
                }
            }.awaitAll()
            most shouldBe 1
        }

    private companion object {
        const val CHURN = 100_000L
        const val SUBSCRIPTION = 42L
        const val PARALLEL = 8
    }
}
