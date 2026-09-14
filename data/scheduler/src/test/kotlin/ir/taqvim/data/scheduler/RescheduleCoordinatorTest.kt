/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.scheduler

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.preferences.ThemeMode
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-604 (U): reschedule events and fired alarms drive the scheduler through the matrix. */
class RescheduleCoordinatorTest {
    private val clock = FakeClock(Instant.parse("2026-09-13T12:00:00Z"))
    private val start = clock.now()
    private val store = FakeAlarmStore()
    private val alarmClock = FakeAlarmClock()
    private val scheduler = AlarmScheduler(store, alarmClock, clock)
    private val prayers = FakeSource(AlarmKind.PRAYER, listOf(start + 1.hours, start + 2.hours))
    private val prayerDelivery = FakeDelivery(AlarmKind.PRAYER)
    private val reminderDelivery = FakeDelivery(AlarmKind.REMINDER)
    private val coordinator =
        RescheduleCoordinator(scheduler, listOf(prayers), listOf(prayerDelivery, reminderDelivery), clock)

    @Test
    fun `bursts of alarm input changes recompute their kinds once after a quiet period`(): Unit =
        runTest {
            val handled = mutableListOf<RescheduleEvent>()
            val events =
                object : SchedulerEvents {
                    override suspend fun handle(event: RescheduleEvent) {
                        handled += event
                    }

                    override suspend fun onAlarmFired(id: Long) = Unit
                }
            val changes =
                flow {
                    emit(setOf("reminders"))
                    delay(100.milliseconds)
                    emit(setOf("personal_events"))
                    delay(5.seconds)
                    emit(setOf("official_reminders"))
                }

            AlarmInputWatcher(changes, setOf(AlarmKind.REMINDER), events).watch()

            handled shouldBe List(2) { RescheduleEvent.AlarmInputsChanged(setOf(AlarmKind.REMINDER)) }
        }

    @Test
    fun `boot restores stored alarms and recomputes kinds that have sources`(): Unit =
        runTest {
            val reminder = AlarmKey(AlarmKind.REMINDER, 4, start + 30.minutes)
            scheduler.replace(AlarmKind.REMINDER, listOf(reminder))
            alarmClock.registered.clear()

            coordinator.handle(RescheduleEvent.BootCompleted)

            alarmClock.registered.values.map { it.triggerAt } shouldContainExactlyInAnyOrder
                listOf(start + 30.minutes, start + 1.hours, start + 2.hours)
            prayers.queries shouldBe 1
        }

    @Test
    fun `clock and time-zone changes recompute without registering unchanged alarms again`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.PRAYER)
            val sets = alarmClock.setCalls

            coordinator.handle(RescheduleEvent.TimeZoneChanged)
            coordinator.handle(RescheduleEvent.TimeChanged)

            prayers.queries shouldBe 3
            alarmClock.setCalls shouldBe sets
        }

    @Test
    fun `a permission change re-registers without asking the sources`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.PRAYER)
            val sets = alarmClock.setCalls

            coordinator.handle(RescheduleEvent.ExactAlarmPermissionChanged)

            prayers.queries shouldBe 1
            alarmClock.setCalls shouldBe sets + 2
        }

    @Test
    fun `preference changes recompute only when they move alarm times`(): Unit =
        runTest {
            val base = UserPreferences.defaultsFor("fa")
            val method = PrayerMethod.entries.first { it != base.prayerMethod }

            coordinator.handle(RescheduleEvent.PreferencesChanged(base, base.copy(themeMode = ThemeMode.DARK)))
            prayers.queries shouldBe 0
            coordinator.handle(RescheduleEvent.PreferencesChanged(base, base.copy(prayerMethod = method)))
            prayers.queries shouldBe 1
        }

    @Test
    fun `an alarm fired on time is delivered to its kind and the next one is scheduled`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.PRAYER)
            val first = store.alarms().first()
            clock.advanceBy(1.hours)

            coordinator.onAlarmFired(first.id)

            prayerDelivery.delivered shouldBe listOf(first)
            reminderDelivery.delivered.shouldBeEmpty()
            store.alarms().map { it.toKey().triggerAt } shouldBe listOf(start + 2.hours)
        }

    @Test
    fun `late, early and unknown alarms are not delivered`(): Unit =
        runTest {
            coordinator.recompute(AlarmKind.PRAYER)
            val first = store.alarms().first()

            coordinator.onAlarmFired(first.id)
            coordinator.onAlarmFired(12_345)
            prayers.queries shouldBe 1

            clock.advanceBy(2.hours + 20.minutes)
            coordinator.onAlarmFired(first.id)
            prayers.queries shouldBe 2

            prayerDelivery.delivered.shouldBeEmpty()
            store.alarms().shouldBeEmpty()
        }

    @Test
    fun `the preference watcher reports only changes that move alarm times`(): Unit =
        runTest {
            val base = UserPreferences.defaultsFor("fa")
            val themed = base.copy(themeMode = ThemeMode.BLACK)
            val method = themed.copy(prayerMethod = PrayerMethod.entries.first { it != base.prayerMethod })
            val events = RecordingEvents()

            PreferenceChangeWatcher(flowOf(base, base, themed, method), events).watch()

            events.events shouldBe listOf(RescheduleEvent.PreferencesChanged(themed, method))
        }
}
