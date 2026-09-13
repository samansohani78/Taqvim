/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.buildlogic.license

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PomLicensesTest {
    private fun pom(
        licenses: String = "",
        parent: String = "",
    ) = """<?xml version="1.0"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          $parent
          <licenses>$licenses</licenses>
        </project>"""

    private fun parent(
        artifact: String,
        version: String = "1",
    ) = "<parent><groupId>g</groupId><artifactId>$artifact</artifactId><version>$version</version></parent>"

    private fun license(
        name: String? = null,
        url: String? = null,
    ) = "<license>" + name.orEmpty().let { if (name == null) "" else "<name>$it</name>" } +
        url.orEmpty().let { if (url == null) "" else "<url>$it</url>" } + "</license>"

    @Test
    fun `parses licenses and parent`() {
        val apacheUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"

        val metadata =
            PomLicenseParser.parse(
                pom(
                    licenses = license("\n  The Apache Software License,\n Version 2.0 ", apacheUrl),
                    parent = parent("p"),
                ),
            )

        metadata.licenses shouldContainExactly
            listOf(DeclaredLicense("The Apache Software License, Version 2.0", apacheUrl, "Apache-2.0"))
        metadata.parent shouldBe "g:p:1"
    }

    @Test
    fun `splits disjunctive licenses and keeps unnamed ones`() {
        val licenses =
            license("EPL-2.0 OR GPL-2.0-with-classpath-exception") +
                license(url = "https://opensource.org/licenses/MIT")

        val metadata = PomLicenseParser.parse(pom(licenses = licenses))

        metadata.licenses.map { it.spdxId } shouldContainExactly
            listOf("EPL-2.0", "GPL-2.0-with-classpath-exception", "MIT")
        metadata.parent shouldBe null
    }

    @Test
    fun `ignores parents with unresolved properties`() {
        PomLicenseParser.parse(pom(parent = parent("a", version = "\${revision}"))).parent shouldBe null
    }

    @Test
    fun `rejects doctype declarations`() {
        val xxe =
            """<?xml version="1.0"?>""" +
                """<!DOCTYPE p [<!ENTITY x SYSTEM "file:///etc/passwd">]><project>&x;</project>"""

        shouldThrowAny { PomLicenseParser.parse(xxe) }
    }

    @Test
    fun `inherits licenses from parents and grandparents`() {
        val poms =
            mapOf(
                "g:child:1" to pom(parent = parent("parent")),
                "g:parent:1" to pom(parent = parent("root")),
                "g:root:1" to pom(licenses = license("MIT")),
                "g:own:1" to pom(licenses = license("ISC")),
                "g:broken:1" to "<not-xml",
            )
        val requests = mutableListOf<Collection<String>>()
        val resolver =
            PomLicenseResolver({ wanted ->
                requests.add(wanted)
                poms.filterKeys { it in wanted }
            })

        val resolved = resolver.resolve(listOf("g:child:1", "g:own:1", "g:missing:1", "g:broken:1"))

        resolved.getValue("g:child:1").map { it.spdxId } shouldContainExactly listOf("MIT")
        resolved.getValue("g:own:1").map { it.spdxId } shouldContainExactly listOf("ISC")
        resolved.getValue("g:missing:1").shouldBeEmpty()
        resolved.getValue("g:broken:1").shouldBeEmpty()
        requests.size shouldBe 3
    }

    @Test
    fun `finds cached poms in the gradle module cache layout`(
        @TempDir filesRoot: File,
    ) {
        val coordinate = ModuleCoordinate("androidx.test", "core", "1.7.0")
        File(filesRoot, "androidx.test/core/1.7.0/aaa").mkdirs()
        File(filesRoot, "androidx.test/core/1.7.0/aaa/core-1.7.0.aar").writeText("binary")
        val pomFile = File(filesRoot, "androidx.test/core/1.7.0/bbb/core-1.7.0.pom").apply { parentFile.mkdirs() }
        pomFile.writeText(pom(licenses = license("Apache-2.0")))

        GradleModuleCache.findPom(filesRoot, coordinate) shouldBe pomFile
        GradleModuleCache.findPom(filesRoot, coordinate.copy(version = "9.9.9")) shouldBe null
    }

    @Test
    fun `stops following parents at the depth limit`() {
        val chain = (0..3).associate { level -> "g:p$level:1" to pom(parent = parent("p${level + 1}")) }
        val poms = chain + ("g:p4:1" to pom(licenses = license("MIT")))

        PomLicenseResolver({ wanted -> poms.filterKeys { it in wanted } }, maxParentDepth = 2)
            .resolve(listOf("g:p0:1"))
            .getValue("g:p0:1")
            .shouldBeEmpty()
    }
}
