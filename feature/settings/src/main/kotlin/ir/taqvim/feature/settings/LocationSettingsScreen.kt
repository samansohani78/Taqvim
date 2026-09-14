/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

/**
 * Location settings (T-1502), stateless: renders [state] and reports user actions through [actions]; [embedded] leaves
 * out the title bar when the settings are a page of another screen.
 */
@Composable
fun LocationSettingsScreen(
    state: LocationSettingsUiState,
    actions: LocationSettingsActions,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { if (!embedded) TopBar(stringResource(R.string.settings_location_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.settings_location_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                LocationContent(state, actions)
            }
        }
    }
}

@Composable
private fun LocationContent(
    state: LocationSettingsUiState,
    actions: LocationSettingsActions,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CurrentPlaceCard(state.current)
        SegmentedTabs(
            tabs = LocationMode.entries.map { stringResource(it.label) },
            selectedIndex = state.mode.ordinal,
            onSelect = { actions.onModeSelected(LocationMode.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        when (state.mode) {
            LocationMode.CITY -> CitySearchSection(state.search, actions)
            LocationMode.DEVICE -> DeviceSection(state.device, actions)
            LocationMode.COORDINATES -> ManualSection(state.manual, actions)
        }
    }
}

@Composable
private fun CurrentPlaceCard(current: CurrentPlace?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.settings_location_current),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (current == null) {
            Text(stringResource(R.string.settings_location_none), style = MaterialTheme.typography.titleMedium)
        } else {
            current.name?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            LtrText(current.coordinates)
            LtrText(current.timeZoneId)
            Text(
                stringResource(current.kind.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Numbers and IANA ids read left to right in every layout direction. */
@Composable
internal fun LtrText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@get:StringRes
private val LocationMode.label: Int
    get() =
        when (this) {
            LocationMode.CITY -> R.string.settings_location_tab_city
            LocationMode.DEVICE -> R.string.settings_location_tab_device
            LocationMode.COORDINATES -> R.string.settings_location_tab_coordinates
        }

@get:StringRes
private val PlaceKind.label: Int
    get() =
        when (this) {
            PlaceKind.CITY -> R.string.settings_location_kind_city
            PlaceKind.DEVICE -> R.string.settings_location_kind_device
            PlaceKind.COORDINATES -> R.string.settings_location_kind_coordinates
        }
