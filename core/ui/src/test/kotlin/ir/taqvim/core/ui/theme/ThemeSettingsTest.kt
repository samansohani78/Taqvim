/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ir.taqvim.core.i18n.TextDirection
import org.junit.jupiter.api.Test

class ThemeSettingsTest {
    private val fakeLight = lightColorScheme(primary = Color(0xFF3355AA))
    private val fakeDark = darkColorScheme(primary = Color(0xFFAA5533))
    private val platform: (Boolean) -> ColorScheme = { dark -> if (dark) fakeDark else fakeLight }

    @Test
    fun `settings resolve the mode, color source and contrast`() {
        val defaults = ThemeSettings()
        defaults.resolve(systemDark = true, supportsDynamicColor = true) shouldBe
            Appearance(dark = true, black = false, ColorSource.PLATFORM_DYNAMIC, ContrastLevel.STANDARD)
        defaults.resolve(systemDark = false, supportsDynamicColor = false) shouldBe
            Appearance(dark = false, black = false, ColorSource.CUSTOM_SEED, ContrastLevel.STANDARD)
        ThemeSettings(mode = ThemeMode.BLACK, highContrast = true).resolve(false, true) shouldBe
            Appearance(dark = true, black = true, ColorSource.DYNAMIC_SEED, ContrastLevel.HIGH)
        ThemeSettings(mode = ThemeMode.LIGHT, dynamicColor = false).resolve(true, true) shouldBe
            Appearance(dark = false, black = false, ColorSource.CUSTOM_SEED, ContrastLevel.STANDARD)
        ThemeSettings(mode = ThemeMode.DARK).resolve(false, true).dark shouldBe true
    }

    @Test
    fun `layout direction follows the app language`() {
        TextDirection.RTL.toLayoutDirection() shouldBe LayoutDirection.Rtl
        TextDirection.LTR.toLayoutDirection() shouldBe LayoutDirection.Ltr
    }

    @Test
    fun `color schemes come from the platform or the generator`() {
        val dynamic = Appearance(dark = false, black = false, ColorSource.PLATFORM_DYNAMIC, ContrastLevel.STANDARD)
        colorSchemeFor(dynamic, 0, platform) shouldBeSameInstanceAs fakeLight
        val blackDynamic = colorSchemeFor(dynamic.copy(dark = true, black = true), 0, platform)
        blackDynamic.primary shouldBe fakeDark.primary
        blackDynamic.background shouldBe Color.Black

        val seed = SchemeGenerator.DEFAULT_SEED
        val generated = SchemeGenerator.generate(seed, dark = false).toColorScheme()
        colorSchemeFor(dynamic, seed, null).primary shouldBe generated.primary
        val custom = dynamic.copy(source = ColorSource.CUSTOM_SEED)
        colorSchemeFor(custom, seed, platform).primary shouldBe generated.primary

        val high = Appearance(dark = true, black = false, ColorSource.DYNAMIC_SEED, ContrastLevel.HIGH)
        colorSchemeFor(high, seed, platform).primary.toArgb() shouldBe
            SchemeGenerator.generate(fakeDark.primary.toArgb(), true, false, ContrastLevel.HIGH)[ColorRole.PRIMARY]
    }

    @Test
    fun `generated schemes map every role to Material 3`() {
        val colors = SchemeGenerator.generate(0xFF6A4FB3.toInt(), dark = true, contrast = ContrastLevel.HIGH)
        val scheme = colors.toColorScheme()
        ColorRole.entries.forEach { role -> materialColor(scheme, role).toArgb() shouldBe colors[role] }

        val black = scheme.withBlackSurfaces()
        listOf(black.background, black.surface, black.surfaceDim, black.surfaceContainerLowest)
            .forEach { it shouldBe Color.Black }
        black.primary shouldBe scheme.primary
    }

    @Test
    fun `decoration carries the gradient only when enabled`() {
        val scheme = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = false).toColorScheme()
        themeDecoration(ThemeSettings(gradient = true), scheme, null).gradient shouldBe
            Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.surface))
        themeDecoration(ThemeSettings(), scheme, null).gradient.shouldBeNull()
    }

    @Test
    fun `typography applies the custom font and the bold setting`() {
        val base = Typography()
        taqvimTypography() shouldBe base
        val serif = taqvimTypography(FontFamily.Serif, bold = false)
        serif.bodyLarge.fontFamily shouldBe FontFamily.Serif
        serif.bodyLarge.fontWeight shouldBe base.bodyLarge.fontWeight
        val bold = taqvimTypography(bold = true)
        bold.bodyLarge.fontWeight shouldBe emboldened(base.bodyLarge.fontWeight)
        bold.bodyLarge.fontFamily shouldBe base.bodyLarge.fontFamily
        bold.displayLarge.fontWeight shouldBe emboldened(base.displayLarge.fontWeight)
        bold.labelSmall.fontWeight shouldBe emboldened(base.labelSmall.fontWeight)
        emboldened(null) shouldBe FontWeight.SemiBold
        emboldened(FontWeight.Medium) shouldBe FontWeight.Bold
        emboldened(FontWeight.ExtraBold) shouldBe FontWeight.Black
    }
}
