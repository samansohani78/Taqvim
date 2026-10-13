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
    fun `no manifest requests a broad storage permission`() {
        // One document at a time, picked by the user through the Storage Access Framework (T-1003). The debug
        // manifest removes LeakCanary's legacy pair; a removal is not a request, so it does not appear here.
        ManifestSecurityRules.manifestsRequestingStorage(root).shouldBeEmpty()
    }

    @Test
    fun `no production code schedules background work`() {
        // ADR-0054: the only background work is one AlarmManager alarm. WorkManager stays on the runtime classpath
        // because Glance depends on it, so nothing but this stops a worker being enqueued again — and a worker
        // with a network constraint brings ACCESS_NETWORK_STATE back with it.
        ManifestSecurityRules.backgroundWorkSchedulers(root).shouldBeEmpty()
    }

    @Test
    fun `no manifest requests a device location permission`() {
        // ADR-0055: a place is a bundled city, a point on the map or typed coordinates. Taqvim never asks a device
        // where it is, so the question is never put to the user either.
        ManifestSecurityRules.manifestsRequestingLocation(root).shouldBeEmpty()
    }

    @Test
    fun `no production code reads the device position or geocodes`() {
        // The platform Geocoder needs no permission and usually resolves over the network, so the manifest rule
        // above would not catch it; this is the half that does.
        ManifestSecurityRules.locationReaders(root).shouldBeEmpty()
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
        ManifestSecurityRules.mutablePendingIntents(fixture) shouldBe listOf("feature/x/src/main/kotlin/A.kt")
        ManifestSecurityRules.broadFileProviderPaths(fixture) shouldBe
            listOf(
                "feature/x/src/main/res/xml/paths.xml: root-path ''",
                "feature/x/src/main/res/xml/paths.xml: files-path '.'",
            )
    }

    @Test
    fun `planted network and location violations are detected`(
        @TempDir fixture: File,
    ) {
        write(
            File(fixture, "feature/net/src/main/kotlin/Fetch.kt"),
            "import java.net.HttpURLConnection\nval c = url.openConnection() as HttpURLConnection",
        )
        write(
            File(fixture, "feature/where/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
              |<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
              |<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
              |</manifest>
            """.trimMargin(),
        )
        write(
            File(fixture, "feature/where/src/main/kotlin/Here.kt"),
            "import android.location.LocationManager\nval m: LocationManager? = null",
        )
        // A manifest that only takes a library's permission out is not requesting it (as :wear did, ADR-0019).
        write(
            File(fixture, "watch/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android"
              |  xmlns:tools="http://schemas.android.com/tools">
              |<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:node="remove"/>
              |</manifest>
            """.trimMargin(),
        )

        // The permission and the code that would use it are found separately, so removing either alone still fails.
        ManifestSecurityRules.networkClientUsers(fixture) shouldBe
            listOf("feature/net/src/main/kotlin/Fetch.kt: HttpURLConnection")
        ManifestSecurityRules.manifestsRequestingNetworkState(fixture) shouldBe
            listOf("feature/where/src/main/AndroidManifest.xml")
        ManifestSecurityRules.manifestsRequestingLocation(fixture) shouldBe
            listOf("feature/where/src/main/AndroidManifest.xml")
        ManifestSecurityRules.locationReaders(fixture) shouldBe
            listOf("feature/where/src/main/kotlin/Here.kt: LocationManager")
    }

    @Test
    fun `planted storage and background-work violations are detected`(
        @TempDir fixture: File,
    ) {
        write(
            File(fixture, "feature/store/src/main/AndroidManifest.xml"),
            """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
              |<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
              |</manifest>
            """.trimMargin(),
        )
        write(
            File(fixture, "feature/store/src/main/kotlin/Later.kt"),
            "import androidx.work.PeriodicWorkRequestBuilder\nval w = 1",
        )

        ManifestSecurityRules.manifestsRequestingStorage(fixture) shouldBe
            listOf("feature/store/src/main/AndroidManifest.xml")
        // One file naming two of the shapes is reported once per shape, so a reviewer sees all of them.
        ManifestSecurityRules.backgroundWorkSchedulers(fixture) shouldBe
            listOf(
                "feature/store/src/main/kotlin/Later.kt: androidx.work.",
                "feature/store/src/main/kotlin/Later.kt: PeriodicWorkRequest",
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
