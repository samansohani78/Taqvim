/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.data.database.SubscriptionErrorCodes

/** The code stored for this failure (F03), or `null` when there is nothing to store (the subscription is gone). */
internal fun RefreshError.storedCode(): String? =
    when (this) {
        is RefreshError.Fetch -> error.storedCode()
        RefreshError.Unreadable -> SubscriptionErrorCodes.UNREADABLE
        RefreshError.InsecureUrl -> SubscriptionErrorCodes.INSECURE
        RefreshError.InvalidUrl -> SubscriptionErrorCodes.INVALID_ADDRESS
        RefreshError.NotFound -> null
    }

private fun FetchError.storedCode(): String =
    when (this) {
        is FetchError.HttpStatus -> SubscriptionErrorCodes.http(code)
        FetchError.TooLarge -> SubscriptionErrorCodes.TOO_LARGE
        FetchError.Timeout -> SubscriptionErrorCodes.TIMEOUT
        FetchError.Network -> SubscriptionErrorCodes.NETWORK
        FetchError.InsecureRedirect -> SubscriptionErrorCodes.INSECURE
        FetchError.InvalidUrl -> SubscriptionErrorCodes.INVALID_ADDRESS
    }
