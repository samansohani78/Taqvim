/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.diagnostics

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * T-1504: the crash log keeps the last records of this device, caps what it writes, and the handler adds the facts of
 * the run without ever swallowing the crash.
 */
class CrashLogTest {
    private val directory: File = createTempDirectory("crash").toFile()
    private val store = CrashLogStore(directory)

    private val facts =
        CrashFacts(
            app = "1.0.0 (10099, release)",
            android = "16 (API 37)",
            device = "OnePlus PJZ110",
            locale = "fa-IR",
        )

    @Test
    fun `keeps only the newest records, newest first`() {
        repeat(CrashLogStore.MAX_FILES + 2) { store.write(1_000L + it, "crash $it") }

        val stored = store.stored()

        stored shouldHaveSize CrashLogStore.MAX_FILES
        stored.map { it.text } shouldBe listOf("crash 4", "crash 3", "crash 2")
        stored.map { it.atEpochMillis } shouldBe listOf(1_004L, 1_003L, 1_002L)
    }

    @Test
    fun `cuts a record at the cap and forgets everything on clear`() {
        store.write(1_000L, "x".repeat(CrashLogStore.MAX_CHARS * 2))

        store
            .stored()
            .single()
            .text.length shouldBe CrashLogStore.MAX_CHARS

        store.clear()

        store.stored() shouldBe emptyList()
    }

    @Test
    fun `writes the facts of the run and the stack trace, and lets the previous handler run`() {
        val context = CrashContext(facts)
        context.onRoute("Calendar")
        context.onSettings("calendars=PERSIAN+GREGORIAN islamic=IRAN_CRESCENT")
        val chained = mutableListOf<Throwable>()
        val capture = CrashCapture(store, context, { 1_789_000_000_000L }) { _, error -> chained += error }
        val failure = IllegalStateException("boom", IllegalArgumentException("cause"))

        capture.uncaughtException(Thread.currentThread(), failure)

        chained shouldBe listOf(failure)
        val record = store.stored().single()
        record.atEpochMillis shouldBe 1_789_000_000_000L
        record.text shouldContain "app=1.0.0 (10099, release)"
        record.text shouldContain "android=16 (API 37)"
        record.text shouldContain "device=OnePlus PJZ110"
        record.text shouldContain "locale=fa-IR"
        record.text shouldContain "route=Calendar"
        record.text shouldContain "islamic=IRAN_CRESCENT"
        record.text shouldContain "IllegalStateException: boom"
        record.text shouldContain "Caused by: java.lang.IllegalArgumentException: cause"
    }

    @Test
    fun `records the crash even when no handler was installed before it`() {
        val capture = CrashCapture(store, CrashContext(facts), { 5_000L }, previous = null)

        capture.uncaughtException(Thread.currentThread(), IllegalStateException("alone"))

        store.stored().single().text shouldContain "alone"
    }

    @Test
    fun `a store that cannot write does not fail the crash`() {
        val file = File(directory, "blocked").apply { writeText("not a directory") }
        val blocked = CrashLogStore(File(file, "crash"))
        val chained = mutableListOf<Throwable>()
        val capture = CrashCapture(blocked, CrashContext(facts), { 1L }) { _, error -> chained += error }
        val failure = IllegalStateException("boom")

        capture.uncaughtException(Thread.currentThread(), failure)

        blocked.stored() shouldBe emptyList()
        chained shouldBe listOf(failure)
    }

    @Test
    fun `the report source reads the stored records and clearing empties it`(): Unit =
        runTest {
            store.write(2_000L, "stored crash")
            val source = FileCrashReportSource(store, io = Dispatchers.Unconfined)

            source
                .crashes()
                .first()
                .single()
                .text shouldBe "stored crash"

            source.clear()

            source.crashes().first() shouldBe emptyList()
        }

    @Test
    fun `facts start unknown until the app learns them`() {
        val context = CrashContext(facts)

        context.facts.route shouldBe CrashFacts.UNKNOWN
        context.facts.settings shouldBe CrashFacts.UNKNOWN
        CrashRecord.text(context.facts, "main", IllegalStateException("x"), 1L) shouldNotContain "route=Calendar"
    }
}
