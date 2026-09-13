/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One event of a day: its [title], the [color] of its calendar or source, and the [contentDescription] read by
 * accessibility services (title, time and source). [isHoliday] draws the title in the holiday color.
 */
@Immutable
public data class EventChipModel(
    public val title: String,
    public val color: Color,
    public val contentDescription: String,
    public val isHoliday: Boolean = false,
)
