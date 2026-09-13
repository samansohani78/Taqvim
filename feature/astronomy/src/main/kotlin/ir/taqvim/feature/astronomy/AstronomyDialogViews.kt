/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.ui.component.DatePickerLabels
import ir.taqvim.core.ui.component.DatePickerModel
import ir.taqvim.core.ui.component.DatePickerSheet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DIALOG_MAX_HEIGHT = 480.dp
private const val HALF_TURN = 180f
private const val INNER_RADIUS = 0.55f
private const val STROKE = 2f
private const val ANGLE_STROKE = 4f

/** Cusps 1, 4, 7 and 10 are the angles (ascendant, IC, descendant, midheaven). */
private const val HOUSES_PER_QUADRANT = 3

/** The open Astronomy dialog with a close button. */
@Composable
internal fun AstronomyDialogHost(
    dialog: AstronomyDialog,
    onDismiss: () -> Unit,
) {
    val (title, body) = dialogTitleAndBody(dialog)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.astronomy_close)) } },
        title = { Text(title) },
        text = {
            Column(
                Modifier.heightIn(max = DIALOG_MAX_HEIGHT).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) { body() }
        },
    )
}

@Composable
private fun dialogTitleAndBody(dialog: AstronomyDialog): Pair<String, @Composable () -> Unit> =
    when (dialog) {
        is AstronomyDialog.Horoscope -> {
            val kind = if (dialog.yearly) AstronomyDialogKind.YEAR_HOROSCOPE else AstronomyDialogKind.HOROSCOPE
            stringResource(AstronomyLabels.dialog(kind)) to { HoroscopeBody(dialog) }
        }

        is AstronomyDialog.PlanetaryHours -> {
            stringResource(R.string.astronomy_planetary_hours_of, dialog.dayTitle) to { PlanetaryHoursBody(dialog) }
        }

        is AstronomyDialog.MoonInScorpio -> {
            stringResource(R.string.astronomy_moon_in_scorpio_in, dialog.yearTitle) to { MoonInScorpioBody(dialog) }
        }
    }

@Composable
private fun HoroscopeBody(dialog: AstronomyDialog.Horoscope) {
    val at = if (dialog.yearly) R.string.astronomy_year_horoscope_at else R.string.astronomy_horoscope_at
    Text(stringResource(at, dialog.at), style = MaterialTheme.typography.bodyMedium)
    val chart = dialog.chart
    if (chart == null) {
        Text(stringResource(R.string.astronomy_polar_houses))
        return
    }
    val wheel = stringResource(R.string.astronomy_chart_wheel)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ChartWheel(chart.cuspLongitudes, Modifier.size(200.dp).semantics { contentDescription = wheel })
    }
    Text(stringResource(if (chart.dayChart) R.string.astronomy_day_chart else R.string.astronomy_night_chart))
    InfoRow(stringResource(R.string.astronomy_ascendant), position(chart.ascendant))
    InfoRow(stringResource(R.string.astronomy_midheaven), position(chart.midheaven))
    chart.cusps.forEach { InfoRow(stringResource(R.string.astronomy_house, it.house), position(it.position)) }
    InfoRow(stringResource(R.string.astronomy_part_of_fortune), position(chart.fortune))
    InfoRow(stringResource(R.string.astronomy_part_of_spirit), position(chart.spirit))
}

@Composable
private fun position(position: SignPosition): String =
    stringResource(
        R.string.astronomy_sign_position,
        position.degrees,
        stringResource(AstronomyLabels.sign(position.sign)),
    )

@Composable
private fun PlanetaryHoursBody(dialog: AstronomyDialog.PlanetaryHours) {
    if (dialog.hours.isEmpty()) {
        Text(stringResource(R.string.astronomy_no_sunrise_sunset))
        return
    }
    val day = stringResource(R.string.astronomy_day_hour)
    val night = stringResource(R.string.astronomy_night_hour)
    dialog.hours.forEach { hour ->
        val planet = stringResource(AstronomyLabels.planet(hour.ruler))
        val label = "${hour.number}. $planet (${if (hour.daytime) day else night})"
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { selected = hour.isCurrent }) {
            InfoRow(label, hour.span)
        }
    }
}

@Composable
private fun MoonInScorpioBody(dialog: AstronomyDialog.MoonInScorpio) {
    val system =
        when (dialog.system) {
            ZodiacSystem.TROPICAL -> R.string.astronomy_scorpio_tropical
            ZodiacSystem.IAU_CONSTELLATION -> R.string.astronomy_scorpio_constellation
        }
    Text(stringResource(system), style = MaterialTheme.typography.bodySmall)
    if (dialog.periods.isEmpty()) Text(stringResource(R.string.astronomy_no_periods))
    dialog.periods.forEach { Text(stringResource(R.string.astronomy_period, it.start, it.end)) }
}

/** A chart wheel: two circles and the twelve cusps, cusp 1 (ascendant) on the left and longitudes counterclockwise. */
@Composable
private fun ChartWheel(
    cusps: List<Float>,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val angles = MaterialTheme.colorScheme.primary
    Canvas(modifier) { drawChartWheel(cusps, outline, angles) }
}

internal fun DrawScope.drawChartWheel(
    cusps: List<Float>,
    outline: Color,
    angles: Color,
) {
    val radius = size.minDimension / 2
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(outline, radius, center, style = Stroke(STROKE))
    drawCircle(outline, radius * INNER_RADIUS, center, style = Stroke(STROKE))
    val ascendant = cusps.firstOrNull() ?: 0f
    cusps.forEachIndexed { index, longitude ->
        val angle = (HALF_TURN + longitude - ascendant) * PI.toFloat() / HALF_TURN
        val direction = Offset(cos(angle), -sin(angle))
        val angular = index % HOUSES_PER_QUADRANT == 0
        drawLine(
            color = if (angular) angles else outline,
            start = center + direction * (radius * INNER_RADIUS),
            end = center + direction * radius,
            strokeWidth = if (angular) ANGLE_STROKE else STROKE,
        )
    }
}

/** The date picker for the settings' calendar. */
@Composable
internal fun AstronomyDatePicker(
    picker: PickerData,
    actions: AstronomyActions,
) {
    val labels =
        DatePickerLabels(
            title = stringResource(R.string.astronomy_choose_date),
            year = stringResource(R.string.astronomy_picker_year),
            month = stringResource(R.string.astronomy_picker_month),
            day = stringResource(R.string.astronomy_picker_day),
            confirm = stringResource(R.string.astronomy_picker_confirm),
            cancel = stringResource(R.string.astronomy_picker_cancel),
        )
    DatePickerSheet(
        model =
            DatePickerModel(picker.initial, picker.years, picker.monthNames, picker.daysInMonth, picker.digits, labels),
        onConfirm = actions.onDatePicked,
        onDismiss = actions.onDismissPicker,
    )
}
