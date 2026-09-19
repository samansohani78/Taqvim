/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

/** What the merged ART profile of one variant contains (T-1800). */
data class BaselineProfileReport(
    val variant: String,
    val totalRules: Int,
    val appRules: Int,
    val minimumAppRules: Int,
) {
    /** True when the variant carries enough of the app's own rules to be worth measuring. */
    val isComplete: Boolean get() = appRules >= minimumAppRules

    val message: String
        get() =
            if (isComplete) {
                "$variant baseline profile: $appRules rules of the app in $totalRules total"
            } else {
                "The $variant baseline profile has $appRules rules of the app, fewer than the $minimumAppRules " +
                    "expected ($totalRules rules in total, so the libraries' bundled profiles are there). " +
                    "The variant no longer reads src/main/generated/baselineProfiles: check the " +
                    "androidComponents block in app/build.gradle.kts, because a variant without the app's " +
                    "profile starts cold like an unoptimised build and every benchmark measured against it is " +
                    "pessimistic (T-1800, ADR-0018 addendum)."
            }
}

/**
 * T-1800: reads the merged ART profile of a variant, the text AGP hands to R8 before it renames anything, and counts
 * how many of its rules belong to the app.
 *
 * The packaged `assets/dexopt/baseline.prof` cannot answer this: it stores method and class *indices* into the dex
 * files, not names, so a profile holding nothing but the AndroidX libraries' rules looks much like a complete one
 * from the outside. Counting names in the merged text is exact, and it runs before R8 shortens them.
 */
object BaselineProfileRules {
    /** Every rule of the app's own code starts with this, in the profile's JVM form. */
    const val APP_PACKAGE = "ir/taqvim"

    /**
     * The floor for the app's rules. The generated profile holds about 6 000; a variant that stopped reading it
     * drops to 0. The floor sits far below the real figure on purpose, so ordinary changes to the app never move it
     * and only a broken wiring fails the build.
     */
    const val MINIMUM_APP_RULES: Int = 1_000

    /** Counts the lines of [profile] that name a class or method of [appPackage]. */
    fun appRules(
        profile: Sequence<String>,
        appPackage: String = APP_PACKAGE,
    ): Int = profile.count { it.contains(appPackage) }

    fun report(
        variant: String,
        profile: List<String>,
        appPackage: String = APP_PACKAGE,
        minimumAppRules: Int = MINIMUM_APP_RULES,
    ): BaselineProfileReport =
        BaselineProfileReport(
            variant = variant,
            totalRules = profile.count { it.isNotBlank() },
            appRules = appRules(profile.asSequence(), appPackage),
            minimumAppRules = minimumAppRules,
        )
}
