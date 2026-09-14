/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant

/** The prayers that can have an athan, in the order of the day. */
enum class AthanPrayer {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

/** Whether the athan of one prayer sounds, [gapMinutes] after the prayer time (negative is before). */
data class AthanAlertRule(
    val enabled: Boolean,
    val gapMinutes: Int,
)

/** Everything that decides when athans are due (T-1101 settings, T-1502 place, A-10 method). */
data class AthanPlanSettings(
    /** One rule per [AthanPrayer]; a missing prayer has no athan. */
    val alerts: Map<AthanPrayer, AthanAlertRule>,
    val place: Coordinates,
    /** Civil time zone of the place. */
    val timeZone: TimeZone,
    val prayer: PrayerSettings,
    /** Whether athan times use Iran Standard Time (UTC+03:30) instead of the rules of [timeZone]. */
    val useIranTime: Boolean,
)

/** One athan: [prayer] of the civil [day] sounds at [at]. */
data class PlannedAthan(
    val prayer: AthanPrayer,
    val day: Jdn,
    val at: Instant,
)

/** When athans are due (T-1102 on A-10); pure, so the alarm source and the delivery agree on every instant. */
object AthanPlanner {
    /** Iran Standard Time, used when [AthanPlanSettings.useIranTime] is on. */
    val IRAN_STANDARD_TIME: TimeZone = FixedOffsetTimeZone(UtcOffset(hours = 3, minutes = 30))

    /** Days after today that are planned; alarms are replanned each time one fires. */
    const val DAYS_AHEAD: Int = 2

    private const val NOON_HOUR = 12
    private const val SECONDS_PER_MINUTE = 60

    /** Enabled athans strictly after [now], in time order, from yesterday (negative gaps) to [DAYS_AHEAD] days on. */
    fun upcoming(
        now: Instant,
        settings: AthanPlanSettings,
    ): List<PlannedAthan> {
        val zone = zoneOf(settings)
        val today = now.toJdn(zone)
        return (-1..DAYS_AHEAD)
            .flatMap { offset -> athansOf(today + offset, settings, zone) }
            .filter { it.at > now }
            .sortedBy { it.at }
    }

    /** The planned athan that sounds exactly at [instant] (the first prayer of the day when several coincide). */
    fun at(
        instant: Instant,
        settings: AthanPlanSettings,
    ): PlannedAthan? {
        val zone = zoneOf(settings)
        val day = instant.toJdn(zone)
        return (-1..1).flatMap { offset -> athansOf(day + offset, settings, zone) }.firstOrNull { it.at == instant }
    }

    /** The zone athan times are expressed in. */
    fun zoneOf(settings: AthanPlanSettings): TimeZone =
        if (settings.useIranTime) IRAN_STANDARD_TIME else settings.timeZone

    private fun athansOf(
        day: Jdn,
        settings: AthanPlanSettings,
        zone: TimeZone,
    ): List<PlannedAthan> {
        val offset = offsetMinutes(day, zone)
        val times =
            when (val result = PrayerTimesCalculator.calculate(day, settings.place, offset, settings.prayer)) {
                is PrayerTimesResult.Available -> result.times
                is PrayerTimesResult.Unavailable -> return emptyList()
            }
        return prayerInstants(day, times, zone).mapNotNull { (prayer, base) ->
            settings.alerts[prayer]
                ?.takeIf { it.enabled }
                ?.let { PlannedAthan(prayer, day, base + it.gapMinutes.minutes) }
        }
    }

    /** Defined prayer times of [day] as instants, increasing: a time not after the previous one is on the next date. */
    private fun prayerInstants(
        day: Jdn,
        times: PrayerTimes,
        zone: TimeZone,
    ): List<Pair<AthanPrayer, Instant>> {
        val date = day.toLocalDate()
        val instants = mutableListOf<Pair<AthanPrayer, Instant>>()
        timesOf(times).forEach { (prayer, time) ->
            val instant = date.atTime(time.hour, time.minute).toInstant(zone)
            val last = instants.lastOrNull()?.second
            instants += prayer to if (last != null && instant <= last) instant + 1.days else instant
        }
        return instants
    }

    private fun timesOf(times: PrayerTimes): List<Pair<AthanPrayer, MinuteOfDay>> =
        listOfNotNull(
            times.fajr?.let { AthanPrayer.FAJR to it },
            AthanPrayer.DHUHR to times.dhuhr,
            AthanPrayer.ASR to times.asr,
            times.maghrib?.let { AthanPrayer.MAGHRIB to it },
            times.isha?.let { AthanPrayer.ISHA to it },
        )

    private fun offsetMinutes(
        day: Jdn,
        zone: TimeZone,
    ): Int {
        val noon = day.toLocalDate().atTime(NOON_HOUR, 0).toInstant(zone)
        return zone.offsetAt(noon).totalSeconds / SECONDS_PER_MINUTE
    }
}
