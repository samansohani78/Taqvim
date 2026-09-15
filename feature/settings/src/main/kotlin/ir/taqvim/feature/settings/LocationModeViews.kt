/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

@Composable
internal fun CitySearchSection(
    search: CitySearchState,
    actions: LocationSettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = search.query,
            onValueChange = actions.onQueryChanged,
            label = { Text(stringResource(R.string.settings_location_search_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (search.searching) {
            val description = stringResource(R.string.settings_location_searching)
            LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
        }
        if (search.noResults) {
            Text(stringResource(R.string.settings_location_no_results, search.query))
        }
        search.results.forEach { row -> CityRowItem(row, actions.onCitySelected) }
    }
}

@Composable
private fun CityRowItem(
    row: CityRow,
    onSelect: (Long) -> Unit,
) {
    val chooseLabel = stringResource(R.string.settings_location_choose_city)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = row.selectable, onClickLabel = chooseLabel) { onSelect(row.id) }
            .semantics(mergeDescendants = true) { selected = row.selected }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (row.selected) {
                Text(
                    stringResource(R.string.settings_location_chosen_city),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        row.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        LtrText(row.coordinates)
        if (!row.selectable) {
            Text(
                stringResource(R.string.settings_location_city_no_zone),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun DeviceSection(
    device: DeviceState,
    actions: LocationSettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.settings_location_device_explanation))
        Button(onClick = actions.onUseDeviceLocation, enabled = device != DeviceState.Locating) {
            Text(stringResource(R.string.settings_location_use_device))
        }
        DeviceStatus(device)
    }
}

@Composable
private fun DeviceStatus(device: DeviceState) {
    val live = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    when (device) {
        DeviceState.Idle -> {}

        DeviceState.Locating -> {
            Row(
                live,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.settings_location_locating))
            }
        }

        DeviceState.PermissionDenied -> {
            ProblemText(R.string.settings_location_denied, live)
        }

        DeviceState.LocationDisabled -> {
            ProblemText(R.string.settings_location_disabled, live)
        }

        DeviceState.TimedOut -> {
            ProblemText(R.string.settings_location_timed_out, live)
        }

        DeviceState.Unavailable -> {
            ProblemText(R.string.settings_location_unavailable, live)
        }

        is DeviceState.Saved -> {
            Column(live) {
                Text(stringResource(R.string.settings_location_device_saved, device.name ?: device.coordinates))
                LtrText(device.coordinates)
                LtrText(device.timeZoneId)
            }
        }
    }
}

@Composable
private fun ProblemText(
    @StringRes text: Int,
    modifier: Modifier,
) {
    Text(stringResource(text), modifier = modifier, color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun ManualSection(
    manual: ManualState,
    actions: LocationSettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LtrField(
            value = manual.latitude,
            onChange = actions.onLatitudeChanged,
            label = R.string.settings_location_latitude,
            supporting = R.string.settings_location_latitude_error.takeIf { manual.latitudeError },
        )
        LtrField(
            value = manual.longitude,
            onChange = actions.onLongitudeChanged,
            label = R.string.settings_location_longitude,
            supporting = R.string.settings_location_longitude_error.takeIf { manual.longitudeError },
        )
        LtrField(
            value = manual.timeZoneId,
            onChange = actions.onTimeZoneChanged,
            label = R.string.settings_location_time_zone,
            supporting =
                if (manual.timeZoneError) {
                    R.string.settings_location_time_zone_error
                } else {
                    R.string.settings_location_time_zone_hint
                },
            isError = manual.timeZoneError,
        )
        manual.suggestedName?.let { Text(stringResource(R.string.settings_location_nearby, it)) }
        Button(onClick = actions.onSaveCoordinates) {
            Text(stringResource(R.string.settings_location_save_coordinates))
        }
        actions.onPickOnMap?.let { pickOnMap ->
            OutlinedButton(onClick = pickOnMap) { Text(stringResource(R.string.settings_location_pick_on_map)) }
        }
        if (manual.saved) {
            Text(
                stringResource(R.string.settings_location_coordinates_saved),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LtrField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes supporting: Int?,
    isError: Boolean = supporting != null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = isError,
        supportingText = supporting?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
    )
}
