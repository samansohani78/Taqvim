/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1214 (U): one launcher entry per plan, the day alias only for a real day while the dynamic icon is on. */
class LauncherIconPlanTest {
    @Test
    fun `day aliases are the manifest's two-digit names`() {
        LauncherIcons.dayAlias(1) shouldBe "ir.taqvim.app.LauncherDay01"
        LauncherIcons.dayAlias(31) shouldBe "ir.taqvim.app.LauncherDay31"
        LauncherIcons.all.size shouldBe 32
        LauncherIcons.all.distinct().size shouldBe 32
        shouldThrow<IllegalArgumentException> { LauncherIcons.dayAlias(0) }
        shouldThrow<IllegalArgumentException> { LauncherIcons.dayAlias(32) }
    }

    @Test
    fun `the dynamic icon shows the day and falls back to the default`() {
        LauncherIconPlan.of(22, dynamic = true).enabled shouldBe LauncherIcons.dayAlias(22)
        LauncherIconPlan.of(22, dynamic = false).enabled shouldBe LauncherIcons.DEFAULT
        LauncherIconPlan.of(32, dynamic = true).enabled shouldBe LauncherIcons.DEFAULT
        LauncherIconPlan.of(null, dynamic = true).enabled shouldBe LauncherIcons.DEFAULT
    }

    @Test
    fun `property - exactly one entry is enabled and every other one is disabled`(): Unit =
        runTest {
            checkAll(PropertyTesting.iterations, Arb.int(-5..40).orNull(0.1), Arb.boolean()) { day, dynamic ->
                val plan = LauncherIconPlan.of(day, dynamic)

                (plan.disabled + plan.enabled).sorted() shouldBe LauncherIcons.all.sorted()
                (plan.enabled in plan.disabled) shouldBe false
                val wanted =
                    if (dynamic && day != null &&
                        day in 1..31
                    ) {
                        LauncherIcons.dayAlias(day)
                    } else {
                        LauncherIcons.DEFAULT
                    }
                plan.enabled shouldBe wanted
            }
        }
}
