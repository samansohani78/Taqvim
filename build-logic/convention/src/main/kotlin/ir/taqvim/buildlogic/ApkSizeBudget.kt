/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

/** How an artifact sits inside the plan §9 size budget (T-1800). */
enum class ApkSizeStatus {
    /** Below the warning margin: nothing to report beyond the measured size. */
    WITHIN_BUDGET,

    /** At or above the warning margin but still inside the budget: the build warns, the check passes. */
    NEAR_BUDGET,

    /** Over the budget: the check fails. */
    OVER_BUDGET,
}

/**
 * The verdict on one measured artifact.
 *
 * [message] is the single line the build prints for it; [status] decides whether that line is a warning and
 * whether the check fails.
 */
data class ApkSizeReport(
    val name: String,
    val bytes: Long,
    val budgetBytes: Long,
    val warnAtPercent: Int,
) {
    /** Share of the budget used, rounded to one decimal, e.g. `86.6`. */
    val usedPercent: Double
        get() = ApkSizeBudget.percentOf(bytes, budgetBytes)

    /** Bytes that may still be added before the check fails; negative once the budget is exceeded. */
    val headroomBytes: Long
        get() = budgetBytes - bytes

    val status: ApkSizeStatus
        get() =
            when {
                bytes > budgetBytes -> ApkSizeStatus.OVER_BUDGET
                usedPercent >= warnAtPercent -> ApkSizeStatus.NEAR_BUDGET
                else -> ApkSizeStatus.WITHIN_BUDGET
            }

    val message: String
        get() =
            when (status) {
                ApkSizeStatus.WITHIN_BUDGET -> {
                    "$name: $bytes bytes, $usedPercent % of the $budgetBytes byte budget " +
                        "($headroomBytes bytes spare)"
                }

                ApkSizeStatus.NEAR_BUDGET -> {
                    "$name: $bytes bytes, $usedPercent % of the $budgetBytes byte budget — " +
                        "at or over the $warnAtPercent % warning margin, only $headroomBytes bytes spare"
                }

                ApkSizeStatus.OVER_BUDGET -> {
                    "$name is $bytes bytes, ${-headroomBytes} bytes over the $budgetBytes byte budget"
                }
            }
}

/**
 * Plan §9 size budget with a warning margin (T-1800, ADR-0018 addendum).
 *
 * The gate used to fail at the budget and say nothing below it, so a release could grow by a megabyte between
 * two commits without anyone noticing. It now warns from [WARN_AT_PERCENT] of the budget, which leaves room to
 * react before a release is blocked, and still fails only above the budget.
 */
object ApkSizeBudget {
    /** Plan §9: a release APK is at most 8 MiB. */
    const val BUDGET_BYTES: Long = 8L * 1024 * 1024

    /** Share of the budget from which the build warns. */
    const val WARN_AT_PERCENT: Int = 90

    private const val PERCENT = 100.0
    private const val ONE_DECIMAL = 10.0

    /** [bytes] as a share of [budgetBytes], rounded to one decimal. A budget of zero or less counts as full. */
    fun percentOf(
        bytes: Long,
        budgetBytes: Long,
    ): Double =
        if (budgetBytes <= 0L) {
            PERCENT
        } else {
            Math.round(bytes * PERCENT * ONE_DECIMAL / budgetBytes) / ONE_DECIMAL
        }

    fun report(
        name: String,
        bytes: Long,
        budgetBytes: Long = BUDGET_BYTES,
        warnAtPercent: Int = WARN_AT_PERCENT,
    ): ApkSizeReport {
        require(bytes >= 0L) { "$name: size must not be negative, was $bytes" }
        require(budgetBytes > 0L) { "$name: budget must be positive, was $budgetBytes" }
        require(warnAtPercent in 1..PERCENT.toInt()) { "$name: warning margin must be 1–100 %, was $warnAtPercent" }
        return ApkSizeReport(name = name, bytes = bytes, budgetBytes = budgetBytes, warnAtPercent = warnAtPercent)
    }
}
