/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.model

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] for work that may run in a coroutine: a failure of [block] becomes a failed [Result], but a
 * [CancellationException] is rethrown so a cancelled coroutine stays cancelled instead of reporting an ordinary error
 * (code review I04). Being `inline`, [block] may call suspending functions when the caller suspends.
 */
public inline fun <T> attempt(block: () -> T): Result<T> =
    runCatching(block).onFailure { failure -> if (failure is CancellationException) throw failure }
