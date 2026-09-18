/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ApkSizeBudgetTest {
    @Test
    fun `the budget is the 8 MiB of plan section 9`() {
        ApkSizeBudget.BUDGET_BYTES shouldBe 8L * 1024 * 1024
    }

    @Test
    fun `the gate warns from 90 percent of the budget`() {
        ApkSizeBudget.WARN_AT_PERCENT shouldBe 90
    }

    @TestFactory
    fun `the status follows the warning margin and the budget`(): List<DynamicTest> =
        listOf(
            Triple(0L, ApkSizeStatus.WITHIN_BUDGET, "an empty artifact"),
            Triple(BUDGET / 2, ApkSizeStatus.WITHIN_BUDGET, "half the budget"),
            Triple(justUnder(WARN_BYTES), ApkSizeStatus.WITHIN_BUDGET, "just under the margin"),
            Triple(WARN_BYTES, ApkSizeStatus.NEAR_BUDGET, "exactly the margin"),
            Triple(BUDGET - 1, ApkSizeStatus.NEAR_BUDGET, "one byte under the budget"),
            Triple(BUDGET, ApkSizeStatus.NEAR_BUDGET, "exactly the budget"),
            Triple(BUDGET + 1, ApkSizeStatus.OVER_BUDGET, "one byte over the budget"),
        ).map { (bytes, status, label) ->
            DynamicTest.dynamicTest("$label is $status") {
                ApkSizeBudget.report(name = "app-release.apk", bytes = bytes).status shouldBe status
            }
        }

    @Test
    fun `the budget itself passes, so the gate fails only above it`() {
        val report = ApkSizeBudget.report(name = "app-release.apk", bytes = BUDGET)
        report.status shouldBe ApkSizeStatus.NEAR_BUDGET
        report.headroomBytes shouldBe 0L
        report.usedPercent shouldBe 100.0
    }

    @Test
    fun `the warning names the margin and the remaining headroom`() {
        val report = ApkSizeBudget.report(name = "app-release.apk", bytes = WARN_BYTES)
        report.message shouldContain "90 %"
        report.message shouldContain "${BUDGET - WARN_BYTES} bytes spare"
        report.usedPercent shouldBe 90.0
    }

    @Test
    fun `a size within the budget reports the spare bytes`() {
        val report = ApkSizeBudget.report(name = "app-release.apk", bytes = BUDGET / 2)
        report.message shouldContain "50.0 %"
        report.message shouldContain "${BUDGET / 2} bytes spare"
        report.status shouldBe ApkSizeStatus.WITHIN_BUDGET
    }

    @Test
    fun `an oversized artifact reports how far over it is`() {
        val report = ApkSizeBudget.report(name = "app-release.apk", bytes = BUDGET + 2_048)
        report.message shouldContain "2048 bytes over"
        report.headroomBytes shouldBe -2_048L
    }

    @TestFactory
    fun `impossible measurements are rejected`(): List<DynamicTest> =
        listOf(
            "a negative size" to { ApkSizeBudget.report(name = "a", bytes = -1L) },
            "a budget of zero" to { ApkSizeBudget.report(name = "a", bytes = 1L, budgetBytes = 0L) },
            "a margin of zero" to { ApkSizeBudget.report(name = "a", bytes = 1L, warnAtPercent = 0) },
            "a margin over 100 %" to { ApkSizeBudget.report(name = "a", bytes = 1L, warnAtPercent = 101) },
        ).map { (label, call) ->
            DynamicTest.dynamicTest("$label is rejected") { shouldThrow<IllegalArgumentException> { call() } }
        }

    @Test
    fun `percentages are rounded to one decimal`() {
        ApkSizeBudget.percentOf(bytes = 1L, budgetBytes = 3L) shouldBe 33.3
        ApkSizeBudget.percentOf(bytes = 2L, budgetBytes = 3L) shouldBe 66.7
    }

    private companion object {
        const val BUDGET = ApkSizeBudget.BUDGET_BYTES
        const val WARN_BYTES = BUDGET / 100 * ApkSizeBudget.WARN_AT_PERCENT

        /** The largest size that still rounds below the warning margin. */
        fun justUnder(bytes: Long): Long {
            var candidate = bytes - 1
            while (ApkSizeBudget.percentOf(candidate, BUDGET) >= ApkSizeBudget.WARN_AT_PERCENT) candidate--
            return candidate
        }
    }
}
