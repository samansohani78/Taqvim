/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GenerateEventsCliTest {
    private fun run(vararg args: String): Pair<Int, List<String>> {
        val output = mutableListOf<String>()
        return runGenerateCli(args.toList()) { output += it } to output
    }

    @Test
    fun `generates from a valid dataset, removes stale sources and refuses invalid data`(
        @TempDir root: File,
    ) {
        val schema = File(root, "events.v1.json").apply { writeText(schemaText()) }
        val dataset = File(root, "dataset").apply { mkdirs() }
        val output = File(root, "generated").apply { mkdirs() }
        File(dataset, "sample.json").writeText(resource("valid/sample.json"))
        File(output, "Stale.kt").writeText("stale")

        run(schema.path, dataset.path, output.path).first shouldBe 0
        output.list().orEmpty().sorted() shouldBe
            listOf("OfficialEvents.kt", "OfficialEventsPart1.kt", "OfficialEventsPart2.kt")

        File(dataset, "broken.json").writeText(resource("invalid/11-missing-fa.json"))
        File(output, "OfficialEvents.kt").writeText("untouched")
        val (code, messages) = run(schema.path, dataset.path, output.path)
        code shouldBe 1
        messages.last() shouldContain "refusing to generate"
        File(output, "OfficialEvents.kt").readText() shouldBe "untouched"

        run(schema.path, dataset.path).first shouldBe 2
    }

    @Test
    fun `the committed data-events sources are up to date`() {
        val datasetDirectory = File(requireNotNull(System.getProperty("taqvim.dataset.directory")))
        val generatedDirectory = File(requireNotNull(System.getProperty("taqvim.generated.events.directory")))
        val schemaFile = File(datasetDirectory, "events.v1.json")
        val expected = EventsCodeGenerator.generate(datasetFiles(schemaFile, datasetDirectory))
        val committed =
            generatedDirectory.listFiles { file -> file.extension == "kt" }.orEmpty().associate {
                it.name to
                    it.readText()
            }

        val stale =
            (expected.map { it.fileName }.toSet() + committed.keys).filter { name ->
                val fresh = expected.firstOrNull { it.fileName == name }?.content
                val current = committed[name]
                fresh == null || current == null || normalized(fresh) != normalized(current)
            }
        withClue("run ./gradlew :tools:dataset:generateEvents spotlessApply") { stale.shouldBeEmpty() }
    }

    /** Ignores formatting applied by spotless/ktlint: whitespace and trailing commas. */
    private fun normalized(source: String): String = source.replace(TRAILING_COMMA, "$1").filterNot(Char::isWhitespace)

    private companion object {
        val TRAILING_COMMA = Regex(""",(\s*[)\]}])""")
    }
}
