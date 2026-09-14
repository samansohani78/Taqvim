/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin bindings of the timeline (ADR-0002). `:app` provides [TimelineSettingsSource], [TimelineClockSource],
 * [TimelinePlaceSource] and [TimelineDaysSource].
 */
val timelineFeatureModule =
    module {
        viewModelOf(::TimelineViewModel)
    }
