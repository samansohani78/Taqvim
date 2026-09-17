/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * The available calendars of [settings] and month arithmetic in the primary one (T-901). Month offsets count whole
 * months of the primary calendar from the month containing today.
 */
class AgendaCalendars(
    val settings: AgendaSettings,
) {
    /** Arithmetic of the available calendars in the user's order; Gregorian when none of them is available. */
    val arithmetic: List<CalendarArithmetic> =
        settings.calendars
            .distinct()
            .map { IslamicCalendarSelection.arithmeticFor(it, settings.islamicVariant, settings.islamicOverrides) }
            .ifEmpty { listOf(GregorianCalendarSystem) }

    private val primary: CalendarArithmetic = arithmetic.first()

    /** [day] in every available calendar, primary first. */
    fun datesOf(day: Jdn): List<CalendarDate> = arithmetic.map { it.fromJdn(day) }

    /** The first day of the month [offset] months after the month containing [today]. */
    fun monthStartAt(
        today: Jdn,
        offset: Int,
    ): Jdn {
        val date = primary.fromJdn(today)
        return primary.toJdn(primary.addMonths(primary.date(date.year, date.month, 1), offset))
    }

    /** Every day of the months [first]‥[last] (offsets from the month of [today]). */
    fun range(
        today: Jdn,
        first: Int,
        last: Int,
    ): JdnRange = JdnRange(monthStartAt(today, first), monthStartAt(today, last + 1) - 1)
}

/** [AgendaTodaySource] reading [provider] every [interval], so a new day (or time zone) shows within [interval]. */
class TickingAgendaTodaySource(
    private val provider: TodayProvider,
    private val interval: Duration = 1.minutes,
) : AgendaTodaySource {
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
}
