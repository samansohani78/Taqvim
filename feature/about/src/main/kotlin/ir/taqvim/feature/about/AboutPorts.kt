/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import android.os.Build
import kotlinx.coroutines.flow.Flow

/** Facts about the installed app, supplied by `:app` (BuildConfig and preferences). */
data class AboutInfo(
    val versionName: String,
    val versionCode: Long,
    /** Build type, e.g. `release`. */
    val buildType: String,
    /** The app language code (e.g. `fa`), included in problem reports. */
    val languageCode: String,
    val links: AboutLinks = AboutLinks(),
)

/** Public links of the About page, supplied by the caller; `null` hides a link. */
data class AboutLinks(
    val website: String? = null,
    val sourceCode: String? = null,
    val privacyPolicy: String? = null,
    /** Address of problem reports; without it the report goes to the share sheet instead of e-mail. */
    val supportEmail: String? = null,
)

/** The app facts; `:app` binds it from BuildConfig and the preferences. */
fun interface AboutInfoSource {
    fun about(): Flow<AboutInfo>
}

/** Severity of a diagnostics entry, lowest first. */
enum class DiagnosticLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** One local diagnostics entry. */
data class DiagnosticEntry(
    val atEpochMillis: Long,
    val level: DiagnosticLevel,
    val tag: String,
    val message: String,
)

/** The newest local diagnostics entries (the T-601 ring buffer), newest first. */
fun interface DiagnosticsSource {
    fun recent(limit: Int): Flow<List<DiagnosticEntry>>
}

/**
 * A crash of an earlier run, kept in app-private storage (T-1504). [text] is the whole stored record — the facts of
 * the run and the stack trace — and is redacted like every other diagnostic before it is shown or reported.
 */
data class CrashReport(
    val atEpochMillis: Long,
    val text: String,
)

/** The crashes stored since the user last cleared them, newest first. */
interface CrashReportSource {
    fun crashes(): Flow<List<CrashReport>>

    /** Forgets every stored crash. */
    suspend fun clear()
}

/** Device facts included in a problem report: no identifiers, only the maker, model and Android version. */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
)

/** The device facts of a problem report. */
fun interface DeviceInfoSource {
    fun device(): DeviceInfo
}

/** Device facts from `android.os.Build`. */
class BuildDeviceInfoSource : DeviceInfoSource {
    override fun device(): DeviceInfo =
        DeviceInfo(Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
}

/** The bundled open-source license catalog and license texts. */
interface LicenseCatalogSource {
    suspend fun catalog(): LicenseCatalog

    /** The text of the license [asset] (a path such as `licenses/texts/MIT.txt`). */
    suspend fun text(asset: String): String
}
