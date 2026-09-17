/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class LicensePolicyTest {
    private val runtime = setOf(LicenseScope.RUNTIME)
    private val test = setOf(LicenseScope.TEST)
    private val allowList =
        AllowList(
            licenses =
                mapOf(
                    "Apache-2.0" to LicenseScope.entries.toSet(),
                    "MIT" to LicenseScope.entries.toSet(),
                    "EPL-2.0" to setOf(LicenseScope.TEST),
                ),
            overrides =
                listOf(
                    LicenseOverride("com.example:nometa", "MIT", "https://example.com/LICENSE"),
                    LicenseOverride(
                        "com.example:pinned",
                        "Apache-2.0",
                        "https://example.com/LICENSE",
                        versions = "2.0",
                    ),
                ),
        )

    private fun dep(
        coordinate: String,
        scopes: Set<LicenseScope>,
        vararg spdx: String?,
    ) = DependencyLicenseInfo(
        ModuleCoordinate.parse(coordinate),
        scopes,
        spdx.map { DeclaredLicense(it ?: "Custom", null, it) },
    )

    private fun reason(dependency: DependencyLicenseInfo) = LicensePolicy.evaluate(dependency, allowList)?.reason

    @Test
    fun `permissive licenses pass in every scope`() {
        reason(dep("a:b:1", LicenseScope.entries.toSet(), "Apache-2.0")) shouldBe null
    }

    @Test
    fun `test-only licenses are rejected when shipped`() {
        reason(dep("org.junit:junit:5", test, "EPL-2.0")) shouldBe null
        reason(dep("org.junit:junit:5", runtime, "EPL-2.0")) shouldBe LicenseViolation.Reason.NOT_ALLOWED
        reason(dep("org.junit:junit:5", runtime + test, "EPL-2.0")) shouldBe LicenseViolation.Reason.NOT_ALLOWED
    }

    @Test
    fun `debug-only dependencies follow the runtime rules`() {
        val debug = setOf(LicenseScope.DEBUG)
        reason(dep("org.junit:junit:5", debug, "EPL-2.0")) shouldBe LicenseViolation.Reason.NOT_ALLOWED
        reason(dep("gpl:lib:1", debug, "GPL-2.0-or-later")) shouldBe LicenseViolation.Reason.NOT_ALLOWED
        val runtimeOnly = AllowList(licenses = mapOf("Apache-2.0" to runtime), overrides = emptyList())
        LicensePolicy.evaluate(dep("a:b:1", debug, "Apache-2.0"), runtimeOnly) shouldBe null
    }

    @Test
    fun `copyleft and unknown licenses fail`() {
        reason(dep("org.mariadb.jdbc:mariadb-java-client:3", runtime, "LGPL-2.1-or-later")) shouldBe
            LicenseViolation.Reason.NOT_ALLOWED
        reason(dep("gpl:lib:1", test, "GPL-2.0-or-later")) shouldBe LicenseViolation.Reason.NOT_ALLOWED
        reason(dep("mystery:lib:1", runtime, null)) shouldBe LicenseViolation.Reason.UNKNOWN_LICENSE
        reason(dep("mystery:lib:1", runtime)) shouldBe LicenseViolation.Reason.UNKNOWN_LICENSE
    }

    @Test
    fun `dual licensing passes when one alternative is allowed`() {
        reason(dep("jakarta:api:1", runtime, "GPL-2.0-with-classpath-exception", "Apache-2.0")) shouldBe null
    }

    @Test
    fun `overrides assign a license, optionally per version`() {
        reason(dep("com.example:nometa:9", runtime, null)) shouldBe null
        reason(dep("com.example:pinned:2.0", runtime, null)) shouldBe null
        reason(dep("com.example:pinned:3.0", runtime, null)) shouldBe LicenseViolation.Reason.UNKNOWN_LICENSE
    }

    @Test
    fun `evaluates all dependencies sorted and summarises`() {
        val deps =
            listOf(
                dep("z:z:1", runtime, "LGPL-2.1-or-later"),
                dep("a:a:1", runtime, "MIT"),
                dep("b:b:1", runtime, null),
            )

        val violations = LicensePolicy.evaluate(deps, allowList)
        val summary = LicensePolicy.summary(deps, violations)

        violations.map { it.dependency.coordinate.toString() } shouldContainExactly listOf("b:b:1", "z:z:1")
        summary shouldContain "3 external modules scanned, 2 violation(s)"
        summary shouldContain "z:z:1 [runtime] NOT_ALLOWED — licenses: 'LGPL-2.1-or-later' (LGPL-2.1-or-later)"
        summary shouldContain "b:b:1 [runtime] UNKNOWN_LICENSE — licenses: 'Custom'"
        summary shouldContain "docs/adr/0003-license-policy.md"
    }

    @Test
    fun `clean summary has no policy footer`() {
        val deps = listOf(dep("a:a:1", runtime, "MIT"))

        LicensePolicy.evaluate(deps, allowList).shouldBeEmpty()
        LicensePolicy.summary(deps, emptyList()).contains("Policy:") shouldBe false
    }

    @Test
    fun `describes dependencies without any declared license`() {
        val violation = LicenseViolation(dep("x:y:1", runtime + test), LicenseViolation.Reason.UNKNOWN_LICENSE)

        violation.describe() shouldContain "x:y:1 [runtime+test] UNKNOWN_LICENSE — licenses: none declared"
    }
}
