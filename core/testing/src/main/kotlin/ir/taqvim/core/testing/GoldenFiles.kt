/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import java.io.File

/** A golden fixture: externally published reference data with its [provenance] (docs/PLAN.md §0.3, §8.2). */
public data class GoldenFile(
    public val provenance: Provenance,
    public val body: String,
) {
    /** Non-blank body lines, the usual shape of tabular fixtures. */
    public val lines: List<String>
        get() = body.lines().filter { it.isNotBlank() }

    public companion object {
        /** Parses fixture [text]; [name] identifies the fixture in error messages. */
        public fun parse(
            text: String,
            name: String,
        ): Result<GoldenFile> {
            val allLines = text.lines()
            val header = allLines.takeWhile { it.startsWith(Provenance.HEADER_PREFIX) }
            val body = allLines.drop(header.size).joinToString("\n")
            return Provenance
                .parse(header)
                .map { GoldenFile(it, body) }
                .recoverCatching {
                    throw IllegalArgumentException(
                        "$name: invalid provenance header: ${it.message}",
                        it,
                    )
                }
        }

        /** Loads a golden fixture from the test classpath; throws when missing or when its header is invalid. */
        public fun load(resourcePath: String): GoldenFile =
            parse(Fixtures.text(resourcePath), resourcePath).getOrThrow()
    }
}

/** Reads fixture resources from the test classpath with actionable errors. */
public object Fixtures {
    /** UTF-8 text of the classpath resource at [resourcePath] (leading `/` optional). */
    public fun text(resourcePath: String): String {
        val normalized = resourcePath.removePrefix("/")
        val stream =
            Fixtures::class.java.classLoader?.getResourceAsStream(normalized)
                ?: throw IllegalArgumentException("Fixture not found on the test classpath: '$normalized'")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** Lines of the classpath resource at [resourcePath]. */
    public fun lines(resourcePath: String): List<String> = text(resourcePath).lines()
}

/**
 * Finds golden fixtures without a valid provenance header. Golden fixtures live under
 * `src/test/resources/golden/` of any module; a repository test fails if this returns anything (plan §8.2).
 */
public object FixtureAudit {
    /** Resource directory that holds cited golden fixtures. */
    public const val GOLDEN_DIRECTORY: String = "src/test/resources/golden/"

    private val SKIPPED_DIRECTORIES = setOf("build", ".gradle", ".git", ".kotlin", ".idea")

    /** Relative paths of invalid golden fixtures under [root], each with the reason. */
    public fun findInvalid(root: File): List<String> =
        root
            .walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRECTORIES }
            .filter { it.isFile && isGoldenFixture(it) }
            .mapNotNull { file ->
                GoldenFile.parse(file.readText(), file.name).exceptionOrNull()?.let {
                    "${file.relativeTo(root).invariantSeparatorsPath}: ${it.message}"
                }
            }.sorted()
            .toList()

    private fun isGoldenFixture(file: File): Boolean =
        "/$GOLDEN_DIRECTORY" in "/" + file.invariantSeparatorsPath && file.name != "README.md"
}
