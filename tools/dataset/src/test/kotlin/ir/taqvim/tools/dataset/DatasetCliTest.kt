/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DatasetCliTest {
    private fun run(vararg args: String): Pair<Int, List<String>> {
        val output = mutableListOf<String>()
        return runCli(args.toList()) { output += it } to output
    }

    @Test
    fun `valid, invalid and unusable invocations have distinct exit codes`(
        @TempDir directory: File,
    ) {
        val schema = File(directory, "events.v1.json").apply { writeText(schemaText()) }
        File(directory, "iran").mkdirs()
        File(directory, "iran/sample.json").writeText(resource("valid/sample.json"))

        run(schema.path, directory.path).first shouldBe 0

        File(directory, "iran/broken.json").writeText(resource("invalid/11-missing-fa.json"))
        val (code, output) = run(schema.path, directory.path)
        code shouldBe 1
        output.last() shouldContain "2 dataset file(s) checked, 1 issue(s)"

        run(schema.path).first shouldBe 2
        run(File(directory, "missing.json").path, directory.path).second shouldContain
            "usage: validate <schema.json> <dataset directory> (both must exist)"
    }
}
