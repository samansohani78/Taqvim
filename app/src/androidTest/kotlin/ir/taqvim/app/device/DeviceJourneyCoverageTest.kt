/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import org.junit.Test

/**
 * The declarative half of R11: [JourneyRegistry] is data, so a journey that regresses to "no test" fails here instead
 * of the suite silently covering fewer of PLAN §8.3's scenarios than it claims to (mirrors how
 * `benchmark/required.json` plus `tools/benchmark/compare_benchmarks.py --required` fail a nightly run that is
 * missing a required benchmark, rather than only comparing whatever happened to be reported).
 *
 * Every [JourneyStatus.COVERED] or [JourneyStatus.PARTIAL] entry names a `ClassName#methodName`; this test loads that
 * class by reflection and requires a `@Test` method of that name to exist, so renaming or deleting a device test
 * without updating the registry fails immediately rather than leaving a stale claim. [JourneyStatus.MISSING] entries
 * make no claim and are only reported (docs/device-journeys.md carries the plan to close them).
 */
class DeviceJourneyCoverageTest {
    @Test
    fun everyJourneyHasAStableUniqueId() {
        val ids = JourneyRegistry.ALL.map { it.id }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }
        check(ids.toSet().size == ids.size) { "duplicate journey ids: $duplicates" }
    }

    @Test
    fun idsRunFromJ01ToJ40WithNoGaps() {
        val expected = (1..JOURNEY_COUNT).map { "J%02d".format(it) }
        check(JourneyRegistry.ALL.map { it.id } == expected) {
            "expected ids $expected in order, got ${JourneyRegistry.ALL.map { it.id }}"
        }
    }

    @Test
    fun everyClaimedJourneyNamesARealTest() {
        val problems = JourneyRegistry.ALL.filter { it.status != JourneyStatus.MISSING }.flatMap(::problemsOf)
        check(problems.isEmpty()) { problems.joinToString(separator = "\n") }
    }

    @Test
    fun everyMissingJourneyNamesNoTest() {
        val problems =
            JourneyRegistry.ALL
                .filter { it.status == JourneyStatus.MISSING && it.coveringTest != null }
                .map { "${it.id} is MISSING but names a test (${it.coveringTest}); mark it COVERED or PARTIAL instead" }
        check(problems.isEmpty()) { problems.joinToString(separator = "\n") }
    }

    /** Every reflection failure for [journey]'s `coveringTest` references, or the missing-reference failure itself. */
    private fun problemsOf(journey: Journey): List<String> {
        val reference = journey.coveringTest
        if (reference.isNullOrBlank()) return listOf("${journey.id} is ${journey.status} but names no covering test")
        return reference.split(REFERENCE_SEPARATOR).mapNotNull { spec -> problemFor(journey.id, spec.trim()) }
    }

    /** `null` when [spec] ("ClassName#methodName", or "module:..." for coverage outside this module) resolves. */
    private fun problemFor(
        journeyId: String,
        spec: String,
    ): String? =
        if (spec.startsWith(OTHER_MODULE_PREFIX)) {
            null // Recorded, not reflectively checkable from here.
        } else {
            val parts = spec.split(METHOD_SEPARATOR)
            if (parts.size != 2) {
                "$journeyId: '$spec' is not 'ClassName#methodName' or 'module:...'"
            } else {
                val (className, methodName) = parts
                runCatching { Class.forName("$DEVICE_PACKAGE.$className") }.fold(
                    onSuccess = { testClass -> methodProblem(journeyId, className, methodName, testClass) },
                    onFailure = { error -> "$journeyId: test class $className does not exist ($error)" },
                )
            }
        }

    /** `null` when [testClass] has a `@Test`-annotated method named [methodName]. */
    private fun methodProblem(
        journeyId: String,
        className: String,
        methodName: String,
        testClass: Class<*>,
    ): String? {
        val hasTest = testClass.methods.any { it.name == methodName && it.isAnnotationPresent(Test::class.java) }
        return if (hasTest) null else "$journeyId: $className has no @Test method named $methodName"
    }

    private companion object {
        const val JOURNEY_COUNT = 40
        const val DEVICE_PACKAGE = "ir.taqvim.app.device"
        const val OTHER_MODULE_PREFIX = "module:"
        const val METHOD_SEPARATOR = "#"
        const val REFERENCE_SEPARATOR = ";"
    }
}
