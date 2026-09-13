/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import org.junit.jupiter.api.Test

/** Architecture rules (T-002) applied to the real code base. Runs in `check` via `:konsist:test`. */
class ArchitectureKonsistTest {
    private val production = KonsistFacts.production()
    private val allSources = KonsistFacts.allSources()

    @Test
    fun `konsist sees the project sources and build scripts`() {
        production.files.map { it.path } shouldContain "app/src/main/kotlin/ir/taqvim/app/MainActivity.kt"
        KonsistFacts.buildScripts() shouldContainKey ":feature:calendar"
    }

    @Test
    fun `packages match their module`() {
        ArchitectureRules.packageViolations(allSources.files).shouldBeEmpty()
    }

    @Test
    fun `imports respect layering and pure core stays Android free`() {
        ArchitectureRules.importViolations(allSources.files).shouldBeEmpty()
    }

    @Test
    fun `module build scripts respect layering`() {
        KonsistFacts
            .buildScripts()
            .flatMap { (module, script) -> ArchitectureRules.buildScriptViolations(module, script) }
            .shouldBeEmpty()
    }

    @Test
    fun `files have at most 400 lines`() {
        ArchitectureRules.oversizedFiles(allSources.files).shouldBeEmpty()
    }

    @Test
    fun `production functions have at most 50 lines`() {
        ArchitectureRules.oversizedFunctions(production.functions).shouldBeEmpty()
    }

    @Test
    fun `view models follow the unidirectional data flow contract`() {
        ArchitectureRules.viewModelViolations(production.classes).shouldBeEmpty()
    }

    @Test
    fun `ui states are immutable data classes`() {
        ArchitectureRules.uiStateViolations(production.classes).shouldBeEmpty()
    }

    @Test
    fun `test functions cannot be silently skipped`() {
        ArchitectureRules.testFunctionViolations(allSources.testFunctions).shouldBeEmpty()
    }

    @Test
    fun `there is no global mutable state`() {
        ArchitectureRules
            .globalMutableStateViolations(production.topLevelProperties, production.objectProperties)
            .shouldBeEmpty()
    }
}
