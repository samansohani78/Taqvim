/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.konsist

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Source-tree security checks of T-1804 (ADR-0017), run without the Android toolchain. */
object ManifestSecurityRules {
    private const val ANDROID = "http://schemas.android.com/apk/res/android"
    private const val TOOLS = "http://schemas.android.com/tools"
    private val COMPONENT_TAGS = listOf("activity", "activity-alias", "service", "receiver", "provider")
    private val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".kotlin", ".idea")
    private val PENDING_INTENT_CALL = Regex("""PendingIntent\.get(Activity|Broadcast|Service|ForegroundService)\(""")
    private val BROAD_PATH_TAGS = setOf("root-path", "external-path", "external-files-path", "external-media-path")
    private val BROAD_PATHS = setOf("", ".", "/")
    private const val INTERNET = "android.permission.INTERNET"
    private const val NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE"

    /**
     * Ways production code could open a socket (ADR-0054).
     *
     * The permission's absence is the enforcement; this is the half that catches the attempt before the manifest
     * does, so a fetch path cannot be written and sit dormant waiting for someone to add `INTERNET` back. The
     * platform types are named as they are written in source, since an import is a reference like any other.
     */
    private val NETWORK_CLIENT_REFERENCES =
        listOf(
            "HttpURLConnection",
            "java.net.URLConnection",
            "okhttp3",
            "retrofit2",
            "io.ktor.client",
            "java.net.Socket",
            "javax.net.ssl.HttpsURLConnection",
        )

    /**
     * Broad storage permissions. Taqvim reads and writes one document at a time, picked by the user through the
     * Storage Access Framework, which needs none of these; a manifest asking for one would be asking for the
     * whole device's media instead.
     */
    private val STORAGE_PERMISSIONS =
        setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        )

    /**
     * Ways production code could schedule background work (ADR-0054).
     *
     * Taqvim's only background work is one `AlarmManager` alarm. WorkManager is still on the runtime classpath
     * because Glance depends on it, so nothing stops a future change from enqueueing work again — and the
     * subscription refresh that was removed was exactly that. A worker also brings `ACCESS_NETWORK_STATE` back
     * the moment it carries a network constraint.
     */
    private val BACKGROUND_WORK_REFERENCES =
        listOf("androidx.work.", "CoroutineWorker", "WorkManager.getInstance", "PeriodicWorkRequest")

    /**
     * Ways production code could cancel background work wholesale.
     *
     * `JobScheduler.cancelAll()` cancels every job the *app* owns, not every job the caller scheduled — and the app
     * owns the jobs its libraries schedule on its behalf. Glance draws every widget through
     * `androidx.glance.appwidget.AsyncRequestWorker`, which WorkManager schedules as a platform job, so one
     * `cancelAll()` on `MY_PACKAGE_REPLACED` left every placed widget on the launcher's loading spinner after an
     * upgrade (v1.3.0). The reasoning that justified it — "this app schedules no job of any kind" — was true of
     * Taqvim's own source and false of the APK, which is precisely the gap a source-level grep cannot see.
     *
     * So the rule is about the dangerous verb, not about widgets: nothing may cancel work it did not schedule.
     */
    private val WHOLESALE_WORK_CANCELLERS =
        listOf("cancelAll()", "cancelAllWork", "cancelAllWorkByTag")

    /** Device location permissions. ADR-0055: Taqvim requests neither, and no manifest may bring one back. */
    private val LOCATION_PERMISSIONS =
        setOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")

    /**
     * Ways production code could ask the device where it is, or send coordinates to a geocoding service.
     *
     * The permission is the enforcement for the first; the platform `Geocoder` needs none, which is exactly why it
     * is listed — on most devices it resolves over the network, so it would be a hole in the claim that a place is
     * only ever something the user chose.
     */
    private val LOCATION_READER_REFERENCES =
        listOf("LocationManager", "FusedLocationProvider", "android.location.Geocoder", "Geocoder(")

    /** Calendar provider permissions. ADR-0051: Taqvim requests neither, and no manifest may bring one back. */
    private val CALENDAR_PERMISSIONS =
        setOf("android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR")

    /**
     * Ways production code could reach the calendar provider.
     *
     * The permission name alone is not enough: a `ContentResolver` query against the provider authority needs
     * it, but the string that names the permission can be absent while the query is still written. These are
     * the shapes that read it — the contract class, the authority, and the permission constant in any form.
     */
    private val CALENDAR_PROVIDER_REFERENCES =
        listOf("CalendarContract", "com.android.calendar", "READ_CALENDAR", "WRITE_CALENDAR")

    /** `manifest: component` for every component in a source manifest that does not declare `android:exported`. */
    fun componentsWithoutExported(root: File): List<String> =
        sourceManifests(root).flatMap { manifest ->
            components(manifest)
                .filterNot { it.hasAttributeNS(ANDROID, "exported") }
                .map { "${manifest.relativeTo(root).invariantSeparatorsPath}: ${it.getAttributeNS(ANDROID, "name")}" }
        }

    /** Source manifests (relative paths) that request network access. */
    fun manifestsRequestingInternet(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).contains(INTERNET) }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /** Source manifests (relative paths) that request `ACCESS_NETWORK_STATE` (ADR-0054). */
    fun manifestsRequestingNetworkState(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).contains(NETWORK_STATE) }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /**
     * `file: reference` for every main Kotlin source that names an HTTP client (ADR-0054).
     *
     * `:konsist` has no Android toolchain, so this reads the source tree rather than the merged manifest; the
     * manifest half is `auditReleasePermissions`, and the two fail independently.
     */
    fun networkClientUsers(root: File): List<String> =
        mainKotlinFiles(root).flatMap { file ->
            val text = file.readText()
            NETWORK_CLIENT_REFERENCES
                .filter { text.contains(it) }
                .map { "${file.relativeTo(root).invariantSeparatorsPath}: $it" }
        }

    /** Source manifests (relative paths) that request a device location permission (ADR-0055). */
    fun manifestsRequestingLocation(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).any { it in LOCATION_PERMISSIONS } }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /** `file: reference` for every main Kotlin source that reads the device position or geocodes (ADR-0055). */
    fun locationReaders(root: File): List<String> =
        mainKotlinFiles(root).flatMap { file ->
            val text = file.readText()
            LOCATION_READER_REFERENCES
                .filter { text.contains(it) }
                .map { "${file.relativeTo(root).invariantSeparatorsPath}: $it" }
        }

    /** Source manifests (relative paths) that request a broad storage or media permission. */
    fun manifestsRequestingStorage(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).any { it in STORAGE_PERMISSIONS } }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /** `file: reference` for every main Kotlin source that schedules background work (ADR-0054). */
    fun backgroundWorkSchedulers(root: File): List<String> =
        mainKotlinFiles(root).flatMap { file ->
            val text = file.readText()
            BACKGROUND_WORK_REFERENCES
                .filter { text.contains(it) }
                .map { "${file.relativeTo(root).invariantSeparatorsPath}: $it" }
        }

    /** `file: reference` for every main Kotlin source that cancels background work wholesale (T-1200). */
    fun wholesaleWorkCancellers(root: File): List<String> =
        mainKotlinFiles(root).flatMap { file ->
            val text = file.readText()
            WHOLESALE_WORK_CANCELLERS
                .filter { text.contains(it) }
                .map { "${file.relativeTo(root).invariantSeparatorsPath}: $it" }
        }

    /**
     * Source manifests (relative paths) that request a calendar permission.
     *
     * A `tools:node="remove"` entry is not a request, so a manifest taking a library's permission out does not
     * appear here — which is also why this cannot be satisfied by removal alone: nothing may declare one.
     */
    fun manifestsRequestingCalendarAccess(root: File): List<String> =
        sourceManifests(root)
            .filter { manifest -> permissions(manifest).any { it in CALENDAR_PERMISSIONS } }
            .map { it.relativeTo(root).invariantSeparatorsPath }

    /**
     * `file: reference` for every main Kotlin source that names the calendar provider (ADR-0051).
     *
     * This is the check that a removal actually removed something. Deleting the permission from a manifest while
     * the query code stays is a build that silently reads nothing on a device where another app holds the
     * permission for a shared uid, and a single line re-adding the permission turns it back on.
     */
    fun calendarProviderReaders(root: File): List<String> =
        mainKotlinFiles(root).flatMap { file ->
            val text = file.readText()
            CALENDAR_PROVIDER_REFERENCES
                .filter { text.contains(it) }
                .map { "${file.relativeTo(root).invariantSeparatorsPath}: $it" }
        }

    /** Main Kotlin sources that create a `PendingIntent` without `FLAG_IMMUTABLE`, or with `FLAG_MUTABLE`. */
    fun mutablePendingIntents(root: File): List<String> =
        mainKotlinFiles(root)
            .filter { file ->
                val text = file.readText()
                PENDING_INTENT_CALL.containsMatchIn(text) &&
                    (!text.contains("FLAG_IMMUTABLE") || text.contains("FLAG_MUTABLE"))
            }.map { it.relativeTo(root).invariantSeparatorsPath }

    /** `file: tag path` for every `FileProvider` path that shares more than a dedicated sub-directory. */
    fun broadFileProviderPaths(root: File): List<String> =
        walk(root)
            .filter {
                it.isFile && it.extension == "xml" && it.parentFile.name == "xml" &&
                    it.path.contains(
                        "src/main",
                    )
            }.flatMap { file ->
                val document = parse(file)
                if (document.documentElement.tagName != "paths") return@flatMap emptyList()
                children(document.documentElement)
                    .filter { it.tagName in BROAD_PATH_TAGS || it.getAttribute("path").trim() in BROAD_PATHS }
                    .map {
                        "${file.relativeTo(
                            root,
                        ).invariantSeparatorsPath}: ${it.tagName} '${it.getAttribute("path")}'"
                    }
            }

    private fun sourceManifests(root: File): List<File> =
        walk(root).filter { it.isFile && it.name == "AndroidManifest.xml" && it.parentFile.path.endsWith("src/main") }

    private fun mainKotlinFiles(root: File): List<File> =
        walk(root).filter { it.isFile && it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/") }

    private fun walk(root: File): List<File> =
        root
            .walkTopDown()
            .onEnter { it == root || it.name !in SKIPPED_DIRS }
            .toList()

    private fun components(manifest: File): List<Element> {
        val document = parse(manifest)
        return COMPONENT_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
    }

    /** Requested permissions; `tools:node="remove"` entries take a library's permission out and are not requests. */
    private fun permissions(manifest: File): Set<String> {
        val nodes = parse(manifest).getElementsByTagName("uses-permission")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filterNot { it.getAttributeNS(TOOLS, "node") == "remove" }
            .map { it.getAttributeNS(ANDROID, "name") }
            .toSet()
    }

    private fun children(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun parse(file: File) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
}
