/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** The Material 3 [ColorScheme] holding these colors. */
public fun SchemeColors.toColorScheme(): ColorScheme =
    lightColorScheme(
        primary = color(ColorRole.PRIMARY),
        onPrimary = color(ColorRole.ON_PRIMARY),
        primaryContainer = color(ColorRole.PRIMARY_CONTAINER),
        onPrimaryContainer = color(ColorRole.ON_PRIMARY_CONTAINER),
        inversePrimary = color(ColorRole.INVERSE_PRIMARY),
        secondary = color(ColorRole.SECONDARY),
        onSecondary = color(ColorRole.ON_SECONDARY),
        secondaryContainer = color(ColorRole.SECONDARY_CONTAINER),
        onSecondaryContainer = color(ColorRole.ON_SECONDARY_CONTAINER),
        tertiary = color(ColorRole.TERTIARY),
        onTertiary = color(ColorRole.ON_TERTIARY),
        tertiaryContainer = color(ColorRole.TERTIARY_CONTAINER),
        onTertiaryContainer = color(ColorRole.ON_TERTIARY_CONTAINER),
        background = color(ColorRole.BACKGROUND),
        onBackground = color(ColorRole.ON_BACKGROUND),
        surface = color(ColorRole.SURFACE),
        onSurface = color(ColorRole.ON_SURFACE),
        surfaceVariant = color(ColorRole.SURFACE_VARIANT),
        onSurfaceVariant = color(ColorRole.ON_SURFACE_VARIANT),
        surfaceTint = color(ColorRole.SURFACE_TINT),
        inverseSurface = color(ColorRole.INVERSE_SURFACE),
        inverseOnSurface = color(ColorRole.INVERSE_ON_SURFACE),
        error = color(ColorRole.ERROR),
        onError = color(ColorRole.ON_ERROR),
    ).withRemainingRoles(this)

private fun ColorScheme.withRemainingRoles(colors: SchemeColors): ColorScheme =
    copy(
        errorContainer = colors.color(ColorRole.ERROR_CONTAINER),
        onErrorContainer = colors.color(ColorRole.ON_ERROR_CONTAINER),
        outline = colors.color(ColorRole.OUTLINE),
        outlineVariant = colors.color(ColorRole.OUTLINE_VARIANT),
        scrim = colors.color(ColorRole.SCRIM),
        surfaceBright = colors.color(ColorRole.SURFACE_BRIGHT),
        surfaceDim = colors.color(ColorRole.SURFACE_DIM),
        surfaceContainer = colors.color(ColorRole.SURFACE_CONTAINER),
        surfaceContainerHigh = colors.color(ColorRole.SURFACE_CONTAINER_HIGH),
        surfaceContainerHighest = colors.color(ColorRole.SURFACE_CONTAINER_HIGHEST),
        surfaceContainerLow = colors.color(ColorRole.SURFACE_CONTAINER_LOW),
        surfaceContainerLowest = colors.color(ColorRole.SURFACE_CONTAINER_LOWEST),
        primaryFixed = colors.color(ColorRole.PRIMARY_FIXED),
        primaryFixedDim = colors.color(ColorRole.PRIMARY_FIXED_DIM),
        onPrimaryFixed = colors.color(ColorRole.ON_PRIMARY_FIXED),
        onPrimaryFixedVariant = colors.color(ColorRole.ON_PRIMARY_FIXED_VARIANT),
        secondaryFixed = colors.color(ColorRole.SECONDARY_FIXED),
        secondaryFixedDim = colors.color(ColorRole.SECONDARY_FIXED_DIM),
        onSecondaryFixed = colors.color(ColorRole.ON_SECONDARY_FIXED),
        onSecondaryFixedVariant = colors.color(ColorRole.ON_SECONDARY_FIXED_VARIANT),
        tertiaryFixed = colors.color(ColorRole.TERTIARY_FIXED),
        tertiaryFixedDim = colors.color(ColorRole.TERTIARY_FIXED_DIM),
        onTertiaryFixed = colors.color(ColorRole.ON_TERTIARY_FIXED),
        onTertiaryFixedVariant = colors.color(ColorRole.ON_TERTIARY_FIXED_VARIANT),
    )

private fun SchemeColors.color(role: ColorRole): Color = Color(get(role))

/** This scheme with its background, surface and lowest surfaces on pure black (the BLACK theme mode). */
public fun ColorScheme.withBlackSurfaces(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
    )

/**
 * The color scheme for [appearance]. [platformDynamic] returns the platform's dynamic light or dark scheme, or is
 * `null` when the platform has none; [seedColor] seeds generated schemes.
 */
public fun colorSchemeFor(
    appearance: Appearance,
    seedColor: Int,
    platformDynamic: ((dark: Boolean) -> ColorScheme)?,
): ColorScheme {
    val dynamic = platformDynamic?.invoke(appearance.dark)
    if (dynamic != null && appearance.source == ColorSource.PLATFORM_DYNAMIC) {
        return if (appearance.black) dynamic.withBlackSurfaces() else dynamic
    }
    val dynamicSeed = dynamic?.primary?.toArgb()?.takeIf { appearance.source == ColorSource.DYNAMIC_SEED }
    val seed = dynamicSeed ?: seedColor
    return SchemeGenerator
        .generate(seed, dark = appearance.dark, black = appearance.black, contrast = appearance.contrast)
        .toColorScheme()
}

/** The screen background gradient of [scheme]: from its primary container at the top down to its surface. */
public fun backgroundGradient(scheme: ColorScheme): Brush =
    Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.surface))
