/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.database.ScheduledAlarmEntity
import ir.taqvim.data.preferences.AthanAlert
import ir.taqvim.data.preferences.AthanPrayer
import ir.taqvim.data.preferences.AthanSound
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.AlarmClock
import ir.taqvim.data.scheduler.AlarmKey
import ir.taqvim.data.scheduler.AlarmScheduler
import ir.taqvim.data.scheduler.AlarmStore
import ir.taqvim.data.scheduler.DeliveryOutcome
import ir.taqvim.data.scheduler.snoozedFrom
import ir.taqvim.feature.notification.AlarmDeliveryResult
import ir.taqvim.feature.notification.AthanAlarms
import ir.taqvim.feature.notification.AthanAlertRule
import ir.taqvim.feature.notification.AthanPlayback
import ir.taqvim.feature.notification.AthanPrayer as PlannedPrayer
import ir.taqvim.feature.notification.AthanRequest
import ir.taqvim.feature.notification.DeliveryLog
import ir.taqvim.feature.notification.DeliveryState
import ir.taqvim.feature.notification.PlannedAthan
import ir.taqvim.feature.notification.PlannedReminder
import ir.taqvim.feature.notification.ReminderKind
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1102 wiring: the athan setup comes from the preferences, and athans reach the scheduler as prayer alarms. */
class NotificationAdaptersTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val persian = UserPreferences.defaultsFor("fa")
    private val athansOn =
        persian.copy(
            place = tehran,
            athan = persian.athan.copy(alerts = AthanPrayer.entries.associateWith { AthanAlert(true, 0) }),
        )

    @Test
    fun `there is no athan setup without a chosen place`() {
        athanSetup(persian).shouldBeNull()
    }

    @Test
    fun `the setup carries the place, the prayer conventions and the athan settings`() {
        val athan =
            persian.athan.copy(
                alerts = AthanPrayer.entries.associateWith { AthanAlert(it == AthanPrayer.FAJR, -5) },
                sound = AthanSound("content://sounds/2", "Adhan"),
                vibrate = false,
                bypassDndForFajr = true,
                volumePercent = 70,
                useIranTime = true,
            )

        val setup = athanSetup(persian.copy(place = tehran, athan = athan)).shouldNotBeNull()

        setup.plan.place shouldBe tehran.coordinates
        setup.plan.timeZone shouldBe TimeZone.of("Asia/Tehran")
        setup.plan.prayer shouldBe PrayerSettings(method = persian.prayerMethod, asr = persian.asrJuristic)
        setup.plan.useIranTime shouldBe true
        setup.plan.alerts.getValue(PlannedPrayer.FAJR) shouldBe AthanAlertRule(enabled = true, gapMinutes = -5)
        setup.plan.alerts.getValue(PlannedPrayer.ISHA) shouldBe AthanAlertRule(enabled = false, gapMinutes = -5)
        setup.plan.alerts.keys shouldBe PlannedPrayer.entries.toSet()
        setup.playback shouldBe AthanPlayback("content://sounds/2", 70, vibrate = false, bypassDndForFajr = true)
    }

    @Test
    fun `athans become prayer alarms and fired prayer alarms and snoozes play the athan at their instant`(): Unit =
        runTest {
            val now = Instant.parse("2026-03-21T00:00:00Z")
            val log = MemoryDeliveryLog()
            val started = mutableListOf<AthanRequest>()
            val alarms =
                AthanAlarms({ athanSetup(athansOn) }, log, {
                    started += it
                    true
                })
            val fajr = alarms.upcoming(now).first()
            val source = AthanAlarmSource(alarms)

            source.kind shouldBe AlarmKind.PRAYER
            source.upcomingAlarms(now).first() shouldBe AlarmKey(AlarmKind.PRAYER, null, fajr)
            source.keepsSnooze(snooze(AlarmKind.PRAYER_SNOOZE, null, fajr)) shouldBe true
            source.keepsSnooze(snooze(AlarmKind.PRAYER_SNOOZE, null, fajr + 1.minutes)) shouldBe false
            source.keepsSnooze(prayer(fajr)) shouldBe false

            val delivery = AthanAlarmDelivery(alarms)
            delivery.kind shouldBe AlarmKind.PRAYER
            delivery.deliver(prayer(fajr)) shouldBe DeliveryOutcome.DELIVERED
            delivery.deliver(prayer(fajr)) shouldBe DeliveryOutcome.SKIPPED
            delivery.deliver(snooze(AlarmKind.PRAYER_SNOOZE, null, fajr)) shouldBe DeliveryOutcome.DELIVERED
            started.map { it.athan.at } shouldBe listOf(fajr, fajr)

            val refusing = AthanAlarms({ athanSetup(athansOn) }, log, { false })
            AthanAlarmDelivery(refusing).deliver(prayer(fajr + 1.minutes)) shouldBe DeliveryOutcome.SKIPPED
            val next = alarms.upcoming(fajr).first()
            AthanAlarmDelivery(refusing).deliver(prayer(next)) shouldBe DeliveryOutcome.FAILED
            AthanAlarmDelivery(refusing).onGaveUp(prayer(next))
            log.states.values.toList() shouldBe listOf(DeliveryState.DELIVERED, DeliveryState.FAILED)
        }

    @Test
    fun `snoozes are stored in the scheduler with the instant they repeat`(): Unit =
        runTest {
            val clock = FakeClock(Instant.parse("2026-03-21T00:00:00Z"))
            val store = MemoryAlarmStore()
            val scheduler = AlarmScheduler(store, AcceptingAlarmClock(), clock)
            val snoozes = SchedulerSnoozeScheduler(scheduler)
            val planned = clock.now() + 1.minutes
            val later = planned + 10.minutes
            val athan = PlannedAthan(PlannedPrayer.FAJR, Jdn(2_461_121), planned)
            val reminder =
                PlannedReminder(ReminderKind.OFFICIAL, 4, "ir.holiday.nowruz-1", "Nowruz", Jdn(2_461_121), 0, planned)

            snoozes.snoozeAthan(athan, later)
            snoozes.snoozeReminder(reminder, later)

            store.alarms().map { Triple(it.kind, it.sourceId, it.snoozedFrom()) } shouldBe
                listOf(
                    Triple(AlarmKind.PRAYER_SNOOZE, null, planned),
                    Triple(AlarmKind.REMINDER_SNOOZE, -4L, planned),
                )
            store.alarms().map { it.plannedAt() } shouldBe listOf(planned, planned)
            prayer(later).plannedAt() shouldBe later
        }

    @Test
    fun `feature delivery results map to scheduler outcomes`() {
        AlarmDeliveryResult.entries.map { it.toOutcome() } shouldBe
            listOf(DeliveryOutcome.DELIVERED, DeliveryOutcome.SKIPPED, DeliveryOutcome.FAILED)
    }

    private fun prayer(at: Instant): ScheduledAlarmEntity =
        ScheduledAlarmEntity(kind = AlarmKind.PRAYER, sourceId = null, triggerAtEpochMillis = at.toEpochMilliseconds())
}

/** A stored snooze of [kind] for [sourceId] repeating [snoozedFrom] ten minutes later. */
internal fun snooze(
    kind: AlarmKind,
    sourceId: Long?,
    snoozedFrom: Instant,
): ScheduledAlarmEntity =
    ScheduledAlarmEntity(
        kind = kind,
        sourceId = sourceId,
        triggerAtEpochMillis = (snoozedFrom + 10.minutes).toEpochMilliseconds(),
        snoozedFromEpochMillis = snoozedFrom.toEpochMilliseconds(),
    )

/** In-memory [DeliveryLog]. */
internal class MemoryDeliveryLog : DeliveryLog {
    val states = LinkedHashMap<String, DeliveryState>()

    override suspend fun state(key: String): DeliveryState? = states[key]

    override suspend fun record(
        key: String,
        state: DeliveryState,
    ) {
        states[key] = state
    }
}

/** In-memory `scheduled_alarms`. */
private class MemoryAlarmStore : AlarmStore {
    private val rows = mutableListOf<ScheduledAlarmEntity>()

    override suspend fun alarms(): List<ScheduledAlarmEntity> = rows.toList()

    override suspend fun insert(
        key: AlarmKey,
        snoozedFrom: Instant?,
    ): Long {
        val id = rows.size + 1L
        rows +=
            ScheduledAlarmEntity(
                id,
                key.kind,
                key.sourceId,
                key.triggerAt.toEpochMilliseconds(),
                snoozedFromEpochMillis = snoozedFrom?.toEpochMilliseconds(),
            )
        return id
    }

    override suspend fun delete(id: Long) {
        rows.removeAll { it.id == id }
    }

    override suspend fun updateAttempts(
        id: Long,
        attempts: Int,
        retryAt: Instant,
    ) = Unit
}

/** A system alarm service that accepts every alarm. */
private class AcceptingAlarmClock : AlarmClock {
    override fun canScheduleExact(): Boolean = true

    override fun set(
        requestCode: Int,
        triggerAt: Instant,
        exact: Boolean,
    ): Boolean = exact

    override fun cancel(requestCode: Int) = Unit
}
