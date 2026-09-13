/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.MoonDisc
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.SunArc
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.component.TopBar

private const val LAST_MINUTE = 1_439f

/** The Astronomy screen (T-1300), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun AstronomyScreen(
    state: AstronomyUiState,
    actions: AstronomyActions,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    val sky = content as? AstronomyContent.Sky
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.astronomy_title), subtitle = sky?.placeName) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (content) {
                AstronomyContent.Loading -> {
                    val description = stringResource(R.string.astronomy_loading)
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).semantics { contentDescription = description },
                    )
                }

                AstronomyContent.NoLocation -> {
                    EmptyState(
                        title = stringResource(R.string.astronomy_no_location_title),
                        message = stringResource(R.string.astronomy_no_location_message),
                    )
                }

                is AstronomyContent.Sky -> {
                    SkyContent(content, state.mode, actions)
                }
            }
        }
    }
    state.dialog?.let { AstronomyDialogHost(it, actions.onDismissDialog) }
    if (state.pickingDate && sky != null) AstronomyDatePicker(sky.picker, actions)
}

@Composable
private fun SkyContent(
    sky: AstronomyContent.Sky,
    mode: AstronomyMode,
    actions: AstronomyActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SegmentedTabs(
            tabs = AstronomyMode.entries.map { stringResource(AstronomyLabels.mode(it)) },
            selectedIndex = mode.ordinal,
            onSelect = { actions.onMode(AstronomyMode.entries[it]) },
        )
        TimeControls(sky, actions)
        when (mode) {
            AstronomyMode.EARTH -> EarthPanel(sky.earth)
            AstronomyMode.MOON -> MoonPanel(sky.moon)
            AstronomyMode.SUN -> SunPanel(sky.sun)
        }
        HeaderCard(sky.header)
        DialogButtons(actions)
    }
}

@Composable
private fun TimeControls(
    sky: AstronomyContent.Sky,
    actions: AstronomyActions,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(sky.dateTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(sky.timeText, style = MaterialTheme.typography.headlineSmall)
        val sliderLabel = stringResource(R.string.astronomy_time_of_day)
        Slider(
            value = sky.minuteOfDay.toFloat(),
            onValueChange = { actions.onMinuteOfDay(it.toInt()) },
            valueRange = 0f..LAST_MINUTE,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = sliderLabel },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StepButton(R.string.astronomy_previous_year) { actions.onStepYears(-1) }
            StepButton(R.string.astronomy_previous_day) { actions.onStepDays(-1) }
            StepButton(R.string.astronomy_next_day) { actions.onStepDays(1) }
            StepButton(R.string.astronomy_next_year) { actions.onStepYears(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onPickDate) { Text(stringResource(R.string.astronomy_choose_date)) }
            if (!sky.isNow) {
                OutlinedButton(onClick = actions.onNow) { Text(stringResource(R.string.astronomy_now)) }
            }
        }
    }
}

@Composable
private fun StepButton(
    @StringRes label: Int,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) { Text(stringResource(label)) }
}

@Composable
private fun EarthPanel(earth: EarthText) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InfoRow(
            stringResource(R.string.astronomy_daylight),
            earth.dayLength ?: stringResource(R.string.astronomy_no_sunrise_sunset),
        )
        InfoRow(stringResource(R.string.astronomy_subsolar_latitude), earth.subsolarLatitude)
        InfoRow(
            stringResource(R.string.astronomy_next_season),
            stringResource(
                R.string.astronomy_at,
                stringResource(AstronomyLabels.season(earth.nextSeason)),
                earth.nextSeasonAt,
            ),
        )
    }
}

@Composable
private fun MoonPanel(moon: MoonText) {
    val fraction by animateFloatAsState(moon.illuminatedFraction, label = "moonFraction")
    val phase = stringResource(AstronomyLabels.phase(moon.phase))
    val illumination = stringResource(R.string.astronomy_percent, moon.illumination)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MoonDisc(
            illuminatedFraction = fraction,
            waxing = moon.waxing,
            contentDescription = stringResource(R.string.astronomy_moon_description, phase, illumination),
            modifier = Modifier.size(120.dp),
        )
        InfoRow(stringResource(R.string.astronomy_moon_phase), phase)
        InfoRow(stringResource(R.string.astronomy_illumination), illumination)
        InfoRow(stringResource(R.string.astronomy_azimuth), moon.azimuth)
        InfoRow(stringResource(R.string.astronomy_altitude), moon.altitude)
        InfoRow(stringResource(R.string.astronomy_moonrise), moon.rise ?: stringResource(R.string.astronomy_none))
        InfoRow(stringResource(R.string.astronomy_moonset), moon.set ?: stringResource(R.string.astronomy_none))
        InfoRow(
            stringResource(R.string.astronomy_moon_distance),
            stringResource(R.string.astronomy_kilometers, moon.distance),
        )
    }
}

@Composable
private fun SunPanel(sun: SunText) {
    val none = stringResource(R.string.astronomy_none)
    val progress by animateFloatAsState(sun.progress ?: 0f, label = "sunProgress")
    val rise = sun.rise ?: none
    val set = sun.set ?: none
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SunArc(
            SunArcModel(
                progress = sun.progress?.let { progress },
                startLabel = rise,
                endLabel = set,
                contentDescription = stringResource(R.string.astronomy_sun_path, rise, set),
            ),
        )
        InfoRow(stringResource(R.string.astronomy_azimuth), sun.azimuth)
        InfoRow(stringResource(R.string.astronomy_altitude), sun.altitude)
        InfoRow(stringResource(R.string.astronomy_sunrise), rise)
        InfoRow(stringResource(R.string.astronomy_solar_noon), sun.transit ?: none)
        InfoRow(stringResource(R.string.astronomy_sunset), set)
    }
}

@Composable
private fun HeaderCard(header: HeaderText) {
    val none = stringResource(R.string.astronomy_no_eclipse)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.astronomy_header),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            InfoRow(stringResource(R.string.astronomy_sun_sign), stringResource(AstronomyLabels.sign(header.sunSign)))
            InfoRow(stringResource(R.string.astronomy_moon_sign), stringResource(AstronomyLabels.sign(header.moonSign)))
            InfoRow(stringResource(R.string.astronomy_moon_constellation), header.moonConstellation)
            InfoRow(stringResource(R.string.astronomy_moon_phase), stringResource(AstronomyLabels.phase(header.phase)))
            InfoRow(
                stringResource(R.string.astronomy_moon_distance),
                stringResource(R.string.astronomy_kilometers, header.moonDistance),
            )
            InfoRow(
                stringResource(R.string.astronomy_next_season),
                stringResource(
                    R.string.astronomy_at,
                    stringResource(AstronomyLabels.season(header.nextSeason)),
                    header.nextSeasonAt,
                ),
            )
            val solar = header.solarEclipse?.let { eclipse(it) } ?: none
            val lunar = header.lunarEclipse?.let { eclipse(it) } ?: none
            InfoRow(stringResource(R.string.astronomy_next_solar_eclipse), solar)
            InfoRow(stringResource(R.string.astronomy_next_lunar_eclipse), lunar)
        }
    }
}

@Composable
private fun eclipse(eclipse: EclipseText): String =
    stringResource(R.string.astronomy_at, stringResource(AstronomyLabels.eclipse(eclipse.kind)), eclipse.peak)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogButtons(actions: AstronomyActions) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AstronomyDialogKind.entries.forEach { kind ->
            OutlinedButton(onClick = { actions.onOpenDialog(kind) }) {
                Text(stringResource(AstronomyLabels.dialog(kind)))
            }
        }
    }
}

/** A label and its value on one line, read as one item by accessibility services. */
@Composable
internal fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}
