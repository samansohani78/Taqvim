/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.runtime.Composable
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.TimeZones
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Sample places and instants for the Astronomy screen tests (rounded sample coordinates, not official data). */
object AstronomyFixtures {
    fun language(code: String): LanguageSpec = requireNotNull(LanguageTable.forCode(code)) { "no language $code" }

    fun tehran(
        languageCode: String = "en",
        scorpio: ZodiacSystem = ZodiacSystem.IAU_CONSTELLATION,
    ): AstronomySettings =
        AstronomySettings(
            placeName = "Tehran",
            place = Coordinates(35.69, 51.39),
            timeZone = TimeZones.TEHRAN,
            language = language(languageCode),
            scorpioSystem = scorpio,
        )

    /** Tromsø: inside the Arctic Circle, the Sun does not set around the June solstice. */
    fun tromso(): AstronomySettings =
        AstronomySettings(
            placeName = "Tromso",
            place = Coordinates(69.65, 18.96),
            timeZone = TimeZone.of("Europe/Oslo"),
            language = language("en"),
        )

    /** [isoLocal] (e.g. `2026-06-21T12:00`) in the zone of [settings]. */
    fun at(
        isoLocal: String,
        settings: AstronomySettings,
    ): Instant = LocalDateTime.parse(isoLocal).toInstant(settings.timeZone)

    /** Content of [settings] at [instant] with a fresh cache. */
    fun sky(
        settings: AstronomySettings,
        instant: Instant,
        isNow: Boolean = true,
    ): AstronomyContent.Sky =
        AstronomyStateMapper
            .content(settings, instant, isNow, AstronomyHeaderCache())
            .shouldBeInstanceOf<AstronomyContent.Sky>()
}

/** The app theme with fixed (non-dynamic) colors for tests and screenshots. */
@Composable
fun AstronomyTestTheme(
    rtl: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val settings = ThemeSettings(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
    TaqvimTheme(settings, if (rtl) TextDirection.RTL else TextDirection.LTR, content = content)
}
