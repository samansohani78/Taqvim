/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.lifecycle.viewModelScope
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.testing.TimingTest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Code review I05: a long pasted text and a large QR code are computed off the main dispatcher. The main dispatcher is
 * a single thread that records its longest task; the work the tools used to do there is measured for comparison.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Tag(TimingTest.TAG)
class ToolsMainThreadTimingTest {
    private val executor = Executors.newSingleThreadExecutor()
    private val longestNanos = AtomicLong()
    private val main =
        object : CoroutineDispatcher() {
            override fun dispatch(
                context: CoroutineContext,
                block: Runnable,
            ) {
                executor.execute {
                    val started = System.nanoTime()
                    block.run()
                    longestNanos.accumulateAndGet(System.nanoTime() - started, ::maxOf)
                }
            }
        }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }

    @Test
    fun `a long paste and a large QR code do not block the main dispatcher`() {
        val settings = ToolsFixtures.settings()
        val paste = "1405/6/22 " + "lorem ipsum dolor sit amet ".repeat(PASTE_REPEATS)
        val qrText = "€".repeat(QR_CHARACTERS)
        val today = ToolsFixtures.NOW.toJdn(settings.homeZone)
        val convertNanos = timed { DateTools.convert(paste, today, settings) }
        val qrNanos = timed { QrEncoder.encode(qrText) }

        Dispatchers.setMain(main)
        val viewModel =
            ToolsViewModel(
                { MutableStateFlow(settings) },
                FakeClock(ToolsFixtures.NOW),
                computeDispatcher = Dispatchers.Default,
            )
        runBlocking {
            val collecting = launch(main) { viewModel.uiState.collect {} }
            withTimeout(TIMEOUT) { viewModel.uiState.first { it.content is ToolsContent.Ready } }
            longestNanos.set(0)
            launch(main) { viewModel.onInputsChange(ToolsInputs(converter = paste, qr = qrText)) }
            withTimeout(TIMEOUT) {
                viewModel.uiState.first { (it.content as? ToolsContent.Ready)?.converter.isConverted() }
            }
            launch(main) { viewModel.onSelectTab(ToolsTab.QR) }
            val coded = withTimeout(TIMEOUT) { viewModel.uiState.first { it.tab == ToolsTab.QR } }
            (coded.content as ToolsContent.Ready).qr.shouldBeInstanceOf<QrState.Code>()
            collecting.cancel()
        }
        viewModel.viewModelScope.cancel()
        println(
            "I05: convert ${paste.length} chars ${convertNanos / NANOS_PER_MILLI} ms, " +
                "QR ${qrText.length} chars ${qrNanos / NANOS_PER_MILLI} ms (formerly on the main thread); " +
                "longest main-dispatcher task now ${longestNanos.get() / NANOS_PER_MILLI} ms",
        )
        longestNanos.get() shouldBeLessThan MAIN_BUDGET_NANOS
    }

    private fun ConverterResult?.isConverted(): Boolean = (this as? ConverterResult.Converted)?.isToday == false

    private inline fun timed(block: () -> Unit): Long {
        val started = System.nanoTime()
        block()
        return System.nanoTime() - started
    }

    private companion object {
        const val PASTE_REPEATS = 3_800
        const val QR_CHARACTERS = 300
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAIN_BUDGET_NANOS = 50L * NANOS_PER_MILLI
        val TIMEOUT = 30.seconds
    }
}
