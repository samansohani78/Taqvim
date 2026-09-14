/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.model.Jdn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** [TodaySource] that reads [provider] every [interval], so a new day (or a new time zone) shows within [interval]. */
class TickingTodaySource(
    private val provider: TodayProvider,
    private val interval: Duration = DEFAULT_INTERVAL,
) : TodaySource {
    init {
        require(interval.isPositive()) { "interval must be positive (was $interval)" }
    }

    override fun today(): Flow<Jdn> =
        flow {
            while (true) {
                emit(provider.today())
                delay(interval)
            }
        }.distinctUntilChanged()

    companion object {
        val DEFAULT_INTERVAL: Duration = 1.minutes
    }
}

/** [NowSource] from [clock]: the time now, then again at every following minute boundary. */
class MinuteNowSource(
    private val clock: Clock,
) : NowSource {
    override fun now(): Flow<Instant> =
        flow {
            while (true) {
                val now = clock.now()
                emit(now)
                delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
            }
        }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/** Searches events for the calendar screen; blank queries have no results and do not reach [source]. */
class SearchEventsUseCase(
    private val source: EventSearchSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend operator fun invoke(query: String): List<EventSearchResult> {
        val text = query.trim()
        return if (text.isEmpty()) emptyList() else withContext(dispatcher) { source.search(text, LIMIT) }
    }

    companion object {
        /** Results shown under the search bar. */
        const val LIMIT: Int = 20
    }
}

/** The grid days of the months [prefetch] either side of the month [offset] months from [today], in offset order. */
internal fun CalendarMonthSource.monthEvents(
    today: Jdn,
    calendars: CalendarCalendars,
    offset: Int,
    prefetch: Int,
): Flow<ImmutableList<MonthEvents>> {
    val weekStart = calendars.settings.weekStart
    val pages =
        (offset - prefetch..offset + prefetch).map { page ->
            val monthStart = calendars.monthStartAt(today, page)
            days(MonthLayout.gridDays(monthStart, weekStart)).map { MonthEvents(page, it.toImmutableList()) }
        }
    return combine(pages) { it.toList().toImmutableList() }
}
