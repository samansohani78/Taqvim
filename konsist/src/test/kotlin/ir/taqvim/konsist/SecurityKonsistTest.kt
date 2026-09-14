/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * T-1804 (ADR-0017): components declare whether they are exported, only `:data:events` requests network access,
 * every `PendingIntent` is immutable and `FileProvider`s share dedicated directories only.
 */
class SecurityKonsistTest {
    private val root = File(Konsist.projectRootPath)

    @Test
    fun `every manifest component declares android exported`() {
        ManifestSecurityRules.componentsWithoutExported(root).shouldBeEmpty()
    }

    @Test
    fun `only the subscriptions module requests network access`() {
        ManifestSecurityRules.manifestsRequestingInternet(root) shouldBe
            listOf("data/events/src/main/AndroidManifest.xml")
    }

    @Test
    fun `pending intents are immutable`() {
        ManifestSecurityRules.mutablePendingIntents(root).shouldBeEmpty()
    }

    @Test
    fun `file providers share dedicated directories only`() {
        ManifestSecurityRules.broadFileProviderPaths(root).shouldBeEmpty()
    }

    @Test
    fun `planted violations are detected`(
        @TempDir fixture: File,
    ) {
        write(
            File(fixture, "feature/x/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
              |<uses-permission android:name="android.permission.INTERNET"/>
              |<application><receiver android:name="a.R"/><service android:name="a.S" android:exported="false"/>
              |</application></manifest>
            """.trimMargin(),
        )
        write(
            File(fixture, "feature/x/src/main/kotlin/A.kt"),
            "val p = PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)",
        )
        write(
            File(fixture, "feature/x/src/main/kotlin/B.kt"),
            "val p = PendingIntent.getService(c, 0, i, FLAG_IMMUTABLE)",
        )
        write(
            File(fixture, "feature/x/src/main/res/xml/paths.xml"),
            """<paths><cache-path name="ok" path="shared/"/><root-path name="all" path=""/>""" +
                """<files-path name="files" path="."/></paths>""",
        )
        write(File(fixture, "feature/x/build/AndroidManifest.xml"), "<manifest/>")

        ManifestSecurityRules.componentsWithoutExported(fixture) shouldBe
            listOf("feature/x/src/main/AndroidManifest.xml: a.R")
        ManifestSecurityRules.manifestsRequestingInternet(fixture) shouldBe
            listOf("feature/x/src/main/AndroidManifest.xml")
        ManifestSecurityRules.mutablePendingIntents(fixture) shouldBe listOf("feature/x/src/main/kotlin/A.kt")
        ManifestSecurityRules.broadFileProviderPaths(fixture) shouldBe
            listOf(
                "feature/x/src/main/res/xml/paths.xml: root-path ''",
                "feature/x/src/main/res/xml/paths.xml: files-path '.'",
            )
    }

    private fun write(
        file: File,
        text: String,
    ) {
        file.parentFile.mkdirs()
        file.writeText(text)
    }
}
