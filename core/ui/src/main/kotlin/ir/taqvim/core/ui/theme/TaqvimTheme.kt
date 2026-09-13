/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import ir.taqvim.core.i18n.TextDirection

/** Background decoration of the current theme: an optional [gradient] and an optional user [backgroundImage]. */
@Immutable
public data class ThemeDecoration(
    public val gradient: Brush? = null,
    public val backgroundImage: ImageBitmap? = null,
)

/** Decoration provided by [TaqvimTheme]; screens draw it through [TaqvimBackground]. */
public val LocalThemeDecoration: ProvidableCompositionLocal<ThemeDecoration> =
    staticCompositionLocalOf { ThemeDecoration() }

/** Opacity of the background-colored veil over a user background image, which keeps text readable on it. */
private const val IMAGE_VEIL_ALPHA = 0.72f

/** The decoration for [settings] with [scheme] and an optional [backgroundImage]. */
public fun themeDecoration(
    settings: ThemeSettings,
    scheme: ColorScheme,
    backgroundImage: ImageBitmap?,
): ThemeDecoration =
    ThemeDecoration(
        gradient = if (settings.gradient) backgroundGradient(scheme) else null,
        backgroundImage = backgroundImage,
    )

/** The platform's wallpaper-derived schemes (Android 12+). */
@RequiresApi(Build.VERSION_CODES.S)
internal class PlatformDynamicSchemes(
    private val context: Context,
) : (Boolean) -> ColorScheme {
    override fun invoke(dark: Boolean): ColorScheme =
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/**
 * Taqvim's Material 3 theme (T-700): colors from [settings] (system/light/dark/black, dynamic color, custom seed, high
 * contrast), the type scale in the optional user [fontFamily] and bold setting, the gradient and [backgroundImage]
 * decoration, and the layout [direction] of the app language (which may differ from the system locale).
 */
@Composable
public fun TaqvimTheme(
    settings: ThemeSettings,
    direction: TextDirection,
    fontFamily: FontFamily? = null,
    backgroundImage: ImageBitmap? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appearance = settings.resolve(isSystemInDarkTheme(), supportsDynamicColor)
    val scheme =
        remember(appearance, settings.seedColor, context) {
            val platform = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PlatformDynamicSchemes(context) else null
            colorSchemeFor(appearance, settings.seedColor, platform)
        }
    val typography = remember(fontFamily, settings.boldText) { taqvimTypography(fontFamily, settings.boldText) }
    val decoration = remember(settings, scheme, backgroundImage) { themeDecoration(settings, scheme, backgroundImage) }
    CompositionLocalProvider(
        LocalLayoutDirection provides direction.toLayoutDirection(),
        LocalThemeDecoration provides decoration,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** A full-screen background: the theme background color, then the gradient or the veiled background image. */
@Composable
public fun TaqvimBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val decoration = LocalThemeDecoration.current
    val colors = MaterialTheme.colorScheme
    val gradient = decoration.gradient
    val base = modifier.background(colors.background)
    Box(if (gradient == null) base else base.background(gradient)) {
        val image = decoration.backgroundImage
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = IMAGE_VEIL_ALPHA)))
        }
        content()
    }
}
