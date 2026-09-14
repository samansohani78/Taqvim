/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.praytimes.PrayerTimesResult
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ProgressRing
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SunArc
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.component.TopBar

/** The Times tab (T-1100), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun TimesScreen(
    state: TimesUiState,
    actions: TimesActions,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.times_title), subtitle = (content as? TimesContent.Day)?.placeName) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (content) {
                TimesContent.Loading -> {
                    val description = stringResource(R.string.times_loading)
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).semantics { contentDescription = description },
                    )
                }

                TimesContent.NoLocation -> {
                    EmptyState(
                        title = stringResource(R.string.times_no_location_title),
                        message = stringResource(R.string.times_no_location_message),
                    )
                }

                is TimesContent.Day -> {
                    DayTimes(content, state.expanded, actions)
                }
            }
        }
    }
}

@Composable
private fun DayTimes(
    day: TimesContent.Day,
    expanded: Boolean,
    actions: TimesActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DayNavigation(day, actions)
        day.next?.let { NextPrayerCard(it) }
        day.sunPath?.let { path ->
            val description = stringResource(R.string.times_sun_path, path.sunrise, path.sunset)
            SunArc(SunArcModel(path.progress, path.sunrise, path.sunset, description))
        }
        when (day.unavailable) {
            PrayerTimesResult.Reason.POLAR_DAY -> Text(stringResource(R.string.times_polar_day))
            PrayerTimesResult.Reason.POLAR_NIGHT -> Text(stringResource(R.string.times_polar_night))
            null -> TimesList(day, expanded, actions)
        }
        OutlinedButton(onClick = actions.onPrintReport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.times_print_report))
        }
    }
}

@Composable
private fun DayNavigation(
    day: TimesContent.Day,
    actions: TimesActions,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            day.dayTitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = actions.onPreviousDay) { Text(stringResource(R.string.times_previous_day)) }
            if (!day.isToday) {
                TextButton(onClick = actions.onToday) { Text(stringResource(R.string.times_today)) }
            }
            TextButton(onClick = actions.onNextDay) { Text(stringResource(R.string.times_next_day)) }
        }
    }
}

@Composable
private fun NextPrayerCard(next: NextPrayerText) {
    val name = stringResource(next.kind.label)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProgressRing(
            progress = next.progress,
            contentDescription = stringResource(R.string.times_next_prayer_progress, name),
            modifier = Modifier.size(56.dp),
        )
        // Updated every minute; a polite live region lets TalkBack read the new countdown (T-1700).
        Text(
            stringResource(R.string.times_next_prayer, name, next.remaining),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun TimesList(
    day: TimesContent.Day,
    expanded: Boolean,
    actions: TimesActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        day.rows.forEach { TimeRowItem(it) }
        TextButton(onClick = actions.onToggleExpanded, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (expanded) R.string.times_show_fewer else R.string.times_show_all))
        }
    }
}

@Composable
private fun TimeRowItem(row: TimeRow) {
    val colors = MaterialTheme.colorScheme
    val highlight = if (row.isNext) colors.primaryContainer else Color.Transparent
    val textColor = if (row.isNext) colors.onPrimaryContainer else colors.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .background(highlight, RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) { selected = row.isNext }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(row.kind.label), color = textColor, style = MaterialTheme.typography.bodyLarge)
        Text(
            row.time ?: stringResource(R.string.times_undefined),
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** String resource naming this time. */
@get:StringRes
internal val PrayerKind.label: Int
    get() =
        when (this) {
            PrayerKind.FAJR -> R.string.times_prayer_fajr
            PrayerKind.SUNRISE -> R.string.times_prayer_sunrise
            PrayerKind.DHUHR -> R.string.times_prayer_dhuhr
            PrayerKind.ASR -> R.string.times_prayer_asr
            PrayerKind.SUNSET -> R.string.times_prayer_sunset
            PrayerKind.MAGHRIB -> R.string.times_prayer_maghrib
            PrayerKind.ISHA -> R.string.times_prayer_isha
            PrayerKind.MIDNIGHT -> R.string.times_prayer_midnight
        }
