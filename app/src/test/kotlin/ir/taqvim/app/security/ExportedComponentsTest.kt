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
 * exported implicitly, and only the documented permissions are requested — no network, no calendar.
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
        val allowed = MergedManifest.permissionAllowlist(resource("security/requested-permissions.txt"))

        val appPermissions =
            permissions.filterNot {
                it.endsWith(DYNAMIC_RECEIVER_PERMISSION) ||
                    it in TEST_ONLY_PERMISSIONS
            }
        assertEquals(allowed.toSet(), appPermissions.toSet())
        assertTrue(permissions.any { it.endsWith(DYNAMIC_RECEIVER_PERMISSION) })
    }

    @Test
    fun noCalendarPermissionIsRequested() {
        // ADR-0051, named rather than left to the set comparison above: the set would also fail, but it would fail
        // with "these two sets differ", and the one permission whose absence is a promise to users deserves to say
        // so when it comes back. The allowlist itself must not name one either, or this would pass by being updated.
        val permissions = MergedManifest.requestedPermissions(manifest)
        val allowed = MergedManifest.permissionAllowlist(resource("security/requested-permissions.txt"))

        CALENDAR_PERMISSIONS.forEach { permission ->
            assertTrue(
                "$permission is in the merged manifest; Taqvim does not read the device calendar",
                permission !in permissions,
            )
            assertTrue("$permission is on the allowlist; it must never be", permission !in allowed)
        }
    }

    @Test
    fun noNetworkPermissionIsRequested() {
        // ADR-0054, named for the same reason as the calendar permissions above: without INTERNET no dependency can
        // open a socket however it is written, and that is the whole weight behind "Taqvim sends nothing anywhere".
        val permissions = MergedManifest.requestedPermissions(manifest)
        val allowed = MergedManifest.permissionAllowlist(resource("security/requested-permissions.txt"))

        NETWORK_PERMISSIONS.forEach { permission ->
            assertTrue(
                "$permission is in the merged manifest; Taqvim does not use the network",
                permission !in permissions,
            )
            assertTrue("$permission is on the allowlist; it must never be", permission !in allowed)
        }
    }

    @Test
    fun permissionAllowlistLinesAreValidated() {
        assertEquals(listOf("a.B"), MergedManifest.permissionAllowlist("# comment\n\na.B | why\n"))
        assertTrue(runCatching { MergedManifest.permissionAllowlist("a.B") }.isFailure)
        assertTrue(runCatching { MergedManifest.permissionAllowlist("a.B | ") }.isFailure)
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

        /** ADR-0051: neither may ever appear, in the manifest or on the allowlist. */
        val CALENDAR_PERMISSIONS: Set<String> =
            setOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR")

        /** ADR-0054: neither may ever appear either. */
        val NETWORK_PERMISSIONS: Set<String> =
            setOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
    }
}
