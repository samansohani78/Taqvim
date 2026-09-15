/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.core.ics.IcsParseResult
import ir.taqvim.core.ics.IcsReader
import ir.taqvim.data.database.IcsSubscriptionDao
import ir.taqvim.data.database.IcsSubscriptionEntity
import ir.taqvim.data.devicecalendar.InstantWindow
import java.net.HttpURLConnection
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

/** Why a subscription could not be refreshed. */
sealed interface RefreshError {
    data class Fetch(
        val error: FetchError,
    ) : RefreshError

    /** The feed is not a readable iCalendar stream; the cached events are kept. */
    data object Unreadable : RefreshError

    data object InsecureUrl : RefreshError

    data object InvalidUrl : RefreshError

    data object NotFound : RefreshError
}

/** Outcome of refreshing one subscription. */
sealed interface RefreshOutcome {
    val subscriptionId: Long

    /** The feed changed: [occurrences] cached rows, with [problems] reported by the reader. */
    data class Updated(
        override val subscriptionId: Long,
        val occurrences: Int,
        val problems: Int,
    ) : RefreshOutcome

    data class Unchanged(
        override val subscriptionId: Long,
    ) : RefreshOutcome

    data class Failed(
        override val subscriptionId: Long,
        val error: RefreshError,
    ) : RefreshOutcome
}

/**
 * When subscriptions are refreshed (T-1003): a subscription is due [interval] after its last answered request, never
 * more often than [minimumInterval] (WorkManager's shortest period). Conditional requests are sent only while the last
 * full download is younger than [revalidateFor], so the cached expansion ([keepPast] before to [expandAhead] after the
 * download) never falls behind by more than that.
 */
data class SubscriptionRefreshPolicy(
    val minimumInterval: Duration = 15.minutes,
    val revalidateFor: Duration = 7.days,
    val keepPast: Duration = 31.days,
    val expandAhead: Duration = 400.days,
) {
    fun interval(subscription: IcsSubscriptionEntity): Duration =
        maxOf(subscription.refreshIntervalMinutes.minutes, minimumInterval)

    fun isDue(
        subscription: IcsSubscriptionEntity,
        now: Instant,
    ): Boolean {
        val lastChecked = subscription.lastCheckedAtEpochMillis?.let(Instant::fromEpochMilliseconds)
        return subscription.enabled && (lastChecked == null || now - lastChecked >= interval(subscription))
    }

    fun validatorsFor(
        subscription: IcsSubscriptionEntity,
        now: Instant,
    ): HttpValidators {
        val lastFetched = subscription.lastFetchedAtEpochMillis?.let(Instant::fromEpochMilliseconds)
        val recent = lastFetched != null && now - lastFetched < revalidateFor
        return if (recent) HttpValidators(subscription.etag, subscription.lastModified) else HttpValidators()
    }

    fun window(now: Instant): InstantWindow =
        InstantWindow((now - keepPast).toEpochMilliseconds(), (now + expandAhead).toEpochMilliseconds())

    /** Period of the refresh work: the shortest interval of enabled subscriptions, or `null` when none is enabled. */
    fun workPeriod(subscriptions: List<IcsSubscriptionEntity>): Duration? =
        subscriptions.filter { it.enabled }.minOfOrNull(::interval)

    /** Whether [outcomes] include a failure worth retrying soon: a timeout, a network error, 429 or a server error. */
    fun shouldRetry(outcomes: List<RefreshOutcome>): Boolean =
        outcomes.any { outcome ->
            when (val error = ((outcome as? RefreshOutcome.Failed)?.error as? RefreshError.Fetch)?.error) {
                FetchError.Timeout, FetchError.Network -> {
                    true
                }

                is FetchError.HttpStatus -> {
                    error.code == TOO_MANY_REQUESTS ||
                        error.code >= HttpURLConnection.HTTP_INTERNAL_ERROR
                }

                else -> {
                    false
                }
            }
        }

    private companion object {
        const val TOO_MANY_REQUESTS = 429
    }
}

/** Downloads subscribed feeds and replaces their cached occurrences (T-1003). */
class SubscriptionRefresher(
    private val dao: IcsSubscriptionDao,
    private val fetcher: IcsFetcher,
    private val clock: Clock,
    private val zone: () -> TimeZone,
    private val policy: SubscriptionRefreshPolicy = SubscriptionRefreshPolicy(),
) {
    /** Refreshes every due subscription. */
    suspend fun refreshDue(): List<RefreshOutcome> {
        val now = clock.now()
        return dao
            .observeSubscriptions()
            .first()
            .filter { policy.isDue(it, now) }
            .map { refresh(it, now) }
    }

    /** Refreshes subscription [id] now, due or not (e.g. when it is added or the user asks). */
    suspend fun refresh(id: Long): RefreshOutcome =
        dao.getSubscription(id)?.let { refresh(it, clock.now()) } ?: RefreshOutcome.Failed(id, RefreshError.NotFound)

    private suspend fun refresh(
        subscription: IcsSubscriptionEntity,
        now: Instant,
    ): RefreshOutcome {
        val url =
            when (val normalized = SubscriptionUrls.normalize(subscription.url)) {
                is SubscriptionUrl.Valid -> normalized.url
                SubscriptionUrl.Insecure -> return RefreshOutcome.Failed(subscription.id, RefreshError.InsecureUrl)
                SubscriptionUrl.Invalid -> return RefreshOutcome.Failed(subscription.id, RefreshError.InvalidUrl)
            }
        val checked = subscription.copy(lastCheckedAtEpochMillis = now.toEpochMilliseconds())
        return when (val result = fetcher.fetch(url, policy.validatorsFor(subscription, now))) {
            is FetchResult.Modified -> {
                store(checked, result, now)
            }

            FetchResult.NotModified -> {
                dao.updateSubscription(checked)
                RefreshOutcome.Unchanged(subscription.id)
            }

            is FetchResult.Failed -> {
                RefreshOutcome.Failed(subscription.id, RefreshError.Fetch(result.error))
            }
        }
    }

    private suspend fun store(
        subscription: IcsSubscriptionEntity,
        result: FetchResult.Modified,
        now: Instant,
    ): RefreshOutcome =
        when (val parsed = IcsReader.read(result.body)) {
            is IcsParseResult.Failure -> {
                RefreshOutcome.Failed(subscription.id, RefreshError.Unreadable)
            }

            is IcsParseResult.Success -> {
                val expander = IcsOccurrenceExpander(zone())
                val window = policy.window(now)
                val rows =
                    expander
                        .expandAll(subscription.id, parsed.calendar.events, window)
                        .distinctBy { it.uid to it.startEpochMillis }
                dao.replaceEvents(subscription.id, rows)
                dao.updateSubscription(
                    subscription.copy(
                        lastFetchedAtEpochMillis = now.toEpochMilliseconds(),
                        etag = result.validators.etag,
                        lastModified = result.validators.lastModified,
                    ),
                )
                RefreshOutcome.Updated(subscription.id, rows.size, parsed.warnings.size)
            }
        }
}
