/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.attempt
import kotlin.time.Clock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/** State of the calendar subscriptions page (T-1500 over T-1003). */
data class SubscriptionsUiState(
    val loading: Boolean = true,
    val networkAllowed: Boolean = true,
    val items: ImmutableList<SubscriptionRow> = persistentListOf(),
    /** The address being typed. */
    val draft: String = "",
    val adding: Boolean = false,
    /** The outcome of the last action, or `null`. */
    val message: SubscriptionMessage? = null,
    /** Subscriptions whose health details are shown (F03). */
    val expanded: ImmutableSet<Long> = persistentSetOf(),
)

/** One subscribed calendar. */
data class SubscriptionRow(
    val id: Long,
    val name: String,
    val url: String,
    val enabled: Boolean,
    val downloaded: Boolean,
    val refreshing: Boolean,
    val health: SubscriptionHealth = if (downloaded) SubscriptionHealth.OK else SubscriptionHealth.NEVER_FETCHED,
    val details: SubscriptionDetails = SubscriptionDetails(),
)

/** What the last action on the page did. */
enum class SubscriptionMessage {
    ADDED,
    REFRESHED,
    INVALID_ADDRESS,
    ALREADY_SUBSCRIBED,
    NETWORK_NOT_ALLOWED,
    FAILED,

    /** Pausing, removing or the network choice could not be stored; the user can try again. */
    CHANGE_FAILED,
}

/** User actions of the subscriptions page. */
@Immutable
data class SubscriptionsActions(
    val onDraftChanged: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onRefresh: (Long) -> Unit = {},
    val onRemove: (Long) -> Unit = {},
    val onEnabledChanged: (Long, Boolean) -> Unit = { _, _ -> },
    val onNetworkAllowedChanged: (Boolean) -> Unit = {},
    val onDetailsToggled: (Long) -> Unit = {},
)

/** Addresses a subscription can be added from. */
internal object SubscriptionAddress {
    private val SCHEMES = listOf("https://", "webcal://")

    /** Whether [text] is an `https://` or `webcal://` address with a host and no spaces. */
    fun isValid(text: String): Boolean {
        val address = text.trim()
        val scheme = SCHEMES.firstOrNull { address.startsWith(it, ignoreCase = true) } ?: return false
        val rest = address.substring(scheme.length)
        return rest.isNotEmpty() && !rest.startsWith("/") && rest.none(Char::isWhitespace)
    }
}

/**
 * Lists calendar subscriptions with their health (F03) and adds, refreshes, pauses and removes them through
 * [SubscriptionsStore]; [clock] and [zone] date the check times.
 */
class SubscriptionsViewModel(
    private val store: SubscriptionsStore,
    private val settings: GeneralSettingsStore,
    private val clock: Clock = Clock.System,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ViewModel() {
    private val state = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = state.asStateFlow()

    private val refreshing = MutableStateFlow<Set<Long>>(emptySet())

    init {
        combine(store.subscriptions(), settings.settings(), refreshing) { items, data, busy ->
            state.update { current ->
                current.copy(
                    loading = false,
                    networkAllowed = data.settings.subscriptionsNetworkAllowed,
                    items = items.map { it.toRow(refreshing = it.id in busy, data.language) }.toImmutableList(),
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onDraftChanged(text: String) {
        state.update { it.copy(draft = text, message = null) }
    }

    fun onAdd() {
        val current = state.value
        if (current.adding) return
        if (!SubscriptionAddress.isValid(current.draft)) {
            state.update { it.copy(message = SubscriptionMessage.INVALID_ADDRESS) }
            return
        }
        state.update { it.copy(adding = true, message = null) }
        viewModelScope
            .launch {
                val outcome = attempt { store.add(current.draft.trim()) }.getOrDefault(SubscriptionOutcome.FAILED)
                state.update {
                    it.copy(
                        draft = if (outcome == SubscriptionOutcome.DONE) "" else it.draft,
                        message = message(outcome, SubscriptionMessage.ADDED),
                    )
                }
            }.invokeOnCompletion { state.update { it.copy(adding = false) } }
    }

    fun onRefresh(id: Long) {
        if (id in refreshing.value) return
        refreshing.update { it + id }
        viewModelScope
            .launch {
                val outcome = attempt { store.refresh(id) }.getOrDefault(SubscriptionOutcome.FAILED)
                state.update { it.copy(message = message(outcome, SubscriptionMessage.REFRESHED)) }
            }.invokeOnCompletion { refreshing.update { it - id } }
    }

    /** Shows or hides the health details of subscription [id]. */
    fun onDetailsToggled(id: Long) {
        state.update {
            val next = if (id in it.expanded) it.expanded - id else it.expanded + id
            it.copy(expanded = next.toImmutableSet())
        }
    }

    fun onRemove(id: Long) {
        change { store.remove(id) }
    }

    fun onEnabledChanged(
        id: Long,
        enabled: Boolean,
    ) {
        change { store.setEnabled(id, enabled) }
    }

    fun onNetworkAllowedChanged(allowed: Boolean) {
        change { settings.update { it.copy(subscriptionsNetworkAllowed = allowed) } }
    }

    /** Runs a stored change; a failure is reported so the user can try again, a cancellation stays silent. */
    private fun change(write: suspend () -> Unit) {
        viewModelScope.launch {
            if (attempt { write() }.isFailure) {
                state.update { it.copy(message = SubscriptionMessage.CHANGE_FAILED) }
            }
        }
    }

    private fun SubscriptionItem.toRow(
        refreshing: Boolean,
        language: LanguageSpec,
    ): SubscriptionRow {
        val now = clock.now().toEpochMilliseconds()
        return SubscriptionRow(
            id = id,
            name = name,
            url = url,
            enabled = enabled,
            downloaded = lastFetchedAtEpochMillis != null,
            refreshing = refreshing,
            health = SubscriptionHealthRules.of(this, now),
            details = SubscriptionHealthRules.details(this, language, zone(), now),
        )
    }

    private fun message(
        outcome: SubscriptionOutcome,
        done: SubscriptionMessage,
    ): SubscriptionMessage =
        when (outcome) {
            SubscriptionOutcome.DONE -> done
            SubscriptionOutcome.INVALID_ADDRESS -> SubscriptionMessage.INVALID_ADDRESS
            SubscriptionOutcome.ALREADY_SUBSCRIBED -> SubscriptionMessage.ALREADY_SUBSCRIBED
            SubscriptionOutcome.NETWORK_NOT_ALLOWED -> SubscriptionMessage.NETWORK_NOT_ALLOWED
            SubscriptionOutcome.FAILED -> SubscriptionMessage.FAILED
        }
}
