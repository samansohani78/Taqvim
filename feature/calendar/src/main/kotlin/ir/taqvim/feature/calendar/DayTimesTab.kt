/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.SunArc
import ir.taqvim.core.ui.component.SunArcModel

private val ROW_SHAPE = RoundedCornerShape(8.dp)
private const val TWO_DIGITS = 2

/** The Times tab: the place and method, the Sun's arc and the day's times with the next one highlighted. */
@Composable
internal fun DayTimesTab(
    state: DayTimesState,
    language: LanguageSpec,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DayTimesState.Loading -> {
            DetailsLoading(stringResource(R.string.calendar_times_loading), modifier)
        }

        DayTimesState.NoPlace -> {
            EmptyState(
                title = stringResource(R.string.calendar_no_place_title),
                modifier = modifier,
                message = stringResource(R.string.calendar_no_place_message),
            )
        }

        is DayTimesState.Ready -> {
            TimesContent(state.times, language, modifier)
        }
    }
}

@Composable
private fun TimesContent(
    times: DayTimes,
    language: LanguageSpec,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow(
            stringResource(R.string.calendar_times_place_label),
            stringResource(
                R.string.calendar_times_place,
                times.placeName,
                stringResource(DayDetailsLabels.of(times.method)),
            ),
        )
        val reason = times.unavailable
        if (reason != null) {
            Text(stringResource(DayDetailsLabels.of(reason)), style = MaterialTheme.typography.bodyMedium)
        } else {
            SunPath(times, language)
            times.entries.forEach { PrayerRow(it, it.kind == times.next, language) }
        }
    }
}

@Composable
private fun SunPath(
    times: DayTimes,
    language: LanguageSpec,
) {
    val undefined = stringResource(R.string.calendar_time_undefined)
    val sunrise = times.entries.timeOf(PrayerTimeKind.SUNRISE)?.let { localized(it, language) } ?: undefined
    val sunset = times.entries.timeOf(PrayerTimeKind.SUNSET)?.let { localized(it, language) } ?: undefined
    SunArc(
        SunArcModel(
            progress = times.sunProgress,
            startLabel = sunrise,
            endLabel = sunset,
            contentDescription = stringResource(R.string.calendar_sun_path, sunrise, sunset),
        ),
        Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PrayerRow(
    entry: PrayerTimeEntry,
    isNext: Boolean,
    language: LanguageSpec,
) {
    val colors = MaterialTheme.colorScheme
    val nextLabel = stringResource(R.string.calendar_next_time)
    val weight = if (isNext) FontWeight.Bold else FontWeight.Normal
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                selected = isNext
                if (isNext) stateDescription = nextLabel
            }.background(if (isNext) colors.secondaryContainer else Color.Transparent, ROW_SHAPE)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(DayDetailsLabels.of(entry.kind)), fontWeight = weight)
        Text(
            entry.time?.let {
                localized(it, language)
            } ?: stringResource(R.string.calendar_time_undefined),
            fontWeight = weight,
        )
    }
}

private fun List<PrayerTimeEntry>.timeOf(kind: PrayerTimeKind): MinuteOfDay? = firstOrNull { it.kind == kind }?.time

/** `HH:mm` in the digits of [language]. */
internal fun localized(
    time: MinuteOfDay,
    language: LanguageSpec,
): String {
    val text = time.hour.toString().padStart(TWO_DIGITS, '0') + ":" + time.minute.toString().padStart(TWO_DIGITS, '0')
    return Numerals.localizeDigits(text, language.numerals)
}
