/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private const val DISC_FILL = 0.95f
private const val TOP_ANGLE = -90f
private const val BOTTOM_ANGLE = 90f
private const val LEFT_ANGLE = 180f
private const val HALF_TURN_SWEEP = 180f
private const val DASH_ON = 6f
private const val DASH_OFF = 6f
private val MOON_OUTLINE = 1.dp
private val ARC_STROKE = 2.dp
private val SUN_RADIUS = 7.dp
private val ARC_HEIGHT = 72.dp

/** The lit part of a disc of [radius] around [center] for illuminated [fraction], lit limb on the right. */
internal fun moonLitPath(
    center: Offset,
    radius: Float,
    fraction: Float,
): Path =
    Path().apply {
        moveTo(center.x, center.y - radius)
        arcTo(Rect(center, radius), TOP_ANGLE, HALF_TURN_SWEEP, false)
        val semiAxis = radius * MoonGeometry.terminatorScale(fraction)
        val terminator = Rect(center.x - semiAxis, center.y - radius, center.x + semiAxis, center.y + radius)
        arcTo(terminator, BOTTOM_ANGLE, MoonGeometry.terminatorSweep(fraction), false)
        close()
    }

/**
 * The Moon's phase (T-701): [illuminatedFraction] (0 new … 1 full) lit from the right when [waxing], as seen from the
 * northern hemisphere; [rotationDegrees] turns the disc (e.g. 180 for the southern hemisphere). Not mirrored in RTL.
 */
@Composable
public fun MoonDisc(
    illuminatedFraction: Float,
    waxing: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
) {
    val colors = MaterialTheme.colorScheme
    val lit = colors.tertiary
    val shadow = colors.surfaceContainerHighest
    val outline = colors.outlineVariant
    Canvas(
        modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            role = Role.Image
        },
    ) {
        val radius = size.minDimension / 2 * DISC_FILL
        rotate(rotationDegrees) {
            scale(scaleX = if (waxing) 1f else -1f, scaleY = 1f) {
                drawCircle(shadow, radius)
                drawPath(moonLitPath(center, radius, illuminatedFraction), lit)
                drawCircle(outline, radius, style = Stroke(MOON_OUTLINE.toPx()))
            }
        }
    }
}

/**
 * What a [SunArc] shows: [progress] of daylight (0 at sunrise, 1 at sunset; `null` or outside at night), the labels
 * under both ends (e.g. sunrise and sunset times) and the spoken summary.
 */
@Immutable
public data class SunArcModel(
    public val progress: Float?,
    public val startLabel: String,
    public val endLabel: String,
    public val contentDescription: String,
)

/** The Sun's daily path (T-701): a dashed arc over the horizon, the travelled part and the Sun when it is up. */
@Composable
public fun SunArc(
    model: SunArcModel,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val palette = SunArcPalette(track = colors.outlineVariant, travelled = colors.primary, sun = colors.tertiary)
    Column(modifier.clearAndSetSemantics { contentDescription = model.contentDescription }) {
        Canvas(Modifier.fillMaxWidth().height(ARC_HEIGHT)) { drawSunArc(model.progress, rtl, palette) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(model.startLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(model.endLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}

private data class SunArcPalette(
    val track: Color,
    val travelled: Color,
    val sun: Color,
)

private fun DrawScope.drawSunArc(
    progress: Float?,
    rtl: Boolean,
    palette: SunArcPalette,
) {
    val inset = SUN_RADIUS.toPx()
    val width = size.width - 2 * inset
    val arcHeight = size.height - 2 * inset
    val baseline = size.height - inset
    val arcTopLeft = Offset(inset, inset)
    val arcSize = Size(width, 2 * arcHeight)
    val stroke = ARC_STROKE.toPx()
    drawLine(palette.track, Offset(0f, baseline), Offset(size.width, baseline), stroke)
    val dashes = PathEffect.dashPathEffect(floatArrayOf(DASH_ON * stroke, DASH_OFF * stroke))
    val dashed = Stroke(stroke, pathEffect = dashes)
    drawArc(palette.track, LEFT_ANGLE, HALF_TURN_SWEEP, false, arcTopLeft, arcSize, style = dashed)
    if (SunArcGeometry.isAboveHorizon(progress)) {
        val fraction = progress ?: 0f
        val (start, sweep) = SunArcGeometry.travelledArc(fraction, rtl)
        drawArc(
            palette.travelled,
            start,
            sweep,
            false,
            arcTopLeft,
            arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        val sun = SunArcGeometry.sunPosition(fraction, width, arcHeight, arcHeight, rtl)
        drawCircle(palette.sun, inset, Offset(sun.x + inset, sun.y + inset))
    }
}

/**
 * A circular progress indicator (T-701), e.g. for countdowns (F-09): [progress] 0‥1 fills clockwise from the top
 * (counter-clockwise in RTL) around optional centered [content]. Announced with [contentDescription] and its range.
 */
@Composable
public fun ProgressRing(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val track = colors.surfaceContainerHighest
    val indicator = colors.primary
    Box(
        modifier
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(unitFraction(progress), 0f..1f)
            }.drawBehind {
                val stroke = strokeWidth.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val ring = Size(diameter, diameter)
                drawArc(track, 0f, 360f, false, topLeft, ring, style = Stroke(stroke))
                val sweep = ProgressRingGeometry.sweep(progress, rtl)
                val style = Stroke(stroke, cap = StrokeCap.Round)
                drawArc(indicator, ProgressRingGeometry.START_ANGLE, sweep, false, topLeft, ring, style = style)
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
