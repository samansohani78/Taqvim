/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GoldenFilesTest {
    private val validHeader =
        listOf(
            "# source: Example Observatory — Yearbook 2026",
            "# url: https://example.org/yearbook-2026.pdf",
            "# retrieved: 2026-09-13",
            "# page: 12",
            "# reviewer: pending",
            "# notes: transcribed manually",
        )

    private val minimalHeader =
        listOf("#", "# source: S", "#url: http://example.org/x", "# retrieved: 2026-01-02", "# reviewer: R")

    private fun failureOf(vararg lines: String): String =
        Provenance
            .parse(lines.toList())
            .exceptionOrNull()
            ?.message
            .orEmpty()

    @Test
    fun `parses a complete provenance header`() {
        val provenance = Provenance.parse(validHeader).getOrThrow()

        provenance shouldBe
            Provenance(
                source = "Example Observatory — Yearbook 2026",
                url = "https://example.org/yearbook-2026.pdf",
                retrieved = LocalDate(2026, 9, 13),
                reviewer = "pending",
                page = "12",
                notes = "transcribed manually",
            )
    }

    @Test
    fun `optional keys may be omitted and blank header lines are ignored`() {
        val provenance =
            Provenance
                .parse(minimalHeader)
                .getOrThrow()

        provenance.page shouldBe null
        provenance.notes shouldBe null
    }

    @Test
    fun `reports every provenance problem`() {
        failureOf() shouldBe "missing 'source'; missing 'url'; missing 'retrieved'; missing 'reviewer'"
        failureOf("# source: S", "# url: ftp://x", "# retrieved: 13/09/2026", "# reviewer: R") shouldContain
            "url must be an http(s) URL: 'ftp://x'; retrieved must be an ISO-8601 date: '13/09/2026'"
        failureOf("# no separator", "# :empty key", "# colour: red", "# source: a", "# source: b") shouldContain
            "malformed header line 'no separator'; malformed header line ':empty key'; unknown key 'colour'; " +
            "duplicate key 'source'"
    }

    @Test
    fun `golden file splits header and body`() {
        val text = (validHeader + listOf("a,1", "", "b,2")).joinToString("\n")
        val golden = GoldenFile.parse(text, "sample").getOrThrow()

        golden.provenance.page shouldBe "12"
        golden.body shouldBe "a,1\n\nb,2"
        golden.lines shouldContainExactly listOf("a,1", "b,2")
    }

    @Test
    fun `golden file without header fails with its name`() {
        GoldenFile
            .parse("a,1", "orphan.csv")
            .exceptionOrNull()
            ?.message
            .orEmpty() shouldContain
            "orphan.csv: invalid provenance header: missing 'source'"
    }

    @Test
    fun `loads fixtures from the classpath`() {
        val golden = GoldenFile.load("/golden/sample/valid-fixture.csv")

        golden.provenance.source shouldBe "Taqvim test infrastructure self-test"
        golden.lines shouldContainExactly listOf("key,value", "answer,42")
        Fixtures.lines("golden/sample/valid-fixture.csv").first() shouldContain "# source:"
    }

    @Test
    fun `missing fixtures fail with an actionable message`() {
        shouldThrow<IllegalArgumentException> { Fixtures.text("golden/nope.csv") }.message shouldBe
            "Fixture not found on the test classpath: 'golden/nope.csv'"
    }

    @Test
    fun `audit finds golden fixtures without valid provenance`(
        @TempDir root: File,
    ) {
        fun write(
            path: String,
            text: String,
        ) = File(root, path).apply { parentFile.mkdirs() }.writeText(text)

        write("core/calendar/src/test/resources/golden/persian/nowruz.csv", validHeader.joinToString("\n") + "\n1404,x")
        write("core/calendar/src/test/resources/golden/persian/uncited.csv", "1404,x")
        write("core/calendar/src/test/resources/golden/README.md", "not a fixture")
        write("core/calendar/src/test/resources/snapshots/format.txt", "# generated-by: FormatTest\nx")
        write("core/calendar/build/tmp/src/test/resources/golden/ignored.csv", "no header")

        val invalid = FixtureAudit.findInvalid(root)

        invalid shouldHaveSize 1
        invalid.single() shouldContain "core/calendar/src/test/resources/golden/persian/uncited.csv: " +
            "uncited.csv: invalid provenance header"
    }

    @Test
    fun `audit of an empty tree is clean`(
        @TempDir root: File,
    ) {
        FixtureAudit.findInvalid(root).shouldBeEmpty()
    }
}
