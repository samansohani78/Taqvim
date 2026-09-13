/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ArchitectureRulesTest {
    private fun file(
        module: String?,
        packageName: String?,
        vararg imports: String,
        lines: Int = 10,
    ) = FileFacts("x/File.kt", module, packageName, imports.toList(), lines)

    private fun property(
        name: String,
        type: String? = null,
        isVar: Boolean = false,
        isPrivate: Boolean = false,
        declaration: String = "val $name",
    ) = PropertyFacts("x/File.kt:1:1", name, type, isVar, isPrivate, declaration)

    private fun type(
        name: String,
        parents: List<String> = emptyList(),
        isData: Boolean = false,
        properties: List<PropertyFacts> = emptyList(),
    ) = ClassFacts("x/File.kt:1:1", name, parents, isData, properties)

    private fun violation(
        module: String,
        import: String,
    ) = ArchitectureRules.importViolation(module, import)

    @Test
    fun `layers are derived from module paths and imports`() {
        Layer.ofModule(":feature:calendar") shouldBe Layer.FEATURE
        Layer.ofModule(":tools:dataset") shouldBe Layer.OTHER
        Layer.ofModule(":app") shouldBe Layer.APP
        Layer.ofImport("ir.taqvim.data.events.Repo") shouldBe Layer.DATA
        Layer.ofImport("ir.taqvim.buildlogic.Thing") shouldBe null
        Layer.ofImport("kotlinx.coroutines.flow.Flow") shouldBe null
    }

    @Test
    fun `expected packages follow the namespace convention`() {
        ArchitectureRules.expectedPackage(":core:ui-testing") shouldBe "ir.taqvim.core.uitesting"
        ArchitectureRules.expectedPackage(":data:device-calendar") shouldBe "ir.taqvim.data.devicecalendar"
        ArchitectureRules.expectedPackage(":build-logic:convention") shouldBe "ir.taqvim.buildlogic"
    }

    @Test
    fun `package rule accepts sub-packages and rejects foreign packages`() {
        val files =
            listOf(
                file(":core:model", "ir.taqvim.core.model"),
                file(":core:model", "ir.taqvim.core.model.time"),
                file(":core:model", "ir.taqvim.core.modelx"),
                file(":feature:month", null),
                file(null, "anything"),
            )

        ArchitectureRules.packageViolations(files) shouldHaveSize 2
    }

    @Test
    fun `pure core modules must not import Android`() {
        val pureCore = "pure :core modules must not use Android APIs"

        violation(":core:model", "android.util.Log") shouldBe pureCore
        violation(":core:calendar", "androidx.annotation.Keep") shouldBe pureCore
        violation(":core:ui", "android.graphics.Paint") shouldBe null
        violation(":core:ui-testing", "androidx.compose.ui.test.junit4.v2.createComposeRule") shouldBe null
        violation(":data:location", "android.location.LocationManager") shouldBe null
    }

    @Test
    fun `imports must respect layering`() {
        violation(":core:model", "ir.taqvim.data.events.Repo") shouldBe ":core must not depend on :data"
        violation(":core:events", "ir.taqvim.feature.calendar.Screen") shouldBe ":core must not depend on :feature"
        violation(":data:events", "ir.taqvim.feature.calendar.Screen") shouldBe ":data must not depend on :feature"
        violation(":feature:times", "ir.taqvim.data.scheduler.Alarm") shouldBe ":feature must not depend on :data"
        violation(":feature:times", "ir.taqvim.app.MainActivity") shouldBe ":feature must not depend on :app"
        violation(":feature:times", "ir.taqvim.core.praytimes.PrayerTimes") shouldBe null
        violation(":app", "ir.taqvim.data.events.Repo") shouldBe null
    }

    @Test
    fun `features may only import their own feature`() {
        val crossFeature = "features must not depend on other features"

        violation(":feature:calendar", "ir.taqvim.feature.calendar.detail.Tab") shouldBe null
        violation(":feature:calendar", "ir.taqvim.feature.month.MonthScreen") shouldBe crossFeature
        violation(":feature:calendar", "ir.taqvim.feature.calendarx.Clash") shouldBe crossFeature
    }

    @Test
    fun `import violations are reported per file`() {
        val coreFile = file(":core:model", "ir.taqvim.core.model", "android.util.Log", "kotlin.math.abs")
        val unknownModule = file(null, "x", "android.os.Build")

        val violations = ArchitectureRules.importViolations(listOf(coreFile, unknownModule))

        violations.single() shouldContain "import android.util.Log"
    }

    @Test
    fun `build scripts must respect layering`() {
        val featureScript =
            "dependencies {\n implementation(projects.feature.month)\n" +
                " implementation(project(\":data:x\"))\n}"
        val androidx = "implementation(libs.androidx.core.ktx)"

        fun scripts(
            module: String,
            script: String,
        ) = ArchitectureRules.buildScriptViolations(module, script)

        scripts(":feature:calendar", featureScript) shouldHaveSize 2
        scripts(":feature:calendar", "implementation(projects.core.model)").shouldBeEmpty()
        scripts(":core:model", "implementation(projects.app)").single() shouldContain ":core must not depend on :app"
        scripts(":core:calendar", androidx).single() shouldContain "AndroidX"
        scripts(":core:ui", androidx).shouldBeEmpty()
        scripts(":app", "implementation(projects.feature.calendar)").shouldBeEmpty()
        scripts(":data:events", "implementation(projects.data.database)").shouldBeEmpty()
    }

    @Test
    fun `size limits flag only what exceeds them`() {
        val files = listOf(file(":app", "ir.taqvim.app", lines = 400), file(":app", "ir.taqvim.app", lines = 401))
        val functions = listOf(FunctionFacts("a:1", 50), FunctionFacts("b:1", 51))

        ArchitectureRules.oversizedFiles(files) shouldHaveSize 1
        ArchitectureRules.oversizedFunctions(functions).single() shouldContain "b:1: function has 51 lines"
    }

    @Test
    fun `view models must be named, typed and expose immutable state`() {
        val state = property("uiState", "StateFlow<MonthUiState>")
        val leakyProperties =
            listOf(
                property("uiState", "StateFlow< YearUiState >"),
                property("events", "MutableSharedFlow<Unit>"),
                property("hidden", "MutableStateFlow<Int>", isPrivate = true),
            )

        fun report(type: ClassFacts) = ArchitectureRules.viewModelViolations(listOf(type))

        report(type("MonthViewModel", listOf("ViewModel"), properties = listOf(state))).shouldBeEmpty()
        report(type("MonthScreenModel", listOf("ViewModel"), properties = listOf(state))).single() shouldContain
            "must be named '*ViewModel'"
        val untyped = type("DayViewModel", listOf("AndroidViewModel"), properties = listOf(property("uiState")))
        report(untyped).single() shouldContain "StateFlow<*UiState>"
        report(type("YearViewModel", listOf("ViewModel"), properties = leakyProperties)).single() shouldContain
            "'YearViewModel.events' exposes mutable state"
        report(type("FakeViewModel")).single() shouldContain "does not extend ViewModel"
    }

    @Test
    fun `ui states must be immutable data classes`() {
        val good = type("MonthUiState", isData = true, properties = listOf(property("days", "List<Int>")))
        val badProperties =
            listOf(
                property("title", "String", isVar = true),
                property("cells", "MutableList<Int>"),
                property("raw", "IntArray"),
            )

        ArchitectureRules.uiStateViolations(listOf(good, type("NotAState"))).shouldBeEmpty()
        ArchitectureRules.uiStateViolations(listOf(type("DayUiState", properties = badProperties))) shouldHaveSize 4
    }

    @Test
    fun `global mutable state is rejected`() {
        val topLevel =
            listOf(
                property("counter", "Int", isVar = true),
                property("cache", declaration = "private val cache = mutableMapOf<String, Int>()"),
                property("state", declaration = "val state = MutableStateFlow(0)"),
                property("names", declaration = "val names = listOf(\"a\")"),
                property("MAX", declaration = "const val MAX = 3"),
            )
        val objectMembers =
            listOf(
                property("hits", declaration = "val hits = AtomicInteger()"),
                property("id", declaration = "val id = 1"),
            )

        val violations = ArchitectureRules.globalMutableStateViolations(topLevel, objectMembers)

        violations.map { it.substringAfter(": ") } shouldContainExactly
            listOf(
                "top-level property 'counter' is a var",
                "top-level property 'cache' holds mutable state",
                "top-level property 'state' holds mutable state",
                "object property 'hits' holds mutable state",
            )
    }

    @Test
    fun `mutable-sounding words inside string literals are not state`() {
        val regexConstant = "private val HOLDER = Regex(\"\"\"\\b(MutableStateFlow|mutableListOf)\\b\"\"\")"
        val message = "val HINT = \"Use MutableStateFlow in a ViewModel\""
        val real = "val state = MutableStateFlow(0) // \"quoted\""

        val violations =
            ArchitectureRules.globalMutableStateViolations(
                topLevel =
                    listOf(
                        property("HOLDER", declaration = regexConstant),
                        property("HINT", declaration = message),
                    ),
                objectMembers = listOf(property("state", declaration = real)),
            )

        violations.single() shouldContain "object property 'state' holds mutable state"
    }

    @Test
    fun `expression-bodied tests must declare Unit`() {
        val facts =
            listOf(
                TestFunctionFacts("a:1", hasExpressionBody = true, declaresReturnType = false),
                TestFunctionFacts("b:1", hasExpressionBody = true, declaresReturnType = true),
                TestFunctionFacts("c:1", hasExpressionBody = false, declaresReturnType = false),
            )

        ArchitectureRules.testFunctionViolations(facts).single() shouldContain
            "a:1: expression-bodied @Test must declare"
    }

    @Test
    fun `module paths are derived from the source location`() {
        KonsistFacts.modulePathOf("/repo", "/repo/core/ui-testing/src/main/kotlin/A.kt") shouldBe ":core:ui-testing"
        KonsistFacts.modulePathOf("/repo/", "/repo/app/src/test/kotlin/B.kt") shouldBe ":app"
        KonsistFacts.modulePathOf("/repo", "/repo/build.gradle.kts") shouldBe null
        KonsistFacts.modulePathOf("/repo", "/elsewhere/app/src/main/kotlin/C.kt") shouldBe null
        KonsistFacts.modulePathOf("/repo", "/repo/src/main/kotlin/D.kt") shouldBe null
    }

    @Test
    fun `declaration lines start at the keyword`() {
        val text = "/**\n * Docs.\n */\n@Suppress(\"x\")\nfun a() {\n}"
        val funKeyword = Regex("""\bfun\b""")

        KonsistFacts.declarationLines(text, funKeyword) shouldContainExactly listOf("fun a() {", "}")
        KonsistFacts.declarationLines("no keyword", funKeyword) shouldContainExactly listOf("no keyword")
    }

    @Test
    fun `build scripts are discovered per module`(
        @TempDir root: File,
    ) {
        File(root, "build.gradle.kts").writeText("root")
        File(root, "feature/month/build/generated").mkdirs()
        File(root, "feature/month/build.gradle.kts").writeText("month")
        File(root, "feature/month/build/generated/build.gradle.kts").writeText("ignored")

        KonsistFacts.buildScripts(root) shouldBe mapOf(":feature:month" to "month")
    }
}
