/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import java.io.File

/**
 * Compares generated text with a committed snapshot (e.g. the T-202 formatting matrix).
 *
 * Snapshots are produced by Taqvim itself, so they carry a `# generated-by:` header instead of a citation and
 * live outside `golden/`. Run tests with `-Ptaqvim.updateSnapshots=true` to (re)write mismatching snapshots.
 *
 * @param update rewrite the snapshot instead of failing; defaults to the `taqvim.updateSnapshots` system property.
 */
public class SnapshotVerifier(
    private val update: Boolean = System.getProperty(UPDATE_PROPERTY).toBoolean(),
) {
    /** Verifies (or updates) [snapshot] against [actual]; [generator] names the producing test in the header. */
    public fun verify(
        snapshot: File,
        actual: String,
        generator: String,
    ): Result<Unit> {
        val rendered = "$GENERATED_BY_PREFIX $generator\n${actual.trimEnd()}\n"
        val expected = snapshot.takeIf { it.isFile }?.readText()
        return when {
            expected == rendered -> {
                Result.success(Unit)
            }

            update -> {
                Result.success(write(snapshot, rendered))
            }

            expected == null -> {
                Result.failure(AssertionError("Snapshot ${snapshot.path} is missing; run with -P$UPDATE_PROPERTY=true"))
            }

            else -> {
                Result.failure(
                    AssertionError(
                        "Snapshot ${snapshot.path} differs at line ${firstDifferentLine(expected, rendered)}; " +
                            "run with -P$UPDATE_PROPERTY=true after reviewing the change",
                    ),
                )
            }
        }
    }

    private fun write(
        snapshot: File,
        rendered: String,
    ) {
        snapshot.parentFile?.mkdirs()
        snapshot.writeText(rendered)
    }

    public companion object {
        /** System property (forwarded from the Gradle property of the same name) enabling snapshot updates. */
        public const val UPDATE_PROPERTY: String = "taqvim.updateSnapshots"

        /** First line of every snapshot. */
        public const val GENERATED_BY_PREFIX: String = "# generated-by:"

        /** 1-based number of the first line where [expected] and [actual] differ. */
        public fun firstDifferentLine(
            expected: String,
            actual: String,
        ): Int {
            val expectedLines = expected.lines()
            val actualLines = actual.lines()
            val index = expectedLines.zip(actualLines).indexOfFirst { (left, right) -> left != right }
            return if (index >= 0) index + 1 else minOf(expectedLines.size, actualLines.size) + 1
        }
    }
}
