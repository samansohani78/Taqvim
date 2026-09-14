/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-1804 manifest audit (ADR-0017): the merged manifest exports exactly the allowlisted components, nothing is
 * exported implicitly, and only the documented permissions are requested — network access only for subscriptions.
 */
class ExportedComponentsTest {
    private val manifest = MergedManifest.unitTestManifest()

    @Test
    fun theMergedManifestExportsExactlyTheAllowlistedComponents() {
        val allowlist = MergedManifest.allowlist(resource("security/exported-components.txt"))

        assertEquals(
            allowlist.map { it.component }.toSet(),
            MergedManifest.exportedComponents(manifest),
        )
    }

    @Test
    fun everyComponentWithAnIntentFilterDeclaresWhetherItIsExported() {
        assertEquals(emptyList<String>(), MergedManifest.implicitlyExported(manifest))
    }

    @Test
    fun onlyTheDocumentedPermissionsAreRequested() {
        val permissions = MergedManifest.requestedPermissions(manifest)

        val appPermissions =
            permissions.filterNot {
                it.endsWith(DYNAMIC_RECEIVER_PERMISSION) ||
                    it in TEST_ONLY_PERMISSIONS
            }
        assertEquals(EXPECTED_PERMISSIONS, appPermissions.toSet())
        assertTrue(permissions.any { it.endsWith(DYNAMIC_RECEIVER_PERMISSION) })
    }

    @Test
    fun allowlistLinesAreValidated() {
        val parsed = MergedManifest.allowlist("# comment\n\na.B | - | all | why\n")

        assertEquals(listOf(AllowedComponent(ExportedComponent("a.B", "-"), "all")), parsed)
        assertTrue(runCatching { MergedManifest.allowlist("a.B | -") }.isFailure)
    }

    private fun resource(path: String): String =
        requireNotNull(ExportedComponentsTest::class.java.getResource("/$path")) { "missing $path" }.readText()

    private companion object {
        /** AndroidX core adds `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (signature-level). */
        const val DYNAMIC_RECEIVER_PERMISSION = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

        /** Merged into the unit-test manifest by androidx.test core; not part of the app. */
        val TEST_ONLY_PERMISSIONS: Set<String> = setOf("android.permission.REORDER_TASKS")

        val EXPECTED_PERMISSIONS: Set<String> =
            setOf(
                // T-1003 subscriptions only, off until the user allows them; ACCESS_NETWORK_STATE is WorkManager's.
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                // T-603 location, T-602 device calendar: requested at runtime.
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_CALENDAR",
                // T-604 scheduler, T-1103 day change, T-1102 athan, T-1001 reminders.
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.VIBRATE",
                "android.permission.WAKE_LOCK",
                "android.permission.ACCESS_NOTIFICATION_POLICY",
            )
    }
}
