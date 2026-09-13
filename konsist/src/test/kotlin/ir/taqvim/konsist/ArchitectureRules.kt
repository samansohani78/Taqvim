/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

/** Facts about one Kotlin source file. */
data class FileFacts(
    val path: String,
    val modulePath: String?,
    val packageName: String?,
    val imports: List<String>,
    val lineCount: Int,
)

/** Facts about one function; [lineCount] starts at the `fun` keyword (KDoc and annotations excluded). */
data class FunctionFacts(
    val location: String,
    val lineCount: Int,
)

/** Facts about a property or a primary-constructor `val`/`var` parameter. */
data class PropertyFacts(
    val location: String,
    val name: String,
    val type: String?,
    val isVar: Boolean,
    val isPrivate: Boolean,
    val declaration: String,
)

/** Facts about a class. */
data class ClassFacts(
    val location: String,
    val name: String,
    val parents: List<String>,
    val isData: Boolean,
    val properties: List<PropertyFacts>,
)

/** Architectural layer of a Gradle module (plan §3.1). */
enum class Layer(
    val label: String,
) {
    APP(":app"),
    CORE(":core"),
    DATA(":data"),
    FEATURE(":feature"),
    OTHER("module"),
    ;

    companion object {
        /** Layer of a Gradle module path such as `:feature:calendar`. */
        fun ofModule(modulePath: String): Layer = ofName(modulePath.split(':').getOrNull(1).orEmpty())

        /** Layer named by a path segment (`app`, `core`, `data`, `feature`), [OTHER] otherwise. */
        fun ofName(name: String): Layer =
            entries.firstOrNull { it != OTHER && it.name.equals(name, ignoreCase = true) } ?: OTHER

        /** Layer that owns a Taqvim import, or `null` for third-party imports. */
        fun ofImport(import: String): Layer? =
            listOf(APP, CORE, DATA, FEATURE).firstOrNull { import.startsWith("ir.taqvim.${it.name.lowercase()}.") }
    }
}

/**
 * Architecture rules from docs/PLAN.md §3.1 and T-002. Every rule returns human-readable violations;
 * an empty list means the code base complies.
 */
object ArchitectureRules {
    const val MAX_FILE_LINES = 400
    const val MAX_FUNCTION_LINES = 50

    /** Android-bound modules under `:core` (ADR-0004 §1–2); every other `:core` module is pure JVM. */
    val ANDROID_CORE_MODULES = setOf(":core:ui", ":core:ui-testing")

    private val ANDROID_PACKAGES = listOf("android.", "androidx.", "com.google.android.")

    /** Layers each layer must never depend on. Feature→feature is handled separately (own feature allowed). */
    private val FORBIDDEN_LAYERS =
        mapOf(
            Layer.CORE to setOf(Layer.APP, Layer.DATA, Layer.FEATURE),
            Layer.DATA to setOf(Layer.APP, Layer.FEATURE),
            Layer.FEATURE to setOf(Layer.APP, Layer.DATA),
        )

    private val PROJECT_REFERENCE =
        Regex("""projects\.(app|core|data|feature)\b|project\(\s*":(app|core|data|feature)\b""")
    private val ANDROIDX_LIBRARY = Regex("""\blibs\.androidx\.""")
    private val VIEW_MODEL_PARENTS = setOf("ViewModel", "AndroidViewModel")
    private val UI_STATE_FLOW = Regex("""^StateFlow<\w*UiState>$""")
    private val EXPOSED_MUTABLE_STREAM =
        Regex("""^(MutableStateFlow|MutableSharedFlow|MutableLiveData|LiveData|MutableState)\b""")
    private val MUTABLE_TYPE = Regex("""^(Mutable\w*|Array<|\w+Array\b|ArrayList|HashMap|HashSet|LinkedHashMap)""")
    private val MUTABLE_HOLDER =
        Regex("""\b(mutable\w*Of|Mutable[A-Z]\w*|ArrayList|HashMap|HashSet|LinkedHashMap|Atomic[A-Z]\w*)\b""")

    /** Package every file of [modulePath] must live in (or below). */
    fun expectedPackage(modulePath: String): String =
        if (modulePath.startsWith(":build-logic")) {
            "ir.taqvim.buildlogic"
        } else {
            "ir.taqvim" + modulePath.replace(':', '.').replace("-", "")
        }

    /** Files whose package does not match their module. */
    fun packageViolations(files: List<FileFacts>): List<String> =
        files.mapNotNull { file ->
            val module = file.modulePath ?: return@mapNotNull null
            val expected = expectedPackage(module)
            val actual = file.packageName.orEmpty()
            if (actual == expected || actual.startsWith("$expected.")) {
                null
            } else {
                "${file.path}: package '$actual' must be '$expected' or below (module $module)"
            }
        }

    /** Imports that break layering or the Android boundary of pure `:core` modules. */
    fun importViolations(files: List<FileFacts>): List<String> =
        files.flatMap { file ->
            val module = file.modulePath ?: return@flatMap emptyList()
            file.imports.mapNotNull { import ->
                importViolation(module, import)?.let { "${file.path}: import $import — $it" }
            }
        }

    /** Why [import] is forbidden in [modulePath], or `null` when it is allowed. */
    fun importViolation(
        modulePath: String,
        import: String,
    ): String? =
        androidViolation(modulePath, import) ?: layerViolation(modulePath, import)
            ?: crossFeatureViolation(modulePath, import)

    /** Project dependencies in a module build script that break layering. */
    fun buildScriptViolations(
        modulePath: String,
        script: String,
    ): List<String> {
        val layer = Layer.ofModule(modulePath)
        val forbidden =
            FORBIDDEN_LAYERS[layer].orEmpty() + if (layer == Layer.FEATURE) setOf(Layer.FEATURE) else emptySet()
        val layering =
            PROJECT_REFERENCE
                .findAll(script)
                .map { match -> match to Layer.ofName(match.groupValues[1].ifEmpty { match.groupValues[2] }) }
                .filter { (_, target) -> target in forbidden }
                .map { (match, target) ->
                    "$modulePath: depends on '${match.value}' — ${layer.label} must not depend on ${target.label}"
                }.toList()
        val android =
            if (isPureCore(modulePath) && ANDROIDX_LIBRARY.containsMatchIn(script)) {
                listOf("$modulePath: pure :core modules must not depend on AndroidX libraries")
            } else {
                emptyList()
            }
        return layering + android
    }

    /** Files longer than [MAX_FILE_LINES]. */
    fun oversizedFiles(files: List<FileFacts>): List<String> =
        files.filter { it.lineCount > MAX_FILE_LINES }.map { "${it.path}: ${it.lineCount} lines (max $MAX_FILE_LINES)" }

    /** Functions longer than [MAX_FUNCTION_LINES]. */
    fun oversizedFunctions(functions: List<FunctionFacts>): List<String> =
        functions
            .filter { it.lineCount > MAX_FUNCTION_LINES }
            .map { "${it.location}: function has ${it.lineCount} lines (max $MAX_FUNCTION_LINES)" }

    /** ViewModels are named `*ViewModel`, extend `ViewModel`, and expose typed `StateFlow<*UiState>` only. */
    fun viewModelViolations(classes: List<ClassFacts>): List<String> =
        classes.flatMap { type ->
            val extendsViewModel = type.parents.any { it in VIEW_MODEL_PARENTS }
            when {
                extendsViewModel -> viewModelContractViolations(type)
                type.name.endsWith("ViewModel") -> listOf("${type.location}: '${type.name}' does not extend ViewModel")
                else -> emptyList()
            }
        }

    /** `*UiState` classes are immutable data classes. */
    fun uiStateViolations(classes: List<ClassFacts>): List<String> =
        classes.filter { it.name.endsWith("UiState") }.flatMap { type ->
            buildList {
                if (!type.isData) add("${type.location}: '${type.name}' must be a data class")
                type.properties.filter { it.isVar }.forEach {
                    add(
                        "${type.location}: '${type.name}.${it.name}' must be val",
                    )
                }
                type.properties
                    .filter { MUTABLE_TYPE.containsMatchIn(it.type.orEmpty()) }
                    .forEach { add("${type.location}: '${type.name}.${it.name}' has mutable type '${it.type}'") }
            }
        }

    /** Top-level and `object` properties must not be `var` or hold mutable containers/state. */
    fun globalMutableStateViolations(
        topLevel: List<PropertyFacts>,
        objectMembers: List<PropertyFacts>,
    ): List<String> =
        (topLevel.map { it to "top-level" } + objectMembers.map { it to "object" }).mapNotNull { (property, scope) ->
            when {
                property.isVar -> {
                    "${property.location}: $scope property '${property.name}' is a var"
                }

                MUTABLE_HOLDER.containsMatchIn(property.declaration) -> {
                    "${property.location}: $scope property '${property.name}' holds mutable state"
                }

                else -> {
                    null
                }
            }
        }

    private fun isPureCore(modulePath: String): Boolean =
        Layer.ofModule(modulePath) == Layer.CORE && modulePath !in ANDROID_CORE_MODULES

    private fun androidViolation(
        modulePath: String,
        import: String,
    ): String? =
        if (isPureCore(modulePath) && ANDROID_PACKAGES.any(import::startsWith)) {
            "pure :core modules must not use Android APIs"
        } else {
            null
        }

    private fun layerViolation(
        modulePath: String,
        import: String,
    ): String? {
        val layer = Layer.ofModule(modulePath)
        val target = Layer.ofImport(import) ?: return null
        return if (target in
            FORBIDDEN_LAYERS[layer].orEmpty()
        ) {
            "${layer.label} must not depend on ${target.label}"
        } else {
            null
        }
    }

    private fun crossFeatureViolation(
        modulePath: String,
        import: String,
    ): String? {
        val isFeatureImport = Layer.ofImport(import) == Layer.FEATURE
        val ownPackage = expectedPackage(modulePath) + "."
        return if (Layer.ofModule(modulePath) == Layer.FEATURE && isFeatureImport && !import.startsWith(ownPackage)) {
            "features must not depend on other features"
        } else {
            null
        }
    }

    private fun viewModelContractViolations(type: ClassFacts): List<String> =
        buildList {
            if (!type.name.endsWith(
                    "ViewModel",
                )
            ) {
                add("${type.location}: ViewModel '${type.name}' must be named '*ViewModel'")
            }
            val exposed = type.properties.filterNot { it.isPrivate }
            if (exposed.none { UI_STATE_FLOW.matches(it.type.orEmpty().replace(" ", "")) }) {
                add("${type.location}: '${type.name}' must expose an explicitly typed StateFlow<*UiState>")
            }
            exposed
                .filter { EXPOSED_MUTABLE_STREAM.containsMatchIn(it.type.orEmpty()) }
                .forEach { add("${type.location}: '${type.name}.${it.name}' exposes mutable state '${it.type}'") }
        }
}
