/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import kotlinx.coroutines.sync.Mutex

/**
 * A fixed set of [STRIPES] locks shared by any number of ids (review R15): one id always maps to the same lock, so
 * work on it is serialised, and the set never grows however many ids come and go. Two ids may share a stripe and then
 * wait for each other, which costs nothing for a handful of subscriptions refreshed in the background.
 */
internal class StripedLocks {
    private val stripes = List(STRIPES) { Mutex() }

    fun lockFor(id: Long): Mutex = stripes[Math.floorMod(id, STRIPES)]

    companion object {
        const val STRIPES: Int = 32
    }
}
