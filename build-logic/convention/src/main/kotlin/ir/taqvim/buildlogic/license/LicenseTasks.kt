/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import java.io.File
import java.util.TreeSet
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedArtifactResult
import org.gradle.api.artifacts.result.UnresolvedComponentResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.kotlin.dsl.register
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.work.DisableCachingByDefault

/** Where each project writes the external modules it resolves. */
internal const val LICENSE_COORDINATES_PATH = "reports/licenses/dependency-coordinates.json"

private const val SAMPLE_FAILURES = 5

/**
 * Records every external module of a project's classified configurations (see [ConfigurationClassifier]).
 * Configuration-cache compatible: it only consumes lazily resolved dependency graphs.
 */
@UntrackedTask(because = "Dependency graphs are re-resolved on every run")
abstract class CollectDependencyCoordinatesTask : DefaultTask() {
    @get:Internal
    abstract val runtimeGraphs: ListProperty<ResolvedComponentResult>

    @get:Internal
    abstract val debugGraphs: ListProperty<ResolvedComponentResult>

    @get:Internal
    abstract val buildGraphs: ListProperty<ResolvedComponentResult>

    @get:Internal
    abstract val testGraphs: ListProperty<ResolvedComponentResult>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun collect() {
        val found = sortedMapOf<String, MutableSet<LicenseScope>>()
        mapOf(
            LicenseScope.RUNTIME to runtimeGraphs,
            LicenseScope.DEBUG to debugGraphs,
            LicenseScope.BUILD to buildGraphs,
            LicenseScope.TEST to testGraphs,
        ).forEach { (scope, graphs) ->
            graphs.get().flatMap(::externalModules).forEach { found.getOrPut(it) { mutableSetOf() }.add(scope) }
        }
        outputFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(LicenseJsonCodec.encodeCoordinates(found))
    }
}

/** Walks a resolved graph and returns the `group:name:version` of every external module in it. */
internal fun externalModules(root: ResolvedComponentResult): Set<String> {
    val seen = HashSet<ResolvedComponentResult>()
    val modules = TreeSet<String>()
    val queue = ArrayDeque(listOf(root))
    while (queue.isNotEmpty()) {
        val component = queue.removeFirst()
        if (seen.add(component)) {
            (component.id as? ModuleComponentIdentifier)?.let { modules += "${it.group}:${it.module}:${it.version}" }
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { queue += it.selected }
        }
    }
    return modules
}

/**
 * Aggregates all projects' coordinates, downloads their POMs (following parents) and writes the
 * license report consumed by [LicenseCheckTask] and uploaded as a CI artifact.
 */
@DisableCachingByDefault(because = "Downloads POM metadata from the configured repositories")
abstract class LicenseReportTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val coordinateFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    init {
        notCompatibleWithConfigurationCache(
            "Queries POM artifacts through the project dependency handler at execution time",
        )
    }

    @TaskAction
    fun report() {
        val scopes = sortedMapOf<String, MutableSet<LicenseScope>>()
        coordinateFiles.filter { it.isFile }.forEach { file ->
            LicenseJsonCodec.decodeCoordinates(file.readText()).forEach { (coordinate, found) ->
                scopes.getOrPut(coordinate) { mutableSetOf() }.addAll(found)
            }
        }
        val licenses = PomLicenseResolver(::fetchPoms).resolve(scopes.keys)
        val dependencies =
            scopes.map { (coordinate, found) ->
                DependencyLicenseInfo(ModuleCoordinate.parse(coordinate), found, licenses[coordinate].orEmpty())
            }
        reportFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(LicenseJsonCodec.encodeReport(dependencies))
        logger.lifecycle("License report: ${dependencies.size} external modules -> ${reportFile.get().asFile}")
    }

    /** Cache first (immutable per version), then the configured repositories for anything missing. */
    private fun fetchPoms(coordinates: Collection<String>): Map<String, String> {
        val filesRoot = File(project.gradle.gradleUserHomeDir, "caches/modules-2/files-2.1")
        val cached =
            coordinates
                .mapNotNull { text ->
                    val pom = GradleModuleCache.findPom(filesRoot, ModuleCoordinate.parse(text))
                    pom?.let { text to it.readText() }
                }.toMap()
        val missing = coordinates.filterNot(cached::containsKey)
        return cached + queryRepositories(missing)
    }

    /**
     * Fetches POMs with an [org.gradle.api.artifacts.query.ArtifactResolutionQuery]: unlike a detached
     * configuration, it performs no version-conflict resolution, so every requested version is retrieved.
     */
    private fun queryRepositories(coordinates: Collection<String>): Map<String, String> {
        if (coordinates.isEmpty()) return emptyMap()
        val query = project.dependencies.createArtifactResolutionQuery()
        coordinates.map(ModuleCoordinate::parse).forEach { query.forModule(it.group, it.name, it.version) }
        val result = query.withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute()
        val failures =
            result.components
                .filterIsInstance<UnresolvedComponentResult>()
                .map { "${it.id}: ${it.failure.message}" }
                .toMutableList()
        val poms =
            result.resolvedComponents
                .mapNotNull { component ->
                    val artifacts = component.getArtifacts(MavenPomArtifact::class.java)
                    artifacts.filterIsInstance<UnresolvedArtifactResult>().forEach {
                        failures += "${component.id}: ${it.failure.message?.lineSequence()?.firstOrNull()}"
                    }
                    val id = component.id as? ModuleComponentIdentifier
                    val pom = artifacts.filterIsInstance<ResolvedArtifactResult>().firstOrNull()
                    if (id == null || pom == null) {
                        null
                    } else {
                        "${id.group}:${id.module}:${id.version}" to pom.file.readText()
                    }
                }.toMap()
        if (failures.isNotEmpty()) {
            val unavailable = coordinates.size - poms.size
            logger.warn(
                "License report: $unavailable POM(s) not in the Gradle cache and not downloadable:\n    " +
                    failures.take(SAMPLE_FAILURES).joinToString("\n    "),
            )
        }
        return poms
    }
}

/** Fails the build when any dependency violates the allow-list. */
@CacheableTask
abstract class LicenseCheckTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val allowListFile: RegularFileProperty

    @get:OutputFile
    abstract val resultFile: RegularFileProperty

    @TaskAction
    fun check() {
        val allowList = AllowListParser.parse(allowListFile.get().asFile.readText())
        val dependencies = LicenseJsonCodec.decodeReport(reportFile.get().asFile.readText())
        val violations = LicensePolicy.evaluate(dependencies, allowList)
        val summary = LicensePolicy.summary(dependencies, violations)
        resultFile
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
            .writeText(summary)
        if (violations.isNotEmpty()) throw GradleException(summary)
        logger.lifecycle(summary.trim())
    }
}

/** Registers `licenseDependencies` in a project (applied to every module and the root). */
internal fun Project.registerLicenseCoordinatesTask() {
    val projectPath = path
    tasks.register<CollectDependencyCoordinatesTask>("licenseDependencies") {
        group = "verification"
        description = "Collects external modules for the license gate."
        outputFile.set(layout.buildDirectory.file(LICENSE_COORDINATES_PATH))
        configurations.filter { it.isCanBeResolved }.forEach { configuration ->
            val graph = configuration.incoming.resolutionResult.rootComponent
            when (ConfigurationClassifier.classify(projectPath, configuration.name)) {
                LicenseScope.RUNTIME -> runtimeGraphs.add(graph)
                LicenseScope.DEBUG -> debugGraphs.add(graph)
                LicenseScope.BUILD -> buildGraphs.add(graph)
                LicenseScope.TEST -> testGraphs.add(graph)
                null -> Unit
            }
        }
    }
}
