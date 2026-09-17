/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** The status label of a subscription row (F03). */
@StringRes
internal fun healthText(row: SubscriptionRow): Int =
    when {
        row.refreshing -> {
            R.string.settings_subscriptions_refreshing
        }

        else -> {
            when (row.health) {
                SubscriptionHealth.PAUSED -> R.string.settings_subscriptions_health_paused
                SubscriptionHealth.FAILED -> R.string.settings_subscriptions_health_failed
                SubscriptionHealth.NEVER_FETCHED -> R.string.settings_subscriptions_never_downloaded
                SubscriptionHealth.STALE -> R.string.settings_subscriptions_health_stale
                SubscriptionHealth.OK -> R.string.settings_subscriptions_downloaded
            }
        }
    }

/** Whether the status needs the user's attention, so it is drawn in the error color. */
internal fun SubscriptionRow.needsAttention(): Boolean =
    !refreshing && (health == SubscriptionHealth.FAILED || health == SubscriptionHealth.STALE)

@Composable
internal fun healthColor(row: SubscriptionRow): Color =
    if (row.needsAttention()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

/** The expanded health details of one subscription (F03): download and check times, cache, problems and failure. */
@Composable
internal fun SubscriptionDetailsSection(
    row: SubscriptionRow,
    modifier: Modifier = Modifier,
) {
    val details = row.details
    val never = stringResource(R.string.settings_subscriptions_never)
    Column(
        modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        details.error?.let { error ->
            val reason = stringResource(errorText(error), details.errorStatus.orEmpty())
            val at = details.errorAt?.let { momentText(it) } ?: never
            DetailLine(stringResource(R.string.settings_subscriptions_error_at, at, reason), error = true)
        }
        DetailLine(
            stringResource(
                R.string.settings_subscriptions_last_success,
                details.lastSuccess?.let { momentText(it) } ?: never,
            ),
        )
        DetailLine(
            stringResource(
                R.string.settings_subscriptions_last_check,
                details.lastCheck?.let { momentText(it) } ?: never,
            ),
        )
        DetailLine(nextCheckText(row))
        DetailLine(cacheText(details))
        details.problems?.let { DetailLine(stringResource(R.string.settings_subscriptions_problems, it)) }
    }
}

@Composable
private fun DetailLine(
    text: String,
    error: Boolean = false,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun momentText(moment: SubscriptionMoment): String =
    stringResource(R.string.settings_subscriptions_date_time, moment.date, moment.time)

@Composable
private fun nextCheckText(row: SubscriptionRow): String {
    val next = row.details.nextCheck
    return when {
        !row.enabled -> stringResource(R.string.settings_subscriptions_next_check_paused)
        next == null -> stringResource(R.string.settings_subscriptions_next_check_soon)
        else -> stringResource(R.string.settings_subscriptions_next_check, momentText(next))
    }
}

@Composable
private fun cacheText(details: SubscriptionDetails): String {
    val from = details.cachedFrom
    val until = details.cachedUntil
    return if (from == null || until == null) {
        stringResource(R.string.settings_subscriptions_cached_none)
    } else {
        stringResource(R.string.settings_subscriptions_cached, details.eventCount, from, until)
    }
}

@StringRes
private fun errorText(error: SubscriptionError): Int =
    when (error) {
        SubscriptionError.NETWORK -> R.string.settings_subscriptions_error_network
        SubscriptionError.TIMEOUT -> R.string.settings_subscriptions_error_timeout
        SubscriptionError.SERVER -> R.string.settings_subscriptions_error_server
        SubscriptionError.NOT_AVAILABLE -> R.string.settings_subscriptions_error_not_available
        SubscriptionError.TOO_LARGE -> R.string.settings_subscriptions_error_too_large
        SubscriptionError.INSECURE -> R.string.settings_subscriptions_error_insecure
        SubscriptionError.INVALID_ADDRESS -> R.string.settings_subscriptions_error_invalid
        SubscriptionError.UNREADABLE -> R.string.settings_subscriptions_error_unreadable
    }
