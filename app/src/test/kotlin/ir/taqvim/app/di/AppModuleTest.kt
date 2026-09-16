/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import ir.taqvim.data.database.AlarmKind
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.OfficialCatalog
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.scheduler.AlarmDelivery
import ir.taqvim.data.scheduler.AlarmSource
import ir.taqvim.data.scheduler.RescheduleCoordinator
import ir.taqvim.feature.notification.AthanAlarms
import ir.taqvim.feature.notification.AthanDeliveryLog
import ir.taqvim.feature.notification.AthanPlaybackStarter
import ir.taqvim.feature.notification.CalculatorOfficialEventSchedule
import ir.taqvim.feature.notification.ReminderAlarms
import ir.taqvim.feature.notification.ReminderSetup
import java.io.File
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

class AppModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `dependency graph is complete`() {
        appModule.verify(
            // Platform objects and constructor parameters with defaults that the graph does not bind on purpose.
            extraTypes =
                listOf(
                    Context::class,
                    DataStore::class,
                    Flow::class,
                    Function0::class,
                    Function1::class,
                    CoroutineDispatcher::class,
                    Duration::class,
                    List::class,
                    Set::class,
                    OfficialCatalog::class,
                    // Built inline from the three bound event sources in eventsDataModule.
                    EventInputs::class,
                    // Widget registrations (T-1201…) are constant values holding their receiver and widget classes.
                    Class::class,
                    // Supplied per navigation entry by koinViewModel (the event editor's draft, B11).
                    SavedStateHandle::class,
                    // The restore journal directory is built inline from the app's no-backup directory (B09).
                    File::class,
                ),
            // The scheduler collects every AlarmSource and AlarmDelivery with getAll(), which verify() cannot follow;
            // the test below checks that the athan's prayer source and delivery are among them (T-604, T-1102).
            injections = injectedParameters(definition<RescheduleCoordinator>(List::class)),
        )
    }

    @Test
    fun `the athan and reminders are the scheduler's alarm sources and deliveries`(): Unit =
        runTest {
            val application =
                koinApplication {
                    modules(
                        module {
                            single { repositoryOf(UserPreferences.defaultsFor("fa")) }
                            single {
                                AthanAlarms(get(), AthanDeliveryLog { _, _ -> true }, AthanPlaybackStarter { true })
                            }
                            single { ReminderAlarms({ emptySetup() }, { true }, { true }) }
                        },
                        athanAlarmPortsModule,
                        module {
                            single<AlarmSource>(named(REMINDER_ALARMS)) {
                                val alarms = get<ReminderAlarms>()
                                ReminderAlarmSource { alarms.upcoming(it) }
                            }
                            single<AlarmDelivery>(named(REMINDER_ALARMS)) {
                                val alarms = get<ReminderAlarms>()
                                ReminderAlarmDelivery { id, at -> alarms.onAlarm(id, at) }
                            }
                        },
                    )
                }
            val koin = application.koin
            val kinds = setOf(AlarmKind.PRAYER, AlarmKind.REMINDER)

            koin.getAll<AlarmSource>().map { it.kind }.toSet() shouldBe kinds
            koin.getAll<AlarmDelivery>().map { it.kind }.toSet() shouldBe kinds
            // Without a chosen place no athan is planned.
            val prayers = koin.get<AlarmSource>(named(PRAYER_ALARMS))
            prayers.upcomingAlarms(Instant.parse("2026-03-21T00:00:00Z")) shouldBe emptyList()
            application.close()
        }

    private fun emptySetup(): ReminderSetup =
        ReminderSetup(emptyList(), emptyList(), CalculatorOfficialEventSchedule(emptyList(), "fa"), TimeZone.UTC)
}
