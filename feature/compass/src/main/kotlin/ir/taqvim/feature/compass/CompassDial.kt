/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MagicNumber") // Drawing proportions of the dial.

package ir.taqvim.feature.compass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Colors of the dial, resolved from the theme once per composition. */
private data class DialColors(
    val ring: Color,
    val tick: Color,
    val north: Color,
    val qibla: Color,
    val sun: Color,
    val moon: Color,
    val path: Color,
    val lubber: Color,
)

/**
 * The compass rose rotated so that the device's heading is at the top, with the Qibla, Sun and Moon markers and,
 * when enabled, the Sun's path over the next 24 hours in a polar projection (zenith at the centre). The dial is
 * decorative for accessibility: the screen describes its content in text.
 */
@Composable
internal fun CompassDial(
    dial: CompassContent.Dial,
    showSunPath: Boolean,
    animatePath: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = dialColors()
    val letters = dialLetters()
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val pathProgress =
        if (showSunPath && animatePath && dial.sunPath.isNotEmpty()) {
            rememberInfiniteTransition(label = "sunPath")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
                    label = "step",
                ).value
        } else {
            null
        }
    Canvas(modifier.fillMaxWidth().aspectRatio(1f)) {
        val radius = min(size.width, size.height) / 2f * 0.9f
        rotate(-dial.headingDegrees) {
            drawRose(radius, colors)
            letters.forEachIndexed { index, letter ->
                val layout = measurer.measure(letter, letterStyle)
                val position = polar(index * 90f, radius * 0.78f)
                rotate(index * 90f, position) {
                    drawText(layout, topLeft = position - Offset(layout.size.width / 2f, layout.size.height / 2f))
                }
            }
            dial.qibla?.let { drawQibla(it.azimuthDegrees, radius, colors.qibla) }
            dial.sun?.let { drawBody(it, radius, colors.sun) }
            dial.moon?.let { drawBody(it, radius, colors.moon) }
            if (showSunPath) drawSunPath(dial.sunPath, radius, colors.path, pathProgress)
        }
        drawLine(colors.lubber, polar(0f, radius * 1.08f), polar(0f, radius * 0.9f), strokeWidth = 6f)
    }
}

@Composable
private fun dialColors(): DialColors {
    val scheme = MaterialTheme.colorScheme
    return DialColors(
        ring = scheme.outline,
        tick = scheme.onSurfaceVariant,
        north = scheme.error,
        qibla = scheme.primary,
        sun = scheme.tertiary,
        moon = scheme.secondary,
        path = scheme.tertiary.copy(alpha = 0.6f),
        lubber = scheme.onSurface,
    )
}

/** The localized north, east, south and west letters, in that order. */
@Composable
private fun dialLetters(): List<String> =
    listOf(
        stringResource(R.string.compass_letter_north),
        stringResource(R.string.compass_letter_east),
        stringResource(R.string.compass_letter_south),
        stringResource(R.string.compass_letter_west),
    )

private fun DrawScope.polar(
    azimuthDegrees: Float,
    distance: Float,
): Offset {
    val radians = Math.toRadians(azimuthDegrees.toDouble())
    return center + Offset((distance * sin(radians)).toFloat(), (-distance * cos(radians)).toFloat())
}

private fun DrawScope.drawRose(
    radius: Float,
    colors: DialColors,
) {
    drawCircle(colors.ring, radius, style = Stroke(width = 3f))
    for (degrees in 0 until 360 step 5) {
        val inner = if (degrees % 30 == 0) 0.88f else 0.94f
        drawLine(colors.tick, polar(degrees.toFloat(), radius * inner), polar(degrees.toFloat(), radius), 2f)
    }
    drawLine(colors.north, center, polar(0f, radius * 0.6f), strokeWidth = 8f)
}

private fun DrawScope.drawQibla(
    azimuthDegrees: Float,
    radius: Float,
    color: Color,
) {
    val tip = polar(azimuthDegrees, radius * 0.66f)
    drawLine(color, center, tip, strokeWidth = 10f)
    drawRect(color, topLeft = tip - Offset(14f, 14f), size = Size(28f, 28f))
}

private fun DrawScope.drawBody(
    marker: BodyMarker,
    radius: Float,
    color: Color,
) {
    val position = polar(marker.azimuthDegrees, radius * 1.0f)
    if (marker.aboveHorizon) {
        drawCircle(color, 16f, position)
    } else {
        drawCircle(color, 16f, position, style = Stroke(width = 4f))
    }
}

private fun DrawScope.drawSunPath(
    path: List<PathPoint>,
    radius: Float,
    color: Color,
    progress: Float?,
) {
    val points = path.map { polar(it.azimuthDegrees, radius * (1f - it.altitudeDegrees / 90f)) }
    points.forEach { drawCircle(color, 6f, it) }
    if (progress != null && points.isNotEmpty()) {
        val index = (progress * points.size).toInt().coerceAtMost(points.lastIndex)
        drawCircle(color.copy(alpha = 1f), 12f, points[index])
    }
}
