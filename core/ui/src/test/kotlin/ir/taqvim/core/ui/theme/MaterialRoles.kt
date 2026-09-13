/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** The property of a Material 3 scheme holding each role, written out independently of the production mapping. */
private val MATERIAL_PROPERTIES: Map<ColorRole, (ColorScheme) -> Color> =
    mapOf(
        ColorRole.PRIMARY to ColorScheme::primary,
        ColorRole.ON_PRIMARY to ColorScheme::onPrimary,
        ColorRole.PRIMARY_CONTAINER to ColorScheme::primaryContainer,
        ColorRole.ON_PRIMARY_CONTAINER to ColorScheme::onPrimaryContainer,
        ColorRole.INVERSE_PRIMARY to ColorScheme::inversePrimary,
        ColorRole.SECONDARY to ColorScheme::secondary,
        ColorRole.ON_SECONDARY to ColorScheme::onSecondary,
        ColorRole.SECONDARY_CONTAINER to ColorScheme::secondaryContainer,
        ColorRole.ON_SECONDARY_CONTAINER to ColorScheme::onSecondaryContainer,
        ColorRole.TERTIARY to ColorScheme::tertiary,
        ColorRole.ON_TERTIARY to ColorScheme::onTertiary,
        ColorRole.TERTIARY_CONTAINER to ColorScheme::tertiaryContainer,
        ColorRole.ON_TERTIARY_CONTAINER to ColorScheme::onTertiaryContainer,
        ColorRole.BACKGROUND to ColorScheme::background,
        ColorRole.ON_BACKGROUND to ColorScheme::onBackground,
        ColorRole.SURFACE to ColorScheme::surface,
        ColorRole.ON_SURFACE to ColorScheme::onSurface,
        ColorRole.SURFACE_VARIANT to ColorScheme::surfaceVariant,
        ColorRole.ON_SURFACE_VARIANT to ColorScheme::onSurfaceVariant,
        ColorRole.SURFACE_TINT to ColorScheme::surfaceTint,
        ColorRole.INVERSE_SURFACE to ColorScheme::inverseSurface,
        ColorRole.INVERSE_ON_SURFACE to ColorScheme::inverseOnSurface,
        ColorRole.ERROR to ColorScheme::error,
        ColorRole.ON_ERROR to ColorScheme::onError,
        ColorRole.ERROR_CONTAINER to ColorScheme::errorContainer,
        ColorRole.ON_ERROR_CONTAINER to ColorScheme::onErrorContainer,
        ColorRole.OUTLINE to ColorScheme::outline,
        ColorRole.OUTLINE_VARIANT to ColorScheme::outlineVariant,
        ColorRole.SCRIM to ColorScheme::scrim,
        ColorRole.SURFACE_BRIGHT to ColorScheme::surfaceBright,
        ColorRole.SURFACE_DIM to ColorScheme::surfaceDim,
        ColorRole.SURFACE_CONTAINER to ColorScheme::surfaceContainer,
        ColorRole.SURFACE_CONTAINER_HIGH to ColorScheme::surfaceContainerHigh,
        ColorRole.SURFACE_CONTAINER_HIGHEST to ColorScheme::surfaceContainerHighest,
        ColorRole.SURFACE_CONTAINER_LOW to ColorScheme::surfaceContainerLow,
        ColorRole.SURFACE_CONTAINER_LOWEST to ColorScheme::surfaceContainerLowest,
        ColorRole.PRIMARY_FIXED to ColorScheme::primaryFixed,
        ColorRole.PRIMARY_FIXED_DIM to ColorScheme::primaryFixedDim,
        ColorRole.ON_PRIMARY_FIXED to ColorScheme::onPrimaryFixed,
        ColorRole.ON_PRIMARY_FIXED_VARIANT to ColorScheme::onPrimaryFixedVariant,
        ColorRole.SECONDARY_FIXED to ColorScheme::secondaryFixed,
        ColorRole.SECONDARY_FIXED_DIM to ColorScheme::secondaryFixedDim,
        ColorRole.ON_SECONDARY_FIXED to ColorScheme::onSecondaryFixed,
        ColorRole.ON_SECONDARY_FIXED_VARIANT to ColorScheme::onSecondaryFixedVariant,
        ColorRole.TERTIARY_FIXED to ColorScheme::tertiaryFixed,
        ColorRole.TERTIARY_FIXED_DIM to ColorScheme::tertiaryFixedDim,
        ColorRole.ON_TERTIARY_FIXED to ColorScheme::onTertiaryFixed,
        ColorRole.ON_TERTIARY_FIXED_VARIANT to ColorScheme::onTertiaryFixedVariant,
    )

/** The color [scheme] holds for [role]. */
fun materialColor(
    scheme: ColorScheme,
    role: ColorRole,
): Color = MATERIAL_PROPERTIES.getValue(role)(scheme)
