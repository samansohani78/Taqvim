/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar
import kotlin.math.min

/** Degrees of tilt that move the bubble to the edge of its vial. */
private const val FULL_SCALE_DEGREES = 10f

/** The bubble level and ruler (T-1303), stateless. */
@Composable
fun LevelScreen(
    state: LevelUiState,
    actions: LevelActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(modifier = modifier, topBar = { TopBar(stringResource(R.string.level_title)) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SegmentedTabs(
                tabs = listOf(stringResource(R.string.level_tab_level), stringResource(R.string.level_tab_ruler)),
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onSelectTab(LevelTab.entries[it]) },
            )
            when (state.tab) {
                LevelTab.LEVEL -> LevelTabContent(state.content, actions)
                LevelTab.RULER -> RulerTabContent(state, actions)
            }
        }
    }
}

@Composable
private fun LevelTabContent(
    content: LevelContent,
    actions: LevelActions,
) {
    when (content) {
        LevelContent.Loading -> {
            val description = stringResource(R.string.level_loading)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = description })
            }
        }

        LevelContent.SensorUnavailable -> {
            EmptyState(
                title = stringResource(R.string.level_unavailable_title),
                message = stringResource(R.string.level_unavailable_message),
            )
        }

        is LevelContent.Reading -> {
            ReadingContent(content, actions)
        }
    }
}

@Composable
private fun ReadingContent(
    reading: LevelContent.Reading,
    actions: LevelActions,
) {
    val summary =
        if (reading.isLevel) {
            stringResource(R.string.level_is_level)
        } else if (reading.orientation == DeviceOrientation.FLAT) {
            stringResource(R.string.level_tilt_two_axes, reading.xText, reading.yText)
        } else {
            stringResource(R.string.level_tilt_one_axis, reading.xText)
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(reading.orientation.label), style = MaterialTheme.typography.titleMedium)
        Text(
            summary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Bubble(reading, Modifier.clearAndSetSemantics {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onCalibrate) { Text(stringResource(R.string.level_calibrate)) }
            if (reading.calibrated) {
                TextButton(onClick = actions.onResetCalibration) {
                    Text(stringResource(R.string.level_reset_calibration))
                }
            }
        }
    }
}

@Composable
private fun Bubble(
    reading: LevelContent.Reading,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val bubbleColor = if (reading.isLevel) colors.primary else colors.tertiary
    val flat = reading.orientation == DeviceOrientation.FLAT
    Canvas(modifier.fillMaxWidth().aspectRatio(if (flat) 1f else 4f)) {
        val bubbleRadius = min(size.width, size.height) * if (flat) 0.08f else 0.3f
        val travelX = size.width / 2f - bubbleRadius
        val travelY = if (flat) size.height / 2f - bubbleRadius else 0f
        val dx = (reading.x / FULL_SCALE_DEGREES).coerceIn(-1f, 1f) * travelX
        val dy = if (flat) (-reading.y / FULL_SCALE_DEGREES).coerceIn(-1f, 1f) * travelY else 0f
        if (flat) {
            drawCircle(colors.outline, min(size.width, size.height) / 2f, style = Stroke(width = 3f))
            drawCircle(colors.outline, bubbleRadius * 1.4f, style = Stroke(width = 2f))
        } else {
            drawRoundRect(colors.outline, style = Stroke(width = 3f))
            drawLine(
                colors.outline,
                Offset(center.x - bubbleRadius * 1.4f, 0f),
                Offset(center.x - bubbleRadius * 1.4f, size.height),
            )
            drawLine(
                colors.outline,
                Offset(center.x + bubbleRadius * 1.4f, 0f),
                Offset(center.x + bubbleRadius * 1.4f, size.height),
            )
        }
        drawCircle(bubbleColor, bubbleRadius, center + Offset(dx, dy))
    }
}

@Composable
private fun RulerTabContent(
    state: LevelUiState,
    actions: LevelActions,
) {
    val dotsPerInch = LocalResources.current.displayMetrics.xdpi
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurface)
    val description = stringResource(R.string.level_ruler_description)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedTabs(
            tabs = listOf(stringResource(R.string.level_unit_cm), stringResource(R.string.level_unit_inch)),
            selectedIndex = state.rulerUnit.ordinal,
            onSelect = { actions.onSelectUnit(RulerUnit.entries[it]) },
        )
        // The ruler starts at the physical left edge in both layout directions: it measures from the screen's edge.
        Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = description }) {
            RulerMath.ticks(size.width, dotsPerInch, state.rulerUnit).forEach { tick ->
                val length =
                    when (tick.kind) {
                        TickKind.MAJOR -> size.height * 0.5f
                        TickKind.MEDIUM -> size.height * 0.35f
                        TickKind.MINOR -> size.height * 0.2f
                    }
                drawLine(colors.onSurface, Offset(tick.positionPx, 0f), Offset(tick.positionPx, length), 2f)
                tick.label?.let { value ->
                    val layout = measurer.measure(Numerals.localizeDigits(value.toString(), state.numerals), labelStyle)
                    drawText(layout, topLeft = Offset(tick.positionPx + 4f, length))
                }
            }
        }
    }
}

/** String resource naming this orientation. */
@get:StringRes
internal val DeviceOrientation.label: Int
    get() =
        when (this) {
            DeviceOrientation.FLAT -> R.string.level_orientation_flat
            DeviceOrientation.PORTRAIT -> R.string.level_orientation_portrait
            DeviceOrientation.LANDSCAPE -> R.string.level_orientation_landscape
        }
