/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import androidx.datastore.core.DataStore
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
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
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
                    OfficialCatalog::class,
                    // Built inline from the three bound event sources in eventsDataModule.
                    EventInputs::class,
                ),
            // The scheduler collects every AlarmSource and AlarmDelivery with getAll(), which verify() cannot follow;
            // the test below checks that the athan's prayer source and delivery are among them (T-604, T-1102).
            injections = injectedParameters(definition<RescheduleCoordinator>(List::class)),
        )
    }

    @Test
    fun `the athan is the scheduler's prayer alarm source and delivery`(): Unit =
        runTest {
            val application =
                koinApplication {
                    modules(
                        module {
                            single { repositoryOf(UserPreferences.defaultsFor("fa")) }
                            single {
                                AthanAlarms(get(), AthanDeliveryLog { _, _ -> true }, AthanPlaybackStarter { true })
                            }
                        },
                        athanAlarmPortsModule,
                    )
                }
            val koin = application.koin

            koin.getAll<AlarmSource>().map { it.kind } shouldBe listOf(AlarmKind.PRAYER)
            koin.getAll<AlarmDelivery>().map { it.kind } shouldBe listOf(AlarmKind.PRAYER)
            // Without a chosen place no athan is planned.
            koin.get<AlarmSource>().upcomingAlarms(Instant.parse("2026-03-21T00:00:00Z")) shouldBe emptyList()
            application.close()
        }
}
