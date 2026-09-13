/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.OfficialCatalog
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.Test
import org.koin.core.annotation.KoinExperimentalAPI
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
        )
    }
}
