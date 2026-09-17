/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.Immutable
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The state a subscription is in (F03), most urgent first. */
enum class SubscriptionHealth {
    /** Turned off by the user; its cached events are hidden and it is not refreshed. */
    PAUSED,

    /** The last refresh failed; the cached events, if any, are from an earlier download. */
    FAILED,

    /** Never downloaded, so the calendar is empty. */
    NEVER_FETCHED,

    /** No answered request for more than two refresh periods, so the cache may be old. */
    STALE,

    /** Downloaded and checked on time. */
    OK,
}

/** A moment shown as a [date] and a [time] of day in the user's language. */
@Immutable
data class SubscriptionMoment(
    val date: String,
    val time: String,
)

/** What the details section of a subscription shows (F03); texts are already in the user's language and digits. */
@Immutable
data class SubscriptionDetails(
    val lastSuccess: SubscriptionMoment? = null,
    val lastCheck: SubscriptionMoment? = null,
    /** When the feed is next due; `null` when it is due already (it is checked as soon as the network allows). */
    val nextCheck: SubscriptionMoment? = null,
    val eventCount: String = "",
    val cachedFrom: String? = null,
    val cachedUntil: String? = null,
    /** How many feed items were ignored or approximated, or `null` when none were. */
    val problems: String? = null,
    val error: SubscriptionError? = null,
    val errorStatus: String? = null,
    val errorAt: SubscriptionMoment? = null,
)

/** Decides a subscription's [SubscriptionHealth] and renders its [SubscriptionDetails]. */
internal object SubscriptionHealthRules {
    /** A subscription is stale after this many refresh periods without an answered request. */
    private const val STALE_PERIODS = 2

    fun of(
        item: SubscriptionItem,
        nowEpochMillis: Long,
    ): SubscriptionHealth {
        val health = item.health
        val checked = health.lastCheckedAtEpochMillis ?: item.lastFetchedAtEpochMillis
        val period = health.refreshIntervalMinutes.minutes.inWholeMilliseconds
        return when {
            !item.enabled -> SubscriptionHealth.PAUSED
            health.error != null -> SubscriptionHealth.FAILED
            item.lastFetchedAtEpochMillis == null || checked == null -> SubscriptionHealth.NEVER_FETCHED
            period > 0 && nowEpochMillis - checked > STALE_PERIODS * period -> SubscriptionHealth.STALE
            else -> SubscriptionHealth.OK
        }
    }

    fun details(
        item: SubscriptionItem,
        language: LanguageSpec,
        zone: TimeZone,
        nowEpochMillis: Long,
    ): SubscriptionDetails {
        val health = item.health

        fun moment(epochMillis: Long?) = epochMillis?.let { moment(it, language, zone) }

        fun day(epochMillis: Long?) = epochMillis?.let { date(it, language, zone) }
        return SubscriptionDetails(
            lastSuccess = moment(item.lastFetchedAtEpochMillis),
            lastCheck = moment(health.lastCheckedAtEpochMillis),
            nextCheck = moment(health.nextCheckAtEpochMillis?.takeIf { item.enabled && it > nowEpochMillis }),
            eventCount = Numerals.format(health.cachedEvents.toLong(), language.numerals),
            cachedFrom = day(health.cachedFromEpochMillis),
            cachedUntil = day(health.cachedUntilEpochMillis),
            problems = health.problemCount.takeIf { it > 0 }?.let { Numerals.format(it.toLong(), language.numerals) },
            error = health.error,
            errorStatus = health.httpStatus?.let { Numerals.format(it.toLong(), language.numerals) },
            errorAt = moment(health.errorAtEpochMillis),
        )
    }

    private fun moment(
        epochMillis: Long,
        language: LanguageSpec,
        zone: TimeZone,
    ): SubscriptionMoment {
        val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
        val clock = "%02d:%02d".format(Locale.ROOT, local.hour, local.minute)
        return SubscriptionMoment(date(epochMillis, language, zone), Numerals.localizeDigits(clock, language.numerals))
    }

    /** The day in the language's first Persian or Gregorian calendar (Gregorian when it has neither), as backups do. */
    private fun date(
        epochMillis: Long,
        language: LanguageSpec,
        zone: TimeZone,
    ): String {
        val day = Instant.fromEpochMilliseconds(epochMillis).toJdn(zone)
        val calendar =
            language.calendars.firstOrNull { it == CalendarSystem.PERSIAN || it == CalendarSystem.GREGORIAN }
        val arithmetic = if (calendar == CalendarSystem.PERSIAN) PersianCalendarSystem else GregorianCalendarSystem
        return DateFormatter.format(arithmetic.fromJdn(day), day.weekday(), language, DateStyle.LONG)
    }
}
