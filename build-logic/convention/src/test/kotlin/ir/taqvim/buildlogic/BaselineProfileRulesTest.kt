/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/** T-1800: what the baseline-profile check counts, and what it says when a variant lost the app's profile. */
class BaselineProfileRulesTest {
    private fun libraryRules(count: Int) = List(count) { "HSPLandroidx/compose/runtime/Composer;->rule$it()V" }

    private fun appRules(count: Int) = List(count) { "HSPLir/taqvim/core/calendar/PersianCalendarSystem;->of$it()V" }

    @Test
    fun `rules of the app are counted, whatever their flags`() {
        val profile =
            listOf(
                "Lir/taqvim/app/MainActivity;",
                "HSPLir/taqvim/core/calendar/PersianCalendarSystem;->fromJdn(J)V",
                "PLandroidx/compose/ui/Modifier;->then(Landroidx/compose/ui/Modifier;)V",
            )
        BaselineProfileRules.appRules(profile.asSequence()) shouldBe 2
    }

    @Test
    fun `a complete profile passes and reports both counts`() {
        val report = BaselineProfileRules.report("release", libraryRules(4_000) + appRules(6_269))
        report.isComplete shouldBe true
        report.appRules shouldBe 6_269
        report.totalRules shouldBe 10_269
        report.message shouldContain "6269 rules of the app in 10269 total"
    }

    @Test
    fun `a profile with the libraries only fails and names the cause`() {
        val report = BaselineProfileRules.report("benchmark", libraryRules(4_131))
        report.isComplete shouldBe false
        report.appRules shouldBe 0
        report.message shouldContain "src/main/generated/baselineProfiles"
        report.message shouldContain "fewer than the 1000 expected"
    }

    @Test
    fun `the floor is the boundary, not the exact figure`() {
        BaselineProfileRules.report("benchmark", appRules(1_000)).isComplete shouldBe true
        BaselineProfileRules.report("benchmark", appRules(999)).isComplete shouldBe false
    }

    @Test
    fun `blank lines are not rules`() {
        BaselineProfileRules.report("release", appRules(2) + listOf("", "  ")).totalRules shouldBe 2
    }
}
