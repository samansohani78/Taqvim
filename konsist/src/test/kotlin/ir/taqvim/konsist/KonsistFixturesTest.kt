/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * End-to-end negative tests: Konsist parses fixture trees under `konsist/src/test/resources/fixtures` that
 * mimic the module layout, and every rule must flag the planted violations while accepting compliant code.
 */
class KonsistFixturesTest {
    private val violations = KonsistFacts.fixture("violations")
    private val compliant = KonsistFacts.fixture("compliant")

    @Test
    fun `fixture trees resolve module paths`() {
        violations.files.map { it.modulePath }.toSet() shouldContainAll
            listOf(":core:model", ":feature:calendar", ":data:events")
        compliant.files.map { it.modulePath }.toSet() shouldContainAll listOf(":core:ui", ":feature:month")
    }

    @Test
    fun `layering and Android violations are detected`() {
        val found = ArchitectureRules.importViolations(violations.files)

        found shouldHaveSize 5
        found.joinToString("\n").let { report ->
            report shouldContain "import android.util.Log — pure :core modules must not use Android APIs"
            report shouldContain
                "import ir.taqvim.feature.calendar.CalendarScreen — :core must not depend on :feature"
            report shouldContain "import ir.taqvim.data.events.EventsRepository — :feature must not depend on :data"
            report shouldContain
                "import ir.taqvim.feature.month.MonthScreen — features must not depend on other features"
            report shouldContain
                "import ir.taqvim.feature.calendar.CalendarViewModel — :data must not depend on :feature"
        }
    }

    @Test
    fun `misplaced packages are detected`() {
        ArchitectureRules.packageViolations(violations.files).single() shouldContain "package 'ir.taqvim.wrong'"
    }

    @Test
    fun `view model contract violations are detected`() {
        val report = ArchitectureRules.viewModelViolations(violations.classes).joinToString("\n")

        report shouldContain "ViewModel 'CalendarScreenModel' must be named '*ViewModel'"
        report shouldContain "'CalendarViewModel' must expose an explicitly typed StateFlow<*UiState>"
        report shouldContain "'CalendarViewModel.state' exposes mutable state"
        report shouldContain "'PretendViewModel' does not extend ViewModel"
    }

    @Test
    fun `mutable ui state is detected`() {
        val report = ArchitectureRules.uiStateViolations(violations.classes).joinToString("\n")

        report shouldContain "'CalendarUiState' must be a data class"
        report shouldContain "'CalendarUiState.title' must be val"
        report shouldContain "'CalendarUiState.selected' has mutable type"
    }

    @Test
    fun `global mutable state is detected`() {
        val report =
            ArchitectureRules
                .globalMutableStateViolations(violations.topLevelProperties, violations.objectProperties)
                .joinToString("\n")

        report shouldContain "top-level property 'lastMessage' is a var"
        report shouldContain "object property 'listeners' holds mutable state"
    }

    @Test
    fun `compliant code passes every rule`() {
        ArchitectureRules.packageViolations(compliant.files).shouldBeEmpty()
        ArchitectureRules.importViolations(compliant.files).shouldBeEmpty()
        ArchitectureRules.viewModelViolations(compliant.classes).shouldBeEmpty()
        ArchitectureRules.uiStateViolations(compliant.classes).shouldBeEmpty()
        ArchitectureRules
            .globalMutableStateViolations(
                compliant.topLevelProperties,
                compliant.objectProperties,
            ).shouldBeEmpty()
        ArchitectureRules.oversizedFunctions(compliant.functions).shouldBeEmpty()
    }
}
