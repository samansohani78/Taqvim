/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** One unstable type that Compose would compare by identity instead of skipping (T-1802, ADR-0021). */
data class StabilityFinding(
    val kind: Kind,
    /** The composable function or UI model class, fully qualified. */
    val owner: String,
    /** `parameter: Type` for parameters, empty for classes. */
    val detail: String,
) {
    /** What was found unstable. */
    enum class Kind {
        /** A parameter of a composable function. */
        COMPOSABLE_PARAMETER,

        /** A `*UiState`, `*Content` or `*Model` class. */
        UI_MODEL,
    }

    /** The name an exception entry uses: `owner(parameter)` or the class name. */
    val key: String
        get() = if (kind == Kind.UI_MODEL) owner else "$owner(${detail.substringBefore(':')})"

    override fun toString(): String = if (kind == Kind.UI_MODEL) "unstable UI model $owner" else "$owner — $detail"
}

/** Reads Compose compiler reports (`*-composables.txt`, `*-classes.txt`) for unstable UI types. */
object ComposeStability {
    private val FUNCTION = Regex("""\bfun ([\w.]+)\($""")
    private val UNSTABLE_PARAMETER = Regex("""^\s+unstable ([\w<>]+): (.+?)(?: = .*)?$""")
    private val UNSTABLE_CLASS = Regex("""^unstable class ([\w.]+) \{$""")
    private val UI_MODEL_SUFFIX = Regex("""(UiState|Content|(?<!View)Model)$""")
    private val EXCEPTION = Regex("""^(\S+)\s*\|\s*(\S.*)$""")

    /** Composable parameters marked `unstable` in a `*-composables.txt` report. */
    fun unstableParameters(composables: String): List<StabilityFinding> {
        val findings = mutableListOf<StabilityFinding>()
        var function = ""
        composables.lineSequence().forEach { line ->
            FUNCTION.find(line)?.let { function = it.groupValues[1] }
            UNSTABLE_PARAMETER.matchEntire(line)?.let { match ->
                val (name, type) = match.destructured
                findings += StabilityFinding(StabilityFinding.Kind.COMPOSABLE_PARAMETER, function, "$name: $type")
            }
        }
        return findings
    }

    /** UI model classes (`*UiState`, `*Content`, `*Model`) marked `unstable` in a `*-classes.txt` report. */
    fun unstableUiModels(classes: String): List<StabilityFinding> =
        classes
            .lineSequence()
            .mapNotNull { UNSTABLE_CLASS.matchEntire(it)?.groupValues?.get(1) }
            .filter { UI_MODEL_SUFFIX.containsMatchIn(it.substringAfterLast('.')) }
            .map { StabilityFinding(StabilityFinding.Kind.UI_MODEL, it, "") }
            .toList()

    /**
     * Accepted exceptions: one `key | reason` per line (blank lines and `#` comments ignored). An entry without a
     * reason is rejected so every exception stays documented.
     */
    fun exceptions(text: String): Set<String> =
        text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                EXCEPTION.matchEntire(line)?.groupValues?.get(1)
                    ?: throw GradleException("Compose stability exception without a reason: '$line'")
            }.toSet()

    /** Findings of [composables] and [classes] reports that no entry of [accepted] covers. */
    fun violations(
        composables: List<String>,
        classes: List<String>,
        accepted: Set<String>,
    ): List<StabilityFinding> =
        (composables.flatMap(::unstableParameters) + classes.flatMap(::unstableUiModels))
            .distinct()
            .filterNot { it.key in accepted }
}

/**
 * Fails when this module's Compose compiler reports show an unstable composable parameter or UI model (T-1802).
 * Needs the reports, so it runs with `-Ptaqvim.composeMetrics=true` (CI static job).
 */
abstract class ComposeStabilityCheckTask : DefaultTask() {
    /** `build/compose/reports` of this module. */
    @get:Internal
    abstract val reportsDirectory: DirectoryProperty

    /** Documented exceptions shared by all modules. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val exceptionsFile: RegularFileProperty

    /** Whether the build was started with Compose compiler reports enabled. */
    @get:Input
    abstract val reportsEnabled: Property<Boolean>

    @TaskAction
    fun check() {
        if (!reportsEnabled.get()) {
            throw GradleException("$path needs Compose compiler reports: run with -Ptaqvim.composeMetrics=true")
        }
        // A module without Kotlin sources (e.g. a feature not started yet) compiles nothing and writes no reports.
        val reports =
            reportsDirectory
                .get()
                .asFile
                .listFiles()
                .orEmpty()
        val violations =
            ComposeStability.violations(
                composables = reports.filter { it.name.endsWith("-composables.txt") }.map { it.readText() },
                classes = reports.filter { it.name.endsWith("-classes.txt") }.map { it.readText() },
                accepted = ComposeStability.exceptions(exceptionsFile.get().asFile.readText()),
            )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Unstable Compose types in $path (ADR-0021; add immutable core types to config/compose/" +
                    "stability.conf or document an exception):\n" + violations.joinToString("\n") { "  $it" },
            )
        }
    }
}
