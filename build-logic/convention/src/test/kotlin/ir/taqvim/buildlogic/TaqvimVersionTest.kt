/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TaqvimVersionTest {
    @TestFactory
    fun `valid tags give the documented name and code`(): List<DynamicTest> =
        listOf(
            Triple("v0.1.0", "0.1.0", 10_099),
            Triple("0.1.0", "0.1.0", 10_099),
            Triple("v1.0.0-beta.3", "1.0.0-beta.3", 1_000_003),
            Triple("v1.0.0", "1.0.0", 1_000_099),
            Triple("v1.2.3-beta.4", "1.2.3-beta.4", 1_020_304),
            Triple("v2099.99.99", "2099.99.99", 2_099_999_999),
            Triple(" v3.10.20-beta.98 ", "3.10.20-beta.98", 3_102_098),
        ).map { (tag, name, code) ->
            DynamicTest.dynamicTest(tag) {
                val version = TaqvimVersion.parse(tag, "test")
                version.name shouldBe name
                version.code shouldBe code
            }
        }

    @TestFactory
    fun `malformed or out-of-range tags are rejected`(): List<DynamicTest> =
        listOf(
            "",
            "v",
            "v1.0",
            "v1.0.0.0",
            "V1.0.0",
            "vv1.0.0",
            "v01.0.0",
            "v1.00.0",
            "v1.0.-1",
            "v1.100.0",
            "v1.0.100",
            "v2100.0.0",
            "v1.0.0-beta",
            "v1.0.0-beta.0",
            "v1.0.0-beta.99",
            "v1.0.0-beta.01",
            "v1.0.0-rc.1",
            "v1.0.0-beta.1+build",
            "release-1.0.0",
        ).map { tag ->
            DynamicTest.dynamicTest("'$tag'") {
                TaqvimVersion.parseOrNull(tag).shouldBeNull()
                shouldThrow<IllegalArgumentException> { TaqvimVersion.parse(tag, "-Ptaqvim.version") }
                    .message
                    .orEmpty() shouldContain "-Ptaqvim.version"
            }
        }

    @Test
    fun `constructor rejects parts outside the formula`() {
        shouldThrow<IllegalArgumentException> { TaqvimVersion(0, 100, 0) }
        shouldThrow<IllegalArgumentException> { TaqvimVersion(0, 0, 0, beta = 99) }
        shouldThrow<IllegalArgumentException> { TaqvimVersion(2100, 0, 0) }
    }

    @Test
    fun `codes follow version order`(): Unit =
        runBlocking {
            checkAll(versions, versions) { a, b ->
                if (order.compare(a, b) < 0) a.code shouldBeLessThan b.code
                if (order.compare(a, b) == 0) a.code shouldBe b.code
                TaqvimVersion.parse("v${a.name}", "test") shouldBe a
            }
        }

    @Test
    fun `betas come before their final release and patches before minors`() {
        TaqvimVersion(1, 0, 0, beta = 98).code shouldBeLessThan TaqvimVersion(1, 0, 0).code
        TaqvimVersion(1, 0, 0).code shouldBeLessThan TaqvimVersion(1, 0, 1, beta = 1).code
        TaqvimVersion(1, 0, 99).code shouldBeLessThan TaqvimVersion(1, 1, 0, beta = 1).code
        TaqvimVersion(1, 99, 99).code shouldBeLessThan TaqvimVersion(2, 0, 0, beta = 1).code
    }

    @Test
    fun `the Gradle property wins over version properties`() {
        val file = "# Local default\nversion=0.1.0\n"
        TaqvimVersion.resolve("v1.2.3-beta.4", file).name shouldBe "1.2.3-beta.4"
        TaqvimVersion.resolve(null, file).code shouldBe 10_099
        TaqvimVersion.resolve("  ", file).name shouldBe "0.1.0"
        shouldThrow<IllegalArgumentException> { TaqvimVersion.resolve(null, "name=0.1.0") }
        shouldThrow<IllegalArgumentException> { TaqvimVersion.resolve(null, null) }
        shouldThrow<IllegalArgumentException> { TaqvimVersion.resolve(null, "version=1.0") }
            .message
            .orEmpty() shouldContain "version.properties"
    }

    private companion object {
        val versions =
            arbitrary {
                TaqvimVersion(
                    major = Arb.int(0..2099).bind(),
                    minor = Arb.int(0..99).bind(),
                    patch = Arb.int(0..99).bind(),
                    beta = Arb.int(1..98).orNull(0.3).bind(),
                )
            }

        val order: Comparator<TaqvimVersion> =
            compareBy<TaqvimVersion>({ it.major }, { it.minor }, { it.patch })
                .thenBy { it.beta ?: TaqvimVersion.FINAL_STAGE }
    }
}
