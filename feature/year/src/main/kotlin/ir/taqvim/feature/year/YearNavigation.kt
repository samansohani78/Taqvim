/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.runtime.Immutable
import ir.taqvim.core.model.Jdn

/** Where the year view leads; the app's navigation provides these. */
@Immutable
class YearNavigation(
    /** Opens the calendar at the month beginning on the given first day. */
    val onOpenMonth: (firstDay: Jdn) -> Unit = {},
)
