/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/** The compass screen (T-1302), stateless; [animatePath] is off in screenshots. */
@Composable
fun CompassScreen(
    state: CompassUiState,
    actions: CompassActions,
    modifier: Modifier = Modifier,
    animatePath: Boolean = true,
) {
    val content = state.content
    ScreenSurface(
        modifier = modifier,
        topBar = {
            TopBar(stringResource(R.string.compass_title), subtitle = (content as? CompassContent.Dial)?.placeName)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (content) {
                CompassContent.Loading -> {
                    val description = stringResource(R.string.compass_loading)
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).semantics { contentDescription = description },
                    )
                }

                CompassContent.SensorUnavailable -> {
                    EmptyState(
                        title = stringResource(R.string.compass_unavailable_title),
                        message = stringResource(R.string.compass_unavailable_message),
                    )
                }

                is CompassContent.Dial -> {
                    DialContent(content, state, actions, animatePath)
                }
            }
        }
    }
}

@Composable
private fun DialContent(
    dial: CompassContent.Dial,
    state: CompassUiState,
    actions: CompassActions,
    animatePath: Boolean,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeadingText(dial)
        Text(
            stringResource(
                if (dial.north == NorthReference.TRUE) R.string.compass_true_north else R.string.compass_magnetic_north,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (dial.accuracy == CompassAccuracy.LOW || dial.accuracy == CompassAccuracy.UNRELIABLE) {
            Text(
                stringResource(R.string.compass_calibrate_hint),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        CompassDial(dial, state.showSunPath, animatePath, Modifier.clearAndSetSemantics {})
        Markers(dial)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.frozen,
                onClick = actions.onToggleFrozen,
                label = { Text(stringResource(R.string.compass_stopped)) },
            )
            if (dial.sunPath.isNotEmpty()) {
                FilterChip(
                    selected = state.showSunPath,
                    onClick = actions.onToggleSunPath,
                    label = { Text(stringResource(R.string.compass_sun_path)) },
                )
            }
        }
    }
}

@Composable
private fun HeadingText(dial: CompassContent.Dial) {
    val announcedPoint = stringResource(dial.announcedCardinal.label)
    val announcement = stringResource(R.string.compass_heading_announcement, dial.announcedText, announcedPoint)
    Text(
        stringResource(R.string.compass_heading, dial.headingText, stringResource(dial.cardinal.label)),
        style = MaterialTheme.typography.displaySmall,
        modifier =
            Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            },
    )
}

@Composable
private fun Markers(dial: CompassContent.Dial) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val qibla = dial.qibla
        when {
            dial.placeName == null -> {
                Text(stringResource(R.string.compass_choose_place))
            }

            qibla == null -> {
                Text(stringResource(R.string.compass_qibla_undefined))
            }

            else -> {
                val turn =
                    when {
                        qibla.aligned -> stringResource(R.string.compass_qibla_ahead)
                        qibla.turnDegrees > 0f -> stringResource(R.string.compass_turn_right, qibla.turnText)
                        else -> stringResource(R.string.compass_turn_left, qibla.turnText)
                    }
                Text(stringResource(R.string.compass_qibla, qibla.azimuthText, turn))
            }
        }
        dial.sun?.let { BodyText(R.string.compass_sun, it) }
        dial.moon?.let { BodyText(R.string.compass_moon, it) }
    }
}

@Composable
private fun BodyText(
    @StringRes name: Int,
    marker: BodyMarker,
) {
    val position =
        if (marker.aboveHorizon) {
            stringResource(R.string.compass_above_horizon, marker.altitudeText)
        } else {
            stringResource(R.string.compass_below_horizon, marker.altitudeText)
        }
    Text(stringResource(R.string.compass_body, stringResource(name), marker.azimuthText, position))
}

/** String resource naming this compass point. */
@get:StringRes
internal val Cardinal.label: Int
    get() =
        when (this) {
            Cardinal.N -> R.string.compass_point_n
            Cardinal.NE -> R.string.compass_point_ne
            Cardinal.E -> R.string.compass_point_e
            Cardinal.SE -> R.string.compass_point_se
            Cardinal.S -> R.string.compass_point_s
            Cardinal.SW -> R.string.compass_point_sw
            Cardinal.W -> R.string.compass_point_w
            Cardinal.NW -> R.string.compass_point_nw
        }
