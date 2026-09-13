/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeSorted
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import org.junit.jupiter.api.Test

/** D-08: generator output for the synthetic sample (all rule types, validity, flags, aliases, links). */
class EventsCodeGeneratorTest {
    private val sample = mapOf("sample.json" to resource("valid/sample.json"))

    private fun ids(source: String): List<String> =
        Regex("""id = EventId\("([^"]+)"\)""").findAll(source).map { it.groupValues[1] }.toList()

    @Test
    fun `the synthetic sample matches its snapshot`() {
        val rendered =
            EventsCodeGenerator
                .generate(sample, eventsPerFile = 4)
                .joinToString("\n") { "// ===== ${it.fileName}\n${it.content}" }

        SnapshotVerifier().verify(File(SNAPSHOT), rendered, "EventsCodeGeneratorTest").getOrThrow()
    }

    @Test
    fun `output is deterministic, sorted by id and split into parts`() {
        val sources = EventsCodeGenerator.generate(sample, eventsPerFile = 4)

        sources shouldBe EventsCodeGenerator.generate(sample, eventsPerFile = 4)
        sources.map { it.fileName } shouldBe
            listOf("OfficialEventsPart1.kt", "OfficialEventsPart2.kt", "OfficialEventsPart3.kt", "OfficialEvents.kt")
        sources.dropLast(1).flatMap { ids(it.content) }.shouldBeSorted()
        sources.last().content shouldContain "OFFICIAL_EVENTS_PART_1 +"
        sources.forEach { it.content shouldStartWith "/*\n * Copyright (c) 2026 Saman Sohani." }
        sources.first().content shouldContain "@file:Suppress(\"NoHardcodedNonLatinText\", \"MaxLineLength\")"
    }

    @Test
    fun `an empty dataset still yields an index`() {
        val sources = EventsCodeGenerator.generate(mapOf("empty.json" to """{"schemaVersion":1,"events":[]}"""))

        sources.map { it.fileName } shouldBe listOf("OfficialEvents.kt")
        sources.single().content shouldContain "emptyList()"
        shouldThrow<IllegalArgumentException> { EventsCodeGenerator.generate(sample, eventsPerFile = 0) }
    }

    private companion object {
        const val SNAPSHOT = "src/test/resources/snapshots/generated-sample.kt.txt"
    }
}
