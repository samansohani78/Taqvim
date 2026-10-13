/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A widget load always finishes (T-1200).
 *
 * `provideGlance` awaits [WidgetStateLoader.load] before it calls `provideContent`, so a load that never returns is
 * not a slow widget: the launcher keeps showing `android:initialLayout` — Glance's loading spinner — and nothing
 * ever replaces it. That is what shipped in v1.3.0, and no test could see it because the whole local layer only
 * ever asked what the loader *returns*, never whether it returns at all.
 *
 * These are the tests that would have failed before the loader was bounded. They are deliberately about a store
 * that goes silent rather than one that throws: a throw was always handled, and `runCatching` around a suspend
 * that never resumes catches nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetLoadNeverHangsTest {
    private val clock = Clock.System

    private fun silentSource() = WidgetDataSource { _, _, _, _ -> awaitCancellation() }

    /** A configuration store that never answers, as a DataStore whose actor cannot make progress would not. */
    private object SilentConfigStore : WidgetConfigStore {
        override suspend fun config(appWidgetId: Int): WidgetConfig? = awaitCancellation()

        override suspend fun save(
            appWidgetId: Int,
            config: WidgetConfig,
        ) = Unit

        override suspend fun delete(appWidgetIds: Set<Int>) = Unit
    }

    @Test
    fun `a content source that never answers gives the failure state instead of hanging`(): Unit =
        runTest {
            val loader =
                WidgetStateLoader(
                    FakeWidgetConfigStore(),
                    silentSource(),
                    clock,
                    StandardTestDispatcher(testScheduler),
                )

            val state = loader.load(WidgetKind.THREE_DATES, 1)

            state.shouldBeInstanceOf<WidgetContentState.Failed>().config shouldBe
                WidgetConfig.defaultFor(WidgetKind.THREE_DATES)
        }

    @Test
    fun `a configuration store that never answers falls back to the default and still draws content`(): Unit =
        runTest {
            val loader =
                WidgetStateLoader(
                    SilentConfigStore,
                    { _, _, _, _ -> WidgetSamples.data() },
                    clock,
                    StandardTestDispatcher(testScheduler),
                )

            val state = loader.load(WidgetKind.DATE_1X1, 1)

            // The widget is worth more than its stored transparency: content first, configuration best-effort.
            state.shouldBeInstanceOf<WidgetContentState.Ready>().config shouldBe
                WidgetConfig.defaultFor(WidgetKind.DATE_1X1)
        }

    @Test
    fun `both stores silent still ends in a drawable state`(): Unit =
        runTest {
            val loader =
                WidgetStateLoader(SilentConfigStore, silentSource(), clock, StandardTestDispatcher(testScheduler))

            loader.load(WidgetKind.MONTH_BITMAP, 9).shouldBeInstanceOf<WidgetContentState.Failed>()
        }

    @Test
    fun `a source slower than the timeout loses, one faster than it wins`(): Unit =
        runTest {
            val slow =
                WidgetStateLoader(
                    FakeWidgetConfigStore(),
                    { _, _, _, _ ->
                        delay(WidgetStateLoader.LOAD_TIMEOUT * 2)
                        WidgetSamples.data()
                    },
                    clock,
                    StandardTestDispatcher(testScheduler),
                )
            slow.load(WidgetKind.DATE_1X1, 1).shouldBeInstanceOf<WidgetContentState.Failed>()

            val quick =
                WidgetStateLoader(
                    FakeWidgetConfigStore(),
                    { _, _, _, _ ->
                        delay(WidgetStateLoader.LOAD_TIMEOUT / 2)
                        WidgetSamples.data()
                    },
                    clock,
                    StandardTestDispatcher(testScheduler),
                )
            quick.load(WidgetKind.DATE_1X1, 1).shouldBeInstanceOf<WidgetContentState.Ready>()
        }
}
