/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoParameterDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import java.io.File

/**
 * Extracts [ArchitectureRules] facts from a Konsist [scope]. The only place that touches the Konsist API.
 *
 * @param rootPath directory that module paths are relative to: the project root, or a fixture tree.
 */
class KonsistFacts(
    private val scope: KoScope,
    private val rootPath: String,
) {
    val files: List<FileFacts>
        get() =
            scope.files.map { file ->
                FileFacts(
                    path = relative(file.path),
                    modulePath = modulePathOf(rootPath, file.path),
                    packageName = file.packagee?.name,
                    imports = file.imports.map { it.name },
                    lineCount = file.text.lines().size,
                )
            }

    val functions: List<FunctionFacts>
        get() =
            scope.functions(includeNested = true, includeLocal = true).map {
                FunctionFacts(relative(it.location), declarationLines(it.text, FUN_KEYWORD).size)
            }

    val classes: List<ClassFacts>
        get() =
            scope.classes(includeNested = true, includeLocal = true).map { type ->
                val constructorProperties =
                    type.primaryConstructor
                        ?.parameters
                        .orEmpty()
                        .filter { CONSTRUCTOR_PROPERTY.containsMatchIn(it.text) }
                        .map(::parameterFacts)
                ClassFacts(
                    location = relative(type.location),
                    name = type.name,
                    parents = type.parents(indirectParents = false).map { it.name },
                    isData = type.hasDataModifier,
                    properties = type.properties(includeNested = false).map(::propertyFacts) + constructorProperties,
                )
            }

    val topLevelProperties: List<PropertyFacts>
        get() = scope.properties(includeNested = false).filter { it.isTopLevel }.map(::propertyFacts)

    val objectProperties: List<PropertyFacts>
        get() =
            scope
                .objects(
                    includeNested = true,
                ).flatMap { it.properties(includeNested = false) }
                .map(::propertyFacts)

    private fun propertyFacts(property: KoPropertyDeclaration) =
        PropertyFacts(
            location = relative(property.location),
            name = property.name,
            type = property.type?.text,
            isVar = property.isVar,
            isPrivate = property.hasPrivateModifier,
            declaration = declarationLines(property.text, VAL_OR_VAR).joinToString("\n"),
        )

    private fun parameterFacts(parameter: KoParameterDeclaration) =
        PropertyFacts(
            location = relative(parameter.location),
            name = parameter.name,
            type = parameter.type.text,
            isVar = CONSTRUCTOR_PROPERTY.find(parameter.text)?.groupValues?.get(1) == "var",
            // Konsist exposes no visibility provider for constructor parameters; read the modifier from source.
            isPrivate = PRIVATE_MODIFIER.containsMatchIn(parameter.text),
            declaration = parameter.text,
        )

    private fun relative(path: String): String = path.removePrefix(rootPath.trimEnd('/') + "/")

    companion object {
        /** Fixture trees with intentional violations; excluded from every real-project scope. */
        const val FIXTURES_DIR = "konsist/src/test/resources/fixtures"

        private val FUN_KEYWORD = Regex("""\bfun\b""")
        private val VAL_OR_VAR = Regex("""\b(val|var)\b""")
        private val PRIVATE_MODIFIER = Regex("""^\s*private\s""")

        /** A primary-constructor parameter that declares a property (`val`/`var`, after modifiers/annotations). */
        private val CONSTRUCTOR_PROPERTY =
            Regex("""^\s*(?:@\S+\s+)*(?:(?:private|protected|internal|public|override|open|final)\s+)*(val|var)\s""")

        /** Production (`main`) sources of the whole project, including build-logic. */
        fun production(): KonsistFacts = KonsistFacts(Konsist.scopeFromProduction(), Konsist.projectRootPath)

        /** Every Kotlin source of the project except the fixtures. */
        fun allSources(): KonsistFacts =
            KonsistFacts(Konsist.scopeFromProject().slice { FIXTURES_DIR !in it.path }, Konsist.projectRootPath)

        /** A fixture tree under [FIXTURES_DIR], anchored at the fixture root so module paths resolve. */
        fun fixture(name: String): KonsistFacts =
            KonsistFacts(
                Konsist.scopeFromDirectory("$FIXTURES_DIR/$name"),
                "${Konsist.projectRootPath}/$FIXTURES_DIR/$name",
            )

        /** Every `build.gradle.kts` of a module, as `modulePath → script text`. */
        fun buildScripts(root: File = File(Konsist.projectRootPath)): Map<String, String> =
            root
                .walkTopDown()
                .onEnter { it.name !in SKIPPED_DIRS }
                .filter { it.isFile && it.name == "build.gradle.kts" && it.parentFile != root }
                .associate { script -> modulePathOfDirectory(root, script.parentFile) to script.readText() }

        private fun modulePathOfDirectory(
            root: File,
            directory: File,
        ): String = ":" + directory.relativeTo(root).path.replace(File.separatorChar, ':')

        /** Gradle module path of a source file: the directory before its `/src/` segment, relative to [rootPath]. */
        fun modulePathOf(
            rootPath: String,
            absolutePath: String,
        ): String? {
            val prefix = rootPath.trimEnd('/') + "/"
            val relative = absolutePath.takeIf { it.startsWith(prefix) }?.removePrefix(prefix) ?: return null
            val sourceIndex = relative.indexOf("/src/")
            return if (sourceIndex <= 0) null else ":" + relative.substring(0, sourceIndex).replace('/', ':')
        }

        /** Lines of a declaration starting at its [keyword], dropping KDoc and annotations above it. */
        fun declarationLines(
            text: String,
            keyword: Regex,
        ): List<String> {
            val lines = text.lines()
            val start = lines.indexOfFirst { keyword.containsMatchIn(it) }
            return if (start < 0) lines else lines.drop(start)
        }

        private val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".kotlin", "resources", ".idea")
    }
}
