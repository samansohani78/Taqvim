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
 * T-1804 (ADR-0017, ADR-0054): components declare whether they are exported, no module requests network access,
 * every `PendingIntent` is immutable and `FileProvider`s share dedicated directories only.
 */
class SecurityKonsistTest {
    private val root = File(Konsist.projectRootPath)

    @Test
    fun `every manifest component declares android exported`() {
        ManifestSecurityRules.componentsWithoutExported(root).shouldBeEmpty()
    }

    @Test
    fun `no manifest requests network access`() {
        // ADR-0054: remote calendar subscriptions were the only feature that opened a socket, and they are gone,
        // so INTERNET is not requested anywhere. The watch used to take it back out of the merged manifest; now
        // nothing puts it in, so there is nothing to remove either.
        ManifestSecurityRules.manifestsRequestingInternet(root).shouldBeEmpty()
    }

    @Test
    fun `no manifest requests the network state permission`() {
        // ADR-0054: WorkManager brought this one in for the subscription refresh constraints. Both are gone.
        ManifestSecurityRules.manifestsRequestingNetworkState(root).shouldBeEmpty()
    }

    @Test
    fun `no production code uses an HTTP client`() {
        // The permission disappearing from a manifest is not the same as the fetch path being gone. This is the
        // half that would still fail if a client survived the permission's removal.
        ManifestSecurityRules.networkClientUsers(root).shouldBeEmpty()
    }

    @Test
    fun `no manifest requests a calendar permission`() {
        // ADR-0051: Taqvim does not read the device calendar. The watch used to take READ_CALENDAR back out of the
        // merged manifest; now nothing puts it in, so there is nothing to remove either.
        ManifestSecurityRules.manifestsRequestingCalendarAccess(root).shouldBeEmpty()
    }

    @Test
    fun `no production code reaches the calendar provider`() {
        // The permission disappearing from a manifest is not the same as the read path being gone. This is the
        // half that would still fail if the provider query survived the permission's removal.
        ManifestSecurityRules.calendarProviderReaders(root).shouldBeEmpty()
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
            File(fixture, "feature/x/src/main/kotlin/Net.kt"),
            "import java.net.HttpURLConnection\nval c = url.openConnection() as HttpURLConnection",
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
        // A manifest that removes a library's network permission (as :wear does, ADR-0019) does not request it.
        write(
            File(fixture, "wear/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
              |  xmlns:tools="http://schemas.android.com/tools">
              |<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>
              |</manifest>
            """.trimMargin(),
        )

        ManifestSecurityRules.componentsWithoutExported(fixture) shouldBe
            listOf("feature/x/src/main/AndroidManifest.xml: a.R")
        ManifestSecurityRules.manifestsRequestingInternet(fixture) shouldBe
            listOf("feature/x/src/main/AndroidManifest.xml")
        ManifestSecurityRules.networkClientUsers(fixture) shouldBe
            listOf("feature/x/src/main/kotlin/Net.kt: HttpURLConnection")
        ManifestSecurityRules.mutablePendingIntents(fixture) shouldBe listOf("feature/x/src/main/kotlin/A.kt")
        ManifestSecurityRules.broadFileProviderPaths(fixture) shouldBe
            listOf(
                "feature/x/src/main/res/xml/paths.xml: root-path ''",
                "feature/x/src/main/res/xml/paths.xml: files-path '.'",
            )
    }

    @Test
    fun `planted calendar violations are detected`(
        @TempDir fixture: File,
    ) {
        write(
            File(fixture, "feature/cal/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
              |<uses-permission android:name="android.permission.READ_CALENDAR"/>
              |</manifest>
            """.trimMargin(),
        )
        write(
            File(fixture, "feature/cal/src/main/kotlin/C.kt"),
            "val uri = CalendarContract.Instances.CONTENT_URI",
        )
        // A manifest that only takes the permission out (as :wear did before ADR-0051) is not requesting it.
        write(
            File(fixture, "watch/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
              |  xmlns:tools="http://schemas.android.com/tools">
              |<uses-permission android:name="android.permission.READ_CALENDAR" tools:node="remove"/>
              |</manifest>
            """.trimMargin(),
        )

        // The request and the read are found separately, so removing either one alone still fails.
        ManifestSecurityRules.manifestsRequestingCalendarAccess(fixture) shouldBe
            listOf("feature/cal/src/main/AndroidManifest.xml")
        ManifestSecurityRules.calendarProviderReaders(fixture) shouldBe
            listOf("feature/cal/src/main/kotlin/C.kt: CalendarContract")
    }

    private fun write(
        file: File,
        text: String,
    ) {
        file.parentFile.mkdirs()
        file.writeText(text)
    }
}
