/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/** T-1200 update policy: targeted updates of installed widgets and the wake-up schedule. */
class WidgetUpdatePolicyTest {
    private val policy = WidgetUpdatePolicy()
    private val installed: InstalledWidgetIds =
        mapOf(
            WidgetKind.DATE_1X1 to setOf(1, 2),
            WidgetKind.SCHEDULE to setOf(3),
            WidgetKind.MAP to setOf(4),
            WidgetKind.SUN_ARC to setOf(5),
            WidgetKind.DAY_SUMMARY_2X2 to setOf(6),
        )

    @Test
    fun `each trigger reaches only the widgets that depend on it`() {
        policy.targets(WidgetUpdateTrigger.DayChanged, installed).keys shouldBe
            setOf(WidgetKind.DATE_1X1, WidgetKind.SCHEDULE, WidgetKind.SUN_ARC, WidgetKind.DAY_SUMMARY_2X2)
        policy.targets(WidgetUpdateTrigger.EventsChanged, installed) shouldBe
            mapOf(WidgetKind.SCHEDULE to setOf(3), WidgetKind.DAY_SUMMARY_2X2 to setOf(6))
        policy.targets(WidgetUpdateTrigger.MinuteTick, installed).keys shouldBe
            setOf(WidgetKind.MAP, WidgetKind.SUN_ARC)
        policy.targets(WidgetUpdateTrigger.PrayerTimeReached, installed) shouldBe
            mapOf(WidgetKind.DAY_SUMMARY_2X2 to setOf(6))
        policy.targets(WidgetUpdateTrigger.ConfigChanged(2), installed) shouldBe mapOf(WidgetKind.DATE_1X1 to setOf(2))
        policy.targets(WidgetUpdateTrigger.ConfigChanged(99), installed) shouldBe emptyMap()
        policy
            .targets(
                WidgetUpdateTrigger.PreferencesChanged(setOf(WidgetDependency.LOCATION)),
                installed,
            ).keys shouldBe
            setOf(WidgetKind.MAP, WidgetKind.SUN_ARC, WidgetKind.DAY_SUMMARY_2X2)
        policy.targets(WidgetUpdateTrigger.PreferencesChanged(setOf(WidgetDependency.APPEARANCE)), installed) shouldBe
            installed
        policy.targets(WidgetUpdateTrigger.PreferencesChanged(emptySet()), installed) shouldBe emptyMap()
        policy.targets(WidgetUpdateTrigger.Everything, installed) shouldBe installed
        policy.targets(WidgetUpdateTrigger.Everything, emptyMap()) shouldBe emptyMap()
    }

    @Test
    fun `targets never leave the installed widgets and merge across triggers`(): Unit =
        runBlocking {
            val widgets = Arb.map(Arb.enum<WidgetKind>(), Arb.set(Arb.int(1..20), 0..3), maxSize = 12)
            val triggers =
                Arb.set(
                    Arb.element(
                        WidgetUpdateTrigger.DayChanged,
                        WidgetUpdateTrigger.PrayerTimeReached,
                        WidgetUpdateTrigger.MinuteTick,
                        WidgetUpdateTrigger.EventsChanged,
                        WidgetUpdateTrigger.Everything,
                        WidgetUpdateTrigger.ConfigChanged(7),
                        WidgetUpdateTrigger.PreferencesChanged(setOf(WidgetDependency.EVENTS)),
                    ),
                    0..4,
                )
            checkAll(PropertyTesting.iterations, widgets, triggers) { installedWidgets, due ->
                val merged = policy.targets(due, installedWidgets)
                merged.forEach { (kind, ids) ->
                    ids.isNotEmpty().shouldBeTrue()
                    installedWidgets.getValue(kind).containsAll(ids).shouldBeTrue()
                }
                val union = due.flatMap { policy.targets(it, installedWidgets).entries }
                merged.values.sumOf { it.size } shouldBe
                    union.groupBy { it.key }.values.sumOf { entries -> entries.flatMap { it.value }.toSet().size }
            }
        }

    @Test
    fun `the next day starts at local midnight, or at the first instant of a day whose midnight is skipped`() {
        fun next(
            now: String,
            zone: String,
        ) = WidgetUpdatePolicy.nextMidnight(Instant.parse(now), TimeZone.of(zone))

        next("2026-09-14T10:00:00Z", "Asia/Tehran") shouldBe Instant.parse("2026-09-14T20:30:00Z")
        next("2026-09-14T20:30:00Z", "Asia/Tehran") shouldBe Instant.parse("2026-09-15T20:30:00Z")
        // Brazil began DST at 00:00 on 2018-11-04, so that day started at 01:00 (−02:00).
        next("2018-11-03T15:00:00Z", "America/Sao_Paulo") shouldBe Instant.parse("2018-11-04T03:00:00Z")
        // Berlin's 23-hour day: 2026-03-29 starts at 00:00 CET and ends at 00:00 CEST.
        next("2026-03-28T12:00:00Z", "Europe/Berlin") shouldBe Instant.parse("2026-03-28T23:00:00Z")
        next("2026-03-29T10:00:00Z", "Europe/Berlin") shouldBe Instant.parse("2026-03-29T22:00:00Z")
        // Samoa skipped 30 December 2011 outright, moving west of the date line: the day after the 29th is the
        // 31st, 12 hours later. A widget there must wake at the start of the 31st and not at a date that never
        // happened. Pinned here because the property test only draws this day by luck (CI 38243236904).
        next("2011-12-29T22:00:00Z", "Pacific/Apia") shouldBe Instant.parse("2011-12-30T10:00:00Z")
        // Tehran's spring-forward skips midnight itself: 2016-03-21 begins at 01:00 local, not 00:00.
        next("2016-03-20T12:00:00Z", "Asia/Tehran") shouldBe Instant.parse("2016-03-20T20:30:00Z")
    }

    @Test
    fun `the next wake-up is the first instant of a later local day, within 25 hours`(): Unit =
        runBlocking {
            val zones = listOf("Asia/Tehran", "Europe/Berlin", "America/Sao_Paulo", "Asia/Kathmandu", "Pacific/Apia")
            val instants =
                Arb.long(
                    Instant
                        .parse(
                            "1990-01-01T00:00:00Z",
                        ).epochSeconds..Instant.parse("2040-01-01T00:00:00Z").epochSeconds,
                )
            checkAll(PropertyTesting.iterations, instants, Arb.element(zones)) { seconds, zoneId ->
                val zone = TimeZone.of(zoneId)
                val now = Instant.fromEpochSeconds(seconds)
                val next = WidgetUpdatePolicy.nextMidnight(now, zone)
                (next > now && next - now <= 25.hours).shouldBeTrue()
                // What must hold is that `next` is the *first instant of a later local day*. Two things that
                // sound equivalent are not, and both were asserted here before and are false:
                //
                //  - "the local date is today + 1" — Samoa skipped 30 December 2011 crossing the date line, so in
                //    Pacific/Apia the day after the 29th is the 31st. This is what failed CI 38243236904, after
                //    passing for as long as no run happened to draw that one day in fifty years;
                //  - "the local time is 00:00" — Tehran's spring-forward skips midnight, so 2016-03-21 begins at
                //    01:00. A day does not always start at midnight.
                //
                // The invariant below says it without saying either: a later local date, and one second earlier
                // is still today, so no day boundary was stepped over.
                val today = now.toLocalDateTime(zone).date
                (next.toLocalDateTime(zone).date > today).shouldBeTrue()
                (next - 1.seconds).toLocalDateTime(zone).date shouldBe today
            }
        }

    @Test
    fun `minute boundaries`() {
        WidgetUpdatePolicy.nextMinute(Instant.parse("2026-09-14T10:00:00Z")) shouldBe
            Instant.parse("2026-09-14T10:01:00Z")
        WidgetUpdatePolicy.nextMinute(Instant.parse("2026-09-14T10:00:59.5Z")) shouldBe
            Instant.parse("2026-09-14T10:01:00Z")
        WidgetUpdatePolicy.nextMinute(Instant.fromEpochSeconds(-30)) shouldBe Instant.fromEpochSeconds(0)
    }

    @Test
    fun `the wake-up is the earliest reason installed widgets have`() {
        val now = Instant.parse("2026-09-14T10:00:30Z")
        val midnight = Instant.parse("2026-09-14T20:30:00Z")
        val maghrib = Instant.parse("2026-09-14T15:12:00Z")

        fun wake(
            kinds: Set<WidgetKind>,
            prayer: Instant? = maghrib,
        ) = policy.nextWakeUp(now, WidgetTimeline(WidgetSamples.tehran, prayer), kinds)

        wake(setOf(WidgetKind.DATE_1X1)) shouldBe WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged))
        wake(setOf(WidgetKind.MOON)) shouldBe WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged))
        wake(setOf(WidgetKind.DAY_SUMMARY_2X2)) shouldBe
            WidgetWakeUp(maghrib, setOf(WidgetUpdateTrigger.PrayerTimeReached))
        wake(setOf(WidgetKind.DAY_SUMMARY_2X2), prayer = null) shouldBe
            WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged))
        wake(setOf(WidgetKind.DAY_SUMMARY_2X2), prayer = now) shouldBe
            WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged))
        wake(setOf(WidgetKind.DAY_SUMMARY_2X2), prayer = midnight) shouldBe
            WidgetWakeUp(midnight, setOf(WidgetUpdateTrigger.DayChanged, WidgetUpdateTrigger.PrayerTimeReached))
        wake(setOf(WidgetKind.MAP, WidgetKind.DATE_1X1)) shouldBe
            WidgetWakeUp(Instant.parse("2026-09-14T10:01:00Z"), setOf(WidgetUpdateTrigger.MinuteTick))
        wake(emptySet()).shouldBeNull()
    }
}
