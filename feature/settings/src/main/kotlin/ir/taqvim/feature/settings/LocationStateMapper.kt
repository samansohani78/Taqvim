/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import ir.taqvim.core.i18n.NumeralSystem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Formats ports' values into [LocationSettingsUiState] parts. */
internal object LocationStateMapper {
    fun current(
        place: PlaceChoice?,
        numerals: NumeralSystem,
    ): CurrentPlace? =
        place?.let {
            CurrentPlace(it.kind, it.name, CoordinateInput.format(it.coordinates, numerals), it.timeZoneId)
        }

    fun rows(
        options: List<CityOption>,
        numerals: NumeralSystem,
        chosen: PlaceChoice?,
    ): ImmutableList<CityRow> =
        options
            .map { option ->
                CityRow(
                    id = option.id,
                    name = option.name,
                    detail = option.detail,
                    coordinates = CoordinateInput.format(option.coordinates, numerals),
                    selectable = option.timeZoneId != null,
                    selected = chosen?.kind == PlaceKind.CITY && chosen.cityId == option.id,
                )
            }.toImmutableList()

    fun saved(
        place: PlaceChoice,
        numerals: NumeralSystem,
    ): DeviceState.Saved =
        DeviceState.Saved(place.name, CoordinateInput.format(place.coordinates, numerals), place.timeZoneId)
}
