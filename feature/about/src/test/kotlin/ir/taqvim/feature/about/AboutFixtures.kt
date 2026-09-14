/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.runtime.Composable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import java.util.concurrent.atomic.AtomicInteger

/** Synthetic app facts, diagnostics with planted personal data, and a small license catalog. */
internal object AboutFixtures {
    const val SUPPORT = "support@taqvim.example"
    const val TITLE = "Dentist with Sara"
    const val TOKEN = "abc123secret"
    const val OWNER = "owner@mail.example"

    val info =
        AboutInfo(
            versionName = "1.0.0",
            versionCode = 42,
            buildType = "debug",
            languageCode = "en",
            links = AboutLinks(website = "https://taqvim.example", supportEmail = SUPPORT),
        )

    val device = DeviceInfo("Google", "Pixel 8", "16", 36)

    val entries =
        listOf(
            DiagnosticEntry(
                1_789_000_000_000,
                DiagnosticLevel.ERROR,
                "Subscriptions",
                "Fetch https://cal.example.org/feed.ics?token=$TOKEN failed",
            ),
            DiagnosticEntry(1_788_999_990_000, DiagnosticLevel.WARN, "Reminders", "Reminder for \"$TITLE\" was late"),
            DiagnosticEntry(1_788_999_980_000, DiagnosticLevel.INFO, "Location", "Fix at 35.689123,51.389456 from gps"),
            DiagnosticEntry(1_788_999_970_000, DiagnosticLevel.DEBUG, "Backup", "Exported for $OWNER"),
        )

    /** Texts that must never appear in shown, copied, shared or reported diagnostics. */
    val personalData = listOf(TITLE, "Sara", TOKEN, "35.689123", "51.389456", OWNER, "feed.ics")

    val apache =
        LicenseInfo(
            id = "Apache-2.0",
            name = "Apache License 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0",
            textAsset = "licenses/texts/Apache-2.0.txt",
        )
    val mit = LicenseInfo("MIT", "MIT License", null, null)

    val catalog =
        LicenseCatalog(
            listOf(apache, mit),
            listOf(
                ThirdPartyComponent("androidx.core:core:1.13.1", listOf("Apache-2.0")),
                ThirdPartyComponent("androidx.core:core:1.16.0", listOf("Apache-2.0")),
                ThirdPartyComponent("io.github.cosinekitty:astronomy:2.1.19", listOf("MIT")),
            ),
        )

    val texts =
        ReportTexts(
            subject = "Report",
            app = { name, code, build -> "App: $name ($code, $build)" },
            device = { "Device: $it" },
            android = { release, sdk -> "Android: $release (API $sdk)" },
            language = { "Language: $it" },
            diagnostics = { "Diagnostics ($it):" },
            noDiagnostics = "No diagnostics",
        )
}

/** A license source over [catalog] (`null` fails) and [texts] by asset, counting catalog loads. */
internal class FakeLicenses(
    private val catalog: LicenseCatalog?,
    private val texts: Map<String, String> = emptyMap(),
) : LicenseCatalogSource {
    val catalogCalls = AtomicInteger()

    override suspend fun catalog(): LicenseCatalog {
        catalogCalls.incrementAndGet()
        return checkNotNull(catalog) { "catalog unavailable" }
    }

    override suspend fun text(asset: String): String = checkNotNull(texts[asset]) { "missing $asset" }
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun AboutTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
