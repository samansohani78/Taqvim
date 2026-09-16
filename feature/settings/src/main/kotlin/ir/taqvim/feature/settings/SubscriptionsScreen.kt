/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/** Calendar subscriptions (T-1500), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun SubscriptionsScreen(
    state: SubscriptionsUiState,
    actions: SubscriptionsActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.settings_subscriptions_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.settings_subscriptions_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                SubscriptionsContent(state, actions)
            }
        }
    }
}

@Composable
private fun SubscriptionsContent(
    state: SubscriptionsUiState,
    actions: SubscriptionsActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SwitchItem(
            title = stringResource(R.string.settings_subscriptions_network),
            detail = stringResource(R.string.settings_subscriptions_network_detail),
            checked = state.networkAllowed,
            onChange = actions.onNetworkAllowedChanged,
        )
        OutlinedTextField(
            value = state.draft,
            onValueChange = actions.onDraftChanged,
            label = { Text(stringResource(R.string.settings_subscriptions_address_label)) },
            singleLine = true,
            textStyle = TextStyle(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = actions.onAdd, enabled = !state.adding) {
            Text(stringResource(R.string.settings_subscriptions_add))
        }
        state.message?.let { message ->
            Text(
                stringResource(messageText(message)),
                color = if (message.isProblem()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (state.items.isEmpty()) Text(stringResource(R.string.settings_subscriptions_empty))
        state.items.forEach { SubscriptionCard(it, actions) }
    }
}

@Composable
private fun SubscriptionCard(
    row: SubscriptionRow,
    actions: SubscriptionsActions,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SwitchItem(row.name, row.url, row.enabled, { actions.onEnabledChanged(row.id, it) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    when {
                        row.refreshing -> R.string.settings_subscriptions_refreshing
                        row.downloaded -> R.string.settings_subscriptions_downloaded
                        else -> R.string.settings_subscriptions_never_downloaded
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            val refresh = stringResource(R.string.settings_subscriptions_refresh, row.name)
            TextButton(
                onClick = { actions.onRefresh(row.id) },
                enabled = !row.refreshing,
                modifier = Modifier.semantics { contentDescription = refresh },
            ) { Text(stringResource(R.string.settings_subscriptions_refresh_button)) }
            val remove = stringResource(R.string.settings_subscriptions_remove, row.name)
            TextButton(
                onClick = { actions.onRemove(row.id) },
                modifier = Modifier.semantics { contentDescription = remove },
            ) { Text(stringResource(R.string.settings_subscriptions_remove_button)) }
        }
    }
}

private fun SubscriptionMessage.isProblem(): Boolean =
    this != SubscriptionMessage.ADDED && this != SubscriptionMessage.REFRESHED

private fun messageText(message: SubscriptionMessage): Int =
    when (message) {
        SubscriptionMessage.ADDED -> R.string.settings_subscriptions_message_added
        SubscriptionMessage.REFRESHED -> R.string.settings_subscriptions_message_refreshed
        SubscriptionMessage.INVALID_ADDRESS -> R.string.settings_subscriptions_message_invalid
        SubscriptionMessage.ALREADY_SUBSCRIBED -> R.string.settings_subscriptions_message_duplicate
        SubscriptionMessage.NETWORK_NOT_ALLOWED -> R.string.settings_subscriptions_message_network
        SubscriptionMessage.FAILED -> R.string.settings_subscriptions_message_failed
        SubscriptionMessage.CHANGE_FAILED -> R.string.settings_subscriptions_message_change_failed
    }
