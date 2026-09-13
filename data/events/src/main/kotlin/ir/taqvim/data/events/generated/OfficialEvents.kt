/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.generated

import ir.taqvim.core.events.EventDefinition

/**
 * Events compiled from the dataset (D-08).
 *
 * Generated from `dataset/` by `:tools:dataset:generateEvents` — do not edit.
 */
public object OfficialEvents {
    /**
     * Every dataset event, sorted by id.
     */
    public val ALL: List<EventDefinition> =
        OFFICIAL_EVENTS_PART_1 +
            OFFICIAL_EVENTS_PART_2 +
            OFFICIAL_EVENTS_PART_3 +
            OFFICIAL_EVENTS_PART_4
}
